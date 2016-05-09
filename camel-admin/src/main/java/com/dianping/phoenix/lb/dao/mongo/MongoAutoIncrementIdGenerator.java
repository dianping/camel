package com.dianping.phoenix.lb.dao.mongo;

import com.dianping.phoenix.lb.api.dao.AutoIncrementIdGenerator;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年11月13日 上午9:48:38
 */
@Component(value = "mongoAutoIncrementIdGenerator")
public class MongoAutoIncrementIdGenerator implements AutoIncrementIdGenerator {

	public static final String COLLECTION_NAME = "AUTOINCREMENT_ID";
	private static final String ID_KEY = "idKey";
	private static final String ID_VALUE = "idValue";
	@Resource(name = "mongoTemplateConfig")
	private MongoTemplate mongoTemplate;

	@Override
	public long getNextId(String idKey) {

		Query query = new Query(Criteria.where(ID_KEY).is(idKey));
		Update update = new Update();
		update.inc(ID_VALUE, 1);

		GenerateId newId = mongoTemplate
				.findAndModify(query, update, new FindAndModifyOptions().upsert(true).returnNew(true), GenerateId.class,
						COLLECTION_NAME);
		return newId.getIdValue();
	}

	public void clear(String idKey) {

		mongoTemplate
				.findAllAndRemove(Query.query(Criteria.where(ID_KEY).is(idKey)), GenerateId.class, COLLECTION_NAME);
	}

	@PostConstruct
	public void createRelatedResources() {

		if (!mongoTemplate.collectionExists(COLLECTION_NAME)) {
			mongoTemplate.createCollection(COLLECTION_NAME);
		}
	}

	private static class GenerateId {

		@Id
		private String idKey;

		private long idValue;

		public long getIdValue() {
			return idValue;
		}

	}
}
