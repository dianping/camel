package com.dianping.phoenix.lb.deploy.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

public class PhoenixInputStreamReader extends Reader {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private Reader sr;

	private int offset = 0;

	private int timeout = 1000;

	private int retryCount = 10;

	private String url;

	public PhoenixInputStreamReader(String url, int timeout, int retryCount) {
		this.timeout = timeout;
		this.retryCount = retryCount;
		this.url = url;
		try {
			sr = getReader(url);
		} catch (Exception e) {
			throw new RuntimeException("Can not get inputstream from url: " + url);
		}
	}

	protected Reader getReader(String url) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setConnectTimeout(timeout);
		return new InputStreamReader(conn.getInputStream(), "utf-8");

	}

	@Override
	public int read(char[] cbuf) {
		int len = 0;
		try {
			if ((len = sr.read(cbuf)) > 0) {
				offset += len;
			}
		} catch (IOException e) {
			logger.error("[read]", e);
			refresh();
			return read(cbuf);
		}
		return len;
	}

	private void refresh() {
		if (retryCount-- > 0) {
			try {
				Thread.sleep(timeout);
			} catch (Exception e1) {
				// do nothing
			}
			try {
				sr = getReader(url);
				sr.skip(offset);
			} catch (Exception e) {
				logger.error("[refresh]", e);
				refresh();
			}
		} else {
			throw new RuntimeException("Can not get inputstream from url: " + url);
		}
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		return sr.read(cbuf, off, len);
	}

	@Override
	public void close() throws IOException {
		sr.close();
	}
}
