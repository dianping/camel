package com.dianping.platform.slb.agent.task.workflow.step.impl;

import com.dianping.platform.slb.agent.conf.ConfigureManager;
import com.dianping.platform.slb.agent.constant.Constants;
import com.dianping.platform.slb.agent.task.Task;
import com.dianping.platform.slb.agent.task.model.file.FileUpdateTask;
import com.dianping.platform.slb.agent.task.workflow.step.Step;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public abstract class FileUpdateStep implements Step {

	public static final FileUpdateStep SUCCESS = new FileUpdateStep(null, null, 5) {
		@Override
		public int doStep(Task task) throws Exception {
			return Step.CODE_SUCCESS;
		}

		@Override
		public String toString() {
			return "SUCCESS";
		}

		@Override
		public Map<String, String> getHeader() {
			Map<String, String> headers = super.getHeader();

			headers.put(HEADER_STATUS, STATUS_SUCCESS);
			return headers;
		}
	};

	public static final FileUpdateStep FAIL = new FileUpdateStep(null, null, 6) {
		@Override
		public int doStep(Task task) throws Exception {
			return Step.CODE_FAIL;
		}

		@Override
		public String toString() {
			return "FAIL";
		}

		@Override
		public Map<String, String> getHeader() {
			Map<String, String> headers = super.getHeader();

			headers.put(HEADER_STATUS, STATUS_FAIL);
			return headers;
		}
	};

	public static final FileUpdateStep ROLL_BACK_CONFIG = new FileUpdateStep(FAIL, FAIL, 5) {
		@Override
		public int doStep(Task task) throws Exception {
			FileUpdateTask fileUpdateTask = (FileUpdateTask) task;
			String[] fileNames = fileUpdateTask.getFileNames();
			int fileSize = fileNames.length;

			for (String vs : fileUpdateTask.getVirtualServerNames()) {
				for (String fileName : fileNames) {
					File vsDir = new File(ConfigureManager.getNginxConfDir(), vs);
					String backupFileName = generateBackupFileName(fileName);
					File backupFile = new File(vsDir, backupFileName);
					File newFile = new File(vsDir, fileName);

					if (backupFile.exists()) {
						if (!backupFile.renameTo(newFile)) {
							return Step.CODE_FAIL;
						}
					} else {
						if (newFile.exists() && !newFile.delete()) {
							return Step.CODE_FAIL;
						}
					}
				}
			}
			return Step.CODE_SUCCESS;
		}

		@Override
		public String toString() {
			return "ROLL_BACK_CONFIG";
		}
	};

	public static final FileUpdateStep PUT_NEW_CONFIG = new FileUpdateStep(SUCCESS, ROLL_BACK_CONFIG, 4) {
		@Override
		public int doStep(Task task) throws Exception {
			FileUpdateTask fileUpdateTask = (FileUpdateTask) task;
			String[] fileNames = fileUpdateTask.getFileNames();
			int fileSize = fileNames.length;

			for (String vsName : fileUpdateTask.getVirtualServerNames()) {
				String[] fileContents = fileUpdateTask.getFileContents().get(vsName);

				Validate.isTrue(fileSize == fileContents.length);
				for (int i = 0; i < fileSize; i++) {
					if (!putConfigFile(vsName, fileNames[i], fileContents[i])) {
						return Step.CODE_FAIL;
					}
				}
			}
			return Step.CODE_SUCCESS;
		}

		private boolean putConfigFile(String vsName, String fileName, String content) {
			if (StringUtils.isEmpty(content)) {
				return false;
			}

			File vsDir = new File(ConfigureManager.getNginxConfDir(), vsName);
			File config = new File(vsDir, fileName);

			try {
				FileUtils.writeStringToFile(config, content, Constants.CHAREST_UTF8);
				return true;
			} catch (IOException e) {
				return false;
			}
		}

		@Override
		public String toString() {
			return "PUT_NEW_CONFIG";
		}
	};

	public static final FileUpdateStep BACKUP_OLD_CONFIG = new FileUpdateStep(PUT_NEW_CONFIG, FAIL, 3) {
		@Override
		public int doStep(Task task) throws Exception {
			FileUpdateTask fileUpdateTask = (FileUpdateTask) task;

			for (String vsName : fileUpdateTask.getVirtualServerNames()) {
				for (String fileName : fileUpdateTask.getFileNames()) {
					if (!backupConfigFile(vsName, fileName)) {
						return Step.CODE_FAIL;
					}
				}
			}
			return Step.CODE_SUCCESS;
		}

		private boolean backupConfigFile(String vsName, String fileName) {
			File vsDir = new File(ConfigureManager.getNginxConfDir(), vsName);
			File config = new File(vsDir, fileName);

			if (config.exists() && config.isFile()) {
				return config.renameTo(new File(vsDir, generateBackupFileName(fileName)));
			}
			return true;
		}

		@Override
		public String toString() {
			return "BACKUP_OLD_CONFIG";
		}
	};

	public static final FileUpdateStep CHECK_ARGUMENT = new FileUpdateStep(BACKUP_OLD_CONFIG, FAIL, 2) {
		@Override
		public int doStep(Task task) throws Exception {
			FileUpdateTask fileUpdateTask = (FileUpdateTask) task;

			trimArrays(fileUpdateTask.getVirtualServerNames());
			trimArrays(fileUpdateTask.getFileNames());
			return Step.CODE_SUCCESS;
		}

		private void trimArrays(String[] array) {
			int arrayIndex = array.length;

			for (int i = 0; i < arrayIndex; i++) {
				String rawMetric = array[i];
				String trimMetric = rawMetric.trim();

				if (!rawMetric.equals(trimMetric)) {
					array[i] = trimMetric;
				}
			}
		}

		@Override
		public String toString() {
			return "CHECK_ARGUMENT";
		}
	};

	public static final FileUpdateStep INIT = new FileUpdateStep(CHECK_ARGUMENT, FAIL, 1) {
		@Override
		public int doStep(Task task) throws Exception {
			FileUpdateTask fileUpdateStep = (FileUpdateTask) task;

			for (String vsName : fileUpdateStep.getVirtualServerNames()) {
				File vsDir = new File(ConfigureManager.getNginxConfDir(), vsName);

				if (!vsDir.exists() || vsDir.isFile()) {
					if (!vsDir.mkdirs()) {
						return Step.CODE_FAIL;
					}
				}
			}
			return Step.CODE_SUCCESS;
		}

		@Override
		public String toString() {
			return "INIT";
		}
	};

	private FileUpdateStep m_nextSuccessStep;

	private FileUpdateStep m_nextFailStep;

	private int m_sequence;

	private FileUpdateStep(FileUpdateStep nextSuccessStep, FileUpdateStep nextFailStep, int sequence) {
		m_nextSuccessStep = nextSuccessStep;
		m_nextFailStep = nextFailStep;
		m_sequence = sequence;
	}

	@Override
	public Step getNextStep(int status) {
		if (status == CODE_SUCCESS) {
			return m_nextSuccessStep;
		} else {
			return m_nextFailStep;
		}
	}

	@Override
	public int getTotalSteps() {
		return 7;
	}

	@Override
	public Map<String, String> getHeader() {
		Map<String, String> header = new HashMap<String, String>();

		header.put(HEADER_STEP, toString());
		header.put(HEADER_PROGRESS, String.format("%s/%s", m_sequence, getTotalSteps()));
		return header;
	}

	private static String generateBackupFileName(String configFileName) {
		return "." + configFileName + "bak";
	}

}