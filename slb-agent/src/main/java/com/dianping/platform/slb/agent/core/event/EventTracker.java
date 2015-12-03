package com.dianping.platform.slb.agent.core.event;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface EventTracker {

	void onEvent(Event event);

	EventTracker DUMMY_TRACKER = new EventTracker() {
		@Override
		public void onEvent(Event event) {
			// do nothing
		}
	};

}
