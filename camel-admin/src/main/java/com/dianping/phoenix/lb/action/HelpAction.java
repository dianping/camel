package com.dianping.phoenix.lb.action;

import org.springframework.stereotype.Component;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年10月28日 下午7:06:25
 */
@Component
public class HelpAction extends MenuAction {

	private static final long serialVersionUID = 1L;

	public String index() {
		setMenu("help");
		return SUCCESS;
	}

}
