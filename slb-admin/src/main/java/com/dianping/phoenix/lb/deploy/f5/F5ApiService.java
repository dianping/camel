package com.dianping.phoenix.lb.deploy.f5;

import com.dianping.phoenix.lb.deploy.agent.AgentClientResult;

import java.io.IOException;

public interface F5ApiService {

	NodeStatus getNodeStatus(String nodeIp) throws IOException;

	void offline(String nodeIp, AgentClientResult result) throws IOException;

	void online(String nodeIp, AgentClientResult result) throws IOException;

}
