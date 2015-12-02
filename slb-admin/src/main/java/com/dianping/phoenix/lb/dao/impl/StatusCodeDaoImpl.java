package com.dianping.phoenix.lb.dao.impl;

import com.dianping.phoenix.lb.dao.ModelStore;
import com.dianping.phoenix.lb.dao.StatusCodeDao;
import com.dianping.phoenix.lb.model.entity.StatusCode;
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
public class StatusCodeDaoImpl extends AbstractDao implements StatusCodeDao {

	@Autowired(required = true)
	public StatusCodeDaoImpl(ModelStore store) {
		super(store);
	}

	@Override
	public void addIfNullStatusCode(StatusCode statusCode) {
		store.addIfNullStatusCode(statusCode);
	}

	@Override
	public List<StatusCode> listStatusCodes() {
		return store.listStatusCode();
	}

	@Override
	public void removeStatusCode(StatusCode statusCode) {
		store.removeStatusCode(statusCode);
	}

}
