package com.dianping.platform.slb.agent.core.util;

import com.dianping.platform.slb.agent.core.constant.MessageID;
import com.dianping.platform.slb.agent.core.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public class ExceptionUtil {

	private static final Logger m_logger = LoggerFactory.getLogger(ExceptionUtil.class);

	public static void logAndRethrowException(Throwable throwable) throws BizException {
		m_logger.error(throwable.getMessage(), throwable);
		throw new BizException(throwable);
	}

	public static void logAndRethrowException(MessageID messageId, String... args) throws BizException {
		String message = MessageUtil.getMessage(messageId, args);

		m_logger.error(message);
		throw new BizException(messageId, args);
	}

	public static void logAndRethrowException(MessageID messageId, Throwable throwable, String... args)
			throws BizException {
		String message = MessageUtil.getMessage(messageId, args);

		m_logger.error(message, throwable);
		throw new BizException(messageId, throwable, args);
	}

}
