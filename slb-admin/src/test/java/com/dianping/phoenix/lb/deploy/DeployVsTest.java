package com.dianping.phoenix.lb.deploy;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 测试发布
 *
 * @author mengwenchao
 *         <p/>
 *         2014年8月11日 下午6:11:12
 */
public class DeployVsTest extends AbstractDeployTest {

	private int deployCount = 10;

	@Before
	public void before() throws IOException {

	}

	@Test
	public void testDeployVs() throws Exception {

		Assert.assertTrue(deployRandomVs());
	}

	@Test
	public void testSequenceDeployVs() throws Exception {

		for (int i = 0; i < deployCount / 2; i++) {
			Assert.assertTrue(deployRandomVs());
		}
	}

	@Test
	public void testConcurrentDeployVs() throws InterruptedException {

		ExecutorService executors = Executors.newCachedThreadPool();

		final List<ResultWrapper> results = new LinkedList<ResultWrapper>();
		final CountDownLatch latch = new CountDownLatch(deployCount);

		for (int i = 0; i < deployCount; i++) {
			executors.execute(new Runnable() {

				@Override
				public void run() {
					boolean result = false;
					String message = "";
					try {
						result = deployRandomVs();
						if (!result) {
							message = "result not equal to expected";
						}
					} catch (Exception e) {
						result = false;
						message = e.getMessage();
						logger.error("[testConcurrentDeployVs]", e);
					} finally {
						latch.countDown();
						results.add(new ResultWrapper(result, message));
					}

				}
			});
		}

		latch.await(5 * 30, TimeUnit.SECONDS);

		for (ResultWrapper result : results) {

			Assert.assertTrue(result.isSuccessed());
		}
	}

	@After
	public void after() throws IOException {

	}
}
