package com.dianping.phoenix.lb.util.repeattest;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

public class RepeatTestRule implements MethodRule {

	private int count;

	public RepeatTestRule(int count) {

		this.count = count;
	}

	@Override
	public Statement apply(final Statement base, FrameworkMethod method, Object target) {

		Repeat repeat = method.getAnnotation(Repeat.class);
		if (repeat != null) {
			count = repeat.value();
		}

		return new Statement() {

			@Override
			public void evaluate() throws Throwable {
				for (int i = 0; i < count; i++) {
					base.evaluate();
				}
			}
		};
	}

}
