package com.dianping.phoenix.lb.service.model;

import com.dianping.phoenix.lb.dao.StatusCodeDao;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.StatusCode;
import com.dianping.phoenix.lb.service.ConcurrentControlServiceTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

/*-
 * @author liyang
 *
 * 2015年4月8日 下午4:18:49
 */
@Service
public class StatusCodeServiceImpl extends ConcurrentControlServiceTemplate implements StatusCodeService {

	@Autowired
	private StatusCodeDao m_statusCodeDao;

	private Set<StatusCode> m_statusCodes = new HashSet<StatusCode>();

	@PostConstruct
	public void addDefaultStatusCode() throws BizException {
		try {
			listStatusCodes();
			addIfNull(new StatusCode("5XX").setValue("5XX"));
			addIfNull(new StatusCode("4XX").setValue("4XX"));
			addIfNull(new StatusCode("502").setValue("502"));
		}catch(Exception ex){
			// ignore it.
		}
	}

	@Override
	public void addIfNull(final StatusCode statusCode) throws BizException {
		if (isStatusCodeValid(statusCode) && !m_statusCodes.contains(statusCode)) {
			write(new WriteOperation<Void>() {
				@Override
				public Void doWrite() throws Exception {
					m_statusCodeDao.addIfNullStatusCode(statusCode);
					return null;
				}
			});
			m_statusCodes.add(statusCode);
		}
	}

	@Override
	public List<StatusCode> listStatusCodes() throws BizException {
		return read(new ReadOperation<List<StatusCode>>() {
			@Override
			public List<StatusCode> doRead() throws Exception {
				if (m_statusCodes.size() == 0) {
					m_statusCodes = new HashSet<StatusCode>(m_statusCodeDao.listStatusCodes());
				}
				cleanUnvalidStatusCode(m_statusCodes);
				return new ArrayList<StatusCode>(m_statusCodes);
			}
		});
	}

	private boolean isStatusCodeValid(StatusCode statusCode) {
		if (statusCode != null && statusCode.getValue() != null) {
			return true;
		}
		return false;
	}

	private void cleanUnvalidStatusCode(Set<StatusCode> statusCodes) {
		Iterator<StatusCode> iterator = statusCodes.iterator();

		while (iterator.hasNext()) {
			StatusCode statusCode = iterator.next();

			if (!isStatusCodeValid(statusCode)) {
				m_statusCodeDao.removeStatusCode(statusCode);
				iterator.remove();
			}
		}
	}

}
