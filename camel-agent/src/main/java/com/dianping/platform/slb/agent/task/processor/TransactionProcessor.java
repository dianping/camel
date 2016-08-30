package com.dianping.platform.slb.agent.task.processor;

import com.dianping.platform.slb.agent.task.model.SubmitResult;
import com.dianping.platform.slb.agent.transaction.Transaction;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface TransactionProcessor extends Processor {

	SubmitResult submit(Transaction transaction) throws Exception;

	boolean isTransactionProcessing(long transactionId);

	boolean cancel(long txId);

}