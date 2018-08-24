package com.thed.zephyr.je.config.upgrade;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.context.manager.JiraContextTreeManager;
import com.atlassian.jira.issue.customfields.CustomFieldUtils;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.config.FieldConfigScheme;
import com.atlassian.jira.issue.fields.config.manager.FieldConfigSchemeManager;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.util.ofbiz.GenericValueUtils;
import com.atlassian.sal.api.message.Message;
import com.atlassian.sal.api.upgrade.PluginUpgradeTask;
import com.thed.zephyr.util.ConfigurationConstants;
import com.thed.zephyr.util.JiraUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Upgrade task to configure Zephyr custom field "Zephyt Teststep". It should only relate to issue type "Test" instead global issues.
 * refer to: https://defects.yourzephyr.com/browse/ZFJ-1411
 */
public class ZephyrCFTypeToIssueTypesUpgradeTask implements PluginUpgradeTask {

    private static final Logger log = Logger.getLogger(ZephyrIssueLinksRelationUpgradeTask.class);

    private final CustomFieldManager customFieldManager;
    private final FieldConfigSchemeManager fieldConfigSchemeManager;
    private final ProjectManager projectManager;

    public ZephyrCFTypeToIssueTypesUpgradeTask(CustomFieldManager customFieldManager, FieldConfigSchemeManager fieldConfigSchemeManager, ProjectManager projectManager) {
        this.customFieldManager = customFieldManager;
        this.fieldConfigSchemeManager = fieldConfigSchemeManager;
        this.projectManager = projectManager;
    }

    @Override
    /**
     * The build number for this upgrade task. Once this upgrade task has run the plugin manager will store this
     * build number against this plugin type.  After this only upgrade tasks with higher build numbers will be run
     */
    public int getBuildNumber() {
        return 8;
    }

    @Override
    public String getShortDescription() {
        return "Configure Zephyr custom field \"Zephyt Teststep\" to issue type \"Test\".";
    }

    @Override
    public Collection<Message> doUpgrade() throws Exception {
        log.debug("Performing ZephyrCFTypeToIssueTypesUpgradeTask - Configure Zephyr custom field \"Zephyt Teststep\" to issue type \"Test\".");
        CustomField existingCustomField = null;
        try {
            String customFieldValueFromDBProps = JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                    .getString(ConfigurationConstants.ZEPHYR_CF_TESTSTEP_KEY);

            String testcaseId = JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                    .getString(ConfigurationConstants.ZEPHYR_ISSUETYPE_KEY);

            // check if Zephyr Custom Type "Zephyr Teststep is already created in DB Properties. If not, skip this entire upgrade task.
            if (StringUtils.isNotBlank(customFieldValueFromDBProps) && StringUtils.isNotBlank(testcaseId)) {
                log.debug("ZephyrCFTypeToIssueTypesUpgradeTask: Zephyr custom field \"Zephyt Teststep\" found in DB Properties.");

                // get custom field Id from fieldValueFromDBProperties ( which is the numeric string after '_'
                Long customFieldId = -1L;
                String numericStringId = customFieldValueFromDBProps.substring(customFieldValueFromDBProps.indexOf('_') + 1);
                if (StringUtils.isNotBlank(numericStringId))
                    customFieldId = Long.parseLong(numericStringId);

                // get the custom field by it's ID
                existingCustomField = customFieldManager.getCustomFieldObject(customFieldId);
                // Custom field by it's ID not found, so try to find it by name
                if (null == existingCustomField) {
                    existingCustomField = customFieldManager.getCustomFieldObject(ConfigurationConstants.ZEPHYR_CF_TESTSTEP_KEY);
                }
                // update custom field
                updateIssueTypeTest(existingCustomField, testcaseId);
            }
        } catch (Exception e) {
            log.error("Error configuring Zephyr custom field \"Zephyt Teststep\" to issue type \"Test\" relation!!!. " +
                    "Please contact JIRA administrator to manually configure this.", e);
        }
        return null;
    }

    private void updateIssueTypeTest(CustomField customField, String testcaseId) {
        List<FieldConfigScheme> fieldConfigSchemes = customField.getConfigurationSchemes();

        for (FieldConfigScheme configScheme : JiraUtil.safe(fieldConfigSchemes)) {
            List<Long> projectsList = configScheme.getAssociatedProjectIds();
            List contexts = CustomFieldUtils.buildJiraIssueContexts(this.isGlobal(customField, configScheme), projectsList.toArray(new Long[0]), this.projectManager);
            if (configScheme.getId() != null) {
                Set configEntries = configScheme.getConfigsByConfig().keySet();
                Iterator configEntriesItr = configEntries.iterator();
                if (configEntriesItr.hasNext()) {
                    FieldConfig config = (FieldConfig) configEntriesItr.next();
                    HashMap configs = new HashMap(1);
                    configs.put(testcaseId, config);
                    configScheme = (new FieldConfigScheme.Builder(configScheme)).setConfigs(configs).toFieldConfigScheme();
                }
                this.fieldConfigSchemeManager.updateFieldConfigScheme(configScheme, contexts, customField);
            }
            ComponentAccessor.getFieldManager().refresh();
            customFieldManager.refreshConfigurationSchemes(customField.getIdAsLong());
        }
    }

    public boolean isGlobal(CustomField customField, FieldConfigScheme configScheme) {
        return !customField.isAllProjects() ? true : configScheme != null && configScheme.isAllProjects();
    }

    /**
     * Identifies the plugin that will be upgraded.
     */
    @Override
    public String getPluginKey() {
        return "com.thed.zephyr.je";
    }

}