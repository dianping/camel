package com.dianping.platform.slb.agent.task.workflow.log;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Service
public class DefaultLogPrinter implements LogPrinter {

	private final static String NEW_LINE = "\r\n";

	public static final String CHUNK_TERMINATOR = "\r\n--9ed2b78c112fbd17a8511812c554da62941629a8--\r\n";

	public static final String LOG_TERMINATOR = "\r\n--255220d51dc7fb4aacddadedfe252a346da267d4--\r\n";

	@Override
	public void writeTerminator(OutputStream outputStream) throws IOException {
		outputStream.write(LOG_TERMINATOR.getBytes("ascii"));
		outputStream.flush();
	}

	@Override
	public void writeChunkTerminator(OutputStream outputStream) throws IOException {
		outputStream.write(CHUNK_TERMINATOR.getBytes("ascii"));
		outputStream.flush();
	}

	@Override
	public void writeChunkHeader(OutputStream outputStream, Map<String, String> headers) throws IOException {
		StringBuilder sb = new StringBuilder();

		if (headers != null) {
			for (Map.Entry<String, String> headEntry : headers.entrySet()) {
				sb.append(headEntry.getKey());
				sb.append(": ");
				sb.append(headEntry.getValue());
				sb.append(NEW_LINE);
			}
		}

		if (sb.length() > 0) {
			sb.append(NEW_LINE);
		} else {
			sb.append(NEW_LINE);
			sb.append(NEW_LINE);
		}

		outputStream.write(sb.toString().getBytes());
	}

}
