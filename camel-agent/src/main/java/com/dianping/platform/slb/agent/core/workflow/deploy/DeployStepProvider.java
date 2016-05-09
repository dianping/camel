package com.dianping.platform.slb.agent.core.workflow.deploy;

import com.dianping.platform.slb.agent.core.workflow.Context;

public interface DeployStepProvider {

	int init(Context ctx) throws Exception;

	int checkArgument(Context ctx) throws Exception;

	int backupOldAndPutNewConfig(Context ctx) throws Exception;

	int reloadOrDynamicRefreshConfig(Context ctx) throws Exception;

	int commit(Context ctx) throws Exception;

	int rollback(Context ctx) throws Exception;
}
