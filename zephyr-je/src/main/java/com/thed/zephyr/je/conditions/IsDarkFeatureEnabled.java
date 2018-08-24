package com.thed.zephyr.je.conditions;

import com.atlassian.jira.plugin.webfragment.conditions.AbstractWebCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.PluginParseException;
import com.thed.zephyr.je.config.ZephyrFeatureManager;

import java.util.Map;

/**
 * Created by smangal on 10/3/15.
 */
public class IsDarkFeatureEnabled extends AbstractWebCondition{

    private String darkFeatureKey;
    private ZephyrFeatureManager zephyrFeatureManager;

    public IsDarkFeatureEnabled(ZephyrFeatureManager zephyrFeatureManager) {
        assert zephyrFeatureManager != null;
        this.zephyrFeatureManager = zephyrFeatureManager;
    }

    @Override
    public void init(Map<String, String> params) throws PluginParseException {
        if(params != null && params.containsKey("featureKey")){
            darkFeatureKey = params.get("featureKey");
        }
    }


    @Override
    public boolean shouldDisplay(ApplicationUser applicationUser, JiraHelper jiraHelper) {
        if(darkFeatureKey != null){
            return zephyrFeatureManager.isEnabled(darkFeatureKey);
        }
        return false;
    }
}
