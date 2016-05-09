package com.dianping.phoenix.lb.action;

import org.apache.struts2.ServletActionContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author wukezhu
 */
@Component("batchTagAction")
@Scope("prototype")
public class BatchTagAction extends MenuAction {

	private static final long serialVersionUID = -7250754630706893980L;

	private static final String MENU = "batchTag";

	public String index() {
		return SUCCESS;
	}

	@Override
	public void validate() {
		super.validate();
		if (contextPath == null) {
			contextPath = ServletActionContext.getServletContext().getContextPath();
		}
		this.setMenu(MENU);
	}

}
