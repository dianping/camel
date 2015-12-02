package com.dianping.phoenix.lb.deploy.agent;

/**
 * agentClient內部需要使用service(比如访问tag，生成config)，使用ioc框架的get的方式去獲取（
 * PlexusComponentContainer.INSTANCE.lookup(ConfigManager.class);）
 * service改爲使用plexus，action使用plexcus獲取service
 * <p/>
 * agenCleint里面不会使用线程。发布操作在execute方法里一次性执行。
 */
public interface AgentClient {
	/**
	 * 执行发布运行
	 */
	void execute();

	AgentClientResult getResult();
}
