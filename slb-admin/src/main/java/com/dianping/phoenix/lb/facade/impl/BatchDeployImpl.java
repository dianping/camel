package com.dianping.phoenix.lb.facade.impl;

import com.dianping.phoenix.lb.deploy.bo.NewTaskInfo.VsAndTag;
import com.dianping.phoenix.lb.deploy.bo.api.DeployAgentApiBo;
import com.dianping.phoenix.lb.deploy.bo.api.DeployTaskApiBo;
import com.dianping.phoenix.lb.deploy.executor.ApiTaskExecutorContainer;
import com.dianping.phoenix.lb.deploy.executor.TaskExecutor;
import com.dianping.phoenix.lb.deploy.executor.TaskExecutorContainer;
import com.dianping.phoenix.lb.deploy.model.DeployAgentStatus;
import com.dianping.phoenix.lb.deploy.model.DeployTaskStatus;
import com.dianping.phoenix.lb.deploy.model.StateAction;
import com.dianping.phoenix.lb.deploy.model.api.DeployAgentApi;
import com.dianping.phoenix.lb.deploy.model.api.DeployTaskApi;
import com.dianping.phoenix.lb.deploy.service.DeployTaskApiService;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.facade.BatchDeploy;
import com.dianping.phoenix.lb.model.entity.Instance;
import com.dianping.phoenix.lb.model.entity.SlbPool;
import com.dianping.phoenix.lb.model.entity.VirtualServer;
import com.dianping.phoenix.lb.service.model.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年9月30日 下午2:11:53
 */
@Service
public class BatchDeployImpl implements BatchDeploy {

	private static final int TASK_NAME_MAX_LENGTH = 32;

	private static final String PATTERN = " yyyyMMdd HH:mm";
	private final Logger logger = LoggerFactory.getLogger(getClass());
	@Autowired
	protected VirtualServerService virtualServerService;
	@Autowired
	protected CommonAspectService commonAspectService;
	@Autowired
	private PoolService poolService;
	@Autowired
	private SlbPoolService slbPoolService;
	@Autowired
	private VariableService variableService;
	@Autowired
	private DeployTaskApiService deployTaskService;
	@Resource(type = ApiTaskExecutorContainer.class)
	private TaskExecutorContainer<DeployTaskApiBo> taskContainer;
	private ExecutorService executor = Executors.newCachedThreadPool();

	@Override
	public TaskExecutor<DeployTaskApiBo> deployVs(String deployDesc, List<String> influencingVsList, String deployedBy)
			throws BizException {

		if (influencingVsList == null || influencingVsList.size() == 0) {
			throw new IllegalArgumentException("influencing vs size == 0");
		}
		final List<VsAndTag> selectedVsAndTags = new ArrayList<VsAndTag>();

		final List<BizException> exceptions = Collections.synchronizedList(new ArrayList<BizException>());

		final CountDownLatch latch = new CountDownLatch(influencingVsList.size());

		long start = System.currentTimeMillis();
		for (final String vsName : influencingVsList) {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						VirtualServer virtualServer = virtualServerService.findVirtualServer(vsName);
						Validate.notNull(virtualServer, "vs(" + vsName + ") not found.");
						String tag = virtualServerService.tag(vsName, virtualServer.getVersion());

						VsAndTag vsAndTag = new VsAndTag();
						vsAndTag.setVsName(vsName);
						vsAndTag.setTag(tag);
						selectedVsAndTags.add(vsAndTag);

					} catch (BizException e) {
						exceptions.add(e);
						logger.error("[deployVs]", e);
					} finally {
						latch.countDown();
					}
				}
			});
		}

		try {
			latch.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		long tagEndTime = System.currentTimeMillis();
		if (logger.isInfoEnabled()) {
			logger.info("[deployVs][Tag spend time]" + (tagEndTime - start) + "," + deployDesc);
		}

		// 打tag部分是否有error
		if (exceptions.size() > 0) {
			throw exceptions.get(0);
		}

		// 创建一个DeployTaskApiBo
		DeployTaskApiBo deployTaskApiBo = new DeployTaskApiBo();
		DeployTaskApi task = new DeployTaskApi();
		task.setDeployedBy(deployedBy);
		task.setName(StringUtils.abbreviate(deployDesc + DateFormatUtils.format(System.currentTimeMillis(), PATTERN),
				TASK_NAME_MAX_LENGTH));
		task.setStateAction(StateAction.START);
		task.setStatus(DeployTaskStatus.CREATED);
		deployTaskApiBo.setTask(task);

		List<DeployAgentApiBo> deployAgentBos = getDeployAgentBos(selectedVsAndTags);

		deployTaskApiBo.setDeployAgentBos(deployAgentBos);

		// 更新task
		deployTaskService.addTask(deployTaskApiBo);

		if (logger.isInfoEnabled()) {
			logger.info("[deployVs][add task]" + deployTaskApiBo);
		}

		// 提交任务
		TaskExecutor<DeployTaskApiBo> taskExecutor = taskContainer.submitTask(deployTaskApiBo);

		taskExecutor.start();

		return taskExecutor;
	}

	private List<DeployAgentApiBo> getDeployAgentBos(List<VsAndTag> vsAndTags) throws BizException {
		// 根据vs获取其agent ip列表，每个agent ip创建一个DeployAgentApiBo
		Map<String, DeployAgentApiBo> existAgents = new HashMap<String, DeployAgentApiBo>();
		for (VsAndTag vsAndTag : vsAndTags) {
			String vsName = vsAndTag.getVsName();
			String tag = vsAndTag.getTag();

			VirtualServer vs = virtualServerService.findVirtualServer(vsName);
			SlbPool slbPool = slbPoolService.findSlbPool(vs.getSlbPool());

			List<Instance> instances = slbPool.getInstances();
			if (instances != null) {
				for (Instance instance : instances) {
					String ip = instance.getIp();
					DeployAgentApiBo agentApiBo = existAgents.get(ip);
					if (agentApiBo == null) {
						agentApiBo = new DeployAgentApiBo();
						DeployAgentApi deployAgent = new DeployAgentApi();
						deployAgent.setIpAddress(ip);
						deployAgent.setStatus(DeployAgentStatus.READY);
						deployAgent.setVsNames(vsName);
						deployAgent.setVsTags(tag);
						agentApiBo.setDeployAgent(deployAgent);
						existAgents.put(ip, agentApiBo);
					} else {
						DeployAgentApi deployAgent = agentApiBo.getDeployAgent();
						deployAgent.setVsNames(deployAgent.getVsNames() + "," + vsName);
						deployAgent.setVsTags(deployAgent.getVsTags() + "," + tag);
					}
				}
			}
		}

		return new ArrayList<DeployAgentApiBo>(existAgents.values());
	}

}
