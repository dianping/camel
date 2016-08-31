package com.dianping.platform.slb.agent.task.workflow.engine;

import com.dianping.platform.slb.agent.task.workflow.step.Step;
import com.dianping.platform.slb.agent.transaction.Transaction;

import java.io.IOException;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface Engine {

	int executeStep(Step initStep, Transaction transaction) throws IOException;

	void kill();

}
