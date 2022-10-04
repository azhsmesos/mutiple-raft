package com.github.raft.demo;

import com.github.raft.core.RemoteRouter;
import com.github.raft.core.common.NodeIpPort;
import com.github.raft.core.rpc.replication.AppendEntriesRpc;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-04
 */
public class AppendEntriesRpcRemoteRouter implements RemoteRouter<AppendEntriesRpc> {

    private final int curNodeID;

    private RaftClient raftClient;

    public AppendEntriesRpcRemoteRouter(int curNodeID, RaftClient raftClient) {
        this.curNodeID = curNodeID;
        this.raftClient = raftClient;
    }

    @Override
    public AppendEntriesRpc routeRpc(NodeIpPort toNode) {
        if (toNode.getNodeId() == curNodeID) {
            throw new RuntimeException("not supper rpc invoke self...");
        }
        return new AppendEntriesRpcImpl(raftClient.getNettyClient(toNode.getNodeId()));
    }
}
