package com.thed.zephyr.je.vo;


public class ExecutionSummaryImpl {
	private Integer count;
	private Integer executionStatusKey;
	private String executionStatusName;
    private String executionStatusDescription;
    private String executionStatusColor;
	private String percentage;
	
	
	/**
	 * @param count
	 * @param executionStatusKey
	 * @param executionStatusName
	 * @param executionStatusDescription
	 * @param executionStatusColor
	 */
	public ExecutionSummaryImpl(Integer count, Integer executionStatusKey,
			String executionStatusName, String executionStatusDescription,
			String executionStatusColor) {
		super();
		this.count = count;
		this.executionStatusKey = executionStatusKey;
		this.executionStatusName = executionStatusName;
		this.executionStatusDescription = executionStatusDescription;
		this.executionStatusColor = executionStatusColor;
	}
	

	/**
	 * @return the count
	 */
	public Integer getCount() {
		return count;
	}
	/**
	 * @param count the count to set
	 */
	public void setCount(Integer count) {
		this.count = count;
	}

	/**
	 * @return the executionStatusKey
	 */
	public Integer getExecutionStatusKey() {
		return executionStatusKey;
	}

	/**
	 * @param executionStatusKey the executionStatusKey to set
	 */
	public void setExecutionStatusKey(Integer executionStatusKey) {
		this.executionStatusKey = executionStatusKey;
	}

	/**
	 * @return the executionStatusName
	 */
	public String getExecutionStatusName() {
		return executionStatusName;
	}

	/**
	 * @param executionStatusName the executionStatusName to set
	 */
	public void setExecutionStatusName(String executionStatusName) {
		this.executionStatusName = executionStatusName;
	}

	/**
	 * @return the executionStatusDescription
	 */
	public String getExecutionStatusDescription() {
		return executionStatusDescription;
	}

	/**
	 * @param executionStatusDescription the executionStatusDescription to set
	 */
	public void setExecutionStatusDescription(String executionStatusDescription) {
		this.executionStatusDescription = executionStatusDescription;
	}

	/**
	 * @return the executionStatusColor
	 */
	public String getExecutionStatusColor() {
		return executionStatusColor;
	}

	/**
	 * @param executionStatusColor the executionStatusColor to set
	 */
	public void setExecutionStatusColor(String executionStatusColor) {
		this.executionStatusColor = executionStatusColor;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ExecutionSummaryImpl [count=" + count + ", executionStatusKey="
				+ executionStatusKey + ", executionStatusName="
				+ executionStatusName + ", executionStatusDescription="
				+ executionStatusDescription + ", executionStatusColor="
				+ executionStatusColor + "]";
	}

	/**
	 * @return the percentage
	 */
	public String getPercentage() {
		return percentage;
	}

	/**
	 * @param percentage the percentage to set
	 */
	public void setPercentage(String percentage) {
		this.percentage = percentage;
	}
}
