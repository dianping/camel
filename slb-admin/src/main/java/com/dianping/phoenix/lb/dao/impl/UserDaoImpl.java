package com.dianping.phoenix.lb.dao.impl;

import com.dianping.phoenix.lb.dao.ModelStore;
import com.dianping.phoenix.lb.dao.UserDao;
import com.dianping.phoenix.lb.model.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Service
public class UserDaoImpl extends AbstractDao implements UserDao {

	/**
	 * @param store
	 */
	@Autowired(required = true)
	public UserDaoImpl(ModelStore store) {
		super(store);
	}

	@Override
	public List<User> listUsers() {
		return store.listUsers();
	}

	@Override
	public User findUser(String accountName) {
		return store.findUser(accountName);
	}

	@Override
	public void updateOrCreateUser(User user) {
		store.updateOrCreateUser(user);
	}

	@Override
	public void removeUser(String account) {
		store.removeUser(account);
	}

}
