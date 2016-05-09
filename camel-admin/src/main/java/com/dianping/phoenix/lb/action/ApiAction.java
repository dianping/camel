package com.dianping.phoenix.lb.action;

import com.dianping.phoenix.lb.action.search.And;
import com.dianping.phoenix.lb.api.action.Api;
import com.dianping.phoenix.lb.api.aspect.ApiAspect;
import com.dianping.phoenix.lb.api.aspect.ApiWrapper;
import com.dianping.phoenix.lb.api.dengine.ForceState;
import com.dianping.phoenix.lb.constant.Constants;
import com.dianping.phoenix.lb.deploy.bo.DeployAgentBo;
import com.dianping.phoenix.lb.deploy.bo.DeployTaskBo;
import com.dianping.phoenix.lb.deploy.bo.DeployVsBo;
import com.dianping.phoenix.lb.deploy.bo.NewTaskInfo;
import com.dianping.phoenix.lb.deploy.bo.NewTaskInfo.VsAndTag;
import com.dianping.phoenix.lb.deploy.executor.DefaultTaskExecutorContainer;
import com.dianping.phoenix.lb.deploy.executor.TaskExecutor;
import com.dianping.phoenix.lb.deploy.executor.TaskExecutorContainer;
import com.dianping.phoenix.lb.deploy.model.DeployAgent;
import com.dianping.phoenix.lb.deploy.model.DeployTaskStatus;
import com.dianping.phoenix.lb.deploy.service.DeployTaskService;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.facade.PoolAvailableRateException;
import com.dianping.phoenix.lb.model.InfluencingVs;
import com.dianping.phoenix.lb.model.api.PoolNameChangedContext;
import com.dianping.phoenix.lb.model.entity.*;
import com.dianping.phoenix.lb.monitor.ApiResult;
import com.dianping.phoenix.lb.monitor.StatusContainer;
import com.dianping.phoenix.lb.monitor.StatusContainer.ShowResult;
import com.dianping.phoenix.lb.monitor.nginx.log.NginxStatusRecorder;
import com.dianping.phoenix.lb.service.model.PoolService;
import com.dianping.phoenix.lb.service.model.StrategyService;
import com.dianping.phoenix.lb.service.model.VariableService;
import com.dianping.phoenix.lb.service.model.VirtualServerService;
import com.dianping.phoenix.lb.utils.IOUtilsWrapper;
import com.dianping.phoenix.lb.utils.JsonBinder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.struts2.ServletActionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Component("apiAction")
@Scope("prototype")
public class ApiAction extends AbstractApiAction implements Api {

	private static final String DEFAULT_PAAS_POOL = "paas-pool";

	private static final int DEFAULT_PORT = 80;

	private static final String DEFAULT_PAAS_GROUP = "paas";

	private static final long serialVersionUID = -1084994778030229218L;
	private static final String TYPE = "API_CALL";
	private static ReentrantLock changeNameLock = new ReentrantLock();
	@Autowired
	protected VirtualServerService virtualServerService;

	@Autowired
	protected StrategyService strategyService;

	@Autowired
	protected PoolService poolService;

	@Autowired
	protected VariableService variableService;

	@Autowired
	protected StatusContainer statusContainer;

	@Autowired
	protected NginxStatusRecorder nginxStatusRecorder;
	private List<PoolNameChangedContext> changedContexts = new ArrayList<PoolNameChangedContext>();
	private String postBody;
	private String nodeIp;
	private int nodePort = 80;
	@Resource(name = "apiAspect")
	private ApiAspect m_apiAspect;
	private String vsName;
	@Autowired
	private DeployTaskService deployTaskService;
	@Resource(type = DefaultTaskExecutorContainer.class)
	private TaskExecutorContainer<DeployTaskBo> taskContainer;
	private List<ShowResult> upstreamStatus;
	private String forceState;
	private Set<String> servers;
	private List<String> poolNames;

	private static List<Directive> findDirectionsRelative(List<Directive> directives, String upstreamName,
			String newUpstreamName) {
		List<Directive> list = new ArrayList<Directive>();
		for (Directive directive : directives) {
			String type = directive.getType();
			if ("proxy_pass".equalsIgnoreCase(type) && upstreamName
					.equals(directive.getDynamicAttribute("pool-name"))) {
				list.add(directive);
				directive.setDynamicAttribute("pool-name", newUpstreamName);
			} else if ("ifelse".equalsIgnoreCase(type) && (
					StringUtils.contains(directive.getDynamicAttribute("if-statement"), upstreamName) || StringUtils
							.contains(directive.getDynamicAttribute("else-statement"), upstreamName))) {
				list.add(directive);
			} else if ("custom".equalsIgnoreCase(type) && StringUtils
					.contains(directive.getDynamicAttribute("value"), upstreamName)) {
				list.add(directive);
			}
		}
		return list;
	}

