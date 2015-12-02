package com.dianping.phoenix.lb.action;

import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.MonitorRule;
import com.dianping.phoenix.lb.model.entity.StatusCode;
import com.dianping.phoenix.lb.monitor.ChartBuilder;
import com.dianping.phoenix.lb.monitor.ReqStatusContainer.DataWrapper;
import com.dianping.phoenix.lb.monitor.highcharts.HighChartsWrapper;
import com.dianping.phoenix.lb.monitor.highcharts.HighChartsWrapper.Series;
import com.dianping.phoenix.lb.monitor.nginx.log.NginxStatusRecorder;
import com.dianping.phoenix.lb.monitor.nginx.log.content.MailContentGenerator;
import com.dianping.phoenix.lb.monitor.nginx.log.dashboard.DashboardContainer;
import com.dianping.phoenix.lb.monitor.nginx.log.dashboard.MinuteEntry;
import com.dianping.phoenix.lb.monitor.nginx.log.rule.MonitorRuleManager;
import com.dianping.phoenix.lb.service.model.StatusCodeService;
import com.dianping.phoenix.lb.utils.IOUtilsWrapper;
import com.dianping.phoenix.lb.utils.JsonBinder;
import com.dianping.phoenix.lb.utils.TimeUtil;
import com.opensymphony.xwork2.Action;
import org.apache.struts2.ServletActionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.unidal.lookup.util.StringUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Component("statusMonitorAction")
@Scope("session")
public class StatusMonitorAction extends AbstractMonitorAction {

	private static final long serialVersionUID = 1L;

	private String m_secondSubMenu;

	private String m_poolName;

	private String m_ruleId;

	private Date m_startTime = TimeUtil.getLastHour();

	private Date m_endTime = TimeUtil.getCurrentHour();

	private DateFormat m_sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	private boolean isInitURL = false;

	private int m_size = 7;

	private String m_statusCode = "all";

	private Map<String, List<MinuteEntry>> m_minuteEntries;

	private Set<String> m_statusCodes = new HashSet<String>();

	private String m_errorMessage;

	private String m_refresh = "false";

	@Autowired
	private MonitorRuleManager m_ruleManager;

	@Autowired
	private NginxStatusRecorder m_nginxStatusRecorder;

	@Autowired
	private MailContentGenerator m_mailContentGenerator;

	@Autowired
	private StatusCodeService m_statusCodeService;

	@Autowired
	private DashboardContainer m_dashoboardContainer;

	public String getEndTime() {
		return m_sdf.format(m_endTime);
	}

	public void setEndTime(String rawTimeStr) {
		try {
			m_endTime = TimeUtil.trimHour(m_sdf.parse(rawTimeStr));
		} catch (ParseException e) {
			m_endTime = TimeUtil.getCurrentHour();
		}
	}

	public String getErrorMessage() {
		return m_errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		m_errorMessage = errorMessage;
	}

	public Map<String, List<MinuteEntry>> getMinuteEntries() {
		return m_minuteEntries;
	}

	public void setMinuteEntries(Map<String, List<MinuteEntry>> minuteEntries) {
		m_minuteEntries = minuteEntries;
	}

	public String getMonitorRules() {
		dataMap.put("rules", m_ruleManager.getMonitorRules());
		dataMap.put("errorCode", ERRORCODE_SUCCESS);
		return SUCCESS;
	}

	public String getPoolName() {
		return m_poolName;
	}

	public void setPoolName(String poolName) {
		m_poolName = poolName;
	}

	public String getPoolStatusTrend() {
		try {
			if (StringUtils.isEmpty(m_poolName)) {
				throw new Exception("pool name cannot be none");
			}
			if (logger.isInfoEnabled()) {
				logger.info("[getStatusTrendData]" + m_poolName);
			}
			if (TimeUtil.calIntervalMinutes(m_startTime, m_endTime) / (24 * 60) > 7) {
				throw new Exception("请选择一周内的间隔");
			}

			String title, subTitle = "";
			Collection<DataWrapper> statusDatas;

			if ("all".equals(m_poolName)) {
				title = "SLB Nginx状态码统计";
			} else {
				title = "集群：" + m_poolName + "状态码统计";
			}
			statusDatas = m_nginxStatusRecorder.extractStatusData(m_poolName, m_startTime, m_endTime);
			dataMap.put("charts", splitChartsByStatusCode(ChartBuilder
							.getHighChart(m_endTime.getTime() + TimeUtil.ONE_HOUR_MILLS, TimeUtil.ONE_MINUTE_MILLS,
									statusDatas, title, subTitle)));
			dataMap.put(ERROR_CODE, ERRORCODE_SUCCESS);
		} catch (Exception e) {
			logger.error("[getStatusTrendData]", e);
			dataMap.put(ERROR_CODE, ERRORCODE_INNER_ERROR);
			dataMap.put(ERROR_MESSAGE, e.getMessage());
		}
		return Action.SUCCESS;
	}

	public String getRefresh() {
		return m_refresh;
	}

	public void setRefresh(String refresh) {
		this.m_refresh = refresh;
	}

	public String getRuleId() {
		return m_ruleId;
	}

	public void setRuleId(String ruleId) {
		m_ruleId = ruleId;
	}

	public String getSecondSubMenu() {
		if (StringUtils.isEmpty(m_secondSubMenu)) {
			return "monitorRule";
		}
		return m_secondSubMenu;
	}

	public void setSecondSubMenu(String secondSubMenu) {
		this.m_secondSubMenu = secondSubMenu;
	}

	public int getSize() {
		return m_size;
	}

	public void setSize(int size) {
		m_size = size;
	}

	public String getStartTime() {
		return m_sdf.format(m_startTime);
	}

