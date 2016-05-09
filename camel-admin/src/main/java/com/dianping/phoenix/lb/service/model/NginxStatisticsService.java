package com.dianping.phoenix.lb.service.model;

import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.slb.nginx.statistics.hour.entity.NginxHourStatistics;

import java.util.Date;
import java.util.List;

/*-
 * @author liyang
 *
 * 2015年3月31日 下午4:17:52
 */
public interface NginxStatisticsService {

	List<NginxHourStatistics> findHourlyStatistics(Date startHour, Date endHour);

	List<NginxHourStatistics> findHourlyStatistics(String poolName, Date startHour, Date endHour);

	void addOrUpdateHourlyStatistics(NginxHourStatistics hourStatistics) throws BizException;

}
