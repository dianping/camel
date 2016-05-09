package com.dianping.phoenix.lb.dengine;

import com.dianping.phoenix.lb.api.dengine.DengineConfig;
import com.dianping.phoenix.lb.api.dengine.DengineDegradeApi;
import com.dianping.phoenix.lb.api.dengine.DengineManager;
import com.dianping.phoenix.lb.api.dengine.ForceState;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.Instance;
import com.dianping.phoenix.lb.model.entity.SlbPool;
import com.dianping.phoenix.lb.model.entity.VirtualServer;
import com.dianping.phoenix.lb.service.model.SlbPoolService;
import com.dianping.phoenix.lb.service.model.VirtualServerService;
import com.dianping.phoenix.lb.utils.PoolNameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年11月11日 下午3:23:21
 */
@Service
public class DefaultDengineManager implements DengineManager {

	@Autowired
	private VirtualServerService virtualServiceService;

	@Autowired
	private SlbPoolService slbPoolService;

	@Autowired
	private DengineConfig dengineConfig;

	@Override
	public void forceState(String poolName, ForceState state) throws BizException, DengineException {

		List<String> vss = virtualServiceService.findVirtualServerByPool(poolName);
		Map<String, List<String>> force = new HashMap<String, List<String>>();

		for (String vs : vss) {

			VirtualServer virtualServer = virtualServiceService.findVirtualServer(vs);
			String slbPoolName = virtualServer.getSlbPool();
			SlbPool slbPool = slbPoolService.findSlbPool(slbPoolName);
			for (Instance instance : slbPool.getInstances()) {
				String dengineIp = instance.getIp();
				List<String> upstreams = force.get(dengineIp);
				if (upstreams == null) {
					upstreams = new LinkedList<String>();
					force.put(dengineIp, upstreams);
				}
				upstreams.add(PoolNameUtils.rewriteToPoolNamePrefix(virtualServer.getName(), poolName));
			}
		}

		for (String dengineIp : force.keySet()) {
			List<String> upstreams = force.get(dengineIp);
			DengineDegradeApi dengineDegradeApi = new DefaultDengineDegradeApi(dengineIp, dengineConfig);
			dengineDegradeApi.force(upstreams, state);
		}
	}

}
