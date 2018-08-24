package com.thed.zephyr.je.model;

/**
 * @author manjunath
 *
 */
public enum ExecutionWorkflowStatus {
	
	CREATED(1, "CREATED"), STARTED(2, "STARTED"), PAUSED(3, "PAUSED"), COMPLETED(4, "COMPLETED"), REOPEN(4, "REOPEN");
	
	private int id;
	
	private String name;
	
	private ExecutionWorkflowStatus(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

}
