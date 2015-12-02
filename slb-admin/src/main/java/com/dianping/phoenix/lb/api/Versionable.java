package com.dianping.phoenix.lb.api;

/**
 * 版本控制，防止并发修改
 *
 * @author mengwenchao
 *         <p/>
 *         2014年11月13日 下午3:27:06
 */
public interface Versionable {

	Object setVersion(int version);

	int getVersion();
}
