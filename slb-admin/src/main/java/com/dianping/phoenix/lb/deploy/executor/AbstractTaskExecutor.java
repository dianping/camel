package com.dianping.phoenix.lb.deploy.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 抽象类
 *
 * @author mengwenchao
 *         <p/>
 *         2014年8月13日 下午2:21:25
 */
public abstract class AbstractTaskExecutor<T> implements TaskExecutor<T> {

	protected static final String pattern = "yyyy-MM-dd HH:mm:ss";
	protected final Logger logger = LoggerFactory.getLogger(getClass());
	protected Thread taskThread;

	@Override
	public void join() throws InterruptedException {
		if (taskThread != null) {
			taskThread.join();
		}
	}

}
