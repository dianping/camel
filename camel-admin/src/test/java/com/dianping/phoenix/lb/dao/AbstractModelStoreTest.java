package com.dianping.phoenix.lb.dao;

import com.dianping.phoenix.lb.constant.MessageID;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.PointCut;
import com.dianping.phoenix.lb.model.State;
import com.dianping.phoenix.lb.model.entity.*;
import com.dianping.phoenix.lb.model.transform.DefaultMerger;
import com.dianping.phoenix.lb.model.transform.DefaultSaxParser;
import com.dianping.phoenix.lb.utils.AdvEqualsBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年11月19日 上午10:48:21
 */
public abstract class AbstractModelStoreTest extends AbstractDaoTest {

	protected File baseDir;

	protected ModelStore store;

	protected List<String> excludeFields = Arrays.asList("m_lastModifiedDate");

	protected SlbModelTree slbBaseModelTree;

	protected SlbModelTree wwwSlbModelTree;

	protected SlbModelTree tuangouSlbModelTree;

	@Before
	public void beforeAbstractModelStoreTest() throws URISyntaxException, SAXException, IOException {

		URL url = getClass().getClassLoader().getResource("storeTest");
		baseDir = new File(url.toURI());

		slbBaseModelTree = DefaultSaxParser.parse(FileUtils.readFileToString(new File(baseDir, "slb_base.xml")));
		wwwSlbModelTree = DefaultSaxParser.parse(FileUtils.readFileToString(new File(baseDir, WWW_FILENAME)));
		tuangouSlbModelTree = DefaultSaxParser.parse(FileUtils.readFileToString(new File(baseDir, "slb_tuangou.xml")));

	}

	@Test
	public void testListVirtualServers() throws Exception {
		SlbModelTree wwwSlbModelTree = DefaultSaxParser
				.parse(FileUtils.readFileToString(new File(baseDir, WWW_FILENAME)));

		List<VirtualServer> expected = new ArrayList<VirtualServer>(
				tuangouSlbModelTree.getVirtualServers().values().size() + wwwSlbModelTree.getVirtualServers().values()
						.size());
		expected.addAll(tuangouSlbModelTree.getVirtualServers().values());
		expected.addAll(wwwSlbModelTree.getVirtualServers().values());

		List<VirtualServer> actual = store.listVirtualServers();

		assertEquals(expected, actual);
	}

	@Test
	public void testListStrategies() throws Exception {

		assertEquals(new ArrayList<Strategy>(slbBaseModelTree.getStrategies().values()), store.listStrategies());
	}

	@Test
	public void testListSlbPools() throws Exception {

		assertEquals(new ArrayList<SlbPool>(slbBaseModelTree.getSlbPools().values()), store.listSlbPools());
	}

	@Test
	public void testListCommonAspectss() throws Exception {

		assertEquals(slbBaseModelTree.getAspects(), store.listCommonAspects());
	}

	@Test
	public void testListPools() throws Exception {

		assertEquals(new ArrayList<Pool>(slbBaseModelTree.getPools().values()), store.listPools());
	}

	@Test
	public void testListVariables() {

		Assert.assertEquals(slbBaseModelTree.getVariables().size(), store.listVariables().size());
	}

	@Test
	public void testFindStrategy() throws Exception {
		Strategy expected = slbBaseModelTree.findStrategy("uri-hash");
		Assert.assertTrue(EqualsBuilder.reflectionEquals(expected, store.findStrategy("uri-hash"), true));
	}

	@Test
	public void testFindSlbPool() throws Exception {
		SlbPool expected = slbBaseModelTree.findSlbPool("test-pool");
		Assert.assertTrue(EqualsBuilder.reflectionEquals(expected, store.findSlbPool("test-pool"), true));
		expected = slbBaseModelTree.findSlbPool("test-pool2");
		Assert.assertTrue(EqualsBuilder.reflectionEquals(expected, store.findSlbPool("test-pool2"), true));
	}

	@Test
	public void testFindPool() throws Exception {

		Pool expected = slbBaseModelTree.findPool("Web.Tuangou");
		Assert.assertTrue(EqualsBuilder.reflectionEquals(expected, store.findPool("Web.Tuangou"), true));

		String nameNotExist = "Web.Tuangou.notExist";
		slbBaseModelTree.findPool(nameNotExist);
		Assert.assertNull(store.findPool(nameNotExist));
	}

	@Test
	public void testFindCommonAspect() throws Exception {
		Aspect expected = null;
		for (Aspect aspect : slbBaseModelTree.getAspects()) {
			if ("commonRequest".equalsIgnoreCase(aspect.getName())) {
				expected = aspect;
				break;
			}
		}

		Assert.assertTrue(EqualsBuilder.reflectionEquals(expected, store.findCommonAspect("commonRequest"), true));
	}

