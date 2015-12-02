/**
 * Project: phoenix-load-balancer
 * <p/>
 * File Created at 2013-10-17
 */
package com.dianping.phoenix.lb.service.model;

import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.Aspect;

import java.util.List;

/**
 * @author Leo Liang
 *
 */
public interface CommonAspectService {
	List<Aspect> listCommonAspects();

	Aspect findCommonAspect(String name) throws BizException;

	void saveCommonAspect(List<Aspect> aspects) throws BizException;
}
