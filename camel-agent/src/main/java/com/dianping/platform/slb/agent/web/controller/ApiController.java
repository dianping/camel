package com.dianping.platform.slb.agent.web.controller;

import com.dianping.platform.slb.agent.conf.ConfigureManager;
import com.dianping.platform.slb.agent.constant.Constants;
import com.dianping.platform.slb.agent.shell.ScriptExecutor;
import com.dianping.platform.slb.agent.shell.impl.DefaultScriptExecutor;
import com.dianping.platform.slb.agent.task.model.SubmitResult;
import com.dianping.platform.slb.agent.task.model.config.upgrade.ConfigUpgradeTask;
import com.dianping.platform.slb.agent.task.processor.TransactionProcessor;
import com.dianping.platform.slb.agent.transaction.Transaction;
import com.dianping.platform.slb.agent.transaction.manager.TransactionManager;
import com.dianping.platform.slb.agent.web.api.API;
import com.dianping.platform.slb.agent.web.model.Response;
import com.dianping.platform.slb.agent.web.wrapper.ResponseAction;
import com.dianping.platform.slb.agent.web.wrapper.Wrapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
				if (needReload && StringUtils.isBlank(refreshPostDataStr)) {
					throw new IllegalArgumentException("refreshPostData cannot be null when not need reload");
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
	@RequestMapping(params = "op=log")
	public Response fetchLog(
			@RequestParam("deployId")
			final long deployId) {
		return null;
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
		})
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
	@RequestMapping(params = "op=listvs")
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
