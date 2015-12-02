package com.dianping.phoenix.lb.dao;

import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.Variable;

import java.util.List;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年9月3日 上午11:11:14
 */
public interface VariableDao {

	List<Variable> list();

	void save(List<Variable> variables) throws BizException;

	Variable find(String key) throws BizException;

}
