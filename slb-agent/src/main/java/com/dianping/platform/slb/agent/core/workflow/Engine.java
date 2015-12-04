package com.dianping.platform.slb.agent.core.workflow;

import com.dianping.platform.slb.agent.core.task.LogFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.util.Map;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Service
public class Engine {

	private static Logger logger = LoggerFactory.getLogger(Engine.class);

	@Autowired
	LogFormatter logFormatter;

	public int start(Step startStep, Context ctx) {
		Step curStep = startStep;

		int exitCode = 0;

		OutputStream logOut = ctx.getOutput();

		while (curStep != null) {
			if (ctx.isKilled()) {
				break;
			}
			writeLogChunkHeader(curStep.getLogChunkHeader(), logOut);
			logger.info(String.format(">>>>> Current Step Is [%s] <<<<<", curStep));
			try {
				exitCode = curStep.doStep(ctx);
			} catch (Exception e) {
				logger.error(String.format(">>>>> Exception When Doing Step %s <<<<<", curStep), e);
				exitCode = Step.CODE_ERROR;
			}
			logger.info(String.format("##### Current Step [%s] Is [%s] ####", curStep,
					exitCode == Step.CODE_OK ? "SUCCESS" : "FAILED"));
			writeLogChunkTerminator(logOut);
			curStep = curStep.getNextStep(exitCode);
		}
		writeLogTerminator(logOut);
		return exitCode;
	}

	private void writeLogChunkHeader(Map<String, String> headers, OutputStream logOut) {
		try {
			logFormatter.writeHeader(logOut, headers);
		} catch (Exception e) {
			logger.error("error write log chunk header", e);
		}
	}

	private void writeLogChunkTerminator(OutputStream logOut) {
		try {
			logFormatter.writeChunkTerminator(logOut);
		} catch (Exception e) {
			logger.error("error write log chunk terminator", e);
		}
	}

	private void writeLogTerminator(OutputStream logOut) {
		try {
			logFormatter.writeTerminator(logOut);
		} catch (Exception e) {
			logger.error("error write log terminator", e);
		}
	}

	public void kill(Context ctx) {
		ctx.kill();
	}
}
