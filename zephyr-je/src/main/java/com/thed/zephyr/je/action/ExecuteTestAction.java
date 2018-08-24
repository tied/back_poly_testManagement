package com.thed.zephyr.je.action;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.exception.IssueNotFoundException;
import com.atlassian.jira.exception.IssuePermissionException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.RendererManager;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.JiraUrlCodec;
import com.atlassian.jira.util.URLCodec;
import com.atlassian.jira.web.action.issue.AbstractIssueSelectAction;
import com.thed.zephyr.je.attachment.SessionKeys;
import com.thed.zephyr.je.config.license.ZephyrLicenseManager;
import com.thed.zephyr.je.config.license.ZephyrLicenseVerificationResult;
import com.thed.zephyr.je.model.Cycle;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.service.CycleManager;
import com.thed.zephyr.je.service.ScheduleManager;
import com.thed.zephyr.je.service.StepResultManager;
import com.thed.zephyr.je.service.TeststepManager;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.JiraUtil;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import webwork.action.ActionContext;

import java.io.UnsupportedEncodingException;
import java.util.*;

public class ExecuteTestAction extends AbstractIssueSelectAction {
    private final JiraAuthenticationContext authenticationContext;
    private final VersionManager versionManager;
	private final ScheduleManager scheduleManager;
	private final CycleManager 	cycleManager;
	private final DateTimeFormatterFactory dateTimeFormatterFactory;
	private final I18nHelper i18n; 
    private final ZephyrLicenseManager zLicenseManager;
    private final TeststepManager teststepManager;
    private final StepResultManager stepResultManager;
    private final IssueManager issueManager;
	private final RendererManager rendererManager;

	
	public ExecuteTestAction(JiraAuthenticationContext authenticationContext,  
							RendererManager rendererManager, 
							VersionManager versionManager,
							ScheduleManager scheduleManager,
							CycleManager cycleManager,
							DateTimeFormatterFactory dateTimeFormatterFactory,
							ZephyrLicenseManager zLicenseManager,
							TeststepManager teststepManager,
							StepResultManager stepResultManager,
							IssueManager issueManager) {
		this.authenticationContext=authenticationContext;
		this.rendererManager = rendererManager;
        this.versionManager=versionManager;
        this.scheduleManager=scheduleManager;
        this.cycleManager=cycleManager;
		this.dateTimeFormatterFactory = dateTimeFormatterFactory;
		this.i18n = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
		this.zLicenseManager=zLicenseManager;
		this.teststepManager = teststepManager;
		this.stepResultManager = stepResultManager;
		this.issueManager = issueManager;
	}
	

    @Override
	public String doDefault() throws Exception{
        if(authenticationContext.getLoggedInUser() == null) {
        	String uri = getFWDRequestURI();
        	return getRedirect("/login.jsp?permissionViolation=true&os_destination=" + uri);
        }

	    //Do License Validation.
        ZephyrLicenseVerificationResult licenseVerificationResult = performLicenseValidation();
	    if(!licenseVerificationResult.isValid()) {
	    	if(JiraUtil.hasGlobalRights(GlobalPermissionKey.ADMINISTER)){
	        	return getRedirect(licenseVerificationResult.getForwardURI().toString());
		    }
	    	return super.doDefault();
	    }
	    
    	String scheduleId = request.getParameter("scheduleId");
    	Schedule schedule = scheduleManager.getSchedule(Integer.valueOf(scheduleId));
    	if(schedule == null){
    		return ERROR;
    	}
    	
    	Project currentProject = super.getSelectedProjectObject();
    	//Either there is no current project set, or currentProject is not same as context project, we will set it.
    	if(currentProject == null || !currentProject.getId().equals(schedule.getProjectId())){
    		currentProject = ComponentAccessor.getProjectManager().getProjectObj(schedule.getProjectId());
    		super.setSelectedProject(currentProject);
    	}
    	request.getSession(false).setAttribute(SessionKeys.CURRENT_SELECTED_SCHEDULE_ID, scheduleId);
        // Check if the user was starting a new search, looking at a
        // filter, or looking at JQL. We want to redirect accordingly.
        String filterId = request.getParameter("filterId");
        if(StringUtils.isBlank(filterId)) {
        	filterId = request.getParameter("filter");
        }

        String zqlQuery = request.getParameter("query");
        String view = request.getParameter("view");
        String offset = request.getParameter("offset");
        StringBuilder redirectUrl = new StringBuilder("/secure/enav/#/"+scheduleId);

        if (filterId != null || zqlQuery != null || view != null || offset != null) {
        	redirectUrl.append("?");
        }
        
        if (filterId != null) {
            redirectUrl.append("filter=").append(JiraUrlCodec.encode(filterId));
        } else if (zqlQuery != null) {
            redirectUrl.append("query=").append(zqlQuery);
        } 
        if (view != null) {
            redirectUrl.append("&view=").append(JiraUrlCodec.encode(view));
        } 
        if (offset != null) {
            redirectUrl.append("&offset=").append(JiraUrlCodec.encode(view));
        } 
    	return getRedirect(redirectUrl.toString());
    }


	/**
	 * Verifies if License is Valid
	 */
	private ZephyrLicenseVerificationResult performLicenseValidation() {
		ZephyrLicenseVerificationResult licVerificationResult = JiraUtil.performLicenseValidation(zLicenseManager);
	    
	    //license is invalid
	    if( !licVerificationResult.isValid()) {
        	ActionContext.getRequest().setAttribute("errors", licVerificationResult.getGeneralMessage());
		}
	    return licVerificationResult;
	}


