package com.dianping.phoenix.lb;

import com.dianping.phoenix.lb.dao.mongo.MongoAutoIncrementIdGeneratorTest;
import com.dianping.phoenix.lb.dao.mongo.MongoModelStoreImplTest;
import com.dianping.phoenix.lb.deploy.DeployPoolTest;
import com.dianping.phoenix.lb.deploy.DeployVsTest;
import com.dianping.phoenix.lb.lock.DefaultKeyLockTest;
import com.dianping.phoenix.lb.monitor.ReqStatusDengineTest;
import com.dianping.phoenix.lb.monitor.ReqStatusTest;
import com.dianping.phoenix.lb.service.model.VirtualServerServiceImplTest;
import com.dianping.phoenix.lb.utils.BeanUtilHelperTest;
import com.dianping.phoenix.lb.utils.PoolNameUtilsTest;
import com.dianping.phoenix.lb.visitor.VirtualServerComparisionVisitorTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ MongoAutoIncrementIdGeneratorTest.class, MongoModelStoreImplTest.class,

		DeployPoolTest.class, DeployVsTest.class,

		DefaultKeyLockTest.class,

		ReqStatusTest.class, ReqStatusDengineTest.class,

		VirtualServerServiceImplTest.class,

		BeanUtilHelperTest.class, PoolNameUtilsTest.class,

		VirtualServerComparisionVisitorTest.class

})
public class AllTests {

}
