package com.dianping.phoenix.lb.dao.mongo;

import com.dianping.phoenix.lb.api.lock.KeyLock;
import com.dianping.phoenix.lb.constant.MessageID;
import com.dianping.phoenix.lb.dao.ModelStore;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.*;
import com.dianping.phoenix.lb.utils.ExceptionUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/*-
 * mongo数据库存数数据
 * 
 * @author mengwenchao
 *
 * 2014年11月10日 上午11:52:33
 */
@Service
public class MongoModelStoreImpl extends AbstractDbStore implements ModelStore {

	public static final String SLB_MODEL_TAG = "m_tag";
	public static final String SLB_MODEL_VIRTUAL_SERVER_KEY = "m_virtualServers";
	public static final String SLB_MODEL_VIRTUAL_SERVER_NAME_KEY = "m_name";
	public static final String SLB_MODEL_VIRTUAL_SERVER_GROUP_KEY = "m_group";
	public static final String SLB_MODEL_STRATEGIES_KEY = "m_strategies";
	public static final String SLB_MODEL_POOLS_KEY = "m_pools";
	public static final String SLB_MODEL_ASPECTS_KEY = "m_aspects";
	public static final String SLB_MODEL_VARIABLES_KEY = "m_variables";
	public static final String SLB_MODEL_SLBPOOLS_KEY = "m_slbPools";
	public static final String SLB_MODEL_MONITORRULES_KEY = "m_monitorRules";
	public static final String SLB_MODEL_USERS_KEY = "m_users";
	public static final String SLB_MODEL_STATUS_CODE_KEY = "m_statusCodes";
	public static final String SLB_MODEL_CMDB_INFO_KEY = "m_cmdnInfos";
	private static final String SLB_BASE_COLLECTION_NAME = "slb_base";
	private static final String SLB_INDEX_COLLECTION_NAME = "system.indexes";
	/**
	 * 当前最新配置的版本号
	 */
	private static final int SLB_MODEL_INIT_TAG = 0;
	@Resource(name = "concurrentLock")
	private KeyLock singleKeyLock;
	@Resource(name = "mongoTemplateConfig")
	private MongoTemplate mongoTemplate;

	@Override
	public VirtualServer findVirtualServer(String vsName) {

		SlbModelTree slbModelTree = findSlbModelTree(vsName, SLB_MODEL_INIT_TAG);
		if (slbModelTree == null) {
			return null;
		}

		checkSlbModelTree(slbModelTree, vsName, SLB_MODEL_INIT_TAG);
		return (VirtualServer) slbModelTree.getVirtualServers().values().toArray()[0];
	}

	@Override
	protected VirtualServer findVirtualServerWithNameAndGroup(String vsName) {

		String[] keys = new String[] {
				getDocumentKey(SLB_MODEL_VIRTUAL_SERVER_KEY, vsName, SLB_MODEL_VIRTUAL_SERVER_NAME_KEY),
				getDocumentKey(SLB_MODEL_VIRTUAL_SERVER_KEY, vsName, SLB_MODEL_VIRTUAL_SERVER_GROUP_KEY) };

		SlbModelTree slbModelTree = findSlbModelTree(vsName, SLB_MODEL_INIT_TAG, keys);
		if (slbModelTree == null) {
			return null;
		}

		checkSlbModelTree(slbModelTree, vsName, SLB_MODEL_INIT_TAG);
		return (VirtualServer) slbModelTree.getVirtualServers().values().toArray()[0];
	}

	private void checkSlbModelTree(SlbModelTree slbModelTree, String vsName, int tag) {

		Validate.notNull(slbModelTree);
		if (slbModelTree.getVirtualServers().size() > 1) {
			throw new IllegalStateException(vsName + "-" + tag + "illegal, multi virtualservers");
		}
	}

