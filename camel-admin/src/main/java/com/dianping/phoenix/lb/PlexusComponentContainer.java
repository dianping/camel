package com.dianping.phoenix.lb;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.unidal.lookup.ContainerLoader;

/**
 * @author Leo Liang
 *
 */
public enum PlexusComponentContainer {

	INSTANCE;

	private transient PlexusContainer container;

	private PlexusComponentContainer() {
		this.container = ContainerLoader.getDefaultContainer();
	}

	public <T> T lookup(Class<T> type) throws ComponentLookupException {
		return this.container.lookup(type);
	}

	public <T> T lookup(Class<T> type, String roleHint) throws ComponentLookupException {
		return this.container.lookup(type, roleHint);
	}

}
