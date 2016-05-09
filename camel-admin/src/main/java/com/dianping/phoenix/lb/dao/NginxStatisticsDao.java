package com.dianping.phoenix.lb.dao;

import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.slb.nginx.statistics.hour.entity.NginxHourStatistics;

import java.util.Date;
import java.util.List;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface NginxStatisticsDao {

	List<NginxHourStatistics> findHourlyStatistics(Date startHour, Date endHour) throws BizException;

	List<NginxHourStatistics> findHourlyStatistics(String poolName, Date startHour, Date endHour) throws BizException;

	void addOrUpdateHourlyStatistics(NginxHourStatistics status) throws BizException;

}
