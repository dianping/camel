package com.dianping.platform.slb.agent.task.workflow.engine;

import com.dianping.platform.slb.agent.constant.Constants;
import com.dianping.platform.slb.agent.task.workflow.log.LogPrinter;
import com.dianping.platform.slb.agent.task.workflow.step.Step;
import com.dianping.platform.slb.agent.transaction.Transaction;
import com.dianping.platform.slb.agent.transaction.manager.TransactionManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Service
public class DefaultEngine implements Engine {

	private final static Logger m_logger = Logger.getLogger(DefaultEngine.class);

	private AtomicBoolean m_isKilled = new AtomicBoolean(false);

	@Autowired
	private LogPrinter m_logPrinter;

	@Autowired
	private TransactionManager m_transactionManager;

	@Override
	public int executeStep(Step initStep, Transaction transaction) throws IOException {
		Step currentStep = initStep;
		long transactionID = transaction.getTransactionID();
		OutputStream transactionOutputStream = m_transactionManager.getLogOutputStream(transactionID);
		int exitCode = 0;

		transaction.addProperty(Constants.TX_PROPERTY_OUTPUT_STREAM, transactionOutputStream);

		while (currentStep != null) {
			if (m_isKilled.get()) {
				break;
			}
			try {
				m_logPrinter.writeChunkHeader(transactionOutputStream, currentStep.getHeader());
			} catch (IOException e) {
				m_logger.error("write chunk header error" + transactionID, e);
			}

			try {
				exitCode = currentStep.doStep(transaction);
			} catch (Exception ex) {
				exitCode = Step.CODE_FAIL;
				m_logger.error("execute step error" + transactionID, ex);
			}

			try {
				m_logPrinter.writeChunkTerminator(transactionOutputStream);
			} catch (IOException e) {
				m_logger.error("write chunk terminator error" + transactionID, e);
			}
			currentStep = currentStep.getNextStep(exitCode);
		}
		try {
			m_logPrinter.writeTerminator(transactionOutputStream);
		} catch (IOException e) {
			m_logger.error("write log terminator error" + transactionID, e);
		}
		return exitCode;
	}

	@Override
	public void kill() {
		m_isKilled.set(true);
	}

}
