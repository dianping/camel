package com.dianping.phoenix.lb.dao.impl;

import com.dianping.phoenix.lb.dao.ModelStore;
import com.dianping.phoenix.lb.dao.SlbPoolDao;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.SlbPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Leo Liang
 *
 */
@Service
public class SlbPoolDaoImpl extends AbstractDao implements SlbPoolDao {

	/**
	 * @param store
	 */
	@Autowired(required = true)
	public SlbPoolDaoImpl(ModelStore store) {
		super(store);
	}

	@Override
	public List<SlbPool> list() {
		return store.listSlbPools();
	}

	@Override
	public SlbPool find(String poolName) {
		return store.findSlbPool(poolName);
	}

	@Override
	public void add(SlbPool pool) throws BizException {
		store.updateOrCreateSlbPool(pool.getName(), pool);
	}

	@Override
	public void delete(String poolName) throws BizException {
		store.removeSlbPool(poolName);
	}

	@Override
	public void update(SlbPool pool) throws BizException {
		store.updateOrCreateSlbPool(pool.getName(), pool);
	}

}
