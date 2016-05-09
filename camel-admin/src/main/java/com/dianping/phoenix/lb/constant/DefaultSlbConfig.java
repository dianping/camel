package com.dianping.phoenix.lb.constant;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 * <p/>
 * this class is used for fetching configs from zookeeper. simply hardcode default configs here to work.
 */
@Service
public class DefaultSlbConfig implements SlbConfig {

	private static final List<Pattern> RECORD_PATTERNS = Collections.unmodifiableList(
			Arrays.asList(Pattern.compile("save$"), Pattern.compile("remove$"), Pattern.compile("api")));

	private static final List<Pattern> NO_RECORD_PATTERNS = Collections
			.unmodifiableList(Arrays.asList(Pattern.compile("addLog$")));

	@Override
	public String getLogoutURL() {
		return "";
	}

	@Override
	public List<Pattern> getRecordURLPatterns() {
		return RECORD_PATTERNS;
	}

	@Override
	public List<Pattern> getNoRecordURLPatterns() {
		return NO_RECORD_PATTERNS;
	}

	@Override
	public String getSLBServerName() {
		return "";
	}

	@Override
	public double getAddMemberValidateMinRate() {
		return 0;
	}

	@Override
	public double getDelMemberValidateMinRate() {
		return 0.5;
	}

	@Override
	public double getDefaultMemberValidateMinRate() {
		return 0;
	}

	@Override
	public int getStatusCodeDashboardRowSize() {
		return 10;
	}

}
