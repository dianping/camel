package com.dianping.phoenix.lb.deploy.executor;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年12月26日 下午4:33:27
 */
public interface TaskExecutorListener {

	void onBegin(long taskId);

	void onFinish(long taskId);

	void onException(long taskId, Throwable th);
}
