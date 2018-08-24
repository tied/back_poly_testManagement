package com.thed.zephyr.je.config.action;

import com.atlassian.jira.bc.issue.link.IssueLinkService;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.portal.PortalPage;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.thed.zephyr.je.config.ZephyrFeatureManager;
import com.thed.zephyr.je.config.license.ZephyrLicenseManager;
import com.thed.zephyr.je.config.license.ZephyrLicenseVerificationResult;
import com.thed.zephyr.je.permissions.service.PermissionConfigManager;
import com.thed.zephyr.je.service.ScheduleManager;
import com.thed.zephyr.util.ConfigurationConstants;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.logger.ZephyrLogger;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import webwork.action.ServletActionContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.thed.zephyr.util.ConfigurationConstants.ZEPHYR_DISABLE_PROJECT_EXEC_WORKFLOW;
import static com.thed.zephyr.util.ConfigurationConstants.ZEPHYR_DISABLE_PROJECT_TESTMENU;

@SuppressWarnings("serial")
public class ZephyrGeneralConfiguration extends JiraWebActionSupport {
    protected static final Logger log = Logger.getLogger(ZephyrGeneralConfiguration.class);
    private final ZephyrLicenseManager zLicenseManager;
    private final IssueLinkService issueLinkService;
    private ZephyrFeatureManager zephyrFeatureManager;
    private PermissionConfigManager permissionConfigManager;
    private final ZephyrLogger zephyrLogger;
    private Logger logger;
    private final IssueLinkTypeManager issueLinkTypeManager;
    private final IssueTypeManager issueTypeManager;
    private final ScheduleManager scheduleManager;

    public ZephyrGeneralConfiguration(ZephyrLicenseManager zLicenseManager, IssueLinkService issueLinkService, ZephyrFeatureManager zephyrFeatureManager,
    		PermissionConfigManager permissionConfigManager,ZephyrLogger zephyrLogger,IssueLinkTypeManager issueLinkTypeManager,
            IssueTypeManager issueTypeManager,ScheduleManager scheduleManager) {
        this.zLicenseManager = zLicenseManager;
        this.issueLinkService = issueLinkService;
        this.zephyrFeatureManager = zephyrFeatureManager;
        this.permissionConfigManager=permissionConfigManager;
        this.zephyrLogger=zephyrLogger;
        this.issueLinkTypeManager=issueLinkTypeManager;
        this.issueTypeManager = issueTypeManager;
        this.scheduleManager = scheduleManager;
    }

    @Override
    public String doDefault() throws Exception {
        ZephyrLicenseVerificationResult licenseVerificationResult = JiraUtil.performLicenseValidation(zLicenseManager);
        if (!licenseVerificationResult.isValid())
            return getRedirect(licenseVerificationResult.getForwardURI().toString());

        return SUCCESS;
    }

    public String doInfo() throws Exception {
        ZephyrLicenseVerificationResult licenseVerificationResult = JiraUtil.performLicenseValidation(zLicenseManager);
        if (!licenseVerificationResult.isValid())
            return getRedirect(licenseVerificationResult.getForwardURI().toString());
        return "info";
    }

    public String doProducts() throws Exception {
        ZephyrLicenseVerificationResult licenseVerificationResult = JiraUtil.performLicenseValidation(zLicenseManager);
        if (!licenseVerificationResult.isValid())
            return getRedirect(licenseVerificationResult.getForwardURI().toString());
        return "products";
    }

    public String doConfigureVersionCheck() {
        String versionCheck = ServletActionContext.getRequest().getParameter("versionCheck");
        log.debug("Should we display Version or not? - " + versionCheck);

        return updateConfigProperty(ConfigurationConstants.ZEPHYR_DISPLAY_VERSION_SETTINGS, versionCheck);
    }
    
    public String doConfigurePermissionCheck() {
        String permissionCheck = ServletActionContext.getRequest().getParameter("permissionCheck");
        String status = updateConfigProperty(ConfigurationConstants.ZEPHYR_DISPLAY_PERMISSION_SETTINGS, permissionCheck);
        if(StringUtils.equalsIgnoreCase(status,SUCCESS)) { 
	        log.debug("Enable Custom Zephyr Permissions? - " + permissionCheck);
	        boolean permissionCheckEnabled = Boolean.parseBoolean(permissionCheck);
	        if(permissionCheckEnabled) {
	        	permissionConfigManager.addAndConfigurePermissions();
	        }
        }
        return status;
    }

