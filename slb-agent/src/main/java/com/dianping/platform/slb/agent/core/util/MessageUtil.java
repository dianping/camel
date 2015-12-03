package com.dianping.platform.slb.agent.core.util;

import com.dianping.platform.slb.agent.core.constant.MessageID;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public class MessageUtil {

	private static final String MESSAGE_PROPERTIES = "message.properties";

	private static Properties m_properties = new Properties();

	static {
		InputStream inputStream = null;

		try {
			inputStream = MessageUtil.class.getClassLoader().getResourceAsStream(MESSAGE_PROPERTIES);

			m_properties.load(inputStream);
		} catch (IOException e) {
			throw new RuntimeException("[fatal]MessageUtil init failed");
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}

	public static String getMessage(MessageID messageId, String... params) {
		String messagePattern = m_properties.getProperty(messageId.getMessageId());

		if (StringUtils.isEmpty(messagePattern)) {
			return "unknown message";
		}
		return String.format(messagePattern, params);
	}
}
