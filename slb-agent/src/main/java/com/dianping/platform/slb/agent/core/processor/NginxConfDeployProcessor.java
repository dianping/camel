package com.dianping.platform.slb.agent.core.processor;

import com.dianping.platform.slb.agent.core.transaction.Transaction;
import com.dianping.platform.slb.agent.core.transaction.TransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Service
public class NginxConfDeployProcessor implements Processor {

	@Autowired
	TransactionManager m_transactionManager;

	private BlockingQueue<Transaction> m_transactionQueue = new LinkedBlockingDeque<Transaction>();

	private boolean m_processing = false;

	@Override
	public SubmitResult submitTransaction(Transaction transaction) {
		SubmitResult submitResult = new SubmitResult();

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
		}
		return submitResult;
	}

	@Override
	public Transaction getCurrentTranasction() {
		return null;
	}

	@Override
	public void cancel(int id) {

	}

	private void startProcessorTransaction() {
		if (!m_processing) {
			new Thread(new InnerTask()).start();
			m_processing = true;
		}

	}

	private class InnerTask implements Runnable {

		@Override
		public void run() {

		}
	}
}



