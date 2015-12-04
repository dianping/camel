package com.dianping.platform.slb.agent.web;

import com.dianping.platform.slb.agent.core.constant.MessageID;
import com.dianping.platform.slb.agent.core.event.EventTracker;
import com.dianping.platform.slb.agent.core.exception.BizException;
import com.dianping.platform.slb.agent.core.processor.Processor;
import com.dianping.platform.slb.agent.core.script.DefaultScriptExecutor;
import com.dianping.platform.slb.agent.core.script.ScriptExecutor;
import com.dianping.platform.slb.agent.core.script.ScriptFileManager;
import com.dianping.platform.slb.agent.core.task.Task;
import com.dianping.platform.slb.agent.core.transaction.Transaction;
import com.dianping.platform.slb.agent.core.util.ExceptionUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@RequestMapping("/slb/agent/nginx")
@Controller
public class ApiController {

	@Autowired
	private ScriptFileManager m_scriptFileManager;

	@Autowired
	@Qualifier("nginxConfDeployProcessor")
	private Processor m_nginxConfDeployProcessor;

	@RequestMapping(value = "/home")
	@ResponseBody
	public String home() {
		return "hello slb-agent";
	}

	@RequestMapping(value = "/reload")
	@ResponseBody
	public Result reloadNginx() {
		Result result = new Result();
		OutputStream normalOutput = new ByteArrayOutputStream();
		OutputStream errorOutput = new ByteArrayOutputStream();

		try {
			ScriptExecutor scriptExecutor = new DefaultScriptExecutor();
			int execResult = scriptExecutor.exec(m_scriptFileManager.getTengineReloadPath(), normalOutput, errorOutput);

			if (execResult == 0) {
				result.setStatus(ResultStatus.SUCCESS);
			} else {
				result.setStatus(ResultStatus.FAIL);
			}
			result.setMessage(normalOutput.toString());
		} catch (Exception ex) {
			result.setStatus(ResultStatus.FAIL);
			result.setMessage(errorOutput.toString());
		} finally {
			IOUtils.closeQuietly(normalOutput);
			IOUtils.closeQuietly(errorOutput);
			return result;
		}
	}

	@RequestMapping(value = "/vslist")
	@ResponseBody
	public Result fetchVsList() {
		Result result = new Result();
		OutputStream normalOutput = new ByteArrayOutputStream();
		OutputStream errorOutput = new ByteArrayOutputStream();

		try {
			ScriptExecutor scriptExecutor = new DefaultScriptExecutor();
			int execResult = scriptExecutor.exec(m_scriptFileManager.getListVsScript(), normalOutput, errorOutput);

			if (execResult == 0) {
				String vsList = generateVsList(normalOutput.toString());

				result.setMessage(vsList);
				result.setStatus(ResultStatus.SUCCESS);
			} else {
				result.setStatus(ResultStatus.FAIL);
			}
		} catch (Exception ex) {
			result.setStatus(ResultStatus.FAIL);
		} finally {
			IOUtils.closeQuietly(normalOutput);
			IOUtils.closeQuietly(errorOutput);
			return result;
		}
	}

	@RequestMapping(value = "/vs/delete")
	@ResponseBody
	public Result delVirtualServer(String vs) {
		Result result = new Result();
		OutputStream normalOutput = new ByteArrayOutputStream();
		OutputStream errorOutput = new ByteArrayOutputStream();

		try {
			ScriptExecutor scriptExecutor = new DefaultScriptExecutor();
			int execResult = scriptExecutor.exec(m_scriptFileManager.getDelVsScript(vs), normalOutput, errorOutput);

			if (execResult == 0) {
				result.setStatus(ResultStatus.SUCCESS);
			} else {
				result.setStatus(ResultStatus.FAIL);
			}
			result.setMessage(normalOutput.toString());
		} catch (Exception ex) {
			result.setStatus(ResultStatus.FAIL);
			result.setMessage(errorOutput.toString());
		} finally {
			IOUtils.closeQuietly(normalOutput);
			IOUtils.closeQuietly(errorOutput);
			return result;
		}
	}

	@RequestMapping(value = "deploy")
	@ResponseBody
	public Result deploy(DeployTask task) {
		Result result = new Result();

		try {
			task.initArgument();
			Transaction transaction = new Transaction(task, task.getDeployId(), EventTracker.DUMMY_TRACKER);

			Processor.SubmitResult submitResult = m_nginxConfDeployProcessor.submitTransaction(transaction);

			if (submitResult.isResult()) {
				result.setStatus(ResultStatus.SUCCESS);
			} else {
				result.setStatus(ResultStatus.FAIL);
			}
			result.setMessage(submitResult.getMessage());
		} catch (Exception ex) {
			result.setStatus(ResultStatus.FAIL);
			result.setMessage(ex.getMessage());
		} finally {
			return result;
		}
	}

	private String generateVsList(String rawVsList) {
		StringBuilder builder = new StringBuilder();
		boolean isFirst = true;

		for (String str : rawVsList.split("\n")) {
			if (str.startsWith("d")) {
				String[] metrics = str.split("\\s+");
				String dirName = metrics[metrics.length - 1];

				if (isFirst) {
					isFirst = false;

					builder.append(dirName);
				} else {
					builder.append("\t").append(dirName);
				}
			}
		}
		return builder.toString();
	}

