package com.dianping.platform.slb.agent.web.controller;

import com.dianping.platform.slb.agent.conf.ConfigureManager;
import com.dianping.platform.slb.agent.constant.Constants;
import com.dianping.platform.slb.agent.shell.ScriptExecutor;
import com.dianping.platform.slb.agent.shell.impl.DefaultScriptExecutor;
import com.dianping.platform.slb.agent.task.model.SubmitResult;
import com.dianping.platform.slb.agent.task.model.config.upgrade.ConfigUpgradeTask;
import com.dianping.platform.slb.agent.task.model.file.FileUpdateTask;
import com.dianping.platform.slb.agent.task.processor.TransactionProcessor;
import com.dianping.platform.slb.agent.task.workflow.engine.Engine;
import com.dianping.platform.slb.agent.task.workflow.step.Step;
import com.dianping.platform.slb.agent.task.workflow.step.impl.FileUpdateStep;
import com.dianping.platform.slb.agent.transaction.Transaction;
import com.dianping.platform.slb.agent.transaction.manager.TransactionManager;
import com.dianping.platform.slb.agent.utils.CharacterReplaceFilterWriter;
import com.dianping.platform.slb.agent.web.api.API;
import com.dianping.platform.slb.agent.web.model.Response;
import com.dianping.platform.slb.agent.web.wrapper.ResponseAction;
import com.dianping.platform.slb.agent.web.wrapper.Wrapper;
import com.dianping.platform.slb.agent.web.wrapper.impl.DefaultResponseAction;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@RestController
@RequestMapping("/slb/agent/nginx")
public class ApiController implements API {

	@Autowired
	private ResponseAction m_responseAction;

	@Autowired
	private TransactionProcessor m_transactionProcessor;

	@Autowired
	private TransactionManager m_transactionManager;

	@Autowired
	private Engine m_engine;

	@Override
	@RequestMapping(params = "op=deploy", method = RequestMethod.POST)
	public Response deploy(
			@RequestParam("deployId")
			final long deployId,
			@RequestParam("vs")
			final String vsName,
			@RequestParam("version")
			final String version,
			@RequestParam("config")
			final String config,
			@RequestParam("reload")
			final boolean needReload,
			@RequestParam(value = "refreshPostData", required = false)
			final String refreshPostDataStr,
			@RequestParam("vsPostData")
			final String vsPostDataStr) {
		final Response response = new Response();

		return m_responseAction.doTransaction(response, "deploy exception", new Wrapper<Response>() {
			@Override
			public Response doAction() throws Exception {
				if (deployId <= 0) {
					throw new IllegalArgumentException("deployId is invalid.");
				}

				String[] vsNames = vsName.trim().split(",");
				String[] versions = version.trim().split(",");

				if (vsNames == null || versions == null || vsNames.length != versions.length) {
					throw new IllegalArgumentException("vs count not equal version count.");
				}
				if (!needReload && StringUtils.isBlank(refreshPostDataStr)) {
					throw new IllegalArgumentException("refreshPostData cannot be null when no need reload nginx");
				}

				List<Map<String, String>> refreshPostData = null;

				if (needReload) {
					Gson gson = new Gson();

					refreshPostData = gson.fromJson(refreshPostDataStr, new TypeToken<List<Map<String, String>>>() {
					}.getType());
				}

				Gson gson = new Gson();
				Map<String, String> vsPostData = gson.fromJson(vsPostDataStr, new TypeToken<Map<String, String>>() {
				}.getType());

				ConfigUpgradeTask configUpgradeTask = new ConfigUpgradeTask(vsNames, versions, config, needReload,
						refreshPostData, vsPostData);
				Transaction transaction = new Transaction(deployId, configUpgradeTask);
				SubmitResult submitResult = m_transactionProcessor.submit(transaction);

				if (submitResult.isAccepted()) {
					response.setStatus(Response.Status.SUCCESS);
					response.setMessage(submitResult.getMessage());
				} else {
					response.setStatus(Response.Status.FAIL);
					response.setMessage(submitResult.getMessage());
				}
				return response;
			}
		});
	}

