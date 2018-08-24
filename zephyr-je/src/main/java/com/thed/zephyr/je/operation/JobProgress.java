package com.thed.zephyr.je.operation;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.abs;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobProgress implements Serializable {

    private String id;
    private Integer status;
    private String name;
    private Integer totalSteps;
    private Integer completedSteps;
    private Date startTime;
    private Date endTime;
    private long progress;
    private long timeTaken;
    private Boolean canceledJob;
    private String entity;
    private String entityId;
    private String timeSpend;
    private String jobStoppedBy;

    /**
     * Contains information about current processing step.
     */
    private String stepMessage;
    /**
     * Contains all stepMessages
     */
    private List<String> stepMessages;
    /**
     * Contains detailed message of all the processed steps.
     */
    private String message;
    /**
     * Contains the final summary of job.
     */
    private String summaryMessage;
    /**
     * Contains the entity name of job.
     */
    private String stepLabel;
    /**
     * Contains the error message.
     */
    private String errorMessage;

    public JobProgress() {
        this.stepMessages = new ArrayList<>();
    }

    public JobProgress(String id, String name, Integer status, Date startTime,  Integer totalSteps, Integer completedSteps) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.startTime = startTime;
        this.totalSteps = totalSteps;
        this.completedSteps = completedSteps;
        this.stepMessages = new ArrayList<>();
    }

    public static String toJsonString(String message, Integer value) {
        String result;
        result = "{\"message\": \"" + message + "\", \"value\": \"" + value +"\"}";
        return result;
    }

    public String getId() {
        return id;
    }

    public Integer getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public Integer getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(Integer totalSteps) {
        this.totalSteps = totalSteps;
    }

    public Integer getCompletedSteps() {
        return completedSteps;
    }

    public void setCompletedSteps(Integer completedSteps) {
        this.completedSteps = completedSteps;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getStepMessage() {
        return stepMessage;
    }

    public void setStepMessage(String stepMessage) {
        this.stepMessage = stepMessage;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSummaryMessage() {
        return summaryMessage;
    }

    public String getStepLabel() {
        return stepLabel;
    }

    public void setStepLabel(String stepLabel) {
        this.stepLabel = stepLabel;
    }

    public void setSummaryMessage(String summaryMessage) {
        this.summaryMessage = summaryMessage;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public List<String> getStepMessages() {
        return this.stepMessages;
    }

    public void addStepMassages(String stepMessage) {
        this.stepMessages.add(stepMessage);
    }
    public Double getProgress() {
    	if(completedSteps == 0){
    		return 0.0;
    	}
        if (totalSteps == 0) {
            return 1.0;
        } else {
            return  completedSteps.doubleValue() / totalSteps.doubleValue() < 1.0 ? completedSteps.doubleValue() / totalSteps.doubleValue() : 1.0;
        }
    }

    public String getTimeSpend() {
        Date endTime = new Date();
        if(this.endTime != null) {
            endTime = this.endTime;
        }
        //calculate time spent
        long timeSpent = abs(endTime.getTime() - this.startTime.getTime());
        return  String.format("%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes(timeSpent),
                TimeUnit.MILLISECONDS.toSeconds(timeSpent) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeSpent))
                );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JobProgress that = (JobProgress) o;

        if (completedSteps != null ? !completedSteps.equals(that.completedSteps) : that.completedSteps != null)
            return false;
        if (endTime != null ? !endTime.equals(that.endTime) : that.endTime != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (message != null ? !message.equals(that.message) : that.message != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (startTime != null ? !startTime.equals(that.startTime) : that.startTime != null) return false;
        if (status != null ? !status.equals(that.status) : that.status != null) return false;
        if (stepLabel != null ? !stepLabel.equals(that.stepLabel) : that.stepLabel != null) return false;
        if (stepMessage != null ? !stepMessage.equals(that.stepMessage) : that.stepMessage != null) return false;
        if (summaryMessage != null ? !summaryMessage.equals(that.summaryMessage) : that.summaryMessage != null)
            return false;
        if (totalSteps != null ? !totalSteps.equals(that.totalSteps) : that.totalSteps != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (totalSteps != null ? totalSteps.hashCode() : 0);
        result = 31 * result + (completedSteps != null ? completedSteps.hashCode() : 0);
        result = 31 * result + (startTime != null ? startTime.hashCode() : 0);
        result = 31 * result + (endTime != null ? endTime.hashCode() : 0);
        result = 31 * result + (stepMessage != null ? stepMessage.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (summaryMessage != null ? summaryMessage.hashCode() : 0);
        result = 31 * result + (stepLabel != null ? stepLabel.hashCode() : 0);
        return result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Boolean getCanceledJob() {
        return canceledJob;
    }

    public void setCanceledJob(Boolean canceledJob) {
        this.canceledJob = canceledJob;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public void setProgress(long progress) {
        this.progress = progress;
    }

    public long getTimeTaken() {
        return timeTaken;
    }

    public void setTimeTaken(long timeTaken) {
        this.timeTaken = timeTaken;
    }

    public void setStepMessages(List<String> stepMessages) {
        this.stepMessages = stepMessages;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public void setTimeSpend(String timeSpend) {
        this.timeSpend = timeSpend;
    }

	public String getJobStoppedBy() {
		return jobStoppedBy;
	}

	public void setJobStoppedBy(String jobStoppedBy) {
		this.jobStoppedBy = jobStoppedBy;
	}

}
