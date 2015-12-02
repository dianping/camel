package com.dianping.phoenix.lb.monitor.nginx.log;

import com.dianping.phoenix.lb.model.entity.StatusCode;
import com.dianping.phoenix.lb.monitor.ReqStatusContainer.DataWrapper;
import com.dianping.phoenix.lb.monitor.nginx.log.statistics.HourlyStatisticsCounter;
import com.dianping.phoenix.lb.service.model.NginxStatisticsService;
import com.dianping.phoenix.lb.service.model.StatusCodeService;
import com.dianping.phoenix.lb.utils.TimeUtil;
import com.dianping.phoenix.slb.nginx.log.entity.NginxLog;
import com.dianping.phoenix.slb.nginx.log.transform.DefaultJsonParser;
import com.dianping.phoenix.slb.nginx.statistics.hour.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.unidal.lookup.util.StringUtils;
import org.unidal.tuple.Pair;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Service
public class NginxStatusRecorder {

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	@Autowired(required = true)
	NginxStatisticsService m_nginxStatisticsService;
	@Autowired(required = true)
	HourlyStatisticsCounter m_hourlyStatisticsCounter;
	@Autowired
	private StatusCodeService m_statusCodeService;
	private NginxHourStatistics m_currentHourStatistics;
	private NginxHourStatistics m_lastHourStatistics;
	private Object lock = new Object();

	private AtomicInteger m_threshold = new AtomicInteger(0);

	private void addDefaultWrapper(Map<Integer, DataWrapper> wrappers, int length) {
		DataWrapper wrapper4 = new DataWrapper();
		DataWrapper wrapper5 = new DataWrapper();

		wrapper4.setDesc("status code: 4XX");
		wrapper4.setData(generateLongArray(length));
		wrapper4.setInterval(60 * 1000);
		wrapper4.setTotal(0);
		wrapper5.setDesc("status code: 5XX");
		wrapper5.setData(generateLongArray(length));
		wrapper5.setInterval(60 * 1000);
		wrapper5.setTotal(0);
		wrappers.put(4, wrapper4);
		wrappers.put(5, wrapper5);
	}

	public void addNginxLogs(String jsonArray) throws Exception {
		List<NginxLog> nginxLogs = DefaultJsonParser.parseArray(NginxLog.class, jsonArray);
		long currentStatusMills = m_currentHourStatistics.getHour().getTime();

		for (NginxLog nginxLog : nginxLogs) {
			long logMills = nginxLog.getTime().getTime();

			if (logMills >= currentStatusMills && logMills < currentStatusMills + TimeUtil.ONE_HOUR_MILLS) {
				m_hourlyStatisticsCounter.addLogToHourStatistics(nginxLog, m_currentHourStatistics);

				String statusCode = nginxLog.getStatus().toString();

				m_statusCodeService.addIfNull(new StatusCode(statusCode).setValue(statusCode));
			}
		}
		m_threshold.getAndIncrement();
		if (m_threshold.get() >= 100) {
			logger.info("update current hour statistics");
			synchronized (lock) {
				m_nginxStatisticsService.addOrUpdateHourlyStatistics(m_currentHourStatistics);
			}
			m_threshold.set(0);
		}
	}

	public List<Pair<String, Integer>> calCount(int minute, String statusCode, String filterPools) {
		List<Pair<String, Integer>> countByPoolName = new ArrayList<Pair<String, Integer>>();
		int alreadyMinute = TimeUtil.getMinuteInHour(System.currentTimeMillis()) - 1;
		NginxHourStatistics status = alreadyMinute < 0 ? m_lastHourStatistics : m_currentHourStatistics;

		for (Pool pool : status.getPools().values()) {
			String poolName = pool.getId();
			if ("all".equals(poolName)) {
				continue;
			}

			if (!StringUtils.isEmpty(filterPools) && filterPools.contains(poolName)) {
				continue;
			} else {
				int count = calCountByPoolName(minute, poolName, statusCode);

				countByPoolName.add(new Pair<String, Integer>(poolName, count));
			}
		}
		return countByPoolName;
	}

