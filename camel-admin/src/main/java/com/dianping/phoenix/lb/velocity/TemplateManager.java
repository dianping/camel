package com.dianping.phoenix.lb.velocity;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public enum TemplateManager {
	INSTANCE;

	private Map<String, Map<String, String>> templateCache = new ConcurrentHashMap<String, Map<String, String>>();

	private File templateDir;

	private TemplateManager() {
		URL templateDirUrl = TemplateManager.class.getClassLoader().getResource("template/");
		templateDir = FileUtils.toFile(templateDirUrl);
		if (templateDir != null && templateDir.isDirectory()) {
			File[] subFolders = templateDir.listFiles();
			for (File folder : subFolders) {
				String folderName = folder.getName();
				if (!templateCache.containsKey(folderName)) {
					templateCache.put(folderName, new ConcurrentHashMap<String, String>());
				}
				Collection<File> templateFiles = FileUtils.listFiles(folder, new String[] { "vm" }, false);
				for (File file : templateFiles) {
					if (!templateCache.get(folderName).containsKey(file.getName())) {
						try {
							templateCache.get(folderName)
									.put(file.getName().substring(0, file.getName().length() - ".vm".length()),
											FileUtils.readFileToString(file));
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
				}
			}
		}
	}

	public String getTemplate(String schema, String fileName) {
		if (templateCache.containsKey(schema)) {
			if (templateCache.get(schema).containsKey(fileName)) {
				return templateCache.get(schema).get(fileName);
			}
		}
		return "";
	}

	public Set<String> availableFiles(String schema) {
		return templateCache.containsKey(schema) ? templateCache.get(schema).keySet() : new HashSet<String>();
	}
}
