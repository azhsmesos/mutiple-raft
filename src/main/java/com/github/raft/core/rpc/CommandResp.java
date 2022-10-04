package com.github.raft.core.rpc;

import com.github.raft.core.common.NodeIpPort;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-04
 */
public class CommandResp {

    /**
     * 命令是否提交成功
     */
    private boolean success;
    /**
     * 当需要重定向时
     */
    private NodeIpPort redirectToLeader;

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public NodeIpPort getRedirectToLeader() {
        return redirectToLeader;
    }

    public void setRedirectToLeader(NodeIpPort redirectToLeader) {
        this.redirectToLeader = redirectToLeader;
    }

    public boolean isSuccess() {
        return success;
    }

}
