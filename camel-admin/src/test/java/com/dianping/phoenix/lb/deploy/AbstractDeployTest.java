package com.dianping.phoenix.lb.deploy;

import com.dianping.phoenix.AbstractSkipTest;
import com.dianping.phoenix.TestConfig;
import com.dianping.phoenix.lb.model.entity.*;
import com.dianping.phoenix.lb.utils.GsonUtils;
import com.dianping.phoenix.lb.utils.IOUtilsWrapper;
import com.google.gson.reflect.TypeToken;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.BasicManagedEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Before;

import java.io.*;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <ol>想要运行单元测试，需要
 * <li>增加名字为${slbPool}(默认为default)的nginx Pool
 * <li>运行测试用力的用户具有root权限，sudo不需要密码(修改/etc/hosts文件)
 * </ol>
 *
 * @author mengwenchao
 *         <p/>
 *         2014年8月11日 下午6:54:31
 */
public abstract class AbstractDeployTest extends AbstractSkipTest {

	protected String slbAddress = "127.0.0.1:8080";

	protected String slbPool = "default";

	protected String slbTengineAddress = "127.0.0.1";

	protected int slbTengineDyupsPort = 8866;

	@Before
	public void initPara() {

		slbAddress = TestConfig.getSlbAddress();
		slbPool = TestConfig.getSlbPool();
		slbTengineAddress = TestConfig.getSlbTengineAddress();

	}

	protected Map<String, String> postGetReturnResult(String url, String data)
			throws ClientProtocolException, IOException {
		return postGetReturnResult(url, data, false);
	}

	@SuppressWarnings("unchecked")
	protected Map<String, String> postGetReturnResult(String url, String data, boolean isDebugModelOn)
			throws ClientProtocolException, IOException {

		if (logger.isInfoEnabled()) {
			logger.info("[postGetReturnResult]" + url);
		}
		HttpPost post = new HttpPost(url);
		HttpEntity entity = new StringEntity(data);
		post.setEntity(entity);

		if (isDebugModelOn) {
			post.setHeader("DEBUG", "true");
		}

		HttpClient httpClient = new DefaultHttpClient();
		HttpResponse response = httpClient.execute(post);
		String result = resultToString(response.getEntity());

		if (response.getStatusLine().getStatusCode() != 200) {
			logger.error(result);
			throw new IllegalStateException("exception return code:" + response.getStatusLine().getStatusCode());

		}
		return (Map<String, String>) GsonUtils.fromJson(result, new TypeToken<Map<String, String>>() {
		}.getType());

	}

