package com.dianping.phoenix.lb.mainutil;

import com.dianping.phoenix.lb.dao.mongo.MongoModelStoreImpl;
import com.dianping.phoenix.lb.model.entity.Instance;
import com.dianping.phoenix.lb.model.entity.SlbModelTree;
import com.dianping.phoenix.lb.model.entity.SlbPool;
import com.mongodb.Mongo;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.net.UnknownHostException;
import java.util.Map;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年12月5日 下午6:24:37
 */
public class ChangeMongoForLocal {

	private String url;

	private String db;

	public ChangeMongoForLocal(String url, String db) {
		this.url = url;
		this.db = db;
	}

	public static void main(String[] argc) throws UnknownHostException {

		String url = "";
		String db = "";

		if (argc.length >= 1) {
			url = argc[0];
		}
		if (argc.length >= 2) {
			db = argc[1];
		}

		new ChangeMongoForLocal(url, db).start();
	}

	private void start() throws UnknownHostException {

		String[] urlDetail = url.split(":");
		@SuppressWarnings("deprecation")
		MongoTemplate mongoTemplate = new MongoTemplate(
				new SimpleMongoDbFactory(new Mongo(urlDetail[0], Integer.parseInt(urlDetail[1])), db));

		Query query = new Query();
		query.fields().include(MongoModelStoreImpl.SLB_MODEL_SLBPOOLS_KEY);

		SlbModelTree slbModelTree = mongoTemplate.findOne(query, SlbModelTree.class, "");

		//change slbmodeltree
		Map<String, SlbPool> pools = slbModelTree.getSlbPools();
		for (String poolName : pools.keySet()) {
			SlbPool slbPool = pools.get(poolName);

			slbPool.getInstances().clear();
			Instance instance = new Instance();
			instance.setIp("127.0.0.1");
			slbPool.getInstances().add(instance);

			pools.put(poolName, slbPool);
		}

		mongoTemplate.updateFirst(new Query(), new Update().set(MongoModelStoreImpl.SLB_MODEL_SLBPOOLS_KEY, pools),
				SlbModelTree.class, "");

	}
}
