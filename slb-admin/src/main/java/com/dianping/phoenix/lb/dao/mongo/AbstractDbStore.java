package com.dianping.phoenix.lb.dao.mongo;

import com.dianping.phoenix.lb.api.Versionable;
import com.dianping.phoenix.lb.api.dao.AutoIncrementIdGenerator;
import com.dianping.phoenix.lb.constant.MessageID;
import com.dianping.phoenix.lb.dao.AbstractStore;
import com.dianping.phoenix.lb.dao.ModelStore;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.*;
import com.dianping.phoenix.lb.utils.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年11月13日 下午3:41:28
 */
public abstract class AbstractDbStore extends AbstractStore implements ModelStore {

	@Autowired
	private AutoIncrementIdGenerator idGenerator;

	/**
	 * @param variables
	 * @param commonAspects
	 * @param pools
	 * @param currentVersion
	 * @param tag
	 * @return
	 */
	public static SlbModelTree createSlbModelTree(VirtualServer virtualServer, List<Pool> pools,
			List<Aspect> commonAspects, List<Variable> variables, List<Strategy> strategies, long tag) {

		SlbModelTree tagSlbModelTree = new SlbModelTree();

		for (Strategy strategy : strategies) {
			tagSlbModelTree.addStrategy(strategy);
		}

		for (Pool pool : pools) {
			tagSlbModelTree.addPool(pool);
		}

		if (commonAspects != null) {
			for (Aspect aspect : commonAspects) {
				tagSlbModelTree.addAspect(aspect);
			}
		}
		if (variables != null) {
			for (Variable variable : variables) {
				tagSlbModelTree.addVariable(variable);
			}
		}
		tagSlbModelTree.addVirtualServer(virtualServer);
		tagSlbModelTree.setTag(tag);
		tagSlbModelTree.setTagDate(new Date());

		return tagSlbModelTree;
	}

	public static SlbModelTree createSlbModelTree(VirtualServer virtualServer, List<Pool> pools,
			List<Aspect> commonAspects, List<Variable> variables, List<Strategy> strategies, String tagId) {

		long tag = convertFromStrTagId(virtualServer.getName(), tagId);
		return createSlbModelTree(virtualServer, pools, commonAspects, variables, strategies, tag);
	}

	@Override
	public void init() {

	}

	@Override
	public Set<String> listVirtualServerNames() {
		return getVirtualServerNames();
	}

