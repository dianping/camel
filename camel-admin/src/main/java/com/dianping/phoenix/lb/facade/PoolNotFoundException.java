package com.dianping.phoenix.lb.facade;

import com.dianping.phoenix.lb.exception.SlbException;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年12月3日 下午2:14:02
 */
public class PoolNotFoundException extends SlbException {

	private static final long serialVersionUID = 1L;

	public PoolNotFoundException(String poolName) {
		super("the pool is not found:" + poolName);
	}

}
