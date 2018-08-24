package com.thed.zephyr.je.conditions;

import java.util.List;

import com.atlassian.jira.user.ApplicationUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractJiraCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.portal.PortletConfigurationManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.user.preferences.UserPreferencesManager;
import com.thed.zephyr.je.config.ZephyrJEDefaultConfiguration;

public class IsProjectPresentAndUserLoggedInCondition extends AbstractJiraCondition {
	private final ProjectManager projectManager;
	private static final Logger log = LoggerFactory.getLogger(IsProjectPresentAndUserLoggedInCondition.class);
	
    public IsProjectPresentAndUserLoggedInCondition(ProjectManager projectManager)
    {
        this.projectManager = projectManager;
    }
    
	@Override
	public boolean shouldDisplay(ApplicationUser user, JiraHelper jiraHelper) {
		if(user != null && !ZephyrJEDefaultConfiguration.initialized){
			initializeZephyr();
		}
		
        List<Project> projectObjects = projectManager.getProjectObjects();
        Boolean shouldDisplay = (projectObjects != null && projectObjects.size() > 0 && user != null);
		return shouldDisplay;
	}

	/**
	 * 
	 */
	private void initializeZephyr() {
		log.info("Starting ZFJ Initialization");
		synchronized (ZephyrJEDefaultConfiguration.initialized){
			PortletConfigurationManager pConfigurationManager = ComponentAccessor.getComponentOfType(PortletConfigurationManager.class);
			UserPreferencesManager userPreferencesManager = ComponentAccessor.getUserPreferencesManager();
			final ZephyrJEDefaultConfiguration zephyrConfiguration = new ZephyrJEDefaultConfiguration(pConfigurationManager, userPreferencesManager );
			zephyrConfiguration.postInit();
		}
		log.info("ZFJ Initialization is Done");
	}
}