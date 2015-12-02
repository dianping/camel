package com.dianping.phoenix.lb.dengine;

import com.dianping.phoenix.lb.api.dengine.DengineConfig;
import com.dianping.phoenix.lb.api.dengine.DengineDegradeApi;
import com.dianping.phoenix.lb.api.dengine.ForceState;
import com.dianping.phoenix.lb.monitor.HttpClientUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年11月11日 下午2:07:13
 */
public class DefaultDengineDegradeApi extends AbstractDengineApi implements DengineDegradeApi {

	private String forceUpUrl = "http://%s:6666/degrade/force/up?upstreams=%s";

	private String forceDownUrl = "http://%s:6666/degrade/force/down?upstreams=%s";

	private String forceAutoUrl = "http://%s:6666/degrade/force/auto?upstreams=%s";

	private String dengineIp;

	public DefaultDengineDegradeApi(String dengineIp) {

		this.dengineIp = dengineIp;
	}

	public DefaultDengineDegradeApi(String dengineIp, DengineConfig dengineConfig) {

		this.dengineIp = dengineIp;
		this.forceUpUrl = dengineConfig.getForceUpUrl();
		this.forceDownUrl = dengineConfig.getForceDownUrl();
		this.forceAutoUrl = dengineConfig.getForceAutoUrl();
	}

	@Override
	public void forceUp(List<String> upstreams) throws DengineException {

		if (logger.isInfoEnabled()) {
			logger.info("[forceUp]" + upstreams);
		}
		String url = String.format(forceUpUrl, dengineIp, StringUtils.join(upstreams, ","));
		callRemoteDengine(url);
	}

	private void callRemoteDengine(String url) throws DengineException {
		try {
			HttpClientUtil.getAsString(url, null, DEFAULT_ENCODING);
		} catch (Exception e) {
			throw new DengineException("error call dengine " + dengineIp + "," + e.getMessage(), e);
		}
	}

	@Override
	public void forceDown(List<String> upstreams) throws DengineException {

		if (logger.isInfoEnabled()) {
			logger.info("[forceDown]" + upstreams);
		}
		String url = String.format(forceDownUrl, dengineIp, StringUtils.join(upstreams, ","));
		callRemoteDengine(url);

	}

	@Override
	public void forceAuto(List<String> upstreams) throws DengineException {

		if (logger.isInfoEnabled()) {
			logger.info("[forceAuto]" + upstreams);
		}
		String url = String.format(forceAutoUrl, dengineIp, StringUtils.join(upstreams, ","));
		callRemoteDengine(url);
	}

	@Override
	public void force(List<String> upstreams, ForceState state) throws DengineException {

		switch (state) {
		case UP:
			forceUp(upstreams);
			break;
		case DOWN:
			forceDown(upstreams);
			break;
		case AUTO:
			forceAuto(upstreams);
			break;
		default:
			throw new IllegalStateException("should not be here");
		}
	}

}