	private static List<Directive> findDirectionsRelative(List<Directive> directives, String upstreamName) {
		List<Directive> list = new ArrayList<Directive>();
		for (Directive directive : directives) {
			String type = directive.getType();
			if (Constants.DIRECTIVE_PROXY_PASS.equalsIgnoreCase(type) && upstreamName
					.equals(directive.getDynamicAttribute(Constants.DIRECTIVE_PROXY_PASS_POOL_NAME))) {
				list.add(directive);
			} else if (Constants.DIRECTIVE_PROXY_IFELSE.equalsIgnoreCase(type) && (
					StringUtils.contains(directive.getDynamicAttribute("if-statement"), upstreamName) || StringUtils
							.contains(directive.getDynamicAttribute("else-statement"), upstreamName))) {
				list.add(directive);
			} else if ("custom".equalsIgnoreCase(type) && StringUtils
					.contains(directive.getDynamicAttribute("value"), upstreamName)) {
				list.add(directive);
			}
		}
		return list;
	}

	@Override
	public String updateMember() throws Exception {
		m_apiAspect.doTransactionWithResultMap(TYPE, "updateMember", new ApiWrapper() {
			@Override
			public void doAction() throws Exception {
				updateMemberAction();
			}
		}, dataMap);
		return SUCCESS;
	}

	@Override
	public String addMember() throws Exception {
		m_apiAspect.doTransactionWithResultMap(TYPE, "addMember", new ApiWrapper() {
			@Override
			public void doAction() throws Exception {
				addMemberAction();
			}
		}, dataMap);
		return SUCCESS;
	}

	@Override
	public String delMember() throws Exception {
		m_apiAspect.doTransactionWithResultMap(TYPE, "delMember", new ApiWrapper() {
			@Override
			public void doAction() throws Exception {
				delMemberAction();
			}
		}, dataMap);
		return SUCCESS;
	}

	@Override
	public String deploy() throws Exception {
		m_apiAspect.doTransactionWithResultMap(TYPE, "deploy", new ApiWrapper() {
			@Override
			public void doAction() throws Exception {
				deployAction();
			}
		}, dataMap);
		return SUCCESS;
	}

	@Override
	public String delVs() {

		m_apiAspect.doTransactionWithResultMap(TYPE, "addVs", new ApiWrapper() {

			@Override
			public void doAction() throws Exception {

				Validate.isTrue(StringUtils.equalsIgnoreCase(ServletActionContext.getRequest().getMethod(), "POST"),
						"Only allow POST method.");

				if (logger.isInfoEnabled()) {
					logger.info("[delVs]" + vsName);
				}
				virtualServerService.deleteVirtualServer(vsName);

			}
		}, dataMap);
		return SUCCESS;
	}

