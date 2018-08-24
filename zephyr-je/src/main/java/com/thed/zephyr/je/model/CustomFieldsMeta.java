package com.thed.zephyr.je.model;

import net.java.ao.Entity;
import net.java.ao.Preload;

@Preload
public interface CustomFieldsMeta extends Entity {

	public String getLabel();

	public void setLabel(String label);

	public String getDescription();

	public void setDescription(String description);

	public boolean isOptions();

	public void setOptions(boolean options);

	public String getType();

	public void setType(String type);

	public String getImage();

	public void setImage(String imageClass);

	public String getDisplayType();

	public void setDisplayType(String displayType);
}
