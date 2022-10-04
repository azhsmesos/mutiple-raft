package com.github.raft.core;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-02
 */
public interface StateMachine {

    /**
     * 状态机执行日志
     * @param commandLog 日志
     */
    void apply(CommandLog commandLog);
}
