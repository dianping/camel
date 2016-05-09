package com.dianping.phoenix.lb.api.dengine;

import org.apache.commons.lang.Validate;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public enum ForceState {

	UP(1), DOWN(-1), AUTO(0);

	private int forceState;

	ForceState(int forceState) {
		this.forceState = forceState;
	}

	public static ForceState get(String state) {

		Validate.notNull(state);
		state = state.trim();
		if (state.equalsIgnoreCase("up")) {
			return UP;
		}
		if (state.equalsIgnoreCase("down")) {
			return DOWN;
		}

		return AUTO;
	}

	public int getIntForceState() {
		return forceState;
	}
}
