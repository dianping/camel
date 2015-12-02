package com.dianping.phoenix.lb.api.lock;

/**
 * 提供此接口，在分布式的环境中，方便替换为分布式的锁
 *
 * @author mengwenchao
 *         <p/>
 *         2014年11月13日 上午10:44:07
 */
public interface LockManager {

	KeyLock getConcurrentLock();

}
