package com.dianping.phoenix.lb.deploy.f5;

import com.dianping.phoenix.lb.PlexusComponentContainer;
import com.dianping.phoenix.lb.configure.ConfigManager;
import com.dianping.phoenix.lb.deploy.agent.AgentClientResult;
import com.dianping.phoenix.lb.utils.Md5sumUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//checkUser='wwwop'
//checkPasswd='dpopPOPD'
//checkKey=`echo -n "${checkUser}${checkPasswd}${localIp}" | md5sum | awk '{print $1}'`
@Service
public class F5ApiServiceImpl implements F5ApiService {

	private static final String FAILURE = "Failure";

	private static final String OK = "OK";

	private static final String DEFAULT_ENCODE = "UTF-8";

	private String f5Url;

	private String f5User;

	private String f5Password;

	@PostConstruct
	public void init() throws ComponentLookupException {
		ConfigManager configManager = PlexusComponentContainer.INSTANCE.lookup(ConfigManager.class);
		String f5Host = configManager.getF5Host();
		Validate.isTrue(StringUtils.isNotBlank(f5Host), "f5host is empty");
		this.f5Url = "http://" + f5Host + "/";
		this.f5User = configManager.getF5User();
		this.f5Password = configManager.getF5Password();
	}

	// http://10.1.2.181:8070/status?addresses=${localIp}
	// 该ip不在F5的pool里面,返回全空信息
	// 当该IP处于online状态时,返回": OK -  Enable node"; offline
	// "; Failure - Disable node"
	@Override
	public NodeStatus getNodeStatus(String nodeIp) throws IOException {
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("addresses", nodeIp));

		String result = StringUtils.trim(HttpClientUtil.getAsString(f5Url + "status", nvps, DEFAULT_ENCODE));

		if (StringUtils.isBlank(result)) {
			return NodeStatus.NotExists;
		} else if (StringUtils.contains(result, OK)) {
			return NodeStatus.ONLine;
		} else if (StringUtils.contains(result, FAILURE)) {
			return NodeStatus.Offline;
		} else {
			throw new IllegalStateException("F5 return unknown result: " + result);
		}
	}

	// http://10.1.2.181:8070/disable?user=${checkUser}&address=${localIp}&key=${checkKey}
	@Override
	public void offline(String nodeIp, AgentClientResult agentClientResult) throws IOException {
		List<NameValuePair> nvps = buildParameter(nodeIp);

		String url = f5Url + "disable";

		long start = System.currentTimeMillis();
		String result = null;
		try {
			result = StringUtils.trim(HttpClientUtil.getAsString(url, nvps, DEFAULT_ENCODE));
		} finally {
			agentClientResult.logInfo(
					"****** http client invoke (Get method), url: " + url + ", result is: " + result + ", time: "
							+ String.valueOf(System.currentTimeMillis() - start) + "ms.");
		}

		if (!StringUtils.contains(result, OK)) {
			throw new IllegalStateException("F5 offline api call failed, result is: " + result);
		}
	}

	// http://10.1.2.181:8070/enable?user=${checkUser}&address=${localIp}&key=${checkKey}
	@Override
	public void online(String nodeIp, AgentClientResult agentClientResult) throws IOException {
		List<NameValuePair> nvps = buildParameter(nodeIp);

		String url = f5Url + "enable";

		long start = System.currentTimeMillis();
		String result = null;
		try {
			result = StringUtils.trim(HttpClientUtil.getAsString(url, nvps, DEFAULT_ENCODE));
		} finally {
			agentClientResult.logInfo(
					"****** http client invoke (Get method), url: " + url + ", result is: " + result + ", time: "
							+ String.valueOf(System.currentTimeMillis() - start) + "ms.");
		}

		if (!StringUtils.contains(result, OK)) {
			throw new IllegalStateException("F5 online api call failed: " + result);
		}
	}

	private List<NameValuePair> buildParameter(String nodeIp) {
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("address", nodeIp));
		nvps.add(new BasicNameValuePair("user", this.f5User));
		nvps.add(new BasicNameValuePair("key", Md5sumUtil.md5sum(this.f5User + this.f5Password + nodeIp)));
		return nvps;
	}

}
