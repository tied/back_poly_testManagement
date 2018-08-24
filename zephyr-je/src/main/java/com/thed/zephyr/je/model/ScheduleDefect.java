package com.thed.zephyr.je.model;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;

public interface ScheduleDefect extends Entity {
	@Indexed
	public Integer getScheduleId();
	public void setScheduleId(Integer scheduleId);

	@Indexed
	public Integer getDefectId();
	public void setDefectId(Integer defectId);
}
