package com.dianping.phoenix.lb.service.model;

import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.Record;

/*-
 * @author liyang
 *
 * 2015年5月7日 上午12:06:58
 */
public interface RecordService {

	void addRecord(Record record) throws BizException;

}
