package com.github.raft.core.common.file;

import com.github.raft.core.CommandLog;
import com.github.raft.core.common.LoggerUtils;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-01
 */
public class AppendLogFile implements Closeable {

    private final static String SUFFIX = ".log";

    private String rootPath, logFileName;

    private AtomicReference<IndexFile> indexFile = new AtomicReference<>(null);

    private AtomicReference<String> curFile = new AtomicReference<>(null);

    private AtomicLong curFileLen = new AtomicLong(0);

    private AtomicReference<FileOutputStream> outputStream = new AtomicReference<>(null);

    public AppendLogFile(String rootPath, String logFileName) throws IOException {
        this.rootPath = rootPath;
        this.logFileName = logFileName;
        ensureFileExist();
    }

    public synchronized void appendLog(CommandLog log) throws IOException {
        // 后续改为 用户自定义
        appendLog(log, true);
    }

    public synchronized void removeLog(long index) throws IOException {
        indexFile.get().updateOffset(index, Long.MIN_VALUE);
    }

    public CommandLog upToDateCommandLog() {
        IndexFile.Offset offset = indexFile.get().lastEffectiveIndexOffset();
        if (offset == null) {
            return null;
        }
        return findCommandLog(offset);
    }

    public CommandLog findCommandLog(long index) {
        if (index < 0) {
            return null;
        }
        IndexFile.Offset offset = indexFile.get().findOffset(index);
        if (offset == null) {
            return null;
        }
        return findCommandLog(offset);
    }

    public synchronized void updateLog(CommandLog log) throws IOException {
        long newOffset = appendLog(log, false);
        indexFile.get().updateOffset(log.getIndex(), newOffset);
    }

    private long appendLog(CommandLog log, boolean appendIndex) throws IOException {
        ensureFileExist();
        long offset = curFileLen.get();
        byte[] bytes = (log.toSaveString() + "\n").getBytes();
        outputStream.get().write(bytes);
        if (appendIndex) {
            indexFile.get().appendOffset(log.getIndex(), offset);
        }
        return offset;
    }


    private void ensureFileExist() throws IOException {
        createNewFile();
        removeExpireFile(3);
    }

    private void createNewFile() throws IOException {
        String newFileName = logFileName + "-";
        newFileName += LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String newIndexFileName = newFileName;
        newFileName += SUFFIX;
        File file = new File(rootPath + "/" + newFileName);
        if (!file.exists()) {
            if (file.createNewFile()) {
                if (outputStream.get() != null) {
                    outputStream.get().close();
                }
                if (indexFile.get() != null) {
                    indexFile.get().close();
                }
                curFile.set(newFileName);
                outputStream.set(new FileOutputStream(file, true));
                indexFile.set(new IndexFile(rootPath, newIndexFileName));
            }
        } else if (outputStream.get() == null) {
            curFile.set(newFileName);
            outputStream.set(new FileOutputStream(file, true));
            indexFile.set(new IndexFile(rootPath, newIndexFileName));
        }
        curFileLen.set(file.length());
    }

    private void removeExpireFile(int before) throws IOException {
        List<String> files = listFiles();
        if (files.size() * 0.75 > before) {
            for (int i = 0; i < before; i++) {
                File file = new File(rootPath + "/" + files.get(i));
                if (file.exists()) {
                    if (!file.delete()) {
                        LoggerUtils.getLogger().warn("delete file {} error", file.getName());
                    }
                    LoggerUtils.getLogger().warn("delete file {}", file.getName());
                }
            }
        }
    }

    private List<String> listFiles() throws IOException {
        File file = new File(rootPath);
        if (!file.exists()) {
            if (!file.createNewFile()) {
                LoggerUtils.getLogger().warn("create file errorr, file: {}", file.getName());
            }
            return Collections.emptyList();
        }
        if (!file.isDirectory()) {
            if (!file.delete()) {
                LoggerUtils.getLogger().warn("delete file error, file: {}", file.getName());
            }
            if (!file.createNewFile()) {
                LoggerUtils.getLogger().warn("create file errorr, file: {}", file.getName());
            }
            return Collections.emptyList();
        }
        String[] fileNames = file.list((dir, name) -> name.startsWith(logFileName + "-") && name.endsWith(SUFFIX));
        if (fileNames == null || fileNames.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.stream(fileNames).collect(Collectors.toList());
    }

    private CommandLog findCommandLog(IndexFile.Offset offset) {
        try (FileInputStream inputStream = new FileInputStream(rootPath + "/" + offset.getLogFileName() + SUFFIX)) {
            inputStream.getChannel().position(offset.getPhysicsOffset());
            ByteBuffer buffer = ByteBuffer.allocate(2000);
            int len;
            byte[] bytes = new byte[8];
            loop:
            while ((len = inputStream.read(bytes)) > 0) {
                for (int i = 0; i < len; i++) {
                    if (bytes[i] == '\n') {
                        buffer.put(bytes, 0, i);
                        break loop;
                    }
                }
                buffer.put(bytes, 0, len);
            }
            if (buffer.position() == 0) {
                return null;
            }
            buffer.flip();
            String log = new String(buffer.array(), buffer.position(), buffer.limit(), StandardCharsets.UTF_8);
            return CommandLog.forSaveString(log);
        } catch (IOException e) {
            LoggerUtils.getLogger().error("findCommandLog function error");
            return null;
        }
    }

    @Override
    public void close() throws IOException {
        if (outputStream.get() != null) {
            outputStream.get().close();
        }
        if (indexFile.get() != null) {
            indexFile.get().close();
        }
    }
}
