package com.dianping.phoenix.lb.deploy.bo.api;

import com.dianping.phoenix.lb.deploy.model.api.DeployTaskApi;

import java.util.List;

public class DeployTaskApiBo {

	// private long agentId;

	private DeployTaskApi task;

	private List<DeployAgentApiBo> deployAgentBos;

	public DeployTaskApi getTask() {
		return task;
	}

	public void setTask(DeployTaskApi task) {
		this.task = task;
	}

	public List<DeployAgentApiBo> getDeployAgentBos() {
		return deployAgentBos;
	}

	public void setDeployAgentBos(List<DeployAgentApiBo> deployAgentBos) {
		this.deployAgentBos = deployAgentBos;
	}

	// public long getAgentId() {
	// return agentId;
	// }
	//
	// public void setAgentId(long agentId) {
	// this.agentId = agentId;
	// }

	@Override
	public String toString() {

		return "taskId:" + task.getId() + ", taskName:" + task.getName();
	}
}
