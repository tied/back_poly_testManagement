package com.thed.zephyr.je.config.upgrade;

import com.atlassian.jira.bc.issue.link.IssueLinkService;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.sal.api.message.Message;
import com.atlassian.sal.api.upgrade.PluginUpgradeTask;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.thed.zephyr.util.ConfigurationConstants;
import com.thed.zephyr.util.JiraUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.util.Collection;

/**
 * Created by mukul on 7/14/15.
 */
public class ZephyrIssueLinksRelationUpgradeTask implements PluginUpgradeTask {

    private static final Logger log = Logger.getLogger(ZephyrIssueLinksRelationUpgradeTask.class);
    private final IssueLinkService issueLinkService;

    public ZephyrIssueLinksRelationUpgradeTask(IssueLinkService issueLinkService) {
        this.issueLinkService = issueLinkService;
    }

    @Override
    /**
     * The build number for this upgrade task. Once this upgrade task has run the plugin manager will store this
     * build number against this plugin type.  After this only upgrade tasks with higher build numbers will be run
     */
    public int getBuildNumber() {
        return 7;
    }

    @Override
    public String getShortDescription() {
        return "Add default value for IssueLinkType Relation.";
    }

    @Override
    public Collection<Message> doUpgrade() throws Exception {
        log.info("Performing ZephyrIssueLinksRelationUpgradeTask - default value for IssueLinkType Relation will be added to JIRA properties.");
        Long defaultRelationId = (Long) JiraUtil.getSimpleDBProperty(ConfigurationConstants.ZEPHYR_ISSUE_LINK_RELATION, 0L);

        //If defaultRelationId is null or 0, then add default IssueLinkType relation again.
        if (null == defaultRelationId || defaultRelationId == 0L) {
            Collection<IssueLinkType> issueLinkTypes = issueLinkService.getIssueLinkTypes();

            if (CollectionUtils.isNotEmpty(issueLinkTypes)) {
                Long issueLinkTypeId = 10000L; // JIRA schemes generally have id's start with 10000. It will cater to default value
                Collection<IssueLinkType> filtered = Collections2.filter(issueLinkTypes, new Predicate<IssueLinkType>() {
                    @Override
                    public boolean apply(IssueLinkType issueLinkType) {
                        return StringUtils.equals(issueLinkType.getName(), "Relates");
                    }
                });
                if (CollectionUtils.isNotEmpty(filtered)) {
                    issueLinkTypeId = filtered.iterator().next().getId();
                }
                JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                        .setLong(ConfigurationConstants.ZEPHYR_ISSUE_LINK_RELATION, issueLinkTypeId);

            } else
                log.error("Error saving default IssueLink Relation!!! IssueLinking is either not enabled or no IssueLink Types are found. " +
                        "Please contact JIRA administrator to enable Issue Linking and add appropriate IssueLink Types.");
        }

        return null;
    }

    /**
     * Identifies the plugin that will be upgraded.
     */
    @Override
    public String getPluginKey() {
        return "com.thed.zephyr.je";
    }

}