	@Override
	protected void saveVirtualServer(VirtualServer virtualServer, int tag) {

		String name = virtualServer.getName();
		Query query = Query.query(Criteria.where(SLB_MODEL_TAG).is(tag));
		Update update = new Update();

		Map<String, VirtualServer> virtualServers = new HashMap<String, VirtualServer>();
		virtualServers.put(name, virtualServer);

		update.set(SLB_MODEL_VIRTUAL_SERVER_KEY, convertToMongoObject(virtualServers));

		mongoTemplate.updateFirst(query, update, SlbModelTree.class, name);
	}

	private Object convertToMongoObject(Object srcObject) {

		DBObject object = new BasicDBObject();
		mongoTemplate.getConverter().write(srcObject, object);
		return object;
	}

	@Override
	protected SlbModelTree findSlbModelTree(String vsName) {
		return findSlbModelTree(vsName, SLB_MODEL_INIT_TAG);
	}

	protected SlbModelTree findSlbModelTree(String collectionName, int tag, String... keys) {

		Query query = Query.query(Criteria.where(SLB_MODEL_TAG).is(tag));
		for (String key : keys) {
			query.fields().include(key);
		}
		SlbModelTree slbModelTree = mongoTemplate.findOne(query, SlbModelTree.class, collectionName);

		return slbModelTree;

	}

	@Override
	protected void saveVirtualServer(VirtualServer virtualServer) {
		saveVirtualServer(virtualServer, SLB_MODEL_INIT_TAG);
	}

	@Override
	protected Set<String> getVirtualServerNames() {
		Set<String> names = mongoTemplate.getCollectionNames();
		names.remove(SLB_BASE_COLLECTION_NAME);
		names.remove(SLB_INDEX_COLLECTION_NAME);
		names.remove(MongoAutoIncrementIdGenerator.COLLECTION_NAME);
		return names;
	}

	@Override
	public void removeVirtualServer(String name) throws BizException {
		if (!_virtualServerExists(name)) {
			ExceptionUtils.throwBizException(MessageID.VIRTUALSERVER_NOT_EXISTS, name);
		}
		mongoTemplate.dropCollection(name);
	}

	@Override
	protected void _addVirtualServer(String name, VirtualServer virtualServer) {

		createCollection(name);
		SlbModelTree slbModelTree = new SlbModelTree();
		slbModelTree.setTag(SLB_MODEL_INIT_TAG);
		slbModelTree.addVirtualServer(virtualServer);

		mongoTemplate.insert(slbModelTree, name);
	}

	private void createCollection(String collectionName) {

		if (mongoTemplate.collectionExists(collectionName)) {
			logger.warn("[createCollection][collection already exist]" + collectionName);
			return;
		}

		mongoTemplate.createCollection(collectionName);
		IndexDefinition index = new CompoundIndexDefinition(new BasicDBObject().append(SLB_MODEL_TAG, 1)).unique();
		mongoTemplate.indexOps(collectionName).ensureIndex(index);
	}

	@Override
	public List<Strategy> listStrategies() {

		SlbModelTree slbModelTree = findBaseSlbModelTree(SLB_MODEL_STRATEGIES_KEY);

		return valuesToList(slbModelTree.getStrategies());
	}

	private <T> List<T> valuesToList(Map<String, T> objects) {

		List<T> result = new LinkedList<T>();
		result.addAll(objects.values());
		return result;
	}

	private SlbModelTree findBaseSlbModelTree(String... includeFields) {

		Query query = new Query(Criteria.where(SLB_MODEL_TAG).is(SLB_MODEL_INIT_TAG));
		for (String field : includeFields) {
			query.fields().include(field);
		}
		List<SlbModelTree> slbModelTrees = mongoTemplate.find(query, SlbModelTree.class, SLB_BASE_COLLECTION_NAME);
		if (slbModelTrees.size() != 1) {
			throw new IllegalStateException("slb_base collection doc count:" + slbModelTrees.size());
		}
		return slbModelTrees.get(0);
	}

