package com.dianping.phoenix.lb.facade.impl;

import com.dianping.phoenix.lb.constant.MessageID;
import com.dianping.phoenix.lb.deploy.bo.api.DeployTaskApiBo;
import com.dianping.phoenix.lb.deploy.executor.TaskExecutor;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.facade.ApiDeployExecutor;
import com.dianping.phoenix.lb.facade.DeployResult;
import com.dianping.phoenix.lb.facade.PoolFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Service
public class DefaultApiDeployExecutor implements ApiDeployExecutor {

	private static final Logger logger = LoggerFactory.getLogger(DefaultApiDeployExecutor.class);
	private static final int EXPECTED_BATCH_SIZE = 3;
	private LinkedBlockingQueue<DeployResult> poolQueue = new LinkedBlockingQueue<DeployResult>();
	@Autowired
	private PoolFacade<DeployTaskApiBo> poolFacade;

	private volatile boolean stop = false;

	@Resource(name = "deployThreadPool")
	private ExecutorService executors;

	@Override
	public DeployResult execute(String poolName) {

		logger.info("Deploy api call of PoolName[" + poolName + "] submited");

		DeployResult deployResult = new DeployResult(poolName);
		poolQueue.add(deployResult);

		synchronized (deployResult) {
			try {
				deployResult.wait();
			} catch (InterruptedException e) {
			}
		}
		return deployResult;

	}

	private void dispatchTask(final List<DeployResult> deployResults) {

		executors.execute(new DeployTask(deployResults));
	}

	@PostConstruct
	public void init() {

		executors.execute(new Runnable() {

			@Override
			public void run() {
				// task dispatcher
				while (!stop) {
					try {
						List<DeployResult> deployResults = pollRequest();
						if (logger.isInfoEnabled()) {
							logger.info("[run]" + deployResults);
						}
						dispatchTask(deployResults);
					} catch (InterruptedException e) {
						logger.error("[run]", e);
					} catch (Throwable th) {
						logger.error("[run]", th);
					}
				}
			}
		});
	}

	private List<DeployResult> pollRequest() throws InterruptedException {

		List<DeployResult> deployResults = new ArrayList<DeployResult>();

		DeployResult deployResult = poolQueue.take();

		deployResults.add(deployResult);

		// 一次部署多个pool
		for (int i = 0; i < EXPECTED_BATCH_SIZE; i++) {
			try {
				TimeUnit.MILLISECONDS.sleep(10);
			} catch (InterruptedException e) {
				//nothing to do
			}
			deployResult = poolQueue.poll();
			if (deployResult != null) {
				deployResults.add(deployResult);
				continue;
			}
			break;
		}
		return deployResults;
	}

	@PreDestroy
	public void destroy() {

		stop = true;
		executors.shutdownNow();
	}

	protected List<String> getPoolNames(List<DeployResult> deployResults) {
		List<String> list = new ArrayList<String>();
		if (deployResults != null && deployResults.size() > 0) {
			for (DeployResult deployResult : deployResults) {
				list.add(deployResult.getPoolName());
			}
		}
		return list;
	}

	class DeployTask implements Runnable {

		private List<DeployResult> deployResults;

		public DeployTask(List<DeployResult> deployResults) {

			this.deployResults = deployResults;
		}

		@Override
		public void run() {

			List<String> poolNames = null;
			try {
				poolNames = getPoolNames(deployResults);

				logger.info("[run][Going to deploy these pools]" + poolNames);

				TaskExecutor<DeployTaskApiBo> taskExecutor = poolFacade.deploy(poolNames);

				DeployTaskApiBo deployTaskBo = taskExecutor.getDeployTaskBo();

				for (DeployResult deployResult0 : deployResults) {
					deployResult0.setDeployTaskApiBo(deployTaskBo);
				}

				while (!deployTaskBo.getTask().getStatus().isCompleted()) {
					TimeUnit.MILLISECONDS.sleep(10);
				}

				logger.info("Deploy these pools done: " + poolNames);

				// 结果
				boolean result = true;
				Exception e = null;
				long taskId = deployTaskBo.getTask().getId();
				switch (deployTaskBo.getTask().getStatus()) {
				case WARNING:
					result = false;
					e = new Exception("warning, see log, " + taskId);
					break;
				case FAILED:
					result = false;
					e = new Exception("failed, see log, " + taskId);
					break;
				case CANCELLED:
					result = false;
					e = new Exception("cancelled, see log, " + taskId);
					break;
				default:
					result = true;
					break;
				}

				for (DeployResult deployResult0 : deployResults) {
					deployResult0.setSuccess(result);
					deployResult0.setException(e);
				}
			} catch (Exception e) {

				boolean result = false;
				if (e instanceof BizException) {
					BizException bz = (BizException) e;
					if (bz.getMessageId() == MessageID.DEPLOY_POOL_NOT_RELATED_VS) {
						// 认为发布成功
						if (logger.isInfoEnabled()) {
							logger.info("[run][deploy pools not related with vs, treat it as succeed]" + poolNames);
						}
						result = true;
					}
				}

				// 发布失败
				for (DeployResult deployResult0 : deployResults) {
					deployResult0.setSuccess(result);
					deployResult0.setException(e);
				}
			} finally {
				// 唤醒所有请求
				for (DeployResult deployResult0 : deployResults) {
					synchronized (deployResult0) {
						deployResult0.notifyAll();
					}
				}
			}
		}
	}
}
