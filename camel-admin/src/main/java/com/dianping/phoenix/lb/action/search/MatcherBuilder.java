package com.dianping.phoenix.lb.action.search;

/**
 * build matcher
 *
 * @author mengwenchao
 *         <p/>
 *         2014年7月23日 下午5:46:40
 */
public interface MatcherBuilder {

	Matcher build(String matchString);
}
