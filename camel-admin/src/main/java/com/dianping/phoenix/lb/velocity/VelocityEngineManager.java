package com.dianping.phoenix.lb.velocity;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import java.io.StringWriter;
import java.util.Map;

public enum VelocityEngineManager {

	INSTANCE;

	private VelocityEngine ve;

	private VelocityContext toolContext;

	private VelocityEngineManager() {
		ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "class");
		ve.setProperty("class.resource.loader.class", ClasspathResourceLoader.class.getName());
		ve.setProperty("class.resource.loader.cache", true);
		ve.setProperty("class.resource.loader.modificationCheckInterval", "-1");
		ve.setProperty("input.encoding", "UTF-8");
		ve.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogSystem");
		toolContext = new VelocityContext();
		toolContext.put("nginx", new NginxVelocityTools());
		toolContext.put("stringUtils", new org.apache.commons.lang.StringUtils());

		ve.init();
	}

	public String merge(String templateContent, Map<String, Object> context) {
		StringWriter out = new StringWriter();
		VelocityContext vContext = new VelocityContext(toolContext);
		for (Map.Entry<String, Object> entry : context.entrySet()) {
			vContext.put(entry.getKey(), entry.getValue());
		}
		ve.evaluate(vContext, out, "dynamic template", templateContent);
		return out.toString();
	}
}
