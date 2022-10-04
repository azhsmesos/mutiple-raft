package com.github.raft.demo;

import com.github.raft.core.Raft;
import com.github.raft.core.RemoteRouter;
import com.github.raft.core.StateMachine;
import com.github.raft.core.appender.CommandLogAppender;
import com.github.raft.core.common.NodeIpPort;
import com.github.raft.core.common.SignalManager;
import com.github.raft.core.rpc.RaftCommandClient;
import com.github.raft.core.rpc.RaftVoteClient;
import com.github.raft.core.rpc.replication.AppendEntriesRpc;
import com.github.raft.core.rpc.vote.RequestVoteRpc;
import java.io.IOException;
import java.util.Set;
import sun.misc.SignalHandler;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-04
 */
public class RaftBootstarter {

    public static RaftCommandClient bootstarterRaftNode(int curNodeID,
                                                        Set<NodeIpPort> nodeIpPorts,
                                                        StateMachine stateMachine,
                                                        CommandLogAppender commandLogAppender) {
        // server
        RaftServer raftServer = new RaftServer(curNodeID, nodeIpPorts, stateMachine, commandLogAppender);
        raftServer.startRaftServer();

        // client
        RaftClient raftClient = new RaftClient(curNodeID);
        raftClient.restConnectToServer(nodeIpPorts, Raft.getRaftConfig());

        // 处理请求
        RemoteRouter<AppendEntriesRpc> rpcRemoteRouter = new AppendEntriesRpcRemoteRouter(curNodeID, raftClient);
        RaftCommandClient raftCommandClient = new RaftCommandClient(
                curNodeID,
                commandLogAppender,
                stateMachine,
                rpcRemoteRouter);

        // election
        RemoteRouter<RequestVoteRpc> remoteRouter = new RequestVoteRpcRemoteRouter(curNodeID, raftClient);
        RaftVoteClient raftVoteClient = new RaftVoteClient(
                curNodeID,
                commandLogAppender,
                remoteRouter,
                raftCommandClient);


        SignalManager.registToFirst(signal -> {
            raftClient.close();
            try {
                raftServer.close();
            } catch (IOException e) {

            }
            try {
                raftCommandClient.close();
            } catch (IOException e) {

            }

            try {
                raftVoteClient.close();
            } catch (IOException e) {

            }
        });
        return raftCommandClient;
    }
}
