package com.dianping.phoenix.lb.facade;

import com.dianping.phoenix.lb.deploy.executor.TaskExecutor;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.Member;
import com.dianping.phoenix.lb.model.entity.Pool;

import java.util.List;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface PoolFacade<T> {

	void addPool(Pool pool) throws BizException, PoolAvailableRateException;

	void forceState(String poolName, int forceState) throws BizException, PoolNotFoundException;

	void addMember(String poolName, List<Member> members)
			throws BizException, PoolNotFoundException, PoolAvailableRateException;

	void addMemberForced(String poolName, List<Member> members)
			throws BizException, PoolNotFoundException, PoolAvailableRateException;

	void updateMember(String poolName, List<Member> members)
			throws BizException, PoolNotFoundException, PoolAvailableRateException, MemberNotFoundException;

	void updateMemberForced(String poolName, List<Member> members)
			throws BizException, PoolNotFoundException, PoolAvailableRateException, MemberNotFoundException;

	void delMember(String poolName, List<String> memberNames)
			throws BizException, PoolAvailableRateException, PoolNotFoundException, MemberExceedException,
			NotSuccessException;

	void delMemberForced(String poolName, List<String> memberNames)
			throws BizException, PoolAvailableRateException, PoolNotFoundException, MemberExceedException,
			NotSuccessException;

	List<Member> findCorrespondMembersByNameOrIp(String poolName, List<Member> members)
			throws BizException, PoolNotFoundException;

	List<Member> findNewMembers(String poolName, List<Member> members) throws BizException, PoolNotFoundException;

	TaskExecutor<T> deploy(String poolName) throws BizException;

	TaskExecutor<T> deploy(List<String> poolNames) throws BizException;

}
