package com.github.raft.core.rpc.replication;

import com.github.raft.core.CommandLog;
import com.github.raft.core.Raft;
import com.github.raft.core.RaftNode;
import com.github.raft.core.RemoteRouter;
import com.github.raft.core.appender.CommandLogAppender;
import com.github.raft.core.common.CountWaiter;
import com.github.raft.core.common.IDUtils;
import com.github.raft.core.common.LoggerUtils;
import java.io.Closeable;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-02
 * leader 向follower发送复制请求
 */
public class LeaderAppendEntriesClient implements Closeable {

    private int nodeID;

    private RemoteRouter<AppendEntriesRpc> remoteRouter;

    private CommandLogAppender commandLogAppender;

    private final ExecutorService commandExecutorService;
    private final ExecutorService heartbeatExecutorService;

    public LeaderAppendEntriesClient(int nodeID, RemoteRouter<AppendEntriesRpc> remoteRouter,
                                     CommandLogAppender commandLogAppender) {
        this.nodeID = nodeID;
        this.remoteRouter = remoteRouter;
        this.commandLogAppender = commandLogAppender;
        commandExecutorService = newExecutorService("command", Raft.nodeCount(), 1);
        heartbeatExecutorService = newExecutorService("heartbeat", Raft.nodeCount(), 1);
    }

    public synchronized boolean appendCommand(AppendEntries appendEntries) {
        Set<RaftNode> nodes = Raft.getAllNodes();
        final CountWaiter countWaiter = new CountWaiter(Raft.nodeCount() - 1);
        for (RaftNode node : nodes) {
            if (node.getId().equals(nodeID)) {
                continue;
            }
            commandExecutorService.execute(() -> {
                RaftNode toNode = Raft.getNode(node.getId());
                AppendEntries toAppendEntries = appendEntries.clone();
                toAppendEntries.setLeaderCommit(toNode.curCommitIndex());
                try {
                    AppendEntriesResp resp = remoteRouter.routeRpc(node.getNodeIpPort()).appendCommand(appendEntries);
                    LoggerUtils.getLogger().debug("appendCommand node: {}, resp: {}", toNode.getId(), resp);
                    if (resp.isSuccess()) {
                        countWaiter.countDownSuccess();
                    } else if (resp.getTerm() <= Raft.getNode(nodeID).getCurTerm()) {
                        long endIndex = toAppendEntries.getPrevLogIndex();
                        long nextIndex = endIndex - 1;
                        while (nextIndex >= -1) {
                            if (nextIndex == -1) {
                                toAppendEntries.setPrevLogTerm(-1);
                                toAppendEntries.setPrevLogIndex(-1);
                                toAppendEntries.setEntries(commandLogAppender.range(0, endIndex));
                            } else {
                                CommandLog commandLog = commandLogAppender.index(nextIndex);
                                toAppendEntries.setPrevLogTerm(commandLog.getTerm());
                                toAppendEntries.setPrevLogIndex(commandLog.getIndex());
                                toAppendEntries.setEntries(commandLogAppender.range(nextIndex, endIndex));
                            }
                            resp = remoteRouter.routeRpc(node.getNodeIpPort()).appendCommand(toAppendEntries);
                            if (resp.isSuccess()) {
                                // 追加当前提交
                                resp = remoteRouter.routeRpc(node.getNodeIpPort()).appendCommand(appendEntries);
                                if (resp.isSuccess()) {
                                    countWaiter.countDownSuccess();
                                }
                                break;
                            }
                            nextIndex--;
                        }
                    }
                    // leader 过期由心跳去处理
                } catch (Throwable throwable) {
                    countWaiter.countDownException();
                    LoggerUtils.getLogger().warn("append command to node {} error, error msg: {}", toNode.getId(),
                            throwable.getMessage());
                } finally {
                    countWaiter.countDown();
                }
            });
        }
        // 这儿应该用异步判断，如果多数节点成功，就返回future然后结束，上面也是异步去进行同步
        int count = Raft.nodeCount();
        int halfCount = (count >> 1) + 1;
        countWaiter.await();
        return countWaiter.successCount() + 1 >= halfCount;
    }

    public void commit(CommandLog commandLog) {
        commandExecutorService.execute(() -> {
            Raft.getAllNodes().forEach(raftNode -> {
                if (raftNode.getId() == nodeID) {
                    return;
                }
                try {
                    if (remoteRouter.routeRpc(raftNode.getNodeIpPort())
                            .commit(commandLog.getTerm(), commandLog.getIndex())) {
                        raftNode.seekCommitIndex(commandLog.getIndex());
                    }
                } catch (Throwable throwable) {
                    LoggerUtils.getLogger().warn("append command to node {} error, error msg: {}", raftNode.getId(),
                            throwable.getMessage());
                }
            });
        });
    }

    public boolean sendHeartbeatCommand() {
        CountWaiter countWaiter = new CountWaiter(Raft.nodeCount() - 1);
        AtomicBoolean success = new AtomicBoolean(Boolean.TRUE);
        Raft.getAllNodes().forEach(raftNode -> {
            if (raftNode.getId() == nodeID) {
                return;
            }
            heartbeatExecutorService.execute(() -> {
                AppendEntries heartbeat = newAppendEntries();
                heartbeat.setLeaderCommit(raftNode.curCommitIndex());
                try {
                    AppendEntriesResp resp = remoteRouter.routeRpc(raftNode.getNodeIpPort()).appendCommand(heartbeat);
                    if (!resp.isSuccess() && resp.getTerm() > Raft.getNode(nodeID).getCurTerm()) {
                        success.set(Boolean.FALSE);
                    }
                } finally {
                    countWaiter.countDown();
                }
            });
        });
        countWaiter.await();
        return success.get();
    }

    public final AppendEntries newAppendEntries() {
        RaftNode leaderRaftNode = Raft.getNode(nodeID);
        AppendEntries entries = new AppendEntries();
        entries.setLeaderID(leaderRaftNode.getId());
        entries.setTerm(leaderRaftNode.getCurTerm());
        CommandLog commandLog = commandLogAppender.index(leaderRaftNode.curCommitIndex());
        if (commandLog == null) {
            entries.setPrevLogTerm(-1);
            entries.setPrevLogIndex(-1);
        } else {
            entries.setPrevLogTerm(commandLog.getTerm());
            entries.setPrevLogIndex(commandLog.getIndex());
        }
        return entries;
    }

    @Override
    public void close() throws IOException {
        commandExecutorService.shutdown();
        heartbeatExecutorService.shutdownNow();
    }

    private static ExecutorService newExecutorService(String name, int threads, int queueCapicaty) {
        return new ThreadPoolExecutor(threads, threads, 60, TimeUnit.SECONDS, new LinkedBlockingDeque<>(queueCapicaty),
                thread -> new Thread(thread, name + "-" + IDUtils.newID()), new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