	@Test
	public void testFindVirtualServer() throws Exception {
		SlbModelTree slbBaseModelTree = DefaultSaxParser
				.parse(FileUtils.readFileToString(new File(baseDir, WWW_FILENAME)));
		VirtualServer expected = slbBaseModelTree.findVirtualServer(WWW_VS);
		Assert.assertTrue(EqualsBuilder.reflectionEquals(expected, store.findVirtualServer(WWW_VS), true));
	}

	protected <T> void assertEquals(List<T> expectedList, List<T> actualList) {
		Assert.assertEquals(expectedList.size(), actualList.size());
		for (T expected : expectedList) {
			Assert.assertTrue(actualList.contains(expected));
			Assert.assertTrue(EqualsBuilder
					.reflectionEquals(expected, actualList.get(actualList.indexOf(expected)), excludeFields));
		}
	}

	@Test
	public void testUpdateVirtualServer() throws Exception {

		VirtualServer newVirtualServer = DefaultSaxParser
				.parse(FileUtils.readFileToString(new File(baseDir, WWW_FILENAME))).findVirtualServer(WWW_VS);
		newVirtualServer.setState(State.DISABLED);
		newVirtualServer.setDefaultPoolName("test-pool");
		Location newLocation = new Location();
		newLocation.setCaseSensitive(false);
		newLocation.setMatchType("exact");
		newLocation.setPattern("/favicon.ico");
		Directive newDirective = new Directive();
		newDirective.setType("static-resource");
		newDirective.setDynamicAttribute("root-doc", "/var/www/virtual/big.server.com/htdocs");
		newDirective.setDynamicAttribute("expires", "30d");
		newLocation.addDirective(newDirective);
		newVirtualServer.addLocation(newLocation);

		VirtualServer originalVirtualServer = store.findVirtualServer(WWW_VS);
		int originalVersion = originalVirtualServer.getVersion();
		Date originalCreationDate = originalVirtualServer.getCreationDate();

		Date now = new Date();
		store.updateVirtualServer(WWW_VS, newVirtualServer);
		SlbModelTree slbBaseModelTree = DefaultSaxParser
				.parse(FileUtils.readFileToString(new File(baseDir, WWW_FILENAME)));
		new DefaultMerger().merge(slbBaseModelTree,
				DefaultSaxParser.parse(FileUtils.readFileToString(new File(baseDir, "slb_tuangou.xml"))));

		slbBaseModelTree.addVirtualServer(newVirtualServer);

		Assert.assertEquals(originalVersion + 1, store.findVirtualServer(WWW_VS).getVersion());
		Assert.assertEquals(originalCreationDate, store.findVirtualServer(WWW_VS).getCreationDate());
		Assert.assertTrue(now.before(store.findVirtualServer(WWW_VS).getLastModifiedDate()) || now
				.equals(store.findVirtualServer(WWW_VS).getLastModifiedDate()));
		Assert.assertTrue(EqualsBuilder
				.reflectionEquals(newVirtualServer, store.findVirtualServer(WWW_VS), "m_version", "m_creationDate",
						"m_lastModifiedDate"));
		// assert the whole model
		assertEquals(new ArrayList<VirtualServer>(slbBaseModelTree.getVirtualServers().values()),
				store.listVirtualServers());
		SlbModelTree wwwSlbModelTree = DefaultSaxParser
				.parse(FileUtils.readFileToString(new File(baseDir, WWW_FILENAME)));
		wwwSlbModelTree.addVirtualServer(newVirtualServer);
		// assert www slbBaseModelTree has updated
		assertEquals(wwwSlbModelTree, WWW_VS);
		assertRawFileNotChanged("tuangou");
		assertRawFileNotChanged("slb_base");
	}

	protected abstract void assertRawFileNotChanged(String vsName) throws IOException;

	protected abstract void assertEquals(SlbModelTree wwwSlbModelTree, String vsName) throws SAXException, IOException;

