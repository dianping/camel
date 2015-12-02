package com.dianping.phoenix.lb.manager;

import com.dianping.phoenix.lb.api.manager.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年9月5日 下午7:13:41
 */
@Configuration
public class ThreadPoolManagerImpl implements ThreadPoolManager {

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	protected int cpuCount = Runtime.getRuntime().availableProcessors();

	@Bean(name = "globalThreadPool")
	public ExecutorService getGlobalThreadPool() {
		if (logger.isInfoEnabled()) {
			logger.info("[getGlobalThreadPool]");
		}
		return createThreadPool("Global_Thread_Pool");
	}

	@Bean(name = "deployThreadPool")
	public ExecutorService getDeployThreadPool() {
		if (logger.isInfoEnabled()) {
			logger.info("[getDeployThreadPool]");
		}
		return createThreadPool("Deploy_Thread_Pool");
	}

	@Bean(name = "scheduledThreadPool")
	public ScheduledExecutorService getScheduledExecutorService() {

		return Executors.newScheduledThreadPool(cpuCount, createNamedThreadFactory("Scheduled_Thread_Pool"));
	}

	private ThreadFactory createNamedThreadFactory(final String threadPoolName) {

		return new ThreadFactory() {

			AtomicInteger threadCount = new AtomicInteger();

			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setName(threadPoolName + "-" + threadCount.incrementAndGet());
				return t;
			}
		};
	}

	private ExecutorService createThreadPool(final String threadPoolName) {

		ExecutorService executors = new ThreadPoolExecutor(2 * cpuCount, 50 * cpuCount, 300, TimeUnit.SECONDS,
				new SynchronousQueue<Runnable>(), createNamedThreadFactory(threadPoolName),
				new ThreadPoolExecutor.CallerRunsPolicy());
		return executors;
	}
}