	public int calCountByPoolName(int minute, String poolName, String statusCode) {
		int alreadyMinute = TimeUtil.getMinuteInHour(System.currentTimeMillis()) - 1;
		int actualCount = 0;

		if (alreadyMinute < 0) {
			int lastHourStartMinute = 59 + 1 - minute;
			int lastHourEndMinute = 59;

			for (int tmpMinute = lastHourStartMinute; tmpMinute <= lastHourEndMinute; tmpMinute++) {
				actualCount += getCountByPoolNameAndMinute(tmpMinute, poolName, statusCode, m_lastHourStatistics);
			}
		} else if (alreadyMinute + 1 < minute) {
			int lastHourStartMinute = 59 + 1 - (minute - alreadyMinute - 1);
			int lastHourEndMinute = 59;

			for (int tmpMinute = lastHourStartMinute; tmpMinute <= lastHourEndMinute; tmpMinute++) {
				actualCount += getCountByPoolNameAndMinute(tmpMinute, poolName, statusCode, m_lastHourStatistics);
			}

			int currentHourStartMinute = 0;
			int currentHourEndMinute = alreadyMinute;

			for (int tmpMinute = currentHourStartMinute; tmpMinute <= currentHourEndMinute; tmpMinute++) {
				actualCount += getCountByPoolNameAndMinute(tmpMinute, poolName, statusCode, m_currentHourStatistics);
			}
		} else {
			int currentHourStartMinute = alreadyMinute + 1 - minute;
			int currentHourEndMinute = alreadyMinute;

			for (int tmpMinute = currentHourStartMinute; tmpMinute <= currentHourEndMinute; tmpMinute++) {
				actualCount += getCountByPoolNameAndMinute(tmpMinute, poolName, statusCode, m_currentHourStatistics);
			}
		}
		return actualCount;
	}

	public Collection<DataWrapper> extractStatusData(String poolName, Date startTime, Date endTime) {
		Map<Integer, DataWrapper> wrappers = new HashMap<Integer, DataWrapper>();
		int length = TimeUtil.calIntervalMinutes(startTime, endTime) + 60;
		long endTimeMills = endTime.getTime();
		List<NginxHourStatistics> statuses;

		if (endTimeMills >= TimeUtil.getLastHour().getTime()) {
			Date realEndTime = TimeUtil.trimHour(TimeUtil.getLastHour(), -1);
			statuses = m_nginxStatisticsService.findHourlyStatistics(poolName, startTime, realEndTime);

			statuses.add(m_lastHourStatistics);
			if (endTimeMills >= TimeUtil.getCurrentHour().getTime()) {
				statuses.add(m_currentHourStatistics);
			}
		} else {
			statuses = m_nginxStatisticsService.findHourlyStatistics(poolName, startTime, endTime);
		}
		addDefaultWrapper(wrappers, length);
		for (NginxHourStatistics status : statuses) {
			try {
				int offset = TimeUtil.calIntervalMinutes(startTime, status.getHour());

				extractHourlyStatusData(wrappers, status.findPool(poolName), offset, length);
			} catch (Exception ex) {
				logger.error("extract data error: length:" + length, ex);
			}
		}
		return wrappers.values();
	}

