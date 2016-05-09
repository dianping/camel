package com.dianping.phoenix.lb.monitor;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年7月7日 下午4:24:27
 */
public class ReqStatus {

	/**
	 * key
	 */
	private String kv;

	private long bytesInTotal;

	private long bytesOutTotal;

	private long connTotal;

	private long reqTotal;

	private long reqTotal2xx;

	private long reqTotal3xx;

	private long reqTotal4xx;

	private long reqTotal5xx;

	private long reqTotalOthers;

	private long rtTotal;

	private long upstreamReq;

	private long upstreamRt;

	private long upstreamTries;

	public ReqStatus(String kv) {

		this.kv = kv;
	}

	public ReqStatus(String[] rqs) {

		int i = 0;
		this.kv = rqs[i++];
		this.bytesInTotal = Long.parseLong(rqs[i++]);
		this.bytesOutTotal = Long.parseLong(rqs[i++]);
		this.connTotal = Long.parseLong(rqs[i++]);
		this.reqTotal = Long.parseLong(rqs[i++]);
		this.reqTotal2xx = Long.parseLong(rqs[i++]);
		this.reqTotal3xx = Long.parseLong(rqs[i++]);
		this.reqTotal4xx = Long.parseLong(rqs[i++]);
		this.reqTotal4xx = Long.parseLong(rqs[i++]);
		this.reqTotalOthers = Long.parseLong(rqs[i++]);
		this.rtTotal = Long.parseLong(rqs[i++]);
		this.upstreamReq = Long.parseLong(rqs[i++]);
		this.upstreamRt = Long.parseLong(rqs[i++]);
		this.upstreamTries = Long.parseLong(rqs[i++]);
	}

	public void add(ReqStatus added) {

		this.bytesInTotal += added.bytesInTotal;
		this.bytesOutTotal += added.bytesOutTotal;
		this.connTotal += added.connTotal;
		this.reqTotal += added.reqTotal;
		this.reqTotal2xx += added.reqTotal2xx;
		this.reqTotal3xx += added.reqTotal3xx;
		this.reqTotal4xx += added.reqTotal4xx;
		this.reqTotal4xx += added.reqTotal4xx;
		this.reqTotalOthers += added.reqTotalOthers;
		this.rtTotal += added.rtTotal;
		this.upstreamReq += added.upstreamReq;
		this.upstreamRt += added.upstreamRt;
		this.upstreamTries += added.upstreamTries;

	}

	public String getKv() {
		return kv;
	}

	public void setKv(String kv) {
		this.kv = kv;
	}

	public long getBytesInTotal() {
		return bytesInTotal;
	}

	public void setBytesInTotal(long bytesInTotal) {
		this.bytesInTotal = bytesInTotal;
	}

	public long getBytesOutTotal() {
		return bytesOutTotal;
	}

	public void setBytesOutTotal(long bytesOutTotal) {
		this.bytesOutTotal = bytesOutTotal;
	}

	public long getConnTotal() {
		return connTotal;
	}

	public void setConnTotal(long connTotal) {
		this.connTotal = connTotal;
	}

	public long getReqTotal() {
		return reqTotal;
	}

	public void setReqTotal(long reqTotal) {
		this.reqTotal = reqTotal;
	}

	public long getReqTotal2xx() {
		return reqTotal2xx;
	}

	public void setReqTotal2xx(long reqTotal2xx) {
		this.reqTotal2xx = reqTotal2xx;
	}

	public long getReqTotal3xx() {
		return reqTotal3xx;
	}

	public void setReqTotal3xx(long reqTotal3xx) {
		this.reqTotal3xx = reqTotal3xx;
	}

	public long getReqTotal4xx() {
		return reqTotal4xx;
	}

	public void setReqTotal4xx(long reqTotal4xx) {
		this.reqTotal4xx = reqTotal4xx;
	}

	public long getReqTotal5xx() {
		return reqTotal5xx;
	}

	public void setReqTotal5xx(long reqTotal5xx) {
		this.reqTotal5xx = reqTotal5xx;
	}

	public long getReqTotalOthers() {
		return reqTotalOthers;
	}

	public void setReqTotalOthers(long reqTotalOthers) {
		this.reqTotalOthers = reqTotalOthers;
	}

	public long getRtTotal() {
		return rtTotal;
	}

	public void setRtTotal(long rtTotal) {
		this.rtTotal = rtTotal;
	}

	public long getUpstreamReq() {
		return upstreamReq;
	}

	public void setUpstreamReq(long upstreamReq) {
		this.upstreamReq = upstreamReq;
	}

	public long getUpstreamRt() {
		return upstreamRt;
	}

	public void setUpstreamRt(long upstreamRt) {
		this.upstreamRt = upstreamRt;
	}

	public long getUpstreamTries() {
		return upstreamTries;
	}

	public void setUpstreamTries(long upstreamTries) {
		this.upstreamTries = upstreamTries;
	}

}
