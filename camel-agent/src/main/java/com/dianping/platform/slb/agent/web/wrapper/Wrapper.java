package com.dianping.platform.slb.agent.web.wrapper;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface Wrapper<T> {

	T doAction() throws Exception;

}
