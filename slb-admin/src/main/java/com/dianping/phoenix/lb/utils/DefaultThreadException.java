package com.dianping.phoenix.lb.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.lang.Thread.UncaughtExceptionHandler;

@Service
public class DefaultThreadException {

	private static final Logger logger = LoggerFactory.getLogger(DefaultThreadException.class);

	@PostConstruct
	public void setExceptionCaughtHandler() {

		if (logger.isInfoEnabled()) {
			logger.info("[setExceptionCaughtHandler]");
		}

		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {

			@Override
			public void uncaughtException(Thread t, Throwable e) {

				logger.error("uncaught exception in thread:" + t, e);

			}
		});

	}

}
