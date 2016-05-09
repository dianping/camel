package com.dianping.phoenix.lb.exception;

import com.dianping.phoenix.lb.constant.MessageID;
import com.dianping.phoenix.lb.utils.MessageUtils;

/**
 * @author Leo Liang
 *
 */
public class BizException extends Exception {
	static final long serialVersionUID = -3387516993124229443L;

	private MessageID messageId;

	public BizException(MessageID messageId, Object... args) {
		super(MessageUtils.getMessage(messageId, args));
		this.messageId = messageId;
	}

	public BizException(MessageID messageId, String message) {
		super(message);
		this.messageId = messageId;
	}

	public BizException(MessageID messageId, Throwable cause, Object... args) {
		super(MessageUtils.getMessage(messageId, args), cause);
		this.messageId = messageId;
	}

	public BizException(Throwable cause) {
		super(cause.getMessage(), cause);
	}

	public MessageID getMessageId() {
		return messageId;
	}
}
