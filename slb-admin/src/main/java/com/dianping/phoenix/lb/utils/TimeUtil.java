package com.dianping.phoenix.lb.utils;

import java.util.Calendar;
import java.util.Date;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public class TimeUtil {

	public final static long ONE_HOUR_MILLS = 60 * 60 * 1000L;

	public final static long ONE_MINUTE_MILLS = 60 * 1000L;

	public static int calIntervalMinutes(Date startDate, Date endDate) {
		return (int) ((endDate.getTime() - startDate.getTime()) / ONE_MINUTE_MILLS);
	}

	public static Date trimHour(Date date) {
		return trimHour(date, 0);
	}

	public static Date trimHour(Date date, int hourOffset) {
		Calendar cal = Calendar.getInstance();

		cal.setTime(date);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.add(Calendar.HOUR_OF_DAY, hourOffset);

		return cal.getTime();
	}

	public static Date getCurrentHour() {
		Calendar cal = Calendar.getInstance();

		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);

		return cal.getTime();
	}

	public static Date getFutureHour() {
		Calendar cal = Calendar.getInstance();

		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.add(Calendar.HOUR_OF_DAY, 1);

		return cal.getTime();
	}

	public static Date getLastHour() {
		Calendar cal = Calendar.getInstance();

		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.add(Calendar.HOUR_OF_DAY, -1);

		return cal.getTime();
	}

	public static int getMinuteInHour(Date time) {
		long mills = time.getTime();

		return getMinuteInHour(mills);
	}

	public static int getMinuteInHour(long mills) {
		return (int) (mills % ONE_HOUR_MILLS / ONE_MINUTE_MILLS);
	}

	public static int getMinute(long mills) {
		return (int) (mills / ONE_MINUTE_MILLS);
	}

	public static boolean isValid(long mills) {
		if (mills <= System.currentTimeMillis() + ONE_MINUTE_MILLS) {
			return true;
		}
		return false;
	}

}
