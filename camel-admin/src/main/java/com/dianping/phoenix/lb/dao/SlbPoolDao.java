package com.dianping.phoenix.lb.dao;

import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.SlbPool;

import java.util.List;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface SlbPoolDao {

	List<SlbPool> list();

	SlbPool find(String poolName);

	void add(SlbPool pool) throws BizException;

	void delete(String poolName) throws BizException;

	void update(SlbPool pool) throws BizException;

}
