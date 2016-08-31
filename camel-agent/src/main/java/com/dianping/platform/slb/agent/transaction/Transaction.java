package com.dianping.platform.slb.agent.transaction;

import com.dianping.platform.slb.agent.task.Task;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public class Transaction implements Serializable {

	private long transactionID;

	private transient Task task;

	private Status status;

	private Map<String, Object> properties;

	public enum Status {
		INIT, RUNNNG, SUCCESS, FAILED, KILLED, REJECTED;

		private final static Set<Status> COMPLETED_STATUS = new HashSet<Status>();

		static {
			COMPLETED_STATUS.add(SUCCESS);
			COMPLETED_STATUS.add(FAILED);
			COMPLETED_STATUS.add(KILLED);
			COMPLETED_STATUS.add(REJECTED);
		}

		public boolean isCompleted() {
			if (COMPLETED_STATUS.contains(this)) {
				return true;
			}
			return false;
		}
	}

	public Transaction(long transactionID, Task task) {
		if (transactionID <= 0) {
			throw new IllegalArgumentException("transaction id cannot less than zero");
		}
		this.transactionID = transactionID;
		this.task = task;
		this.status = Status.INIT;
	}

	public long getTransactionID() {
		return transactionID;
	}

	public void setTransactionID(long transactionID) {
		this.transactionID = transactionID;
	}

	public Task getTask() {
		return task;
	}

	public void setTask(Task task) {
		this.task = task;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public Object findProperty(String key) {
		return properties.get(key);
	}

	public void addProperty(String key, Object value) {
		properties.put(key, value);
	}

	@Override
	public String toString() {
		ReflectionToStringBuilder.setDefaultStyle(ToStringStyle.SHORT_PREFIX_STYLE);

		return ReflectionToStringBuilder.toString(this);
	}

}
