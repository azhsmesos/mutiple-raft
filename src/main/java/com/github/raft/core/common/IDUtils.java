package com.github.raft.core.common;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-01
 * 全局ID生成器
 */
public class IDUtils {

    private static final AtomicInteger index = new AtomicInteger();

    public static int newID() {
        return index.getAndIncrement();
    }
}