    public String doAddExecute() throws Exception {
    	if(authenticationContext.getLoggedInUser() == null) {
        	String uri = getFWDRequestURI();
        	//For popups, We should just send error and display the link instead of redirecting
        	return getRedirect("/login.jsp?permissionViolation=true&os_destination=" + uri);
        }
	    //Do License Validation.
    	ZephyrLicenseVerificationResult licVerificationResult = performLicenseValidation();
	    if(!licVerificationResult.isValid()) {
	    	if(JiraUtil.hasGlobalRights(GlobalPermissionKey.ADMINISTER)){
	        	return getRedirect(licVerificationResult.getForwardURI().toString());
		    }
	    	return super.doDefault();
	    }
		boolean hasIssuePermissionError = populateExecuteDialogData();
		if(hasIssuePermissionError) {
			log.error("User does not have permission to view the Issue");
			addErrorMessage(authenticationContext.getI18nHelper().getText("zephyr.common.login.error"));
			return ERROR;
		}
		return super.doDefault();
    }


    public String doAddToCycle() throws Exception {
    	if(authenticationContext.getLoggedInUser() == null) {
        	String uri = getFWDRequestURI();
        	//For popups, We should just send error and display the link instead of redirecting
        	//return getRedirect("/login.jsp?permissionViolation=true&os_destination=" + uri);
        	addError(PERMISSION_VIOLATION_RESULT, "User not logged in");
        	return ERROR;
        }
	    //Do License Validation.
    	ZephyrLicenseVerificationResult licVerificationResult = performLicenseValidation();
	    if(!licVerificationResult.isValid()) {
	    	if(JiraUtil.hasGlobalRights(GlobalPermissionKey.ADMINISTER)){
	        	return getRedirect(licVerificationResult.getForwardURI().toString());
		    }
	    	return super.doDefault();
	    }
    	boolean hasIssuePermissionError = populateExecuteDialogData();
	    if(hasIssuePermissionError) {
			addErrorMessage(authenticationContext.getI18nHelper().getText("zephyr.common.login.error"));
			return ERROR;
		}
    	return super.doDefault();
    }

    
	/**
	 * @throws Exception
	 */
	private boolean populateExecuteDialogData() throws Exception {
		final Boolean SHOW_ARCHIVED = Boolean.FALSE;
		try {
			Issue issue = getIssueObject();
			if (issue == null) {
				throw new IssueNotFoundException("Issue Context found null");
			}

			Project currentProject = issue.getProjectObject();
			if (currentProject == null) {
				throw new Exception("Invalid Project");
			}

			//Get Versions belonging to Project
			Collection<Version> versionsUnreleased = versionManager.getVersionsUnreleased(currentProject.getId(), SHOW_ARCHIVED);
			Collection<Version> versionsReleased = versionManager.getVersionsReleased(currentProject.getId(), SHOW_ARCHIVED);
			Map<Long, Map<Integer, String>> versionCycleMap = new HashMap<Long, Map<Integer, String>>();

			//get Cycles for Unscheduled Version as they are not part of Project set up
			addCycleVersionMapping(currentProject.getId(), versionCycleMap, new Long(ApplicationConstants.UNSCHEDULED_VERSION_ID));
			//get Cycles for Unreleased Versions
			versionsUnreleased.forEach(version -> addCycleVersionMapping(currentProject.getId(), versionCycleMap, version.getId()));
			//get Cycles for released Versions
			versionsReleased.forEach(version -> addCycleVersionMapping(currentProject.getId(), versionCycleMap, version.getId()));

			ActionContext.getRequest().setAttribute("issue", issue);
			ActionContext.getRequest().setAttribute("project", currentProject);
			ActionContext.getRequest().setAttribute("versionsUnreleased", versionsUnreleased);
			ActionContext.getRequest().setAttribute("versionsReleased", versionsReleased);
			ActionContext.getRequest().setAttribute("cycles", versionCycleMap);
		} catch (IssuePermissionException e) {
			return true;
		}
		return false;
	}

	private void addCycleVersionMapping(Long projectId, Map<Long, Map<Integer, String>> versionCycleMap, Long versionId) {
		List<Cycle> cycles = cycleManager.getCyclesByVersion(versionId, projectId, -1);
		/*
		Map<Integer,String> cycleNameMap  = new HashMap<Integer,String>();
		Added linked hash map implementation in order to preserve the order of retrieved data from the database.
		 */
		Map<Integer,String> cycleNameMap  = new LinkedHashMap<Integer,String>();
		cycles.forEach(cycle -> cycleNameMap.put(cycle.getID(), StringEscapeUtils.escapeJava(cycle.getName())));
		versionCycleMap.put(versionId, cycleNameMap);
	}


	/**
	 * @throws UnsupportedEncodingException 
	 * @returns uri that can be appended to the login page 
	 */
	private String getFWDRequestURI() throws UnsupportedEncodingException {
		String uri = request.getServletPath();
		if(request.getQueryString() != null ){
			uri += "?" + request.getQueryString();
		}
		return URLCodec.encode(uri);
	}
    
    
	@Override
	@RequiresXsrfCheck
    protected String doExecute() throws Exception{
        if (isInlineDialogMode()){
            return returnComplete();
        }
	    //Do License Validation.
        ZephyrLicenseVerificationResult licVerificationResult = performLicenseValidation();
	    if(!licVerificationResult.isValid()) {
	    	if(JiraUtil.hasGlobalRights(GlobalPermissionKey.ADMINISTER)){
	        	return getRedirect(licVerificationResult.getForwardURI().toString());
		    }
	    	return super.doDefault();
	    }     
        return super.doExecute();
    }
 
} 
   