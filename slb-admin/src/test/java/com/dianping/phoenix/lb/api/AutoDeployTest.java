package com.dianping.phoenix.lb.api;

import com.dianping.phoenix.lb.deploy.AbstractDeployTest;
import org.apache.http.client.ClientProtocolException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

/*-
 * @author liyang
 *
 * 2015年5月15日 上午11:22:48
 */
public class AutoDeployTest extends AbstractDeployTest {

	private static final String IP_PREFIX = "255.255.255.";

	private static final String MEMBER_PREFIX = "test_member_";

	private static final String VS_DOMAIN = "AutoDeployTestDomain";

	private static final String SLB_POOL_NAME = "default-pool";

	private void executeAction(MemberAction action, boolean testNormal) throws Exception {
		String poolName = UUID.randomUUID().toString();
		int threshold = 0;
		try {
			addPool(poolName);
			addVsWithoutChangeHost(VS_DOMAIN, SLB_POOL_NAME, VS_DOMAIN, poolName);
			deplopyPool(poolName);

			if (testNormal) {
				threshold = 100;
			} else {
				threshold = 1;
			}
			action.init(poolName);
			for (int i = 0; i < threshold; i++) {
				String memberName = MEMBER_PREFIX + i;
				String memberIp = IP_PREFIX + i;

				if (testNormal) {
					Assert.assertTrue(action.execute(poolName, memberName, memberIp));
				} else {
					Assert.assertFalse(action.execute(poolName, memberName, memberIp));
				}
			}
			action.cleanUp();
		} finally {
			deleteVsWithoutChangeHost(VS_DOMAIN);
			deletePool(poolName);
		}
	}

	@Test
	public void testAddMemberNormal() throws Exception {
		executeAction(new MemberAction() {
			@Override
			public void init(String poolName) {
			}

			@Override
			public boolean execute(String poolName, String memberName, String memberIp)
					throws ClientProtocolException, IOException {
				return addAndDeployMember(poolName, memberName, memberIp, false);
			}

			@Override
			public void cleanUp() {
			}
		}, true);
	}

	@Test
	public void testDeleteMemberNormal() throws Exception {
		executeAction(new MemberAction() {
			@Override
			public void init(String poolName) throws ClientProtocolException, IOException {
				for (int i = 0; i < 100; i++) {
					String memberName = MEMBER_PREFIX + i;

					addMember(poolName, memberName, "127.0.0.1");
				}
			}

			@Override
			public boolean execute(String poolName, String memberName, String memberIp)
					throws ClientProtocolException, IOException {
				return deleteAndDeployMember(poolName, memberName, false);
			}

			@Override
			public void cleanUp() {
			}
		}, true);
	}

	@Test
	public void testUpdateMemberNormal() throws Exception {
		executeAction(new MemberAction() {
			@Override
			public void init(String poolName) throws ClientProtocolException, IOException {
				for (int i = 0; i < 100; i++) {
					String memberName = MEMBER_PREFIX + i;

					addMember(poolName, memberName, "127.0.0.1");
				}
			}

			@Override
			public boolean execute(String poolName, String memberName, String memberIp)
					throws ClientProtocolException, IOException {
				return updateAndDeployMember(poolName, memberName, memberIp, false);
			}

			@Override
			public void cleanUp() {
			}
		}, true);
	}

	@Test
	public void testAddMemberInterupted() throws Exception {
		executeAction(new MemberAction() {
			@Override
			public void init(String poolName) {
			}

			@Override
			public boolean execute(String poolName, String memberName, String memberIp)
					throws ClientProtocolException, IOException {
				return addAndDeployMember(poolName, memberName, memberIp, true);
			}

			@Override
			public void cleanUp() throws InterruptedException {
				Thread.sleep(60 * 1000);
			}
		}, false);
	}

	@Test
	public void testDeleteMemberInterupted() throws Exception {
		executeAction(new MemberAction() {
			@Override
			public void init(String poolName) throws ClientProtocolException, IOException {
				String memberName = MEMBER_PREFIX + 0;

				addMember(poolName, memberName, "127.0.0.1");
			}

			@Override
			public boolean execute(String poolName, String memberName, String memberIp)
					throws ClientProtocolException, IOException {
				return deleteAndDeployMember(poolName, memberName, true);
			}

			@Override
			public void cleanUp() throws InterruptedException {
				Thread.sleep(60 * 1000);
			}
		}, false);
	}

	@Test
	public void testUpdateMemberInterupted() throws Exception {
		executeAction(new MemberAction() {
			@Override
			public void init(String poolName) throws ClientProtocolException, IOException {
				String memberName = MEMBER_PREFIX + 0;

				addMember(poolName, memberName, "127.0.0.1");
			}

			@Override
			public boolean execute(String poolName, String memberName, String memberIp)
					throws ClientProtocolException, IOException {
				return updateAndDeployMember(poolName, memberName, memberIp, true);
			}

			@Override
			public void cleanUp() throws InterruptedException {
				Thread.sleep(60 * 1000);
			}
		}, false);
	}

	private abstract class MemberAction {

		abstract void init(String poolName) throws Exception;

		abstract boolean execute(String poolName, String memberName, String memberIp) throws Exception;

		abstract void cleanUp() throws Exception;

	}

}
