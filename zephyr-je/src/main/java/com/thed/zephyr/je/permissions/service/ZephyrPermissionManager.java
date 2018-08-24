package com.thed.zephyr.je.permissions.service;

import java.util.Collection;

import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.user.ApplicationUser;

public interface ZephyrPermissionManager {
	boolean validateUserPermission(ProjectPermissionKey projectPermissionKey, Project project, ApplicationUser user, Long projectId);
	
	boolean validateUserPermissions(
			Collection<ProjectPermissionKey> projectPermissionKeys, Project project, ApplicationUser loggedInUser, Long projectId);
}
