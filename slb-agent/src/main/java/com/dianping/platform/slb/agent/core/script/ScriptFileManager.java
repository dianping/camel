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

	private File getScriptFile(String fileName) {
		URL url = this.getClass().getClassLoader().getResource(SCRIPT_FOLDER + fileName);

		if (url == null) {
			throw new BizException(MessageID.FILE_NOT_EXIST, SCRIPT_FOLDER + fileName);
		}
		return new File(url.getPath());
	}

	public String getTengineReloadShell() {
		return getScriptFile(TENGINE_RELOAD).getAbsolutePath();
	}

}
