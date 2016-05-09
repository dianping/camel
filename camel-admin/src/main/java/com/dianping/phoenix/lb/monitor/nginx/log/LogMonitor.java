package com.dianping.phoenix.lb.monitor.nginx.log;

import com.dianping.phoenix.lb.model.entity.MonitorRule;
import com.dianping.phoenix.lb.monitor.nginx.log.rule.MonitorRuleManager;
import com.dianping.phoenix.lb.monitor.nginx.log.rule.RuleExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Service
public class LogMonitor {

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	@Autowired
	private MonitorRuleManager m_ruleManager;
	@Autowired
	private RuleExecutor m_ruleExecutor;
	@Resource(name = "scheduledThreadPool")
	private ScheduledExecutorService m_executorService;

	@PostConstruct
	public void run() {
		m_executorService.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				List<MonitorRule> rules = m_ruleManager.getMonitorRules();

				for (MonitorRule rule : rules) {
					try {
						m_ruleExecutor.execute(rule);
					} catch (Exception ex) {
						logger.error("rule execute error! " + rule.getId(), ex);
					}
				}
			}
		}, 1, 60, TimeUnit.SECONDS);
	}

}
