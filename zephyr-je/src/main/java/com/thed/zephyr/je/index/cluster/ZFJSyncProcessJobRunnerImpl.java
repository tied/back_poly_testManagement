package com.thed.zephyr.je.index.cluster;

import com.atlassian.sal.api.scheduling.PluginJob;
import org.apache.log4j.Logger;


import java.util.Map;

public class ZFJSyncProcessJobRunnerImpl implements PluginJob
{
    private static final Logger log = Logger.getLogger(ZFJSyncProcessJobRunnerImpl.class);

    public ZFJSyncProcessJobRunnerImpl() {
    }

    @Override
    public void execute(final Map<String, Object> jobDataMap)
    {
        log.debug("Triggering ZFJ Process For Index Sync.");

        // This data is retrieved from the jobDataMap as objects aren't supposed to be initialised in the PluginJob constructor.
        MessageHandler messageHandler = (MessageHandler)jobDataMap.get("messageHandler");
        if(messageHandler != null) {
            // 1. Run checks and store results
            messageHandler.handleReceivedMessages();
            log.debug("Completed Index Sync Process..");
        } else {
            log.warn("Message handler could not trigger");
        }
    }
}
