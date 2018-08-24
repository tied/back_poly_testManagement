package com.thed.zephyr.je.event.listener;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.issue.index.IndexException;
import com.atlassian.jira.task.context.Contexts;
import com.atlassian.jira.util.collect.CollectionEnclosedIterable;
import com.atlassian.jira.util.collect.EnclosedIterable;
import com.google.common.collect.Table;
import com.thed.zephyr.je.audit.service.AuditManager;
import com.thed.zephyr.je.event.*;
import com.thed.zephyr.je.index.ScheduleIndexManager;
import com.thed.zephyr.je.index.cluster.MessageHandler;
import com.thed.zephyr.je.index.cluster.ZFJMessage;
import com.thed.zephyr.je.index.cluster.ZFJMessage.ZFJMessageType;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.ZephyrComponentAccessor;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ScheduleEventListener implements InitializingBean, DisposableBean  {
	private static final Logger log = LoggerFactory.getLogger(ZephyrIssueEventListener.class);
	
	private final EventPublisher eventPublisher;
	private final ScheduleIndexManager scheduleIndexManager;
	private final AuditManager auditManager;
	
	public ScheduleEventListener(EventPublisher eventPublisher, ScheduleIndexManager scheduleIndexManager,  AuditManager auditManager) {
        this.eventPublisher = eventPublisher;
        this.scheduleIndexManager = scheduleIndexManager;
        this.auditManager = auditManager;
    }

    @EventListener
    public void onZephyrEvent(ZephyrEvent zephyrEvent) {
        //TBI
    }
    
    @EventListener
    public void onScheduleEvent(SingleScheduleEvent scheduleEvent) {
    	if(scheduleEvent.getEventType().equals(EventType.EXECUTION_ADDED) || 
    			scheduleEvent.getEventType().equals(EventType.EXECUTION_UPDATED) 
    			|| scheduleEvent.getEventType().equals(EventType.CYCLE_MOVED)
    			|| scheduleEvent.getEventType().equals(EventType.CYCLE_UPDATED)
    			|| scheduleEvent.getEventType().equals(EventType.FOLDER_UPDATED)) {
	    	reIndexSchedules(scheduleEvent);
    	} else if (scheduleEvent.getEventType().equals(EventType.EXECUTION_DELETED) || scheduleEvent.getEventType().equals(EventType.CYCLE_DELETED)
    			|| scheduleEvent.getEventType().equals(EventType.FOLDER_DELETED) || scheduleEvent.getEventType().equals(EventType.PROJECT_DELETED)) {
	    	deleteIndexByTerm(scheduleEvent);  	
    	}
    }

	/**
	 * Reindexes Schedules
	 * @param scheduleEvent
	 */
	private void reIndexSchedules(SingleScheduleEvent scheduleEvent) {
		try {
			EnclosedIterable<Schedule> schedules = CollectionEnclosedIterable.copy(scheduleEvent.getSchedules());
			scheduleIndexManager.reIndexSchedule(schedules, Contexts.nullContext());
		} catch (IndexException e) {
			log.error("Index Schedules Failed", e);
		}
	}

	
	
	/**
	 * Deletes Index By Term
	 * Passing ENTITY_TYPE:Term and List of Ids
	 * @param scheduleEvent
	 */
	private void deleteIndexByTerm(SingleScheduleEvent scheduleEvent) {
		try {
			String term = (String) scheduleEvent.getParams().get("ENTITY_TYPE");
			if(scheduleEvent.getParams().containsKey("ENTITY_VALUE")) {
				Collection<String> entityIds = (Collection<String>) scheduleEvent.getParams().get("ENTITY_VALUE");
				if(entityIds != null || entityIds.size() > 0) {
		    		EnclosedIterable<String> entityIdIterables = CollectionEnclosedIterable.copy(entityIds);
		    		boolean isTermDeletedInIndexForCurrentNode = scheduleIndexManager.deleteBatchIndexByTerm(entityIdIterables, term, Contexts.nullContext());
		    		//Trigger Delete Indexing on other Nodes..
					if(StringUtils.equalsIgnoreCase(term,ApplicationConstants.CYCLE_IDX)) {
						MessageHandler messageHandler = (MessageHandler)ZephyrComponentAccessor.getInstance().getComponent("messageHandler");
						messageHandler.sendDeletionMessage(ZFJMessage.fromString(ZFJMessageType.DELETE_CYCLE.getMessageType()), entityIdIterables, isTermDeletedInIndexForCurrentNode);
					} else if(StringUtils.equalsIgnoreCase(term,ApplicationConstants.FOLDER_IDX)) {
						MessageHandler messageHandler = (MessageHandler)ZephyrComponentAccessor.getInstance().getComponent("messageHandler");
						messageHandler.sendDeletionMessage(ZFJMessage.fromString(ZFJMessageType.DELETE_FOLDER.getMessageType()), entityIdIterables, isTermDeletedInIndexForCurrentNode);
					}  else if(StringUtils.equalsIgnoreCase(term,ApplicationConstants.PROJECT_ID_IDX)) {
						MessageHandler messageHandler = (MessageHandler)ZephyrComponentAccessor.getInstance().getComponent("messageHandler");
						messageHandler.sendDeletionMessage(ZFJMessage.fromString(ZFJMessageType.DELETE_PROJECT.getMessageType()), entityIdIterables, isTermDeletedInIndexForCurrentNode);
					}  else if(StringUtils.equalsIgnoreCase(term,ApplicationConstants.ISSUE_ID_IDX)) {
						MessageHandler messageHandler = (MessageHandler)ZephyrComponentAccessor.getInstance().getComponent("messageHandler");
						messageHandler.sendDeletionMessage(ZFJMessage.fromString(ZFJMessageType.DELETE_ISSUE.getMessageType()), entityIdIterables, isTermDeletedInIndexForCurrentNode);
					}  else {
						MessageHandler messageHandler = (MessageHandler) ZephyrComponentAccessor.getInstance().getComponent("messageHandler");
						messageHandler.sendDeletionMessage(ZFJMessage.fromString(ZFJMessageType.DELETE_EXECUTION.getMessageType()), entityIdIterables, isTermDeletedInIndexForCurrentNode);
					}
				}
			}
		} catch (Exception e) {
			log.error("Index Deletion Failed", e);
		}
	}
	
    @EventListener
    public void onScheduleEvent(ScheduleModifyEvent scheduleModifyEvent) {
    	String author = scheduleModifyEvent.getUserName();
		if(scheduleModifyEvent.getEventType().equals(EventType.EXECUTION_ADDED) ||
				scheduleModifyEvent.getEventType().equals(EventType.EXECUTION_UPDATED) ||
    			scheduleModifyEvent.getEventType().equals(EventType.EXECUTION_ATTACHMENT_ADDED) ||
                scheduleModifyEvent.getEventType().equals(EventType.EXECUTION_CUSTOMFIELD_UPDATED)) {
    		Table<String, String, Object> changePropertyTable = scheduleModifyEvent.getChangePropertyTable(); 
        	Schedule schedule = scheduleModifyEvent.getSchedule();
			if (null != changePropertyTable && !changePropertyTable.isEmpty()) {
				Integer cycleId = (schedule.getCycle() != null) ? schedule.getCycle().getID() : ApplicationConstants.AD_HOC_CYCLE_ID;
				Map<String, Object> changeGroupProperties = createChangeGroupProperties(scheduleModifyEvent.getEventType().toString(), schedule, schedule.getIssueId(), cycleId, author);

                auditManager.saveZephyrChangeLog(changeGroupProperties, changePropertyTable);
			}    		
    	} else if (scheduleModifyEvent.getEventType().equals(EventType.EXECUTION_DELETED)) {
    		Collection<Schedule> schedules = scheduleModifyEvent.getSchedules();
	    	for (Schedule schedule : schedules) {
	    		Integer cycleId = (schedule.getCycle() != null) ? schedule.getCycle().getID() : ApplicationConstants.AD_HOC_CYCLE_ID;
	    		Map<String, Object> changeGroupProperties = createChangeGroupProperties(scheduleModifyEvent.getEventType().toString(), schedule, schedule.getIssueId(), cycleId, author);
	    		Map<String, Object> changeLogProperties = AuditUtils.createDeleteChangePropertiesFor(schedule.getID(), EntityType.SCHEDULE.getEntityType(), "EXECUTION_ID", String.valueOf(schedule.getID()));

                auditManager.removeZephyrChangeLogs(changeGroupProperties, changeLogProperties);
			}	    	
    	} else if (scheduleModifyEvent.getEventType().equals(EventType.EXECUTION_ATTACHMENT_DELETED)) {
    		Table<String, String, Object> changePropertyTable = scheduleModifyEvent.getChangePropertyTable(); 
    		Schedule schedule = scheduleModifyEvent.getSchedule();
            if (null != changePropertyTable && !changePropertyTable.isEmpty()) {
                Integer cycleId = (schedule.getCycle() != null) ? schedule.getCycle().getID() : ApplicationConstants.AD_HOC_CYCLE_ID;
                Map<String, Object> changeGroupProperties = createChangeGroupProperties(scheduleModifyEvent.getEventType().toString(), schedule, schedule.getIssueId(), cycleId, author);

                //String attachmentFileName = null != changePropertyTable.get("ATTACHMENT", ApplicationConstants.OLD) ? changePropertyTable.get("ATTACHMENT", ApplicationConstants.OLD).toString() : "" ;
                //Map<String, Object> changeItemProperties = AuditUtils.createDeleteChangePropertiesFor(schedule.getID(), EntityType.SCHEDULE.getEntityType(), "ATTACHMENT", attachmentFileName);
                //auditManager.removeZephyrChangeLogs(changeGroupProperties, changeItemProperties);
                auditManager.saveZephyrChangeLog(changeGroupProperties, changePropertyTable);
            }
    	}
    }	
    
    /**
     * 
     * @param entityEvent
     * @param schedule
     * @param issueId
     * @param cycleId
     * @param author
     * @return
     */
	 
	private Map<String, Object> createChangeGroupProperties(String entityEvent, Schedule schedule, Integer issueId, Integer cycleId, String author) {
		Map<String, Object> changeGroupProperties = new HashMap<String, Object>(8);
		changeGroupProperties.put("ZEPHYR_ENTITY_ID", schedule.getID());
		changeGroupProperties.put("ISSUE_ID", issueId);
		changeGroupProperties.put("CYCLE_ID", cycleId);
		changeGroupProperties.put("SCHEDULE_ID", schedule.getID());	  	    		
		changeGroupProperties.put("ZEPHYR_ENTITY_TYPE", EntityType.SCHEDULE.getEntityType());
		changeGroupProperties.put("ZEPHYR_ENTITY_EVENT", entityEvent);
		changeGroupProperties.put("PROJECT_ID", schedule.getProjectId());
		changeGroupProperties.put("AUTHOR", author);
		changeGroupProperties.put("CREATED", System.currentTimeMillis());
		return changeGroupProperties;
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
     * Called when the plugin has been enabled.
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        // register ourselves with the EventPublisher
        eventPublisher.register(this);
    }
}
