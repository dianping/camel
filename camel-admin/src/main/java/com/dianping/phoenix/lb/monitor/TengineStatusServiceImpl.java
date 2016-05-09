package com.dianping.phoenix.lb.monitor;

import com.dianping.phoenix.lb.monitor.NodeStatus.Status;
import com.dianping.phoenix.lb.utils.IOUtilsWrapper;
import com.dianping.phoenix.lb.utils.JsonBinder;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TengineStatusServiceImpl implements TengineStatusService {

	private String encoding = "UTF-8";

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		// TengineStatusServiceImpl service = new TengineStatusServiceImpl();
		// TengineStatus status = service.getStatus("127.0.0.1");
		// System.out.println(status);

		String jsonString = IOUtilsWrapper
				.convetStringFromRequest(new FileInputStream("/home/atell/desktop/test.json"));
		jsonString = StringUtils.remove(jsonString, "\n");
		jsonString = StringUtils.remove(jsonString, "\r");
		if (StringUtils.endsWithIgnoreCase(jsonString, ",  ]}}")) {
			jsonString = jsonString.substring(0, jsonString.length() - 6) + " ]}}";
		}
		HashMap<String, Object> map = JsonBinder.getNonNullBinder().fromJson(jsonString, HashMap.class);
		System.out.println(map);

	}

	@SuppressWarnings("unchecked")
	public TengineStatus getStatus(String tengineIp) throws IOException {
		String url = "http://" + tengineIp + ":6666/status?format=json";
		String jsonString = HttpClientUtil.getAsString(url, null, encoding);
		if (StringUtils.isBlank(jsonString)) {
			throw new IllegalArgumentException("result is empty");
		}
		// 返回的结果在值字符串内竟然有换行，如下（所以需要过滤换行符）
		// (1) {"index": 22, "upstream": "cnc.www.dianping.com.www_recommender-web", "name": "10.2.8.175:80", "status": "down",
		// "rise": 0,
		// "fall": 1418736, "type": "tcp", "por
		// t": 0},
		// (2) 返回的json竟然 以 “, ]}}” 结尾，手工替换掉
		jsonString = StringUtils.remove(jsonString, "\n");
		jsonString = StringUtils.remove(jsonString, "\r");
		if (StringUtils.endsWithIgnoreCase(jsonString, ",  ]}}")) {
			jsonString = jsonString.substring(0, jsonString.length() - 6) + " ]}}";
		}
		HashMap<String, Object> map = JsonBinder.getNonNullBinder().fromJson(jsonString, HashMap.class);

		TengineStatus tengineStatus = new TengineStatus();
		tengineStatus.setIp(tengineIp);

		convert(tengineStatus, map);

		return tengineStatus;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void convert(TengineStatus tengineStatus, HashMap<String, Object> map) {
		Map serversMap = (Map) map.get("servers");

		Integer total = (Integer) serversMap.get("total");
		tengineStatus.setTotalNode(total);

		Map<String, UpstreamStatus> upstreamStatusMap = new HashMap<String, UpstreamStatus>();
		List<Map> upstreamMaps = (List<Map>) serversMap.get("server");
		if (upstreamMaps != null && upstreamMaps.size() > 0) {
			for (Map upstreamMap : upstreamMaps) {
				String upstreamName = (String) upstreamMap.get("upstream");
				UpstreamStatus upstreamStatus = upstreamStatusMap.get(upstreamName);

				if (upstreamStatus == null) {
					upstreamStatus = new UpstreamStatus();
					upstreamStatus.setName(upstreamName);
					upstreamStatusMap.put(upstreamName, upstreamStatus);
				}

				String nodeName = (String) upstreamMap.get("name");
				NodeStatus.Status status = NodeStatus.Status.valueOf((String) upstreamMap.get("status"));
				Integer rise = (Integer) upstreamMap.get("rise");
				Integer fall = (Integer) upstreamMap.get("fall");
				String type = (String) upstreamMap.get("type");
				Integer port = (Integer) upstreamMap.get("port");
				NodeStatus nodeStatus = new NodeStatus();
				nodeStatus.setName(nodeName);
				nodeStatus.setFall(fall);
				nodeStatus.setPort(port);
				nodeStatus.setRise(rise);
				nodeStatus.setStatus(status);
				nodeStatus.setType(type);

				Map<String, NodeStatus> nodeStatusMap = upstreamStatus.getNodeStatus();
				if (nodeStatusMap == null) {
					nodeStatusMap = new HashMap<String, NodeStatus>();
					upstreamStatus.setNodeStatus(nodeStatusMap);
				}

				nodeStatusMap.put(nodeName, nodeStatus);

			}
		}
		tengineStatus.setUpstreamStatusMap(upstreamStatusMap);

		// 计算 availableRate
		Collection<UpstreamStatus> upstreamStatusList = upstreamStatusMap.values();
		for (UpstreamStatus upstreamStatus : upstreamStatusList) {
			int upCount = 0;
			Map<String, NodeStatus> nodeStatusMap = upstreamStatus.getNodeStatus();
			for (NodeStatus nodeStatus : nodeStatusMap.values()) {
				if (nodeStatus.getStatus() == Status.up) {
					upCount++;
				}
			}
			int availableRate = upCount * 100 / nodeStatusMap.size();
			upstreamStatus.setAvailableRate(availableRate);
		}
	}

}
