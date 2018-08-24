package com.thed.zephyr.je.event;

import java.util.Collection;
import java.util.Map;

import com.atlassian.event.api.AsynchronousPreferred;
import com.google.common.collect.Table;
import com.thed.zephyr.je.model.Folder;

/**
 * Event object holds the folder modified changes.
 * 
 * @author manjunath
 *
 */
@AsynchronousPreferred
public class FolderModifyEvent extends ZephyrEvent {

	private Folder folder;
	private String userName;
	private Long cycleId;
	private Long projectId;
	private Collection<Folder> folders;
	private Table<String, String, Object> changePropertyTable;
	
    public FolderModifyEvent(Collection<Folder> folders, Map<String, Object> params, EventType eventType, String userName, Long cycleId, Long projectId) {
        super(params, eventType);
        this.folders = folders;
        this.userName = userName;
        this.cycleId = cycleId;
        this.projectId = projectId;
    }
	
	public FolderModifyEvent(Folder folder, Table<String, String, Object> changePropertyTable, EventType eventType, String userName, Long cycleId, Long projectId) {
        super(null, eventType);
        this.folder = folder;
        this.changePropertyTable = changePropertyTable;
        this.userName = userName;
        this.cycleId = cycleId;
        this.projectId = projectId;
    }

    public int hashCode(){
        int result = super.hashCode();
        result = 29 * result + (folder != null ? folder.hashCode() : 0);
        result = 29 * result + (eventType != null ? eventType.hashCode() : 0);
        result = 29 * result + (changePropertyTable != null ? changePropertyTable.hashCode() : 0);
        result = 29 * result + (userName != null ? userName.hashCode() : 0);
        result = 29 * result + (folders != null ? folders.hashCode() : 0);
        return result;
    }

	public Folder getFolder() {
		return folder;
	}

	public void setFolder(Folder folder) {
		this.folder = folder;
	}

	public Table<String, String, Object> getChangePropertyTable() {
		return changePropertyTable;
	}

	public void setChangePropertyTable(
			Table<String, String, Object> changePropertyTable) {
		this.changePropertyTable = changePropertyTable;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public Collection<Folder> getFolders() {
		return folders;
	}

	public void setFolders(Collection<Folder> folders) {
		this.folders = folders;
	}

	public Long getCycleId() {
		return cycleId;
	}

	public void setCycleId(Long cycleId) {
		this.cycleId = cycleId;
	}

	public Long getProjectId() {
		return projectId;
	}

	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}
	
}

