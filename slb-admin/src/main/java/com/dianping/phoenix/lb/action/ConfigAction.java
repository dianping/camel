package com.dianping.phoenix.lb.action;

import com.dianping.phoenix.lb.action.ConfigAction.AgentPool.Agent;
import com.dianping.phoenix.lb.action.ConfigAction.AgentPool.Agent.VS;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.Instance;
import com.dianping.phoenix.lb.model.entity.SlbPool;
import com.dianping.phoenix.lb.model.entity.Strategy;
import com.dianping.phoenix.lb.model.entity.User;
import com.dianping.phoenix.lb.service.NginxService;
import com.dianping.phoenix.lb.service.model.SlbPoolService;
import com.dianping.phoenix.lb.service.model.StrategyService;
import com.dianping.phoenix.lb.service.model.UserService;
import com.dianping.phoenix.lb.service.model.VirtualServerService;
import com.dianping.phoenix.lb.utils.IOUtilsWrapper;
import com.dianping.phoenix.lb.utils.JsonBinder;
import com.opensymphony.xwork2.Action;
import org.apache.commons.io.IOUtils;
import org.apache.struts2.ServletActionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.unidal.lookup.util.StringUtils;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Component("configAction")
@Scope("session")
public class ConfigAction extends MenuAction {

	private static final long serialVersionUID = 1L;

	private static final String MENU = "config";

	private static final String SUB_MENU_AUTH = "auth";

	private static final String SUB_MENU_VS = "vs";

	private static final String SUB_MENU_STRATEGY = "strategy";

	private static final String THIRD_MENU_AUTH_ADMIN_LIST = "auth_admin_list";

	private static final String THIRD_MENU_VS_CLEAN_LIST = "vs_clean_list";

	private static final String THIRD_MENU_STRATEGY_LIST = "strategy_list";

	private String m_account;

	private String m_vs;

	private String m_host;

	private String m_vsPool;

	private String m_thirdMenu;

	private String m_strategyName;

	@Autowired
	private UserService m_userService;

	@Autowired
	private VirtualServerService m_vsService;

	@Autowired
	private NginxService m_nginxService;

	@Autowired
	private SlbPoolService m_slbPoolService;

	@Autowired
	private StrategyService m_strategyService;

	public String addMember() {
		try {
			String userJson = IOUtilsWrapper
					.convetStringFromRequest(ServletActionContext.getRequest().getInputStream());

			if (StringUtils.isEmpty(userJson)) {
				throw new IllegalArgumentException("user不能为空！");
			}
			User user = JsonBinder.getNonNullBinder().fromJson(userJson, User.class);

			m_userService.updateOrCreateUser(user);
			dataMap.put("errorCode", ERRORCODE_SUCCESS);
		} catch (Exception e) {
			dataMap.put("errorCode", ERRORCODE_INNER_ERROR);
			dataMap.put("errorMessage", e.getMessage());
			logger.error(e.getMessage(), e);
		}
		return Action.SUCCESS;
	}

	public String addStrategy() {
		try {
			String strategyJson = IOUtilsWrapper
					.convetStringFromRequest(ServletActionContext.getRequest().getInputStream());

			if (StringUtils.isEmpty(strategyJson)) {
				throw new IllegalArgumentException("strategy不能为空！");
			}

			Strategy strategy = JsonBinder.getNonNullBinder().fromJson(strategyJson, Strategy.class);
			String strategyName = strategy.getName();

			if (m_strategyService.findStrategy(strategyName) != null) {
				throw new RuntimeException("strategy name duplicated!");
			}
			m_strategyService.addStrategy(strategyName, strategy);
			dataMap.put("errorCode", ERRORCODE_SUCCESS);
		} catch (Exception e) {
			dataMap.put("errorCode", ERRORCODE_INNER_ERROR);
			dataMap.put("errorMessage", e.getMessage());
			logger.error(e.getMessage(), e);
		}
		return Action.SUCCESS;
	}

