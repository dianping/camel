package com.dianping.phoenix.lb.monitor;

import java.util.Map;

public class UpstreamStatus {

	private String name;

	private int availableRate;// 100分制

	private Map<String, NodeStatus> nodeStatus;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAvailableRate() {
		return availableRate;
	}

	public void setAvailableRate(int availableRate) {
		this.availableRate = availableRate;
	}

	public Map<String, NodeStatus> getNodeStatus() {
		return nodeStatus;
	}

	public void setNodeStatus(Map<String, NodeStatus> nodeStatus) {
		this.nodeStatus = nodeStatus;
	}

	@Override
	public String toString() {
		return String
				.format("UpstreamStatus [name=%s, availableRate=%s, nodeStatus=%s]", name, availableRate, nodeStatus);
	}

}
