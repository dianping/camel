package com.dianping.phoenix.lb.monitor;

public class ApiResult {

	private String tengineIp;

	private String upstreamName;

	private int availableRate;

	private NodeStatus.Status nodeStatus;

	public String getTengineIp() {
		return tengineIp;
	}

	public void setTengineIp(String tengineIp) {
		this.tengineIp = tengineIp;
	}

	public String getUpstreamName() {
		return upstreamName;
	}

	public void setUpstreamName(String upstreamName) {
		this.upstreamName = upstreamName;
	}

	public int getAvailableRate() {
		return availableRate;
	}

	public void setAvailableRate(int availableRate) {
		this.availableRate = availableRate;
	}

	public NodeStatus.Status getNodeStatus() {
		return nodeStatus;
	}

	public void setNodeStatus(NodeStatus.Status nodeStatus) {
		this.nodeStatus = nodeStatus;
	}

	@Override
	public String toString() {
		return String.format("ApiResult [tengineIp=%s, upstreamName=%s, availableRate=%s, nodeStatus=%s]", tengineIp,
				upstreamName, availableRate, nodeStatus);
	}

}
