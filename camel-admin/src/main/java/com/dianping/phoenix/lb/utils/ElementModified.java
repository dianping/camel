package com.dianping.phoenix.lb.utils;

/**
 * 元素变更
 *
 * @author mengwenchao
 *         <p/>
 *         2014年7月13日 下午2:40:49
 */
public class ElementModified {

	private Object oldElement;

	private Object newElement;

	public ElementModified(Object oldElement, Object newElement) {

		this.oldElement = oldElement;
		this.newElement = newElement;
	}

	public Object getOldElement() {
		return oldElement;
	}

	public Object getNewElement() {
		return newElement;
	}

}
