package com.dianping.phoenix.lb.model;

import java.util.List;

public class InfluencingVs {

	/**
	 * 影响的vs名称
	 */
	private String vsName;

	/**
	 * 影响的具体位置
	 */
	private List<String> positionDescs;

	public String getVsName() {
		return vsName;
	}

	public void setVsName(String vsName) {
		this.vsName = vsName;
	}

	public List<String> getPositionDescs() {
		return positionDescs;
	}

	public void setPositionDescs(List<String> positionDescs) {
		this.positionDescs = positionDescs;
	}

	@Override
	public int hashCode() {
		return vsName.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof InfluencingVs)) {
			return false;
		}
		InfluencingVs vs = (InfluencingVs) obj;

		return vsName.equals(vs.getVsName());
	}
}
