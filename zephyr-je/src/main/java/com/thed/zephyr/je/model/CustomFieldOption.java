package com.thed.zephyr.je.model;

import net.java.ao.Entity;
import net.java.ao.Preload;

@Preload
public interface CustomFieldOption extends Entity {

	public CustomField getCustomField();

	public void setCustomField(CustomField customField);

	public Long getParentOptionId();

	public void setParentOptionId(Long parentOptionId);

	public int getSequence();

	public void setSequence(int sequence);

	public String getOptionValue();

	public void setOptionValue(String optionValue);

	public boolean getIsDisabled();

	public void setIsDisabled(boolean idDisabled);

}
