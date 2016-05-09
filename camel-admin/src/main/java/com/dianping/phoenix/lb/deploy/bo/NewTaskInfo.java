package com.dianping.phoenix.lb.deploy.bo;

import java.util.List;

public class NewTaskInfo {

	private String taskName;

	private List<VsAndTag> selectedVsAndTags;

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public List<VsAndTag> getSelectedVsAndTags() {
		return selectedVsAndTags;
	}

	public void setSelectedVsAndTags(List<VsAndTag> selectedVsAndTags) {
		this.selectedVsAndTags = selectedVsAndTags;
	}

	@Override
	public String toString() {
		return "NewTaskInfo [vsName=" + taskName + ", selectedVsAndTags=" + selectedVsAndTags + "]";
	}

	public static class VsAndTag implements Comparable<VsAndTag> {
		private String vsName;

		private String tag;

		public VsAndTag() {

		}

		public VsAndTag(String vsName, String tag) {
			this.vsName = vsName;
			this.tag = tag;
		}

		public String getVsName() {
			return vsName;
		}

		public void setVsName(String vsName) {
			this.vsName = vsName;
		}

		public String getTag() {
			return tag;
		}

		public void setTag(String tag) {
			this.tag = tag;
		}

		@Override
		public String toString() {
			return "VsAndTag [vsName=" + vsName + ", tag=" + tag + "]";
		}

		@Override
		public int compareTo(VsAndTag o) {
			return vsName.compareTo(o.vsName);
		}

	}

}
