package com.dianping.phoenix.lb.shell;

import org.apache.commons.exec.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public class DefaultScriptExecutor implements ScriptExecutor {

	private static final Logger logger = LoggerFactory.getLogger(DefaultScriptExecutor.class);

	private ExecuteWatchdog watchdog;

	private DefaultExecutor executor;

	public DefaultScriptExecutor() {
		executor = new DefaultExecutor();
		watchdog = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);
		executor.setWatchdog(watchdog);
	}

	@Override
	public void exec(String scriptPath, OutputStream stdOut, OutputStream stdErr,
			final ExecuteResultCallback execCallback) throws IOException {
		PumpStreamHandler streamHandler = new PumpStreamHandler(stdOut, stdErr);
		CommandLine cmd = CommandLine.parse(scriptPath);
		executor.setExitValues(null);
		executor.setStreamHandler(streamHandler);
		ExecuteResultHandler execResultHandler = new ExecuteResultHandler() {

			@Override
			public void onProcessFailed(ExecuteException e) {
				if (execCallback != null) {
					execCallback.onProcessFailed(e);
				}
			}

			@Override
			public void onProcessComplete(int exitCode) {
				if (execCallback != null) {
					execCallback.onProcessCompleted(exitCode);
				}
			}
		};

		try {
			logger.info("[exec][begin]" + cmd.toString());
			executor.execute(cmd, execResultHandler);
		} finally {
			logger.info("[exec][end]" + cmd.toString());
		}
	}

	@Override
	public void kill() {
		watchdog.destroyProcess();
	}

	@Override
	public int exec(String scriptPath, OutputStream stdOut, OutputStream stdErr) throws IOException {
		PumpStreamHandler streamHandler = new PumpStreamHandler(stdOut, stdErr);
		CommandLine cmd = CommandLine.parse(scriptPath);
		executor.setExitValues(null);
		executor.setStreamHandler(streamHandler);

		try {
			logger.info("[exec][begin]" + cmd.toString());
			return executor.execute(cmd);
		} finally {
			logger.info("[exec][end]" + cmd.toString());
		}
	}

}