    public String doConfigureZephyrLogLevel() {
        String zephyrLogLevel = ServletActionContext.getRequest().getParameter("zephyrLogLevel");
        log.debug("Changing Log Level For Zephyr - " + zephyrLogLevel);
        try {
             if(Logger.getLogger("com.thed.zephyr").getAppender("zephyrForJiraAppender") == null) {
                 zephyrLogger.setLogger(zephyrLogLevel);
             } else {
                 zephyrLogger.setLogLevel(Level.toLevel(zephyrLogLevel));
             }
        } catch (Exception e) {
            log.error("Error setting Zephyr Log Level. Defaulting to Info",e);
        }
        return updateConfigProperty(ConfigurationConstants.ZEPHYR_LOG_LEVEL_INFO, zephyrLogLevel);
    }

    public String doConfigureZephyrLogMaxSize() {
        String zephyrLogMaxSize = ServletActionContext.getRequest().getParameter("zephyrLogMaxSize");
        if(!JiraUtil.getZephyrLogMaxSize().toString().equals(zephyrLogMaxSize)) {
            log.debug("Changing Log Max Size For Zephyr - " + zephyrLogMaxSize);
        }
        return updateConfigProperty(ConfigurationConstants.ZEPHYR_LOG_MAX_SIZE, zephyrLogMaxSize);
    }

    public String doConfigureZephyrLogMaxBackup() {
        String zephyrLogMaxBackup = ServletActionContext.getRequest().getParameter("zephyrLogMaxBackup");
        if(!JiraUtil.getZephyrLogMaxBackup().toString().equals(zephyrLogMaxBackup)) {
            log.debug("Changing Log Max Backup For Zephyr - " + zephyrLogMaxBackup);
        }
        return updateConfigProperty(ConfigurationConstants.ZEPHYR_LOG_MAX_BACKUP, zephyrLogMaxBackup);
    }

    public String doEnableIssueSecurity() {
        String enableIssueSecurity = ServletActionContext.getRequest().getParameter("enableIssueSecurity");
        log.debug("Enable Issue Security For Zephyr - " + enableIssueSecurity);
        return updateConfigProperty(ConfigurationConstants.ZEPHYR_ENABLE_ISSUE_SECURITY, enableIssueSecurity);
    }

    public String doDisableTestSummaryLabels() {
        String disableTestSummaryLabels = ServletActionContext.getRequest().getParameter("disableTestSummaryLabels");
        log.debug("Disable Test Summary Labels Filter For Zephyr - " + disableTestSummaryLabels);
        return updateConfigProperty(ConfigurationConstants.ZEPHYR_DISABLE_LABELS_TEST_SUMMARY_FILTER, disableTestSummaryLabels);
    }

    public String doDisableTestSummaryAllFilters() {
        String disableTestSummaryAllFilters = ServletActionContext.getRequest().getParameter("disableTestSummaryAllFilters");
        log.debug("Disable Test Summary All Filter For Zephyr - " + disableTestSummaryAllFilters);
        return updateConfigProperty(ConfigurationConstants.ZEPHYR_DISABLE_ALL_TEST_SUMMARY_FILTER, disableTestSummaryAllFilters);
    }

    public String doLinkIssueToTest() {
        String issueLinkStatus = ServletActionContext.getRequest().getParameter("issueLink");
        log.debug("Should we create links from Issues back to Testcase? - " + issueLinkStatus);

        return updateConfigProperty(ConfigurationConstants.ZEPHYR_LINK_ISSUE_TO_TESTCASE, issueLinkStatus);
    }

    public String doLinkIssueToTestStep() {
        String issueLinkStatus = ServletActionContext.getRequest().getParameter("issueLinkStep");
        log.debug("Should we create links from Issues back to Testcase step ? - " + issueLinkStatus);

        return updateConfigProperty(ConfigurationConstants.ZEPHYR_LINK_TESTCASE_STEP_TO_ISSUE, issueLinkStatus);
    }

    public String doLinkTestToIssue() {
        String issueLinkStatus = ServletActionContext.getRequest().getParameter("issueLink");
        log.debug("Should we create links from Testcase to Issues ? - " + issueLinkStatus);

        return updateConfigProperty(ConfigurationConstants.ZEPHYR_LINK_TESTCASE_TO_ISSUE, issueLinkStatus);
    }

