package com.thed.zephyr.je.vo;

import com.opensymphony.util.TextUtils;
import com.thed.zephyr.je.config.model.ExecutionStatus;
import com.thed.zephyr.je.model.StepResult;
import com.thed.zephyr.je.rest.CustomFieldValueResource;
import com.thed.zephyr.util.JiraUtil;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Map;


@XmlRootElement(name = "StepResult")
public class

StepResultBean {
	
	@XmlElement
	private Integer id; 
	@XmlElement
	private Long executedOn;
	@XmlElement
	private String status;
	@XmlElement
	private String comment;
	/*Temporary property to hold html version of comment. This way we can entertain links etc and not have to use soy escape.*/
	@XmlElement
	private String htmlComment;
	@XmlElement
	private String executedBy;
	@XmlElement
	private Integer executionId;
	@XmlElement
	private Integer stepId;
	@XmlElement
	private List<String> defectList;
	@XmlElement
	private List<ExecutionStatus> executionStatus;
	@XmlElement
	private Integer issueId;
	//readableDefectList
	@XmlElement
	private List<Map<String, String>> defects;
	@XmlElement
	private String updateDefectList;
	@XmlElement
	public String createdBy;
	@XmlElement
	public String modifiedBy;

	private Integer projectId;

    @XmlElement
	private Integer executionDefectCount;

    @XmlElement
	private Integer stepDefectCount;

    @XmlElement
	private Integer totalDefectCount;

	@XmlElement
	private Integer stepResultAttachmentCount;

	@XmlElement
	private Integer stepResultsCount;

    @XmlElement
    public Integer testStepId;

    @XmlElement
    public Integer orderId;

    @XmlElement
    public String step;

    @XmlElement
    public String data;

    @XmlElement
    public String result;

    @XmlElement
    public String htmlStep;

    @XmlElement
    public String htmlData;

    @XmlElement
    public String htmlResult;

    @XmlElement
    List<Map<String, String>> attachmentsMap;

    @XmlElement
    Map<String, CustomFieldValueResource.CustomFieldValueResponse> customFieldValues;
	
	public StepResultBean(){
	}

	public StepResultBean(Integer id, String status, Integer executionId, Integer stepId, String executedBy, Long executedOn, String comment){
		this.id = id;
		this.executedOn = executedOn;
		this.status = status;
		this.comment = comment;
		this.htmlComment = TextUtils.plainTextToHtml(this.comment, "_blank", true);
		this.executedBy = executedBy;
		this.executionId = executionId;
		this.stepId = stepId;
	}
	
	public StepResultBean(StepResult sResult){
		this.id = sResult.getID();
		this.executedOn = sResult.getExecutedOn();
		this.status = sResult.getStatus();
		this.comment = sResult.getComment();
		this.htmlComment = TextUtils.plainTextToHtml(this.comment, "_blank", true);
		this.executedBy = sResult.getExecutedBy();
		this.executionId = sResult.getScheduleId();
		this.stepId = sResult.getStep().getID();
		this.createdBy=sResult.getCreatedBy();
		this.modifiedBy = sResult.getModifiedBy();
	}
	
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Long getExecutedOn(){
		return executedOn;
	}
	
    public void setExecutedOn(Long executedOn){
    	this.executedOn = executedOn;
    }

    public String getStatus(){
    	return status;
    }
    
    public void setStatus(String status){
    	this.status = status;
    }

	public String getComment(){
		return comment;
	}
	
	public void setComment(String comment){
		this.comment = comment;
		this.htmlComment = TextUtils.plainTextToHtml(this.comment, "_blank", true);
	}
	
	public String getHtmlComment() {
		return htmlComment;
	}

	public String getTruncatedComment(){
		if(comment!=null && comment.length() > 12){
			return comment.substring(0,12) + "...";
		}
		
		return comment;
	}
	public String getExecutedBy(){
		return executedBy;
	}
	public void setExecutedBy(String executedBy){
		this.executedBy = executedBy;
	}

	public Integer getExecutionId(){
		return executionId;
	}
	public void setExecutionId(Integer executionId){
		this.executionId = executionId;
	}

	public Integer getStepId(){
		return stepId;
	}
	
	public void setStepId(Integer stepId){
		this.stepId = stepId;
	}

	public Integer getIssueId(){
		return issueId;
	}
	
	public void setIssueId(Integer issueId){
		this.issueId = issueId;
	}

	public Integer getProjectId(){
		return projectId;
	}
	
	public void setProjectId(Integer projectId){
		this.projectId = projectId;
	}
	
	public List<Map<String, String>> getDefects(){
		return defects;
	}

	/*	public void setDefects(List<StepDefect> defectList, IssueManager issueManager){
		StringBuilder defectBuffer = new StringBuilder();
    	for(StepDefect defect : defectList){
    		MutableIssue issue = issueManager.getIssueObject(new Long(defect.getDefectId()));
    		if(issue != null) {
    			defectBuffer.append(issue.getKey() + ", ");
    		} 
    	}
    	
    	if (defectBuffer.length() > 0){
    		this.defects = defectBuffer.substring(0, defectBuffer.length() - 2); 
    	}
    	else
    		this.defects = "";
	}*/
	public void setDefects(List<Map<String, String>> stepResultDefectList){		
    	this.defects = stepResultDefectList;
	}
	
	public List<String> getDefectList(){
		return defectList;
	}
	
	public void setDefectList(List<String> defectList){
		this.defectList = defectList;
	}

	public String getUpdateDefectList() {
		return updateDefectList;
	}

	public void setUpdateDefectList(String updateDefectList) {
		this.updateDefectList = updateDefectList;
	}
	
	public void setExecutionStatus(List<ExecutionStatus> executionStatus) {
		this.executionStatus = executionStatus;
	}

    public List<ExecutionStatus> getExecutionStatus() {
    	return executionStatus;
    }

    public ExecutionStatus getStepExecutionStatus() {
    	return JiraUtil.getStepExecutionStatuses().get(Integer.valueOf(getStatus()));
    }

    public Integer getExecutionDefectCount() {
        return executionDefectCount;
    }

    public void setExecutionDefectCount(Integer executionDefectCount) {
        this.executionDefectCount = executionDefectCount;
    }

    public Integer getStepDefectCount() {
        return stepDefectCount;
    }

    public void setStepDefectCount(Integer stepDefectCount) {
        this.stepDefectCount = stepDefectCount;
    }

    public Integer getTotalDefectCount() {
        return totalDefectCount;
    }

    public void setTotalDefectCount(Integer totalDefectCount) {
        this.totalDefectCount = totalDefectCount;
    }

    public Integer getStepResultAttachmentCount() {
        return stepResultAttachmentCount;
    }

    public void setStepResultAttachmentCount(Integer stepResultAttachmentCount) {
        this.stepResultAttachmentCount = stepResultAttachmentCount;
    }

	public Integer getStepResultsCount() {
		return stepResultsCount;
	}

	public void setStepResultsCount(Integer stepResultsCount) {
		this.stepResultsCount = stepResultsCount;
	}

    public Integer getTestStepId() {
        return testStepId;
    }

    public void setTestStepId(Integer testStepId) {
        this.testStepId = testStepId;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getHtmlStep() {
        return htmlStep;
    }

    public void setHtmlStep(String htmlStep) {
        this.htmlStep = htmlStep;
    }

    public String getHtmlData() {
        return htmlData;
    }

    public void setHtmlData(String htmlData) {
        this.htmlData = htmlData;
    }

    public String getHtmlResult() {
        return htmlResult;
    }

    public void setHtmlResult(String htmlResult) {
        this.htmlResult = htmlResult;
    }

    public List<Map<String, String>> getAttachmentsMap() {
        return attachmentsMap;
    }

    public void setAttachmentsMap(List<Map<String, String>> attachmentsMap) {
        this.attachmentsMap = attachmentsMap;
    }

    public Map<String, CustomFieldValueResource.CustomFieldValueResponse> getCustomFieldValues() {
        return customFieldValues;
    }

    public void setCustomFieldValues(Map<String, CustomFieldValueResource.CustomFieldValueResponse> customFieldValues) {
        this.customFieldValues = customFieldValues;
    }

    public String toString(){
		return " StepResult ID: " + id
				+ " Step ID: " + stepId
				+ " Execution ID: "+ executionId
				+ " Status: "+ status
				+ " Comment: " + comment
				+ " Readable Defect List: " + defects
				+ " Associated Issue Id: " + issueId;
	}
}
