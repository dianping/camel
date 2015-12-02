package com.dianping.phoenix.lb.action;

import com.dianping.phoenix.lb.utils.DefinedInputUtils;
import com.opensymphony.xwork2.ActionSupport;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wukezhu
 */
@Component("definedInputAction")
public class DefinedInputAction extends ActionSupport {

	private static final long serialVersionUID = 2150069350934991522L;

	private Map<String, DefinedInput> propertiesDefinedInputs = new HashMap<String, DefinedInput>();

	private Map<String, Map<String, DefinedInput>> directiveDefinedInputs = new HashMap<String, Map<String, DefinedInput>>();

	@PostConstruct
	public void init() {
		propertiesDefinedInputs = DefinedInputUtils.INSTANCE.getPropertiesDefinedInputs();
		List<String> types = DefinedInputUtils.INSTANCE.getDirectiveTypes();
		if (types != null) {
			for (String type : types) {
				Map<String, DefinedInput> directiveDefinedInputs0 = DefinedInputUtils.INSTANCE
						.getDirectiveDefinedInputs(type);
				directiveDefinedInputs.put(type, directiveDefinedInputs0);
			}
		}
	}

	@Override
	public String execute() throws Exception {
		return SUCCESS;
	}

	@Override
	public void validate() {
		super.validate();
	}

	public Map<String, DefinedInput> getPropertiesDefinedInputs() {
		return propertiesDefinedInputs;
	}

	public Map<String, Map<String, DefinedInput>> getDirectiveDefinedInputs() {
		return directiveDefinedInputs;
	}

}
