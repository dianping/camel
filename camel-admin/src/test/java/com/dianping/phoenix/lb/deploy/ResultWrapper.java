package com.dianping.phoenix.lb.deploy;

public class ResultWrapper {

	private boolean successed;

	private String message;

	public ResultWrapper(boolean successed, String message) {

		this.successed = successed;
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public boolean isSuccessed() {
		return successed;
	}

	public void setSuccessed(boolean successed) {
		this.successed = successed;
	}

}
