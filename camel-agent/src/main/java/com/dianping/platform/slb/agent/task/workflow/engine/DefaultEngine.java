package com.dianping.platform.slb.agent.task.workflow.engine;

import com.dianping.platform.slb.agent.task.Task;
import com.dianping.platform.slb.agent.task.workflow.log.LogPrinter;
import com.dianping.platform.slb.agent.task.workflow.step.Step;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
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

	@Override
	public int executeStep(Step initStep, Task task) throws IOException {
		Step currentStep = initStep;
		int exitCode = 0;

		while (currentStep != null) {
			if (m_isKilled.get()) {
				break;
			}
			try {
				m_logPrinter.writeChunkHeader(task.getTaskOutputStream(), currentStep.getHeader());
			} catch (IOException e) {
				m_logger.error("write chunk header error", e);
			}

			try {
				exitCode = currentStep.doStep(task);
			} catch (Exception ex) {
				exitCode = Step.CODE_FAIL;
				m_logger.error("execute step error", ex);
			}

			try {
				m_logPrinter.writeChunkTerminator(task.getTaskOutputStream());
			} catch (IOException e) {
				m_logger.error("write chunk terminator error", e);
			}
			currentStep = currentStep.getNextStep(exitCode);
		}
		try {
			m_logPrinter.writeTerminator(task.getTaskOutputStream());
		} catch (IOException e) {
			m_logger.error("write log terminator error", e);
		}
		IOUtils.closeQuietly(task.getTaskOutputStream());
		return exitCode;
	}

	@Override
	public void kill() {
		m_isKilled.set(true);
	}

}