	@Test
	public void testUpdateVirtualServerConcurrentModification() throws Exception {

		VirtualServer newVirtualServer = DefaultSaxParser
				.parse(FileUtils.readFileToString(new File(baseDir, WWW_FILENAME))).findVirtualServer(WWW_VS);
		newVirtualServer.setState(State.DISABLED);
		newVirtualServer.setDefaultPoolName("test-pool");
		Location newLocation = new Location();
		newLocation.setCaseSensitive(false);
		newLocation.setMatchType("exact");
		newLocation.setPattern("/favicon.ico");
		Directive newDirective = new Directive();
		newDirective.setType("static-resource");
		newDirective.setDynamicAttribute("root-doc", "/var/www/virtual/big.server.com/htdocs");
		newDirective.setDynamicAttribute("expires", "30d");
		newLocation.addDirective(newDirective);
		newVirtualServer.addLocation(newLocation);

		VirtualServer originalVirtualServer = store.findVirtualServer(WWW_VS);
		int originalVersion = originalVirtualServer.getVersion();
		Date originalCreationDate = originalVirtualServer.getCreationDate();

		Date now = new Date();
		store.updateVirtualServer(WWW_VS, newVirtualServer);

		// modify concurrent
		VirtualServer newVirtualServer1 = DefaultSaxParser
				.parse(FileUtils.readFileToString(new File(baseDir, WWW_FILENAME))).findVirtualServer(WWW_VS);
		newVirtualServer1.setState(State.DISABLED);
		newVirtualServer1.setDefaultPoolName("test-pool1");
		Location newLocation1 = new Location();
		newLocation1.setCaseSensitive(true);
		newLocation1.setMatchType("prefix");
		newLocation1.setPattern("/");
		Directive newDirective1 = new Directive();
		newDirective1.setType("static-resource1");
		newDirective1.setDynamicAttribute("root-doc", "/var/www/virtual/big.server.com/htdocs1");
		newDirective1.setDynamicAttribute("expires", "300d");
		newLocation1.addDirective(newDirective1);
		newVirtualServer1.addLocation(newLocation1);
		try {
			store.updateVirtualServer(WWW_VS, newVirtualServer1);
			Assert.fail();
		} catch (BizException e) {
			Assert.assertEquals(MessageID.VIRTUALSERVER_CONCURRENT_MOD, e.getMessageId());
		} catch (ConcurrentModificationException e) {
		} catch (Exception e1) {
			Assert.fail();
		}
		// modify concurrent end

		SlbModelTree slbBaseModelTree = DefaultSaxParser
				.parse(FileUtils.readFileToString(new File(baseDir, WWW_FILENAME)));
		new DefaultMerger().merge(slbBaseModelTree,
				DefaultSaxParser.parse(FileUtils.readFileToString(new File(baseDir, "slb_tuangou.xml"))));

		slbBaseModelTree.addVirtualServer(newVirtualServer);

		Assert.assertEquals(originalVersion + 1, store.findVirtualServer(WWW_VS).getVersion());
		Assert.assertEquals(originalCreationDate, store.findVirtualServer(WWW_VS).getCreationDate());
		assertTimeEqualsUsingSecond(now, store.findVirtualServer(WWW_VS).getLastModifiedDate());
		Assert.assertTrue(EqualsBuilder
				.reflectionEquals(newVirtualServer, store.findVirtualServer(WWW_VS), "m_version", "m_creationDate",
						"m_lastModifiedDate"));
		// assert the whole model
		assertEquals(new ArrayList<VirtualServer>(slbBaseModelTree.getVirtualServers().values()),
				store.listVirtualServers());
		SlbModelTree wwwSlbModelTree = DefaultSaxParser
				.parse(FileUtils.readFileToString(new File(baseDir, WWW_FILENAME)));
		wwwSlbModelTree.addVirtualServer(newVirtualServer);
		// assert www slbBaseModelTree has updated
		assertEquals(wwwSlbModelTree, WWW_FILENAME);
		assertRawFileNotChanged("tuangou");
		assertRawFileNotChanged("slb_base");

	}

	@Test
	public void testUpdateVirtualServerNotExists() throws Exception {

		VirtualServer newVirtualServer = DefaultSaxParser
				.parse(FileUtils.readFileToString(new File(baseDir, WWW_FILENAME))).findVirtualServer(WWW_VS);
		newVirtualServer.setName("test");
		newVirtualServer.setState(State.DISABLED);
		newVirtualServer.setDefaultPoolName("test-pool");
		Location newLocation = new Location();
		newLocation.setCaseSensitive(false);
		newLocation.setMatchType("exact");
		newLocation.setPattern("/favicon.ico");
		Directive newDirective = new Directive();
		newDirective.setType("static-resource");
		newDirective.setDynamicAttribute("root-doc", "/var/www/virtual/big.server.com/htdocs");
		newDirective.setDynamicAttribute("expires", "30d");
		newLocation.addDirective(newDirective);
		newVirtualServer.addLocation(newLocation);

		try {
			store.updateVirtualServer("test", newVirtualServer);
			Assert.fail();
		} catch (BizException e) {

		} catch (Exception e1) {
			Assert.fail();
		}

		SlbModelTree slbBaseModelTree = DefaultSaxParser
				.parse(FileUtils.readFileToString(new File(baseDir, WWW_FILENAME)));
		new DefaultMerger().merge(slbBaseModelTree,
				DefaultSaxParser.parse(FileUtils.readFileToString(new File(baseDir, "slb_tuangou.xml"))));

		// assert the whole model
		assertEquals(new ArrayList<VirtualServer>(slbBaseModelTree.getVirtualServers().values()),
				store.listVirtualServers());
		assertRawFileNotChanged(WWW_VS);
		assertRawFileNotChanged("tuangou");
		assertRawFileNotChanged("slb_base");

	}