    public String doRemoteLinkIssueToTestExecution() {
        String remoteIssueLinkStatus = ServletActionContext.getRequest().getParameter("remoteIssueLink");
        log.debug("Should we create links from Issues back to Test Execution? - " + remoteIssueLinkStatus);

        return updateConfigProperty(ConfigurationConstants.ZEPHYR_REMOTE_LINK_ISSUE_TO_EXECUTION, remoteIssueLinkStatus);
    }

    public String doRemoteLinkIssueToTestStepExecution() {
        String remoteIssueLinkStatus = ServletActionContext.getRequest().getParameter("remoteIssueLinkStep");
        log.debug("Should we create links from Issues back to Test Step Execution? - " + remoteIssueLinkStatus);

        return updateConfigProperty(ConfigurationConstants.ZEPHYR_REMOTE_LINK_ISSUE_TO_STEP_EXECUTION, remoteIssueLinkStatus);
    }

    public String doConfigureWorkflowSettings() {
        String showWorkflow = ServletActionContext.getRequest().getParameter("showWorkflow");
        log.debug("Should we display Workflow or not? - " + showWorkflow);

        return updateConfigProperty(ConfigurationConstants.ZEPHYR_DISPLAY_WORKFLOW_SETTINGS, showWorkflow);
    }

    public String doUpdateTestMetricsMenu() {
        String dashboardId = ServletActionContext.getRequest().getParameter("dashboardId");
        log.debug("new selected dashboard Id - " + dashboardId);

        return updateConfigProperty(ConfigurationConstants.ZEPHYR_DASHBOARD_KEY, dashboardId);
    }

    public String doAddIssueTypeTestonProjectCreate() {
        String associateIssueTypeTest = ServletActionContext.getRequest().getParameter("associateIssueTypeTestOnCreate");
        log.debug("Associate IssueType Test to Project on Create? - " + associateIssueTypeTest);
        return updateConfigProperty(ConfigurationConstants.ZEPHYR_ISSUE_TYPE_TEST_PROJECT_CREATE, associateIssueTypeTest);
    }
    
    private String updateConfigProperty(String propertyName, String propertyValue) {
        try {
            JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                    .setString(propertyName, propertyValue);
        } catch (Exception e) {
        	log.error("Exception while updating configuration:", e);
            addError("Exception Message", e.getMessage());
            return ERROR;
        }

        return SUCCESS;
    }

    private String updateConfigPropertyText(String propertyName, String propertyValue) {
        try {
            JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                    .setText(propertyName, propertyValue);
        } catch (Exception e) {
        	log.error("Exception while updating configuration text:", e);
            addError("Exception Message", e.getMessage());
            return ERROR;
        }

        return SUCCESS;
    }

    public String doTestToRequirementLinkType() {
        String testReqLinkType = ServletActionContext.getRequest().getParameter("testReqLinkType");
        String oldTestReqLinkType = ServletActionContext.getRequest().getParameter("oldTestReqLinkType");
        log.debug("Setting Test to Requirement Link Type as  - " + testReqLinkType);
        log.debug("Setting Old value of Test to Requirement Link Type as  - " + oldTestReqLinkType);
        updateConfigProperty(ConfigurationConstants.ZEPHYR_REQ_TO_TEST_LINK_OLD_RELATION, oldTestReqLinkType);
        return updateConfigProperty(ConfigurationConstants.ZEPHYR_REQ_TO_TEST_LINK_RELATION, testReqLinkType);
    }

    public boolean getVersionCheck() {
        return JiraUtil.getVersionCheck().get();
    }

    public Boolean getIssueToTestLink() {
        return JiraUtil.isIssueToTestLinkingEnabled();
    }

    public Boolean getIssueToTestStepLink() {
        return JiraUtil.isIssueToTestStepLinkingEnabled();
    }

    public String getTestToRequirementLink() {
        return JiraUtil.getTestcaseToRequirementLinkType();
    }


    public Boolean getIssueToTestExecutionRemoteLink() {
        return JiraUtil.isIssueToTestExecutionRemoteLinkingEnabled();
    }

    public Boolean getIssueToTestStepExecutionRemoteLink() {
        return JiraUtil.isIssueToTestStepExecutionRemoteLinkingEnabled();
    }

