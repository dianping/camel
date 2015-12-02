/**
 * Project: phoenix-load-balancer
 * <p/>
 * File Created at 2013-10-17
 */
package com.dianping.phoenix.lb.service.model;

import com.dianping.phoenix.lb.api.util.Observable;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.SlbPool;

import java.util.List;

/**
 * @author Leo Liang
 *
 */
public interface SlbPoolService extends Observable {
	List<SlbPool> listSlbPools();

	SlbPool findSlbPool(String poolName) throws BizException;

	void addSlbPool(String poolName, SlbPool pool) throws BizException;

	void deleteSlbPool(String poolName) throws BizException;

	void modifySlbPool(String poolName, SlbPool pool) throws BizException;
}
