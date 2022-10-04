package com.github.raft.core.appender;

import com.github.raft.core.CommandLog;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-01
 */
public interface CommandLogAppender {

    /**
     * 获取最近写入的一个日志条目
     * @return CommandLog
     */
    CommandLog peek();

    /**
     * 获取索引位置的日志条目
     * @param index 索引
     * @return CommandLog
     */
    CommandLog index(long index);

    /**
     * 追加
     * @param commandLog 追加日志
     */
    void append(CommandLog commandLog);

    /**
     * 更新日志
     * @param commandLog 日志
     */
    void update(CommandLog commandLog);

    /**
     * 删除 任期为 iterm  startIndex开始的日志
     * @param term 任期
     * @param startIndex index
     */
    void removeRange(long term, long startIndex);

    CommandLog[] range(long startIndex, long endIndex);
}
