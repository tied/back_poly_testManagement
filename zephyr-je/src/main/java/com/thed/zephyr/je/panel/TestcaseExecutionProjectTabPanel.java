package com.thed.zephyr.je.panel;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.datetime.DateTimeFormatUtils;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.plugin.projectpanel.impl.AbstractProjectTabPanel;
import com.atlassian.jira.project.browse.BrowseContext;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.web.FieldVisibilityManager;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.thed.zephyr.je.attachment.SessionKeys;
import com.thed.zephyr.je.config.license.ZephyrLicenseManager;
import com.thed.zephyr.je.config.license.ZephyrLicenseVerificationResult;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.JiraUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import webwork.action.ActionContext;

import java.util.Collection;
import java.util.Map;


public class TestcaseExecutionProjectTabPanel extends AbstractProjectTabPanel {
    protected final Logger log = Logger.getLogger(TestcaseExecutionProjectTabPanel.class);
    private final CustomFieldManager customFieldManager;
    private final FieldVisibilityManager fieldVisibilityManager;
    private final FieldManager fieldManager;
    private final VersionManager versionManager;
    private final WebResourceManager webResourceManager;
    private final ZephyrLicenseManager zLicenseManager;
    private final FeatureManager featureManager;

    public TestcaseExecutionProjectTabPanel(final JiraAuthenticationContext jiraAuthenticationContext,
                                            final CustomFieldManager customFieldManager, final FieldVisibilityManager fieldVisibilityManager,
                                            final FieldManager fieldManager, final VersionManager versionManager, WebResourceManager webResourceManager,
                                            final ZephyrLicenseManager zLicenseManager, final FeatureManager featureManager) {
        super(jiraAuthenticationContext);
        this.customFieldManager = customFieldManager;
        this.fieldVisibilityManager = fieldVisibilityManager;
        this.fieldManager = fieldManager;
        this.versionManager = versionManager;
        this.webResourceManager = webResourceManager;
        this.zLicenseManager = zLicenseManager;
        this.featureManager = featureManager;
    }

    @Override
    public String getHtml(BrowseContext ctx) {
        //Stick the last visited Version on cycle Summary
        String versionSelected = getSelected();
        if (!StringUtils.isBlank(versionSelected)) {
            ActionContext.getRequest().setAttribute("lastvisitedVersion", versionSelected);
        }
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
        webResourceManager.requireResource("com.thed.zephyr.je:zephyr-project-dashboard-resources");
        Map<String, Object> paramMap = ctx.createParameterMap();
        if (ctx.getProject() != null) {
            Collection<Version> unreleasedVersions = versionManager.getVersionsUnreleased(ctx.getProject().getId(), true);
            Collection<Version> releasedVersions = versionManager.getVersionsReleasedDesc(ctx.getProject().getId(), true);
            paramMap.put("unreleasedVersions", unreleasedVersions);
            paramMap.put("releasedVersions", releasedVersions);
            paramMap.put("unscheduledVersionId", ApplicationConstants.UNSCHEDULED_VERSION_ID);
            paramMap.put("unscheduledVersionName", ApplicationConstants.UNSCHEDULED_VERSION_NAME);
            paramMap.put("datePickerFormat", DateTimeFormatUtils.getDateFormat());
        }
        return paramMap;
    }

    private String getSelected() {
        String lastVisitedVersionId = null;
        try{
            lastVisitedVersionId = (String) ActionContext.getServletContext().getAttribute(SessionKeys.CYCLE_SUMMARY_VERSION + authenticationContext.getLoggedInUser());
        }catch (Exception e){
            log.warn("Couldn't fetch last visited version, ", e);
        }
        return lastVisitedVersionId;
    }
}
