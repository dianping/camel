package com.dianping.phoenix.lb.api.lock;

/**
 * lock based on key
 *
 * @author mengwenchao
 *         <p/>
 *         2014年8月18日 下午5:43:08
 */
public interface KeyLock {

	void readLock(String name);

	void readUnLock(String name);

	void lock(String name);

	void unlock(String name);
}
