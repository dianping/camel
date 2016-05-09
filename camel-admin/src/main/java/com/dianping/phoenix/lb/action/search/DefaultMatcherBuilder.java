package com.dianping.phoenix.lb.action.search;

import org.apache.commons.lang.StringUtils;

/**
 * Default build matcher
 *
 * @author mengwenchao
 *         <p/>
 *         2014年7月23日 下午5:46:40
 */
public class DefaultMatcherBuilder implements MatcherBuilder {

	public static final String AND = " and ";

	public static final String OR = " or ";

	/**
	 * 只支持 a and b and c
	 * 或者 a b c
	 */
	@Override
	public Matcher build(String matchString) {

		if (matchString.contains(AND)) {

			And result = new And();

			String[] ands = matchString.split("\\s+" + AND.trim() + "\\s+");
			for (String and : ands) {
				and = and.trim();
				if (!StringUtils.isEmpty(and)) {
					result.addMatcher(new StringInMatcher(and));
				}
			}
			return result;
		}

		Or result = new Or();
		matchString = matchString.replace(OR, " ");

		String[] ors = matchString.split("\\s+");
		for (String or : ors) {
			or = or.trim();
			if (!StringUtils.isEmpty(or)) {
				result.addMatcher(new StringInMatcher(or));
			}
		}
		return result;
	}

}
