package com.dianping.phoenix.lb.action;

import com.dianping.phoenix.lb.deploy.bo.api.DeployTaskApiBo;
import com.dianping.phoenix.lb.deploy.executor.TaskExecutor;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.facade.BatchDeploy;
import com.dianping.phoenix.lb.model.InfluencingVs;
import com.dianping.phoenix.lb.model.entity.Variable;
import com.dianping.phoenix.lb.service.model.VariableService;
import com.dianping.phoenix.lb.utils.IOUtilsWrapper;
import com.dianping.phoenix.lb.utils.JsonBinder;
import org.apache.commons.lang.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Component("variableAction")
@Scope("prototype")
public class VariableAction extends AbstractRulesAction {

	private static final long serialVersionUID = -1084994778030229218L;
	protected List<Variable> variables;
	@Autowired
	private VariableService variableService;
	@Autowired
	private BatchDeploy batchDeploy;
	private String key;

	public VariableAction() {

		super.setSubMenu("variable");
	}

	public String show() {

		editOrShow = "show";

		return SUCCESS;
	}

	public String edit() {
		editOrShow = "edit";

		return SUCCESS;
	}

	public String index() {
		return SUCCESS;
	}

	public String get() {

		updateVariables();
		return SUCCESS;
	}

	private void updateVariables() {
		try {
			variables = variableService.listVariables();
		} catch (BizException e) {
			logger.error("[updateVariables]", e);
		}
	}

	public String getInfluencingVs() {

		try {

			Set<InfluencingVs> influencingVs = variableService.findInfluencedVs(key);
			dataMap.put("errorCode", ERRORCODE_SUCCESS);
			dataMap.put("influencingVs", influencingVs);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			dataMap.put("errorCode", ERRORCODE_INNER_ERROR);
			dataMap.put("errorMessage", e.getMessage());
		}

		return SUCCESS;
	}

	public String deployVariable() {

		try {
			org.apache.commons.lang3.Validate.notNull(key);
			Set<InfluencingVs> influencingVs = variableService.findInfluencedVs(key);
			List<String> influencingVsList = new LinkedList<String>();
			for (InfluencingVs ivs : influencingVs) {
				influencingVsList.add(ivs.getVsName());
			}

			TaskExecutor<DeployTaskApiBo> taskExecutor = batchDeploy
					.deployVs("deploy variable:" + key, influencingVsList, "variable");
			dataMap.put("errorCode", ERRORCODE_SUCCESS);
			dataMap.put("taskId", taskExecutor.getDeployTaskBo().getTask().getId());
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			dataMap.put("errorCode", ERRORCODE_INNER_ERROR);
			dataMap.put("errorMessage", e.getMessage());
		}

		return SUCCESS;
	}

	public List<Variable> getVariables() {
		return this.variables;
	}

	public void setVariables(List<Variable> variables) {
		this.variables = variables;
	}

	public String save() {

		try {
			String variablesJson = IOUtilsWrapper
					.convetStringFromRequest(ServletActionContext.getRequest().getInputStream());
			if (StringUtils.isBlank(variablesJson)) {
				throw new IllegalArgumentException("规则名不能为空！");
			}

			@SuppressWarnings("unchecked")
			List<Variable> variables = JsonBinder.getNonNullBinder()
					.fromJson(variablesJson, List.class, Variable.class);
			variableService.saveVariables(variables);

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
			@Value("${rules.variable.breadcrumb}")
			String breadcrumb) {
		super.setBreadcrumb(breadcrumb);
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

}
