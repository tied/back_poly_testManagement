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

import org.apache.log4j.Logger;

import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.bc.project.component.ProjectComponentManager;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.label.LabelManager;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.jql.util.JqlStringSupportImpl;
import com.atlassian.jira.plugin.projectpanel.impl.AbstractProjectTabPanel;
import com.atlassian.jira.project.browse.BrowseContext;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.web.FieldVisibilityManager;
import com.atlassian.query.Query;
import com.google.common.base.Charsets;
import com.thed.zephyr.je.config.license.ZephyrLicenseManager;
import com.thed.zephyr.je.config.license.ZephyrLicenseVerificationResult;
import com.thed.zephyr.je.service.ScheduleManager;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.JiraUtil;

import webwork.action.ActionContext;


public class TestcaseCreationProjectTabPanel extends AbstractProjectTabPanel {

    protected final Logger log = Logger.getLogger(TestcaseCreationProjectTabPanel.class);

    private final CustomFieldManager customFieldManager;
    private final FieldVisibilityManager fieldVisibilityManager;
    private final FieldManager fieldManager;
    private final VersionManager versionManager;
    private final SearchProvider searchProvider;
    private final LabelManager labelManager;
    private final I18nHelper i18n;
    private final ProjectComponentManager projComponentManager;
    private final ScheduleManager schedulemanager;
    private final ZephyrLicenseManager zLicenseManager;
    private final FeatureManager featureManager;

    public TestcaseCreationProjectTabPanel(final JiraAuthenticationContext jiraAuthenticationContext,
                                           final CustomFieldManager customFieldManager, final FieldVisibilityManager fieldVisibilityManager,
                                           final FieldManager fieldManager, VersionManager versionManager, ProjectComponentManager componentManager,
                                           final ScheduleManager schedManager, SearchProvider searchProvider, final LabelManager lm, final ZephyrLicenseManager zLicenseManager,
                                           final FeatureManager featureManager) {
        super(jiraAuthenticationContext);
        this.customFieldManager = customFieldManager;
        this.fieldVisibilityManager = fieldVisibilityManager;
        this.fieldManager = fieldManager;
        this.versionManager = versionManager;
        this.searchProvider = searchProvider;
        this.labelManager = lm;
        this.projComponentManager = componentManager;
        this.schedulemanager = schedManager;
        this.i18n = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
        this.zLicenseManager = zLicenseManager;
        this.featureManager = featureManager;
    }

