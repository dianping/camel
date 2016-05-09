package com.dianping.phoenix.lb.lock;

import com.dianping.phoenix.lb.api.lock.KeyLock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Map 存储lock
 *
 * @author mengwenchao
 *         <p/>
 *         2014年8月18日 下午5:47:48
 */
public class DefaultKeyLock implements KeyLock {

	private Map<String, ReadWriteLock> locks;

	public DefaultKeyLock() {

		locks = new ConcurrentHashMap<String, ReadWriteLock>();
	}

	@Override
	public void lock(String name) {

		ReadWriteLock lock = getLock(name);

		lock.writeLock().lock();

	}

	private ReadWriteLock getLock(String name) {

		synchronized (this) {
			ReadWriteLock lock = locks.get(name);

			if (lock == null) {
				lock = new ReentrantReadWriteLock();
				locks.put(name, lock);
			}
			return lock;
		}
	}

	@Override
	public void unlock(String name) {

		locks.get(name).writeLock().unlock();
	}

	@Override
	public void readLock(String name) {

		ReadWriteLock lock = getLock(name);

		lock.readLock().lock();

	}

	@Override
	public void readUnLock(String name) {

		locks.get(name).readLock().unlock();
	}

}
