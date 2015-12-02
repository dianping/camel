package com.dianping.phoenix.lb.deploy.bo;

import com.dianping.phoenix.lb.deploy.model.DeployAgent;

public class DeployAgentBo {

	private DeployAgent deployAgent;

	//正在执行时的内存状态，不持久化
	private String currentStep;

	//正在执行时的内存状态，不持久化
	private int processPct;

	public DeployAgentBo(DeployAgent deployAgent) {
		this.deployAgent = deployAgent;
	}

	public DeployAgentBo() {
	}

	public DeployAgent getDeployAgent() {
		return deployAgent;
	}

	public void setDeployAgent(DeployAgent deployAgent) {
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
