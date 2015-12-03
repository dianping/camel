package com.dianping.phoenix.lb.monitor;

import com.dianping.phoenix.lb.api.Startable;
import com.dianping.phoenix.lb.api.util.Observable;
import com.dianping.phoenix.lb.api.util.Observer;
import com.dianping.phoenix.lb.model.entity.SlbPool;
import com.dianping.phoenix.lb.utils.ElementAdded;
import com.dianping.phoenix.lb.utils.ElementModified;
import com.dianping.phoenix.lb.utils.ElementRemoved;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dengine监控入口
 *
 * @author mengwenchao
 *         <p/>
 *         2014年7月7日 下午5:34:25
 */
@Service
public class ReqStatusContainer extends AbstractContainer implements Startable, Observer {

	private static String ReqStatusThreadPoolName = "REQ_STATUS_THREAD_POOL";

	@Autowired
	private TengineStatusService tengineStatusService;

	@Value("${monitor.reqstat.interval}")
	private int interval;

	@Value("${monitor.on}")
	private boolean monitorOn;

	@Value("${monitor.reqstat.keepInMemoryHours}")
	private int keepInMemoryHours = 24;

	/**
	 * 6666/reqstatus
	 */
	@Value("${monitor.reqstat.reqStatusAddress}")
	private String reqStatusAddress;

	private Map<String, ReqStatusPool> reqStatus;
	private ScheduledExecutorService scheduledExecutor;

	{
		reqStatus = new ConcurrentHashMap<String, ReqStatusPool>();
	}

