package com.thed.zephyr.je.audit.model;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;

public interface ChangeZJEGroup extends Entity{

	public Integer getZephyrEntityId();
	public void setZephyrEntityId(Integer zephyrEntityId);
	
	public Integer getIssueId();
	public void setIssueId(Integer issueId);

	@Indexed
	public Integer getScheduleId();
	public void setScheduleId(Integer scheduleId);

	@Indexed
	public Integer getCycleId();
	public void setCycleId(Integer cycleId);

	@Indexed
	public String getZephyrEntityType();
	public void setZephyrEntityType(String zephyrEntityType);

	@Indexed
	public String getZephyrEntityEvent();
	public void setZephyrEntityEvent(String zephyrEntityEvent);
	
	public String getAuthor();
	public void setAuthor(String author);

	@Indexed
	public Long getCreated();
	public void setCreated(Long created);
	
	public Long getProjectId();
    public void setProjectId(Long projectId);
	
}