	protected void extractHourlyStatusData(Map<Integer, DataWrapper> wrappers, Pool pool, int offset, int interval) {
		if (pool == null) {
			return;
		}
		for (Server server : pool.getServers().values()) {
			for (Domain domain : server.getDomains().values()) {
				for (Period period : domain.getPeriods().values()) {
					int minute = period.getMinute();

					if (minute < 0 || minute > 60) {
						continue;
					}
					for (Status status : period.getStatuses().values()) {
						Integer code = status.getCode();
						DataWrapper currentWrapper = wrappers.get(code);

						if (currentWrapper == null) {
							DataWrapper wrapper = new DataWrapper();

							wrapper.setDesc("status code: " + code);
							wrapper.setData(generateLongArray(interval));
							wrapper.setInterval(60 * 1000);
							wrapper.setTotal(0);
							wrappers.put(code, wrapper);
							currentWrapper = wrapper;
						}
						currentWrapper.getData()[minute + offset] += status.getCount();
						currentWrapper.setTotal(currentWrapper.getTotal() + status.getCount());

						if (code >= 400 && code <= 499) {
							DataWrapper wrapper4 = wrappers.get(4);

							wrapper4.getData()[minute + offset] += status.getCount();
							wrapper4.setTotal(wrapper4.getTotal() + status.getCount());
						} else if (code >= 500 && code <= 599) {
							DataWrapper wrapper5 = wrappers.get(5);

							wrapper5.getData()[minute + offset] += status.getCount();
							wrapper5.setTotal(wrapper5.getTotal() + status.getCount());
						}
					}
				}
			}
		}
	}

	private Long[] generateLongArray(int length) {
		Long[] array = new Long[length];

		for (int i = 0; i < length; i++) {
			array[i] = 0L;
		}
		return array;
	}

	private int getCountByPoolNameAndMinute(int minute, String poolName, String statusCode,
			NginxHourStatistics hourStatistics) {
		int currentMinuteCount = 0;
		Pool pool = hourStatistics.findPool(poolName);

		if (pool != null) {
			for (Server server : pool.getServers().values()) {
				for (Domain domain : server.getDomains().values()) {
					Period period = domain.findPeriod(minute);

					if (period != null) {
						if ("5XX".equals(statusCode)) {
							for (Status periodStatus : period.getStatuses().values()) {
								if (periodStatus.getCode() >= 500 && periodStatus.getCode() <= 599) {
									currentMinuteCount += periodStatus.getCount();
								}
							}
						} else if ("4XX".equals(statusCode)) {
							for (Status periodStatus : period.getStatuses().values()) {
								if (periodStatus.getCode() >= 400 && periodStatus.getCode() <= 499) {
									currentMinuteCount += periodStatus.getCount();
								}
							}
						} else {
							try {
								Status periodStatus = period.findStatus(Integer.parseInt(statusCode));

								if (periodStatus != null) {
									currentMinuteCount += periodStatus.getCount();
								}
							} catch (Exception ex) {
							}
						}
					}
				}
			}
		}
		return currentMinuteCount;
	}

	public NginxHourStatistics getLastHourStatistics() {
		return m_lastHourStatistics;
	}

	public NginxHourStatistics getCurrentHourStatistics() {
		return m_currentHourStatistics;
	}

	@PostConstruct
	private void initLatest2HourStatistics() {
		try {
			m_lastHourStatistics = m_nginxStatisticsService
					.findHourlyStatistics(TimeUtil.getLastHour(), TimeUtil.getLastHour()).get(0);
		} catch (Exception e) {
			m_lastHourStatistics = new NginxHourStatistics();
			m_lastHourStatistics.setHour(TimeUtil.getLastHour());
		}
		try {
			m_currentHourStatistics = m_nginxStatisticsService
					.findHourlyStatistics(TimeUtil.getCurrentHour(), TimeUtil.getCurrentHour()).get(0);
		} catch (Exception e) {
			m_currentHourStatistics = new NginxHourStatistics();
			m_currentHourStatistics.setHour(TimeUtil.getCurrentHour());
		}
	}

	@Scheduled(cron = "0 0 * * * ?")
	public void updateStatusByHour() {
		NginxHourStatistics currentStatus = new NginxHourStatistics();

		currentStatus.setHour(TimeUtil.getCurrentHour());
		m_lastHourStatistics = m_currentHourStatistics;
		m_currentHourStatistics = currentStatus;
	}

}
