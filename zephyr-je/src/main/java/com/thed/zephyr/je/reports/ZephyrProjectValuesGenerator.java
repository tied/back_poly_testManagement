package com.thed.zephyr.je.reports;

import com.atlassian.configurable.ValuesGenerator;
import com.atlassian.jira.application.ApplicationAuthorizationService;
import com.atlassian.jira.application.ApplicationKeys;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.ApplicationUser;
import com.thed.zephyr.util.JiraUtil;
import org.apache.commons.collections.OrderedMap;
import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.log4j.Logger;
import org.ofbiz.core.entity.GenericValue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mukul on 1/14/15.
 */
public class ZephyrProjectValuesGenerator implements ValuesGenerator {
    private static final Logger log = Logger.getLogger(ZephyrProjectValuesGenerator.class);
    private final ApplicationAuthorizationService applicationAuthorizationService;

    public ZephyrProjectValuesGenerator(ApplicationAuthorizationService applicationAuthorizationService) {
    	this.applicationAuthorizationService = applicationAuthorizationService;
    }

    @Override
    public Map getValues(Map params) {
        GenericValue projectGV = (GenericValue) params.get("project");
        ApplicationUser remoteUser = (ApplicationUser) params.get("User");
        Project projectObj = ComponentAccessor.getProjectManager().getProjectObj(projectGV.getLong("id"));

        if (projectObj == null) {
            // they haven't selected a project, if there is only one - select it for them
            final Collection<Project> projects = ComponentAccessor.getPermissionManager().getProjects(ProjectPermissions.BROWSE_PROJECTS, remoteUser);
            //Project is Null only if User does not have permission, Redirect them to Permission page
            if (projects == null || projects.size() == 0) {
                log.error("getValues() : No project found.");
                return null;
            }

            if (projects.size() > 0) {
                projectObj = (Project) projects.toArray()[0];
            }
        }

        if (!JiraUtil.hasAnonymousPermission(remoteUser, projectObj)) {
            log.error("getValues() : User has no permissions to browse the projects.");
            return null;
        }

        OrderedMap projectMap = ListOrderedMap.decorate(new HashMap(2));
        projectMap.put(projectObj.getId(), "- " + projectObj.getName());
        projectMap.put("hasAccessToSoftware", String.valueOf(applicationAuthorizationService.canUseApplication(remoteUser, ApplicationKeys.SOFTWARE)));
        return projectMap;
    }
}