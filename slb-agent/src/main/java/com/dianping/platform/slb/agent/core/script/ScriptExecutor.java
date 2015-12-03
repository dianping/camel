package com.dianping.platform.slb.agent.core.script;

import java.io.IOException;
import java.io.OutputStream;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface ScriptExecutor {

	int exec(String scriptPath, OutputStream normalOutput, OutputStream errorOutput) throws IOException;

	void kill();

}
