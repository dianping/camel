package com.dianping.platform.slb.agent.web.api;

import com.dianping.platform.slb.agent.web.model.Response;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface API {

	Object fetchLog(long deployId);

	Response deploy(long deployId, String vsName, String version, String config, boolean needReload,
			String dynamicRefreshPostDataStr, String dynamicVsPostData);

	Response fetchStatus(long deployId);

	Response fetchVersion(String vsName);

	Response listVsNames();

	Response delVsConfig(String vsName);

	Response reloadNginx();

}
