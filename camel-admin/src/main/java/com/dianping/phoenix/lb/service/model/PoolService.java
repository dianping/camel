/**
 * Project: phoenix-load-balancer
 * <p/>
 * File Created at 2013-10-17
 */
package com.dianping.phoenix.lb.service.model;

import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.facade.PoolAvailableRateException;
import com.dianping.phoenix.lb.model.entity.Pool;
import com.dianping.phoenix.lb.service.model.PoolServiceImpl.MemberModifier;

import java.util.List;
import java.util.Set;

/**
 * @author Leo Liang
 *
 */
public interface PoolService {

	List<Pool> listPools();

	Set<String> listPoolNames();

	Pool findPool(String poolName) throws BizException;

	void addPool(String poolName, Pool pool) throws BizException, PoolAvailableRateException;

	void deletePool(String poolName) throws BizException;

	void modifyPool(String poolName, Pool pool, MemberModifier memberModifier)
			throws BizException, PoolAvailableRateException;
}