    public Boolean getTestToIssueLink() {
        return JiraUtil.isTestToIssueLinkingEnabled();
    }

    public Boolean getIssueSecurityEnabled() {
        return JiraUtil.isIssueSecurityEnabled();
    }

    public Boolean getTestSummaryLabelsFilterDisabled() {
        return JiraUtil.isTestSummaryLabelsFilterDisabled();
    }

    public Boolean getTestSummaryAllFiltersDisabled() {
        return JiraUtil.isTestSummaryAllFiltersDisabled();
    }


    public Boolean getAssociateIssueTypeTestToProjectCreate() {
        return JiraUtil.isAssociatedIssueTypeTestToProjectCreate();
    }
    
    public Boolean getZephyrPermissionSchemeCheck() {
        return JiraUtil.getPermissionSchemeFlag();
    }
    
    public boolean showWorkflow() {
        return JiraUtil.showWorkflow();
    }


    public String getZephyrLogLevel() {
        return JiraUtil.getZephyrLogLevel();
    }

    public String getZephyrLogMaxSize() {
        return JiraUtil.getZephyrLogMaxSize();
    }

    public String getZephyrLogMaxBackup() {
        return JiraUtil.getZephyrLogMaxBackup();
    }

//	public List<PortalPage> getDashboardList(){
//		return JiraUtil.getDashboardList();
//	}

    public PortalPage getTestMetricsDashboard() {
        try {
            PortalPage dashboard = JiraUtil.getTestMetricsDashboard();
            if (dashboard != null)
                return dashboard;
        } catch (Exception e) {
            log.fatal("Unable to find Dashboard", e);
        }
        return null;
    }

    public String getIssueTypeId() {
        String typeId = JiraUtil.getTestcaseIssueTypeId();
        return typeId;
    }

