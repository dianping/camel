package com.dianping.platform.slb.agent.core.workflow.deploy;

import com.dianping.platform.slb.agent.core.script.DefaultScriptExecutor;
import com.dianping.platform.slb.agent.core.script.ScriptExecutor;
import com.dianping.platform.slb.agent.core.script.ScriptFileManager;
import com.dianping.platform.slb.agent.core.workflow.Context;

public class DeployContext extends Context {

	private ScriptExecutor scriptExecutor;

	private DeployStepProvider stepProvider;

	public DeployContext(ScriptFileManager scriptFileManager) {
		scriptExecutor = new DefaultScriptExecutor();
		stepProvider = new DefaultDeployStepProvider(scriptFileManager);
	}

	public ScriptExecutor getScriptExecutor() {
		return scriptExecutor;
	}

	public DeployStepProvider getStepProvider() {
		return stepProvider;
	}

	@Override
	public void kill() {
		try {
			super.kill();
			scriptExecutor.kill();
		} catch (Exception e) {
			// ignore
		}
	}

}