	{
		scheduledExecutor = Executors.newScheduledThreadPool(20, new ThreadFactory() {

			AtomicInteger count = new AtomicInteger();

			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setName(ReqStatusThreadPoolName + "-" + count.incrementAndGet());
				return t;
			}
		});
	}

	/**
	 * slbPoll变化时，同步更新
	 */
	@Override
	public void update(Observable o, Object args) {

		if (args instanceof ElementAdded) {
			addPool((SlbPool) ((ElementAdded) args).getElement());
			return;
		}

		if (args instanceof ElementRemoved) {
			removePool((String) ((ElementRemoved) args).getElement());
			return;
		}
		if (args instanceof ElementModified) {
			ElementModified elementModified = (ElementModified) args;

			modifyPool((SlbPool) elementModified.getOldElement(), (SlbPool) elementModified.getNewElement());
			return;
		}

		throw new IllegalArgumentException("unknown type:" + args.getClass());
	}

	/**
	 * pool改变了，对应处理响应Dengine的监控
	 *
	 * @param oldElement
	 * @param newElement
	 */
	private void modifyPool(SlbPool oldElement, SlbPool newElement) {

		String poolName = newElement.getName();

		ReqStatusPool reqStatusPool = reqStatus.get(poolName);

		if (reqStatusPool == null) {
			logger.warn("[modifyPool][pool not exist]" + poolName);
			return;
		}

		reqStatusPool.poolModified(newElement);

	}

	/**
	 * 删除针对pool的监控
	 *
	 * @param poolName
	 */
	private void removePool(String poolName) {

		ReqStatusPool reqStatusPool = reqStatus.remove(poolName);

		if (reqStatusPool == null) {
			logger.warn("[removePool][pool not exist]" + poolName);
			return;
		}

		reqStatusPool.stop();
	}

	/**
	 * @param poolName    需要获取的pool名字
	 * @param dataDengine 对应返回数据的Dengine ip
	 * @return
	 */
	public List<DataWrapper> getDataForPool(int duration, int viewCount, long viewEndTime, String poolName) {
		Validate.isTrue(monitorOn, "monitor is shut down, check slb.properties");

		ReqStatusPool reqStatusPool = reqStatus.get(poolName);
		Validate.notNull(reqStatusPool);

		return reqStatusPool.getViewData(viewCount, duration, viewEndTime);
	}

	/**
	 * 全局数据，所有Dengine节点的qps值取平均
	 *
	 * @param duration 获取数据的时间长度
	 */
	public List<DataWrapper> getTotalData(int duration, int viewCount, long viewEndTime) {

		Validate.isTrue(monitorOn, "monitor is shut down, check slb.properties");

		Long[] sum = null;
		int count = 0;
		long total = 0;
		int realInterval = 0;
		for (String poolName : reqStatus.keySet()) {

			List<DataWrapper> poolData = getDataForPool(duration, viewCount, viewEndTime, poolName);
			for (DataWrapper dw : poolData) {
				total += dw.getTotal();
				realInterval = dw.getInterval();
			}
			count += poolData.size();
			Long[] tpSum = sum(poolData);
			if (sum == null) {
				sum = tpSum;
				continue;
			}
			sum = sum(sum, tpSum);
		}

		//求平均
		for (int i = 0; i < sum.length; i++) {
			sum[i] /= count;
		}

		DataWrapper all = new DataWrapper();
		all.setDesc("all");
		all.setData(sum);
		all.setTotal(total);
		all.setInterval(realInterval);
		List<DataWrapper> result = new ArrayList<DataWrapper>();
		result.add(all);
		return result;
	}

	private Long[] sum(Long[] sum, Long[] tpSum) {

		if (sum.length <= tpSum.length) {
			int sub = tpSum.length - sum.length;
			for (int i = 0; i < sum.length; i++) {
				sum[i] += tpSum[i + sub];
			}
			return sum;
		}

		int sub = sum.length - tpSum.length;
		for (int i = 0; i < tpSum.length; i++) {
			tpSum[i] += sum[i + sub];
		}
		return tpSum;
	}

	private Long[] sum(List<DataWrapper> poolData) {

		int length = Integer.MAX_VALUE;

		//长度可能不一致，取最小值
		for (DataWrapper dw : poolData) {
			if (dw.getData().length < length) {
				length = dw.getData().length;
			}
		}

		Long[] result = new Long[length];
		for (int i = 0; i < length; i++) {

			result[length - i - 1] = 0L;
			for (DataWrapper dw : poolData) {
				result[length - i - 1] += dw.getData()[dw.getData().length - 1 - i];
			}
		}

		return result;
	}

	/**
	 * 开启监控，每个Tengine执行对应监控动作
	 */
	@PostConstruct
	@Override
	public void start() {

		if (!monitorOn) {
			if (logger.isInfoEnabled()) {
				logger.info("[start][monitor is switched off, check slb.properties!]");
			}
			return;
		}

		if (logger.isInfoEnabled()) {
			logger.info("[start][begin start]");
		}

		//检测pool的变化，调整监控
		slbPoolService.addObserver(this);

		reqStatusAddress = reqStatusAddress.trim();
		List<SlbPool> pools = slbPoolService.listSlbPools();
		if (pools != null) {
			for (SlbPool pool : pools) {
				String poolName = pool.getName();

				if (StringUtils.isNotEmpty(poolName) && poolName.startsWith("dengine-web-sl-m")) {
					continue;
				} else {
					addPool(pool);
				}
			}
		}

		//data cleaner
		scheduledExecutor.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				clean();
			}
		}, 1, 1, TimeUnit.HOURS);

		if (logger.isInfoEnabled()) {
			logger.info("[start][end start]");
		}

	}

	private void addPool(SlbPool pool) {

		String poolName = pool.getName();
		ReqStatusPool reqStatusPool = reqStatus.get(poolName);

		if (reqStatusPool == null) {

			reqStatusPool = new ReqStatusPool(pool, scheduledExecutor, reqStatusAddress, keepInMemoryHours, interval);
			initReqStatusPool(reqStatusPool);
			reqStatusPool.start();
			reqStatus.put(poolName, reqStatusPool);
		} else {

			throw new IllegalArgumentException("pool already exist." + poolName);
		}

	}

	/**
	 * 清除过期数据
	 */
	protected void clean() {

		logger.info("[clean][begin clean]");
		for (ReqStatusPool reqStatusPool : reqStatus.values()) {

			reqStatusPool.clean();
		}
		logger.info("[clean][end clean]");

	}

	private void initReqStatusPool(ReqStatusPool reqStatusPool) {

		reqStatusPool.setKeepInMemoryHours(keepInMemoryHours);
	}

	public static class DataWrapper {

		private String desc;

		private Long[] data;

		private int interval;

		/**
		 * 所有数据求和
		 */
		private long total;

		public Long[] getData() {
			return data;
		}

		public void setData(Long[] data) {
			this.data = data;
		}

		public String getDesc() {
			return desc;
		}

		public void setDesc(String desc) {
			this.desc = desc;
		}

		public long getTotal() {
			return total;
		}

		public void setTotal(long total) {
			this.total = total;
		}

		public int getInterval() {
			return interval;
		}

		public void setInterval(int interval) {
			this.interval = interval;
		}

	}

}
