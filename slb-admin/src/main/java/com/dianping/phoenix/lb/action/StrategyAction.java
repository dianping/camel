package com.dianping.phoenix.lb.action;

import com.dianping.phoenix.lb.model.entity.Strategy;
import com.dianping.phoenix.lb.service.model.StrategyService;
import com.opensymphony.xwork2.ActionSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Component("strategyAction")
public class StrategyAction extends ActionSupport {

	private static final long serialVersionUID = -6727172351979878969L;

	@Autowired
	private StrategyService strategyService;

	private List<Strategy> strategies;

	private String poolName;

	public String listStrategies() {
		strategies = strategyService.listStrategies();
		return SUCCESS;
	}

	public List<Strategy> getStrategies() {
		return strategies;
	}

	public String getPoolName() {
		return poolName;
	}

	public void setPoolName(String poolName) {
		this.poolName = poolName;
	}

}
