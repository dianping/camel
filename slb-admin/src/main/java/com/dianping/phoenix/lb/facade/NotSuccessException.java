package com.dianping.phoenix.lb.facade;

import com.dianping.phoenix.lb.exception.SlbException;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public class NotSuccessException extends SlbException {

	private static final long serialVersionUID = 1L;

	public NotSuccessException(String successInfo) {
		super(successInfo);
	}

}
