package com.dianping.phoenix.lb.deploy.executor;

import com.dianping.phoenix.lb.configure.ConfigManager;
import com.dianping.phoenix.lb.deploy.agent.AgentClient;
import com.dianping.phoenix.lb.deploy.agent.AgentClientResult;
import com.dianping.phoenix.lb.deploy.agent.DefaultAgentClient;
import com.dianping.phoenix.lb.deploy.bo.DeployAgentBo;
import com.dianping.phoenix.lb.deploy.bo.DeployTaskBo;
import com.dianping.phoenix.lb.deploy.bo.DeployVsBo;
import com.dianping.phoenix.lb.deploy.f5.F5ApiService;
import com.dianping.phoenix.lb.deploy.model.*;
import com.dianping.phoenix.lb.deploy.service.AgentSequenceService;
import com.dianping.phoenix.lb.deploy.service.DeployTaskService;
import com.dianping.phoenix.lb.service.model.StrategyService;
import com.dianping.phoenix.lb.service.model.VirtualServerService;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class DefaultTaskExecutor extends AbstractTaskExecutor<DeployTaskBo> implements TaskExecutor<DeployTaskBo> {

	private final DeployTaskBo deployTaskBo;

	private final DeployTaskService deployTaskService;

	private final VirtualServerService virtualServerService;

	private final StrategyService strategyService;

	private final AgentSequenceService agentSequenceService;

	private final F5ApiService f5ApiService;

	private final ConfigManager configManager;

	private final boolean autoContinue;

	private final Integer deployInterval;

	private final AgentBatch agentBatch;

	private final ErrorPolicy errorPolicy;

	private final ReentrantLock actionLock = new ReentrantLock();

	private TaskExecutorListener taskExecutorListener;

	private ExecutorService executor;

	public DefaultTaskExecutor(TaskExecutorListener taskExecutorListener, DeployTaskBo deployTaskBo,
			DeployTaskService deployTaskService, VirtualServerService virtualServerService,
			StrategyService strategyService, AgentSequenceService agentSequenceService, ConfigManager configManager,
			F5ApiService f5ApiService) {

		this.taskExecutorListener = taskExecutorListener;
		this.deployTaskBo = deployTaskBo;

		this.deployTaskService = deployTaskService;
		this.virtualServerService = virtualServerService;
		this.strategyService = strategyService;
		this.agentSequenceService = agentSequenceService;
		this.f5ApiService = f5ApiService;
		this.configManager = configManager;

		this.autoContinue = deployTaskBo.getTask().getAutoContinue();
		this.deployInterval = deployTaskBo.getTask().getDeployInterval();
		this.agentBatch = deployTaskBo.getTask().getAgentBatch();
		this.errorPolicy = deployTaskBo.getTask().getErrorPolicy();
		this.executor = Executors.newCachedThreadPool();

	}

	private DeployVsBo nextDeployVs(DeployTaskBo deployTaskBo) {
		Map<String, DeployVsBo> deployVsBos = deployTaskBo.getDeployVsBos();
		DeployVsBo vsToBeDeploy = null;
		Set<String> vsNameSet = deployVsBos.keySet();
		for (String vsName : vsNameSet) {
			DeployVsBo deployVsBo = deployVsBos.get(vsName);
			DeployVs deployVs = deployVsBo.getDeployVs();
			// 顺序遍历vs,获取到第一个未完成的vs。
			if (!deployVs.getStatus().isCompleted()) {
				vsToBeDeploy = deployVsBo;
				break;
			}
		}
		return vsToBeDeploy;
	}

	private Map<String, DeployAgentBo> nextReadyDeployAgents(DeployVsBo deployVsBo) {
		Map<String, DeployAgentBo> re = new LinkedHashMap<String, DeployAgentBo>();
		Map<String, DeployAgentBo> deployAgentBos = deployVsBo.getDeployAgentBos();
		Set<String> hostNameSet = deployAgentBos.keySet();
		// 顺序遍历agent,获取到第一个需要执行的agent。
		int index = 0;
		for (String ip : hostNameSet) {
			index++;
			DeployAgentBo deployAgentBo = deployAgentBos.get(ip);
			if (!deployAgentBo.getDeployAgent().getStatus().isCompleted()) {
				re.put(ip, deployAgentBo);
				if (agentBatch.isTryFirst() && index == 1) {// 如果要执行的是第一个agent，那么只执行这一个agent。因为第一次都是执行第一台的。
					break;
				}
				if (re.size() >= agentBatch.getBatchSize()) {
					break;
				}
			}
		}
		return re;
	}

	private void execute(DeployVsBo deployVsBo, Map<String, DeployAgentBo> deployAgentBos) throws InterruptedException {
		// 创建Agent执行者
		Map<String, AgentClient> agentClients = new LinkedHashMap<String, AgentClient>();
		for (DeployAgentBo deployAgentBo : deployAgentBos.values()) {
			long agentId = agentSequenceService.getAgentId();
			String vsName = deployVsBo.getDeployVs().getVsName();
			String vsTag = deployVsBo.getDeployVs().getVsTag();
			String ip = deployAgentBo.getDeployAgent().getIpAddress();
			DefaultAgentClient agentClient = new DefaultAgentClient(agentId, vsName, vsTag, ip, virtualServerService,
					strategyService, configManager, f5ApiService, executor);
			agentClients.put(ip, agentClient);
		}

		// 多线程执行agent执行者
		long start = System.currentTimeMillis();
		CountDownLatch doneSignal = new CountDownLatch(agentClients.size());
		for (Map.Entry<String, AgentClient> entry : agentClients.entrySet()) {
			String ip = entry.getKey();
			AgentClient agentClient = entry.getValue();
			executor.execute(new AgentTask(agentClient, doneSignal, deployVsBo.getDeployVs(), ip));
		}
		// 执行的过程中，所有状态，需要反馈过去，包括持久花到数据库
		while (!doneSignal.await(100, TimeUnit.MILLISECONDS)) {
			//更新oldTag
			String oldTag = getOldTag(agentClients);
			deployVsBo.getDeployVs().setOldVsTag(oldTag);
			// 获取agent的执行状态，设置到deployTaskBo中(deployTaskBo含有持久化的状态和内存状态，此处要不要更新deployTaskBo的状态呢？或者在外面Task的管理者统一定时持久化状态？)。
			updateAgentStatus(deployAgentBos, agentClients);
		}
		// 执行完了，再更新一下agent状态
		updateAgentStatus(deployAgentBos, agentClients);
		logger.info("Task Executor spend time: " + (System.currentTimeMillis() - start));

		// 一个batch的agent执行完，根据成败看看如何继续
		boolean hasError = hasError(agentClients);
		if (!hasError) {
			doWhenAgentBatchSuccess();
		} else {
			doWhenAgentBatchError();
		}
	}

	private String getOldTag(Map<String, AgentClient> agentClients) {
		for (Map.Entry<String, AgentClient> entry : agentClients.entrySet()) {
			AgentClient agentClient = entry.getValue();
			AgentClientResult result = agentClient.getResult();
			if (result != null) {
				return result.getOldTag();
			}
		}
		return null;
	}

	/**
	 * 给vs添加log
	 */
	private void appendLog(DeployVs deployVs, String line) {
		String timeStamp = DateFormatUtils.format(System.currentTimeMillis(), pattern);

		String summaryLog = deployVs.getSummaryLog();

		StringBuilder sb = new StringBuilder(summaryLog != null ? summaryLog : "");
		sb.append("\n[").append(timeStamp).append("] ").append(line);

		deployVs.setSummaryLog(sb.toString());// mybatis会trim字符串，故前后到\n会被去除

		deployTaskService.updateDeployVsSummaryLog(deployVs);
	}

	/**
	 * 执行完，如果有错误<br>
	 * 策略是错误跳过，则继续，和上面一样<br>
	 * 策略是错误不跳过，则fail，等待处理<br>
	 */
	private void doWhenAgentBatchError() throws InterruptedException {
		if (this.errorPolicy == ErrorPolicy.FALL_THROUGH) {
			doWhenAgentBatchSuccess();
		} else {
			// task不设置fail状态，有失败不跳过，则是暂停。
			this.autoPause();
		}
	}

	/**
	 * 一个batch的agent执行完，如果都成功 <br>
	 * 策略是手动，则pause <br>
	 * 策略是自动，则睡眠interval后继续
	 */
	private void doWhenAgentBatchSuccess() throws InterruptedException {
		if (!this.autoContinue) {
			this.autoPause();
		} else {
			// this.innerPause(this.deployInterval);
			TimeUnit.SECONDS.sleep(deployInterval);
		}
	}

	/**
	 * 这个batch的agent执行，是否有失败的
	 */
	private boolean hasError(Map<String, AgentClient> agentClients) {
		boolean hasError = false;
		for (AgentClient agentClient : agentClients.values()) {
			AgentClientResult result = agentClient.getResult();
			if (result.getStatus().isNotSuccess()) {
				hasError = true;
				break;
			}
		}
		return hasError;
	}

	/**
	 * 根据孩子(vs)更新task状态
	 */
	private void autoUpdateTaskStatusByChildren(DeployTaskBo deployTaskBo) {
		Map<String, DeployVsBo> deployVsBos = deployTaskBo.getDeployVsBos();

		List<DeployVsStatus> list = new ArrayList<DeployVsStatus>();
		for (DeployVsBo deployVsBo : deployVsBos.values()) {
			list.add(deployVsBo.getDeployVs().getStatus());
		}

		DeployTaskStatus status = deployTaskBo.getTask().getStatus();
		deployTaskBo.getTask().setStatus(status.calculate(list));

		deployTaskService.updateDeployTaskStatus(deployTaskBo.getTask());

		// 如果是结束状态，则StateAction设置为DONE
		if (deployTaskBo.getTask().getStatus().isCompleted()) {
			DefaultTaskExecutor.this.deployTaskBo.getTask().setStateAction(StateAction.STOP);
			deployTaskService.updateDeployTaskStateAction(DefaultTaskExecutor.this.deployTaskBo.getTask());
		}

	}

	/**
	 * 根据孩子(agent)更新vs状态
	 */
	private void autoUpdateVsStatusByChildren(DeployVsBo deployVsBo) {
		Map<String, DeployAgentBo> deployAgentBos = deployVsBo.getDeployAgentBos();

		List<DeployAgentStatus> list = new ArrayList<DeployAgentStatus>();
		for (DeployAgentBo deployAgentBo : deployAgentBos.values()) {
			list.add(deployAgentBo.getDeployAgent().getStatus());
		}

		DeployVsStatus status = deployVsBo.getDeployVs().getStatus();
		deployVsBo.getDeployVs().setStatus(status.calculate(list));

		deployTaskService.updateDeployVsStatus(deployVsBo.getDeployVs());
	}

	/**
	 * agent执行过程中，更新agent状态
	 */
	private void updateAgentStatus(Map<String, DeployAgentBo> deployAgentBos, Map<String, AgentClient> agentClients) {
		for (Map.Entry<String, AgentClient> entry : agentClients.entrySet()) {
			String ip = entry.getKey();
			AgentClient agentClient = entry.getValue();

			AgentClientResult result = agentClient.getResult();
			String currentStep = result.getCurrentStep();
			int processPct = result.getProcessPct();
			List<String> log = result.getLogs();
			DeployAgentStatus status = result.getStatus();

			DeployAgentBo deployAgentBo = deployAgentBos.get(ip);
			deployAgentBo.setCurrentStep(currentStep);
			deployAgentBo.setProcessPct(processPct);
			deployAgentBo.getDeployAgent().setRawLog(convertToRawLog(log));
			deployAgentBo.getDeployAgent().setStatus(status);

			deployTaskService.updateDeployAgentStatusAndLog(deployAgentBo.getDeployAgent());
		}
	}

	private String convertToRawLog(List<String> logs) {
		StringBuilder sb = new StringBuilder();
		for (String line : logs) {
			sb.append(line).append('\n');
		}
		return sb.toString();
	}

	@Override
	public DeployTaskBo getDeployTaskBo() {
		return deployTaskBo;
	}

	/**
	 * 将非SUCCESS状态的task,vs,agent的状态重置为READY
	 */
	private void resetAllAgentStatus() {
		DefaultTaskExecutor.this.deployTaskBo.getTask().setStatus(DeployTaskStatus.READY);
		deployTaskService.updateDeployTaskStatus(DefaultTaskExecutor.this.deployTaskBo.getTask());

		Map<String, DeployVsBo> deployVsBos = this.deployTaskBo.getDeployVsBos();

		for (DeployVsBo deployVsBo : deployVsBos.values()) {
			DeployVsStatus vsStatus = deployVsBo.getDeployVs().getStatus();
			if (vsStatus.isNotSuccess()) {

				deployVsBo.getDeployVs().setStatus(DeployVsStatus.READY);
				deployTaskService.updateDeployVsStatus(deployVsBo.getDeployVs());

				Map<String, DeployAgentBo> deployAgentBos = deployVsBo.getDeployAgentBos();
				for (DeployAgentBo deployAgentBo : deployAgentBos.values()) {
					DeployAgentStatus agentStatus = deployAgentBo.getDeployAgent().getStatus();
					if (agentStatus.isNotSuccess()) {
						deployAgentBo.getDeployAgent().setStatus(DeployAgentStatus.READY);
						deployTaskService.updateDeployAgentStatusAndLog(deployAgentBo.getDeployAgent());
					}
				}
			}
		}
	}

	@Override
	public void start() {
		if (actionLock.tryLock()) {
			logger.info("Task " + DefaultTaskExecutor.this.deployTaskBo.getTask().getName() + " start().");

			try {
				if (taskThread != null && taskThread.isAlive()) {// make sure
					throw new IllegalArgumentException("Task is running, cannot start.");
				}

				// 如果上次的状态是失败的，则认为本次启动是重试失败
				if (DefaultTaskExecutor.this.deployTaskBo.getTask().getStatus().isNotSuccess()) {
					resetAllAgentStatus();
				}

				taskThread = new Thread(new InnerTask(), "TaskExecutor-" + this.deployTaskBo.getTask().getName());
				taskThread.start();

				DefaultTaskExecutor.this.deployTaskBo.getTask().setStateAction(StateAction.START);
				deployTaskService.updateDeployTaskStateAction(DefaultTaskExecutor.this.deployTaskBo.getTask());

			} finally {
				actionLock.unlock();
			}
		}
	}

	@Override
	public void stop() {
		if (actionLock.tryLock()) {
			try {
				if (taskThread != null && taskThread.isAlive()) {
					logger.info("Task " + DefaultTaskExecutor.this.deployTaskBo.getTask().getName() + " stop().");

					// 终止agent线程
					executor.shutdownNow();

					// 终止主线程
					taskThread.interrupt();
					while (taskThread.isAlive()) {
						try {
							taskThread.interrupt();
							taskThread.join(100);
						} catch (InterruptedException e) {
						}
					}

					deployTaskBo.getTask().setStateAction(StateAction.STOP);
					deployTaskService.updateDeployTaskStateAction(DefaultTaskExecutor.this.deployTaskBo.getTask());
				}

			} finally {
				actionLock.unlock();
			}
		}
	}

	private void autoPause() {
		logger.info("Task " + DefaultTaskExecutor.this.deployTaskBo.getTask().getName() + " auto pause.");

		// 终止agent线程
		executor.shutdownNow();

		// 终止主线程
		Thread.currentThread().interrupt();

		DefaultTaskExecutor.this.deployTaskBo.getTask().setStateAction(StateAction.PAUSE);
		deployTaskService.updateDeployTaskStateAction(DefaultTaskExecutor.this.deployTaskBo.getTask());

	}

	private class InnerTask implements Runnable {

		@Override
		public void run() {
			try {
				taskExecutorListener.onBegin(deployTaskBo.getTask().getId());

				logger.info("Task " + DefaultTaskExecutor.this.deployTaskBo.getTask().getName() + " begin.");
				// 获取接下来要发布的vs和agent
				DeployVsBo deployVsBo = null;
				while ((deployVsBo = nextDeployVs(deployTaskBo)) != null && !Thread.currentThread().isInterrupted()) {

					Map<String, DeployAgentBo> deployAgentBos = null;
					while ((deployAgentBos = nextReadyDeployAgents(deployVsBo)).size() > 0 && !Thread.currentThread()
							.isInterrupted()) {
						try {
							execute(deployVsBo, deployAgentBos);
						} catch (InterruptedException e) {
							// 运行Task的主线程中被中断了(应该是被暂停或取消了，此处不要管，结束该方法即可)
							Thread.currentThread().interrupt();
						}
					}
					// 执行完一个vs(或者因为暂停/取消而退出)，需要更新一下vs的状态
					autoUpdateVsStatusByChildren(deployVsBo);
				}

				// 执行完所有vs(或者因为暂停/取消而退出)，需要更新一下task的状态
				autoUpdateTaskStatusByChildren(deployTaskBo);

				logger.info("Task " + DefaultTaskExecutor.this.deployTaskBo.getTask().getName() + " end.");
			} catch (Exception e) {
				taskExecutorListener.onException(deployTaskBo.getTask().getId(), e);
				logger.error("[run]" + deployTaskBo, e);
			} finally {
				taskExecutorListener.onFinish(deployTaskBo.getTask().getId());
			}
		}
	}

	private class AgentTask implements Runnable {
		private AgentClient agentClient;

		private CountDownLatch doneSignal;

		private DeployVs deployVs;

		private String ip;

		public AgentTask(AgentClient agentClient, CountDownLatch doneSignal, DeployVs deployVs, String ip) {
			this.agentClient = agentClient;
			this.doneSignal = doneSignal;
			this.ip = ip;
			this.deployVs = deployVs;
		}

		@Override
		public void run() {
			try {
				appendLog(deployVs, "Agent(" + ip + ") executing.");
				agentClient.execute();
				appendLog(deployVs,
						"Agent(" + ip + ") done. Result is " + agentClient.getResult().getStatus().getDesc());
			} finally {
				doneSignal.countDown();
			}
		}

	}
}
