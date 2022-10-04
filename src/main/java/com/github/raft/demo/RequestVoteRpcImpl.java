package com.github.raft.demo;

import com.github.raft.core.rpc.vote.RequestVote;
import com.github.raft.core.rpc.vote.RequestVoteResp;
import com.github.raft.core.rpc.vote.RequestVoteRpc;
import com.github.raft.transport.api.connection.TransportIOException;
import com.github.raft.transport.api.rpc.RpcRequest;
import com.github.raft.transport.api.rpc.RpcResponse;
import com.github.raft.transport.netty.client.NettyTransportClient;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-04
 */
public class RequestVoteRpcImpl implements RequestVoteRpc {

    private NettyTransportClient client;

    public RequestVoteRpcImpl(NettyTransportClient client) {
        this.client = client;
    }

    @Override
    public RequestVoteResp requestVote(RequestVote requestVote) {
        RpcRequest request = new RpcRequest();
        request.setInterfaces(RequestVoteRpc.class);
        request.setMethodName("requestVote");
        request.setParameterTypes(new Class<?>[]{RequestVote.class});
        request.setArguments(new Object[]{requestVote});
        try {
            RpcResponse response = client.remoteInvoke(request);
            if (response.getException() != null) {
                throw new RuntimeException(response.getException());
            }
            return (RequestVoteResp) response.getResult();
        } catch (TransportIOException e) {
            throw new RuntimeException(e);
        }
    }
}
