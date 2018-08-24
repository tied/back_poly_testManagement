package com.thed.zephyr.je.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import webwork.action.ActionContext;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.web.action.issue.AbstractIssueSelectAction;
import com.thed.zephyr.je.attachment.SessionKeys;
import com.thed.zephyr.je.config.license.ZephyrLicenseManager;
import com.thed.zephyr.je.config.license.ZephyrLicenseVerificationResult;
import com.thed.zephyr.je.model.Cycle;
import com.thed.zephyr.je.service.CycleManager;
import com.thed.zephyr.util.JiraUtil;

public class CreateCycleAction extends AbstractIssueSelectAction {
    private final JiraAuthenticationContext authenticationContext;
	private final VersionManager versionManager;
 	private CycleManager cycleManager;
    private final ZephyrLicenseManager zLicenseManager;
    
	public CreateCycleAction(JiraAuthenticationContext authenticationContext, CycleManager cycleManager,  
			VersionManager versionManager, ZephyrLicenseManager zLicenseManager) {
		this.authenticationContext=authenticationContext;
        this.cycleManager = cycleManager;
        this.versionManager=versionManager;
		this.zLicenseManager = zLicenseManager;
	}
	

    @Override
	public String doDefault() throws Exception{
        if(authenticationContext.getLoggedInUser() == null) {
        	throw new Exception("Authenticaton failed");
        }
        
        ZephyrLicenseVerificationResult licenseVerificationResult = JiraUtil.performLicenseValidation(zLicenseManager);
		if(!licenseVerificationResult.isValid())
			return getRedirect(licenseVerificationResult.getForwardURI().toString());
        
        //If the action passed is new, set the Request Attribute , so that we can display the Create New Cycle Form 
        
        if(StringUtils.equals("new",request.getParameter("action"))) {
        	ActionContext.getRequest().setAttribute("isNew","new");
        	Project currentProject = super.getSelectedProjectObject();
        	if(currentProject != null) {
            	ActionContext.getRequest().setAttribute("projectId",currentProject.getId());
            	List<Version> versions = versionManager.getVersions(currentProject.getId());
            	ActionContext.getRequest().setAttribute("versions",versions);
        	}
        	//Stick the last visited Version on cycle Summary
        	String versionSelected = getSelected();
            if(!StringUtils.isBlank(versionSelected)) {
            	ActionContext.getRequest().setAttribute("lastvisitedVersion",versionSelected);
            }
        } 
        return super.doDefault();
    }
    
    
 
    //@RequiresXsrfCheck
    @Override
	protected String doExecute() throws Exception{
        if (isInlineDialogMode()){
            return returnComplete();
        }
        if(getIssue() != null) {
        	return getRedirect("/browse/" + getIssue().getString("key"));
        } 
        return super.doExecute();
    }
    
    
    /**
     * Called from 
     * @return list of cycles matching with version and project criteria of the issue
     */
    public List<Cycle> getCycles() {
    	List<Cycle> cycles = new ArrayList<Cycle>();
    	if(!ActionContext.getRequest().getAttribute("isNew").equals("new")) {
			Issue issue = getIssueObject();
			if(issue == null)
				return null;
			Long version = getIssueVersion(issue);
			cycles = cycleManager.getCyclesByVersion(version,issue.getProjectObject().getId(),null);
    	}
    	return cycles;
    }
    
    /**
     * Temporary method to determine issue version
     * @param issue
     * @return
     */
    private Long getIssueVersion(Issue issue){
    	Collection<Version> versions = issue.getFixVersions();
    	if(versions != null && versions.size() > 0){
			/*@TODO We are only taking first version into considerations, this will change in final version*/
			return versions.iterator().next().getId();
		}else{
			versions = issue.getAffectedVersions();
			if(versions != null && versions.size() > 0){
				/*@TODO We are only taking first version into considerations, this will change in final version*/
				return versions.iterator().next().getId();
			}
		}
    	return null;
    }
    
    private String getSelected()
    {
        final String currentVersionId = (String) ActionContext.getServletContext().getAttribute(SessionKeys.CYCLE_SUMMARY_VERSION+authenticationContext.getLoggedInUser());
        if (!StringUtils.isBlank(currentVersionId)) {
            return currentVersionId;
        }
        return null;
    }
}
