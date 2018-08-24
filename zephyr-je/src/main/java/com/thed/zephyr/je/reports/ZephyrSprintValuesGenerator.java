package com.thed.zephyr.je.reports;

import com.atlassian.configurable.ValuesGenerator;
import com.atlassian.jira.application.ApplicationAuthorizationService;
import com.atlassian.jira.application.ApplicationKeys;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.thed.zephyr.util.JiraUtil;

import org.apache.commons.collections.OrderedMap;
import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.log4j.Logger;
import org.ofbiz.core.entity.GenericValue;

import java.util.HashMap;
import java.util.Map;


public class ZephyrSprintValuesGenerator implements ValuesGenerator {
    private static final Logger log = Logger.getLogger(ZephyrSprintValuesGenerator.class);
    private final JiraAuthenticationContext authContext;
    private final ApplicationAuthorizationService applicationAuthorizationService;
    
    public ZephyrSprintValuesGenerator(JiraAuthenticationContext authContext, ApplicationAuthorizationService applicationAuthorizationService) {
    	this.authContext = authContext;
		this.applicationAuthorizationService = applicationAuthorizationService;
    }

    @Override
    public Map getValues(Map params) {
    	final ApplicationUser user = authContext.getLoggedInUser();
    	    	
        ComponentAccessor.getWebResourceManager().requireResource("com.thed.zephyr.je:zephyr-reports-resources");
        GenericValue projectGV = (GenericValue) params.get("project");
        OrderedMap sprints = ListOrderedMap.decorate(new HashMap(1));
        sprints.put(projectGV.getLong("id"), String.valueOf(applicationAuthorizationService.canUseApplication(user, ApplicationKeys.SOFTWARE)));
        return sprints;
    }
}
