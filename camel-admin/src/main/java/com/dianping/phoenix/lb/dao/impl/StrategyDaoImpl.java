/**
 * Project: phoenix-load-balancer
 * <p/>
 * File Created at 2013-10-18
 */
package com.dianping.phoenix.lb.dao.impl;

import com.dianping.phoenix.lb.dao.ModelStore;
import com.dianping.phoenix.lb.dao.StrategyDao;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.Strategy;
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
public class StrategyDaoImpl extends AbstractDao implements StrategyDao {

	/**
	 * @param store
	 */
	@Autowired(required = true)
	public StrategyDaoImpl(ModelStore store) {
		super(store);
	}

	@Override
	public List<Strategy> list() {
		return store.listStrategies();
	}

	@Override
	public Strategy find(String strategyName) {
		return store.findStrategy(strategyName);
	}

	@Override
	public void add(Strategy strategy) throws BizException {
		store.updateOrCreateStrategy(strategy.getName(), strategy);
	}

	@Override
	public void delete(String strategyName) throws BizException {
		store.removeStrategy(strategyName);
	}

	@Override
	public void update(Strategy strategy) throws BizException {
		store.updateOrCreateStrategy(strategy.getName(), strategy);
	}

}
