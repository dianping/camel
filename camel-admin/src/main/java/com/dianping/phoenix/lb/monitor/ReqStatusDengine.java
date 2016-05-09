package com.dianping.phoenix.lb.monitor;

import com.dianping.phoenix.lb.api.Startable;
import com.dianping.phoenix.lb.api.Stoppable;
import com.dianping.phoenix.lb.api.cleanable;
import com.dianping.phoenix.lb.monitor.ReqStatusContainer.DataWrapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author mengwenchao
 *         <p/>
 *         一个Tengine对应数据的记录
 *         <p/>
 *         2014年7月7日 下午4:34:29
 */
public class ReqStatusDengine implements cleanable, Startable, Stoppable, Runnable {

	private static final Logger logger = LoggerFactory.getLogger(ReqStatusDengine.class);
	private static final int REQ_STATUS_COUNT = 14;
	private String encoding = "UTF-8";
	private String ip;

	private int interval;

	/**
	 * 最后一个数据的时间，以毫秒计数
	 */
	private long lastDataTime;

	private Queue<ReqStatus> reqStatusTengine;

	private int keepInMemoryHours = 1;

	private String reqStatusAddress;

	private ScheduledExecutorService scheduledExecutor;

	private ScheduledFuture<?> scheduleFuture;

	private ReqStatus pre, cur;

	public ReqStatusDengine(ScheduledExecutorService scheduledExecutor, String ip, String reqStatusAddress,
			int keepInMemoryHours, int interval) {

		this.scheduledExecutor = scheduledExecutor;
		this.ip = ip;
		this.reqStatusAddress = reqStatusAddress;
		this.keepInMemoryHours = keepInMemoryHours;
		this.interval = interval;
		reqStatusTengine = new ConcurrentLinkedQueue<ReqStatus>();
	}

	@Override
	public void run() {

		//抓取数据，记录
		String url = "http://" + ip + ":" + reqStatusAddress;
		try {
			this.setLastDataTime(System.currentTimeMillis());
			String rq = HttpClientUtil.getAsString(url, null, encoding);
			getReqStatus(rq);
		} catch (Throwable e) {
			logger.error("error fetch status " + ip + "," + e);
			//如果出错，添加一条0数据
			//因为只记录了终点时间，没有记录每条数据时间
			offer(new ReqStatus(ip));
		}
	}

	/**
	 * 内部数据大小，单元测试使用
	 */
	public int count() {

		return this.reqStatusTengine.size();
	}

	private void getReqStatus(String rq) {

		String[] rqs = rq.split("\n");

		ReqStatus totalStatus = new ReqStatus(ip + ":" + reqStatusAddress);

		for (String domain : rqs) {
			try {
				ReqStatus domainReqStatus = new ReqStatus(split(domain));
				if (canAdd(domainReqStatus)) {
					totalStatus.add(domainReqStatus);
				}
			} catch (Exception e) {
				logger.error("[getReqStatus][one status error]" + domain + "," + e.getMessage());
			}

		}
		offer(totalStatus);
	}

	private String[] split(String domain) {
		//拆分
		String sp[] = domain.split(",");
		if (sp.length == REQ_STATUS_COUNT) {
			return sp;
		}
		if (sp.length < REQ_STATUS_COUNT) {
			throw new IllegalStateException(
					"[split][expected " + REQ_STATUS_COUNT + ", but" + sp.length + "]" + domain);
		}
		int diff = sp.length - REQ_STATUS_COUNT;
		String[] result = new String[REQ_STATUS_COUNT];

		result[0] = StringUtils.join(sp, "", 0, diff);
		for (int i = 1; i < REQ_STATUS_COUNT; i++) {
			result[i] = sp[i + diff];
		}

		return result;
	}

	private boolean canAdd(ReqStatus domainReqStatus) {

		//slb自己产生的流量不算
		if (domainReqStatus.getKv().matches("^\\d+.*")) {
			return false;
		}
		return true;
	}

	@Override
	public void start() {

		if (logger.isInfoEnabled()) {
			logger.info("[start]" + ip + "," + interval);
		}
		scheduleFuture = scheduledExecutor.scheduleAtFixedRate(this, 0, interval, TimeUnit.MILLISECONDS);

	}

