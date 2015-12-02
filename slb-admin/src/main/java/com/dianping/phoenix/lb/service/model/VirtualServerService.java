/**
 * Project: phoenix-load-balancer
 * <p/>
 * File Created at 2013-10-17
 */
package com.dianping.phoenix.lb.service.model;

import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.VirtualServerGroup;
import com.dianping.phoenix.lb.model.entity.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Leo Liang
 *
 */
public interface VirtualServerService {
	List<VirtualServer> listVirtualServers();

	List<VirtualServer> listVirtualServersWithNameAndGroup();

	VirtualServer findVirtualServer(String virtualServerName) throws BizException;

	void addVirtualServer(String virtualServerName, VirtualServer virtualServer) throws BizException;

	void deleteVirtualServer(String virtualServerName) throws BizException;

	void modifyVirtualServer(String virtualServerName, VirtualServer virtualServer) throws BizException;

	String generateNginxConfig(VirtualServer virtualServer, List<Pool> pools, List<Aspect> commonAspects,
			List<Variable> variables, List<Strategy> strategies) throws BizException;

	/**
	 * 根据历史tag的SlbModelTree生成nginx配置
	 * <br/>注意：内部只能有一个vs，如果含有多个vs，会报异常
	 * @param virtualServer
	 * @return
	 * @throws BizException
	 */
	String generateNginxConfig(SlbModelTree slbModelTree) throws BizException;

	String tag(String virtualServerName, int virtualServerVersion) throws BizException;

	SlbModelTree findTagById(String virtualServerName, String tagId) throws BizException;

	String findPrevTagId(String virtualServerName, String tagId) throws BizException;

	void removeTag(String virtualServerName, String tagId) throws BizException;

	String findLatestTagId(String virtualServerName) throws BizException;

	List<String> listTag(String virtualServerName, int maxNum) throws BizException;

	List<String> findVirtualServerByPool(String poolName) throws BizException;

	Map<String, VirtualServerGroup> listGroups();

	Set<String> findUndeleteDengineVSNames(String host) throws BizException;

	Set<String> listVSNames() throws BizException;

}
