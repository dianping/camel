package com.dianping.phoenix.lb.service.model;

import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.User;

import java.util.List;

/*-
 * @author liyang
 *
 * 2015年5月13日 上午8:27:12
 */
public interface UserService {

	List<User> listUsers();

	void updateOrCreateUser(User user) throws BizException;

	void removeUser(String account) throws BizException;

	User findUser(String accountName);

}
