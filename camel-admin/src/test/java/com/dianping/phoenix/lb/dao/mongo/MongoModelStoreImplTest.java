package com.dianping.phoenix.lb.dao.mongo;

import com.dianping.phoenix.lb.dao.AbstractModelStoreTest;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.SlbModelTree;
import com.dianping.phoenix.lb.model.entity.SlbPool;
import com.dianping.phoenix.lb.model.entity.VirtualServer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年11月17日 上午11:57:21
 */
public class MongoModelStoreImplTest extends AbstractModelStoreTest {

	private MongoTemplate mongoTemplate;

	@Before
	public void beforeMongoModelStoreImplTest() throws Exception {

		store = getApplicationContext().getBean(MongoModelStoreImpl.class);
		mongoTemplate = (MongoTemplate) getApplicationContext().getBean("mongoTemplateConfig");

	}

	@Test
	public void cleanBadData() {

		//		List<SlbPool> slbPools = store.listSlbPools();
		Map<String, SlbPool> slbPools = new HashMap<String, SlbPool>();
		for (SlbPool slbPool : store.listSlbPools()) {
			slbPools.put(slbPool.getName(), slbPool);
		}

		for (VirtualServer vs : store.listVirtualServers()) {
			if (slbPools.get(vs.getSlbPool()) == null) {
				System.out.println(vs.getName());
			}
		}

	}

	@Override
	protected void assertRawFileNotChanged(String vsName) throws IOException {

	}

	@Override
	protected void assertEquals(SlbModelTree wwwSlbModelTree, String vsName) throws SAXException, IOException {

	}

	@Override
	protected boolean vsExistsInStore(String vsName) {

		return store.findVirtualServer(vsName) != null;
	}

	@Override
	protected SlbModelTree getTagSlbModelTree(String vs, int tag) throws SAXException, IOException {

		return mongoTemplate.findOne(new Query().addCriteria(Criteria.where(MongoModelStoreImpl.SLB_MODEL_TAG).is(tag)),
				SlbModelTree.class, vs);
	}

	@Override
	protected boolean tagExist(String vs, int tag) {

		Query query = new Query();
		query.addCriteria(Criteria.where(MongoModelStoreImpl.SLB_MODEL_TAG).is(tag));
		long count = mongoTemplate.count(query, vs);
		return count == 1;
	}

	@Override
	protected void prepareVirtualServerAtTag(String vs, int initTag, String date, int newTag)
			throws IOException, BizException {

		store.tag(vs, initTag, store.listPools(), store.listCommonAspects(), store.listVariables(),
				store.listStrategies());
	}

	@Override
	protected void initStore() {

	}

	@Override
	protected boolean tagExists(String vsName, String date, int tagId) {

		return mongoTemplate
				.exists(new Query().addCriteria(Criteria.where(MongoModelStoreImpl.SLB_MODEL_TAG).is(tagId)), vsName);
	}
}
