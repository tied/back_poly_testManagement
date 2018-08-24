package com.thed.zephyr.util;

import com.atlassian.cache.compat.CachedReference;
import com.atlassian.cache.compat.CompatibilityCacheFactory;
import com.atlassian.cache.compat.Supplier;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.fugue.Option;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.context.manager.JiraContextTreeManager;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.fields.screen.FieldScreenManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.portal.PortalPage;
import com.atlassian.jira.portal.PortalPageManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.propertyset.JiraPropertySetFactory;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.preferences.UserPreferencesManager;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.BuildUtilsInfo;
import com.atlassian.jira.util.BuildUtilsInfoImpl;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.workflow.JiraWorkflow;
import com.atlassian.jira.workflow.WorkflowManager;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.upm.api.license.entity.PluginLicense;
import com.atlassian.util.concurrent.Nullable;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Ints;
import com.opensymphony.module.propertyset.PropertyException;
import com.opensymphony.module.propertyset.PropertySet;
import com.thed.zephyr.je.config.license.ZephyrLicense;
import com.thed.zephyr.je.config.license.ZephyrLicenseManager;
import com.thed.zephyr.je.config.license.ZephyrLicenseVerificationResult;
import com.thed.zephyr.je.config.model.ExecutionStatus;
import com.thed.zephyr.je.permissions.model.PermissionType;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.atlassian.jira.util.dbc.Assertions.notNull;
import static org.apache.commons.io.IOUtils.closeQuietly;

public class JiraUtil {

    private static final Logger log = Logger.getLogger(JiraUtil.class);
    private static final String HEX_COLOR_PATTERN = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$";
    private static final Pattern pattern = Pattern.compile(HEX_COLOR_PATTERN);
    private static final String ERROR_LOG_MESSAGE = "[Error] [Error code: %s %s Error Message : %s]";
    private static final String BUILD_PROPERTIES_RESOURCE = "build.properties";

    //This variable will always hold the latest and greatest map of Execution Statuses available.
    private static CachedReference<Map<Integer, ExecutionStatus>> executionStatusCache = null;
    //This variable will always hold the latest and greatest map of Step Execution Statuses available.
    private static CachedReference<Map<Integer, ExecutionStatus>> stepExecutionStatusCache = null;

    private static PropertySet propertySet = null;

    //This variable holds latest valid license
    private static ZephyrLicense currentZephyrLicense = null;
    
    private static ThreadLocal<Project> PROJECT_THREAD_LOCAL = new ThreadLocal<Project>();
    /**
     * Lazily initialized when first asked for, Stores if jira is 5.0.
     */
    private static Boolean jira50;

    /**
     * Lazily initialized when first asked for, Stores if jira is > 6.3.
     */
    private static Boolean jira63;

    /**
     * Lazily initialized when first asked for, Stores if jira is < 6.4.
     */
    private static Boolean jiraOlderThan64;

    /**
     * Lazily initialized when first asked for, Stores if jira is > 7.10.
     */
    private static Boolean jira710;

    public static ZephyrLicense getCurrentZephyrLicense() {
        return currentZephyrLicense;
    }

    public static void setCurrentZephyrLicense(ZephyrLicense currentZephyrLicense) {
        JiraUtil.currentZephyrLicense = currentZephyrLicense;
    }

    public static ApplicationProperties getApplicationProperties() {
        return ComponentAccessor.getApplicationProperties();
    }

    public static Locale getUserLocale() {
        return ComponentAccessor.getApplicationProperties().getDefaultLocale();
    }

    /**
     * Validate color value in hex with regular expression
     *
     * @param hex hex for validation
     * @return true valid hex, false invalid hex
     * Reference: http://www.mkyong.com/regular-expressions/how-to-validate-hex-color-code-with-regular-expression/
     */
    public static boolean validateHexColor(final String hex) {
        Matcher matcher = pattern.matcher(hex);
        return matcher.matches();
    }

