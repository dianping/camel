package com.dianping.phoenix.lb.dao.mongo.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 *
 * use it to track db status
 */
// @Aspect
// @Component
public class DbAspect {

	private final Logger m_logger = LoggerFactory.getLogger(getClass());

	// @Around("execution(* com.dianping.phoenix.lb.dao.mongo.MongoModelStoreImpl.*(..))")
	public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
		Object[] args = joinPoint.getArgs();
		Object result = null;
		String methodName = extractMethodName(joinPoint.getSignature());

		try {
			result = joinPoint.proceed(args);
			m_logger.info("[success][db][" + methodName + "]");
		} catch (Throwable e) {
			m_logger.info("[fail][db]["+methodName+"]");
			throw e;
		}
		return result;
	}

	private String extractMethodName(Signature signature) {
		try {
			MethodSignature methodSignature = (MethodSignature) signature;
			Method method = methodSignature.getMethod();

			return method.getName();
		} catch (Exception ex) {
			return "default";
		}
	}

}
