package com.thed.zephyr.je.action;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.UserProjectHistoryManager;
import com.atlassian.jira.util.URLCodec;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.jira.web.action.util.navigator.IssueNavigatorType;
import com.thed.zephyr.je.attachment.SessionKeys;
import com.thed.zephyr.je.config.license.ZephyrLicenseManager;
import com.thed.zephyr.je.config.license.ZephyrLicenseVerificationResult;
import com.thed.zephyr.je.zql.core.AutoCompleteJsonGenerator;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.ConfigurationConstants;
import com.thed.zephyr.util.JiraUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import webwork.action.Action;
import webwork.action.ActionContext;

import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import com.atlassian.sal.api.ApplicationProperties;
import com.thed.zephyr.util.ZephyrComponentAccessor;

public class RedirectionAction extends JiraWebActionSupport{
    private final UserProjectHistoryManager projectHistoryManager;
    private final ZephyrLicenseManager zLicenseManager;
    private final JiraAuthenticationContext authenticationContext;
	private AutoCompleteJsonGenerator autocompleteJsonGenerator;
	private final FeatureManager featureManager;

    public RedirectionAction(UserProjectHistoryManager projectHistoryManager,
    						final ZephyrLicenseManager zLicenseManager,
    						final JiraAuthenticationContext authenticationContext,
    						final AutoCompleteJsonGenerator autocompleteJsonGenerator,
							final FeatureManager featureManager) {

    	this.projectHistoryManager=projectHistoryManager;
    	this.zLicenseManager=zLicenseManager;
    	this.authenticationContext=authenticationContext;
    	this.autocompleteJsonGenerator=autocompleteJsonGenerator;
		this.featureManager = featureManager;
    }

    /**
     * Create Issue
     * @return
     * @throws Exception
     */
	public String doCreateTestIssue() throws Exception {
		String redirectURL = "/secure/CreateIssue.jspa?";

		//Do License Validation.
		ZephyrLicenseVerificationResult licVerificationResult = performLicenseValidation();
	    if(!licVerificationResult.isValid()) {
	    	if(JiraUtil.hasGlobalRights(GlobalPermissionKey.ADMINISTER)){
	        	return getRedirect(licVerificationResult.getForwardURI().toString());
		    }
	    	return ERROR;
	    }
	    List<NameValuePair> queryParams = new ArrayList<NameValuePair>();

		Project currentProject = getSelectedProjectObject();
        if (currentProject != null && getAllowedProjects().contains(currentProject))
        	queryParams.add(new BasicNameValuePair("pid", currentProject.getId().toString()));

        String typeId = JiraUtil.getTestcaseIssueTypeId();
        if(typeId != null)
        	queryParams.add(new BasicNameValuePair("issuetype", typeId));

		return getRedirect(redirectURL + URLEncodedUtils.format(queryParams, "UTF-8"));
	}

