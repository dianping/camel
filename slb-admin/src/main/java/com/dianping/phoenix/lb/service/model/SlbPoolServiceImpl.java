/**
 * Project: phoenix-load-balancer
 * <p/>
 * File Created at 2013-10-17
 */
package com.dianping.phoenix.lb.service.model;

import com.dianping.phoenix.lb.constant.MessageID;
import com.dianping.phoenix.lb.dao.SlbPoolDao;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.Instance;
import com.dianping.phoenix.lb.model.entity.SlbPool;
import com.dianping.phoenix.lb.service.ConcurrentControlServiceTemplate;
import com.dianping.phoenix.lb.utils.ElementAdded;
import com.dianping.phoenix.lb.utils.ElementModified;
import com.dianping.phoenix.lb.utils.ElementRemoved;
import com.dianping.phoenix.lb.utils.ExceptionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Leo Liang
 *
 */
@Service
public class SlbPoolServiceImpl extends ConcurrentControlServiceTemplate implements SlbPoolService {

	private static final Logger logger = LoggerFactory.getLogger(SlbPoolServiceImpl.class);
	private SlbPoolDao slbPoolDao;

	/**
	 * @param poolDao
	 */
	@Autowired(required = true)
	public SlbPoolServiceImpl(SlbPoolDao slbPoolDao) {
		super();
		this.slbPoolDao = slbPoolDao;
	}

	@Override
	public List<SlbPool> listSlbPools() {
		try {
			return read(new ReadOperation<List<SlbPool>>() {

				@Override
				public List<SlbPool> doRead() throws Exception {
					return slbPoolDao.list();
				}
			});
		} catch (BizException e) {
			logger.error("[listSlbPools]", e);
			return null;
		}
	}

	@Override
	public SlbPool findSlbPool(final String poolName) throws BizException {
		if (StringUtils.isBlank(poolName)) {
			ExceptionUtils.throwBizException(MessageID.SLBPOOL_NAME_EMPTY);
		}

		return read(new ReadOperation<SlbPool>() {

			@Override
			public SlbPool doRead() throws BizException {
				return slbPoolDao.find(poolName);
			}
		});
	}

	@Override
	public void addSlbPool(String poolName, final SlbPool pool) throws BizException {
		if (poolName == null || pool == null) {
			return;
		}

		if (!poolName.equals(pool.getName())) {
			return;
		}

		validate(pool);

		write(new WriteOperation<Void>() {

			@Override
			public Void doWrite() throws Exception {
				slbPoolDao.add(pool);
				return null;
			}
		});

		//通知观察者变化
		notifyObservers(new ElementAdded(pool));
	}

	@Override
	public void deleteSlbPool(final String poolName) throws BizException {
		if (StringUtils.isBlank(poolName)) {
			ExceptionUtils.throwBizException(MessageID.SLBPOOL_NAME_EMPTY);
		}

		try {
			write(new WriteOperation<Void>() {

				@Override
				public Void doWrite() throws Exception {
					slbPoolDao.delete(poolName);
					return null;
				}
			});

			notifyObservers(new ElementRemoved(poolName));
		} catch (BizException e) {
			// ignore
			logger.error("[deleteSlbPool]", e);
		}

	}

	@Override
	public void modifySlbPool(final String poolName, final SlbPool pool) throws BizException {
		if (poolName == null || pool == null) {
			return;
		}

		if (!poolName.equals(pool.getName())) {
			return;
		}

		validate(pool);

		SlbPool oldPool = findSlbPool(poolName);

		write(new WriteOperation<Void>() {

			@Override
			public Void doWrite() throws Exception {
				slbPoolDao.update(pool);
				return null;
			}
		});

		notifyObservers(new ElementModified(oldPool, pool));
	}

	private void validate(SlbPool pool) throws BizException {
		if (StringUtils.isBlank(pool.getName())) {
			ExceptionUtils.throwBizException(MessageID.SLBPOOL_NAME_EMPTY);
		}

		if (pool.getInstances().size() == 0) {
			ExceptionUtils.throwBizException(MessageID.SLBPOOL_NO_MEMBER, pool.getName());
		}

		for (Instance member : pool.getInstances()) {
			if (StringUtils.isBlank(member.getIp())) {
				ExceptionUtils.throwBizException(MessageID.SLBPOOL_MEMBER_NO_IP, pool.getName());
			}
		}

	}
}
