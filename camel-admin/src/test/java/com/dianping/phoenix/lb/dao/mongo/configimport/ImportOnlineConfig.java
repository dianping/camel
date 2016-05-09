package com.dianping.phoenix.lb.dao.mongo.configimport;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年11月10日 下午6:56:46
 */
public class ImportOnlineConfig extends AbstractImportFromLocalFile {

	public static void main(String[] argc) throws Exception {

		String configFile = null, localDir = null;

		if (argc.length >= 1) {
			configFile = argc[0];
		}
		if (argc.length >= 2) {
			localDir = argc[1];
		}

		ImportOnlineConfig im = new ImportOnlineConfig();
		if (configFile != null) {
			im.setConfig(configFile);
		}
		if (localDir != null) {
			im.setLocalFileDir(localDir);
		}
		im.importFiles();
	}

}
