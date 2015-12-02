package com.dianping.phoenix.lb.monitor;

import com.dianping.phoenix.lb.api.manager.ThreadPoolManager;
import com.dianping.phoenix.lb.model.entity.Instance;
import com.dianping.phoenix.lb.model.entity.SlbPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * 降级统计
 *
 * @author mengwenchao
 *         <p/>
 *         2014年9月5日 下午6:33:24
 */
@Service
public class DegradeStatusContainer extends AbstractContainer {

	private static final int argsLength = 8;
	@Value("${monitor.degrade.degradeStatusAddress}")
	private String degradeStatusAddress;
	private String BACKUP_SUFFIX = "#BACKUP";

	@Autowired
	private ThreadPoolManager threadPoolManager;

	/**
	 * dengineip :
	 * upstreamName:DegradeStatus
	 */
	private Map<String, Map<String, DegradeStatus>> statusMap;

	@Resource(name = "globalThreadPool")
	private ExecutorService executors;

	public DegradeStatusContainer() {

		statusMap = new ConcurrentHashMap<String, Map<String, DegradeStatus>>();
	}

	private static DegradeStatus getDegradeStatus(String[] args) {

		int i = 0;
		DegradeStatus ds = new DegradeStatus();

		ds.setUpstreamName(args[i++]);
		ds.setChecked(args[i++].equals("checked") ? true : false);
		ds.setDegradeState(Integer.parseInt(args[i++]));
		ds.setServerCount(Integer.parseInt(args[i++]));
		ds.setUpCount(Integer.parseInt(args[i++]));
		String degradeRate = args[i++];
		ds.setDegradeRate(Integer.parseInt(degradeRate.substring(0, degradeRate.indexOf("%"))));
		ds.setForceState(Integer.parseInt(args[i++].trim()));
		ds.setDeleted(Boolean.parseBoolean(args[i++]));

		return ds;
	}

	@PostConstruct
	public void postConstruct() {
	}

	@Scheduled(fixedDelay = 1000)
	public void report() {

		doCheckAnWait(executors);
	}

	@Override
	protected void doDengineTask(SlbPool slbPool, Instance instance) throws IOException {

		String ip = instance.getIp();
		String address = "http://" + ip + ":" + degradeStatusAddress;

		try {
			statusMap.put(slbPool.getName() + ":" + ip, getSingleDengine(address));
		} catch (Exception e) {
			logger.error("[doDengineTask]", e);
		}

	}

	/**
	 * 为了unit test调用，所以方法public
	 *
	 * @param slbPoolName
	 * @param ip
	 * @param status
	 * @return
	 * @throws IOException
	 */
	public Map<String, DegradeStatus> getSingleDengine(String address) throws IOException {

		String status = HttpClientUtil.getAsString(address, null, encoding);

		String[] upstreams = status.split("\n");
		Map<String, DegradeStatus> degradeStatus = new HashMap<String, DegradeStatus>();

		for (int i = 1; i < upstreams.length; i++) {

			String[] args = upstreams[i].split("[, ]+");
			if (args.length != argsLength) {
				throw new IllegalArgumentException("wrong result:" + upstreams[i]);
			}
			degradeStatus.put(args[0], getDegradeStatus(args));
		}

		return degradeStatus;
	}

	/**
	 * 降级数据，过滤掉没有配置BACKUP的数据
	 * dengineip:
	 * List<DegradeStatus>
	 */
	public List<DegradeStatusResult> getDegradeData() {

		List<DegradeStatusResult> result = new LinkedList<DegradeStatusContainer.DegradeStatusResult>();

		for (Entry<String, Map<String, DegradeStatus>> entry : statusMap.entrySet()) {

			String key = entry.getKey();
			int index = key.lastIndexOf(":");
			String slbPoolName = key.substring(0, index);
			String dengineIp = key.substring(index + 1);
			Map<String, DegradeStatus> degradeStatus = entry.getValue();

			for (String upstream : degradeStatus.keySet()) {

				DegradeStatus current = degradeStatus.get(upstream);
				if (current.isDeleted()) {
					continue;
				}

				DegradeStatus backup = degradeStatus.get(upstream + BACKUP_SUFFIX);
				if (backup != null) {
					//有降级pool
					result.add(createDegradeStatusResult(slbPoolName, dengineIp, degradeStatus.get(upstream), backup));
				}

			}
		}

		return result;
	}

	private DegradeStatusResult createDegradeStatusResult(String slbPoolName, String dengineIp,
			DegradeStatus degradeStatus, DegradeStatus degradeBackupStatus) {
		return new DegradeStatusResult(slbPoolName, dengineIp, degradeStatus, degradeBackupStatus);
	}

	public static class DegradeStatusResult {

		private String slbPoolName;

		private String dengineIp;

		private DegradeStatus degradeStatus;

		private DegradeStatus degradeBackupStatus;

		public DegradeStatusResult(String slbPoolName, String dengineIp, DegradeStatus degradeStatus,
				DegradeStatus degradeBackupStatus) {

			this.slbPoolName = slbPoolName;
			this.dengineIp = dengineIp;
			this.degradeStatus = degradeStatus;
			this.degradeBackupStatus = degradeBackupStatus;
		}

		public String getDengineIp() {
			return dengineIp;
		}

		public void setDengineIp(String dengineIp) {
			this.dengineIp = dengineIp;
		}

		public DegradeStatus getDegradeStatus() {
			return degradeStatus;
		}

		public void setDegradeStatus(DegradeStatus degradeStatus) {
			this.degradeStatus = degradeStatus;
		}

		public String getSlbPoolName() {
			return slbPoolName;
		}

		public void setSlbPoolName(String slbPoolName) {
			this.slbPoolName = slbPoolName;
		}

		public DegradeStatus getDegradeBackupStatus() {
			return degradeBackupStatus;
		}

		public void setDegradeBackupStatus(DegradeStatus degradeBackupStatus) {
			this.degradeBackupStatus = degradeBackupStatus;
		}

	}
}
