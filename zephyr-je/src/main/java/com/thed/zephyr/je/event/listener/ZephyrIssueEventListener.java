package com.thed.zephyr.je.event.listener;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.thed.zephyr.je.service.StepResultManager;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.issue.IssueEventSource;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.changehistory.ChangeHistory;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.issue.index.DocumentConstants;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.thed.zephyr.je.audit.service.AuditManager;
import com.thed.zephyr.je.event.EntityType;
import com.thed.zephyr.je.event.SingleScheduleEvent;
import com.thed.zephyr.je.model.ExecutionWorkflowStatus;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.service.ScheduleManager;
import com.thed.zephyr.je.service.TeststepManager;
import com.thed.zephyr.util.JiraUtil;
 
/**
 * JIRA listener to listen for Issue events (Issue Update/delete, IssueType update).
 */
public class ZephyrIssueEventListener implements InitializingBean, DisposableBean {
 
    private static final Logger log = LoggerFactory.getLogger(ZephyrIssueEventListener.class);
 
    private final EventPublisher eventPublisher;

	private final ScheduleManager scheduleManager;

	private final TeststepManager teststepManager;

    private final StepResultManager stepResultManager;
    
    private final AuditManager auditManager;
	
	private final Collection<Long>STATUS_EVENT_IDs = new ArrayList<Long>(5);


    /**
     * Constructor.
     * @param eventPublisher injected {@code EventPublisher} implementation.
     */
    public ZephyrIssueEventListener(EventPublisher eventPublisher, 
    								ScheduleManager scheduleManager, 
    								TeststepManager teststepManager, StepResultManager stepResultManager, AuditManager auditManager) {
        this.eventPublisher = eventPublisher;
        this.scheduleManager = scheduleManager;
        this.teststepManager = teststepManager;
        this.stepResultManager = stepResultManager;
        this.auditManager = auditManager;
        
        STATUS_EVENT_IDs.add(EventType.ISSUE_RESOLVED_ID);
        STATUS_EVENT_IDs.add(EventType.ISSUE_REOPENED_ID);
        STATUS_EVENT_IDs.add(EventType.ISSUE_CLOSED_ID);
        STATUS_EVENT_IDs.add(EventType.ISSUE_WORKSTARTED_ID);
        STATUS_EVENT_IDs.add(EventType.ISSUE_WORKSTOPPED_ID);
    }
 
    /**
     * Called when the plugin has been enabled.
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        // register ourselves with the EventPublisher
        eventPublisher.register(this);
    }
 
    /**
     * Called when the plugin is being disabled or removed.
     * @throws Exception
     */
    @Override
    public void destroy() throws Exception {
        // unregister ourselves with the EventPublisher
        eventPublisher.unregister(this);
    }
 