    public static boolean isTypeAssociatedToDefaultScheme(String issueTypeId) {
        for (IssueType issueType : getIssueTypeSchemeManager().getIssueTypesForDefaultScheme()) {
            if (issueType.getId().equals(issueTypeId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Locate PropertySet using PropertyStore for this sequenceName/sequenceId
     * mapping.
     *
     * @param entityName
     */
    @SuppressWarnings("unchecked")
    public static PropertySet getPropertySet(String entityName, Long entityId) {
        if (propertySet == null) {
            JiraPropertySetFactory jiraPropertySetFactory = ComponentAccessor.getComponentOfType(JiraPropertySetFactory.class);
            propertySet = jiraPropertySetFactory.buildCachingPropertySet(entityName, entityId, true);
        }
        return propertySet;
    }

    private static ImmutableMap<String, ? extends Object> buildPropertySet(String entityName, Long entityId) {
        return ImmutableMap.of("delegator.name", "default", "entityName", entityName, "entityId", entityId);
    }

    public static ZephyrLicenseVerificationResult performLicenseValidation(ZephyrLicenseManager zLicenseManager) {
        I18nHelper i18nHelper = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
        ZephyrLicenseVerificationResult outList = new ZephyrLicenseVerificationResult();

        try {
            zLicenseManager.verify();
            outList.setValid(true);
        } catch (ZephyrLicenseException e) {
            log.fatal("Invalid license " + e.getMessage());
            outList.setValid(false);
            outList.setException(e);
            outList.setErrorMessage(i18nHelper.getText(e.getMessage()));
            URI forwardUri = null;
            try {
                forwardUri = zLicenseManager.getRedirectionUrl();
            } catch (Exception e1) {
                log.fatal("Error in getting redirection url, please check Zephyr license ", e1);
            }
            try {
                if (forwardUri == null) {
                    String baseUrl = ComponentAccessor.getWebResourceUrlProvider().getBaseUrl();
                    forwardUri = new URI(baseUrl + "/secure/admin/ZLicense.jspa");
                }
            } catch (URISyntaxException e1) {
                log.fatal("Error in setting default redirection url, please contact Zephyr Support ", e1);
            }
            outList.setForwardURI(forwardUri);
            outList.setGeneralMessage(i18nHelper.getText("zephyr.license.invalid.generalmessage", forwardUri.toString()));
        }
        return outList;
    }

    public static ConstantsManager getConstantsManager() {
        return ComponentAccessor.getConstantsManager();
    }

    public static IssueTypeSchemeManager getIssueTypeSchemeManager() {
        return ComponentAccessor.getComponentOfType(IssueTypeSchemeManager.class);
    }

    public static Collection<IssueType> getAllIssueTypes() {
        return ComponentAccessor.getConstantsManager().getAllIssueTypeObjects();
    }

    public static Collection<Status> getIssueStatusesForProject(Long projectID) {
        WorkflowManager wfmgr = ComponentAccessor.getWorkflowManager();
        IssueTypeSchemeManager issueTypeSchemeManager = ComponentAccessor.getIssueTypeSchemeManager();
        Collection<IssueType> issueTypeCollection = issueTypeSchemeManager.getIssueTypesForProject(ComponentAccessor.getProjectManager().getProjectObj(projectID));

        TreeSet<Status> statusList = new TreeSet<Status>(new JiraUtil.StatusComparator());
        for (IssueType iType : issueTypeCollection) {
            //if(!iType.isSubTask()){
            JiraWorkflow flow = wfmgr.getWorkflow(projectID, iType.getId());
            List<Status> list = flow.getLinkedStatusObjects();
            statusList.addAll(list);
            //}
        }

        return statusList;
    }

    /**
     * Public accessor to fetch executionStatuses
     *
     * @return hashmap containing status ID and status Object
     */
    public static Map<Integer, ExecutionStatus> getExecutionStatuses() {
        if (executionStatusCache == null) {
            CompatibilityCacheFactory cacheFactory = (CompatibilityCacheFactory) ZephyrComponentAccessor.getInstance().getComponent("compatibilityCacheFactory");
            executionStatusCache = cacheFactory.getCachedReference(JiraUtil.class, ConfigurationConstants.PLUGIN_KEY + ".executionStatus", new ExecutionStatusCacheLoader());
        }
        return executionStatusCache.get();
    }

    //Call this function every time there is a change (add/remove/update) in Execution Statuses.
    public static void buildExecutionStatusMap() {
        if (executionStatusCache != null) {
            executionStatusCache.reset();
        } else {
            getExecutionStatuses();
        }
    }

    public static ArrayList<ExecutionStatus> getExecutionStatusList() {
        String defaultStatusListString = getPropertySet(
                ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID).getText(
                ConfigurationConstants.ZEPHYR_EXECUTION_STATUSES);

        ArrayList<ExecutionStatus> statusList = new ArrayList<ExecutionStatus>();
        try {
            statusList = new ObjectMapper().readValue(defaultStatusListString, new TypeReference<ArrayList<ExecutionStatus>>() {
            });
        } catch (IOException ioe) {
            //Ignore the error and send empty statusList.
            log.error("Unable to get Schedule Execution Status List. Sending empty list");
            ioe.printStackTrace();
        }

        return statusList;
    }

    /**
     * Public accessor to fetch executionStatuses
     *
     * @return hashmap containing status ID and status Object
     */
    public static Map<Integer, ExecutionStatus> getStepExecutionStatuses() {
        if (stepExecutionStatusCache == null) {
            CompatibilityCacheFactory cacheFactory = (CompatibilityCacheFactory) ZephyrComponentAccessor.getInstance().getComponent("compatibilityCacheFactory");
            stepExecutionStatusCache = cacheFactory.getCachedReference(JiraUtil.class, ConfigurationConstants.PLUGIN_KEY + ".stepExecutionStatus", new StepExecutionStatusCacheLoader());
        }
        return stepExecutionStatusCache.get();
    }

    //Call this function every time there is a change (add/remove/update) in Execution Statuses.
    public static void buildStepExecutionStatusMap() {
        if (stepExecutionStatusCache != null) {
            stepExecutionStatusCache.reset();
        } else {
            getStepExecutionStatuses();
        }
    }

    public static ArrayList<ExecutionStatus> getStepExecutionStatusList() {
        String defaultStatusListString = getPropertySet(
                ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID).getText(
                ConfigurationConstants.ZEPHYR_STEP_EXECUTION_STATUSES);

        ObjectMapper mapper = new ObjectMapper();
        ArrayList<ExecutionStatus> statusList = new ArrayList<ExecutionStatus>();

        try {
            statusList = mapper.readValue(defaultStatusListString, new TypeReference<ArrayList<ExecutionStatus>>() {
            });
        } catch (IOException ioe) {
            //Ignore the error and send empty statusList.
            log.error("Unable to get Step Execution Status List. Sending empty list");
            ioe.printStackTrace();
        }

        return statusList;
    }

    public static Response buildErrorResponse(Response.Status status, String errorId, String errorDesc, String errorHtml) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(ApplicationConstants.ERROR_ID, errorId);
        map.put(ApplicationConstants.ERROR_DESC, errorDesc);
        map.put(ApplicationConstants.ERROR_DESC_HTML, errorHtml);
        JSONObject jsonResponse = new JSONObject(map);

        return Response.status(status).entity(jsonResponse.toString()).cacheControl(ZephyrCacheControl.never()).build();
    }

    public static String buildIconURL(String img, String ImageType) {
        StringBuilder sb = new StringBuilder(buildPluginDownloadPath());
        sb.append("/images/icons/");
        sb.append(img).append(ImageType);

        return sb.toString();
    }

    public static String buildPluginDownloadPath() {
        return "/download/resources/" + ConfigurationConstants.PLUGIN_KEY;
    }

    public static JiraContextTreeManager getTreeManager() {
        return ComponentAccessor.getComponentOfType(JiraContextTreeManager.class);
    }

    public static boolean hasGlobalRights(GlobalPermissionKey permission) {
        return ComponentAccessor.getGlobalPermissionManager().hasPermission(permission, getRemoteUser());
    }

    public static ApplicationUser getRemoteUser() {
        return ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
    }

    public static User getLoggedInUser(JiraAuthenticationContext authContext){
        ApplicationUser appUser = authContext.getLoggedInUser();
        if(appUser != null){
            return appUser.getDirectoryUser();
        }
        return null;
    }

    public static CustomFieldManager getCustomFieldManager() {
        return ComponentAccessor.getCustomFieldManager();
    }

    public static FieldScreenManager getFieldScreenManager() {
        return ComponentAccessor.getFieldScreenManager();
    }

//	 public static FieldScreenSchemeManager getFieldScreenSchemeManager(){
//		 return ComponentAccessor.getComponentOfType(FieldScreenSchemeManager.class);
//	 }

    public static PortalPageManager getPortalPageManager() {
        return ComponentAccessor.getComponentOfType(PortalPageManager.class);
    }
    //
    // public static PortalPageService getPortalPageService()
    // {
    // return ComponentManager.getInstance().getPortalPageService();
    // }

    public static UserPreferencesManager getUserPreferencesManager() {
        return ComponentAccessor.getUserPreferencesManager();
    }

    public static UserManager getUserManager() {
        return ComponentAccessor.getUserManager();
    }

    public static Hashtable<String, String> decodeFilterArgs(String filterArgument,
                                                             final String argumentSeparator,
                                                             final String keyValueSeparator) {
//		if (StringUtils.isBlank(filterArgument))
//			return null;

        Hashtable<String, String> map = null;
        String[] pairs = filterArgument.split(argumentSeparator);
        if (pairs != null && pairs.length < 0) {
            pairs = new String[1];
            pairs[0] = filterArgument;
        }
        for (String pair : pairs) {
            String[] value = pair.split(keyValueSeparator);
            if (value.length > 0) {
                if (map == null) {
                    map = new Hashtable<String, String>();
                }
                map.put(value[0], value[1]);
            }
        }
        return map;
    }

    /**
     * @return issueTypeId
     */
    public static String getTestcaseIssueTypeId() {
        return getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID).getString(ConfigurationConstants.ZEPHYR_ISSUETYPE_KEY);
    }

    public static Option<Boolean> getVersionCheck() {
        String versionShowFlag = getPropertySet(
                ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                .getString(ConfigurationConstants.ZEPHYR_DISPLAY_VERSION_SETTINGS);

        if (versionShowFlag == null)
            return Option.none(Boolean.class);
        if (versionShowFlag.toLowerCase().equals("true"))
            return Option.some(true);
        else
            return Option.some(false);
    }

    public static boolean showWorkflow() {

        String workflowShowFlag = getPropertySet(
                ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                .getString(ConfigurationConstants.ZEPHYR_DISPLAY_WORKFLOW_SETTINGS);

        if ((workflowShowFlag != null) && (workflowShowFlag.toLowerCase().equals("true")))
            return true;
        else
            return false;
    }

    public static Boolean isIssueToTestLinkingEnabled() {
        String linkIssueToTest = getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                .getString(ConfigurationConstants.ZEPHYR_LINK_ISSUE_TO_TESTCASE);
        if (StringUtils.isBlank(linkIssueToTest))
            return Boolean.TRUE;
        return Boolean.parseBoolean(linkIssueToTest);
    }

    public static Boolean isIssueToTestStepLinkingEnabled() {
        String linkIssueToTestStep = getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                .getString(ConfigurationConstants.ZEPHYR_LINK_TESTCASE_STEP_TO_ISSUE);
        if (StringUtils.isBlank(linkIssueToTestStep))
            return Boolean.FALSE;
        return Boolean.parseBoolean(linkIssueToTestStep);
    }


    public static String getTestcaseToRequirementLinkType() {
        String linkTestToReq = null;
        IssueLinkTypeManager issueLinkTypeManager = ComponentAccessor.getComponentOfType(IssueLinkTypeManager.class);
        String defaultRelationId = String.valueOf(JiraUtil.getSimpleDBProperty(ConfigurationConstants.ZEPHYR_REQ_TO_TEST_LINK_RELATION,""));
        if (StringUtils.isBlank(defaultRelationId)) {
            Option<Long> issueLinkType = getMatchingNonSystemLinkType(issueLinkTypeManager);
            if(issueLinkType.isDefined()) {
                linkTestToReq = String.valueOf(issueLinkType.get());
                JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                        .setString(ConfigurationConstants.ZEPHYR_REQ_TO_TEST_LINK_RELATION, String.valueOf(issueLinkType.get()));
            } else {
                log.error("Error saving default Requirement to Test Link Relation!!! Requirement to Test Linking is either not enabled or no IssueLink Types are found. " +
                        "Please contact JIRA administrator to enable Issue Linking and add appropriate IssueLink Types.");
            }
        } else {
            linkTestToReq = defaultRelationId;
        }
        return linkTestToReq;
    }

    public static String getTestcaseToRequirementOldLinkType() {
        String linkTestToReq = null;
        IssueLinkTypeManager issueLinkTypeManager = ComponentAccessor.getComponentOfType(IssueLinkTypeManager.class);
        String defaultRelationId = String.valueOf(JiraUtil.getSimpleDBProperty(ConfigurationConstants.ZEPHYR_REQ_TO_TEST_LINK_OLD_RELATION,""));
        if (StringUtils.isBlank(defaultRelationId)) {
            Option<Long> issueLinkType = getMatchingNonSystemLinkType(issueLinkTypeManager);
            if(issueLinkType.isDefined()) {
                linkTestToReq = String.valueOf(issueLinkType.get());
                JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                        .setString(ConfigurationConstants.ZEPHYR_REQ_TO_TEST_LINK_OLD_RELATION, String.valueOf(issueLinkType.get()));
            } else {
                log.error("Error saving in old link type for test to requirement. Requirement to Test Linking is either not enabled or no IssueLink Types are found. " +
                        "Please contact JIRA administrator to enable Issue Linking and add appropriate IssueLink Types.");
            }
        } else {
            linkTestToReq = defaultRelationId;
        }
        return linkTestToReq;
    }

    public static Boolean isTestToIssueLinkingEnabled() {
        String linkIssueToTest = getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                .getString(ConfigurationConstants.ZEPHYR_LINK_TESTCASE_TO_ISSUE);
        if (StringUtils.isBlank(linkIssueToTest))
            return Boolean.FALSE;
        return Boolean.parseBoolean(linkIssueToTest);
    }

    public static Boolean isIssueSecurityEnabled() {
        String isIssueSecurityEnabled = getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                .getString(ConfigurationConstants.ZEPHYR_ENABLE_ISSUE_SECURITY);
        if (StringUtils.isBlank(isIssueSecurityEnabled))
            return Boolean.FALSE;
        return Boolean.parseBoolean(isIssueSecurityEnabled);
    }

    public static Boolean isTestSummaryLabelsFilterDisabled() {
        String isTestSummaryLabelsFilterDisabled = getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                .getString(ConfigurationConstants.ZEPHYR_DISABLE_LABELS_TEST_SUMMARY_FILTER);
        if (StringUtils.isBlank(isTestSummaryLabelsFilterDisabled))
            return Boolean.FALSE;
        return Boolean.parseBoolean(isTestSummaryLabelsFilterDisabled);
    }

    public static Boolean isTestSummaryAllFiltersDisabled() {
        String isTestSummaryAllFilterDisabled = getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                .getString(ConfigurationConstants.ZEPHYR_DISABLE_ALL_TEST_SUMMARY_FILTER);
        if (StringUtils.isBlank(isTestSummaryAllFilterDisabled))
            return Boolean.FALSE;
        return Boolean.parseBoolean(isTestSummaryAllFilterDisabled);
    }

    public static Boolean isIssueToTestExecutionRemoteLinkingEnabled() {
        String remoteLinkIssueToTestExecution = getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                .getString(ConfigurationConstants.ZEPHYR_REMOTE_LINK_ISSUE_TO_EXECUTION);
        if (StringUtils.isBlank(remoteLinkIssueToTestExecution))
            return Boolean.TRUE;
        return Boolean.parseBoolean(remoteLinkIssueToTestExecution);
    }

    public static Boolean isIssueToTestStepExecutionRemoteLinkingEnabled() {
        String remoteLinkIssueToTestStepExecution = getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                .getString(ConfigurationConstants.ZEPHYR_REMOTE_LINK_ISSUE_TO_STEP_EXECUTION);
        if (StringUtils.isBlank(remoteLinkIssueToTestStepExecution))
            return Boolean.FALSE;
        return Boolean.parseBoolean(remoteLinkIssueToTestStepExecution);
    }

    public static Boolean isSystemPropertyEnabled(String entityName, Long entityId, String propertyName) {
        String dbPropertyValue = getPropertySet(entityName, entityId).getString(propertyName);
        if (StringUtils.isBlank(dbPropertyValue))
            return Boolean.FALSE;
        return Boolean.parseBoolean(dbPropertyValue);
    }

    public static String getZephyrLogLevel() {
        String logLevel = getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                .getString(ConfigurationConstants.ZEPHYR_LOG_LEVEL_INFO);
        if (StringUtils.isBlank(logLevel))
            return Level.INFO.toString();
        return logLevel;
    }

    public static String getZephyrLogMaxSize() {
        String logMaxSize = getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                .getString(ConfigurationConstants.ZEPHYR_LOG_MAX_SIZE);
        if (logMaxSize == null)
            return ConfigurationConstants.ZEPHYR_LOG_DEFAULT_MAX_SIZE;
        return logMaxSize;
    }

    public static String getZephyrLogMaxBackup() {
        String logMaxSize = getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                .getString(ConfigurationConstants.ZEPHYR_LOG_MAX_BACKUP);
        if (logMaxSize == null || logMaxSize.equals(""))
            return ConfigurationConstants.ZEPHYR_LOG_DEFAULT_MAX_BACKUP;
        return logMaxSize;
    }

    public static void setZephyrLogLevel(String logLevel) {
        getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                .setString(ConfigurationConstants.ZEPHYR_LOG_LEVEL_INFO, logLevel);
    }

    public static void setZephyrLogMaxSize(String logMaxSize) {
        getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                .setString(ConfigurationConstants.ZEPHYR_LOG_MAX_SIZE, logMaxSize);
    }

    public static void setZephyrLogMaxBackup(String logMaxBackup) {
        getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                .setString(ConfigurationConstants.ZEPHYR_LOG_MAX_BACKUP, logMaxBackup);
    }

    public static Boolean isDarkFeatureEnabled(String featureKey) {
        return isSystemPropertyEnabled(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID, featureKey);
    }
    
    public static void enablePluginModuleByKey(String pluginModuleKey) throws Exception {
        ComponentAccessor.getPluginController().enablePluginModule(pluginModuleKey);
    }

    public static void disablePluginModuleByKey(String pluginModuleKey) throws Exception {
        ComponentAccessor.getPluginController().disablePluginModule(pluginModuleKey);
    }

    public static PortalPage getTestMetricsDashboard() {

        String dashboardId = getPropertySet(
                ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                .getString(ConfigurationConstants.ZEPHYR_DASHBOARD_KEY);

        Long dashId = null;

        if (null != dashboardId) {
            try {
                dashId = Long.parseLong(dashboardId);
                PortalPageManager pageManager = getPortalPageManager();
                return pageManager.getPortalPageById(dashId);
            } catch (NumberFormatException nfe) {
                log.error("Invalid dashboardId, can't be formatted as Number." + nfe);
            }
        }
        return null;
    }

    /**
     * Checks if underlying JIRA version is 5.0. Caches this value for the life of this plugin
     */
    public static Boolean isJIRA50() {
        if (jira50 == null) {
            try {
                BuildUtilsInfo buildUtils = new BuildUtilsInfoImpl();
                int[] versions = Arrays.copyOf(buildUtils.getVersionNumbers(), 2);
                int[] v50x = {5, 0};
                jira50 = Ints.lexicographicalComparator().compare(versions, v50x) == 0;
            } catch (Exception ex) {
                log.fatal("Unable to determine JIRA Version, If you are running JIRA lower version than 5.1, this may cause problems", ex);
            }
        }
        return jira50;
    }

    /**
     * Checks if underlying JIRA version is > 6.3. Caches this value for the life of this plugin
     */
    public static Boolean isJIRAGreaterThan63() {
        if (jira63 == null) {
            try {
                BuildUtilsInfo buildUtils = new BuildUtilsInfoImpl();
                int[] versions = Arrays.copyOf(buildUtils.getVersionNumbers(), 2);
                int[] v63x = {6, 3};
                jira63 = Ints.lexicographicalComparator().compare(versions, v63x) >= 0;
            } catch (Exception ex) {
                log.fatal("Unable to determine JIRA Version, If you are running JIRA lower version than 5.2, this may cause problems", ex);
            }
        }
        return jira63;
    }

    /**
     * Checks if underlying JIRA version is > 6.3. Caches this value for the life of this plugin
     */
    public static Boolean isJIRAOlderThan64() {
        if (jiraOlderThan64 == null) {
            try {
                BuildUtilsInfo buildUtils = new BuildUtilsInfoImpl();
                int[] versions = Arrays.copyOf(buildUtils.getVersionNumbers(), 2);
                int[] v64x = {6, 4};
                jiraOlderThan64 = Ints.lexicographicalComparator().compare(versions, v64x) < 0;
            } catch (Exception ex) {
                log.fatal("Unable to determine JIRA Version. This may cause issues in showing Summary & Cycle Panels ", ex);
            }
        }
        return jiraOlderThan64;
    }

    /**
     * Checks if underlying JIRA version is > 7.10. Caches this value for the life of this plugin
     */
    public static Boolean isJIRAGreaterThan710() {
        if (jira710 == null) {
            try {
                BuildUtilsInfo buildUtils = new BuildUtilsInfoImpl();
                int[] versions = Arrays.copyOf(buildUtils.getVersionNumbers(), 2);
                int[] v710x = {7, 10};
                jira710 = Ints.lexicographicalComparator().compare(versions, v710x) >= 0;
            } catch (Exception ex) {
                log.fatal("Unable to determine JIRA Version. This may cause issues in showing Summary & Cycle Panels ", ex);
            }
        }
        return jira710;
    }

    /**
     * Is ZFJ version >= given version
     *
     * @param currentVersion
     * @param expectedMajorVersion
     * @param expectedMinorVersion
     * @return true if current version is >= given version, else returns false
     */
    public static Boolean isZFJVersionGreaterThanEqualsTo(String currentVersion, int expectedMajorVersion, int expectedMinorVersion) {
        if (StringUtils.isBlank(currentVersion))
            return false;

        try {
            String[] tokens = currentVersion.split("\\.");
            int[] results = new int[2];
            for (int i = 0; i < 2; i++) {
                results[i] = Integer.parseInt(tokens[i]);
            }

            int[] versions = Arrays.copyOf(results, 2);
            int[] expectedVersion = {expectedMajorVersion, expectedMinorVersion};
            return Ints.lexicographicalComparator().compare(versions, expectedVersion) >= 0;
        } catch (Exception ex) {
            log.error("Invalid ZFJ version " + currentVersion + " " + ex.getMessage());
        }
        return false;
    }

    /**
     * @param propertyKey
     * @param defaultPropertyValue
     * @return
     */
    public static Object getSimpleDBProperty(final String propertyKey, Object defaultPropertyValue) {
        Object value = null;
        try {
            value = JiraUtil.getPropertySet(
                    ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID).getAsActualType(propertyKey);
        } catch (PropertyException e) {
            if(!StringUtils.isBlank(propertyKey) && !StringUtils.equalsIgnoreCase(propertyKey,"zephyr.zql.search.result.max")) {
                log.info("No such key " + propertyKey + " found , default: " + defaultPropertyValue.toString());
            }
        }
        if (value == null) {
            value = defaultPropertyValue;
        }
        return value;
    }

    /**
     * Checks if JIRA is running in dev mode
     *
     * @return
     */
    public static Boolean isDevMode() {
        return new Boolean(System.getProperty("atlassian.dev.mode", "false"));
    }

    /**
     * Checks to see if Project exists and has Browse Permission
     *
     * @param projectId
     * @return
     */
    public static boolean hasBrowseProjectPermission(Long projectId, ApplicationUser user) {
        Project project = ComponentAccessor.getProjectManager().getProjectObj(projectId);
        if (project == null) {
            return false;
        }

        // checking the project browse permissions
        if (!ComponentAccessor.getPermissionManager().hasPermission(ProjectPermissions.BROWSE_PROJECTS, project, user)) {
            return false;
        }
        return true;
    }


    /**
     * Checks to see if Project exists and has Browse Permission
     * @param project
     * @param user
     * @return
     */
    public static boolean hasBrowseProjectPermission(Project project, ApplicationUser user) {
        if (project == null) {
            return false;
        }

        // checking the project browse permissions
        if (!ComponentAccessor.getPermissionManager().hasPermission(ProjectPermissions.BROWSE_PROJECTS, project, user)) {
            return false;
        }
        return true;
    }


    /**
     * Checks to see if User has access to view the issue (Changes for IssueSecurity)
     *
     * @param issue
     * @param user
     * @return
     */
    public static boolean hasIssueViewPermission(Long issueId, Issue issue, ApplicationUser user) {
        if(!isIssueSecurityEnabled()) {
            return true;
        }
        if (issue == null) {
            if(issueId == null){
                return true;
            }
            issue = ComponentAccessor.getIssueManager().getIssueObject(issueId);
        }

        try {
            if (!ComponentAccessor.getPermissionManager().hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue, user)) {
                return false;
            }
        } catch(Exception e) {
            String errorMessage = String.format("Error checking JIRA permission for Issue %s. Defaulting to true.",issue.getKey());
            log.error(String.format(ERROR_LOG_MESSAGE,Response.Status.BAD_REQUEST.getStatusCode(), Response.Status.BAD_REQUEST,errorMessage));
        }

        return true;
    }


    /**
     * Checks to see if Project exists and has Browse Permission
     * @param permissionTypes
     * @param loggedInUser
     * @param project
     * @return
     */
    public static boolean hasZephyrPermissions(Collection<PermissionType> permissionTypes,ApplicationUser loggedInUser, Project project) {
        if (project == null) {
            return false;
        }

        for(PermissionType permissionType : permissionTypes) {
	        ProjectPermissionKey projectPermissionKey = new ProjectPermissionKey(permissionType.toString());
	        if(ComponentAccessor.getPermissionManager().hasPermission(projectPermissionKey, project, loggedInUser)) {
	        	return true;
	        }
        }
        return false;
    }
    
    /**
     * Checks to see if Anonymous Permission exists
     *
     * @param user
     * @param project
     * @return
     */
    public static boolean hasAnonymousPermission(ApplicationUser user, Project project) {
        return ComponentAccessor.getPermissionManager().hasPermission(ProjectPermissions.BROWSE_PROJECTS, project, user);
    }


    /**
     * Checks to see if Anonymous Permission exists
     *
     * @param user
     * @return
     */
    public static boolean hasAnonymousPermission(ApplicationUser user) {
        boolean hasAdministerRights = ComponentAccessor.getGlobalPermissionManager().hasPermission(GlobalPermissionKey.ADMINISTER, user);
        boolean hasSystemRights = ComponentAccessor.getGlobalPermissionManager().hasPermission(GlobalPermissionKey.SYSTEM_ADMIN, user);
        boolean hasBrowseRights = ComponentAccessor.getGlobalPermissionManager().hasPermission(GlobalPermissionKey.USE, user);

        if (!hasAdministerRights && !hasSystemRights && !hasBrowseRights && user == null) {
            return ComponentAccessor.getPermissionManager().hasProjects(ProjectPermissions.BROWSE_PROJECTS, user);
        }
        return hasAdministerRights || hasSystemRights || hasBrowseRights;
    }


    /**
     * Checks to see if Anonymous Permission exists ( JIRA 6.4)
     *
     * @param user
     * @param project
     * @return boolean
     */
    /*
    public static boolean hasAnonymousPermission(ApplicationUser user, Project project) {
        boolean hasAdministerRights = ComponentAccessor.getPermissionManager().hasPermission(ProjectPermissions.ADMINISTER_PROJECTS, project, user);
        boolean hasBrowseRights = ComponentAccessor.getPermissionManager().hasPermission(ProjectPermissions.BROWSE_PROJECTS, project, user);

        if (!hasAdministerRights && !hasBrowseRights) {
            return false;
        }
        return hasAdministerRights || hasBrowseRights;
    }
   */

    /**
     * Checks to see if logged in user has System Admin permission ( JIRA 6.4)
     *
     * @param user
     * @return boolean
     */
    public static boolean hasSystemAdminPermission(ApplicationUser user) {
        return ComponentAccessor.getGlobalPermissionManager().hasPermission(Permissions.ADMINISTER, user);
    }

    /**
     * Checks to see if logged in user has Project Admin permission ( JIRA 6.4)
     *
     * @param project
     * @param user
     * @return boolean
     */
    public static boolean hasProjectAdminPermission(Project project, ApplicationUser user) {
        return ComponentAccessor.getPermissionManager().hasPermission(Permissions.PROJECT_ADMIN, project, user);
    }

    public static boolean isCaseSensitiveDatabaseType(String databaseType) {
        return StringUtils.startsWithIgnoreCase(databaseType, "postgres") || StringUtils.startsWithIgnoreCase(databaseType, "oracle")
                || StringUtils.startsWithIgnoreCase(databaseType, "hsql");
    }

    public static void resetCache() {
        executionStatusCache = null;
        stepExecutionStatusCache = null;
    }

    public static Boolean getZephyrUpdateExecutionExecutedonFlag() {
        String updateExecutionExecutedonFlag = getPropertySet(
                ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                .getString(ConfigurationConstants.ZEPHYR_UPDATE_EXECUTION_EXECUTED_ON);

        if (StringUtils.isBlank(updateExecutionExecutedonFlag))
            return Boolean.FALSE;
        return Boolean.parseBoolean(updateExecutionExecutedonFlag);
    }

    /**
     * This method validates whether client browser whether is IE or not.
     * Returns true if the client browser is flavour of IE.
     * @param browserDetails
     * @return
     */
    public static Boolean isIeBrowser(String browserDetails) {
        final String browser = browserDetails.toLowerCase();

        if (browser.contains("msie") || browser.contains("trident")
                || browser.contains("edge")) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    static class StatusComparator implements Comparator<Status> {

        public int compare(Status c1, Status c2) {
            int nameOrder = c1.getName().compareTo(c2.getName());
            return nameOrder;
        }
    }

    private static class ExecutionStatusCacheLoader implements Supplier<Map<Integer, ExecutionStatus>> {
        @Override
        public Map<Integer, ExecutionStatus> get() {
            ArrayList<ExecutionStatus> currentExecStatusList = getExecutionStatusList();
            Map<Integer, ExecutionStatus> statusMap = new TreeMap<Integer, ExecutionStatus>();

            for (ExecutionStatus execStatusObj : currentExecStatusList) {
                statusMap.put(execStatusObj.getId(), execStatusObj);
            }
            return statusMap;
        }
    }

    private static class StepExecutionStatusCacheLoader implements Supplier<Map<Integer, ExecutionStatus>> {
        @Override
        public Map<Integer, ExecutionStatus> get() {
            ArrayList<ExecutionStatus> currentStepExecStatusList = getStepExecutionStatusList();
            Map<Integer, ExecutionStatus> statusMap = new HashMap<Integer, ExecutionStatus>();

            for (ExecutionStatus execStatusObj : currentStepExecStatusList) {
                statusMap.put(execStatusObj.getId(), execStatusObj);
            }
            return statusMap;
        }
    }

    public static <T> List<T> safe(List<T> other) {
        return null == other ? Collections.EMPTY_LIST : other;
    }

    public static <T> Collection<T> safe(Collection<T> other) {
        return null == other ? CollectionUtils.EMPTY_COLLECTION : other;
    }

	public static Boolean isAssociatedIssueTypeTestToProjectCreate() {
        String addIssueTypeTestToProject = getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                .getString(ConfigurationConstants.ZEPHYR_ISSUE_TYPE_TEST_PROJECT_CREATE);
        if (StringUtils.isBlank(addIssueTypeTestToProject))
            return Boolean.TRUE;
        return Boolean.parseBoolean(addIssueTypeTestToProject);
	}

    /**
     * Method to check whether zephyr permission scheme has been enabled.
     * @return
     */
    public static Boolean getPermissionSchemeFlag() {
        String permissionShowFlag = getPropertySet(
                ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                .getString(ConfigurationConstants.ZEPHYR_DISPLAY_PERMISSION_SETTINGS);

        if (StringUtils.isBlank(permissionShowFlag))
            return Boolean.FALSE;
        return Boolean.parseBoolean(permissionShowFlag);
    }

    /**
     * Method to check whether zephyr analytics scheme has been enabled.
     * @return
     */
    public static Boolean getZephyrAnalyticsFlag() {
        String analyticsEnabled = getPropertySet(
                ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                .getString(ConfigurationConstants.ZEPHYR_ANALYTICS_SETTINGS);

        if (StringUtils.isBlank(analyticsEnabled))
            return Boolean.TRUE;
        return Boolean.parseBoolean(analyticsEnabled);
    }

    /**
     *
     * @return
     */
    public static String getNonIssueTypeTestSaveList() {

        //To avoid character limit exceed exception, moved the implementation to setText() method.
        // Fetching the data from getText() method, if it doesn't contain any value then fetch from getString() method.
        String nonIssueTypeTestSavedListValue = JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                .getText(ConfigurationConstants.ZEPHYR_SHOW_CREATE_ZEPHYR_TEST_ON_ISSUE_TYPES);
        if(StringUtils.isEmpty(nonIssueTypeTestSavedListValue)){
            nonIssueTypeTestSavedListValue = JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                    .getString(ConfigurationConstants.ZEPHYR_SHOW_CREATE_ZEPHYR_TEST_ON_ISSUE_TYPES);
        }
        return nonIssueTypeTestSavedListValue;
    }

//    public static void checkAndCreateReqToTestTypeRelation(Boolean forceCreate) {
//        Option<Long> reqToTestLinkTypeId = getRequirementToTestLinkTypeId();
//        //Check if this linkType exists
//        IssueLinkTypeManager issueLinkTypeManager = ComponentAccessor.getComponentOfType(IssueLinkTypeManager.class);
//        IssueLinkType issueLinkType;
//        if(!reqToTestLinkTypeId.isDefined() || forceCreate){
//            /*To avoid duplicate*/
//            Collection<IssueLinkType> issueLinkTypeCol = issueLinkTypeManager.getIssueLinkTypesByName(ConfigurationConstants.ZEPHYR_REQ_TO_TEST_LINK_NAME);
//            if(issueLinkTypeCol == null || issueLinkTypeCol.size() < 1) {
//                try {
//                    issueLinkTypeManager.createIssueLinkType(ConfigurationConstants.ZEPHYR_REQ_TO_TEST_LINK_NAME, ConfigurationConstants.ZEPHYR_REQ_TO_TEST_LINK_OUTWARD, ConfigurationConstants.ZEPHYR_REQ_TO_TEST_LINK_INWARD, "Z");
//                } catch (Exception ex) {
//                    log.warn("Error in creating test IssueLinkType " + ex);
//                }
//                issueLinkTypeCol = issueLinkTypeManager.getIssueLinkTypesByName(ConfigurationConstants.ZEPHYR_REQ_TO_TEST_LINK_NAME);
//            }
//
//            assert issueLinkTypeCol.size() > 0;
//            issueLinkType = issueLinkTypeCol.iterator().next();
//        }else{
//            issueLinkType = issueLinkTypeManager.getIssueLinkType(reqToTestLinkTypeId.get());
//        }
//         /* Update our DB with Test link type */
//        assert issueLinkType != null;
//        saveReqTestIssueLinkTypeId(issueLinkType);
//    }
//
//    private static void saveReqTestIssueLinkTypeId(IssueLinkType issueLinkType) {
//        JiraUtil.getPropertySet(
//                ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID).setLong(ConfigurationConstants.ZEPHYR_REQ_TO_TEST_LINK_RELATION, issueLinkType.getId());
//    }
//
//    public static Option<Long> getRequirementToTestLinkTypeId() {
//        Object id = JiraUtil.getSimpleDBProperty(ConfigurationConstants.ZEPHYR_REQ_TO_TEST_LINK_RELATION, null);
//        try {
//            if (id != null) {
//                return Option.some(Long.parseLong(id.toString()));
//            }
//        }catch(Exception ex){
//            log.error("Wrong value for Issue --> Test issueLinkType, return empty. This will prompt system to create new issueLinkType." , ex);
//        }
//        return Option.none();
//    }

	
    public static Project getProjectThreadLocal() {
        return PROJECT_THREAD_LOCAL.get();
    }
    
    public static void setProjectThreadLocal(Project project) {
        PROJECT_THREAD_LOCAL.set(project);
    }
    
    public static boolean isTestManagementForProjects() {
    	return true;
    }
    
	/**
	 * @return 
	 */
	public static Response getPermissionDeniedErrorResponse(String errorMessage) {
		JSONObject errorJsonObject = null;
		try {
			errorJsonObject = new JSONObject();
            // build error map
            errorJsonObject.put("PERM_DENIED", errorMessage);
            Response.ResponseBuilder builder = Response.status(Response.Status.FORBIDDEN);
            builder.type(MediaType.APPLICATION_JSON);
            builder.entity(errorJsonObject.toString());
            return builder.build();
		} catch(JSONException e) {
			log.error("Error creating JSON Object",e);
		}
		return Response.status(javax.ws.rs.core.Response.Status.BAD_REQUEST).entity(errorJsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
	}

    public static String maskIssueKey(Issue issue) {
        String[] issueKeyArr = StringUtils.split(issue.getKey(), "-");
        String maskedKey = issueKeyArr.length > 0 ? issueKeyArr[0] : "";
        return maskedKey + "-" + ApplicationConstants.MASKED_DATA;
    }

    private static Option<Long> getMatchingNonSystemLinkType(IssueLinkTypeManager issueLinkTypeManager){
        Collection<IssueLinkType> issueLinkTypes = issueLinkTypeManager.getIssueLinkTypes();
        if (CollectionUtils.isEmpty(issueLinkTypes)) {
            return Option.none();
        }
        IssueLinkType matchedIssueLink = null;
        for(IssueLinkType issueLinkType : issueLinkTypes){
            if(issueLinkType.isSystemLinkType())
                continue;
            if(matchedIssueLink == null)
                matchedIssueLink = issueLinkType;
            //get approx match
            if(StringUtils.containsIgnoreCase(issueLinkType.getName(), "relate")){
                matchedIssueLink = issueLinkType;
            }
            //Check exact match
            if(StringUtils.equals(issueLinkType.getName(), "Relates")){
                matchedIssueLink = issueLinkType;
                break;
            }
        }
        return Option.some(matchedIssueLink.getId());
    }

    /**
     * Return true if the projectId is added to the disabled projects
     * @param projectId
     * @return
     */
	public static boolean isTestMenuDisabled(String projectId){
    	String value = getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
    			.getText(ConfigurationConstants.ZEPHYR_DISABLE_PROJECT_TESTMENU);
    	if(StringUtils.isBlank(value)){
    		//Below code is required as the config has been moved from String to Text
    		value = getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
        			.getString(ConfigurationConstants.ZEPHYR_DISABLE_PROJECT_TESTMENU);
    	}
    	if(StringUtils.isNotBlank(value) && value.indexOf(projectId) > 0){
    		return Boolean.TRUE;
    	}
    	return Boolean.FALSE;
    }

    /**
     * Gives the all projectIds as List<String> from the configuration.
     * @return
     */
    public static List<String> getDisabledProjectIdsList(String configuration){
        String value = getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                .getText(configuration);
        List<String> newList = new ArrayList<>();
        if(org.apache.commons.lang3.StringUtils.isNotBlank(value)){
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
     * @param pingParams
     */
    public static void populateLicParams(List<NameValuePair> pingParams) {
        ZephyrLicenseManager licManager = (ZephyrLicenseManager) ZephyrComponentAccessor.getInstance().getComponent("zephyr-je-Licensemanager");
        PluginLicense pluginLic = licManager.getZephyrMarketplaceLicense();
        if(pluginLic != null){
            pingParams.add(new NameValuePair("custId", pluginLic.getOrganization().getName()));
            pingParams.add(new NameValuePair("licenseId", pluginLic.getSupportEntitlementNumber().getOrElse(pluginLic.getServerId())));
            pingParams.add(new NameValuePair("licSrc", "MP"));
            pingParams.add(new NameValuePair("licType", pluginLic.getLicenseType().name()));
        } else {
            ZephyrLicense lic = licManager.getLicense();
            if(lic != null){
                pingParams.add(new NameValuePair("custId", lic.getOrganisationId()));
                pingParams.add(new NameValuePair("licenseId", lic.getLicenseId()));
                pingParams.add(new NameValuePair("licSrc", "ZEP"));
                pingParams.add(new NameValuePair("licType", lic.getLicenseType().name()));
            }else{
                log.fatal(" ZEPHYR FOR JIRA License is not installed");
            }
        }
    }

    /**
     * This method will get the user customization preference object from jira db.
     * @param searchKey
     * @return
     */
    public static String getUserCustomizationPreferenceByKey(String searchKey) {
        String userCustomizationPreference = getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                .getText(searchKey);

        if(StringUtils.isNotBlank(userCustomizationPreference)) {
            return userCustomizationPreference;
        }

        return null;
    }

    /**
     *
     * @param key
     * @param userCustomizationPreference
     * @return
     */
    public static String updateUserCustomizationPreferenceByKey(String key, String userCustomizationPreference) {
        getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID).setText(key,userCustomizationPreference);
        return getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                .getText(key);
    }

    /**
     *
     * @param projectId
     * @return
     */
    public static boolean getExecutionWorkflowEnabled(Long projectId) {
        List<String> disabledProjectIdsList = getDisabledProjectIdsList(ConfigurationConstants.ZEPHYR_DISABLE_PROJECT_EXEC_WORKFLOW);
        boolean isExecutionWorkflowEnabled = Boolean.TRUE;
        if(CollectionUtils.isNotEmpty(disabledProjectIdsList)) {
            if(disabledProjectIdsList.contains(String.valueOf(projectId))) {
                isExecutionWorkflowEnabled = Boolean.FALSE;
            }
        }
        return isExecutionWorkflowEnabled;
    }

    public static String getInAppMessageUrl() {
        final PluginAccessor pluginAccessor = ComponentAccessor.getPluginAccessor();
        Plugin plugin = pluginAccessor.getEnabledPlugin(ApplicationConstants.ZFJ_PLUGIN_KEY);
        Properties properties = loadProperties(BUILD_PROPERTIES_RESOURCE, pluginLoader(plugin));
        String inAppMessageUrl = StringUtils.EMPTY;

        if(null != properties) {
            inAppMessageUrl = properties.getProperty("inappmessage.url");
        }
        return inAppMessageUrl;
    }

    public static String getAnalyticUrl() {
        final PluginAccessor pluginAccessor = ComponentAccessor.getPluginAccessor();
        Plugin plugin = pluginAccessor.getEnabledPlugin(ApplicationConstants.ZFJ_PLUGIN_KEY);
        Properties properties = loadProperties(BUILD_PROPERTIES_RESOURCE, pluginLoader(plugin));
        String analyticUrl = StringUtils.EMPTY;

        if(null != properties) {
            analyticUrl = properties.getProperty("analytic.url");
        }
        return analyticUrl;
    }

    /**
     * Load the property from the resource.
     * @param path
     * @param loader
     * @return
     */
    private static Properties loadProperties(String path, Function<String, InputStream> loader) {
        final InputStream is = notNull(String.format("Resource %s not found", path), loader.apply(path));
        try {
            final Properties props = new Properties();
            props.load(is);
            return props;
        } catch (IOException e) {
            log.error("Unable to load inapp message url." + path, e);
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException io) {
                    log.error("Unable to close input stream.", io);
                }
            }
        }
        return null;
    }

    private static Function<String, InputStream> pluginLoader(final Plugin plugin) {
        return new Function<String, InputStream>() {
            @Override
            public InputStream apply(@Nullable String name) {
                return plugin.getResourceAsStream(name);
            }
        };
    }
    
    
    public static String setBackupRecoveryByKey(String key, String expressionJson) {
        getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID).setText(key,expressionJson);
        return getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                .getText(key);
    }
    
    public static String getBackupRecoveryByKey(String key) {
        return getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                .getText(key);
    }

}
