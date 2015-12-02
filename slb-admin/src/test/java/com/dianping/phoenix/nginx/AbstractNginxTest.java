package com.dianping.phoenix.nginx;

import com.dianping.phoenix.AbstractSkipTest;
import com.dianping.phoenix.TestConfig;
import com.dianping.phoenix.lb.monitor.HttpClientUtil;
import com.dianping.phoenix.lb.utils.IOUtilsWrapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 测试nginx的功能，用于升级nginx后的功能测试
 *
 * @author mengwenchao
 *         <p/>
 *         2014年8月1日 下午4:35:40
 */
@SuppressWarnings("deprecation")
public abstract class AbstractNginxTest extends AbstractSkipTest {

	protected String requestIp = "192.168.218.22";

	protected int requestPort;

	protected String unitTestUpstreamName = "unit_test_upstream";

	protected ClientConnectionManager ccm;

	protected String localAddress;

	protected int startPort = 7000;

	protected Map<Integer, ServerSocket> servers = new HashMap<Integer, ServerSocket>();

	protected ExecutorService executors = Executors.newCachedThreadPool();

	@Before
	public void prepareHttpClient() throws UnknownHostException {

		ccm = new ThreadSafeClientConnManager();
		requestIp = TestConfig.getDengineAddress();
		requestPort = Integer.parseInt(TestConfig.getDengineAddressPort());
		localAddress = Inet4Address.getLocalHost().getHostAddress();
	}

	public HttpClient createHttpClient() {
		return new DefaultHttpClient(ccm);
	}

	protected HttpResponse executeAndGetResponse(HttpGet get) throws ClientProtocolException, IOException {

		try {
			HttpClient httpClient = createHttpClient();
			return httpClient.execute(get);
		} finally {
			get.releaseConnection();
		}

	}

	protected int startRandomServer(final String random) throws IOException {

		return startRandomPortServer(new RandomAction(random));
	}

	protected void sleepSeconds(int count) {

		try {
			TimeUnit.SECONDS.sleep(5);
		} catch (InterruptedException e) {
			logger.error("[sleepSeconds]", e);
		}

	}

	protected void stopServer(int port) throws IOException {

		ServerSocket ss = servers.get(port);
		if (ss == null) {
			return;
		}
		ss.close();
		servers.remove(port);
		if (logger.isInfoEnabled()) {
			logger.info("[stopServer][server stopped]" + port);
		}
	}

	protected int startRandomPortServer(final SocketAction action) throws IOException {

		int port;

		while (true) {

			try {
				port = startPort++;
				startServer(port, action);
				break;
			} catch (BindException e) {
				logger.info("[startRandomPortServer]", e);
			}
		}
		return port;
	}

	protected void startServer(int port, final SocketAction action) throws IOException {

		if (servers.get(port) != null) {
			logger.warn("[startServer][server already started]" + port);
			throw new IllegalArgumentException("server already started " + port);
		}

		if (logger.isInfoEnabled()) {
			logger.info("[startServer][begin]" + port);
		}
		final ServerSocket ss = new ServerSocket(port);
		servers.put(port, ss);

		executors.execute(new Runnable() {

			@Override
			public void run() {
				try {
					while (true) {
						Socket s = ss.accept();
						if (logger.isInfoEnabled()) {
							logger.info("[run][new socket]" + s);
						}
						executors.execute(new TemplateTask(s, action));
					}
				} catch (IOException e) {
					logger.error("error get result", e);
				}

			}
		});
	}

	@After
	public void disposeHttpClient() throws IOException {
		ccm.shutdown();

		for (Integer key : servers.keySet()) {
			if (logger.isInfoEnabled()) {
				logger.info("[disposeHttpClient][stop]" + key);
			}
			ServerSocket ss = servers.get(key);
			ss.close();
		}
		if (logger.isInfoEnabled()) {
			logger.info("[disposeHttpClient][stop]" + this);
		}
		servers.clear();
	}

	protected String callWithResult(String url) throws IOException {
		try {
			if (logger.isInfoEnabled()) {
				logger.info("[callWithResult]" + url);
			}
			String result = HttpClientUtil.getAsString(url, null, DEFAULT_ENCODING);
			if (logger.isInfoEnabled()) {
				logger.info("[callWithResult]" + result);
			}
			return result;
		} catch (IOException e) {
			logger.error("[callWithResult]", e);
			throw e;
		}
	}

	/**
	 * 期望特定返回码
	 *
	 * @param statusCode
	 * @return
	 * @throws IOException
	 */
	protected boolean requestExpectStatus(String url, int statusCode) throws IOException {

		try {
			String result = callWithResult(url);
			if (logger.isDebugEnabled()) {
				logger.debug("[requestExpectStatus]" + result);
			}
			Assert.assertTrue(false);
		} catch (Exception e) {

			logger.error(e.getMessage());
			if (e.getMessage().indexOf(String.valueOf(statusCode)) >= 0) {
				return true;
			}
		}
		return false;
	}

	protected String getResponseBody(HttpResponse response) throws IllegalStateException, IOException {
		HttpEntity entity = response.getEntity();
		return IOUtilsWrapper.convetStringFromRequest(entity.getContent());
	}

	static interface SocketAction {

		int read(InputStream ins) throws IOException;

		void write(OutputStream ous) throws IOException;

		boolean isOnce();
	}

	static abstract class AbstractSocketAction implements SocketAction {

		protected byte[] getHttpBytes(String content) throws IOException {

			byte[] byteContent = content.getBytes();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write("HTTP/1.1 200 OK\r\n".getBytes());
			baos.write("Content-Type: text/plain\r\n".getBytes());
			baos.write(("Content-length: " + byteContent.length + "/plain\r\n").getBytes());
			baos.write("\r\n".getBytes());
			baos.write(byteContent);

			return baos.toByteArray();

		}
	}

	static class RandomAction extends AbstractSocketAction implements SocketAction {

		private String random;

		public RandomAction(String random) {
			this.random = random;
		}

		@Override
		public int read(InputStream ins) {
			return 0;
		}

		@Override
		public void write(OutputStream ous) throws IOException {
			ous.write(getHttpBytes(random));
		}

		@Override
		public boolean isOnce() {
			return true;
		}

	}

	static class TemplateTask implements Runnable {

		protected final Logger logger = LoggerFactory.getLogger(getClass());
		private Socket s;
		private SocketAction socketAction;

		public TemplateTask(Socket s, SocketAction action) {
			this.s = s;
			this.socketAction = action;
		}

		@Override
		public void run() {
			try {

				while (true) {
					int ret = socketAction.read(s.getInputStream());
					if (ret == -1) {
						break;
					}
					socketAction.write(s.getOutputStream());
					s.getOutputStream().flush();
					if (socketAction.isOnce()) {
						break;
					}
				}
			} catch (IOException e) {
				logger.error("[run]", e);
			} finally {
				try {
					s.close();
				} catch (IOException e) {
					logger.error("[run]", e);
				}
			}
		}
	}

}
