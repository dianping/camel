package com.dianping.phoenix.lb.monitor.nginx.log.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Service
public class SenderManager {

	@Autowired
	private MailSender m_mailSender;

	@Autowired
	private SmsSender m_smsSender;

	@Resource(name = "globalThreadPool")
	private ExecutorService m_executorService;

	private Logger m_logger = LoggerFactory.getLogger(getClass());

	private BlockingQueue<AlertMessageEntity> m_alerts = new LinkedBlockingDeque<AlertMessageEntity>(10000);

	public void addAlert(AlertMessageEntity message) {
		m_alerts.offer(message);
	}

	private void sendAlert(AlertMessageEntity message) {
		m_mailSender.send(message);
		m_smsSender.send(message);
	}

	@PostConstruct
	private void send() {
		m_executorService.execute(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						AlertMessageEntity alert = m_alerts.poll(5, TimeUnit.MILLISECONDS);

						if (alert != null) {
							sendAlert(alert);
						}
					} catch (Exception e) {
						m_logger.error("[error][send alert]", e);
					}
				}
			}
		});
	}

}
