package com.thed.zephyr.je.vo;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.thed.zephyr.je.config.model.ExecutionStatus;

/**
 * @since v2.0
 */
@XmlRootElement(name = "ZQLSchedule")
public class ZQLScheduleBean {
	@XmlElement
	private Integer id;
	@XmlElement
	private Integer orderId;
	@XmlElement
	private Integer cycleId;
	@XmlElement
	private String cycleName;
	@XmlElement
	private String issueId;
	@XmlElement
	private String issueKey;
	@XmlElement
	private String issueSummary;
	@XmlElement
	private List<String> labels;
	@XmlElement
	private String issueDescription;
	@XmlElement
	private String projectKey;
	@XmlElement
	private Long projectId;
	@XmlElement
	private String project;
	@XmlElement
	private Long projectAvatarId;	
	@XmlElement
	private String priority;
	@XmlElement
	private List<Map<String,Object>> components;
	@XmlElement
	private Long versionId;
	@XmlElement
	private String versionName;
	@XmlElement
	private ExecutionStatus status;
    @XmlElement
    private Integer statusId;
	@XmlElement
	private String executedOn;
	@XmlElement
	private String creationDate;	
	@XmlElement
	private String comment;
	@XmlElement
	private String htmlComment;	
	@XmlElement
	private String executedBy;
	@XmlElement
	private String executedByUserName;
	@XmlElement
	private List<TeststepBean> testStepBean;
	@XmlElement
	private List<ExecutionDefectBean> executionDefects;	
	@XmlElement
	private List<String> stepDefects;
	@XmlElement
	private List<String> testDefectsUnMasked;
	@XmlElement
	private List<String> stepDefectsUnMasked;
	@XmlElement
	private Integer executionDefectCount;	
	@XmlElement
	private Integer stepDefectCount;	
	@XmlElement
	private Integer totalDefectCount;
	@XmlElement
	private String executedByDisplay;	
	@XmlElement
	private String assignee;
	@XmlElement
	private String assigneeUserName;
	@XmlElement
	private String assigneeDisplay;
	@XmlElement
	private boolean canViewIssue = true;
	@XmlElement
	private Long folderId;
	@XmlElement
	private String folderName;
	@XmlElement
	private  Map<String,String> customFieldsValueMap;

	@XmlElement
	private  Long estimatedTime;

	@XmlElement
	private String formattedEstimatedTime;

	@XmlElement
	private Long loggedTime;

	@XmlElement
	private String formattedLoggedTime;

	@XmlElement
    private String executionWorkflowStatus;

	public ZQLScheduleBean() {}
	
