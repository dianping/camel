package com.dianping.phoenix.lb.service;

import com.dianping.phoenix.lb.service.model.VariableService;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年11月13日 下午6:15:30
 */
public class VariableReplacer {

	private Map<String, String> variables;

	public VariableReplacer(Map<String, String> variables) {

		this.variables = variables;
	}

	public String translateValue(String initValue) {
		//变量替换 ${key}
		return replace(initValue, VariableService.pattern);
	}

	private String replace(String buff, Pattern pattern) {

		Matcher m = pattern.matcher(buff);
		StringBuilder sb = new StringBuilder();
		int pre = 0;
		boolean found = false;
		while (m.find()) {

			sb.append(buff.subSequence(pre, m.start()));
			pre = m.end();

			String variableName = m.group(1);
			String value = variables.get(variableName);
			if (value == null) {
				throw new IllegalStateException("[replace][undefined variable]" + variableName);
			}
			sb.append(value);
			found = true;
		}

		sb.append(buff.substring(pre));

		if (!found) {
			return buff;
		}
		return replace(sb.toString(), pattern);
	}
}
