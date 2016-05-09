package com.dianping.platform.slb.agent.core.workflow.deploy;

import com.dianping.platform.slb.agent.core.workflow.AbstractStep;
import com.dianping.platform.slb.agent.core.workflow.Context;
import com.dianping.platform.slb.agent.core.workflow.Step;

import java.util.Map;

public class DeployStep extends AbstractStep {

	private static DeployStep FAILED = new DeployStep(null, null, 6) {
		@Override
		public int doStep(Context ctx) throws Exception {
			ctx.setExitStatus(Step.STATUS_FAIL);
			return Step.CODE_ERROR;
		}

		@Override
		public Map<String, String> getLogChunkHeader() {
			Map<String, String> header = super.getLogChunkHeader();
			header.put(HEADER_STATUS, STATUS_FAIL);
			return header;
		}

		@Override
		public String toString() {
			return "FAILED";
		}

	};

	private static DeployStep SUCCESS = new DeployStep(null, null, 6) {
		@Override
		public int doStep(Context ctx) throws Exception {
			ctx.setExitStatus(Step.STATUS_SUCCESS);
			return Step.CODE_OK;
		}

		@Override
		public Map<String, String> getLogChunkHeader() {
			Map<String, String> header = super.getLogChunkHeader();
			header.put(HEADER_STATUS, STATUS_SUCCESS);
			return header;
		}

		@Override
		public String toString() {
			return "SUCCESS";
		}
	};

	private static DeployStep ROLLBACK = new DeployStep(FAILED, FAILED, 5) {

		@Override
		protected int doActivity(Context ctx) throws Exception {
			return getStepProvider(ctx).rollback(ctx);
		}

		@Override
		public String toString() {
			return "ROLLBACK";
		}
	};

	private static DeployStep COMMIT = new DeployStep(SUCCESS, FAILED, 5) {

		@Override
		protected int doActivity(Context ctx) throws Exception {
			return getStepProvider(ctx).commit(ctx);
		}

		@Override
		public String toString() {
			return "COMMIT";
		}
	};

	private static DeployStep RELOAD_OR_DYNAMIC_REFRESH_CONFIG = new DeployStep(COMMIT, ROLLBACK, 4) {

		@Override
		protected int doActivity(Context ctx) throws Exception {
			return getStepProvider(ctx).reloadOrDynamicRefreshConfig(ctx);
		}

		@Override
		public String toString() {
			return "RELOAD_OR_DYNAMIC_REFRESH_CONFIG";
		}
	};

	private static DeployStep BACKUP_OLD_AND_PUT_NEW_CONFIG = new DeployStep(RELOAD_OR_DYNAMIC_REFRESH_CONFIG, ROLLBACK,
			3) {

		@Override
		protected int doActivity(Context ctx) throws Exception {
			return getStepProvider(ctx).backupOldAndPutNewConfig(ctx);
		}

		@Override
		public String toString() {
			return "BACKUP_OLD_AND_PUT_NEW_CONFIG";
		}
	};

	private static DeployStep CHECK_ARGUMENT = new DeployStep(BACKUP_OLD_AND_PUT_NEW_CONFIG, FAILED, 2) {

		@Override
		protected int doActivity(Context ctx) throws Exception {
			return getStepProvider(ctx).checkArgument(ctx);
		}

		@Override
		public String toString() {
			return "CHECK_ARGUMENT";
		}
	};

	private static DeployStep INIT = new DeployStep(CHECK_ARGUMENT, FAILED, 1) {

		@Override
		protected int doActivity(Context ctx) throws Exception {
			return getStepProvider(ctx).init(ctx);
		}

		@Override
		public String toString() {
			return "INIT";
		}
	};

	public static DeployStep START = new DeployStep(INIT, FAILED, 0) {
		@Override
		public int doStep(Context ctx) throws Exception {
			return doActivity(ctx);
		}

		@Override
		public String toString() {
			return "START";
		}
	};

	public DeployStep(DeployStep success, DeployStep fail, int index) {
		super(success, fail, index);
	}

	private static DeployStepProvider getStepProvider(Context ctx) {
		return ((DeployContext) ctx).getStepProvider();
	}

	@Override
	protected int getTotalStep() {
		return 6;
	}

	@Override
	protected int doActivity(Context ctx) throws Exception {
		return Step.CODE_OK;
	}
}
