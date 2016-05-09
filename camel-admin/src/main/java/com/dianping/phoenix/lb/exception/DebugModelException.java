package com.dianping.phoenix.lb.exception;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public class DebugModelException extends Exception {

	private static final long serialVersionUID = 1L;

	public DebugModelException() {
		super("action stop for debug mode is on");
	}

}
