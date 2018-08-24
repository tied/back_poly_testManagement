package com.thed.zephyr.je.index.cluster;

import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.atlassian.sal.api.scheduling.PluginScheduler;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.thed.zephyr.util.ConfigurationConstants;
import org.apache.log4j.Logger;

/**
 * Service Manager to start and stop Cluster Messaging scheduler service.
 * This class is responsible for cancelling the scheduler when the plugin is undeployed.
 *
 */
public class ClusterSchedulerServiceImpl implements ClusterSchedulerService, LifecycleAware
{
    protected final Logger log = org.apache.log4j.Logger.getLogger(ClusterSchedulerService.class);

    private final MessageHandler messageHandler;
    private final ExecClusterIndexCleanService execClusterIndexCleanService;

    private final PluginScheduler pluginScheduler;
    private long deleteInterval = 1000L * 60L * 20; // Once every 12 hours; starts when the instance first starts up
    private long syncInterval = 1000L * 60L * 5; // Once every 5 minutes; starts when the instance first starts up
//    private long deleteInterval = 1000L * 60L; // Once every 24 hours; starts when the instance first starts up
//    private long syncInterval = 1000L * 60L * 5L ; // Once every 24 hours; starts when the instance first starts up
    private Map<String, Object> jobData = new HashMap<>();


    public ClusterSchedulerServiceImpl(final PluginScheduler pluginScheduler,
    		final MessageHandler messageHandler,ExecClusterIndexCleanService execClusterIndexCleanService) {
        this.pluginScheduler = pluginScheduler;
        this.messageHandler= messageHandler;
        this.execClusterIndexCleanService=execClusterIndexCleanService;
    }

    @Override
    public void reschedule(long deleteInterval,long syncInterval)
    {
        this.deleteInterval = deleteInterval;
        this.syncInterval=syncInterval;
        this.jobData.put("messageHandler", this.messageHandler);
        this.jobData.put("execClusterIndexCleanService", this.execClusterIndexCleanService);

        pluginScheduler.scheduleJob(
                ConfigurationConstants.DELETION_SYNC_INDEX_JOB_NAME,
                ZFJDeletionProcessJobRunnerImpl.class,
                this.jobData,
                new Date(System.currentTimeMillis()),
                deleteInterval);

        log.debug(String.format("Scheduled Deletion Process Job running every %dms", deleteInterval));

        pluginScheduler.scheduleJob(
                ConfigurationConstants.INDEX_SYNC_INDEX_JOB_NAME,
                ZFJSyncProcessJobRunnerImpl.class,
                this.jobData,
                new Date(System.currentTimeMillis()),
                syncInterval);
        log.debug(String.format("Scheduled Index Sync Process Job running every %dms", syncInterval));
    }

    @Override
    public void onStart() {
        try
        {
            // Unschedule any job if it already exists:
            log.debug("Unscheduling ZFJ Deletion Job and Sync Job set earlier");
            pluginScheduler.unscheduleJob(ConfigurationConstants.DELETION_SYNC_INDEX_JOB_NAME);
            pluginScheduler.unscheduleJob(ConfigurationConstants.INDEX_SYNC_INDEX_JOB_NAME);
        }
        catch (IllegalArgumentException e)
        {
            log.debug("Attempting to remove the previous job failed - likely because it did not exist "
                    + "previously. This should be safe to ignore");
        }

        // Schedule the new job:
        log.debug("Scheduling the Deletion job");
        reschedule(deleteInterval,syncInterval);

        // Done!
        log.debug("Startup complete");
    }
}
