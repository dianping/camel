package com.dianping.phoenix.lb.dao.impl;

import com.dianping.phoenix.lb.dao.CommonAspectDao;
import com.dianping.phoenix.lb.dao.ModelStore;
import com.dianping.phoenix.lb.dao.PoolDao;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.Aspect;
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
public class CommonAspectDaoImpl extends AbstractDao implements CommonAspectDao {

	/**
	 * @param store
	 */
	@Autowired(required = true)
	public CommonAspectDaoImpl(ModelStore store) {
		super(store);
	}

	@Override
	public List<Aspect> list() {
		return store.listCommonAspects();
	}

	@Override
	public Aspect find(String name) {
		return store.findCommonAspect(name);
	}

	@Override
	public void save(List<Aspect> aspects) throws BizException {
		store.saveCommonAspects(aspects);
	}
}
