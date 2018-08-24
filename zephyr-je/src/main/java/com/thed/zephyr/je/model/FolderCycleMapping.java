package com.thed.zephyr.je.model;

import java.util.Date;

import com.thed.zephyr.je.index.Searchable;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.Indexed;

/**
 * Class acts as model which holds cycle-folder mapping information.
 * 
 * @author manjunath
 *
 */
@Preload
public interface FolderCycleMapping extends Entity {
	@Searchable
    public Folder getFolder();
    public void setFolder(Folder folder);
    
    @Searchable
    public Cycle getCycle();
    public void setCycle(Cycle cycle);
    
    public Date getDateCreated();
    public void setDateCreated(Date dateCreated);
    
	public String getCreatedBy();
	public void setCreatedBy(String createdBy);
	
	public Long getSprintId() ;
    public void setSprintId(Long sprintId);
    
    @Searchable
    @Indexed
    public Long getVersionId();

    public void setVersionId(Long versionId);

    @Searchable
    @Indexed
    public Long getProjectId();

    public void setProjectId(Long projectId);
}