	@Override
	public List<Pool> listPools() {

		SlbModelTree slbModelTree = findBaseSlbModelTree(SLB_MODEL_POOLS_KEY);
		return valuesToList(slbModelTree.getPools());
	}

	@Override
	public List<SlbPool> listSlbPools() {

		SlbModelTree slbModelTree = findBaseSlbModelTree(SLB_MODEL_SLBPOOLS_KEY);
		return valuesToList(slbModelTree.getSlbPools());
	}

	@Override
	public List<Variable> listVariables() {

		SlbModelTree slbModelTree = findBaseSlbModelTree(SLB_MODEL_VARIABLES_KEY);
		return slbModelTree.getVariables();
	}

	@Override
	public List<Aspect> listCommonAspects() {

		SlbModelTree slbModelTree = findBaseSlbModelTree(SLB_MODEL_ASPECTS_KEY);
		return slbModelTree.getAspects();
	}

	@Override
	protected boolean _virtualServerExists(String name) {

		return mongoTemplate.collectionExists(name);
	}

	@Override
	public Strategy findStrategy(String name) {

		SlbModelTree slbModelTree = findBaseSlbModelTree(SLB_MODEL_STRATEGIES_KEY);
		return slbModelTree.getStrategies().get(name);
	}

	@Override
	public Pool findPool(String name) {

		SlbModelTree slbModelTree = findBaseSlbModelTree(getDocumentKey(SLB_MODEL_POOLS_KEY, name));
		return slbModelTree.getPools().get(name);
	}

	@Override
	public SlbPool findSlbPool(String name) {

		SlbModelTree slbModelTree = findBaseSlbModelTree(SLB_MODEL_SLBPOOLS_KEY);
		return slbModelTree.getSlbPools().get(name);
	}

	@Override
	public Aspect findCommonAspect(String name) {

		SlbModelTree slbModelTree = findBaseSlbModelTree(SLB_MODEL_ASPECTS_KEY);

		for (Aspect aspect : slbModelTree.getAspects()) {
			if (aspect.getName().equals(name)) {
				return aspect;
			}
		}
		return null;
	}

	@Override
	public Variable findVariable(String key) throws BizException {

		SlbModelTree slbModelTree = findBaseSlbModelTree(SLB_MODEL_VARIABLES_KEY);
		for (Variable variable : slbModelTree.getVariables()) {
			if (variable.getKey().equals(key)) {
				return variable;
			}
		}
		return null;
	}

	@Override
	protected void _updateOrCreateStrategy(String name, Strategy strategy) {

		Update update = new Update();
		update.set(getDocumentKey(SLB_MODEL_STRATEGIES_KEY, name), strategy);

		mongoTemplate.upsert(slbBaseQuery(), update, SLB_BASE_COLLECTION_NAME);

	}

	private Query slbBaseQuery() {
		return new Query(Criteria.where(SLB_MODEL_TAG).is(SLB_MODEL_INIT_TAG));
	}

	@Override
	public void removeStrategy(String name) {

		mongoTemplate.updateMulti(slbBaseQuery(), new Update().unset(getDocumentKey(SLB_MODEL_STRATEGIES_KEY, name)),
				SLB_BASE_COLLECTION_NAME);
	}

	private String getDocumentKey(String... paths) {

		List<String> replace = new LinkedList<String>();
		for (String path : paths) {
			replace.add(path.replace(".", MongoDbStartInitializer.MAP_KEY_DOT_REPLACEMENT));
		}
		return StringUtils.join(replace, ".");
	}

	@Override
	public void removeSlbPool(String name) throws BizException {

		mongoTemplate.updateMulti(slbBaseQuery(), new Update().unset(getDocumentKey(SLB_MODEL_SLBPOOLS_KEY, name)),
				SLB_BASE_COLLECTION_NAME);

	}

