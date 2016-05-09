package com.dianping.phoenix.nginx;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 此处降级测试使用dyups更新upstream状态
 *
 * @author mengwenchao
 *         <p/>
 *         2014年11月20日 下午7:25:55
 */
public class DegradeForceWithDyupsTest extends DegradeTest {

	private int FORCE_DEGRADE_AUTO = 0;

	private int FORCE_DEGRADE_UP = 1;

	private int FORCE_DEGRADE_DOWN = -1;

	protected String getUpstreamContent(String upstreamName) throws IOException {

		String url = dyupsAddress + "/upstream/" + upstreamName;
		return callWithResult(url);
	}

	@Override
	protected void forceNormal(List<String> upstreams) throws IOException {

		for (String upstream : upstreams) {
			setUpstreamWithDegradeInformation(upstream, FORCE_DEGRADE_AUTO);
		}
		sleepSeconds(5);
	}

	private void setUpstreamWithDegradeInformation(String upstreamName, int forceDegrade) throws IOException {

		String upstream = getUpstreamContent(upstreamName);
		upstream = upstream.replace('\n', ';');
		upstream += " upstream_degrade_rate " + degradeRate + ";";
		upstream += " check type=tcp interval=100 default_down=false;";

		upstream += "upstream_degrade_force_state " + forceDegrade + ";";
		putUpstream(upstreamName, upstream);
	}

	@Override
	protected void forceNormal(String... upstreams) throws IOException {
		forceNormal(Arrays.asList(upstreams));
	}

	@Override
	protected void forceUp(List<String> upstreams) throws IOException {

		for (String upstream : upstreams) {
			setUpstreamWithDegradeInformation(upstream, FORCE_DEGRADE_UP);
		}
		sleepSeconds(5);
	}

	@Override
	protected void forceUp(String... upstreams) throws IOException {
		forceUp(Arrays.asList(upstreams));
	}

	@Override
	protected void forceDown(List<String> upstreams) throws IOException {

		for (String upstream : upstreams) {
			setUpstreamWithDegradeInformation(upstream, FORCE_DEGRADE_DOWN);
		}
		sleepSeconds(5);
	}

	@Override
	protected void forceDown(String... upstreams) throws IOException {
		forceDown(Arrays.asList(upstreams));
	}

	@Override
	public void testAutoDegrade() throws Exception {
		//dont test it here
	}

}