	public ZQLScheduleBean(Integer scheduleId,String cycleName,String issueKey,String project,
			String priority,List<Map<String,Object>> component,String versionName,ExecutionStatus status,String executedOn,
			String comment,String executedBy,Long projectAvatarId,String creationDate,String assignee,Long folderId,String folderName, Long estimatedTime, Long loggedTime) {
		this.id = scheduleId;
		this.cycleName=cycleName;
		this.issueKey=issueKey;
		this.project=project;
		this.priority=priority;
		this.components=components;
		this.versionName=versionName;
		this.status=status;
		this.executedOn=executedOn;
		this.comment=comment;
		this.executedBy=executedBy;
		this.projectAvatarId=projectAvatarId;
		this.creationDate=creationDate;
		this.assignee = assignee;
		this.folderId = folderId;
		this.folderName = folderName;
		this.estimatedTime=estimatedTime;
		this.loggedTime=loggedTime;
	}
	
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer scheduleId) {
		this.id = scheduleId;
	}
	public String getCycleName() {
		return cycleName;
	}
	public void setCycleName(String cycleName) {
		this.cycleName = cycleName;
	}
	public String getIssueKey() {
		return issueKey;
	}
	public void setIssueKey(String issueKey) {
		this.issueKey = issueKey;
	}
	public String getProject() {
		return project;
	}
	public void setProject(String project) {
		this.project = project;
	}
	public String getPriority() {
		return priority;
	}
	public void setPriority(String priority) {
		this.priority = priority;
	}

	public String getVersionName() {
		return versionName;
	}
	public void setVersionName(String versionName) {
		this.versionName = versionName;
	}
	public ExecutionStatus getStatus() {
		return status;
	}
	public void setStatus(ExecutionStatus status) {
		this.status = status;
	}
	public String getExecutedOn() {
		return executedOn;
	}
	public void setExecutedOn(String executedOn) {
		this.executedOn = executedOn;
	}
	public String getExecutedBy() {
		return executedBy;
	}
	public void setExecutedBy(String executedBy) {
		this.executedBy = executedBy;
	}
    

	public List<TeststepBean> getTestStepBean() {
		return testStepBean;
	}

	public void setTestStepBean(List<TeststepBean> testStepBean) {
		this.testStepBean = testStepBean;
	}

	/**
	 * @return the cycleId
	 */
	public Integer getCycleId() {
		return cycleId;
	}

	/**
	 * @param cycleId the cycleId to set
	 */
	public void setCycleId(Integer cycleId) {
		this.cycleId = cycleId;
	}

	/**
	 * @return the versionId
	 */
	public Long getVersionId() {
		return versionId;
	}

	/**
	 * @param versionId the versionId to set
	 */
	public void setVersionId(Long versionId) {
		this.versionId = versionId;
	}

	/**
	 * @return the projectKey
	 */
	public String getProjectKey() {
		return projectKey;
	}

	/**
	 * @param projectKey the projectKey to set
	 */
	public void setProjectKey(String projectKey) {
		this.projectKey = projectKey;
	}

	public String getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(String creationDate) {
		this.creationDate = creationDate;
	}

	public String getExecutedByUserName() {
		return executedByUserName;
	}

	public void setExecutedByUserName(String executedByUserName) {
		this.executedByUserName = executedByUserName;
	}

	public String getIssueSummary() {
		return issueSummary;
	}

	public void setIssueSummary(String issueSummary) {
		this.issueSummary = issueSummary;
	}

	public List<String> getLabels() {
		return labels;
	}

	public void setLabels(List<String> labels) {
		this.labels = labels;
	}

	/**
	 * @return the issueId
	 */
	public String getIssueId() {
		return issueId;
	}

	/**
	 * @param issueId the issueId to set
	 */
	public void setIssueId(String issueId) {
		this.issueId = issueId;
	}

	/**
	 * @return the issueDescription
	 */
	public String getIssueDescription() {
		return issueDescription;
	}

	/**
	 * @param issueDescription the issueDescription to set
	 */
	public void setIssueDescription(String issueDescription) {
		this.issueDescription = issueDescription;
	}

	/**
	 * @return the comment
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * @param comment the comment to set
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}
	
	/**
	 * @return the htmlComment
	 */
	public String getHtmlComment() {
		return htmlComment;
	}

	/**
	 * @param htmlComment the htmlComment to set
	 */
	public void setHtmlComment(String htmlComment) {
		this.htmlComment = htmlComment;
	}

	/**
	 * @return the projectAvatarId
	 */
	public Long getProjectAvatarId() {
		return projectAvatarId;
	}

	/**
	 * @param projectAvatarId the projectAvatarId to set
	 */
	public void setProjectAvatarId(Long projectAvatarId) {
		this.projectAvatarId = projectAvatarId;
	}

	/**
	 * @return the projectId
	 */
	public Long getProjectId() {
		return projectId;
	}

	/**
	 * @param projectId the projectId to set
	 */
	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	/**
	 * @return the executionDefects
	 */
	public List<ExecutionDefectBean> getExecutionDefects() {
		return executionDefects;
	}

	/**
	 * @param executionDefects the executionDefects to set
	 */
	public void setExecutionDefects(List<ExecutionDefectBean> executionDefects) {
		this.executionDefects = executionDefects;
	}

	/**
	 * @return the stepDefects
	 */
	public List<String> getStepDefects() {
		return stepDefects;
	}

	/**
	 * @param stepDefects the stepDefects to set
	 */
	public void setStepDefects(List<String> stepDefects) {
		this.stepDefects = stepDefects;
	}

	/**
	 * @return the executionDefectCount
	 */
	public Integer getExecutionDefectCount() {
		return executionDefectCount;
	}

	/**
	 * @param executionDefectCount the executionDefectCount to set
	 */
	public void setExecutionDefectCount(Integer executionDefectCount) {
		this.executionDefectCount = executionDefectCount;
	}

	/**
	 * @return the stepDefectCount
	 */
	public Integer getStepDefectCount() {
		return stepDefectCount;
	}

	/**
	 * @param stepDefectCount the stepDefectCount to set
	 */
	public void setStepDefectCount(Integer stepDefectCount) {
		this.stepDefectCount = stepDefectCount;
	}

	/**
	 * @return the totalDefectCount
	 */
	public Integer getTotalDefectCount() {
		return totalDefectCount;
	}

	/**
	 * @param totalDefectCount the totalDefectCount to set
	 */
	public void setTotalDefectCount(Integer totalDefectCount) {
		this.totalDefectCount = totalDefectCount;
	}

	/**
	 * @return the orderId
	 */
	public Integer getOrderId() {
		return orderId;
	}

	/**
	 * @param orderId the orderId to set
	 */
	public void setOrderId(Integer orderId) {
		this.orderId = orderId;
	}

	/**
	 * @return the components
	 */
	public List<Map<String, Object>> getComponents() {
		return components;
	}

	/**
	 * @param components the components to set
	 */
	public void setComponents(List<Map<String, Object>> components) {
		this.components = components;
	}
	
	/**
	 * @return executedByDisplay
	 */
	public String getExecutedByDisplay() {
		return executedByDisplay;
	}
	
	/**
	 * @param executedByDisplay
	 */
	public void setExecutedByDisplay(String executedByDisplay) {
		this.executedByDisplay = executedByDisplay;
	}

	public String getAssignee() {
		return assignee;
	}

	public void setAssignee(String assignee) {
		this.assignee = assignee;
	}

	public String getAssigneeUserName() {
		return assigneeUserName;
	}

	public void setAssigneeUserName(String assigneeUserName) {
		this.assigneeUserName = assigneeUserName;
	}

	public String getAssigneeDisplay() {
		return assigneeDisplay;
	}

	public void setAssigneeDisplay(String assigneeDisplay) {
		this.assigneeDisplay = assigneeDisplay;
	}

	public boolean isCanViewIssue() {
		return canViewIssue;
	}

	public void setCanViewIssue(boolean canViewIssue) {
		this.canViewIssue = canViewIssue;
	}

	public List<String> getTestDefectsUnMasked() {
		return testDefectsUnMasked;
	}

	public void setTestDefectsUnMasked(List<String> testDefectsUnMasked) {
		this.testDefectsUnMasked = testDefectsUnMasked;
	}

	public List<String> getStepDefectsUnMasked() {
		return stepDefectsUnMasked;
	}

	public void setStepDefectsUnMasked(List<String> stepDefectsUnMasked) {
		this.stepDefectsUnMasked = stepDefectsUnMasked;
	}

    public Integer getStatusId() {
        return statusId;
    }

    public void setStatusId(Integer statusId) {
        this.statusId = statusId;
    }

	public Long getFolderId() {
		return folderId;
	}

	public void setFolderId(Long folderId) {
		this.folderId = folderId;
	}

	public String getFolderName() {
		return folderName;
	}

	public void setFolderName(String folderName) {
		this.folderName = folderName;
	}

	public  Map<String, String> getCustomFieldsValueMap() {
		return customFieldsValueMap;
	}

	public void setCustomFieldsValueMap(Map<String, String> customFieldsValueMap) {
		this.customFieldsValueMap = customFieldsValueMap;
	}

    public String getExecutionWorkflowStatus() {
        return executionWorkflowStatus;
    }

    public void setExecutionWorkflowStatus(String executionWorkflowStatus) {
        this.executionWorkflowStatus = executionWorkflowStatus;
    }

    public Long getEstimatedTime() {
        return estimatedTime;
    }

    public void setEstimatedTime(Long estimatedTime) {
        this.estimatedTime = estimatedTime;
    }

	public String getFormattedEstimatedTime() {
		return formattedEstimatedTime;
	}

	public void setFormattedEstimatedTime(String formattedEstimatedTime) {
		this.formattedEstimatedTime = formattedEstimatedTime;
	}

    public Long getLoggedTime() {
        return loggedTime;
    }

    public void setLoggedTime(Long loggedTime) {
        this.loggedTime = loggedTime;
    }

	public String getFormattedLoggedTime() {
		return formattedLoggedTime;
	}

	public void setFormattedLoggedTime(String formattedLoggedTime) {
		this.formattedLoggedTime = formattedLoggedTime;
	}

    @Override
    public String toString() {
        return "ZQLScheduleBean{" +
                "id=" + id +
                ", orderId=" + orderId +
                ", cycleId=" + cycleId +
                ", cycleName='" + cycleName + '\'' +
                ", issueId='" + issueId + '\'' +
                ", issueKey='" + issueKey + '\'' +
                ", issueSummary='" + issueSummary + '\'' +
                ", labels=" + labels +
                ", issueDescription='" + issueDescription + '\'' +
                ", projectKey='" + projectKey + '\'' +
                ", projectId=" + projectId +
                ", project='" + project + '\'' +
                ", projectAvatarId=" + projectAvatarId +
                ", priority='" + priority + '\'' +
                ", components=" + components +
                ", versionId=" + versionId +
                ", versionName='" + versionName + '\'' +
                ", status=" + status +
                ", statusId=" + statusId +
                ", executedOn='" + executedOn + '\'' +
                ", creationDate='" + creationDate + '\'' +
                ", comment='" + comment + '\'' +
                ", htmlComment='" + htmlComment + '\'' +
                ", executedBy='" + executedBy + '\'' +
                ", executedByUserName='" + executedByUserName + '\'' +
                ", testStepBean=" + testStepBean +
                ", executionDefects=" + executionDefects +
                ", stepDefects=" + stepDefects +
                ", testDefectsUnMasked=" + testDefectsUnMasked +
                ", stepDefectsUnMasked=" + stepDefectsUnMasked +
                ", executionDefectCount=" + executionDefectCount +
                ", stepDefectCount=" + stepDefectCount +
                ", totalDefectCount=" + totalDefectCount +
                ", executedByDisplay='" + executedByDisplay + '\'' +
                ", assignee='" + assignee + '\'' +
                ", assigneeUserName='" + assigneeUserName + '\'' +
                ", assigneeDisplay='" + assigneeDisplay + '\'' +
                ", canViewIssue=" + canViewIssue +
                ", folderId=" + folderId +
                ", folderName='" + folderName + '\'' +
                ", customFieldsValueMap=" + customFieldsValueMap +
                ", estimatedTime=" + estimatedTime +
				", formattedEstimatedTime=" + formattedEstimatedTime +
                ", loggedTime=" + loggedTime +
				", formattedLoggedTime=" + formattedLoggedTime +
				", executionWorkflowStatus='" + executionWorkflowStatus + '\'' +
                '}';
    }
}
