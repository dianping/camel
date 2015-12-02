package com.dianping.phoenix.lb.dao;

import com.dianping.phoenix.lb.model.entity.User;

import java.util.List;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface UserDao {

	List<User> listUsers();

	User findUser(String accountName);

	void updateOrCreateUser(User user);

	void removeUser(String account);

}
