package com.dianping.phoenix.lb.action;

import com.dianping.phoenix.lb.constant.Constants;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.InfluencingVs;
import com.dianping.phoenix.lb.model.State;
import com.dianping.phoenix.lb.model.entity.*;
import com.dianping.phoenix.lb.service.model.PoolService;
import com.dianping.phoenix.lb.service.model.PoolServiceImpl.MemberModifier;
import com.dianping.phoenix.lb.service.model.VirtualServerService;
import com.dianping.phoenix.lb.utils.IOUtilsWrapper;
import com.dianping.phoenix.lb.utils.JsonBinder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.struts2.ServletActionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author wukezhu
 */
@Component("poolAction")
@Scope("prototype")
public class PoolAction extends MenuAction {

	private static final Logger logger = LoggerFactory.getLogger(PoolAction.class);

	private static final long serialVersionUID = -1084994778030229218L;

	private static final String MENU = "pool";
	protected List<Pool> pools;
	@Autowired
	protected PoolService poolService;
	@Autowired
	protected VirtualServerService virtualServerService;
	private String poolName;
	private Boolean showInfluencing;

	private static List<Directive> findDirectionsRelative(List<Directive> directives, String upstreamName) {
		List<Directive> list = new ArrayList<Directive>();
		for (Directive directive : directives) {
			String type = directive.getType();
			if (Constants.DIRECTIVE_PROXY_PASS.equalsIgnoreCase(type) && upstreamName
					.equals(directive.getDynamicAttribute(Constants.DIRECTIVE_PROXY_PASS_POOL_NAME))) {
				list.add(directive);
			} else if (Constants.DIRECTIVE_PROXY_IFELSE.equalsIgnoreCase(type) && (
					StringUtils.contains(directive.getDynamicAttribute("if-statement"), upstreamName) || StringUtils
							.contains(directive.getDynamicAttribute("else-statement"), upstreamName))) {
				list.add(directive);
			} else if ("custom".equalsIgnoreCase(type) && StringUtils
					.contains(directive.getDynamicAttribute("value"), upstreamName)) {
				list.add(directive);
			}
		}
		return list;
	}

	public String show() {
		pools = poolService.listPools();
		editOrShow = "show";
		return SUCCESS;
	}

	public String edit() {
		pools = poolService.listPools();
		editOrShow = "edit";
		return SUCCESS;
	}

	public List<Pool> getPools() {
		return pools;
	}

	public String index() {
		pools = poolService.listPools();
		if (pools.size() == 0) {
			return "noneVs";
		}
		poolName = pools.get(0).getName();//重定向
		return "redirect";
	}

	public String listPools() {
		pools = poolService.listPools();
		return SUCCESS;
	}

	public String get() throws Exception {
		try {
			//获取pool
			Pool pool = poolService.findPool(poolName);

			//该pool影响的vs
			if (showInfluencing != null && showInfluencing) {
				List<String> influencingVsList = virtualServerService.findVirtualServerByPool(poolName);
				if (influencingVsList != null && influencingVsList.size() > 0) {
					dataMap.put("influencingVsList", influencingVsList);
				}
			}

			dataMap.put("pool", pool);
			dataMap.put("errorCode", ERRORCODE_SUCCESS);
		} catch (BizException e) {
			dataMap.put("errorCode", e.getMessageId());
			dataMap.put("errorMessage", e.getMessage());
			logger.error("Bussiness Error: " + e.getMessage());
		} catch (IllegalArgumentException e) {
			dataMap.put("errorCode", ERRORCODE_PARAM_ERROR);
			dataMap.put("errorMessage", e.getMessage());
			logger.error("Param Error: " + e.getMessage());
		} catch (Exception e) {
			dataMap.put("errorCode", ERRORCODE_INNER_ERROR);
			dataMap.put("errorMessage", e.getMessage());
			logger.error(e.getMessage(), e);
		}
		return SUCCESS;
	}

	public String save() throws Exception {
		try {
			String poolJson = IOUtilsWrapper
					.convetStringFromRequest(ServletActionContext.getRequest().getInputStream());
			if (StringUtils.isBlank(poolJson)) {
				throw new IllegalArgumentException("pool 参数不能为空！");
			}
			Pool pool = JsonBinder.getNonNullBinder().fromJson(poolJson, Pool.class);

			String poolName = pool.getName();
			Pool pool0 = poolService.findPool(poolName);
			if (pool0 != null) {
				List<Member> originMembers = pool0.getMembers();
				List<Member> members = pool.getMembers();
				int originPoolEnableCount = calEnableMemberCount(originMembers);
				int poolEnableCount = calEnableMemberCount(members);

				if (poolEnableCount > originPoolEnableCount) {
					poolService.modifyPool(poolName, pool, MemberModifier.ADD);
				} else if (poolEnableCount < originPoolEnableCount) {
					poolService.modifyPool(poolName, pool, MemberModifier.DELETE);
				} else {
					int dbMemberSize = originMembers.size();
					int jsonMemberSize = members.size();

					if (jsonMemberSize > dbMemberSize) {
						poolService.modifyPool(poolName, pool, MemberModifier.ADD);
					} else if (jsonMemberSize < dbMemberSize) {
						poolService.modifyPool(poolName, pool, MemberModifier.DELETE);
					} else {
						poolService.modifyPool(poolName, pool, MemberModifier.DEFAULT);
					}
				}
			} else {
				poolService.addPool(poolName, pool);
			}

			dataMap.put("errorCode", ERRORCODE_SUCCESS);
		} catch (BizException e) {
			dataMap.put("errorCode", e.getMessageId());
			dataMap.put("errorMessage", e.getMessage());
			logger.error("Bussiness Error: " + e.getMessage());
		} catch (IllegalArgumentException e) {
			dataMap.put("errorCode", ERRORCODE_PARAM_ERROR);
			dataMap.put("errorMessage", e.getMessage());
			logger.error("Param Error: " + e.getMessage());
		} catch (Exception e) {
			dataMap.put("errorCode", ERRORCODE_INNER_ERROR);
			dataMap.put("errorMessage", e.getMessage());
			logger.error(e.getMessage(), e);
		}
		return SUCCESS;
	}

