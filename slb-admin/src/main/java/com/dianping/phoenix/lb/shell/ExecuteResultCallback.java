package com.dianping.phoenix.lb.shell;

/**
 * dianping.com @2015
 * slb - soft load balance
 * <p/>
 * Created by leon.li(Li Yang)
 */
public interface ExecuteResultCallback {

	void onProcessCompleted(int exitCode);

	void onProcessFailed(Exception e);

}
