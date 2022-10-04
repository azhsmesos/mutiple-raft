package com.github.raft.core;

import com.github.raft.core.common.NodeIpPort;
import com.github.raft.core.common.TermVoteHolder;
import com.github.raft.core.common.TermVoter;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-01
 */
public class RaftNode {

    // 节点ID
    private Integer id;

    // 节点当前任期号
    private AtomicLong curTerm = new AtomicLong(0);

    // 节点角色，默认候选人
    private AtomicReference<RaftNodeRole> raftNodeRole = new AtomicReference<>(RaftNodeRole.Candidate);

    // 当前任期选举投票总数
    private AtomicInteger voteNumber = new AtomicInteger(0);

    // 记录已提交日志条目的索引
    private AtomicLong commitIndex = new AtomicLong(0);

    private AtomicReference<NodeIpPort> nodeIpPort = new AtomicReference<>();

    private TermVoteHolder termVoteHolder = new TermVoteHolder();

    public TermVoteHolder getTermVoterHolder() {
        return termVoteHolder;
    }

    public RaftNode(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public long getCurTerm() {
        return curTerm.longValue();
    }

    public RaftNodeRole getRaftNodeRole() {
        return raftNodeRole.get();
    }

    public void setNodeIpPort(NodeIpPort nodeIpPort) {
        this.nodeIpPort.set(nodeIpPort);
    }

    public NodeIpPort getNodeIpPort() {
        return nodeIpPort.get();
    }

    public void initCurTerm(long term) {
        this.curTerm.compareAndSet(0, term);
    }

    /**
     * 重置到新的一期开始选举状态
     * @return true：获取自己的选票，false：没有获取到自己的选票
     */
    public synchronized boolean restNewTermVote() {
        this.raftNodeRole.set(RaftNodeRole.Candidate);
        long curTerm = this.curTerm.incrementAndGet();
        TermVoter termVoter = termVoteHolder.getTermVoter(curTerm);
        if (termVoter.lockVote(this.id)) {
            this.voteNumber.set(1);
            return true;
        }
        return false;
    }

    public void incrVoteNumber() {
        this.voteNumber.incrementAndGet();
    }

    public int getVoteNumber() {
        return voteNumber.intValue();
    }

    public void setFollower(long leaderTerm) {
        if (leaderTerm >= getCurTerm()) {
            this.raftNodeRole.set(RaftNodeRole.Follower);
            this.curTerm.set(leaderTerm);
        }
    }

    public void setLeader(long leaderTerm) {
        if (leaderTerm >= getCurTerm()) {
            this.raftNodeRole.set(RaftNodeRole.Leader);
            this.curTerm.set(leaderTerm);
            for (RaftNode raftNode : Raft.getAllNodes()) {
                if (raftNode.getId().equals(id)) {
                    continue;
                }
                raftNode.setFollower(leaderTerm);
            }
        }
    }

    public void seekCommitIndex(long newCommitIndex) {
        this.commitIndex.set(newCommitIndex);
    }

    public long curCommitIndex() {
        return commitIndex.longValue();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        InputStream file = null;
        if (!(object instanceof RaftNode node)) {
            return false;
        }
        return Objects.equals(getId(), node.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
