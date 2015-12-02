package com.dianping.phoenix.lb.monitor;

import com.dianping.phoenix.AbstractTest;
import com.dianping.phoenix.lb.monitor.ReqStatusContainer.DataWrapper;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Executors;

public class ReqStatusDengineTest extends AbstractTest {

	private int keepInMemoryHours = 1;

	private int interval = 5000;

	private ReqStatusDengine rsd = new ReqStatusDengine(Executors.newScheduledThreadPool(3), "127.0.0.1",
			"80/reqstatus", keepInMemoryHours, interval);

	@Test
	public void testGetView() {

		long current = System.currentTimeMillis();
		rsd.setLastDataTime(current);
		int count = 1002;
		for (int i = 0; i < count; i++) {
			ReqStatus rs = new ReqStatus("kv");
			rs.setReqTotal(i * 5);
			rsd.offer(rs);
		}

		DataWrapper dw = rsd.getViewData(2, 3 * 60, current);
		Long[] data = dw.getData();
		Assert.assertEquals(data.length, 3);

		int i = -1;
		for (Long qps : data) {

			i++;
			if (i == 0) {
				Assert.assertEquals(qps.longValue(), 0L);
				continue;
			}
			Assert.assertEquals(qps.longValue(), 1L);
		}
	}

	@Test
	public void testClean() {

		int keepCount = keepInMemoryHours * 3600 / (interval / 1000);
		for (int i = 0; i < keepCount * 2; i++) {
			ReqStatus rs = new ReqStatus("kv");
			rs.setReqTotal(i * 5);
			rsd.offer(rs);
			rsd.clean();
			Assert.assertEquals(Math.min(keepCount, i + 1), rsd.count());
		}

	}

}
