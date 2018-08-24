package com.thed.zephyr.je.service;

/**
 * Created by Masud on 3/15/18.
 */
public interface AnalyticService {
    Integer getTestCaseCount();
    Integer getTestStepCount();
    Integer getAttachmentCount(String entityType);
}
