package com.dianping.platform.slb.agent.core.constant;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public enum MessageID {

	SCRIPT_EXCUTE_EXCEPTION("script_execute_exception"), FILE_NOT_EXIST("file_not_exist"), DEFAULT(
			"default"), ARGUMENT_CHECK_FAIL("argument_check_fail");

	private String m_messageId;

	MessageID(String messageId) {
		m_messageId = messageId;
	}

	public String getMessageId() {
		return m_messageId;
	}

}
