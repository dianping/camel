package com.dianping.phoenix.lb.processor;

import com.dianping.phoenix.lb.api.processor.PreprocessBeforeGenerateNginxConfig;
import com.dianping.phoenix.lb.model.entity.Aspect;
import com.dianping.phoenix.lb.model.entity.SlbModelTree;
import com.dianping.phoenix.lb.model.entity.VirtualServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 在配置中加入全局配置，如果存在，则不做任何事情
 *
 * @author mengwenchao
 *         <p/>
 *         2014年12月10日 下午3:54:34
 */
@Component
public class InsertGlobalAspect implements PreprocessBeforeGenerateNginxConfig {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Value("${global.aspect.before.name}")
	private String globalAspectBefore;

	@Value("${global.aspect.after.name}")
	private String globalAspectAfter;

	@Override
	public void process(SlbModelTree slbModelTree) {

		if (slbModelTree.getVirtualServers().size() != 1) {
			throw new IllegalArgumentException(
					"VirtualServer size not 1, but " + slbModelTree.getVirtualServers().size());
		}

		VirtualServer virtualServer = (VirtualServer) slbModelTree.getVirtualServers().values().toArray()[0];

		if (shouldInsertGlobalAspectBefore(slbModelTree, virtualServer)) {
			Aspect aspect = new Aspect();
			aspect.setRef(globalAspectBefore);
			virtualServer.getAspects().add(0, aspect);
		}

		if (shouldInsertGlobalAspectAfter(slbModelTree, virtualServer)) {
			Aspect aspect = new Aspect();
			aspect.setRef(globalAspectAfter);
			virtualServer.addAspect(aspect);
		}
	}

	/**
	 * @param slbModelTree
	 * @param virtualServer
	 * @return
	 */
	private boolean shouldInsertGlobalAspectAfter(SlbModelTree slbModelTree, VirtualServer virtualServer) {

		if (exists(slbModelTree.getAspects(), globalAspectAfter) && !exists(virtualServer.getAspects(),
				globalAspectAfter)) {
			return true;
		}

		return false;
	}

	/**
	 * @param slbModelTree
	 * @param virtualServer
	 * @return
	 */
	private boolean shouldInsertGlobalAspectBefore(SlbModelTree slbModelTree, VirtualServer virtualServer) {

		if (exists(slbModelTree.getAspects(), globalAspectBefore) && !exists(virtualServer.getAspects(),
				globalAspectBefore)) {
			return true;
		}

		return false;
	}

	private boolean exists(List<Aspect> aspects, String aspectName) {

		for (Aspect aspect : aspects) {
			if (aspect.getName().equals(aspectName) || aspect.getRef().equals(aspectName)) {
				return true;
			}
		}
		return false;
	}
}
