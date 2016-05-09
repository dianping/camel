package com.dianping.phoenix.lb.shell;

import java.io.IOException;
import java.io.OutputStream;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface ScriptExecutor {

	int exec(String scriptPath, OutputStream stdOut, OutputStream stdErr) throws IOException;

	void exec(String scriptPath, OutputStream stdOut, OutputStream stdErr, ExecuteResultCallback callback)
			throws IOException;

	void kill();

}
