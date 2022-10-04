package com.github.raft.core.common;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-01
 * 没期自己拥有的投票
 */
public class TermVoteHolder {

    private final Map<Long, TermVoter> termVoterMap = new HashMap<>();

    public volatile long newTerm = 0;

    public synchronized TermVoter getTermVoter(long term) {
        newTerm = term;
        remove();
        if (!termVoterMap.containsKey(term)) {
            termVoterMap.put(term, new TermVoter(term));
        }
        return termVoterMap.get(term);
    }

    private void remove() {
        long curTerm = newTerm - 5;
        for (; curTerm >= 0; curTerm--) {
            if (termVoterMap.remove(curTerm) == null) {
                break;
            }
        }
    }
}
