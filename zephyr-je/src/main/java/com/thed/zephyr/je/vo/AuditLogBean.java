package com.thed.zephyr.je.vo;

import javax.xml.bind.annotation.XmlElement;

public class AuditLogBean {
	@XmlElement
	private String entityId; //SCHEDULE
	
	@XmlElement
	private String entityType; //SCHEDULE

	@XmlElement
	private String entityEvent;//UPDATED,DELETED
	
	@XmlElement
	private AuditItemBean auditItems;
	
	@XmlElement
	private String creationDate;
	
	@XmlElement 
	private String issueKey;
	
	@XmlElement
	private String executionId;

	@XmlElement
	private String creator;

    @XmlElement
    private String creatorKey;
	
	@XmlElement 
	private int totalItems;

    @XmlElement
    private boolean creatorActive;

    @XmlElement
    private boolean creatorExists;

    @XmlElement
    private String avatarUrl;

    @XmlElement
    private Boolean isDefaultAvatar;


	public AuditLogBean(){
	}
	

	
	public AuditItemBean getAuditItems() {
		return auditItems;
	}

	public void setAuditItems(AuditItemBean auditItems) {
		this.auditItems = auditItems;
	}

	public String getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(String creationDate) {
		this.creationDate = creationDate;
	}

	public String getEntityType() {
		return entityType;
	}
	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}
	
	public String getEntityEvent() {
		return entityEvent;
	}
	
	public void setEntityEvent(String entityEvent) {
		this.entityEvent = entityEvent;
	}

	
	public String getIssueKey() {
		return issueKey;
	}

	public void setIssueKey(String issueKey) {
		this.issueKey = issueKey;
	}

	public String getExecutionId() {
		return executionId;
	}

	public void setExecutionId(String executionId) {
		this.executionId = executionId;
	}
	
	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}

	public String getEntityId() {
		return entityId;
	}

	public int getTotalItems() {
		return totalItems;
	}

	public void setTotalItems(int totalItems) {
		this.totalItems = totalItems;
	}

    public boolean isCreatorExists() {
        return creatorExists;
    }

    public void setCreatorExists(boolean creatorExists) {
        this.creatorExists = creatorExists;
    }

    public boolean isCreatorActive() {
        return creatorActive;
    }

    public void setCreatorActive(boolean creatorActive) {
        this.creatorActive = creatorActive;
    }

    public String getCreatorKey() {
        return creatorKey;
    }

    public void setCreatorKey(String creatorKey) {
        this.creatorKey = creatorKey;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Boolean getDefaultAvatar() {
        return isDefaultAvatar;
    }

    public void setDefaultAvatar(Boolean defaultAvatar) {
        isDefaultAvatar = defaultAvatar;
    }
}
