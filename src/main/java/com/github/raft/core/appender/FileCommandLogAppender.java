package com.github.raft.core.appender;

import com.github.raft.core.CommandLog;
import com.github.raft.core.common.file.AppendLogFile;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-02
 */
public class FileCommandLogAppender implements CommandLogAppender, Closeable {

    private AppendLogFile appendLogFile;

    public FileCommandLogAppender(String rootPath) throws IOException {
        File file = new File(rootPath);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                throw new IOException("create command log dir error...");
            }
        }
        this.appendLogFile = new AppendLogFile(rootPath, "command-log");
    }

    @Override
    public CommandLog peek() {
        return appendLogFile.upToDateCommandLog();
    }

    @Override
    public CommandLog index(long index) {
        return appendLogFile.findCommandLog(index);
    }

    @Override
    public void append(CommandLog commandLog) {
        try {
            appendLogFile.appendLog(commandLog);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void update(CommandLog commandLog) {
        try {
            appendLogFile.updateLog(commandLog);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeRange(long term, long startIndex) {
        long curIndex = appendLogFile.upToDateCommandLog().getIndex();
        startIndex = startIndex < 0 ? 0 : startIndex;
        for (long i = startIndex; i <= curIndex; i++) {
            try {
                appendLogFile.removeLog(i);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public CommandLog[] range(long startIndex, long endIndex) {
        startIndex = startIndex < 0 ? 0 : startIndex;
        endIndex = endIndex < 0 ? 0 : endIndex;
        if (startIndex > endIndex) {
            return new CommandLog[0];
        }
        CommandLog[] commandLogs = new CommandLog[(int) ((endIndex - startIndex) + 1)];
        int index = 0;
        for (long i = startIndex; i <= endIndex; i++) {
            commandLogs[index++] = appendLogFile.findCommandLog(i);
        }
        return commandLogs;
    }

    @Override
    public void close() throws IOException {
        if (appendLogFile != null) {
            appendLogFile.close();
        }
    }

}
