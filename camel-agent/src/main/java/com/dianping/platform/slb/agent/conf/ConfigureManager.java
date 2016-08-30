package com.dianping.platform.slb.agent.conf;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.URL;
import java.util.Iterator;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public class ConfigureManager {

	private static final String COMMAND_LIST_VS = "ls -l /usr/local/nginx/conf/phoenix-slb";

	private static final String COMMAND_DEL_VS_PATTERN = "rm -rf /usr/local/nginx/conf/phoenix-slb/%s";

	private static final String PATH_NGINX_CONF_DIR = "/usr/local/nginx/conf/phoenix-slb";

	public static final String PROPERTY_TRANSACTIONMANAGER = "transaction_manager";

	static {
		File scriptDir = getNginxReloadScriptFile().getParentFile();
		Iterator<File> scriptIter = FileUtils.iterateFiles(scriptDir, new String[] { "sh" }, true);

		while (scriptIter != null && scriptIter.hasNext()) {
			scriptIter.next().setExecutable(true, false);
		}
	}

	public static String getCommandListVs() {
		return COMMAND_LIST_VS;
	}

	public static String getCommandDelVsPattern() {
		return COMMAND_DEL_VS_PATTERN;
	}

	private static File getScriptFile(String scriptFileName) {
		URL url = ConfigureManager.class.getClassLoader().getResource("script/" + scriptFileName);

		if (url == null) {
			throw new IllegalArgumentException("script not exists: " + scriptFileName);
		}
		return new File(url.getPath());
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
