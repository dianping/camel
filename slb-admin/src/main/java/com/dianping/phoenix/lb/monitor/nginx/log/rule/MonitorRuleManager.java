package com.dianping.phoenix.lb.monitor.nginx.log.rule;

import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.MonitorRule;
import com.dianping.phoenix.lb.service.model.MonitorRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Service
public class MonitorRuleManager {

	@Autowired
	private MonitorRuleService m_ruleService;

	public void addOrUpdateMonitorRule(MonitorRule rule) throws BizException {
		m_ruleService.addOrUpdateMonitorRule(rule.getId(), rule);
	}

	public List<MonitorRule> getMonitorRules() {
		return m_ruleService.listMonitorRules();
	}

	public void removeMonitorRule(String ruleId) throws BizException {
		m_ruleService.deleteMonitorRule(ruleId);
	}

}
