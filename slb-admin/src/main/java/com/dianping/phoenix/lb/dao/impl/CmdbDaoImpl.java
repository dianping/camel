package com.dianping.phoenix.lb.dao.impl;

import com.dianping.phoenix.lb.dao.CmdbDao;
import com.dianping.phoenix.lb.dao.ModelStore;
import com.dianping.phoenix.lb.dao.PoolDao;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.CmdbInfo;
import com.dianping.phoenix.lb.model.entity.Pool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Service
public class CmdbDaoImpl extends AbstractDao implements CmdbDao {

	/**
	 * @param store
	 */
	@Autowired(required = true)
	public CmdbDaoImpl(ModelStore store) {
		super(store);
	}

	@Override
	public CmdbInfo findByPoolName(String poolName) {
		return store.findCmdbInfoByPoolName(poolName);
	}

	@Override
	public void addOrUpdate(CmdbInfo cmdbInfo) {
		store.addOrUpdateCmdbInfo(cmdbInfo);
	}
}
