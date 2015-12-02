package com.dianping.phoenix.lb.api.action;

/**
 * 对外提供的调用api
 *
 * @author mengwenchao
 *         <p/>
 *         2014年11月11日 下午2:51:54
 */
public interface Api {

	/**
	 * 添加节点<br>
	 * usage:<br>
	 * post http://localhost:8080/api/pool/<localhost>/updateMember 其中<localhost>是pool名称<br>
	 * <p/>
	 * post内容(要求是json格式)：<br>
	 * [{"name":"a", "ip":"125.2.3.2"}] <br>
	 * 其中name是节点名称，ip是节点地址，另外还有可选属性，端口port(默认是80)，权重weight（默认是100），最大失败次数maxFails（默认是3），失败超时时间failTimeout（2s），状态state（枚举值：ENABLED,
	 * DISABLED, FORCED_OFFLINE）
	 * <br/>
	 * 如果指定name，则修改对应name的节点
	 * 如果只有ip，则修改对应ip的节点
	 */
	String updateMember() throws Exception;

	/**
	 * 添加节点<br>
	 * usage:<br>
	 * post http://localhost:8080/api/pool/<localhost>/addMember 其中<localhost>是pool名称<br>
	 * <p/>
	 * post内容(要求是json格式)：<br>
	 * [{"name":"a", "ip":"125.2.3.2"}] <br>
	 * 其中name是节点名称，ip是节点地址，另外还有可选属性，端口port(默认是80)，权重weight（默认是100），最大失败次数maxFails（默认是3），失败超时时间failTimeout（2s），状态state（枚举值：ENABLED,
	 * DISABLED, FORCED_OFFLINE）
	 */
	String addMember() throws Exception;

	/**
	 * 删除节点<br>
	 * usage:<br>
	 * post http://localhost:8080/api/pool/<localhost>/delMember 其中<localhost>是pool名称<br>
	 * <p/>
	 * post内容(要求是json格式)：<br>
	 * ["a","b"] , 其中a，b都是节点名称 <br>
	 * <p/>
	 * 响应结果：<br>
	 * 正确示例： {"errorCode":0} <br>
	 * 错误示例： {"message":"Pool localhost has no member.","errorCode":-1} <br>
	 */
	String delMember() throws Exception;

	/**
	 * 发布特定的pool，将此pool关联的所有vs一起发布
	 *
	 * @return
	 * @throws Exception
	 */
	String deploy() throws Exception;

	/**
	 * 删除vs站点<br/>
	 * 为单元测试，不对外发布<br/>
	 * post http://localhost:8080/api/vs/{vsName}/del
	 *
	 * @return
	 */
	String delVs();

	/**
	 * 部署站点<br/>
	 * 为单元测试，不对外发布<br/>
	 * post http://localhost:8080/api/vs/{vsName}/deploy
	 *
	 * @return
	 */
	String deployVs();

	/**
	 * 添加vs站点<br>
	 * VirtualServerServiceImpl usage:<br>
	 * post http://localhost:8080/api/vs/add <br>
	 * <p/>
	 * post内容(要求是json格式)：<br>
	 * {"name":"paas.dianping.com","slbPool":"paas-pool"} , 其中vs是站点名称(必填)，slbPool是路由机器的组名(可选，默认是paas-pool) <br>
	 * <p/>
	 * 响应结果：<br>
	 * 正确示例： {"errorCode":0} <br>
	 * 错误示例： {"message":"Vs ... already exists.","errorCode":-1} <br>
	 */
	String addVs() throws Exception;

	/**
	 * 添加Pool(如果pool已经存在，则是追加member)<br>
	 * usage:<br>
	 * post http://localhost:8080/api/pool/add <br>
	 * <p/>
	 * post内容(要求是json格式)：<br>
	 * {"name":"test","members":[{"ip":"10.1.1.1"}]} , 其中name是pool名称(必填)，members参数(可选)中可以是多个节点，节点ip使用ip参数，节点名称是name参数(不填默认是ip_port命名) <br>
	 * <p/>
	 * 响应结果：<br>
	 * 正确示例： {"errorCode":0} <br>
	 * 错误示例： {"message":"Post body cannot be empty.","errorCode":-1} <br>
	 * 错误示例： {"message":"Pool name cannot be empty.","errorCode":-1} <br>
	 * 错误示例： {"message":"Member'ip cannot be empty.","errorCode":-1} <br>
	 * 错误示例： {"message":"Pool xxx already exists.","errorCode":-1} <br>
	 */
	String addPool() throws Exception;

