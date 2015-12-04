package com.dianping.platform.slb.agent.core.processor;

import com.dianping.platform.slb.agent.core.transaction.Transaction;
import com.dianping.platform.slb.agent.core.workflow.Context;
import com.dianping.platform.slb.agent.core.workflow.Engine;
import com.dianping.platform.slb.agent.core.workflow.LogFormatter;
import com.dianping.platform.slb.agent.core.workflow.deploy.DeployContext;
import com.dianping.platform.slb.agent.core.workflow.deploy.DeployStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Service
public class NginxConfDeployProcessor extends AbstractProcessor {

	@Autowired
	LogFormatter m_logFormatter;

	@Autowired
	Engine m_engine;

	private BlockingQueue<Transaction> m_transactionQueue = new LinkedBlockingDeque<Transaction>();

	private boolean m_processing = false;

	@Override
	public SubmitResult submitTransaction(Transaction transaction) {
		SubmitResult submitResult = new SubmitResult();

		try {
			if (m_transactionManager.startTransaction(transaction)) {
				if (m_transactionQueue.offer(transaction)) {
					submitResult.setResult(true);
					startProcessorTransaction();
				} else {
					submitResult.setResult(false);
					submitResult.setMessage("[error]add queue fail");
				}
			} else {
				submitResult.setResult(false);
				submitResult.setMessage("[error]transaction already exists!");
			}
		} catch (IOException e) {
			submitResult.setResult(false);
			submitResult.setMessage(e.getMessage());
		}
		return submitResult;
	}

	@Override
	public void cancel(int id) {
		if (m_currentTransaction != null && m_currentTransaction.get().getId() == id) {
			m_engine.kill(m_currentContext.get());
		}
	}

	private void startProcessorTransaction() {
		if (!m_processing) {
			new Thread(new InnerTask()).start();
			m_processing = true;
		}
	}

	@Override
	protected Transaction.Status doTransaction(Transaction transaction) throws Exception {
		m_currentTransaction.set(transaction);

		Context context = new DeployContext();

		context.setTask(transaction.getTask());
		context.setOutput(m_transactionManager.getLogOut(transaction));
		m_currentContext.set(context);

		if (m_engine.start(DeployStep.START, context) == 0) {
			return Transaction.Status.SUCCESS;
		} else {
			return Transaction.Status.FAILED;
		}
	}

	private class InnerTask implements Runnable {
		@Override
		public void run() {
			try {
				while (true) {
					Transaction transaction = m_transactionQueue.poll(30, TimeUnit.SECONDS);

					if (transaction == null) {
						break;
					}
					runTransaction(transaction);
				}
			} catch (InterruptedException e) {
				m_logger.error("[processor error][NginxConfDeployProcessor]", e);
			} finally {
				m_processing = false;
			}
		}
	}

}