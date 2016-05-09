package com.dianping.phoenix.lb.monitor.nginx.log.dashboard;

import com.dianping.phoenix.lb.monitor.nginx.log.alert.AlertStatusContainer;
import com.dianping.phoenix.lb.monitor.nginx.log.dashboard.MinuteEntry.PoolEntry;
import com.dianping.phoenix.slb.nginx.statistics.hour.IVisitor;
import com.dianping.phoenix.slb.nginx.statistics.hour.entity.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

public class DashboardVisitor implements IVisitor {

	private int m_minute;

	private String m_descDate;

	private String m_startDate;

	private String m_endDate;

	private int m_poolSize;

	private AlertStatusContainer m_alertContainer;

	private Map<String, List<PoolEntry>> m_minuteStatistics = new HashMap<String, List<PoolEntry>>();

	private Map<String, Integer> m_poolStatistics = new HashMap<String, Integer>();

	private int m_statusAll = 0;

	private int m_status5XX = 0;

	private int m_status4XX = 0;

	public DashboardVisitor(int minute, int poolSize, Calendar cal, AlertStatusContainer container) {
		m_minute = minute;
		m_poolSize = poolSize;
		m_alertContainer = container;

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

		m_descDate = simpleDateFormat.format(cal.getTime());

		cal.set(Calendar.MINUTE, 0);

		m_startDate = simpleDateFormat.format(cal.getTime());

		cal.add(Calendar.HOUR_OF_DAY, 1);

		m_endDate = simpleDateFormat.format(cal.getTime());
	}

	@Override
	public void visitDomain(Domain domain) {
		Period period = domain.findPeriod(m_minute);

		if (period != null) {
			for (Status status : period.getStatuses().values()) {
				visitStatus(status);
			}
		}
	}

	@Override
	public void visitNginxHourStatistics(NginxHourStatistics nginxHourStatistics) {
		for (Pool pool : nginxHourStatistics.getPools().values()) {
			visitPool(pool);
		}
	}

	@Override
	public void visitPeriod(Period period) {
		// not used
	}

	@Override
	public void visitPool(Pool pool) {
		String poolName = pool.getId();

		if ("all".equals(poolName)) {
			return;
		}

		m_poolStatistics = new HashMap<String, Integer>();
		m_statusAll = 0;
		m_status5XX = 0;
		m_status4XX = 0;

		for (Server server : pool.getServers().values()) {
			visitServer(server);
		}
		m_poolStatistics.put("all", m_statusAll);
		m_poolStatistics.put("5XX", m_status5XX);
		m_poolStatistics.put("4XX", m_status4XX);
		for (Entry<String, Integer> entry : m_poolStatistics.entrySet()) {
			String statusCode = entry.getKey();
			List<PoolEntry> poolEntries = m_minuteStatistics.get(statusCode);

			if (poolEntries == null) {
				poolEntries = new ArrayList<PoolEntry>();
				m_minuteStatistics.put(statusCode, poolEntries);
			}
			boolean hasAlert = m_alertContainer.contains(poolName, statusCode, m_descDate);

			poolEntries
					.add(new MinuteEntry().new PoolEntry(poolName, entry.getValue(), m_startDate, m_endDate, hasAlert));
		}
	}

	@Override
	public void visitServer(Server server) {
		for (Domain domain : server.getDomains().values()) {
			visitDomain(domain);
		}
	}

	@Override
	public void visitStatus(Status status) {
		String statusCode = status.getCode().toString();
		int value = status.getCount();
		Integer beforeVal = m_poolStatistics.get(statusCode);

		if (beforeVal == null) {
			beforeVal = 0;
		}

		m_poolStatistics.put(statusCode, beforeVal + value);
		m_statusAll += value;
		if (statusCode.startsWith("5")) {
			m_status5XX += value;
		} else if (statusCode.startsWith("4")) {
			m_status4XX += value;
		}
	}

	public Map<String, MinuteEntry> getResult() {
		Map<String, MinuteEntry> result = new HashMap<String, MinuteEntry>();

		for (Entry<String, List<PoolEntry>> entry : m_minuteStatistics.entrySet()) {
			MinuteEntry minuteEntry = new MinuteEntry();

			Collections.sort(entry.getValue(), new Comparator<PoolEntry>() {
				@Override
				public int compare(PoolEntry p1, PoolEntry p2) {
					if (p1.getCount() > p2.getCount()) {
						return -1;
					} else if (p1.getCount() < p2.getCount()) {
						return 1;
					}
					return 0;
				}
			});

			int valueSize = entry.getValue().size();

			minuteEntry.setTime(m_descDate);
			minuteEntry.setPools(entry.getValue().subList(0, valueSize >= m_poolSize ? m_poolSize : valueSize));
			result.put(entry.getKey(), minuteEntry);
		}
		return result;
	}

}
