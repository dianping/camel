package com.dianping.phoenix.lb.model;

public enum CheckType {

	TCP("tcp"), HTTP("http"), SSL_HELLO("ssl_hello"), MYSQL("mysql"), AJP("ajp");

	String name;

	private CheckType(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
