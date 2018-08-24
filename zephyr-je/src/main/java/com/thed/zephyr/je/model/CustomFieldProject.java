package com.thed.zephyr.je.model;


import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.Indexed;

import java.util.Date;

@Preload
public interface CustomFieldProject extends Entity {

	@Indexed
	public Long getProjectId();
	public void setProjectId(Long projectId);

	public Integer getCustomFieldId();
	public void setCustomFieldId(Integer customFieldId);

	@Indexed
	public boolean getIsActive();
	public void setIsActive(boolean isActive);
}
