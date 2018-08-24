package com.thed.zephyr.je.ui.issue;

import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.webfragment.CacheableContextProvider;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.collect.MapBuilder;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.thed.zephyr.je.config.license.ZephyrLicenseManager;
import com.thed.zephyr.je.config.license.ZephyrLicenseVerificationResult;
import com.thed.zephyr.je.model.Teststep;
import com.thed.zephyr.je.service.TeststepManager;
import com.thed.zephyr.util.JiraUtil;
import org.apache.log4j.Logger;
import webwork.action.ActionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Context Provider for the Test block section on View Issue page.
 *
 */
public class ZephyrTestDescriptionBlockContextProvider implements CacheableContextProvider
{
    protected final Logger log = Logger.getLogger(ZephyrTestDescriptionBlockContextProvider.class);

    private final JiraAuthenticationContext authenticationContext;
    private final DateTimeFormatter dateTimeFormatter;
    private TeststepManager teststepManager;
    private final ZephyrLicenseManager zLicenseManager;

	private final WebResourceManager webResourceManager;

    public ZephyrTestDescriptionBlockContextProvider(JiraAuthenticationContext authenticationContext,
    		DateTimeFormatterFactory dateTimeFormatterFactory, WebResourceManager webResourceManager,
    		TeststepManager teststepManager, final ZephyrLicenseManager zLicenseManager) {
        this.authenticationContext = authenticationContext;
        // prepare the needed formatter
        dateTimeFormatter = dateTimeFormatterFactory.formatter().forLoggedInUser();
        this.webResourceManager = webResourceManager;
        this.teststepManager = checkNotNull(teststepManager);
        this.zLicenseManager= checkNotNull(zLicenseManager);
    }

    @Override
    public void init(Map<String, String> params) throws PluginParseException {
    }


    @Override
    public Map<String, Object> getContextMap(Map<String, Object> context) {
    	if(JiraUtil.isJIRA50()){
			webResourceManager.requireResource("com.thed.zephyr.je:zephyr-je-backbone");
		}
        final Issue issue = (Issue) context.get("issue");

        final MapBuilder<String, Object> paramsBuilder = MapBuilder.newBuilder(context);
    	List<Long> issueIds = new ArrayList<Long>();
    	issueIds.add(Long.valueOf(issue.getId()));
	List<Teststep> steps = teststepManager.getTeststeps(issue.getId(), Optional.empty(), Optional.empty());
    	
		paramsBuilder.add("teststeps", steps);
	
	    //Do License Validation.
		ZephyrLicenseVerificationResult licVerificationResult = JiraUtil.performLicenseValidation(zLicenseManager);
	    
	    //license is invalid
	    if( !licVerificationResult.isValid()) {
        	paramsBuilder.add("errors", licVerificationResult.getGeneralMessage());
		}


        paramsBuilder.add("isIE",JiraUtil.isIeBrowser(ActionContext.getRequest().getHeader("User-Agent")));
		return paramsBuilder.toMap();
    }

    @Override
    public String getUniqueContextKey(Map<String, Object> context){
        final Issue issue = (Issue) context.get("issue");
        final ApplicationUser user = authenticationContext.getLoggedInUser();

        return issue.getId() + "/" + (user == null ? "" : user.getName());

    }
}
