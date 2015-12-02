package com.dianping.phoenix.lb.dao.mongo;

import com.mongodb.WriteConcern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 初始化mongodb配置项
 *
 * @author mengwenchao
 *         <p/>
 *         2014年11月14日 下午4:42:17
 */
@Component
public class MongoDbStartInitializer implements ApplicationContextAware {

	public static final String MAP_KEY_DOT_REPLACEMENT = "__";
	protected final Logger logger = LoggerFactory.getLogger(getClass());
	private ApplicationContext applicationContext;
	@Resource(name = "scheduledThreadPool")
	private ScheduledExecutorService scheduledExecutor;

	@PostConstruct
	public void initMongoDb() {

		setDotReplacement();

	}

	private void setDotReplacement() {

		if (logger.isInfoEnabled()) {
			logger.info("[setDotReplacement]" + MAP_KEY_DOT_REPLACEMENT);
		}

		Map<String, MongoTemplate> templates = applicationContext.getBeansOfType(MongoTemplate.class);

		for (Entry<String, MongoTemplate> entry : templates.entrySet()) {
			String name = entry.getKey();
			MongoTemplate template = entry.getValue();

			if (logger.isInfoEnabled()) {
				logger.info("[initMongoDb]" + name);
			}

			if (logger.isInfoEnabled()) {
				logger.info("[initMongoDb][set write concern]safe");
			}
			template.setWriteConcern(WriteConcern.SAFE);

			MongoConverter converter = template.getConverter();
			if (converter instanceof MappingMongoConverter) {
				((MappingMongoConverter) converter).setMapKeyDotReplacement(MAP_KEY_DOT_REPLACEMENT);
			}
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

		this.applicationContext = applicationContext;
	}
}
