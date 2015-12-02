package com.dianping.phoenix.lb.action.search;

import com.dianping.phoenix.lb.monitor.StatusContainer.ShowResult;

/**
 * 字符串中含有特定字符
 *
 * @author mengwenchao
 *         <p/>
 *         2014年7月22日 下午8:37:10
 */
public class StringInMatcher implements Matcher {

	private String matchString;

	public StringInMatcher(String matchString) {

		this.matchString = matchString;
	}

	@Override
	public boolean match(Object o) {

		if (o instanceof ShowResult) {
			return poolMatcher((ShowResult) o);
		}
		return false;
	}

	private boolean poolMatcher(ShowResult sr) {

		if (sr.getAddress().contains(matchString) || sr.getAvailableRate().contains(matchString) || sr.getStatus()
				.contains(matchString) || sr.getUpstream().contains(matchString) || sr.getDengine()
				.contains(matchString)) {
			return true;
		}
		return false;
	}

}
