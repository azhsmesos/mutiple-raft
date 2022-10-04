package com.github.raft.core.rpc.replication;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-02
 * AppendEntries rpc 的响应结果
 */
public class AppendEntriesResp {

    /**
     * 当前任期号
     * 如果leader发现当前任期比follower的任期还小，说明leader过时了
     * 马上将leader角色变为follower
     */
    private long term;

    // follower 是否可以匹配 preLogIndex和prevLogTerm
    private boolean success;

    public AppendEntriesResp() {}

    public AppendEntriesResp(long term, boolean success) {
        this.term = term;
        this.success = success;
    }

    public long getTerm() {
        return term;
    }

    public boolean isSuccess() {
        return success;
    }


}
