package com.dianping.phoenix.lb.dao.impl;

import com.dianping.phoenix.lb.dao.ModelStore;
import com.dianping.phoenix.lb.dao.VirtualServerDao;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * @author Leo Liang
 *
 */
@Service
public class VirtualServerDaoImpl extends AbstractDao implements VirtualServerDao {

	/**
	 * @param store
	 */
	@Autowired(required = true)
	public VirtualServerDaoImpl(ModelStore store) {
		super(store);
	}

	@Override
	public VirtualServer find(String virtualServerName) {
		return store.findVirtualServer(virtualServerName);
	}

	@Override
	public void add(VirtualServer virtualServer) throws BizException {
		store.addVirtualServer(virtualServer.getName(), virtualServer);
	}

	@Override
	public void update(VirtualServer virtualServer) throws BizException {
		store.updateVirtualServer(virtualServer.getName(), virtualServer);
	}

	@Override
	public Set<String> listNames() {
		return store.listVirtualServerNames();
	}

	@Override
	public List<VirtualServer> list() {
		return store.listVirtualServers();
	}

	@Override
	public void delete(String virtualServerName) throws BizException {
		store.removeVirtualServer(virtualServerName);
	}

	@Override
	public String tag(String virtualServerName, int virtualServerVersion, List<Pool> pools, List<Aspect> commonAspects,
			List<Variable> variables, List<Strategy> strategies) throws BizException {
		return store.tag(virtualServerName, virtualServerVersion, pools, commonAspects, variables, strategies);
	}

	@Override
	public List<String> listTags(String virtualServerName) throws BizException {
		return store.listTagIdsDesc(virtualServerName);
	}

	@Override
	public SlbModelTree findTagById(String virtualServerName, String tagId) throws BizException {
		return store.getTag(virtualServerName, tagId);
	}

	@Override
	public String findPrevTagId(String virtualServerName, String tagId) throws BizException {
		return store.findPrevTagId(virtualServerName, tagId);
	}

	@Override
	public void removeTag(String virtualServerName, String tagId) throws BizException {
		store.removeTag(virtualServerName, tagId);
	}

	@Override
	public String findLatestTagId(String virtualServerName) throws BizException {
		return store.findLatestTagId(virtualServerName);
	}

	@Override
	public List<VirtualServer> listWithNameAndGroup() {

		return store.listVirtualServersWithNameAndGroup();
	}

}
