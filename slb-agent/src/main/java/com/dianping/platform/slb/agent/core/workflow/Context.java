package com.dianping.platform.slb.agent.core.workflow;

import com.dianping.platform.slb.agent.core.task.Task;

import java.io.OutputStream;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public class Context {

	private Task m_task;

	private OutputStream m_output;

	private boolean m_isKilled;

	public Task getTask() {
		return m_task;
	}

	public void setTask(Task task) {
		m_task = task;
	}

	public OutputStream getOutput() {
		return m_output;
	}

	public void setOutput(OutputStream output) {
		m_output = output;
	}

	public boolean isKilled() {
		return m_isKilled;
	}

	public void kill() {
		m_isKilled = true;
	}
}
