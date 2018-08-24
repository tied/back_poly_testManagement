package com.thed.zephyr.je.conditions;

import org.apache.log4j.Logger;

import com.atlassian.fugue.Option;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.permission.GlobalPermissionType;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractWebCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.thed.zephyr.je.permissions.model.PermissionType;
import com.thed.zephyr.util.JiraUtil;

/**
 * Condition that determines whether the Test Management is enabled for User is "Valid" .
 */
public class IsTestManagementEnabledForUserCondition  extends AbstractWebCondition {
    protected final Logger log = Logger.getLogger(IsTestManagementEnabledForUserCondition.class);

    public IsTestManagementEnabledForUserCondition(){
    }
    
    @Override
	public boolean shouldDisplay(ApplicationUser user, JiraHelper jiraHelper)
    {	
    	if(JiraUtil.getPermissionSchemeFlag()) {
    		try {
		    	Option<GlobalPermissionType> globalPermissionTypeOption =  ComponentAccessor.getGlobalPermissionManager().getGlobalPermission(PermissionType.ZEPHYR_TEST_MANAGEMENT_PERMISSION.toString());
				GlobalPermissionType globalPermissionType = globalPermissionTypeOption.getOrNull();
				if(globalPermissionType != null) {
					return ComponentAccessor.getGlobalPermissionManager().hasPermission(globalPermissionType.getGlobalPermissionKey(), user);
				}
    		} catch(Exception e) {
    			log.warn("Unable to find Zephyr Test Management Global Permission Type. Can't fail. Defaulting to true",e);
    			return true;
    		}
    	} 
    	return true;
    }
}
