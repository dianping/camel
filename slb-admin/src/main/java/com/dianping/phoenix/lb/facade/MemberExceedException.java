package com.dianping.phoenix.lb.facade;

import com.dianping.phoenix.lb.exception.SlbException;

public class MemberExceedException extends SlbException {

	private static final long serialVersionUID = 1L;

	public MemberExceedException(int actualMemberCount) {

		super("Cannot del so many members. actual members count in this pool: " + actualMemberCount);
	}

}
