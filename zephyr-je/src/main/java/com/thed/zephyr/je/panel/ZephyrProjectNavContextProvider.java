package com.thed.zephyr.je.panel;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.atlassian.plugin.webresource.WebResourceManager;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.bc.project.component.ProjectComponentManager;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.datetime.DateTimeFormatUtils;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.label.LabelManager;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.jql.util.JqlStringSupportImpl;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.velocity.VelocityRequestContextFactory;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.query.Query;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.thed.zephyr.je.attachment.SessionKeys;
import com.thed.zephyr.je.config.license.ZephyrLicenseManager;
import com.thed.zephyr.je.config.license.ZephyrLicenseVerificationResult;
import com.thed.zephyr.je.service.ScheduleManager;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.JiraUtil;
import com.atlassian.sal.api.ApplicationProperties;
import com.thed.zephyr.util.ZephyrComponentAccessor;

import webwork.action.ActionContext;

/**
 * Created by mukul on 1/13/15.
 */
public class ZephyrProjectNavContextProvider implements ContextProvider {

    protected final Logger log = Logger.getLogger(ZephyrProjectNavContextProvider.class);
    private final VersionManager versionManager;
    private final SearchProvider searchProvider;
    private final LabelManager labelManager;
    private final I18nHelper i18n;
    private final ProjectComponentManager projComponentManager;
    private final ScheduleManager schedulemanager;
    protected VelocityRequestContextFactory velocityRequestContextFactory;
    private final ZephyrLicenseManager zLicenseManager;

    private final WebResourceManager webResourceManager;

    public ZephyrProjectNavContextProvider(VersionManager versionManager, ProjectComponentManager componentManager,
                                           final ScheduleManager schedManager, SearchProvider searchProvider, final LabelManager lm, 
                                           final VelocityRequestContextFactory velocityRequestContextFactory, 
                                           final ZephyrLicenseManager zLicenseManager, final WebResourceManager webResourceManager) {
        this.versionManager = versionManager;
        this.searchProvider = searchProvider;
        this.labelManager = lm;
        this.projComponentManager = componentManager;
        this.schedulemanager = schedManager;
        this.i18n = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
        this.velocityRequestContextFactory = velocityRequestContextFactory;
        this.zLicenseManager = zLicenseManager;
        this.webResourceManager = webResourceManager;
    }

    @Override
    public void init(Map<String, String> params) throws PluginParseException {
    }

    @Override
    public Map<String, Object> getContextMap(Map<String, Object> context) {
        Project project = (Project) Preconditions.checkNotNull(context.get("project"));
        boolean isPermissionSchemeEnabled = JiraUtil.getPermissionSchemeFlag();

        webResourceManager.requireResource("com.thed.zephyr.je:zephyr-project-dashboard-resources-tree");

        if(isPermissionSchemeEnabled) {
	        ApplicationUser user = context.get("user") != null ? (ApplicationUser) context.get("user") : null;
	        return this.getContextMap(project, user, context);
        } else {
	        ApplicationUser user = (ApplicationUser) Preconditions.checkNotNull(context.get("user"));
	        return this.getContextMap(project, user, context);
        }
    }

