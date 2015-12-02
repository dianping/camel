package com.dianping.phoenix.lb.facade.impl;

import com.dianping.phoenix.lb.PlexusComponentContainer;
import com.dianping.phoenix.lb.api.lock.KeyLock;
import com.dianping.phoenix.lb.configure.ConfigManager;
import com.dianping.phoenix.lb.constant.MessageID;
import com.dianping.phoenix.lb.deploy.bo.api.DeployTaskApiBo;
import com.dianping.phoenix.lb.deploy.executor.ApiTaskExecutorContainer;
import com.dianping.phoenix.lb.deploy.executor.TaskExecutor;
import com.dianping.phoenix.lb.deploy.executor.TaskExecutorContainer;
import com.dianping.phoenix.lb.deploy.service.AgentSequenceService;
import com.dianping.phoenix.lb.deploy.service.DeployTaskApiService;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.facade.*;
import com.dianping.phoenix.lb.model.State;
import com.dianping.phoenix.lb.model.entity.Check;
import com.dianping.phoenix.lb.model.entity.Member;
import com.dianping.phoenix.lb.model.entity.Pool;
import com.dianping.phoenix.lb.service.model.*;
import com.dianping.phoenix.lb.service.model.PoolServiceImpl.MemberModifier;
import com.dianping.phoenix.lb.utils.ExceptionUtils;
import org.apache.commons.lang.Validate;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtilsHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Service
@SuppressWarnings("rawtypes")
public class PoolFacadeImpl implements PoolFacade<DeployTaskApiBo> {

	private static final Logger logger = LoggerFactory.getLogger(PoolFacadeImpl.class);
	@Autowired
	protected VirtualServerService virtualServerService;
	@Autowired
	protected CommonAspectService commonAspectService;
	@Autowired
	protected AgentSequenceService agentSequenceService;
	@Autowired
	protected StrategyService strategyService;
	@Autowired
	protected BatchDeploy batchDeploy;
	protected ConfigManager configManager;
	@Autowired
	private PoolService poolService;
	@Autowired
	private VariableService variableService;
	@Autowired
	private SlbPoolService slbPoolService;
	@Autowired
	private DeployTaskApiService deployTaskService;
	@Resource(type = ApiTaskExecutorContainer.class)
	private TaskExecutorContainer taskContainer;

	@Resource(name = "concurrentLock")
	private KeyLock locks;

	public static void validateMember(List<Member> members) {
		if (members != null) {
			for (Member member : members) {
				Validate.notEmpty(member.getIp(), "Member'ip cannot be empty.");
				if (member.getName() == null) {// member's default name is ip
					member.setName(member.getIp() + "_" + member.getPort());
				}
			}
		}
	}

	@PostConstruct
	public void init() throws ComponentLookupException {
		configManager = PlexusComponentContainer.INSTANCE.lookup(ConfigManager.class);
	}

	@Override
	public void addPool(Pool pool) throws BizException, PoolAvailableRateException {

		// member ip不能为空，member 名称处理一下，默认以ip为名
		List<Member> members = pool.getMembers();
		validateMember(members);

		locks.lock(pool.getName());
		try {

			Pool pool0 = poolService.findPool(pool.getName());
			// pool存在则追加member
			if (pool0 != null) {
				if (logger.isInfoEnabled()) {
					logger.info("[addPool][pool exist, addMember]" + members);
				}
				if (members != null) {
					for (Member member : members) {
						pool0.addMember(member);
					}
				}
				poolService.modifyPool(pool0.getName(), pool0, MemberModifier.ADD);

			} else {
				if (logger.isInfoEnabled()) {
					logger.info("[addPool][pool not exist, add it]");
				}
				if (pool.getCheck() == null) {
					pool.setCheck(new Check());
				}
				poolService.addPool(pool.getName(), pool);
			}
		} finally {
			locks.unlock(pool.getName());
		}
	}

	@Override
	public void addMember(String poolName, List<Member> members)
			throws BizException, PoolNotFoundException, PoolAvailableRateException {
		addMember(poolName, members, false);
	}

	@Override
	public void addMemberForced(String poolName, List<Member> members)
			throws BizException, PoolNotFoundException, PoolAvailableRateException {
		addMember(poolName, members, true);
	}