	@Test
	public void testAddVirtualServer() throws Exception {
		VirtualServer newVirtualServer = new VirtualServer("testVs");
		newVirtualServer.setState(State.DISABLED);
		newVirtualServer.setDefaultPoolName("test-pool");
		Location newLocation = new Location();
		newLocation.setCaseSensitive(false);
		newLocation.setMatchType("exact");
		newLocation.setPattern("/favicon.ico");
		Directive newDirective = new Directive();
		newDirective.setType("static-resource");
		newDirective.setDynamicAttribute("root-doc", "/var/www/virtual/big.server.com/htdocs");
		newDirective.setDynamicAttribute("expires", "30d");
		newLocation.addDirective(newDirective);
		newVirtualServer.addLocation(newLocation);

		Date now = new Date();
		store.addVirtualServer("testVs", newVirtualServer);
		Assert.assertTrue(EqualsBuilder.reflectionEquals(newVirtualServer, store.findVirtualServer("testVs"), true));
		Assert.assertEquals(1, newVirtualServer.getVersion());
		assertTimeEqualsUsingSecond(now, newVirtualServer.getCreationDate());
		assertTimeEqualsUsingSecond(now, newVirtualServer.getLastModifiedDate());

		new DefaultMerger().merge(slbBaseModelTree,
				DefaultSaxParser.parse(FileUtils.readFileToString(new File(baseDir, WWW_FILENAME))));
		new DefaultMerger().merge(slbBaseModelTree,
				DefaultSaxParser.parse(FileUtils.readFileToString(new File(baseDir, "slb_tuangou.xml"))));
		slbBaseModelTree.addVirtualServer(newVirtualServer);
		assertEquals(new ArrayList<VirtualServer>(slbBaseModelTree.getVirtualServers().values()),
				store.listVirtualServers());
		// assert new file created

		Assert.assertTrue(vsExistsInStore("testVs"));
		VirtualServer virtualServerFromFile = store.findVirtualServer("testVs");

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Assert.assertTrue(EqualsBuilder
				.reflectionEquals(newVirtualServer, virtualServerFromFile, "m_creationDate", "m_lastModifiedDate"));
		Assert.assertEquals(sdf.format(newVirtualServer.getCreationDate()),
				sdf.format(virtualServerFromFile.getCreationDate()));
		Assert.assertEquals(sdf.format(newVirtualServer.getLastModifiedDate()),
				sdf.format(virtualServerFromFile.getLastModifiedDate()));

		assertRawFileNotChanged(WWW_VS);
		assertRawFileNotChanged("tuangou");
		assertRawFileNotChanged("slb_base");

	}

	private void assertTimeEqualsUsingSecond(Date expected, Date real) {
		Assert.assertEquals(expected.getTime() / 1000, real.getTime() / 1000);
	}

	protected abstract boolean vsExistsInStore(String string);

	@Test
	public void testAddVirtualServerExists() throws Exception {
		VirtualServer newVirtualServer = new VirtualServer(WWW_VS);
		newVirtualServer.setState(State.DISABLED);
		newVirtualServer.setDefaultPoolName("test-pool");
		Location newLocation = new Location();
		newLocation.setCaseSensitive(false);
		newLocation.setMatchType("exact");
		newLocation.setPattern("/favicon.ico");
		Directive newDirective = new Directive();
		newDirective.setType("static-resource");
		newDirective.setDynamicAttribute("root-doc", "/var/www/virtual/big.server.com/htdocs");
		newDirective.setDynamicAttribute("expires", "30d");
		newLocation.addDirective(newDirective);
		newVirtualServer.addLocation(newLocation);

		try {
			store.addVirtualServer(WWW_VS, newVirtualServer);
			Assert.fail();
		} catch (BizException e) {

		} catch (Exception e) {
			Assert.fail();
		}

		new DefaultMerger().merge(slbBaseModelTree,
				DefaultSaxParser.parse(FileUtils.readFileToString(new File(baseDir, WWW_FILENAME))));
		new DefaultMerger().merge(slbBaseModelTree,
				DefaultSaxParser.parse(FileUtils.readFileToString(new File(baseDir, "slb_tuangou.xml"))));
		assertEquals(new ArrayList<VirtualServer>(slbBaseModelTree.getVirtualServers().values()),
				store.listVirtualServers());
		// assert new file not created
		Assert.assertFalse(vsExistsInStore("testVs"));
		assertRawFileNotChanged(WWW_VS);
		assertRawFileNotChanged("tuangou");
		assertRawFileNotChanged("slb_base");

	}

	@Test
	public void testRemoveVirtualServer() throws Exception {
		store.removeVirtualServer(WWW_VS);

		Assert.assertNull(store.findVirtualServer(WWW_VS));
		// assert www slbBaseModelTree deleted
		Assert.assertFalse(vsExistsInStore(WWW_VS));

		assertRawFileNotChanged("tuangou");
		assertRawFileNotChanged("slb_base");
	}

	@Test
	public void testRemoveVirtualServerNotExist() throws Exception {
		try {
			store.removeVirtualServer("sss");
			Assert.fail();
		} catch (BizException e) {
		} catch (Exception e) {
			Assert.fail();
		}

		assertRawFileNotChanged(WWW_VS);
		assertRawFileNotChanged("tuangou");
		assertRawFileNotChanged("slb_base");
	}

