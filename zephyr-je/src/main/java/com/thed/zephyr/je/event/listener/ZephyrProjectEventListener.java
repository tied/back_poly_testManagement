package com.thed.zephyr.je.event.listener;


import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.event.AbstractProjectEvent;
import com.atlassian.jira.event.ProjectCreatedEvent;
import com.atlassian.jira.event.ProjectDeletedEvent;
import com.atlassian.jira.issue.fields.config.FieldConfigScheme;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.fields.screen.*;
import com.atlassian.jira.issue.fields.screen.issuetype.IssueTypeScreenScheme;
import com.atlassian.jira.issue.fields.screen.issuetype.IssueTypeScreenSchemeEntity;
import com.atlassian.jira.issue.fields.screen.issuetype.IssueTypeScreenSchemeManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.util.Function;
import com.atlassian.jira.util.collect.CollectionUtil;
import com.thed.zephyr.je.audit.service.AuditManager;
import com.thed.zephyr.je.event.EntityType;
import com.thed.zephyr.util.ConfigurationConstants;
import com.thed.zephyr.util.JiraUtil;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.*;

/**
 * JIRA listener to listen for Project events (Project Create/Update/delete).
 */
public class ZephyrProjectEventListener implements InitializingBean, DisposableBean {

    private static final Logger LOG = LoggerFactory.getLogger(ZephyrProjectEventListener.class);

    private final EventPublisher eventPublisher;
    private final IssueTypeSchemeManager issueTypeSchemeManager;
    private final IssueTypeScreenSchemeManager issueTypeScreenSchemeManager;
    private final IssueTypeManager issueTypeManager;
    private final AuditManager auditManager;

    /**
     * Constructor.
     *
     * @param eventPublisher               injected {@code EventPublisher} implementation.
     * @param issueTypeManager             injected {@code IssueTypeManager} implementation.
     * @param issueTypeSchemeManager
     * @param issueTypeScreenSchemeManager
     */
    public ZephyrProjectEventListener(EventPublisher eventPublisher, IssueTypeManager issueTypeManager, IssueTypeSchemeManager issueTypeSchemeManager,
                                      IssueTypeScreenSchemeManager issueTypeScreenSchemeManager, AuditManager auditManager) {
        this.eventPublisher = eventPublisher;
        this.issueTypeSchemeManager = issueTypeSchemeManager;
        this.issueTypeManager = issueTypeManager;
        this.issueTypeScreenSchemeManager = issueTypeScreenSchemeManager;
        this.auditManager = auditManager;
    }

    /**
     * Called when the plugin has been enabled.
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        // register ourselves with the EventPublisher
        eventPublisher.register(this);
    }

    /**
     * Called when the plugin is being disabled or removed.
     *
     * @throws Exception
     */
    @Override
    public void destroy() throws Exception {
        // unregister ourselves with the EventPublisher
        eventPublisher.unregister(this);
    }

