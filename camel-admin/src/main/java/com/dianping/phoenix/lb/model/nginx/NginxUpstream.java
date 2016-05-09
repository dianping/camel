/**
 * Project: phoenix-load-balancer
 * <p/>
 * File Created at Oct 30, 2013
 */
package com.dianping.phoenix.lb.model.nginx;

import com.dianping.phoenix.lb.model.entity.Check;
import com.dianping.phoenix.lb.model.entity.Strategy;
import com.dianping.phoenix.lb.model.entity.UpstreamFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Leo Liang
 *
 */
public class NginxUpstream {
	private String name;

	private List<NginxUpstreamServer> servers = new ArrayList<NginxUpstreamServer>();

	private Strategy lbStrategy;

	private Check check;

	private UpstreamFilter upstreamFilter;

	private boolean used;

	private boolean needDegrade;

	private Integer degradeRate;

	private Integer degradeForceState;

	private int keepalive;

	private int keepaliveTimeout;

	public boolean isNeedDegrade() {
		return needDegrade;
	}

	public void setNeedDegrade(boolean needDegrade) {
		this.needDegrade = needDegrade;
	}

	public boolean isUsed() {
		return used;
	}

	public void setUsed(boolean used) {
		this.used = used;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *           the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the servers
	 */
	public List<NginxUpstreamServer> getServers() {
		return servers;
	}

	public void addServer(NginxUpstreamServer server) {
		this.servers.add(server);
	}

	/**
	 * @return the lbStrategy
	 */
	public Strategy getLbStrategy() {
		return lbStrategy;
	}

	/**
	 * @param lbStrategy
	 *           the lbStrategy to set
	 */
	public void setLbStrategy(Strategy lbStrategy) {
		this.lbStrategy = lbStrategy;
	}

	public Check getCheck() {
		return check;
	}

	public void setCheck(Check check) {
		this.check = check;
	}

	public Integer getDegradeRate() {
		return degradeRate;
	}

	public void setDegradeRate(Integer degradeRate) {
		this.degradeRate = degradeRate;
	}

	public UpstreamFilter getUpstreamFilter() {
		return upstreamFilter;
	}

	public void setUpstreamFilter(UpstreamFilter upstreamFilter) {
		this.upstreamFilter = upstreamFilter;
	}

	public Integer getDegradeForceState() {
		return degradeForceState;
	}

	public void setDegradeForceState(Integer degradeForceState) {
		this.degradeForceState = degradeForceState;
	}

	public int getKeepalive() {
		return keepalive;
	}

	public void setKeepalive(int keepalive) {
		this.keepalive = keepalive;
	}

	public int getKeepaliveTimeout() {
		return keepaliveTimeout;
	}

	public void setKeepaliveTimeout(int keepaliveTimeout) {
		this.keepaliveTimeout = keepaliveTimeout;
	}

}
