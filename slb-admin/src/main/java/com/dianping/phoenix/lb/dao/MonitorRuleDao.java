package com.dianping.phoenix.lb.dao;

import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.MonitorRule;

import java.util.List;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface MonitorRuleDao {

	List<MonitorRule> list();

	MonitorRule find(String ruleId);

	void addOrUpdate(MonitorRule rule) throws BizException;

	void delete(String ruleId) throws BizException;

}
