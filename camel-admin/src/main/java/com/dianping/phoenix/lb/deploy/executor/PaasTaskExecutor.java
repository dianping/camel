package com.dianping.phoenix.lb.deploy.executor;

import com.dianping.phoenix.lb.configure.ConfigManager;
import com.dianping.phoenix.lb.deploy.agent.AgentClient;
import com.dianping.phoenix.lb.deploy.agent.AgentClientResult;
import com.dianping.phoenix.lb.deploy.agent.BatchAgentClient;
import com.dianping.phoenix.lb.deploy.bo.api.DeployAgentApiBo;
import com.dianping.phoenix.lb.deploy.bo.api.DeployTaskApiBo;
import com.dianping.phoenix.lb.deploy.f5.F5ApiService;
import com.dianping.phoenix.lb.deploy.model.DeployAgentStatus;
import com.dianping.phoenix.lb.deploy.model.DeployTaskStatus;
import com.dianping.phoenix.lb.deploy.model.StateAction;
import com.dianping.phoenix.lb.deploy.service.AgentSequenceService;
import com.dianping.phoenix.lb.deploy.service.DeployTaskApiService;
import com.dianping.phoenix.lb.service.model.StrategyService;
import com.dianping.phoenix.lb.service.model.VirtualServerService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class PaasTaskExecutor extends AbstractTaskExecutor<DeployTaskApiBo> implements TaskExecutor<DeployTaskApiBo> {

	private static final char SEPARATOR_CHAR = ',';

	private final DeployTaskApiBo deployTaskBo;

	private final DeployTaskApiService deployTaskService;

	private final VirtualServerService virtualServerService;

	private final StrategyService strategyService;

	private final AgentSequenceService agentSequenceService;

	private final F5ApiService f5ApiService;

	private final ConfigManager configManager;

	private final ReentrantLock actionLock = new ReentrantLock();

	private ExecutorService executor;

	private TaskExecutorListener taskExecutorListener;

	public PaasTaskExecutor(TaskExecutorListener taskExecutorListener, DeployTaskApiBo deployTaskBo,
			DeployTaskApiService deployTaskService, VirtualServerService virtualServerService,
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
		this.executor = Executors.newCachedThreadPool();
	}

	private void execute() {

		if (logger.isInfoEnabled()) {
			logger.info("[execute][begin]" + getDeployTaskBo());
		}
		// 创建Agent执行者
		Map<String, AgentClient> agentClients = new LinkedHashMap<String, AgentClient>();

		for (DeployAgentApiBo deployAgentBo : deployTaskBo.getDeployAgentBos()) {
			long agentId = agentSequenceService.getAgentId();

			// 构建BatchAgentClient
			String ip = deployAgentBo.getDeployAgent().getIpAddress();
			List<String> vsNames = Arrays
					.asList(StringUtils.split(deployAgentBo.getDeployAgent().getVsNames(), SEPARATOR_CHAR));
			List<String> tags = Arrays
					.asList(StringUtils.split(deployAgentBo.getDeployAgent().getVsTags(), SEPARATOR_CHAR));

			BatchAgentClient agentClient = new BatchAgentClient(agentId, vsNames, tags, ip, virtualServerService,
					strategyService, configManager, f5ApiService, executor);
			agentClients.put(ip, agentClient);
		}

		// 多线程执行agent执行者
		long start = System.currentTimeMillis();
		int size = agentClients.size();
		if (logger.isInfoEnabled()) {
			logger.info("[execute][create latch]" + size + "," + getDeployTaskBo());
		}

		CountDownLatch doneSignal = new CountDownLatch(size);
		for (Map.Entry<String, AgentClient> entry : agentClients.entrySet()) {
			String ip = entry.getKey();
			AgentClient agentClient = entry.getValue();
			executor.execute(new AgentTask(agentClient, doneSignal, ip));
		}

		if (logger.isInfoEnabled()) {
			logger.info("[execute][submit agent task]" + getDeployTaskBo());
		}
		// 执行的过程中，所有状态，需要反馈过去，包括持久花到数据库
		try {
			while (!doneSignal.await(100, TimeUnit.MILLISECONDS)) {
				// 获取agent的执行状态，设置到deployTaskBo中(deployTaskBo含有持久化的状态和内存状态，此处要不要更新deployTaskBo的状态呢？或者在外面Task的管理者统一定时持久化状态？)。
				updateAgentStatus(agentClients);
			}
		} catch (Exception e) {
			logger.error("[execute]" + getDeployTaskBo(), e);
		}

		if (logger.isInfoEnabled()) {
			logger.info("[execute][wait task execute finished]" + getDeployTaskBo());
		}

		// 执行完了，再更新一下agent状态
		updateAgentStatus(agentClients);

		if (logger.isInfoEnabled()) {
			logger.info("[execute][Task Executor spend time]" + (System.currentTimeMillis() - start));
		}
		if (logger.isInfoEnabled()) {
			logger.info("[execute][end]" + getDeployTaskBo());
		}
	}

	/**
	 * 给Task添加log
	 */
	private void appendLog(String line) {
		String timeStamp = DateFormatUtils.format(System.currentTimeMillis(), pattern);

		String summaryLog = deployTaskBo.getTask().getSummaryLog();

		StringBuilder sb = new StringBuilder(summaryLog != null ? summaryLog : "");
		sb.append("\n[").append(timeStamp).append("] ").append(line);

		deployTaskBo.getTask().setSummaryLog(sb.toString());// mybatis会trim字符串，故前后到\n会被去除

		deployTaskService.updateDeployTaskStatusAndLog(deployTaskBo.getTask());
	}

	/**
	 * 根据所有agent状态更新task状态
	 */
	private void autoUpdateTaskStatusByChildren() {
		List<DeployAgentApiBo> deployAgentBos = deployTaskBo.getDeployAgentBos();

		List<DeployAgentStatus> list = new ArrayList<DeployAgentStatus>();
		for (DeployAgentApiBo deployAgentBo : deployAgentBos) {
			list.add(deployAgentBo.getDeployAgent().getStatus());
		}

		DeployTaskStatus status = deployTaskBo.getTask().getStatus();
		deployTaskBo.getTask().setStatus(status.calculateForAgent(list));

		deployTaskService.updateDeployTaskStatusAndLog(deployTaskBo.getTask());

		// 如果是结束状态，则StateAction设置为DONE
		if (deployTaskBo.getTask().getStatus().isCompleted()) {
			//TODO done??
			PaasTaskExecutor.this.deployTaskBo.getTask().setStateAction(StateAction.STOP);
			deployTaskService.updateDeployTaskStateAction(PaasTaskExecutor.this.deployTaskBo.getTask());
		}

	}

	/**
	 * agent执行过程中，更新agent状态
	 */
	private void updateAgentStatus(Map<String, AgentClient> agentClients) {
		for (DeployAgentApiBo deployAgentBo : deployTaskBo.getDeployAgentBos()) {
			String ip = deployAgentBo.getDeployAgent().getIpAddress();
			AgentClient agentClient = agentClients.get(ip);

			AgentClientResult result = agentClient.getResult();
			String currentStep = result.getCurrentStep();
			int processPct = result.getProcessPct();
			List<String> log = result.getLogs();
			DeployAgentStatus status = result.getStatus();

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
	public DeployTaskApiBo getDeployTaskBo() {
		return deployTaskBo;
	}

	/**
	 * 将非SUCCESS状态的task,vs,agent的状态重置为READY
	 */
	private void resetAllAgentStatus() {
		PaasTaskExecutor.this.deployTaskBo.getTask().setStatus(DeployTaskStatus.READY);
		deployTaskService.updateDeployTaskStatusAndLog(PaasTaskExecutor.this.deployTaskBo.getTask());

		List<DeployAgentApiBo> deployAgentBos = deployTaskBo.getDeployAgentBos();
		for (DeployAgentApiBo deployAgentBo : deployAgentBos) {
			DeployAgentStatus agentStatus = deployAgentBo.getDeployAgent().getStatus();
			if (agentStatus.isNotSuccess()) {
				deployAgentBo.getDeployAgent().setStatus(DeployAgentStatus.READY);
				deployTaskService.updateDeployAgentStatusAndLog(deployAgentBo.getDeployAgent());
			}
		}
	}

	@Override
	public void start() {
		if (actionLock.tryLock()) {
			logger.info("Task " + PaasTaskExecutor.this.deployTaskBo.getTask().getName() + " start().");

			try {
				if (taskThread != null && taskThread.isAlive()) {// make sure
					throw new IllegalArgumentException("Task is running, cannot start.");
				}

				// 如果上次的状态是失败的，则认为本次启动是重试失败
				if (PaasTaskExecutor.this.deployTaskBo.getTask().getStatus().isNotSuccess()) {
					resetAllAgentStatus();
				}

				taskThread = new Thread(new InnerTask(), "TaskExecutor-" + this.deployTaskBo.getTask().getName());

				PaasTaskExecutor.this.deployTaskBo.getTask().setStateAction(StateAction.START);
				// start的状态不持久化，因为应用重启也不会重新执行
				// deployTaskService.updateDeployTaskStateAction(PaasTaskExecutor.this.deployTaskBo.getTask());

				taskThread.start();
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

					if (logger.isInfoEnabled()) {
						logger.info("[stop][stop task begin]" + PaasTaskExecutor.this.deployTaskBo.getTask().getName());
					}

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
				}
				if (deployTaskBo.getTask().getStateAction() != StateAction.STOP) {
					deployTaskBo.getTask().setStateAction(StateAction.STOP);

					//设置任务取消，防止死等
					deployTaskBo.getTask().setStatus(DeployTaskStatus.CANCELLED);
					deployTaskService.updateDeployTaskStatusAndLog(deployTaskBo.getTask());

					deployTaskService.updateDeployTaskStateAction(PaasTaskExecutor.this.deployTaskBo.getTask());
				}
				if (logger.isInfoEnabled()) {
					logger.info("[stop][stop task end]" + PaasTaskExecutor.this.deployTaskBo.getTask().getName());
				}
			} finally {
				actionLock.unlock();
			}
		}
	}

	private class InnerTask implements Runnable {

		@Override
		public void run() {
			try {

				taskExecutorListener.onBegin(deployTaskBo.getTask().getId());

				logger.info("[run][task begin]" + getDeployTaskBo());
				// 直接运行多个agentCleint，每个agent线程运行完自己更新status。 所有agent运行完，再更新task的status。
				try {
					execute();
				} catch (Exception e) {
					logger.error("[error executing task]", e);
				}
				// 执行完所有vs(或者因为暂停/取消而退出)，需要更新一下task的状态
				autoUpdateTaskStatusByChildren();
				logger.info("[run][task end]" + getDeployTaskBo());

			} catch (Exception e) {
				logger.error("[error executing task]" + getDeployTaskBo(), e);
				taskExecutorListener.onException(deployTaskBo.getTask().getId(), e);
			} finally {
				taskExecutorListener.onFinish(deployTaskBo.getTask().getId());
			}
		}
	}

	private class AgentTask implements Runnable {
		private AgentClient agentClient;

		private CountDownLatch doneSignal;

		private String ip;

		public AgentTask(AgentClient agentClient, CountDownLatch doneSignal, String ip) {
			this.agentClient = agentClient;
			this.doneSignal = doneSignal;
			this.ip = ip;
		}

		@Override
		public void run() {
			try {
				appendLog("Agent(" + ip + ") executing.");
				agentClient.execute();
				appendLog("Agent(" + ip + ") done. Result is " + agentClient.getResult().getStatus().getDesc());
			} finally {
				doneSignal.countDown();
			}
		}

	}

}
