package com.github.raft.core.common;

import java.lang.reflect.Proxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLogger;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-02
 */
public class LoggerUtils {

    private final static Logger logger;

    static {
        Logger tempLog = LoggerFactory.getLogger("raft-core");
        if (tempLog instanceof NOPLogger) {
            tempLog = (Logger) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                    new Class<?>[]{Logger.class}, (proxy, method, args) -> {
                        System.err.println(formatMessage(args));
                        return null;
                    });
        }
        logger = tempLog;
    }

    public static Logger getLogger() {
        return logger;
    }

    private static String formatMessage(Object[] args) {
        String msg = (String) args[0];
        if (args.length > 1) {
            msg = msg.replace("{}", "%s");
            Object[] param;
            if (args.length == 2 && args[1].getClass().isArray()) {
                param = (Object[]) args[1];
            } else {
                param = new Object[args.length - 1];
                System.arraycopy(args, 1, param, 0, param.length);
            }
            msg = String.format(msg, param);
        }
        return msg;
    }
}
