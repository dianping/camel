package com.dianping.phoenix.lb.monitor.cmdb;

import com.dianping.phoenix.lb.dao.PoolDao;
import com.dianping.phoenix.lb.model.entity.Pool;
import com.dianping.phoenix.lb.service.model.CmdbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 *
 * not available for out environment. cmdb is used to store project and server infos inner dianping.
 */
@Service
public class CmdbUpdater {

	private final Logger m_logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private PoolDao m_poolDao;

	@Autowired
	private CmdbService m_cmdbService;

	@Resource(name = "scheduledThreadPool")
	private ScheduledExecutorService m_executorService;

	// @PostConstruct
	public void init() {
		m_executorService.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				for (Pool pool : m_poolDao.list()) {
					String poolName = pool.getName();

					try {
						m_cmdbService.addOrUpdate(poolName);
					} catch (Exception ex) {
						m_logger.error("warning--update_cmdbInfo " + poolName, ex);

					}
				}
			}
		}, 1, 1, TimeUnit.HOURS);
	}

}
