package com.thed.zephyr.je.conditions;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractIssueWebCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.thed.zephyr.util.JiraUtil;

import java.util.Map;

/**
 * Condition that determines whether the issue type is "Test" .
 */
public class IsWorkFlowEnabledCondition  extends AbstractIssueWebCondition {
    @Override
	public boolean shouldDisplay(ApplicationUser user, Issue issue, JiraHelper jiraHelper){
        return JiraUtil.showWorkflow();
    }

    @Override
    public boolean shouldDisplay(ApplicationUser user, JiraHelper jiraHelper) {
        Map params = jiraHelper.getContextParams();
        Issue issue = (Issue)params.get("issue");
        if(issue == null) {
            return false;
        } else {
            return this.shouldDisplay(user, issue, jiraHelper);
        }
    }
}