	public String editStrategy() {
		try {
			String strategyJson = IOUtilsWrapper
					.convetStringFromRequest(ServletActionContext.getRequest().getInputStream());

			if (StringUtils.isEmpty(strategyJson)) {
				throw new IllegalArgumentException("strategy不能为空！");
			}
			Strategy strategy = JsonBinder.getNonNullBinder().fromJson(strategyJson, Strategy.class);

			m_strategyService.modifyStrategy(strategy.getName(), strategy);
			dataMap.put("errorCode", ERRORCODE_SUCCESS);
		} catch (Exception e) {
			dataMap.put("errorCode", ERRORCODE_INNER_ERROR);
			dataMap.put("errorMessage", e.getMessage());
			logger.error(e.getMessage(), e);
		}
		return Action.SUCCESS;
	}

	public String deleteStrategy() {
		try {
			m_strategyService.deleteStrategy(m_strategyName);
			dataMap.put("errorCode", ERRORCODE_SUCCESS);
		} catch (Exception e) {
			dataMap.put("errorCode", ERRORCODE_INNER_ERROR);
			dataMap.put("errorMessage", e.getMessage());
			logger.error(e.getMessage(), e);
		}
		return Action.SUCCESS;
	}

	private void checkVSInvalid() throws IllegalStateException, BizException {
		if (m_vsService.listVSNames().contains(m_host)) {
			throw new IllegalStateException("db contains vs name " + m_host);
		}
	}

	private void checkVSInvalid(String vsName) throws IllegalStateException, BizException {
		if (m_vsService.listVSNames().contains(vsName)) {
			throw new IllegalStateException("db contains vs name " + vsName);
		}
	}

	public String deleteMember() {
		try {
			m_userService.removeUser(URLDecoder.decode(m_account, "UTF-8"));
			dataMap.put(ERROR_CODE, ERRORCODE_SUCCESS);
		} catch (Exception ex) {
			dataMap.put(ERROR_CODE, ERRORCODE_INNER_ERROR);
			dataMap.put(ERROR_MESSAGE, ex.getMessage());
		}
		return Action.SUCCESS;
	}

	public String batchDeleteVS() {
		try {
			String bodyContent = IOUtils.toString(ServletActionContext.getRequest().getInputStream());
			String[] vsNames = bodyContent.split(",");
			String host = URLDecoder.decode(m_host, "UTF-8");
			boolean result = true;

			for (String vsName : vsNames) {
				checkVSInvalid(vsName);
				result = result && m_nginxService.removeVS(host, URLDecoder.decode(vsName, "UTF-8"));
			}
			result = result && m_nginxService.reloadNginx(host);
			if (result) {
				dataMap.put(ERROR_CODE, ERRORCODE_SUCCESS);
			} else {
				throw new RuntimeException("execute result is not success!");
			}
		} catch (Exception ex) {
			dataMap.put(ERROR_CODE, ERRORCODE_INNER_ERROR);
			dataMap.put(ERROR_MESSAGE, ex.getMessage());
		}
		return Action.SUCCESS;
	}

	public String deleteVS() {
		try {
			checkVSInvalid();

			String host = URLDecoder.decode(m_host, "UTF-8");
			boolean result = true;

			result = result && m_nginxService.removeVS(host, URLDecoder.decode(m_vs, "UTF-8"));
			result = result && m_nginxService.reloadNginx(host);
			if (result) {
				dataMap.put(ERROR_CODE, ERRORCODE_SUCCESS);
			} else {
				throw new RuntimeException("execute result is not success!");
			}
		} catch (Exception ex) {
			dataMap.put(ERROR_CODE, ERRORCODE_INNER_ERROR);
			dataMap.put(ERROR_MESSAGE, ex.getMessage());
		}
		return Action.SUCCESS;
	}

	public String batchDeleteVSByPool() {
		try {
			String bodyContent = IOUtils.toString(ServletActionContext.getRequest().getInputStream());
			String[] vsNames = bodyContent.split(",");
			SlbPool slbPool = m_slbPoolService.findSlbPool(m_vsPool);
			boolean result = true;

			for (String vsName : vsNames) {
				checkVSInvalid(vsName);
			}
			for (Instance instance : slbPool.getInstances()) {
				String host = instance.getIp();

				for (String vsName : vsNames) {
					result = result && m_nginxService.removeVS(host, URLDecoder.decode(vsName, "UTF-8"));
				}
				result = result && m_nginxService.reloadNginx(host);
			}
			if (result) {
				dataMap.put(ERROR_CODE, ERRORCODE_SUCCESS);
			} else {
				throw new RuntimeException("execute result is not success!");
			}
		} catch (Exception ex) {
			dataMap.put(ERROR_CODE, ERRORCODE_INNER_ERROR);
			dataMap.put(ERROR_MESSAGE, ex.getMessage());
		}
		return Action.SUCCESS;
	}