	@Test
	public void testAddStrategy() throws Exception {
		Strategy newStrategy = new Strategy("dper-hash");
		newStrategy.setType("hash");
		newStrategy.setDynamicAttribute("target", "dper");
		newStrategy.setDynamicAttribute("method", "crc32");
		store.updateOrCreateStrategy("dper-hash", newStrategy);

		slbBaseModelTree.addStrategy(newStrategy);

		assertEquals(new ArrayList<Strategy>(slbBaseModelTree.getStrategies().values()), store.listStrategies());
		Assert.assertNotNull(newStrategy.getCreationDate());
		Assert.assertEquals(newStrategy.getLastModifiedDate(), newStrategy.getCreationDate());

		assertEquals(slbBaseModelTree, "slb_base.xml");
		assertRawFileNotChanged(WWW_VS);
		assertRawFileNotChanged("tuangou");
	}

	@Test
	public void testAddPool() throws Exception {
		Pool pool = new Pool("TestPool");
		pool.setMinAvailableMemberPercentage(40);
		pool.setLoadbalanceStrategyName("uri-hash");
		Member member1 = new Member("test01");
		member1.setIp("10.1.1.1");
		pool.addMember(member1);
		Member member2 = new Member("test02");
		member2.setIp("10.1.1.2");
		pool.addMember(member2);
		store.updateOrCreatePool("TestPool", pool);

		slbBaseModelTree.addPool(pool);

		assertEquals(new ArrayList<Pool>(slbBaseModelTree.getPools().values()), store.listPools());
		Assert.assertNotNull(pool.getCreationDate());
		Assert.assertEquals(pool.getLastModifiedDate(), pool.getCreationDate());

		assertEquals(slbBaseModelTree, "slb_base.xml");
		assertRawFileNotChanged(WWW_VS);
		assertRawFileNotChanged("tuangou");
	}

	@Test
	public void testAddSlbPool() throws Exception {
		SlbPool slbPool = new SlbPool("ut-pool");

		Instance instance = new Instance();
		instance.setIp("1.1.1.1");
		slbPool.addInstance(instance);
		store.updateOrCreateSlbPool("ut-pool", slbPool);

		slbBaseModelTree.addSlbPool(slbPool);

		assertEquals(new ArrayList<SlbPool>(slbBaseModelTree.getSlbPools().values()), store.listSlbPools());

		assertEquals(slbBaseModelTree, "slb_base.xml");
		assertRawFileNotChanged(WWW_VS);
		assertRawFileNotChanged("tuangou");
	}

	@Test
	public void testSaveAspects() throws Exception {
		List<Aspect> aspects = new ArrayList<Aspect>();
		Aspect aspect1 = new Aspect();
		aspect1.setName("t1");
		aspect1.setPointCut(PointCut.BEFORE);
		Directive d1 = new Directive();
		d1.setType("t1");
		aspect1.addDirective(d1);
		aspects.add(aspect1);
		Aspect aspect2 = new Aspect();
		aspect2.setName("t2");
		aspect2.setPointCut(PointCut.AFTER);
		Directive d2 = new Directive();
		d2.setType("t2");
		aspect2.addDirective(d2);
		aspects.add(aspect2);

		store.saveCommonAspects(aspects);

		slbBaseModelTree.getAspects().clear();
		slbBaseModelTree.getAspects().addAll(aspects);
		assertEquals(slbBaseModelTree.getAspects(), store.listCommonAspects());

		assertEquals(slbBaseModelTree, "slb_base.xml");
		assertRawFileNotChanged(WWW_VS);
		assertRawFileNotChanged("tuangou");
	}

	@Test
	public void testUpdateStrategy() throws Exception {
		Strategy modifiedStrategy = new Strategy("uri-hash");
		modifiedStrategy.setType("hash");
		modifiedStrategy.setDynamicAttribute("target", "$request_uri");
		modifiedStrategy.setDynamicAttribute("method", "md5");
		Date now = new Date();
		store.updateOrCreateStrategy("uri-hash", modifiedStrategy);

		Strategy expectedStrategy = slbBaseModelTree.findStrategy("uri-hash");
		expectedStrategy.setDynamicAttribute("method", "md5");
		expectedStrategy.setLastModifiedDate(now);
		expectedStrategy.setVersion(expectedStrategy.getVersion() + 1);

		assertEquals(new ArrayList<Strategy>(slbBaseModelTree.getStrategies().values()), store.listStrategies());
		assertTimeEqualsUsingSecond(now, modifiedStrategy.getLastModifiedDate());
		Assert.assertEquals(expectedStrategy.getCreationDate(), modifiedStrategy.getCreationDate());

		assertEquals(slbBaseModelTree, "slb_base.xml");
		assertRawFileNotChanged(WWW_VS);
		assertRawFileNotChanged("tuangou");
	}

