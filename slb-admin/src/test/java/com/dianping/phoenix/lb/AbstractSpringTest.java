package com.dianping.phoenix.lb;

import com.dianping.phoenix.AbstractTest;
import com.dianping.phoenix.lb.dao.mongo.configimport.ImportTestConfig;
import org.junit.Before;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 全局测试抽象类
 *
 * @author mengwenchao
 *         <p/>
 *         2014年9月9日 下午3:38:59
 */
public abstract class AbstractSpringTest extends AbstractTest {

	protected static final String WWW_VS = "www.dianping.com";
	protected static final String WWW_FILENAME = "slb_www.dianping.com.xml";
	private static ApplicationContext applicationContext;

	@Before
	public void beforeAbstractSpringTest() throws Exception {

		new ImportTestConfig().importFiles();

		synchronized (AbstractSpringTest.class) {
			if (applicationContext == null) {
				try {
					applicationContext = new ClassPathXmlApplicationContext("spring/applicationContext-test.xml");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public ApplicationContext getApplicationContext() {

		return applicationContext;
	}

}
