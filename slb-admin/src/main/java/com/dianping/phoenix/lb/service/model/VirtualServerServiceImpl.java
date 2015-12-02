package com.dianping.phoenix.lb.service.model;

import com.dianping.phoenix.lb.api.manager.CacheManager;
import com.dianping.phoenix.lb.api.processor.PreprocessBeforeGenerateNginxConfig;
import com.dianping.phoenix.lb.constant.Constants;
import com.dianping.phoenix.lb.constant.MessageID;
import com.dianping.phoenix.lb.dao.*;
import com.dianping.phoenix.lb.dao.mongo.AbstractDbStore;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.VirtualServerGroup;
import com.dianping.phoenix.lb.model.entity.*;
import com.dianping.phoenix.lb.service.ConcurrentControlServiceTemplate;
import com.dianping.phoenix.lb.service.GitService;
import com.dianping.phoenix.lb.service.NginxService;
import com.dianping.phoenix.lb.service.NginxService.NginxCheckResult;
import com.dianping.phoenix.lb.utils.ExceptionUtils;
import com.dianping.phoenix.lb.utils.MessageUtils;
import com.dianping.phoenix.lb.utils.PoolNameUtils;
import com.dianping.phoenix.lb.velocity.TemplateManager;
import com.dianping.phoenix.lb.velocity.VelocityEngineManager;
import com.dianping.phoenix.lb.visitor.NginxConfigVisitor;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.unidal.tuple.Pair;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * t
 *
 * @author Leo Liang
 *
 */
@Service
public class VirtualServerServiceImpl extends ConcurrentControlServiceTemplate implements VirtualServerService {

	private VirtualServerDao virtualServerDao;

	private StrategyDao strategyDao;

	private PoolDao poolDao;

	private SlbPoolDao slbPoolDao;

	private NginxService nginxService;

	private CommonAspectDao commonAspectDao;

	private VariableDao variableDao;

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private List<PreprocessBeforeGenerateNginxConfig> preprocessBeforeGenerateNginxConfigs;

	private LoadingCache<TagCacheEntryKey, TagCacheEntryValue> tagCache;

	private int cacheCount = 100;

	private List<Pattern> m_poolPatterns = Arrays.asList(Pattern.compile("proxy_pass\\s+http://([^$]+?);"));

	@Autowired(required = true)
	public VirtualServerServiceImpl(VirtualServerDao virtualServerDao, StrategyDao strategyDao, PoolDao poolDao,
			SlbPoolDao slbPoolDao, VariableDao variableDao, CommonAspectDao commonAspectDao, NginxService nginxService,
			GitService gitService) throws ComponentLookupException {
		super();
		this.virtualServerDao = virtualServerDao;
		this.strategyDao = strategyDao;
		this.poolDao = poolDao;
		this.slbPoolDao = slbPoolDao;
		this.variableDao = variableDao;
		this.nginxService = nginxService;
		this.commonAspectDao = commonAspectDao;
	}

	@PostConstruct
	public void init() throws ComponentLookupException, BizException {

		tagCache = cacheManager.createGuavaCache(cacheCount, new CacheLoader<TagCacheEntryKey, TagCacheEntryValue>() {

					@Override
					public TagCacheEntryValue load(TagCacheEntryKey key) throws Exception {

						if (logger.isInfoEnabled()) {
							logger.info("[load]" + key);
						}
						SlbModelTree slbModelTree = virtualServerDao.findTagById(key.getVsName(), key.getTagId());
						String nginxConfig = null;
						if (slbModelTree != null) {
							nginxConfig = getNginxConfig(slbModelTree);
						}
						VirtualServer vs = new ArrayList<VirtualServer>(slbModelTree.getVirtualServers().values())
								.get(0);

						return new TagCacheEntryValue(nginxConfig, slbModelTree, vs.getSslCertificate(),
								vs.getSslCertificateKey());
					}

				});

	}

