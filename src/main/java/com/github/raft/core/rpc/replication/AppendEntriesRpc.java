package com.github.raft.core.rpc.replication;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-02
 * leader向其他节点发起请求
 */
public interface AppendEntriesRpc {

    /**
     * 1。如果自身的term大于AppendEntries#term，
     * 则说明leader过期，返回自身term，success为false
     * 2。如果follower在prevLogIndex的任期和prevLogTerm不匹配，
     * 则返回自身term，且success = false
     * 3。否则follower进行一致性检查
     * 4。添加任何在现有日志中不存在的数据，删除多余数据
     * 5。如果AppendEntries#leaderCommit大于自身的当前commitIndex，
     *    则将commitIndex更新为Max(leaderCommit,commitIndex)，乐观
     *    地将本地已提交日记的commitIndex"跃进"到领导人为该Follower跟踪
     *    记得的值。【用于Follower刚从故障中恢复过来的场景。】
     * @param appendEntries 日志
     * @return resp
     */
    AppendEntriesResp appendCommand(AppendEntries appendEntries);

    Boolean commit(Long term, Long index);
}
