package com.dianping.phoenix.lb.monitor.nginx.log.content;

import com.dianping.phoenix.lb.monitor.nginx.log.sender.AlertMessageEntity;
import com.dianping.phoenix.lb.utils.TimeUtil;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Service
public class MailContentGenerator extends ContentGenerator {

	private String m_hostname = null;

	private String m_port = null;

	private boolean m_isURLReady = false;

	private String buildURL(AlertMessageEntity message) {
		try {
			StringBuilder builder = new StringBuilder(1000);
			String startTimeStr = m_df.format(TimeUtil.trimHour(message.getDate()));
			String endTimeStr = m_df.format(TimeUtil.trimHour(message.getDate(), 1));

			builder.append("[告警详情][点击：");
			builder.append("<a href=\"http://").append(m_hostname).append(":").append(m_port)
					.append("/monitor/status/data/");
			builder.append(URLEncoder.encode(message.getPoolName(), "UTF-8")).append("?startTime=");
			builder.append(URLEncoder.encode(startTimeStr, "UTF-8"));
			builder.append(message.getPoolName()).append("&endTime=");
			builder.append(URLEncoder.encode(endTimeStr, "UTF-8"));
			builder.append("\">链接</a>]");
			return builder.toString();
		} catch (UnsupportedEncodingException e) {
			return "";
		}
	}

	@Override
	public String generateContent(AlertMessageEntity message) {
		if (m_isURLReady) {
			return super.generateContent(message) + buildURL(message);
		} else {
			return super.generateContent(message);
		}
	}

	@Override
	public String generateTitle(AlertMessageEntity message) {
		return "[SLB HTTP状态码告警] 集群：" + message.getPoolName();
	}

	public void setHostname(String hostname) {
		m_hostname = hostname;
		m_isURLReady = true;
	}

	public void setPort(String port) {
		m_port = port;
		m_isURLReady = true;
	}

}
