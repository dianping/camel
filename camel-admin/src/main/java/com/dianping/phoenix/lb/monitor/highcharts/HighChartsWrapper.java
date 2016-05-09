package com.dianping.phoenix.lb.monitor.highcharts;

/**
 * High charts展示
 *
 * @author mengwenchao
 *         <p/>
 *         2014年7月9日 上午10:31:54
 */
public class HighChartsWrapper {

	private String title;

	private String subTitle;

	private Series[] series;

	private PlotOption plotOption;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSubTitle() {
		return subTitle;
	}

	public void setSubTitle(String subTitle) {
		this.subTitle = subTitle;
	}

	public Series[] getSeries() {
		return series;
	}

	public void setSeries(Series[] series) {
		this.series = series;
	}

	public PlotOption getPlotOption() {
		return plotOption;
	}

	public void setPlotOption(PlotOption plotOption) {
		this.plotOption = plotOption;
	}

	public static class Series {

		private String name;

		private Long[] data;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Long[] getData() {
			return data;
		}

		public void setData(Long[] ls) {
			this.data = ls;
		}
	}

	public static class PlotOption {

		private PlotOptionSeries series;

		public PlotOptionSeries getSeries() {
			return series;
		}

		public void setSeries(PlotOptionSeries series) {
			this.series = series;
		}
	}

	public static class PlotOptionSeries {

		/**
		 * 图表开始时间(毫秒)
		 */
		private long pointStart;
		/**
		 * 图标时间间隔（毫秒）
		 */
		private long pointInterval;

		public long getPointStart() {
			return pointStart;
		}

		public void setPointStart(long pointStart) {
			this.pointStart = pointStart;
		}

		public long getPointInterval() {
			return pointInterval;
		}

		public void setPointInterval(long pointInterval) {
			this.pointInterval = pointInterval;
		}

	}

}
