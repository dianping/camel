package com.dianping.phoenix.lb.lock;

import com.dianping.phoenix.lb.api.lock.KeyLock;
import com.dianping.phoenix.lb.api.lock.LockManager;

/**
 * 集群环境下分布式锁
 *
 * @author mengwenchao
 *         <p/>
 *         2014年11月13日 上午10:47:18
 */
public class ClusterLockManager implements LockManager {

	/* (non-Javadoc)
	 * @see com.dianping.phoenix.lb.api.lock.LockManager#getConcurrentLock()
	 */
	@Override
	public KeyLock getConcurrentLock() {
		// TODO Auto-generated method stub
		return null;
	}

}
