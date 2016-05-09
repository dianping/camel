package com.dianping.phoenix.lb.dao;

import com.dianping.phoenix.lb.model.entity.StatusCode;

import java.util.List;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface StatusCodeDao {

	void addIfNullStatusCode(StatusCode statusCode);

	List<StatusCode> listStatusCodes();

	void removeStatusCode(StatusCode statusCode);

}