	@Override
	@RequestMapping(params = "op=status")
	public Response fetchStatus(
			@RequestParam("deployId")
			final long deployId) {
		final Response response = new Response();

		return m_responseAction.doTransaction(response, "load transaction error", new Wrapper<Response>() {
			@Override
			public Response doAction() throws Exception {
				Transaction transaction = m_transactionManager.loadTransaction(deployId);

				response.setStatus(transaction.getStatus().toString().toLowerCase());
				return response;
			}
		});
	}

	@Override
	@RequestMapping(params = "op=update")
	public Response update(
			@RequestParam("vs")
			final String vsName,
			@RequestParam("fileName")
			final String fileName,
			@RequestParam("vsPostData")
			final String vsPostData) {
		final Response response = new Response();

		return m_responseAction.doTransaction(response, "update file error", new Wrapper<Response>() {
			@Override
			public Response doAction() throws Exception {
				String[] vsNames = vsName.split(",");
				String[] fileNames = fileName.split(",");
				Map<String, String[]> fileContents = new Gson()
						.fromJson(vsPostData, new TypeToken<Map<String, String[]>>() {
						}.getType());

				validateParameters(vsNames, fileNames, fileContents);

				FileUpdateTask task = new FileUpdateTask(vsNames, fileNames, fileContents);
				OutputStream outputStream = new ByteArrayOutputStream();

				task.setTaskOutputStream(outputStream);

				int statusCode = m_engine.executeStep(FileUpdateStep.INIT, task);

				if (statusCode == Step.CODE_SUCCESS) {
					response.setStatus(Response.Status.SUCCESS);
				} else {
					response.setStatus(Response.Status.FAIL);
				}
				response.setMessage(outputStream.toString());
				return response;
			}

			private void validateParameters(String[] vsNames, String[] fileNames, Map<String, String[]> fileContents) {
				Validate.isTrue(vsNames != null && fileNames != null);
				Validate.isTrue(vsNames.length == fileContents.size());
				Validate.isTrue(new ArrayList<>(fileContents.values()).get(0).length == fileNames.length);
			}
		});
	}

	@Override
	@RequestMapping(params = "op=log")
	public Response fetchLog(
			@RequestParam("deployId")
			long deployId,
			@RequestParam(value = "offset", required = false)
			Integer offset,
			@RequestParam(value = "br", required = false)
			Integer br, HttpServletResponse httpResponse) {
		try {
			if (offset == null) {
				offset = 0;
			}
			if (br == null) {
				br = 0;
			}
			Reader reader = m_transactionManager.getLogReader(deployId, offset);

			while (reader == null && m_transactionProcessor.isTransactionProcessing(deployId)) {
				TimeUnit.MILLISECONDS.sleep(200);
				reader = m_transactionManager.getLogReader(deployId, offset);
			}
			if (reader != null) {
				httpResponse.setHeader("Content-Type", "text/html; charset=UTF-8");
				Writer writer = httpResponse.getWriter();

				if (br > 0) {
					writer = new CharacterReplaceFilterWriter(writer, '\n', "<br/>");
				}
				transfer(reader, writer, m_transactionManager, deployId);
				return null;
			} else {
				throw new IllegalStateException("no log for transaction " + deployId);
			}
		} catch (Exception ex) {
			Response response = new Response();

			response.setStatus(Response.Status.FAIL);
			response.setMessage(DefaultResponseAction.buildErrorMessage("fetch log error", ex));
			return response;
		}
	}

	private void transfer(Reader reader, Writer writer, TransactionManager transactionManager, long deployId)
			throws IOException, ClassNotFoundException {
		char[] cacheChar = new char[4096];

		while (true) {
			int length = reader.read(cacheChar);

			if (length > 0) {
				writer.write(cacheChar, 0, length);
				writer.flush();
			} else {
				Transaction tx = transactionManager.loadTransaction(deployId);

				if (!tx.getStatus().isCompleted()) {
					try {
						TimeUnit.MILLISECONDS.sleep(500);
					} catch (Exception ex) {
						// ignore
					}
				} else {
					break;
				}
			}
		}
	}

