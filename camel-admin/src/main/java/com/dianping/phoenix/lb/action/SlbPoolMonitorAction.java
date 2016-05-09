package com.dianping.phoenix.lb.action;

import com.dianping.phoenix.lb.monitor.ChartBuilder;
import com.dianping.phoenix.lb.monitor.ReqStatusContainer;
import com.dianping.phoenix.lb.monitor.ReqStatusContainer.DataWrapper;
import com.opensymphony.xwork2.Action;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 监控相关
 *
 * @author mengwenchao
 *         <p/>
 *         2014年7月8日 上午11:19:41
 */
@Component("slbPoolMonitorAction")
@Scope("session")
public class SlbPoolMonitorAction extends AbstractMonitorAction {

	private static final long serialVersionUID = 1L;

	/**
	 * 获取数值时间范围，单位分钟
	 */
	private int duration = 3 * 60;

	/**
	 * 采样结束时间
	 */
	private long viewEndTime = System.currentTimeMillis();

	/**
	 * 采样点数，默认180
	 */
	private int viewCount = 180;

	private String poolName;

	private String nodeIp;

	@Autowired
	private ReqStatusContainer reqStatusContainer;

	private Map<String, Object> result = new HashMap<String, Object>();

	public String index() {

		return Action.SUCCESS;
	}

	/**
	 * 获取qps数据
	 * 访问url：
	 * http://localhost:{port}/monitor/qps/pool/${poolName}/get?viewEndTime=&duration=
	 */
	public String getPoolQpsData() {

		Validate.notEmpty(poolName);
		if (logger.isInfoEnabled()) {
			logger.info("[getPoolQpsData]" + poolName);
		}

		if (viewEndTime == 0) {
			viewEndTime = System.currentTimeMillis();
		}
		try {
			String title, subTitle = "";
			List<DataWrapper> qpsResult;

			List<String> dataDengine = new ArrayList<String>();

			if (poolName.equals("all")) {

				title = "所有Dengine统计(Qps)";
				dataDengine.add("All");
				qpsResult = reqStatusContainer.getTotalData(duration, viewCount, viewEndTime);
			} else {

				title = "单个Dengine统计(Qps)";
				qpsResult = reqStatusContainer.getDataForPool(duration, viewCount, viewEndTime, poolName);
			}

			result.put(ERROR_CODE, ERRORCODE_SUCCESS);
			result.put("qps", ChartBuilder.getHighChart(viewEndTime, duration, qpsResult, title, subTitle));
		} catch (Exception e) {

			logger.error("[getPoolQpsData]", e);
			result.put(ERROR_CODE, ERRORCODE_INNER_ERROR);
			result.put(ERROR_MESSAGE, e.getMessage());
		} finally {
			//清除time
			viewEndTime = 0;
		}
		return Action.SUCCESS;
	}

	public String qpsShow() {

		return Action.SUCCESS;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public String getNodeIp() {
		return nodeIp;
	}

	public void setNodeIp(String nodeIp) {
		this.nodeIp = nodeIp;
	}

	public Map<String, Object> getResult() {
		return result;
	}

	public void setResult(Map<String, Object> result) {
		this.result = result;
	}

	public String getPoolName() {
		return poolName;
	}

	public void setPoolName(String poolName) {
		this.poolName = poolName;
	}

	public long getViewEndTime() {
		return viewEndTime;
	}

	public void setViewEndTime(long viewEndTime) {
		this.viewEndTime = viewEndTime;
	}

	@Override
	public void validate() {
		super.validate();
		setSubMenu("slbPool");
	}

}
