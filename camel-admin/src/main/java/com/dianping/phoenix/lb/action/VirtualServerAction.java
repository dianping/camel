package com.dianping.phoenix.lb.action;

import com.dianping.phoenix.lb.constant.MessageID;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.VirtualServerGroup;
import com.dianping.phoenix.lb.model.entity.*;
import com.dianping.phoenix.lb.service.NginxService;
import com.dianping.phoenix.lb.service.model.*;
import com.dianping.phoenix.lb.utils.IOUtilsWrapper;
import com.dianping.phoenix.lb.utils.JsonBinder;
import freemarker.template.utility.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.struts2.ServletActionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Component("virtualServerAction")
@Scope("prototype")
public class VirtualServerAction extends MenuAction {

	private static final Logger logger = LoggerFactory.getLogger(VirtualServerAction.class);
	private static final long serialVersionUID = -1084994778030229218L;
	private static final String MENU = "vs";
	private static int tagNum = 10;
	protected Map<String, VirtualServerGroup> groups;
	@Autowired
	protected PoolService poolService;
	@Autowired
	protected VariableService variableService;
	@Autowired
	protected VirtualServerService virtualServerService;
	@Autowired
	protected StrategyService strategyService;
	private String virtualServerName;
	private String tagId;
	private Integer version;
	private List<String> tags;
	private List<VirtualServer> list;
	private String[] vsListToTag;
	private String vsListToTagStr;
	private String tagIdsStr;
	@Autowired
	private SlbPoolService m_slbPoolService;

	@Autowired
	private NginxService m_nginxService;

	@Autowired
	private CommonAspectService m_aspectService;

	private List<Pattern> m_poolPatterns = Arrays.asList(Pattern.compile("proxy_pass\\s+http://([^$]+?);"),
			Pattern.compile("proxy_pass\\s+http://([^$]+?)"));

	public String index() {
		groups = virtualServerService.listGroups();
		VirtualServer vs = null;
		for (VirtualServerGroup group : groups.values()) {
			List<VirtualServer> vsList = group.getVirtualServers();
			if (vsList != null) {
				for (VirtualServer item : vsList) {
					vs = item;
					break;
				}
			}
			if (vs != null) {
				break;
			}
		}
		if (vs == null) {
			return "noneVs";
		}
		virtualServerName = vs.getName();// 重定向
		return "redirect";
	}

	public String show() {
		groups = virtualServerService.listGroups();
		editOrShow = "show";
		return SUCCESS;
	}

	public String edit() {
		groups = virtualServerService.listGroups();
		editOrShow = "edit";
		return SUCCESS;
	}

	public String get() throws Exception {
		try {
			// 获取vs
			VirtualServer virtualServer = virtualServerService.findVirtualServer(virtualServerName);
			dataMap.put("virtualServer", virtualServer);
			dataMap.put("errorCode", ERRORCODE_SUCCESS);
		} catch (BizException e) {
			dataMap.put("errorCode", e.getMessageId());
			dataMap.put("errorMessage", e.getMessage());
		} catch (IllegalArgumentException e) {
			dataMap.put("errorCode", ERRORCODE_PARAM_ERROR);
			dataMap.put("errorMessage", e.getMessage());
		} catch (Exception e) {
			dataMap.put("errorCode", ERRORCODE_INNER_ERROR);
			dataMap.put("errorMessage", e.getMessage());
			logger.error(e.getMessage(), e);
		}
		return SUCCESS;
	}

	public String save() throws Exception {
		try {
			String vsJson = IOUtilsWrapper.convetStringFromRequest(ServletActionContext.getRequest().getInputStream());
			if (StringUtils.isBlank(vsJson)) {
				throw new IllegalArgumentException("vs 参数不能为空！");
			}
			VirtualServer virtualServer = JsonBinder.getNonNullBinder().fromJson(vsJson, VirtualServer.class);

			checkVS(virtualServer);
			String virtualServerName = virtualServer.getName();
			VirtualServer virtualServer0 = virtualServerService.findVirtualServer(virtualServerName);
			if (virtualServer0 != null) {
				virtualServerService.modifyVirtualServer(virtualServerName, virtualServer);
			} else {
				virtualServerService.addVirtualServer(virtualServerName, virtualServer);
			}
			dataMap.put("errorCode", ERRORCODE_SUCCESS);
		} catch (BizException e) {
			dataMap.put("errorCode", e.getMessageId());
			dataMap.put("errorMessage", e.getMessage());
		} catch (IllegalArgumentException e) {
			dataMap.put("errorCode", ERRORCODE_PARAM_ERROR);
			dataMap.put("errorMessage", e.getMessage());
		} catch (Exception e) {
			dataMap.put("errorCode", ERRORCODE_INNER_ERROR);
			dataMap.put("errorMessage", e.getMessage());
			logger.error(e.getMessage(), e);
		}
		return SUCCESS;
	}

