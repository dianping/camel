package com.dianping.platform.slb.agent.core.transaction;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface TransactionManager {

	boolean startTransaction(Transaction transaction);

	boolean endTransaction(Transaction transaction);

}