	public String deleteVSByPool() {
		try {
			boolean result = true;

			checkVSInvalid();
			for (Instance instance : m_slbPoolService.findSlbPool(m_vsPool).getInstances()) {
				String host = instance.getIp();

				result = result && m_nginxService.removeVS(host, URLDecoder.decode(m_vs, "UTF-8"));
				result = result && m_nginxService.reloadNginx(host);
			}
			if (result) {
				dataMap.put(ERROR_CODE, ERRORCODE_SUCCESS);
			} else {
				throw new RuntimeException("execute result is not success!");
			}
		} catch (Exception ex) {
			dataMap.put(ERROR_CODE, ERRORCODE_INNER_ERROR);
			dataMap.put(ERROR_MESSAGE, ex.getMessage());
		}
		return Action.SUCCESS;
	}

	public String getAccount() {
		return m_account;
	}

	public void setAccount(String account) {
		m_account = account;
	}

	public String getHost() {
		return m_host;
	}

	public void setHost(String host) {
		m_host = host;
	}

	public String getThirdMenu() {
		return m_thirdMenu;
	}

	public void setThirdMenu(String thirdMenu) {
		m_thirdMenu = thirdMenu;
	}

	public String getVs() {
		return m_vs;
	}

	public void setVs(String vs) {
		m_vs = vs;
	}

	public String getVsPool() {
		return m_vsPool;
	}

	public void setVsPool(String vsPool) {
		m_vsPool = vsPool;
	}

	public String getStrategyName() {
		return m_strategyName;
	}

	public void setStrategyName(String strategyName) {
		this.m_strategyName = strategyName;
	}

	public String index() {
		setSubMenu(SUB_MENU_AUTH);
		setThirdMenu(THIRD_MENU_AUTH_ADMIN_LIST);
		return Action.SUCCESS;
	}

	public String listMember() {
		dataMap.put("users", m_userService.listUsers());
		dataMap.put("errorCode", ERRORCODE_SUCCESS);
		return Action.SUCCESS;
	}

	public String listUncleanVS() {
		List<SlbPool> slbPools = m_slbPoolService.listSlbPools();
		List<AgentPool> agentPools = new ArrayList<AgentPool>();
		boolean isAnySlbPoolSuccess = false;
		String errorMessage = "";

		for (SlbPool slbPool : slbPools) {
			try {
				String poolName = slbPool.getName();
				List<Agent> agents = new ArrayList<Agent>();
				AgentPool agentPool = new AgentPool(poolName, agents);
				Set<VS> commonVSNames = new HashSet<VS>();
				List<Agent> rawAgents = new ArrayList<Agent>();
				boolean isFirstInstance = true;

				for (Instance instance : slbPool.getInstances()) {
					String host = instance.getIp();
					Set<String> vsNames = m_vsService.findUndeleteDengineVSNames(host);
					Set<VS> vsSet = new HashSet<VS>();
					Agent agent = agentPool.new Agent(host, vsSet);

					for (String vsName : vsNames) {
						vsSet.add(agent.new VS(vsName));
					}
					if (isFirstInstance) {
						isFirstInstance = false;
						commonVSNames.addAll(vsSet);
					} else {
						commonVSNames.retainAll(vsSet);
					}
					rawAgents.add(agent);
				}
				if (commonVSNames.size() > 0) {
					agents.add(agentPool.new Agent("COMMON", commonVSNames));
				}
				for (Agent rawAgent : rawAgents) {
					Set<VS> vsList = rawAgent.getVsList();

					vsList.removeAll(commonVSNames);
					if (vsList.size() > 0) {
						agents.add(rawAgent);
					}
				}
				agentPools.add(agentPool);
				isAnySlbPoolSuccess = true;
			} catch (Exception ex) {
				errorMessage = ex.getMessage();
				logger.error("list unuse vs dir fail:" + slbPool, ex);
			}
		}
		if (isAnySlbPoolSuccess) {
			dataMap.put("agentPools", agentPools);
			dataMap.put("errorCode", ERRORCODE_SUCCESS);
		} else {
			dataMap.put("errorCode", ERRORCODE_INNER_ERROR);
			dataMap.put("errorMessage", errorMessage);
		}
		return Action.SUCCESS;
	}

