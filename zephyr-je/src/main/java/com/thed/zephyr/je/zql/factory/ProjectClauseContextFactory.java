package com.thed.zephyr.je.zql.factory;

import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.resolver.ProjectResolver;
import com.atlassian.jira.security.PermissionManager;

public class ProjectClauseContextFactory extends com.atlassian.jira.jql.context.ProjectClauseContextFactory {
	private static ProjectResolver projectResolver = ComponentManager.getComponentInstanceOfType(ProjectResolver.class);
	
	public ProjectClauseContextFactory(JqlOperandResolver jqlOperandResolver,
			PermissionManager permissionManager) {
		super(jqlOperandResolver, projectResolver, permissionManager);
	}
}
