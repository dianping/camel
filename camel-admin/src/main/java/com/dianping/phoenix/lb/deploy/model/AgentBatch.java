package com.dianping.phoenix.lb.deploy.model;

public enum AgentBatch {
	ONE_BY_ONE("one-by-one", true, 1, "1 -> 1 -> 1 -> 1 (每次一台)"),

	TWO_BY_TWO("two-by-two", true, 2, "1 -> 2 -> 2 -> 2 (每次两台)"),

	THREE_BY_THREE("three-by-three", true, 3, "1 -> 3 -> 3 -> 3 (每次三台)"),

	FOUR_BY_FOUR("four-by-four", true, 4, "1 -> 4 -> 4 -> 4 (每次四台)"),

	FIVE_BY_FIVE("five-by-five", true, 5, "1 -> 5 -> 5 -> 5 (每次五台)"),

	SIX_BY_SIX("six-by-six", true, 6, "1 -> 6 -> 6 -> 6 (每次六台)"),

	PARALLEL("parallel", false, Integer.MAX_VALUE, "完全并行");

	private String id;

	private int batchSize;

	private String desc;

	/**
	 * 是否先尝试第一台，再做并行发布
	 */
	private boolean tryFirst;

	private AgentBatch(String id, boolean tryFirst, int batchSize, String description) {
		this.id = id;
		this.tryFirst = tryFirst;
		this.batchSize = batchSize;
		this.desc = description;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public String getId() {
		return id;
	}

	public String getDesc() {
		return desc;
	}

	public boolean isTryFirst() {
		return tryFirst;
	}

}
