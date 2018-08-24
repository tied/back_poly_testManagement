package com.thed.zephyr.je.conditions;

import java.util.function.Predicate;

import org.apache.log4j.Logger;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractWebCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.portal.PortletConfigurationManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.preferences.UserPreferencesManager;
import com.thed.zephyr.je.config.ZephyrJEDefaultConfiguration;
import com.thed.zephyr.util.JiraUtil;

public class IsTestManagementEnabledForProjectCondition extends AbstractWebCondition {
	protected final Logger log = Logger.getLogger(this.getClass().getName());
	private final ProjectManager projectManager;

	public IsTestManagementEnabledForProjectCondition(ProjectManager projectManager) {
		super();
		this.projectManager = projectManager;
	}

	@Override
	public boolean shouldDisplay(ApplicationUser user, JiraHelper helper) {
		if (user != null && !ZephyrJEDefaultConfiguration.initialized) {
			initializeZephyr();
		}
		Project project = helper.getProject();
		if(project == null){
			String projectName = getProjectName(helper.getRequest().getRequestURL());
			project = projectManager.getProjects().parallelStream().filter(new Predicate<Project>() {
				@Override
				public boolean test(Project t) {
					return t.getOriginalKey().equals(projectName);
				}
			}).findFirst().orElse(null);
		}
		if (project != null) {
			log.info("Project found is :" + project);
			return !JiraUtil.isTestMenuDisabled(String.valueOf(project.getId()));
		}
		return true;
	}
	/**
	 * Find the project key using the contextPath
	 * @param contextPath
	 * @return
	 */
	private String getProjectName(StringBuffer contextPath) {
		int index = contextPath.toString().indexOf("lastVisited");
		if(index > 0){
			String path = contextPath.substring(0,index-1);
			return path.substring(0,index-1).substring(path.lastIndexOf("/") + 1);
		}else {
			return contextPath.substring(contextPath.lastIndexOf("/") + 1).trim();
		}
	}

	/**
	 * 
	 */
	private void initializeZephyr() {
		log.info("Starting ZFJ Initialization");
		synchronized (ZephyrJEDefaultConfiguration.initialized) {
			PortletConfigurationManager pConfigurationManager = ComponentAccessor
					.getComponentOfType(PortletConfigurationManager.class);
			UserPreferencesManager userPreferencesManager = ComponentAccessor.getUserPreferencesManager();
			final ZephyrJEDefaultConfiguration zephyrConfiguration = new ZephyrJEDefaultConfiguration(
					pConfigurationManager, userPreferencesManager);
			zephyrConfiguration.postInit();
			zephyrConfiguration.getZephyrIssueTypeId();
		}
		log.info("ZFJ Initialization is Done");
	}
}
