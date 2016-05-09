package com.dianping.phoenix.lb.dao.impl;

import com.dianping.phoenix.lb.dao.ModelStore;
import com.dianping.phoenix.lb.dao.VariableDao;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.Variable;
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
public class VariableDaoImpl extends AbstractDao implements VariableDao {

	@Autowired(required = true)
	public VariableDaoImpl(ModelStore store) {
		super(store);
	}

	@Override
	public List<Variable> list() {

		return store.listVariables();
	}

	@Override
	public Variable find(String key) throws BizException {

		return store.findVariable(key);
	}

	@Override
	public void save(List<Variable> variables) throws BizException {

		store.saveVariables(variables);
	}

}
