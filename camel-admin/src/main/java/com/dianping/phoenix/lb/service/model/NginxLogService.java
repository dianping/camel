package com.dianping.phoenix.lb.service.model;

import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.slb.nginx.log.entity.NginxLog;

import java.util.Date;
import java.util.List;

/**
 * @author liyang
 *         <p/>
 *         2015年4月7日 下午9:49:33
 */
public interface NginxLogService {

	void addNginxLogs(List<NginxLog> logs) throws BizException;

	List<NginxLog> findNginxLogs(Date startTime, Date endTime) throws BizException;

}
