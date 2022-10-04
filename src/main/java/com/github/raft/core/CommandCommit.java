package com.github.raft.core;

import com.github.raft.core.appender.CommandLogAppender;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-02
 * 将日志提交到状态机执行
 */
public class CommandCommit {

    private CommandLogAppender commandLogAppender;

    private StateMachine stateMachine;

    public CommandCommit(CommandLogAppender commandLogAppender, StateMachine stateMachine) {
        this.commandLogAppender = commandLogAppender;
        this.stateMachine = stateMachine;
    }

    /**
     * 提交日志记录
     * @param commandLog 日志
     */
    public void commit(CommandLog commandLog) {
        if (commandLog != null) {
            commandLog.setStatus(1);
            commandLogAppender.update(commandLog);
            stateMachine.apply(commandLog);
        }
    }

    public long maxCommitIndex() {
        CommandLog commandLog = commandLogAppender.peek();
        if (commandLog == null) {
            return -1;
        }
        if (commandLog.getStatus() == 1) {
            return commandLog.getIndex();
        }
        long lastCommitIndex = commandLog.getIndex();
        while (lastCommitIndex >= 0) {
            commandLog = commandLogAppender.index(lastCommitIndex);
            if (commandLog.getStatus() == 1) {
                return commandLog.getIndex();
            }
            lastCommitIndex--;
        }
        return -1;
    }
}
