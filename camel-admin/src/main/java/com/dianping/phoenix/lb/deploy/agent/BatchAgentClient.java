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
import com.dianping.phoenix.lb.utils.IOUtilsWrapper;
import com.dianping.phoenix.lb.visitor.VirtualServerComparisionVisitor;
import com.dianping.phoenix.lb.visitor.VirtualServerComparisionVisitor.ComparisionResult;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;

public class BatchAgentClient extends AbstractAgentClient implements AgentClient {

	private List<String> vsNames;

	private List<String> tags;

	public BatchAgentClient(long deployId, List<String> vsNames, List<String> tags, String ip,
			VirtualServerService virtualServerService, StrategyService strategyService, ConfigManager configManager,
			F5ApiService f5ApiService, ExecutorService executor) {
		super(virtualServerService, configManager, strategyService, f5ApiService, ip, false, deployId, executor);
		this.vsNames = vsNames;
		this.tags = tags;
	}

	@Override
	public void execute() {
		result.setStatus(DeployAgentStatus.PROCESSING);
		result.logInfo(String.format("Deploying phoenix-slb config(%s) to host(%s) for deploy(%s) of vs(%s)  ... ",
				StringUtils.join(tags, ","), ip, deployId, StringUtils.join(vsNames, ",")));

		if (!validateParams()) {
			endWithFail();
			return;
		}

		String[] currentWorkingVersions = getAgentConfigVersions();
		List<SlbModelTree> slbModelTrees = new LinkedList<SlbModelTree>();

		if (currentWorkingVersions != null) {
			try {
				List<String> virtualServersToDeploy = new ArrayList<String>();
				List<String> versionsToDeploy = new ArrayList<String>();
				Map<String, ComparisionResult> comparisionResults = new HashMap<String, ComparisionResult>();

				boolean needReload = false;

				result.logInfo("check need reload begin");
				for (int i = 0; i < currentWorkingVersions.length; i++) {
					String vsName = vsNames.get(i);
					String tag = tags.get(i);
					String currentWorkingVersion = currentWorkingVersions[i];

					SlbModelTree deployingSlbModelTree = virtualServerService.findTagById(vsName, tag);

					if (!sameVersion(vsName, currentWorkingVersion, tag)) {
						SlbModelTree currentWorkingSlbModelTree = virtualServerService
								.findTagById(vsName, currentWorkingVersion);

						if (!FIRST_VERSION.equals(currentWorkingVersion)) {
							if (!versionExists(vsName, currentWorkingVersion, currentWorkingSlbModelTree)) {
								needReload = true;
							}

							if (!versionExists(vsName, tag, deployingSlbModelTree)) {
								endWithFail();
								return;
							} else {
								if (!needReload) {
									VirtualServerComparisionVisitor comparisionVisitor = new VirtualServerComparisionVisitor(
											vsName, currentWorkingSlbModelTree);
									deployingSlbModelTree.accept(comparisionVisitor);
									ComparisionResult compareResult = comparisionVisitor.getVisitorResult();
									comparisionResults.put(vsName, compareResult);

									if (compareResult.needReload()) {
										needReload = true;
									}
								}
							}
						} else {
							needReload = true;
						}

						virtualServersToDeploy.add(vsName);
						versionsToDeploy.add(tag);
						slbModelTrees.add(deployingSlbModelTree);

						VirtualServer deployVs = deployingSlbModelTree.findVirtualServer(vsName);

						if (isHttpsInfoChanged(deployVs, currentWorkingSlbModelTree.findVirtualServer(vsName))) {
							updateAgentSslFile(deployVs);
							needReload = true;
						}
					}
				}
				result.logInfo("check need reload end");

				boolean callAgentResult = true;
				if (!versionsToDeploy.isEmpty() && !virtualServersToDeploy.isEmpty()) {
					if (needReload) {
						callAgentResult = callAgentWithReload(virtualServersToDeploy, versionsToDeploy, slbModelTrees);
					} else {
						callAgentResult = callAgentWithDynamicRefresh(virtualServersToDeploy, versionsToDeploy,
								comparisionResults, slbModelTrees);
					}

					if (callAgentResult) {
						result.logInfo("Agent accepted.");
						readLog();
					}
				} else {
					result.logInfo("All virtualServers are no need to deploy.");
					endWithSuccess();
				}

			} catch (Throwable e) {
				result.logError("Exception occurs", e);
				endWithFail();
				return;
			}
		}

	}

