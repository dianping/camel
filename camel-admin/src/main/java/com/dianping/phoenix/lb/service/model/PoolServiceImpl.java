package com.dianping.phoenix.lb.service.model;

import com.dianping.phoenix.lb.constant.MessageID;
import com.dianping.phoenix.lb.constant.SlbConfig;
import com.dianping.phoenix.lb.dao.PoolDao;
import com.dianping.phoenix.lb.dao.StrategyDao;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.facade.PoolAvailableRateException;
import com.dianping.phoenix.lb.model.State;
import com.dianping.phoenix.lb.model.entity.Member;
import com.dianping.phoenix.lb.model.entity.Pool;
import com.dianping.phoenix.lb.model.entity.Strategy;
import com.dianping.phoenix.lb.service.ConcurrentControlServiceTemplate;
import com.dianping.phoenix.lb.utils.ExceptionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author Leo Liang
 *
 */
@Service
public class PoolServiceImpl extends ConcurrentControlServiceTemplate implements PoolService {

	@Autowired
	private SlbConfig m_slbConfig;

	private PoolDao poolDao;

	private StrategyDao strategyDao;

	/**
	 * @param poolDao
	 */
	@Autowired(required = true)
	public PoolServiceImpl(PoolDao poolDao, StrategyDao strategyDao) {
		super();
		this.poolDao = poolDao;
		this.strategyDao = strategyDao;
	}

	/**
	 * @param poolDao
	 *           the poolDao to set
	 */
	public void setStrategyDao(PoolDao poolDao) {
		this.poolDao = poolDao;
	}

	@Override
	public List<Pool> listPools() {
		try {
			return read(new ReadOperation<List<Pool>>() {

				@Override
				public List<Pool> doRead() throws Exception {
					return poolDao.list();
				}
			});
		} catch (BizException e) {
			// ignore
			return null;
		}
	}

	@Override
	public Set<String> listPoolNames() {
		Set<String> result = new HashSet<String>();

		List<Pool> pools = listPools();

		if (pools != null) {
			for (Pool pool : pools) {
				result.add(pool.getName());
			}
		}
		return result;
	}

	@Override
	public Pool findPool(final String poolName) throws BizException {
		if (StringUtils.isBlank(poolName)) {
			ExceptionUtils.throwBizException(MessageID.POOL_NAME_EMPTY);
		}

		return read(new ReadOperation<Pool>() {

			@Override
			public Pool doRead() throws BizException {
				return poolDao.find(poolName);
			}
		});
	}

	@Override
	public void addPool(String poolName, final Pool pool) throws BizException, PoolAvailableRateException {
		if (poolName == null || pool == null) {
			return;
		}

		if (!poolName.equals(pool.getName())) {
			return;
		}

		validate(pool, MemberModifier.DEFAULT);

		write(new WriteOperation<Void>() {

			@Override
			public Void doWrite() throws Exception {
				poolDao.add(pool);
				return null;
			}
		});
	}

	@Override
	public void deletePool(final String poolName) throws BizException {
		if (StringUtils.isBlank(poolName)) {
			ExceptionUtils.throwBizException(MessageID.POOL_NAME_EMPTY);
		}

		try {
			write(new WriteOperation<Void>() {

				@Override
				public Void doWrite() throws Exception {
					poolDao.delete(poolName);
					return null;
				}
			});
		} catch (BizException e) {
			// ignore
		}
	}

	@Override
	public void modifyPool(final String poolName, final Pool pool, MemberModifier memberModifier)
			throws BizException, PoolAvailableRateException {
		if (poolName == null || pool == null) {
			return;
		}

		if (!poolName.equals(pool.getName())) {
			return;
		}

		validate(pool, memberModifier);

		write(new WriteOperation<Void>() {

			@Override
			public Void doWrite() throws Exception {
				poolDao.update(pool);
				return null;
			}
		});

	}

	private void validate(Pool pool, MemberModifier memberModifier) throws BizException, PoolAvailableRateException {
		if (StringUtils.isBlank(pool.getName())) {
			ExceptionUtils.throwBizException(MessageID.POOL_NAME_EMPTY);
		}

		// 允许member为0个
		// if (pool.getMembers().size() == 0) {
		// ExceptionUtils.throwBizException(MessageID.POOL_NO_MEMBER, pool.getName());
		// }

		if (pool.getMembers() != null) {
			for (Member member : pool.getMembers()) {
				if (StringUtils.isBlank(member.getName())) {
					ExceptionUtils.throwBizException(MessageID.POOL_MEMBER_NO_NAME);
				}
				if (StringUtils.isBlank(member.getIp())) {
					ExceptionUtils.throwBizException(MessageID.POOL_MEMBER_NO_IP, member.getName());
				}

			}
		}

		// member不可重复
		Set<String> names = new HashSet<String>();
		List<Member> members = pool.getMembers();
		if (members != null) {
			Iterator<Member> iterator = members.iterator();
			while (iterator.hasNext()) {
				Member m = iterator.next();
				if (!names.add(m.getName())) {
					iterator.remove();
					//不报错，直接去重。
					//					throw new IllegalArgumentException("Member name '" + m.getName() + "' is reduplicate.");
				}
			}
		}

		//检查可用member数量，不能为0
		int enabled = 0;
		int degrade = 0;
		for (Member member : pool.getMembers()) {
			if (member.getState() == State.ENABLED) {
				enabled++;
			}
			if (member.getState() == State.DEGRADE) {
				degrade++;
			}
		}

		int total = pool.getMembers().size() - degrade;
		double minEnableRate = 0.5;

		switch (memberModifier) {
		case ADD:
			minEnableRate = m_slbConfig.getAddMemberValidateMinRate();
			break;
		case DELETE:
			minEnableRate = m_slbConfig.getDelMemberValidateMinRate();
			break;
		case DEFAULT:
			minEnableRate = m_slbConfig.getDefaultMemberValidateMinRate();
			break;
		case FORCED:
			minEnableRate = 0;
			break;
		}

		if ((double) enabled / total < minEnableRate) {
			throw new PoolAvailableRateException(pool.getName(), minEnableRate);
		}

		List<Strategy> strategies = strategyDao.list();
		List<String> strategyNames = new ArrayList<String>();
		for (Strategy strategy : strategies) {
			strategyNames.add(strategy.getName());
		}

		if (!strategyNames.contains(pool.getLoadbalanceStrategyName())) {
			ExceptionUtils.throwBizException(MessageID.POOL_STRATEGY_NOT_SUPPORT, pool.getLoadbalanceStrategyName(),
					pool.getName());
		}

	}

	public enum MemberModifier {
		ADD, DELETE, FORCED, DEFAULT;
	}

}
