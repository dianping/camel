package com.dianping.phoenix.lb.utils;

import com.dianping.phoenix.lb.model.entity.Pool;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年12月18日 下午10:41:56
 */
public class PoolNameUtilsTest {

	private String[] protocals = new String[] { "http", "ssl", "https" };

	@Test
	public void testExtractPoolNameFromProxyPassStringProxyPass() {

		Assert.assertEquals("destn-index-web",
				PoolNameUtils.extractPoolNameFromProxyPassString("proxy_pass http://destn-index-web ;break"));
		Assert.assertEquals("destn-index-web",
				PoolNameUtils.extractPoolNameFromProxyPassString("proxy_pass http://destn-index-web;break"));
		Assert.assertEquals("destn-index-web",
				PoolNameUtils.extractPoolNameFromProxyPassString("proxy_pass http://destn-index-web/;break"));

		for (String protocal : protocals) {

			Assert.assertEquals("www.123.456",
					PoolNameUtils.extractPoolNameFromProxyPassString("proxy_pass " + protocal + "://www.123.456"));

			Assert.assertEquals("www123456",
					PoolNameUtils.extractPoolNameFromProxyPassString("proxy_pass " + protocal + "://www123456"));

			Assert.assertEquals("www.123.456", PoolNameUtils
					.extractPoolNameFromProxyPassString("return 404;proxy_pass " + protocal + "://www.123.456"));

			Assert.assertEquals("www.123.456", PoolNameUtils.extractPoolNameFromProxyPassString(
							"return 404;proxy_pass " + protocal + "://www.123.456;break"));

			Assert.assertEquals("www.123.456", PoolNameUtils.extractPoolNameFromProxyPassString(
							"return 404;proxy_pass " + protocal + "://www.123.456;break"));
		}

		try {
			PoolNameUtils.extractPoolNameFromProxyPassString(
					"return 404;proxy_pass http://www.123.456; proxy_pass https://baidu.com;break");
			Assert.assertTrue(false);
		} catch (IllegalStateException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void testReplacePoolNameFromProxyPassStringProxyPass() {

		Map<String, Pool> pools = new HashMap<String, Pool>();
		pools.put("www", new Pool());
		pools.put("www.pool", new Pool());
		String vsName = "www.dianping.com";

		for (String protocal : protocals) {

			Assert.assertEquals("", PoolNameUtils.replacePoolNameFromProxyPassString(vsName, "", pools));

			Assert.assertEquals("break", PoolNameUtils.replacePoolNameFromProxyPassString(vsName, "break", pools));

			Assert.assertEquals("proxy_pass " + protocal + "://" + vsName + "." + "www", PoolNameUtils
					.replacePoolNameFromProxyPassString(vsName, "proxy_pass " + protocal + "://www", pools));

			Assert.assertEquals("proxy_pass " + protocal + "://" + vsName + "." + "www/", PoolNameUtils
					.replacePoolNameFromProxyPassString(vsName, "proxy_pass " + protocal + "://www/", pools));

			Assert.assertEquals("proxy_pass " + protocal + "://" + vsName + "." + "www.pool", PoolNameUtils
					.replacePoolNameFromProxyPassString(vsName, "proxy_pass " + protocal + "://www.pool", pools));

			Assert.assertEquals("break; proxy_pass " + protocal + "://" + vsName + "." + "www.pool", PoolNameUtils
					.replacePoolNameFromProxyPassString(vsName, "break; proxy_pass " + protocal + "://www.pool",
							pools));

			Assert.assertEquals(
					"proxy_pass " + protocal + "://" + vsName + "." + "www.pool; proxy_pass " + protocal + "://"
							+ vsName + "." + "www", PoolNameUtils.replacePoolNameFromProxyPassString(vsName,
							"proxy_pass " + protocal + "://www.pool; proxy_pass " + protocal + "://www", pools));

			Assert.assertEquals("break; proxy_pass " + protocal + "://" + vsName + "." + "www.pool; break",
					PoolNameUtils.replacePoolNameFromProxyPassString(vsName,
							"break; proxy_pass " + protocal + "://www.pool; break", pools));

			Assert.assertEquals("proxy_pass " + protocal + "://www.baidu.com", PoolNameUtils
					.replacePoolNameFromProxyPassString(vsName, "proxy_pass " + protocal + "://www.baidu.com", pools));

			Assert.assertEquals(
					"proxy_pass " + protocal + "://www.baidu.com; proxy_pass http://" + vsName + "." + "www",
					PoolNameUtils.replacePoolNameFromProxyPassString(vsName,
							"proxy_pass " + protocal + "://www.baidu.com; proxy_pass http://www", pools));

		}

	}

	@Test
	public void testExtractPoolNameFromProxyPassStringDpDomain() {

		Assert.assertEquals("destn-index-web",
				PoolNameUtils.extractPoolNameFromProxyPassString("dp_domain destn-index-web"));
		Assert.assertEquals("destn-index-web",
				PoolNameUtils.extractPoolNameFromProxyPassString("dp_domain destn-index-web "));
		Assert.assertEquals("destn-index-web",
				PoolNameUtils.extractPoolNameFromProxyPassString("dp_domain destn-index-web ;break"));
		Assert.assertEquals("destn-index-web",
				PoolNameUtils.extractPoolNameFromProxyPassString("dp_domain destn-index-web;break"));
		Assert.assertEquals("destn-index-web",
				PoolNameUtils.extractPoolNameFromProxyPassString("break;dp_domain destn-index-web;break"));

		try {
			Assert.assertEquals("destn-index-web", PoolNameUtils
					.extractPoolNameFromProxyPassString("break;dp_domain destn-index-web;dp_domain destn-index-web;"));
			Assert.assertTrue(false);
		} catch (IllegalStateException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void testReplacePoolNameFromProxyPassStringDpDomain() {

		Map<String, Pool> pools = new HashMap<String, Pool>();
		pools.put("www", new Pool());
		pools.put("www.pool", new Pool());
		String vsName = "www.dianping.com";

		Assert.assertEquals("dp_domain www.dianping.com.www",
				PoolNameUtils.replacePoolNameFromProxyPassString(vsName, "dp_domain www", pools));

		Assert.assertEquals("dp_domain www.dianping.com.www;break",
				PoolNameUtils.replacePoolNameFromProxyPassString(vsName, "dp_domain www;break", pools));

		Assert.assertEquals("break;dp_domain www.dianping.com.www;break",
				PoolNameUtils.replacePoolNameFromProxyPassString(vsName, "break;dp_domain www;break", pools));

		Assert.assertEquals("break;dp_domain www.baidu.com;break",
				PoolNameUtils.replacePoolNameFromProxyPassString(vsName, "break;dp_domain www.baidu.com;break", pools));

	}

}
