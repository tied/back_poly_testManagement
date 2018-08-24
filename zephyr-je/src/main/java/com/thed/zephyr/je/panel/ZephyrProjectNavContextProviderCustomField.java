package com.thed.zephyr.je.panel;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.google.common.base.Preconditions;
import com.thed.zephyr.je.config.license.ZephyrLicenseManager;
import com.thed.zephyr.je.config.license.ZephyrLicenseVerificationResult;
import com.thed.zephyr.util.JiraUtil;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mukul on 1/13/15.
 */
public class ZephyrProjectNavContextProviderCustomField implements ContextProvider {

    protected final Logger log = Logger.getLogger(ZephyrProjectNavContextProviderCustomField.class);
    private final I18nHelper i18n;
    private final WebResourceManager webResourceManager;
    private final ZephyrLicenseManager zLicenseManager;

    public ZephyrProjectNavContextProviderCustomField(final WebResourceManager webResourceManager,
                                                      final ZephyrLicenseManager zLicenseManager) {
        this.i18n = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
        this.webResourceManager = webResourceManager;
        this.zLicenseManager = zLicenseManager;
    }

    @Override
    public void init(Map<String, String> params) throws PluginParseException {
    }

    @Override
    public Map<String, Object> getContextMap(Map<String, Object> context) {
        Map<String, Object> projectCtx = new HashMap<>();
        Map<String, Object> paramMap = new HashMap<>();
        Project project = (Project) Preconditions.checkNotNull(context.get("project"));
        boolean isPermissionSchemeEnabled = JiraUtil.getPermissionSchemeFlag();

        boolean isTestManagementForProjects = JiraUtil.isTestManagementForProjects();
  		  if(!isTestManagementForProjects) {
              paramMap.put("errors", ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error"));
              return paramMap;
      	}

        //Do License Validation.
        ZephyrLicenseVerificationResult licVerificationResult = JiraUtil.performLicenseValidation(zLicenseManager);
        //license is invalid
        if (!licVerificationResult.isValid()) {
            //ActionContext.getRequest().setAttribute("errors", licVerificationResult.getErrorMessage() + "<br>" + licVerificationResult.getGeneralMessage());
            paramMap.put("errors", licVerificationResult.getErrorMessage() + "<br>" + licVerificationResult.getGeneralMessage());
            return paramMap;
        }

        webResourceManager.requireResource("com.thed.zephyr.je:zephyr-project-dashboard-resources-tree");
        if(isPermissionSchemeEnabled) {
	        ApplicationUser user = context.get("user") != null ? (ApplicationUser) context.get("user") : null;
          projectCtx.put("project",project);
	        return projectCtx;
        } else {
	        ApplicationUser user = (ApplicationUser) Preconditions.checkNotNull(context.get("user"));
          projectCtx.put("project",project);
          return projectCtx;
        }
    }
}