	@Override
	public String deployVs() {

		m_apiAspect.doTransactionWithResultMap(TYPE, "deployVs", new ApiWrapper() {

			@Override
			public void doAction() throws Exception {
				VirtualServer vs = virtualServerService.findVirtualServer(vsName);
				if (vs == null) {
					throw new IllegalArgumentException("vs " + vsName + " not found!");
				}

				commonAspects = commonAspectService.listCommonAspects();
				// tag
				String tag = virtualServerService.tag(vsName, vs.getVersion());

				NewTaskInfo task = new NewTaskInfo();
				task.setTaskName("deploy vs vpi:" + vsName);
				List<VsAndTag> vsAndTags = new ArrayList<NewTaskInfo.VsAndTag>();
				vsAndTags.add(new VsAndTag(vsName, tag));
				task.setSelectedVsAndTags(vsAndTags);

				long taskId = deployTaskService.addTask(task);

				// 添加需要部署的agent
				DeployTaskBo deployTaskBo = deployTaskService.getTask(taskId);
				Map<String, DeployVsBo> deployVsBos = deployTaskBo.getDeployVsBos();
				for (DeployVsBo deployVsBo : deployVsBos.values()) {
					Map<String, DeployAgentBo> dabs = deployVsBo.getDeployAgentBos();
					for (Instance instance : deployVsBo.getSlbPool().getInstances()) {
						DeployAgentBo deployAgentBo = new DeployAgentBo();
						DeployAgent agent = new DeployAgent();
						agent.setIpAddress(instance.getIp());
						deployAgentBo.setDeployAgent(agent);
						dabs.put(instance.getIp(), deployAgentBo);
					}
				}
				deployTaskService.updateTask(deployTaskBo);

				TaskExecutor<DeployTaskBo> taskExecutor = taskContainer.submitTask(taskId);
				taskExecutor.start();
				// wait until finish
				taskExecutor.join();

				// 部署失败
				if (taskExecutor.getDeployTaskBo().getTask().getStatus() != DeployTaskStatus.SUCCESS) {
					throw new IllegalStateException(
							"deploy vs to agent fail " + taskExecutor.getDeployTaskBo().getTask().getStatus());
				}
				dataMap.put("taskId", taskId);
				dataMap.put("taskResult", taskExecutor.getDeployTaskBo().getTask().getStatus());
			}
		}, dataMap);
		return SUCCESS;
	}

	@Override
	public String addVs() throws Exception {

		m_apiAspect.doTransactionWithResultMap(TYPE, "addVs", new ApiWrapper() {

			@Override
			public void doAction() throws Exception {

				Validate.isTrue(StringUtils.equalsIgnoreCase(ServletActionContext.getRequest().getMethod(), "POST"),
						"Only allow POST method.");

				String vsJson = IOUtilsWrapper
						.convetStringFromRequest(ServletActionContext.getRequest().getInputStream());
				if (StringUtils.isBlank(vsJson)) {
					throw new IllegalArgumentException("Post body cannot be empty.");
				}
				VirtualServer virtualServer = JsonBinder.getNonNullBinder().fromJson(vsJson, VirtualServer.class);

				Validate.notEmpty(virtualServer.getName(), "Vs name cannot be empty.");

				if (logger.isInfoEnabled()) {
					logger.info("[addVs]" + virtualServer.getName());
				}

				if (virtualServer.getDomain() == null) {
					virtualServer.setDomain(virtualServer.getName());
				}
				if (virtualServer.getGroup() == null) {
					virtualServer.setGroup(DEFAULT_PAAS_GROUP);
				}
				if (virtualServer.getSlbPool() == null) {
					virtualServer.setSlbPool(DEFAULT_PAAS_POOL);
				}
				if (virtualServer.getDefaultPoolName() == null) {
					virtualServer.setDefaultPoolName(virtualServer.getName());
				}
				if (virtualServer.getPort() == null) {
					virtualServer.setPort(DEFAULT_PORT);
				}

				// 验证vs,pool不存在
				VirtualServer virtualServer0 = virtualServerService.findVirtualServer(virtualServer.getName());
				Validate.isTrue(virtualServer0 == null, "Vs already exists: " + virtualServer.getName());
				Pool pool0 = poolService.findPool(virtualServer.getDefaultPoolName());

				if (pool0 == null) {
					// 先加pool
					Pool pool = new Pool();
					pool.setName(virtualServer.getName());
					pool.setMinAvailableMemberPercentage(50);
					poolService.addPool(pool.getName(), pool);
				}

				if (logger.isInfoEnabled()) {
					logger.info("[addVs]" + virtualServer);
				}
				// 再加vs
				virtualServerService.addVirtualServer(virtualServer.getName(), virtualServer);
			}
		}, dataMap);

		return SUCCESS;
	}

	@Override
	public String addPool() throws Exception {

		m_apiAspect.doTransactionWithResultMap(TYPE, "addPool", new ApiWrapper() {

			@Override
			public void doAction() throws Exception {
				Validate.isTrue(StringUtils.equalsIgnoreCase(ServletActionContext.getRequest().getMethod(), "POST"),
						"Only allow POST method.");

				String poolJson = IOUtilsWrapper
						.convetStringFromRequest(ServletActionContext.getRequest().getInputStream());
				if (StringUtils.isBlank(poolJson)) {
					throw new IllegalArgumentException("Post body cannot be empty.");
				}
				Pool pool = JsonBinder.getNonNullBinder().fromJson(poolJson, Pool.class);

				if (logger.isInfoEnabled()) {
					logger.info("[addPool]" + pool);
				}

				Validate.notEmpty(pool.getName(), "Pool name cannot be empty.");

				poolFacade.addPool(pool);
			}
		}, dataMap);
		return SUCCESS;
	}

