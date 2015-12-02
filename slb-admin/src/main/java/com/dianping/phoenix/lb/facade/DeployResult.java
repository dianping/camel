package com.dianping.phoenix.lb.facade;

import com.dianping.phoenix.lb.deploy.bo.api.DeployTaskApiBo;

public class DeployResult {
	private String poolName;

	private DeployTaskApiBo deployTaskApiBo;

	private boolean success;

	private Exception exception;

	public DeployResult(String poolName) {
		this.poolName = poolName;
	}

	public String getPoolName() {
		return poolName;
	}

	public void setPoolName(String poolName) {
		this.poolName = poolName;
	}

	public DeployTaskApiBo getDeployTaskApiBo() {
		return deployTaskApiBo;
	}

	public void setDeployTaskApiBo(DeployTaskApiBo deployTaskApiBo) {
		this.deployTaskApiBo = deployTaskApiBo;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public Exception getException() {
		return exception;
	}

	public void setException(Exception exception) {
		this.exception = exception;
	}

}
