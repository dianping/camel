package com.dianping.phoenix.lb.api.dengine;

import com.dianping.phoenix.lb.dengine.DengineException;

import java.io.IOException;
import java.util.List;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年11月11日 下午2:04:09
 */
public interface DengineDegradeApi extends DengineApi {

	/**
	 * 强制升级
	 *
	 * @param upstreams
	 * @throws IOException
	 * @throws DengineException
	 */
	void forceUp(List<String> upstreams) throws DengineException;

	/**
	 * 强制降级
	 *
	 * @param upstreams
	 */
	void forceDown(List<String> upstreams) throws DengineException;

	/**
	 * 强制自动升降级
	 *
	 * @param upstreams
	 */
	void forceAuto(List<String> upstreams) throws DengineException;

	/**
	 * 根据传入状态，做相应的动作
	 *
	 * @param upstreams
	 * @param state
	 * @throws IOException
	 */
	void force(List<String> upstreams, ForceState state) throws DengineException;
}