	@Override
	public String delPool() throws Exception {

		m_apiAspect.doTransactionWithResultMap(TYPE, "delPool", new ApiWrapper() {

			@Override
			public void doAction() throws Exception {

				if (logger.isInfoEnabled()) {
					logger.info("[delPool]" + poolName);
				}
				Validate.isTrue(StringUtils.equalsIgnoreCase(ServletActionContext.getRequest().getMethod(), "POST"),
						"Only allow POST method.");

				Validate.notEmpty(poolName, "Pool name cannot be empty.");

				// 验证没有vs依赖pool
				List<String> influencingVsList = virtualServerService.findVirtualServerByPool(poolName);
				Validate.isTrue((influencingVsList == null || influencingVsList.size() <= 0),
						"Can't delete this pool, because these VirtualServers use it:" + influencingVsList);

				poolService.deletePool(poolName);
			}
		}, dataMap);
		return SUCCESS;
	}

	@Override
	public String getPool() throws Exception {

		m_apiAspect.doTransactionWithResultMap(TYPE, "getPool", new ApiWrapper() {

			@Override
			public void doAction() throws Exception {
				if (logger.isInfoEnabled()) {
					logger.info("[delPool]" + poolName);
				}
				Validate.isTrue(StringUtils.equalsIgnoreCase(ServletActionContext.getRequest().getMethod(), "POST"),
						"Only allow POST method.");

				Validate.notEmpty(poolName, "Pool name cannot be empty.");

				Pool pool = poolService.findPool(poolName);

				dataMap.put("pool", pool);
			}
		}, dataMap);
		return SUCCESS;
	}

	/**
	 * 修改pool名字，非api，界面调用
	 *
	 * @return
	 * @throws Exception
	 */
	public String changePoolName() throws Exception {

		if (StringUtils.equalsIgnoreCase(ServletActionContext.getRequest().getMethod(), "GET")) {
			return SUCCESS;
		}

		// 多行，每行的oldName,newName
		if (StringUtils.isBlank(postBody)) {
			throw new IllegalArgumentException("Post body cannot be empty.");
		}

		changeNameLock.lock();
		try {
			String[] lines = StringUtils.split(postBody, "\n\r");
			if (lines != null) {
				for (String line : lines) {
					String[] names = StringUtils.split(line, ',');
					if (names.length == 2) {
						String oldName = names[0].trim();
						String newName = names[1].trim();
						PoolNameChangedContext changedContext = new PoolNameChangedContext(oldName, newName);
						try {
							_changePoolName(changedContext);
						} catch (Exception e) {
							changedContext.markError();
							changedContext.outputln(e.getClass() + ": " + e.getMessage());
							StackTraceElement[] stackTrace = e.getStackTrace();
							for (StackTraceElement ele : stackTrace) {
								changedContext.outputln(ele.toString());
							}
						}
						this.changedContexts.add(changedContext);
					}
				}
			}
		} finally {
			changeNameLock.unlock();
		}
		return SUCCESS;
	}

