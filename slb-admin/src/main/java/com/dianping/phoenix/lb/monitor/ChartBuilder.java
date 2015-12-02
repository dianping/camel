package com.dianping.phoenix.lb.monitor;

import com.dianping.phoenix.lb.monitor.ReqStatusContainer.DataWrapper;
import com.dianping.phoenix.lb.monitor.highcharts.HighChartsWrapper;
import com.dianping.phoenix.lb.monitor.highcharts.HighChartsWrapper.PlotOption;
import com.dianping.phoenix.lb.monitor.highcharts.HighChartsWrapper.PlotOptionSeries;
import com.dianping.phoenix.lb.monitor.highcharts.HighChartsWrapper.Series;

import java.util.Collection;

/**
 * 构建展示数据
 *
 * @author mengwenchao
 *         <p/>
 *         2014年7月9日 上午10:32:51
 */
public class ChartBuilder {

	/**
	 * @param endTime     数据终止时间(ms)
	 * @param interval    数据采集间隔(ms)
	 * @param duration    数据采集长度(minutes)
	 * @param data
	 * @param dataDengine
	 * @param title
	 * @param subTitle
	 * @return
	 */
	public static HighChartsWrapper getHighChart(long endTime, long duration, Collection<DataWrapper> data,
			String title, String subTitle) {

		HighChartsWrapper hcw = new HighChartsWrapper();
		hcw.setTitle(title);
		hcw.setSubTitle(subTitle);

		Series[] series = new Series[data.size()];
		int i = 0;

		int dataLength = 0;
		int realInterval = 0;
		for (DataWrapper dw : data) {

			dataLength = dw.getData().length;
			realInterval = dw.getInterval();
			Series se = new Series();
			se.setData(dw.getData());
			se.setName(dw.getDesc() + ",全部请求数:" + dw.getTotal());
			series[i++] = se;
		}
		hcw.setSeries(series);

		PlotOption plotOption = new PlotOption();
		PlotOptionSeries pos = new PlotOptionSeries();

		pos.setPointStart(endTime - dataLength * realInterval);
		pos.setPointInterval(realInterval);
		plotOption.setSeries(pos);

		hcw.setPlotOption(plotOption);
		return hcw;
	}
}
