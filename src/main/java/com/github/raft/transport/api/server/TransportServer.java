package com.github.raft.transport.api.server;


import com.github.raft.transport.api.server.config.ServiceConfig;
import com.github.raft.transport.api.server.connection.ConnectionPool;

/**
 * 集群通信服务端
 *
 * @author wujiuye 2020/10/12
 */
public interface TransportServer {

    /**
     * 启动
     */
    void start(ServiceConfig serviceConfig) throws Exception;

    /**
     * 获取连接池
     *
     * @return
     */
    ConnectionPool getConnectionPool();

    /**
     * 停止
     */
    void stop() throws Exception;

}

