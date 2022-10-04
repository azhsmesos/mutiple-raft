package com.github.raft.core.rpc.vote;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-01
 */
public interface RequestVoteRpc {

    /**
     * 发起拉票请求
     * 1. 如果请求的term小于自身term，则返回false
     * 2. 如果term大于当前term，而且之前没有投票给任何人，就返回true
     * 3. 如果已经投票给了请求方，且请求方的日志和自己一样新，就返回true
     * 4. 如果投票给了别人，则返回false
     * @param requestVote 请求vote
     * @return response
     */
    RequestVoteResp requestVote(RequestVote requestVote);
}
