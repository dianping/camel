package com.dianping.platform.slb.agent.core.script;

import com.dianping.platform.slb.agent.core.constant.MessageID;
import com.dianping.platform.slb.agent.core.exception.BizException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URL;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Service
public class ScriptFileManager {

	private static final String SCRIPT_FOLDER = "script/";

	private static final String TENGINE_RELOAD = "tengine_reload.sh";

	private static final String SCRIPT_LIST_VS = "ls -l /usr/local/nginx/conf/phoenix-slb/";

	private static final String SCRIPT_DEL_VS = "rm -rf /usr/local/nginx/conf/phoenix-slb/%s/";

	private File getScriptFile(String fileName) {
		URL url = this.getClass().getClassLoader().getResource(SCRIPT_FOLDER + fileName);

		if (url == null) {
			throw new BizException(MessageID.FILE_NOT_EXIST, SCRIPT_FOLDER + fileName);
		}
		return new File(url.getPath());
	}

	public String getTengineReloadPath() {
		return getScriptFile(TENGINE_RELOAD).getAbsolutePath();
	}

	public String getListVsScript() {
		return SCRIPT_LIST_VS;
	}

	public String getDelVsScript(String vs) {
		return String.format(SCRIPT_DEL_VS, vs);
	}

}
