package com.dianping.platform.slb.agent.task;

import java.io.OutputStream;
import java.io.Serializable;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface Task extends Serializable {

	OutputStream getTaskOutputStream();

	void setTaskOutputStream(OutputStream outputStream);

}
