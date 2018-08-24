package com.thed.zephyr.je.conditions;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractIssueWebCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.thed.zephyr.util.JiraUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * Condition that determines whether the issue type is "Test" .
 */
public class IsIssueTypeTestCondition  extends AbstractIssueWebCondition {
	protected static final Logger log = Logger.getLogger(IsIssueTypeTestCondition.class);

    @Override
	public boolean shouldDisplay(ApplicationUser user, Issue issue, JiraHelper jiraHelper)
    {
        String typeId = JiraUtil.getTestcaseIssueTypeId();
        final boolean hasPermission = ComponentAccessor.getPermissionManager().hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue, user);

        if(hasPermission && StringUtils.equalsIgnoreCase(issue.getIssueType().getId(),typeId)) {
        	return true;
        }
        return user != null && StringUtils.equalsIgnoreCase(issue.getIssueType().getId(),typeId);
    }

    @Override
    public boolean shouldDisplay(ApplicationUser user, JiraHelper jiraHelper) {
        Map params = jiraHelper.getContextParams();
        Issue issue = (Issue) params.get("issue");
        if(issue == null) {
            return false;
        } else {
            return this.shouldDisplay(user, issue, jiraHelper);
        }
    }
}
