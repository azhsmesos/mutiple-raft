package com.github.raft.core.common.file;

import com.github.raft.core.common.LoggerUtils;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-03
 */
public class IndexFile implements Closeable {

    private final static String SUFFIX = ".idx";

    private final static int max_record = 65536;

    private String rootPath;

    private String logFileName;

    private AtomicReference<String> curFile = new AtomicReference<>(null);

    private AtomicReference<MappedByteBuffer> mappedByteBuffer = new AtomicReference<>(null);

    private AtomicLong curIndexFileStart = new AtomicLong(-1);

    private AtomicLong curIndexFileEnd = new AtomicLong(-1);

    private Thread thread;

    private AtomicBoolean force = new AtomicBoolean(true);

    public IndexFile(String rootPath, String logFileName) throws IOException {
        this.rootPath = rootPath;
        this.logFileName = logFileName;
        ensureFileExist();
    }

    public synchronized void appendOffset(long index, long offset) throws IOException {
        if (index < curIndexFileStart.get()) {
            updateOffset(index, offset);
            return;
        }
        if (index > curIndexFileEnd.get()) {
            createNewFile(curIndexFileEnd.get() + 1);
        }
        mappedByteBuffer.get().position((int) ((index - curIndexFileStart.get()) * indexLength() + headerLength()));
        mappedByteBuffer.get().put(ByteBuffer.wrap(longToByte(index)));
        mappedByteBuffer.get().put(ByteBuffer.wrap(longToByte(offset)));
        writeFileHeader(mappedByteBuffer.get(), index);
    }

    /**
     * 该方法应该尽量少调用
     * @param index
     * @param offset
     * @throws IOException
     */
    public synchronized void updateOffset(long index, long offset) throws IOException {
        if (index >= curIndexFileStart.get() && index <= curIndexFileEnd.get()) {
            mappedByteBuffer.get().position((int) ((index - curIndexFileStart.get()) * indexLength() + headerLength()));
            mappedByteBuffer.get().put(ByteBuffer.wrap(longToByte(index)));
            mappedByteBuffer.get().put(ByteBuffer.wrap(longToByte(offset)));
            return;
        }
        Position position = positionByIndex(index);
        if (position == null) {
            LoggerUtils.getLogger().error("not found by index: {}", index);
        }
        RandomAccessFile randomAccessFile = new RandomAccessFile(rootPath + "/" + position.fileName, "rw");
        randomAccessFile.seek((index - position.startIndex) * indexLength() + headerLength());
        randomAccessFile.write(longToByte(index));
        randomAccessFile.write(longToByte(offset));
    }

