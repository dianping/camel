package com.dianping.phoenix.nginx;

import com.dianping.phoenix.lb.monitor.DegradeStatus;
import org.apache.http.client.ClientProtocolException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 测试在dyups参与的情况下，功能的正确性
 *
 * @author mengwenchao
 *         <p/>
 *         2014年9月9日 下午2:45:26
 */
public class DegradeDyupsTest extends AbstractDegradeTest {

	private static final int testCount = 500;

	@Test
	public void testAddCheck() throws ClientProtocolException, IOException, InterruptedException {
		addCheck(true);
	}

	@Test
	public void testAddUnCheck() throws ClientProtocolException, IOException, InterruptedException {
		addCheck(false);
	}

	@Test
	public void testRemove() throws ClientProtocolException, IOException, InterruptedException {

		getDegradeUpstreamsCount();

		List<String> added = addUpstreams(testCount,
				"server 127.0.0.1:1111; server 127.0.0.1:2222; check type=tcp interval=100;");
		TimeUnit.SECONDS.sleep(5);
		Map<String, Integer> initSize = getDegradeUpstreamsCount();

		deleteUpstreams(added);
		TimeUnit.SECONDS.sleep(5);

		Map<String, Integer> newSize = getDegradeUpstreamsCount();

		int expected = initSize.get("") == null ? 0 : initSize.get("").intValue();
		Assert.assertEquals(expected + testCount, newSize.get("").intValue());

	}

	@Test
	public void testRemoveAdd() throws ClientProtocolException, IOException, InterruptedException {

		List<String> added = null;
		try {
			added = addUpstreams(testCount,
					"server 127.0.0.1:1111; server 127.0.0.1:2222; check type=tcp interval=100;");
			TimeUnit.SECONDS.sleep(5);
			int initSize = getDegradeUpstreamsTotalCount();

			deleteUpstreams(added);
			//占据原来的位置，总量不变
			added = addUpstreams(testCount,
					"server 127.0.0.1:4444; server 127.0.0.1:3333; check type=tcp interval=100;");

			TimeUnit.SECONDS.sleep(5);
			int newSize = getDegradeUpstreamsTotalCount();
			Assert.assertEquals(initSize, newSize);
		} finally {
			deleteUpstreams(added);
		}
	}

	@Test
	public void testUpdate() throws ClientProtocolException, IOException, InterruptedException {
		List<String> added = null;
		try {
			added = addUpstreams(testCount,
					"server 127.0.0.1:1111; server 127.0.0.1:2222; check type=tcp interval=100;");
			TimeUnit.SECONDS.sleep(5);
			Map<String, DegradeStatus> upstreams = getDegradeUpstreams();
			for (String add : added) {
				DegradeStatus ds = upstreams.get(add);
				Assert.assertNotNull(ds);
				Assert.assertEquals(2, ds.getServerCount());
			}

			//更新upstream数目为1
			updateUpstreams(added, "server 127.0.0.1:1111; check type=tcp interval=100;");
			TimeUnit.SECONDS.sleep(5);
			upstreams = getDegradeUpstreams();
			for (String add : added) {
				DegradeStatus ds = upstreams.get(add);
				Assert.assertNotNull(ds);
				Assert.assertEquals(1, ds.getServerCount());
			}
		} finally {
			deleteUpstreams(added);
		}
	}

	private void updateUpstreams(List<String> added, String detail) throws ClientProtocolException, IOException {

		for (String add : added) {
			putUpstream(add, detail);
		}

	}

	public void addCheck(boolean checked) throws ClientProtocolException, IOException, InterruptedException {

		String detail = "server 127.0.0.1:1111; server 127.0.0.1:2222; check type=tcp interval=100;";
		if (!checked) {
			detail = "server 127.0.0.1:1111; server 127.0.0.1:2222;";
		}

		List<String> added = null;
		try {
			added = addUpstreams(testCount, detail);
			TimeUnit.SECONDS.sleep(5);
			Map<String, DegradeStatus> upstreams = getDegradeUpstreams();

			for (String add : added) {
				DegradeStatus ds = upstreams.get(add);
				Assert.assertNotNull(ds);
				Assert.assertEquals(checked, ds.isChecked());
			}
		} finally {
			deleteUpstreams(added);
		}
	}
}
