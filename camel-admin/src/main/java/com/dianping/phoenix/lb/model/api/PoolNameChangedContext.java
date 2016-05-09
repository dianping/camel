package com.dianping.phoenix.lb.model.api;

import java.util.ArrayList;
import java.util.List;

public class PoolNameChangedContext {

	private int result;

	private String oldName;

	private String newName;

	private List<String> output = new ArrayList<String>();

	public PoolNameChangedContext(String oldName, String newName) {
		super();
		this.oldName = oldName;
		this.newName = newName;
	}

	public void markError() {
		result |= 1;
	}

	public void markWarn() {
		result |= 2;
	}

	public boolean hasError() {
		return (result & 1) == 1;
	}

	public boolean hasWarn() {
		return (result & 2) == 2;
	}

	public String getOldName() {
		return oldName;
	}

	public void setOldName(String oldName) {
		this.oldName = oldName;
	}

	public String getNewName() {
		return newName;
	}

	public void setNewName(String newName) {
		this.newName = newName;
	}

	public List<String> getOutput() {
		return output;
	}

	public void setOutput(List<String> output) {
		this.output = output;
	}

	public void outputln(String line) {
		output.add(line);
	}

	public void outputln(Object line) {
		output.add(line.toString());
	}

	public void outputln() {
		output.add("");
	}

	@Override
	public String toString() {
		return String.format("PoolNameChangedResult [oldName=%s, newName=%s, output=%s]", oldName, newName, output);
	}

}
