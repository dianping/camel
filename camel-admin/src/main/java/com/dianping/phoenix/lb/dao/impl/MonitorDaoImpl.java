package com.dianping.phoenix.lb.dao.impl;

import com.dianping.phoenix.lb.dao.ModelStore;
import com.dianping.phoenix.lb.dao.MonitorRuleDao;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.MonitorRule;
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
public class MonitorDaoImpl extends AbstractDao implements MonitorRuleDao {

	/**
	 * @param store
	 */
	@Autowired(required = true)
	public MonitorDaoImpl(ModelStore store) {
		super(store);
	}

	@Override
	public List<MonitorRule> list() {
		return store.listMonitorRules();
	}

	@Override
	public MonitorRule find(String ruleId) {
		return store.findMonitorRule(ruleId);
	}

	@Override
	public void addOrUpdate(MonitorRule rule) throws BizException {
		store.updateOrCreateMonitorRule(rule.getId(), rule);
	}

	@Override
	public void delete(String ruleId) throws BizException {
		store.removeMonitorRule(ruleId);
	}
}