    public String getCustomFieldTypeId() {
        String typeId = JiraUtil.getPropertySet(
                ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                .getString(ConfigurationConstants.ZEPHYR_CF_TESTSTEP_KEY);
        return typeId;
    }

    public Long getIssueLinkTypeId() {
        return (Long) JiraUtil.getSimpleDBProperty(ConfigurationConstants.ZEPHYR_ISSUE_LINK_RELATION, 0L);
    }

    public Collection<IssueLinkType> getIssueLinkTypes() {
//        Boolean issueLinkStatus = JiraUtil.isIssueToTestLinkingEnabled();
//        if (!issueLinkStatus)
//            return CollectionUtils.EMPTY_COLLECTION;

        Collection<IssueLinkType> issueLinkTypes = issueLinkService.getIssueLinkTypes();
        return CollectionUtils.isEmpty(issueLinkTypes) ? CollectionUtils.EMPTY_COLLECTION : issueLinkTypes;
    }

    public boolean isDarkFeatureEnabled(String featureKey){
        return zephyrFeatureManager.isEnabled(featureKey);
    }

    public List<IssueType> getIssueTypes() {
        Collection<IssueType> issueTypes = issueTypeManager.getIssueTypes();
        List<IssueType> issueTypeList = new ArrayList<>();
        for(IssueType issueType : issueTypes) {
            if(!(issueType.getId().contains(getIssueTypeId()) || issueType.getName().contains("Sub-task") )) {
                issueTypeList.add(issueType);
            }
        }
        return CollectionUtils.isNotEmpty(issueTypeList) ? issueTypeList : Collections.EMPTY_LIST;
    }

    public String doSaveNonIssueTypeTestList() {
        String nonIssueTypeTestList = ServletActionContext.getRequest().getParameter("nonIssueTypeTestList");
        log.debug("Saving admin preference of non issue type test list." + nonIssueTypeTestList);
        return updateConfigPropertyText(ConfigurationConstants.ZEPHYR_SHOW_CREATE_ZEPHYR_TEST_ON_ISSUE_TYPES, nonIssueTypeTestList);
    }

    public List<IssueType> getNonIssueTypeTestSavedList() {
        Collection<IssueType> issueTypes = issueTypeManager.getIssueTypes();
        List<IssueType> savedNonIssueTypes = new ArrayList<>();
        String savedNonIssueTypeList = JiraUtil.getNonIssueTypeTestSaveList();
        if (StringUtils.isNotBlank(savedNonIssueTypeList)) {
            String[] nonIssueTypeTestList = savedNonIssueTypeList.split(",");

            for (String issueTypeId : nonIssueTypeTestList) {
                if (StringUtils.isNotBlank(issueTypeId)) {
                    IssueType issueType = issueTypeManager.getIssueType(issueTypeId);
                    savedNonIssueTypes.add(issueType);
                }
            }
        } else {
            for (IssueType issueType : issueTypes) {
                if (!(issueType.getId().contains(getIssueTypeId()) || issueType.getName().contains("Sub-task"))) {
                    savedNonIssueTypes.add(issueType);
                }
            }
        }
        return CollectionUtils.isNotEmpty(savedNonIssueTypes) ? savedNonIssueTypes : Collections.EMPTY_LIST;
    }

    public List<String> getNonIssueTypeTestIdSavedList() {
        Collection<IssueType> issueTypes = issueTypeManager.getIssueTypes();
        List<String> savedNonIssueTypes = new ArrayList<>();
        String savedNonIssueTypeList = JiraUtil.getNonIssueTypeTestSaveList();
        if (StringUtils.isNotBlank(savedNonIssueTypeList)) {
            String[] nonIssueTypeTestList = savedNonIssueTypeList.split(",");

            for (String issueTypeId : nonIssueTypeTestList) {
                if (StringUtils.isNotBlank(issueTypeId)) {
                    IssueType issueType = issueTypeManager.getIssueType(issueTypeId);
                    savedNonIssueTypes.add(issueType.getId());
                }
            }
        } else {
            for (IssueType issueType : issueTypes) {
                if (!(issueType.getId().contains(getIssueTypeId()) || issueType.getName().contains("Sub-task"))) {
                    savedNonIssueTypes.add(issueType.getId());
                }
            }
        }
        return CollectionUtils.isNotEmpty(savedNonIssueTypes) ? savedNonIssueTypes : Collections.EMPTY_LIST;
    }
    //For the Test Menu disabling.
	/**
	 * Update the configuration disable test menu for the supplied projectId, if
	 * projectId is not provided return the existing configured projects
	 * 
	 * @return SUCCESS/ERROR depending on the operation.
	 */
	public String doUpdateDisableTestMenu() {
		log.debug("doUpdateDisableTestMenu start");
		return doUpdateDisableProject(ZEPHYR_DISABLE_PROJECT_TESTMENU);
	}
	private String doUpdateDisableProject(String configuration){
		String projectId = ServletActionContext.getRequest().getParameter("projectId");
		log.debug("New selected project Id - " + projectId);
		String response = null;
		if (StringUtils.isNotBlank(projectId)) {
			List<String> newList = getDisabledProjectIdsList(configuration);
			response = addProjectToDisableTestMenuList(newList, configuration, projectId);
		} else {
			response = getDisabledProjectIds(configuration);
		}
		return processResponse(response);
	}
	/**
	 * Remove the projectId from the disabled test menu projects.
	 * @return
	 */
	public String doRemoveDisableTestMenu(){
		log.debug("doRemoveDisableTestMenu start");
		return doRemoveDisableProject(ZEPHYR_DISABLE_PROJECT_TESTMENU);
	}
	private String doRemoveDisableProject(String configuration){
		String projectId = ServletActionContext.getRequest().getParameter("projectId");
		log.debug("Removed project Id - " + projectId);
		String response = null;
		if (StringUtils.isNotBlank(projectId)) {
			List<String> newList = getDisabledProjectIdsList(configuration);
			if(newList == null){
				log.warn("Nothing to remove from disable test menu project id list");
				return null;
			}
			response = removeProjectFromDisableList(newList, configuration, projectId);
		}else{
			return ERROR;
		}
		return processResponse(response);
	
	}
	public String processResponse(String response){
		if (response != null) {
			try {
				ServletActionContext.getResponse().setContentType("application/json");
				ServletActionContext.getResponse().getWriter().write(response);
			} catch (Exception e) {
				log.warn("Exception while creating response for processResponse" + e);
				return ERROR;
			}
		} else {
			return ERROR;
		}
		log.debug("processResponse:" + response);
		return SUCCESS;
	}
	/**
	 * Add the projectId to the disabled project list using the standard Zephyr Configuration.
	 * @param projectId
	 * @return
	 */
    private String addProjectToDisableTestMenuList(List<String> newList, String configuration, String projectId){
		//List<String> newList = getDisabledProjectIdsList(ConfigurationConstants.ZEPHYR_DISABLE_PROJECT_TESTMENU);
		if(newList == null) {newList = new ArrayList<>();}
        newList.add(projectId);
        String status =  updateConfigPropertyText(configuration,  new JSONArray(newList).toString());
        if(SUCCESS.equals(status))
        	return new JSONArray(newList).toString();
        else
        	return null;
    }
    /**
     * Remove the projectId fromt the disabled Test menu configuration.
     * @param projectId
     * @return
     */
    public String removeProjectFromDisableList(List<String> newList, String configuration, String projectId){
        newList.remove(projectId);
        String status =  updateConfigPropertyText(configuration,  new JSONArray(newList).toString());
        if(SUCCESS.equals(status))
        	return new JSONArray(newList).toString();
        else
        	return null;
    }
    /**
     * Gives the all projectIds as List<String> from the configuration.
     * @return
     */
    public List<String> getDisabledProjectIdsList(String configuration){
    	String value = getDisabledProjectIds(configuration);
		List<String> newList = new ArrayList<>();
        if(StringUtils.isNotBlank(value)){
        	try{
	        	JSONArray ja = new JSONArray(value);
	    		for(int i = 0 ; i < ja.length(); i++){
	    			newList.add(ja.get(i).toString());
	    		}
        	}catch(Exception e){
        		log.warn("Exception while creating JSON for the disabled project ids addProjectToDisableTestMenuList" + e);
        		return null;
        	}
        }
        return newList;
    }
    /**
     * Get the configuration value from database.
     * @return
     */
    private String getDisabledProjectIds(String configuration){
    	return JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
        		.getText(configuration);
    }

    private String getDisabledTestMenuProjectIds(){
        //Priority is to be given to getText() as it is the most recent update.
        String value = JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                .getText(ConfigurationConstants.ZEPHYR_DISABLE_PROJECT_TESTMENU);
        if(StringUtils.isEmpty(value)){
            value = JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                .getString(ConfigurationConstants.ZEPHYR_DISABLE_PROJECT_TESTMENU);
        }
        return value;
    }

    /**
     * Get Projects list for reindex.
     * @return
     */
    public List<Project> getProjectListForReindex() {
        List<Project> projectList = getProjectManager().getProjects();
        return projectList;
    }

    public String doEnableZephyrUpdateExecutionExecutedon() {
        String updateExecutionExecutedonFlag = ServletActionContext.getRequest().getParameter("updateExecutionExecutedonFlag");
        log.debug("Saving admin preference of zephyr update execution executed on field." + updateExecutionExecutedonFlag);
        return updateConfigProperty(ConfigurationConstants.ZEPHYR_UPDATE_EXECUTION_EXECUTED_ON, updateExecutionExecutedonFlag);
    }

    public Boolean getZephyrUpdateExecutionExecutedOnFlag() {
        return JiraUtil.getZephyrUpdateExecutionExecutedonFlag();
    }

    //For Disabling Execution Workflow
    /**
     * Update the configuration disable Execution Workflow for the supplied projectId, if
     * projectId is not provided return the existing configured projects
     *
     * @return SUCCESS/ERROR depending on the operation.
     */
    public String doUpdateDisableExecWorkflow() {
        log.debug("doUpdateDisableExecWorkflow start");
        return doUpdateDisableProject(ZEPHYR_DISABLE_PROJECT_EXEC_WORKFLOW);
    }
    /**
     * Remove the projectId from the disabeld test menu projects.
     * @return
     */
    public String doRemoveDisableExecWorkflow(){
        log.debug("doRemoveDisableExecWorkflow start");
        return doRemoveDisableProject(ZEPHYR_DISABLE_PROJECT_EXEC_WORKFLOW);
    }

    public Boolean getAnalyticsCheck(){
        return JiraUtil.getZephyrAnalyticsFlag();
    }

    public String doAddAnalytics() {
        String zephyrAnalyticsCheck = ServletActionContext.getRequest().getParameter("zephyrAnalytics");
        String enabled = updateConfigProperty(ConfigurationConstants.ZEPHYR_ANALYTICS_SETTINGS, zephyrAnalyticsCheck);
        return enabled;
    }
}