    @Override
    public String getHtml(BrowseContext ctx) {
//        final Map<String, Object> startingParams = JiraVelocityUtils.getDefaultVelocityParams(authenticationContext);
//        startingParams.put("i18n", new I18nBean(ctx.getUser()));
//        startingParams.put("project", ctx.getProject());
//        return descriptor.getHtml("view", startingParams);
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

//    public List<Field> getLabelFields(final Long projectId) {
//    	final List<Field> ret = new ArrayList<Field>();
//    	if (!fieldVisibilityManager.isFieldHiddenInAllSchemes(projectId, IssueFieldConstants.LABELS))
//    	{
//    		ret.add(fieldManager.getField(IssueFieldConstants.LABELS));
//    	}
//    	final List<CustomField> customFieldList = customFieldManager.getCustomFieldObjects();
//    	for (CustomField customField : customFieldList)
//    	{
//    		if (customField.getCustomFieldType() instanceof LabelsCFType)
//    		{
//    			if (!fieldVisibilityManager.isFieldHiddenInAllSchemes(projectId, customField.getId()))
//    			{
//    				ret.add(customField);
//    			}
//    		}
//    	}
//    	return ret;
//     }

    @Override
    protected Map<String, Object> createVelocityParams(final BrowseContext ctx) {
        Map<String, Object> paramMap = ctx.createParameterMap();

        //First getHtml(BrowseContext ctx) gets called. In that we are setting licenseErrorMessage.
        //If this error Message is present then we shouldn't build this panel instead throw error.
        //so skip the details of this method and return empty parampMap!
        String licenseErrorMessage = (String) ActionContext.getRequest().getAttribute("errors");

        if ((ctx.getProject() != null) && (licenseErrorMessage == null)) {
            //license is invalid, no need to fetch any data from backend.
            long totalTests = 0;
            long totalDistinctRemainingTests = 0;
            Long projectId = ctx.getProject().getId();
            /* PROJECT SUMMARY */
            String typeId = JiraUtil.getTestcaseIssueTypeId();
            IssueType testType = JiraUtil.getConstantsManager().getIssueTypeObject(typeId);
            JqlClauseBuilder builder = JqlQueryBuilder.newClauseBuilder();
            builder.project(new String[]{ctx.getProject().getId().toString()}).and().issueType(testType.getId());
            Query query = builder.buildQuery();
            try {
                totalTests = searchProvider.searchCount(query, ctx.getUser());
            } catch (SearchException e) {
                e.printStackTrace();
            }
            paramMap.put("totalTestsByProjectUrl", "/secure/IssueNavigator.jspa?reset=true&jqlQuery=PROJECT='" + ctx.getProject().getKey() + "' AND ISSUETYPE=" + testType.getId());
            paramMap.put("totalTestsByProjCnt", totalTests);

            String encPrj = ctx.getProject().getKey();
            try {
                encPrj = URLEncoder.encode("PROJECT='" + ctx.getProject().getKey() + "' AND executionStatus != " + ApplicationConstants.UNEXECUTED_STATUS, "UTF-8").replace("+", "%20");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            paramMap.put("totalExecutionsByProjCntUrl", "/secure/enav/#?query=" + encPrj);
            paramMap.put("totalExecutionsByProjCnt", schedulemanager.getTestcaseExecutionCount(null, ctx.getProject().getId()));

            /* Added for ZFJ-1213 */
			Integer totatTestsExecuted = schedulemanager.getScheduleCountByProjectIdAndGroupby(new Integer(projectId.intValue()), true);
            totalDistinctRemainingTests = totalTests - totatTestsExecuted;

            paramMap.put("totalDistinctRemainingTestsCnt", totalDistinctRemainingTests);

			/* ** TEST BY VERSION ** */
            List<Version> versions = versionManager.getVersions(ctx.getProject().getId());
            List<Map<String, String>> versionList = new ArrayList<Map<String, String>>();
            populateVersionMap(ctx, testType, builder, versionList, i18n.getText("zephyr.je.version.unscheduled"), null);    //TOdo: implement i18n for this
            for (Version ver : versions) {
                populateVersionMap(ctx, testType, builder, versionList, ver.getName(), ver.getId());
            }
            // sort the versionList
            sortList(versionList);
            paramMap.put("versions", versionList);
        	
        	/* **  TEST BY COMPONENT ** */
            Collection<ProjectComponent> compoenents = projComponentManager.findAllForProject(ctx.getProject().getId());
            List<Map<String, String>> componentList = new ArrayList<Map<String, String>>();
            populateComponentMap(ctx, testType, builder, componentList, i18n.getText("zephyr.je.component.nocomponent"), null);    //TOdo: implement i18n for this
            for (ProjectComponent comp : compoenents) {
                populateComponentMap(ctx, testType, builder, componentList, comp.getName(), comp.getId());
            }
            // sort the componentList
            sortList(componentList);
            paramMap.put("components", componentList);
        	
        	/* TEST BY LABEL */
            Set<String> labels = labelManager.getSuggestedLabels(ctx.getUser(), null, "");
        	/* Ignoring text case for labels */
            labels = labelsIgnoreCase(labels);
            List<Map<String, String>> labelList = new ArrayList<Map<String, String>>();
            populateLabelMap(ctx, testType, builder, labelList, null);
            for (String label : labels) {
                populateLabelMap(ctx, testType, builder, labelList, label);
            }
            // sort the labelList
            sortList(labelList);
            paramMap.put("labels", labelList);

            paramMap.put("pKey", ctx.getProject().getKey());
            paramMap.put("pid", ctx.getProject().getId());
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
     * @param ctx
     * @param testType
     * @param builder
     * @param versionList
     * @param versionName
     * @param versionId
     */
    private void populateVersionMap(final BrowseContext ctx, IssueType testType, JqlClauseBuilder builder, List<Map<String, String>> versionList, String versionName, Long versionId) {
        try {
            builder.clear();
            if (versionId != null) {
                builder.project(new String[]{ctx.getProject().getId().toString()}).and().issueType(testType.getId()).and().fixVersion(versionId);
            } else {
                builder.project(new String[]{ctx.getProject().getId().toString()}).and().issueType(testType.getId()).and().fixVersionIsEmpty();
            }
            long tests = searchProvider.searchCount(builder.buildQuery(), ctx.getUser());
            Map<String, String> verMap = new HashMap<String, String>();
            verMap.put("name", versionName);
            String urlFragment = "/secure/IssueNavigator.jspa?reset=true&jqlQuery=project='" + ctx.getProject().getKey() + "' AND issuetype=" + testType.getId() +
                    (versionId == null ? " AND fixVersion is EMPTY" : " AND fixVersion=" + versionId);
            verMap.put("url", urlFragment);
            verMap.put("tcCnt", String.valueOf(tests));
            versionList.add(verMap);
        } catch (SearchException e) {
            log.fatal("Error in populating component Map", e);
        }
    }


    private void populateComponentMap(final BrowseContext ctx, IssueType testType, JqlClauseBuilder builder, List<Map<String, String>> componentList, String componentName, Long componentId) {
        try {
            builder.clear();
            if (componentId != null) {
                builder.project(new String[]{ctx.getProject().getId().toString()}).and().issueType(testType.getId()).and().component(componentId);
            } else {
                builder.project(new String[]{ctx.getProject().getId().toString()}).and().issueType(testType.getId()).and().componentIsEmpty();
            }
            long tests = searchProvider.searchCount(builder.buildQuery(), ctx.getUser());
            Map<String, String> verMap = new HashMap<String, String>();
            verMap.put("name", componentName);
            String urlFragment = "/secure/IssueNavigator.jspa?reset=true&jqlQuery=project='" + ctx.getProject().getKey() + "' AND issuetype=" + testType.getId() +
                    (componentId == null ? " AND component is EMPTY" : " AND component=" + componentId);
            verMap.put("url", urlFragment);
            verMap.put("tcCnt", String.valueOf(tests));
            componentList.add(verMap);
        } catch (SearchException e) {
            log.fatal("Error in populating component Map", e);
        }
    }

    /**
     * @param ctx
     * @param testType
     * @param builder
     * @param labelList
     * @param labelName
     */
    private void populateLabelMap(final BrowseContext ctx, IssueType testType, JqlClauseBuilder builder, List<Map<String, String>> labelList, String labelName) {
        try {
            builder.clear();
            if (labelName != null) {
                builder.project(new String[]{ctx.getProject().getId().toString()}).and().issueType(testType.getId()).and().labels(labelName);
            } else {
                builder.project(new String[]{ctx.getProject().getId().toString()}).and().issueType(testType.getId()).and().labelsIsEmpty();
            }

            log.debug("Label Search Query -" + builder.buildQuery().toString());
            long tests = searchProvider.searchCount(builder.buildQuery(), ctx.getUser());
            if (tests > 0) {
                Map<String, String> verMap = new HashMap<String, String>();
                if (labelName != null)
                    verMap.put("name", labelName);
                else
                    verMap.put("name", i18n.getText("project.testcase.by.label.noLabel.label"));
                String jql = "project='" + ctx.getProject().getKey()
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
    private Set<String> labelsIgnoreCase(Set<String> labels) {
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
    
}
