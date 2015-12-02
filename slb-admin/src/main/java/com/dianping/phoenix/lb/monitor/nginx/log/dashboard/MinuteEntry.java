package com.dianping.phoenix.lb.monitor.nginx.log.dashboard;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

public class MinuteEntry {

	private String m_time;

	private List<PoolEntry> m_pools;

	public List<PoolEntry> getPools() {
		return m_pools;
	}

	public void setPools(List<PoolEntry> pools) {
		m_pools = pools;
	}

	public String getTime() {
		return m_time;
	}

	public void setTime(String time) {
		m_time = time;
	}

	public class PoolEntry {

		private String m_poolName;

		private int m_count;

		private String m_startDate;

		private String m_endDate;

		private String m_shortName;

		private String m_encode = "UTF-8";

		private String m_hasAlert = "false";

		public PoolEntry(String name, int count, String startDate, String endDate, boolean hasAlert) {
			this.m_poolName = name;
			this.m_count = count;
			this.m_startDate = startDate;
			this.m_endDate = endDate;
			int length = m_poolName.length();

			if (length >= 15) {
				m_shortName = m_poolName.substring(0, 5) + "..." + m_poolName.substring(length - 5, length);
			} else {
				m_shortName = m_poolName;
			}
			if (hasAlert) {
				m_hasAlert = "true";
			}
		}

		public int getCount() {
			return m_count;
		}

		public String getShortName() {
			return m_shortName;
		}

		public String getUrl() throws UnsupportedEncodingException {
			String startTime = URLEncoder.encode(m_startDate, m_encode);
			String endTime = URLEncoder.encode(m_endDate, m_encode);

			return "/monitor/status/data/" + m_poolName + "?startTime=" + startTime + "&endTime=" + endTime;
		}

		public String getHasAlert() {
			return m_hasAlert;
		}

	}
}
