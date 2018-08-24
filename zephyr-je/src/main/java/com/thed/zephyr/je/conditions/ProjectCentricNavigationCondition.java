package com.thed.zephyr.je.conditions;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import java.util.Map;

/**
 * Condition to check if JIRA 6.4 Project Centric View darkFeature is enabled.
 * Created by mukul on 1/12/15.
 */
public class ProjectCentricNavigationCondition implements Condition {
    private final FeatureManager featureManager;

    private String darkFeatureKey;
    private boolean checkIfEnabled;

    public ProjectCentricNavigationCondition(FeatureManager featureManager) {
        this.featureManager = featureManager;
    }

    public void init(Map<String, String> params) throws PluginParseException {
        this.darkFeatureKey = params.get("darkFeatureKey");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(this.darkFeatureKey));
        String checkIfEnabledValue = params.get("checkIfEnabled");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(checkIfEnabledValue));
        this.checkIfEnabled = Boolean.valueOf(checkIfEnabledValue).booleanValue();
    }

    public boolean shouldDisplay(Map<String, Object> context) {
//        boolean enabledForCurrentUser = this.featureManager.isEnabledForUser(
//                ComponentAccessor.getJiraAuthenticationContext().getUser(), this.darkFeatureKey);
        boolean enabledForCurrentUser = this.featureManager.isEnabled(this.darkFeatureKey);
        return this.checkIfEnabled ? enabledForCurrentUser : !enabledForCurrentUser;
    }
}
