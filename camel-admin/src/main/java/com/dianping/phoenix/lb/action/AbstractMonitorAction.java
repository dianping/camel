package com.dianping.phoenix.lb.action;

import com.dianping.phoenix.lb.model.entity.SlbPool;
import com.dianping.phoenix.lb.service.model.SlbPoolService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 监控抽象类
 *
 * @author mengwenchao
 *         <p/>
 *         2014年9月5日 下午4:35:35
 */
public abstract class AbstractMonitorAction extends MenuAction {

	private static final long serialVersionUID = 1L;

	private static final String MENU = "monitor";
	protected List<SlbPool> pools;
	@Autowired
	private SlbPoolService slbPoolService;

	@Override
	public void validate() {

		super.validate();
		super.setMenu(MENU);
		pools = slbPoolService.listSlbPools();
	}

	public List<SlbPool> getPools() {

		return pools;
	}

}
