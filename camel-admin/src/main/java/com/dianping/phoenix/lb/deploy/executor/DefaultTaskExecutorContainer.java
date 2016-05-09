package com.dianping.phoenix.lb.deploy.executor;

import com.dianping.phoenix.lb.PlexusComponentContainer;
import com.dianping.phoenix.lb.configure.ConfigManager;
import com.dianping.phoenix.lb.deploy.bo.DeployTaskBo;
import com.dianping.phoenix.lb.deploy.f5.F5ApiService;
import com.dianping.phoenix.lb.deploy.model.StateAction;
import com.dianping.phoenix.lb.deploy.service.AgentSequenceService;
import com.dianping.phoenix.lb.deploy.service.DeployTaskService;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.service.model.StrategyService;
import com.dianping.phoenix.lb.service.model.VirtualServerService;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

@Service
@SuppressWarnings("rawtypes")
public class DefaultTaskExecutorContainer implements TaskExecutorContainer<DeployTaskBo>, TaskExecutorListener {

	private static final Logger logger = LoggerFactory.getLogger(DefaultTaskExecutorContainer.class);

	private ConcurrentHashMap<Long, TaskExecutor<DeployTaskBo>> container = new ConcurrentHashMap<Long, TaskExecutor<DeployTaskBo>>();

	@Autowired
	private DeployTaskService deployTaskService;

	@Autowired
	private VirtualServerService virtualServerService;

	@Autowired
	private StrategyService strategyService;

	@Autowired
	private AgentSequenceService agentSequenceService;

	@Autowired
	private F5ApiService f5ApiService;

	@Resource(name = "deployThreadPool")
	private ExecutorService executor;

	private ConfigManager configManager;

	@PostConstruct
	public void init() throws ComponentLookupException, BizException {
		configManager = PlexusComponentContainer.INSTANCE.lookup(ConfigManager.class);
		//获取数据库中，状态为READY的task，将其启动起来
		try {
			List<DeployTaskBo> list = deployTaskService.getTasksByStateAction(StateAction.START);
			if (list != null && list.size() > 0) {
				for (DeployTaskBo deployTaskBo : list) {
					TaskExecutor taskExecutor = this.submitTask(deployTaskBo);
					taskExecutor.start();
				}
			}
			list = deployTaskService.getTasksByStateAction(StateAction.PAUSE);
			if (list != null && list.size() > 0) {
				for (DeployTaskBo deployTaskBo : list) {
					this.submitTask(deployTaskBo);
				}
			}
		} catch (RuntimeException e) {
			logger.warn("Error when init Task, just ignore it. Somebody should start it manually.", e);
		}
	}

	@Override
	public TaskExecutor<DeployTaskBo> getTaskExecutor(long taskId) {
		return container.get(taskId);
	}

	@Override
	public TaskExecutor<DeployTaskBo> submitTask(long taskId) throws BizException {
		DeployTaskBo deployTaskBo = deployTaskService.getTask(taskId);
		if (deployTaskBo == null) {
			throw new IllegalArgumentException("Task is not exist associate with id(" + taskId + ")");
		}
		return this.submitTask(deployTaskBo);
	}

	@Override
	public TaskExecutor<DeployTaskBo> submitTask(DeployTaskBo deployTaskBo) {

		TaskExecutor<DeployTaskBo> taskExecutor = new DefaultTaskExecutor(this, deployTaskBo, deployTaskService,
				virtualServerService, strategyService, agentSequenceService, configManager, f5ApiService);

		TaskExecutor<DeployTaskBo> taskExecutor0 = this.container
				.putIfAbsent(deployTaskBo.getTask().getId(), taskExecutor);
		if (taskExecutor0 != null) {
			return taskExecutor0;
		} else {
			return taskExecutor;
		}

	}

	@Override
	public void delTaskExecutor(long taskId) {
		TaskExecutor taskExecutor = container.remove(taskId);
		if (taskExecutor != null) {
			taskExecutor.stop();
		}
	}

	@Override
	public void onBegin(long taskId) {

	}

	@Override
	public void onFinish(long taskId) {
		container.remove(taskId);
	}

	@Override
	public void onException(long taskId, Throwable th) {

	}

}
