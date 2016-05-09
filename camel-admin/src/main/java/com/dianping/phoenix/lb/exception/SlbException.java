package com.dianping.phoenix.lb.exception;

/**
 * SLB异常基类
 *
 * @author mengwenchao
 *         <p/>
 *         2014年11月12日 下午4:42:37
 */
public class SlbException extends Exception {

	private static final long serialVersionUID = 1L;

	public SlbException(String message) {
		super(message);
	}

	public SlbException(String message, Throwable th) {
		super(message, th);
	}

}
