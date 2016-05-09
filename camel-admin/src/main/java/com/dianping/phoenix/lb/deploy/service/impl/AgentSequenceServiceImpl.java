package com.dianping.phoenix.lb.deploy.service.impl;

import com.dianping.phoenix.lb.deploy.dao.AgentIdSequenceMapper;
import com.dianping.phoenix.lb.deploy.model.AgentIdSequence;
import com.dianping.phoenix.lb.deploy.service.AgentSequenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class AgentSequenceServiceImpl implements AgentSequenceService {

	@Autowired
	private AgentIdSequenceMapper mapper;

	/**
	 * 获取agentId
	 */
	public long getAgentId() {
		AgentIdSequence record = new AgentIdSequence();
		record.setCreationDate(new Date());
		mapper.insert(record);
		return record.getAgentId();
	}

}
