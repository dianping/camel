package com.dianping.phoenix.lb.dao.impl;

import com.dianping.phoenix.lb.dao.AbstractMongoDao;
import com.dianping.phoenix.lb.dao.NginxLogDao;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.slb.nginx.log.entity.NginxLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
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
public class NginxLogDaoImpl extends AbstractMongoDao implements NginxLogDao {

	private static final String SLB_NGINX_LOG_COLLECTION_NAME = "nginx_log";
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
			Index index = new Index().on("m_time", Direction.DESC);
			CollectionOptions options = new CollectionOptions(100 * 1024 * 1024 * 1024, 200 * 60 * 60 * 24 * 7, true);

			createIfNullCollection(SLB_NGINX_LOG_COLLECTION_NAME, options, index);
		} catch (Exception ex) {
			logger.error("create colection failed: " + SLB_NGINX_LOG_COLLECTION_NAME, ex);
		}
	}

	@Override
	public void addNginxLogs(List<NginxLog> logs) throws BizException {
		m_mongoTemplate.insert(logs, SLB_NGINX_LOG_COLLECTION_NAME);
	}

	@Override
	public List<NginxLog> findNginxLogs(Date startTime, Date endTime) throws BizException {
		Query query = Query.query(Criteria.where("m_time").lt(endTime).gte(startTime));
		List<NginxLog> logs = m_mongoTemplate.find(query, NginxLog.class, SLB_NGINX_LOG_COLLECTION_NAME);

		if (logs == null) {
			logs = new ArrayList<NginxLog>();
		}
		return logs;
	}

}
