package com.thed.zephyr.je.index.cluster;

import java.util.Calendar;

import org.apache.log4j.Logger;

import com.atlassian.scheduler.JobRunner;
import com.atlassian.scheduler.JobRunnerRequest;
import com.atlassian.scheduler.JobRunnerResponse;
import com.thed.zephyr.je.rest.delegate.ScheduleResourceDelegate;

public class CronSyncupRunnerImpl implements JobRunner{
	
	 private static final Logger log = Logger.getLogger(CronSyncupRunnerImpl.class);
	 
	 private final ScheduleResourceDelegate scheduleResourceDelegate;
	 
	 public CronSyncupRunnerImpl(ScheduleResourceDelegate scheduleResourceDelegate) {
		this.scheduleResourceDelegate = scheduleResourceDelegate;
	 }

	@Override
	public JobRunnerResponse runJob(JobRunnerRequest request) {
		try {
			log.debug("Syncup index job triggered.");
			scheduleResourceDelegate.indexAll(Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, null);
			log.debug("Syncup index job execution completed.");
		} catch (Exception e) {
			log.debug("Error while syncup the index ", e);
		}
		 return JobRunnerResponse.success();
	}

}
