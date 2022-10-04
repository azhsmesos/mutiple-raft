package com.github.raft.core.common;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-01
 */
public class TermVoter {

    private long term;

    // 获取选票节点的ID
    private int voteNodeID;

    private AtomicBoolean alreadyVoted = new AtomicBoolean(Boolean.FALSE);

    public TermVoter(long term) {
        this.term = term;
    }

    public long getTerm() {
        return term;
    }

    /**
     * 锁定投票状态
     * @param voteNodeID 当前锁定的ID
     * @return true就是获取选票成功
     */
    public boolean lockVote(int voteNodeID) {
        if (alreadyVoted.compareAndSet(Boolean.FALSE, Boolean.TRUE)) {
            this.voteNodeID = voteNodeID;
            return true;
        }
        return false;
    }

    public int getVoteNodeID() {
        return voteNodeID;
    }
}
