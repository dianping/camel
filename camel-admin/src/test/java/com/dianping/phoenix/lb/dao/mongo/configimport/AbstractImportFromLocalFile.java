package com.dianping.phoenix.lb.dao.mongo.configimport;

import com.dianping.phoenix.lb.dao.mongo.MongoAutoIncrementIdGenerator;
import com.dianping.phoenix.lb.dao.mongo.MongoDbStartInitializer;
import com.dianping.phoenix.lb.model.entity.SlbModelTree;
import com.dianping.phoenix.lb.model.entity.VirtualServer;
import com.dianping.phoenix.lb.model.transform.DefaultSaxParser;
import com.mongodb.BasicDBObject;
import com.mongodb.Mongo;
import com.mongodb.ServerAddress;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.WriteResultChecking;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年11月10日 下午6:56:46
 */
public abstract class AbstractImportFromLocalFile implements Importer {

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	Map<String, Integer> tags = new HashMap<String, Integer>();
	private String localFileDir = "/data/appdatas/phoenix/slb/git/phoenix-slb-model";
	private String config = "/data/appdatas/phoenix/slb/slb-lion.properties";
	private MongoTemplate mongoTemplate;
	private String mongoDb = "192.168.218.49:27017";
	private String dataBase = "slb";
	private ExecutorService executors = Executors.newFixedThreadPool(8);

	public void importFiles() throws Exception {

		beforeLocalFile();
		importFromLocalFile();
	}

	@SuppressWarnings("deprecation")
	public void beforeLocalFile() throws NumberFormatException, FileNotFoundException, IOException {

		readConfig();

		List<ServerAddress> address = new LinkedList<ServerAddress>();

		for (String url : mongoDb.split(",")) {

			String[] addr = url.split(":");
			if (addr.length != 2) {
				throw new IllegalArgumentException("wrong mongo db address:" + mongoDb);
			}
			ServerAddress sa = new ServerAddress(addr[0], Integer.parseInt(addr[1]));
			address.add(sa);
		}
		mongoTemplate = new MongoTemplate(new Mongo(address), dataBase);
		mongoTemplate.setWriteResultChecking(WriteResultChecking.EXCEPTION);
		if (mongoTemplate.getConverter() instanceof MappingMongoConverter) {
			MappingMongoConverter converter = (MappingMongoConverter) mongoTemplate.getConverter();
			converter.setMapKeyDotReplacement(MongoDbStartInitializer.MAP_KEY_DOT_REPLACEMENT);
		}

	}

	private void readConfig() throws FileNotFoundException, IOException {

		Properties p = new Properties();
		p.load(new FileInputStream(new File(config)));
		mongoDb = p.getProperty("mongodb.url").trim();
		dataBase = p.getProperty("mongodb.dbname_config").trim();
	}

	private void importFromLocalFile() throws IOException, SAXException, InterruptedException {

		clearCollections();

		long start = System.currentTimeMillis();
		importAllFiles(new File(localFileDir), 0);

		executors.shutdown();
		executors.awaitTermination(1000, TimeUnit.SECONDS);

		long end = System.currentTimeMillis();
		System.out.println("Total cost time:" + (end - start) / 1000 + " seconds");

	}

	private void clearCollections() {

		for (String collectionName : mongoTemplate.getCollectionNames()) {
			if (logger.isInfoEnabled()) {
				logger.info("[clearCollections]" + collectionName);
			}
			if (collectionName.equals("system.indexes")) {
				continue;
			}
			mongoTemplate.dropCollection(collectionName);
		}
	}

