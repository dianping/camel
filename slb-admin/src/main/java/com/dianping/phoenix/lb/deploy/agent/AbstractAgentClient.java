package com.dianping.phoenix.lb.deploy.agent;

import com.dianping.phoenix.agent.response.entity.Response;
import com.dianping.phoenix.agent.response.transform.DefaultJsonParser;
import com.dianping.phoenix.lb.configure.ConfigManager;
import com.dianping.phoenix.lb.deploy.f5.F5ApiService;
import com.dianping.phoenix.lb.deploy.model.DeployAgentStatus;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.State;
import com.dianping.phoenix.lb.model.entity.Member;
import com.dianping.phoenix.lb.model.entity.Pool;
import com.dianping.phoenix.lb.model.entity.SlbModelTree;
import com.dianping.phoenix.lb.model.entity.VirtualServer;
import com.dianping.phoenix.lb.model.nginx.NginxUpstream;
import com.dianping.phoenix.lb.service.model.StrategyService;
import com.dianping.phoenix.lb.service.model.VirtualServerService;
import com.dianping.phoenix.lb.utils.GsonUtils;
import com.dianping.phoenix.lb.utils.IOUtilsWrapper;
import com.dianping.phoenix.lb.utils.PoolNameUtils;
import com.dianping.phoenix.lb.velocity.NginxVelocityTools;
import com.dianping.phoenix.lb.visitor.NginxConfigVisitor;
import com.dianping.phoenix.lb.visitor.VirtualServerComparisionVisitor.ComparisionResult;
import com.dianping.phoenix.lb.visitor.VirtualServerComparisionVisitor.PoolPair;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 抽象层，提取公共方法
 *
 * @author mengwenchao
 *         <p/>
 *         2014年8月7日 下午3:34:52
 */
public abstract class AbstractAgentClient implements AgentClient {

	protected static final String REFRESH_POST_DATA_KEY = "refreshPostData";

	protected static final String VS_POST_DATA_KEY = "vsPostData";
	protected static final String RESP_MSG_OK = "ok";
	protected static final String FIRST_VERSION = "firstV";
	protected final Logger logger = LoggerFactory.getLogger(getClass());
	protected VirtualServerService virtualServerService;
	protected ConfigManager configManager;
	protected StrategyService strategyService;
	protected F5ApiService f5ApiService;
	protected AgentClientResult result;
	protected String ip;
	protected boolean needOnline;
	protected long deployId;
	protected ExecutorService executor;

	public AbstractAgentClient(VirtualServerService virtualServerService, ConfigManager configManager,
			StrategyService strategyService, F5ApiService f5ApiService, String ip, boolean needOnline, long deployId,
			ExecutorService executor) {
		this.virtualServerService = virtualServerService;
		this.configManager = configManager;
		this.strategyService = strategyService;
		this.f5ApiService = f5ApiService;
		this.result = new AgentClientResult();
		this.ip = ip;
		this.needOnline = needOnline;
		this.deployId = deployId;
		this.executor = executor;
	}

	protected String genDynamicVsData(SlbModelTree deployingSlbModelTree) throws BizException {

		Map<String, String> map = new HashMap<String, String>();

		String vsName = getVsName(deployingSlbModelTree);
		map.put(vsName, getNginxConfig(deployingSlbModelTree));
		return GsonUtils.toJson(map);
	}

	private String getNginxConfig(SlbModelTree slbModelTree) throws BizException {

		return virtualServerService.generateNginxConfig(slbModelTree);
	}

	private String getVsName(SlbModelTree tree) {
		Collection<VirtualServer> vs = tree.getVirtualServers().values();
		if (vs.size() != 1) {
			throw new IllegalArgumentException("vs size not 1, but " + vs.size() + "," + vs);
		}
		return ((VirtualServer) vs.toArray()[0]).getName();
	}

	protected String genDynamicVsData(List<SlbModelTree> deployingSlbModelTrees)
			throws BizException, InterruptedException {

		final Map<String, String> map = new HashMap<String, String>();
		final CountDownLatch latch = new CountDownLatch(deployingSlbModelTrees.size());
		final AtomicReference<BizException> exception = new AtomicReference<BizException>();

		for (final SlbModelTree deployingSlbModelTree : deployingSlbModelTrees) {

			executor.execute(new Runnable() {

				@Override
				public void run() {
					try {
						map.put(getVsName(deployingSlbModelTree), getNginxConfig(deployingSlbModelTree));
					} catch (BizException e) {
						exception.set(e);
					} finally {
						latch.countDown();
					}
				}
			});
		}
		latch.await();
		if (exception.get() != null) {
			throw exception.get();
		}
		return GsonUtils.toJson(map);
	}

