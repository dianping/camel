package com.dianping.phoenix.lb.service;

import com.dianping.phoenix.lb.exception.BizException;

import java.util.Set;

/**
 * @author Leo Liang
 *
 */
public interface NginxService {

	NginxCheckResult checkConfig(String configContent) throws BizException;

	NginxCheckResult checkConfig(String configContent, String vsName, String certifacate, String key)
			throws BizException;

	Set<String> listVSNames(String host) throws BizException;

	boolean removeVS(String host, String vsName) throws BizException;

	boolean reloadNginx(String host) throws BizException;

	class NginxCheckResult {

		private boolean sucess;

		private String msg;

		public NginxCheckResult(boolean sucess, String msg) {
			this.sucess = sucess;
			this.msg = msg;
		}

		public boolean isSucess() {
			return sucess;
		}

		public String getMsg() {
			return msg;
		}

	}

}
