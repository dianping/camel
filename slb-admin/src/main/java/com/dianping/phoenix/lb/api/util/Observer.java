package com.dianping.phoenix.lb.api.util;

/**
 * 观察者，侦测数据变化
 *
 * @author mengwenchao
 *         <p/>
 *         2014年7月13日 下午2:57:50
 */
public interface Observer {

	void update(Observable o, Object args);
}
