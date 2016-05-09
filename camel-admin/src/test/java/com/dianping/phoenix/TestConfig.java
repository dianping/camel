package com.dianping.phoenix;

import java.io.IOException;
import java.util.Properties;

public class TestConfig {

	private static String file = "slb_test.properties";

	private static Properties properties;

	public static Properties getProperties() throws IOException {

		synchronized (file) {
			if (properties == null) {

				properties = new Properties();
				properties.load(TestConfig.class.getClassLoader().getResourceAsStream(file));
			}
		}

		return properties;
	}

	public static String getSlbAddress() {

		try {
			return getProperties().getProperty("slb.address");
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public static String getSlbPool() {

		try {
			return getProperties().getProperty("slb.test.pool");
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public static String getSlbTengineAddress() {

		try {
			return getProperties().getProperty("slb.tengine.address");
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public static String getDengineAddress() {
		try {
			return getProperties().getProperty("dengine.address");
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public static String getDengineAddressPort() {
		try {
			return getProperties().getProperty("dengine.address.port");
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public static String getLogLocalAddress() {
		try {
			return getProperties().getProperty("log.local.address");
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public static String getLogPath() {
		try {
			return getProperties().getProperty("log.path");
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public static String[] getLogTestPaths() {
		try {
			return getProperties().getProperty("log.test.path").split("\\|");
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
