package com.dianping.phoenix.lb.monitor.nginx.log.alert;

import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.Queue;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Service
public class AlertStatusContainer {

	int m_size = 10000;

	Queue<String> m_alerts = new LinkedList<String>();

	public synchronized void add(String poolName, String statusCode, String dateStr) {
		int size = m_alerts.size();

		while (size >= m_size) {
			m_alerts.poll();
			size--;
		}
		m_alerts.offer(poolName + ":" + statusCode + ":" + dateStr);
	}

	public synchronized boolean contains(String poolName, String statusCode, String dateStr) {
		String queryStr = poolName + ":" + statusCode + ":" + dateStr;

		return m_alerts.contains(queryStr);
	}

}
