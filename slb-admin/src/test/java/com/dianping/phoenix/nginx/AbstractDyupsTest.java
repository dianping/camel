package com.dianping.phoenix.nginx;

import com.dianping.phoenix.lb.utils.IOUtilsWrapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.junit.Before;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 提供Dyups操作接口
 *
 * @author mengwenchao
 *         <p/>
 *         2014年9月9日 下午2:46:53
 */
public abstract class AbstractDyupsTest extends AbstractNginxTest {

	public int port = 8866;

	public String dyupsAddress;

	@Before
	public void preparedyupsAddress() {

		dyupsAddress = "http://" + requestIp + ":" + port;

	}

	protected void putUpstream(String upstreamName, String detail) throws ClientProtocolException, IOException {

		String updateUri = dyupsAddress + "/upstream/" + urlencode(upstreamName);
		HttpPost post = new HttpPost(updateUri);
		try {
			HttpEntity entity = new StringEntity(detail);
			post.setEntity(entity);
			if (logger.isInfoEnabled()) {
				logger.info("[putUpstream]" + updateUri);
				logger.info("[putUpstream]" + detail);
			}
			HttpResponse response = executeRequest(post);
			String result = resultToString(response.getEntity());
			if (logger.isInfoEnabled()) {
				logger.info("[putUpstream]" + result);
			}
			int code = response.getStatusLine().getStatusCode();

			if (code != 200) {
				throw new IllegalStateException("put upstream status not 200" + upstreamName + "," + result);
			}
		} finally {
			post.releaseConnection();
		}
	}

	protected HttpResponse executeRequest(HttpRequestBase request) throws ClientProtocolException, IOException {
		try {
			return createHttpClient().execute(request);
		} finally {
		}
	}

	protected String resultToString(HttpEntity entity) throws IOException {
		if (entity == null) {
			throw new IllegalArgumentException("null entity");
		}
		return IOUtilsWrapper.convetStringFromRequest(entity.getContent());
	}

	protected String getUpstream(String upstreamName) throws ClientProtocolException, IOException {

		String uri = dyupsAddress + "/upstream/" + urlencode(upstreamName);
		HttpGet get = new HttpGet(uri);
		if (logger.isInfoEnabled()) {
			logger.info("[getUpstream]" + uri);
		}
		try {
			HttpResponse response = executeRequest(get);
			int statusCode = response.getStatusLine().getStatusCode();
			String result = resultToString(response.getEntity());
			if (logger.isInfoEnabled()) {
				logger.info("[getUpstream]" + result);
			}
			if (statusCode == 404) {
				return null;
			}
			if (statusCode != 200) {
				throw new IllegalStateException("get upstream " + upstreamName + " fails!!" + result);
			}
			return result;
		} finally {
			get.releaseConnection();
		}
	}

	protected void deleteUpstream(String upstreamName) throws ClientProtocolException, IOException {

		String uri = dyupsAddress + "/upstream/" + urlencode(upstreamName);
		if (logger.isInfoEnabled()) {
			logger.info("[deleteUpstream]" + uri);
		}
		HttpDelete delete = new HttpDelete(uri);
		try {
			HttpResponse response = executeRequest(delete);
			String result = resultToString(response.getEntity());
			if (logger.isInfoEnabled()) {
				logger.info("[deleteUpstream]" + result);
			}
			if (response.getStatusLine().getStatusCode() != 200) {
				throw new IllegalStateException("delete upstream " + upstreamName + " fails!!" + result);
			}
		} finally {
			delete.releaseConnection();
		}
	}

	private String urlencode(String upstreamName) {

		try {
			return URLEncoder.encode(upstreamName, DEFAULT_ENCODING);
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	protected List<String> addUpstreams(int count, String detail) throws ClientProtocolException, IOException {

		List<String> added = new ArrayList<String>(count);
		for (int i = 0; i < count; i++) {

			String upstreamName = UUID.randomUUID().toString();
			putUpstream(upstreamName, detail);
			added.add(upstreamName);
		}

		return added;

	}

	protected void deleteUpstreams(List<String> added) throws ClientProtocolException, IOException {

		if (added == null) {
			throw new IllegalArgumentException("added is null!!!");
		}
		for (String add : added) {
			deleteUpstream(add);
		}
	}

}
