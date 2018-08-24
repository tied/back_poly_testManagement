package com.thed.zephyr.je.action;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.URLCodec;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.log4j.Logger;

import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.thed.zephyr.je.config.license.ZephyrLicenseManager;
import com.thed.zephyr.je.config.license.ZephyrLicenseVerificationResult;
import com.thed.zephyr.util.JiraUtil;

import webwork.action.ActionContext;

public class TestIssueImporter extends JiraWebActionSupport {
	protected static final Logger log = Logger.getLogger(JiraWebActionSupport.class);

	private final ZephyrLicenseManager zLicenseManager;
	private final JiraAuthenticationContext authenticationContext;

	public TestIssueImporter(ZephyrLicenseManager zLicenseManager,JiraAuthenticationContext authenticationContext){
		this.zLicenseManager = zLicenseManager;
		this.authenticationContext=authenticationContext;
	}


	public String doImport() throws Exception {
		ZephyrLicenseVerificationResult licenseVerificationResult = JiraUtil.performLicenseValidation(zLicenseManager);
		if (!licenseVerificationResult.isValid())
			return getRedirect(licenseVerificationResult.getForwardURI().toString());

		if(authenticationContext.getLoggedInUser() == null) {
			String uri = getFWDRequestURI();
			return getRedirect("/login.jsp?permissionViolation=true&os_destination=" + uri);
		}

		List<Project> projectsList = getBrowsableProjects();
		
		ActionContext.getRequest().setAttribute("projects", getBrowsableProjects());
		if(projectsList != null && projectsList.size() > 0) {
			long projectId = projectsList.get(0).getId();
			ActionContext.getRequest().setAttribute("issueTypes", projectsList.get(0).getIssueTypes());
			ActionContext.getRequest().setAttribute("fileTypes", new ArrayList<String>(){{add("XML"); add("Excel");}});
		}

		
		return SUCCESS;
	}



	/**
	 * Returns the projects that the current user is allowed to Browse.
	 * @return the projects that the current user is allowed to Browse.
	 */
	public List<Project> getBrowsableProjects(){
		Collection<Project> browsableProjects = getPermissionManager().getProjects(ProjectPermissions.BROWSE_PROJECTS, getLoggedInUser());
		return new ArrayList<Project>(browsableProjects);
	}

	public Collection<Project> getAllowedProjects(){
		return getPermissionManager().getProjects(ProjectPermissions.CREATE_ISSUES, getLoggedInUser());
	}

	/**
	 * @throws UnsupportedEncodingException
	 * @returns uri that can be appended to the login page
	 */
	//TODO: move to common Util
	private String getFWDRequestURI() throws UnsupportedEncodingException {
		String uri = request.getServletPath();
		if(request.getQueryString() != null ){
			uri += "?" + request.getQueryString();
		}
		return URLCodec.encode(uri);
	}
}
