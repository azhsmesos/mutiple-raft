package com.github.raft.core.common;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-01
 */
public interface HeartbeatCallable {

    /**
     * 接收到心跳包时回调
     */
    void onHeartbeat();
}
