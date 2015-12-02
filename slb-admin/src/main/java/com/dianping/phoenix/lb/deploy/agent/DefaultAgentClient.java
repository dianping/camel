package com.dianping.phoenix.lb.deploy.agent;

import com.dianping.phoenix.agent.response.entity.Response;
import com.dianping.phoenix.agent.response.transform.DefaultJsonParser;
import com.dianping.phoenix.lb.configure.ConfigManager;
import com.dianping.phoenix.lb.deploy.f5.F5ApiService;
import com.dianping.phoenix.lb.deploy.model.DeployAgentStatus;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.SlbModelTree;
import com.dianping.phoenix.lb.model.entity.VirtualServer;
import com.dianping.phoenix.lb.service.model.StrategyService;
import com.dianping.phoenix.lb.service.model.VirtualServerService;
import com.dianping.phoenix.lb.utils.GsonUtils;
import com.dianping.phoenix.lb.utils.IOUtilsWrapper;
import com.dianping.phoenix.lb.visitor.VirtualServerComparisionVisitor;
import com.dianping.phoenix.lb.visitor.VirtualServerComparisionVisitor.ComparisionResult;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class DefaultAgentClient extends AbstractAgentClient implements AgentClient {

	private String vsName;

	private String tag;

	public DefaultAgentClient(long deployId, String vsName, String tag, String ip,
			VirtualServerService virtualServerService, StrategyService strategyService, ConfigManager configManager,
			F5ApiService f5ApiService, ExecutorService executor) {

		super(virtualServerService, configManager, strategyService, f5ApiService, ip, false, deployId, executor);
		this.vsName = vsName;
		this.tag = tag;
	}

	@Override
	public void execute() {
		long start = System.currentTimeMillis();
		result.setStatus(DeployAgentStatus.PROCESSING);
		result.logInfo(
				String.format("Deploying phoenix-slb config(%s) to host(%s) for deploy(%s) of vs(%s)  ... ", tag, ip,
						deployId, vsName));

		String currentWorkingVersion = getAgentConfigVersion();
		ComparisionResult compareResult = null;

		result.setOldTag(currentWorkingVersion);
		try {
			Validate.isTrue(StringUtils.isNotBlank(currentWorkingVersion),
					"currentWorkingVersion is blank: " + currentWorkingVersion);

			if (sameVersion(vsName, currentWorkingVersion, tag)) {
				endWithSuccess();
				return;
			}

			SlbModelTree deployingSlbModelTree = virtualServerService.findTagById(vsName, tag);
			boolean needReload = false;

			if (!FIRST_VERSION.equals(currentWorkingVersion)) {
				SlbModelTree currentWorkingSlbModelTree = virtualServerService
						.findTagById(vsName, currentWorkingVersion);

				if (!versionExists(vsName, currentWorkingVersion, currentWorkingSlbModelTree)) {
					needReload = true;
				}
				if (!versionExists(vsName, tag, deployingSlbModelTree)) {
					endWithFail();
					return;
				}

				VirtualServer deployVs = deployingSlbModelTree.findVirtualServer(vsName);

				if (isHttpsInfoChanged(deployVs, currentWorkingSlbModelTree.findVirtualServer(vsName))) {
					updateAgentSslFile(deployVs);
					needReload = true;
				}

				if (!needReload) {
					VirtualServerComparisionVisitor comparisionVisitor = new VirtualServerComparisionVisitor(vsName,
							currentWorkingSlbModelTree);
					deployingSlbModelTree.accept(comparisionVisitor);
					compareResult = comparisionVisitor.getVisitorResult();

					if (compareResult.needReload()) {
						needReload = true;
					}
				}
			} else {
				needReload = true;
			}

			if (needReload) {
				if (!callAgentWithReload(deployingSlbModelTree)) {
					return;
				}
			} else {
				if (!callAgentWithDynamicRefresh(compareResult, deployingSlbModelTree)) {
					return;
				}
			}

			result.logInfo("Agent accepted.");

			readLog();

		} catch (Throwable e) {
			logger.error("[execute]", e);
			result.logError("Exception occurs", e);
			endWithFail();
			return;
		}

		logger.info("Time for single agent: " + (System.currentTimeMillis() - start));
	}

	private boolean callAgentWithDynamicRefresh(ComparisionResult compareResult, SlbModelTree deployingSlbModelTree)
			throws MalformedURLException, BizException, IOException, ProtocolException, UnsupportedEncodingException {
		result.logInfo("No need to reload nginx, switch to dynamic refresh strategy");
		URL deployUrl = new URL(configManager
				.getDeployWithDynamicRefreshUrl(ip, deployId, vsName, configManager.getTengineConfigFileName(), tag));

		String dynamicRefreshPostData = genDynamicRefreshPostData(compareResult, vsName);
		String dynamicVsPostData = genDynamicVsData(deployingSlbModelTree);

		result.logInfo(String.format("Deploy url is %s, post data is %s", deployUrl, dynamicRefreshPostData));

		return sendPostData(deployUrl, "POST", REFRESH_POST_DATA_KEY, dynamicRefreshPostData, VS_POST_DATA_KEY,
				dynamicVsPostData);
	}

	private boolean callAgentWithReload(SlbModelTree deployingSlbModelTree)
			throws MalformedURLException, IOException, BizException {

		result.logInfo("Need to reload nginx");

		if (configManager.isNeedCallF5()) {
			result.logInfo(String.format("Disabling host(%s) from F5 ... ", ip));
			f5ApiService.offline(ip, result);
			needOnline = true;
			result.logInfo(String.format("host(%s) disabled.", ip));
		}

		String reloadData = genDynamicVsData(deployingSlbModelTree);
		URL deployUrl = new URL(configManager
				.getDeployWithReloadUrl(ip, deployId, vsName, configManager.getTengineConfigFileName(), tag));
		result.logInfo(String.format("Deploy url is %s", deployUrl));

		return sendPostData(deployUrl, "POST", VS_POST_DATA_KEY, reloadData);

	}

	private String getAgentConfigVersion() {
		try {
			URL versionUrl = new URL(configManager.getAgentTengineConfigVersionUrl(ip, vsName));
			result.logInfo(
					String.format("Fetching version of current working config through url %s", versionUrl.toString()));

			HttpURLConnection connection = (HttpURLConnection) versionUrl.openConnection();
			connection.setConnectTimeout(configManager.getDeployConnectTimeout());

			String responseStr = IOUtilsWrapper.convetStringFromRequest(connection.getInputStream());

			Response response = DefaultJsonParser.parse(Response.class, responseStr);

			if (RESP_MSG_OK.equals(response.getStatus()) && StringUtils.isNotBlank(response.getMessage())) {
				String version = StringUtils.trim(response.getMessage());
				result.logInfo(String.format("Version fetched, current working config version is %s", version));
				if ("Unknown version".equals(version)) {
					result.logInfo(String.format("Vs(%s) is the first deployment for host(%s)", this.vsName, this.ip));
					return FIRST_VERSION;
				}
				return getRealVersion(result, vsName, version);
			}

			endWithFail();
		} catch (Exception e) {
			result.logError("Exception occurs while fetching version of current working config", e);
			endWithFail();
		}
		return null;
	}

}
