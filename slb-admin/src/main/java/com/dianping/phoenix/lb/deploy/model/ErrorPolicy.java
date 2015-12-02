package com.dianping.phoenix.lb.deploy.model;

public enum ErrorPolicy {
	ABORT_ON_ERROR("错误时停止"), FALL_THROUGH("错误时跳过");

	private String desc;

	private ErrorPolicy(String desc) {
		this.desc = desc;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

}
