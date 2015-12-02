package com.dianping.phoenix.lb.action;

import com.dianping.phoenix.lb.action.search.DefaultMatcherBuilder;
import com.dianping.phoenix.lb.action.search.Matcher;
import com.dianping.phoenix.lb.action.search.MatcherBuilder;
import com.dianping.phoenix.lb.monitor.ApiResult;
import com.dianping.phoenix.lb.monitor.StatusContainer;
import com.dianping.phoenix.lb.monitor.StatusContainer.ShowResult;
import com.opensymphony.xwork2.Action;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

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
@Component("upstreamMonitorAction")
@Scope("session")
public class UpstreamMonitorAction extends AbstractMonitorAction {

	private static final long serialVersionUID = 1L;

	private String app;

	private String nodeIp;

	@Autowired
	private StatusContainer statusContainer;

	private Map<String, Object> result = new HashMap<String, Object>();

	private List<ShowResult> appStatus;

	/**
	 * 当前显示的页码
	 */
	private int pageNum = 1;

	/**
	 * 每页显示的数据大小
	 */
	private int itemsPerPage = 50;

	private String queryString = "down";

	private Paginator paginator;

	private List<ApiResult> singleAppStatus;

	/**
	 * 返回应用连接信息
	 *
	 * @return
	 */
	public String appStatus() {

		MatcherBuilder matcherBuilder = new DefaultMatcherBuilder();
		Matcher matcher = matcherBuilder.build(queryString);

		appStatus = statusContainer.getStatusForShow(matcher);

		//处理分页
		paginator = new Paginator();
		paginator.setItems(appStatus.size());
		paginator.setItemsPerPage(itemsPerPage);
		paginator.setPage(pageNum);

		int beginIndex = paginator.getBeginIndex() - 1;
		if (beginIndex < 0) {
			beginIndex = 0;
		}
		appStatus = appStatus.subList(beginIndex, paginator.getEndIndex());

		return Action.SUCCESS;

	}

	/**
	 * 返回特定应用机器连接信息
	 *
	 * @return
	 */
	public String singleAppStatus() {

		int index = app.lastIndexOf(".");
		if (index < 0) {
			logger.error("[singleAppStatus][wrong app]", app);
			return Action.ERROR;
		}
		this.singleAppStatus = statusContainer.getStatus(app.substring(0, index) + ":" + app.substring(index + 1));

		return Action.SUCCESS;

	}

	public String appShow() {

		return Action.SUCCESS;
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

	public String getApp() {
		return app;
	}

	public void setApp(String app) {
		this.app = app;
	}

	public List<ShowResult> getAppStatus() {
		return appStatus;
	}

	public void setAppStatus(List<ShowResult> appStatus) {
		this.appStatus = appStatus;
	}

	public List<ApiResult> getSingleAppStatus() {
		return singleAppStatus;
	}

	public void setSingleAppStatus(List<ApiResult> singleAppStatus) {
		this.singleAppStatus = singleAppStatus;
	}

	public int getPageNum() {
		return pageNum;
	}

	public void setPageNum(int pageNum) {
		this.pageNum = pageNum;
	}

	public Paginator getPaginator() {
		return paginator;
	}

	public void setPaginator(Paginator paginator) {
		this.paginator = paginator;
	}

	public int getItemsPerPage() {
		return itemsPerPage;
	}

	public void setItemsPerPage(int itemsPerPage) {
		this.itemsPerPage = itemsPerPage;
	}

	public String getQueryString() {
		return queryString;
	}

	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	@Override
	public void validate() {

		super.validate();
		setSubMenu("upstream");
	}
}