	@Test
	public void testUpdatePool() throws Exception {
		Pool modifiedPool = new Pool("Web.Tuangou");
		modifiedPool.setMinAvailableMemberPercentage(10);
		modifiedPool.setLoadbalanceStrategyName("roundrobin");
		Member member = new Member("t1");
		member.setIp("12.12.12.12");
		modifiedPool.addMember(member);

		Date now = new Date();
		store.updateOrCreatePool("Web.Tuangou", modifiedPool);

		Pool expectedPool = slbBaseModelTree.findPool("Web.Tuangou");
		expectedPool.setMinAvailableMemberPercentage(10);
		expectedPool.setLoadbalanceStrategyName("roundrobin");
		Member member2 = new Member("t1");
		member2.setIp("12.12.12.12");
		expectedPool.removeMember("tuangou-web01");
		expectedPool.removeMember("tuangou-web02");
		expectedPool.addMember(member2);
		expectedPool.setVersion(expectedPool.getVersion() + 1);
		expectedPool.setLastModifiedDate(now);

		assertEquals(new ArrayList<Pool>(slbBaseModelTree.getPools().values()), store.listPools());
		assertTimeEqualsUsingSecond(now, modifiedPool.getLastModifiedDate());
		Assert.assertEquals(expectedPool.getCreationDate(), modifiedPool.getCreationDate());

		assertEquals(slbBaseModelTree, "slb_base.xml");
		assertRawFileNotChanged(WWW_VS);
		assertRawFileNotChanged("tuangou");
	}

	@Test
	public void testUpdateSlbPool() throws Exception {
		SlbPool modifiedPool = new SlbPool("test-pool");
		Instance instance = new Instance();
		instance.setIp("2.2.2.2");
		modifiedPool.addInstance(instance);

		store.updateOrCreateSlbPool("test-pool", modifiedPool);

		SlbPool expectedPool = slbBaseModelTree.findSlbPool("test-pool");
		expectedPool.setVersion(expectedPool.getVersion() + 1);
		expectedPool.getInstances().clear();
		expectedPool.addInstance(instance);

		assertEquals(new ArrayList<SlbPool>(slbBaseModelTree.getSlbPools().values()), store.listSlbPools());

		assertEquals(slbBaseModelTree, "slb_base.xml");
		assertRawFileNotChanged(WWW_VS);
		assertRawFileNotChanged("tuangou");
	}

	@Test
	public void testRemoveStrategy() throws Exception {
		store.removeStrategy("uri-hash");

		slbBaseModelTree.removeStrategy("uri-hash");

		assertEquals(new ArrayList<Strategy>(slbBaseModelTree.getStrategies().values()), store.listStrategies());

		assertEquals(slbBaseModelTree, "slb_base.xml");
		assertRawFileNotChanged(WWW_VS);
		assertRawFileNotChanged("tuangou");
	}

	@Test
	public void testRemoveSlbPool() throws Exception {
		store.removeSlbPool("test-pool");

		slbBaseModelTree.removeSlbPool("test-pool");

		assertEquals(new ArrayList<SlbPool>(slbBaseModelTree.getSlbPools().values()), store.listSlbPools());

		assertEquals(slbBaseModelTree, "slb_base.xml");
		assertRawFileNotChanged(WWW_VS);
		assertRawFileNotChanged("tuangou");
	}

	@Test
	public void testRemovePool() throws Exception {
		store.removePool("Web.Tuangou");

		slbBaseModelTree.removePool("Web.Tuangou");

		assertEquals(new ArrayList<Pool>(slbBaseModelTree.getPools().values()), store.listPools());

		assertEquals(slbBaseModelTree, "slb_base.xml");
		assertRawFileNotChanged(WWW_VS);
		assertRawFileNotChanged("tuangou");
	}

	@Test
	public void testTag() throws Exception {
		store.tag(WWW_VS, 1, store.listPools(), store.listCommonAspects(), store.listVariables(),
				store.listStrategies());
		store.tag(WWW_VS, 1, store.listPools(), store.listCommonAspects(), store.listVariables(),
				store.listStrategies());

		Assert.assertTrue(tagExist(WWW_VS, 1));
		Assert.assertTrue(tagExist(WWW_VS, 2));
		SlbModelTree expected = DefaultSaxParser.parse(FileUtils.readFileToString(new File(baseDir, WWW_FILENAME)));
		for (Pool pool : store.listPools()) {
			expected.addPool(pool);
		}
		for (Aspect aspect : store.listCommonAspects()) {
			expected.addAspect(aspect);
		}
		for (Variable variable : store.listVariables()) {
			expected.addVariable(variable);
		}
		long initTag = expected.getTag();
		expected.setTag(initTag + 1);

		Assert.assertTrue(AdvEqualsBuilder
				.reflectionEquals(expected, getTagSlbModelTree(WWW_VS, 1), new String[] { "m_tagDate" }));
		;

		expected.setTag(initTag + 2);
		Assert.assertTrue(AdvEqualsBuilder
				.reflectionEquals(expected, getTagSlbModelTree(WWW_VS, 2), new String[] { "m_tagDate" }));
		;

		assertRawFileNotChanged(WWW_VS);
		assertRawFileNotChanged("tuangou");
		assertRawFileNotChanged("slb_base");
	}

