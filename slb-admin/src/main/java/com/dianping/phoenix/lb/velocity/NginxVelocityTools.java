package com.dianping.phoenix.lb.velocity;

import com.dianping.phoenix.lb.constant.Constants;
import com.dianping.phoenix.lb.model.State;
import com.dianping.phoenix.lb.model.entity.Check;
import com.dianping.phoenix.lb.model.entity.Directive;
import com.dianping.phoenix.lb.model.entity.Strategy;
import com.dianping.phoenix.lb.model.entity.UpstreamFilter;
import com.dianping.phoenix.lb.model.nginx.NginxLocation.MatchType;
import com.dianping.phoenix.lb.model.nginx.NginxUpstream;
import com.dianping.phoenix.lb.model.nginx.NginxUpstreamServer;
import com.dianping.phoenix.lb.utils.DateUtils;
import com.dianping.phoenix.lb.utils.PoolNameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public class NginxVelocityTools {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	public String locationMatchOp(MatchType matchType) {
		switch (matchType) {
		case COMMON:
			return "";
		case PREFIX:
			return "~*";
		case REGEX_CASE_INSENSITIVE:
			return "~*";
		case REGEX_CASE_SENSITIVE:
			return "~";
		case EXACT:
			return "=";
		default:
			return "";
		}
	}

	public String rewriteLocationIfNeeded(MatchType matchType, String locationPattern) {
		if (matchType == MatchType.PREFIX) {
			return "^" + locationPattern.replaceAll("\\.", "\\\\.");
		}
		return locationPattern;
	}

	public String poolName(String prefix, String poolName) {
		return PoolNameUtils.rewriteToPoolNamePrefix(prefix, poolName);
	}

	public String poolDegradeName(String prefix, String poolName) {
		return PoolNameUtils.rewriteToPoolNameDegradePrefix(prefix, poolName);
	}

	public String properties(Map<String, String> properties) {
		StringBuilder content = new StringBuilder();
		for (Map.Entry<String, String> entry : properties.entrySet()) {
			String template = getTemplate("properties", entry.getKey());
			if (StringUtils.isNotBlank(template)) {
				Map<String, Object> context = new HashMap<String, Object>();
				context.put("value", entry.getValue());
				content.append(VelocityEngineManager.INSTANCE.merge(template, context));
			} else {
				content.append("    " + entry.getKey() + " " + entry.getValue()).append(";");
			}
			content.append("\n");
		}
		return content.toString();
	}

	public String lbStrategy(Strategy strategy) {
		String template = getTemplate("strategy", strategy.getType());
		if (StringUtils.isNotBlank(template)) {
			Map<String, Object> context = new HashMap<String, Object>();
			context.put("strategy", strategy);
			return VelocityEngineManager.INSTANCE.merge(template, context);
		} else {
			return "";
		}
	}

	public String upstreamContent(NginxUpstream upstream, boolean isDegrade) {

		String template = getTemplate("upstream", "upstream-content");
		if (StringUtils.isNotBlank(template)) {
			Map<String, Object> context = new HashMap<String, Object>();
			context.put("upstream", upstream);
			context.put("isDegrade", isDegrade);
			return VelocityEngineManager.INSTANCE.merge(template, context);
		} else {
			return "";
		}
	}

	public String check(Check check) {
		String template = getTemplate("check", "default");
		if (StringUtils.isNotBlank(template)) {
			Map<String, Object> context = new HashMap<String, Object>();
			context.put("check", check);
			return VelocityEngineManager.INSTANCE.merge(template, context);
		} else {
			return "";
		}
	}

	public String upstreamFilter(UpstreamFilter upstreamFilter) {
		if (upstreamFilter == null) {
			return "";
		}

		String template = getTemplate("upstreamfilter", "default");
		if (StringUtils.isNotBlank(template)) {
			Map<String, Object> context = new HashMap<String, Object>();
			context.put("upstreamFilter", upstreamFilter);
			return VelocityEngineManager.INSTANCE.merge(template, context);
		} else {
			return "";
		}
	}

	public String directive(String vsName, Directive directive) {
		String template = getTemplate("directive", directive.getType());
		if (StringUtils.isNotBlank(template)) {
			Map<String, Object> context = new HashMap<String, Object>();
			context.put("directive", directive);
			context.put("vsName", vsName);
			if (Constants.DIRECTIVE_PROXY_PASS.equals(directive.getType())) {
				context.put(Constants.DIRECTIVE_DP_DOMAIN,
						directive.getDynamicAttribute(Constants.DIRECTIVE_PROXY_PASS_POOL_NAME));
			}
			return VelocityEngineManager.INSTANCE.merge(template, context);
		} else {
			return "";
		}
	}

	public String upstreamServer(NginxUpstreamServer server, Strategy strategy) {
		if (server.getMember().getState() == State.ENABLED) {
			return getServerConfig(server, strategy);
		}
		return "";
	}

	private String getServerConfig(NginxUpstreamServer server, Strategy strategy) {

		String template = getTemplate("upstream", "hash".equals(strategy.getType()) ? "server_hash" : "server");
		if (StringUtils.isNotBlank(template)) {
			Map<String, Object> context = new HashMap<String, Object>();
			context.put("server", server);
			return VelocityEngineManager.INSTANCE.merge(template, context);
		}
		return "";
	}

	public String upstreamDegradeServer(NginxUpstreamServer server, Strategy strategy) {
		if (server.getMember().getState() == State.DEGRADE) {
			return getServerConfig(server, strategy);
		}
		return "";
	}

	public String nowTimeStamp() {
		return DateUtils.format(new Date());
	}

	private String getTemplate(String schema, String file) {
		return TemplateManager.INSTANCE.getTemplate(schema, file);
	}

}
