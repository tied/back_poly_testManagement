package com.thed.zephyr.je.job;

import com.atlassian.scheduler.compat.JobHandler;
import com.atlassian.scheduler.compat.JobHandlerKey;
import com.thed.zephyr.util.ConfigurationConstants;

public interface ZFJJobRunner extends JobHandler {
	  /** Our job runner key */
	JobHandlerKey ZFJ_JOB = JobHandlerKey.of(ConfigurationConstants.JOB_NAME);

    /** The prefix we use for construction our job keys */
    String JOB_KEY_PREFIX = "ZFJ:";
}
