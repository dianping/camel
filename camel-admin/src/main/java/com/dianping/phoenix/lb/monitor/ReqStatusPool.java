package com.dianping.phoenix.lb.monitor;

import com.dianping.phoenix.lb.api.Startable;
import com.dianping.phoenix.lb.api.Stoppable;
import com.dianping.phoenix.lb.api.cleanable;
import com.dianping.phoenix.lb.model.entity.Instance;
import com.dianping.phoenix.lb.model.entity.SlbPool;
import com.dianping.phoenix.lb.monitor.ReqStatusContainer.DataWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 存储每个Pool里面的Dengine监控信息
 *
 * @author mengwenchao
 *         <p/>
 *         2014年7月7日 下午5:34:10
 */
public class ReqStatusPool implements cleanable, Startable, Stoppable {

	private static final Logger logger = LoggerFactory.getLogger(ReqStatusPool.class);
	private SlbPool pool;
	private String poolName;
	private int keepInMemoryHours = 1;
	private int interval;
	private Map<String, ReqStatusDengine> reqStatusPool;
	private ScheduledExecutorService scheduledExecutor;
	private String reqStatusAddress;

	public ReqStatusPool(SlbPool pool, ScheduledExecutorService scheduledExecutor, String reqStatusAddress,
			int keepInMemoryHours, int interval) {

		this.poolName = pool.getName();
		this.pool = pool;
		this.scheduledExecutor = scheduledExecutor;
		this.reqStatusAddress = reqStatusAddress;
		this.keepInMemoryHours = keepInMemoryHours;
		this.interval = interval;
		this.reqStatusPool = new HashMap<String, ReqStatusDengine>();
	}

	public synchronized void addReqStatus(String ip, ReqStatusDengine reqStatusTengine) {

		if (logger.isInfoEnabled()) {
			logger.info("[addReqStatus]" + ip);
		}

		if (reqStatusPool.get(ip) != null) {
			logger.error("[addReqStatus][already exist]" + ip);
			return;
		}

		reqStatusPool.put(ip, reqStatusTengine);
		start(ip);
	}

	public synchronized Object removeReqStatus(String ip) {
		if (logger.isInfoEnabled()) {
			logger.info("[removeReqStatus]" + ip);
		}

		stop(ip);

		return reqStatusPool.remove(ip);
	}

	/**
	 * pool修改之后，比较响应的ip，做处理
	 *
	 * @param newPool
	 */
	public void poolModified(SlbPool newPool) {

		List<Instance> instances = newPool.getInstances();
		Set<String> newIps = new HashSet<String>();

		for (Instance instance : instances) {
			String ip = instance.getIp();
			ReqStatusDengine reqStatusDengine = reqStatusPool.get(ip);
			if (reqStatusDengine == null) {
				//增加或者修改的ip 
				addReqStatus(ip,
						new ReqStatusDengine(scheduledExecutor, ip, reqStatusAddress, keepInMemoryHours, interval));
			}

			newIps.add(ip);
		}

		for (String ip : reqStatusPool.keySet()) {
			if (!newIps.contains(ip)) {
				//删除
				removeReqStatus(ip);
			}
		}
	}

	@Override
	public void clean() {

		for (ReqStatusDengine reqStatusTengine : reqStatusPool.values()) {
			reqStatusTengine.clean();
		}

	}

	/**
	 * 动态添加节点，启动数据抓取
	 *
	 * @param ip
	 */
	private void start(String ip) {
		reqStatusPool.get(ip).start();
	}

	@Override
	public void start() {

		for (Instance member : pool.getInstances()) {
			addReqStatus(member.getIp(),
					new ReqStatusDengine(scheduledExecutor, member.getIp(), reqStatusAddress, keepInMemoryHours,
							interval));
		}

	}

	/**
	 * 动态删除节点，停止数据抓取
	 *
	 * @param ip
	 */
	private void stop(String ip) {

		reqStatusPool.get(ip).stop();
	}

	@Override
	public void stop() {

		for (ReqStatusDengine reqStatusDengine : reqStatusPool.values()) {
			reqStatusDengine.stop();
		}
	}

	public List<DataWrapper> getViewData(int viewCount, int duration, long viewEndTime) {

		List<DataWrapper> result = new ArrayList<DataWrapper>();

		for (ReqStatusDengine reqStatusDengine : reqStatusPool.values()) {

			result.add(reqStatusDengine.getViewData(viewCount, duration, viewEndTime));
		}

		return result;
	}

	public String getPoolName() {
		return poolName;
	}

	public void setPoolName(String poolName) {
		this.poolName = poolName;
	}

	public int getKeepInMemoryHours() {
		return keepInMemoryHours;
	}

	public void setKeepInMemoryHours(int keepInMemoryHours) {
		this.keepInMemoryHours = keepInMemoryHours;
	}

	public int getInterval() {
		return interval;
	}

	public void setInterval(int interval) {
		this.interval = interval;
	}

	public Map<String, ReqStatusDengine> getReqStatusPool() {
		return reqStatusPool;
	}
}
