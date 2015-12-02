package com.dianping.phoenix.lb.model;

import com.dianping.phoenix.lb.model.entity.VirtualServer;

import java.util.List;

public class VirtualServerGroup {

	private String name;

	private List<VirtualServer> virtualServers;

	public VirtualServerGroup() {
		super();
	}

	public VirtualServerGroup(String name, List<VirtualServer> virtualServers) {
		super();
		this.name = name;
		this.virtualServers = virtualServers;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<VirtualServer> getVirtualServers() {
		return virtualServers;
	}

	public void setVirtualServers(List<VirtualServer> virtualServers) {
		this.virtualServers = virtualServers;
	}

	@Override
	public String toString() {
		return String.format("VirtualServerGroup [name=%s, virtualServers=%s]", name, virtualServers);
	}

}