	@Override
	protected void _updateOrCreateSlbPool(String name, SlbPool slbPool) {

		mongoTemplate.upsert(slbBaseQuery(), new Update().set(getDocumentKey(SLB_MODEL_SLBPOOLS_KEY, name), slbPool),
				SLB_BASE_COLLECTION_NAME);
	}

	@Override
	protected void _saveVariables(List<Variable> variables) {
		mongoTemplate
				.upsert(slbBaseQuery(), new Update().set(SLB_MODEL_VARIABLES_KEY, variables), SLB_BASE_COLLECTION_NAME);
	}

	@Override
	public void updateOrCreatePool(String name, Pool pool) throws BizException {

		Pool current = findPool(name);
		if (current != null) {
			checkAndModifyVersion(current, pool);
			pool.setCreationDate(current.getCreationDate());
		} else {
			pool.setCreationDate(new Date());
		}
		pool.setLastModifiedDate(new Date());

		mongoTemplate.upsert(slbBaseQuery(), new Update().set(getDocumentKey(SLB_MODEL_POOLS_KEY, name), pool),
				SLB_BASE_COLLECTION_NAME);
	}

	@Override
	public void removePool(String name) throws BizException {

		mongoTemplate.upsert(slbBaseQuery(), new Update().unset(getDocumentKey(SLB_MODEL_POOLS_KEY, name)),
				SLB_BASE_COLLECTION_NAME);
	}

	@Override
	protected void _saveCommonAspects(List<Aspect> aspects) {

		mongoTemplate
				.upsert(slbBaseQuery(), new Update().set(SLB_MODEL_ASPECTS_KEY, aspects), SLB_BASE_COLLECTION_NAME);
	}

	@Override
	protected void _tag(String name, long tag, SlbModelTree tagSlbModelTree) {

		mongoTemplate.insert(tagSlbModelTree, name);

	}

	@Override
	public SlbModelTree getTag(String name, String tagId) throws BizException {

		Query query = new Query();
		long longTagId = convertFromStrTagId(name, tagId);
		query.addCriteria(Criteria.where(SLB_MODEL_TAG).is(longTagId));
		return mongoTemplate.findOne(query, SlbModelTree.class, name);
	}

	@Override
	public Long _queryTagCount(String name) throws BizException {
		Query query = new Query();
		query.fields().include(SLB_MODEL_TAG);

		return mongoTemplate.count(query, name);
	}

	@Override
	public List<String> _listTagIds(String name) throws BizException {

		Query query = new Query();
		query.fields().include(SLB_MODEL_TAG);
		query.with(new Sort(Direction.DESC, SLB_MODEL_TAG));
		List<SlbModelTree> slbModelTrees = mongoTemplate.find(query, SlbModelTree.class, name);

		List<String> tagIds = new LinkedList<String>();

		for (SlbModelTree slbModelTree : slbModelTrees) {
			if (slbModelTree.getTag() != SLB_MODEL_INIT_TAG) {
				tagIds.add(convertToStrTagId(name, slbModelTree.getTag()));
			}
		}

		return tagIds;
	}

	@Override
	protected String _findLatestTagId(String virtualServerName) {

		Query query = new Query();
		query.fields().include(SLB_MODEL_TAG);
		query.with(new Sort(Direction.DESC, SLB_MODEL_TAG));
		query.limit(1);
		List<SlbModelTree> slbModelTrees = mongoTemplate.find(query, SlbModelTree.class, virtualServerName);
		if (slbModelTrees.size() == 1) {
			long tag = slbModelTrees.get(0).getTag();
			if (tag != SLB_MODEL_INIT_TAG) {
				return convertToStrTagId(virtualServerName, tag);
			}
		}
		return null;
	}

	@Override
	protected void _removeTag(String virtualServerName, String tagId) {

		Long tag = convertFromStrTagId(virtualServerName, tagId);

		Query query = new Query();
		query.addCriteria(Criteria.where(SLB_MODEL_TAG).is(tag));
		mongoTemplate.remove(query, virtualServerName);
	}