	public String listStrategies() {
		dataMap.put("strategyList", m_strategyService.listStrategies());
		dataMap.put("errorCode", ERRORCODE_SUCCESS);
		return Action.SUCCESS;
	}

	public String memberIndex() {
		setSubMenu(SUB_MENU_AUTH);
		setThirdMenu(THIRD_MENU_AUTH_ADMIN_LIST);
		return Action.SUCCESS;
	}

	public String updateMember() {
		try {
			m_userService.removeUser(URLDecoder.decode(m_account, "UTF-8"));

			String userJson = IOUtilsWrapper
					.convetStringFromRequest(ServletActionContext.getRequest().getInputStream());

			if (StringUtils.isEmpty(userJson)) {
				throw new IllegalArgumentException("user不能为空！");
			}
			User user = JsonBinder.getNonNullBinder().fromJson(userJson, User.class);

			m_userService.updateOrCreateUser(user);
			dataMap.put("errorCode", ERRORCODE_SUCCESS);
		} catch (Exception e) {
			dataMap.put("errorCode", ERRORCODE_INNER_ERROR);
			dataMap.put("errorMessage", e.getMessage());
			logger.error(e.getMessage(), e);
		}
		return Action.SUCCESS;
	}

	@Override
	public void validate() {
		super.validate();
		setMenu(MENU);
		if (getSubMenu() == null) {
			setSubMenu(SUB_MENU_AUTH);
			setThirdMenu(THIRD_MENU_AUTH_ADMIN_LIST);
		}
	}

	public String vsCleanIndex() {
		setSubMenu(SUB_MENU_VS);
		setThirdMenu(THIRD_MENU_VS_CLEAN_LIST);
		return Action.SUCCESS;
	}

	public String strategyIndex() {
		setSubMenu(SUB_MENU_STRATEGY);
		setThirdMenu(THIRD_MENU_STRATEGY_LIST);
		return Action.SUCCESS;
	}

	public class AgentPool {

		private String m_poolName;

		private List<Agent> m_agents;

		public AgentPool(String poolName, List<Agent> agents) {
			this.m_poolName = poolName;
			this.m_agents = agents;
		}

		public List<Agent> getAgents() {
			return m_agents;
		}

		public void setAgents(List<Agent> agents) {
			m_agents = agents;
		}

		public String getPoolName() {
			return m_poolName;
		}

		public void setPoolName(String poolName) {
			m_poolName = poolName;
		}

		public class Agent {

			private String m_host;

			private Set<VS> m_vsList;

			public Agent(String host, Set<VS> vsList) {
				this.m_host = host;
				this.m_vsList = vsList;
			}

			public String getHost() {
				return m_host;
			}

			public void setHost(String host) {
				this.m_host = host;
			}

			public Set<VS> getVsList() {
				return m_vsList;
			}

			public void setVsList(Set<VS> vsList) {
				this.m_vsList = vsList;
			}

			public class VS {

				private String m_name;

				private boolean m_selected;

				public VS(String name) {
					m_name = name;
					m_selected = false;
				}

				public String getName() {
					return m_name;
				}

				public void setName(String name) {
					m_name = name;
				}

				public boolean isSelected() {
					return m_selected;
				}

				public void setSelected(boolean selected) {
					m_selected = selected;
				}

				@Override
				public int hashCode() {
					return m_name.hashCode();
				}

				@Override
				public boolean equals(Object o) {
					if (o instanceof VS) {
						return m_name.equals(((VS) o).getName());
					}
					return false;
				}

			}

		}

	}

}
