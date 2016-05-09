package com.dianping.phoenix.lb.monitor.nginx.log.sender;

import com.dianping.phoenix.lb.monitor.nginx.log.content.MailContentGenerator;
import com.dianping.phoenix.lb.service.model.CmdbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.unidal.helper.Files;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
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
public class MailSender implements Sender {

	@Autowired
	private MailContentGenerator m_contentGenerator;

	@Autowired
	private CmdbService m_cmdbService;

	Logger logger = LoggerFactory.getLogger(getClass());

	// not available for outer environment, need to change
	@Override
	public boolean send(AlertMessageEntity message) {
		String title = m_contentGenerator.generateTitle(message);
		String content = m_contentGenerator.generateContent(message);
		List<String> emails = m_cmdbService.getCmdbMails(message.getPoolName());
		StringBuilder sb = new StringBuilder();

		for (String email : emails) {
			InputStream in = null;
			OutputStreamWriter writer = null;
			try {
				title = title.replaceAll(",", " ");
				content = content.replaceAll(",", " ");

				String value = title + "," + content;
				// call send email api
				URL url = new URL(
						"fake url" + email);
				URLConnection conn = url.openConnection();

				conn.setConnectTimeout(2000);
				conn.setReadTimeout(3000);
				conn.setDoOutput(true);
				conn.setDoInput(true);
				writer = new OutputStreamWriter(conn.getOutputStream());
				String encode = "&value=" + URLEncoder.encode(value, "utf-8");

				writer.write(encode);
				writer.flush();

				in = conn.getInputStream();
				String result = Files.forIO().readFrom(in, "utf-8");

				sb.append(result).append("");
			} catch (Exception e) {
				logger.error("[error][send][mail]", e);
			} finally {
				try {
					if (in != null) {
						in.close();
					}
					if (writer != null) {
						writer.close();
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
