package com.dianping.phoenix.lb.monitor.nginx.log.sender;

import com.dianping.phoenix.lb.model.entity.MonitorRule;

import java.util.Date;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public class AlertMessageEntity {

	private MonitorRule m_rule;

	private Date m_date;

	private int m_actualCount;

	private String m_poolName;

	public AlertMessageEntity(String poolName, int actualCount, MonitorRule rule) {
		m_poolName = poolName;
		m_actualCount = actualCount;
		m_rule = rule;
		m_date = new Date();
	}

	public int getActualCount() {
		return m_actualCount;
	}

	public Date getDate() {
		return m_date;
	}

	public String getPoolName() {
		return m_poolName;
	}

	public MonitorRule getRule() {
		return m_rule;
	}

}
