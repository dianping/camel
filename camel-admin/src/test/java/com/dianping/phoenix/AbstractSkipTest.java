package com.dianping.phoenix;

import org.junit.Rule;

/**
 * 如果单元测试只能在本地跑，需要扩展此抽象类
 *
 * @author mengwenchao
 *         <p/>
 *         2014年8月13日 下午1:43:21
 */
public abstract class AbstractSkipTest extends AbstractTest {

	//加上如下rule，则不运行此单元测试
	//这些单元测试需要在本地跑
	@Rule
	public SkipTest skipTest = new SkipTest();

}
