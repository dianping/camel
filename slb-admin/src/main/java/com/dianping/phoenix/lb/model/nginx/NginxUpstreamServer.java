/**
 * Project: phoenix-load-balancer
 * <p/>
 * File Created at Oct 30, 2013
 */
package com.dianping.phoenix.lb.model.nginx;

import com.dianping.phoenix.lb.model.entity.Member;

/**
 * @author Leo Liang
 *
 */
public class NginxUpstreamServer {
	private Member member;

	/**
	 * @return the member
	 */
	public Member getMember() {
		return member;
	}

	/**
	 * @param member
	 *            the member to set
	 */
	public void setMember(Member member) {
		this.member = member;
	}

}
