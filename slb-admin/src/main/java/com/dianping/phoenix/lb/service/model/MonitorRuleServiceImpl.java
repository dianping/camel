package com.dianping.phoenix.lb.service.model;

import com.dianping.phoenix.lb.constant.MessageID;
import com.dianping.phoenix.lb.dao.MonitorRuleDao;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.model.entity.MonitorRule;
import com.dianping.phoenix.lb.service.ConcurrentControlServiceTemplate;
import com.dianping.phoenix.lb.utils.ExceptionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author liyang
 *         <p/>
 *         2015年3月31日 下午4:54:04
 */
@Service
public class MonitorRuleServiceImpl extends ConcurrentControlServiceTemplate implements MonitorRuleService {

	private MonitorRuleDao m_monitorRuleDao;

	@Autowired(required = true)
	public MonitorRuleServiceImpl(MonitorRuleDao monitorRuleDao) {
		super();
		this.m_monitorRuleDao = monitorRuleDao;
	}

	@Override
	public void addOrUpdateMonitorRule(String ruleId, final MonitorRule rule) throws BizException {
		if (ruleId == null || rule == null) {
			return;
		}
		if (!ruleId.equals(rule.getId())) {
			return;
		}
		validate(rule);

		write(new WriteOperation<Void>() {
			@Override
			public Void doWrite() throws Exception {
				m_monitorRuleDao.addOrUpdate(rule);
				return null;
			}
		});
	}

	@Override
	public void deleteMonitorRule(final String ruleId) throws BizException {
		if (StringUtils.isBlank(ruleId)) {
			ExceptionUtils.throwBizException(MessageID.MONITORRULE_ID_EMPTY);
		}

		try {
			write(new WriteOperation<Void>() {
				@Override
				public Void doWrite() throws Exception {
					m_monitorRuleDao.delete(ruleId);
					return null;
				}
			});
		} catch (BizException e) {
		}
	}

	@Override
	public MonitorRule findMonitorRule(final String ruleId) throws BizException {
		if (StringUtils.isBlank(ruleId)) {
			ExceptionUtils.throwBizException(MessageID.MONITORRULE_ID_EMPTY);
		}

		return read(new ReadOperation<MonitorRule>() {
			@Override
			public MonitorRule doRead() throws BizException {
				return m_monitorRuleDao.find(ruleId);
			}
		});
	}

	@Override
	public List<MonitorRule> listMonitorRules() {
		try {
			return read(new ReadOperation<List<MonitorRule>>() {
				@Override
				public List<MonitorRule> doRead() throws Exception {
					return m_monitorRuleDao.list();
				}
			});
		} catch (BizException e) {
			return null;
		}
	}

	public void setMonitorRuleDao(MonitorRuleDao monitorRuleDao) {
		this.m_monitorRuleDao = monitorRuleDao;
	}

	private void validate(MonitorRule monitorRule) throws BizException {
		if (StringUtils.isBlank(monitorRule.getId())) {
			ExceptionUtils.throwBizException(MessageID.MONITORRULE_ID_EMPTY);
		}
		if (StringUtils.isBlank(monitorRule.getPool())) {
			ExceptionUtils.throwBizException(MessageID.MONITORRULE_POOL_EMPTY);
		}
		if (StringUtils.isBlank(monitorRule.getStatusCode())) {
			ExceptionUtils.throwBizException(MessageID.MONITORRULE_STATUS_CODE_EMPTY);
		}
		if (monitorRule.getMinute() == null) {
			ExceptionUtils.throwBizException(MessageID.MONITORRULE_MINUTE_EMPTY);
		}
		if (monitorRule.getValue() == null) {
			ExceptionUtils.throwBizException(MessageID.MONITORRULE_VALUE_EMPTY);
		}
	}
}
