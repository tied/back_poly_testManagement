package com.thed.zephyr.je.model;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.Indexed;

import java.util.Date;

@Preload
public interface Cycle extends Entity{
	public String getDescription();
    public void setDescription(String description);
    
    public String getEnvironment();
    public void setEnvironment(String environment);
    
    public String getBuild();
    public void setBuild(String build);

    public String getName() ;
    public void setName(String name);

    @Indexed
    public Long getVersionId() ;
    public void setVersionId(Long versionId);

    @Indexed
    public Long getSprintId() ;
    public void setSprintId(Long sprintId);
    
    /**
     * Cycle always need to belong to a project, version can be null 
     * @return projectId
     */
    @Indexed
    public Long getProjectId() ;
    public void setProjectId(Long projectId);

    public Long getStartDate() ;
    public void setStartDate(Long startDate);

    public Long getEndDate() ;
    public void setEndDate(Long endDate) ;
    
	public String getCreatedBy();
	public void setCreatedBy(String createdBy);
	
	public String getModifiedBy();
	public void setModifiedBy(String modifiedBy);

    public Date getDateCreated();
    public void setDateCreated(Date dateCreated);

    public Date getModifiedDate();
    public void setModifiedDate(Date modifiedDate);
}