	private void checkVS(VirtualServer vs) throws BizException {
		boolean result = true;
		StringBuilder builder = new StringBuilder(100);

		builder.append("请使用$dp_domain $dp_upstream!非法参数:\t");
		for (Location location : vs.getLocations()) {
			result &= checkDirectives("location:" + location.getPattern(), location.getDirectives(), builder);
		}
		for (Aspect aspect : vs.getAspects()) {
			String name = aspect.getName();

			if (StringUtils.isNotEmpty(name)) {
				List<Directive> paramDirections = aspect.getDirectives();

				if (paramDirections != null && paramDirections.size() > 0) {
					result &= checkDirectives("aspect:" + name, paramDirections, builder);
				}
			} else {
				String aspectRef = aspect.getRef();
				Aspect realAspect = m_aspectService.findCommonAspect(aspectRef);

				if (realAspect != null) {
					result &= checkDirectives("aspect:" + aspectRef, realAspect.getDirectives(), builder);
				}
			}
		}
		if (!result) {
			throw new BizException(MessageID.VIRTUALSERVER_CHECK_FAILED, builder.toString());
		}
	}

	private boolean checkDirectives(String prefix, List<Directive> directives, StringBuilder builder) {
		boolean result = true;

		for (Directive directive : directives) {
			for (Entry<String, String> pair : directive.getDynamicAttributes().entrySet()) {
				String name = pair.getKey();
				String value = pair.getValue();

				if (value.contains("proxy_pass")) {
					try {
						String poolName = extractPool(value);

						if (StringUtils.isNotEmpty(poolName) && poolName != "$dp_upstream"
								&& poolService.findPool(poolName) != null) {
							result = false;
							builder.append(prefix).append(" ").append("directive:").append(name).append(";\t");
						}
					} catch (Exception ex) {
						// ignore exception
					}
				}
			}
		}
		return result;
	}

	private String extractPool(String value) {
		for (Pattern pattern : m_poolPatterns) {
			Matcher matcher = pattern.matcher(value);

			if (matcher.find()) {
				return matcher.group(1).trim();
			}
		}
		return null;
	}

	public String remove() throws Exception {
		try {
			VirtualServer virtualServer0 = virtualServerService.findVirtualServer(virtualServerName);
			if (virtualServer0 == null) {
				throw new IllegalArgumentException("不存在该站点：" + virtualServerName);
			}

			virtualServerService.deleteVirtualServer(virtualServerName);

			SlbPool slbPool = m_slbPoolService.findSlbPool(virtualServer0.getSlbPool());

			deleteNginxConfDir(slbPool, virtualServerName);
			dataMap.put("errorCode", ERRORCODE_SUCCESS);
		} catch (BizException e) {
			dataMap.put("errorCode", e.getMessageId());
			dataMap.put("errorMessage", e.getMessage());
		} catch (IllegalArgumentException e) {
			dataMap.put("errorCode", ERRORCODE_PARAM_ERROR);
			dataMap.put("errorMessage", e.getMessage());
		} catch (Exception e) {
			dataMap.put("errorCode", ERRORCODE_INNER_ERROR);
			dataMap.put("errorMessage", e.getMessage());
			logger.error(e.getMessage(), e);
		}
		return SUCCESS;
	}

	private void deleteNginxConfDir(SlbPool slbPool, String virtualServerName)
			throws UnsupportedEncodingException, BizException {
		for (Instance instance : slbPool.getInstances()) {
			String ip = instance.getIp();

			m_nginxService.removeVS(ip, URLDecoder.decode(virtualServerName, "UTF-8"));
			m_nginxService.reloadNginx(ip);
		}
	}

	public String preview() throws Exception {
		try {
			String vsJson = IOUtilsWrapper.convetStringFromRequest(ServletActionContext.getRequest().getInputStream());
			if (StringUtils.isBlank(vsJson)) {
				throw new IllegalArgumentException("vs 参数不能为空！");
			}
			VirtualServer virtualServer = JsonBinder.getNonNullBinder().fromJson(vsJson, VirtualServer.class);

			List<Pool> pools = poolService.listPools();
			List<Variable> variables = variableService.listVariables();
			commonAspects = commonAspectService.listCommonAspects();
			String nginxConfig = virtualServerService
					.generateNginxConfig(virtualServer, pools, commonAspects, variables,
							strategyService.listStrategies());

			dataMap.put("nginxConfig", nginxConfig);
			dataMap.put("errorCode", ERRORCODE_SUCCESS);
		} catch (BizException e) {
			dataMap.put("errorCode", e.getMessageId());
			dataMap.put("errorMessage", e.getMessage());
		} catch (IllegalArgumentException e) {
			dataMap.put("errorCode", ERRORCODE_PARAM_ERROR);
			dataMap.put("errorMessage", e.getMessage());
		} catch (Exception e) {
			dataMap.put("errorCode", ERRORCODE_INNER_ERROR);
			dataMap.put("errorMessage", e.getMessage());
			logger.error(e.getMessage(), e);
		}
		return SUCCESS;
	}

