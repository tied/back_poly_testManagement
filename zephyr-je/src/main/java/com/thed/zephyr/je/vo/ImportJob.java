package com.thed.zephyr.je.vo;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ImportJob {

	private String projectId;

	private String issueType;

	private ImportDetails importDetails;

	private Set<ImportJobHistory> history;

	private String sheetFilterPattern;

	private boolean attachFile = false;
	
	private String linkTypeId;

	private String linkingIssueKey;

	private String fileType;
	private boolean importingTestStepsOnly = false;
	//private String issueKey;
	private String jobProgressKey;

	private Map<String, ImportFieldConfig> fieldConfigMap;

	/**
	 * @param importDetails
	 * @param fieldConfigMap
	 * @param scheduledDate
	 * @param history
	 */

	public ImportJob(ImportDetails importDetails,
			Map<String, ImportFieldConfig> fieldConfigMap,
			Set<ImportJobHistory> history) {
		super();

		this.importDetails = importDetails;
		this.fieldConfigMap = fieldConfigMap;
		this.history = history;
	}

	public ImportJob() {
	}

	public ImportDetails getImportDetails() {
		return importDetails;
	}

	public void setImportDetails(ImportDetails importDetails) {
		this.importDetails = importDetails;
	}

	public Set<ImportJobHistory> getHistory() {
		return history;
	}

	public void setHistory(Set<ImportJobHistory> history) {
		this.history = history;
	}

	public void addHistory(ImportJobHistory history) {
		if(this.history == null) {
			this.history = new HashSet<ImportJobHistory>();
		}
		this.history.add(history);
	}

	public String getSheetFilterPattern() {
		return sheetFilterPattern;
	}

	public void setSheetFilterPattern(String sheetFilterPattern) {
		this.sheetFilterPattern = sheetFilterPattern;
	}

	public boolean isAttachFile() {
		return attachFile;
	}

	public void setAttachFile(boolean attachFile) {
		this.attachFile = attachFile;
	}

	public Map<String, ImportFieldConfig> getFieldConfigMap() {
		return fieldConfigMap;
	}

	public void setFieldConfigMap(Map<String, ImportFieldConfig> fieldConfigMap) {
		this.fieldConfigMap = fieldConfigMap;
	}

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public String getIssueType() {
		return issueType;
	}

	public void setIssueType(String issueType) {
		this.issueType = issueType;
	}

	public String getLinkingIssueKey() {
		return linkingIssueKey;
	}

	public void setLinkingIssueKey(String linkingIssueKey) {
		this.linkingIssueKey = linkingIssueKey;
	}

	public String getFileType() {
		return fileType;
	}

	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	public boolean isFileTypeExcel(){
		return fileType != null && ( "excel".equalsIgnoreCase(fileType) );
	}
	public boolean isFileTypeXml(){
		return fileType != null && "xml".equalsIgnoreCase(fileType);
	}

	public String getLinkTypeId() {
		return linkTypeId;
	}

	public void setLinkTypeId(String linkTypeId) {
		this.linkTypeId = linkTypeId;
	}

	public boolean isImportingTestStepsOnly() {
		return importingTestStepsOnly;
	}

	public void setImportingTestStepsOnly(boolean importingTestStepsOnly) {
		this.importingTestStepsOnly = importingTestStepsOnly;
	}

	public String getJobProgressKey() {
		return jobProgressKey;
	}

	public void setJobProgressKey(String jobProgressKey) {
		this.jobProgressKey = jobProgressKey;
	}
	
}
