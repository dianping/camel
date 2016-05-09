package com.dianping.phoenix.nginx;

import com.dianping.phoenix.lb.monitor.DegradeStatus;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 降级测试
 * 此处强制升降级调用http接口
 *
 * @author mengwenchao
 *         <p/>
 *         2014年9月10日 下午1:46:39
 */
public class DegradeTest extends AbstractDegradeTest {

	protected static final int degradeRate = 60;
	private static final int FORCE_STATE_UP = 1;
	private static final int FORCE_STATE_DOWN = -1;
	private static final int FORCE_STATE_NORMAL = 0;
	private static final int testCount = 300;

	private int[] ports = { 7777, 7778, 7779, 7780 };

	private Map<Integer, ServerSocket> servers = new ConcurrentHashMap<Integer, ServerSocket>();

	@Before
	public void beforeDegradeTest() throws IOException {

		for (int port : ports) {
			startServer(port, new EchoPortAction(port));
		}
		putUpstreamDegradeBackup();
		putUpstreamDegradeNormal();

	}

	@Test
	public void multiFoceTest() throws InterruptedException, ClientProtocolException, IOException {

		List<String> checkUpstreams = null;
		List<String> unCheckedUpstreams = null;
		try {
			checkUpstreams = addUpstreams(testCount / 2, simpleCheckUpstreamConfig);
			unCheckedUpstreams = addUpstreams(testCount / 2, simpleUnCheckUpstreamConfig);
			TimeUnit.SECONDS.sleep(5);

			forceDown(checkUpstreams);
			checkForceState(checkUpstreams, FORCE_STATE_DOWN);
			checkForceState(unCheckedUpstreams, FORCE_STATE_NORMAL);
			forceDown(unCheckedUpstreams);
			checkForceState(checkUpstreams, FORCE_STATE_DOWN);
			checkForceState(unCheckedUpstreams, FORCE_STATE_DOWN);

			forceUp(checkUpstreams);
			forceUp(unCheckedUpstreams);
			checkForceState(checkUpstreams, FORCE_STATE_UP);
			checkForceState(unCheckedUpstreams, FORCE_STATE_UP);

			forceNormal(checkUpstreams);
			forceNormal(unCheckedUpstreams);
			checkForceState(checkUpstreams, FORCE_STATE_NORMAL);
			checkForceState(unCheckedUpstreams, FORCE_STATE_NORMAL);
		} finally {
			deleteUpstreams(checkUpstreams);
			deleteUpstreams(unCheckedUpstreams);
		}
	}

	private void checkForceState(List<String> upstreams, int forceState) throws IOException {
		Map<String, DegradeStatus> status = getDegradeUpstreams();
		for (String upstream : upstreams) {
			DegradeStatus ds = status.get(upstream);
			Assert.assertNotNull(ds);
			Assert.assertEquals(forceState, ds.getForceState());
		}
	}

	/**
	 * 在降级状态下测试
	 *
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testForceWhileDown() throws IOException, InterruptedException {

		shutdownNormal();
		TimeUnit.SECONDS.sleep(5);
		requestExpectBackup();

		forceUp(unitTestUpstreamName);
		//502
		requestManyExpectStatus(502);

		forceNormal(unitTestUpstreamName);
		requestExpectBackup();

		forceDown(unitTestUpstreamName);
		requestExpectBackup();

		forceNormal(unitTestUpstreamName);
	}

	/**
	 * 在升级状态下测试
	 *
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testForceWhileUp() throws IOException, InterruptedException {

		TimeUnit.SECONDS.sleep(5);
		requestExpectNormal();

		forceDown(unitTestUpstreamName);
		requestExpectBackup();

		forceNormal(unitTestUpstreamName);
		requestExpectNormal();

		forceUp(unitTestUpstreamName);
		requestExpectNormal();

		forceNormal(unitTestUpstreamName);
	}

	protected void forceNormal(List<String> upstreams) throws IOException {
		String url = "http://" + requestIp + ":6666/degrade/force/auto?upstreams=" + StringUtils.join(upstreams, ",");
		callWithResult(url);
	}

	protected void forceNormal(String... upstreams) throws IOException {

		String url = "http://" + requestIp + ":6666/degrade/force/auto?upstreams=" + StringUtils.join(upstreams, ",");
		callWithResult(url);
	}

	protected void forceUp(List<String> upstreams) throws IOException {

		String url = "http://" + requestIp + ":6666/degrade/force/up?upstreams=" + StringUtils.join(upstreams, ",");
		callWithResult(url);
	}

	protected void forceUp(String... upstreams) throws IOException {

		String url = "http://" + requestIp + ":6666/degrade/force/up?upstreams=" + StringUtils.join(upstreams, ",");
		callWithResult(url);
	}

	protected void forceDown(List<String> upstreams) throws IOException {

		String url = "http://" + requestIp + ":6666/degrade/force/down?upstreams=" + StringUtils.join(upstreams, ",");
		callWithResult(url);
	}

	protected void forceDown(String... upstreams) throws IOException {

		String url = "http://" + requestIp + ":6666/degrade/force/down?upstreams=" + StringUtils.join(upstreams, ",");
		callWithResult(url);
	}

	/**
	 * 自动降级测试
	 *
	 * @throws Exception
	 */
	@Test
	public void testAutoDegrade() throws Exception {
		try {

			for (int i = 0; i < 10; i++) {

				TimeUnit.SECONDS.sleep(6);
				requestExpectNormal();

				shutdownNormal();
				TimeUnit.SECONDS.sleep(6);
				requestExpectBackup();

				startNormal();
			}
		} catch (Exception e) {
			logger.error("[testDegeade]", e);
			throw e;
		} finally {
		}
	}