	private void _changePoolName(PoolNameChangedContext changedContext) throws BizException {
		String oldPoolName = changedContext.getOldName();
		String newPoolName = changedContext.getNewName();

		List<Aspect> commonAspects = commonAspectService.listCommonAspects();
		List<Variable> variables = variableService.listVariables();
		List<Strategy> strategies = strategyService.listStrategies();
		// 创建新pool(内容完全复制oldPool)
		Pool oldPool = poolService.findPool(oldPoolName);
		if (oldPool == null) {
			throw new IllegalArgumentException("Pool'" + oldPoolName + "' not found.");
		}
		oldPool.setName(newPoolName);
		try {
			poolService.addPool(newPoolName, oldPool);
		} catch (PoolAvailableRateException e) {
			logger.error("[_changePoolName]", e);
			throw new BizException(e);
		}
		changedContext.outputln("增加了新的pool：" + newPoolName);

		List<Pool> pools = poolService.listPools();

		// 找到所有影响到的vs, 找到涉及oldPoolName的地方（defaultPool或directive），把oldPoolName改为newPoolName
		List<String> virtualServers = virtualServerService.findVirtualServerByPool(oldPoolName);
		if (virtualServers != null && virtualServers.size() > 0) {
			changedContext.outputln("影响到" + virtualServers.size() + "个vs.");
			for (int i = 0; i < virtualServers.size(); i++) {
				String vsName = virtualServers.get(i);
				changedContext.outputln();
				changedContext.outputln("正在处理第" + i + "个站点 ：" + vsName);

				VirtualServer virtualServer = virtualServerService.findVirtualServer(vsName);

				String defaultPoolName = virtualServer.getDefaultPoolName();
				if (defaultPoolName.equals(oldPoolName)) {
					changedContext.outputln("涉及默认pool的修改：" + oldPoolName + " 改为 " + newPoolName);
					virtualServer.setDefaultPoolName(newPoolName);
				}

				List<Location> locations = virtualServer.getLocations();
				for (Location location : locations) {
					List<Directive> directives = findDirectionsRelative(location.getDirectives(), oldPoolName,
							newPoolName);
					for (Directive directive : directives) {
						changedContext
								.outputln("涉及规则的修改：" + directive.getType() + " " + directive.getDynamicAttributes());
					}
				}
				// 保存
				virtualServerService.modifyVirtualServer(vsName, virtualServer);

				// check 一下 nginx config
				String nginxConfig = virtualServerService
						.generateNginxConfig(virtualServer, pools, commonAspects, variables, strategies);

				if (StringUtils.contains(nginxConfig, oldPoolName)) {
					changedContext.markWarn();
					changedContext
							.outputln("【警告】站点" + vsName + " 的nginx配置文件，仍然含有旧pool名称'" + oldPoolName + "'，请人工检查并处理。");
					changedContext.outputln(nginxConfig);
				}
			}
		} else {
			changedContext.outputln("没有影响vs，就此结束。");
		}
	}

	@Override
	public String status() throws Exception {

		m_apiAspect.doTransactionWithResultMap(TYPE, "status", new ApiWrapper() {

			@Override
			public void doAction() throws Exception {
				if (logger.isInfoEnabled()) {
					logger.info("[status]" + nodeIp);
				}
				Validate.notEmpty(nodeIp, "'nodeIp' cannot be empty.");

				List<ApiResult> apiResult = statusContainer.getStatus(nodeIp, nodePort);

				dataMap.put("result", apiResult);
			}
		}, dataMap);
		return SUCCESS;
	}

	@Override
	public String upstreamStatus() {

		m_apiAspect.transaction(TYPE, "upstreamStatus", new ApiWrapper() {

			@Override
			public void doAction() throws Exception {
				setUpstreamStatus(statusContainer.getStatusForShow(new And()));
			}
		});
		return SUCCESS;
	}

	@Override
	public void validate() {
		super.validate();
	}

	public String getPoolName() {
		return poolName;
	}

	public void setPoolName(String poolName) {
		this.poolName = poolName;
	}

	public List<PoolNameChangedContext> getChangedContexts() {
		return changedContexts;
	}

	public void setChangedContexts(List<PoolNameChangedContext> changedContexts) {
		this.changedContexts = changedContexts;
	}

	public String getPostBody() {
		return postBody;
	}

	public void setPostBody(String postBody) {
		this.postBody = postBody;
	}

	public String getNodeIp() {
		return nodeIp;
	}

	public void setNodeIp(String nodeIp) {
		this.nodeIp = nodeIp;
	}

	public int getNodePort() {
		return nodePort;
	}

	public void setNodePort(int nodePort) {
		this.nodePort = nodePort;
	}

	public List<ShowResult> getUpstreamStatus() {
		return upstreamStatus;
	}

	public void setUpstreamStatus(List<ShowResult> upstreamStatus) {
		this.upstreamStatus = upstreamStatus;
	}

	public String getVsName() {
		return vsName;
	}

	public void setVsName(String vsName) {
		this.vsName = vsName;
	}

	@Override
	public String forceUpstreamState() {

		m_apiAspect.doTransactionWithResultMap(TYPE, "forceUpstreamState", new ApiWrapper() {

			@Override
			public void doAction() throws Exception {

				Validate.isTrue(StringUtils.equalsIgnoreCase(ServletActionContext.getRequest().getMethod(), "POST"),
						"Only allow POST method.");
				if (logger.isInfoEnabled()) {
					logger.info("[forceUpstreamState]" + forceState);
				}
				poolFacade.forceState(poolName, ForceState.get(forceState).getIntForceState());
				deployPool(poolName);
			}
		}, dataMap);
		return SUCCESS;
	}

