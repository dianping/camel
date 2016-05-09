package com.dianping.phoenix.lb.utils;

import com.dianping.phoenix.lb.model.entity.Pool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public class PoolNameUtils {

	public static String POOL_DEGRADE_SUFFIX = "#BACKUP";

	public static Pattern proxy_pass_pattern = Pattern.compile("proxy_pass\\s+(\\w+)://([^$\\s]+?)\\s*(;|$|/)");

	public static Pattern dp_domain_pattern = Pattern.compile("dp_domain(\\s+)([^\\s$]+?)\\s*(;|$)");

	public static String getPoolNamePrefix(String poolName) {
		int prefixPos = poolName.indexOf("@");
		if (prefixPos >= 0) {
			return poolName.substring(0, prefixPos);
		} else {
			return poolName;
		}
	}

	public static String rewriteToPoolNamePrefix(String vsName, String poolName) {
		return vsName + "." + poolName;
	}

	public static String rewriteToPoolNameDegradePrefix(String vsName, String poolName) {
		return vsName + "." + poolName + POOL_DEGRADE_SUFFIX;
	}

	public static String extractPoolNameFromProxyPassString(String text) {

		List<String> poolNames = new ArrayList<String>();

		findPoolNamesWithPattern(proxy_pass_pattern, text, poolNames);
		findPoolNamesWithPattern(dp_domain_pattern, text, poolNames);

		if (poolNames.size() == 0) {
			return "";
		}
		if (poolNames.size() >= 2) {
			throw new IllegalStateException("more than one proxy_pass in " + text);
		}
		return poolNames.get(0);
	}

	private static void findPoolNamesWithPattern(Pattern pattern, String text, List<String> poolNames) {

		Matcher matcher = pattern.matcher(text);
		while (matcher.find()) {
			poolNames.add(matcher.group(2));
		}
	}

	public static String replacePoolNameFromProxyPassString(String vsName, String text, Map<String, Pool> pools) {

		String result = replaceWithPattern(vsName, text, pools, proxy_pass_pattern);
		result = replaceWithPattern(vsName, result, pools, dp_domain_pattern);
		return result;

	}

	private static String replaceWithPattern(String vsName, String text, Map<String, Pool> pools, Pattern pattern) {
		Matcher matcher = pattern.matcher(text);
		StringBuilder result = new StringBuilder();
		int lastEnd = 0;

		while (matcher.find()) {
			int start = matcher.start();

			String matchText = matcher.group();
			String pool = matcher.group(2);

			if (pools.get(pool) != null) {
				matchText = matchText.replace(pool, rewriteToPoolNamePrefix(vsName, pool));
			}

			result.append(text.subSequence(lastEnd, start)).append(matchText);
			lastEnd = matcher.end();
		}

		result.append(text.substring(lastEnd));
		return result.toString();
	}

}
