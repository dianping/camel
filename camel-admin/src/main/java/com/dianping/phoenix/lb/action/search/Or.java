package com.dianping.phoenix.lb.action.search;

import java.util.LinkedList;
import java.util.List;

/**
 * or
 *
 * @author mengwenchao
 *         <p/>
 *         2014年7月23日 下午8:31:01
 */
public class Or implements Matcher {

	private List<Matcher> matchers;

	public Or(Matcher... matchers) {

		this.matchers = new LinkedList<Matcher>();
		for (Matcher m : matchers) {
			this.matchers.add(m);
		}
	}

	public void addMatcher(Matcher m) {
		this.matchers.add(m);
	}

	@Override
	public boolean match(Object o) {

		if (matchers.size() == 0) {
			return true;
		}

		boolean result = false;

		for (Matcher m : matchers) {
			result = result || m.match(o);

			if (result) {
				break;
			}
		}

		return result;
	}

}
