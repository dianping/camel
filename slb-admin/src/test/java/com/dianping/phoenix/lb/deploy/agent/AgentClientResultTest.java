package com.dianping.phoenix.lb.deploy.agent;

import org.junit.Assert;
import org.junit.Test;

import java.util.ConcurrentModificationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AgentClientResultTest {

	ExecutorService executors = Executors.newCachedThreadPool();

	@Test
	public void testConcurrentModification() throws InterruptedException {

		final AgentClientResult result = new AgentClientResult();
		final AtomicBoolean assertResult = new AtomicBoolean(true);
		final CountDownLatch latch = new CountDownLatch(2);
		executors.execute(new Runnable() {

			@Override
			public void run() {
				try {
					for (int i = 0; i < 1000; i++) {
						read(result);
					}
				} catch (ConcurrentModificationException e) {
					e.printStackTrace();
					assertResult.set(false);
				} finally {
					latch.countDown();
				}
			}

		});

		executors.execute(new Runnable() {
			@Override
			public void run() {
				try {
					for (int i = 0; i < 1000; i++) {
						write(result);
					}
				} finally {
					latch.countDown();
				}
			}
		});

		latch.await();
		Assert.assertTrue(assertResult.get());

	}

	private void read(AgentClientResult result) {
		for (String line : result.getLogs()) {
			//nothong to do
			line = line + "";
		}
	}

	private void write(AgentClientResult result) {

		result.addRawLog("line ...");

	}
}
