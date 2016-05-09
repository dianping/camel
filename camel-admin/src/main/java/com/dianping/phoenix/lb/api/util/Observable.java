package com.dianping.phoenix.lb.api.util;

/**
 * 观察者模式，数据变化一方实现
 *
 * @author mengwenchao
 *         <p/>
 *         2014年7月13日 下午2:56:28
 */
public interface Observable {

	void addObserver(Observer observer);

	boolean removeObserver(Observer observer);

	void notifyObservers(Object args);
}