	public String getForceState() {
		return forceState;
	}

	public void setForceState(String forceState) {
		this.forceState = forceState;
	}

	@Override
	public String listServers() {

		m_apiAspect.transaction(TYPE, "getServers", new ApiWrapper() {

			@Override
			public void doAction() throws Exception {

				servers = new HashSet<String>();
				for (Pool pool : poolService.listPools()) {
					for (Member member : pool.getMembers()) {
						servers.add(member.getIp() + ":" + member.getPort());
					}
				}
			}
		});
		return SUCCESS;
	}

	@Override
	public String listPoolNames() {

		m_apiAspect.transaction(TYPE, "listPoolNames", new ApiWrapper() {

			@Override
			public void doAction() throws Exception {
				poolNames = new LinkedList<String>();

				for (Pool pool : poolService.listPools()) {
					poolNames.add(pool.getName());
				}
			}
		});
		return SUCCESS;
	}

	public Set<String> getServers() {
		return servers;
	}

	public void setServers(Set<String> servers) {
		this.servers = servers;
	}

	public List<String> getPoolNames() {
		return poolNames;
	}

	public void setPoolNames(List<String> poolNames) {
		this.poolNames = poolNames;
	}

	@Override
	public String receiveNginxLogs() throws Exception {

		m_apiAspect.doTransactionWithResultMap(TYPE, "receiveNginxLogs", new ApiWrapper() {

			@Override
			public void doAction() throws Exception {
				Validate.isTrue(StringUtils.equalsIgnoreCase(ServletActionContext.getRequest().getMethod(), "POST"),
						"Only allow POST method.");

				String logs = IOUtilsWrapper
						.convetStringFromRequest(ServletActionContext.getRequest().getInputStream());

				if (StringUtils.isBlank(logs)) {
					throw new IllegalArgumentException("logs cannot be empty.");
				}
				nginxStatusRecorder.addNginxLogs(logs);
			}
		}, dataMap);

		return SUCCESS;
	}

	@Override
	public String listVsAndPath() {
		m_apiAspect.doTransactionWithResultMap(TYPE, "listVsAndPath", new ApiWrapper() {
			@Override
			public void doAction() throws Exception {
				if (StringUtils.isBlank(poolName)) {
					throw new IllegalArgumentException("pool name cannot be empty.");
				}

				List<InfluencingVs> re = new ArrayList<InfluencingVs>();
				List<String> virtualServers = virtualServerService.findVirtualServerByPool(poolName);

				if (virtualServers != null) {
					for (String vsName : virtualServers) {
						Set<String> positionDescs = new HashSet<String>();
						VirtualServer virtualServer = virtualServerService.findVirtualServer(vsName);

						String defaultPoolName = virtualServer.getDefaultPoolName();
						if (defaultPoolName.equals(poolName)) {
							positionDescs.add("默认集群");

						}

						List<Location> locations = virtualServer.getLocations();
						for (Location location : locations) {
							List<Directive> directives = findDirectionsRelative(location.getDirectives(), poolName);
							if (directives != null && directives.size() > 0) {
								positionDescs.add(location.getPattern());
							}
						}

						InfluencingVs influencingVs = new InfluencingVs();

						influencingVs.setVsName(vsName);
						influencingVs.setPositionDescs(new ArrayList<String>(positionDescs));
						re.add(influencingVs);
					}
				}
				dataMap.put("influencingVsList", re);
			}
		}, dataMap);

		return SUCCESS;
	}

	@Override
	public String listPoolMemberDict() throws Exception {
		m_apiAspect.doTransactionWithResultMap(TYPE, "listPoolMemberDict", new ApiWrapper() {
			@Override
			public void doAction() throws Exception {
				List<Pool> pools = poolService.listPools();
				Map<String, List<String>> poolMemberDict = new HashMap<String, List<String>>();

				for (Pool pool : pools) {
					String poolName = pool.getName();
					List<String> members = new ArrayList<String>();

					for (Member member : pool.getMembers()) {
						members.add(member.getIp());
					}
					poolMemberDict.put(poolName, members);
				}
				dataMap.put("poolMemberDict", poolMemberDict);
			}
		}, dataMap);

		return SUCCESS;
	}

}
