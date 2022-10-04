package com.github.raft.core.rpc.replication;

import com.github.raft.core.CommandLog;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-01
 */
public class AppendEntries {

    private long term;


    private int leaderID;

    // leader 已经提交中最新一条日志的索引
    private long prevLogIndex;

    // leader 已经提交中最新一条日志的任期号
    private long prevLogTerm;

    /**
     * leader给每个follower都维护一个leaderCommit
     * 表示leader认为follower已经提交的日志条目索引
     */
    private long leaderCommit;

    // 追加的日志，如果是心跳包，则为null
    private CommandLog[] entries;

    public long getTerm() {
        return term;
    }

    public void setTerm(long term) {
        this.term = term;
    }

    public int getLeaderID() {
        return leaderID;
    }

    public void setLeaderID(int leaderID) {
        this.leaderID = leaderID;
    }

    public long getPrevLogIndex() {
        return prevLogIndex;
    }

    public void setPrevLogIndex(long prevLogIndex) {
        this.prevLogIndex = prevLogIndex;
    }

    public long getPrevLogTerm() {
        return prevLogTerm;
    }

    public void setPrevLogTerm(long prevLogTerm) {
        this.prevLogTerm = prevLogTerm;
    }

    public long getLeaderCommit() {
        return leaderCommit;
    }

    public void setLeaderCommit(long leaderCommit) {
        this.leaderCommit = leaderCommit;
    }

    public CommandLog[] getEntries() {
        return entries;
    }

    public void setEntries(CommandLog[] entries) {
        this.entries = entries;
    }

    @Override
    protected AppendEntries clone() {
        AppendEntries appendEntries = new AppendEntries();
        appendEntries.setTerm(this.getTerm());
        appendEntries.setLeaderID(this.getLeaderID());
        appendEntries.setLeaderCommit(this.getLeaderCommit());
        appendEntries.setPrevLogTerm(this.getPrevLogTerm());
        appendEntries.setPrevLogIndex(this.getPrevLogIndex());
        appendEntries.setEntries(this.getEntries());
        return appendEntries;
    }
}
