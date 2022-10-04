package com.github.raft.logger;

import com.github.raft.core.common.LoggerUtils;
import org.junit.Test;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-02
 */

public class LoggerTest {

    @Test
    public void testLogger() {
        LoggerUtils.getLogger().info("raft info");
    }
}
