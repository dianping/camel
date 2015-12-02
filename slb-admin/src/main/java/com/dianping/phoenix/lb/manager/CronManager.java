package com.dianping.phoenix.lb.manager;

import com.dianping.phoenix.lb.dao.ModelStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务管理
 *
 * @author mengwenchao
 *         <p/>
 *         2014年11月25日 上午11:18:35
 */
@Component
public class CronManager {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private ModelStore modelStore;

	/**
	 * 保存最近多少天的数据记录
	 */
	@Value("${documents.cleaner.keepsize}")
	private int keepDocuments = 80;

	@Scheduled(cron = "${documents.cleaner.cron}")
	public void dataCleaner() {
		if (logger.isInfoEnabled()) {
			logger.info("[startDataCleaner][keep Documents]" + keepDocuments);
		}
		try {
			modelStore.cleanVirtualServerHistory(keepDocuments);
		} catch (Exception e) {
			logger.error("[dataCleaner]", e);
		}
	}

}