    /**
     * Receives any {@code IssueEvent}s sent by JIRA.
     * @param issueEvent the IssueEvent passed to us
     */
    @EventListener
    public void onIssueEvent(IssueEvent issueEvent) {
        Long eventTypeId = issueEvent.getEventTypeId();
        String eventSource = issueEvent.getParams() != null && issueEvent.getParams().containsKey("eventsource") 
        		? (String)issueEvent.getParams().get("eventsource") : null;
        
        Issue issue = issueEvent.getIssue();
        boolean shouldReindexSchedule = false;
        boolean shouldDeleteIndexSchedule = false;
        boolean shouldReindexScheduleDefectMapping = false;
        boolean shouldReindexStepDefectMapping = false;
        boolean shouldUpdateEstimatedTime = false;

        final String testcaseIssueTypeId = JiraUtil.getTestcaseIssueTypeId();
        final boolean isTestIssue = StringUtils.equals(testcaseIssueTypeId, issue.getIssueTypeObject().getId()); 
        
        if (eventTypeId.equals(EventType.ISSUE_DELETED_ID)) {
        	
        	if(isTestIssue){	
	        	//if issue of type TEST is deleted then remove all schedules and steps assocaited to it.
	            log.info("Issue {} has been deleted at {}, starting test artifacts cleanup", issue.getKey(), issue.getUpdated());
	            teststepManager.removeTeststeps(issue.getId());
	            scheduleManager.removeSchedules(issue.getId());
	        	shouldDeleteIndexSchedule = true;
        	}
        	
        	//shouldReindexScheduleDefectMapping = true;
        	Collection<Schedule> schedules = scheduleManager.getSchedulesByDefectId(issue.getId().intValue(), true);
        	//Remove Schedule Defect Association will be common task irrespective issue is of type TEST or any other!
        	scheduleManager.removeScheduleDefectsAssociation(issue.getId());
            stepResultManager.removeStepDefectsAssociation(issue.getId());
        	performReIndex(schedules);

        } else if (eventTypeId.equals(EventType.ISSUE_MOVED_ID)) {
        	Integer scheduleCount = scheduleManager.getSchedulesCountByIssueId(issue.getId().intValue());
        	if(scheduleCount != null && scheduleCount.intValue() > 0) {
        		log.info("Remove the schedule and associated index as this test has either chanegd project or changed issuetype, schedules to be deleted :"+scheduleCount.intValue());
        		scheduleManager.removeSchedules(issue.getId());
        		shouldDeleteIndexSchedule = true;
        	}
        	//Check if Moved Issue is a part of execution defect associated
        	Integer scheduleDefectCount =  scheduleManager.getScheduleCountByDefectId(issue.getId().intValue());
        	if(scheduleDefectCount != null && scheduleDefectCount.intValue() > 0) {
        		shouldReindexScheduleDefectMapping = true;
        	}

        	//Check if Moved Issue is a part of step defect associated
        	Integer stepDefectCount =  scheduleManager.getScheduleCountByStepDefectId(issue.getId().intValue());
        	if(stepDefectCount != null && stepDefectCount.intValue() > 0) {
        		shouldReindexStepDefectMapping = true;
        	}
        } else if (eventTypeId.equals(EventType.ISSUE_UPDATED_ID)) {

        	//If issue type has been changed from "TEST" to something else, delete all schedules assocaited to this issue!
        	log.debug("Issue {0} has been updated at {1}.", issue.getKey(), issue.getUpdated());

        	ChangeHistoryManager chManager = ComponentAccessor.getChangeHistoryManager();
        	if(issueEvent.getChangeLog() != null && issueEvent.getChangeLog().containsKey("id")) {
	            final Long changeLogGroupId = new Long(issueEvent.getChangeLog().getString("id"));
	            
	    		List<ChangeHistory> changeList = chManager.getChangeHistoriesForUser(issue, ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser());
	    		ChangeHistory changeHistory = Iterables.find(changeList, new Predicate<ChangeHistory>(){
	
					@Override
					public boolean apply(ChangeHistory input) {
						return input.getId().equals(changeLogGroupId);
					}
	
	    		});
	    		
	    		if(changeHistory == null){
	    			log.error("No Change History found for changeGroup. This may cause ZFJ indexes to go out of sync. Please perform manual sync in Zephyr Admin" + changeLogGroupId);
	    			return;
	    		}
	    		for(ChangeItemBean item : changeHistory.getChangeItemBeans()){	
	    			//Find out if latest change is issuetype change.
	    			if(item.getField().equals(IssueFieldConstants.ISSUE_TYPE)){
	    				if(StringUtils.equals(item.getFrom(), testcaseIssueTypeId)){
	    					log.debug("Issue Type is changed from Test, so let's delete any schedules associated with it.");
	    					//Delete any schedules, steps associated with this Issue.
	    		            teststepManager.removeTeststeps(issue.getId());
	    		            scheduleManager.removeSchedules(issue.getId());
	    		            shouldDeleteIndexSchedule=true;
	    				}
	    			}
	    			
	    			if(isTestIssue && item.getField().equalsIgnoreCase(DocumentConstants.ISSUE_SUMMARY)) {
						shouldReindexSchedule = true;
	    			}
	    			
	    			if(item.getField().equalsIgnoreCase(DocumentConstants.ISSUE_PRIORITY) 		|| 
							item.getField().equalsIgnoreCase(DocumentConstants.ISSUE_COMPONENT) 	||
							item.getField().equalsIgnoreCase(DocumentConstants.ISSUE_LABELS)){
						shouldReindexSchedule = true;
					}
					
					if(item.getField().equals(IssueFieldConstants.STATUS)){
						shouldReindexScheduleDefectMapping = true;
					}
					
					if(isTestIssue && item.getField().equalsIgnoreCase(DocumentConstants.ISSUE_TIME_ESTIMATE_ORIG)){
						log.debug("Issue original estimate time update event called successfully.");
						shouldUpdateEstimatedTime = true;
					}
	    		}
        	}
        } else if(STATUS_EVENT_IDs.contains(eventTypeId)) {
			shouldReindexScheduleDefectMapping = true;
        }
        else if( EventType.ISSUE_GENERICEVENT_ID.equals(eventTypeId) && IssueEventSource.WORKFLOW.equals(eventSource)){
        	//Custom workflow event.
			shouldReindexScheduleDefectMapping = true;
        }
        
        if(shouldUpdateEstimatedTime){
        	performUpdateEstimatedTime(issue.getId(), issue.getOriginalEstimate());
        }
        
        if(shouldDeleteIndexSchedule) {
        	performDeleteIndex(issue.getId());
        	Map<String, Object> changeGroupProperties = createChangeGroupProperties(com.thed.zephyr.je.event.EventType.ISSUE_DELETED.toString(), issueEvent.getUser().getUsername(),
        			issue.getId().intValue(), issueEvent.getProject().getId());

			Map<String, Object> changeLogProperties = AuditUtils.createDeleteChangePropertiesFor(issue.getId().intValue(), com.thed.zephyr.je.event.EventType.ISSUE_DELETED.toString(), null, null);
            auditManager.removeZephyrChangeLogs(changeGroupProperties, changeLogProperties);        	
        }
        
		if(shouldReindexSchedule){
        	performReindex(issue.getId());
        }
		
		if(shouldReindexScheduleDefectMapping){
        	performReIndexScheduleDefect(issue.getId(),false);
		}
		
		if(shouldReindexStepDefectMapping){
			performReIndexScheduleDefect(issue.getId(),true);
		}
    }
    
