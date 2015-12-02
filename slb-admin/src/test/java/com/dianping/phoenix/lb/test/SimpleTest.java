package com.dianping.phoenix.lb.test;

import com.dianping.phoenix.lb.api.dengine.ForceState;
import com.dianping.phoenix.lb.utils.JsonBinder;
import org.apache.commons.beanutils.BeanUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.reflect.InvocationTargetException;

@RunWith(JUnit4.class)
public class SimpleTest {

	public static void main(String[] argc) throws IllegalAccessException, InvocationTargetException {

		new SimpleTest().testBean();
	}

	@Test
	public void testBean() throws IllegalAccessException, InvocationTargetException {

		System.out.println(this);
		Bean ip1 = new Bean(), ip2 = new Bean();

		ip1.setIp("ip1");
		ip2.setIp(null);
		BeanUtils.copyProperties(ip1, ip2);
		System.out.println(ip1);
		System.out.println(ip2);

		ip1.setIp("ip1");
		ip2.setIp(null);
		org.springframework.beans.BeanUtils.copyProperties(ip2, ip1);
		System.out.println(ip1);
		System.out.println(ip2);

	}

	@Test
	public void testString() {
		System.out.println(this);

		String a = "abc", b = new String("abc");
		System.out.println(a == b);
		System.out.println(a.intern() == b.intern());

	}

	@Test
	public void testJson() {

		//		VirtualServer virtualServer = JsonBinder.getNonNullBinder().fromJson(vsJson, VirtualServer.class);
		JsonTest jsonTest = new JsonTest();
		jsonTest.forceState = ForceState.AUTO;
		jsonTest.name = "unit";

		String json = JsonBinder.getNonNullBinder().toJson(jsonTest);
		System.out.println(json);

		JsonTest jt = JsonBinder.getNonNullBinder().fromJson(json, JsonTest.class);
		System.out.println(jt);

	}

	public static class JsonTest {

		String name;

		ForceState forceState;

		@Override
		public String toString() {
			return name + "," + forceState;
		}

	}

	class Bean {

		private String ip;

		public String getIp() {
			return ip;
		}

		public void setIp(String ip) {
			this.ip = ip;
		}

		@Override
		public String toString() {
			return ip;
		}
	}

}
