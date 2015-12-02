package com.dianping.phoenix.lb.deploy.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * vs只记录结果，当作result来使用
 */
public enum DeployVsStatus {

	READY("就绪"), //创建完，选择了vs，未选择agent

	//    WAITING("已就绪，未执行"), //创建完，选择了vs和选择agent，可以被点击启动执行

	//    READY("即将执行"), //已准备好，等待executor调度 （只有这个状态可被执行）

	//    PROCESSING("正在执行"), //内存状态，不需要持久化

	WARNING("执行完成(未完全成功)"), // completed with partial failures

	FAILED("执行失败"), // complete with all failed

	SUCCESS("执行成功"); // completed with all successful

	private final static Set<DeployVsStatus> COMPLETED_STATUS_SET = new HashSet<DeployVsStatus>();
	private final static Set<DeployVsStatus> ERROR_STATUS_SET = new HashSet<DeployVsStatus>();

	static {
		COMPLETED_STATUS_SET.add(WARNING);
		COMPLETED_STATUS_SET.add(FAILED);
		COMPLETED_STATUS_SET.add(SUCCESS);
	}

	static {
		ERROR_STATUS_SET.add(WARNING);
		ERROR_STATUS_SET.add(FAILED);
	}

	;

	private String desc;

	private DeployVsStatus(String desc) {
		this.desc = desc;
	}

	;

	public boolean isCompleted() {
		return COMPLETED_STATUS_SET.contains(this);
	}

	public boolean isNotSuccess() {
		return ERROR_STATUS_SET.contains(this);
	}

	/**
	 * 根据子任务(agent任务)的状态，计算父亲的状态
	 */
	public DeployVsStatus calculate(List<DeployAgentStatus> deployAgentStatusList) {
		boolean hasFailed = false;
		boolean hasSuccess = false;
		boolean hasNoCompleted = false;
		for (DeployAgentStatus deployAgentStatus : deployAgentStatusList) {
			if (deployAgentStatus == DeployAgentStatus.SUCCESS) {
				hasSuccess = true;
			} else if (deployAgentStatus.isNotSuccess()) {
				hasFailed = true;
			} else if (!deployAgentStatus.isCompleted()) {
				hasNoCompleted = true;
			}
		}
		if (hasNoCompleted) {
			//有未完成的孩子状态，则Task保持现有状态
			return this;
		} else if (hasSuccess && hasFailed) {
			return DeployVsStatus.WARNING;
		} else if (hasSuccess) {
			return DeployVsStatus.SUCCESS;
		} else {
			return DeployVsStatus.FAILED;
		}

	}

	public String getDesc() {
		return desc;
	}

}
