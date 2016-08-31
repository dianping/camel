package com.dianping.platform.slb.agent.task.model.config.upgrade;

import com.dianping.platform.slb.agent.task.AbstractTask;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public class ConfigUpgradeTask extends AbstractTask {

	private String[] virtualServerNames;
	private String[] versions;
	private String configFileName;
	private boolean reload = true;
	private List<Map<String, String>> dynamicRefreshPostData;
	private Map<String, String> dynamicVsPostData;
	private static Set<String> excludes = new HashSet<String>();
	private OutputStream m_outputStream;

	static {
		excludes.add("dynamicVsPostData");
		excludes.add("dynamicRefreshPostData");
	}

	public ConfigUpgradeTask(String[] virtualServerNames, String[] versions, String configFileName, boolean reload,
			List<Map<String, String>> dynamicRefreshPostData, Map<String, String> dynamicVsPostData) {
		super();
		this.virtualServerNames = virtualServerNames;
		this.configFileName = configFileName;
		this.versions = versions;
		this.reload = reload;
		this.dynamicRefreshPostData = dynamicRefreshPostData;
		this.dynamicVsPostData = dynamicVsPostData;
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toStringExclude(this, excludes);
	}

	public String[] getVirtualServerNames() {
		return virtualServerNames;
	}

	public void setVirtualServerNames(String[] virtualServerNames) {
		this.virtualServerNames = virtualServerNames;
	}

	public String[] getVersions() {
		return versions;
	}

	public void setVersions(String[] versions) {
		this.versions = versions;
	}

	public String getConfigFileName() {
		return configFileName;
	}

	public void setConfigFileName(String configFileName) {
		this.configFileName = configFileName;
	}

	public boolean isReload() {
		return reload;
	}

	public void setReload(boolean reload) {
		this.reload = reload;
	}

	public List<Map<String, String>> getDynamicRefreshPostData() {
		return dynamicRefreshPostData;
	}

	public void setDynamicRefreshPostData(List<Map<String, String>> dynamicRefreshPostData) {
		this.dynamicRefreshPostData = dynamicRefreshPostData;
	}

	public Map<String, String> getDynamicVsPostData() {
		return dynamicVsPostData;
	}

	public void setDynamicVsPostData(Map<String, String> dynamicVsPostData) {
		this.dynamicVsPostData = dynamicVsPostData;
	}

	public static Set<String> getExcludes() {
		return excludes;
	}

	public static void setExcludes(Set<String> excludes) {
		ConfigUpgradeTask.excludes = excludes;
	}

	@Override
	public void setTaskOutputStream(OutputStream outputStream) {
		this.m_outputStream = outputStream;
	}

	@Override
	public OutputStream getTaskOutputStream() {
		return m_outputStream;
	}
}
