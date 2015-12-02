package com.dianping.phoenix.lb.service.model;

import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.MonitorRule;

import java.util.List;

/**
 * @author liyang
 *         <p/>
 *         2015年3月31日 下午4:17:52
 */
public interface MonitorRuleService {

	List<MonitorRule> listMonitorRules();

	MonitorRule findMonitorRule(String ruleId) throws BizException;

	void addOrUpdateMonitorRule(String ruleId, MonitorRule rule) throws BizException;

	void deleteMonitorRule(String ruleId) throws BizException;

}
