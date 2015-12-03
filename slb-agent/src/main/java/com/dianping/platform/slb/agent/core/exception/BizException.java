package com.dianping.platform.slb.agent.core.exception;

import com.dianping.platform.slb.agent.core.constant.MessageID;
import com.dianping.platform.slb.agent.core.util.MessageUtil;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public class BizException extends RuntimeException {

	private MessageID m_messageId;

	public BizException(MessageID messageId, String... args) {
		super(MessageUtil.getMessage(messageId, args));
		this.m_messageId = messageId;
	}

	public BizException(MessageID messageId, Throwable throwable, String... args) {
		super(MessageUtil.getMessage(messageId, args), throwable);
		this.m_messageId = messageId;
	}

	public BizException(Throwable throwable) {
		super(throwable.getMessage());
		this.m_messageId = MessageID.DEFAULT;
	}

}
