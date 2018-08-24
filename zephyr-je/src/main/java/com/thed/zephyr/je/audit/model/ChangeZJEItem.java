package com.thed.zephyr.je.audit.model;

import net.java.ao.Entity;
import net.java.ao.schema.StringLength;

public interface ChangeZJEItem extends Entity{
	
	public ChangeZJEGroup getChangeZJEGroup();
	public void setChangeZJEGroup(ChangeZJEGroup changeZJEGroup);
	
	public String getZephyrFieldType();
	public void setZephyrFieldType(String zephyrFieldType);
	
	public String getZephyrField();
	public void setZephyrField(String zephyrField);
	
	@StringLength(StringLength.UNLIMITED)
	public String getOldValue();
	public void setOldValue(String oldValue);
	
	@StringLength(StringLength.UNLIMITED)
	public String getNewValue();
	public void setNewValue(String newValue);
	
}
