package com.dianping.platform.slb.agent.core.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractStep implements Step {

	private final Logger m_logger = LoggerFactory.getLogger(getClass());

	private AbstractStep nextStepWhenSuccess;

	private AbstractStep nextStepWhenFail;

	private int stepSeq;

	protected AbstractStep(AbstractStep nextStepWhenSuccess, AbstractStep nextStepWhenFail, int stepSeq) {
		this.nextStepWhenSuccess = nextStepWhenSuccess;
		this.nextStepWhenFail = nextStepWhenFail;
		this.stepSeq = stepSeq;
	}

	@Override
	public Step getNextStep(int exitCode) {
		if (exitCode != CODE_OK) {
			return nextStepWhenFail;
		} else {
			return nextStepWhenSuccess;
		}
	}

	@Override
	public Map<String, String> getLogChunkHeader() {
		Map<String, String> header = new HashMap<String, String>();
		header.put(HEADER_STEP, toString());
		header.put(HEADER_PROGRESS, String.format("%s/%s", stepSeq, getTotalStep()));
		return header;
	}

	public int doStep(Context ctx) {
		int stepCode = Step.CODE_ERROR;

		try {
			stepCode = doActivity(ctx);
		} catch (Exception e) {
			m_logger.error("[step failed]", e);
		} finally {
		}
		return stepCode;
	}

	protected abstract int getTotalStep();

	protected abstract int doActivity(Context ctx) throws Exception;

}
