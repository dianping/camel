package com.dianping.phoenix.lb.service.model;

import com.dianping.phoenix.lb.dao.VariableDao;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.InfluencingVs;
import com.dianping.phoenix.lb.model.entity.*;
import com.dianping.phoenix.lb.service.ConcurrentControlServiceTemplate;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年9月3日 下午2:01:32
 */
@Service
public class VariableServiceImpl extends ConcurrentControlServiceTemplate implements VariableService {

	@Autowired
	private VariableDao variableDao;

	@Autowired
	private CommonAspectService commonAspectService;

	@Autowired
	private VirtualServerService virtualServerService;

	@Override
	public List<Variable> listVariables() throws BizException {

		return read(new ReadOperation<List<Variable>>() {

			@Override
			public List<Variable> doRead() throws Exception {

				return variableDao.list();
			}
		});
	}

	@Override
	public void saveVariables(final List<Variable> variables) throws BizException {

		Validate.notNull(variables);

		write(new WriteOperation<Void>() {

			@Override
			public Void doWrite() throws Exception {

				variableDao.save(variables);

				return null;
			}
		});
	}

	@Override
	public Variable findVariable(final String key) throws BizException {

		return read(new ReadOperation<Variable>() {

			@Override
			public Variable doRead() throws Exception {

				return variableDao.find(key);
			}
		});
	}

	@Override
	public Set<InfluencingVs> findInfluencedVs(String key) throws BizException {

		Set<InfluencingVs> influencingVs = new HashSet<InfluencingVs>();
		Variable variable = findVariable(key);
		if (variable == null) {
			return influencingVs;
		}

		Set<Variable> influencedVariables = new HashSet<Variable>();
		findInfluencedVariables(key, influencedVariables);
		Set<String> influencedAspects = findInfluencedAspects(influencedVariables);

		for (VirtualServer vs : virtualServerService.listVirtualServers()) {

			List<String> positionDescs = new LinkedList<String>();

			for (Aspect aspect : vs.getAspects()) {

				if (!StringUtils.isEmpty(aspect.getRef())) {
					if (influencedAspects.contains(aspect.getRef())) {
						positionDescs.add("引用公共规则:" + aspect.getRef());
					}
					continue;
				}
				if (isAspectInfluenced(aspect, influencedVariables)) {
					positionDescs.add("映射规则:" + aspect.getName());
				}
			}

			for (Location location : vs.getLocations()) {
				if (isLocationInfluenced(location, influencedVariables)) {
					positionDescs.add("Location:" + location.getPattern());
				}
			}
			if (positionDescs.size() > 0) {
				InfluencingVs ivs = new InfluencingVs();
				ivs.setVsName(vs.getName());
				ivs.setPositionDescs(positionDescs);
				influencingVs.add(ivs);
			}

		}

		return influencingVs;
	}

	private Set<String> findInfluencedAspects(Set<Variable> influencedVariables) {

		Set<String> influencedAspects = new HashSet<String>();

		List<Aspect> aspects = commonAspectService.listCommonAspects();
		for (Aspect aspect : aspects) {

			if (isAspectInfluenced(aspect, influencedVariables)) {
				influencedAspects.add(aspect.getName());
			}
		}
		return influencedAspects;
	}

	private boolean isLocationInfluenced(Location location, Set<Variable> influencedVariables) {

		return isDirectivesInfluenced(location.getDirectives(), influencedVariables);

	}

	private boolean isAspectInfluenced(Aspect aspect, Set<Variable> influencedVariables) {

		return isDirectivesInfluenced(aspect.getDirectives(), influencedVariables);

	}

	private boolean isDirectivesInfluenced(List<Directive> directives, Set<Variable> influencedVariables) {

		for (Directive directive : directives) {

			for (String value : directive.getDynamicAttributes().values()) {
				if (hasVariable(influencedVariables, value)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean hasVariable(Set<Variable> influencedVariables, String value) {
		for (Variable variable : influencedVariables) {

			String reference = getVariableReference(variable.getKey());
			if (value.indexOf(reference) >= 0) {
				return true;
			}
		}
		return false;
	}

	private Set<Variable> findInfluencedVariables(String key, Set<Variable> influencedVariables) throws BizException {

		String variableReference = getVariableReference(key);

		for (Variable variable : listVariables()) {
			if (variable.getKey().equals(key)) {
				influencedVariables.add(variable);
			}
			if (variable.getValue().indexOf(variableReference) >= 0) {
				if (influencedVariables.add(variable)) {
					//新变量
					findInfluencedVariables(variable.getKey(), influencedVariables);
				}
			}
		}

		return influencedVariables;
	}

	private String getVariableReference(String key) {
		return String.format(variableFormat, key);
	}

	public VariableDao getVariableDao() {
		return variableDao;
	}

	public void setVariableDao(VariableDao variableDao) {
		this.variableDao = variableDao;
	}

	public CommonAspectService getCommonAspectService() {
		return commonAspectService;
	}

	public void setCommonAspectService(CommonAspectService commonAspectService) {
		this.commonAspectService = commonAspectService;
	}

	public VirtualServerService getVirtualServerService() {
		return virtualServerService;
	}

	public void setVirtualServerService(VirtualServerService virtualServerService) {
		this.virtualServerService = virtualServerService;
	}
}
