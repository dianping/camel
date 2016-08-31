package com.dianping.platform.slb.agent.task.workflow.engine;

import com.dianping.platform.slb.agent.task.Task;
import com.dianping.platform.slb.agent.task.workflow.step.Step;

import java.io.IOException;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface Engine {

	int executeStep(Step initStep, Task task) throws IOException;

	void kill();

}
