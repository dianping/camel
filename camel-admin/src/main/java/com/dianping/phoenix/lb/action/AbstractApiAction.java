package com.dianping.phoenix.lb.action;

import com.dianping.phoenix.lb.constant.MessageID;
import com.dianping.phoenix.lb.deploy.bo.api.DeployTaskApiBo;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.facade.*;
import com.dianping.phoenix.lb.facade.impl.PoolFacadeImpl;
import com.dianping.phoenix.lb.model.entity.Member;
import com.dianping.phoenix.lb.utils.IOUtilsWrapper;
import com.dianping.phoenix.lb.utils.JsonBinder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.struts2.ServletActionContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public abstract class AbstractApiAction extends MenuAction {

	private static final long INVALID_TASK_ID = -1;

	protected String poolName;

	@Autowired
	protected PoolFacade<DeployTaskApiBo> poolFacade;

	@Autowired
	protected ApiDeployExecutor apiDeployExecutor;

	@SuppressWarnings("unchecked")
	//支持强制执行方式：当存在http头为Forced: true时，则不进行member可用率检查
	protected List<Member> addMemberAction()
			throws IOException, PoolNotFoundException, PoolAvailableRateException, BizException {
		if (logger.isInfoEnabled()) {
			logger.info("[addMember][pool]" + poolName);
		}
		Validate.isTrue(StringUtils.equalsIgnoreCase(ServletActionContext.getRequest().getMethod(), "POST"),
				"Only allow POST method.");

		Validate.notEmpty(poolName, "Pool name cannot be empty.");

		String membersJson = IOUtilsWrapper.convetStringFromRequest(ServletActionContext.getRequest().getInputStream());
		if (StringUtils.isBlank(membersJson)) {
			throw new IllegalArgumentException("member parameter cannot be empty.");
		}

		List<Member> requestMembers = JsonBinder.getNonNullBinder().fromJson(membersJson, List.class, Member.class);
		List<Member> newMembers = poolFacade.findNewMembers(poolName, requestMembers);

		if (logger.isInfoEnabled()) {
			logger.info("[addMember][members]" + newMembers);
		}

		PoolFacadeImpl.validateMember(newMembers);

		if (isRequestForced()) {
			poolFacade.addMemberForced(poolName, newMembers);
		} else {
			poolFacade.addMember(poolName, newMembers);
		}
		return newMembers;
	}

	//支持强制执行方式：当存在http头为Forced: true时，则不进行member可用率检查
	@SuppressWarnings("unchecked")
	protected List<Member> delMemberAction()
			throws IOException, PoolAvailableRateException, PoolNotFoundException, MemberExceedException,
			NotSuccessException, BizException {
		if (logger.isInfoEnabled()) {
			logger.info("[delMember][pool]" + poolName);
		}
		Validate.isTrue(StringUtils.equalsIgnoreCase(ServletActionContext.getRequest().getMethod(), "POST"),
				"Only allow POST method.");

		Validate.notEmpty(poolName, "Pool name cannot be empty.");

		String membersJson = IOUtilsWrapper.convetStringFromRequest(ServletActionContext.getRequest().getInputStream());
		if (StringUtils.isBlank(membersJson)) {
			throw new IllegalArgumentException("member parameter cannot be empty.");
		}

		List<String> members = JsonBinder.getNonNullBinder().fromJson(membersJson, List.class, String.class);
		List<Member> memberObjects = generateMembers(members);
		List<Member> originMembers = poolFacade.findCorrespondMembersByNameOrIp(poolName, memberObjects);

		if (logger.isInfoEnabled()) {
			logger.info("[delMember][members]" + members);
		}
		if (isRequestForced()) {
			poolFacade.delMemberForced(poolName, members);
		} else {
			poolFacade.delMember(poolName, members);
		}
		return originMembers;
	}

	protected void deployAction() throws Exception {
		Validate.isTrue(StringUtils.equalsIgnoreCase(ServletActionContext.getRequest().getMethod(), "POST"),
				"Only allow POST method.");

		Validate.notEmpty(poolName, "Pool name cannot be empty.");

		if (logger.isInfoEnabled()) {
			logger.info("[deploy][pool]" + poolName);
		}

		long startTime = System.currentTimeMillis();
		long taskId = INVALID_TASK_ID;
		try {
			taskId = deployPool(poolName);
		} finally {
			if (logger.isInfoEnabled()) {
				logger.info("[deploy][pool]" + poolName + "," + taskId + ", cost time:" + (System.currentTimeMillis()
						- startTime));
			}
		}
	}

	protected long deployPool(String poolName) throws Exception {
		DeployResult deployResult = apiDeployExecutor.execute(poolName);

		if (!deployResult.isSuccess()) {
			throw deployResult.getException();
		}

		// 成功，但是没有关联vs
		if (deployResult.getException() != null && deployResult.getException() instanceof BizException) {
			BizException bz = (BizException) deployResult.getException();
			if (bz.getMessageId() == MessageID.DEPLOY_POOL_NOT_RELATED_VS) {
				if (logger.isInfoEnabled()) {
					logger.info("[deploy][DEPLOY_POOL_NOT_RELATED_VS]");
				}
				return INVALID_TASK_ID;
			}
		}

		DeployTaskApiBo deployTaskBo = deployResult.getDeployTaskApiBo();

		long taskId = deployTaskBo.getTask().getId();
		dataMap.put("taskId", taskId);
		return taskId;
	}

	private List<Member> generateMembers(List<String> memberNames) {
		List<Member> members = new ArrayList<Member>();

		for (String name : memberNames) {
			members.add(new Member(name));
		}
		return members;
	}

	//支持强制执行方式：当存在http头为Forced: true时，则不进行member可用率检查
	@SuppressWarnings("unchecked")
	protected List<Member> updateMemberAction()
			throws IOException, PoolNotFoundException, PoolAvailableRateException, MemberNotFoundException,
			BizException {
		if (logger.isInfoEnabled()) {
			logger.info("[updateMember][pool]" + poolName);
		}

		Validate.notEmpty(poolName, "Pool name cannot be empty.");

		String membersJson = IOUtilsWrapper.convetStringFromRequest(ServletActionContext.getRequest().getInputStream());
		if (StringUtils.isBlank(membersJson)) {
			throw new IllegalArgumentException("member parameter cannot be empty.");
		}

		List<Member> requestMembers = JsonBinder.getNonNullBinder().fromJson(membersJson, List.class, Member.class);

		if (logger.isInfoEnabled()) {
			logger.info("[updateMember][members]" + requestMembers);
		}
		List<Member> originMembers = poolFacade.findCorrespondMembersByNameOrIp(poolName, requestMembers);

		if (isRequestForced()) {
			poolFacade.updateMemberForced(poolName, requestMembers);
		} else {
			poolFacade.updateMember(poolName, requestMembers);
		}
		return originMembers;
	}

	// use for debug
	private boolean isRequestForced() {
		String forcedHeader = ServletActionContext.getRequest().getHeader("Forced");

		if (StringUtils.isNotEmpty(forcedHeader)) {
			return "true".equals(forcedHeader.trim());
		}
		return false;
	}

	@Override
	public void validate() {
		super.validate();
	}

}
