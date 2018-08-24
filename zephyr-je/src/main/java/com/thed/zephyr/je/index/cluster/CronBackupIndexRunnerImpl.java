package com.thed.zephyr.je.index.cluster;

import org.apache.log4j.Logger;

import com.atlassian.scheduler.JobRunner;
import com.atlassian.scheduler.JobRunnerRequest;
import com.atlassian.scheduler.JobRunnerResponse;
import com.thed.zephyr.je.rest.delegate.ScheduleResourceDelegate;

public class CronBackupIndexRunnerImpl implements JobRunner{
	
	 private static final Logger log = Logger.getLogger(CronBackupIndexRunnerImpl.class);
	 
	 private final ScheduleResourceDelegate scheduleResourceDelegate;
	 
	 public CronBackupIndexRunnerImpl(ScheduleResourceDelegate scheduleResourceDelegate) {
		this.scheduleResourceDelegate = scheduleResourceDelegate;
	 }



	@Override
	public JobRunnerResponse runJob(JobRunnerRequest request) {
		try {
			log.debug("Trigger the indexAll and then take a backup of those index files");
			scheduleResourceDelegate.indexAll(Boolean.TRUE, Boolean.FALSE, Boolean.TRUE, null);
			log.debug("Backup index job execution completed.");
		} catch (Exception e) {
			log.debug("Error while took the backup index files", e);
		}
		return JobRunnerResponse.success();
	}

}
