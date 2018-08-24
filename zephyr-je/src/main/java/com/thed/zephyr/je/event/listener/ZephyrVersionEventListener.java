package com.thed.zephyr.je.event.listener;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.fugue.Option;
import com.atlassian.jira.event.project.AbstractVersionEvent;
import com.atlassian.jira.event.project.VersionDeleteEvent;
import com.atlassian.jira.event.project.VersionMergeEvent;
import com.atlassian.jira.project.version.Version;
import com.thed.zephyr.je.service.CycleManager;
import com.thed.zephyr.je.service.ScheduleManager;
import com.thed.zephyr.util.ApplicationConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;



public class ZephyrVersionEventListener implements InitializingBean, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(ZephyrVersionEventListener.class);
    
    private final EventPublisher eventPublisher;
    private final CycleManager cycleManager;
    private final ScheduleManager scheduleManager;

    public ZephyrVersionEventListener(EventPublisher eventPublisher, CycleManager cycleManager,ScheduleManager scheduleManager) {
    	this.eventPublisher = eventPublisher;
    	this.cycleManager = cycleManager;
    	this.scheduleManager=scheduleManager;
    }
    
    
	@Override
	public void destroy() throws Exception {
        eventPublisher.unregister(this);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
        eventPublisher.register(this);		
	}
	
	   /**
     * Receives any {@code IssueEvent}s sent by JIRA.
     * @param versionEvent the IssueEvent passed to us
     */
    @EventListener
    public void onVersionEvent(AbstractVersionEvent versionEvent) {
    	if(versionEvent instanceof VersionDeleteEvent) {
    		VersionDeleteEvent versionDeleteEvent = (VersionDeleteEvent)versionEvent;
    		Long deletedVersionId = versionDeleteEvent.getVersionId();
    		Option<Version> swappedVersion = versionDeleteEvent.getFixVersionSwappedTo();
    		if(!swappedVersion.isEmpty()) {
        		Long  targetVersionId = swappedVersion.get().getId();
        		mergeOrSwapVersion(targetVersionId, deletedVersionId);
    		} else {
	    		Long projectId = cycleManager.getDistinctProjectIdByVersionId(deletedVersionId);
	    		if(projectId == null || projectId.longValue() == 0l) {
	    			projectId = scheduleManager.getDistinctProjectIdByVersionId(deletedVersionId);
				}
				if (null != projectId && projectId.longValue() > 0l) {
					int noOfCycles = cycleManager.swapVersion(deletedVersionId, new Long(ApplicationConstants.UNSCHEDULED_VERSION_ID), projectId);
					log.debug("No. Of Cycles swapped to Unscheduled Version:" + noOfCycles);
				}
    		}
		}
    	
    	if(versionEvent instanceof VersionMergeEvent) {
        	//Move all schedules with the deleted version to version i in target. 
    		VersionMergeEvent versionMergeEvent = (VersionMergeEvent)versionEvent;
    		Long  targetVersionId = versionMergeEvent.getVersionId();
    		Long deletedVersionId = versionMergeEvent.getMergedVersionId();
    		mergeOrSwapVersion(targetVersionId, deletedVersionId);
    	}
    }


	private void mergeOrSwapVersion(Long targetVersionId, Long deletedVersionId) {
		Long projectId = cycleManager.getDistinctProjectIdByVersionId(deletedVersionId);
		if(projectId == null || projectId.longValue() == 0) {
			projectId = scheduleManager.getDistinctProjectIdByVersionId(deletedVersionId);
		}
		Integer noOfCycles = cycleManager.swapVersion(deletedVersionId, targetVersionId, projectId);
		log.debug("No. Of Cycles swapped:"+noOfCycles);
	}
}
