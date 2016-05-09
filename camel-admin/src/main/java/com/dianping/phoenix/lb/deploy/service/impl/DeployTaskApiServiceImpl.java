package com.dianping.phoenix.lb.deploy.service.impl;

import com.dianping.phoenix.lb.PlexusComponentContainer;
import com.dianping.phoenix.lb.action.Paginator;
import com.dianping.phoenix.lb.configure.ConfigManager;
import com.dianping.phoenix.lb.deploy.bo.api.DeployAgentApiBo;
import com.dianping.phoenix.lb.deploy.bo.api.DeployTaskApiBo;
import com.dianping.phoenix.lb.deploy.dao.api.DeployAgentApiMapper;
import com.dianping.phoenix.lb.deploy.dao.api.DeployTaskApiMapper;
import com.dianping.phoenix.lb.deploy.model.DeployAgentStatus;
import com.dianping.phoenix.lb.deploy.model.DeployTaskStatus;
import com.dianping.phoenix.lb.deploy.model.api.DeployAgentApi;
import com.dianping.phoenix.lb.deploy.model.api.DeployAgentApiExample;
import com.dianping.phoenix.lb.deploy.model.api.DeployTaskApi;
import com.dianping.phoenix.lb.deploy.model.api.DeployTaskApiExample;
import com.dianping.phoenix.lb.deploy.service.DeployTaskApiService;
import com.dianping.phoenix.lb.exception.BizException;
import org.apache.ibatis.session.RowBounds;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class DeployTaskApiServiceImpl implements DeployTaskApiService {

	private static final int PAGE_SIZE = 15;

	@SuppressWarnings("unused")
	private ConfigManager configManager;

	@Autowired
	private DeployAgentApiMapper deployAgentMapper;

	@Autowired
	private DeployTaskApiMapper deployTaskMapper;

	@PostConstruct
	public void init() throws ComponentLookupException {
		configManager = PlexusComponentContainer.INSTANCE.lookup(ConfigManager.class);
	}

	@Override
	public List<DeployTaskApi> list(Paginator paginator, int pageNum) {
		if (pageNum <= 0) {
			pageNum = 1;
		}

		DeployTaskApiExample example = new DeployTaskApiExample();
		example.setOrderByClause("creation_date DESC");

		int count = deployTaskMapper.countByExample(example);

		paginator.setItemsPerPage(PAGE_SIZE);
		paginator.setItems(count);
		paginator.setPage(pageNum);

		if (pageNum > paginator.getLastPage()) {
			pageNum = paginator.getLastPage();
		}
		int offset = PAGE_SIZE * (pageNum - 1);
		int limit = PAGE_SIZE;
		RowBounds rowBounds = new RowBounds(offset, limit);

		return deployTaskMapper.selectByExampleWithRowbounds(example, rowBounds);
	}

	@Override
	public DeployTaskApiBo getTask(long taskId) throws BizException {
		DeployTaskApi deployTask = deployTaskMapper.selectByPrimaryKey(taskId);

		DeployTaskApiBo deployTaskBo = new DeployTaskApiBo();
		buildTaskBo(taskId, deployTaskBo);

		deployTaskBo.setTask(deployTask);

		return deployTaskBo;
	}

	@Override
	public void delTask(long taskId) {
		deployTaskMapper.deleteByPrimaryKey(taskId);
	}

	@Override
	public long addTask(DeployTaskApiBo deployTaskBo) {
		DeployTaskApi task = deployTaskBo.getTask();
		task.setLastModifiedDate(new Date());
		task.setStatus(DeployTaskStatus.CREATED);
		deployTaskMapper.insert(task);

		long taskId = task.getId();

		List<DeployAgentApiBo> agentBos = deployTaskBo.getDeployAgentBos();

		for (DeployAgentApiBo agentBo : agentBos) {
			DeployAgentApi agent = agentBo.getDeployAgent();
			agent.setDeployTaskId(taskId);
			agent.setLastModifiedDate(new Date());
			deployAgentMapper.insert(agent);
		}

		return taskId;
	}

	@Override
	public void updateDeployTaskStatusAndLog(DeployTaskApi deployTask) {
		DeployTaskApi deployTask0 = new DeployTaskApi();
		deployTask0.setId(deployTask.getId());
		deployTask0.setStatus(deployTask.getStatus());
		deployTask0.setSummaryLog(deployTask.getSummaryLog());
		deployTask0.setLastModifiedDate(new Date());

		deployTaskMapper.updateByPrimaryKeySelective(deployTask0);

	}

	@Override
	public void updateDeployAgentStatusAndLog(DeployAgentApi deployAgent) {
		if (deployAgent.getStatus() == DeployAgentStatus.PROCESSING) {
			return;
		}
		DeployAgentApi deployAgent0 = new DeployAgentApi();
		deployAgent0.setId(deployAgent.getId());
		deployAgent0.setStatus(deployAgent.getStatus());
		if (deployAgent.getRawLog() != null) {
			deployAgent0.setRawLog(deployAgent.getRawLog());
		}
		deployAgent0.setLastModifiedDate(new Date());

		deployAgentMapper.updateByPrimaryKeySelective(deployAgent0);

	}

	@Override
	public void updateDeployTaskStateAction(DeployTaskApi deployTask) {
		DeployTaskApi deployTask0 = new DeployTaskApi();
		deployTask0.setId(deployTask.getId());
		deployTask0.setStateAction(deployTask.getStateAction());
		deployTask0.setLastModifiedDate(new Date());

		deployTaskMapper.updateByPrimaryKeySelective(deployTask0);
	}

	private void buildTaskBo(long taskId, DeployTaskApiBo task) throws BizException {
		DeployAgentApiExample example = new DeployAgentApiExample();
		example.createCriteria().andDeployTaskIdEqualTo(taskId);
		List<DeployAgentApi> deployAgentList = deployAgentMapper.selectByExampleWithBLOBs(example);

		List<DeployAgentApiBo> deployAgentApiBos = new ArrayList<DeployAgentApiBo>();
		for (DeployAgentApi deployAgent : deployAgentList) {
			DeployAgentApiBo deployAgentBo = new DeployAgentApiBo();

			deployAgentBo.setDeployAgent(deployAgent);

			deployAgentApiBos.add(deployAgentBo);
		}
		task.setDeployAgentBos(deployAgentApiBos);
	}

}