	private void importAllFiles(final File f, final int level) throws IOException, SAXException {

		if (f.getName().equals(".git")) {
			logger.info("[importAllFiles][skip]" + f);
			return;
		}

		if (f.isFile()) {
			if (f.getName().indexOf(".xml") < 0) {
				logger.info("[importAllFiles][skip]" + f);
				return;
			}

			executors.execute(new Runnable() {

				@Override
				public void run() {

					try {
						if (logger.isInfoEnabled()) {
							logger.info("[run]" + f);
						}
						if (level == 1) {
							if (f.getName().endsWith(".xml")) {//只处理.xml文件，其它类型忽略
								processConcurrentConfig(f);
							} else {
								logger.warn("[run][ignore file]" + f);
							}

						} else {
							processHistroyFile(f);
						}
					} catch (Exception e) {
						logger.error("[error]" + f, e);
					}
				}

			});

			return;
		}
		for (File subFile : f.listFiles()) {
			importAllFiles(subFile, level + 1);
		}
	}

	private void processHistroyFile(File f) throws IOException, SAXException {

		processConcurrentConfig(f);
	}

	private void processConcurrentConfig(File f) throws IOException, SAXException {

		SlbModelTree slbmodel = readFromFile(f);
		String collectionName = getCollectionName(f, slbmodel);
		synchronized (this) {
			if (!mongoTemplate.collectionExists(collectionName)) {
				createCollection(collectionName);
			}
		}

		DocumentKey key = getDocumentKey(f, slbmodel);
		slbmodel.setTag(key.getTag());
		updateTagGlobalId(collectionName, key.getTag());
		mongoTemplate.insert(slbmodel, collectionName);
	}

	private synchronized void updateTagGlobalId(String collectionName, int tag) {

		Integer prev = tags.get(collectionName);
		Integer set = null;

		if (prev == null || tag > prev) {
			tags.put(collectionName, tag);
			set = tag;
		}

		if (set != null) {
			String valueKey = "idValue";
			mongoTemplate
					.upsert(new Query().addCriteria(Criteria.where("_id").is(collectionName).and(valueKey).lt(tag)),
							new Update().set(valueKey, tag), MongoAutoIncrementIdGenerator.COLLECTION_NAME);
		}

	}

	private void createCollection(String collectionName) {

		mongoTemplate.createCollection(collectionName);
		IndexDefinition index = new CompoundIndexDefinition(new BasicDBObject().append("m_tag", 1)).unique();
		mongoTemplate.indexOps(collectionName).ensureIndex(index);
	}

	private DocumentKey getDocumentKey(File f, SlbModelTree slbmodel) {

		String fileName = f.getName();
		if (fileName.indexOf("slb_base.xml") >= 0) {
			return new DocumentKey("slb_base");
		}

		VirtualServer vs = (VirtualServer) slbmodel.getVirtualServers().values().toArray()[0];
		String name = vs.getName();

		int index = fileName.indexOf(".xml_");
		if (index >= 0) {
			String tag = fileName.substring(index + ".xml_".length()).trim();
			return new DocumentKey(name, Integer.parseInt(tag));
		}
		//concurrent version
		return new DocumentKey(name);
	}

	private String getCollectionName(File f, SlbModelTree slbmodel) {

		if (f.getName().indexOf("slb_base.xml") >= 0) {
			return "slb_base";
		}

		if (slbmodel.getVirtualServers().size() != 1) {
			throw new IllegalStateException("wrong file format, multi virtual servers:" + f);
		}

		VirtualServer vs = (VirtualServer) slbmodel.getVirtualServers().values().toArray()[0];
		return vs.getName();
	}

	private SlbModelTree readFromFile(File f) throws IOException, SAXException {

		String xml = FileUtils.readFileToString(f);
		return DefaultSaxParser.parse(xml);

	}

	public String getLocalFileDir() {
		return localFileDir;
	}

	public void setLocalFileDir(String localFileDir) {
		this.localFileDir = localFileDir;
	}

	public String getConfig() {
		return config;
	}

	public void setConfig(String config) {
		this.config = config;
	}

	private class DocumentKey {

		private String key;

		private int tag = 0;

		public DocumentKey(String key) {
			this.setKey(key);
		}

		public DocumentKey(String key, int tag) {
			this.setKey(key);
			this.setTag(tag);
		}

		@SuppressWarnings("unused")
		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public int getTag() {
			return tag;
		}

		public void setTag(int tag) {
			this.tag = tag;
		}

	}

}
