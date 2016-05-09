package com.dianping.phoenix.lb.visitor;

import com.dianping.phoenix.lb.constant.Constants;
import com.dianping.phoenix.lb.constant.MessageID;
import com.dianping.phoenix.lb.model.State;
import com.dianping.phoenix.lb.model.entity.*;
import com.dianping.phoenix.lb.model.nginx.*;
import com.dianping.phoenix.lb.model.nginx.NginxLocation.MatchType;
import com.dianping.phoenix.lb.service.VariableReplacer;
import com.dianping.phoenix.lb.utils.MessageUtils;
import com.dianping.phoenix.lb.utils.PoolNameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Leo Liang
 *
 */
public class NginxConfigVisitor extends AbstractVisitor<NginxConfig> {

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	private Map<String, Strategy> strategies = new HashMap<String, Strategy>();
	private Map<String, Pool> pools = new HashMap<String, Pool>();
	private Map<String, Aspect> commonAspects = new HashMap<String, Aspect>();
	private Map<String, String> variables = new HashMap<String, String>();

	public NginxConfigVisitor() {
		result = new NginxConfig();
	}

	public static NginxUpstream generateUpstream(Pool pool, Strategy strategy) {

		NginxUpstream upstream = new NginxUpstream();

		Check check = pool.getCheck();
		if (check == null) {
			//为了兼容，给默认值（之前pool未配置check）
			check = new Check();
		}

		upstream.setCheck(check);
		upstream.setUpstreamFilter(pool.getUpstreamFilter());

		upstream.setDegradeRate(pool.getDegradeRate());
		upstream.setDegradeForceState(pool.getDegradeForceState());

		upstream.setLbStrategy(strategy);
		upstream.setName(toUpstreamName(pool.getName()));

		int enableCount = 0;
		for (Member member : pool.getMembers()) {
			NginxUpstreamServer nginxUpstreamServer = new NginxUpstreamServer();
			nginxUpstreamServer.setMember(member);
			upstream.addServer(nginxUpstreamServer);
			if (member.getState() == State.DEGRADE) {
				upstream.setNeedDegrade(true);
			}
			if (member.getState() == State.ENABLED) {
				enableCount++;
			}
		}

		upstream.setKeepalive(pool.getKeepalive() * enableCount);
		upstream.setKeepaliveTimeout(pool.getKeepaliveTimeout());

		return upstream;
	}

	private static String toUpstreamName(String poolName) {
		return poolName;
	}

	@Override
	public void visitSlbModelTree(SlbModelTree slbModelTree) {

		result.setTagTime(slbModelTree.getTagDate());

		for (Variable variable : slbModelTree.getVariables()) {
			visitVariable(variable);
		}

		for (Strategy strategy : slbModelTree.getStrategies().values()) {
			visitStrategy(strategy);
		}

		for (Pool pool : slbModelTree.getPools().values()) {
			visitPool(pool);
		}

		for (Aspect aspect : slbModelTree.getAspects()) {
			visitCommonAspect(aspect);
		}

		for (SlbPool slbPool : slbModelTree.getSlbPools().values()) {
			visitSlbPool(slbPool);
		}

		for (VirtualServer virtualServer : slbModelTree.getVirtualServers().values()) {
			visitVirtualServer(virtualServer);
		}
	}

	@Override
	public void visitVariable(Variable variable) {
		variables.put(variable.getKey(), variable.getValue());
	}

	@Override
	public void visitStrategy(Strategy strategy) {
		strategies.put(strategy.getName(), strategy);
	}

	@Override
	public void visitVirtualServer(VirtualServer virtualServer) {
		result.setName(virtualServer.getName());

		NginxServer server = new NginxServer();

		server.setProperties(virtualServer.getDynamicAttributes());
		server.setListen(virtualServer.getPort());
		server.setServerName(virtualServer.getDomain());
		server.setDefaultPool(virtualServer.getDefaultPoolName());
		server.setHttpsOpen(virtualServer.getHttpsOpen());
		server.setHttpsPort(virtualServer.getHttpsPort());
		server.setDefaultHttpsType(virtualServer.getDefaultHttpsType());
		result.setServer(server);

		super.visitVirtualServer(virtualServer);

		setUpstreamsAsUsed(virtualServer.getDefaultPoolName(), MessageUtils
				.getMessage(MessageID.VIRTUALSERVER_DEFAULTPOOL_NOT_EXISTS, virtualServer.getDefaultPoolName()));
	}

	public void visitCommonAspect(Aspect aspect) {
		commonAspects.put(aspect.getName(), aspect);
	}

