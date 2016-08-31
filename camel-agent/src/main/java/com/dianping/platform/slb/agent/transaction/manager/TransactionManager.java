package com.dianping.platform.slb.agent.transaction.manager;

import com.dianping.platform.slb.agent.transaction.Transaction;

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

	boolean startTransaction(long txId) throws IOException;

	void saveTransaction(Transaction tx) throws IOException;

	Transaction loadTransaction(long txId) throws IOException, ClassNotFoundException;

	boolean transactionExists(long txId);

	OutputStream getLogOutputStream(long txId) throws IOException;

	Reader getLogReader(long txId, int offset) throws IOException;

}
