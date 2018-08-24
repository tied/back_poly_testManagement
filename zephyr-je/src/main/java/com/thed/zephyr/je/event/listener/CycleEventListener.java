package com.thed.zephyr.je.event.listener;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.google.common.collect.Table;
import com.thed.zephyr.je.audit.service.AuditManager;
import com.thed.zephyr.je.event.CycleModifyEvent;
import com.thed.zephyr.je.event.EntityType;
import com.thed.zephyr.je.event.EventType;
import com.thed.zephyr.je.event.SingleScheduleEvent;
import com.thed.zephyr.je.event.ZephyrEvent;
import com.thed.zephyr.je.model.Cycle;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.service.ScheduleManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CycleEventListener  implements InitializingBean, DisposableBean  {
	private static final Logger log = LoggerFactory.getLogger(CycleEventListener.class);
	
	private final EventPublisher eventPublisher;
	private final AuditManager auditManager;
	private final ScheduleManager scheduleManager;

    /**
     * Constructor.
     * @param eventPublisher injected {@code EventPublisher} implementation.
     * @param auditManager injected {@code AuditManager} implementation.
     */	
	public CycleEventListener(EventPublisher eventPublisher, AuditManager auditManager,ScheduleManager scheduleManager) {
		log.debug("# Initializing CycleEventListener");
        this.eventPublisher = eventPublisher;
        this.auditManager = auditManager;
        this.scheduleManager=scheduleManager;
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
    public void onScheduleEvent(CycleModifyEvent cycleModifyEvent) {    	
    	String author = cycleModifyEvent.getUserName();
    	if(cycleModifyEvent.getEventType().equals(EventType.CYCLE_UPDATED)) {    
    		Table<String, String, Object> changePropertyTable = cycleModifyEvent.getChangePropertyTable(); 
        	Cycle cycle = cycleModifyEvent.getCycle();
			if (null != changePropertyTable && !changePropertyTable.isEmpty()) {
				Map<String, Object> changeGroupProperties = createChangeGroupProperties(cycleModifyEvent.getEventType().toString(), author, cycle);
                auditManager.saveZephyrChangeLog(changeGroupProperties, changePropertyTable);
			}    		
			//Trigger Schedule Index update
			if(changePropertyTable.containsRow("NAME")) {
	    		Collection<Schedule> schedules = new ArrayList<Schedule>();
				schedules.addAll(getSchedulesByCycle(cycle.getID()));
				eventPublisher.publish(new SingleScheduleEvent(schedules, new HashMap<String,Object>(), EventType.CYCLE_UPDATED));
			}
    	} else if (cycleModifyEvent.getEventType().equals(EventType.CYCLE_DELETED)) {
	    	Collection<Cycle> cycles = cycleModifyEvent.getCycles();
	    	for (Cycle cycle : cycles) {
	    		Map<String, Object> changeGroupProperties = createChangeGroupProperties(cycleModifyEvent.getEventType().toString(), author, cycle);

				Map<String, Object> changeLogProperties = AuditUtils.createDeleteChangePropertiesFor(cycle.getID(), EntityType.CYCLE.getEntityType(), null, cycle.getName());
                auditManager.removeZephyrChangeLogs(changeGroupProperties, changeLogProperties);
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
	private Map<String, Object> createChangeGroupProperties(String entityEvent, String author, Cycle cycle) {
		Map<String, Object> changeGroupProperties = new HashMap<String, Object>(8);
		changeGroupProperties.put("ZEPHYR_ENTITY_ID", cycle.getID());
		changeGroupProperties.put("ISSUE_ID", -1);
		changeGroupProperties.put("CYCLE_ID", cycle.getID());
		changeGroupProperties.put("SCHEDULE_ID", -1);	  	    		
		changeGroupProperties.put("ZEPHYR_ENTITY_TYPE", EntityType.CYCLE.getEntityType());
		changeGroupProperties.put("ZEPHYR_ENTITY_EVENT", entityEvent);
		changeGroupProperties.put("PROJECT_ID", cycle.getProjectId());
		changeGroupProperties.put("AUTHOR", author);
		changeGroupProperties.put("CREATED", System.currentTimeMillis());
		return changeGroupProperties;
	}	
	
	private List<Schedule> getSchedulesByCycle(Integer cycleId) {
		return scheduleManager.getSchedulesByCycleId(null, null, cycleId, null, null, null);
	}
}
