package com.github.raft.core.rpc;

import com.github.raft.core.CommandCommit;
import com.github.raft.core.CommandLog;
import com.github.raft.core.Raft;
import com.github.raft.core.RaftNode;
import com.github.raft.core.RaftNodeRole;
import com.github.raft.core.RemoteRouter;
import com.github.raft.core.StateMachine;
import com.github.raft.core.appender.CommandLogAppender;
import com.github.raft.core.common.HeartbeatCallable;
import com.github.raft.core.common.HeartbeatScheduler;
import com.github.raft.core.common.LoggerUtils;
import com.github.raft.core.rpc.replication.AppendEntries;
import com.github.raft.core.rpc.replication.AppendEntriesRpc;
import com.github.raft.core.rpc.replication.LeaderAppendEntriesClient;
import com.github.raft.core.rpc.vote.VoteListener;
import java.io.Closeable;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-02
 * 选举具体实现，只有leader可以接收请求，如果当前节点非leader，则返回leader信息
 * 让客户端重定向leader
 */
public class RaftCommandClient implements VoteListener, Closeable {

    private int nodeID;
    private CommandCommit commandCommit;
    private CommandLogAppender commandLogAppender;
    private LeaderAppendEntriesClient leaderAppendEntriesClient;
    private volatile HeartbeatScheduler heartbeatScheduler;

    public RaftCommandClient(int nodeID, CommandLogAppender commandLogAppender, StateMachine machine,
                             RemoteRouter<AppendEntriesRpc> remoteRouter) {
        this.nodeID = nodeID;
        this.commandCommit = new CommandCommit(commandLogAppender, machine);
        this.commandLogAppender = commandLogAppender;
        this.leaderAppendEntriesClient = new LeaderAppendEntriesClient(nodeID, remoteRouter, commandLogAppender);
        this.seekCommitIndex(nodeID);
        this.initCurNodeTerm();
    }

    public synchronized CommandResp handleCommand(byte[] command) {
        CommandResp resp = new CommandResp();
        if (Raft.getNode(nodeID).getRaftNodeRole() != RaftNodeRole.Leader) {
            resp.setSuccess(false);
            for (RaftNode raftNode : Raft.getAllNodes()) {

                if (raftNode.getRaftNodeRole() == RaftNodeRole.Leader) {
                    resp.setRedirectToLeader(raftNode.getNodeIpPort());
                    break;
                }
            }
            if (resp.getRedirectToLeader() == null) {
                throw new RuntimeException("the leader has not been elected");
            }
            return resp;
        }
        CommandLog commandLog = new CommandLog();
        commandLog.setCommand(command);
        commandLog.setTerm(Raft.getNode(nodeID).getCurTerm());
        commandLog.setIndex(nextLogIndex());
        commandLog.setStatus(0);
        AppendEntries appendEntries = leaderAppendEntriesClient.newAppendEntries();
        appendEntries.setEntries(new CommandLog[]{commandLog});
        boolean success = leaderAppendEntriesClient.appendCommand(appendEntries);
        resp.setSuccess(success);
        if (success) {
            commandCommit.commit(commandLog);
            Raft.getNode(nodeID).seekCommitIndex(commandLog.getIndex());
            leaderAppendEntriesClient.commit(commandLog);
        }
        return resp;
    }

    @Override
    public void onVoteStart() {
        closeHeartbeat();
    }

    @Override
    public void onVoteEnd(RaftNodeRole curRaftNodeRole) {
        closeHeartbeat();
        if (curRaftNodeRole == RaftNodeRole.Leader) {
            heartbeatScheduler = new HeartbeatScheduler(Raft.getRaftConfig().getHeartbeatMs(),
                    () -> {
                        if (leaderAppendEntriesClient.sendHeartbeatCommand()) {
                            resetHeartbeatTimer();
                        }
                    });
        }
    }

    @Override
    public void close() throws IOException {
        LoggerUtils.getLogger().debug("raft command client close...");
        closeHeartbeat();
        leaderAppendEntriesClient.close();
    }

    private synchronized long nextLogIndex() {
        long commitIndex = Raft.getNode(nodeID).curCommitIndex();
        return commitIndex + 1;
    }

    /**
     * 初始化节点commitIndex
     * @param nodeID
     */
    private void seekCommitIndex(int nodeID) {
        RaftNode raftNode = Raft.getNode(nodeID);
        if (raftNode != null) {
            raftNode.seekCommitIndex(commandCommit.maxCommitIndex());
        }
    }

    /**
     * 初始化节点任期
     */
    private void initCurNodeTerm() {
        CommandLog log = commandLogAppender.peek();
        long term = log == null ? 0 : log.getTerm();
        RaftNode raftNode = Raft.getNode(nodeID);
        if (raftNode != null) {
            raftNode.initCurTerm(term);
        }
    }

    private void closeHeartbeat() {
        if (heartbeatScheduler != null) {
            heartbeatScheduler.stopHeartbeat();
            heartbeatScheduler = null;
        }
    }

    /**
     * Follower在接收Leader心跳包时重置选举超时
     * Leader在发送心跳包时重置选举超时
     * Leader停止发送心跳包说明任期过期
     */
    private void resetHeartbeatTimer() {
        try {
            HeartbeatCallable heartbeatCallable = Raft.getHeartbeatCallback();
            if (heartbeatCallable != null) {
                heartbeatCallable.onHeartbeat();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
