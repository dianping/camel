package com.dianping.platform.slb.agent.core.script;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.IOException;
import java.io.OutputStream;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public class DefaultScriptExecutor implements ScriptExecutor {

	private DefaultExecutor m_executor = new DefaultExecutor();

	private ExecuteWatchdog m_watchdog = new ExecuteWatchdog(300000);

	{
		m_executor.setWatchdog(m_watchdog);
	}

	@Override
	public int exec(String scriptPath, OutputStream normalOutput, OutputStream errorOutput) throws IOException {
		CommandLine commandLine = CommandLine.parse(scriptPath);
		PumpStreamHandler m_handler = new PumpStreamHandler(normalOutput, errorOutput);

		m_executor.setStreamHandler(m_handler);
		m_executor.setExitValues(null);
		return m_executor.execute(commandLine);
	}

	@Override
	public void kill() {
		m_watchdog.destroyProcess();
	}

}
