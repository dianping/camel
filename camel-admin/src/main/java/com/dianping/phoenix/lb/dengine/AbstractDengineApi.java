package com.dianping.phoenix.lb.dengine;

import com.dianping.phoenix.lb.api.dengine.DengineApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年11月11日 下午2:07:13
 */
public abstract class AbstractDengineApi implements DengineApi {

	protected static final String DEFAULT_ENCODING = "UTF-8";

	protected final Logger logger = LoggerFactory.getLogger(getClass());

}
