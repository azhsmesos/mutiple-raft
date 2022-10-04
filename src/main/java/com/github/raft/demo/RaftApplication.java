package com.github.raft.demo;

import com.github.raft.core.RaftNodeBootstarter;
import com.github.raft.core.StateMachine;
import com.github.raft.core.appender.CommandLogAppender;
import com.github.raft.core.appender.DefaultCommandLogAppender;
import com.github.raft.core.appender.FileCommandLogAppender;
import com.github.raft.core.common.NodeIpPort;
import com.github.raft.core.common.SignalManager;
import com.github.raft.core.rpc.CommandResp;
import com.github.raft.core.rpc.RaftCommandClient;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.Buffer;
import java.util.HashSet;
import java.util.Set;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-01
 */
public class RaftApplication {

    private static Set<NodeIpPort> allNodes() {
        Set<NodeIpPort> nodeIpPorts = new HashSet<>();
        nodeIpPorts.add(new NodeIpPort(1, "127.0.0.1", 8090));
        nodeIpPorts.add(new NodeIpPort(2, "127.0.0.1", 8091));
        nodeIpPorts.add(new NodeIpPort(3, "127.0.0.1", 8092));
        nodeIpPorts.add(new NodeIpPort(4, "127.0.0.1", 8093));
        nodeIpPorts.add(new NodeIpPort(5, "127.0.0.1", 8094));
        nodeIpPorts.add(new NodeIpPort(6, "127.0.0.1", 8095));
        return nodeIpPorts;
    }

    private static StateMachine stateMachine() {
        return new PrintStateMachine();
    }

    private static CommandLogAppender createDefualtCommandLogAppender() {
        return new DefaultCommandLogAppender();
    }

    private static CommandLogAppender createFileCommandLogAppender() {
        try {
            FileCommandLogAppender appender = new FileCommandLogAppender("src/main/resources/tmp");
            SignalManager.registToLast(signal -> {
                try {
                    appender.close();
                } catch (IOException e) {

                }
            });
            return appender;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static void main(String[] args) throws IOException {
        int nodeID = 1;
        RaftCommandClient raftCommandClient = RaftBootstarter.bootstarterRaftNode(nodeID,
                allNodes(),
                stateMachine(),
                createFileCommandLogAppender());
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String res;
//        while (!"q".equalsIgnoreCase(res = reader.readLine())) {
//            byte[] comamnd = ("save {\"taskName\":\"payCallback\",\"param\":" + res + "}").getBytes();
//            try {
//                CommandResp resp = raftCommandClient.handleCommand(comamnd);
//                System.out.println(res);
//            } catch (Throwable throwable) {
//                throwable.printStackTrace();
//            }
//        }
        CommandResp resp = raftCommandClient.handleCommand("azh".getBytes());
    }
}
