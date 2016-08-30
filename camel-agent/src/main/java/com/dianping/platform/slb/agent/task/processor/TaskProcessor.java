package com.dianping.platform.slb.agent.task.processor;

import com.dianping.platform.slb.agent.task.Task;
import com.dianping.platform.slb.agent.task.model.SubmitResult;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface TaskProcessor extends Processor {

	SubmitResult submit(Task task) throws Exception;

}