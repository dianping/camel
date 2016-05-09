package com.dianping.phoenix.lb.api.dao;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年11月13日 上午9:29:19
 */
public interface AutoIncrementIdGenerator {

	void clear(String idKey);

	long getNextId(String idKey);
}