	public String addTag() throws Exception {

		commonAspects = commonAspectService.listCommonAspects();
		try {
			Validate.notNull(virtualServerName);
			Validate.notNull(version);
			dataMap.put("tagId", virtualServerService.tag(virtualServerName, version));

			dataMap.put("errorCode", ERRORCODE_SUCCESS);
		} catch (BizException e) {
			dataMap.put("errorCode", e.getMessageId());
			dataMap.put("errorMessage", e.getMessage());
		} catch (IllegalArgumentException e) {
			dataMap.put("errorCode", ERRORCODE_PARAM_ERROR);
			dataMap.put("errorMessage", e.getMessage());
		} catch (Exception e) {
			dataMap.put("errorCode", ERRORCODE_INNER_ERROR);
			dataMap.put("errorMessage", e.getMessage());
			logger.error(e.getMessage(), e);
		}
		return SUCCESS;
	}

	public String addBatchTag() throws Exception {
		List<String> tagIds = new ArrayList<String>();

		commonAspects = commonAspectService.listCommonAspects();
		if (vsListToTag != null) {
			for (String vs : vsListToTag) {
				VirtualServer virtualServer = virtualServerService.findVirtualServer(vs);
				Validate.notNull(virtualServer, "vs(" + vs + ") not found.");
				tagIds.add(virtualServerService.tag(vs, virtualServer.getVersion()));
			}
		}
		vsListToTagStr = StringUtils.join(vsListToTag, ',');
		tagIdsStr = StringUtils.join(tagIds, ',');
		return "redirect";
	}

	/**
	 * 查看某个tagId当时的config快照
	 */
	public String getNginxConfigByTagId() throws Exception {
		try {

			SlbModelTree tree = virtualServerService.findTagById(virtualServerName, tagId);
			dataMap.put("nginxConfig", virtualServerService.generateNginxConfig(tree));

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

	public String listTags() throws Exception {
		try {
			tags = virtualServerService.listTag(virtualServerName, tagNum);

		} catch (BizException e) {
			logger.error("Bussiness Error: " + e.getMessage());
		} catch (IllegalArgumentException e) {
			logger.error("Param Error: " + e.getMessage());
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return SUCCESS;
	}

	public String list() throws Exception {
		try {
			list = virtualServerService.listVirtualServers();

		} catch (IllegalArgumentException e) {
			logger.error("Param Error: " + e.getMessage());
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return SUCCESS;
	}

	public String deploy() {
		editOrShow = "edit";
		return SUCCESS;
	}

	public String getVirtualServerName() {
		return virtualServerName;
	}

	public void setVirtualServerName(String virtualServerName) {
		this.virtualServerName = virtualServerName;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public String getTagId() {
		return tagId;
	}

	public void setTagId(String tagId) {
		this.tagId = tagId;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public List<VirtualServer> getList() {
		return list;
	}

	public void setList(List<VirtualServer> list) {
		this.list = list;
	}

	public String[] getVsListToTag() {
		return vsListToTag;
	}

	public void setVsListToTag(String[] vsListToTag) {
		this.vsListToTag = vsListToTag;
	}

	public String getVsListToTagStr() {
		return vsListToTagStr;
	}

	public void setVsListToTagStr(String vsListToTagStr) {
		this.vsListToTagStr = vsListToTagStr;
	}

	public String getTagIdsStr() {
		return tagIdsStr;
	}

	public void setTagIdsStr(String tagIdsStr) {
		this.tagIdsStr = tagIdsStr;
	}

	@Override
	public void validate() {
		super.validate();
		this.setMenu(MENU);
	}

	public Map<String, VirtualServerGroup> getGroups() {
		return groups;
	}

	public void setGroups(Map<String, VirtualServerGroup> groups) {
		this.groups = groups;
	}

	public int getTagNum() {
		return tagNum;
	}

	public void setTagNum(int tagNum) {
		VirtualServerAction.tagNum = tagNum;
	}

}