	/**
	 * 根据返回码判断，返回码200，成功
	 *
	 * @param url
	 * @param data
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	protected boolean post(String url, String data) throws ClientProtocolException, IOException {

		if (logger.isInfoEnabled()) {
			logger.info("[post]" + url);
		}

		HttpPost post = new HttpPost(url);
		HttpEntity entity = new StringEntity(data);
		post.setEntity(entity);

		HttpClient httpClient = new DefaultHttpClient();
		HttpResponse response = httpClient.execute(post);

		if (response.getStatusLine().getStatusCode() != 200) {

			logger.error(resultToString(response.getEntity()));
			return false;
		}
		return true;
	}

	/**
	 * 返回码200，且返回字符串成功
	 *
	 * @param url
	 * @param data
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	protected boolean postCheckResultCode(String url, String data) throws ClientProtocolException, IOException {
		return postCheckResultCode(url, data, false);
	}

	protected boolean postCheckResultCode(String url, String data, boolean isDebugModelOn)
			throws ClientProtocolException, IOException {

		if (logger.isInfoEnabled()) {
			logger.info("[postCheckResultCode]" + url);
		}

		Map<String, String> result = postGetReturnResult(url, data, isDebugModelOn);

		if (logger.isInfoEnabled()) {
			logger.info("[postCheckResultCode]" + result);
		}

		if (!result.get("errorCode").equals("0")) {
			return false;
		}

		return true;
	}

	protected String getResponseData(String url) throws ClientProtocolException, IOException {

		if (logger.isInfoEnabled()) {
			logger.info("[getResponseData]" + url);
		}
		HttpGet get = new HttpGet(url);
		HttpClient httpClient = new DefaultHttpClient();
		HttpResponse response = httpClient.execute(get);
		return resultToString(response.getEntity());

	}

	private String resultToString(HttpEntity entity) throws IOException {
		BasicManagedEntity bme = (BasicManagedEntity) entity;
		return read(bme.getContent());
	}

	private String read(InputStream content) throws IOException {

		StringBuilder sb = new StringBuilder();

		while (true) {
			int data = content.read();
			if (data == -1) {
				break;
			}
			sb.append((char) data);
		}
		return sb.toString();
	}

	protected void deployVs(String vs) throws Exception {

		Map<String, String> result = postGetReturnResult("http://" + slbAddress + "/api/vs/" + vs + "/deploy", "");
		if (logger.isInfoEnabled()) {
			logger.info("[deployVs]" + result);
		}
		if (!result.get("errorCode").equals("0")) {
			throw new Exception("deploy fail");
		}
	}

	protected void deplopyPool(String poolName) throws Exception {

		Map<String, String> result = postGetReturnResult("http://" + slbAddress + "/api/pool/" + poolName + "/deploy",
				"");
		if (logger.isInfoEnabled()) {
			logger.info("[deployPool]" + result);
		}

		if (!result.get("errorCode").equals("0")) {
			throw new Exception("deploy pool fail, " + poolName);
		}

	}

	protected void removeVs(String vs) throws IOException {

		post("http://" + slbAddress + "/api/vs/" + vs + "/del", "");
	}

	protected void removeHost(String vs) throws IOException {

		URL url = getClass().getClassLoader().getResource("deleteHosts.sh");
		Process p = Runtime.getRuntime().exec("sh " + url.getFile() + " " + vs);
		System.out.println(IOUtilsWrapper.convetStringFromRequest(p.getInputStream()));
		System.out.println(IOUtilsWrapper.convetStringFromRequest(p.getErrorStream()));

	}

	protected void addHost(String vs) throws IOException {

		OutputStream ous = null;
		try {
			ous = new FileOutputStream(new File("/etc/hosts"), true);
			ous.write(("\n" + slbTengineAddress + "\t" + vs).getBytes());
			ous.flush();
		} finally {
			if (ous != null) {
				ous.close();
			}
		}
	}

	protected void addPool(String poolName) throws ClientProtocolException, IOException {
		String url = "http://" + slbAddress + "/api/pool/add";

		Pool pool = new Pool();
		Check check = new Check();
		check.setTimeout(3000);
		check.setInterval(2000L);
		pool.setCheck(check);
		pool.setName(poolName);
		Member member = new Member();

		member.setIp("127.0.0.1");
		member.setName("unit test");
		pool.addMember(member);

		post(url, GsonUtils.toString(pool));

	}

	protected void deletePool(String poolName) throws ClientProtocolException, IOException {

		String url = "http://" + slbAddress + "/api/pool/" + poolName + "/delete";
		post(url, "");
	}

	protected void deleteVs(String vsName) throws ClientProtocolException, IOException {
		delelteVs(vsName, true);
	}

	protected void deleteVsWithoutChangeHost(String vsName) throws ClientProtocolException, IOException {
		delelteVs(vsName, false);
	}

	private void delelteVs(String vsName, boolean changeHost) throws ClientProtocolException, IOException {
		try {
			String url = "http://" + slbAddress + "/api/vs/" + vsName + "/del";
			post(url, "");
		} finally {
			if (changeHost) {
				removeHost(vsName);
			}
		}
	}

	/**
	 * addVs
	 *
	 * @param vs
	 * @throws IOException
	 */
	protected void addVs(String vsName, String echoData, String defaultPoolName) throws IOException {
		addVs(vsName, echoData, defaultPoolName, true);
	}

	protected void addVsWithoutChangeHost(String vsName, String slbPoolName, String echoData, String defaultPoolName)
			throws IOException {
		addVs(vsName, slbPoolName, echoData, defaultPoolName, false);
	}

	private void addVs(String vsName, String echoData, String defaultPoolName, boolean changeHost) throws IOException {
		addVs(vsName, null, echoData, defaultPoolName, changeHost);
	}

