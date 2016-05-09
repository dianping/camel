package com.dianping.phoenix.lb.dao.mongo.configimport;

import java.net.URL;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年11月10日 下午6:56:46
 */
public class ImportTestConfig extends AbstractImportFromLocalFile {

	public static void main(String[] argc) throws Exception {

		new ImportTestConfig().importFiles();
	}

	@Override
	public void importFiles() throws Exception {

		URL urlConfig = ImportTestConfig.class.getClassLoader().getResource("spring/slb-lion-test.properties");
		URL urlLocal = ImportTestConfig.class.getClassLoader().getResource("storeTest");

		setConfig(urlConfig.getPath());
		setLocalFileDir(urlLocal.getPath());
		super.importFiles();
	}

}
