package com.dianping.platform.slb.agent.core.processor;

import com.dianping.platform.slb.agent.core.transaction.Transaction;

import java.io.Reader;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface Processor {

	SubmitResult submitTransaction(Transaction transaction);

	boolean isTransactionCurrent(int id);

	void cancel(int id);

	class SubmitResult {

		private boolean m_result;

		private String m_message;

		public boolean isResult() {
			return m_result;
		}

		public void setResult(boolean result) {
			m_result = result;
		}

		public String getMessage() {
			return m_message;
		}

		public void setMessage(String message) {
			m_message = message;
		}
	}

}
