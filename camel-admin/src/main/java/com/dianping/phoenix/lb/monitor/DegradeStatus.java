package com.dianping.phoenix.lb.monitor;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年9月5日 下午6:25:24
 */
public class DegradeStatus {

	private String upstreamName;

	private boolean isChecked;

	private int degradeState;

	private int serverCount;

	private int upCount;

	private int degradeRate;

	private int forceState;

	private boolean isDeleted;

	public String getUpstreamName() {
		return upstreamName;
	}

	public void setUpstreamName(String upstreamName) {
		this.upstreamName = upstreamName;
	}

	public boolean isChecked() {
		return isChecked;
	}

	public void setChecked(boolean isChecked) {
		this.isChecked = isChecked;
	}

	public int getDegradeState() {
		return degradeState;
	}

	public void setDegradeState(int degradeState) {
		this.degradeState = degradeState;
	}

	public int getServerCount() {
		return serverCount;
	}

	public void setServerCount(int serverCount) {
		this.serverCount = serverCount;
	}

	public int getUpCount() {
		return upCount;
	}

	public void setUpCount(int upCount) {
		this.upCount = upCount;
	}

	public int getDegradeRate() {
		return degradeRate;
	}

	public void setDegradeRate(int degradeRate) {
		this.degradeRate = degradeRate;
	}

	public boolean isDeleted() {
		return isDeleted;
	}

	public void setDeleted(boolean isDeleted) {
		this.isDeleted = isDeleted;
	}

	public int getForceState() {
		return forceState;
	}

	public void setForceState(int forceState) {
		this.forceState = forceState;
	}
}
