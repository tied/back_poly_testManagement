package com.thed.zephyr.je.conditions;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractIssueWebCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.thed.zephyr.util.JiraUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * Created by dubey on 26-05-2017.
 */
public class IsShowZephyrTestToNonIssueTypeCondition extends AbstractIssueWebCondition {

    final Logger log = Logger.getLogger(IsShowZephyrTestToNonIssueTypeCondition.class);

    @Override
    public boolean shouldDisplay(ApplicationUser applicationUser, Issue issue, JiraHelper jiraHelper) {
        String nonIssueTypesList = JiraUtil.getNonIssueTypeTestSaveList();
        if(StringUtils.isNotBlank(nonIssueTypesList)) {
            log.debug("nonIssueTypesList condition validation:"+nonIssueTypesList);
            return applicationUser != null && StringUtils.contains(nonIssueTypesList,issue.getIssueType().getId());
        }
        return true;
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