	private boolean callAgentWithDynamicRefresh(List<String> virtualServersToDeploy, List<String> versionsToDeploy,
			Map<String, ComparisionResult> compareResults, List<SlbModelTree> slbModelTrees)
			throws MalformedURLException, BizException, IOException, ProtocolException, UnsupportedEncodingException,
			InterruptedException {

		result.logInfo("No need to reload nginx, switch to dynamic refresh strategy");
		URL deployUrl = new URL(configManager
				.getDeployWithDynamicRefreshUrl(ip, deployId, StringUtils.join(virtualServersToDeploy, ","),
						configManager.getTengineConfigFileName(), StringUtils.join(versionsToDeploy, ",")));

		result.logInfo("Begin get refresh data");
		String dynamicRefreshPostData = genDynamicRefreshPostData(compareResults);
		result.logInfo("Begin get vs data");
		String dynamicVsPostData = genDynamicVsData(slbModelTrees);
		result.logInfo(String.format("Deploy url is %s, post data is %s", deployUrl, dynamicRefreshPostData));

		return sendPostData(deployUrl, "POST", REFRESH_POST_DATA_KEY, dynamicRefreshPostData, VS_POST_DATA_KEY,
				dynamicVsPostData);
	}

	private boolean callAgentWithReload(List<String> virtualServersToDeploy, List<String> versionsToDeploy,
			List<SlbModelTree> slbModelTrees)
			throws MalformedURLException, IOException, BizException, InterruptedException {
		result.logInfo("Need to reload nginx");

		if (configManager.isNeedCallF5()) {
			result.logInfo(String.format("Disabling host(%s) from F5 ... ", ip));
			f5ApiService.offline(ip, result);
			needOnline = true;
			result.logInfo(String.format("host(%s) disabled.", ip));
		}

		URL deployUrl = new URL(configManager
				.getDeployWithReloadUrl(ip, deployId, StringUtils.join(virtualServersToDeploy, ","),
						configManager.getTengineConfigFileName(), StringUtils.join(versionsToDeploy, ",")));

		result.logInfo("Begin get vs data");
		String vsData = genDynamicVsData(slbModelTrees);

		result.logInfo(String.format("Deploy url is %s", deployUrl));
		return sendPostData(deployUrl, "POST", VS_POST_DATA_KEY, vsData);
	}

	private boolean validateParams() {
		if (vsNames == null || tags == null) {
			result.logError("vsNames or tags is null");
			return false;
		}

		if (vsNames.size() != tags.size()) {
			result.logError("vsNames.size != tags.size");
			return false;
		}

		for (String vs : vsNames) {
			if (StringUtils.isBlank(vs)) {
				result.logError("vsNames has blank element.");
				return false;
			}
		}

		for (String tag : tags) {
			if (StringUtils.isBlank(tag)) {
				result.logError("tags has blank element.");
				return false;
			}
		}

		return true;
	}

	private String[] getAgentConfigVersions() {
		try {
			URL versionUrl = new URL(configManager.getAgentTengineConfigVersionUrl(ip, StringUtils.join(vsNames, ",")));
			result.logInfo(
					String.format("Fetching versions of current working config through url %s", versionUrl.toString()));

			HttpURLConnection connection = (HttpURLConnection) versionUrl.openConnection();
			connection.setConnectTimeout(configManager.getDeployConnectTimeout());

			String responseStr = IOUtilsWrapper.convetStringFromRequest(connection.getInputStream());

			Response response;

			response = DefaultJsonParser.parse(Response.class, responseStr);

			if (RESP_MSG_OK.equals(response.getStatus()) && StringUtils.isNotBlank(response.getMessage())) {
				String versionStr = StringUtils.trim(response.getMessage());
				result.logInfo(String.format("Versions fetched, current working config versions are %s", versionStr));

				if (StringUtils.isBlank(versionStr)) {
					result.logError("Versions fetched is blank");
					endWithFail();
					return null;
				}

				String[] versions = versionStr.split(",");

				if (versions == null) {
					result.logError("Versions splitted is null");
					endWithFail();
					return null;
				}

				if (versions.length != vsNames.size()) {
					result.logError("Versions.size splitted != vsNames.size");
					endWithFail();
					return null;
				}

				result.logInfo("check version begin");
				for (int i = 0; i < versions.length; i++) {
					String vs = this.vsNames.get(i);
					String version = versions[i];
					if ("Unknown version".equals(version)) {
						result.logInfo(String.format("Vs(%s) is the first deployment for host(%s)", vs, this.ip));
						versions[i] = FIRST_VERSION;
					} else {
						versions[i] = getRealVersion(result, vs, version);
					}
				}
				result.logInfo("check version end");

				return versions;
			}

		} catch (Exception e) {
			result.logError("Exception occurs while fetching version of current working config", e);
			endWithFail();
		}
		return null;
	}
}