	protected boolean sendPostData(URL deployUrl, String method, String... values) throws IOException {

		if ((values.length & 1) != 0) {
			throw new IllegalArgumentException("Values should be couples! but " + values.length);
		}
		HttpURLConnection conn = (HttpURLConnection) deployUrl.openConnection();
		conn.setConnectTimeout(configManager.getDeployConnectTimeout());
		conn.setDoOutput(true);
		conn.setRequestMethod(method);
		PrintWriter out = new PrintWriter(conn.getOutputStream());

		String keyValue[] = new String[values.length / 2];

		for (int i = 0; i < values.length / 2; i++) {

			keyValue[i] =
					URLEncoder.encode(values[i << 1], "UTF-8") + "=" + URLEncoder.encode(values[(i << 1) + 1], "UTF-8");
		}

		out.print(StringUtils.join(keyValue, "&"));
		out.flush();
		out.close();

		return checkAgentResponse(conn);
	}

	protected boolean checkAgentResponse(HttpURLConnection conn) throws IOException {
		Response response = DefaultJsonParser
				.parse(Response.class, IOUtilsWrapper.convetStringFromRequest(conn.getInputStream()));
		if (!RESP_MSG_OK.equals(response.getStatus())) {
			result.logError(String.format("Failed to deploy (status: %s, error msg: %s)", response.getStatus(),
					response.getMessage()));
			endWithFail();
			return false;
		}

		return true;
	}

	protected void endWithFail() {
		result.setStatus(DeployAgentStatus.FAILED);
		result.logInfo("End deploying with status failed");
	}

	protected String genDynamicRefreshPostData(ComparisionResult compareResult, String vsName)
			throws BizException, UnsupportedEncodingException {
		List<Map<String, String>> postDataList = new ArrayList<Map<String, String>>();

		addCompareResult(postDataList, compareResult, vsName);
		return GsonUtils.toJson(postDataList);
	}

	private void addCompareResult(List<Map<String, String>> postDataList, ComparisionResult compareResult,
			String vsName) throws BizException, UnsupportedEncodingException {

		for (Pool pool : compareResult.getAddedPools()) {
			addPostCompareResult(null, pool, postDataList, vsName);
		}

		for (PoolPair pool : compareResult.getModifiedPools()) {
			addPostCompareResult(pool.getOldPool(), pool.getNewPool(), postDataList, vsName);
		}

		for (Pool pool : compareResult.getDeletedPools()) {
			Map<String, String> postData = new HashMap<String, String>();
			postData.put("url", configManager.getNginxDynamicDeleteUpstreamUrlPattern(
					PoolNameUtils.rewriteToPoolNamePrefix(vsName, pool.getName())));
			postData.put("method", "DELETE");
			postData.put("data", "not_used");
			postDataList.add(postData);

			if (needDegrade(pool)) {
				postData = new HashMap<String, String>();
				postData.put("url",

						configManager.getNginxDynamicDeleteUpstreamUrlPattern(
								PoolNameUtils.rewriteToPoolNameDegradePrefix(vsName, pool.getName())));
				postData.put("method", "DELETE");
				postData.put("data", "not_used");
				postDataList.add(postData);
			}
		}
	}

	private void addPostCompareResult(Pool oldPool, Pool newPool, List<Map<String, String>> postDataList, String vsName)
			throws BizException, UnsupportedEncodingException {

		Map<String, String> postData = new HashMap<String, String>();
		postData.put("url", configManager
				.getNginxDynamicAddUpstreamUrlPattern(PoolNameUtils.rewriteToPoolNamePrefix(vsName, newPool.getName())));
		postData.put("method", "POST");
		postData.put("data", generateUpstreamContent(newPool, false));
		postDataList.add(postData);

		if (needDegrade(newPool)) {

			postData = new HashMap<String, String>();
			postData.put("url", configManager.getNginxDynamicAddUpstreamUrlPattern(
					PoolNameUtils.rewriteToPoolNameDegradePrefix(vsName, newPool.getName())));
			postData.put("method", "POST");
			postData.put("data", generateUpstreamContent(newPool, true));
			postDataList.add(postData);
		}

		if (needDegrade(oldPool) && !needDegrade(newPool)) {
			// 删除老的backup
			postData = new HashMap<String, String>();
			postData.put("url", configManager.getNginxDynamicDeleteUpstreamUrlPattern(
					PoolNameUtils.rewriteToPoolNameDegradePrefix(vsName, oldPool.getName())));
			postData.put("method", "DELETE");
			postData.put("data", "not_used");
			postDataList.add(postData);
		}
	}

