package com.thed.zephyr.je.config.action;

import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.websudo.WebSudoRequired;
import com.thed.zephyr.je.config.ZephyrFeatureManager;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * Manage site wide dark features.  This is copied from JIRA
 * refer to com.atlassian.jira.web.action.admin.darkfeatures.SiteDarkFeatures
 * <p/>
 * Created by mukul on 7/7/15.
 */
@WebSudoRequired
public class ZephyrLabFeatures extends JiraWebActionSupport {
    private String featureKey;
    private final ZephyrFeatureManager zephyrFeatureManager;

    public ZephyrLabFeatures(ZephyrFeatureManager zephyrFeatureManager) {
        this.zephyrFeatureManager = zephyrFeatureManager;
    }

    public boolean isPermitted() {
        return zephyrFeatureManager.hasSiteEditPermission();
    }

    @Override
    public String doDefault() throws Exception {
        if (!zephyrFeatureManager.hasSiteEditPermission()) {
            return "securitybreach";
        }
        return SUCCESS;
    }

    @Override
    @RequiresXsrfCheck
    protected String doExecute() throws Exception {
        if (!zephyrFeatureManager.hasSiteEditPermission()) {
            return "securitybreach";
        }

        // Enable a dark feature
        if (StringUtils.isNotBlank(featureKey)) {
            zephyrFeatureManager.enableSiteDarkFeature(featureKey.trim());
        }

        featureKey = "";
        return forceRedirect("ZephyrLabFeatures!default.jspa");
    }

    @RequiresXsrfCheck
    public String doRemove() {
        // Disable a dark feature
        if (StringUtils.isNotBlank(featureKey)) {
            zephyrFeatureManager.disableSiteDarkFeature(featureKey.trim());
        }

        featureKey = "";
        return forceRedirect("ZephyrLabFeatures!default.jspa");
    }

    public List<String> getSystemEnabledFeatures() {
        List<String> enabledFeatures = zephyrFeatureManager.getSystemEnabledFeatureKeys();
        Collections.sort(enabledFeatures);
        return enabledFeatures;
    }

    public List<String> getSiteEnabledFeatures() {
        List<String> enabledFeatures = zephyrFeatureManager.getEnabledFeatureKeys();
        Collections.sort(enabledFeatures);
        return enabledFeatures;
    }

    public void setFeatureKey(String featureKey) {
        this.featureKey = featureKey;
    }

    public boolean isEnabled(String featureKey) {
        return zephyrFeatureManager.isEnabled(featureKey);
    }

    public String getFeatureKey() {
        return featureKey;
    }
}
