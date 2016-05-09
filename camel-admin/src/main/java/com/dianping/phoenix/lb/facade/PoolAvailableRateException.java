package com.dianping.phoenix.lb.facade;

import com.dianping.phoenix.lb.exception.SlbException;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年12月3日 下午2:14:02
 */
public class PoolAvailableRateException extends SlbException {

	private static final long serialVersionUID = 1L;

	public PoolAvailableRateException(String poolName, double rate) {
		super("pool " + poolName + " available rate <" + rate * 100 + "%");
	}
}
