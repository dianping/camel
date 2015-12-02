package com.dianping.phoenix.lb.service.model;

import com.dianping.phoenix.lb.AbstractSpringTest;
import com.dianping.phoenix.lb.dao.ModelStore;
import com.dianping.phoenix.lb.dao.VirtualServerDao;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Leo Liang
 */
public class VirtualServerServiceImplTest extends AbstractSpringTest {

	private VirtualServerService virtualServerService;

	private VirtualServerDao virtualServerDao;

	private ModelStore modelStore;

	@Before
	public void before() throws Exception {

		virtualServerService = getApplicationContext().getBean(VirtualServerServiceImpl.class);
		virtualServerDao = getApplicationContext().getBean(VirtualServerDao.class);
		modelStore = getApplicationContext().getBean(ModelStore.class);

	}

	@After
	public void after() throws Exception {
	}

	@Test
	public void testNginxConf() throws Exception {
		/**
		 *
		 <location match-type="prefix" pattern="/testNginxConfig1">
		 <directive type="proxy_pass" pool-name="Web.Index" />
		 </location>
		 <location match-type="prefix" pattern="/testNginxConfig2">
		 <directive type="custom" value="proxy_pass http://www.baidu.com" />
		 </location>
		 <location match-type="prefix" pattern="/testNginxConfig3">
		 <directive type="custom" value="proxy_pass http://Web.Tuangou" />
		 </location>
		 <location match-type="prefix" pattern="/testNginxConfig4">
		 <directive type="ifelse" if-statement="proxy_pass http://Web.Tuangou" if-condition="$cookie_cy = &quot;2335&quot;"/>
		 </location>
		 <location match-type="prefix" pattern="/testNginxConfig5">
		 <directive type="ifelse" if-statement="proxy_pass http://www.baidu.com" if-condition="$cookie_cy = &quot;2335&quot;"/>
		 </location>
		 */

		String nginxConfig = virtualServerService
				.generateNginxConfig(virtualServerDao.find(WWW_VS), modelStore.listPools(),
						modelStore.listCommonAspects(), modelStore.listVariables(), modelStore.listStrategies());
		String result[] = new String[] { "dp_domain www.dianping.com.Web.Index;", "proxy_pass http://www.baidu.com",
				"proxy_pass http://" + WWW_VS + "." + "Web.Tuangou", "proxy_pass http://www.dianping.com.Web.Tuangou;",
				"proxy_pass http://www.baidu.com" };
		for (int i = 1; i <= 5; i++) {
			Pattern p = Pattern.compile("/testNginxConfig" + i + ".*?\\{.*?\\}", Pattern.DOTALL);
			Matcher m = p.matcher(nginxConfig);
			String targetLocation = "";

			while (m.find()) {
				targetLocation = m.group();
			}

			System.out.println(targetLocation);

			Assert.assertTrue(targetLocation.indexOf(result[i - 1]) >= 0);
		}
	}
}
