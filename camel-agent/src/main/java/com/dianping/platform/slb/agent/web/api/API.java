package com.dianping.platform.slb.agent.web.api;

import com.dianping.platform.slb.agent.web.model.Response;

import javax.servlet.http.HttpServletResponse;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface API {

	Response fetchLog(long deployId, Integer offset, Integer br, HttpServletResponse response);

	Response deploy(long deployId, String vsName, String version, String config, boolean needReload,
			String dynamicRefreshPostDataStr, String dynamicVsPostData);

	Response update(String vsName, String fileName, String vsPostData);

	Response fetchStatus(long deployId);

	Response fetchVersion(String vsName);

	Response listVsNames();

	Response delVsConfig(String vsName);

	Response reloadNginx();

	Response cancel(long deployId);

}
