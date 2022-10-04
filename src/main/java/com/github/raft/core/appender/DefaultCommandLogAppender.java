package com.github.raft.core.appender;

import com.github.raft.core.CommandLog;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-02
 */
public class DefaultCommandLogAppender implements CommandLogAppender {

    private EntityNode endNode;
    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    @Override
    public CommandLog peek() {
        readWriteLock.readLock().lock();
        try {
            return endNode == null ? null : endNode.commandLog;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    @Override
    public CommandLog index(long index) {
        readWriteLock.readLock().lock();
        try {
            EntityNode node = endNode;
//            for (; node != null && node.commandLog.getIndex() != index; node = node.preNode) {
//            }
            while (node != null && node.commandLog.getIndex() != index) {
                node = node.preNode;
            }
            return node == null ? null : node.commandLog;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    @Override
    public void append(CommandLog commandLog) {
        readWriteLock.writeLock().lock();
        try {
            EntityNode node = new EntityNode(commandLog);
            node.preNode = endNode;
            endNode = node;
            EntityNode ptr = endNode, next = null;
            int i = 1;
            for (; ptr != null && i <= 10000; i++, next = ptr, ptr = ptr.preNode) {

            }
            if (i > 10000 && next != null) {
                next.preNode = null;
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public void update(CommandLog commandLog) {
        readWriteLock.writeLock().lock();
        try {
            EntityNode node = endNode;
            while (node != null && node.commandLog.getIndex() != commandLog.getIndex()
                    && node.commandLog.getTerm() != commandLog.getTerm()) {
                node = node.preNode;
            }
            if (node != null) {
                node.commandLog = commandLog;
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public void removeRange(long term, long startIndex) {
        readWriteLock.writeLock().lock();
        try {
            int count = 0;
            EntityNode node = endNode;
            while (node != null && node.commandLog.getIndex() != startIndex && node.commandLog.getTerm() != term) {
                node = node.preNode;
                count++;
            }
            if (node != null) {
                endNode = node.preNode;
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public CommandLog[] range(long startIndex, long endIndex) {
        readWriteLock.readLock().lock();
        try {
            CommandLog[] entities = new CommandLog[(int) (endIndex - startIndex + 1)];
            EntityNode node = endNode;
            int index = entities.length - 1;
            while (node != null && node.commandLog.getIndex() != startIndex) {
                node = node.preNode;
                index--;
                entities[index] = node.commandLog;
            }
            return entities;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    private static class EntityNode {
        private EntityNode preNode;
        private CommandLog commandLog;

        public EntityNode(CommandLog commandLog) {
            this.commandLog = commandLog;
        }
    }
}
