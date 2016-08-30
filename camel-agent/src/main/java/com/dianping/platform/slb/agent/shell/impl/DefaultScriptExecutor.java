package com.dianping.platform.slb.agent.shell.impl;

import com.dianping.platform.slb.agent.shell.ScriptExecutor;
import org.apache.commons.exec.*;

import java.io.IOException;
import java.io.OutputStream;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public class DefaultScriptExecutor implements ScriptExecutor {

	private Executor m_executor = new DefaultExecutor();

	private ExecuteWatchdog m_executeWatchdog;

	public DefaultScriptExecutor() {
		this(300000);
	}

	public DefaultScriptExecutor(long watchDogMills) {
		m_executeWatchdog = new ExecuteWatchdog(watchDogMills);
		m_executor.setWatchdog(m_executeWatchdog);
	}

	@Override
	public int execute(String command, OutputStream stdOutput, OutputStream errorOutput) throws IOException {
		PumpStreamHandler streamHandler = new PumpStreamHandler(stdOutput, errorOutput);

		m_executor.setExitValues(null);
		m_executor.setStreamHandler(streamHandler);

		CommandLine commandLine = CommandLine.parse(command);

		return m_executor.execute(commandLine);
	}

	@Override
	public void kill() {
		m_executeWatchdog.killedProcess();
	}
}
