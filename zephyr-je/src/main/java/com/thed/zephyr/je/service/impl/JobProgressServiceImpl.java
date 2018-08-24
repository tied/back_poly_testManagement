package com.thed.zephyr.je.service.impl;

import com.google.common.collect.Maps;
import com.thed.zephyr.je.operation.JobProgress;
import com.thed.zephyr.je.rest.exception.JobFailedException;
import com.thed.zephyr.je.service.JobProgressService;
import com.thed.zephyr.je.service.ZFJCacheService;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.UniqueIdGenerator;
import com.thed.zephyr.util.ZephyrUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class JobProgressServiceImpl implements JobProgressService {

    private static final Logger log = LoggerFactory.getLogger(JobProgressServiceImpl.class);

    private final ZFJCacheService zfjCacheService;

    public JobProgressServiceImpl(ZFJCacheService zfjCacheService) {
        this.zfjCacheService = zfjCacheService;
    }

    public JobProgress createJobProgress(String name, Integer status, String jobProgressTicket) {
        jobProgressTicket = StringUtils.isNotBlank(jobProgressTicket) ? jobProgressTicket : new UniqueIdGenerator().getStringId();
        JobProgress jobProgress = new JobProgress(jobProgressTicket, name, status, new Date(), 1, 0);
        jobProgress.setCanceledJob(false);//to avoid early NPE
        setJobProgressToCache(jobProgressTicket, jobProgress);
        log.info("Job Progress instance was created and pushed to cache jobProgressTicket: " + jobProgressTicket + " jobName: " + name );
        return jobProgress;
    }
    
    public JobProgress createJobProgressWithNoSteps(String name, Integer status, String jobProgressTicket) {
        jobProgressTicket = StringUtils.isNotBlank(jobProgressTicket) ? jobProgressTicket : new UniqueIdGenerator().getStringId();
        JobProgress jobProgress = new JobProgress(jobProgressTicket, name, status, new Date(), 0, 0);
        setJobProgressToCache(jobProgressTicket, jobProgress);
        log.info("Job Progress instance was created and pushed to cache jobProgressTicket: " + jobProgressTicket + " jobName: " + name );
        return jobProgress;
    }

    public JobProgress completedWithStatus(Integer status, String jobProgressTicket)  {
        JobProgress jobProgress = getJobProgress(jobProgressTicket);
        if (jobProgress != null) {
            jobProgress.setStatus(status);
            jobProgress.setEndTime(new Date());
            setJobProgressToCache(jobProgressTicket, jobProgress);
            log.info("Job Progress was completed jobProgressTicket: " + jobProgressTicket + " jobName: " + jobProgress.getName() + " status: " + status );
        }

        return jobProgress;
    }

    @Override
    public Map<String, Object> completedWithStatusStop(final Integer status, final String jobProgressTicket, String user) {
        JobProgress jobProgress = getJobProgress(jobProgressTicket);
        if (jobProgress != null) {
            jobProgress.setStatus(status);
            jobProgress.setEndTime(new Date());
            jobProgress.setJobStoppedBy(user);
            jobProgress.setProgress(1l);
            jobProgress.setCanceledJob(true);
            jobProgress.setCompletedSteps(jobProgress.getTotalSteps());
            setJobProgressToCache(jobProgressTicket, jobProgress);
            log.info("Job Progress was completed jobProgressTicket: " + jobProgressTicket + " jobName: " + jobProgress.getName() + " status: " + status );
        }
        return convertJobProgressToMap(jobProgress, 1.0);
    }
    @Override
    public JobProgress setTotalSteps( String cacheKey, Integer totalSteps)  {
        JobProgress jobProgress = getJobProgress(cacheKey);
        if (jobProgress != null) {
            jobProgress.setTotalSteps(totalSteps);
            setJobProgressToCache(cacheKey, jobProgress);
        }
        return jobProgress;
    }

    @Override
    public Integer getTotalSteps(String cacheKey)  {
        JobProgress jobProgress = getJobProgress(cacheKey);
        Integer message = null;
        if (jobProgress != null) {
            message = jobProgress.getTotalSteps();
        }
        return message;
    }

    @Override
    public JobProgress addSteps(String cacheKey, Integer steps)  {
        JobProgress jobProgress = getJobProgress(cacheKey);
        if (jobProgress != null) {
            jobProgress.setTotalSteps(jobProgress.getTotalSteps() + steps);
            setJobProgressToCache(cacheKey, jobProgress);
        }
        return jobProgress;
    }

    @Override
    public JobProgress setCompletedSteps(String cacheKey, Integer completedSteps)  {
        JobProgress jobProgress = getJobProgress(cacheKey);
        if (jobProgress != null) {
            jobProgress.setCompletedSteps(completedSteps);
            setJobProgressToCache(cacheKey, jobProgress);
        }
        return jobProgress;
    }

    @Override
    public Integer getCompletedSteps(String cacheKey)  {
        JobProgress jobProgress = getJobProgress(cacheKey);
        Integer message = null;
        if (jobProgress != null) {
            message = jobProgress.getCompletedSteps();
        }
        return message;
    }

    @Override
    public JobProgress addCompletedSteps(String cacheKey, Integer completedSteps)  {
        JobProgress jobProgress = getJobProgress(cacheKey);
        if (jobProgress != null) {
            jobProgress.setCompletedSteps(jobProgress.getCompletedSteps() + completedSteps);
            setJobProgressToCache(cacheKey, jobProgress);
        }
        return jobProgress;
    }

    @Override
    public JobProgress addCompletedSteps(String cacheKey, JobProgress jobProgress, Integer completedSteps)  {
        if (jobProgress != null) {
            jobProgress.setCompletedSteps(jobProgress.getCompletedSteps() + completedSteps);
            setJobProgressToCache(cacheKey, jobProgress);
        }
        return jobProgress;
    }

    @Override
    public JobProgress setMessage(String cacheKey, String message)  {
        JobProgress jobProgress = getJobProgress(cacheKey);
        if (jobProgress != null) {
            jobProgress.setMessage(message);
            setJobProgressToCache(cacheKey, jobProgress);
        }
        return jobProgress;
    }

    @Override
    public JobProgress setStepMessage(String cacheKey, String message)  {
        JobProgress jobProgress = getJobProgress(cacheKey);
        if (jobProgress != null) {
            jobProgress.setStepMessage(message);
            setJobProgressToCache(cacheKey, jobProgress);
        }
        return jobProgress;
    }

    @Override
    public String getStepMessage(String cacheKey)  {
        JobProgress jobProgress = getJobProgress(cacheKey);
        String message = null;
        if (jobProgress != null) {
            message = jobProgress.getStepMessage();
        }
        return message;
    }

    @Override
    public JobProgress addStepMessages(String cacheKey, String message)  {
        JobProgress jobProgress = getJobProgress(cacheKey);
        if (jobProgress != null) {
            jobProgress.addStepMassages(message);
            setJobProgressToCache(cacheKey, jobProgress);
        }
        return jobProgress;
    }

    @Override
    public JobProgress addCurrentStepMessageToMessages(String cacheKey)  {
        JobProgress jobProgress = getJobProgress(cacheKey);
        if (jobProgress != null) {
            jobProgress.addStepMassages(jobProgress.getStepMessage());
            setJobProgressToCache(cacheKey, jobProgress);
        }
        return jobProgress;
    }

    @Override
    public List<String> getStepMessages(String cacheKey)  {
        JobProgress jobProgress = getJobProgress(cacheKey);
        List<String> message = null;
        if (jobProgress != null) {
            message = jobProgress.getStepMessages();
        }
        return message;
    }

    @Override
    public JobProgress setStepLabel(String cacheKey, String label)  {
        JobProgress jobProgress = getJobProgress(cacheKey);
        if (jobProgress != null) {
            jobProgress.setStepLabel(label);
            setJobProgressToCache(cacheKey, jobProgress);
        }
        return jobProgress;
    }

    @Override
    public String getStepLabel(String cacheKey)  {
        JobProgress jobProgress = getJobProgress(cacheKey);
        String message = null;
        if (jobProgress != null) {
            message = jobProgress.getStepLabel();
        }
        return message;
    }

    @Override
    public void setEntityWithId(String cacheKey, String entity, String id) {
        JobProgress jobProgress = getJobProgress(cacheKey);
         if (jobProgress != null) {
             jobProgress.setEntity(entity);
             jobProgress.setEntityId(id);
             setJobProgressToCache(cacheKey, jobProgress);
        }
     }

    @Override
    public String getSummaryMessage(String cacheKey)  {
        JobProgress jobProgress = getJobProgress(cacheKey);
        String message = null;
        if (jobProgress != null) {
            message = jobProgress.getSummaryMessage();
        }
        return message;
    }

    @Override
    public JobProgress setSummaryMessage(String cacheKey, String message)  {
        JobProgress jobProgress = getJobProgress(cacheKey);
        if (jobProgress != null) {
            jobProgress.setSummaryMessage(message);
            setJobProgressToCache(cacheKey, jobProgress);
        }
        return jobProgress;
    }

    @Override
    public String getErrorMessage(String cacheKey)  {
        JobProgress jobProgress = getJobProgress(cacheKey);
        String message = null;
        if (jobProgress != null) {
            message = jobProgress.getErrorMessage();
        }
        return message;
    }

    @Override
    public JobProgress setErrorMessage(String cacheKey, String message)  {
        JobProgress jobProgress = getJobProgress(cacheKey);
        if (jobProgress != null) {
            jobProgress.setErrorMessage(message);
            setJobProgressToCache(cacheKey, jobProgress);
        }
        return jobProgress;
    }

    @Override
    public JobProgress cancelJob(String cacheKey)  {
        JobProgress jobProgress = getJobProgress(cacheKey);
        if (jobProgress != null) {
            jobProgress.setCanceledJob(true);
            setJobProgressToCache(cacheKey, jobProgress);
        }
        return jobProgress;
    }

    @Override
    public Boolean isJobCanceled(String cacheKey)  {
        JobProgress jobProgress = getJobProgress(cacheKey);
        Boolean isCanceled = null;
        if (jobProgress != null) {
            isCanceled = jobProgress.getCanceledJob();
        }
        return isCanceled;
    }

    @Override
    public Map<String, Object> checkJobProgress(String cacheKey, String type) throws JobFailedException {
        JobProgress jobProgress = getJobProgress(cacheKey);
        if (jobProgress == null){
            return null;
        }
        Double progress = jobProgress.getProgress();
        if (ApplicationConstants.JOB_STATUS_COMPLETED == jobProgress.getStatus()) {
            progress = 1.0;
            zfjCacheService.removeCacheByKey(cacheKey);
            if(StringUtils.isNotBlank(type) &&
                    StringUtils.equalsIgnoreCase(type,"reindex_job_progress")) {
                log.info("Successfully Completed re-indexing.");
            }
        } else if (ApplicationConstants.JOB_STATUS_FAILED == jobProgress.getStatus()) {
            zfjCacheService.removeCacheByKey(cacheKey);
            String errorMessage = jobProgress.getErrorMessage() != null ? jobProgress.getErrorMessage() : "zephyr.common.internal.server.error";
            throw new JobFailedException(errorMessage);
        } else if (ApplicationConstants.JOB_STATUS_INPROGRESS == jobProgress.getStatus()) {
            if(StringUtils.isNotBlank(type) &&
                    StringUtils.equalsIgnoreCase(type,"reindex_job_progress") &&
                    jobProgress.getCompletedSteps().intValue() >= jobProgress.getTotalSteps().intValue()) {
                progress = 1.0;
                zfjCacheService.removeCacheByKey(cacheKey);
                log.info("Completed Steps Indexed Matches Total Steps. Marking the ReIndex as complete.");
            }
        }else if (ApplicationConstants.JOB_STATUS_STOPPED == jobProgress.getStatus()) {
            progress = 1.0;
            zfjCacheService.removeCacheByKey(cacheKey);
            if(StringUtils.isNotBlank(type) &&
                    StringUtils.equalsIgnoreCase(type,"import_create_issues_job_progress")) {
                log.info("Successfully Stopped the importer job.");
            }
        }

        return convertJobProgressToMap(jobProgress, progress);
    }

    @Override
    public Map<String, Object> convertJobProgressToMap(JobProgress jobProgress, Double progress) {
        if (progress == null) {
            progress = jobProgress.getProgress();
        }
        Map<String, Object> progressMap = Maps.newHashMap();
        progressMap.put("totalSteps", jobProgress.getTotalSteps() != null ? jobProgress.getTotalSteps() : "");
        progressMap.put("completedSteps", jobProgress.getCompletedSteps() != null ? jobProgress.getCompletedSteps() : "");
        progressMap.put("timeTaken", jobProgress.getTimeSpend() != null ? jobProgress.getTimeSpend() : "");
        progressMap.put("stepMessage", jobProgress.getStepMessage() != null ? jobProgress.getStepMessage() : "");
        progressMap.put("summaryMessage", jobProgress.getSummaryMessage() != null ? jobProgress.getSummaryMessage() : "");
        progressMap.put("stepMessages", jobProgress.getStepMessages());
        progressMap.put("progress", progress);
        progressMap.put("message", jobProgress.getMessage() != null ? jobProgress.getMessage() : "");
        progressMap.put("errorMessage", jobProgress.getErrorMessage() != null ? jobProgress.getErrorMessage() : "");
        progressMap.put("stepLabel", jobProgress.getStepLabel() != null ? jobProgress.getStepLabel() : "");
        progressMap.put("entity", jobProgress.getEntity() != null ? jobProgress.getEntity() : "");
        progressMap.put("entityId", jobProgress.getEntityId() != null ? jobProgress.getEntityId() : "");
        progressMap.put("id", jobProgress.getId());
        if(StringUtils.isNotEmpty(jobProgress.getJobStoppedBy())){
        	progressMap.put("jobStoppedBy", jobProgress.getJobStoppedBy());
        }
        return progressMap;
    }

    @Override
    public void removeJobProgress(String cacheKey) {
        zfjCacheService.removeCacheByKey(cacheKey);
    }

    public JobProgress getJobProgress(String jobProgressTicket)  {
        if (StringUtils.isBlank(jobProgressTicket)){
            return null;
        }
        String jobProgressStr =  (String) zfjCacheService.getCacheByKey(jobProgressTicket,null);
        if(jobProgressStr == null){
        	return null;
        }
        JobProgress jobProgress = ZephyrUtil.getJobProgressFromStr(jobProgressStr);
        if (jobProgress == null){
            jobProgress = new JobProgress();
         }

        return jobProgress;
    }

    @Override
    public JobProgress getJobProgressByType(String jobProgressToken, String type) {
        if (StringUtils.isBlank(jobProgressToken)){
            return null;
        }
        String jobProgressStr =  (String) zfjCacheService.getCacheByKey(jobProgressToken,null);
        JobProgress jobProgress = ZephyrUtil.getJobProgressFromStr(jobProgressStr);
        if(null == jobProgress || !jobProgress.getName().equalsIgnoreCase(type)){
            jobProgress = new JobProgress();
        }
        return jobProgress;
    }

    private void setJobProgressToCache(String jobProgressTicket, JobProgress jobProgress) {
        if (StringUtils.isBlank(jobProgressTicket)){
            return;
        }
         if (jobProgress.getStatus() == ApplicationConstants.JOB_STATUS_COMPLETED || jobProgress.getStatus() == ApplicationConstants.JOB_STATUS_FAILED){
             removeJobProgress(jobProgressTicket, ApplicationConstants.IN_PROGRESS_JOBS_MAP);
         } else {
             String valueString = ZephyrUtil.getString(jobProgress);
             zfjCacheService.createOrUpdateCache(jobProgressTicket,valueString);
        }
    }

    private void removeJobProgress(String jobProgressTicket, String iMapName)  {
         zfjCacheService.removeCacheByKey(jobProgressTicket);
    }

    private void cleanCompletedJobsMap(final Map<String, JobProgress> jobsMap)  {
        if (jobsMap.size() < ApplicationConstants.DEFAULT_COMPLETED_JOB_MAP_SIZE){
            return;
        }
        String lockKey = ApplicationConstants.CLEAN_JOB_PROGRESS_COMPLETED_MAP_LOCK_KEY;
        jobsMap.remove(lockKey);
        jobsMap.clear();
    }

    private String getIMapKey(String jobProgressTicket){
        return String.valueOf(jobProgressTicket);
    }
}
