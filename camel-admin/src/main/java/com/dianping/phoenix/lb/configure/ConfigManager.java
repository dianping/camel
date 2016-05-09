package com.dianping.phoenix.lb.configure;

import com.dianping.phoenix.lb.config.entity.RuntimeConfig;
import com.dianping.phoenix.lb.utils.UrlUtils;
import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;

import java.io.File;
import java.net.URL;
import java.util.Iterator;

public class ConfigManager implements Initializable {

	public static final String CHECK_TEST_CONF = "test.conf";

	public static final String CHECK_SERVER_CONF = "server.conf";

	public static final String CHECK_CERTIFACATE_CONF = "ssl.crt";

	public static final String CHECK_KEY_CONF = "ssl.key";

	private RuntimeConfig m_config;

	private boolean m_showLogTimestamp = true;

	private String nginxCheckConfigFileName;

	private String nginxCheckMainConfigFileName;

	private void check() {
		if (m_config == null) {
			throw new RuntimeException("ConfigManager is not initialized properly!");
		}
	}

	public String getModelStoreBaseDir() {
		check();
		return m_config.getModelStoreBaseDir();
	}

	public String getModelGitUrl() {
		check();
		return m_config.getModelGitUrl();
	}

	public String getNginxCheckConfigFileName() {
		check();
		return this.nginxCheckConfigFileName;
	}

	public String getNginxCheckCertifacateFileName(String vsName) {
		check();
		String folder = m_config.getNginxCheckConfigFolder();

		return (folder.endsWith("/") ?
				folder + vsName + "/" + CHECK_CERTIFACATE_CONF :
				folder + "/" + vsName + "/" + CHECK_CERTIFACATE_CONF);
	}

	public String getNginxCheckKeyFileName(String vsName) {
		check();
		String folder = m_config.getNginxCheckConfigFolder();

		return (folder.endsWith("/") ?
				folder + vsName + "/" + CHECK_KEY_CONF :
				folder + "/" + vsName + "/" + CHECK_KEY_CONF);
	}

	public String getTengineConfigBaseDir() {
		check();
		return m_config.getTengineConfigBaseDir();
	}

	public String getNginxCheckConfigFolder() {
		check();
		return m_config.getNginxCheckConfigFolder();
	}

	public String getNginxCheckMainConfigFileName() {
		check();
		return this.nginxCheckMainConfigFileName;
	}

	public int getDeployConnectTimeout() {
		check();

		return m_config.getDeployConnectTimeout();
	}

	public int getDeployGetlogRetrycount() {
		check();
		return m_config.getDeployGetlogRetrycount();
	}

	public String getDeployLogUrl(String host, long deployId) {
		check();

		String pattern = m_config.getDeployLogUrlPattern();

		return String.format(pattern, host, deployId);
	}

	public long getDeployRetryInterval() {
		check();

		int interval = m_config.getDeployRetryInterval(); // in second

		return interval;
	}

	public void setDeployRetryInterval(int retryInterval) {
		check();

		m_config.setDeployRetryInterval(retryInterval);
	}

	public String getDeployStatusUrl(String host, long deployId) {
		check();

		String pattern = m_config.getDeployStatusUrlPattern();

		return String.format(pattern, host, deployId);
	}

	public String getDeployWithReloadUrl(String host, long deployId, String vsName, String configFileName,
			String version) {
		check();
		String gitUrl = getTengineConfigGitUrl();
		return String
				.format(m_config.getDeployUrlReloadPattern(), host, deployId, vsName, configFileName, version, gitUrl);
	}

	public String getDeployWithDynamicRefreshUrl(String host, long deployId, String vsName, String configFileName,
			String version) {
		check();
		String gitUrl = getTengineConfigGitUrl();
		return String
				.format(m_config.getDeployUrlDynamicRefreshPattern(), host, deployId, vsName, configFileName, version,
						gitUrl);
	}

	public String getTengineConfigGitUrl() {
		check();
		return m_config.getTengineConfigGitUrl();
	}

	public String getTengineConfigFileName() {
		check();
		return m_config.getTengineConfigFileName();
	}

	@Override
	public void initialize() throws InitializationException {
		try {
			m_config = new RuntimeConfig();

			String folder = m_config.getNginxCheckConfigFolder();

			this.nginxCheckConfigFileName = (folder.endsWith("/") ?
					folder + CHECK_SERVER_CONF :
					folder + "/" + CHECK_SERVER_CONF);
			this.nginxCheckMainConfigFileName = (folder.endsWith("/") ?
					folder + CHECK_TEST_CONF :
					folder + "/" + CHECK_TEST_CONF);

		} catch (Exception e) {
			throw new InitializationException("init RuntimeConfig fail!", e);
		}
		makeShellScriptExecutable();
	}

	public String getAgentTengineConfigVersionUrl(String host, String vsName) {
		check();
		return String.format(m_config.getAgentTengineConfigVersionUrlPattern(), host, vsName);
	}

	public String getUpdateFileUrl(String agentIp, String vs, String fileNames) {
		check();
		return String.format(m_config.getUpdateFileUrlPattern(), agentIp, vs, fileNames);
	}

	public String getAgentVSListUrl(String host) {
		check();
		return String.format(m_config.getAgentVsListUrlPattern(), host);
	}

	public String getAgentRemoveVSUrl(String host, String vsName) {
		check();
		return String.format(m_config.getAgentRemoveVsUrlPattern(), host, vsName);
	}

	public String getAgentReloadUrl(String host) {
		check();
		return String.format(m_config.getAgentReloadUrlPattern(), host);
	}

	public String getNginxDynamicAddUpstreamUrlPattern(String upstreamName) {
		check();
		return String.format(m_config.getNginxDynamicAddUpstreamUrlPattern(), UrlUtils.encode(upstreamName));
	}

	public String getNginxDynamicDeleteUpstreamUrlPattern(String upstreamName) {
		check();
		return String.format(m_config.getNginxDynamicDeleteUpstreamUrlPattern(), UrlUtils.encode(upstreamName));
	}

	public String getNginxDynamicUpdateUpstreamUrlPattern(String upstreamName) {
		check();
		return String.format(m_config.getNginxDynamicUpdateUpstreamUrlPattern(), UrlUtils.encode(upstreamName));
	}

	public File getGitScript() {
		check();
		return getScriptFile("git.sh");
	}

	public File getNginxScript() {
		check();
		return getScriptFile("nginx.sh");
	}

	public String getF5Host() {
		check();
		return m_config.getF5Host();
	}

	public String getF5User() {
		check();
		return m_config.getF5User();
	}

	public String getF5Password() {
		check();
		return m_config.getF5Password();
	}

	public boolean isNeedCallF5() {
		check();
		return m_config.isNeedCallF5();
	}

	private File getScriptFile(String scriptFileName) {
		URL scriptUrl = this.getClass().getClassLoader().getResource("script/" + scriptFileName);
		if (scriptUrl == null) {
			throw new RuntimeException(scriptFileName + " not found");
		}
		return new File(scriptUrl.getPath());
	}

	private void makeShellScriptExecutable() {
		File scriptDir = getGitScript().getParentFile();
		Iterator<File> scriptIter = FileUtils.iterateFiles(scriptDir, new String[] { "sh" }, true);
		while (scriptIter != null && scriptIter.hasNext()) {
			scriptIter.next().setExecutable(true, false);
		}
	}
}
