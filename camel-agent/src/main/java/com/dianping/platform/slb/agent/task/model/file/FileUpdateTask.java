package com.dianping.platform.slb.agent.task.model.file;

import com.dianping.platform.slb.agent.task.AbstractTask;

import java.io.OutputStream;
import java.util.Map;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public class FileUpdateTask extends AbstractTask {

	private String[] virtualServerNames;
	private String[] fileNames;
	private Map<String, String[]> fileContents;
	private OutputStream m_outputStream;

	public FileUpdateTask(String[] virtualServerNames, String[] fileNames, Map<String, String[]> fileContents) {
		super();
		this.virtualServerNames = virtualServerNames;
		this.fileNames = fileNames;
		this.fileContents = fileContents;
	}

	@Override
	public void setTaskOutputStream(OutputStream outputStream) {
		this.m_outputStream = outputStream;
	}

	@Override
	public OutputStream getTaskOutputStream() {
		return m_outputStream;
	}

	public String[] getVirtualServerNames() {
		return virtualServerNames;
	}

	public void setVirtualServerNames(String[] virtualServerNames) {
		this.virtualServerNames = virtualServerNames;
	}

	public String[] getFileNames() {
		return fileNames;
	}

	public void setFileNames(String[] fileNames) {
		this.fileNames = fileNames;
	}

	public Map<String, String[]> getFileContents() {
		return fileContents;
	}

	public void setFileContents(Map<String, String[]> fileContents) {
		this.fileContents = fileContents;
	}

	public OutputStream getOutputStream() {
		return m_outputStream;
	}

	public void setOutputStream(OutputStream outputStream) {
		m_outputStream = outputStream;
	}
}
