package com.dianping.phoenix.lb.deploy.executor;

import com.dianping.phoenix.lb.PlexusComponentContainer;
import com.dianping.phoenix.lb.configure.ConfigManager;
import com.dianping.phoenix.lb.deploy.bo.api.DeployTaskApiBo;
import com.dianping.phoenix.lb.deploy.f5.F5ApiService;
import com.dianping.phoenix.lb.deploy.service.AgentSequenceService;
import com.dianping.phoenix.lb.deploy.service.DeployTaskApiService;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.service.model.StrategyService;
import com.dianping.phoenix.lb.service.model.VirtualServerService;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

@Service
@SuppressWarnings("rawtypes")
public class ApiTaskExecutorContainer implements TaskExecutorContainer<DeployTaskApiBo>, TaskExecutorListener {

	private ConcurrentHashMap<Long, TaskExecutor<DeployTaskApiBo>> container = new ConcurrentHashMap<Long, TaskExecutor<DeployTaskApiBo>>();

	@Autowired
	private DeployTaskApiService deployTaskService;

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
	}

	@Override
	public TaskExecutor<DeployTaskApiBo> getTaskExecutor(long taskId) {
		return container.get(taskId);
	}

	@Override
	public TaskExecutor<DeployTaskApiBo> submitTask(long taskId) throws BizException {
		DeployTaskApiBo deployTaskBo = deployTaskService.getTask(taskId);
		if (deployTaskBo == null) {
			throw new IllegalArgumentException("Task is not exist associate with id(" + taskId + ")");
		}
		return this.submitTask(deployTaskBo);
	}

	@Override
	public TaskExecutor<DeployTaskApiBo> submitTask(DeployTaskApiBo deployTaskBo) {
		TaskExecutor<DeployTaskApiBo> taskExecutor = new PaasTaskExecutor(this, deployTaskBo, deployTaskService,
				virtualServerService, strategyService, agentSequenceService, configManager, f5ApiService);

		TaskExecutor<DeployTaskApiBo> taskExecutor0 = this.container
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
		this.container.remove(taskId);
	}

	@Override
	public void onException(long taskId, Throwable th) {
	}

}
