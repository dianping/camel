package com.dianping.phoenix.lb.service.model;

import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.StatusCode;

import java.util.List;

/*-
 * @author liyang
 *
 * 2015年4月8日 下午4:13:08
 */
public interface StatusCodeService {

	void addIfNull(StatusCode statusCode) throws BizException;

	List<StatusCode> listStatusCodes() throws BizException;

}
