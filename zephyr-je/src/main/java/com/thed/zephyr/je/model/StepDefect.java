package com.thed.zephyr.je.model;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.Indexed;

@Preload
public interface StepDefect extends Entity {
	
	public Integer getStepId();
	public void setStepId(Integer stepId);

	@Indexed
	public Integer getDefectId();
	public void setDefectId(Integer defectId);

	@Indexed
	public Integer getScheduleId();
	public void setScheduleId(Integer scheduleId);

	public StepResult getStepResult();
	public void setStepResult(StepResult stepResult);
}