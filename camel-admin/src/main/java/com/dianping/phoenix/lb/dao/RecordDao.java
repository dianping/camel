package com.dianping.phoenix.lb.dao;

import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.Record;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface RecordDao {

	void addRecord(Record record) throws BizException;

}
