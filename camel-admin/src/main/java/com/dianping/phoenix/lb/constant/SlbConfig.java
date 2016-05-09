package com.dianping.phoenix.lb.constant;

import java.util.List;
import java.util.regex.Pattern;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface SlbConfig {

	public String getLogoutURL();

	public List<Pattern> getRecordURLPatterns();

	public List<Pattern> getNoRecordURLPatterns();

	public String getSLBServerName();

	public double getAddMemberValidateMinRate();

	public double getDelMemberValidateMinRate();

	public double getDefaultMemberValidateMinRate();

	public int getStatusCodeDashboardRowSize();

}
