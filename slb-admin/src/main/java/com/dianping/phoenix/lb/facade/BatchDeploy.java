package com.dianping.phoenix.lb.facade;

import com.dianping.phoenix.lb.deploy.bo.api.DeployTaskApiBo;
import com.dianping.phoenix.lb.deploy.executor.TaskExecutor;
import com.dianping.phoenix.lb.exception.BizException;

import java.util.List;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年9月30日 下午2:10:21
 */
public interface BatchDeploy {

	TaskExecutor<DeployTaskApiBo> deployVs(String deployDesc, List<String> influencingVsList, String deployedBy)
			throws BizException;
}
