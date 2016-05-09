package com.dianping.phoenix.lb.service;

import com.dianping.phoenix.agent.response.entity.Response;
import com.dianping.phoenix.agent.response.transform.DefaultJsonParser;
import com.dianping.phoenix.lb.PlexusComponentContainer;
import com.dianping.phoenix.lb.configure.ConfigManager;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.shell.ScriptExecutor;
import com.dianping.phoenix.lb.utils.ExceptionUtils;
import com.dianping.phoenix.lb.utils.IOUtilsWrapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.unidal.tuple.Pair;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Service
public class DefaultNginxServiceImpl implements NginxService {

	private static final String TEST_CONF = "test.conf";

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private ConfigManager configManager;

	public static void main(String[] args) throws Exception {
		DefaultNginxServiceImpl service = new DefaultNginxServiceImpl();
		service.init();
		NginxCheckResult res = service.checkConfig(
				"server { listen 443 ssl; server_name example.com; ssl on; ssl_certificate /usr/local/nginx/conf/phoenix-slb/example.com/ssl.crt; ssl_certificate_key /usr/local/nginx/conf/phoenix-slb/example.com/ssl.key; location / { echo \"8777 ssl\"; } }",
				"example.com",
				"-----BEGIN CERTIFICATE-----\nMIIDIjCCAougAwIBAgIBCjANBgkqhkiG9w0BAQUFADCBnDELMAkGA1UEBhMCQ04x\nETAPBgNVBAgMCFNoYW5naGFpMRIwEAYDVQQHDAlDaGFuZ25pbmcxGDAWBgNVBAoM\nD0RhWmhvbmdEaWFuUGluZzEPMA0GA1UECwwGWXVud2VpMRkwFwYDVQQDDBBEaWFu\nUGluZy1Sb290LUNBMSAwHgYJKoZIhvcNAQkBFhFkcG9wQGRpYW5waW5nLmNvbTAe\nFw0xNTExMTYwNzQ3NDlaFw0xNjExMTUwNzQ3NDlaMIGTMQswCQYDVQQGEwJDTjER\nMA8GA1UECAwIU2hhbmdoYWkxGDAWBgNVBAoMD0RhWmhvbmdEaWFuUGluZzEYMBYG\nA1UECwwPRGFaaG9uZ0RpYW5QaW5nMRswGQYDVQQDDBJwcGUuYi5kaWFucGluZy5j\nb20xIDAeBgkqhkiG9w0BCQEWEWRwb3BAZGlhbnBpbmcuY29tMIGfMA0GCSqGSIb3\nDQEBAQUAA4GNADCBiQKBgQDAQXj2te20q6YAKiN0sDUeEL4dEGV5bNbmusdnS8CT\nZRJFgiMTYpZFkVc6dY/L7V6y9W66KL9rN4SkRvd9/92vP6C0SZpJMNwBKGcQmWh6\n1B8vs++OmL1UbEjV5IjzgtDWqlMPsCY2caleBXwwZw3/5Jdu0eLXZOh9bAQWl9vm\nwQIDAQABo3sweTAJBgNVHRMEAjAAMCwGCWCGSAGG+EIBDQQfFh1PcGVuU1NMIEdl\nbmVyYXRlZCBDZXJ0aWZpY2F0ZTAdBgNVHQ4EFgQUHHqrfKPop3/ZQvJyNH/HWBDp\n/bcwHwYDVR0jBBgwFoAUvu/ksFbejx/hlDKoiF6Ziv9Ph9YwDQYJKoZIhvcNAQEF\nBQADgYEAAzDWwgsI1Xezt9aALNJXa+q1NDsMk/Sn9xBedZunhYLx531IW34yvT0f\nH7lywlRQnsHHUv81j5ceOb2UKA6LUYLQB3WtGQCgueBbM/E8KApsSoII9Bo5LyrI\niciuq6yYtG3AhcX7drvwiNFLFdntEIdzis8/0bn4kNcj4xoj5o4=\n-----END CERTIFICATE-----",
				"-----BEGIN RSA PRIVATE KEY-----\nMIICWwIBAAKBgQDAQXj2te20q6YAKiN0sDUeEL4dEGV5bNbmusdnS8CTZRJFgiMT\nYpZFkVc6dY/L7V6y9W66KL9rN4SkRvd9/92vP6C0SZpJMNwBKGcQmWh61B8vs++O\nmL1UbEjV5IjzgtDWqlMPsCY2caleBXwwZw3/5Jdu0eLXZOh9bAQWl9vmwQIDAQAB\nAoGANcxccBUarlr1+cfQ4h9Izd/7gyCKdL8LJ8eOcw5UlipLQZ4X+J221ULFePta\nwMLspAFf+cHbRsJjYKDnMp/9xUdK8TlLFuETUdEuVzJ1c2IWZ0Yp7d4jX51x9pZ3\nOILiZrt2mH+gMudXWU2IJR1/KKVYzJbkBqoPlPJvAXIic30CQQDgHHqxy/VvhCar\n/XLnnEYgw8yhlR4lGsaScjAmTSpsUlJLnOVMsk0v3HGVWveegnw5RZ8bL4hp+g8m\n5fW7gmwTAkEA25yeYEHobwW0UHBBXtB+Wz+sNes2AMnMFYjPTtdAM4fjncZ05ncZ\nt1zD8ZUPqgo/wAPZdiv+OWx/TNZ9pXoUWwJAJt4JkWhUCqEaq91q3ixGJUyP4r4f\n2kOIiMFxBFOBtgOY7jApvGF37YMH1+VM6JqsvKoMbASUXfzWP+LF+V0nLQJAKL6L\nB7LSq950EMRy7GNkPgu3KJ3F/Cl0ar5iL+9xot3gVgJe5+9K3yEf3W9ZY6PZJgfQ\nzcLlvMOrbpcQ4qWepwJASkD6YBO9u7r6xPMiWFlN1JvswVoKIkuZIbGgxksHFcZ5\ng6Bvx99/ArHDm7d9ftz6GB7fnSuBdWEGov+dBVuqfw==\n-----END RSA PRIVATE KEY-----");
		if (!res.isSucess()) {
			System.out.println(res.getMsg());
		} else {
			System.out.println("ok");
		}
	}

