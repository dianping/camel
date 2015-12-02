package org.springframework.beans;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class BeanUtilsHelper {

	public static void copyNotNullProperties(Object src, Object dst)
			throws IllegalAccessException, InvocationTargetException {

		PropertyDescriptor[] targetPds = getPropertyDescriptors(dst.getClass());

		for (PropertyDescriptor targetPd : targetPds) {
			if (targetPd.getWriteMethod() != null) {
				PropertyDescriptor srcPd = getPropertyDescriptor(src.getClass(), targetPd.getName());
				if (srcPd != null && srcPd.getReadMethod() != null) {
					try {
						Method readMethod = srcPd.getReadMethod();
						if (!Modifier.isPublic(readMethod.getDeclaringClass().getModifiers())) {
							readMethod.setAccessible(true);
						}
						Object value = readMethod.invoke(src);
						if (value == null) {
							continue;
						}
						Method writeMethod = targetPd.getWriteMethod();
						if (!Modifier.isPublic(writeMethod.getDeclaringClass().getModifiers())) {
							writeMethod.setAccessible(true);
						}
						writeMethod.invoke(dst, value);
					} catch (Throwable ex) {
						throw new FatalBeanException("Could not copy properties from src to target", ex);
					}
				}
			}
		}
	}

	private static PropertyDescriptor[] getPropertyDescriptors(Class<? extends Object> clazz) {

		CachedIntrospectionResults cr = CachedIntrospectionResults.forClass(clazz);
		return cr.getPropertyDescriptors();

	}

	public static PropertyDescriptor getPropertyDescriptor(Class<?> clazz, String propertyName) throws BeansException {

		CachedIntrospectionResults cr = CachedIntrospectionResults.forClass(clazz);
		return cr.getPropertyDescriptor(propertyName);
	}

}
