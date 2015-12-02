package com.dianping.phoenix.lb.dao.mongo;

import com.dianping.phoenix.lb.AbstractSpringTest;
import com.dianping.phoenix.lb.api.dao.AutoIncrementIdGenerator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年11月13日 下午2:31:24
 */
public class MongoAutoIncrementIdGeneratorTest extends AbstractSpringTest {

	private AutoIncrementIdGenerator idGenerator;

	private int concurrent = 10;

	private int count = 1000;

	private String KEY = "UNIT_TEST_KEY";

	@Before
	public void before() {
		idGenerator = (AutoIncrementIdGenerator) getApplicationContext().getBean("mongoAutoIncrementIdGenerator");
		idGenerator.clear(KEY);
	}

	@Test
	public void testID() throws InterruptedException {

		for (int i = 0; i < concurrent; i++) {
			executors.execute(new Runnable() {
				@Override
				public void run() {
					for (int i = 0; i < count; i++) {
						idGenerator.getNextId(KEY);
					}
				}
			});
		}

		executors.shutdown();
		executors.awaitTermination(60, TimeUnit.SECONDS);

		long id = idGenerator.getNextId(KEY);
		Assert.assertEquals(concurrent * count + 1, id);
	}

}
