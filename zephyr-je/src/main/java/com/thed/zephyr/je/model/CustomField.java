package com.thed.zephyr.je.model;


import java.util.Date;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.Unique;

@Preload
public interface CustomField extends Entity {

	@Indexed
	public String getName();

	public void setName(String name);

	public String getDescription();

	public void setDescription(String description);

	@Indexed
	public String getCustomFieldType();

	public void setCustomFieldType(String customFieldType);

	public String getAliasName();

	public void setAliasName(String aliasName);

	@Indexed
	public boolean getIsActive();

	public void setIsActive(boolean isActive);

	public String getDefaultValue();

	public void setDefaultValue(String defaultValue);

	public Long getProjectId();

	public void setProjectId(Long projectId);

	public String getDisplayName();

	public void setDisplayName(String displayName);

	public Date getCreatedOn();

	public void setCreatedOn(Date createdOn);

	public Date getModifiedOn();

	public void setModifiedOn(Date createdOn);

	public String getModifiedBy();

	public void setModifiedBy(String modifiedBy);

	public String getCreatedBy();

	public void setCreatedBy(String createdBy);

	@Indexed
	public String getZFJEntityType();
	
	public void setZFJEntityType(String zfjEntityType);

	public String getDisplayFieldType();

	public void setDisplayFieldType(String displayFieldType);

}
