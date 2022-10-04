package com.github.raft.core.rpc.vote;

import com.github.raft.core.CommandLog;
import com.github.raft.core.Raft;
import com.github.raft.core.RaftNode;
import com.github.raft.core.appender.CommandLogAppender;
import com.github.raft.core.common.TermVoter;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-01
 */
public class RequestVoteRpcImpl implements RequestVoteRpc {

    private int nodeID;
    private CommandLogAppender commandLogAppender;

    public RequestVoteRpcImpl(int nodeID, CommandLogAppender commandLogAppender) {
        this.nodeID = nodeID;
        this.commandLogAppender = commandLogAppender;
    }

    @Override
    public RequestVoteResp requestVote(RequestVote requestVote) {
        RequestVoteResp resp = new RequestVoteResp();
        RaftNode raftNode = Raft.getNode(nodeID);
        long selfTerm = raftNode.getCurTerm();
        if (selfTerm > requestVote.getTerm()) {
            resp.setTerm(selfTerm);
            resp.setVoteGranted(false);
            return resp;
        }
        TermVoter termVoter = raftNode.getTermVoterHolder().getTermVoter(requestVote.getTerm());
        // 请求没有投票给别人
        if (termVoter.lockVote(nodeID)) {
            resp.setTerm(requestVote.getTerm());
            resp.setVoteGranted(true);
            return resp;
        } else if (requestVote.getCadidateID() == termVoter.getVoteNodeID()) {
            // 选票投递给了请求方, 且日志记录一样新
            CommandLog commandLog = commandLogAppender.peek();
            if (commandLog == null && requestVote.getLastLogIndex() == -1 && requestVote.getLastLongTerm() == -1) {
                resp.setTerm(requestVote.getTerm());
                resp.setVoteGranted(true);
                return resp;
            }
            if (commandLog != null && commandLog.getTerm() == requestVote.getLastLongTerm()
                    && commandLog.getIndex() == requestVote.getLastLogIndex()) {
                resp.setTerm(requestVote.getTerm());
                resp.setVoteGranted(true);
                return resp;
            }
        }
        // 最后就是投票给了别人
        resp.setTerm(requestVote.getTerm());
        resp.setVoteGranted(false);
        return resp;
    }
}
