package com.thed.zephyr.je.conditions;

import java.util.Collection;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractJiraCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.project.Project;

import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.user.ApplicationUser;

public class CanCreateJiraIssue extends AbstractJiraCondition{

	final PermissionManager permissionManager;
	
	public CanCreateJiraIssue (PermissionManager permissionManager){
		this.permissionManager = permissionManager;
	}
	
	@Override
	public boolean shouldDisplay(ApplicationUser user, JiraHelper helper) {
		Project project = helper.getProjectObject();
		
		if(project == null){
			Collection<Project> projectList = permissionManager.getProjects(ProjectPermissions.CREATE_ISSUES, user);
			if(projectList != null && !projectList.isEmpty()){
				project = (Project) projectList.toArray()[0];
			}
			else
				return false;
		}
	
		return permissionManager.hasPermission(ProjectPermissions.CREATE_ISSUES, project, user);
	}

}
