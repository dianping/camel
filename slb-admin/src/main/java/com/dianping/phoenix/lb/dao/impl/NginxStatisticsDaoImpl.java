package com.dianping.phoenix.lb.dao.impl;

import com.dianping.phoenix.lb.dao.AbstractMongoDao;
import com.dianping.phoenix.lb.dao.NginxStatisticsDao;
import com.dianping.phoenix.lb.dao.mongo.MongoDbStartInitializer;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.slb.nginx.statistics.hour.entity.NginxHourStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Service
public class NginxStatisticsDaoImpl extends AbstractMongoDao implements NginxStatisticsDao {

	private static final String SLB_NGINX_HOURLY_STATISTICS_COLLECTION_NAME = "nginx_hourly_statistics";
	private static final String SLB_MODEL_STATUS_POOL_KEY = "m_pools";
	private static final String NGINX_STATISTICS_HOUR = "m_hour";
	protected final Logger logger = LoggerFactory.getLogger(getClass());
	@Resource(name = "nginxLogMongoTemplate")
	protected MongoTemplate m_mongoTemplate;

	public void setMongoTemplate(MongoTemplate mongoTemplate) {
		this.m_mongoTemplate = mongoTemplate;
	}

	@Override
	protected MongoTemplate getTemplate() {
		return m_mongoTemplate;
	}

	@PostConstruct
	public void initCollection() {
		try {
			Index index = new Index().on("m_hour", Direction.DESC);

			createIfNullCollection(SLB_NGINX_HOURLY_STATISTICS_COLLECTION_NAME, null, index);
		} catch (Exception ex) {
			logger.error("create colection failed: " + SLB_NGINX_HOURLY_STATISTICS_COLLECTION_NAME, ex);
		}
	}

	@Override
	public void addOrUpdateHourlyStatistics(NginxHourStatistics hourStatistics) throws BizException {
		Date hourStatisticsTime = hourStatistics.getHour();

		m_mongoTemplate.upsert(Query.query(Criteria.where("m_hour").is(hourStatisticsTime)),
				new Update().set(SLB_MODEL_STATUS_POOL_KEY, convertToMongoObject(hourStatistics.getPools())),
				SLB_NGINX_HOURLY_STATISTICS_COLLECTION_NAME);
	}

	@Override
	public List<NginxHourStatistics> findHourlyStatistics(Date startHour, Date endHour) throws BizException {
		Query query = Query.query(Criteria.where("m_hour").lte(endHour).gte(startHour));
		List<NginxHourStatistics> statistics = m_mongoTemplate
				.find(query, NginxHourStatistics.class, SLB_NGINX_HOURLY_STATISTICS_COLLECTION_NAME);

		if (statistics == null) {
			statistics = new ArrayList<NginxHourStatistics>();
		}
		return statistics;
	}

	@Override
	public List<NginxHourStatistics> findHourlyStatistics(String poolName, Date startHour, Date endHour)
			throws BizException {
		Query query = Query.query(Criteria.where(NGINX_STATISTICS_HOUR).lte(endHour).gte(startHour));
		query.fields().include(generatePoolKey(poolName)).include(NGINX_STATISTICS_HOUR);
		List<NginxHourStatistics> statistics = m_mongoTemplate
				.find(query, NginxHourStatistics.class, SLB_NGINX_HOURLY_STATISTICS_COLLECTION_NAME);

		if (statistics == null) {
			statistics = new ArrayList<NginxHourStatistics>();
		}
		return statistics;
	}

	private String generatePoolKey(String poolName) {
		String poolNameAfterParse = poolName.replace(".", MongoDbStartInitializer.MAP_KEY_DOT_REPLACEMENT);
		return SLB_MODEL_STATUS_POOL_KEY + "." + poolNameAfterParse;
	}

}