	/**
	 * @param strategyDao
	 *            the strategyDao to set
	 */
	public void setStrategyDao(StrategyDao strategyDao) {
		this.strategyDao = strategyDao;
	}

	/**
	 * @param virtualServerDao
	 *            the virtualServerDao to set
	 */
	public void setVirtualServerDao(VirtualServerDao virtualServerDao) {
		this.virtualServerDao = virtualServerDao;
	}

	public List<String> findVirtualServerByPool(String poolName) throws BizException {
		final String poolNamePrefix = PoolNameUtils.getPoolNamePrefix(poolName);
		try {
			return read(new ReadOperation<List<String>>() {
				@Override
				public List<String> doRead() throws Exception {
					List<String> vsNames = new ArrayList<String>();

					for (VirtualServer vs : virtualServerDao.list()) {
						if (StringUtils.equals(vs.getDefaultPoolName(), poolNamePrefix)) {
							vsNames.add(vs.getName());
							continue;
						}

						boolean isLocationMath = false;

						for (Location location : vs.getLocations()) {
							if (isLocationMath) {
								break;
							}
							for (Directive directive : location.getDirectives()) {
								boolean isDirectiveMatch = false;

								if (Constants.DIRECTIVE_PROXY_PASS.equals(directive.getType())) {
									isDirectiveMatch = StringUtils.equals(directive
													.getDynamicAttribute(Constants.DIRECTIVE_PROXY_PASS_POOL_NAME),
											poolNamePrefix);
								} else if (Constants.DIRECTIVE_CUSTOM.equals(directive.getType())) {
									String valueAttribute = directive.getDynamicAttribute(Constants.DIRECTIVE_VALUE);
									String pool = PoolNameUtils.extractPoolNameFromProxyPassString(valueAttribute);

									if (pool.equals(poolNamePrefix)) {
										isDirectiveMatch = true;
									}
								}
								if (isDirectiveMatch) {
									vsNames.add(vs.getName());
									isLocationMath = true;
									break;
								}
							}
						}
					}
					return vsNames;
				}

			});
		} catch (BizException e) {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.dianping.phoenix.lb.service.VirtualServerService#listVirtualServers()
	 */
	@Override
	public List<VirtualServer> listVirtualServers() {
		try {
			return read(new ReadOperation<List<VirtualServer>>() {

				@Override
				public List<VirtualServer> doRead() throws Exception {
					return virtualServerDao.list();
				}
			});
		} catch (BizException e) {
			return null;
		}
	}

	@Override
	public List<VirtualServer> listVirtualServersWithNameAndGroup() {

		try {
			return read(new ReadOperation<List<VirtualServer>>() {

				@Override
				public List<VirtualServer> doRead() throws Exception {
					return virtualServerDao.listWithNameAndGroup();
				}
			});
		} catch (BizException e) {
			return null;
		}
	}

	@Override
	public Map<String, VirtualServerGroup> listGroups() {
		Map<String, VirtualServerGroup> groups = new LinkedHashMap<String, VirtualServerGroup>();
		List<VirtualServer> vsList = this.listVirtualServersWithNameAndGroup();

		for (VirtualServer vs : vsList) {
			String name = vs.getGroup();
			if (StringUtils.isBlank(name)) {
				name = MessageUtils.getMessage(MessageID.GROUP_NAME_DEFAULT);
			}
			VirtualServerGroup group = groups.get(name);
			if (group == null) {
				group = new VirtualServerGroup();
				group.setName(name);
				group.setVirtualServers(new ArrayList<VirtualServer>());
				groups.put(name, group);
			}
			group.getVirtualServers().add(vs);
		}
		return groups;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.dianping.phoenix.lb.service.VirtualServerService#findVirtualServer
	 * (java.lang.String)
	 */
	@Override
	public VirtualServer findVirtualServer(final String virtualServerName) throws BizException {
		if (StringUtils.isBlank(virtualServerName)) {
			ExceptionUtils.throwBizException(MessageID.VIRTUALSERVER_NAME_EMPTY);
		}

		return read(new ReadOperation<VirtualServer>() {

			@Override
			public VirtualServer doRead() throws BizException {
				return virtualServerDao.find(virtualServerName);
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.dianping.phoenix.lb.service.VirtualServerService#addVirtualServer
	 * (java.lang.String,
	 * com.dianping.phoenix.lb.model.configure.entity.VirtualServer)
	 */
	@Override
	public void addVirtualServer(String virtualServerName, final VirtualServer virtualServer) throws BizException {
		if (virtualServerName == null || virtualServer == null) {
			return;
		}

		if (!virtualServerName.equals(virtualServer.getName())) {
			return;
		}

		validate(virtualServer);

		write(new WriteOperation<Void>() {

			@Override
			public Void doWrite() throws Exception {
				virtualServerDao.add(virtualServer);
				return null;
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.dianping.phoenix.lb.service.VirtualServerService#deleteVirtualServer
	 * (java.lang.String)
	 */
	@Override
	public void deleteVirtualServer(final String virtualServerName) throws BizException {
		if (StringUtils.isBlank(virtualServerName)) {
			ExceptionUtils.throwBizException(MessageID.VIRTUALSERVER_NAME_EMPTY);
		}

		try {
			write(new WriteOperation<Void>() {

				@Override
				public Void doWrite() throws Exception {
					virtualServerDao.delete(virtualServerName);
					return null;
				}
			});
		} catch (BizException e) {
			// ignore
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.dianping.phoenix.lb.service.VirtualServerService#modifyVirtualServer
	 * (java.lang.String,
	 * com.dianping.phoenix.lb.model.configure.entity.VirtualServer)
	 */
	@Override
	public void modifyVirtualServer(final String virtualServerName, final VirtualServer virtualServer)
			throws BizException {
		if (virtualServerName == null || virtualServer == null) {
			return;
		}

		if (!virtualServerName.equals(virtualServer.getName())) {
			return;
		}

		validate(virtualServer);

		write(new WriteOperation<Void>() {

			@Override
			public Void doWrite() throws Exception {
				virtualServerDao.update(virtualServer);
				return null;
			}
		});

	}

	private void validate(VirtualServer virtualServer) throws BizException {

		if (StringUtils.isBlank(virtualServer.getName())) {
			ExceptionUtils.throwBizException(MessageID.VIRTUALSERVER_NAME_EMPTY);
		}

		if (virtualServer.getPort() == null) {
			ExceptionUtils.throwBizException(MessageID.VIRTUALSERVER_PORT_EMPTY);
		}

		if (StringUtils.isBlank(virtualServer.getDefaultPoolName())) {
			ExceptionUtils.throwBizException(MessageID.VIRTUALSERVER_NO_DEFAULT_POOL_NAME);
		}

		if (poolDao.find(virtualServer.getDefaultPoolName()) == null) {
			ExceptionUtils.throwBizException(MessageID.VIRTUALSERVER_DEFAULTPOOL_NOT_EXISTS,
					virtualServer.getDefaultPoolName());
		}

		if (slbPoolDao.find(virtualServer.getSlbPool()) == null) {
			ExceptionUtils.throwBizException(MessageID.VIRTUALSERVER_SLBPOOL_NOT_EXISTS, virtualServer.getSlbPool());
		}

		for (Location location : virtualServer.getLocations()) {

			if (StringUtils.isBlank(location.getPattern())) {
				ExceptionUtils.throwBizException(MessageID.VIRTUALSERVER_LOCATION_NO_PATTERN);
			}

			if (StringUtils.isBlank(location.getMatchType())) {
				ExceptionUtils.throwBizException(MessageID.VIRTUALSERVER_LOCATION_NO_MATCHTYPE);
			}

			if (location.getDirectives().size() == 0) {
				ExceptionUtils.throwBizException(MessageID.VIRTUALSERVER_LOCATION_NO_DIRECTIVE, location.getPattern());
			}

			for (Directive directive : location.getDirectives()) {
				if (!TemplateManager.INSTANCE.availableFiles("directive").contains(directive.getType())) {
					ExceptionUtils
							.throwBizException(MessageID.VIRTUALSERVER_DIRECTIVE_TYPE_NOT_SUPPORT, directive.getType());
				}
			}

		}
	}

	@Override
	public String generateNginxConfig(VirtualServer virtualServer, List<Pool> pools, List<Aspect> commonAspects,
			List<Variable> variables, List<Strategy> strategies) throws BizException {

		try {
			if (logger.isInfoEnabled()) {
				logger.info("[generateNginxConfig][create nginx config][begin]" + virtualServer.getName());
			}

			SlbModelTree tmpSlbModelTree = AbstractDbStore
					.createSlbModelTree(virtualServer, pools, commonAspects, variables, strategies, -1);

			return getNginxConfig(tmpSlbModelTree);
		} catch (Exception e) {
			ExceptionUtils.logAndRethrowBizException(e);
		} finally {
			if (logger.isInfoEnabled()) {
				logger.info("[generateNginxConfig][create nginx config][end]" + virtualServer.getName());
			}
		}
		return "";
	}

	private String getNginxConfig(SlbModelTree tmpSlbModelTree) throws BizException {

		SlbModelTree slbModelTree = SerializationUtils.clone(tmpSlbModelTree);
		// 兼容以往bug
		if (slbModelTree.getStrategies() == null || slbModelTree.getStrategies().size() == 0) {
			for (Strategy strategy : strategyDao.list()) {
				slbModelTree.addStrategy(strategy);
			}
		}

		for (PreprocessBeforeGenerateNginxConfig processor : preprocessBeforeGenerateNginxConfigs) {
			processor.process(slbModelTree);
		}

		NginxConfigVisitor visitor = new NginxConfigVisitor();
		slbModelTree.accept(visitor);
		Map<String, Object> context = new HashMap<String, Object>();
		context.put("config", visitor.getVisitorResult());
		return VelocityEngineManager.INSTANCE.merge(TemplateManager.INSTANCE.getTemplate("server", "default"), context);
	}

	@Override
	public String generateNginxConfig(SlbModelTree _slbModelTree) throws BizException {

		if (_slbModelTree.getVirtualServers().size() != 1) {
			throw new BizException(MessageID.VIRTUALSERVER_MULTIVS_GENERATE_NGINX_CONFIG);
		}
		try {

			TagCacheEntryValue value = tagCache.get(new TagCacheEntryKey(_slbModelTree));
			return value.getNginxConfig();
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof BizException) {
				throw (BizException) cause;
			}
			throw new BizException(cause);
		}
	}

	@Override
	public String tag(final String virtualServerName, final int virtualServerVersion) throws BizException {
		if (StringUtils.isBlank(virtualServerName)) {
			ExceptionUtils.throwBizException(MessageID.VIRTUALSERVER_NAME_EMPTY);
		}

		final List<Pool> pools = poolDao.list();
		final List<Aspect> commonAspects = commonAspectDao.list();
		final List<Variable> variables = variableDao.list();
		final List<Strategy> strategies = strategyDao.list();

		try {
			VirtualServer virtualServer = virtualServerDao.find(virtualServerName);

			if (virtualServer.getVersion() != virtualServerVersion) {
				ExceptionUtils.logAndRethrowBizException(new ConcurrentModificationException(),
						MessageID.VIRTUALSERVER_CONCURRENT_MOD, virtualServerName);
			}

			String nginxConfigContent = generateNginxConfig(virtualServer, pools, commonAspects, variables, strategies);
			Pair<Boolean, String> checkResult = null;

			try {
				checkResult = checkNginxConfig(virtualServerName, nginxConfigContent, pools);
			} catch (Exception ex) {
			}
			if (checkResult != null && !checkResult.getKey()) {
				ExceptionUtils.throwBizException(MessageID.UNSAFE_CONFIG, checkResult.getValue());
			}

			NginxCheckResult nginxCheckResult = null;

			if (virtualServer.isHttpsOpen()) {
				nginxCheckResult = nginxService
						.checkConfig(nginxConfigContent, virtualServerName, virtualServer.getSslCertificate(),
								virtualServer.getSslCertificateKey());
			} else {
				nginxCheckResult = nginxService.checkConfig(nginxConfigContent);
			}
			if (!nginxCheckResult.isSucess()) {
				ExceptionUtils.throwBizException(MessageID.UNSAFE_CONFIG, nginxCheckResult.getMsg());
			}

			String tagId = virtualServerDao
					.tag(virtualServerName, virtualServerVersion, pools, commonAspects, variables, strategies);

			TagCacheEntryValue value = new TagCacheEntryValue(nginxConfigContent, AbstractDbStore
					.createSlbModelTree(virtualServer, pools, commonAspects, variables, strategies, tagId),
					virtualServer.getSslCertificate(), virtualServer.getSslCertificateKey());

			tagCache.put(new TagCacheEntryKey(virtualServerName, tagId), value);
			return tagId;
		} catch (Exception e) {
			logger.error("[doWrite]error", e);
			ExceptionUtils.rethrowBizException(e);
		}
		return null;
	}

	private Pair<Boolean, String> checkNginxConfig(String virtualServerName, String config, List<Pool> pools) {
		boolean result = true;
		StringBuilder builder = null;
		String vsPrefix = virtualServerName + ".";
		Set<String> poolNames = new HashSet<String>();

		for (Pool pool : pools) {
			poolNames.add(pool.getName());
		}
		for (Pattern pattern : m_poolPatterns) {
			Matcher matcher = pattern.matcher(config);

			while (matcher.find()) {
				String rawPoolName = matcher.group(1).trim();
				int index = rawPoolName.indexOf(vsPrefix);

				if (index >= 0) {
					String poolName = rawPoolName.substring(index + vsPrefix.length());

					if (poolNames.contains(poolName)) {
						result = false;
						if (builder == null) {
							builder = new StringBuilder(50);
							builder.append("cannot use pool directly instead of use $dp_domain:");
						}
						builder.append(rawPoolName).append("\t");
					}
				}

			}
		}
		if (result) {
			return new Pair<Boolean, String>(true, "");
		} else {
			return new Pair<Boolean, String>(false, builder.toString());
		}
	}

	@Override
	public SlbModelTree findTagById(final String virtualServerName, final String tagId) throws BizException {
		if (StringUtils.isBlank(virtualServerName)) {
			ExceptionUtils.throwBizException(MessageID.VIRTUALSERVER_NAME_EMPTY);
		}

		if (StringUtils.isBlank(tagId)) {
			ExceptionUtils.throwBizException(MessageID.VIRTUALSERVER_TAGID_EMPTY);
		}
		try {
			TagCacheEntryValue value = tagCache.get(new TagCacheEntryKey(virtualServerName, tagId));
			return value.getSlbModelTree();
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof BizException) {
				throw (BizException) cause;
			}
			throw new BizException(cause);
		}
	}

	@Override
	public String findPrevTagId(final String virtualServerName, final String tagId) throws BizException {
		if (StringUtils.isBlank(virtualServerName)) {
			ExceptionUtils.throwBizException(MessageID.VIRTUALSERVER_NAME_EMPTY);
		}

		if (StringUtils.isBlank(tagId)) {
			ExceptionUtils.throwBizException(MessageID.VIRTUALSERVER_TAGID_EMPTY);
		}

		return read(new ReadOperation<String>() {

			@Override
			public String doRead() throws BizException {
				return virtualServerDao.findPrevTagId(virtualServerName, tagId);
			}

		});
	}

	@Override
	public void removeTag(final String virtualServerName, final String tagId) throws BizException {
		if (StringUtils.isBlank(virtualServerName)) {
			ExceptionUtils.throwBizException(MessageID.VIRTUALSERVER_NAME_EMPTY);
		}

		if (StringUtils.isBlank(tagId)) {
			ExceptionUtils.throwBizException(MessageID.VIRTUALSERVER_TAGID_EMPTY);
		}

		read(new ReadOperation<Void>() {

			@Override
			public Void doRead() throws BizException {
				virtualServerDao.removeTag(virtualServerName, tagId);
				return null;
			}

		});
	}

	@Override
	public String findLatestTagId(final String virtualServerName) throws BizException {
		if (StringUtils.isBlank(virtualServerName)) {
			ExceptionUtils.throwBizException(MessageID.VIRTUALSERVER_NAME_EMPTY);
		}

		return read(new ReadOperation<String>() {

			@Override
			public String doRead() throws BizException {
				return virtualServerDao.findLatestTagId(virtualServerName);
			}

		});
	}

	@Override
	public List<String> listTag(final String virtualServerName, final int maxNum) throws BizException {
		if (StringUtils.isBlank(virtualServerName)) {
			ExceptionUtils.throwBizException(MessageID.VIRTUALSERVER_NAME_EMPTY);
		}

		return read(new ReadOperation<List<String>>() {

			@Override
			public List<String> doRead() throws BizException {
				List<String> tags = virtualServerDao.listTags(virtualServerName);
				if (tags != null && tags.size() < maxNum) {
					return tags;
				} else {
					List<String> result = new ArrayList<String>(maxNum);
					if (tags != null) {
						for (int pos = 0; pos < tags.size() && pos < maxNum; pos++) {
							result.add(tags.get(pos));
						}
					}
					return result;
				}
			}

		});
	}

	@Override
	public Set<String> listVSNames() throws BizException {
		return virtualServerDao.listNames();
	}

	@Override
	public Set<String> findUndeleteDengineVSNames(String host) throws BizException {
		Set<String> dbVSNames = virtualServerDao.listNames();
		Set<String> agentVSNames = nginxService.listVSNames(host);

		agentVSNames.removeAll(dbVSNames);
		return agentVSNames;
	}

	static class TagCacheEntryKey {

		/**
		 * Tagid
		 */
		private final String tagId;

		private final String vsName;

		public TagCacheEntryKey(String vsName, String tagId) {
			this.vsName = vsName;
			this.tagId = tagId;
		}

		public TagCacheEntryKey(SlbModelTree slbModelTree) {

			this.vsName = ((VirtualServer) slbModelTree.getVirtualServers().values().toArray()[0]).getName();
			this.tagId = AbstractDbStore.convertToStrTagId(vsName, slbModelTree.getTag());
		}

		public String getTagId() {
			return tagId;
		}

		public String getVsName() {
			return vsName;
		}

		@Override
		public int hashCode() {
			return tagId.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof TagCacheEntryKey) {
				return ((TagCacheEntryKey) obj).tagId.equals(tagId);
			}
			return false;
		}

		@Override
		public String toString() {
			return tagId;
		}
	}

	static class TagCacheEntryValue {

		private String nginxConfig;

		private SlbModelTree slbModelTree;

		private String sslCertificate;

		private String sslKey;

		public TagCacheEntryValue(String nginxConfig, SlbModelTree slbModelTree, String certificate, String key) {
			this.nginxConfig = nginxConfig;
			this.slbModelTree = slbModelTree;
			this.sslCertificate = certificate;
			this.sslKey = key;
		}

		public String getNginxConfig() {
			return nginxConfig;
		}

		public void setNginxConfig(String nginxConfig) {
			this.nginxConfig = nginxConfig;
		}

		public SlbModelTree getSlbModelTree() {
			return slbModelTree;
		}

		public void setSlbModelTree(SlbModelTree slbModelTree) {
			this.slbModelTree = slbModelTree;
		}

		public String getSslCertificate() {
			return sslCertificate;
		}

		public void setSslCertificate(String sslCertificate) {
			this.sslCertificate = sslCertificate;
		}

		public String getSslKey() {
			return sslKey;
		}

		public void setSslKey(String sslKey) {
			this.sslKey = sslKey;
		}

	}

}
