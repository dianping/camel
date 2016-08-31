package com.dianping.platform.slb.agent.web.wrapper.impl;

import com.dianping.platform.slb.agent.web.model.Response;
import com.dianping.platform.slb.agent.web.wrapper.ResponseAction;
import com.dianping.platform.slb.agent.web.wrapper.Wrapper;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
@Service
public class DefaultResponseAction implements ResponseAction {

	private static final String EXCEPTION_STACK_INFO = "\nException stack: ";

	@Override
	public Response doTransaction(Response response, String exceptionMessage, Wrapper<Response> wrapper) {
		try {
			return wrapper.doAction();
		} catch (Exception ex) {
			response.setStatus(Response.Status.FAIL);

			String errorMessage = buildErrorMessage(exceptionMessage, ex);

			response.setMessage(errorMessage);
			return response;
		}
	}

	public static String buildErrorMessage(String exceptionMessage, Exception ex) {
		StringBuilder builder = new StringBuilder(
				exceptionMessage == null ? EXCEPTION_STACK_INFO : exceptionMessage + EXCEPTION_STACK_INFO);
		StringWriter strWriter = new StringWriter();

		ex.printStackTrace(new PrintWriter(strWriter));
		builder.append(strWriter.toString());

		return builder.toString();
	}

}
