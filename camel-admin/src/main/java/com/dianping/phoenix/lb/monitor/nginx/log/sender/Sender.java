package com.dianping.phoenix.lb.monitor.nginx.log.sender;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface Sender {

	boolean send(AlertMessageEntity message);



}
