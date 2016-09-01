package com.dianping.platform.slb.agent.task.workflow.step.impl;

import com.dianping.platform.slb.agent.conf.ConfigureManager;
import com.dianping.platform.slb.agent.constant.Constants;
import com.dianping.platform.slb.agent.shell.ScriptExecutor;
import com.dianping.platform.slb.agent.shell.impl.DefaultScriptExecutor;
import com.dianping.platform.slb.agent.task.Task;
import com.dianping.platform.slb.agent.task.model.config.upgrade.ConfigUpgradeTask;
import com.dianping.platform.slb.agent.task.workflow.step.Step;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public abstract class ConfigUpgradeStep implements Step {

	public static final ConfigUpgradeStep SUCCESS = new ConfigUpgradeStep(null, null, 7) {
		@Override
		public int doStep(Task task) throws Exception {
			return Step.CODE_SUCCESS;
		}

		@Override
		public String toString() {
			return "SUCCESS";
		}

		@Override
		public Map<String, String> getHeader() {
			Map<String, String> headers = super.getHeader();

			headers.put(HEADER_STATUS, STATUS_SUCCESS);
			return headers;
		}
	};

	public static final ConfigUpgradeStep FAIL = new ConfigUpgradeStep(null, null, 7) {
		@Override
		public int doStep(Task task) throws Exception {
			return Step.CODE_FAIL;
		}

		@Override
		public String toString() {
			return "FAIL";
		}

		@Override
		public Map<String, String> getHeader() {
			Map<String, String> headers = super.getHeader();

			headers.put(HEADER_STATUS, STATUS_FAIL);
			return headers;
		}
	};

	public static final ConfigUpgradeStep ROLL_BACK_CONFIG = new ConfigUpgradeStep(FAIL, FAIL, 6) {
		@Override
		public int doStep(Task task) throws Exception {
			ConfigUpgradeTask configUpgradeTask = (ConfigUpgradeTask) task;
			String configFileName = configUpgradeTask.getConfigFileName();

			for (String vsName : configUpgradeTask.getVirtualServerNames()) {
				File vsDir = new File(ConfigureManager.getNginxConfDir(), vsName);
				File backupFile = new File(vsDir, generateBackupFileName(configFileName));
				File configFile = new File(vsDir, configFileName);

				if (!backupFile.exists()) {
					if (configFile.exists() && !configFile.delete()) {
						return Step.CODE_FAIL;
					}
				} else {
					if (!backupFile.renameTo(configFile)) {
						return Step.CODE_FAIL;
					}
				}
			}
			return Step.CODE_SUCCESS;
		}

		@Override
		public String toString() {
			return "ROLL_BACK_CONFIG";
		}
	};

	public static final ConfigUpgradeStep UPDATE_CONFIG_VERSION = new ConfigUpgradeStep(SUCCESS, ROLL_BACK_CONFIG, 6) {
		@Override
		public int doStep(Task task) throws Exception {
			ConfigUpgradeTask configUpgradeTask = (ConfigUpgradeTask) task;
			String[] vsNames = configUpgradeTask.getVirtualServerNames();
			String[] versions = configUpgradeTask.getVersions();
			int length = vsNames.length;

			for (int i = 0; i < length; i++) {
				String vsName = vsNames[i];
				String version = versions[i];
				File versionFile = new File(new File(ConfigureManager.getNginxConfDir(), vsName), ".version");

				try {
					FileUtils.writeStringToFile(versionFile, version, Constants.CHAREST_UTF8);
				} catch (IOException ex) {
					return Step.CODE_FAIL;
				}
			}
			return Step.CODE_SUCCESS;
		}

		@Override
		public String toString() {
			return "UPDATE_CONFIG_VERSION";
		}
	};

	public static final ConfigUpgradeStep RELOAD_OR_DYNAMIC_REFRESH_NGINX = new ConfigUpgradeStep(UPDATE_CONFIG_VERSION,
			ROLL_BACK_CONFIG, 5) {
		@Override
		public int doStep(Task task) throws Exception {
			ConfigUpgradeTask configUpgradeTask = (ConfigUpgradeTask) task;
			OutputStream outputStream = configUpgradeTask.getTaskOutputStream();
			ScriptExecutor scriptExecutor = new DefaultScriptExecutor();
			int shellExecuteCode;

			/* if (configUpgradeTask.isReload()) {
				shellExecuteCode = runShellCmd("reload_config", configUpgradeTask, outputStream, scriptExecutor);
			} else {
				shellExecuteCode = runDynamicRefreshShellCmd(configUpgradeTask, outputStream, scriptExecutor);
			} */
			shellExecuteCode = scriptExecutor
					.execute(ConfigureManager.getNginxReloadScriptFile().getAbsolutePath(), outputStream, outputStream);
			if (shellExecuteCode == 0) {
				return Step.CODE_SUCCESS;
			} else {
				return Step.CODE_FAIL;
			}
		}

		private int runShellCmd(String shellFunc, ConfigUpgradeTask task, OutputStream outputStream,
				ScriptExecutor scriptExecutor) throws Exception {
			String script = jointShellCmd(shellFunc, task);

			return scriptExecutor.execute(script, outputStream, outputStream);
		}

		private String jointShellCmd(String shellFunc, ConfigUpgradeTask task) {
			StringBuilder sb = new StringBuilder();
			String tengineConfigDocBase = ConfigureManager.getNginxConfDir();

			sb.append(ConfigureManager.getTengineScriptFile().getAbsolutePath());
			sb.append(String.format(" --tengine_config_doc_base \"%s\" ", tengineConfigDocBase));
			sb.append(String.format(" --config_file \"%s\" ", task.getConfigFileName()));
			sb.append(String.format(" --virtual_server_names \"%s\" ",
					StringUtils.join(task.getVirtualServerNames(), ",")));
			sb.append(String.format(" --versions \"%s\" ", StringUtils.join(task.getVersions(), ",")));
			sb.append(String.format(" --tengine_reload \"%s\" ", task.isReload() ? "1" : "0"));
			sb.append(String.format(" --func \"%s\" ", shellFunc));

			return sb.toString();
		}

		private int runDynamicRefreshShellCmd(ConfigUpgradeTask task, OutputStream outputStream,
				ScriptExecutor scriptExecutor) throws Exception {
			if (task.getDynamicRefreshPostData() != null || !task.getDynamicRefreshPostData().isEmpty()) {
				for (Map<String, String> postDataItem : task.getDynamicRefreshPostData()) {
					StringBuilder script = new StringBuilder();

					script.append(ConfigureManager.getTengineScriptFile().getAbsolutePath());
					script.append(String.format(" --dynamic_refresh_url \"%s\" ", postDataItem.get("url")));
					script.append(String.format(" --refresh_method \"%s\" ", postDataItem.get("method").toUpperCase()));
					script.append(String.format(" --dynamic_refresh_post_data \"%s\" ",
							escapeArgument(postDataItem.get("data"))));
					script.append(String.format(" --func \"%s\" ", "dynamic_refresh_config"));

					int exitCode = scriptExecutor.execute(script.toString(), outputStream, outputStream);

					if (exitCode != 0) {
						return exitCode;
					}
				}
			}
			return 0;
		}

		private Object escapeArgument(String data) {
			return data.replace("$", "\\$");
		}

		@Override
		public String toString() {
			return "RELOAD_OR_DYNAMIC_REFRESH_NGINX";
		}
	};

	public static final ConfigUpgradeStep PUT_NEW_CONFIG = new ConfigUpgradeStep(RELOAD_OR_DYNAMIC_REFRESH_NGINX,
			ROLL_BACK_CONFIG, 4) {
		@Override
		public int doStep(Task task) throws Exception {
			ConfigUpgradeTask configUpgradeTask = (ConfigUpgradeTask) task;
			String fileName = configUpgradeTask.getConfigFileName();
			Map<String, String> dynamicVsPostData = configUpgradeTask.getDynamicVsPostData();

			for (String vsName : configUpgradeTask.getVirtualServerNames()) {
				if (!putConfigFile(vsName, fileName, dynamicVsPostData.get((vsName)))) {
					return Step.CODE_FAIL;
				}
			}
			return Step.CODE_SUCCESS;
		}

		private boolean putConfigFile(String vsName, String fileName, String content) {
			if (StringUtils.isEmpty(content)) {
				return false;
			}

			File vsDir = new File(ConfigureManager.getNginxConfDir(), vsName);
			File config = new File(vsDir, fileName);

			try {
				FileUtils.writeStringToFile(config, content, Constants.CHAREST_UTF8);
				return true;
			} catch (IOException e) {
				return false;
			}
		}

		@Override
		public String toString() {
			return "PUT_NEW_CONFIG";
		}
	};

	public static final ConfigUpgradeStep BACKUP_OLD_CONFIG = new ConfigUpgradeStep(PUT_NEW_CONFIG, FAIL, 3) {
		@Override
		public int doStep(Task task) throws Exception {
			ConfigUpgradeTask configUpgradeTask = (ConfigUpgradeTask) task;
			String fileName = configUpgradeTask.getConfigFileName();

			for (String vsName : configUpgradeTask.getVirtualServerNames()) {
				if (!backupConfigFile(vsName, fileName)) {
					return Step.CODE_FAIL;
				}
			}
			return Step.CODE_SUCCESS;
		}

		private boolean backupConfigFile(String vsName, String fileName) {
			File vsDir = new File(ConfigureManager.getNginxConfDir(), vsName);
			File config = new File(vsDir, fileName);

			if (config.exists() && config.isFile()) {
				return config.renameTo(new File(vsDir, generateBackupFileName(fileName)));
			}
			return true;
		}

		@Override
		public String toString() {
			return "BACKUP_OLD_CONFIG";
		}
	};

	public static final ConfigUpgradeStep CHECK_ARGUMENT = new ConfigUpgradeStep(BACKUP_OLD_CONFIG, FAIL, 2) {
		@Override
		public int doStep(Task task) throws Exception {
			ConfigUpgradeTask configUpgradeTask = (ConfigUpgradeTask) task;

			if (configUpgradeTask.getVirtualServerNames() == null || configUpgradeTask.getVersions() == null) {
				return Step.CODE_FAIL;
			}

			int length = configUpgradeTask.getVirtualServerNames().length;

			Map<String, String> dynamicVsPostData = configUpgradeTask.getDynamicVsPostData();

			if (length != configUpgradeTask.getVersions().length || length != dynamicVsPostData.size()) {
				return Step.CODE_FAIL;
			}
			for (String version : configUpgradeTask.getVersions()) {
				if (StringUtils.isEmpty(version)) {
					return Step.CODE_FAIL;
				}
			}
			for (String vsName : configUpgradeTask.getVirtualServerNames()) {
				if (StringUtils.isEmpty(vsName)) {
					return Step.CODE_FAIL;
				}
				if (!dynamicVsPostData.containsKey(vsName)) {
					return Step.CODE_FAIL;
				}
			}
			return Step.CODE_SUCCESS;
		}

		@Override
		public String toString() {
			return "CHECK_ARGUMENT";
		}
	};

	public static final ConfigUpgradeStep INIT = new ConfigUpgradeStep(CHECK_ARGUMENT, FAIL, 1) {
		@Override
		public int doStep(Task task) throws Exception {
			ConfigUpgradeTask configUpgradeTask = (ConfigUpgradeTask) task;

			for (String vsName : configUpgradeTask.getVirtualServerNames()) {
				File vsDir = new File(ConfigureManager.getNginxConfDir(), vsName);

				if (!vsDir.exists() || vsDir.isFile()) {
					if (!vsDir.mkdirs()) {
						return Step.CODE_FAIL;
					}
				}
			}
			return Step.CODE_SUCCESS;
		}

		@Override
		public String toString() {
			return "INIT";
		}
	};

	private ConfigUpgradeStep m_nextSuccessStep;

	private ConfigUpgradeStep m_nextFailStep;

	private int m_sequence;

	private ConfigUpgradeStep(ConfigUpgradeStep nextSuccessStep, ConfigUpgradeStep nextFailStep, int sequence) {
		m_nextSuccessStep = nextSuccessStep;
		m_nextFailStep = nextFailStep;
		m_sequence = sequence;
	}

	@Override
	public Step getNextStep(int status) {
		if (status == CODE_SUCCESS) {
			return m_nextSuccessStep;
		} else {
			return m_nextFailStep;
		}
	}

	@Override
	public int getTotalSteps() {
		return 7;
	}

	@Override
	public Map<String, String> getHeader() {
		Map<String, String> header = new HashMap<String, String>();

		header.put(HEADER_STEP, toString());
		header.put(HEADER_PROGRESS, String.format("%s/%s", m_sequence, getTotalSteps()));
		return header;
	}

	private static String generateBackupFileName(String configFileName) {
		return "." + configFileName + "bak";
	}

}