	private void requestExpectBackup() throws IOException {

		String url = "http://" + requestIp + ":" + requestPort + "/";

		for (int i = 0; i < testCount; i++) {
			String result = callWithResult(url);
			if (logger.isDebugEnabled()) {
				logger.debug("[requestExpectBackup]" + result);
			}
			Assert.assertTrue(resultBackup(Integer.parseInt(result.trim())));
		}
	}

	private boolean resultBackup(int result) {
		for (int i = ports.length / 2; i < ports.length; i++) {
			if (result == ports[i]) {
				return true;
			}
		}
		return false;
	}

	private boolean resultNormal(int result) {

		for (int i = 0; i < ports.length / 2; i++) {
			if (result == ports[i]) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 期望特定返回码
	 *
	 * @param statusCode
	 * @throws IOException
	 */
	protected void requestManyExpectStatus(int statusCode) throws IOException {

		String url = "http://" + requestIp + ":" + requestPort + "/";

		for (int i = 0; i < testCount; i++) {
			Assert.assertTrue(requestExpectStatus(url, statusCode));
		}
	}

	private void requestExpectNormal() throws IOException {

		String url = "http://" + requestIp + ":" + requestPort + "/";

		for (int i = 0; i < testCount; i++) {

			String result = callWithResult(url);
			if (logger.isDebugEnabled()) {
				logger.debug("[requestExpectNormal]" + result);
			}
			Assert.assertTrue(resultNormal(Integer.parseInt(result.trim())));
		}

	}

	private void putUpstreamDegradeNormal() throws ClientProtocolException, IOException {
		putUpstreamServers(0, ports.length / 2, unitTestUpstreamName);
	}

	private void putUpstreamDegradeBackup() throws ClientProtocolException, IOException {
		putUpstreamServers(ports.length / 2, ports.length, getBackUpUpstreamName(unitTestUpstreamName));
	}

	private void startNormal() throws IOException {
		startServers(0, ports.length / 2);
	}

	private void shutdownNormal() throws IOException {

		shutdownServers(0, ports.length / 2);
	}

	private void startServers(int start, int end) throws IOException {

		for (int i = start; i < end; i++) {
			startServer(ports[i], new EchoPortAction(ports[i]));
		}
	}

	private void shutdownServers(int start, int end) throws IOException {

		for (int i = start; i < end; i++) {
			stopServer(ports[i]);
		}
	}

	private String getBackUpUpstreamName(String unitTestUpstreamName) {

		return unitTestUpstreamName + "#BACKUP";
	}

	private void putUpstreamServers(int start, int end, String unitTestUpstreamName)
			throws ClientProtocolException, IOException {

		StringBuilder sb = new StringBuilder();
		for (int i = start; i < end; i++) {
			sb.append("server " + localAddress + ":" + ports[i] + ";");
		}
		sb.append(" check type=tcp interval=100 default_down=false;");
		sb.append(" upstream_degrade_rate " + degradeRate + ";");
		putUpstream(unitTestUpstreamName, sb.toString());
	}

	@After
	public void afterDegradeTest() throws IOException, InterruptedException {
		for (ServerSocket ss : servers.values()) {
			ss.close();
		}
		deleteUpstream(unitTestUpstreamName);
		deleteUpstream(getBackUpUpstreamName(unitTestUpstreamName));
	}

	/**
	 * 输出端口
	 *
	 * @author mengwenchao
	 *         <p/>
	 *         2014年9月10日 下午2:00:42
	 */
	public static class EchoPortAction extends AbstractSocketAction implements SocketAction {

		protected final Logger logger = LoggerFactory.getLogger(getClass());
		private int port;

		public EchoPortAction(int port) {

			this.port = port;
		}

		@Override
		public int read(InputStream ins) throws IOException {
			return 0;
		}

		@Override
		public void write(OutputStream ous) throws IOException {

			ous.write(getHttpBytes(String.valueOf(port)));
		}

		@Override
		public boolean isOnce() {
			return true;
		}
	}
}
