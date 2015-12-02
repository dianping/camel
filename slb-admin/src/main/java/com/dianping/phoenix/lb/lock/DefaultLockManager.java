package com.dianping.phoenix.lb.lock;

import com.dianping.phoenix.lb.api.lock.KeyLock;
import com.dianping.phoenix.lb.api.lock.LockManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * 单机锁
 *
 * @author mengwenchao
 *         <p/>
 *         2014年11月13日 上午10:45:03
 */
@Configuration
public class DefaultLockManager implements LockManager {

	@Override
	@Bean(name = "concurrentLock")
	@Scope(value = "prototype")
	public KeyLock getConcurrentLock() {
		return new DefaultKeyLock();
	}

}
