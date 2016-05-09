package com.dianping.phoenix.lb.visitor;

import com.dianping.phoenix.lb.model.transform.BaseVisitor;

/**
 * @author Leo Liang
 *
 */
public abstract class AbstractVisitor<T> extends BaseVisitor {
	protected T result;

	public T getVisitorResult() {
		return result;
	}
}
