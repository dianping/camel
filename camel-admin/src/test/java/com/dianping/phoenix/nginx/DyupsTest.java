package com.dianping.phoenix.nginx;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dyups测试
 *
 * @author mengwenchao
 *         <p/>
 *         2014年9月9日 下午2:55:16
 */
public class DyupsTest extends AbstractDyupsTest {

	public int testCount = 2000;

	@Test
	public void testSequennceUpdate() {

		String upstreamName = "testConcurrentUpdate" + UUID.randomUUID();
		String realData = "server 127.0.0.1:80 max_fails=3 weight=1 fail_timeout=5s;server 127.0.0.1:80 max_fails=3 weight=1 fail_timeout=2s;server 10.101.0.51:8080 max_fails=3 weight=100 fail_timeout=2s;server 10.101.2.52:8080 max_fails=3 weight=100 fail_timeout=2s; check interval=1000 fall=3 rise=2 timeout=500 default_down=false type=http; check_http_send 'GET /inspect/healthcheck HTTP/1.0\r\n\r\n'; check_http_expect_alive http_2xx;";
		int realServerSize = 4;

		for (int i = 0; i < testCount; i++) {
			try {
				putUpstream(upstreamName, realData);
				int count = getServerCount(getUpstream(upstreamName));
				if (count != realServerSize) {
				}
				deleteUpstream(upstreamName);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				System.out.println("finish...");
			}
		}
	}

	@Test
	public void testConcurrentUpdate() throws ClientProtocolException, IOException, InterruptedException {

		final AtomicBoolean result = new AtomicBoolean(true);
		final AtomicReference<String> reason = new AtomicReference<String>();
		final CountDownLatch latch = new CountDownLatch(testCount);

		for (int i = 0; i < testCount; i++) {
			executors.execute(new Runnable() {

				@Override
				public void run() {

					String upstreamName = "testConcurrentUpdate" + UUID.randomUUID();
					String realData = "server 127.0.0.1:80 max_fails=3 weight=1 fail_timeout=5s;server 127.0.0.1:80 max_fails=3 weight=1 fail_timeout=2s;server 10.101.0.51:8080 max_fails=3 weight=100 fail_timeout=2s;server 10.101.2.52:8080 max_fails=3 weight=100 fail_timeout=2s; check interval=1000 fall=3 rise=2 timeout=500 default_down=false type=http; check_http_send 'GET /inspect/healthcheck HTTP/1.0\r\n\r\n'; check_http_expect_alive http_2xx;";
					int realServerSize = 4;
					try {
						putUpstream(upstreamName, realData);
						int count = getServerCount(getUpstream(upstreamName));
						if (count != realServerSize) {
							result.set(false);
							reason.set("server count wrong!! not " + realServerSize);
						}
						deleteUpstream(upstreamName);
					} catch (Exception e) {
						logger.error("[exception]", e);
						e.printStackTrace();
						result.set(false);
						reason.set(e.getMessage());
					} finally {
						latch.countDown();
					}
				}
			});
		}

		Assert.assertTrue(latch.await(300, TimeUnit.SECONDS));
		if (!result.get()) {
			System.out.println(reason.get());
		}
		Assert.assertTrue(result.get());
	}

	@Test
	public void testAddDelete() throws ClientProtocolException, IOException {

		String upstreamName = "testAddDelete" + UUID.randomUUID();
		Assert.assertEquals(null, getUpstream(upstreamName));

		putUpstream(upstreamName, " server 127.0.0.1 ;");
		Assert.assertEquals(1, getServerCount(getUpstream(upstreamName)));

		deleteUpstream(upstreamName);
		Assert.assertEquals(null, getUpstream(upstreamName));
	}

	@Test
	public void testGet() throws ClientProtocolException, IOException {

		String getDetail = dyupsAddress + "/detail";
		String getList = dyupsAddress + "/list";

		HttpGet get = new HttpGet(getList);
		executeRequest(get);

		get = new HttpGet(getDetail);
		executeRequest(get);
	}

	private int getServerCount(String upstream) {

		Pattern p = Pattern.compile("server");
		Matcher m = p.matcher(upstream);
		int count = 0;
		while (m.find()) {
			count++;
		}
		return count;
	}

}
