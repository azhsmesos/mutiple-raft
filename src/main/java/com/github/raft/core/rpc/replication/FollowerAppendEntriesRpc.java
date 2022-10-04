package com.github.raft.core.rpc.replication;

import com.github.raft.core.CommandCommit;
import com.github.raft.core.CommandLog;
import com.github.raft.core.Raft;
import com.github.raft.core.RaftNode;
import com.github.raft.core.StateMachine;
import com.github.raft.core.appender.CommandLogAppender;
import com.github.raft.core.common.HeartbeatCallable;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-02
 * flollower 接收 leader请求
 */
public class FollowerAppendEntriesRpc implements AppendEntriesRpc {

    private int nodeID;

    private CommandCommit commandCommit;

    private CommandLogAppender commandLogAppender;

    public FollowerAppendEntriesRpc(int nodeID, CommandLogAppender commandLogAppender, StateMachine stateMachine) {
        this.nodeID = nodeID;
        this.commandLogAppender = commandLogAppender;
        this.commandCommit = new CommandCommit(commandLogAppender, stateMachine);
    }

    @Override
    public AppendEntriesResp appendCommand(AppendEntries appendEntries) {
        resetHeartbeatTimer();
        RaftNode raftNode = Raft.getNode(nodeID);
        AppendEntriesResp resp = null;
        // 自身term大于AppendEntries的term，并且额leader的term小于自身的term
        if (raftNode.getCurTerm() > appendEntries.getTerm()
                && Raft.getNode(appendEntries.getLeaderID()).getCurTerm() < raftNode.getCurTerm()) {
            resp = new AppendEntriesResp(raftNode.getCurTerm(), false);
            return resp;
        }
        CommandLog commandLog = commandLogAppender.index(appendEntries.getPrevLogIndex());
        // 在index处的term不同
        if ((appendEntries.getPrevLogIndex() >= 0 && commandLog == null) || (commandLog != null
                && commandLog.getTerm() != appendEntries.getPrevLogTerm())) {
            resp = new AppendEntriesResp(raftNode.getCurTerm(), false);
            return resp;
        }
        if (appendEntries.getEntries() == null || appendEntries.getEntries().length == 0) {
            handleVoteResult(appendEntries.getLeaderID(), appendEntries.getTerm());
            resp = new AppendEntriesResp(appendEntries.getTerm(), true);
            return resp;
        }
        CommandLog[] entires = appendEntries.getEntries();
        int notExistStartIndex = 0;
        for (int i = 0; i < entires.length; i++) {
            CommandLog log = entires[i];
            commandLog = commandLogAppender.index(log.getIndex());
            if (commandLog != null && commandLog.getTerm() != log.getTerm()) {
                // 删除存在，但是不一致的日志
                commandLogAppender.removeRange(log.getTerm(), log.getIndex());
                notExistStartIndex = i + 1;
                break;
            }
        }

        for (; notExistStartIndex < entires.length; notExistStartIndex++) {
            commandLogAppender.append(entires[notExistStartIndex]);
            CommandLog copyCommandLog = entires[notExistStartIndex];
            if (copyCommandLog.getStatus() == 1) {
                this.commit(copyCommandLog.getTerm(), copyCommandLog.getIndex());
            }
        }
        resp = new AppendEntriesResp(appendEntries.getTerm(), true);
        if (appendEntries.getLeaderCommit() > raftNode.curCommitIndex()) {
            raftNode.seekCommitIndex(appendEntries.getLeaderCommit());
        }
        return resp;
    }

    @Override
    public Boolean commit(Long term, Long index) {
        CommandLog commandLog = commandLogAppender.index(index);
        commandCommit.commit(commandLog);
        Raft.getNode(nodeID).seekCommitIndex(index);
        return true;
    }

    /**
     * 接收到请求时，就会回调心跳监听器
     */
    private void resetHeartbeatTimer() {
        HeartbeatCallable heartbeatCallable = Raft.getHeartbeatCallback();
        if (heartbeatCallable != null) {
            heartbeatCallable.onHeartbeat();
        }
    }

    private void handleVoteResult(int leaderNodeID, long leaderTerm) {
        Raft.getNode(leaderNodeID).setLeader(leaderTerm);
    }
}