	@Override
	public List<MonitorRule> listMonitorRules() {
		SlbModelTree slbModelTree = findBaseSlbModelTree(SLB_MODEL_MONITORRULES_KEY);

		return valuesToList(slbModelTree.getMonitorRules());
	}

	@Override
	public MonitorRule findMonitorRule(String ruleId) {
		SlbModelTree slbModelTree = findBaseSlbModelTree(SLB_MODEL_MONITORRULES_KEY);

		return slbModelTree.getMonitorRules().get(ruleId);
	}

	@Override
	public void updateOrCreateMonitorRule(String ruleId, MonitorRule monitorRule) {
		Update update = new Update();
		update.set(getDocumentKey(SLB_MODEL_MONITORRULES_KEY, ruleId), monitorRule);

		mongoTemplate.upsert(slbBaseQuery(), update, SLB_BASE_COLLECTION_NAME);
	}

	@Override
	public void removeMonitorRule(String ruleId) {
		mongoTemplate
				.updateMulti(slbBaseQuery(), new Update().unset(getDocumentKey(SLB_MODEL_MONITORRULES_KEY, ruleId)),
						SLB_BASE_COLLECTION_NAME);
	}

	@Override
	public List<User> listUsers() {
		SlbModelTree slbModelTree = findBaseSlbModelTree(SLB_MODEL_USERS_KEY);

		return valuesToList(slbModelTree.getUsers());
	}

	@Override
	public User findUser(String accountName) {
		SlbModelTree slbModelTree = findBaseSlbModelTree(SLB_MODEL_USERS_KEY);

		return slbModelTree.findUser(accountName);
	}

	@Override
	public void updateOrCreateUser(User user) {
		Update update = new Update();
		update.set(getDocumentKey(SLB_MODEL_USERS_KEY, user.getAccount()), user);

		mongoTemplate.upsert(slbBaseQuery(), update, SLB_BASE_COLLECTION_NAME);
	}

	@Override
	public void removeUser(String account) {
		mongoTemplate.updateMulti(slbBaseQuery(), new Update().unset(getDocumentKey(SLB_MODEL_USERS_KEY, account)),
				SLB_BASE_COLLECTION_NAME);
	}

	@Override
	public void addIfNullStatusCode(StatusCode statusCode) {
		Update update = new Update();
		update.set(getDocumentKey(SLB_MODEL_STATUS_CODE_KEY, statusCode.getId()), statusCode);

		mongoTemplate.upsert(slbBaseQuery(), update, SLB_BASE_COLLECTION_NAME);
	}

	@Override
	public List<StatusCode> listStatusCode() {
		SlbModelTree slbModelTree = findBaseSlbModelTree(SLB_MODEL_STATUS_CODE_KEY);

		return valuesToList(slbModelTree.getStatusCodes());
	}

	@Override
	public void removeStatusCode(StatusCode statusCode) {
		mongoTemplate.updateMulti(slbBaseQuery(),
				new Update().unset(getDocumentKey(SLB_MODEL_STATUS_CODE_KEY, statusCode.getId())),
				SLB_BASE_COLLECTION_NAME);
	}

	@Override
	public CmdbInfo findCmdbInfoByPoolName(String poolName) {
		SlbModelTree slbModelTree = findBaseSlbModelTree(getDocumentKey(SLB_MODEL_CMDB_INFO_KEY, poolName));

		return slbModelTree.findCmdbInfo(poolName);
	}

	@Override
	public void addOrUpdateCmdbInfo(CmdbInfo cmdbInfo) {
		Update update = new Update();

		update.set(getDocumentKey(SLB_MODEL_CMDB_INFO_KEY, cmdbInfo.getId()), cmdbInfo);
		mongoTemplate.upsert(slbBaseQuery(), update, SLB_BASE_COLLECTION_NAME);
	}

}
