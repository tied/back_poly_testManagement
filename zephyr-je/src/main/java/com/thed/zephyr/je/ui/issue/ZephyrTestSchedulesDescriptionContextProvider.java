package com.thed.zephyr.je.ui.issue;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import com.atlassian.jira.user.ApplicationUser;
import org.apache.log4j.Logger;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.webfragment.CacheableContextProvider;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.collect.MapBuilder;
import com.atlassian.plugin.PluginParseException;
import com.thed.zephyr.je.config.license.ZephyrLicenseManager;
import com.thed.zephyr.je.config.license.ZephyrLicenseVerificationResult;
import com.thed.zephyr.util.JiraUtil;

/**
 * Context Provider for the Schedules section on View Issue page.
 *
 */
public class ZephyrTestSchedulesDescriptionContextProvider implements CacheableContextProvider
{
    protected final Logger log = Logger.getLogger(ZephyrTestSchedulesDescriptionContextProvider.class);

    private final JiraAuthenticationContext authenticationContext;
    private final ZephyrLicenseManager zLicenseManager;
    
    public ZephyrTestSchedulesDescriptionContextProvider(JiraAuthenticationContext authenticationContext,
    		final ZephyrLicenseManager zLicenseManager) {
        this.authenticationContext = authenticationContext;
        this.zLicenseManager=checkNotNull(zLicenseManager);
    }

    @Override
    public void init(Map<String, String> params) throws PluginParseException {
    }
    
    @Override
    public Map<String, Object> getContextMap(Map<String, Object> context) {
        final MapBuilder<String, Object> paramsBuilder = MapBuilder.newBuilder(context);
	    //Do License Validation.
        ZephyrLicenseVerificationResult licVerificationResult = JiraUtil.performLicenseValidation(zLicenseManager);
	    
	    //license is invalid
	    if( !licVerificationResult.isValid()) {
        	paramsBuilder.add("errors", licVerificationResult.getGeneralMessage());
		}
        return paramsBuilder.toMap();
    }

    @Override
    public String getUniqueContextKey(Map<String, Object> context)
    {
        final Issue issue = (Issue) context.get("issue");
        final ApplicationUser user = authenticationContext.getLoggedInUser();

        return issue.getId() + "/" + (user == null ? "" : user.getName());

    }
}