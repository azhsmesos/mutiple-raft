package com.github.raft.demo;

import com.github.raft.core.RaftConfig;
import com.github.raft.core.common.NodeIpPort;
import com.github.raft.transport.api.client.config.ClientConfig;
import com.github.raft.transport.netty.client.NettyTransportClient;
import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-04
 */
public class RaftClient implements Closeable {

    private final int curNodeID;

    private volatile Map<Integer, NettyTransportClient> clientMap;

    public RaftClient(int curNodeID) {
        this.curNodeID = curNodeID;
    }

    public synchronized void restConnectToServer(Set<NodeIpPort> nodeIpPorts, RaftConfig raftConfig) {
        close();
        Map<Integer, NettyTransportClient> clientMap = new HashMap<>();
        for (NodeIpPort nodeIpPort : nodeIpPorts) {
            if (nodeIpPort.getNodeId() == curNodeID) {
                continue;
            }
            NettyTransportClient client = new NettyTransportClient();
            ClientConfig config = new ClientConfig();
            config.setHost(nodeIpPort.getIp());
            config.setPort(nodeIpPort.getPort());
            config.setRequestTimeout(raftConfig.getHeartbeatMs());
            config.setConnectionTimeout(raftConfig.getHeartbeatMs());
            client.start(config, raftConfig.getHeartbeatMs());
            clientMap.put(nodeIpPort.getNodeId(), client);
        }
        this.clientMap = clientMap;
    }

    public NettyTransportClient getNettyClient(int nodeID) {
        return clientMap.get(nodeID);
    }

    @Override
    public void close() {
        Map<Integer, NettyTransportClient> clientMap = this.clientMap;
        if (clientMap != null) {
            clientMap.values().forEach(client -> {
                try {
                    client.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
