package com.dianping.phoenix;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 单元测试，不在远程运行
 *
 * @author mengwenchao
 *         <p/>
 *         2014年8月11日 下午7:56:04
 */
public class SkipTest implements MethodRule {

	private static boolean isRun;

	static {
		isRun = isRun();
	}

	private static boolean isRun() {

		InetAddress address;
		try {
			address = InetAddress.getLocalHost();
			String ip = address.getHostAddress().toString();
			//本机运行
			if (ip.startsWith("10")) {
				return true;
			}
		} catch (UnknownHostException e) {
		}
		return true;
	}

	@Override
	public Statement apply(final Statement base, FrameworkMethod method, Object target) {

		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				if (isRun) {
					base.evaluate();
				}
			}
		};
	}

}
