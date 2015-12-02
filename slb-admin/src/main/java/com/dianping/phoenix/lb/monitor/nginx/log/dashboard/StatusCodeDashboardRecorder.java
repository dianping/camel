package com.dianping.phoenix.lb.monitor.nginx.log.dashboard;

import com.dianping.phoenix.lb.constant.SlbConfig;
import com.dianping.phoenix.lb.monitor.nginx.log.NginxStatusRecorder;
import com.dianping.phoenix.lb.monitor.nginx.log.alert.AlertStatusContainer;
import com.dianping.phoenix.slb.nginx.statistics.hour.entity.NginxHourStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Calendar;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Service
public class StatusCodeDashboardRecorder {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "scheduledThreadPool")
	private ScheduledExecutorService m_executorService;

	@Autowired
	private NginxStatusRecorder m_nginxStatusRecorder;

	@Autowired
	private DashboardContainer m_dashboardContainer;

	@Autowired
	private SlbConfig m_slbConfig;

	@Autowired
	private AlertStatusContainer m_container;

	@PostConstruct
	public void run() {
		m_executorService.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				Calendar cal = Calendar.getInstance();

				cal.add(Calendar.MINUTE, -2);

				int minute = cal.get(Calendar.MINUTE);
				NginxHourStatistics hourStatistics;

				if (minute >= 58) {
					hourStatistics = m_nginxStatusRecorder.getLastHourStatistics();
				} else {
					hourStatistics = m_nginxStatusRecorder.getCurrentHourStatistics();
				}

				int poolSize = m_slbConfig.getStatusCodeDashboardRowSize();
				DashboardVisitor visitor = new DashboardVisitor(minute, poolSize, cal, m_container);

				hourStatistics.accept(visitor);

				Map<String, MinuteEntry> resultPair = visitor.getResult();

				for (Entry<String, MinuteEntry> entry : resultPair.entrySet()) {
					String statusCode = entry.getKey();

					m_dashboardContainer.insertMinuteEntry(statusCode, entry.getValue());
					m_dashboardContainer.addStatusCode(statusCode);
				}
			}
		}, 1, 60, TimeUnit.SECONDS);
	}

}
