package com.dianping.phoenix.lb.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class UrlUtils {

	private static String encoding = "UTF-8";

	public static String encode(String data) {

		try {
			return URLEncoder.encode(data, encoding);
		} catch (UnsupportedEncodingException e) {
		}
		return data;
	}

}
