package com.thed.zephyr.je.service;

import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.model.TestStepCf;
import com.thed.zephyr.je.model.Teststep;
import com.thed.zephyr.je.vo.TeststepBean;

import java.util.List;
import java.util.Map;
import java.util.Optional;


public interface TeststepManager {
	
	/**
	 * 
	 * @param id unique identifier of the step
	 * @return
	 */
	Teststep getTeststep(Integer id);
	
	/**
	 * 
	 * @param issueId
	 * @return
	 */
	List<Teststep> getTeststeps(Long issueId, Optional<Integer> offset, Optional<Integer> limit);
	
	/**
	 * 
	 * @param fromIssueId
	 * @param toIssueId
	 * @return
	 */
	Boolean copySteps(Long fromIssueId, Long toIssueId);
	
	/**
	 * 
	 * @param fromIssueToIssueIdMap
	 * @return
	 */
	Boolean copyStepsInBulk(Map<Long, Long> fromIssueToIssueIdMap);
	
	/**
	 * 
	 * @param issueId
	 */
	void removeTeststeps(Long issueId);
	
	/**
	 * 
	 * @param id
	 * @return 
	 */
	List<Teststep> removeTeststep(Integer id);
	
	/**
	 * 
	 * @param steps
	 * @return
	 */
	Boolean saveTeststeps(List<Teststep> steps);
	
	/**
	 * 
	 * @param steps
	 * @return
	 */
	List<Teststep> updateOrderId(final List<Teststep> steps);

    /**
     * Saves a List of teststeps information
     * @param stepsProperties List of map of properties to be saved
     * @return 
     */
	List<Teststep> saveTeststepProperties(List<Map<String,Object>> stepsProperties);
	
	/**
	 * Creates a testcase Bases on bean
	 * @param stepBean
	 * @param issueId
	 * @return
	 */
	Teststep createTeststep(TeststepBean stepBean, Long issueId);

	/**
	 * Verifies if the StepResults are present. If not, will add them.
	 * @param schedule
	 * @return
	 */
	boolean verifyAndAddStepResult(Schedule schedule);

	List<TestStepCf> getCustomFieldValuesForTeststep(Integer valueOf);

	Integer getTotalStepCount(Long issueId);

	/**
	 *
	 * @param issueId
	 * @param nextOffsetValue
	 * @param limit
	 * @return
	 */
    Teststep[] getNextTeststep(Long issueId, Integer nextOffsetValue, Integer limit);

    Teststep getPrevTeststep(Long issueId, Integer prevOffsetValue, Integer limit);
}
