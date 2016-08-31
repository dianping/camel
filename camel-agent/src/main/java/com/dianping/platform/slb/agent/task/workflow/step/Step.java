package com.dianping.platform.slb.agent.task.workflow.step;

import com.dianping.platform.slb.agent.transaction.Transaction;

import java.util.Map;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface Step {

	int CODE_SUCCESS = 0;

	int CODE_FAIL = 1;

	String HEADER_STEP = "Step";

	String HEADER_PROGRESS = "Progress";

	String HEADER_STATUS = "Status";

	String STATUS_SUCCESS = "successful";

	String STATUS_FAIL = "failed";

	int doStep(Transaction transaction) throws Exception;

	Step getNextStep(int status);

	int getTotalSteps();

	Map<String, String> getHeader();

}
