package com.github.raft.core.common;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-03
 * leader在任期之内定期广播心跳包
 */
public class HeartbeatScheduler implements Runnable {

    private long heartbeatMs;

    private HeartbeatTask heartbeatTask;

    private Thread thread;

    private volatile boolean stop;

    private AtomicLong lastMs = new AtomicLong(System.currentTimeMillis());

    public HeartbeatScheduler(long heartbeatMs, HeartbeatTask task) {
        this.heartbeatMs = heartbeatMs;
        this.heartbeatTask = task;
        this.thread = new Thread(this, "heartbeat-thread-" + IDUtils.newID());
        this.thread.setDaemon(true);
    }

    public void startHeartbeat() {
        this.stop = false;
        this.heartbeatTask.sendHeartbeat();
        this.thread.start();
    }

    public void stopHeartbeat() {
        this.stop = true;
        lastMs.set(Long.MAX_VALUE);
    }

    @Override
    public void run() {
        while (!stop) {
            if ((System.currentTimeMillis() - lastMs.get()) >= heartbeatMs) {
                heartbeatTask.sendHeartbeat();
                lastMs.set(System.currentTimeMillis());
                continue;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public interface HeartbeatTask extends Runnable {

        @Override
        default void run() {
            sendHeartbeat();
        }

        /**
         * 发送心跳包
         */
        void sendHeartbeat();
    }
}
