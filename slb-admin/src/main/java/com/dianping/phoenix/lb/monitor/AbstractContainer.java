package com.dianping.phoenix.lb.monitor;

import com.dianping.phoenix.lb.model.entity.Instance;
import com.dianping.phoenix.lb.model.entity.SlbPool;
import com.dianping.phoenix.lb.service.model.SlbPoolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年9月5日 下午6:34:48
 */
public abstract class AbstractContainer {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	protected SlbPoolService slbPoolService;

	protected String encoding = "UTF-8";

	protected void doCheckAnWait(final Executor executor) {

		List<SlbPool> listSlbPools = slbPoolService.listSlbPools();
		if (listSlbPools == null || listSlbPools.size() <= 0) {
			return;
		}

		int count = 0;

		for (SlbPool slbPool : listSlbPools) {
			List<Instance> instances = slbPool.getInstances();
			if (instances != null) {
				count += instances.size();
			}
		}

		final CountDownLatch latch = new CountDownLatch(count);

		if (listSlbPools != null) {
			for (final SlbPool slbPool : listSlbPools) {
				List<Instance> instances = slbPool.getInstances();
				for (final Instance instance : instances) {
					executor.execute(new Runnable() {
						@Override
						public void run() {
							try {
								doDengineTask(slbPool, instance);
							} catch (Throwable e) {
								logger.error("Fetch status from ip[" + instance.getIp() + "] error:" + e.getMessage());
							} finally {
								latch.countDown();
							}
						}

					});
				}
			}
		}
		try {
			latch.await();
		} catch (InterruptedException e) {
		}
	}

	protected void doDengineTask(SlbPool slbPool, Instance instance) throws Exception {

	}

}
