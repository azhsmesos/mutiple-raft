package com.github.raft.demo;

import com.github.raft.core.CommandLog;
import com.github.raft.core.StateMachine;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2022-10-04
 */
public class PrintStateMachine implements StateMachine {


    @Override
    public void apply(CommandLog commandLog) {
        byte[] command = commandLog.getCommand();
        String cmd = new String(command);
        System.out.println("state machine: " + cmd);
    }
}
