package com.dianping.phoenix.lb.dao;

import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.Strategy;

import java.util.List;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface StrategyDao {

	List<Strategy> list();

	Strategy find(String strategyName);

	void add(Strategy strategy) throws BizException;

	void delete(String strategyName) throws BizException;

	void update(Strategy strategy) throws BizException;

}
