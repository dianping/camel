package com.dianping.phoenix.lb.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author mengwenchao
 *         <p/>
 *         2015年1月7日 上午10:52:34
 */
public class DateUtils {

	private final static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public static String format(Date date) {

		if (date == null) {
			return "--";
		}
		return sdf.format(date);

	}

}
