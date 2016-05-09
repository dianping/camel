package com.dianping.phoenix.lb.service;

import com.dianping.phoenix.lb.exception.BizException;

public interface GitService {

	void commitAllChanges(String targetDir, String comment) throws BizException;

	void tagAndPush(String gitUrl, String targetDir, String tag, String comment) throws BizException;

	void rollback(String targetDir) throws BizException;

	void commitAllChangesAndTagAndPush(String gitUrl, String targetDir, String tag, String comment) throws BizException;

}
