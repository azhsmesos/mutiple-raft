package com.github.raft.core;

import com.github.raft.core.common.NodeIpPort;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-02
 */
public interface RemoteRouter<T> {

    T routeRpc(NodeIpPort toNode);
}