	public void addMember(String poolName, List<Member> members, boolean isForced)
			throws BizException, PoolNotFoundException, PoolAvailableRateException {
		if (members == null || members.size() <= 0) {
			return;
		}

		locks.lock(poolName);
		try {
			// 修改pool
			Pool pool = poolService.findPool(poolName);
			if (pool == null) {
				throw new PoolNotFoundException(poolName);
			}

			for (Member member : members) {
				pool.addMember(member);
			}
			if (isForced) {
				poolService.modifyPool(poolName, pool, MemberModifier.FORCED);
			} else {
				poolService.modifyPool(poolName, pool, MemberModifier.ADD);
			}
		} finally {
			locks.unlock(poolName);
		}
	}

	@Override
	public void forceState(String poolName, int forceState) throws BizException, PoolNotFoundException {

		locks.lock(poolName);
		try {
			// 修改pool
			Pool pool = poolService.findPool(poolName);
			if (pool == null) {
				throw new PoolNotFoundException(poolName);
			}

			pool.setDegradeForceState(forceState);
			poolService.modifyPool(poolName, pool, MemberModifier.DEFAULT);
		} catch (PoolAvailableRateException e) {
			logger.error("[forceState]", e);
		} finally {
			locks.unlock(poolName);
		}
	}

	@Override
	public void updateMember(String poolName, List<Member> members)
			throws BizException, PoolNotFoundException, PoolAvailableRateException, MemberNotFoundException {
		updateMember(poolName, members, false);
	}

	@Override
	public void updateMemberForced(String poolName, List<Member> members)
			throws BizException, PoolNotFoundException, PoolAvailableRateException, MemberNotFoundException {
		updateMember(poolName, members, true);
	}

	public void updateMember(String poolName, List<Member> members, boolean isForced)
			throws BizException, PoolNotFoundException, PoolAvailableRateException, MemberNotFoundException {

		if (members == null || members.size() <= 0) {
			return;
		}

		locks.lock(poolName);
		try {
			// 修改pool
			Pool pool = poolService.findPool(poolName);
			if (pool == null) {
				throw new PoolNotFoundException(poolName);
			}

			List<Member> originMembers = pool.getMembers();
			int deltaEnableCount = 0;

			for (Member member : members) {
				boolean found = false;
				for (Member originMember : originMembers) {
					if (member.getName() != null && member.getName().equals(originMember.getName())) {
						found = true;
						deltaEnableCount += calEnableDelta(originMember.getState(), member.getState());
						BeanUtilsHelper.copyNotNullProperties(member, originMember);
						continue;
					}
					if (member.getIp() != null && member.getIp().equals(originMember.getIp())) {
						String originName = originMember.getName();
						found = true;
						deltaEnableCount += calEnableDelta(originMember.getState(), member.getState());
						BeanUtilsHelper.copyNotNullProperties(member, originMember);
						originMember.setName(originName);
						continue;
					}
				}
				if (!found) {
					throw new MemberNotFoundException(
							"unfound member( name:" + member.getName() + ", ip:" + member.getIp() + ")");
				}
			}
			if (isForced) {
				poolService.modifyPool(poolName, pool, MemberModifier.FORCED);
			} else {
				if (deltaEnableCount > 0) {
					poolService.modifyPool(poolName, pool, MemberModifier.ADD);
				} else if (deltaEnableCount < 0) {
					poolService.modifyPool(poolName, pool, MemberModifier.DELETE);
				} else {
					poolService.modifyPool(poolName, pool, MemberModifier.DEFAULT);
				}
			}
		} catch (IllegalAccessException e) {
			logger.error("[updateMember]", e);
			throw new BizException(e);
		} catch (InvocationTargetException e) {
			logger.error("[updateMember]", e);
			throw new BizException(e);
		} finally {
			locks.unlock(poolName);
		}
	}

	private int calEnableDelta(State originState, State state) {
		if (originState == State.ENABLED && state != State.ENABLED) {
			return -1;
		}
		if (originState != State.ENABLED && state == State.ENABLED) {
			return 1;
		}
		return 0;
	}

	@Override
	public void delMember(String poolName, List<String> memberNames)
			throws BizException, PoolAvailableRateException, PoolNotFoundException, MemberExceedException,
			NotSuccessException {
		delMember(poolName, memberNames, false);
	}

	@Override
	public void delMemberForced(String poolName, List<String> memberNames)
			throws BizException, PoolAvailableRateException, PoolNotFoundException, MemberExceedException,
			NotSuccessException {
		delMember(poolName, memberNames, true);
	}

