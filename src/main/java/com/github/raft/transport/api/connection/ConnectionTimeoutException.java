package com.github.raft.transport.api.connection;

/**
 * 连接超时异常
 *
 * @author wujiuye 2021/02/02
 */
public class ConnectionTimeoutException extends TransportIOException {

    public ConnectionTimeoutException(String message) {
        super(message);
    }

}