    /**
     * Receives any {@code IssueEvent}s sent by JIRA.
     *
     * @param projectEvent the IssueEvent passed to us
     */
    @EventListener
    public void onProjectEvent(AbstractProjectEvent projectEvent) {
        if (projectEvent instanceof ProjectCreatedEvent) {
            String addIssueTypeTestToProject = JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                    .getString(ConfigurationConstants.ZEPHYR_ISSUE_TYPE_TEST_PROJECT_CREATE);

            //If its null, assume its a first time set up and set the Flag to true
            if (StringUtils.isBlank(addIssueTypeTestToProject)) {
                JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                        .setString(ConfigurationConstants.ZEPHYR_ISSUE_TYPE_TEST_PROJECT_CREATE, String.valueOf(true));
                addIssueTypeTestToProject = "true";
            }

            if (!StringUtils.isBlank(addIssueTypeTestToProject) && !Boolean.valueOf(addIssueTypeTestToProject).booleanValue() == Boolean.FALSE.booleanValue()) {
                LOG.debug("Project Create Called");
                FieldConfigScheme fieldConfigScheme = issueTypeSchemeManager.getConfigScheme(projectEvent.getProject());
                IssueType issueType = issueTypeManager.getIssueType(JiraUtil.getTestcaseIssueTypeId());
                if (issueType != null) {
                    Collection<String> optionIssueTypeIds = new HashSet<>();
                    Collection<IssueType> issueTypes = issueTypeSchemeManager.getIssueTypesForProject(projectEvent.getProject());
                    Collection<String> defaultIssueTypeIds = CollectionUtil.transform(issueTypes, new Function<IssueType, String>() {
                        @Override
                        public String get(final IssueType issueType) {
                            return issueType.getId();
                        }
                    });

                    optionIssueTypeIds.addAll(defaultIssueTypeIds);
                    //Add Test as issueType only if the issuetypescheme is associated to one project
                    //if multiple projects, it would have already added in the defaultIssueTypeIds if required.
                    if(fieldConfigScheme.getAssociatedProjectIds().size() == 1){
                    	optionIssueTypeIds.add(issueType.getId());
                    }
                    issueTypeSchemeManager.update(fieldConfigScheme, optionIssueTypeIds);

                    // Update Issue Type Screen Schemes to have zephyr test steps custom field
                    try {
                        String id = JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                                .getString(ConfigurationConstants.ZEPHYR_CF_TESTSTEP_KEY);

                        IssueTypeScreenScheme issueTypeScreenScheme = issueTypeScreenSchemeManager.getIssueTypeScreenScheme(projectEvent.getProject());
                        Collection<IssueTypeScreenSchemeEntity> issueTypeScreenSchemeEntities = issueTypeScreenScheme.getEntities();

                        for (IssueTypeScreenSchemeEntity issueTypeScreenSchemeEntity : JiraUtil.safe(issueTypeScreenSchemeEntities)) {
                            FieldScreenScheme fieldScreenScheme = issueTypeScreenSchemeEntity.getFieldScreenScheme();
                            Collection<FieldScreenSchemeItem> fieldScreenSchemeItems = fieldScreenScheme.getFieldScreenSchemeItems();
                            for (FieldScreenSchemeItem fieldScreenSchemeItem : JiraUtil.safe(fieldScreenSchemeItems)) {
                                FieldScreen fieldScreen = fieldScreenSchemeItem.getFieldScreen();
                                List<FieldScreenTab> fieldScreenTabs = fieldScreen.getTabs();
                                for (FieldScreenTab fieldScreenTab : JiraUtil.safe(fieldScreenTabs)) {
                                    FieldScreenLayoutItem fieldScreenInstance = fieldScreenTab.getFieldScreenLayoutItem(id);
                                    //To Avoid duplication
                                    if(fieldScreenInstance == null && issueTypeScreenScheme.getProjects().size() == 1) {
                                        fieldScreenTab.addFieldScreenLayoutItem(id);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOG.error("Exception adding Zephyr custom field TestStep to IssueTypeScreenScheme for Project: " +
                                projectEvent.getProject().getName() + " due to: " + e.getMessage() +
                                ". In order for Issue Export to work with Zephyr TestSteps, please add Zephyr custom field TestStep manually to IssueTypeScreenScheme. ", e);
                    }
                }
            }
        } else if(projectEvent instanceof ProjectDeletedEvent) {
        	try {
        		Project project = projectEvent.getProject();
        		Map<String, Object> changeGroupProperties = createChangeGroupProperties(com.thed.zephyr.je.event.EventType.PROJECT_DELETED.toString(), projectEvent.getUser().getUsername(), project.getId().intValue());

    			Map<String, Object> changeLogProperties = AuditUtils.createDeleteChangePropertiesFor(project.getId().intValue(), com.thed.zephyr.je.event.EventType.PROJECT_DELETED.toString(), null, null);
                auditManager.removeZephyrChangeLogs(changeGroupProperties, changeLogProperties);    
        	} catch (Exception e) {
                LOG.error("Error while updating project delete event into audit history", e);
            }
        }
    }
    
    /**
	 * 
	 * @param entityEvent
	 * @param author
	 * @param cycle
	 * @return
	 */
	private Map<String, Object> createChangeGroupProperties(String entityEvent, String author, Integer projectId) {
		Map<String, Object> changeGroupProperties = new HashMap<String, Object>(8);
		changeGroupProperties.put("ZEPHYR_ENTITY_ID", projectId);
		changeGroupProperties.put("ISSUE_ID", -1);
		changeGroupProperties.put("CYCLE_ID", -1);
		changeGroupProperties.put("SCHEDULE_ID", -1);	  	    		
		changeGroupProperties.put("ZEPHYR_ENTITY_TYPE", EntityType.PROJECT.getEntityType());
		changeGroupProperties.put("ZEPHYR_ENTITY_EVENT", entityEvent);
		changeGroupProperties.put("PROJECT_ID", projectId);
		changeGroupProperties.put("AUTHOR", author);
		changeGroupProperties.put("CREATED", System.currentTimeMillis());
		return changeGroupProperties;
	}
}