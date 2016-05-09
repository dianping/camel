package com.dianping.phoenix.lb.visitor;

import com.dianping.phoenix.lb.AbstractSpringTest;
import com.dianping.phoenix.lb.dao.CommonAspectDao;
import com.dianping.phoenix.lb.dao.VariableDao;
import com.dianping.phoenix.lb.dao.VirtualServerDao;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.Aspect;
import com.dianping.phoenix.lb.model.entity.SlbModelTree;
import com.dianping.phoenix.lb.model.entity.Variable;
import com.dianping.phoenix.lb.model.entity.VirtualServer;
import com.dianping.phoenix.lb.service.model.VirtualServerService;
import com.dianping.phoenix.lb.service.model.VirtualServerServiceImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年12月19日 上午9:57:33
 */
public class VirtualServerComparisionVisitorTest extends AbstractSpringTest {

	/**
	 * <aspect name="commonRequest" point-cut="BEFORE">
	 * <directive type="more_set_input_headers" value="'commonReq : 123'"/>
	 * </aspect>
	 * <aspect name="commonResponse" point-cut="AFTER">
	 * <directive type="more_set_headers" value="'server : nginx'"/>
	 * </aspect>
	 */
	private final String ASPECT_BEFORE = "commonRequest";
	private final String ASPECT_NOTRELATED = "notRelatedAspect";
	//    private final String ASPECT_AFTER = "commonResponse";
	private final String VARIABLE_NAME = "hello";
	private final String VARIABLE_NOTRELATED = "notRelatedVariable";
	private VirtualServerService virtualServerService;
	private VirtualServerDao virtualServerDao;
	private CommonAspectDao commonAspectDao;
	private VariableDao variableDao;

	@Before
	public void before() throws Exception {

		virtualServerService = getApplicationContext().getBean(VirtualServerServiceImpl.class);
		virtualServerDao = getApplicationContext().getBean(VirtualServerDao.class);
		commonAspectDao = getApplicationContext().getBean(CommonAspectDao.class);
		variableDao = getApplicationContext().getBean(VariableDao.class);

	}

	@Test
	public void testTagCompare() throws BizException {

		int count = 10;
		VirtualServer vs = virtualServerDao.find(WWW_VS);
		int i = 0;
		String preId = virtualServerService.tag(WWW_VS, vs.getVersion());
		SlbModelTree preSlbModelTree = virtualServerService.findTagById(WWW_VS, preId);

		do {

			SlbModelTree currentSlbModelTree = tagAndGet(WWW_VS);

			Assert.assertFalse(needReload(WWW_VS, preSlbModelTree, currentSlbModelTree));

			preSlbModelTree = currentSlbModelTree;
			i++;
		} while (i < count);
	}

	@Test
	public void testTagCompareNeedReloadLocation() throws BizException {

		VirtualServer vs = virtualServerDao.find(WWW_VS);
		String preId = virtualServerService.tag(WWW_VS, vs.getVersion());
		SlbModelTree preSlbModelTree = virtualServerService.findTagById(WWW_VS, preId);

		vs.getLocations().get(0).setPattern(UUID.randomUUID().toString());
		virtualServerService.modifyVirtualServer(WWW_VS, vs);

		SlbModelTree currentSlbModelTree = tagAndGet(WWW_VS);

		Assert.assertTrue(needReload(WWW_VS, preSlbModelTree, currentSlbModelTree));
	}

	@Test
	public void testTagCompareNeedReloadCommonAspect() throws BizException {

		Assert.assertTrue(changeAspectAndCompare(ASPECT_BEFORE));

		Assert.assertFalse(changeAspectAndCompare(ASPECT_NOTRELATED));
	}

	/**
	 * @param
	 * @return
	 * @throws BizException
	 */
	private boolean changeAspectAndCompare(String aspectName) throws BizException {

		SlbModelTree preSlbModelTree = tagAndGet(WWW_VS);

		changeAspect(aspectName);

		SlbModelTree currentSlbModelTree = tagAndGet(WWW_VS);

		return needReload(WWW_VS, preSlbModelTree, currentSlbModelTree);

	}

	private void changeAspect(String aspectName) throws BizException {
		List<Aspect> aspects = commonAspectDao.list();
		int count = 0;
		for (Aspect aspect : aspects) {
			if (aspect.getName().equals(aspectName)) {
				aspect.getDirectives().get(0).setDynamicAttribute("value", "unittest:" + UUID.randomUUID());
				count++;
			}
		}
		if (count != 1) {
			throw new IllegalStateException("aspect:" + aspectName + " not only 1, but " + count);
		}
		commonAspectDao.save(aspects);
	}

	@Test
	public void testTagCompareNeedReloadVariable() throws BizException {

		Assert.assertTrue(changeVariableAndTest(VARIABLE_NAME));

		Assert.assertFalse(changeVariableAndTest(VARIABLE_NOTRELATED));

	}

	private boolean changeVariableAndTest(String variableName) throws BizException {

		SlbModelTree preSlbModelTree = tagAndGet(WWW_VS);

		List<Variable> variables = variableDao.list();
		int count = 0;
		for (Variable variable : variables) {
			if (variable.getKey().equals(variableName)) {
				variable.setValue("echo " + UUID.randomUUID());
				count++;
			}
		}
		if (count != 1) {
			throw new IllegalStateException("variable :" + variableName + " not only one, but" + count);
		}
		variableDao.save(variables);
		return needReload(WWW_VS, preSlbModelTree, tagAndGet(WWW_VS));
	}

	/**
	 * @param preSlbModelTree
	 * @param currentSlbModelTree
	 * @return
	 */
	private boolean needReload(String vsName, SlbModelTree preSlbModelTree, SlbModelTree currentSlbModelTree) {

		VirtualServerComparisionVisitor visitor = new VirtualServerComparisionVisitor(vsName, preSlbModelTree);
		currentSlbModelTree.accept(visitor);
		return visitor.getVisitorResult().needReload();
	}

	/**
	 * @param wwwVs
	 * @return
	 * @throws BizException
	 */
	private SlbModelTree tagAndGet(String vsName) throws BizException {

		VirtualServer vs = virtualServerDao.find(vsName);
		String preId = virtualServerService.tag(vsName, vs.getVersion());
		return virtualServerService.findTagById(WWW_VS, preId);
	}

}
