package com.dianping.phoenix.lb.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public class Md5sumUtil {
	private Md5sumUtil() {
	}

	//根据String生成SHA-1字符串
	public static String md5sum(String str) {
		try {
			return md5sum(str.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException uee) {
			return null;
		}
	}

	//根据bytes生成SHA-1字符串
	private static String md5sum(byte[] bytes) {
		String ret = null;
		try {
			MessageDigest md = MessageDigest.getInstance("md5");
			byte[] byteDigest = md.digest(bytes);
			ret = byteToString(byteDigest);
		} catch (NoSuchAlgorithmException nsae) {
			nsae.printStackTrace();
		}
		return ret;
	}

	//将bytes转化为String
	private static String byteToString(byte[] digest) {
		String tmpStr = "";
		StringBuffer strBuf = new StringBuffer(40);
		for (int i = 0; i < digest.length; i++) {
			tmpStr = (Integer.toHexString(digest[i] & 0xff));
			if (tmpStr.length() == 1) {
				strBuf.append("0" + tmpStr);
			} else {
				strBuf.append(tmpStr);
			}
		}
		return strBuf.toString();
	}
}