    /**
     * 获取最新的索引
     * @return
     */
    public synchronized Offset lastEffectiveIndexOffset() {
        List<String> files = listSortFiles();
        if (!files.isEmpty()) {
            String filename = files.get(0);
            try (FileChannel channel = FileChannel.open(Paths.get(rootPath + "/" + filename), StandardOpenOption.READ,
                    StandardOpenOption.WRITE)) {
                MappedByteBuffer mappedByteBuffer =
                        channel.map(FileChannel.MapMode.READ_WRITE, 0, (long) max_record * indexLength());
                long index = readFileHeader(mappedByteBuffer);
                byte[] bytes = new byte[16];
                mappedByteBuffer.position((int) (index * indexLength() + headerLength()));
                mappedByteBuffer.get(bytes, 0, 16);
                long offset = byteToLong(bytes, 8);
                if (index >= 0 && offset >= 0) {
                    return new Offset(filename.split("\\.")[0], offset);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public Offset findOffset(long index) {

        if (index >= curIndexFileStart.get() && index <= curIndexFileEnd.get()) {
            mappedByteBuffer.get().position((int) (index - curIndexFileStart.get()) * indexLength() + headerLength());
            byte[] bytes = new byte[16];
            mappedByteBuffer.get().get(bytes, 0, 16);
            long offset = byteToLong(bytes, 8);
            if (offset >= 0) {
                return new Offset(curFile.get().split("\\.")[0], offset);
            } else if (offset == Long.MIN_VALUE) {
                return null;
            }
            return null;
        }
        Position position = positionByIndex(index);
        long offset = -1;
        if (position != null) {
            try (FileChannel channel = FileChannel.open(
                    Paths.get(rootPath + "/" + position.fileName), StandardOpenOption.READ)) {
                System.out.println((long) (index - position.startIndex) * indexLength() + headerLength());
                channel.position((index - position.startIndex) * indexLength() + headerLength());
                ByteBuffer buffer = ByteBuffer.allocate(8);
                for (; ; ) {
                    // todo 尝试实现cpu绑核，不然消耗比较高
                    if (channel.read(buffer) != 8) {
                        break;
                    }
                    buffer.flip();
                    long indexRead = buffer.getLong();
                    buffer.clear();
                    if (channel.read(buffer) != 8) {
                        break;
                    }
                    buffer.flip();
                    offset = buffer.getLong();
                    buffer.clear();
                    if (indexRead == offset) {
                        System.out.println("offset: " + offset);
                        break;
                    }
                }
                System.out.println("offset: "+offset);
                buffer.clear();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (offset >= 0) {
                return new Offset(position.fileName.split("\\.")[0], offset);
            } else if (offset == Long.MIN_VALUE) {
                return null;
            }
        }
        return null;
    }

    @Override
    public void close() throws IOException {

    }

    private void ensureFileExist() throws IOException {
        List<String> files = listSortFiles();
        if (files.isEmpty()) {
            createNewFile(0);
        } else {
            String fileName = files.get(0);
            String endStr = fileName.substring(logFileName.length() + 1);
            String[] filenameInfo = endStr.split("\\.");
            String[] indexRange = filenameInfo[0].split("-");
            long indexStart = Long.parseLong(indexRange[0]);
            long indexEnd = Long.parseLong(indexRange[1]);
            curIndexFileStart.set(indexStart);
            curIndexFileEnd.set(indexEnd);
            curFile.set(fileName);
//            System.out.println(Paths.get(URI.create("file:/" + rootPath + "/" + fileName)));
            FileChannel channel = FileChannel.open(Paths.get(rootPath + "/" + fileName),
                    StandardOpenOption.WRITE,
                    StandardOpenOption.READ);
            mappedByteBuffer.set(
                    channel.map(FileChannel.MapMode.READ_WRITE, 0, headerLength() + (long) max_record * indexLength()));
            channel.close();
        }
        removeExpireFile(7);
    }

    private List<String> listSortFiles() {
        File file = new File(rootPath);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                LoggerUtils.getLogger().warn("mkdirs file {} error", file.getName());
            }
            return Collections.emptyList();
        }
        if (!file.isDirectory()) {
            if (!file.delete()) {
                LoggerUtils.getLogger().warn("delete file {} errror", file.getName());
            }
            if (!file.mkdirs()) {
                LoggerUtils.getLogger().warn("mkdirs file {} error", file.getName());
            }
            return Collections.emptyList();
        }
        String[] fileNames = file.list((dir, name) -> name.startsWith(logFileName + ".") && name.endsWith(SUFFIX));
        if (fileNames == null || fileNames.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.stream(fileNames).sorted((f1, f2) -> (int) (Long.parseLong(
                f2.substring(logFileName.length())
                        .split("\\.")[1]
                        .split("-")[0])
                - Long.parseLong(
                f1.substring(logFileName.length())
                        .split("\\.")[1]
                        .split("-")[0]))).collect(Collectors.toList());
    }

    private void createNewFile(long newFileStartIndex) throws IOException {
        String newFileName = logFileName + ".";
        newFileName += (newFileStartIndex) + "-" + (newFileStartIndex + max_record - 1);
        newFileName += SUFFIX;
        File file = new File(rootPath + "/" + newFileName);
        if (!file.exists()) {
            if (file.createNewFile()) {
                if (mappedByteBuffer.get() != null) {
                    mappedByteBuffer.get().force();
                }
                curFile.set(newFileName);
                FileChannel fileChannel = FileChannel.open(Paths.get(rootPath + "/" + newFileName),
                        StandardOpenOption.WRITE,
                        StandardOpenOption.READ);
                mappedByteBuffer.set(fileChannel.map(FileChannel.MapMode.READ_WRITE, 0,
                        headerLength() + (long) max_record * indexLength()));
                fileChannel.close();
                curIndexFileStart.set(newFileStartIndex);
                curIndexFileEnd.set(newFileStartIndex + max_record - 1);
            }
        } else if (mappedByteBuffer.get() == null) {
            curFile.set(newFileName);
            FileChannel channel = FileChannel.open(Paths.get(rootPath + "/" + newFileName),
                    StandardOpenOption.WRITE,
                    StandardOpenOption.READ);
            mappedByteBuffer.set(
                    channel.map(FileChannel.MapMode.READ_WRITE, 0, headerLength() + (long) max_record * indexLength()));
            channel.close();
            curIndexFileStart.set(newFileStartIndex);
            curIndexFileEnd.set(newFileStartIndex + max_record - 1);
        }
    }

    private void removeExpireFile(int before) throws IOException {
        List<String> files = listSortFiles();
        if (files.size() > before) {
            for (int i = before; i < files.size(); i++) {
                File file = new File(rootPath + "/" + files.get(i));
                if (!file.delete()) {
                    LoggerUtils.getLogger().info("delete index file {} error", file.getName());
                }
            }
        }
    }

    private void writeFileHeader(MappedByteBuffer mappedByteBuffer, long latestNewIndex) {
        mappedByteBuffer.position(0);
        mappedByteBuffer.putLong(latestNewIndex);
    }

    private static int headerLength() {
        return longToByte(0).length;
    }

    private static int indexLength() {
        return longToByte(0).length * 2;
    }

    private static byte[] longToByte(long value) {
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

    private static long byteToLong(byte[] bytes, int offset) {
        long value = ((long) (bytes[offset]) & 0xff) << 56;
        value |= ((long) (bytes[offset + 1]) & 0xff) << 48;
        value |= ((long) (bytes[offset + 2]) & 0xff) << 40;
        value |= ((long) (bytes[offset + 3]) & 0xff) << 32;
        value |= ((long) (bytes[offset + 4]) & 0xff) << 24;
        value |= (long) (bytes[offset + 5] & 0xff) << 16;
        value |= (long) (bytes[offset + 6] & 0xff) << 8;
        value |= (long) (bytes[offset + 7] & 0xff);
        return value;
    }

    static class Position {
        private String fileName;

        private long startIndex;

        private long endIndex;

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public long getStartIndex() {
            return startIndex;
        }

        public void setStartIndex(long startIndex) {
            this.startIndex = startIndex;
        }

        public long getEndIndex() {
            return endIndex;
        }

        public void setEndIndex(long endIndex) {
            this.endIndex = endIndex;
        }
    }

    static class Offset {
        private String logFileName;

        private long physicsOffset;

        public Offset(String logFileName, long physicsOffset) {
            this.logFileName = logFileName;
            this.physicsOffset = physicsOffset;
        }

        public String getLogFileName() {
            return logFileName;
        }

        public void setLogFileName(String logFileName) {
            this.logFileName = logFileName;
        }

        public long getPhysicsOffset() {
            return physicsOffset;
        }

        public void setPhysicsOffset(long physicsOffset) {
            this.physicsOffset = physicsOffset;
        }
    }

    private Position positionByIndex(long index) {
        Position position = null;
        if (curIndexFileStart.get() <= index && curIndexFileEnd.get() >= index) {
            position = new Position();
            position.setFileName(curFile.get());
            position.setStartIndex(curIndexFileStart.get());
            position.setEndIndex(curIndexFileStart.get() + max_record - 1);
        } else {
            List<String> files = listSortFiles();
            for (String fname : files) {
                String[] filenameInfo = fname.substring(logFileName.length()).split("\\.");
                String[] indexRange = filenameInfo[1].split("-");
                long indexStart = Long.parseLong(indexRange[0]);
                long indexEnd = Long.parseLong(indexRange[1]);
                if (index >= indexStart && index <= indexEnd) {
                    position = new Position();
                    position.setFileName(fname);
                    position.setStartIndex(indexStart);
                    position.setEndIndex(indexEnd);
                    break;
                }
            }
        }
        return position;
    }

    private long readFileHeader(MappedByteBuffer mappedByteBuffer) {
        mappedByteBuffer.position(0);
        return mappedByteBuffer.getLong();
    }
}
