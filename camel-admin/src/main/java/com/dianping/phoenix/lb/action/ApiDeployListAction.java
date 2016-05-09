package com.dianping.phoenix.lb.action;

import com.dianping.phoenix.lb.deploy.model.api.DeployTaskApi;
import com.dianping.phoenix.lb.deploy.service.DeployTaskApiService;
import org.apache.struts2.ServletActionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @author wukezhu
 */
@Component("apideployListAction")
@Scope("prototype")
public class ApiDeployListAction extends MenuAction {

	private static final long serialVersionUID = -7250754630706893980L;

	private static final String MENU = "apideployList";

	@Autowired
	private DeployTaskApiService deployTaskService;

	private String[] virtualServerNames;

	private int pageNum = 1;

	private List<DeployTaskApi> list;

	private Paginator paginator;

	@PostConstruct
	public void init() {
	}

	/**
	 * 进入发布的页面，需要的参数是vsName列表
	 */
	public String list() {
		// 获取用户的历史重发记录
		paginator = new Paginator();
		list = deployTaskService.list(paginator, pageNum);

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

	public String[] getVirtualServerNames() {
		return virtualServerNames;
	}

	public void setVirtualServerNames(String[] virtualServerNames) {
		this.virtualServerNames = virtualServerNames;
	}

	public int getPageNum() {
		return pageNum;
	}

	public void setPageNum(int pageNum) {
		this.pageNum = pageNum;
	}

	public List<DeployTaskApi> getList() {
		return list;
	}

	public void setList(List<DeployTaskApi> list) {
		this.list = list;
	}

	public Paginator getPaginator() {
		return paginator;
	}

	public void setPaginator(Paginator paginator) {
		this.paginator = paginator;
	}

}
