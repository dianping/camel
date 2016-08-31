package com.dianping.platform.slb.agent.task.workflow.log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface LogPrinter {

	void writeTerminator(OutputStream outputStream) throws IOException;

	void writeChunkTerminator(OutputStream outputStream) throws IOException;

	void writeChunkHeader(OutputStream outputStream, Map<String, String> headers) throws IOException;

}
