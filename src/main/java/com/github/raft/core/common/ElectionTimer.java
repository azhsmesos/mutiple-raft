package com.github.raft.core.common;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-03
 * 选举定时器
 */
public class ElectionTimer {

    private long electionMs;

    private ElectionFunction electionFunction;

    private Thread thread;

    private volatile boolean status = false;

    private final AtomicLong lastTime = new AtomicLong(System.currentTimeMillis());

    public ElectionTimer(long electionMs, ElectionFunction electionFunction) {
        this.electionMs = electionMs;
        this.electionFunction = electionFunction;
        this.thread = new Thread(this::start, "election-timer-" + IDUtils.newID());
        this.thread.setDaemon(true);
    }

    public void resetTimer() {
        lastTime.set(System.currentTimeMillis());
    }

    public void startTimer() {
        this.thread.start();
    }

    public void stopTimer() {
        status = true;
    }

    private void start() {
        while (!status) {
            if (System.currentTimeMillis() - lastTime.get() >= electionMs) {
                callElectionFunction();
            }
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void callElectionFunction() {
        try {
            electionFunction.startElection();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        } finally {
            resetTimer();
        }
    }

    public interface ElectionFunction {

        /**
         * 开始选举
         */
        void startElection();
    }
}
