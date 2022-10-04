package com.github.raft.core.rpc;

import com.github.raft.core.Raft;
import com.github.raft.core.RemoteRouter;
import com.github.raft.core.appender.CommandLogAppender;
import com.github.raft.core.common.ElectionTimer;
import com.github.raft.core.common.HeartbeatCallable;
import com.github.raft.core.rpc.vote.FollowerVoteFunction;
import com.github.raft.core.rpc.vote.RequestVoteRpc;
import com.github.raft.core.rpc.vote.VoteListener;
import java.io.Closeable;
import java.io.IOException;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-01
 */
public class RaftVoteClient implements HeartbeatCallable, Closeable {

    private int nodeID;

    private CommandLogAppender commandLogAppender;

    private RemoteRouter<RequestVoteRpc> requestVoteRpcRemoteRouter;

    private ElectionTimer electionTimer;

    private FollowerVoteFunction followerVoteFunction;

    public RaftVoteClient(int nodeID,
                          CommandLogAppender commandLogAppender,
                          RemoteRouter<RequestVoteRpc> requestVoteRpcRemoteRouter,
                          VoteListener voteListener) {
        this.nodeID = nodeID;
        this.commandLogAppender = commandLogAppender;
        this.requestVoteRpcRemoteRouter = requestVoteRpcRemoteRouter;
        this.init(voteListener);
        Raft.holdHeartbeatCallable(this);
    }

    private void init(VoteListener voteListener) {
        this.followerVoteFunction = new FollowerVoteFunction(nodeID,
                requestVoteRpcRemoteRouter,
                this.commandLogAppender,
                voteListener);
        this.electionTimer = new ElectionTimer(Raft.getRaftConfig().getElectionMs(),
                this.followerVoteFunction);
        this.electionTimer.startTimer();
    }



    @Override
    public void onHeartbeat() {
        electionTimer.resetTimer();
    }

    @Override
    public void close() throws IOException {
        this.electionTimer.stopTimer();
        this.followerVoteFunction.close();
    }
}
