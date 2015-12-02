package com.dianping.phoenix.lb.mainutil;

import com.dianping.phoenix.lb.dao.mongo.MongoModelStoreImpl;
import com.dianping.phoenix.lb.model.entity.SlbModelTree;
import com.dianping.phoenix.lb.model.entity.Strategy;
import com.mongodb.Mongo;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public class AddConsistentHash {

	private String url;

	private String db;

	public AddConsistentHash(String url, String db) {
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

		new AddConsistentHash(url, db).start();
	}

	private void start() throws UnknownHostException {

		String[] urlDetail = url.split(":");
		@SuppressWarnings("deprecation")
		MongoTemplate mongoTemplate = new MongoTemplate(
				new SimpleMongoDbFactory(new Mongo(urlDetail[0], Integer.parseInt(urlDetail[1])), db));

		Query query = new Query();
		query.fields().include(MongoModelStoreImpl.SLB_MODEL_STRATEGIES_KEY);

		SlbModelTree slbModelTree = mongoTemplate.findOne(query, SlbModelTree.class, "");

		Map<String, Strategy> strategies = slbModelTree.getStrategies();

		for (Strategy strategy : getStrategies()) {
			strategies.put(strategy.getName(), strategy);
		}

		mongoTemplate
				.updateFirst(new Query(), new Update().set(MongoModelStoreImpl.SLB_MODEL_STRATEGIES_KEY, strategies),
						SlbModelTree.class, "");

	}

	/**
	 * @return
	 */
	private List<Strategy> getStrategies() {

		List<Strategy> strategies = new ArrayList<Strategy>();

		Strategy consistent = new Strategy();
		consistent.setName("consistent_hash_arg_requestId");
		consistent.setType("consistent_hash");
		consistent.getDynamicAttributes().put("target", "$arg_requestId");

		strategies.add(consistent);

		Strategy rid = new Strategy();
		rid.setName("consistent_hash_rid");
		rid.setType("consistent_hash");
		rid.getDynamicAttributes().put("target", "$arg_rid");

		strategies.add(rid);

		return strategies;
	}
}
