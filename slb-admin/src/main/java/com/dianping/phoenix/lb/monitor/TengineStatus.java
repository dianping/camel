package com.dianping.phoenix.lb.monitor;

import java.util.Map;

public class TengineStatus {

	private String ip;

	/**
	 * 节点总数
	 */
	private int totalNode;

	private Map<String, UpstreamStatus> upstreamStatusMap;

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getTotalNode() {
		return totalNode;
	}

	public void setTotalNode(int totalNode) {
		this.totalNode = totalNode;
	}

	public Map<String, UpstreamStatus> getUpstreamStatusMap() {
		return upstreamStatusMap;
	}

	public void setUpstreamStatusMap(Map<String, UpstreamStatus> upstreamStatusMap) {
		this.upstreamStatusMap = upstreamStatusMap;
	}

	@Override
	public String toString() {
		return String
				.format("TengineStatus [ip=%s, totalNode=%s, upstreamStatusMap=%s]", ip, totalNode, upstreamStatusMap);
	}

}