	protected abstract SlbModelTree getTagSlbModelTree(String vs, int tag) throws SAXException, IOException;

	protected abstract boolean tagExist(String vs, int tag);

	@Test
	public void testTagVSNotExists() throws Exception {
		try {
			store.tag("www2", 1, store.listPools(), store.listCommonAspects(), store.listVariables(),
					store.listStrategies());
			Assert.fail();
		} catch (BizException e) {
			Assert.assertEquals(e.getMessageId(), MessageID.VIRTUALSERVER_NOT_EXISTS);
		} catch (Exception e) {
			Assert.fail();
		}

		assertRawFileNotChanged(WWW_VS);
		assertRawFileNotChanged("tuangou");
		assertRawFileNotChanged("slb_base");
	}

	@Test
	public void testTagConcurrentMod() throws Exception {
		try {
			store.tag(WWW_VS, 2, store.listPools(), store.listCommonAspects(), store.listVariables(),
					store.listStrategies());
			Assert.fail();
		} catch (BizException e) {
			Assert.assertEquals(MessageID.VIRTUALSERVER_CONCURRENT_MOD, e.getMessageId());
			Assert.assertTrue(e.getCause() instanceof ConcurrentModificationException);
		} catch (Exception e) {
			Assert.fail();
		}

		assertRawFileNotChanged(WWW_VS);
		assertRawFileNotChanged("tuangou");
		assertRawFileNotChanged("slb_base");
	}

	@Test
	public void testListTagIds() throws Exception {

		store.tag(WWW_VS, 1, store.listPools(), store.listCommonAspects(), store.listVariables(),
				store.listStrategies());
		store.tag(WWW_VS, 1, store.listPools(), store.listCommonAspects(), store.listVariables(),
				store.listStrategies());
		store.tag("tuangou", 1, store.listPools(), store.listCommonAspects(), store.listVariables(),
				store.listStrategies());
		store.tag("tuangou", 1, store.listPools(), store.listCommonAspects(), store.listVariables(),
				store.listStrategies());
		store.tag("tuangou", 1, store.listPools(), store.listCommonAspects(), store.listVariables(),
				store.listStrategies());

		initStore();

		List<String> wwwTagIds = store.listTagIdsDesc(WWW_VS);
		List<String> tuangouTagIds = store.listTagIdsDesc("tuangou");

		Assert.assertArrayEquals(new String[] { WWW_VS + "-2", WWW_VS + "-1" }, wwwTagIds.toArray(new String[0]));
		Assert.assertArrayEquals(new String[] { "tuangou-3", "tuangou-2", "tuangou-1" },
				tuangouTagIds.toArray(new String[0]));

		store.tag(WWW_VS, 1, store.listPools(), store.listCommonAspects(), store.listVariables(),
				store.listStrategies());
		store.tag("tuangou", 1, store.listPools(), store.listCommonAspects(), store.listVariables(),
				store.listStrategies());

		wwwTagIds = store.listTagIdsDesc(WWW_VS);
		tuangouTagIds = store.listTagIdsDesc("tuangou");

		Assert.assertArrayEquals(new String[] { WWW_VS + "-3", WWW_VS + "-2", WWW_VS + "-1" },
				wwwTagIds.toArray(new String[0]));
		Assert.assertArrayEquals(new String[] { "tuangou-4", "tuangou-3", "tuangou-2", "tuangou-1" },
				tuangouTagIds.toArray(new String[0]));

		assertRawFileNotChanged(WWW_VS);
		assertRawFileNotChanged("tuangou");
		assertRawFileNotChanged("slb_base");
	}

	protected abstract void initStore();

	@Test
	public void testListTagIdsVsNotExists() throws Exception {

		try {
			store.listTagIdsDesc("test");
			Assert.fail();
		} catch (BizException e) {
			Assert.assertEquals(MessageID.VIRTUALSERVER_NOT_EXISTS, e.getMessageId());
		} catch (Exception e) {
			Assert.fail();
		}

		assertRawFileNotChanged(WWW_VS);
		assertRawFileNotChanged("tuangou");
		assertRawFileNotChanged("slb_base");
	}

	@Test
	public void testListTagIdsNoTags() throws Exception {

		Assert.assertTrue(store.listTagIdsDesc(WWW_VS).size() == 0);

		assertRawFileNotChanged(WWW_VS);
		assertRawFileNotChanged("tuangou");
		assertRawFileNotChanged("slb_base");
	}

	@Test
	public void testGetTag() throws Exception {

		String tagId = store.tag(WWW_VS, 1, store.listPools(), store.listCommonAspects(), store.listVariables(),
				store.listStrategies());

		SlbModelTree tagSlbModelTree = store.getTag(WWW_VS, tagId);

		Assert.assertEquals(store.findVirtualServer(WWW_VS).toString(),
				tagSlbModelTree.findVirtualServer(WWW_VS).toString());

		for (Pool expPool : store.listPools()) {
			Assert.assertTrue(
					EqualsBuilder.reflectionEquals(expPool, tagSlbModelTree.findPool(expPool.getName()), true));
		}

		assertEquals(store.listCommonAspects(), tagSlbModelTree.getAspects());

		assertRawFileNotChanged(WWW_VS);
		assertRawFileNotChanged("tuangou");
		assertRawFileNotChanged("slb_base");
	}

