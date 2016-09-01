package com.dianping.platform.slb.agent.transaction.manager;

import com.dianping.platform.slb.agent.transaction.Transaction;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Service
public class DefaultTransactionManager implements TransactionManager {

	private static final String DEFAULT_TRANSACTION_ROOT = "/data/applogs/camel/transaction/";

	private static final String TRANSACTION_LOCK_FILE = "tx.lock";

	private static final String TRANSACTION_LOG_FILE = "tx.log";

	private static final String TRANSACTION_SERIALIZE_FILE = "tx.serialize";

	@PostConstruct
	private void buildTransactionDir() {
		File transactionRoot = new File(DEFAULT_TRANSACTION_ROOT);

		if (!transactionRoot.exists() || transactionRoot.isFile()) {
			transactionRoot.mkdirs();
		}
	}

	@Override
	public boolean startTransaction(long txId) throws IOException {
		File transactionDir = fetchTransactionBaseDir(txId);
		File transactionLock = new File(transactionDir, TRANSACTION_LOCK_FILE);

		FileUtils.forceMkdir(transactionLock.getParentFile());
		return transactionLock.createNewFile();
	}

	private File fetchTransactionBaseDir(long txId) {
		File transactionDir = new File(DEFAULT_TRANSACTION_ROOT, Long.toString(txId));

		if (!transactionDir.exists()) {
			transactionDir.mkdirs();
		}
		return transactionDir;
	}

	@Override
	public void saveTransaction(Transaction tx) throws IOException {
		File serializeFile = fetchSerializeFile(tx.getTransactionID());
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(serializeFile));

		objectOutputStream.writeObject(tx);
		objectOutputStream.close();
	}

	@Override
	public Transaction loadTransaction(long txId) throws IOException, ClassNotFoundException {
		File serializeFile = fetchSerializeFile(txId);
		ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(serializeFile));
		Transaction transaction = (Transaction) objectInputStream.readObject();

		objectInputStream.close();
		return transaction;
	}

	private File fetchSerializeFile(long txId) throws IOException {
		File transactionDir = fetchTransactionBaseDir(txId);
		File transactionSerializeFile = new File(transactionDir, TRANSACTION_SERIALIZE_FILE);

		if (!transactionSerializeFile.exists()) {
			FileUtils.forceMkdir(transactionSerializeFile.getParentFile());

			transactionSerializeFile.createNewFile();
		}
		return transactionSerializeFile;
	}

	@Override
	public boolean transactionExists(long txId) {
		File transactionDir = fetchTransactionBaseDir(txId);
		File transactionLock = new File(transactionDir, TRANSACTION_LOCK_FILE);

		return transactionLock.exists();
	}

	@Override
	public OutputStream getLogOutputStream(long txId) throws IOException {
		File transactionDir = fetchTransactionBaseDir(txId);
		File transactionLog = new File(transactionDir, TRANSACTION_LOG_FILE);

		if (!transactionLog.exists()) {
			FileUtils.forceMkdir(transactionLog.getParentFile());

			transactionLog.createNewFile();
		}
		return new FileOutputStream(transactionLog);
	}

	@Override
	public Reader getLogReader(long txId, int offset) throws IOException {
		File transactionDir = fetchTransactionBaseDir(txId);
		File transactionLog = new File(transactionDir, TRANSACTION_LOG_FILE);

		if (!transactionLog.exists()) {
			return null;
		}
		Reader reader = new FileReader(transactionLog);

		reader.skip(offset);
		return reader;
	}
}
