package com.thed.zephyr.je.conditions;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractJiraCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.thed.zephyr.je.config.license.ZephyrLicenseManager;
import com.thed.zephyr.je.config.license.ZephyrLicenseVerificationResult;
import com.thed.zephyr.util.JiraUtil;

/**
 * Condition that determines whether the license is "Valid" .
 */
public class IsLicenseValidCondition  extends AbstractJiraCondition {
    private final ZephyrLicenseManager zLicenseManager;
	
    public IsLicenseValidCondition(ZephyrLicenseManager zLicenseManager)
    {
        this.zLicenseManager = zLicenseManager;
    }
    
    @Override
	public boolean shouldDisplay(ApplicationUser user, JiraHelper jiraHelper)
    {	
    	ZephyrLicenseVerificationResult licenseVerificationResult = JiraUtil.performLicenseValidation(zLicenseManager);
		return licenseVerificationResult.isValid();
    }
}
