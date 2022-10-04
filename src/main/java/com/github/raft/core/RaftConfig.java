package com.github.raft.core;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-01
 */
public class RaftConfig {

    // raft node ID
    private int id = Integer.getInteger("raft.node.id", ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE));

    /**
     * 选举定时器，每个节点的定时都不相等，防止无限选举循环
     * 如果不配置就随机范围获取一个值
     */
    private long electionMs = Long.getLong("raft.electionMs", ThreadLocalRandom.current().nextInt(50) + 150);

    /**
     * 广播心跳周期时间，比 electionMs 小，目前是取最小值的二分之一
     * todo 考虑心跳在网络中的丢包率
     */
    private long heartbeatMs = Long.getLong("raft.heartbeatMs", 150 / 2);

    public RaftConfig() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getElectionMs() {
        return electionMs;
    }

    public void setElectionMs(long electionMs) {
        this.electionMs = electionMs;
    }

    public long getHeartbeatMs() {
        return heartbeatMs;
    }

    public void setHeartbeatMs(long heartbeatMs) {
        this.heartbeatMs = heartbeatMs;
    }
}
