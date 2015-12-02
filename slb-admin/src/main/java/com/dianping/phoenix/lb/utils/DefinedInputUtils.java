package com.dianping.phoenix.lb.utils;

import com.dianping.phoenix.lb.action.DefinedInput;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wukezhu
 */
public enum DefinedInputUtils {
	INSTANCE;

	public static void main(String[] args) {
		System.out.println(DefinedInputUtils.INSTANCE.getDirectiveTypes());
		System.out.println(DefinedInputUtils.INSTANCE.getDirectiveDefinedInputs("accesslog"));
	}

	public Map<String, DefinedInput> getPropertiesDefinedInputs() {
		String dir = "ui-config/propertiesDefinedInput";
		Map<String, DefinedInput> map = getDefinedInputByDir(dir);
		return map;
	}

	public Map<String, DefinedInput> getDirectiveDefinedInputs(String type) {
		String dir = "ui-config/directiveDefinedInput/" + type;
		Map<String, DefinedInput> map = getDefinedInputByDir(dir);
		return map;
	}

	public List<String> getDirectiveTypes() {
		List<String> list = new ArrayList<String>();

		String dir = "ui-config/directiveDefinedInput/";
		File[] files = this.listFiles(dir);
		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					list.add(file.getName());
				}
			}
		}
		return list;
	}

	private Map<String, DefinedInput> getDefinedInputByDir(String dir) {
		Map<String, DefinedInput> map = new HashMap<String, DefinedInput>();
		File[] files = this.listFiles(dir);
		if (files != null) {
			for (File file : files) {
				DefinedInput definedInput = this.getDefinedInput(file);
				map.put(definedInput.getName(), definedInput);
			}
		}
		return map;
	}

	private File[] listFiles(String dir) {
		URL templateDirUrl = DefinedInputUtils.class.getClassLoader().getResource(dir);
		File templateDir = FileUtils.toFile(templateDirUrl);
		if (templateDir != null && templateDir.isDirectory()) {
			return templateDir.listFiles();
		}
		return new File[0];
	}

	private DefinedInput getDefinedInput(File file) {
		String content;
		try {
			content = FileUtils.readFileToString(file);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		DefinedInput definedInput = GsonUtils.fromJson(content, DefinedInput.class);
		definedInput.setName(file.getName());
		return definedInput;
	}
}
