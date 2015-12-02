package com.dianping.phoenix.nginx;

import com.dianping.phoenix.lb.monitor.DegradeStatus;
import com.dianping.phoenix.lb.monitor.DegradeStatusContainer;
import com.dianping.phoenix.lb.monitor.HttpClientUtil;
import org.junit.Before;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 降级测试基类
 *
 * @author mengwenchao
 *         <p/>
 *         2014年9月12日 下午5:11:00
 */
public abstract class AbstractDegradeTest extends AbstractDyupsTest {

	protected String degradeStatusAddress;

	protected String simpleCheckUpstreamConfig = " server 127.0.0.1:1111;server 127.0.0.1:2222; check type=tcp interval=100;";

	protected String simpleUnCheckUpstreamConfig = " server 127.0.0.1:1111;server 127.0.0.1:2222;";

	@Before
	public void prepareAbstractDegradeTest() {

		degradeStatusAddress = "http://" + requestIp + ":6666/degrade/status/detail";

	}

	protected Map<String, DegradeStatus> getDegradeUpstreams() throws IOException {

		DegradeStatusContainer dsc = new DegradeStatusContainer();
		return dsc.getSingleDengine(degradeStatusAddress);
	}

	protected int getDegradeUpstreamsTotalCount() throws IOException {

		String buf = HttpClientUtil.getAsString(degradeStatusAddress, null, "UTF-8");
		String[] upstreams = buf.split("\n");

		return upstreams.length - 1;
	}

	protected Map<String, Integer> getDegradeUpstreamsCount() throws IOException {

		Map<String, Integer> result = new HashMap<String, Integer>();
		String buf = HttpClientUtil.getAsString(degradeStatusAddress, null, "UTF-8");
		String[] upstreams = buf.split("\n");

		for (int i = 1; i < upstreams.length; i++) {

			String usName = upstreams[i].split(",")[0];
			if (result.get(usName) == null) {
				result.put(usName, new Integer(0));
			}
			result.put(usName, result.get(usName) + 1);
		}

		return result;
	}

}
