package com.thed.zephyr.je.conditions;

import com.atlassian.jira.plugin.webfragment.conditions.AbstractWebCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.thed.zephyr.util.JiraUtil;

/**
 * Condition that determines whether the license is "Valid" .
 */
public class IsTestManagementPermissionEnabledCondition  extends AbstractWebCondition {

    public IsTestManagementPermissionEnabledCondition(){
    }
    
    @Override
	public boolean shouldDisplay(ApplicationUser user, JiraHelper jiraHelper)
    {	
    	return JiraUtil.isTestManagementForProjects();
    }
}
