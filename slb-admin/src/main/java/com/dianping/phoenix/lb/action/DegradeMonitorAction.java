package com.dianping.phoenix.lb.action;

import com.dianping.phoenix.lb.api.dengine.DengineConfig;
import com.dianping.phoenix.lb.monitor.DegradeStatusContainer;
import com.dianping.phoenix.lb.monitor.DegradeStatusContainer.DegradeStatusResult;
import com.opensymphony.xwork2.Action;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 降级监控
 *
 * @author mengwenchao
 *         <p/>
 *         2014年9月5日 下午4:35:45
 */
@Service
public class DegradeMonitorAction extends AbstractMonitorAction {

	private static final long serialVersionUID = 1L;

	@Autowired
	private DegradeStatusContainer degradeStatusContainer;

	private String degradeMark = "degrade";

	private List<DegradeStatusResult> degradeStatusResult;

	@Autowired
	private DengineConfig dengineConfig;

	public String index() {

		setDegradeStatusResult(degradeStatusContainer.getDegradeData());

		return Action.SUCCESS;
	}

	public String getDegrade() {

		return Action.SUCCESS;

	}

	public String getDegradeMark() {
		return degradeMark;
	}

	public void setDegradeMark(String degradeMark) {
		this.degradeMark = degradeMark;
	}

	public List<DegradeStatusResult> getDegradeStatusResult() {
		return degradeStatusResult;
	}

	public void setDegradeStatusResult(List<DegradeStatusResult> degradeStatusResult) {
		this.degradeStatusResult = degradeStatusResult;
	}

}
