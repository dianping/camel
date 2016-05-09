package com.dianping.phoenix.lb.monitor.nginx.log.statistics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Service
public class TaskScheduler {

	private static final int MAX_RETRY_COUNT = 3;
	protected final Logger logger = LoggerFactory.getLogger(getClass());
	@Autowired
	private ApplicationContext m_applicationContext;
	private Map<String, StatisticsCounter> m_counters;

	@PostConstruct
	public void initCounters() {
		m_counters = m_applicationContext.getBeansOfType(StatisticsCounter.class);
	}

	@Scheduled(cron = "0 5 * * * ?")
	public void executeHourlyTasks() {
		for (StatisticsCounter counter : m_counters.values()) {
			int actualCount = 0;

			while (!counter.executeHourlyTask()) {
				actualCount++;
				if (actualCount >= MAX_RETRY_COUNT) {
					logger.error("execute hourly task failed! Counter name: " + counter.getCounterName());
					break;
				}
			}
		}
	}

}