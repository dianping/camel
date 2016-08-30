package com.dianping.platform.slb.agent.web.model;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public class Response {

	private String status;

	private String message;

	public String getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status.getMessage();
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public enum Status {
		SUCCESS("ok"), FAIL("error");

		private String message;

		Status(String message) {
			this.message = message;
		}

		private String getMessage() {
			return this.message;
		}
	}

}
