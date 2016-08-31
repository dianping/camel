package com.dianping.platform.slb.agent.task.processor;

import com.dianping.platform.slb.agent.task.model.SubmitResult;
import com.dianping.platform.slb.agent.transaction.Transaction;
import com.dianping.platform.slb.agent.transaction.manager.TransactionManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public abstract class AbstractTransactionProcessor implements TransactionProcessor {

	private static final Logger m_logger = Logger.getLogger(AbstractTransactionProcessor.class);

	protected BlockingQueue<Transaction> m_transactions = new LinkedBlockingDeque<Transaction>();

	private AtomicReference<Transaction> m_currentTransaction = new AtomicReference<Transaction>();

	private volatile boolean m_isTransactionDealerRunning = false;

	@Autowired
	protected TransactionManager m_transactionManager;

	@Override
	public SubmitResult submit(Transaction transaction) throws Exception {
		m_logger.info("try to submit " + transaction);

		SubmitResult submitResult = new SubmitResult(false);
		final long txId = transaction.getTransactionID();

		if (!m_transactionManager.startTransaction(txId)) {
			m_logger.warn(String.format("transaction with same id exists, reject it %s", txId));
			submitResult.setAccepted(false);
			submitResult.setMessage("duplicate transaction id, please donot submit more than once.");
		} else {
			submitResult = addTransaction(transaction);
		}

		return submitResult;
	}

	protected SubmitResult addTransaction(Transaction transaction) {
		SubmitResult result = new SubmitResult(false);

		if (!m_transactions.offer(transaction)) {
			result.setAccepted(false);
			result.setMessage("put to queue failed");
			return result;
		}
		result.setAccepted(true);
		result.setMessage("queue size: " + m_transactions.size());
		startTransactionDealer();
		return result;
	}

	private void startTransactionDealer() {
		if (!m_isTransactionDealerRunning) {
			new Thread(new TransactionDealer()).start();
		}
	}

	protected void runTransaction(Transaction transaction) throws Exception {
		try {
			startTransaction(transaction);

			transaction.setStatus(doTransaction(transaction));
		} catch (Exception ex) {
			transaction.setStatus(Transaction.Status.FAILED);
			throw ex;
		} finally {
			endTransaction(transaction);
		}
	}

	private void startTransaction(Transaction transaction) throws IOException {
		m_currentTransaction.set(transaction);
		transaction.setStatus(Transaction.Status.RUNNNG);
		m_transactionManager.saveTransaction(transaction);
	}

	private void endTransaction(Transaction transaction) {
		try {
			m_transactionManager.saveTransaction(transaction);
		} catch (IOException e) {
			m_logger.error("[save transaction error]" + transaction.getTransactionID());
		} finally {
			m_currentTransaction.set(null);
		}
	}

	protected abstract Transaction.Status doTransaction(Transaction transaction);

	@Override
	public boolean isTransactionProcessing(long transactionId) {
		Transaction currentTransaction = m_currentTransaction.get();

		if ((currentTransaction != null) && (currentTransaction.getTransactionID() == transactionId)) {
			return true;
		}
		return false;
	}

	class TransactionDealer implements Runnable {
		@Override
		public void run() {
			m_isTransactionDealerRunning = true;

			try {
				while (true) {
					Transaction transaction = m_transactions.poll(30, TimeUnit.SECONDS);

					if (transaction == null) {
						break;
					} else {
						try {
							runTransaction(transaction);
						} catch (Exception ex) {
							m_logger.error("[run transaction error] id: " + transaction.getTransactionID());
						}
					}
				}
			} catch (InterruptedException e) {
				m_logger.error("[extract transaction error]", e);
			} finally {
				m_isTransactionDealerRunning = false;
			}
		}
	}

}