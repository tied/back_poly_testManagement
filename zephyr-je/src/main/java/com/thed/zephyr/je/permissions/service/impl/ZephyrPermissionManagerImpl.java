package com.thed.zephyr.je.permissions.service.impl;

import java.util.Collection;

import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.user.ApplicationUser;
import com.thed.zephyr.je.permissions.service.ZephyrPermissionManager;
import com.thed.zephyr.util.JiraUtil;

public class ZephyrPermissionManagerImpl implements ZephyrPermissionManager {

	private final PermissionManager permissionManager;
	private final ProjectManager projectManager;
	
	public ZephyrPermissionManagerImpl(final PermissionManager permissionManager,final ProjectManager projectManager) {
		this.permissionManager=permissionManager;
		this.projectManager=projectManager;
	}
	
	@Override
	public boolean validateUserPermission(
			ProjectPermissionKey projectPermissionKey, Project project, ApplicationUser loggedInUser, Long projectId) {
		Boolean isZephyrPermissionEnabled = JiraUtil.getPermissionSchemeFlag();
		if(isZephyrPermissionEnabled && (project != null || projectId != null)) {
			if(project == null) {
				project = projectManager.getProjectObj(projectId);
			}
	        if(permissionManager.hasPermission(projectPermissionKey, project, loggedInUser)) {
	        	return true;
	        }
	        return false;
		}
		return true;
	}
	
	
	@Override
	public boolean validateUserPermissions(
			Collection<ProjectPermissionKey> projectPermissionKeys, Project project, ApplicationUser loggedInUser, Long projectId) {
		Boolean isZephyrPermissionEnabled = JiraUtil.getPermissionSchemeFlag();
		if(isZephyrPermissionEnabled && (project != null || projectId != null)) {
			if(project == null) {
				project = projectManager.getProjectObj(projectId);
			}
			boolean hasAllPermissions = true;
			for(ProjectPermissionKey projectPermissionKey : projectPermissionKeys) {
		        if(!permissionManager.hasPermission(projectPermissionKey, project, loggedInUser)) {
		        	hasAllPermissions = false;
		        }
			}
			return hasAllPermissions;
		}
		return true;
	}
}
