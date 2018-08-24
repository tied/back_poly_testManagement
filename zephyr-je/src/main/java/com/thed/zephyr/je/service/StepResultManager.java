package com.thed.zephyr.je.service;

import java.util.List;
import java.util.Map;

import com.thed.zephyr.je.model.StepDefect;
import com.thed.zephyr.je.model.StepResult;

public interface StepResultManager {

    StepResult getStepResult(final Integer stepResultId);
	List<StepResult> getStepResultsByExecutionStatus(String statusId);
	List<StepResult> getStepResultsBySchedule(Integer scheduleId);
	Boolean stepResultExists(Integer scheduleId, Integer stepId);
    
    List<StepDefect> getAssociatedDefects(final Integer stepResultId);
	Map<String, Object> saveAssociatedDefects(final Integer stepId, final Integer scheduleId, final Integer stepResultId, List<Integer> defectsToPersist);
	void removeStepDefectsAssociation(Long defectId);
    
	StepResult addStepResult(Map<String, Object> resultProperties);
	
	Integer removeStepResult(Integer id);
	Integer removeStepResultsBySchedule(Integer scheduleId);
	Integer removeStepResultByStep(Integer stepId);
	void deleteStepDefects(List<Integer> stepResultIds);
	boolean verifyAndAddStepResult(Map<String, Object> resultProperties);
	List<StepDefect> getStepResultsWithDefectBySchedule(Integer scheduleId);

	/**
	 * Fetch step results using pagination.
	 * @param scheduleId
	 * @param offset
	 * @param limit
	 * @return
	 */
    List<StepResult> getStepResultsByScheduleByPagination(Integer scheduleId, Integer offset, Integer limit);

    /**
     *
     * @param scheduleId
     * @return
     */
    Integer getStepResultsCount(Integer scheduleId);

    Integer getStepResultsCountByExecutionStatus(Integer scheduleId, Integer statusId);
}
