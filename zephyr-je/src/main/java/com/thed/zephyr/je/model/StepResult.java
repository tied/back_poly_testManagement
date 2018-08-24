package com.thed.zephyr.je.model;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;

@Preload
public interface StepResult extends Entity {

    public Long getExecutedOn();

    public void setExecutedOn(Long executedOn);

    @Indexed
    public String getStatus();
    public void setStatus(String status);

    @StringLength(StringLength.UNLIMITED)
    public String getComment();

    @StringLength(StringLength.UNLIMITED)
    public void setComment(String comment);

    public String getExecutedBy();

    public void setExecutedBy(String executedBy);

    //How to add Foreign Key constraints on Schedule.getID() and Step.getID() ?
    @Indexed
    public Integer getScheduleId();
    public void setScheduleId(Integer scheduleId);

    public Teststep getStep();
    public void setStep(Teststep step);

    //Project ID is needed for retrieving attachments
    public Long getProjectId();

    public void setProjectId(Long projectId);

    public String getCreatedBy();

    public void setCreatedBy(String createdBy);

    public String getModifiedBy();

    public void setModifiedBy(String modifiedBy);
}
