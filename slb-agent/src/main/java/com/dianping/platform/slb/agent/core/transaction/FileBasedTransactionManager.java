package com.dianping.platform.slb.agent.core.transaction;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public class FileBasedTransactionManager implements TransactionManager {

	public static String TRANSACTION_BASE_DIR = "/data/appdatas/slb-agent/transaction/";

	public static String LOCK = ".lock";

	@Override
	public boolean startTransaction(Transaction transaction) {
		try {
			File lock = new File(getBaseDir(transaction), LOCK);

			if (lock.exists()) {
				return false;
			} else {
				lock.createNewFile();
				return true;
			}
		} catch (Exception ex) {
			return false;
		}
	}

	@Override
	public boolean endTransaction(Transaction transaction) {
		return false;
	}

	private File getBaseDir(Transaction transaction) throws IOException {
		File transactionDir = new File(TRANSACTION_BASE_DIR + transaction.getId());

		if (!transactionDir.exists() || transactionDir.isFile()) {
			FileUtils.forceMkdir(transactionDir);
		}
		return transactionDir;
	}

}
