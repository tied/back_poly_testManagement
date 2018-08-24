package com.thed.zephyr.je.event.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.atlassian.crowd.event.user.UserDeletedEvent;
import com.atlassian.crowd.event.user.UserUpdatedEvent;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.thed.zephyr.je.service.ScheduleManager;

/**
 * JIRA listener to listen for JIRA User events (User Modified/delete).
 */
public class ZephyrUserEventListener  implements InitializingBean, DisposableBean {
 
    private static final Logger log = LoggerFactory.getLogger(ZephyrUserEventListener.class);
 
    private final EventPublisher eventPublisher;
    private final ScheduleManager scheduleManager;

    /**
     * Constructor.
     * @param eventPublisher injected {@code EventPublisher} implementation.
     */
    public ZephyrUserEventListener(EventPublisher eventPublisher, ScheduleManager scheduleManager) {
        this.eventPublisher = eventPublisher;
        this.scheduleManager = scheduleManager;
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
     * Receives any {@code UserDeletedEvent}s sent by JIRA.
     * @param userDeletedEvent the UserDeletedEvent passed to us
     */
    @EventListener
	public void onUserEvent(UserDeletedEvent userDeletedEvent) {
		String userName = userDeletedEvent.getUsername();
		/* Commented the log entry as part of ZFJ customer ticket (21634) since we are not doing performing any action here.
		log.info("User {} has been deleted, starting schedule cleanup", userName);
		*/
		// In case of JIRA User deletion, nothing to Clean up here ...
		//this.scheduleManager.removeSchedulesByUserId(userName);
	} 
    
    /**
     * Receives any {@code UserUpdatedEvent}s sent by JIRA.
     * @param userUpdateEvent the UserUpdatedEvent passed to us
     */
    @EventListener
	public void onUserEvent(UserUpdatedEvent userUpdateEvent) {
		String userName = userUpdateEvent.getUser().getDisplayName();
		/* Commented the log entry as part of ZFJ customer ticket (21634) since we are not doing performing any action here.
		log.info("User {} has been updated, starting schedule cleanup",userName);
		*/
		// In case of JIRA User deletion, Clean up here ...
		//this.scheduleManager.removeSchedulesByUserId(userName);
	} 
}