	public void setStartTime(String rawTimeStr) {
		try {
			m_startTime = TimeUtil.trimHour(m_sdf.parse(rawTimeStr));
		} catch (ParseException e) {
			m_startTime = TimeUtil.getLastHour();
		}
	}

	public String getStatusCode() {
		return m_statusCode;
	}

	public void setStatusCode(String statusCode) {
		m_statusCode = statusCode;
	}

	public Set<String> getStatusCodes() {
		return m_statusCodes;
	}

	public void setStatusCodes(Set<String> statusCodes) {
		m_statusCodes = statusCodes;
	}

	public String listStatusCodes() {
		try {
			List<String> statusCodes = new ArrayList<String>();

			for (StatusCode statusCode : m_statusCodeService.listStatusCodes()) {
				statusCodes.add(statusCode.getValue());
			}
			dataMap.put("errorCode", ERRORCODE_SUCCESS);
			dataMap.put("statusCodes", statusCodes);
		} catch (BizException e) {
			dataMap.put("errorCode", ERRORCODE_INNER_ERROR);
			dataMap.put("errorMessage", e.getMessage());
			LOG.error(e.getMessage(), e);
		}
		return SUCCESS;
	}

	public String removeMonitorRule() {
		try {
			m_ruleManager.removeMonitorRule(m_ruleId);
			dataMap.put(ERROR_CODE, ERRORCODE_SUCCESS);
		} catch (Exception ex) {
			dataMap.put(ERROR_CODE, ERRORCODE_INNER_ERROR);
			dataMap.put(ERROR_MESSAGE, ex.getMessage());
		}
		return SUCCESS;
	}

	public String saveMonitorRule() throws Exception {
		try {
			String ruleJson = IOUtilsWrapper
					.convetStringFromRequest(ServletActionContext.getRequest().getInputStream());

			if (StringUtils.isEmpty(ruleJson)) {
				throw new IllegalArgumentException("rule参数不能为空！");
			}
			MonitorRule rule = JsonBinder.getNonNullBinder().fromJson(ruleJson, MonitorRule.class);

			m_ruleManager.addOrUpdateMonitorRule(rule);
			setSecondSubMenu("monitorRule");
			dataMap.put("errorCode", ERRORCODE_SUCCESS);
		} catch (Exception e) {
			dataMap.put("errorCode", ERRORCODE_INNER_ERROR);
			dataMap.put("errorMessage", e.getMessage());
			logger.error(e.getMessage(), e);
		}
		return SUCCESS;
	}

	public String showAllStatusTrend() {
		setSecondSubMenu("monitorData");
		return Action.SUCCESS;
	}

	public synchronized String showDashboard() {
		m_errorMessage = "";
		m_minuteEntries = new TreeMap<String, List<MinuteEntry>>(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return toInteger(o1) - toInteger(o2);
			}

			private int toInteger(String s) {
				if ("all".equals(s)) {
					return -3;
				} else if ("4XX".equals(s)) {
					return -2;
				} else if ("5XX".equals(s)) {
					return -1;
				} else {
					return Integer.parseInt(s);
				}
			}
		});
		String[] statusCodes = m_statusCode.split(",");

		for (String rawStatusCode : statusCodes) {
			try {
				String statusCode = rawStatusCode.trim();

				m_minuteEntries.put(statusCode, m_dashoboardContainer.fetchMinuteEntries(statusCode, m_size));
			} catch (BizException e) {
				m_errorMessage += rawStatusCode + " ";
			}
		}
		m_statusCodes = m_dashoboardContainer.getStatusCodes();

		if (StringUtils.isNotEmpty(m_errorMessage)) {
			m_errorMessage = "statuscode has no data:" + m_errorMessage;
			logger.error("error", new RuntimeException(m_errorMessage));
		}
		setSecondSubMenu("dashboard");
		return Action.SUCCESS;
	}

	public String showMonitorRules() {
		setSecondSubMenu("monitorRule");
		editOrShow = "show";
		if (!isInitURL) {
			m_mailContentGenerator.setHostname(ServletActionContext.getRequest().getServerName());
			m_mailContentGenerator.setPort(Integer.toString(ServletActionContext.getRequest().getServerPort()));
			isInitURL = true;
		}
		return Action.SUCCESS;
	}

	public String showPoolStatusTrend() {
		setSecondSubMenu("monitorData");
		return Action.SUCCESS;
	}

	private Map<String, HighChartsWrapper> splitChartsByStatusCode(HighChartsWrapper highChart) {
		HighChartsWrapper wrapper4 = JsonBinder.getNonNullBinder()
				.fromJson(JsonBinder.getNonNullBinder().toJson(highChart), HighChartsWrapper.class);
		HighChartsWrapper wrapper5 = JsonBinder.getNonNullBinder()
				.fromJson(JsonBinder.getNonNullBinder().toJson(highChart), HighChartsWrapper.class);
		List<Series> series4 = new ArrayList<Series>();
		List<Series> series5 = new ArrayList<Series>();

		for (Series series : highChart.getSeries()) {
			if (series.getName().startsWith("status code: 4")) {
				series4.add(series);
			} else if (series.getName().startsWith("status code: 5")) {
				series5.add(series);
			} else {
				logger.error("unknown series: " + series.getName());
			}
		}
		wrapper4.setSeries(series4.toArray(new Series[series4.size()]));
		wrapper5.setSeries(series5.toArray(new Series[series5.size()]));

		Map<String, HighChartsWrapper> result = new HashMap<String, HighChartsWrapper>();

		result.put("chart4", wrapper4);
		result.put("chart5", wrapper5);
		return result;
	}

	@Override
	public void validate() {
		super.validate();
		setSubMenu("statusMonitor");
	}

}
