package com.dianping.phoenix.lb.monitor.nginx.log.content;

import com.dianping.phoenix.lb.monitor.nginx.log.sender.AlertMessageEntity;
import org.springframework.stereotype.Service;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Service
public class SmsContentGenerator extends ContentGenerator {

	@Override
	public String generateTitle(AlertMessageEntity message) {
		return "";
	}

	@Override
	public String generateContent(AlertMessageEntity message) {
		return "[SLB告警]" + super.generateContent(message);
	}

}
