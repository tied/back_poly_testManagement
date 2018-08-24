package com.thed.zephyr.je.model;

import com.thed.zephyr.je.index.Searchable;
import com.thed.zephyr.je.index.bridge.*;
import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;

import java.util.Date;

@Preload(value = {"ID"})
@ClassBridge(impl = {ExternalEntityFieldBridge.class, CustomFieldEntityFieldBridge.class})
public interface Schedule extends Entity {

    @Searchable
    @Indexed
    public String getAssignedTo();

    public void setAssignedTo(String testerUserName);

    @Searchable
    @FieldBridge(impl = DateToStringFieldBridge.class)
    public Date getDateCreated();

    public void setDateCreated(Date dateCreated);

    //Savind Date as a Long object.
    public Long getExecutedOn();

    public void setExecutedOn(Long executedOn);

    @Searchable
    public Cycle getCycle();
    public void setCycle(Cycle cycle);

    @Searchable
    @Indexed
    public Long getVersionId();

    public void setVersionId(Long versionId);

    @Searchable
    @Indexed
    public Long getProjectId();

    public void setProjectId(Long projectId);

    @Searchable
    public Long getActualExecutionTime();

    public void setActualExecutionTime(Long actualExecutionTime);

    @Searchable
    @Indexed
    public String getStatus();

    public void setStatus(String status);

    @Searchable
    @StringLength(StringLength.UNLIMITED)
    public String getComment();

    @StringLength(StringLength.UNLIMITED)
    public void setComment(String comment);

    @Searchable
    @Indexed
    public Integer getIssueId();

    public void setIssueId(Integer issueId);

    @Searchable
    @Indexed
    public String getExecutedBy();

    public void setExecutedBy(String executedBy);

    @Searchable
    public Integer getOrderId();

    public void setOrderId(Integer orderId);

    @Searchable
    public String getCreatedBy();

    public void setCreatedBy(String createdBy);

    @Searchable
    public String getModifiedBy();

    public void setModifiedBy(String modifiedBy);
    
    @Searchable
    public Folder getFolder();

    public void setFolder(Folder folder);
    
    @Searchable
    public ExecutionWorkflowStatus getExecutionWorkflowStatus();
    
    public void setExecutionWorkflowStatus(ExecutionWorkflowStatus executionWorkflowStatus);
    
    @Searchable
    public Long getEstimatedTime();
    
    public void setEstimatedTime(Long estimatedTime);
    
    @Searchable
    public Long getLoggedTime();
    
    public void setLoggedTime(Long loggedTime);

    @Searchable
    @FieldBridge(impl = DateToStringFieldBridge.class)
    public Date getModifiedDate();

    public void setModifiedDate(Date modifiedDate);
}
