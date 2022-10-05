package com.github.raft.file;

import com.github.raft.core.CommandLog;
import com.github.raft.core.common.file.AppendLogFile;
import com.github.raft.core.common.file.IndexFile;
import java.io.IOException;
import org.junit.Test;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-03
 */
public class AppendLogFileTest {

    @Test
    public void TestCreateNewFile() throws IOException {
        String rootPath = "src/main/resources/tmp";
        String logFileName = "raft-data";
        AppendLogFile file = new AppendLogFile(rootPath, logFileName);

    }

    @Test
    public void TestCreateNewIndexFile() throws IOException {
        String rootPath = "src/main/resources/tmp";
        String logFileName = "raft-data";
        IndexFile file = new IndexFile(rootPath, logFileName);
    }

    @Test
    public void TestAppendLog() throws IOException {
        String msg = "first hello world!!!";
        String rootPath = "src/main/resources/tmp";
        String logFileName = "data";
        AppendLogFile file = new AppendLogFile(rootPath, logFileName);
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            CommandLog log = new CommandLog();
            log.setTerm(i / 100);
            log.setIndex(i);
            log.setStatus(1);
            log.setCommand(msg.getBytes());
            file.appendLog(log);
        }
        long end = System.currentTimeMillis();
        System.out.println((end - start) / 1000);
    }

    @Test
    public void TestGetLastupToDateCommandLog() throws IOException {
        String rootPath = "src/main/resources/tmp";
        String logFileName = "data";
        AppendLogFile file = new AppendLogFile(rootPath, logFileName);
        CommandLog commandLog = file.upToDateCommandLog();
        System.out.println(commandLog.toSaveString());
    }

    @Test
    public void TestFindCommandLog() throws IOException {
        String rootPath = "src/main/resources/tmp";
        String logFileName = "data";
        AppendLogFile file = new AppendLogFile(rootPath, logFileName);
        CommandLog log = file.findCommandLog(2000);
        System.out.println(log.toSaveString());
    }

    @Test
    public void TestTobytes() {
        byte[] bytes = toByte(0);
        for (byte b : bytes) {
            System.out.println(b);
        }
        System.out.println("o: " + 0xff);
        System.out.println(bytes.length * 2);
    }

    private static byte[] toByte(long value) {
        byte[] writeBuf = new byte[8];
        writeBuf[0] = (byte) (value >>> 56);
        writeBuf[1] = (byte) (value >>> 48);
        writeBuf[2] = (byte) (value >>> 40);
        writeBuf[3] = (byte) (value >>> 32);
        writeBuf[4] = (byte) (value >>> 24);
        writeBuf[5] = (byte) (value >>> 16);
        writeBuf[6] = (byte) (value >>> 8);
        writeBuf[7] = (byte) value;
        return writeBuf;
    }
}