	private int calEnableMemberCount(List<Member> members) {
		int enableMemberCount = 0;

		for (Member member : members) {
			if (member.getState() == State.ENABLED) {
				enableMemberCount++;
			}
		}
		return enableMemberCount;
	}

	public String remove() throws Exception {
		try {
			Pool pool0 = poolService.findPool(poolName);
			if (pool0 == null) {
				throw new IllegalArgumentException("不存在该站点：" + poolName);
			}
			virtualServerService.findVirtualServerByPool(poolName);

			// 验证没有vs依赖pool
			List<String> influencingVsList = virtualServerService.findVirtualServerByPool(poolName);
			Validate.isTrue((influencingVsList == null || influencingVsList.size() <= 0),
					"Can't delete this pool, because these VirtualServers use it:" + influencingVsList);
			poolService.deletePool(poolName);
			dataMap.put("errorCode", ERRORCODE_SUCCESS);
		} catch (BizException e) {
			dataMap.put("errorCode", e.getMessageId());
			dataMap.put("errorMessage", e.getMessage());
			logger.error("Bussiness Error: " + e.getMessage());
		} catch (IllegalArgumentException e) {
			dataMap.put("errorCode", ERRORCODE_PARAM_ERROR);
			dataMap.put("errorMessage", e.getMessage());
			logger.error("Param Error: " + e.getMessage());
		} catch (Exception e) {
			dataMap.put("errorCode", ERRORCODE_INNER_ERROR);
			dataMap.put("errorMessage", e.getMessage());
			logger.error(e.getMessage(), e);
		}
		return SUCCESS;
	}

	public String influencingVsList() throws Exception {
		try {
			List<InfluencingVs> re = new ArrayList<InfluencingVs>();

			// 找到所有影响到的vs, 找到涉及oldPoolName的地方（defaultPool或directive），把oldPoolName改为newPoolName
			List<String> virtualServers = virtualServerService.findVirtualServerByPool(poolName);

			if (virtualServers != null) {
				for (String vsName : virtualServers) {
					Set<String> positionDescs = new HashSet<String>();

					VirtualServer virtualServer = virtualServerService.findVirtualServer(vsName);

					String defaultPoolName = virtualServer.getDefaultPoolName();
					if (defaultPoolName.equals(poolName)) {
						positionDescs.add("默认集群");

					}

					List<Location> locations = virtualServer.getLocations();
					for (Location location : locations) {
						List<Directive> directives = findDirectionsRelative(location.getDirectives(), poolName);
						if (directives != null && directives.size() > 0) {
							positionDescs.add(location.getPattern());
						}
					}

					InfluencingVs influencingVs = new InfluencingVs();
					influencingVs.setVsName(vsName);
					influencingVs.setPositionDescs(new ArrayList<String>(positionDescs));
					re.add(influencingVs);
				}
			}

			dataMap.put("influencingVsList", re);

			dataMap.put("errorCode", ERRORCODE_SUCCESS);
		} catch (BizException e) {
			dataMap.put("errorCode", e.getMessageId());
			dataMap.put("errorMessage", e.getMessage());
			logger.error("Bussiness Error: " + e.getMessage());
		} catch (IllegalArgumentException e) {
			dataMap.put("errorCode", ERRORCODE_PARAM_ERROR);
			dataMap.put("errorMessage", e.getMessage());
			logger.error("Param Error: " + e.getMessage());
		} catch (Exception e) {
			dataMap.put("errorCode", ERRORCODE_INNER_ERROR);
			dataMap.put("errorMessage", e.getMessage());
			logger.error(e.getMessage(), e);
		}
		return SUCCESS;
	}

	public String getPoolName() {
		return poolName;
	}

	public void setPoolName(String poolName) {
		this.poolName = poolName;
	}

	public Boolean getShowInfluencing() {
		return showInfluencing;
	}

	public void setShowInfluencing(Boolean showInfluencing) {
		this.showInfluencing = showInfluencing;
	}

	@Override
	public void validate() {
		super.validate();
		this.setMenu(MENU);
	}
}
