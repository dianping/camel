package com.dianping.phoenix.lb.deploy.bo.api;

import com.dianping.phoenix.lb.deploy.model.api.DeployAgentApi;

public class DeployAgentApiBo {

	private DeployAgentApi deployAgent;

	//正在执行时的内存状态，不持久化
	private String currentStep;

	//正在执行时的内存状态，不持久化
	private int processPct;

	public DeployAgentApiBo(DeployAgentApi deployAgent) {
		this.deployAgent = deployAgent;
	}

	public DeployAgentApiBo() {
	}

	public DeployAgentApi getDeployAgent() {
		return deployAgent;
	}

	public void setDeployAgent(DeployAgentApi deployAgent) {
		this.deployAgent = deployAgent;
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

}
