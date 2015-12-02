package com.dianping.phoenix.lb.utils;

import com.dianping.phoenix.lb.api.util.Observable;
import com.dianping.phoenix.lb.api.util.Observer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 抽象类，实现Observable接口
 *
 * @author mengwenchao
 *         <p/>
 *         2014年7月13日 下午3:01:47
 */
public abstract class AbstractObservable implements Observable {

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	private List<Observer> observers;

	public AbstractObservable() {

		observers = new ArrayList<Observer>();
	}

	@Override
	public synchronized void addObserver(Observer observer) {

		if (observer == null) {
			throw new IllegalArgumentException(" observer null!");
		}

		observers.add(observer);
	}

	@Override
	public synchronized boolean removeObserver(Observer observer) {

		return observers.remove(observer);

	}

	@Override
	public synchronized void notifyObservers(Object args) {

		for (Observer observer : observers) {

			observer.update(this, args);
		}
	}

}
