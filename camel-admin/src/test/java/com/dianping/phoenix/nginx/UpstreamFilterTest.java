package com.dianping.phoenix.nginx;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

/**
 * 测试upstream filter功能
 *
 * @author mengwenchao
 *         <p/>
 *         2014年10月16日 下午3:03:19
 */
public class UpstreamFilterTest extends AbstractDyupsTest {

	private final int testCount = 1000;

	private String random = UUID.randomUUID().toString();

	private String unPassedUrl = "/unpass";

	private String passKey = "abc";

	private String unPassKey = "123";

	private String exceptionKey = "123exception";

	private String exceptionCloseKey = "123close";

	private String unpassHeaderKey = "X-DENGINE-UPSTREAM-FILTER-UNPASS";

	private boolean exceptionPassOn = true;

	@Before
	public void beforeUpstreamFilterTest() throws ClientProtocolException, IOException {

		int port = startRandomServer(random);
		String upstream = " 	server " + localAddress + ":" + port + ";" + " auth_filter_open on;"
				+ " auth_filter_exception_pass " + (exceptionPassOn ? "on" : "off") + ";"
				+ " auth_filter_pass_pattern 12345;" + " keepalive 10;";
		putUpstream(unitTestUpstreamName, upstream);
	}

	@Test
	public void testPassPatternPass() throws IOException {

		for (int i = 0; i < testCount; i++) {

			String url = "http://" + requestIp + ":" + requestPort + "/a.jpg";
			String result = callWithResult(url);
			Assert.assertEquals(random, result);

			url = "http://" + requestIp + ":" + requestPort + "/12345";
			result = callWithResult(url);
			Assert.assertEquals(random, result);
		}

	}

	@Test
	public void testNoAuthorization() throws IOException {

		for (int i = 0; i < testCount; i++) {
			String url = "http://" + requestIp + ":" + requestPort + unPassedUrl;

			HttpGet get = new HttpGet(url);
			HttpResponse response = executeAndGetResponse(get);
			Assert.assertEquals(401, response.getStatusLine().getStatusCode());
			Assert.assertNotNull(response.getFirstHeader(unpassHeaderKey));
		}

	}

	@Test
	public void testAuthorizationNotPass() throws ClientProtocolException, IOException {

		for (int i = 0; i < testCount; i++) {
			//expect 401
			String url = "http://" + requestIp + ":" + requestPort + unPassedUrl;
			HttpGet get = new HttpGet(url);
			get.addHeader("authorization", random() + unPassKey);
			HttpResponse response = executeAndGetResponse(get);
			Assert.assertEquals(401, response.getStatusLine().getStatusCode());
			Assert.assertNotNull(response.getFirstHeader(unpassHeaderKey));
		}
	}

	private String random() {
		String uuid = UUID.randomUUID().toString();
		if (logger.isInfoEnabled()) {
			logger.info("[random]" + uuid);
		}
		return uuid;
	}

	@Test
	public void testAuthorizationPass() throws ClientProtocolException, IOException {

		for (int i = 0; i < testCount; i++) {
			String url = "http://" + requestIp + ":" + requestPort + unPassedUrl;
			HttpGet get = new HttpGet(url);
			get.addHeader("authorization", random() + passKey);
			HttpResponse response = executeAndGetResponse(get);
			Assert.assertEquals(200, response.getStatusLine().getStatusCode());
			Assert.assertEquals(random, getResponseBody(response));
		}

	}

	@Test
	public void testAuthorizationExceptionWrongResult() throws ClientProtocolException, IOException {

		for (int i = 0; i < testCount; i++) {
			String url = "http://" + requestIp + ":" + requestPort + unPassedUrl;
			HttpGet get = new HttpGet(url);
			get.addHeader("authorization", random() + exceptionKey);
			HttpResponse response = executeAndGetResponse(get);
			if (exceptionPassOn) {
				Assert.assertEquals(200, response.getStatusLine().getStatusCode());
				Assert.assertEquals(random, getResponseBody(response));
			} else {
				Assert.assertEquals(401, response.getStatusLine().getStatusCode());
				Assert.assertEquals(random, getResponseBody(response));
			}
		}
	}

	@Test
	public void testAuthorizationExceptionConnectFail() throws ClientProtocolException, IOException {

		for (int i = 0; i < testCount; i++) {
			String url = "http://" + requestIp + ":" + requestPort + unPassedUrl;
			HttpGet get = new HttpGet(url);
			get.addHeader("authorization", random() + exceptionCloseKey);
			HttpResponse response = executeAndGetResponse(get);
			if (exceptionPassOn) {
				Assert.assertEquals(200, response.getStatusLine().getStatusCode());
				Assert.assertEquals(random, getResponseBody(response));
			} else {
				Assert.assertEquals(401, response.getStatusLine().getStatusCode());
				Assert.assertEquals(random, getResponseBody(response));
			}
		}
	}

	//	@Test
	public void testDyups() throws IOException {

		int port = startRandomServer(random);
		String upstream = " 	server " + localAddress + ":" + port + ";" + " auth_filter_open on;"
				+ " auth_filter_config  oauth \".*\" authorization \"https://sso.51ping.com/oauth2.0/profile?access_token=%s\";"
				+ " auth_filter_exception_pass " + (exceptionPassOn ? "on" : "off") + ";"
				+ " auth_filter_pass_pattern 12345;" + " keepalive 10;";
		putUpstream(unitTestUpstreamName, upstream);
		testAuthorizationNotPass();
	}

}
