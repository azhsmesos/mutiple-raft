package com.github.raft.core.common;

import java.util.LinkedList;
import sun.misc.SignalHandler;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-04
 */
public class SignalManager {

    private final static LinkedList<SignalHandler> signal_handlers = new LinkedList<>();

    public static void registToFirst(SignalHandler signalHandler) {
        synchronized (signal_handlers) {
            signal_handlers.addFirst(signalHandler);
        }
    }

    public static void registToLast(SignalHandler signalHandler) {
        synchronized (signal_handlers) {
            signal_handlers.addLast(signalHandler);
        }
    }
}
