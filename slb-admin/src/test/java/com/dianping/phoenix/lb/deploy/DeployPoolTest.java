package com.dianping.phoenix.lb.deploy;

import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 测试发布
 *
 * @author mengwenchao
 *         <p/>
 *         2014年8月11日 下午6:11:12
 */
public class DeployPoolTest extends AbstractDeployTest {

	private int concurrentCount = 20;

	@Test
	public void testDeployPool() throws Exception {

		Assert.assertTrue(deployRandomPool(3));

	}

	/**
	 * 希望refresh data
	 *
	 * @return
	 * @throws Exception
	 */
	@Test
	public void deployRandomRefresh() throws Exception {

		int vsCount = 3;

		String poolName = UUID.randomUUID().toString();
		String[] vsNames = new String[vsCount];

		for (int i = 0; i < vsCount; i++) {
			vsNames[i] = UUID.randomUUID().toString();
		}
		;

		try {
			addPool(poolName);

			for (String vsName : vsNames) {
				addVs(vsName, vsName, poolName);
			}

			int count = getDyupsCallCount();

			deplopyPool(poolName);

			//没有调用接口
			Assert.assertEquals(count, getDyupsCallCount());

			for (String vsName : vsNames) {
				String result = getResponseData("http://" + vsName).trim();

				Assert.assertEquals(vsName, result);
			}

			//refresh
			String ip = "192.168.218.22";
			String realPoolName = vsNames[0] + "." + poolName;
			//add Member
			Assert.assertTrue(addMember(poolName, ip));

			Assert.assertFalse(checkInPool(realPoolName, ip));

			count = getDyupsCallCount();

			deplopyPool(poolName);

			//检查已经调用了refresh接口
			//每个vs调用一次
			Assert.assertEquals(count + 2 * vsCount, getDyupsCallCount());

			Assert.assertTrue(checkInPool(realPoolName, ip));

		} finally {
			for (String vsName : vsNames) {
				deleteVs(vsName);
			}
			deletePool(poolName);
		}

	}

	protected boolean deployRandomPool(int vsCount) throws Exception {

		String poolName = UUID.randomUUID().toString();
		String[] vsNames = new String[vsCount];

		for (int i = 0; i < vsCount; i++) {
			vsNames[i] = UUID.randomUUID().toString();
		}
		;

		try {
			addPool(poolName);

			for (String vsName : vsNames) {
				addVs(vsName, vsName, poolName);
			}

			deplopyPool(poolName);

			for (String vsName : vsNames) {
				String result = getResponseData("http://" + vsName).trim();

				if (!vsName.equals(result)) {
					logger.error("[testRandomPool][result != expected]" + result + " VS " + vsName);
					return false;
				}
			}
		} finally {
			for (String vsName : vsNames) {
				deleteVs(vsName);
			}
			deletePool(poolName);
		}
		return true;
	}

	@Test
	public void testConcurrentDeployPoolAndVs() throws InterruptedException {

		ExecutorService executors = Executors.newCachedThreadPool();

		final CountDownLatch latch = new CountDownLatch(concurrentCount);
		final AtomicBoolean result = new AtomicBoolean(true);

		for (int i = 0; i < concurrentCount; i++) {

			if ((i & 1) == 0) {
				executors.execute(new Runnable() {

					@Override
					public void run() {
						try {
							if (!deployRandomPool(3)) {
								result.set(false);
							}
						} catch (Exception e) {
							result.set(false);
							logger.error("[run][deployRandomPool]", e);
						} finally {
							latch.countDown();
						}
					}

				});
			} else {
				executors.execute(new Runnable() {
					@Override
					public void run() {
						try {
							if (!deployRandomVs()) {
								result.set(false);
							}
						} catch (Exception e) {
							result.set(false);
							logger.error("[run][deployRandomVs]", e);
						} finally {
							latch.countDown();
						}
					}

				});
			}
		}

		latch.await(5 * 60, TimeUnit.SECONDS);
		Assert.assertTrue(result.get());
	}

}
