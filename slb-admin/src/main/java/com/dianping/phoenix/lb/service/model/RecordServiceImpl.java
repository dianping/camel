package com.dianping.phoenix.lb.service.model;

import com.dianping.phoenix.lb.dao.RecordDao;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.Record;
import com.dianping.phoenix.lb.service.ConcurrentControlServiceTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/*-
 * @author liyang
 *
 * 2015年5月7日 上午12:08:04
 */
@Service
public class RecordServiceImpl extends ConcurrentControlServiceTemplate implements RecordService {

	@Autowired
	private RecordDao m_recordDao;

	@Override
	public void addRecord(final Record record) throws BizException {
		write(new WriteOperation<Void>() {
			@Override
			public Void doWrite() throws Exception {
				m_recordDao.addRecord(record);
				return null;
			}
		});
	}

	public void setRecordDao(RecordDao recordDao) {
		m_recordDao = recordDao;
	}

}
