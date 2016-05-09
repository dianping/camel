package com.dianping.phoenix.lb.action;

import com.dianping.phoenix.lb.api.action.ApiV2;
import com.dianping.phoenix.lb.api.aspect.ApiAspect;
import com.dianping.phoenix.lb.api.aspect.ApiWrapper;
import com.dianping.phoenix.lb.exception.DebugModelException;
import com.dianping.phoenix.lb.model.entity.Member;
import com.dianping.phoenix.lb.service.model.PoolService;
import com.dianping.phoenix.lb.service.model.VirtualServerService;
import org.apache.commons.lang.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author leon.li <br>
 */
@Component("apiActionV2")
@Scope("prototype")
public class ApiActionV2 extends AbstractApiAction implements ApiV2 {

	private static final long serialVersionUID = 1L;
	private static final String TYPE = "API_CALL_V2";
	@Autowired
	protected VirtualServerService virtualServerService;
	@Autowired
	protected PoolService poolService;
	@Resource(name = "apiAspectV2")
	private ApiAspect m_apiAspect;

	public String addMember() throws Exception {
		m_apiAspect.doTransactionWithResultMap(TYPE, "addMember", new ApiWrapper() {
			@Override
			public void doAction() throws Exception {
				addMemberAction();
			}
		}, dataMap);
		return SUCCESS;
	}

	@Override
	public String addMemberAndDeploy() throws Exception {
		m_apiAspect.doTransactionWithResultMap(TYPE, "addMemberAndDeploy", new ApiWrapper() {
			@Override
			public void doAction() throws Exception {
				List<Member> requestMembers = addMemberAction();

				try {
					if (isDebugHeaderTrue()) {
						throw new DebugModelException();
					}
					deployAction();
				} catch (Exception ex) {
					List<String> memberNames = new ArrayList<String>();

					for (Member member : requestMembers) {
						memberNames.add(member.getName());
					}
					poolFacade.delMember(poolName, memberNames);
					throw ex;
				}
			}
		}, dataMap);
		return SUCCESS;
	}

	@Override
	public String delMember() throws Exception {
		m_apiAspect.doTransactionWithResultMap(TYPE, "delMember", new ApiWrapper() {
			@Override
			public void doAction() throws Exception {
				delMemberAction();
			}
		}, dataMap);
		return SUCCESS;
	}

	@Override
	public String delMemberAndDeploy() throws Exception {
		m_apiAspect.doTransactionWithResultMap(TYPE, "delMemberAndDeploy", new ApiWrapper() {
			@Override
			public void doAction() throws Exception {
				List<Member> originMembers = delMemberAction();

				try {
					if (isDebugHeaderTrue()) {
						throw new DebugModelException();
					}
					deployAction();
				} catch (Exception ex) {
					poolFacade.addMember(poolName, originMembers);
					throw ex;
				}
			}
		}, dataMap);
		return SUCCESS;
	}

	public String getPoolName() {
		return poolName;
	}

	public void setPoolName(String poolName) {
		this.poolName = poolName;
	}

	private boolean isDebugHeaderTrue() {
		String debugHeader = ServletActionContext.getRequest().getHeader("DEBUG");

		return !StringUtils.isEmpty(debugHeader) ? debugHeader.equals("true") : false;
	}

	@Override
	public String updateMember() throws Exception {
		m_apiAspect.doTransactionWithResultMap(TYPE, "updateMember", new ApiWrapper() {
			@Override
			public void doAction() throws Exception {
				updateMemberAction();
			}
		}, dataMap);
		return SUCCESS;
	}

	@Override
	public String updateMemberAndDeploy() throws Exception {
		m_apiAspect.doTransactionWithResultMap(TYPE, "updateMemberAndDeploy", new ApiWrapper() {
			@Override
			public void doAction() throws Exception {
				List<Member> originMembers = updateMemberAction();

				try {
					if (isDebugHeaderTrue()) {
						throw new DebugModelException();
					}
					deployAction();
				} catch (Exception ex) {
					poolFacade.updateMember(poolName, originMembers);
					throw ex;
				}
			}
		}, dataMap);
		return SUCCESS;
	}

	@Override
	public void validate() {
		super.validate();
	}

}
