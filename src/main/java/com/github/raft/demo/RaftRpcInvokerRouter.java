package com.github.raft.demo;

import com.github.raft.core.rpc.replication.AppendEntriesRpc;
import com.github.raft.core.rpc.vote.RequestVoteRpc;
import com.github.raft.transport.api.rpc.RpcInvokerRouter;
import com.github.raft.transport.api.rpc.RpcMethod;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-04
 */
public class RaftRpcInvokerRouter implements RpcInvokerRouter {

    private Map<Class<?>, Object> objectMap = new HashMap<>();

    private Map<String, RpcMethod> processorMap = new HashMap<>();

    public RaftRpcInvokerRouter(AppendEntriesRpc appendEntriesRpc, RequestVoteRpc requestVoteRpc) {
        objectMap.put(AppendEntriesRpc.class, appendEntriesRpc);
        objectMap.put(RequestVoteRpc.class, requestVoteRpc);
        this.init(AppendEntriesRpc.class, RequestVoteRpc.class);
    }

    @Override
    public RpcMethod processor(Class<?> interfaces, String methodName, Class<?>[] parameterTypes)
            throws NoSuchMethodException {
        String processor = getMethodKey(interfaces, methodName, parameterTypes);
        RpcMethod rpcMethod = processorMap.get(processor);
        if (rpcMethod == null) {
            throw new NoSuchMethodException(processor + "not found");
        }
        return rpcMethod;
    }

    private void init(Class<?>... interfacess) {
        for (Class<?> clazz : interfacess) {
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                RpcMethod rpcMethod = new RpcMethod(objectMap.get(clazz), method);
                processorMap.put(getMethodKey(clazz, method.getName(), method.getParameterTypes()), rpcMethod);
            }
        }
    }

    private static String getMethodKey(Class<?> interfaces, String methodName, Class<?>[] parameterTypes) {
        StringBuilder builder = new StringBuilder();
        if (parameterTypes != null && parameterTypes.length > 0) {
            for (Class<?> clazz : parameterTypes) {
                builder.append(clazz.getName()).append("@");
            }
        }
        return interfaces.getName() + "@" + methodName + "@" + builder.toString();
    }
}
