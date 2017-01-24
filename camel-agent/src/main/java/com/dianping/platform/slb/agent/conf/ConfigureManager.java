package com.dianping.platform.slb.agent.conf;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
	* dianping.com @2015
	* slb - soft load balance
	* <p>
	* Created by leon.li(Li Yang)
	*/
public class ConfigureManager {

	private static final String COMMAND_LIST_VS = "ls -l /usr/local/nginx/conf/phoenix-slb";

	private static final String COMMAND_DEL_VS_PATTERN = "rm -rf /usr/local/nginx/conf/phoenix-slb/%s";

	private static final String PATH_NGINX_CONF_DIR = "/usr/local/nginx/conf/phoenix-slb";

	private static final String DIR_AGENT_SCRIPT = "/data/appdatas/camel/script/";

	private static final String STATUS_SCRIPT_READY = "/data/appdatas/camel/script/status";

	private static final Set<String> SET_SCRIPT_NAMES = new HashSet<String>() {
		{
			add("tengine.sh");
			add("tengine_func.sh");
			add("tengine_reload.sh");
			add("util.sh");
		}
	};

	static {
		boolean scriptReady = false;

		File statusFile = new File(STATUS_SCRIPT_READY);
		if (statusFile.exists()) {
			try {
				String statusContent = FileUtils.readFileToString(statusFile);
				scriptReady = Boolean.parseBoolean(statusContent);
			} catch (Exception e) {
			}
		}

		if (!scriptReady) {
			boolean initialStatus = true;
			RuntimeException ex = null;

			File scriptDir = new File(DIR_AGENT_SCRIPT);
			if (!scriptDir.exists() || scriptDir.isFile()) {
				scriptDir.mkdirs();
			}

			for (String scriptName : SET_SCRIPT_NAMES) {
				InputStream inputStream = ConfigureManager.class.getClassLoader().getResourceAsStream("script/" + scriptName);
				File targetFile = new File(scriptDir, scriptName);

				try {
					FileUtils.writeByteArrayToFile(targetFile, IOUtils.toByteArray(inputStream));
					targetFile.setExecutable(true, false);
				} catch (IOException e) {
					ex = new RuntimeException(e);
					initialStatus = false;
				}
			}

			if (!initialStatus) {
				throw ex;
			} else {
				try {
					FileUtils.writeByteArrayToFile(statusFile, "true".getBytes());
				} catch (IOException e) {
				}
			}
		}
	}

	public static String getCommandListVs() {
		return COMMAND_LIST_VS;
	}

	public static String getCommandDelVsPattern() {
		return COMMAND_DEL_VS_PATTERN;
	}

	private static File getScriptFile(String scriptFileName) {
		return new File(DIR_AGENT_SCRIPT + scriptFileName);
	}

	public static File getNginxReloadScriptFile() {
		return getScriptFile("tengine_reload.sh");
	}

	public static String getNginxConfDir() {
		return PATH_NGINX_CONF_DIR;
	}

	public static File getTengineScriptFile() {
		return getScriptFile("tengine.sh");
	}

}
