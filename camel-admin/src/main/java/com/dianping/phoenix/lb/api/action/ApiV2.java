package com.dianping.phoenix.lb.api.action;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface ApiV2 {

	/**
	 * 添加节点 usage: post http://localhost:8080/api/v2/pool/<localhost>/updateMember 其中<localhost>是pool名称<br>
	 * <p/>
	 * post内容(要求是json格式)：<br>
	 * [{"name":"a", "ip":"125.2.3.2"}] <br>
	 * 其中name是节点名称，ip是节点地址，另外还有可选属性，端口port(默认是80)，权重weight（默认是100），最大失败次数maxFails（默认是3），失败超时时间failTimeout（2s），状态state（枚举值：ENABLED,
	 * DISABLED, FORCED_OFFLINE） <br/>
	 * 如果指定name，则修改对应name的节点 如果只有ip，则修改对应ip的节点
	 */
	String updateMember() throws Exception;

	/**
	 * 添加节点<br>
	 * usage:<br>
	 * post http://localhost:8080/api/v2/pool/<localhost>/addMember 其中<localhost>是pool名称<br>
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
	 * post http://localhost:8080/api/v2/pool/<localhost>/delMember 其中<localhost>是pool名称<br>
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
	 * 编辑节点并发布<br>
	 * usage:<br>
	 * post http://localhost:8080/api/v2/pool/<localhost>/updateMemberAndDeploy 其中<localhost>是pool名称<br>
	 * <p/>
	 * post内容(要求是json格式)：<br>
	 * [{"name":"a", "ip":"125.2.3.2"}] <br>
	 * 其中name是节点名称，ip是节点地址，另外还有可选属性，端口port(默认是80)，权重weight（默认是100），最大失败次数maxFails（默认是3），失败超时时间failTimeout（2s），状态state（枚举值：ENABLED,
	 * DISABLED, FORCED_OFFLINE）
	 * <br/>
	 * 如果指定name，则修改对应name的节点
	 * 如果只有ip，则修改对应ip的节点
	 * <p/>
	 * 如果发布失败，则该pool会退回到原始状态
	 */
	String updateMemberAndDeploy() throws Exception;

	/**
	 * 添加节点并发布<br>
	 * usage:<br>
	 * post http://localhost:8080/api/v2/pool/<localhost>/addMemberAndDeploy 其中<localhost>是pool名称<br>
	 * <p/>
	 * post内容(要求是json格式)：<br>
	 * [{"name":"a", "ip":"125.2.3.2"}] <br>
	 * 其中name是节点名称，ip是节点地址，另外还有可选属性，端口port(默认是80)，权重weight（默认是100），最大失败次数maxFails（默认是3），失败超时时间failTimeout（2s），状态state（枚举值：ENABLED,
	 * DISABLED, FORCED_OFFLINE）
	 * <p/>
	 * 如果发布失败，则该pool会退回到原始状态
	 */
	String addMemberAndDeploy() throws Exception;

	/**
	 * 删除节点并发布<br>
	 * usage:<br>
	 * post http://localhost:8080/api/v2/pool/<localhost>/delMemberAndDeploy 其中<localhost>是pool名称<br>
	 * <p/>
	 * post内容(要求是json格式)：<br>
	 * ["a","b"] , 其中a，b都是节点名称 <br>
	 * <p/>
	 * 如果发布失败，则该pool会退回到原始状态
	 */
	String delMemberAndDeploy() throws Exception;

}
