package com.thed.zephyr.je.config.upgrade;

import com.atlassian.fugue.Option;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.issue.link.RemoteIssueLinkManager;
import com.atlassian.jira.web.ServletContextProvider;
import com.atlassian.sal.api.message.Message;
import com.atlassian.sal.api.upgrade.PluginUpgradeTask;
import com.thed.zephyr.je.helper.ScheduleResourceHelper;
import com.thed.zephyr.je.service.ScheduleManager;
import com.thed.zephyr.je.service.StepResultManager;
import com.thed.zephyr.util.ConfigurationConstants;
import com.thed.zephyr.util.JiraUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import java.text.MessageFormat;
import java.util.Collection;

/**
 * Created by smangal on 11/17/15.
 */
public class ZephyrUpgrade3Dot1Task31 implements PluginUpgradeTask {

    private static final Logger log = Logger.getLogger(ZephyrUpgrade3Dot1Task31.class);
    private final IssueLinkTypeManager issueLinkTypeManager;
    private final ScheduleManager scheduleManager;
    private final RemoteIssueLinkManager remoteIssueLinkManager;
    private final StepResultManager stepResultManager;

    public ZephyrUpgrade3Dot1Task31(IssueLinkTypeManager issueLinkTypeManager, ScheduleManager scheduleManager, RemoteIssueLinkManager remoteIssueLinkManager, StepResultManager stepResultManager) {
        this.issueLinkTypeManager = issueLinkTypeManager;
        this.scheduleManager = scheduleManager;
        this.remoteIssueLinkManager = remoteIssueLinkManager;
        this.stepResultManager = stepResultManager;
    }

    @Override
    /**
     * The build number for this upgrade task. Once this upgrade task has run the plugin manager will store this
     * build number against this plugin type.  After this only upgrade tasks with higher build numbers will be run
     * Make sure this number is > 31 for ZFJ 3.1 onwards
     */
    public int getBuildNumber() {
        return 31;
    }

    @Override
    public String getShortDescription() {
        return "Add default value for IssueLinkType Relation.";
    }

    @Override
    public Collection<Message> doUpgrade() throws Exception {
        fixDefaultIssueLinkType();
        log.debug(MessageFormat.format("Version {0} successfully upgraded.", 3.1));
        return null;
    }

    private void fixDefaultIssueLinkType() {
        log.info("Performing ZephyrIssueLinksRelationUpgradeTask - default value for IssueLinkType Relation will be added to JIRA properties.");
        Long defaultRelationId = (Long) JiraUtil.getSimpleDBProperty(ConfigurationConstants.ZEPHYR_ISSUE_LINK_RELATION, 0L);

        //If defaultRelationId is null or 0, then add default IssueLinkType relation again.
        if (null == defaultRelationId || defaultRelationId == 0L) {
            log.info("Creating remote link type ");
            saveLinkType();
        }else{
            IssueLinkType issueLinkType = issueLinkTypeManager.getIssueLinkType(defaultRelationId);
            //If saved issueLinkType is System or non-existing, we re-create it
            if(issueLinkType == null || issueLinkType.isSystemLinkType()){
                log.info("Replacing old link " + issueLinkType );
                replaceOldLinkType();
            }
        }
    }

    private void replaceOldLinkType() {
        final ScheduleResourceHelper helper = new ScheduleResourceHelper(ComponentAccessor.getIssueManager(), remoteIssueLinkManager, scheduleManager, stepResultManager);
        Option<Long> issueLinkType = getMatchingNonSystemLinkType();
        if(issueLinkType.isDefined()) {
            final Boolean remoteIssueLinkEnabled = JiraUtil.isIssueToTestExecutionRemoteLinkingEnabled();
            final Boolean issuelinkEnabled = JiraUtil.isIssueToTestLinkingEnabled();
            final Boolean issueToTestStepLink = JiraUtil.isIssueToTestStepLinkingEnabled();;
            final Boolean remoteIssueLinkStepExecution = JiraUtil.isIssueToTestStepExecutionRemoteLinkingEnabled();

            ServletContext servletContext = ServletContextProvider.getServletContext();
            log.info("With new link " + issueLinkType.get());
            helper.performLinksRefreshAsync(servletContext.getContextPath(), issueLinkType.get(), remoteIssueLinkEnabled, issuelinkEnabled,issueToTestStepLink,remoteIssueLinkStepExecution);
        }
    }

    private void saveLinkType() {
        Option<Long> issueLinkType = getMatchingNonSystemLinkType();
        if (issueLinkType.isDefined()) {
            JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                    .setLong(ConfigurationConstants.ZEPHYR_ISSUE_LINK_RELATION, issueLinkType.get());
        } else
            log.error("Error saving default IssueLink Relation!!! IssueLinking is either not enabled or no IssueLink Types are found. " +
                    "Please contact JIRA administrator to enable Issue Linking and add appropriate IssueLink Types.");
    }

    private Option<Long> getMatchingNonSystemLinkType(){
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
     * Identifies the plugin that will be upgraded.
     */
    @Override
    public String getPluginKey() {
        return ConfigurationConstants.PLUGIN_KEY;
    }
}
