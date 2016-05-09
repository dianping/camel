package com.dianping.phoenix.lb.service.model;

import com.dianping.phoenix.lb.dao.CmdbDao;
import com.dianping.phoenix.lb.model.entity.CmdbInfo;
import com.dianping.phoenix.lb.utils.GsonUtils;
import com.dianping.phoenix.lb.utils.IOUtilsWrapper;
import com.google.gson.reflect.TypeToken;
import org.codehaus.plexus.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 * <p/>
 * not available for out environment. cmdb is used to store project and server infos inner dianping.
 */
@Service
public class CmdbService {

	private static final List<String> DEFAULT_MAIL_RECEIVERS = Collections
			.unmodifiableList(Arrays.asList("leon.li@dianping.com"));

	private static final List<String> DEFAULT_SMS_RECEIVERS = Collections
			.unmodifiableList(Arrays.asList("18662513308"));

	private static final String CMDB_INFO_URL = "";

	// add switch to shut down this feature
	private static final Boolean SWITCH_STATUS = false;

	@Autowired
	private CmdbDao m_cmdbDao;

	private static CmdbInfo fetchCmdbInfo(String poolName) throws IOException {
		if (!SWITCH_STATUS) {
			throw new RuntimeException("cmdb update feature is off!");
		}
		URL cmdbURL = new URL(String.format(CMDB_INFO_URL, poolName));
		HttpURLConnection connection = (HttpURLConnection) cmdbURL.openConnection();

		connection.setConnectTimeout(2000);

		String responseStr = IOUtilsWrapper.convetStringFromRequest(connection.getInputStream());
		Map<String, Map<String, Object>> responseMap = GsonUtils
				.fromJson(responseStr, new TypeToken<Map<String, Map<String, Object>>>() {
				}.getType());
		Map<String, Object> projectMap = responseMap.get("project");
		CmdbInfo result = new CmdbInfo(poolName);
		String rawOdMail = (String) projectMap.get("project_email");

		if (StringUtils.isNotEmpty(rawOdMail)) {
			for (String receiver : rawOdMail.split(",")) {
				result.addPdMails(receiver);
			}
		}

		String rawOpMail = (String) projectMap.get("op_email");

		if (StringUtils.isNotEmpty(rawOpMail)) {
			for (String receiver : rawOpMail.split(",")) {
				result.addOpMails(receiver);
			}
		}
		String rawOdPhone = (String) projectMap.get("rd_mobile");

		if (StringUtils.isNotEmpty(rawOdPhone)) {
			for (String receiver : rawOdPhone.split(",")) {
				result.addPdPhones(receiver);
			}
		}

		String rawOpPhone = (String) projectMap.get("op_mobile");

		if (StringUtils.isNotEmpty(rawOpPhone)) {
			for (String receiver : rawOpPhone.split(",")) {
				result.addOpPhones(receiver);
			}
		}
		return result;
	}

	public void addOrUpdate(String poolName) throws IOException {
		CmdbInfo cmdbInfo = fetchCmdbInfo(poolName);

		m_cmdbDao.addOrUpdate(cmdbInfo);
	}

	public List<String> getCmdbMails(String poolName) {
		try {
			CmdbInfo cmdbInfo = m_cmdbDao.findByPoolName(poolName);

			if (cmdbInfo == null) {
				cmdbInfo = fetchCmdbInfo(poolName);
				m_cmdbDao.addOrUpdate(cmdbInfo);
			}

			List<String> receivers = new ArrayList<String>();

			receivers.addAll(cmdbInfo.getPdMailsList());
			receivers.addAll(cmdbInfo.getOpMailsList());
			return receivers;
		} catch (IOException ex) {
			return DEFAULT_MAIL_RECEIVERS;
		}
	}

	public List<String> getCmdbPhones(String poolName) {
		try {
			CmdbInfo cmdbInfo = m_cmdbDao.findByPoolName(poolName);

			if (cmdbInfo == null) {
				cmdbInfo = fetchCmdbInfo(poolName);
				m_cmdbDao.addOrUpdate(cmdbInfo);
			}

			List<String> receivers = new ArrayList<String>();

			receivers.addAll(cmdbInfo.getPdPhonesList());
			receivers.addAll(cmdbInfo.getOpPhonesList());
			return receivers;
		} catch (IOException ex) {
			return DEFAULT_SMS_RECEIVERS;
		}
	}

}