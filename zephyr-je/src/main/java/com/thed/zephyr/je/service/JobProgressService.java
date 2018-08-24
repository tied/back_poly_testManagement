package com.thed.zephyr.je.service;

import com.thed.zephyr.je.operation.JobProgress;
import com.thed.zephyr.je.rest.exception.JobFailedException;

import java.util.List;
import java.util.Map;

public interface JobProgressService {

    JobProgress createJobProgress(String name, Integer status, String jobProgressTicket);

    JobProgress createJobProgressWithNoSteps(String name, Integer status, String jobProgressTicket);

    JobProgress completedWithStatus(Integer status, String cacheKey);

    JobProgress setTotalSteps(String cacheKey, Integer totalSteps);

    Integer getTotalSteps(String cacheKey);

    JobProgress addSteps(String cacheKey, Integer steps);

    JobProgress setCompletedSteps(String cacheKey, Integer completedSteps);

    Integer getCompletedSteps(String cacheKey);

    JobProgress addCompletedSteps(String cacheKey, Integer completedSteps);

    JobProgress addCompletedSteps(String cacheKey, JobProgress jobProgress, Integer completedSteps);

    Map<String, Object> completedWithStatusStop(Integer status, String jobProgressTicket, String user);

    JobProgress setMessage(String cacheKey, String message);

    JobProgress setStepMessage(String cacheKey, String message);

    String getStepMessage(String cacheKey);

    JobProgress addStepMessages(String cacheKey, String message);

    JobProgress addCurrentStepMessageToMessages(String cacheKey);

    List<String> getStepMessages(String cacheKey);

    JobProgress setStepLabel(String cacheKey, String label);

    String getStepLabel(String cacheKey);

    void setEntityWithId(String cacheKey, String entity, String id);

    String getSummaryMessage(String cacheKey);

    JobProgress setSummaryMessage(String cacheKey, String message);

    String getErrorMessage(String cacheKey);

    JobProgress setErrorMessage(String cacheKey, String message);

    JobProgress cancelJob(String cacheKey);

    Boolean isJobCanceled(String cacheKey);

    Map<String, Object> checkJobProgress(String cacheKey, String type) throws JobFailedException;

    Map<String, Object> convertJobProgressToMap(JobProgress jobProgress, Double progress);

    void removeJobProgress(String cacheKey);

    JobProgress getJobProgress(String cacheKey);

    JobProgress getJobProgressByType(String jobProgressToken,String type);
}
