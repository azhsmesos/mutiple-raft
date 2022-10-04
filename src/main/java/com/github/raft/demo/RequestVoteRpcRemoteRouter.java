package com.github.raft.demo;

import com.github.raft.core.RemoteRouter;
import com.github.raft.core.common.NodeIpPort;
import com.github.raft.core.rpc.vote.RequestVoteRpc;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-04
 */
public class RequestVoteRpcRemoteRouter implements RemoteRouter<RequestVoteRpc> {

    private final int curNodeID;

    private RaftClient client;

    public RequestVoteRpcRemoteRouter(int curNodeID, RaftClient client) {
        this.curNodeID = curNodeID;
        this.client = client;
    }

    @Override
    public RequestVoteRpc routeRpc(NodeIpPort toNode) {
        if (toNode.getNodeId() == curNodeID) {
            throw new RuntimeException("not supper rpc invoke self...");
        }
        return new RequestVoteRpcImpl(client.getNettyClient(toNode.getNodeId()));
    }
}
