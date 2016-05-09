package com.dianping.platform.slb.agent.core.transaction;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Properties;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Service
public class FileBasedTransactionManager implements TransactionManager {

	public static String TRANSACTION_BASE_DIR = "/data/appdatas/slb-agent/transaction/";

	public static String LOCK = "tx.lock";

	public static String LOG = "tx.log";

	public static String PROPERTIES = "tx.properties";

	public static String TX_CLASSNAME = "tx_class_name";

	public static String TX_JSON = "tx_json";

	@Override
	public boolean startTransaction(Transaction transaction) throws IOException {
		File lock = new File(getBaseDir(transaction), LOCK);

		if (lock.exists()) {
			return false;
		} else {
			lock.createNewFile();
			return true;
		}
	}

	@Override
	public boolean endTransaction(Transaction transaction) {
		throw new RuntimeException("not implement yet!");
	}

	@Override
	public Transaction loadTransaction(int transactionId) throws Exception {
		Transaction transaction = null;
		InputStream inputStream = new FileInputStream(getPropertiesFile(new Transaction(null, transactionId, null)));
		Properties properties = new Properties();

		properties.load(inputStream);

		String txJson = properties.getProperty(TX_JSON, "{}");
		String className = properties.getProperty(TX_CLASSNAME);
		Class clazz = Class.forName(className);

		transaction = (Transaction) new Gson().fromJson(txJson, clazz);
		return transaction;
	}

	@Override
	public void saveTransaction(Transaction transaction) throws IOException {
		Properties properties = new Properties();
		OutputStream outputStream = new FileOutputStream(getPropertiesFile(transaction));

		try {
			properties.setProperty(TX_CLASSNAME, transaction.getClass().getName());
			properties.setProperty(TX_JSON, new Gson().toJson(transaction));
			properties.store(outputStream, "");
		} finally {
			IOUtils.closeQuietly(outputStream);
		}
	}

	@Override
	public OutputStream getLogOut(Transaction transaction) throws IOException {
		File log = getLogFile(transaction);

		return new FileOutputStream(log, true);
	}

	@Override
	public Reader getReader(Transaction transaction, int offset) throws IOException {
		File log = getLogFile(transaction);
		Reader reader = new FileReader(log);

		reader.skip(offset);
		return reader;
	}

	private File getLogFile(Transaction transaction) throws IOException {
		File log = new File(getBaseDir(transaction), LOG);

		if (!log.exists()) {
			log.createNewFile();
		}
		return log;
	}

	private File getPropertiesFile(Transaction transaction) throws IOException {
		File propertiesFile = new File(getBaseDir(transaction), PROPERTIES);

		if (!propertiesFile.exists()) {
			propertiesFile.createNewFile();
		}
		return propertiesFile;
	}

	private File getBaseDir(Transaction transaction) throws IOException {
		File transactionDir = new File(TRANSACTION_BASE_DIR + transaction.getId());

		if (!transactionDir.exists() || transactionDir.isFile()) {
			FileUtils.forceMkdir(transactionDir);
		}
		return transactionDir;
	}

}