	private String generateUpstreamContent(Pool pool, boolean isDegrade) throws BizException {

		NginxUpstream upstream = NginxConfigVisitor
				.generateUpstream(pool, strategyService.findStrategy(pool.getLoadbalanceStrategyName()));

		return new NginxVelocityTools().upstreamContent(upstream, isDegrade);
	}

	private boolean needDegrade(Pool pool) {
		for (Member m : pool.getMembers()) {
			if (m.getState() == State.DEGRADE) {
				return true;
			}
		}
		return false;
	}

	protected String genDynamicRefreshPostData(Map<String, ComparisionResult> compareResults)
			throws BizException, UnsupportedEncodingException {
		List<Map<String, String>> postDataList = new ArrayList<Map<String, String>>();

		if (compareResults != null) {
			for (Map.Entry<String, ComparisionResult> entry : compareResults.entrySet()) {
				ComparisionResult compareResult = entry.getValue();
				String vsName = entry.getKey();
				addCompareResult(postDataList, compareResult, vsName);
			}
		}
		return GsonUtils.toJson(postDataList);
	}

	protected void endWithSuccess() {
		result.setStatus(DeployAgentStatus.SUCCESS);
		result.logInfo("End deploying with status success");
	}

	protected boolean versionExists(String vsName, String version, SlbModelTree slbModelTree) {

		if (slbModelTree == null || slbModelTree.findVirtualServer(vsName) == null) {
			result.logError(String.format("Config with version %s for vs %s not found.(" + (slbModelTree == null) + ")",
					version, vsName));
			return false;
		}
		return true;
	}

	protected boolean sameVersion(String vsName, String currentWorkingVersion, String tag) {
		if (currentWorkingVersion.equals(tag)) {
			result.logInfo(
					String.format("Config of vs(%s) for host(%s) is already version %s, no need to redeploy", vsName,
							ip, tag));
			return true;
		}
		return false;
	}

	protected void readLog() throws IOException {
		result.logInfo(String.format("Getting status from host(%s) for deploy(%s) ... ", ip, deployId));

		AgentReader sr = null;
		try {
			sr = new AgentReader(new PhoenixInputStreamReader(configManager.getDeployLogUrl(ip, deployId),
					configManager.getDeployConnectTimeout(), configManager.getDeployGetlogRetrycount()));
			while (sr.hasNext()) {
				result.addRawLogs(sr.next(result));
			}

			if (result.getStatus() == DeployAgentStatus.SUCCESS) {
				if (needOnline) {
					result.logInfo(String.format("enabling host(%s) from F5 ... ", ip));
					f5ApiService.online(ip, result);
					result.logInfo(String.format("host(%s) enabled.", ip));
					needOnline = false;
				}
				endWithSuccess();
			} else {
				endWithFail();
			}
		} finally {
			if (sr != null) {
				sr.close();
			}
		}
	}

	protected String getRealVersion(AgentClientResult result, String vsName, String version) {
		String versionRet = version;
		try {
			SlbModelTree slbModelTree = virtualServerService.findTagById(vsName, version);
			if (slbModelTree == null) {
				result.logInfo(String.format("Vs(%s) is the first deployment for host(%s), (%s)", vsName, this.ip,
						"can not find virtualServer"));
				versionRet = FIRST_VERSION;
			}
		} catch (Exception e) {
			logger.error("find tag error:" + vsName + "," + version, e);
			result.logInfo(String.format("Vs(%s) is the first deployment for host(%s), (Exception:%s)", vsName, this.ip,
					e.getMessage()));
			versionRet = FIRST_VERSION;
		}
		return versionRet;
	}

	protected boolean isHttpsInfoChanged(VirtualServer newVs, VirtualServer oldVs) {
		if (newVs.isHttpsOpen() == oldVs.isHttpsOpen()) {
			if (!newVs.isHttpsOpen()) {
				return false;
			}
		}
		return true;
	}

	protected boolean updateAgentSslFile(VirtualServer vs) throws BizException, IOException {
		Map<String, String[]> sslFileMap = new HashMap<String, String[]>();
		String[] sslFiles = new String[2];

		sslFiles[0] = vs.getSslCertificate();
		sslFiles[1] = vs.getSslCertificateKey();
		sslFileMap.put(vs.getName(), sslFiles);

		String postData = GsonUtils.toJson(sslFileMap);

		URL updateUrl = new URL(configManager.getUpdateFileUrl(ip, vs.getName(),
				ConfigManager.CHECK_CERTIFACATE_CONF + "," + ConfigManager.CHECK_KEY_CONF));
		result.logInfo(String.format("Update url is %s", updateUrl));

		return sendPostData(updateUrl, "POST", VS_POST_DATA_KEY, postData);
	}

	@Override
	public AgentClientResult getResult() {
		return result;
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

}