	@Override
	public void visitAspect(Aspect aspect) {
		Aspect actualAspect = null;
		Aspect cloneAspect = null;
		if (StringUtils.isBlank(aspect.getRef())) {
			actualAspect = aspect;
		} else {
			if (!commonAspects.containsKey(aspect.getRef())) {
				throw new RuntimeException(MessageUtils.getMessage(MessageID.COMMON_ASPECT_SAVE_FAIL, aspect.getRef()));
			} else {
				actualAspect = commonAspects.get(aspect.getRef());
			}
		}
		cloneAspect = SerializationUtils.clone(actualAspect);
		super.visitAspect(cloneAspect);
		result.getServer().addAspect(cloneAspect);
		setUpstreamAsUsedAndChangeUpstreamName(cloneAspect.getDirectives(),
				MessageUtils.getMessage(MessageID.PROXY_PASS_NO_POOL, "aspect " + aspect.getPointCut()));
	}

	private void setUpstreamAsUsedAndChangeUpstreamName(List<Directive> directives, String errorContent) {
		for (Directive directive : directives) {
			if (Constants.DIRECTIVE_PROXY_PASS.equals(directive.getType())) {

				setUpstreamsAsUsed(directive.getDynamicAttribute(Constants.DIRECTIVE_PROXY_PASS_POOL_NAME),
						errorContent);

				directive.setDynamicAttribute(Constants.DIRECTIVE_PROXY_PASS_POOL_NAME, PoolNameUtils
								.rewriteToPoolNamePrefix(result.getName(),
										directive.getDynamicAttribute(Constants.DIRECTIVE_PROXY_PASS_POOL_NAME)));
			} else {

				for (String key : directive.getDynamicAttributes().keySet()) {

					String replace = findUpstreamAndSetUsedAndGetContent(directive.getDynamicAttributes().get(key),
							errorContent);
					if (!StringUtils.isEmpty(replace)) {
						directive.setDynamicAttribute(key, replace);
					}
				}

			}
		}
	}

	private String findUpstreamAndSetUsedAndGetContent(String content, String errorContent) {

		String poolName = PoolNameUtils.extractPoolNameFromProxyPassString(content);

		if (StringUtils.isNotBlank(poolName)) {
			setUpstreamsAsUsed(poolName, errorContent);
			return PoolNameUtils.replacePoolNameFromProxyPassString(result.getName(), content, pools);
		}

		return content;
	}

	private void setUpstreamsAsUsed(String poolName, String errorContent) {
		List<NginxUpstream> upstreams = result.getUpstream(poolName);
		if (upstreams == null || upstreams.isEmpty()) {
			logger.warn("[setUpstreamsAsUsed][unfound pool]" + poolName);
			return;
		}
		for (NginxUpstream upstream : upstreams) {
			upstream.setUsed(true);
		}
	}

	@Override
	public void visitPool(Pool pool) {

		pools.put(pool.getName(), pool);

		NginxUpstream upstream = generateUpstream(pool, strategies.get(pool.getLoadbalanceStrategyName()));
		result.addUpstream(upstream);
	}

	@Override
	public void visitLocation(Location location) {
		Location cloneLocation = SerializationUtils.clone(location);
		super.visitLocation(cloneLocation);

		NginxLocation nginxLocation = new NginxLocation();
		nginxLocation.setHttpsType(cloneLocation.getHttpsType());
		nginxLocation.setMatchType(toNginxMatchType(cloneLocation));
		nginxLocation.setPattern(cloneLocation.getPattern());
		setUpstreamAsUsedAndChangeUpstreamName(cloneLocation.getDirectives(),
				MessageUtils.getMessage(MessageID.PROXY_PASS_NO_POOL, cloneLocation.getPattern()));

		for (Directive directive : cloneLocation.getDirectives()) {
			nginxLocation.addDirective(directive);
		}

		result.getServer().addLocations(nginxLocation);
	}

	@Override
	public void visitDirective(Directive directive) {

		VariableReplacer variableReplacer = new VariableReplacer(variables);
		//进行变量替换
		Map<String, String> values = directive.getDynamicAttributes();
		for (Entry<String, String> entry : values.entrySet()) {
			String initValue = entry.getValue();
			values.put(entry.getKey(), variableReplacer.translateValue(initValue));
		}
	}

	private MatchType toNginxMatchType(Location location) {
		if (Constants.LOCATION_MATCHTYPE_PREFIX.equals(location.getMatchType())) {
			return MatchType.PREFIX;
		} else if (Constants.LOCATION_MATCHTYPE_REGEX.equals(location.getMatchType())) {
			return location.getCaseSensitive() ? MatchType.REGEX_CASE_SENSITIVE : MatchType.REGEX_CASE_INSENSITIVE;
		} else if (Constants.LOCATION_MATCHTYPE_EXACT.equals(location.getMatchType())) {
			return MatchType.EXACT;
		} else {
			return MatchType.COMMON;
		}
	}

}