	@Override
	@RequestMapping(params = "op=cancel")
	public Response cancel(
			@RequestParam("deployId")
			final long deployId) {
		final Response response = new Response();

		return m_responseAction.doTransaction(response, "cancel failed", new Wrapper<Response>() {
			@Override
			public Response doAction() throws Exception {
				if (m_transactionProcessor.cancel(deployId)) {
					response.setStatus(Response.Status.SUCCESS);
				} else {
					response.setStatus(Response.Status.FAIL);
				}
				return response;
			}
		});
	}

	@Override
	@RequestMapping(params = "op=version")
	public Response fetchVersion(
			@RequestParam("vs")
			final String vsNameStr) {
		final Response response = new Response();

		return m_responseAction.doTransaction(response, "fetch version failed", new Wrapper<Response>() {
			@Override
			public Response doAction() throws Exception {
				if (StringUtils.isEmpty(vsNameStr)) {
					throw new IllegalArgumentException();
				}
				String[] vsNames = vsNameStr.split(",");
				List<String> versions = new ArrayList<String>();

				for (String tmpVsName : vsNames) {
					String currentVsName = tmpVsName.trim();

					versions.add(readVersion(currentVsName));
				}

				response.setMessage(StringUtils.join(versions, ","));
				response.setStatus(Response.Status.SUCCESS);
				return response;
			}

			private String readVersion(String currentVsName) {
				File versionFile = new File(new File(ConfigureManager.getNginxConfDir(), currentVsName), ".version");

				if (versionFile.exists()) {
					try {
						return StringUtils.trimToNull(FileUtils.readFileToString(versionFile, Constants.CHAREST_UTF8));
					} catch (IOException e) {
					}
				}
				return "Unknown version";
			}
		});
	}

	@Override
	@RequestMapping(params = "op=vslist")
	public Response listVsNames() {
		final Response response = new Response();

		return m_responseAction.doTransaction(response, "List vs failed.", new Wrapper<Response>() {
			@Override
			public Response doAction() throws Exception {
				return executScriptWithResponse(ConfigureManager.getCommandListVs(), response, true);
			}
		});
	}

	@Override
	@RequestMapping(params = "op=delvs")
	public Response delVsConfig(
			@RequestParam("vs")
			final String vsName) {
		final Response response = new Response();

		return m_responseAction.doTransaction(response, "Del vs failed.", new Wrapper<Response>() {
			@Override
			public Response doAction() throws Exception {
				return executScriptWithResponse(String.format(ConfigureManager.getCommandDelVsPattern(), vsName),
						response, false);
			}
		});
	}

	@Override
	@RequestMapping(params = "op=reload")
	public Response reloadNginx() {
		final Response response = new Response();

		return m_responseAction.doTransaction(response, "Reload nginx failed.", new Wrapper<Response>() {
			@Override
			public Response doAction() throws Exception {
				return executScriptWithResponse(ConfigureManager.getNginxReloadScriptFile().getAbsolutePath(), response,
						false);
			}
		});
	}

	private Response executScriptWithResponse(String script, Response response, boolean needDealMessage)
			throws IOException {
		ScriptExecutor executor = new DefaultScriptExecutor();
		ByteArrayOutputStream stdOutput = new ByteArrayOutputStream();
		ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();

		if (executor.execute(script, stdOutput, errorOutput) == 0) {
			response.setStatus(Response.Status.SUCCESS);
			if (needDealMessage) {
				response.setMessage(generateVsList(stdOutput.toString()));
			} else {
				response.setMessage(stdOutput.toString());
			}
		} else {
			response.setStatus(Response.Status.FAIL);
			response.setMessage(errorOutput.toString());
		}
		return response;
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

}
