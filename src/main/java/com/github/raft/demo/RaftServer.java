package com.github.raft.demo;

import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import com.github.raft.core.Raft;
import com.github.raft.core.StateMachine;
import com.github.raft.core.appender.CommandLogAppender;
import com.github.raft.core.common.NodeIpPort;
import com.github.raft.core.rpc.replication.FollowerAppendEntriesRpc;
import com.github.raft.core.rpc.vote.RequestVoteRpcImpl;
import com.github.raft.transport.api.rpc.RpcInvokerRouter;
import com.github.raft.transport.api.server.config.ServiceConfig;
import com.github.raft.transport.netty.server.NettyTransportServer;
import com.github.raft.transport.netty.server.ServerConstants;
import java.io.Closeable;
import java.io.IOException;
import java.util.Set;
import org.w3c.dom.Node;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-04
 */
public class RaftServer implements Closeable {

    private NettyTransportServer nettyTransportServer;

    private int curNodeID;

    private Set<NodeIpPort> nodeIpPortSet;

    private StateMachine stateMachine;

    private CommandLogAppender commandLogAppender;

    public RaftServer(int curNodeID,
                      Set<NodeIpPort> nodeIpPortSet,
                      StateMachine stateMachine,
                      CommandLogAppender commandLogAppender) {
        this.curNodeID = curNodeID;
        this.nodeIpPortSet = nodeIpPortSet;
        this.stateMachine = stateMachine;
        this.commandLogAppender = commandLogAppender;
        this.initRaft();
    }

    public synchronized void startRaftServer() {
        check();
        NodeIpPort curNode = findNode(curNodeID, nodeIpPortSet);
        RpcInvokerRouter rpcInvokerRouter = createRpcInvokerMapping(curNode);
        NettyTransportServer nettyTransportServer = new NettyTransportServer(rpcInvokerRouter);
        nettyTransportServer.start(getServiceConfig(curNode));
        this.nettyTransportServer = nettyTransportServer;
    }

    private void initRaft() {
        Raft.getRaftConfig().setId(curNodeID);
        Raft.init(nodeIpPortSet);
    }

    private void check() {
        if (stateMachine == null || commandLogAppender == null) {
            try {
                this.close();
            } catch (IOException e) {
                throw new RuntimeException("stateMachine or commandLogAppender is null");
            }
        }
    }

    private static NodeIpPort findNode(int nodeID, Set<NodeIpPort> nodeIpPorts) {
        for (NodeIpPort ipPort : nodeIpPorts) {
            if (ipPort.getNodeId() == nodeID) {
                return ipPort;
            }
        }
        return null;
    }

    /**
     * 创建rpc路由器
     * @param curNode 当前节点信息
     * @return
     */
    private RpcInvokerRouter createRpcInvokerMapping(NodeIpPort curNode) {
        RequestVoteRpcImpl requestVoteRpc = new RequestVoteRpcImpl(curNode.getNodeId(), commandLogAppender);
        FollowerAppendEntriesRpc followerAppendEntriesRpc =
                new FollowerAppendEntriesRpc(curNode.getNodeId(), commandLogAppender, stateMachine);
        return new RaftRpcInvokerRouter(followerAppendEntriesRpc, requestVoteRpc);
    }

    private static ServiceConfig getServiceConfig(NodeIpPort curNode) {
        ServiceConfig config = new ServiceConfig();
        config.setPort(curNode.getPort());
        config.setWorkThreads(Runtime.getRuntime().availableProcessors());
        config.setIdleTimeout(10);
        return config;
    }

    @Override
    public void close() throws IOException {
        if (nettyTransportServer != null) {
            nettyTransportServer.stop();
        }
    }
}
