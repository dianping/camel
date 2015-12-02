package com.dianping.phoenix.lb.service.model;

import com.dianping.phoenix.lb.constant.MessageID;
import com.dianping.phoenix.lb.dao.UserDao;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.User;
import com.dianping.phoenix.lb.service.ConcurrentControlServiceTemplate;
import com.dianping.phoenix.lb.utils.ExceptionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/*-
 * @author liyang
 *
 * 2015年5月13日 上午8:27:36
 */
@Service
public class UserServiceImpl extends ConcurrentControlServiceTemplate implements UserService {

	private UserDao m_userDao;

	@Autowired(required = true)
	public UserServiceImpl(UserDao userDao) {
		super();
		this.m_userDao = userDao;
	}

	@PostConstruct
	private void initUser() {
		List<User> users = generateUsers();

		for (User user : users) {
			try {
				updateOrCreateUser(user);
			} catch (Exception ex) {
			}
		}

	}

	private List<User> generateUsers() {
		List<User> users = new ArrayList<User>();

		try {
			InputStream is = getClass().getClassLoader().getResourceAsStream("init-data/defaultAdmin");
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line;

			while ((line = br.readLine()) != null) {
				String[] metrics = line.split("\t");

				if (metrics.length == 2) {
					User user = new User();

					user.setAccount(metrics[0]);
					user.setName(metrics[1]);
					user.setIsAdmin(true);
					users.add(user);
				}
			}
		} catch (Exception ex) {
		}
		return users;
	}

	@Override
	public void updateOrCreateUser(final User user) throws BizException {
		if (user == null) {
			return;
		}
		validate(user);

		write(new WriteOperation<Void>() {
			@Override
			public Void doWrite() throws Exception {
				m_userDao.updateOrCreateUser(user);
				return null;
			}
		});
	}

	@Override
	public void removeUser(final String account) throws BizException {
		if (StringUtils.isBlank(account)) {
			ExceptionUtils.throwBizException(MessageID.USER_ACCOUNT_EMPTY);
		}

		try {
			write(new WriteOperation<Void>() {
				@Override
				public Void doWrite() throws Exception {
					m_userDao.removeUser(account);
					return null;
				}
			});
		} catch (BizException e) {
		}
	}

	@Override
	public User findUser(final String accountName) {
		try {
			return read(new ReadOperation<User>() {
				@Override
				public User doRead() throws Exception {
					return m_userDao.findUser(accountName);
				}
			});
		} catch (BizException e) {
			return null;
		}
	}

	@Override
	public List<User> listUsers() {
		try {
			return read(new ReadOperation<List<User>>() {
				@Override
				public List<User> doRead() throws Exception {
					return m_userDao.listUsers();
				}
			});
		} catch (BizException e) {
			return null;
		}
	}

	public void setUserDao(UserDao userDao) {
		this.m_userDao = userDao;
	}

	private void validate(User user) throws BizException {
		if (StringUtils.isBlank(user.getAccount())) {
			ExceptionUtils.throwBizException(MessageID.USER_ACCOUNT_EMPTY);
		}
		if (StringUtils.isBlank(user.getName())) {
			ExceptionUtils.throwBizException(MessageID.USER_NAME_EMPTY);
		}
	}
}
