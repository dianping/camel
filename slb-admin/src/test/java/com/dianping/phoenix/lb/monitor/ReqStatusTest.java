package com.dianping.phoenix.lb.monitor;

import com.dianping.phoenix.AbstractTest;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年7月8日 上午10:17:50
 */
public class ReqStatusTest extends AbstractTest {

	@Test
	public void testAdd() {

		ReqStatus data = new ReqStatus("1");
		data.setReqTotal(1);
		data.setReqTotalOthers(1);

		ReqStatus added = new ReqStatus("1");

		added.add(data);

		Assert.assertEquals(added.getReqTotal(), 1);
		Assert.assertEquals(added.getReqTotalOthers(), 1);
	}
}
