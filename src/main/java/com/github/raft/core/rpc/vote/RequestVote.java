package com.github.raft.core.rpc.vote;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-01
 */
public class RequestVote {

    // 候选人任期
    private long term;

    // 候选人
    private int cadidateID;

    // 候选人最新日志记录的索引值
    private long lastLogIndex;

    // 候选人最新日志记录对应的任期号
    private long lastLongTerm;

    public long getTerm() {
        return term;
    }

    public void setTerm(long term) {
        this.term = term;
    }

    public int getCadidateID() {
        return cadidateID;
    }

    public void setCadidateID(int cadidateID) {
        this.cadidateID = cadidateID;
    }

    public long getLastLogIndex() {
        return lastLogIndex;
    }

    public void setLastLogIndex(long lastLogIndex) {
        this.lastLogIndex = lastLogIndex;
    }

    public long getLastLongTerm() {
        return lastLongTerm;
    }

    public void setLastLongTerm(long lastLongTerm) {
        this.lastLongTerm = lastLongTerm;
    }

    @Override
    public String toString() {
        return "RequestVote{" +
                "term=" + term +
                ", cadidateID=" + cadidateID +
                ", lastLogIndex=" + lastLogIndex +
                ", lastLongTerm=" + lastLongTerm +
                '}';
    }
}
