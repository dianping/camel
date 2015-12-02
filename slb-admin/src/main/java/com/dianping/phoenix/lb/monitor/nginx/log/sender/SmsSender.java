package com.dianping.phoenix.lb.monitor.nginx.log.sender;

import com.dianping.phoenix.lb.monitor.nginx.log.content.SmsContentGenerator;
import com.dianping.phoenix.lb.service.model.CmdbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.unidal.helper.Files;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Service
public class SmsSender implements Sender {

	@Autowired
	private SmsContentGenerator m_contentGenerator;

	@Autowired
	private CmdbService m_cmdbService;

	Logger logger = LoggerFactory.getLogger(getClass());

	// not available for outer environment, need to change
	@Override
	public boolean send(AlertMessageEntity message) {
		String content = m_contentGenerator.generateContent(message);
		List<String> phones = m_cmdbService.getCmdbPhones(message.getPoolName());
		StringBuilder sb = new StringBuilder();

		for (String phone : phones) {
			InputStream in = null;
			try {
				// call send sms api
				String format = "http://fakeurl/sms/send/json?jsonm={type:808,mobile:\"%s\",pair:{body=\"%s\"}}";
				String urlAddress = String.format(format, phone, URLEncoder.encode(content, "utf-8"));
				URL url = new URL(urlAddress);
				URLConnection conn = url.openConnection();

				conn.setConnectTimeout(2000);
				conn.setReadTimeout(3000);
				in = conn.getInputStream();
				sb.append(Files.forIO().readFrom(in, "utf-8")).append("");
			} catch (Exception e) {
				logger.error("[error][send][mail]", e);
			} finally {
				try {
					if (in != null) {
						in.close();
					}
				} catch (IOException e) {
				}
			}
		}
		if (sb.indexOf("200") > -1) {
			return true;
		} else {
			return false;
		}
	}

}
