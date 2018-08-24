package com.thed.zephyr.je.index.cluster;

import com.atlassian.sal.api.scheduling.PluginJob;
import org.apache.log4j.Logger;

import java.util.Map;

public class ZFJDeletionProcessJobRunnerImpl implements PluginJob
{
    private static final Logger log = Logger.getLogger(ZFJDeletionProcessJobRunnerImpl.class);

    public ZFJDeletionProcessJobRunnerImpl() {
    }

    @Override
    public void execute(final Map<String, Object> jobDataMap)
    {
        log.debug("Triggering ZFJ Process For Index Deletion.");

        // This data is retrieved from the jobDataMap as objects aren't supposed to be initialised in the PluginJob constructor.
        ExecClusterIndexCleanService execClusterIndexCleanService = (ExecClusterIndexCleanService)jobDataMap.get("execClusterIndexCleanService");

        // 1. Run checks and store results
        execClusterIndexCleanService.deleteMessages();
        log.debug("Completed Index Deletion Process..");
    }
}
