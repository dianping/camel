package com.dianping.phoenix;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 全局测试抽象类
 *
 * @author mengwenchao
 *         <p/>
 *         2014年9月9日 下午3:38:59
 */
@RunWith(JUnit4.class)
public abstract class AbstractTest {

	protected static final String DEFAULT_ENCODING = "UTF-8";
	protected final Logger logger = LoggerFactory.getLogger(getClass());
	public ExecutorService executors = Executors.newCachedThreadPool();
	@Rule
	public TestName testName = new TestName();

	@Before
	public void beforeAbstractTest() {
		if (logger.isInfoEnabled()) {
			logger.info("[-----------------][begin test]" + testName.getMethodName());
		}
	}

	@After
	public void afterAbstractTest() {
		if (logger.isInfoEnabled()) {
			logger.info("[-----------------][end test]" + testName.getMethodName());
		}
	}

}
