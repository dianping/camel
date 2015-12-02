package com.dianping.phoenix.lb.monitor;

import java.io.IOException;

public interface TengineStatusService {

	TengineStatus getStatus(String tengineIp) throws IOException;

}
