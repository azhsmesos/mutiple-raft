package com.github.raft.demo;

import com.github.raft.core.rpc.replication.AppendEntries;
import com.github.raft.core.rpc.replication.AppendEntriesResp;
import com.github.raft.core.rpc.replication.AppendEntriesRpc;
import com.github.raft.transport.api.connection.TransportIOException;
import com.github.raft.transport.api.rpc.RpcRequest;
import com.github.raft.transport.api.rpc.RpcResponse;
import com.github.raft.transport.netty.client.NettyTransportClient;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-04
 */
public class AppendEntriesRpcImpl implements AppendEntriesRpc {

    private NettyTransportClient client;

    public AppendEntriesRpcImpl(NettyTransportClient client) {
        this.client = client;
    }

    @Override
    public AppendEntriesResp appendCommand(AppendEntries appendEntries) {
        RpcRequest request = new RpcRequest();
        request.setInterfaces(AppendEntriesRpc.class);
        request.setMethodName("appendCommand");
        request.setParameterTypes(new Class<?>[]{AppendEntries.class});
        request.setArguments(new Object[]{appendEntries});
        try {
            RpcResponse response = client.remoteInvoke(request);
            if (response.getException() != null) {
                throw new RuntimeException(response.getException());
            }
            return (AppendEntriesResp) response.getResult();
        } catch (TransportIOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Boolean commit(Long term, Long index) {
        RpcRequest request = new RpcRequest();
        request.setInterfaces(AppendEntriesRpc.class);
        request.setMethodName("commit");
        request.setParameterTypes(new Class<?>[]{Long.class, Long.class});
        request.setArguments(new Object[]{term, index});
        try {
            RpcResponse response = client.remoteInvoke(request);
            if (response.getException() != null) {
                throw new RuntimeException(response.getException());
            }
            return (Boolean) response.getResult();
        } catch (TransportIOException e) {
            throw new RuntimeException(e);
        }
    }
}
