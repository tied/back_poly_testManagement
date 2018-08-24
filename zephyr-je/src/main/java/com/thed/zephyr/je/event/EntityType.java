package com.thed.zephyr.je.event;

public enum EntityType {
	CYCLE("CYCLE"), SCHEDULE("EXECUTION"), SCHEDULE_DEFECT("EXECUTION_DEFECT"), TESTSTEP(
			"TESTSTEP"), STEPRESULT("STEPRESULT"), STEP_DEFECT("STEP_DEFECT"), FOLDER("FOLDER"),
			ISSUE("ISSUE"), PROJECT("PROJECT"), SCHEDULE_WORKFLOW("EXECUTION_WORKFLOW");

	private String entityType = null;

	EntityType(String entityType) {
		this.entityType = entityType;
	}

	public String getEntityType() {
		return entityType;
	}
}
