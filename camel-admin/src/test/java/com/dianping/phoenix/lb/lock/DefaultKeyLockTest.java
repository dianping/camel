package com.dianping.phoenix.lb.lock;

import com.dianping.phoenix.lb.api.lock.KeyLock;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class DefaultKeyLockTest {

	private KeyLock locks = new DefaultKeyLock();

	private String lock1 = "lock", lock2 = "lock", lock3 = "lock3";

	private boolean l1, l2, l3;

	@Test
	public void testLock() throws InterruptedException {

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					locks.lock(lock1);
					l1 = true;
					TimeUnit.SECONDS.sleep(2);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					locks.unlock(lock1);
				}
			}

		}).start();

		sleepSmallTime();
		Assert.assertTrue(l1);

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					locks.lock(lock2);
					l2 = true;
				} finally {
					locks.unlock(lock2);
				}
			}

		}).start();
		sleepSmallTime();
		Assert.assertFalse(l2);

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					locks.lock(lock3);
					l3 = true;
				} finally {
					locks.unlock(lock3);
				}
			}

		}).start();

		sleepSmallTime();
		Assert.assertTrue(l3);

		TimeUnit.SECONDS.sleep(2);
		//l2 become true
		Assert.assertTrue(l2);

	}

	private void sleepSmallTime() {
		try {
			TimeUnit.MILLISECONDS.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
