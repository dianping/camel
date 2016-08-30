package com.dianping.platform.slb.agent.web.wrapper;

import com.dianping.platform.slb.agent.web.model.Response;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface ResponseAction extends Action {

	Response doTransaction(Response response, String exceptionMessage, Wrapper<Response> wrapper);

}
