package com.dianping.phoenix.lb.service.model;

import com.dianping.phoenix.lb.dao.NginxLogDao;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.service.ConcurrentControlServiceTemplate;
import com.dianping.phoenix.slb.nginx.log.entity.NginxLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * @author liyang
 *         <p/>
 *         2015年4月7日 下午9:49:14
 */
@Service
public class NginxLogServiceImpl extends ConcurrentControlServiceTemplate implements NginxLogService {

	@Autowired
	private NginxLogDao m_nginxLogDao;

	@Override
	public void addNginxLogs(final List<NginxLog> logs) throws BizException {
		write(new WriteOperation<Void>() {
			@Override
			public Void doWrite() throws Exception {
				m_nginxLogDao.addNginxLogs(logs);
				return null;
			}
		});
	}

	@Override
	public List<NginxLog> findNginxLogs(final Date startTime, final Date endTime) throws BizException {
		return read(new ReadOperation<List<NginxLog>>() {
			@Override
			public List<NginxLog> doRead() throws Exception {
				return m_nginxLogDao.findNginxLogs(startTime, endTime);
			}
		});
	}

	public void setNginxLogDao(NginxLogDao nginxLogDao) {
		m_nginxLogDao = nginxLogDao;
	}

}
