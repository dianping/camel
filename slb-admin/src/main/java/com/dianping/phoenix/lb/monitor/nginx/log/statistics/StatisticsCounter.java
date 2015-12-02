package com.dianping.phoenix.lb.monitor.nginx.log.statistics;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface StatisticsCounter {

	String getCounterName();

	boolean executeHourlyTask();

}