	public static class DeployTask implements Task {

		private int m_deployId;

		private String m_vs;

		private String m_config;

		private String m_version;

		private String m_reload;

		private String m_refreshPostData;

		private String m_vsPostData;

		private String[] m_vsArray;

		private String[] m_versionArray;

		private boolean m_reloadBoolean;

		private List<Map<String, String>> m_refreshPostDataCollection;

		private Map<String, String> m_vsPostDataCollection;

		public void initArgument() throws BizException {
			if (m_deployId < 0) {
				ExceptionUtil.logAndRethrowException(MessageID.ARGUMENT_CHECK_FAIL, "deployId");
			}
			if (StringUtils.isEmpty(m_vs)) {
				ExceptionUtil.logAndRethrowException(MessageID.ARGUMENT_CHECK_FAIL, "vs");
			} else {
				m_vsArray = m_vs.split(",");
			}
			if (StringUtils.isEmpty(m_version)) {
				ExceptionUtil.logAndRethrowException(MessageID.ARGUMENT_CHECK_FAIL, "version");
			} else {
				m_versionArray = m_version.split(",");
			}
			if (m_versionArray.length != m_vsArray.length) {
				ExceptionUtil.logAndRethrowException(MessageID.ARGUMENT_CHECK_FAIL, "vs size not match version size");
			}
			if (StringUtils.isEmpty(m_config)) {
				ExceptionUtil.logAndRethrowException(MessageID.ARGUMENT_CHECK_FAIL, "config");
			}
			if (StringUtils.isEmpty(m_reload)) {
				ExceptionUtil.logAndRethrowException(MessageID.ARGUMENT_CHECK_FAIL, "reload");
			} else {
				try {
					m_reloadBoolean = new Boolean(m_reload);
				} catch (Exception ex) {
					ExceptionUtil.logAndRethrowException(MessageID.ARGUMENT_CHECK_FAIL, "reload");
				}
			}
			if (!m_reloadBoolean) {
				if (StringUtils.isEmpty(m_refreshPostData)) {
					ExceptionUtil.logAndRethrowException(MessageID.ARGUMENT_CHECK_FAIL, "refreshPostData");
				} else {
					Gson gson = new Gson();
					m_refreshPostDataCollection = gson
							.fromJson(m_refreshPostData, new TypeToken<List<Map<String, String>>>() {
							}.getType());
				}
			}
			if (StringUtils.isEmpty(m_vsPostData)) {
				ExceptionUtil.logAndRethrowException(MessageID.ARGUMENT_CHECK_FAIL, "vsPostData");
			} else {
				Gson gson = new Gson();
				m_vsPostDataCollection = gson.fromJson(m_vsPostData, new TypeToken<Map<String, String>>() {
				}.getType());
			}
		}

		public int getDeployId() {
			return m_deployId;
		}

		public void setDeployId(int deployId) {
			m_deployId = deployId;
		}

		public String getVs() {
			return m_vs;
		}

		public void setVs(String vs) {
			m_vs = vs;
		}

		public String getConfig() {
			return m_config;
		}

		public void setConfig(String config) {
			m_config = config;
		}

		public String getVersion() {
			return m_version;
		}

		public void setVersion(String version) {
			m_version = version;
		}

		public String getReload() {
			return m_reload;
		}

		public void setReload(String reload) {
			m_reload = reload;
		}

		public String getRefreshPostData() {
			return m_refreshPostData;
		}

		public void setRefreshPostData(String refreshPostData) {
			m_refreshPostData = refreshPostData;
		}

		public String getVsPostData() {
			return m_vsPostData;
		}

		public void setVsPostData(String vsPostData) {
			m_vsPostData = vsPostData;
		}

		public String[] getVsArray() {
			return m_vsArray;
		}

		public void setVsArray(String[] vsArray) {
			m_vsArray = vsArray;
		}

		public String[] getVersionArray() {
			return m_versionArray;
		}

		public void setVersionArray(String[] versionArray) {
			m_versionArray = versionArray;
		}

		public boolean isReloadBoolean() {
			return m_reloadBoolean;
		}

		public void setReloadBoolean(boolean reloadBoolean) {
			m_reloadBoolean = reloadBoolean;
		}

		public List<Map<String, String>> getRefreshPostDataCollection() {
			return m_refreshPostDataCollection;
		}

		public void setRefreshPostDataCollection(List<Map<String, String>> refreshPostDataCollection) {
			m_refreshPostDataCollection = refreshPostDataCollection;
		}

		public Map<String, String> getVsPostDataCollection() {
			return m_vsPostDataCollection;
		}

		public void setVsPostDataCollection(Map<String, String> vsPostDataCollection) {
			m_vsPostDataCollection = vsPostDataCollection;
		}
	}

	public static class Result {

		private String m_status;

		private String m_message;

		public String getStatus() {
			return m_status;
		}

		public void setStatus(ResultStatus status) {
			m_status = status.getMessage();
		}

		public String getMessage() {
			return m_message;
		}

		public void setMessage(String message) {
			m_message = message;
		}
	}

	private enum ResultStatus {

		SUCCESS("ok"), FAIL("error");

		private String m_message;

		private ResultStatus(String message) {
			this.m_message = message;
		}

		public String getMessage() {
			return m_message;
		}

	}

}
