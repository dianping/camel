package com.dianping.phoenix.lb.deploy.service;

import com.dianping.phoenix.lb.action.Paginator;
import com.dianping.phoenix.lb.deploy.bo.DeployTaskBo;
import com.dianping.phoenix.lb.deploy.bo.NewTaskInfo;
import com.dianping.phoenix.lb.deploy.model.*;
import com.dianping.phoenix.lb.exception.BizException;

import java.util.List;

public interface DeployTaskService {

	/**
	 * 获取任务列表
	 *
	 * @param paginator
	 * @return
	 */
	List<DeployTask> list(Paginator paginator, int pageNum);

	/**
	 * 获取某个任务
	 *
	 * @throws BizException
	 */
	DeployTaskBo getTask(long taskId) throws BizException;

	void delTask(long taskId);

	/**
	 * 创建任务<br>
	 * 参数是：填写的task名称，所选择的vs名/tag名列表。
	 *
	 * @return
	 */
	long addTask(NewTaskInfo newTaskInfo);

	/**
	 * 点击开始任务后，ajax首先更新task。(更新完初始化页面的显示，开始论询显示各个DeployTask的状态)<br>
	 * (修改过一次后(即创建过deployment后)，不可再修改)
	 * <p/>
	 * 获取用户提交的： 每个vs下的ip列表，DeployPlan，然后更新Task。 <br>
	 * 更新deployTask。创建deployment。创建DeploymentDetail。<br>
	 */
	void updateTask(DeployTaskBo deployTaskBo);

	/**
	 * 更新Task状态（如果状态是Processing，则不更新，此状态不会持久化到数据库）
	 */
	void updateDeployTaskStatus(DeployTask deployTask);

	/**
	 * 更新Vs状态（如果状态是Processing，则不更新，此状态不会持久化到数据库）
	 */
	void updateDeployVsStatus(DeployVs deployVs);

	void updateDeployVsSummaryLog(DeployVs deployVs);

	/**
	 * 更新Agent状态和日志（如果状态是Processing，则不更新，此状态不会持久化到数据库）
	 */
	void updateDeployAgentStatusAndLog(DeployAgent deployAgent);

	/***
	 * 获取状态为ready的task
	 */
	List<DeployTaskBo> getTasksByStatus(DeployTaskStatus ready) throws BizException;

	/***
	 * 获取状态为ready的task
	 */
	List<DeployTaskBo> getTasksByStateAction(StateAction stateAction) throws BizException;

	void updateDeployTaskStateAction(DeployTask deployTask);

}