	@Test
	public void testFindPrevTagId() throws Exception {
		String tag1 = store.tag(WWW_VS, 1, store.listPools(), store.listCommonAspects(), store.listVariables(),
				store.listStrategies());
		String tag2 = store.tag(WWW_VS, 1, store.listPools(), store.listCommonAspects(), store.listVariables(),
				store.listStrategies());

		initStore();

		Assert.assertEquals(tag1, store.findPrevTagId(WWW_VS, tag2));
		Assert.assertNull(store.findPrevTagId(WWW_VS, tag1));

		String tag3 = store.tag(WWW_VS, 1, store.listPools(), store.listCommonAspects(), store.listVariables(),
				store.listStrategies());
		Assert.assertEquals(tag2, store.findPrevTagId(WWW_VS, tag3));
		Assert.assertEquals(tag1, store.findPrevTagId(WWW_VS, tag2));
		Assert.assertNull(store.findPrevTagId(WWW_VS, tag1));

		assertRawFileNotChanged(WWW_VS);
		assertRawFileNotChanged("tuangou");
		assertRawFileNotChanged("slb_base");
	}

	@Test
	public void testListMultiFolderTags() throws Exception {

		prepareVirtualServerAtTag(WWW_VS, 1, "20120101", 1);
		prepareVirtualServerAtTag(WWW_VS, 1, "20120102", 2);

		initStore();

		store.tag(WWW_VS, 1, store.listPools(), store.listCommonAspects(), store.listVariables(),
				store.listStrategies());
		store.tag(WWW_VS, 1, store.listPools(), store.listCommonAspects(), store.listVariables(),
				store.listStrategies());
		store.tag("tuangou", 1, store.listPools(), store.listCommonAspects(), store.listVariables(),
				store.listStrategies());
		store.tag("tuangou", 1, store.listPools(), store.listCommonAspects(), store.listVariables(),
				store.listStrategies());
		store.tag("tuangou", 1, store.listPools(), store.listCommonAspects(), store.listVariables(),
				store.listStrategies());

		List<String> wwwTagIds = store.listTagIdsDesc(WWW_VS);
		List<String> tuangouTagIds = store.listTagIdsDesc("tuangou");

		Assert.assertArrayEquals(new String[] { WWW_VS + "-4", WWW_VS + "-3", WWW_VS + "-2", WWW_VS + "-1" },
				wwwTagIds.toArray(new String[0]));
		Assert.assertArrayEquals(new String[] { "tuangou-3", "tuangou-2", "tuangou-1" },
				tuangouTagIds.toArray(new String[0]));

		assertRawFileNotChanged(WWW_VS);
		assertRawFileNotChanged("tuangou");
		assertRawFileNotChanged("slb_base");
	}

	protected abstract void prepareVirtualServerAtTag(String vs, int initTag, String date, int newTag)
			throws IOException, BizException;

	@Test
	public void testRemoveTagAndFindLatestTag() throws Exception {
		prepareVirtualServerAtTag(WWW_VS, 1, "20120101", 1);
		prepareVirtualServerAtTag(WWW_VS, 1, "20120101", 2);
		prepareVirtualServerAtTag(WWW_VS, 1, "20120102", 3);

		initStore();

		store.tag(WWW_VS, 1, store.listPools(), store.listCommonAspects(), store.listVariables(),
				store.listStrategies());

		store.removeTag(WWW_VS, WWW_VS + "-2");
		store.removeTag(WWW_VS, WWW_VS + "-4");

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

		Assert.assertFalse(tagExists(WWW_VS, "20120101", 2));
		Assert.assertFalse(tagExists(WWW_VS, sdf.format(new Date()), 4));

		List<String> tagIds = store.listTagIdsDesc(WWW_VS);
		Assert.assertArrayEquals(new String[] { WWW_VS + "-3", WWW_VS + "-1" }, tagIds.toArray());
		Assert.assertEquals(WWW_VS + "-3", store.findLatestTagId(WWW_VS));

		store.removeTag(WWW_VS, WWW_VS + "-1");
		store.removeTag(WWW_VS, WWW_VS + "-3");

		Assert.assertFalse(tagExists(WWW_VS, "20120101", 1));
		Assert.assertFalse(tagExists(WWW_VS, "20120102", 3));

		Assert.assertEquals(0, store.listTagIdsDesc(WWW_VS).size());
		Assert.assertNull(store.findLatestTagId(WWW_VS));

		assertRawFileNotChanged(WWW_FILENAME);
		assertRawFileNotChanged("slb_tuangou.xml");
		assertRawFileNotChanged("slb_base.xml");
	}

	protected abstract boolean tagExists(String vsName, String date, int tagId);

}
