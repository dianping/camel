package com.dianping.platform.slb.agent.core.workflow.deploy;

import com.dianping.platform.slb.agent.core.script.ScriptFileManager;
import com.dianping.platform.slb.agent.core.workflow.Context;
import com.dianping.platform.slb.agent.core.workflow.Step;
import com.dianping.platform.slb.agent.web.ApiController;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class DefaultDeployStepProvider implements DeployStepProvider {

	private final Logger logger = LoggerFactory.getLogger(DefaultDeployStepProvider.class);

	private static final String VERSION_FILE_NAME = ".version";

	private static final String TENGINE_DOC_BASE_DIR = "/usr/local/nginx/conf/phoenix-slb";

	private ScriptFileManager m_scriptFileManager;

	public DefaultDeployStepProvider(ScriptFileManager scriptFileManager) {
		this.m_scriptFileManager = scriptFileManager;
	}

	private int runShellCmd(String shellFunc, Context ctx) throws Exception {
		DeployContext myCtx = (DeployContext) ctx;
		String script = jointShellCmd(shellFunc, (ApiController.DeployTask) myCtx.getTask());

		int exitCode = myCtx.getScriptExecutor().exec(script, myCtx.getOutput(), myCtx.getOutput());
		return exitCode;
	}

	private int runDynamicRefreshShellCmd(Context ctx) throws Exception {
		DeployContext myCtx = (DeployContext) ctx;
		ApiController.DeployTask task = (ApiController.DeployTask) ctx.getTask();

		if (task.getRefreshPostDataCollection() == null || task.getRefreshPostDataCollection().isEmpty()) {
			logger.info("No need to refresh upstream.");
		} else {
			for (Map<String, String> postDataItem : task.getRefreshPostDataCollection()) {
				StringBuilder script = new StringBuilder();

				script.append(m_scriptFileManager.getTengineScript());
				script.append(String.format(" --env \"dev\" "));
				script.append(String.format(" --dynamic_refresh_url \"%s\" ", postDataItem.get("url")));
				script.append(String.format(" --refresh_method \"%s\" ", postDataItem.get("method").toUpperCase()));
				script.append(String.format(" --dynamic_refresh_post_data \"%s\" ",
						escapeArgument(postDataItem.get("data"))));
				script.append(String.format(" --func \"%s\" ", "dynamic_refresh_config"));
				int exitCode = myCtx.getScriptExecutor().exec(script.toString(), myCtx.getOutput(), myCtx.getOutput());

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

	private String jointShellCmd(String shellFunc, ApiController.DeployTask task) {
		StringBuilder sb = new StringBuilder();

		sb.append(m_scriptFileManager.getTengineScript());
		sb.append(String.format(" --tengine_config_doc_base \"%s\" ", TENGINE_DOC_BASE_DIR));
		sb.append(String.format(" --config_file \"%s\" ", task.getConfig()));
		sb.append(String.format(" --virtual_server_names \"%s\" ", StringUtils.join(task.getVsArray(), ",")));
		sb.append(String.format(" --versions \"%s\" ", StringUtils.join(task.getVersionArray(), ",")));
		sb.append(String.format(" --env \"dev\" "));
		sb.append(String.format(" --tengine_reload \"%s\" ", task.isReloadBoolean() ? "1" : "0"));
		sb.append(String.format(" --func \"%s\" ", shellFunc));
		return sb.toString();
	}

	@Override
	public int init(Context ctx) throws Exception {
		ApiController.DeployTask task = (ApiController.DeployTask) ctx.getTask();

		for (String vsName : task.getVsArray()) {
			File dir = new File(TENGINE_DOC_BASE_DIR, vsName);

			if (!dir.exists()) {
				if (!dir.mkdirs()) {
					logger.error("[init][make dir fails]" + dir);
					return Step.CODE_ERROR;
				}
			}
		}
		return Step.CODE_OK;
	}

	@Override
	public int checkArgument(Context ctx) throws Exception {
		ApiController.DeployTask task = (ApiController.DeployTask) ctx.getTask();

		if (task.getVersionArray() == null) {
			logger.error("versions must not be null.");
			return Step.CODE_ERROR;
		}

		if (task.getVsArray() == null) {
			logger.error("virtualServerNames must not be null");
			return Step.CODE_ERROR;
		}

		if (task.getVsArray().length != task.getVersionArray().length) {
			logger.error("virtualServerNames.length != versions.length." + task.getVsArray().length + "/" + task
					.getVersionArray().length);
			return Step.CODE_ERROR;
		}

		if (task.getVsPostDataCollection().size() != task.getVsArray().length) {
			logger.error("virtualServerNames.length != vsPostData.length" + task.getVsArray().length + "/" + task
					.getVsPostDataCollection().size());
			return Step.CODE_ERROR;
		}

		for (String version : task.getVsArray()) {
			if (StringUtils.isBlank(version)) {
				logger.error("versions has blank element");
				return Step.CODE_ERROR;
			}
		}

		for (String vsName : task.getVsArray()) {
			if (StringUtils.isBlank(vsName)) {
				logger.error("virtualServerNames has blank element");
				return Step.CODE_ERROR;
			}
		}

		return Step.CODE_OK;
	}

	@Override
	public int backupOldAndPutNewConfig(Context ctx) throws Exception {
		ApiController.DeployTask task = (ApiController.DeployTask) ctx.getTask();
		String fileName = task.getConfig();

		for (String vsName : task.getVsArray()) {
			int result = backUpOldFile(TENGINE_DOC_BASE_DIR, vsName, fileName);

			if (result != Step.CODE_OK) {
				return result;
			}
			result = putNewConfig(TENGINE_DOC_BASE_DIR, vsName, fileName, task.getVsPostDataCollection().get(vsName));
			if (result != Step.CODE_OK) {
				return result;
			}

		}

		return Step.CODE_OK;
	}

	private int putNewConfig(String tengineConfigDocBase, String vsName, String fileName, String serverConfig) {
		File dst = new File(new File(tengineConfigDocBase, vsName), fileName);

		try {
			FileUtils.writeStringToFile(dst, serverConfig);
		} catch (IOException e) {
			logger.error("[putNewConfig]" + dst, e);
			return Step.CODE_ERROR;
		}
		return Step.CODE_OK;

	}

	private int backUpOldFile(String tengineConfigDocBase, String vsName, String fileName) {

		File tengineConfig = new File(tengineConfigDocBase, vsName);
		File configFile = new File(tengineConfig, fileName);
		if (!configFile.exists()) {
			if (logger.isInfoEnabled()) {
				logger.info("[backUpOldFile][old file not exist, may be first time]" + configFile);
			}
			return Step.CODE_OK;
		}

		if (!configFile.renameTo(new File(tengineConfig, getBackUpFileName(fileName)))) {
			logger.error("[backUpOldFile][fail]" + configFile);
			return Step.CODE_ERROR;
		}

		return Step.CODE_OK;
	}

	private String getBackUpFileName(String originalName) {

		return "." + originalName + ".bak";
	}

	@Override
	public int reloadOrDynamicRefreshConfig(Context ctx) throws Exception {
		if (((ApiController.DeployTask) ctx.getTask()).isReloadBoolean()) {
			if (logger.isInfoEnabled()) {
				logger.info("[reload]");
			}
			return runShellCmd("reload_config", ctx);
		} else {
			if (logger.isInfoEnabled()) {
				logger.info("[refresh]");
			}
			return runDynamicRefreshShellCmd(ctx);
		}
	}

	@Override
	public int commit(Context ctx) throws Exception {
		ApiController.DeployTask task = (ApiController.DeployTask) ctx.getTask();
		String[] versions = task.getVersionArray();
		int i = 0;

		for (String vsName : task.getVsArray()) {
			String version = versions[i];
			i++;
			File versionFile = new File(new File(TENGINE_DOC_BASE_DIR, vsName), VERSION_FILE_NAME);
			FileUtils.writeByteArrayToFile(versionFile, version.getBytes());
		}
		return Step.CODE_OK;
	}

	@Override
	public int rollback(Context ctx) throws Exception {
		ApiController.DeployTask task = (ApiController.DeployTask) ctx.getTask();
		String fileName = task.getConfig();
		int result = Step.CODE_OK;

		for (String vsName : task.getVsArray()) {
			if (rollbackFile(vsName, fileName) != Step.CODE_OK) {
				result = Step.CODE_ERROR;
			}
		}
		if (runShellCmd("reload_config", ctx) != Step.CODE_OK) {
			result = Step.CODE_ERROR;
		}

		return result;
	}

	private int rollbackFile(String vsName, String fileName) {
		File tengineConfig = new File(TENGINE_DOC_BASE_DIR, vsName);
		File configFile = new File(tengineConfig, fileName);
		File backFile = new File(tengineConfig, getBackUpFileName(fileName));

		if (!backFile.exists()) {
			if (logger.isInfoEnabled()) {
				logger.info("[backup file not exist][may be first time, just continue]" + backFile);
			}
			if (configFile.exists() && !configFile.delete()) {
				if (logger.isInfoEnabled()) {
					logger.info("[backup file not exist][delete server.conf fail]");
				}
				return Step.CODE_ERROR;
			}
			return Step.CODE_OK;
		}

		if (!backFile.renameTo(configFile)) {

			logger.error("[rollbackFile][fail]" + backFile);
			return Step.CODE_ERROR;
		}
		backFile.delete();
		return Step.CODE_OK;
	}
}
