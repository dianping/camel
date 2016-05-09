package com.dianping.phoenix.lb.deploy.service;

import com.dianping.phoenix.lb.action.Paginator;
import com.dianping.phoenix.lb.deploy.bo.api.DeployTaskApiBo;
import com.dianping.phoenix.lb.deploy.model.api.DeployAgentApi;
import com.dianping.phoenix.lb.deploy.model.api.DeployTaskApi;
import com.dianping.phoenix.lb.exception.BizException;

import java.util.List;

public interface DeployTaskApiService {

	/**
	 * 获取任务列表
	 *
	 * @param paginator
	 * @return
	 */
	List<DeployTaskApi> list(Paginator paginator, int pageNum);

	/**
	 * 获取某个任务
	 *
	 * @throws BizException
	 */
	DeployTaskApiBo getTask(long taskId) throws BizException;

	void delTask(long taskId) throws BizException;

	/**
	 * 创建任务<br>
	 */
	long addTask(DeployTaskApiBo deployTaskBo);

	/**
	 * 更新Task状态（如果状态是Processing，则不更新，此状态不会持久化到数据库）
	 */
	void updateDeployTaskStatusAndLog(DeployTaskApi deployTask);

	/**
	 * 更新Agent状态和日志（如果状态是Processing，则不更新，此状态不会持久化到数据库）
	 */
	void updateDeployAgentStatusAndLog(DeployAgentApi deployAgent);

	void updateDeployTaskStateAction(DeployTaskApi deployTask);

}
