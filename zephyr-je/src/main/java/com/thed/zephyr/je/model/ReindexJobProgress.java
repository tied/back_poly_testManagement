package com.thed.zephyr.je.model;

import java.util.Date;

import net.java.ao.Entity;
import net.java.ao.Preload;

@Preload(value = {"ID"})
public interface ReindexJobProgress extends Entity {
	
	public String getName();

    public void setName(String name);
	
	public String getJobProgressId();

    public void setJobProgressId(String jobProgressId);
	
    public Long getPreviousIndexedCount();

    public void setPreviousIndexedCount(Long previousIndexedCount);
    
    public Integer getNodeCount();
    
    public void setNodeCount(Integer nodeCount);
    
    public Long getCurrentIndexedCount();

    public void setCurrentIndexedCount(Long currentIndexedCount);
    
    public Integer getCompletedNodeCount();

    public void setCompletedNodeCount(Integer completedNodeCount);
    
	public Date getDateIndexed();

    public void setDateIndexed(Date dateIndexed);
    
    public Long getProjectId();
    
    public void setProjectId(Long projectId);

}