    protected Map<String, Object> getContextMap(Project project, ApplicationUser user, Map<String, Object> ctx) {
        Map<String, Object> paramMap = ctx;

        //First getHtml(BrowseContext ctx) gets called. In that we are setting licenseErrorMessage.
        //If this error Message is present then we shouldn't build this panel instead throw error.
        //so skip the details of this method and return empty parampMap!
        String licenseErrorMessage = (String) ActionContext.getRequest().getAttribute("errors");

    	boolean isTestManagementForProjects = JiraUtil.isTestManagementForProjects();
		if(!isTestManagementForProjects) {
            paramMap.put("errors", ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error"));
            return paramMap;
    	}
        
        if ((project != null) && (licenseErrorMessage == null)) {
            //license is invalid, no need to fetch any data from backend.
            long totalTests = 0;
            
            long totalUnscheduledTests = 0;
            Long projectId = project.getId();
            
            /* PROJECT SUMMARY */
            String typeId = JiraUtil.getTestcaseIssueTypeId();
            IssueType testType = JiraUtil.getConstantsManager().getIssueTypeObject(typeId);
            JqlClauseBuilder builder = JqlQueryBuilder.newClauseBuilder();
            builder.project(new String[]{project.getId().toString()}).and().issueType(testType.getId());
            Query query = builder.buildQuery();
            try {
                totalTests = searchProvider.searchCount(query, user);
            } catch (SearchException e) {
                e.printStackTrace();
            }
            paramMap.put("totalTestsByProjectUrl", "/secure/IssueNavigator.jspa?reset=true&jqlQuery=PROJECT='" + project.getKey() + "' AND ISSUETYPE=" + testType.getId());
            paramMap.put("totalTestsByProjCnt", totalTests);
       
            String encPrj = project.getKey();
            try {
                encPrj = URLEncoder.encode("PROJECT='" + project.getKey() + "' AND executionStatus != " + ApplicationConstants.UNEXECUTED_STATUS, "UTF-8").replace("+", "%20");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            paramMap.put("totalExecutionsByProjCntUrl", "/secure/enav/#?query=" + encPrj);
            paramMap.put("totalExecutionsByProjCnt", schedulemanager.getTestcaseExecutionCount(null, project.getId()));
            
			/* Added for ZFJ-1213 */
			Integer totatTestsExecuted = schedulemanager.getScheduleCountByProjectIdAndGroupby(projectId.intValue(),false);
            totalUnscheduledTests = totalTests - totatTestsExecuted;
            paramMap.put("totalUnscheduledTestsCnt",  totalUnscheduledTests > 0 ? totalUnscheduledTests : 0 );

            Integer totatUnexecutedTestsByProjCnt = schedulemanager.getScheduleCountByProjectIdAndGroupby(projectId.intValue(),true);
            try {
                encPrj = URLEncoder.encode("PROJECT='" + project.getKey() + "' AND executionStatus = " + ApplicationConstants.UNEXECUTED_STATUS, "UTF-8").replace("+", "%20");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            paramMap.put("totatUnexecutedTestsByProjCntUrl", "/secure/enav/#?query=" + encPrj);
            paramMap.put("totatUnexecutedTestsByProjCnt",  totatUnexecutedTestsByProjCnt > 0 ? totatUnexecutedTestsByProjCnt : 0 );

			/* ** TEST BY VERSION ** */
//            List<Map<String, String>> versionList = new ArrayList<Map<String, String>>();
//            if(!JiraUtil.isTestSummaryAllFiltersDisabled()) {
//                List<Version> versions = versionManager.getVersions(project.getId());
//                populateVersionMap(project, user, testType, builder, versionList, i18n.getText("zephyr.je.version.unscheduled"), null);    //TOdo: implement i18n for this
//                for (Version ver : versions) {
//                    populateVersionMap(project, user, testType, builder, versionList, ver.getName(), ver.getId());
//                }
//                // sort the versionList
//                sortList(versionList);
//            }
//            paramMap.put("versions", versionList);

        	/* **  TEST BY COMPONENT ** */
//            List<Map<String, String>> componentList = new ArrayList<Map<String, String>>();
//            if(!JiraUtil.isTestSummaryAllFiltersDisabled()) {
//                Collection<ProjectComponent> compoenents = projComponentManager.findAllForProject(project.getId());
//                populateComponentMap(project, user, testType, builder, componentList, i18n.getText("zephyr.je.component.nocomponent"), null);    //TOdo: implement i18n for this
//                for (ProjectComponent comp : compoenents) {
//                    populateComponentMap(project, user, testType, builder, componentList, comp.getName(), comp.getId());
//                }
//                // sort the componentList
//                sortList(componentList);
//            }
//            paramMap.put("components", componentList);

        	/* TEST BY LABEL */
//            List<Map<String, String>> labelList = new ArrayList<Map<String, String>>();
//            if(!JiraUtil.isTestSummaryAllFiltersDisabled() &&  !JiraUtil.isTestSummaryLabelsFilterDisabled()) {
//                try {
//                    populateLabelMap(project, user, testType, builder, labelList, null);
//                    Set<String> labels = labelManager.getSuggestedLabels(user, null, "");
//                    /* Ignoring text case for labels */
//                    labels = labelsIgnoreCase(labels);
//                    for (String label : labels) {
//                        populateLabelMap(project, user, testType, builder, labelList, label);
//                    }
//                    // sort the labelList
//                    sortList(labelList);
//                } catch (Exception e) {
//                    log.error("Error retrieving labels from JIRA getSuggestedLabels call:", e);
//                }
//            }
//            paramMap.put("labels", labelList);
            paramMap.put("allFilterDisabled", JiraUtil.isTestSummaryAllFiltersDisabled());
            paramMap.put("labelFilterDisabled", JiraUtil.isTestSummaryLabelsFilterDisabled());
            paramMap.put("pKey", project.getKey());
            paramMap.put("pid", project.getId());
            paramMap.put("baseurl", velocityRequestContextFactory.getJiraVelocityRequestContext().getBaseUrl());
            ApplicationProperties applicationProperties = (ApplicationProperties) ZephyrComponentAccessor.getInstance().getComponent("applicationProperties");
            paramMap.put("zephyrBaseUrl", applicationProperties.getBaseUrl());

            //setting inApp message Url.
            /* commented as inApp feature is not live.
            paramMap.put("inAppMessageUrl", JiraUtil.getInAppMessageUrl());
            */

            //setting analytic Url.
            paramMap.put("analyticUrl", JiraUtil.getAnalyticUrl());

            //setting analytic Enabled.
            paramMap.put("analyticsEnabled", JiraUtil.getZephyrAnalyticsFlag());


            if (project.getId() != null) {
                Collection<Version> unreleasedVersions = versionManager.getVersionsUnreleased(project.getId(), true);
                Collection<Version> releasedVersions = versionManager.getVersionsReleasedDesc(project.getId(), true);
                String testIssueTypeId = JiraUtil.getTestcaseIssueTypeId();
                paramMap.put("unreleasedVersions", unreleasedVersions);
                paramMap.put("releasedVersions", releasedVersions);
                paramMap.put("unscheduledVersionId", ApplicationConstants.UNSCHEDULED_VERSION_ID);
                paramMap.put("unscheduledVersionName", ApplicationConstants.UNSCHEDULED_VERSION_NAME);
                paramMap.put("datePickerFormat", DateTimeFormatUtils.getDateFormat());
                paramMap.put("testIssueTypeId", testIssueTypeId);
            }

            //Stick the last visited Version on cycle Summary
            String versionSelected = getSelected();
            if (!StringUtils.isBlank(versionSelected)) {
                //ActionContext.getRequest().setAttribute("lastvisitedVersion", versionSelected);
                paramMap.put("lastvisitedVersion", versionSelected);
            }

            //Do License Validation.
            ZephyrLicenseVerificationResult licVerificationResult = JiraUtil.performLicenseValidation(zLicenseManager);
            //license is invalid
            if (!licVerificationResult.isValid()) {
                //ActionContext.getRequest().setAttribute("errors", licVerificationResult.getErrorMessage() + "<br>" + licVerificationResult.getGeneralMessage());
                paramMap.put("errors", licVerificationResult.getErrorMessage() + "<br>" + licVerificationResult.getGeneralMessage());
            }

            // client browser check for IE.
            paramMap.put("isIE",JiraUtil.isIeBrowser(ActionContext.getRequest().getHeader("User-Agent")));
        }
        return paramMap;
    }

    // utility method to sort version, label and component lists
    private void sortList(List<Map<String, String>> list) {
        Collections.sort(list, new Comparator<Map<String, String>>() {
            @Override
            public int compare(Map<String, String> o1, Map<String, String> o2) {
                if (null == o1 || null == o1.get("name"))
                    return -1;
                if (null == o2 || null == o2.get("name"))
                    return 1;

                return o1.get("name").compareToIgnoreCase(o2.get("name"));
            }
        });
    }

    /**
     * @param project
     * @param user
     * @param testType
     * @param builder
     * @param versionList
     * @param versionName
     * @param versionId
     */
    private void populateVersionMap(Project project, ApplicationUser user, IssueType testType, JqlClauseBuilder builder, List<Map<String, String>> versionList, String versionName, Long versionId) {
        try {
            builder.clear();
            if (versionId != null) {
                builder.project(new String[]{project.getId().toString()}).and().issueType(testType.getId()).and().fixVersion(versionId);
            } else {
                builder.project(new String[]{project.getId().toString()}).and().issueType(testType.getId()).and().fixVersionIsEmpty();
            }
            long tests = searchProvider.searchCount(builder.buildQuery(), user);
            Map<String, String> verMap = new HashMap<String, String>();
            verMap.put("name", versionName);
            String urlFragment = "/secure/IssueNavigator.jspa?reset=true&jqlQuery=project='" + project.getKey() + "' AND issuetype=" + testType.getId() +
                    (versionId == null ? " AND fixVersion is EMPTY" : " AND fixVersion=" + versionId);
            verMap.put("url", urlFragment);
            verMap.put("tcCnt", String.valueOf(tests));
            versionList.add(verMap);
        } catch (SearchException e) {
            log.fatal("Error in populating component Map", e);
        }
    }


    private void populateComponentMap(Project project, ApplicationUser user, IssueType testType, JqlClauseBuilder builder, List<Map<String, String>> componentList, String componentName, Long componentId) {
        try {
            builder.clear();
            if (componentId != null) {
                builder.project(new String[]{project.getId().toString()}).and().issueType(testType.getId()).and().component(componentId);
            } else {
                builder.project(new String[]{project.getId().toString()}).and().issueType(testType.getId()).and().componentIsEmpty();
            }
            long tests = searchProvider.searchCount(builder.buildQuery(), user);
            Map<String, String> verMap = new HashMap<String, String>();
            verMap.put("name", componentName);
            String urlFragment = "/secure/IssueNavigator.jspa?reset=true&jqlQuery=project='" + project.getKey() + "' AND issuetype=" + testType.getId() +
                    (componentId == null ? " AND component is EMPTY" : " AND component=" + componentId);
            verMap.put("url", urlFragment);
            verMap.put("tcCnt", String.valueOf(tests));
            componentList.add(verMap);
        } catch (SearchException e) {
            log.fatal("Error in populating component Map", e);
        }
    }

    /**
     * @param project
     * @param user
     * @param testType
     * @param builder
     * @param labelList
     * @param labelName
     */
    private void populateLabelMap(Project project, ApplicationUser user, IssueType testType, JqlClauseBuilder builder, List<Map<String, String>> labelList, String labelName) {
        try {
            builder.clear();
            if (labelName != null) {
                builder.project(new String[]{project.getId().toString()}).and().issueType(testType.getId()).and().labels(labelName);
            } else {
                builder.project(new String[]{project.getId().toString()}).and().issueType(testType.getId()).and().labelsIsEmpty();
            }

            log.debug("Label Search Query -" + builder.buildQuery().toString());
            long tests = searchProvider.searchCount(builder.buildQuery(), user);
            if (tests > 0) {
                Map<String, String> verMap = new HashMap<String, String>();
                if (labelName != null)
                    verMap.put("name", labelName);
                else
                    verMap.put("name", i18n.getText("project.testcase.by.label.noLabel.label"));
                String jql = "project='" + project.getKey()
                        + "' AND issuetype=" + testType.getId()
                        + (labelName == null ? " AND labels is EMPTY" : " AND labels=" + URLEncoder.encode(JqlStringSupportImpl.encodeAsQuotedString(labelName), Charsets.UTF_8.name()));
                String urlFragment = "/secure/IssueNavigator.jspa?" + ("reset=true&jqlQuery=" + jql);
                verMap.put("url", urlFragment);
                verMap.put("tcCnt", String.valueOf(tests));
                labelList.add(verMap);
            }
        } catch (SearchException e) {
            log.fatal("Error in populating label Map", e);
        } catch (UnsupportedEncodingException e) {
            log.fatal("Error in populating label Map", e);
        }
    }

    /**
     * Since JIRA doesn't treat "Labels" as case sensitive, this method will ignore Labels case.
     *
     * @param labels
     * @return labels
     */
    public static Set<String> labelsIgnoreCase(Set<String> labels) {
        Map<String, String> labelsMap = new HashMap<String, String>();

        for (String string : labels) {
            if (!(labelsMap.containsKey(string.toLowerCase()))) {
                labelsMap.put(string.toLowerCase(), string);
            }
        }
        labels = null;
        labels = new HashSet<String>(labelsMap.values());
        labelsMap = null;
        return labels;
    }

    private String getSelected() {
        String currentVersionId = null;
        try {
            currentVersionId = (String) ActionContext.getRequest().getSession().getServletContext().getAttribute(SessionKeys.CYCLE_SUMMARY_VERSION + ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser());
            if (!StringUtils.isBlank(currentVersionId)) {
                return currentVersionId;
            }
        } catch (Exception e) {
            log.error("Error retrieving previous seletected version from ActionContext: " + e);
        }
        return currentVersionId;
    }
}