	/**
	 * 删除Pool(如果pool已经被站点使用，则不可删除)<br>
	 * usage:<br>
	 * post http://localhost:8080/api/pool/{pool}/delete <br>
	 * <p/>
	 * 响应结果：<br>
	 * 正确示例： {"errorCode":0} <br>
	 * 错误示例： {"message":"Pool name cannot be empty.","errorCode":-1} <br>
	 * 错误示例： {"message":"Can't delete this pool, because these VirtualServers use it: ...","errorCode":-1} <br>
	 */
	String delPool() throws Exception;

	/**
	 * 获取Pool信息<br>
	 * usage:<br>
	 * post http://localhost:8080/api/pool/{pool}/get <br>
	 * <p/>
	 * 响应结果：<br>
	 * 正确示例： {"errorCode":0, "pool":{}} <br>
	 * 错误示例： {"message":"Pool name cannot be empty.","errorCode":-1} <br>
	 * 错误示例： {"message":"Can't delete this pool, because these VirtualServers use it: ...","errorCode":-1} <br>
	 */
	String getPool() throws Exception;

	/**
	 * 获取所有pool列表
	 * 比如：
	 * get http://localhost:8080/api/pools/get
	 *
	 * @return
	 */
	String listPoolNames();

	/**
	 * 特定特定应用名以及端口号，获取此应用健康监测的状态<br>
	 * 比如：
	 * get http://localhost:8080/api/monitor/status?nodeIp=10.1.1.79&nodePort=81
	 *
	 * @return
	 * @throws Exception
	 */
	String status() throws Exception;

	/**
	 * 获取所有upstream的状态 <br/>
	 * get http://localhost:8080/api/upstreamStatus
	 *
	 * @return
	 */
	String upstreamStatus();

	/**
	 * 强制upstream升级、降级、或者自动
	 * 比如：
	 * 对于将pool：pool1升级<br/>
	 * post http://localhost:8080/api/pool/forcestate/poo1/up
	 * <p/>
	 * 支持：up/down/auto
	 *
	 * @return
	 */
	String forceUpstreamState();

	/**
	 * 获取所有server列表
	 * 比如：
	 * get http://localhost:8080/api/servers/get
	 *
	 * @return
	 */
	String listServers();

	/**
	 * 接收Nginx log
	 * usage:
	 * post http://localhost:8080/api/nginx/log/addLog
	 * <p/>
	 * post内容(要求是json格式)：
	 * [ {"server-address":"192.168.222.77","time":"2015-03-25 11:26:00","request-method":"POST","status":404,"url":"http:\/\/m.api.51ping.com\/mobile-watch-auto-ga.js","pool":"m.api.51ping.com.mobile-api-web","upstream-server":"192.168.211.162:80"} ]
	 * 响应结果：<br>
	 * 正确示例： {"errorCode":0} <br>
	 * 错误示例： {"message":"logs cannot be empty.","errorCode":-1} <br>
	 */
	String receiveNginxLogs() throws Exception;

	/**
	 * 由pool名获取vs及path列表
	 * usage:
	 * get/post http://localhost:8080/api/vs_path/{poolName}
	 * 响应结果：
	 * 正确示例： { errorCode: 0, influencingVsList: [ { positionDescs: [ "/testNginxConfig4", "/testNginxConfig3" ], vsName: "www.dianping.com" } ] }
	 * 错误示例： {"message":"pool name cannot be empty.","errorCode":-1}
	 */
	String listVsAndPath() throws Exception;

	/**
	 * 获取所有的pool与ip对应列表
	 * usage:
	 * get/post http://localhost:8080/api/pools/poolMemberDict
	 * 响应结果：
	 * 正确示例： { errorCode: 0, poolMemberDict: {"poolName1":[member1,member2]} }
	 * 错误示例： {"message":"error message.","errorCode":-1}
	 */
	String listPoolMemberDict() throws Exception;
}
