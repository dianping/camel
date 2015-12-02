package com.dianping.phoenix.lb.action.interceptor;

import com.dianping.phoenix.lb.constant.SlbConfig;
import com.dianping.phoenix.lb.utils.IOUtilsWrapper;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;
import org.apache.struts2.ServletActionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public class RecordInterceptor extends AbstractInterceptor {

	protected final Logger m_logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private SlbConfig m_slbConfig;

	@Override
	public String intercept(ActionInvocation invocation) throws Exception {
		HttpServletRequest request = ServletActionContext.getRequest();
		String servletPath = request.getServletPath();

		if (needRecord(servletPath)) {
			BufferedRequestWrapper bufferedRequest = new BufferedRequestWrapper(request);

			ServletActionContext.setRequest(bufferedRequest);

			InputStream inputStream = bufferedRequest.getInputStream();
			String requestContent = IOUtilsWrapper.convetStringFromRequest(inputStream);
			String ipAddress = request.getRemoteAddr();
			String result;

			if (servletPath.contains("api")) {
				BufferedResponseWrapper bufferedResponse = new BufferedResponseWrapper(
						ServletActionContext.getResponse());

				ServletActionContext.setResponse(bufferedResponse);

				long startMills = System.currentTimeMillis();
				result = invocation.invoke();

				bufferedResponse.finish();
				m_logger.info("recorder: " + servletPath + " " + ipAddress + "\n\nrequest:" + requestContent + "\n\n"
								+ "response: " + bufferedResponse.getContent() + "\n\nuse: " + (
								System.currentTimeMillis() - startMills) + "ms\n\n\n");
			} else {
				result = invocation.invoke();

				m_logger.info("recorder: " + servletPath + " " + ipAddress + "\n" + requestContent + "\n");
			}

			return result;
		} else {
			return invocation.invoke();
		}
	}

	private boolean needRecord(String servletPath) {
		try {
			List<Pattern> recordPatterns = m_slbConfig.getRecordURLPatterns();

			if (matchPattern(servletPath, recordPatterns)) {
				List<Pattern> noRecordPatterns = m_slbConfig.getNoRecordURLPatterns();

				if (!matchPattern(servletPath, noRecordPatterns)) {
					return true;
				}
			}
			return false;
		} catch (Exception ex) {
			m_logger.error("judge record error", ex);
			return false;
		}
	}

	private boolean matchPattern(String str, List<Pattern> patterns) {
		for (Pattern pattern : patterns) {
			Matcher matcher = pattern.matcher(str);

			if (matcher.find()) {
				return true;
			}
		}
		return false;
	}

	public class BufferedRequestWrapper extends HttpServletRequestWrapper {

		ByteArrayInputStream m_inputStream;

		ByteArrayOutputStream m_outputStream;

		BufferedServletInputStream m_wrappedInputStream;

		byte[] m_buffer;

		public BufferedRequestWrapper(HttpServletRequest req) throws IOException {
			super(req);
			InputStream inputStream = req.getInputStream();
			m_outputStream = new ByteArrayOutputStream();
			byte buf[] = new byte[1024];
			int count;

			while ((count = inputStream.read(buf)) > 0) {
				m_outputStream.write(buf, 0, count);
			}
			m_buffer = m_outputStream.toByteArray();
		}

		@SuppressWarnings("finally")
		@Override
		public ServletInputStream getInputStream() {
			try {
				m_inputStream = new ByteArrayInputStream(m_buffer);
				m_wrappedInputStream = new BufferedServletInputStream(m_inputStream);
			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {
				return m_wrappedInputStream;
			}
		}
	}

	public class BufferedServletInputStream extends ServletInputStream {

		ByteArrayInputStream m_inputStream;

		public BufferedServletInputStream(ByteArrayInputStream bais) {
			this.m_inputStream = bais;
		}

		public int available() {
			return m_inputStream.available();
		}

		public int read() {
			return m_inputStream.read();
		}

		public int read(byte[] buf, int off, int len) {
			return m_inputStream.read(buf, off, len);
		}

	}

	public class BufferedResponseWrapper extends HttpServletResponseWrapper {

		private BufferedServletOutputStream m_outputStream;

		private PrintWriter m_writer;

		private BufferedServletOutputStream m_writerStream;

		public BufferedResponseWrapper(HttpServletResponse response) throws IOException {
			super(response);
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			if (m_writer != null) {
				throw new IllegalStateException();
			}
			if (m_outputStream == null) {
				m_outputStream = new BufferedServletOutputStream(getResponse().getOutputStream());
			}
			return m_outputStream;
		}

		@Override
		public PrintWriter getWriter() throws IOException {
			if (m_outputStream != null) {
				throw new IllegalStateException();
			}
			if (m_writer == null) {
				m_writerStream = new BufferedServletOutputStream(getResponse().getOutputStream());
				m_writer = new PrintWriter(new OutputStreamWriter(m_writerStream));
			}
			return m_writer;
		}

		@Override
		public void setContentLength(int len) {
			//ignore it
		}

		public void finish() throws IOException {
			if (m_outputStream != null) {
				m_outputStream.flush();
			} else {
				m_writer.flush();
			}
		}

		public String getContent() {
			if (m_outputStream != null) {
				return m_outputStream.getContent();
			} else if (m_writerStream != null) {
				return m_writerStream.getContent();
			} else {
				return "";
			}
		}
	}

	public class BufferedServletOutputStream extends ServletOutputStream {

		StringBuilder builder = new StringBuilder();

		ServletOutputStream m_outputStream;

		public BufferedServletOutputStream(ServletOutputStream outputStream) {
			m_outputStream = outputStream;
		}

		@Override
		public void write(int arg0) throws IOException {
			builder.append((char) (byte) arg0);
			m_outputStream.write(arg0);
		}

		public String getContent() {
			return builder.toString();
		}

	}

}
