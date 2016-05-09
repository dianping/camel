package com.dianping.phoenix.lb.service.model;

import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.InfluencingVs;
import com.dianping.phoenix.lb.model.entity.Variable;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年9月3日 下午2:00:28
 */
public interface VariableService {

	static Pattern pattern = Pattern.compile("\\$\\(([^\\$\\(\\)]*?)\\)");

	static String variableFormat = "$(%s)";

	List<Variable> listVariables() throws BizException;

	void saveVariables(List<Variable> variables) throws BizException;

	Variable findVariable(String key) throws BizException;

	Set<InfluencingVs> findInfluencedVs(String key) throws BizException;

}
