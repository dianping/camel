package com.dianping.platform.slb.agent.core.transaction;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface TransactionManager {

	boolean startTransaction(Transaction transaction) throws IOException;

	boolean endTransaction(Transaction transaction);

	Transaction loadTransaction(int transactionId) throws Exception;

	void saveTransaction(Transaction transaction) throws IOException;

	OutputStream getLogOut(Transaction transaction) throws IOException;

	Reader getReader(Transaction transaction, int offset) throws IOException;

}
