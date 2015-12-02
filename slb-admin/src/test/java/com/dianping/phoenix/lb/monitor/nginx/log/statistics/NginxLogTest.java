package com.dianping.phoenix.lb.monitor.nginx.log.statistics;

import com.dianping.phoenix.AbstractTest;
import org.junit.Assert;
import org.junit.Test;
import org.unidal.tuple.Pair;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class NginxLogTest extends AbstractTest {

	@Test
	public void testFetchDomainAndPool() {
		String vs = "vs";
		String pool = "pool";
		Set<String> domains = new HashSet<String>(Arrays.asList("other", vs));
		Set<String> pools = new HashSet<String>(Arrays.asList("other", pool));
		String rawPoolName = "vs.pool";
		HourlyStatisticsCounter hourlyStatistics = new HourlyStatisticsCounter().setVSNames(domains)
				.setPoolNames(pools);
		Pair<String, String> pair = hourlyStatistics.extractDomainAndPool(rawPoolName);

		Assert.assertEquals(vs, pair.getKey());
		Assert.assertEquals(pool, pair.getValue());

		Pair<String, String> unnormalPair = hourlyStatistics.extractDomainAndPool("-");

		Assert.assertEquals("default", unnormalPair.getKey());
		Assert.assertEquals("-", unnormalPair.getValue());
	}
}
