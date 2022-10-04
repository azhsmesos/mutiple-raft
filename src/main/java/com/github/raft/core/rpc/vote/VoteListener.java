package com.github.raft.core.rpc.vote;

import com.github.raft.core.RaftNodeRole;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-02
 * 选举节点监听器
 */
public interface VoteListener {

    void onVoteStart();

    void onVoteEnd(RaftNodeRole curRaftNodeRole);
}
