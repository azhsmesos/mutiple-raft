package com.github.raft.core;

import com.github.raft.core.common.HeartbeatCallable;
import com.github.raft.core.common.NodeIpPort;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-01
 */
public class Raft {

    private static final RaftConfig raft_config = new RaftConfig();

    private static Map<Integer, RaftNode> raftNodes = new HashMap<>();

    private static final AtomicReference<HeartbeatCallable> HEARTBEAT_CALLABLE_REFERENCE = new AtomicReference<>(null);

    public static synchronized void init(Set<NodeIpPort> nodeIpPorts) {
        setAllNode(nodeIpPorts);
    }

    private static void setAllNode(Set<NodeIpPort> nodeIpPorts) {
        for (NodeIpPort nodeIpPort : nodeIpPorts) {
            RaftNode node = new RaftNode(nodeIpPort.getNodeId());
            node.setNodeIpPort(nodeIpPort);
            raftNodes.put(node.getId(), node);
        }
    }

    // 获取所有节点
    public static Set<RaftNode> getAllNodes() {
        return new HashSet<>(raftNodes.values());
    }

    public static RaftNode getNode(Integer nodeID) {
        return raftNodes.get(nodeID);
    }

    public static int nodeCount() {
        return raftNodes.size();
    }

    public static RaftConfig getRaftConfig() {
        return raft_config;
    }

    public static HeartbeatCallable getHeartbeatCallback() {
        return HEARTBEAT_CALLABLE_REFERENCE.get();
    }

    public static synchronized void holdHeartbeatCallable(HeartbeatCallable heartbeatCallable) {
        HEARTBEAT_CALLABLE_REFERENCE.set(heartbeatCallable);
    }
}
