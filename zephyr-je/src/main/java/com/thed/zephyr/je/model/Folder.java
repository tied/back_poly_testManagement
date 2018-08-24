package com.thed.zephyr.je.model;

import java.util.Date;

import net.java.ao.Entity;
import net.java.ao.Preload;

/**
 * This class acts as model which holds the folder related information.
 * 
 * @author manjunath
 *
 */
@Preload
public interface Folder extends Entity {
	
	public String getName() ;
    public void setName(String name);

	public String getDescription();
    public void setDescription(String description);
    
    public Date getDateCreated();
    public void setDateCreated(Date dateCreated);
    
	public String getCreatedBy();
	public void setCreatedBy(String createdBy);
	
	public Date getModifiedDate();
    public void setModifiedDate(Date modifedDate);
	
	public String getModifiedBy();
	public void setModifiedBy(String modifiedBy);

}
