package com.dianping.phoenix.lb.deploy.bo;

import com.dianping.phoenix.lb.deploy.model.DeployTask;

import java.util.Map;

public class DeployTaskBo {

	//    private long                    agentId;

	private DeployTask task;

	private Map<String, DeployVsBo> deployVsBos;

	public DeployTask getTask() {
		return task;
	}

	public void setTask(DeployTask task) {
		this.task = task;
	}

	public Map<String, DeployVsBo> getDeployVsBos() {
		return deployVsBos;
	}

	public void setDeployVsBos(Map<String, DeployVsBo> deploymentBos) {
		this.deployVsBos = deploymentBos;
	}

	//    public long getAgentId() {
	//        return agentId;
	//    }
	//
	//    public void setAgentId(long agentId) {
	//        this.agentId = agentId;
	//    }

}
