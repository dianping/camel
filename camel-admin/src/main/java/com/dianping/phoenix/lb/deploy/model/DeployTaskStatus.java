package com.dianping.phoenix.lb.deploy.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * vs只记录结果，当作result来使用
 *
 * @author atell
 */
public enum DeployTaskStatus {

	CREATED("新建的任务"), //创建完，选择了vs，未选择agent

	//    WAITING("已就绪，未执行"), //创建完，选择了vs和选择agent，可以被点击启动执行

	READY("可执行"), //已准备好，可被执行

	//    PROCESSING("正在执行"), //内存状态，不需要持久化

	//    PAUSED("已暂停"),

	WARNING("执行完成(部分失败)"), // completed with partial failures

	FAILED("执行失败"), // complete with all failed

	CANCELLED("已被取消"),

	SUCCESS("执行成功"); // completed with all successful

	private final static Set<DeployTaskStatus> COMPLETED_STATUS_SET = new HashSet<DeployTaskStatus>();
	private final static Set<DeployTaskStatus> ERROR_STATUS_SET = new HashSet<DeployTaskStatus>();

	//    public static DeployStatus getById(int id, DeployStatus defaultStatus) {
	//        for (DeployStatus status : DeployStatus.values()) {
	//            if (status.getId() == id) {
	//                return status;
	//            }
	//        }
	//
	//        return defaultStatus;
	//    }

	//    public static DeployStatus getByName(String name, DeployStatus defaultStatus) {
	//        for (DeployStatus status : DeployStatus.values()) {
	//            if (status.getName().equals(name)) {
	//                return status;
	//            }
	//        }
	//
	//        return defaultStatus;
	//    }

	static {
		COMPLETED_STATUS_SET.add(WARNING);
		COMPLETED_STATUS_SET.add(FAILED);
		COMPLETED_STATUS_SET.add(SUCCESS);
		COMPLETED_STATUS_SET.add(CANCELLED);
	}

	static {
		ERROR_STATUS_SET.add(WARNING);
		ERROR_STATUS_SET.add(FAILED);
	}

	;

	private String desc;

	private DeployTaskStatus(String desc) {
		this.desc = desc;
	}

	;

	public boolean isCompleted() {
		return COMPLETED_STATUS_SET.contains(this);
	}

	public boolean isNotSuccess() {
		return ERROR_STATUS_SET.contains(this);
	}

	//    public boolean canPaused() {
	//        return this == READY || this == PROCESSING;
	//    }
	//
	//    public boolean canCancel() {
	//        return this != SUCCESS;//非成功的任务，均可取消
	//    }

	/**
	 * 根据子任务(vs任务)的状态，计算父亲的状态
	 */
	public DeployTaskStatus calculate(List<DeployVsStatus> deployVsStatusList) {
		boolean hasFailed = false;
		boolean hasSuccess = false;
		boolean hasNoCompleted = false;
		for (DeployVsStatus deployVsStatus : deployVsStatusList) {
			if (deployVsStatus == DeployVsStatus.SUCCESS) {
				hasSuccess = true;
			} else if (deployVsStatus.isNotSuccess()) {
				hasFailed = true;
			} else if (!deployVsStatus.isCompleted()) {
				hasNoCompleted = true;
			}
		}
		if (hasNoCompleted) {
			//有未完成的状态
			return this;
		} else if (hasSuccess && hasFailed) {
			return DeployTaskStatus.WARNING;
		} else if (hasSuccess) {
			return DeployTaskStatus.SUCCESS;
		} else {
			return DeployTaskStatus.FAILED;
		}

	}

	/**
	 * 根据子任务(agent任务)的状态，计算父亲的状态
	 */
	public DeployTaskStatus calculateForAgent(List<DeployAgentStatus> deployAgentStatusList) {
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
			//有未完成的孩子状态
			return DeployTaskStatus.WARNING;
		} else if (hasSuccess && hasFailed) {
			return DeployTaskStatus.WARNING;
		} else if (hasSuccess) {
			return DeployTaskStatus.SUCCESS;
		} else {
			return DeployTaskStatus.FAILED;
		}

	}

	public String getDesc() {
		return desc;
	}

	public boolean canChangeTo(DeployTaskStatus taskStatus) {
		// TODO Auto-generated method stub
		return false;
	}

}
