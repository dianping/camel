package com.dianping.phoenix.lb.dao;

import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.slb.nginx.log.entity.NginxLog;

import java.util.Date;
import java.util.List;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface NginxLogDao {

	void addNginxLogs(List<NginxLog> logs) throws BizException;

	List<NginxLog> findNginxLogs(Date startTime, Date endTime) throws BizException;

}
