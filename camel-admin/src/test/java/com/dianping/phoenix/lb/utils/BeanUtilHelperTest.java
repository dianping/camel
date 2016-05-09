package com.dianping.phoenix.lb.utils;

import com.dianping.phoenix.AbstractTest;
import com.dianping.phoenix.lb.model.entity.Member;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanUtilsHelper;

import java.lang.reflect.InvocationTargetException;

public class BeanUtilHelperTest extends AbstractTest {

	@Before
	public void beforeBeanUtilHelperTest() {

	}

	@Test
	public void testCopyNotNullProperties() throws IllegalAccessException, InvocationTargetException {

		Member src = new Member();
		Member dst = new Member();

		src.setIp("127.0.0.1");
		src.setMaxFails(30);

		dst.setPort(80);

		BeanUtilsHelper.copyNotNullProperties(src, dst);

		Assert.assertEquals("127.0.0.1", dst.getIp());
		Assert.assertEquals((Integer) 80, dst.getPort());
		Assert.assertEquals((Integer) 30, dst.getMaxFails());

	}

	@Test
	public void testSpringProperties() {

		Member src = new Member();
		Member dst = new Member();

		src.setIp("127.0.0.1");
		src.setMaxFails(30);

		dst.setPort(80);
		BeanUtils.copyProperties(src, dst);

	}

}
