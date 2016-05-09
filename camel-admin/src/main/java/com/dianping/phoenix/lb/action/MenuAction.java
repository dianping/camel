package com.dianping.phoenix.lb.action;

import com.dianping.phoenix.lb.constant.SlbConfig;
import com.dianping.phoenix.lb.model.entity.Aspect;
import com.dianping.phoenix.lb.service.model.CommonAspectService;
import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.ServletActionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public abstract class MenuAction extends ActionSupport {

	public static final String ERROR_CODE = "errorCode";
	public static final String MESSAGE = "message";
	public static final int ERRORCODE_SUCCESS = 0;
	public static final int ERRORCODE_PARAM_ERROR = -2;
	public static final int ERRORCODE_INNER_ERROR = -1;
	public static final int API_ERRORCODE_INNER_ERROR = 1;
	public static final int API_ERRORCODE_ILLEGAL_ARGUMENT = -3;
	public static final int API_ERRORCODE_POOL_NOT_FOUND = -2;
	public static final int API_ERRORCODE_MEMBER_NOT_FOUND = -1;
	public static final int API_ERRORCODE_NOT_SUCCESS = -4;
	public static final int API_ERRORCODE_DEBUG_MODEL = -5;
	public static final int NOT_SAFE_CONFIG = -6;
	protected static final String ERROR_MESSAGE = "errorMessage";
	protected static final String DEFAULT_ENCODING = "UTF-8";
	private static final long serialVersionUID = -1084994778030229218L;
	protected final Logger logger = LoggerFactory.getLogger(getClass());
	protected Map<String, Object> dataMap = new HashMap<String, Object>();
	protected String contextPath;
	protected String editOrShow = "show";
	@Autowired
	protected CommonAspectService commonAspectService;
	protected List<Aspect> commonAspects;
	@Autowired
	protected SlbConfig m_slbConfig;
	private String m_isFullScreen = "false";
	/**
	 * vs,pool,deploy
	 */
	private String menu;
	private String subMenu;

	@Override
	public void validate() {
		super.validate();
		if (contextPath == null) {
			contextPath = ServletActionContext.getServletContext().getContextPath();
		}
	}

	public Map<String, Object> getDataMap() {
		return dataMap;
	}

	public String getContextPath() {
		return contextPath;
	}

	public String getEditOrShow() {
		return editOrShow;
	}

	public String getMenu() {
		return menu;
	}

	public void setMenu(String menu) {
		this.menu = menu;
	}

	public String getSubMenu() {
		return subMenu;
	}

	public void setSubMenu(String subMenu) {
		this.subMenu = subMenu;
	}

	public String getUserName() {
		String rawUser = ServletActionContext.getRequest().getRemoteUser();

		if (!StringUtils.isEmpty(rawUser)) {
			String[] metrics = rawUser.split("\\|");

			return metrics[metrics.length - 1];
		} else {
			return "";
		}
	}

	// logout is not available for env
	public String getLogoutURL() {
		return m_slbConfig.getLogoutURL();
	}

	public String getIsFullScreen() {
		return m_isFullScreen;
	}

	public void setIsFullScreen(String m_isFullScreen) {
		this.m_isFullScreen = m_isFullScreen;
	}

}
