package com.dianping.phoenix.lb.dengine;

import com.dianping.phoenix.lb.api.dengine.DengineConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DefaultDengineConfig implements DengineConfig {

	@Value("${monitor.degrade.force.up}")
	private String forceUpUrl = "http://%s:6666/degrade/force/up?upstreams=%s";

	@Value("${monitor.degrade.force.down}")
	private String forceDownUrl = "http://%s:6666/degrade/force/down?upstreams=%s";

	@Value("${monitor.degrade.force.auto}")
	private String forceAutoUrl = "http://%s:6666/degrade/force/auto?upstreams=%s";

	public String getForceUpUrl() {
		return forceUpUrl;
	}

	public void setForceUpUrl(String forceUpUrl) {
		this.forceUpUrl = forceUpUrl;
	}

	public String getForceDownUrl() {
		return forceDownUrl;
	}

	public void setForceDownUrl(String forceDownUrl) {
		this.forceDownUrl = forceDownUrl;
	}

	public String getForceAutoUrl() {
		return forceAutoUrl;
	}

	public void setForceAutoUrl(String forceAutoUrl) {
		this.forceAutoUrl = forceAutoUrl;
	}

}
