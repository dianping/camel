package com.dianping.phoenix.lb.dao;

import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.*;

import java.util.List;
import java.util.Set;

/**
 * @author Leo Liang
 *
 */
public interface ModelStore {

	void init();

	Set<String> listVirtualServerNames();

	List<VirtualServer> listVirtualServers();

	List<VirtualServer> listVirtualServersWithNameAndGroup();

	List<Strategy> listStrategies();

	List<Pool> listPools();

	List<SlbPool> listSlbPools();

	List<Variable> listVariables();

	public List<Aspect> listCommonAspects();

	Strategy findStrategy(String name);

	VirtualServer findVirtualServer(String name);

	Pool findPool(String name);

	SlbPool findSlbPool(String name);

	Aspect findCommonAspect(String name);

	void updateOrCreateStrategy(String name, Strategy strategy) throws BizException;

	void removeStrategy(String name) throws BizException;

	void updateOrCreateSlbPool(String name, SlbPool slbPool) throws BizException;

	void saveVariables(List<Variable> variables) throws BizException;

	Variable findVariable(String key) throws BizException;

	void removeSlbPool(String name) throws BizException;

	void updateOrCreatePool(String name, Pool pool) throws BizException;

	void removePool(String name) throws BizException;

	void saveCommonAspects(List<Aspect> aspects) throws BizException;

	void updateVirtualServer(String name, VirtualServer virtualServer) throws BizException;

	void removeVirtualServer(String name) throws BizException;

	void addVirtualServer(String name, VirtualServer virtualServer) throws BizException;

	String tag(String name, int version, List<Pool> pools, List<Aspect> aspects, List<Variable> variables,
			List<Strategy> strategies) throws BizException;

	SlbModelTree getTag(String name, String tagId) throws BizException;

	List<String> listTagIdsDesc(String name) throws BizException;

	String findPrevTagId(String virtualServerName, String currentTagId) throws BizException;

	void removeTag(String virtualServerName, String tagId) throws BizException;

	String findLatestTagId(String virtualServerName) throws BizException;

	void cleanVirtualServerHistory(int documentLeft) throws BizException;

	List<MonitorRule> listMonitorRules();

	MonitorRule findMonitorRule(String ruleId);

	void updateOrCreateMonitorRule(String ruleId, MonitorRule monitorRule);

	void removeMonitorRule(String ruleId);

	void addIfNullStatusCode(StatusCode code);

	List<StatusCode> listStatusCode();

	void removeStatusCode(StatusCode statusCode);

	List<User> listUsers();

	User findUser(String accountName);

	void updateOrCreateUser(User user);

	void removeUser(String account);

	CmdbInfo findCmdbInfoByPoolName(String poolName);

	void addOrUpdateCmdbInfo(CmdbInfo cmdbInfo);

}