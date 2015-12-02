package com.dianping.phoenix.lb.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年11月20日 上午11:00:35
 */
public abstract class AbstractStore implements ModelStore {

	public static final String TAGID_SPLITTER = "-";
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	public static String convertToStrTagId(String vsName, long tag) {
		return vsName + TAGID_SPLITTER + tag;
	}

	protected static Long convertFromStrTagId(String vsName, String tagId) {
		String start = vsName + TAGID_SPLITTER;
		if (tagId.startsWith(start)) {

			String tag = tagId.substring(start.length()).trim();
			return Long.valueOf(tag);
		}
		return null;
	}

	protected String doFindPrevTagId(String virtualServerName, String currentTagId, List<String> tagIds) {

		Long currentTagIdInt = convertFromStrTagId(virtualServerName, currentTagId);

		if (currentTagIdInt != null) {

			Long prevTagId = null;

			for (String lastTagId : tagIds) {
				Long lastTagIdInt = convertFromStrTagId(virtualServerName, lastTagId);
				if (lastTagIdInt != null) {
					if (lastTagIdInt < currentTagIdInt) {
						if (prevTagId == null) {
							prevTagId = lastTagIdInt;
						} else {
							if (lastTagIdInt > prevTagId) {
								prevTagId = lastTagIdInt;
							}
						}
					}
				}
			}
			return prevTagId == null ? null : convertToStrTagId(virtualServerName, prevTagId);
		}

		return null;
	}

}
