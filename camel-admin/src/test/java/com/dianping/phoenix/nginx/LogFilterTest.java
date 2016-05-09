package com.dianping.phoenix.nginx;

import com.dianping.phoenix.TestConfig;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.unidal.tuple.Pair;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/*
 *  本test case只能在本地测试
 *  请先对本地nginx配置进行如下更改：
 *    1.将localtest.conf加入nginx配置中
 *    2.执行每个test case之前，按照说明将配置文件中对应部分解除注释，将其它test case对应部分注释掉
 */
public class LogFilterTest extends AbstractNginxTest {

	private static final File LOG_FILE = new File(TestConfig.getLogPath());

	private static final String ADDRESS = TestConfig.getLogLocalAddress();

	private static final String[] TEST_PATHS = TestConfig.getLogTestPaths();

	private static final DateFormat LOG_DATA_FORMAT = new SimpleDateFormat("HH:mm:ss");

	private static final int COUNT = 1000;

	private Map<Integer, Integer> m_tmpResult;

	private void addCount(Map<Integer, Integer> tmpResult, Integer statusCode) {
		Integer count = tmpResult.get(statusCode);

		if (count == null) {
			tmpResult.put(statusCode, 1);
		} else {
			tmpResult.put(statusCode, count + 1);
		}
	}

	@After
	public void cleanUpReultMap() {
		m_tmpResult = null;
	}

	@Before
	public void initResultMap() {
		m_tmpResult = new HashMap<Integer, Integer>();
	}

	private Pair<Long, Integer> parseLine(String line) throws ParseException {
		try {
			String[] parts = line.split("\\|\\|");
			String dateStr = parts[0].split("\\+")[0].split("T")[1];
			long time = LOG_DATA_FORMAT.parse(dateStr).getTime();
			int statusCode = Integer.parseInt(parts[1].trim());

			return new Pair<Long, Integer>(time, statusCode);
		} catch (ParseException e) {
			logger.error("[error]parse data error: " + line);
			throw e;
		}
	}

	protected void process()
			throws ClientProtocolException, IOException, InterruptedException, FileNotFoundException, ParseException {
		long currentMills = System.currentTimeMillis() % (24 * 60 * 60 * 1000);

		Thread.sleep(1000);
		triggerVisit();
		Thread.sleep(1000);

		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(LOG_FILE)));

		try {
			String line;

			while ((line = reader.readLine()) != null) {
				Pair<Long, Integer> args = parseLine(line);

				if (args.getKey() > currentMills) {
					addCount(m_tmpResult, args.getValue());
				}
			}
		} finally {
			reader.close();
		}
	}

	@Test
	public void testAndExpression()
			throws ClientProtocolException, FileNotFoundException, IOException, InterruptedException, ParseException {
		process();
		Assert.assertNull(m_tmpResult.get(500));
		Assert.assertEquals(COUNT, m_tmpResult.get(201).intValue());
		Assert.assertEquals(COUNT, m_tmpResult.get(304).intValue());
		Assert.assertEquals(COUNT, m_tmpResult.get(404).intValue());
	}

	@Test
	public void testEqualExpression()
			throws ClientProtocolException, FileNotFoundException, IOException, InterruptedException, ParseException {
		process();
		Assert.assertNull(m_tmpResult.get(201));
		Assert.assertEquals(COUNT, m_tmpResult.get(304).intValue());
		Assert.assertEquals(COUNT, m_tmpResult.get(404).intValue());
		Assert.assertEquals(COUNT, m_tmpResult.get(500).intValue());
	}

	@Test
	public void testNotEqualExpression()
			throws ClientProtocolException, FileNotFoundException, IOException, InterruptedException, ParseException {
		process();
		Assert.assertEquals(COUNT, m_tmpResult.get(201).intValue());
		Assert.assertNull(m_tmpResult.get(304));
		Assert.assertNull(m_tmpResult.get(404));
		Assert.assertNull(m_tmpResult.get(500));
	}

	private void triggerVisit() throws ClientProtocolException, IOException {
		for (String path : TEST_PATHS) {
			String uri = "http://" + ADDRESS + "/" + path;

			for (int i = 0; i < COUNT; i++) {
				HttpGet get = new HttpGet(uri);

				try {
					createHttpClient().execute(get);
				} finally {
					get.releaseConnection();
				}
			}
		}
	}

}
