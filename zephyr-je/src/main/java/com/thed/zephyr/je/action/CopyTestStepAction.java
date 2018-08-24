package com.thed.zephyr.je.action;

import org.apache.log4j.Logger;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.thed.zephyr.je.config.license.ZephyrLicenseManager;
import com.thed.zephyr.je.config.license.ZephyrLicenseVerificationResult;
import com.thed.zephyr.util.JiraUtil;

import webwork.action.ActionContext;

/**
 * @author manjunath
 * @version 1.0
 *
 */
public class CopyTestStepAction extends JiraWebActionSupport {

	/**
	 * Generated Serial Version Id.
	 */
	private static final long serialVersionUID = -6167374059609815827L;
	
	protected static final Logger log = Logger.getLogger(JiraWebActionSupport.class);
	
	private final ZephyrLicenseManager zLicenseManager;
	
	private final IssueManager issueManager;
	
	public CopyTestStepAction(ZephyrLicenseManager zLicenseManager, IssueManager issueManager) {
		this.zLicenseManager = zLicenseManager;
		this.issueManager = issueManager;
	}
	
	@Override
    public String doDefault() throws Exception {
        ZephyrLicenseVerificationResult licenseVerificationResult = JiraUtil.performLicenseValidation(zLicenseManager);
        if (!licenseVerificationResult.isValid())
            return getRedirect(licenseVerificationResult.getForwardURI().toString());
        
        String issueId = getHttpRequest().getParameter("id");
        
        Issue issue = issueManager.getIssueObject(Long.valueOf(issueId));
        
        ActionContext.getRequest().setAttribute("issueKey", issue.getKey());

		ActionContext.getRequest().setAttribute("isJIRAGreaterThan710", JiraUtil.isJIRAGreaterThan710());
        
        return SUCCESS;
    }
}