	@PostConstruct
	public void init() throws ComponentLookupException, IOException {
		configManager = PlexusComponentContainer.INSTANCE.lookup(ConfigManager.class);
		// /usr/local/nginx/conf/phoenix-slb-test/test.conf
		File testConf = new File(configManager.getNginxCheckMainConfigFileName());

		FileUtils.forceMkdir(testConf.getParentFile());
		if (!testConf.exists()) {
			InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(TEST_CONF);
			FileUtils.copyInputStreamToFile(inputStream, testConf);
		}
	}

	@Override
	public synchronized NginxCheckResult checkConfig(String configContent) throws BizException {
		return checkConfig(configContent, null, null, null);
	}

	@Override
	public synchronized NginxCheckResult checkConfig(String configContent, String vsName, String certifacate,
			String key) throws BizException {
		File serverConf = new File(configManager.getNginxCheckConfigFileName());
		File sslParentFile = null;

		try {
			if (serverConf.exists()) {
				FileUtils.deleteQuietly(serverConf);
			}

			FileUtils.forceMkdir(serverConf.getParentFile());
			FileUtils.writeStringToFile(serverConf, configContent);

			if (StringUtils.isNotEmpty(vsName)) {
				File certifacateFile = new File(configManager.getNginxCheckCertifacateFileName(vsName));
				File keyFile = new File(configManager.getNginxCheckKeyFileName(vsName));
				sslParentFile = certifacateFile.getParentFile();

				if (sslParentFile.exists()) {
					FileUtils.deleteQuietly(sslParentFile);
				}
				FileUtils.forceMkdir(sslParentFile);
				FileUtils.writeStringToFile(certifacateFile, certifacate);
				FileUtils.writeStringToFile(keyFile, key);
			}

			ScriptExecutor scriptExecutor = PlexusComponentContainer.INSTANCE.lookup(ScriptExecutor.class);
			ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
			ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
			int exitCode = scriptExecutor.exec(getNginxCheckScriptCmd(), stdOut, stdErr);
			logger.info("Nginx-check: " + stdOut);
			if (exitCode != 0) {
				return new NginxCheckResult(false, stdErr.toString());
			} else {
				return new NginxCheckResult(true, "");
			}
		} catch (Exception e) {
			ExceptionUtils.logAndRethrowBizException(e);
		} finally {
			FileUtils.deleteQuietly(serverConf);
			if (sslParentFile != null) {
				FileUtils.deleteQuietly(sslParentFile);
			}
		}
		return null;
	}

	@Override
	public Set<String> listVSNames(String host) throws BizException {
		try {
			URL url = new URL(configManager.getAgentVSListUrl(host));
			Pair<Boolean, Response> resultPair = send(url);

			if (resultPair.getKey()) {
				String namesStr = resultPair.getValue().getMessage();
				Set<String> vsNames = new HashSet<String>(Arrays.asList(namesStr.split("\t")));

				logger.info("[success][Agent][listVS]");
				return vsNames;
			} else {
				throw new RuntimeException(host);
			}
		} catch (Exception ex) {
			logger.error("[fail][Agent][listVS]", ex);
			throw new BizException(ex);
		}
	}

	@Override
	public boolean removeVS(String host, String vsName) throws BizException {
		try {
			URL url = new URL(configManager.getAgentRemoveVSUrl(host, vsName));
			Pair<Boolean, Response> resultPair = send(url);

			logger.info("[success][Agent][removeVS]");
			return resultPair.getKey();
		} catch (Exception ex) {
			logger.error("[fail][Agent][removeVS]", ex);
			throw new BizException(ex);
		}
	}

	@Override
	public boolean reloadNginx(String host) throws BizException {
		try {
			URL url = new URL(configManager.getAgentReloadUrl(host));
			Pair<Boolean, Response> resultPair = send(url);

			logger.info("[success][Agent][reloadNginx]");
			return resultPair.getKey();
		} catch (Exception ex) {
			logger.info("[fail][Agent][reloadNginx]");
			throw new BizException(ex);
		}
	}

	private String getNginxCheckScriptCmd() {
		StringBuilder sb = new StringBuilder();

		sb.append("bash ");
		sb.append(configManager.getNginxScript().getAbsolutePath());
		sb.append(String.format(" --func \"%s\" ", "nginx_check"));
		sb.append(String.format(" --config \"%s\" ", configManager.getNginxCheckMainConfigFileName()));

		return sb.toString();
	}

	private Pair<Boolean, Response> send(URL deployUrl) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) deployUrl.openConnection();

		conn.setConnectTimeout(configManager.getDeployConnectTimeout());
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
		conn.connect();
		return checkAgentResponse(conn);
	}

	private Pair<Boolean, Response> checkAgentResponse(HttpURLConnection conn) throws IOException {
		Response response = DefaultJsonParser
				.parse(Response.class, IOUtilsWrapper.convetStringFromRequest(conn.getInputStream()));

		if (!"ok".equals(response.getStatus())) {
			return new Pair<Boolean, Response>(false, response);
		}
		return new Pair<Boolean, Response>(true, response);
	}

}
