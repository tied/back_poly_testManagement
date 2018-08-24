package com.thed.zephyr.je.vo;

public class ImportJobStatus {
	private String status;
	private String fileName;
	private String issuesCount;
	private String issuesCountForSteps;
	private String errorMsg;
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getIssuesCount() {
		return issuesCount;
	}
	public void setIssuesCount(String issuesCount) {
		this.issuesCount = issuesCount;
	}
	public String getErrorMsg() {
		return errorMsg;
	}
	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}
	public String getIssuesCountForSteps() {
		return issuesCountForSteps;
	}
	public void setIssuesCountForSteps(String issuesCountForSteps) {
		this.issuesCountForSteps = issuesCountForSteps;
	}
	
}
