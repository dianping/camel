package com.dianping.phoenix.lb.monitor.nginx.log.dashboard;

import com.dianping.phoenix.lb.exception.BizException;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

@Service
public class DashboardContainer {

	int m_queueSize = 30;

	private Map<String, List<MinuteEntry>> m_dashboards = new HashMap<String, List<MinuteEntry>>();

	private Set<String> m_statusCodes = new TreeSet<String>(new Comparator<String>() {
		@Override
		public int compare(String o1, String o2) {
			return toInteger(o1) - toInteger(o2);
		}

		private int toInteger(String s) {
			if ("all".equals(s)) {
				return -3;
			} else if ("5XX".equals(s)) {
				return -2;
			} else if ("4XX".equals(s)) {
				return -1;
			} else {
				return Integer.parseInt(s);
			}
		}
	});

	@PostConstruct
	private void init() {
		m_statusCodes.add("all");
		m_statusCodes.add("5XX");
		m_statusCodes.add("4XX");
	}

	public boolean insertMinuteEntry(String statusCode, MinuteEntry minuteEntry) {
		List<MinuteEntry> minuteEntries = null;
		boolean result;

		if (m_dashboards.containsKey(statusCode)) {
			minuteEntries = m_dashboards.get(statusCode);
		} else {
			minuteEntries = new ArrayList<MinuteEntry>();
			m_dashboards.put(statusCode, minuteEntries);
		}
		synchronized (minuteEntries) {
			int size = minuteEntries.size();

			while (size >= m_queueSize) {
				minuteEntries.remove(0);
				size--;
			}
			result = minuteEntries.add(minuteEntry);
		}
		return result;
	}

	public List<MinuteEntry> fetchMinuteEntries(String statusCode, int size) throws BizException {
		if (!m_dashboards.containsKey(statusCode)) {
			throw new BizException(new RuntimeException(statusCode + " 没有数据！"));
		}

		List<MinuteEntry> result = new ArrayList<MinuteEntry>();
		List<MinuteEntry> minuteEntries = m_dashboards.get(statusCode);

		synchronized (minuteEntries) {
			int actualSize = minuteEntries.size();

			if (size > actualSize) {
				size = actualSize;
			}
			for (int i = actualSize - size; i < actualSize; i++) {
				result.add(minuteEntries.get(i));
			}
		}
		Collections.reverse(result);
		return result;
	}

	public void addStatusCode(String statusCode) {
		m_statusCodes.add(statusCode);
	}

	public Set<String> getStatusCodes() {
		return m_statusCodes;
	}

}
