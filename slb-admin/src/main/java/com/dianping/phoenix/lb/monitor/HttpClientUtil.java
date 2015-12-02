package com.dianping.phoenix.lb.monitor;

import com.dianping.phoenix.lb.utils.IOUtilsWrapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@SuppressWarnings("deprecation")
public class HttpClientUtil {
	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);

	private static final DefaultHttpClient httpclient;

	static {
		HttpParams httpParams = new BasicHttpParams();
		// 设置最大连接数
		ConnManagerParams.setMaxTotalConnections(httpParams, 500);
		// 设置获取连接的最大等待时间
		ConnManagerParams.setTimeout(httpParams, 5000);
		// 设置每个路由最大连接数
		ConnPerRouteBean connPerRoute = new ConnPerRouteBean(100);
		ConnManagerParams.setMaxConnectionsPerRoute(httpParams, connPerRoute);
		// 设置连接超时时间
		HttpConnectionParams.setConnectionTimeout(httpParams, 1000);
		// 设置读取超时时间
		HttpConnectionParams.setSoTimeout(httpParams, 5000);
		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
		ClientConnectionManager cm = new ThreadSafeClientConnManager(httpParams, registry);
		httpclient = new DefaultHttpClient(cm, httpParams);
	}

	public static String getAsString(String url, List<? extends NameValuePair> nvps, String encoding)
			throws IOException {
		//		long start = System.currentTimeMillis();

		// 构造nvps为queryString
		if (nvps != null && nvps.size() > 0) {
			String query = URLEncodedUtils.format(nvps, encoding);
			if (!url.contains("?")) {
				url += "?" + query;
			} else {
				url += "&" + query;
			}
		}
		HttpGet httpGet = new HttpGet(url);

		HttpEntity entity = null;
		try {
			HttpResponse response = httpclient.execute(httpGet);
			entity = response.getEntity();
			InputStream ins = entity.getContent();
			String result = IOUtilsWrapper.convetStringFromRequest(ins, encoding);

			// logger.info("****** http client invoke (Get method), url: " + url + ", nameValuePair: " + nvps
			// + ", result: " + result);

			int statusCode = response.getStatusLine().getStatusCode();
			if (!is2xx(statusCode)) {
				throw new IOException("Status code is not 2xx, it is " + statusCode + ", body is: " + result);
			}

			return result;
		} finally {
			EntityUtils.consume(entity);
			httpGet.releaseConnection();
			//			logger.info("****** http client invoke (Get method), url: " + url + ", nameValuePair: " + nvps + ", time: "
			//			      + String.valueOf(System.currentTimeMillis() - start) + "ms.");
		}
	}

	private static boolean is2xx(int statusCode) {
		return statusCode >= 200 && statusCode < 300;
	}

}
