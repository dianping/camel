package com.dianping.phoenix.lb.utils;

import com.dianping.phoenix.lb.constant.MessageID;
import com.dianping.phoenix.lb.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Leo Liang
 *
 */
public class ExceptionUtils {
	private static final Logger log = LoggerFactory.getLogger(ExceptionUtils.class);

	public static void logAndRethrowBizException(Throwable e, MessageID messageId, Object... args) throws BizException {
		String msg = MessageUtils.getMessage(messageId, args);
		log.error(msg, e);
		throw new BizException(messageId, e, args);
	}

	public static void logAndRethrowBizException(Throwable e) throws BizException {
		log.error(e.getMessage(), e);
		throw new BizException(e);
	}

	public static void rethrowBizException(Throwable e) throws BizException {
		throw new BizException(e);
	}

	public static void throwBizException(MessageID messageId, Object... args) throws BizException {
		throw new BizException(messageId, args);
	}
}
