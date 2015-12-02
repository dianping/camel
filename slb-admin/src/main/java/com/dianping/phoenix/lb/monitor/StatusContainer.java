package com.dianping.phoenix.lb.monitor;

import com.dianping.phoenix.lb.action.search.Matcher;
import com.dianping.phoenix.lb.api.manager.ThreadPoolManager;
import com.dianping.phoenix.lb.model.entity.Instance;
import com.dianping.phoenix.lb.model.entity.SlbPool;
import com.dianping.phoenix.lb.monitor.NodeStatus.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Service
public class StatusContainer extends AbstractContainer {

	@Autowired
	private TengineStatusService tengineStatusService;

	@Autowired
	private ThreadPoolManager threadPoolManager;

	private Map<String, TengineStatus> statusMap = new ConcurrentHashMap<String, TengineStatus>();

	@Resource(name = "globalThreadPool")
	private Executor executors;

	@PostConstruct
	public void postConstruct() {
	}

	/**
	 * 启动一个定时任务检查状态, 每隔5*1000ms更新一次状态
	 */
	@Scheduled(fixedDelay = 5 * 1000)
	public void report() {

		doCheckAnWait(executors);
	}

	@Override
	protected void doDengineTask(SlbPool slbPool, Instance instance) throws Exception {

		String ip = instance.getIp();
		try {
			TengineStatus tengineStatus = tengineStatusService.getStatus(ip);
			statusMap.put(ip, tengineStatus);
		} catch (Exception e) {
			statusMap.remove(ip);
			throw e;
		}
	}

	/**
	 * @param key ip:port
	 * @return
	 */
	public List<ApiResult> getStatus(String key) {
		List<ApiResult> re = new ArrayList<ApiResult>();
		for (TengineStatus tengineStatus : statusMap.values()) {

			ApiResult result = null;

			Map<String, UpstreamStatus> upstreamStatusMap = tengineStatus.getUpstreamStatusMap();
			if (upstreamStatusMap != null) {

				for (UpstreamStatus upstreamStatus : upstreamStatusMap.values()) {
					Map<String, NodeStatus> nodeStatusMap = upstreamStatus.getNodeStatus();
					NodeStatus nodeStatus = nodeStatusMap.get(key);
					if (nodeStatus != null) {
						result = new ApiResult();
						result.setTengineIp(tengineStatus.getIp());
						result.setAvailableRate(upstreamStatus.getAvailableRate());
						result.setNodeStatus(nodeStatus.getStatus());
						result.setUpstreamName(upstreamStatus.getName());
						break;
					}
				}
			}
			if (result != null) {
				re.add(result);
			}
		}
		return re;

	}

	public List<ApiResult> getStatus(String nodeIp, int port) {
		// 遍历所有node
		String key = nodeIp + ":" + port;
		return getStatus(key);
	}

	/**
	 * 展示所有信息
	 * upstream ip status
	 *
	 * @param matcher 查看特定数据是否符合要求
	 * @return
	 */
	public List<ShowResult> getStatusForShow(Matcher matcher) {

		List<ShowResult> result = new LinkedList<StatusContainer.ShowResult>();

		for (TengineStatus tengineStatus : statusMap.values()) {

			Map<String, UpstreamStatus> upstreamStatusMap = tengineStatus.getUpstreamStatusMap();
			for (UpstreamStatus upstreamStatus : upstreamStatusMap.values()) {
				//每个node状态
				Map<String, NodeStatus> nodes = upstreamStatus.getNodeStatus();
				for (NodeStatus node : nodes.values()) {

					ShowResult sr = new ShowResult(node.getName(), tengineStatus.getIp());
					sr.setStatus(node.getStatus());
					sr.setUpstream(upstreamStatus.getName(), upstreamStatus.getAvailableRate());
					if (matcher.match(sr)) {
						result.add(sr);
					}
				}
			}
		}

		return result;
	}

	public static class ShowResult {

		private String dengine;

		private String upstream;//upstream name 

		private String address; //ip:port

		private String status;//up down

		private String availableRate;

		public ShowResult(String address, String dengine) {
			this.address = address;
			this.dengine = dengine;
		}

		public void setUpstream(String upstream, int availableRate) {

			this.upstream = upstream;
			this.availableRate = String.valueOf(availableRate);
		}

		public String getUpstream() {

			return upstream;
		}

		public String getAddress() {
			return address;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(Status status) {

			this.status = status.toString();
		}

		public String getAvailableRate() {
			return availableRate;
		}

		public void setAvailableRate(String availableRate) {
			this.availableRate = availableRate;
		}

		public String getDengine() {
			return dengine;
		}

		public void setDengine(String dengine) {
			this.dengine = dengine;
		}

	}
}
