package com.github.raft.transport.netty.rpc;

import java.util.Map;

public interface RpcTest {

    void sayHello();

    Map<String, Object> sayHello(String name);

    Integer inc(Integer i1, Integer i2);

}
