package com.dianping.phoenix.lb.action;

import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.SlbPool;
import com.dianping.phoenix.lb.service.model.SlbPoolService;
import com.dianping.phoenix.lb.utils.IOUtilsWrapper;
import com.dianping.phoenix.lb.utils.JsonBinder;
import org.apache.commons.lang.StringUtils;
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
@Component("slbPoolAction")
@Scope("prototype")
public class SlbPoolAction extends MenuAction {

	private static final Logger logger = LoggerFactory.getLogger(SlbPoolAction.class);

	private static final long serialVersionUID = -1084994778030229218L;

	private static final String MENU = "slbPool";
	protected List<SlbPool> slbPools;
	private String slbPoolName;
	private Boolean showInfluencing;
	@Autowired
	private SlbPoolService slbPoolService;

	public List<SlbPool> getSlbPools() {
		return slbPools;
	}

	public String show() {
		slbPools = slbPoolService.listSlbPools();
		editOrShow = "show";
		return SUCCESS;
	}

	public String edit() {
		slbPools = slbPoolService.listSlbPools();
		editOrShow = "edit";
		return SUCCESS;
	}

	public String listSlbPools() {
		slbPools = slbPoolService.listSlbPools();
		return SUCCESS;
	}

	public String index() {
		slbPools = slbPoolService.listSlbPools();
		if (slbPools.size() == 0) {
			return "noneSlbPool";
		}
		slbPoolName = slbPools.get(0).getName();//重定向
		return "redirect";
	}

	public String list() throws Exception {
		try {
			Set<String> slbPoolNames = new HashSet<String>();

			slbPools = slbPoolService.listSlbPools();
			for (SlbPool slbPool : slbPools) {
				slbPoolNames.add(slbPool.getName());
			}
			dataMap.put("slbPoolNames", new ArrayList(slbPoolNames));
			dataMap.put("errorCode", ERRORCODE_SUCCESS);
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

	public String get() throws Exception {
		try {
			//获取slbPool
			SlbPool slbPool = slbPoolService.findSlbPool(slbPoolName);

			dataMap.put("slbPool", slbPool);
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
			String slbPoolJson = IOUtilsWrapper
					.convetStringFromRequest(ServletActionContext.getRequest().getInputStream());
			if (StringUtils.isBlank(slbPoolJson)) {
				throw new IllegalArgumentException("slbPool 参数不能为空！");
			}
			SlbPool slbPool = JsonBinder.getNonNullBinder().fromJson(slbPoolJson, SlbPool.class);

			String slbPoolName = slbPool.getName();
			SlbPool slbPool0 = slbPoolService.findSlbPool(slbPoolName);
			if (slbPool0 != null) {
				slbPoolService.modifySlbPool(slbPoolName, slbPool);
			} else {
				slbPoolService.addSlbPool(slbPoolName, slbPool);
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

	public String remove() throws Exception {
		try {
			SlbPool slbPool0 = slbPoolService.findSlbPool(slbPoolName);
			if (slbPool0 == null) {
				throw new IllegalArgumentException("不存在该站点：" + slbPoolName);
			}
			slbPoolService.deleteSlbPool(slbPoolName);
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

	public String getSlbPoolName() {
		return slbPoolName;
	}

	public void setSlbPoolName(String slbPoolName) {
		this.slbPoolName = slbPoolName;
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
