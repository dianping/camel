/**
 * Project: phoenix-load-balancer
 * <p/>
 * File Created at Oct 30, 2013
 */
package com.dianping.phoenix.lb.model.nginx;

import com.dianping.phoenix.lb.utils.DateUtils;
import com.dianping.phoenix.lb.utils.PoolNameUtils;

import java.util.*;

/**
 * @author Leo Liang
 *
 */
public class NginxConfig {
	private String name;

	private String tagTime;

	private NginxServer server;

	private Map<String, List<NginxUpstream>> upstreams = new HashMap<String, List<NginxUpstream>>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the servers
	 */
	public NginxServer getServer() {
		return server;
	}

	/**
	 * @param servers
	 *            the servers to set
	 */
	public void setServer(NginxServer server) {
		this.server = server;
	}

	/**
	 * @return the upstreams
	 */
	public List<NginxUpstream> getUpstreams() {
		List<NginxUpstream> upstreamList = new ArrayList<NginxUpstream>();
		for (List<NginxUpstream> ele : upstreams.values()) {
			upstreamList.addAll(ele);
		}
		return upstreamList;
	}

	public void addUpstream(NginxUpstream upstream) {
		String upstreamName = upstream.getName();
		if (upstreamName != null) {
			upstreamName = PoolNameUtils.getPoolNamePrefix(upstreamName);
			if (!this.upstreams.containsKey(upstreamName)) {
				this.upstreams.put(upstreamName, new ArrayList<NginxUpstream>());
			}
			this.upstreams.get(upstreamName).add(upstream);
		}
	}

	public List<NginxUpstream> getUpstream(String name) {
		return upstreams.get(PoolNameUtils.getPoolNamePrefix(name));
	}

	public String getTagTime() {
		return tagTime;
	}

	public void setTagTime(Date tagDate) {
		tagTime = DateUtils.format(tagDate);
	}

}
