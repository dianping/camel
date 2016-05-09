/**
 * Project: phoenix-load-balancer
 * <p/>
 * File Created at Oct 30, 2013
 */
package com.dianping.phoenix.lb.model.nginx;

import com.dianping.phoenix.lb.model.entity.Directive;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Leo Liang
 *
 */
public class NginxLocation {
	private MatchType matchType;
	private String pattern;
	private List<Directive> directives = new ArrayList<Directive>();
	private int httpsType = 1;

	public int getHttpsType() {
		return httpsType;
	}

	public void setHttpsType(int httpsType) {
		this.httpsType = httpsType;
	}

	/**
	 * @return the matchType
	 */
	public MatchType getMatchType() {
		return matchType;
	}

	/**
	 * @param matchType
	 *            the matchType to set
	 */
	public void setMatchType(MatchType matchType) {
		this.matchType = matchType;
	}

	/**
	 * @return the pattern
	 */
	public String getPattern() {
		return pattern;
	}

	/**
	 * @param pattern
	 *            the pattern to set
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	/**
	 * @return the directives
	 */
	public List<Directive> getDirectives() {
		return directives;
	}

	public void addDirective(Directive directive) {
		this.directives.add(directive);
	}

	public static enum MatchType {
		EXACT, PREFIX, REGEX_CASE_INSENSITIVE, REGEX_CASE_SENSITIVE, COMMON;
	}

}
