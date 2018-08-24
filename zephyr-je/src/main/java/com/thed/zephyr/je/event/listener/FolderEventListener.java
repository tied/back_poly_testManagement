package com.thed.zephyr.je.event.listener;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.google.common.collect.Table;
import com.thed.zephyr.je.audit.service.AuditManager;
import com.thed.zephyr.je.event.EntityType;
import com.thed.zephyr.je.event.EventType;
import com.thed.zephyr.je.event.FolderModifyEvent;
import com.thed.zephyr.je.event.SingleScheduleEvent;
import com.thed.zephyr.je.event.ZephyrEvent;
import com.thed.zephyr.je.model.Folder;
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

/**
 * Class handles the auditing for folder update and folder delete scenario.
 * 
 * @author manjunath
 *
 */
public class FolderEventListener  implements InitializingBean, DisposableBean  {
	
	private static final Logger log = LoggerFactory.getLogger(FolderEventListener.class);
	
	private final EventPublisher eventPublisher;
	private final AuditManager auditManager;
	private final ScheduleManager scheduleManager;

    /**
     * Constructor.
     * @param eventPublisher injected {@code EventPublisher} implementation.
     * @param auditManager injected {@code AuditManager} implementation.
     */	
	public FolderEventListener(EventPublisher eventPublisher, AuditManager auditManager, ScheduleManager scheduleManager) {
		log.debug("# Initializing FolderEventListener");
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
    public void onScheduleEvent(FolderModifyEvent folderModifyEvent) {    	
    	String author = folderModifyEvent.getUserName();
    	if(folderModifyEvent.getEventType().equals(EventType.FOLDER_UPDATED)) {    
    		Table<String, String, Object> changePropertyTable = folderModifyEvent.getChangePropertyTable(); 
        	Folder folder = folderModifyEvent.getFolder();
			if (null != changePropertyTable && !changePropertyTable.isEmpty()) {
				Map<String, Object> changeGroupProperties = createChangeGroupProperties(folderModifyEvent.getEventType().toString(), author, folder, folderModifyEvent.getProjectId(), folderModifyEvent.getCycleId().intValue());
                auditManager.saveZephyrChangeLog(changeGroupProperties, changePropertyTable);
			}    		
			//Trigger Schedule Index update
			if(changePropertyTable.containsRow("NAME")) {
	    		Collection<Schedule> schedules = new ArrayList<Schedule>();
				schedules.addAll(getSchedulesByCycleAndFolder(folderModifyEvent.getCycleId(), Long.valueOf(folder.getID()+"")));
				eventPublisher.publish(new SingleScheduleEvent(schedules, new HashMap<String,Object>(), EventType.FOLDER_UPDATED));
			}
    	} else if (folderModifyEvent.getEventType().equals(EventType.FOLDER_DELETED)) {
	    	Collection<Folder> folders = folderModifyEvent.getFolders();
	    	for (Folder folder : folders) {
	    		Map<String, Object> changeGroupProperties = createChangeGroupProperties(folderModifyEvent.getEventType().toString(), author, folder, folderModifyEvent.getProjectId(), folderModifyEvent.getCycleId().intValue());

				Map<String, Object> changeLogProperties = AuditUtils.createDeleteChangePropertiesFor(folder.getID(), EntityType.CYCLE.getEntityType(), null, folder.getName());
                auditManager.removeZephyrChangeLogs(changeGroupProperties, changeLogProperties);
			}
    	}
    }

	/**
	 *
	 * @param entityEvent
	 * @param author
	 * @param folder
	 * @return
	 */
	private Map<String, Object> createChangeGroupProperties(String entityEvent, String author, Folder folder, Long projectId, Integer cycleId) {
		Map<String, Object> changeGroupProperties = new HashMap<String, Object>(8);
		changeGroupProperties.put("ZEPHYR_ENTITY_ID", folder.getID());
		changeGroupProperties.put("ISSUE_ID", -1);
		changeGroupProperties.put("CYCLE_ID", cycleId);
		changeGroupProperties.put("SCHEDULE_ID", -1);	  	    		
		changeGroupProperties.put("ZEPHYR_ENTITY_TYPE", EntityType.FOLDER.getEntityType());
		changeGroupProperties.put("ZEPHYR_ENTITY_EVENT", entityEvent);
		changeGroupProperties.put("PROJECT_ID", projectId);
		changeGroupProperties.put("AUTHOR", author);
		changeGroupProperties.put("CREATED", System.currentTimeMillis());
		return changeGroupProperties;
	}	
	
	private List<Schedule> getSchedulesByCycleAndFolder(Long cycleId, Long folderId) {
		return scheduleManager.getSchedulesByCycleAndFolder(null, null, cycleId, -1, null, null, folderId);
	}
}

