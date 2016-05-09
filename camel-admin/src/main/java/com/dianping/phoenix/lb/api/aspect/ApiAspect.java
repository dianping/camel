package com.dianping.phoenix.lb.api.aspect;

import java.util.Map;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface ApiAspect {

	void transaction(String type, String name, ApiWrapper wrapper);

	void doTransactionWithResultMap(String type, String name, ApiWrapper wrapper, Map<String, Object> dataMap);

}