	/**
	 * Test Metrics
	 * @return
	 * @throws Exception
	 */
	public String doDisplayTestMetrics() throws Exception {
        if(authenticationContext.getLoggedInUser() == null) {
        	String uri = getFWDRequestURI();
        	return getRedirect("/login.jsp?permissionViolation=true&os_destination=" + uri);
        }
		//Do License Validation.
		ZephyrLicenseVerificationResult licVerificationResult = performLicenseValidation();
	    if(!licVerificationResult.isValid()) {
	    	if(JiraUtil.hasGlobalRights(GlobalPermissionKey.ADMINISTER)){
	        	return getRedirect(licVerificationResult.getForwardURI().toString());
		    }
	    	return ERROR;
	    }

		String zephyrDashboardId = JiraUtil.getPropertySet(
				ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
				.getString(ConfigurationConstants.ZEPHYR_DASHBOARD_KEY);

		if (zephyrDashboardId == null){
			//This will happen only if Plugin is not installed correctly.
			return getRedirect("/secure/Dashboard.jspa");
		}

        return getRedirect("/secure/Dashboard.jspa?selectPageId="+zephyrDashboardId);
	}

	/**
	 * Test Metrics
	 * @return
	 * @throws Exception
	 */
	public String doZQLManageFilters() throws Exception {
        if(authenticationContext.getLoggedInUser() == null) {
        	String uri = getFWDRequestURI();
        	return getRedirect("/login.jsp?permissionViolation=true&os_destination=" + uri);
        }
		//Do License Validation.
		ZephyrLicenseVerificationResult licVerificationResult = performLicenseValidation();
	    if(!licVerificationResult.isValid()) {
	    	if(JiraUtil.hasGlobalRights(GlobalPermissionKey.ADMINISTER)){
	        	return getRedirect(licVerificationResult.getForwardURI().toString());
		    }
	    	return ERROR;
	    }

	    //If a User does not have access to projects, why take him to Manage Filters
	    final Project project  = projectHistoryManager.getCurrentProject(Permissions.BROWSE, getLoggedInUser());
    	if(project == null) {
            // they haven't selected a project, if there is only one - select it for them
            final List<Project> projects = getBrowsableProjects();
            //Project is Null only if User does not have permission, Redirect them to Permission page
            if(projects == null || projects.size() == 0) {
            	return PERMISSION_VIOLATION_RESULT;
            }
    	}
    	return super.doDefault();
	}

	/**
	 * Redirects to Test Cycles Tab and displays selected Cycle.
	 * @return
	 * @throws Exception
	 */
	public String doDisplayCycle() throws Exception {
  //Do License Validation.
  ZephyrLicenseVerificationResult licVerificationResult = performLicenseValidation();
     if(!licVerificationResult.isValid()) {
      if(JiraUtil.hasGlobalRights(GlobalPermissionKey.ADMINISTER)){
          return getRedirect(licVerificationResult.getForwardURI().toString());
      }
      return ERROR;
     }

     HttpSession session = request.getSession(false);

     String versionId = request.getParameter("versionId");
     String issueKey = request.getParameter("issueKey");
     String projectKey = issueKey.substring(0, issueKey.lastIndexOf('-'));

     String cycleIdString = request.getParameter("cycleId");
     String folderId = request.getParameter("folderId");

     if(cycleIdString != null){
      Integer cycleId = new Integer(cycleIdString);

   Map<String,String> cycleActionMap = new HashMap<String, String>();
   session.setAttribute(SessionKeys.CYCLE_SUMMARY_DETAIL, cycleActionMap);

   StringBuilder sbuilder = new StringBuilder();
   sbuilder.append("action=expand");
   sbuilder.append(",soffset=0");
   sbuilder.append(",sortQuery=ID:DESC");
   String compositekey = String.valueOf(cycleId)+":"+ String.valueOf(versionId);
   cycleActionMap.put(compositekey, sbuilder.toString());
     }

     session.getServletContext().setAttribute(SessionKeys.CYCLE_SUMMARY_VERSION + authenticationContext.getLoggedInUser(), versionId);

  String cycleTabURL = "/projects/" + projectKey + "?";

  if(StringUtils.isNotBlank(versionId))
            cycleTabURL += "&versionId="+ versionId;

        if(StringUtils.isNotBlank(cycleIdString))
            cycleTabURL += "&cycleId="+cycleIdString;

        if(StringUtils.isNotBlank(folderId))
            cycleTabURL += "&folderId="+folderId;


  cycleTabURL += "&selectedItem=" + URLEncoder.encode("com.thed.zephyr.je:zephyr-tests-page", "UTF-8")
                    + "#test-cycles-tab";

  log.debug("Cycle Tab will be displayed using URL - " + cycleTabURL);

  return getRedirect(cycleTabURL);
 }


	/**
	 * Plan Test Top Menu
	 *
	 * @return
	 * @throws Exception
	 */
	public String doPlanTest() throws Exception {
		String selectedTab = "?selectedItem=" + URLEncoder.encode("com.thed.zephyr.je:zephyr-tests-page", "UTF-8");
		selectedTab += "#test-summary-tab";
		return doNavigateToProjectTab(selectedTab);
	}

	/**
	 * PLan Test Cycle Menu
	 *
	 * @return
	 * @throws Exception
	 */
	public String doPlanTestCycle() throws Exception {
		String selectedTab = "?selectedItem=" + URLEncoder.encode("com.thed.zephyr.je:zephyr-tests-page", "UTF-8");
		selectedTab += "#test-cycles-tab";
		return doNavigateToProjectTab(selectedTab);
	}

 	/**
	 * Execute Test Menu
	 * @return
	 * @throws Exception
	 */
	public String doPlanExecuteTest() throws Exception {
		return doPlanTestCycle();
	}

	/**
	 * Traceability Report
	 *
	 * @return
	 * @throws Exception
	 */
	public String doTraceabilityReport() throws Exception {
		String selectedTab = "?selectedItem=" + URLEncoder.encode("com.thed.zephyr.je:zephyr-tests-page", "UTF-8");
		selectedTab += "#traceability-tab";
		return doNavigateToProjectTab(selectedTab);
	}


	/**
	 * Navigate To Project Dashboard
	 * @param selectedTabFragment TODO
	 * @return
	 * @throws Exception
	 */
	private String doNavigateToProjectTab(String selectedTabFragment) throws Exception {
		//Do License Validation.
		ZephyrLicenseVerificationResult licVerificationResult = performLicenseValidation();
		if (!licVerificationResult.isValid()) {
			if (JiraUtil.hasGlobalRights(GlobalPermissionKey.ADMINISTER)) {
				return getRedirect(licVerificationResult.getForwardURI().toString());
			}
			return ERROR;
		}
		final Project project = projectHistoryManager.getCurrentProject(Permissions.BROWSE, getLoggedInUser());
		String tabContext = "/projects/";

		if (project == null) {
			// they haven't selected a project, if there is only one - select it for them
			final List<Project> projects = getBrowsableProjects();
			//Project is Null only if User does not have permission, Redirect them to Permission page
			if (projects == null || projects.size() == 0) {
				return PERMISSION_VIOLATION_RESULT;
			}

			if (projects.size() > 0) {
				Project onlyProject = projects.get(0);
				return getRedirect(tabContext + onlyProject.getKey() + selectedTabFragment);
			}
		}

        //Fix for ZFJ-1370
        String redirectURL = tabContext + project.getKey() + selectedTabFragment;
        String reqParamProjectKey = request.getParameter("projectKey");
        if (null != reqParamProjectKey && reqParamProjectKey.length() > 0)
            redirectURL = tabContext + reqParamProjectKey + selectedTabFragment;
		request.setAttribute("analyticUrl",JiraUtil.getAnalyticUrl());
		request.setAttribute("analyticsEnabled",JiraUtil.getZephyrAnalyticsFlag());
		return getRedirect(redirectURL);
	}

	/**
	 * Test Search Menu
	 * @return
	 * @throws Exception
	 */
	public String doSearchTest() throws Exception {
		String redirectUrl = "/secure/IssueNavigator.jspa?";
		String testIssueTypeId = JiraUtil.getTestcaseIssueTypeId();

		List<NameValuePair> httpParams = new ArrayList<NameValuePair>();
		httpParams.add(new BasicNameValuePair("reset", "true"));

		//Do License Validation.
		ZephyrLicenseVerificationResult licVerificationResult = performLicenseValidation();
	    if(!licVerificationResult.isValid()) {
	    	if(JiraUtil.hasGlobalRights(GlobalPermissionKey.ADMINISTER)){
	        	return getRedirect(licVerificationResult.getForwardURI().toString());
		    }
	    	return ERROR;
	    }

		if(authenticationContext.getLoggedInUser() == null && !JiraUtil.hasAnonymousPermission(authenticationContext.getLoggedInUser())) {
        	String uri = getFWDRequestURI();
        	return getRedirect("/login.jsp?permissionViolation=true&os_destination=" + uri);
        }


	    Project project  = projectHistoryManager.getCurrentProject(Permissions.BROWSE, getLoggedInUser());
    	if(project == null) {
            // they haven't selected a project, if there is only one - select it for them
            final List<Project> projects = getBrowsableProjects();
            //Project is Null only if User does not have permission, Redirect them to Permission page
            if(projects == null || projects.size() == 0) {
            	return PERMISSION_VIOLATION_RESULT;
            }

            if (projects.size() > 0)
            {
            	project = projects.get(0);
            }
    	}

		httpParams.add(new BasicNameValuePair("jqlQuery", "project = " + project.getId() + " and issuetype = " + testIssueTypeId) );

		if(IssueNavigatorType.getFromCookie(request).compareTo(IssueNavigatorType.SIMPLE) == 0)
			httpParams.add(new BasicNameValuePair("navType", "simple"));
		else
			httpParams.add(new BasicNameValuePair("navType", "advanced"));

	    return getRedirect(redirectUrl + URLEncodedUtils.format(httpParams, "UTF-8"));
	}




	/**
	 * Help Menu
	 * @return
	 * @throws Exception
	 */
	public String doHelp() throws Exception {
		//Do License Validation.
		ZephyrLicenseVerificationResult licVerificationResult = performLicenseValidation();
	    if(!licVerificationResult.isValid()) {
	    	if(JiraUtil.hasGlobalRights(GlobalPermissionKey.ADMINISTER)){
	        	return getRedirect(licVerificationResult.getForwardURI().toString());
		    }
	    	return ERROR;
	    }
	    String majorVersion = "1.0";
	    String version = ComponentAccessor.getPluginAccessor().getPlugin(ApplicationConstants.ZFJ_PLUGIN_KEY).getPluginInformation().getVersion();
        String[] versions = StringUtils.split(version, ".");
        if(versions != null && versions.length >= 2) {
            majorVersion = versions[0] + "."+ versions[1];
        }
        String redirectURL = "http://support.yourzephyr.com/product_help/ZFJ/"+majorVersion;
	    return forceRedirect(redirectURL);
	}

	/**
	 * In case of welcome page request, we will just make a passthro and return
	 * @return {@link Action#SUCCESS}
	 * @throws UnsupportedEncodingException
	 *
	 */
	public String doWelcome() throws UnsupportedEncodingException{
		ZephyrLicenseVerificationResult licVerificationResult = performLicenseValidation();
	    if(!licVerificationResult.isValid()) {
	    	if(JiraUtil.hasGlobalRights(GlobalPermissionKey.ADMINISTER)){
	        	return getRedirect(licVerificationResult.getForwardURI().toString());
		    }
	    	return ERROR;
	    }
		if(authenticationContext.getLoggedInUser() == null) {
        	String uri = getFWDRequestURI();
        	return getRedirect("/login.jsp?permissionViolation=true&os_destination=" + uri);
        }
		return SUCCESS;
	}


	/**
	 * In case of welcome page request, we will just make a passthro and return
	 * @return {@link Action#SUCCESS}
	 * @throws UnsupportedEncodingException
	 *
	 */
	public String doExecNavAction() throws UnsupportedEncodingException{
		ZephyrLicenseVerificationResult licVerificationResult = performLicenseValidation();
	    if(!licVerificationResult.isValid()) {
	    	if(JiraUtil.hasGlobalRights(GlobalPermissionKey.ADMINISTER)){
	        	return getRedirect(licVerificationResult.getForwardURI().toString());
		    }
	    	return ERROR;
	    }
		if(authenticationContext.getLoggedInUser() == null) {
        	String uri = getFWDRequestURI();
        	return getRedirect("/login.jsp?permissionViolation=true&os_destination=" + uri);
        }

		try {
			String json = autocompleteJsonGenerator.getVisibleFieldNamesJson(ComponentAccessor.getComponent(JiraAuthenticationContext.class).getLoggedInUser(),getI18nHelper().getLocale());
			ActionContext.getRequest().setAttribute("jqlFieldZ",json);

			String reservedWords = autocompleteJsonGenerator.getJqlReservedWordsJson();
			ActionContext.getRequest().setAttribute("reservedWords",reservedWords);

			String functionZ = autocompleteJsonGenerator.getVisibleFunctionNamesJson(ComponentAccessor.getComponent(JiraAuthenticationContext.class).getLoggedInUser(),getI18nHelper().getLocale());
			ActionContext.getRequest().setAttribute("functionZ",functionZ);

			ApplicationProperties applicationProperties = (ApplicationProperties) ZephyrComponentAccessor.getInstance().getComponent("applicationProperties");
        	ActionContext.getRequest().setAttribute("zephyrBaseUrl", applicationProperties.getBaseUrl());

            /*
            * commented as inApp feature is not live.
            ActionContext.getRequest().setAttribute("inAppMessageUrl", JiraUtil.getInAppMessageUrl());*/

            ActionContext.getRequest().setAttribute("analyticUrl", JiraUtil.getAnalyticUrl());

            ActionContext.getRequest().setAttribute("isIE",JiraUtil.isIeBrowser(ActionContext.getRequest().getHeader("User-Agent")));

            ActionContext.getRequest().setAttribute("analyticsEnabled", JiraUtil.getZephyrAnalyticsFlag());

        } catch(Exception e) {
			log.error("Error retrieving Autocomplete resource",e);
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
}
