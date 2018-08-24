package com.thed.zephyr.je.operation;

import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.web.bean.BulkEditBeanImpl;

public class BulkAddTestBean extends BulkEditBeanImpl {
	private Integer projectId;

	private Integer versionId;

	private Integer folderId;

    public Integer getProjectId() {
		return projectId;
	}


	public void setProjectId(Integer projectId) {
		this.projectId = projectId;
	}


	public Integer getVersionId() {
		return versionId;
	}


	public void setVersionId(Integer versionId) {
		this.versionId = versionId;
	}


	public Integer getCycleId() {
		return cycleId;
	}


	public void setCycleId(Integer cycleId) {
		this.cycleId = cycleId;
	}


	private Integer cycleId;

    public Integer getFolderId() {
        return folderId;
    }

    public void setFolderId(Integer folderId) {
        this.folderId = folderId;
    }


	public BulkAddTestBean(IssueManager issueManager) {
		super(issueManager);
		// TODO Auto-generated constructor stub
	}

}
