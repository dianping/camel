/**
 * Project: phoenix-load-balancer
 * <p/>
 * File Created at 2013-10-17
 */
package com.dianping.phoenix.lb.service.model;

import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.Strategy;

import java.util.List;

/**
 * @author Leo Liang
 *
 */
public interface StrategyService {
	List<Strategy> listStrategies();

	Strategy findStrategy(String strategyName) throws BizException;

	void addStrategy(String strategyName, Strategy strategy) throws BizException;

	void deleteStrategy(String strategyName) throws BizException;

	void modifyStrategy(String strategyName, Strategy strategy) throws BizException;
}
