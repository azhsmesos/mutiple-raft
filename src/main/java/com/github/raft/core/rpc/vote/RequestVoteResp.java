package com.github.raft.core.rpc.vote;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-01
 */
public class RequestVoteResp {

    // 当前任期
    private long term;

    // 如果候选人获取Follower的选票，就是true，否则就是false
    private boolean voteGranted;

    public long getTerm() {
        return term;
    }

    public void setTerm(long term) {
        this.term = term;
    }

    public boolean isVoteGranted() {
        return voteGranted;
    }

    public void setVoteGranted(boolean voteGranted) {
        this.voteGranted = voteGranted;
    }

    @Override
    public String toString() {
        return "RequestVoteResp{" +
                "term=" + term +
                ", voteGranted=" + voteGranted +
                '}';
    }
}