	public void delMember(String poolName, List<String> memberNames, boolean isForced)
			throws BizException, PoolAvailableRateException, PoolNotFoundException, MemberExceedException,
			NotSuccessException {
		if (memberNames == null || memberNames.size() <= 0) {
			return;
		}

		locks.lock(poolName);
		try {
			Pool pool = poolService.findPool(poolName);

			if (pool == null) {
				throw new PoolNotFoundException(poolName);
			}

			List<Member> members = pool.getMembers();
			int originMemberSize = members.size();
			boolean isChanged = false;

			if (originMemberSize > 0) {
				Iterator<Member> iterator = members.iterator();

				while (iterator.hasNext()) {
					Member member = iterator.next();
					String memberName = member.getName();

					if (memberNames.contains(memberName)) {
						iterator.remove();
						memberNames.remove(memberName);
						isChanged = true;
					}
				}
				if (members.size() == 0) {
					throw new MemberExceedException(originMemberSize);
				}
				if (isChanged) {
					if (isForced) {
						poolService.modifyPool(poolName, pool, MemberModifier.FORCED);
					} else {
						poolService.modifyPool(poolName, pool, MemberModifier.DELETE);
					}
				}
			}
			if (memberNames.size() > 0) {
				throw new NotSuccessException(buildDelMemberMessage(memberNames));
			}
		} finally {
			locks.unlock(poolName);
		}
	}

	private String buildDelMemberMessage(List<String> undeletedMemberNames) {
		StringBuilder builder = new StringBuilder(200);

		builder.append("{undeletedMembers:[");
		for (String memberName : undeletedMemberNames) {
			builder.append("\"").append(memberName).append("\",");
		}

		int lastIndex = builder.length() - 1;

		if (",".equals(builder.charAt(lastIndex))) {
			builder.deleteCharAt(lastIndex);
		}
		builder.append("]}");
		return builder.toString();
	}

	@SuppressWarnings("unchecked")
	@Override
	public TaskExecutor deploy(String poolName) throws BizException {

		return deploy(Arrays.asList(new String[] { poolName }));
	}

	private TaskExecutor _execute(String poolName, List<String> influencingVsList) throws BizException {

		return batchDeploy.deployVs(" deploy pool:" + poolName, influencingVsList, "ApiCall");

	}

	@SuppressWarnings("unchecked")
	@Override
	public TaskExecutor<DeployTaskApiBo> deploy(List<String> poolNames) throws BizException {

		Set<String> totalList = new HashSet<String>();
		for (String poolName : poolNames) {
			List<String> influencingVsList = virtualServerService.findVirtualServerByPool(poolName);
			totalList.addAll(influencingVsList);
		}

		if (totalList == null || totalList.size() <= 0) {

			ExceptionUtils.throwBizException(MessageID.DEPLOY_POOL_NOT_RELATED_VS, poolNames);
		}

		List<String> list = new ArrayList<String>(totalList);

		TaskExecutor taskExecutor = _execute(poolNames.toString(), list);

		return taskExecutor;
	}

	@Override
	public List<Member> findCorrespondMembersByNameOrIp(String poolName, List<Member> members)
			throws BizException, PoolNotFoundException {
		List<Member> result = new ArrayList<Member>();

		if (members != null && members.size() > 0) {
			locks.lock(poolName);
			try {
				Pool pool = poolService.findPool(poolName);

				if (pool == null) {
					throw new PoolNotFoundException(poolName);
				}

				List<Member> originMembers = pool.getMembers();

				for (Member member : members) {
					for (Member originMember : originMembers) {
						if (member.getName() != null && member.getName().equals(originMember.getName())) {
							result.add(originMember);
							continue;
						}
						if (member.getIp() != null && member.getIp().equals(originMember.getIp())) {
							result.add(originMember);
							continue;
						}
					}
				}
			} finally {
				locks.unlock(poolName);
			}
		}
		return result;
	}

	@Override
	public List<Member> findNewMembers(String poolName, List<Member> members)
			throws BizException, PoolNotFoundException {
		List<Member> result = new ArrayList<Member>();

		if (members != null && members.size() > 0) {
			locks.lock(poolName);
			try {
				Pool pool = poolService.findPool(poolName);

				if (pool == null) {
					throw new PoolNotFoundException(poolName);
				}

				List<Member> originMembers = pool.getMembers();

				for (Member member : members) {
					boolean exists = false;

					for (Member originMember : originMembers) {
						if (member.getName() != null && member.getName().equals(originMember.getName())) {
							exists = true;
							break;
						}
					}
					if (!exists) {
						result.add(member);
					}
				}
			} finally {
				locks.unlock(poolName);
			}
		}
		return result;
	}

}
