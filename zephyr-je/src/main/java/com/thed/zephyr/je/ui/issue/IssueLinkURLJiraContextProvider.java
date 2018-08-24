package com.thed.zephyr.je.ui.issue;

import java.util.Map;

import com.atlassian.core.util.map.EasyMap;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.plugin.webfragment.contextproviders.AbstractJiraContextProvider;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.thed.zephyr.util.JiraUtil;

public class IssueLinkURLJiraContextProvider extends AbstractJiraContextProvider
{
    private final ApplicationProperties applicationProperties;
 
    public IssueLinkURLJiraContextProvider(ApplicationProperties applicationProperties){
        this.applicationProperties = applicationProperties;
    }
 
    public Map getContextMap(ApplicationUser user, JiraHelper jiraHelper)
    {
    	if(JiraUtil.isJIRA50())
    		return EasyMap.build("linkURL", "/secure/LinkExistingIssue!default.jspa?id=" + jiraHelper.getContextParams().get("issueId"));
    	else
    		return EasyMap.build("linkURL", "/secure/LinkJiraIssue!default.jspa?id=" + jiraHelper.getContextParams().get("issueId"));
    }
}