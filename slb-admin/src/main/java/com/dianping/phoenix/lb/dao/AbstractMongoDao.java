package com.dianping.phoenix.lb.dao;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public abstract class AbstractMongoDao {

	protected Object convertToMongoObject(Object srcObject) {
		DBObject object = new BasicDBObject();

		getTemplate().getConverter().write(srcObject, object);
		return object;
	}

	protected void createIfNullCollection(String collectionName) {
		createIfNullCollection(collectionName, null, null);
	}

	protected void createIfNullCollection(String collectionName, CollectionOptions options, Index index) {
		if (getTemplate().collectionExists(collectionName)) {
			return;
		}
		if (options != null) {
			getTemplate().createCollection(collectionName, options);
		} else {
			getTemplate().createCollection(collectionName);
		}
		if (index != null) {
			getTemplate().indexOps(collectionName).ensureIndex(index);
		}
	}

	protected abstract MongoTemplate getTemplate();

}
