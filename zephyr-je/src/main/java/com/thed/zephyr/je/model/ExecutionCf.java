package com.thed.zephyr.je.model;

import java.util.Date;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;

@Preload
public interface ExecutionCf extends Entity {
	public CustomField getCustomField();

	public void setCustomField(CustomField customField);

	public Double getNumberValue();

	public void setNumberValue(Double numberValue);

    @StringLength(StringLength.UNLIMITED)
	public String getStringValue();

    @StringLength(StringLength.UNLIMITED)
	public void setStringValue(String stringValue);

	public Date getDateValue();

	public void setDateValue(Date dateValue);

	@StringLength(StringLength.UNLIMITED)
	public String getLargeValue();

	@StringLength(StringLength.UNLIMITED)
	public void setLargeValue(String largeValue);

	@Indexed
	public int getExecutionId();

	public void setExecutionId(int executionId);

    @Indexed
	public String getSelectedOptions();

	public void setSelectedOptions(String selectedOptions);
}
