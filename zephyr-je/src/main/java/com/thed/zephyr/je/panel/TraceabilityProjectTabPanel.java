package com.thed.zephyr.je.panel;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.plugin.projectpanel.impl.AbstractProjectTabPanel;
import com.atlassian.jira.project.browse.BrowseContext;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.thed.zephyr.je.config.license.ZephyrLicenseManager;
import com.thed.zephyr.je.config.license.ZephyrLicenseVerificationResult;
import com.thed.zephyr.util.JiraUtil;
import org.apache.log4j.Logger;
import webwork.action.ActionContext;

import java.util.Map;


public class TraceabilityProjectTabPanel extends AbstractProjectTabPanel {
    protected final Logger log = Logger.getLogger(TraceabilityProjectTabPanel.class);
    private final ZephyrLicenseManager zLicenseManager;
    private final FeatureManager featureManager;

    public TraceabilityProjectTabPanel(final JiraAuthenticationContext jiraAuthenticationContext,
                                            final ZephyrLicenseManager zLicenseManager, final FeatureManager featureManager) {
        super(jiraAuthenticationContext);
        this.zLicenseManager = zLicenseManager;
        this.featureManager = featureManager;
    }

    @Override
    public String getHtml(BrowseContext ctx) {
        //Do License Validation.
        ZephyrLicenseVerificationResult licVerificationResult = JiraUtil.performLicenseValidation(zLicenseManager);

        //license is invalid
        if (!licVerificationResult.isValid()) {
            ActionContext.getRequest().setAttribute("errors", licVerificationResult.getErrorMessage() + "<br>" + licVerificationResult.getGeneralMessage());
        }
        return super.getHtml(ctx);
    }

    @Override
    public boolean showPanel(BrowseContext ctx) {
        // return false as we don't want to show it for JIRA 7
        return false;
    }
    
    @Override
    protected Map<String, Object> createVelocityParams(final BrowseContext ctx) {
        Map<String, Object> paramMap = ctx.createParameterMap();
        if (ctx.getProject() != null) {
        	String testIssueTypeId = JiraUtil.getTestcaseIssueTypeId();
        	paramMap.put("pKey", ctx.getProject().getKey());
            paramMap.put("pid", ctx.getProject().getId());
            paramMap.put("testIssueTypeId", testIssueTypeId);
        }
        return paramMap;
    }
}
