package com.dianping.phoenix.lb.api.aspect;

import com.dianping.phoenix.lb.action.MenuAction;
import com.dianping.phoenix.lb.constant.MessageID;
import com.dianping.phoenix.lb.exception.BizException;
import com.dianping.phoenix.lb.facade.NotSuccessException;
import com.dianping.phoenix.lb.facade.PoolNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Service("apiAspect")
public class DefaultApiAspect implements ApiAspect {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void transaction(String type, String name, ApiWrapper wrapper) {
		try {
			wrapper.doAction();
			logger.info("[success][" + type + "]" + "[" + name + "]");
		} catch (Exception e) {
			logger.error("[fail][" + type + " fail]" + "[" + name + "]", e);
		}
	}

	@Override
	public void doTransactionWithResultMap(String type, String name, ApiWrapper wrapper,
			Map<String, Object> resultMap) {
		try {
			wrapper.doAction();
			resultMap.put(MenuAction.ERROR_CODE, MenuAction.ERRORCODE_SUCCESS);
			logger.info("[success][" + type + "]" + "[" + name + "]");
		} catch (PoolNotFoundException e) {
			resultMap.put(MenuAction.ERROR_CODE, MenuAction.ERRORCODE_PARAM_ERROR);
			resultMap.put(MenuAction.MESSAGE, e.getMessage());
			logger.error("[fail][" + type + "]" + "[" + name + "]", e);
		} catch (NotSuccessException ex) {
			// v1 api ignore this exception, return success
			resultMap.put(MenuAction.ERROR_CODE, MenuAction.ERRORCODE_SUCCESS);
			logger.error("[fail][" + type + " fail]" + "[" + name + "]", ex);
		} catch (Exception e) {
			if (e instanceof BizException && ((BizException) e).getMessageId().equals(MessageID.UNSAFE_CONFIG)) {
				resultMap.put(MenuAction.ERROR_CODE, MenuAction.NOT_SAFE_CONFIG);
			} else {
				resultMap.put(MenuAction.ERROR_CODE, MenuAction.ERRORCODE_INNER_ERROR);
			}
			resultMap.put(MenuAction.MESSAGE, e.getMessage());
			logger.error("[fail][" + type + "]" + "[" + name + "]", e);
		}
	}

}
