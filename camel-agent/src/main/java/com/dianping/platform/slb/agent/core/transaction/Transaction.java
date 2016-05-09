package com.dianping.platform.slb.agent.core.transaction;

import com.dianping.platform.slb.agent.core.event.EventTracker;
import com.dianping.platform.slb.agent.core.task.Task;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public class Transaction {

	private Task m_task;

	private int m_id;

	private EventTracker m_eventTracker;

	private Status m_status;

	public Transaction(Task task, int id, EventTracker eventTracker) {
		this.m_task = task;
		this.m_id = id;
		this.m_eventTracker = eventTracker;
		this.m_status = Status.INITIALIZED;
	}

	public Task getTask() {
		return m_task;
	}

	public void setTask(Task task) {
		m_task = task;
	}

	public int getId() {
		return m_id;
	}

	public void setId(int id) {
		m_id = id;
	}

	public EventTracker getEventTracker() {
		return m_eventTracker;
	}

	public void setEventTracker(EventTracker eventTracker) {
		m_eventTracker = eventTracker;
	}

	public Status getStatus() {
		return m_status;
	}

	public void setStatus(Status status) {
		m_status = status;
	}

	public enum Status {
		INITIALIZED, REJECTED, PROGRESSING, SUCCESS, FAILED;

		public boolean isComplete(Status status) {
			if (status == Status.INITIALIZED || status == Status.PROGRESSING) {
				return false;
			}
			return true;
		}
	}

}
