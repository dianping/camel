package com.dianping.phoenix.lb.exception;

/**
 * SLB异常基类
 *
 * @author mengwenchao
 *         <p/>
 *         2014年11月12日 下午4:42:37
 */
public class SlbRuntimeException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public SlbRuntimeException(String message) {
		super(message);
	}

	public SlbRuntimeException(String message, Throwable th) {
		super(message, th);
	}

}
