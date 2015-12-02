package com.dianping.phoenix.lb.model;

public enum CheckStatus {

	JSON("json"), HTML("html"), CVS("cvs");

	String name;

	private CheckStatus(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
