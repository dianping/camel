package com.dianping.phoenix.lb.dao;

import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.Aspect;

import java.util.List;

/**
 * @author Leo Liang
 *
 */
public interface CommonAspectDao {

	List<Aspect> list();

	Aspect find(String name);

	void save(List<Aspect> aspects) throws BizException;

}
