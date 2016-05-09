package com.dianping.phoenix.lb.deploy.model;

import java.util.HashSet;
import java.util.Set;

public enum DeployAgentStatus {
	READY("就绪"), PROCESSING("执行发布中"), REJECTED("拒绝"), FAILED("失败"), SUCCESS("成功"), CANCELLED("被取消");

	private final static Set<DeployAgentStatus> COMPLETED_STATUS_SET = new HashSet<DeployAgentStatus>();
	private final static Set<DeployAgentStatus> ERROR_STATUS_SET = new HashSet<DeployAgentStatus>();

	static {
		COMPLETED_STATUS_SET.add(REJECTED);
		COMPLETED_STATUS_SET.add(FAILED);
		COMPLETED_STATUS_SET.add(SUCCESS);
		COMPLETED_STATUS_SET.add(CANCELLED);
	}

	static {
		ERROR_STATUS_SET.add(REJECTED);
		ERROR_STATUS_SET.add(FAILED);
	}

	;

	private String desc;

	private DeployAgentStatus(String desc) {
		this.desc = desc;
	}

	;

	/**
	 * 是否是终结状态
	 *
	 * @return
	 */
	public boolean isCompleted() {
		return COMPLETED_STATUS_SET.contains(this);
	}

	public boolean isNotSuccess() {
		return ERROR_STATUS_SET.contains(this);
	}

	public String getDesc() {
		return desc;
	}

}