	private void addVs(String vsName, String slbPoolName, String echoData, String defaultPoolName, boolean changeHost)
			throws IOException {
		VirtualServer vs = new VirtualServer();

		if (slbPoolName == null) {
			vs.setSlbPool(slbPool);
		} else {
			vs.setSlbPool(slbPoolName);
		}
		vs.setDefaultPoolName(defaultPoolName);
		vs.setDomain(vsName);
		vs.setName(vsName);
		vs.setPort(80);

		Location location = new Location();
		location.setMatchType("prefix");
		location.setPattern("/");
		Directive directive = new Directive();
		directive.setType("custom");
		directive.setDynamicAttribute("value", "echo " + echoData);
		location.addDirective(directive);
		vs.addLocation(location);

		if (logger.isInfoEnabled()) {
			logger.info("[addVs]" + vs);
		}

		String addVsUrl = "http://" + slbAddress + "/api/vs/add";

		if (post(addVsUrl, GsonUtils.toJson(vs))) {
			if (changeHost) {
				addHost(vsName);
			}
			return;
		}
		throw new IllegalStateException("add vs fail");
	}

	protected boolean deployRandomVs() throws Exception {

		String vsName = UUID.randomUUID().toString();
		String echoData = UUID.randomUUID().toString();
		String poolName = UUID.randomUUID().toString();

		try {
			addPool(poolName);
			addVs(vsName, echoData, poolName);
			deployVs(vsName);
			String result = getResponseData("http://" + vsName).trim();
			boolean ret = echoData.equals(result);
			if (!ret) {
				logger.error("expected:" + echoData + ", but:" + result);
				;
			}
			return ret;
		} finally {
			deleteVs(vsName);
			deletePool(poolName);
		}
	}

	protected boolean addMember(String poolName, String hostip) throws ClientProtocolException, IOException {

		String url = "http://" + slbAddress + "/api/pool/" + poolName + "/addMember";
		String data = "[{\"ip\":\"" + hostip + "\"}]";

		return postCheckResultCode(url, data);
	}

	protected boolean addMember(String poolName, String memberName, String hostip)
			throws ClientProtocolException, IOException {
		String url = "http://" + slbAddress + "/api/pool/" + poolName + "/addMember";
		String data = "[{\"name\":\"" + memberName + "\",\"ip\":\"" + hostip + "\"}]";

		return postCheckResultCode(url, data);
	}

	protected boolean addAndDeployMember(String poolName, String memberName, String hostip, boolean isDebugModel)
			throws ClientProtocolException, IOException {
		String url = "http://" + slbAddress + "/api/v2/pool/" + poolName + "/addMemberAndDeploy";
		String data = "[{\"name\":\"" + memberName + "\",\"ip\":\"" + hostip + "\"}]";

		return postCheckResultCode(url, data, isDebugModel);
	}

	protected boolean updateAndDeployMember(String poolName, String memberName, String hostip, boolean isDebugModel)
			throws ClientProtocolException, IOException {
		String url = "http://" + slbAddress + "/api/v2/pool/" + poolName + "/updateMemberAndDeploy";
		String data = "[{\"name\":\"" + memberName + "\",\"ip\":\"" + hostip + "\"}]";

		return postCheckResultCode(url, data, isDebugModel);
	}

	protected boolean deleteAndDeployMember(String poolName, String memberName, boolean isDebugModel)
			throws ClientProtocolException, IOException {
		String url = "http://" + slbAddress + "/api/v2/pool/" + poolName + "/delMemberAndDeploy";
		String data = "[\"" + memberName + "\"]";

		return postCheckResultCode(url, data, isDebugModel);
	}

	protected int getDyupsCallCount() throws ClientProtocolException, IOException {

		String url = "http://" + slbTengineAddress + "/reqstatus";
		String response = getResponseData(url);
		String matcher = slbTengineAddress + ":" + slbTengineAddress + ":" + slbTengineDyupsPort + ".*\n";
		//		String matcher = "127.0.0.1:127.0.0.1:8866";

		System.out.println("Matcher: " + matcher);
		Pattern p = Pattern.compile(matcher, Pattern.MULTILINE);
		Matcher m = p.matcher(response);

		int result = -1;
		while (m.find()) {

			String line = m.group();
			result = Integer.parseInt(line.split(",")[4]);
		}

		return result;

	}

	protected String getDengineDyupsUpstream(String poolName) throws ClientProtocolException, IOException {

		String url = "http://" + slbTengineAddress + ":" + slbTengineDyupsPort + "/upstream/" + poolName;

		return getResponseData(url);
	}

	protected boolean checkInPool(String poolName, String ip) throws ClientProtocolException, IOException {

		String result = getDengineDyupsUpstream(poolName);
		if (logger.isInfoEnabled()) {
			logger.info("[checkInPool]" + result);
		}
		return result.indexOf(ip) >= 0;
	}
}
