package com.thed.zephyr.je.event.listener;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.google.common.collect.Table;
import com.thed.zephyr.je.audit.service.AuditManager;
import com.thed.zephyr.je.event.*;
import com.thed.zephyr.je.model.StepResult;
import com.thed.zephyr.je.model.Teststep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.Collection;
import java.util.Map;

public class StepEventListener implements InitializingBean, DisposableBean  {
	private static final Logger log = LoggerFactory.getLogger(StepEventListener.class);
	
	private final EventPublisher eventPublisher;
	private final AuditManager auditManager;

    /**
     * Constructor.
     * @param eventPublisher injected {@code EventPublisher} implementation.
     * @param auditManager injected {@code AuditManager} implementation.
     */	
	public StepEventListener(EventPublisher eventPublisher, AuditManager auditManager) {
		log.debug("# Initializing StepEventListener");
        this.eventPublisher = eventPublisher;
        this.auditManager = auditManager;
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

    @EventListener
    public void onZephyrEvent(ZephyrEvent zephyrEvent) {
        //TBI
    }
    
    @EventListener
    public void onScheduleEvent(TeststepModifyEvent testStepModifyEvent) {    	
    	String author = testStepModifyEvent.getUserName();
    	if(testStepModifyEvent.getEventType().equals(EventType.TESTSTEP_UPDATED)) {    
    		Table<String, String, Object> changePropertyTable = testStepModifyEvent.getChangePropertyTable(); 
    		Teststep testStep = testStepModifyEvent.getTestStep();
			if (null != changePropertyTable && !changePropertyTable.isEmpty()) {
                Map<String, Object> changeGroupProperties = AuditUtils.createChangeGroupProperties(testStepModifyEvent.getEventType().toString(), EntityType.TESTSTEP.getEntityType(),
                        testStep, author, testStep.getIssueId().intValue(), -1, -1L);

                auditManager.saveZephyrChangeLog(changeGroupProperties, changePropertyTable);
			}
    	} else if (testStepModifyEvent.getEventType().equals(EventType.TESTSTEP_DELETED)) {
	    	Collection<Teststep> testSteps = testStepModifyEvent.getTestSteps();
	    	for (Teststep testStep : testSteps) {
	    		Map<String, Object> changeGroupProperties = AuditUtils.createChangeGroupProperties(testStepModifyEvent.getEventType().toString(), EntityType.TESTSTEP.getEntityType(),
                        testStep, author, testStep.getIssueId().intValue(), -1, -1L);
				
				Map<String, Object> changeLogProperties = AuditUtils.createDeleteChangePropertiesFor(testStep.getID(), EntityType.TESTSTEP.getEntityType(), null, String.valueOf(testStep.getID()));
	    		auditManager.removeZephyrChangeLogs(changeGroupProperties, changeLogProperties);
			}
    	}
    }	
    
    @EventListener
    public void onScheduleEvent(StepResultModifyEvent stepResultModifyEvent) {    	
    	String author = stepResultModifyEvent.getUserName();
    	if(stepResultModifyEvent.getEventType().equals(EventType.STEPRESULT_UPDATED) || 
    			stepResultModifyEvent.getEventType().equals(EventType.STEPRESULT_ATTACHMENT_ADDED)) {    
    		Table<String, String, Object> changePropertyTable = stepResultModifyEvent.getChangePropertyTable(); 
    		StepResult stepResult = stepResultModifyEvent.getStepResult();    		
			if (null != changePropertyTable && !changePropertyTable.isEmpty()) {
				Map<String, Object> changeGroupProperties = AuditUtils.createChangeGroupProperties(stepResultModifyEvent.getEventType().toString(), EntityType.STEPRESULT.getEntityType(),
                        stepResult, author, -1, stepResult.getScheduleId(), stepResult.getProjectId());

                auditManager.saveZephyrChangeLog(changeGroupProperties, changePropertyTable);
			}    		
    	} else if (stepResultModifyEvent.getEventType().equals(EventType.STEPRESULT_DELETED)) {
	    	Collection<StepResult> stepResults = stepResultModifyEvent.getStepResults();
	    	for (StepResult stepResult : stepResults) {
	    		Map<String, Object> changeGroupProperties = AuditUtils.createChangeGroupProperties(stepResultModifyEvent.getEventType().toString(), EntityType.STEPRESULT.getEntityType(),
                        stepResult, author, -1, stepResult.getScheduleId(), stepResult.getProjectId());

				Map<String, Object> changeLogProperties = AuditUtils.createDeleteChangePropertiesFor(stepResult.getID(), EntityType.STEPRESULT.getEntityType(), EntityType.STEPRESULT.getEntityType(), String.valueOf(stepResult.getID()));
	    		auditManager.removeZephyrChangeLogs(changeGroupProperties, changeLogProperties);
			}
    	} else if (stepResultModifyEvent.getEventType().equals(EventType.STEPRESULT_ATTACHMENT_DELETED)) {
    		StepResult stepResult = stepResultModifyEvent.getStepResult();
            Table<String, String, Object> changePropertyTable = stepResultModifyEvent.getChangePropertyTable();
            if (null != changePropertyTable && !changePropertyTable.isEmpty()) {
                Map<String, Object> changeGroupProperties = AuditUtils.createChangeGroupProperties(stepResultModifyEvent.getEventType().toString(), EntityType.STEPRESULT.getEntityType(),
                        stepResult, author, -1, stepResult.getScheduleId(), stepResult.getProjectId());
//                String attachmentFileName = null != changePropertyTable.get("ATTACHMENT", ApplicationConstants.OLD) ? changePropertyTable.get("ATTACHMENT", ApplicationConstants.OLD).toString() : "" ;
//                Map<String, Object> changeItemProperties = AuditUtils.createDeleteChangePropertiesFor(stepResult.getID(), EntityType.STEPRESULT.getEntityType(), "ATTACHMENT", attachmentFileName);
//                auditManager.removeZephyrChangeLogs(changeGroupProperties, changeItemProperties);
                auditManager.saveZephyrChangeLog(changeGroupProperties, changePropertyTable);
            }
    	}
    }
}
