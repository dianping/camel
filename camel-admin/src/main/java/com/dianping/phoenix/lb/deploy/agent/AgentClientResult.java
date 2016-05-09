/**
 * Project: phoenix-load-balancer
 * <p/>
 * File Created at Dec 9, 2013
 */
package com.dianping.phoenix.lb.deploy.agent;

import com.dianping.phoenix.lb.deploy.model.DeployAgentStatus;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Leo Liang
 *
 */
public class AgentClientResult {

	private static final DateFormat DATE_FOMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	private DeployAgentStatus status = DeployAgentStatus.PROCESSING;

	private List<String> logs = new ArrayList<String>();

	private String currentStep;

	private int processPct;

	private String oldTag;

	public void logInfo(String msg) {
		logs.add("[" + DATE_FOMATTER.format(new Date()) + "] [INFO] " + msg);
	}

	public void logError(String msg, Throwable e) {
		logs.add("[" + DATE_FOMATTER.format(new Date()) + "] [ERROR] " + msg);
		if (e != null) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			e.printStackTrace(new PrintStream(baos));
			try {
				logs.addAll(IOUtils.readLines(new ByteArrayInputStream(baos.toByteArray()), "utf-8"));
			} catch (IOException e2) {
				// ignore
			}
		}
	}

	public void addRawLog(String rawLog) {
		logs.add(rawLog);
	}

	public void addRawLogs(List<String> rawLogs) {
		for (String rawLog : rawLogs) {
			addRawLog(rawLog);
		}
	}

	public void logError(String msg) {
		logError(msg, null);
	}

	public String getCurrentStep() {
		return currentStep;
	}

	public void setCurrentStep(String currentStep) {
		this.currentStep = currentStep;
	}

	public int getProcessPct() {
		return processPct;
	}

	public void setProcessPct(int processPct) {
		this.processPct = processPct;
	}

	public DeployAgentStatus getStatus() {
		return status;
	}

	public void setStatus(DeployAgentStatus status) {
		this.status = status;
	}

	public List<String> getLogs() {

		//避免ConcurrentModifacationException
		return new ArrayList<String>(logs);
	}

	public String getOldTag() {
		return oldTag;
	}

	public void setOldTag(String oldTag) {
		this.oldTag = oldTag;
	}

}
