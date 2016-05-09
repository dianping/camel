package com.dianping.phoenix.lb.build;

import com.dianping.phoenix.lb.configure.ConfigManager;
import com.dianping.phoenix.lb.shell.DefaultScriptExecutor;
import com.dianping.phoenix.lb.shell.ScriptExecutor;
import org.unidal.lookup.configuration.AbstractResourceConfigurator;
import org.unidal.lookup.configuration.Component;

import java.util.ArrayList;
import java.util.List;

public class ComponentsConfigurator extends AbstractResourceConfigurator {
	public static void main(String[] args) {
		generatePlexusComponentsXmlFile(new ComponentsConfigurator());
	}

	@Override
	public List<Component> defineComponents() {
		List<Component> all = new ArrayList<Component>();

		all.add(C(ConfigManager.class));
		all.add(C(ScriptExecutor.class, DefaultScriptExecutor.class).is(PER_LOOKUP));

		return all;
	}
}
