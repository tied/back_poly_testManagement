package com.thed.zephyr.je.filter;

import com.atlassian.core.filters.AbstractHttpFilter;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.usercompatibility.UserCompatibilityHelper;
import com.atlassian.jira.util.URLCodec;
import com.thed.zephyr.je.config.license.ZephyrLicenseManager;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.model.StepResult;
import com.thed.zephyr.je.model.Teststep;
import com.thed.zephyr.je.service.ScheduleManager;
import com.thed.zephyr.je.service.StepResultManager;
import com.thed.zephyr.je.service.TeststepManager;
import com.thed.zephyr.util.JiraUtil;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * Redirects to the plugin's exec navigator servlet.
 *
 */
public class ExecNavRewriteFilter extends AbstractHttpFilter
{
    private final JiraAuthenticationContext authenticationContext;
    private final ZephyrLicenseManager zLicenseManager;
	private final ScheduleManager scheduleManager;
    private final TeststepManager teststepManager;
    private final StepResultManager stepResultManager;
    private final IssueManager issueManager;

    public ExecNavRewriteFilter(JiraAuthenticationContext authenticationContext,  
			ZephyrLicenseManager zLicenseManager,ScheduleManager scheduleManager,
			TeststepManager teststepManager,StepResultManager stepResultManager,
			IssueManager issueManager) {
		this.authenticationContext=authenticationContext;
		this.zLicenseManager=zLicenseManager;
		this.scheduleManager=scheduleManager;
		this.teststepManager=teststepManager;
		this.stepResultManager=stepResultManager;
		this.issueManager = issueManager;
    }

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException
    {
    	if(authenticationContext.getLoggedInUser() == null) {
        	String uri = getFWDRequestURI(request);
        	//For popups, We should just send error and display the link instead of redirecting
        	response.sendRedirect(request.getContextPath() + "/login.jsp?permissionViolation=true&os_destination=" + uri);
            return;
        }
    	String scheduleId = request.getParameter("scheduleId");
    	if(scheduleId != null) {
	    	Schedule schedule = scheduleManager.getSchedule(Integer.valueOf(scheduleId));
	    	if(schedule != null) {
	    		MutableIssue issue = issueManager.getIssueObject(Long.valueOf(schedule.getIssueId()));
			List<Teststep> steps = teststepManager.getTeststeps(issue.getId(), Optional.empty(), Optional.empty());
	    		List<Integer> createStepResultList = new ArrayList<Integer>();
	    		
	    		for(Teststep step : steps){
	    			createStepResultList.add(step.getID());
	    		}
	    		List<StepResult> stepResults = stepResultManager.getStepResultsBySchedule(Integer.valueOf(scheduleId));
	        	for(StepResult sResult: stepResults){
	        		createStepResultList.remove(sResult.getStep().getID());
	        	}
	
	        	//Add "unexecuted" stepResult for all step which are not executed and don't have entry for them in StepResult table.
				String userKey = UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(ComponentAccessor.getJiraAuthenticationContext()));
	        	for(Integer noResultStepId: createStepResultList){
	        		//We need to have step_result_id so that step attachments can get the association available.
	        		//This is the only place where we can create step_results as we have both step_id and schedule_id.
	        		//May be we should try to find out how to create step_results in bulk to improve performance.
	        		
	        		Map<String,Object> resultProperties = new HashMap<String,Object>();
	        		resultProperties.put("SCHEDULE_ID", Integer.valueOf(scheduleId));
	        		resultProperties.put("STEP_ID", Integer.valueOf(noResultStepId));
	        		resultProperties.put("PROJECT_ID", schedule.getProjectId());
	        		resultProperties.put("STATUS", "-1");
					resultProperties.put("CREATED_BY", userKey);
					resultProperties.put("MODIFIED_BY", userKey);

	        		stepResultManager.addStepResult(resultProperties);
	        	}
	    	}
    	}
        request.getRequestDispatcher("/secure/ExecNavAction.jspa").forward(request, response);
    }
    
	/**
	 * @throws UnsupportedEncodingException 
	 * @returns uri that can be appended to the login page 
	 */
	private String getFWDRequestURI(final HttpServletRequest request) throws UnsupportedEncodingException {
		String uri = request.getServletPath();
		if(request.getQueryString() != null ){
			uri += "?" + request.getQueryString();
		}
		return URLCodec.encode(uri);
	}
}