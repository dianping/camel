package com.dianping.platform.slb.agent.shell;

import java.io.IOException;
import java.io.OutputStream;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface ScriptExecutor {

	int execute(String command, OutputStream stdOutput, OutputStream errorOutput) throws IOException;

	void kill();

}
