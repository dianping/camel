package com.dianping.phoenix.lb.api.manager;

import java.util.concurrent.ExecutorService;

/**
 * 全局资源管理
 *
 * @author mengwenchao
 *         <p/>
 *         2014年9月5日 下午7:15:05
 */
public interface ThreadPoolManager {

	/**
	 * 无特别要求，各模块请使用此线程池，统一管理
	 *
	 * @param string
	 * @return
	 */
	ExecutorService getGlobalThreadPool();

}