	@Override
	public List<VirtualServer> listVirtualServers() {

		Set<String> vsNames = getVirtualServerNames();
		List<VirtualServer> vss = new LinkedList<VirtualServer>();
		for (String vsName : vsNames) {
			VirtualServer vs = findVirtualServer(vsName);
			if (vs == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("[listVirtualServers][vs null]" + vsName);
				}
				continue;
			}
			vss.add(vs);
		}
		return vss;
	}

	@Override
	public List<VirtualServer> listVirtualServersWithNameAndGroup() {

		Set<String> vsNames = getVirtualServerNames();
		List<VirtualServer> vss = new LinkedList<VirtualServer>();
		for (String vsName : vsNames) {
			VirtualServer vs = findVirtualServerWithNameAndGroup(vsName);
			if (vs == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("[listVirtualServers][vs null]" + vsName);
				}
				continue;
			}
			vss.add(vs);
		}
		return vss;

	}

	protected abstract VirtualServer findVirtualServerWithNameAndGroup(String vsName);

	@Override
	public void cleanVirtualServerHistory(int documentLeft) throws BizException {
		Set<String> vsNames = getVirtualServerNames();

		for (String vsName : vsNames) {
			Long count = _queryTagCount(vsName);

			if (count > documentLeft) {
				List<String> tagIds = listTagIdsDesc(vsName);
				for (int i = documentLeft; i < tagIds.size(); i++) {
					removeTag(vsName, tagIds.get(i));
				}
			}
		}
	}

	protected abstract Set<String> getVirtualServerNames();

	@Override
	public void updateOrCreateStrategy(String name, Strategy strategy) throws BizException {

		Strategy current = findStrategy(name);

		if (current != null) {
			checkAndModifyVersion(current, strategy);
			strategy.setCreationDate(current.getCreationDate());
		} else {
			strategy.setCreationDate(new Date());
		}

		strategy.setLastModifiedDate(new Date());
		_updateOrCreateStrategy(name, strategy);
	}

	protected abstract void _updateOrCreateStrategy(String name, Strategy strategy);

	@Override
	public void updateOrCreateSlbPool(String name, SlbPool slbPool) throws BizException {

		SlbPool current = findSlbPool(name);
		if (current != null) {
			checkAndModifyVersion(current, slbPool);
		}

		_updateOrCreateSlbPool(name, slbPool);
	}

	protected abstract void _updateOrCreateSlbPool(String name, SlbPool slbPool);

	@Override
	public void saveVariables(List<Variable> variables) throws BizException {

		Map<String, Variable> originalVariables = new HashMap<String, Variable>();
		for (Variable v : listVariables()) {
			originalVariables.put(v.getKey(), v);
		}
		for (Variable variable : variables) {
			Variable currentVersion = originalVariables.get(variable.getKey());
			if (currentVersion != null) {
				checkAndModifyVersion(currentVersion, variable);
			}
		}

		_saveVariables(variables);
	}

	protected abstract void _saveVariables(List<Variable> variables);

	@Override
	public void saveCommonAspects(List<Aspect> aspects) throws BizException {
		Map<String, Aspect> originalAspects = new HashMap<String, Aspect>();
		for (Aspect a : listCommonAspects()) {
			originalAspects.put(a.getName(), a);
		}

		for (Aspect aspect : aspects) {

			Aspect currentVersion = originalAspects.get(aspect.getName());
			if (currentVersion != null) {
				checkAndModifyVersion(currentVersion, aspect);
			}
		}

		_saveCommonAspects(aspects);
	}

	protected abstract void _saveCommonAspects(List<Aspect> aspects);

	@Override
	public String tag(String name, int version, List<Pool> pools, List<Aspect> commonAspects, List<Variable> variables,
			List<Strategy> strategies) throws BizException {

		VirtualServer currentVersion = findVirtualServer(name);
		if (currentVersion == null) {
			ExceptionUtils.throwBizException(MessageID.VIRTUALSERVER_NOT_EXISTS, name);
		}

		if (currentVersion.getVersion() != version) {
			ExceptionUtils.logAndRethrowBizException(new ConcurrentModificationException(),
					MessageID.VIRTUALSERVER_CONCURRENT_MOD, name);
		}

		long tag = idGenerator.getNextId(name);

		SlbModelTree tagSlbModelTree = createSlbModelTree(currentVersion, pools, commonAspects, variables, strategies,
				tag);

		_tag(name, tag, tagSlbModelTree);
		return convertToStrTagId(name, tag);
	}

	protected abstract void _tag(String name, long tag, SlbModelTree tagSlbModelTree);

	@Override
	public List<String> listTagIdsDesc(String name) throws BizException {

		if (!_virtualServerExists(name)) {
			ExceptionUtils.throwBizException(MessageID.VIRTUALSERVER_NOT_EXISTS, name);
		}

		return _listTagIds(name);
	}

	protected abstract List<String> _listTagIds(String name) throws BizException;

	protected abstract Long _queryTagCount(String name) throws BizException;

	@Override
	public String findPrevTagId(String virtualServerName, String currentTagId) throws BizException {

		if (!_virtualServerExists(virtualServerName)) {
			ExceptionUtils.throwBizException(MessageID.VIRTUALSERVER_NOT_EXISTS, virtualServerName);
		}

		List<String> tagIds = listTagIdsDesc(virtualServerName);
		if (tagIds != null && !tagIds.isEmpty()) {
			return doFindPrevTagId(virtualServerName, currentTagId, tagIds);
		}
		return null;
	}

	@Override
	public String findLatestTagId(String virtualServerName) throws BizException {

		if (!_virtualServerExists(virtualServerName)) {
			ExceptionUtils.throwBizException(MessageID.VIRTUALSERVER_NOT_EXISTS, virtualServerName);
		}

		return _findLatestTagId(virtualServerName);
	}

	protected abstract String _findLatestTagId(String virtualServerName);

	@Override
	public void removeTag(String virtualServerName, String tagId) throws BizException {
		if (!_virtualServerExists(virtualServerName)) {
			ExceptionUtils.throwBizException(MessageID.VIRTUALSERVER_NOT_EXISTS, virtualServerName);
		}
		if (logger.isInfoEnabled()) {
			logger.info("[removeTag]" + virtualServerName + ":" + tagId);
		}
		_removeTag(virtualServerName, tagId);
	}

	protected abstract void _removeTag(String virtualServerName, String tagId);

	@Override
	public void updateVirtualServer(String name, VirtualServer virtualServer) throws BizException {

		SlbModelTree originalSlbModelTree = findSlbModelTree(virtualServer.getName());
		if (originalSlbModelTree == null) {
			ExceptionUtils.throwBizException(MessageID.VIRTUALSERVER_NOT_EXISTS, name);
		}

		VirtualServer originalVirtualServer = originalSlbModelTree.findVirtualServer(virtualServer.getName());

		checkAndModifyVersion(originalVirtualServer, virtualServer);

		virtualServer.setCreationDate(originalVirtualServer.getCreationDate());
		virtualServer.setLastModifiedDate(new Date());

		saveVirtualServer(virtualServer);
	}

	protected abstract SlbModelTree findSlbModelTree(String vsName);

	protected abstract void saveVirtualServer(VirtualServer originalVirtualServer);

	protected abstract SlbModelTree findSlbModelTree(String vsName, int slbModelInitTag, String... keys);

	protected abstract void saveVirtualServer(VirtualServer virtualServer, int modelVersion);

	protected void checkAndModifyVersion(Object currentVersion, Object newVersion) {

		if (!(currentVersion instanceof Versionable && newVersion instanceof Versionable)) {
			return;
		}

		Versionable current = (Versionable) currentVersion, new_ = (Versionable) newVersion;
		if (current.getVersion() != new_.getVersion()) {

			throw new ConcurrentModificationException(
					"currentVersion:" + current.getVersion() + ", your saved version:" + new_.getVersion());
		}

		new_.setVersion(current.getVersion() + 1);
	}

	@Override
	public void addVirtualServer(String name, VirtualServer virtualServer) throws BizException {

		if (_virtualServerExists(name)) {
			ExceptionUtils.throwBizException(MessageID.VIRTUALSERVER_ALREADY_EXISTS, name);
		}

		Date now = new Date();
		virtualServer.setVersion(1);
		virtualServer.setCreationDate(now);
		virtualServer.setLastModifiedDate(now);

		_addVirtualServer(name, virtualServer);
	}

	/**
	 * @param name
	 * @return
	 */
	protected abstract boolean _virtualServerExists(String name);

	protected abstract void _addVirtualServer(String name, VirtualServer virtualServer);

}
