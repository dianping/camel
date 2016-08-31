package com.dianping.platform.slb.agent.task.processor.config;

import com.dianping.platform.slb.agent.task.Task;
import com.dianping.platform.slb.agent.task.processor.AbstractTransactionProcessor;
import com.dianping.platform.slb.agent.task.workflow.engine.Engine;
import com.dianping.platform.slb.agent.task.workflow.step.impl.ConfigUpgradeStep;
import com.dianping.platform.slb.agent.task.workflow.step.Step;
import com.dianping.platform.slb.agent.transaction.Transaction;
import com.dianping.platform.slb.agent.transaction.manager.TransactionManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.OutputStream;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Service
public class ConfigUpdateTransactionProcessor extends AbstractTransactionProcessor {

	private final static Logger m_logger = Logger.getLogger(ConfigUpdateTransactionProcessor.class);

	@Autowired
	private Engine m_engine;

	@Autowired
	private TransactionManager m_transactionManager;

	@Override
	protected Transaction.Status doTransaction(Transaction transaction) {
		int exitCode = Step.CODE_FAIL;

		try {
			Task task = transaction.getTask();
			OutputStream txOutputStream = m_transactionManager.getLogOutputStream(transaction.getTransactionID());

			task.setTaskOutputStream(txOutputStream);
			exitCode = m_engine.executeStep(ConfigUpgradeStep.INIT, task);
		} catch (Exception ex) {
			exitCode = Step.CODE_FAIL;
			m_logger.error("[do transaction error]" + transaction.getTransactionID(), ex);
		}
		if (exitCode == Step.CODE_SUCCESS) {
			return Transaction.Status.SUCCESS;
		} else {
			return Transaction.Status.FAILED;
		}
	}

	@Override
	public boolean cancel(long txId) {
		if (isTransactionProcessing(txId)) {
			m_engine.kill();
			return true;
		}
		return false;
	}

}
