package com.dianping.platform.slb.agent.task.model;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public class SubmitResult {

	private boolean isAccepted = false;

	private String message = "";

	public SubmitResult(boolean isAccepted) {
		this.isAccepted = isAccepted;
	}

	public boolean isAccepted() {
		return isAccepted;
	}

	public void setAccepted(boolean accepted) {
		isAccepted = accepted;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
