package com.dianping.phoenix.lb.dao;

import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.*;

import java.util.List;
import java.util.Set;

/**
 * @author Leo Liang
 *
 */
public interface VirtualServerDao {

	VirtualServer find(String virtualServerName);

	void add(VirtualServer virtualServer) throws BizException;

	void update(VirtualServer virtualServer) throws BizException;

	Set<String> listNames();

	List<VirtualServer> list();

	List<VirtualServer> listWithNameAndGroup();

	void delete(String virtualServerName) throws BizException;

	String tag(String virtualServerName, int virtualServerVersion, List<Pool> pools, List<Aspect> commonAspects,
			List<Variable> variables, List<Strategy> strategies) throws BizException;

	List<String> listTags(String virtualServerName) throws BizException;

	SlbModelTree findTagById(String virtualServerName, String tagId) throws BizException;

	String findPrevTagId(String virtualServerName, String tagId) throws BizException;

	void removeTag(String virtualServerName, String tagId) throws BizException;

	String findLatestTagId(String virtualServerName) throws BizException;

}
