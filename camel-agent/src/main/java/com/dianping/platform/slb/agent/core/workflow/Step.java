package com.dianping.platform.slb.agent.core.workflow;

import java.util.Map;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface Step {

	int CODE_OK = 0;

	int CODE_ERROR = -1;

	String HEADER_STEP = "Step";

	String HEADER_PROGRESS = "Progress";

	String HEADER_STATUS = "Status";

	String STATUS_SUCCESS = "successful";

	String STATUS_FAIL = "failed";

	int doStep(Context ctx) throws Exception;

	Step getNextStep(int exitCode);

	Map<String, String> getLogChunkHeader();

}
