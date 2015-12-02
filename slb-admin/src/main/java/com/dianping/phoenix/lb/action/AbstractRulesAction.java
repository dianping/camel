package com.dianping.phoenix.lb.action;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年9月3日 下午3:43:34
 */
public abstract class AbstractRulesAction extends MenuAction {

	private static final long serialVersionUID = 1L;

	private static final String MENU = "rules";

	private String breadcrumb;

	@Override
	public void validate() {
		super.validate();
		this.setMenu(MENU);
	}

	public String getBreadcrumb() {
		return breadcrumb;
	}

	public void setBreadcrumb(String breadcrumb) {
		this.breadcrumb = breadcrumb;
	}

}
