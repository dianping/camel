package com.dianping.phoenix.lb.monitor.nginx.log.statistics;

import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.monitor.nginx.log.NginxStatusRecorder;
import com.dianping.phoenix.lb.service.model.NginxLogService;
import com.dianping.phoenix.lb.service.model.NginxStatisticsService;
import com.dianping.phoenix.lb.service.model.PoolService;
import com.dianping.phoenix.lb.service.model.VirtualServerService;
import com.dianping.phoenix.lb.utils.TimeUtil;
import com.dianping.phoenix.slb.nginx.log.entity.NginxLog;
import com.dianping.phoenix.slb.nginx.statistics.hour.entity.NginxHourStatistics;
import com.dianping.phoenix.slb.nginx.statistics.hour.entity.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.unidal.tuple.Pair;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Service
public class HourlyStatisticsCounter implements StatisticsCounter {

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	@Autowired
	private NginxLogService m_logService;
	@Autowired
	private NginxStatisticsService m_statisticsService;
	@Autowired
	private NginxStatusRecorder m_nginxStatusRecorder;
	@Autowired
	private VirtualServerService m_virtualServerService;
	@Autowired
	private PoolService m_poolService;
	private Set<String> m_vsNames;
	private Set<String> m_poolNames;
	private Object lock = new Object();

	@Scheduled(fixedDelay = 5 * 60 * 1000)
	public void updateVsAndPool() {
		try {
			Set<String> vsNames = m_virtualServerService.listVSNames();
			Set<String> poolNames = m_poolService.listPoolNames();

			synchronized (lock) {
				m_vsNames = vsNames;
				m_poolNames = poolNames;
			}
		} catch (BizException e) {
		}
	}

	public void addLogToHourStatistics(NginxLog log, NginxHourStatistics statistics) {
		int minute = TimeUtil.getMinuteInHour(log.getTime());
		String rawPool = log.getPool();

		if (!"-".equals(rawPool)) {
			Pair<String, String> domainAndPool = extractDomainAndPool(rawPool);
			Status httpStatus = statistics.findOrCreatePool(domainAndPool.getValue())
					.findOrCreateServer(log.getUpstreamServer()).findOrCreateDomain(domainAndPool.getKey())
					.findOrCreatePeriod(minute).findOrCreateStatus(log.getStatus());
			Integer count = httpStatus.getCount();

			httpStatus.setCount(count == null ? 1 : count + 1);

			Status allStatus = statistics.findOrCreatePool("all").findOrCreateServer("default")
					.findOrCreateDomain("default").findOrCreatePeriod(minute).findOrCreateStatus(log.getStatus());
			Integer allCount = allStatus.getCount();

			allStatus.setCount(allCount == null ? 1 : allCount + 1);
		}
	}

	@Override
	public boolean executeHourlyTask() {
		try {
			NginxHourStatistics hourStatistics = getLastHourStatistics();

			m_statisticsService.addOrUpdateHourlyStatistics(hourStatistics);
			return true;
		} catch (BizException e) {
			logger.error(getCounterName() + " executeHourlyTask failed", e);
			return false;
		}
	}

	protected Pair<String, String> extractDomainAndPool(String rawPool) {
		try {
			if (m_vsNames == null || m_poolNames == null) {
				throw new IllegalStateException("uninit vs and pool names!");
			} else {
				synchronized (lock) {
					if (rawPool.endsWith("#BACKUP")) {
						rawPool = rawPool.substring(0, rawPool.length() - 7);
					}
					for (String vsName : m_vsNames) {
						String vsPrefix = vsName + ".";

						if (rawPool.startsWith(vsPrefix)) {
							String suffix = rawPool.substring(vsPrefix.length());

							if (m_poolNames.contains(suffix)) {
								return new Pair<String, String>(vsName, suffix);
							}
						}
					}
				}
				throw new RuntimeException();
			}
		} catch (IllegalStateException ex) {
			logger.error("uninit vs and pool names!", ex);
			return new Pair<String, String>("default", rawPool);
		} catch (Exception ex) {
			return new Pair<String, String>("default", rawPool);
		}
	}

	public NginxHourStatistics generateHourStatistics(Date startHour) throws BizException {
		Date trimHour = TimeUtil.trimHour(startHour);
		List<NginxLog> logs = m_logService.findNginxLogs(trimHour, TimeUtil.trimHour(startHour, 1));
		NginxHourStatistics hourStatistics = new NginxHourStatistics();

		hourStatistics.setHour(trimHour);
		for (NginxLog log : logs) {
			addLogToHourStatistics(log, hourStatistics);
		}

		return hourStatistics;
	}

	@Override
	public String getCounterName() {
		return "HourlyStatisticsCounter";
	}

	private NginxHourStatistics getLastHourStatistics() throws BizException {
		Date lastHour = TimeUtil.getLastHour();
		NginxHourStatistics cachedHourStatistics = m_nginxStatusRecorder.getLastHourStatistics();

		if (cachedHourStatistics != null && cachedHourStatistics.getHour().compareTo(lastHour) == 0) {
			return cachedHourStatistics;
		} else {
			return generateHourStatistics(lastHour);
		}
	}

	public HourlyStatisticsCounter setVSNames(Set<String> vsNames) {
		m_vsNames = vsNames;
		return this;
	}

	public HourlyStatisticsCounter setPoolNames(Set<String> poolNames) {
		m_poolNames = poolNames;
		return this;
	}

}
