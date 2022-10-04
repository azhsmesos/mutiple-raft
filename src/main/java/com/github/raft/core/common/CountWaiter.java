package com.github.raft.core.common;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-02
 */
public class CountWaiter {

    private final int count;
    private final int[] metrics;

    private Lock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();

    public CountWaiter(int count) {
        this.count = count;
        this.metrics = new int[3];
    }

    public void countDown() {
        countDown(0);
    }

    public void countDownSuccess() {
        countDown(1);
    }

    public void countDownException() {
        countDown(2);
    }

    public void await() {
        await(() -> (metrics[0] >= count));
    }

    public void await(int minSuccess) {
        await(() -> (metrics[0] >= count) || (metrics[1] >= minSuccess));
    }

    public int successCount() {
        return readCount(1);
    }

    public int exceptionCount() {
        return readCount(2);
    }

    private void countDown(int index) {
        lock.lock();
        try {
            this.metrics[index]++;
            condition.signal();
        } finally {
            lock.unlock();
        }
    }

    private void await(BooleanSupplier supplier) {
        lock.lock();
        try {
            while (!Thread.interrupted()) {
                if (supplier.getAsBoolean()) {
                    return;
                }
                condition.await();
            }
        } catch (InterruptedException exception) {

        } finally {
            lock.unlock();
        }
    }

    private int readCount(int index) {
        lock.lock();
        try {
            return metrics[index];
        } finally {
            lock.unlock();
        }
    }
}
