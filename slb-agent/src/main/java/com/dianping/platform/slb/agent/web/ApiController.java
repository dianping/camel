package com.dianping.platform.slb.agent.web;

import com.dianping.platform.slb.agent.core.script.DefaultScriptExecutor;
import com.dianping.platform.slb.agent.core.script.ScriptExecutor;
import com.dianping.platform.slb.agent.core.script.ScriptFileManager;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

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

	public static class Parameter {

		private String m_name;

		public String getName() {
			return m_name;
		}

		public void setName(String name) {
			m_name = name;
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