    private void performDeleteIndex(Long issueId){
    	Collection<Long> issueIds = new ArrayList<Long>();
		issueIds.add(issueId);
		performDeleteIndex(issueIds);
    }
    
    private void performReindex(Long issueId){
    	Collection<Schedule> schedules = scheduleManager.getSchedulesByIssueId(issueId.intValue(),-1,null);
		performReIndex(schedules);
    }
    
    private void performReIndexScheduleDefect(Long issueId, boolean includeStepResultDefect){
    	Collection<Schedule> schedules = scheduleManager.getSchedulesByDefectId(issueId.intValue(), includeStepResultDefect);
		performReIndex(schedules);
    }   

    private void performUpdateEstimatedTime(Long issueId, Long estimatedTime){
    	Collection<Schedule> schedules = scheduleManager.getSchedulesByIssueId(issueId.intValue(), -1, null);
        Collection<Schedule> schedulesToBeReIndexed = Lists.newArrayList();
    	for(Schedule schedule : schedules){
    		//No need to check if the cycle is Ad Hoc as the ExecutionWorkFlow for the Ad Hoc cycle is null.
    		if(schedule.getExecutionWorkflowStatus() != null && !ExecutionWorkflowStatus.COMPLETED.name().equals(schedule.getExecutionWorkflowStatus().name())){
    			schedule.setEstimatedTime(estimatedTime);
    			schedule.save();
                schedulesToBeReIndexed.add(schedule);
    		}
    	}
    	performReIndex(schedulesToBeReIndexed);
    }

	/**
	 * @param issueIds
	 */
	@SuppressWarnings("unchecked")
	private void performDeleteIndex(Collection<Long> issueIds) {
		try {
	        final Collection<String> issueList = CollectionUtils.collect(issueIds, new Transformer() {
	            @Override
				public String transform(final Object input) {
	                if (input == null) {
	                    return null;
	                }
	                
	                return String.valueOf(input);
	            }
	        });
			Map<String,Object> params = new HashMap<String,Object>();
			params.put("ENTITY_TYPE", "ISSUE_ID");
			params.put("ENTITY_VALUE", issueList);
	        eventPublisher.publish(new SingleScheduleEvent(null, params, com.thed.zephyr.je.event.EventType.EXECUTION_DELETED));
		} catch (Exception e) {
			log.error("", e);
		}
	}


    /**
     *
     * @param schedules
     */
	private void performReIndex(Collection<Schedule> schedules) {
		try {
			Map<String,Object> param = new HashMap<String,Object>();
	        eventPublisher.publish(new SingleScheduleEvent(schedules, param, com.thed.zephyr.je.event.EventType.EXECUTION_UPDATED));
		} catch (Exception e) {
			log.error("", e);
		}
	}
	
	/**
	 * 
	 * @param entityEvent
	 * @param author
	 * @param cycle
	 * @return
	 */
	private Map<String, Object> createChangeGroupProperties(String entityEvent, String author, Integer issueId, Long projectId) {
		Map<String, Object> changeGroupProperties = new HashMap<String, Object>(8);
		changeGroupProperties.put("ZEPHYR_ENTITY_ID", issueId);
		changeGroupProperties.put("ISSUE_ID", issueId);
		changeGroupProperties.put("CYCLE_ID", -1);
		changeGroupProperties.put("SCHEDULE_ID", -1);
		changeGroupProperties.put("ZEPHYR_ENTITY_TYPE", EntityType.ISSUE.getEntityType());
		changeGroupProperties.put("ZEPHYR_ENTITY_EVENT", entityEvent);
		changeGroupProperties.put("PROJECT_ID", projectId);
		changeGroupProperties.put("AUTHOR", author);
		changeGroupProperties.put("CREATED", System.currentTimeMillis());
		return changeGroupProperties;
	}
}