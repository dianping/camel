package com.dianping.phoenix.lb.api.dengine;

import com.dianping.phoenix.lb.dengine.DengineException;
import com.dianping.phoenix.lb.exception.BizException;

import java.io.IOException;

/**
 * 针对多个dengine进行的操作入口
 *
 * @author mengwenchao
 *         <p/>
 *         2014年11月11日 下午3:13:13
 */
public interface DengineManager {

	/**
	 * 对于pool影响到的所有dengine，进行强制降级、升级、自动操作
	 *
	 * @param pool
	 * @throws BizException
	 * @throws IOException
	 * @throws DengineException
	 */
	void forceState(String pool, ForceState state) throws BizException, DengineException;
}
