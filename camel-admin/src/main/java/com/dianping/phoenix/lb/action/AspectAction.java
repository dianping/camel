package com.dianping.phoenix.lb.action;

import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.Aspect;
import com.dianping.phoenix.lb.service.model.CommonAspectService;
import com.dianping.phoenix.lb.utils.IOUtilsWrapper;
import com.dianping.phoenix.lb.utils.JsonBinder;
import org.apache.commons.lang.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author wukezhu
 */
@Component("aspectAction")
@Scope("prototype")
public class AspectAction extends AbstractRulesAction {

	private static final long serialVersionUID = 1L;

	protected List<Aspect> aspects;

	@Autowired
	private CommonAspectService aspectService;

	public AspectAction() {
		setSubMenu("aspect");
	}

	public List<Aspect> getAspects() {
		return aspects;
	}

	public String list() {
		aspects = aspectService.listCommonAspects();
		return SUCCESS;
	}

	public String index() {
		aspects = aspectService.listCommonAspects();
		return SUCCESS;
	}

	public String show() {
		aspects = aspectService.listCommonAspects();
		editOrShow = "show";
		return SUCCESS;
	}

	public String edit() {
		aspects = aspectService.listCommonAspects();
		editOrShow = "edit";
		return SUCCESS;
	}

	@SuppressWarnings("unchecked")
	public String save() throws Exception {
		try {
			String aspectsJson = IOUtilsWrapper
					.convetStringFromRequest(ServletActionContext.getRequest().getInputStream());
			if (StringUtils.isBlank(aspectsJson)) {
				throw new IllegalArgumentException("规则名不能为空！");
			}
			List<Aspect> aspects = JsonBinder.getNonNullBinder().fromJson(aspectsJson, List.class, Aspect.class);

			aspectService.saveCommonAspect(aspects);

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

	@Override
	@Autowired
	public void setBreadcrumb(
			@Value("${rules.aspect.breadcrumb}")
			String breadcrumb) {
		super.setBreadcrumb(breadcrumb);
	}

	@Override
	public void validate() {
		super.validate();

	}
}
