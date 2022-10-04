package com.github.raft.core.rpc.vote;

import com.github.raft.core.CommandLog;
import com.github.raft.core.Raft;
import com.github.raft.core.RaftNode;
import com.github.raft.core.RaftNodeRole;
import com.github.raft.core.RemoteRouter;
import com.github.raft.core.appender.CommandLogAppender;
import com.github.raft.core.common.CountWaiter;
import com.github.raft.core.common.ElectionTimer;
import com.github.raft.core.common.IDUtils;
import com.github.raft.core.common.LoggerUtils;
import java.io.Closeable;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-04
 * follower 选举的方法
 */
public class FollowerVoteFunction implements ElectionTimer.ElectionFunction, Closeable {

    private int nodeID;

    private RemoteRouter<RequestVoteRpc> remoteRouter;

    private CommandLogAppender commandLogAppender;

    private VoteListener voteListener;

    private ExecutorService voteExecutorService;

    public FollowerVoteFunction(int nodeID,
                                RemoteRouter<RequestVoteRpc> remoteRouter,
                                CommandLogAppender commandLogAppender,
                                VoteListener voteListener) {
        this.nodeID = nodeID;
        this.remoteRouter = remoteRouter;
        this.commandLogAppender = commandLogAppender;
        this.voteListener = voteListener;
        this.initThreadPool();
    }

    private void initThreadPool() {
        voteExecutorService = Executors.newFixedThreadPool(Raft.nodeCount() - 1,
                r -> new Thread(r, "vote-rpc-" + IDUtils.newID()));
    }

    @Override
    public void startElection() {
        RaftNode raftNode = Raft.getNode(nodeID);
        voteListener.onVoteStart();
        if (!raftNode.restNewTermVote()) {
            return;
        }
        while (!voteExecutorService.isShutdown() && !voteExecutorService.isTerminated()) {
            if (raftNode.getRaftNodeRole() != RaftNodeRole.Candidate) {
                return;
            }
            RequestVote requestVote = new RequestVote();
            requestVote.setTerm(raftNode.getCurTerm());
            requestVote.setCadidateID(raftNode.getId());
            CommandLog commandLog = commandLogAppender.peek();
            if (commandLog != null) {
                requestVote.setLastLongTerm(commandLog.getTerm());
                requestVote.setLastLogIndex(commandLog.getIndex());
            } else {
                requestVote.setLastLongTerm(-1);
                requestVote.setLastLogIndex(-1);
            }
            // 开始拉票
            Set<RaftNode> nodes = Raft.getAllNodes();
            final CountWaiter countWaiter = new CountWaiter(Raft.nodeCount() - 1);
            final AtomicLong newLeaderTerm = new AtomicLong(0);
            for (RaftNode node : nodes) {
                if (node.getId().equals(raftNode.getId())) {
                    continue;
                }
                voteExecutorService.execute(() -> {
                    try {
                        RequestVoteResp resp = remoteRouter.routeRpc(node.getNodeIpPort()).requestVote(requestVote);
                        if (resp.isVoteGranted()) {
                            raftNode.incrVoteNumber();
                            countWaiter.countDownSuccess();
                            LoggerUtils.getLogger()
                                    .info("===> vote response by node " + node.getId() + " , term " + resp.getTerm());
                        } else if (resp.getTerm() > raftNode.getCurTerm()) {
                            newLeaderTerm.set(resp.getTerm());
                        }
                    } catch (Throwable throwable) {
                        countWaiter.countDownException();
                    } finally {
                        countWaiter.countDown();
                    }
                });
            }

            int count = Raft.nodeCount();
            int halfVote = (count >> 1) + 1;
            countWaiter.await(halfVote - 1);
            // 多数节点异常，贼哈重新发起选举
            if (countWaiter.exceptionCount() < halfVote - 1) {
                countWaiter.await();
                continue;
            }
            if (raftNode.getRaftNodeRole() == RaftNodeRole.Candidate) {
                if (raftNode.getVoteNumber() >= halfVote) {
                    LoggerUtils.getLogger().info("【INFO】node {} as leader at term {}", nodeID, requestVote.getTerm());
                    Raft.getNode(nodeID).setLeader(requestVote.getTerm());
                    if (voteListener != null) {
                        voteListener.onVoteEnd(RaftNodeRole.Leader);
                    }
                } else if (newLeaderTerm.longValue() > 0) {
                    Raft.getNode(nodeID).setFollower(newLeaderTerm.longValue());
                    if (voteListener != null) {
                        voteListener.onVoteEnd(RaftNodeRole.Follower);
                    }
                }
            }
            return;
        }
    }

    @Override
    public void close() throws IOException {
        voteExecutorService.shutdownNow();
    }
}
