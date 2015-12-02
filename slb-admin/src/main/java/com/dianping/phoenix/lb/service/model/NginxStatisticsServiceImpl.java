package com.dianping.phoenix.lb.service.model;

import com.dianping.phoenix.lb.constant.MessageID;
import com.dianping.phoenix.lb.dao.NginxStatisticsDao;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.service.ConcurrentControlServiceTemplate;
import com.dianping.phoenix.lb.utils.ExceptionUtils;
import com.dianping.phoenix.slb.nginx.statistics.hour.entity.NginxHourStatistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/*-
 * @author liyang
 *
 * 2015年3月31日 下午4:54:04
 */
@Service
public class NginxStatisticsServiceImpl extends ConcurrentControlServiceTemplate implements NginxStatisticsService {

	private NginxStatisticsDao m_nginxStatisticsDao;

	@Autowired(required = true)
	public NginxStatisticsServiceImpl(NginxStatisticsDao nginxStatisticsDao) {
		super();
		this.m_nginxStatisticsDao = nginxStatisticsDao;
	}

	public void setNginxStatusDao(NginxStatisticsDao nginxStatusDao) {
		this.m_nginxStatisticsDao = nginxStatusDao;
	}

	@Override
	public List<NginxHourStatistics> findHourlyStatistics(final Date startHour, final Date endHour) {
		try {
			return read(new ReadOperation<List<NginxHourStatistics>>() {
				@Override
				public List<NginxHourStatistics> doRead() throws Exception {
					return m_nginxStatisticsDao.findHourlyStatistics(startHour, endHour);
				}
			});
		} catch (BizException e) {
			return null;
		}
	}

	@Override
	public List<NginxHourStatistics> findHourlyStatistics(final String poolName, final Date startHour,
			final Date endHour) {
		try {
			return read(new ReadOperation<List<NginxHourStatistics>>() {
				@Override
				public List<NginxHourStatistics> doRead() throws Exception {
					return m_nginxStatisticsDao.findHourlyStatistics(poolName, startHour, endHour);
				}
			});
		} catch (BizException e) {
			return null;
		}
	}

	@Override
	public void addOrUpdateHourlyStatistics(final NginxHourStatistics hourStatistics) throws BizException {
		if (hourStatistics.getHour() == null) {
			ExceptionUtils.throwBizException(MessageID.NGINX_STATUS_DATE_EMPTY);
		}

		write(new WriteOperation<Void>() {
			@Override
			public Void doWrite() throws BizException {
				m_nginxStatisticsDao.addOrUpdateHourlyStatistics(hourStatistics);
				return null;
			}
		});
	}

}