	@Override
	public void stop() {

		if (logger.isInfoEnabled()) {
			logger.info("[stop]" + ip);
		}
		scheduleFuture.cancel(true);
	}

	public void offer(ReqStatus reqStatus) {

		pre = cur;
		cur = reqStatus;
		reqStatusTengine.offer(reqStatus);
	}

	/**
	 * 根据配置的keepInMemoryHours删除时间过长的数据
	 */
	@Override
	public void clean() {

		if (logger.isInfoEnabled()) {
			logger.info("[clean]" + ip);
		}
		long keepCount = keepInMemoryHours * 3600 * 1000L / interval;
		if (reqStatusTengine.size() <= keepCount) {
			return;
		}

		long cleanSize = reqStatusTengine.size() - keepCount;
		if (logger.isInfoEnabled()) {
			logger.info("[clean][data size]" + cleanSize);
		}
		//删除多余的数据
		for (int i = 0; i < cleanSize; i++) {

			reqStatusTengine.poll();
		}
	}

	public int getInterval() {
		return interval;
	}

	public void setInterval(int interval) {
		this.interval = interval;
	}

	public int getKeepInMemoryHours() {
		return keepInMemoryHours;
	}

	public void setKeepInMemoryHours(int keepInMemoryHours) {
		this.keepInMemoryHours = keepInMemoryHours;
	}

	public String getReqStatusAddress() {
		return reqStatusAddress;
	}

	public void setReqStatusAddress(String reqStatusAddress) {
		this.reqStatusAddress = reqStatusAddress;
	}

	public long getLastDataTime() {
		return lastDataTime;
	}

	public void setLastDataTime(long lastDataTime) {
		this.lastDataTime = lastDataTime;
	}

	/**
	 * @param viewInterval 毫秒
	 * @param duration     分钟
	 * @param viewEndTime  毫秒
	 * @return
	 */
	public DataWrapper getViewData(int viewCount, int duration, long viewEndTime) {

		DataWrapper dw = new DataWrapper();
		dw.setDesc(ip);

		if (viewEndTime >= lastDataTime) {
			viewEndTime = lastDataTime;
		}

		Object[] reqstatus = reqStatusTengine.toArray();
		int count = reqstatus.length;
		int end = (int) (count - 1 - (lastDataTime - viewEndTime) / interval);
		int begin = end - duration * 60 * 1000 / interval;

		if (end < 0) {
			throw new IllegalArgumentException("illegal argument, none data, end:" + end);
		}
		if (begin < 0) {
			begin = 0;
		}

		int eachViewCount;
		if ((end - begin) >= viewCount) {
			eachViewCount = (end - begin) / viewCount;
		} else {
			eachViewCount = 1;
		}

		int realViewInterval = eachViewCount * interval;

		List<Long> result = new ArrayList<Long>();

		int step = eachViewCount;
		for (int i = begin; i < end; i += eachViewCount) {

			int next = i + eachViewCount;
			if (next >= count) {
				next = count - 1;
				step = next - i;
			}

			ReqStatus rsb = (ReqStatus) reqstatus[i];
			ReqStatus rse = (ReqStatus) reqstatus[next];

			long qps = getQps(rse, rsb, step);
			result.add(qps);
			if (step != eachViewCount) {
				break;
			}
		}

		dw.setInterval(realViewInterval);
		dw.setData(result.toArray(new Long[0]));
		dw.setTotal(((ReqStatus) reqstatus[reqstatus.length - 1]).getReqTotal());
		return dw;
	}

	private long getQps(ReqStatus rse, ReqStatus rsb, int step) {

		if (rsb.getReqTotal() == 0) {
			//防止短时出问题，数据抖动
			return 0;
		}
		long result = (rse.getReqTotal() - rsb.getReqTotal()) / (step * interval / 1000);
		if (result < 0) {
			//1、短时出现问题，数据变为0
			//2、重启，数据重新统计
			return 0;
		}
		return result;
	}

}
