package com.github.raft.transport.api.server.connection;

import com.github.raft.transport.api.connection.Connection;

/**
 * 连接关闭监听器
 *
 * @author wujiuye 2020/10/12
 */
public interface ConnectionCloseListener {

    /**
     * 客户端掉线
     *
     * @param connection 掉线的客户端
     */
    void onClientClose(Connection connection);

}
