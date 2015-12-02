package com.dianping.phoenix.lb.monitor.nginx.log.rule;

import com.dianping.phoenix.lb.model.entity.MonitorRule;
import com.dianping.phoenix.lb.monitor.nginx.log.NginxStatusRecorder;
import com.dianping.phoenix.lb.monitor.nginx.log.alert.AlertStatusContainer;
import com.dianping.phoenix.lb.monitor.nginx.log.sender.AlertMessageEntity;
import com.dianping.phoenix.lb.monitor.nginx.log.sender.SenderManager;
import com.dianping.phoenix.lb.utils.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.unidal.tuple.Pair;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Service
public class RuleExecutor {

	@Autowired
	private NginxStatusRecorder m_recorder;

	@Autowired
	private SenderManager m_sendManager;

	@Autowired
	private AlertStatusContainer m_container;

	public void execute(MonitorRule rule) {
		int currentMinute = TimeUtil.getMinute(System.currentTimeMillis());

		if (currentMinute % rule.getMinute() != 0) {
			return;
		}

		String poolName = rule.getPool();

		for (String rawTmpPool : poolName.split(",")) {
			String currentPoolName = rawTmpPool.trim();

			executePool(rule, currentPoolName);
		}
	}

	protected void executePool(MonitorRule rule, String poolName) {
		int threshold = rule.getValue();
		String statusCode = rule.getStatusCode();
		Integer minute = rule.getMinute();
		String dateStr = getDateStr();

		if ("All".equals(poolName)) {
			for (Pair<String, Integer> pair : m_recorder.calCount(minute, statusCode, rule.getFilterPool())) {
				int count = pair.getValue();

				if (threshold < count) {
					String currentPoolName = pair.getKey();

					m_sendManager.addAlert(new AlertMessageEntity(currentPoolName, count, rule));
					m_container.add(currentPoolName, statusCode, dateStr);
				}
			}
		} else {
			int count = m_recorder.calCountByPoolName(minute, poolName, rule.getStatusCode());

			if (threshold < count) {
				m_sendManager.addAlert(new AlertMessageEntity(poolName, count, rule));
				m_container.add(poolName, statusCode, dateStr);
			}
		}
	}

	private String getDateStr() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

		cal.add(Calendar.MINUTE, -1);
		return sdf.format(cal.getTime());
	}

}
