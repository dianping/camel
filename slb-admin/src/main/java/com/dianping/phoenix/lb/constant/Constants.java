package com.dianping.phoenix.lb.constant;

import java.util.Arrays;

/**
 * @author Leo Liang
 *
 */
public class Constants {
	public static String DEPLOY_SUMMARY = "summary";

	public static String ENV_DEV = "dev";

	public static String ENV_PRODUCT = "product";

	public static String ENV_PPE = "ppe";

	public static String ENV_PAAS = "paas";

	public static String DIRECTIVE_DP_DOMAIN = "dp_domain";

	public static String DIRECTIVE_PROXY_PASS = "proxy_pass";

	public static String DIRECTIVE_PROXY_IFELSE = "ifelse";

	public static String DIRECTIVE_CUSTOM = "custom";

	public static String DIRECTIVE_PROXY_PASS_POOL_NAME = "pool-name";

	public static String DIRECTIVE_IF_ELSE_IF_STATEMENT = "if-statement";

	public static String DIRECTIVE_IF_ELSE_ELSE_STATEMENT = "else-statement";

	public static String DIRECTIVE_VALUE = "value";

	public static String LOCATION_MATCHTYPE_PREFIX = "prefix";

	public static String LOCATION_MATCHTYPE_REGEX = "regex";

	public static String LOCATION_MATCHTYPE_COMMON = "common";

	public static String LOCATION_MATCHTYPE_EXACT = "exact";

	public static Integer HTTPS_TYPE_SUPPORT_BOTH = 1;

	public static Integer HTTPS_TYPE_FORCE_HTTP = 2;

	public static Integer HTTPS_TYPE_FORCE_HTTPS = 3;

	public static String[] LOCATION_MATCHTYPES = new String[] { LOCATION_MATCHTYPE_PREFIX, LOCATION_MATCHTYPE_REGEX,
			LOCATION_MATCHTYPE_COMMON, LOCATION_MATCHTYPE_EXACT };

	public static String LOAD_BALANCE_STRATEGY_CONSISTENT = "consistent_hash";

	public static void main(String[] args) {
		System.out.println(Arrays.asList(Constants.LOCATION_MATCHTYPES));
	}
}
