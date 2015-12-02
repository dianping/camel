package com.dianping.phoenix.lb.dao.impl;

import com.dianping.phoenix.lb.dao.ModelStore;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Leo Liang
 *
 */
public class AbstractDao {
	@Autowired
	protected ModelStore store;

	/**
	 * @param store
	 */
	public AbstractDao(ModelStore store) {
		super();
		this.store = store;
	}

}
