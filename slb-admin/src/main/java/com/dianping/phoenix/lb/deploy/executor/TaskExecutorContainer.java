package com.dianping.phoenix.lb.deploy.executor;

import com.dianping.phoenix.lb.exception.BizException;

public interface TaskExecutorContainer<T> {

	/**
	 * 获取一个已存在的TaskExecutor
	 */
	TaskExecutor<T> getTaskExecutor(long taskId);

	void delTaskExecutor(long taskId);

	/**
	 * 创建一个TaskExecutor，并且返回
	 *
	 * @throws BizException
	 */
	TaskExecutor<T> submitTask(long taskId) throws BizException;

	/**
	 * 创建一个TaskExecutor，并且返回
	 */
	TaskExecutor<T> submitTask(T deployTaskBo);
}
