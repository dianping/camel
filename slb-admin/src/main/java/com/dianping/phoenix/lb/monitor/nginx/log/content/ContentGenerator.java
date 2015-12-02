package com.dianping.phoenix.lb.monitor.nginx.log.content;

import com.dianping.phoenix.lb.monitor.nginx.log.sender.AlertMessageEntity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public abstract class ContentGenerator {

	protected DateFormat m_df = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	public abstract String generateTitle(AlertMessageEntity message);

	public String generateContent(AlertMessageEntity message) {
		StringBuilder builder = new StringBuilder(1000);

		builder.append("[告警集群: ").append(message.getPoolName()).append(" ][状态码: ")
				.append(message.getRule().getStatusCode()).append(" ][累加分钟: ").append(message.getRule().getMinute())
				.append(" ][阈值: ").append(message.getRule().getValue()).append(" ][实际值: ")
				.append(message.getActualCount()).append(" ][报警时间: ").append(m_df.format(message.getDate()))
				.append(" ]");
		return builder.toString();
	}
}
