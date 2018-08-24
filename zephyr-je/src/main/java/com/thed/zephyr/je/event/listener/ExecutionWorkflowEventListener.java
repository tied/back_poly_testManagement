package com.thed.zephyr.je.event.listener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.thed.zephyr.je.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.google.common.collect.Table;
import com.thed.zephyr.je.audit.service.AuditManager;
import com.thed.zephyr.je.model.Schedule;

/**
 * @author manjunath
 *
 */
public class ExecutionWorkflowEventListener implements InitializingBean, DisposableBean {

	private static final Logger log = LoggerFactory.getLogger(ExecutionWorkflowEventListener.class);

	private final EventPublisher eventPublisher;
	private final AuditManager auditManager;
	
	public ExecutionWorkflowEventListener(EventPublisher eventPublisher, AuditManager auditManager) {
		log.debug("Initializing Execution Workflow EventListener");
		this.eventPublisher = eventPublisher;
		this.auditManager = auditManager;
	}
	
	@Override
	public void destroy() throws Exception {
		eventPublisher.unregister(this);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		eventPublisher.register(this);
	}

	@EventListener
    public void onScheduleEvent(ExecutionWorkflowModifyEvent executionWorkflowModifyEvent) {    	
    	String author = executionWorkflowModifyEvent.getUserName();
    	if(executionWorkflowModifyEvent.getEventType().equals(EventType.EXECUTION_WORKFLOW_UPDATED)) {
    		Table<String, String, Object> changePropertyTable = executionWorkflowModifyEvent.getChangePropertyTable(); 
    		Schedule schedule = executionWorkflowModifyEvent.getSchedule();
			if (null != changePropertyTable && !changePropertyTable.isEmpty()) {
				Map<String, Object> changeGroupProperties = createChangeGroupProperties(executionWorkflowModifyEvent.getEventType().toString(), author, schedule);
                auditManager.saveZephyrChangeLog(changeGroupProperties, changePropertyTable);
			}
    	}
    }
	
	private Map<String, Object> createChangeGroupProperties(String entityEvent, String author, Schedule schedule) {
		Map<String, Object> changeGroupProperties = new HashMap<String, Object>(8);
		changeGroupProperties.put("ZEPHYR_ENTITY_ID", schedule.getID());
		changeGroupProperties.put("ISSUE_ID", schedule.getIssueId());
		changeGroupProperties.put("CYCLE_ID", schedule.getCycle().getID());
		changeGroupProperties.put("SCHEDULE_ID", schedule.getID());	  	    		
		changeGroupProperties.put("ZEPHYR_ENTITY_TYPE", EntityType.SCHEDULE_WORKFLOW.getEntityType());
		changeGroupProperties.put("ZEPHYR_ENTITY_EVENT", entityEvent);
		changeGroupProperties.put("AUTHOR", author);
		changeGroupProperties.put("CREATED", System.currentTimeMillis());
		return changeGroupProperties;
	}

}
