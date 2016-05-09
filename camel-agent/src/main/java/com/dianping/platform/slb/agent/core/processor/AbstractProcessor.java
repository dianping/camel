package com.dianping.platform.slb.agent.core.processor;

import com.dianping.platform.slb.agent.core.transaction.Transaction;
import com.dianping.platform.slb.agent.core.transaction.TransactionManager;
import com.dianping.platform.slb.agent.core.workflow.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public abstract class AbstractProcessor implements Processor {

	@Autowired
	TransactionManager m_transactionManager;

	protected AtomicReference<Transaction> m_currentTransaction = new AtomicReference<Transaction>();

	protected AtomicReference<Context> m_currentContext = new AtomicReference<Context>();

	protected final Logger m_logger = LoggerFactory.getLogger(getClass());

	@Override
	public boolean isTransactionCurrent(int id) {
		return m_currentTransaction.get().getId() == id;
	}

	protected void runTransaction(Transaction transaction) {
		try {
			startTransaction(transaction);

			try {
				transaction.setStatus(doTransaction(transaction));
			} catch (Exception ex) {
				transaction.setStatus(Transaction.Status.FAILED);
				m_logger.error("[processor][exec transaction error]" + transaction.getId(), ex);
			}
		} catch (Exception ex) {
			m_logger.error("[processor][init transaction error]" + transaction.getId(), ex);
			transaction.setStatus(Transaction.Status.FAILED);
		} finally {
			endTransaction(transaction);
		}
	}

	private void startTransaction(Transaction transaction) throws IOException {
		transaction.setStatus(Transaction.Status.PROGRESSING);
		m_transactionManager.saveTransaction(transaction);
	}

	private void endTransaction(Transaction transaction) {
		try {
			m_currentTransaction = new AtomicReference<Transaction>();
			m_currentContext = new AtomicReference<Context>();

			m_transactionManager.saveTransaction(transaction);
		} catch (Exception ex) {
			m_logger.error("[processor][end transaction error]" + transaction.getId(), ex);
		} finally {
			m_logger.info("[process transaction]" + transaction.getId() + " " + transaction.getStatus());
		}
	}

	protected abstract Transaction.Status doTransaction(Transaction transaction) throws Exception;

}
