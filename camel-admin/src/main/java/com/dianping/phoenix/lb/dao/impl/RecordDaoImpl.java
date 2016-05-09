package com.dianping.phoenix.lb.dao.impl;

import com.dianping.phoenix.lb.dao.AbstractMongoDao;
import com.dianping.phoenix.lb.dao.RecordDao;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Service
public class RecordDaoImpl extends AbstractMongoDao implements RecordDao {

	private static final String SLB_RECORD_COLLECTION_NAME = "record";
	protected final Logger logger = LoggerFactory.getLogger(getClass());
	@Resource(name = "recordMongoTemplate")
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
			Index index = new Index().on("date", Direction.DESC);
			CollectionOptions options = new CollectionOptions(1 * 1024 * 1024 * 1024, 100 * 365, true);

			createIfNullCollection(SLB_RECORD_COLLECTION_NAME, options, index);
		} catch (Exception ex) {
			logger.error("create colection failed: " + SLB_RECORD_COLLECTION_NAME, ex);
		}
	}

	@Override
	public void addRecord(Record record) throws BizException {
		m_mongoTemplate.insert(record, SLB_RECORD_COLLECTION_NAME);
	}

}
