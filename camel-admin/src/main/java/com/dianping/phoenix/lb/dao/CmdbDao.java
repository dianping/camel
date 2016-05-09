package com.dianping.phoenix.lb.dao;

import com.dianping.phoenix.lb.model.entity.CmdbInfo;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface CmdbDao {

	CmdbInfo findByPoolName(String poolName);

	void addOrUpdate(CmdbInfo cmdbInfo);

}