package com.dianping.platform.slb.agent.core.transaction;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface TransactionManager {

	boolean startTransaction(Transaction transaction);

	boolean endTransaction(Transaction transaction);

	OutputStream getLogOut(Transaction transaction) throws FileNotFoundException, IOException;


}
