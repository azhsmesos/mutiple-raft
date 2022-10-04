package com.github.raft.transport.api.connection;

/**
 * 等待超时异常
 *
 * @author wujiuye 2021/02/02
 */
public class WaitResponseTimeoutException extends TransportIOException {

    public WaitResponseTimeoutException(String message) {
        super(message);
    }

}
