package com.thed.zephyr.je.service;

import com.atlassian.crowd.embedded.api.User;
import com.thed.zephyr.je.config.model.ExecutionStatus;
import com.thed.zephyr.je.model.Cycle;
import com.thed.zephyr.je.model.ReindexJobProgress;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.model.ScheduleDefect;
import com.thed.zephyr.je.rest.CustomFieldValueResource;
import com.thed.zephyr.je.vo.ExecutionSummaryImpl;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Future;

public interface ScheduleManager {
	  /**
     * Retrieves all of the schedules
	 * @param offset TODO
	 * @param limit TODO
     */
    List<Schedule> getSchedules(Schedule schedule, Integer offset, Integer limit);

    /**
     * Gets schedule's information based on id.
     * @param id the schedule's id
     * @return schedule populated schedule object
     */
    Schedule getSchedule(final Integer id);
    
    /**
     * Gets schedules based on ids
     * @param scheduleIds
     * @return List of Schedule Object
     */
    Schedule[] getSchedules(final List<Integer> scheduleIds);
    
    /**
     * Gets schedules by given filter criteria. Valid options are as following
     * 	offset 	--> int
     * 	size 	--> int
     * 	cid 	--> String[]
     * 	vid 	--> String[]
     * 	status 	--> String[]
     *	exeBy 	--> String[]
     *	asgnTo 	--> String[]
     * @param filters
     * @return
     */
    List<Schedule> searchSchedules(Map<String, Object> filters);
    
    /**
     * Gets List of Schedule information based on version id.
     * @param cycleId the cycle
	 * @param offset -1 for No pagination, else set the limit to 10
     * @param sortQuery query for sorting
     * @return Schedule populated Schedule objects
     */
	List<Schedule> getSchedulesByCycleId(final Long versionId, final Long projectId, final Integer cycleId, Integer offset, String sortQuery, String expandos);
	
	/**
	 * Gets List of Schedule information based on version id.
	 * @param id the issue
	 * @param offset -1 for No pagination, else set the limit to 11
	 * @param maxResult use passed in maxResult or default to 11
	 * @return Schedule populated Schedule objects
	 */
	List<Schedule> getSchedulesByIssueId(final Integer id, Integer offset, Integer maxResult);
	
	/**
	 * Gets Schedule information based on cycleid and issueid.
	 * @param cycleId the Cycle ID
	 * @param offset -1 for No pagination, else set the limit to 10
	 * @return Schedule populated Schedule objects
	 */
	Schedule getSchedulesByIssueIdAndCycleId(final Integer issueId, final Integer cycleId, Integer offset);
	
	/**
	 * Gets Schedule summary information based on version id.
	 * @return set containing Map of cycleId, cycleName, statusId, count
	 */
	Set<Map<String, Object>> getExecutionSummaryGroupedByCycle(Long versionId, Long projectId);

	/**
	 * 
	 * @param issueIds
	 * @param versionId TODO
	 * @return map containing statusId and count
	 */
	Map<String, Object> getExecutionSummaryByIssueIds(Collection<Long> issueIds, Long versionId);
	
    /**
     * Saves a schedule's information
     * @return 
     */
    Schedule saveSchedule(Map<String, Object> scheduleProperties);
	
    /**
     * Removes a schedule from the database by id
     * @param id the schedule's id
     * @return 1 if schedule is deleted, 0 if not
     */
    int removeSchedule(final Integer id);
    
    /**
     * Removes a schedule from the database created for a given testcase
     * @return no of schedules deleted, 0 if none
     */
    int removeSchedules(final Long testcaseId);

    /**
     * Removes schedules from the database
     * @param schedules the schedule object
     * @return no of schedules deleted, 0 if none
     */
    Integer deleteSchedules(final Schedule[] schedules, String jobProgressToken);
    
    /**
     * Removes a schedule from the database created for a given cycle
     * @param id the cycle's id
     * @return no of schedules deleted, 0 if none
     */
    int removeSchedulesByCycleId(Long id, String jobProgressToken);

    /**
     * Removes a schedule from the database created for a given cycle
     * @param projectId the ProjectId
     * @return no of schedules deleted, 0 if none
     */
    int removeSchedulesByCycleIdAndProjectId(Long cycleId,Long projectId);

    /**
     * Gets All Schedules based on passed in criteria
     * @param searchExpression
     * @param maxAllowedRecord
     * @return
     */
    public List<Schedule> getSchedulesByCriteria(String searchExpression,int maxAllowedRecord);
    
    /**
     * Checks and skips if issue is already added to schedule 
     * @param issuesIds
     * @param cycleId cycle to which issues need to be added. It could be null, in which case, 
     * 			they will all be added to default schedule in given version
     * @param versionId - used in case cycleId is not passed
     * @return schedules newly created schedules
     */
    @Deprecated
    List<Schedule> createBulkSchedule(List<Integer> issuesIds, Integer cycleId, Integer versionId);
    
	/**
	 * Gets all Execution Schedules based on CycleID
	 * 
	 * @param cycleId
	 * @return
	 * @deprecated use {@link #getExecutionSummaryGroupedByCycle(Long, Long)} instead
	 */
    @Deprecated
    List<ExecutionSummaryImpl> getExecutionDetailsByCycle(Long versionId, Long projectId, Integer cycleId, String userName);

    /**
     * Gets All Execution Schedules based StatusId
     * @param statusId
     * @return all schedules with input status
     */
	List<Schedule> getSchedulesByExecutionStatus(String statusId);
	
	/**
	 * Fetches No of executed testcases  
	 * @param versionId
	 * @param projectId
	 * @return
	 */
	Integer getTestcaseExecutionCount(Long versionId, Long projectId);
	
	/**
	 * Get all defectSchedules associated to this schedule
	 * @param scheduleId
	 * @return
	 */
	List<ScheduleDefect> getAssociatedDefects(Integer scheduleId);
	
	/**
	 * Saves passed in issueIds into database. Performs unique check and skips over existing scheduleId - issueId combination 
	 * @param schedule
	 * @return final list of associations
	 */
	Map<String, Object> saveAssociatedDefects(Schedule schedule, List<Integer> defectsToPersis);
	
	/**
	 * Delete the passed in defect associations.
	 * @param scheduleId
	 * @param issueIds issues that need to be removed. If nothing is passed in, all the associations would be removed
	 * @return no of associations removed
	 */
	Integer removeAssociatedDefects(Integer scheduleId, Integer[] issueIds);

	/**
	 * Delete the passed in defects from Schedule Defect associations.
	 * @return no of scheduleDefects affected
	 */
	List<ScheduleDefect> removeScheduleDefectsAssociation(Long defectId);
	
	/**
	 * Gets Schedule information based on project id and issueId.
	 * @return Schedule populated Schedule objects
	 */
	Schedule getSchedulesByIssueIdAndProjectId(Integer issueId, Integer pId);

	Integer getSchedulesCount(final Long versionId, final Long projectId, Integer cycleId, Long folderId);

	Integer getSchedulesCountByIssueId(Integer issueId);

	List<ScheduleDefect> getScheduleDefects(Integer scheduleId);
	
	List<Long> getAllScheduleIds();
	
	Map<String, Object> getSchedulesByProjectIdWithDuration(Set<Entry<Integer, ExecutionStatus>> statuses,Integer scheduleId, String duration);

	List<Schedule> getSchedulesByDefectId(Integer defectId, Boolean includeStepResult);
	
	Map<String, User> getExecutedByValues(Long projectId, Long versionId);

	Map<String, User> getAssigneeValues(); 

    /**
     * Removes a schedule from the database executed by given JIRA User
     * @param userId
     * @return no of schedules deleted, 0 if none
     */
    int removeSchedulesByUserId(String userId);	
    
    /**
     * Allow changing status on bulk schedules and clear Defect Association
     * @param scheduleIds
     * @param status
     * @param stepStatus
     * @param clearDefectAssociation
     * @param changeStepStatus
     * @param executedByUser
     * @return
     */
    List<Schedule> updateBulkStatus(Collection<Integer> scheduleIds,String status,String stepStatus,boolean clearDefectAssociation,boolean changeStepStatus,User executedByUser, String jobProgressToken);
    
    /**
     * Copies or moves the Schedules from one cycle to another as long as they belong to the same project where they are moving. action ='copy/move'
     * @param scheduleIds
     * @param action
     * @param projectId
     * @param versionId
     * @param folderId 
     * @param cycleId
     * @param clearStatusFlag
     * @param clearDefectAssociation
     * @param clearAssignmentsFlag 
     * @return
     */
    Map<String,List<Schedule>> copyOrMoveBulkSchedules(Collection<String> scheduleIds,String action,Integer projectId,Integer versionId, Long folderId, Integer cycleId, boolean clearStatusFlag,boolean clearDefectAssociation, boolean clearAssignmentsFlag, String jobProgressToken, boolean clearCustomFields);

    /**
     * Bulk associates defects to schedules.
     * @param scheduleList
     * @param defects
     * @return
     */
    Map<Integer,Map<String, Object>> bulkAssociateDefectsToSchedules(final List<Object> scheduleList,final List<Integer> defects);
    
    /**
     * Get distinct versions for a given project
     * @param projectId
     * @return versionSet
     */
    Set<Long> getDistinctVersionsByProjectId(Long projectId);
    
    /**
     * Get distinct versions for a given project
     * @param versionId
     * @return ProjectId
     */
    Long getDistinctProjectIdByVersionId(Long versionId);
    
    /**
     * Gets the defect count for a given execution
     * @param scheduleId
     * @return count of defects
     */
    Integer getScheduleDefectCountByScheduleId(final Integer scheduleId);
    
    /**
     * Gets the max Order Id from the execution table
    * @return max of orderId
     */
    Integer getMaxOrderId();

    /**
     * Gets Schedule by new orderId. The new orderId should be valid and belonging to the same cycle
     * @param newOrderId
     * @param cycleId
     * @param versionId 
     */
	List<Schedule> getScheduleByOrderId(Integer newOrderId, int cycleId, Integer versionId);


	/**
	 * Gets Schedule By Defect ID with pagination
	 * @param defectId
	 * @param includeStepResult
	 * @param offset
	 * @param maxResult
	 * @return
	 */
	List<Schedule> getSchedulesByDefectId(Integer defectId, Boolean includeStepResult,Integer offset, Integer maxResult);
	
	
	/**
	 * Gets Count of Schedule By Defect ID 
	 * @param defectId
	 * @return
	 */
	Integer getScheduleCountByDefectId(Integer defectId);

	/**
	 * Gets Count of Schedule By Step Defect ID 
	 * @param stepDefectId
	 * @return
	 */
	Integer getScheduleCountByStepDefectId(Integer stepDefectId);

	/**
	 * Gets a Join of Test and Step Defects
	 * @param defectId
	 * @param offset
	 * @param maxRecords
	 * @return
	 */
	Map<String, Object> getTestAndStepSchedulesByDefectId(Integer defectId,
			Integer offset, Integer maxRecords);
	
	/**
	 * Gets the count of issue executed for a given project ID.
	 * @param projectId
	 * @param onlyUnexecuted
     * @return
	 */
	Integer getScheduleCountByProjectIdAndGroupby(Integer projectId, boolean onlyUnexecuted);

	/**
	 * Get removed schedule by cycle.
	 * @param id
	 * @param jobProgressToken
	 * @return
	 */
	Future<Boolean> removeSchedulesByCycleIdPromise(Long id, String jobProgressToken);

	/**
	 * Method deletes the executions for the specific folder id and cycle id.
	 * 
	 * @param id -- Cycle id
	 * @param folderId -- Folder id
	 * @param jobProgressToken -- Job progress token id
	 * @return -- Returns the future which used to tack the progress;
	 */
	Future<Boolean> removeSchedulesByFolderIdAndCycleIdPromise(Long projectId, Long versionId, Long id, Long folderId, String jobProgressToken);
	
	/**
	 * @param projectId -- Project id.
	 * @param versionId -- Version id.
	 * @param cycleId -- Cycle id for which schedules to be fetched.
	 * @param offset -- Start point to fetch the records.
	 * @param sortQuery -- Sort query to sort the schedules.
	 * @param expandos
	 * @param folderId -- Folder for which schedules to be fetched.
	 * @return -- Returns the list of schedules for cycle and folder.
	 */
	public List<Schedule> getSchedulesByCycleAndFolder(Long projectId, Long versionId, Long cycleId, Integer offset, String sortQuery, String expandos, Long folderId);


	/**
	 * Get Folder level Execution estimated data
	 * @param projectId
	 * @param versionId
	 * @param cycleId
	 * @param folderId
	 * @return
	 */
	public Map<String,Long> getExecutionEstimationData(Long projectId, Long versionId, Long cycleId, Long folderId);

	
    /**
     * Get distinct schedule ids for given project ids.s
     * @param placeholderCommaList
     * @param projectIdArray
     * @param offset
     *@param limit @return
     */
	List<Schedule> getAllSchedulesByProjectIds(String placeholderCommaList, List<Integer> projectIdArray, Integer offset, Integer limit);

    /**
     * Get schedule count for given project id array.
     * @param placeholderCommaList
     * @param projectIds
     * @return
     */
	Integer getScheduleCountByProjectIds(String placeholderCommaList, List<Integer> projectIds);
	
	/**
     * Fetches the schedules for cycle id and also for folder id.
     * 
     * @param cycleId the cycle
	 * @param offset -1 for No pagination, else set the limit to 10
     * @param sortQuery query for sorting
     * @param folderId - Folder for which executions to be fetched.
	 * @param limit
     * @return Schedule populated Schedule objects
     */
	List<Schedule> getSchedules(final Long versionId, final Long projectId, final Integer cycleId, Integer offset, String sortQuery, String expandos, Long folderId,Integer limit);
	
	/**
	 * Fetches the execution details for cycle and folder.
	 * @param cycleId -- Cycle under which folder execution details to be fetched.
	 * @param folderId -- Folder for which execution details to be fetched.
	 * @param userName -- User specific executions details to be fetched.
	 * @return -- Returns the list of execution summary for cycle and folder.
	 */
	List<ExecutionSummaryImpl> getExecutionDetailsByCycleAndFolder(List<Long> projectIdList, String[] versionIds, Long cycleId, Long folderId, String userName);
	
	/**
	 * Finds the total number of defects created for executions under a cycle.
	 * 
	 * @param cycleId -- Execution defects count for a cycle.
	 * @param projectId -- Project id
	 * @param versionId -- Version id
	 * @return -- Returns the total number of defects created for executions under a cycle.
	 */
	Integer getTotalDefectsCountByCycle(Long cycleId, Long projectId, Long versionId);

	/**
	 * Get Schedule Count
	 */
	Integer getScheduleCount(Optional<Date> dateOptional, java.util.OptionalLong projectId, java.util.Optional<Boolean> shouldBeGreater);

	/**
	 * Get Schedule by pagination.
	 * @return
	 */
	List<Schedule> getSchedulesByPagination(Integer offset, Integer limit);


	/**
	 * Retrieves the Schedules given the scheduleIds
	 * @param scheduleIds
	 * @return
	 */
	List<Schedule> getSchedulesInBatch(List<Long> scheduleIds);


	/**
	 * Get ScheduleIds by cycle
	 * @param id
	 * @return
	 */
	Schedule[] getSchedulesByCycleId(int id);
	
	/**
	 * @param projectId -- Project id.
	 * @param versionId -- Version id.
	 * @param cycleId -- Cycle id for which schedules to be fetched.
	 * @param offset -- Start point to fetch the records.
	 * @param sortQuery -- Sort query to sort the schedules.
	 * @param expandos
	 * @return -- Returns the list of schedules for cycle.
	 */
	List<Schedule> getSchedulesByCycle(Long projectId, Long versionId, Long cycleId, Integer offset, String sortQuery, String expandos);

    /**
     *
     * @param offset
     * @param limit
     * @return
     */
	List<Long> getScheduleIdsByPagination(java.util.Optional<Date> dateOptional, java.util.OptionalLong projectId, Integer offset, Integer limit, java.util.Optional<Boolean> shouldBeGreater);

    /**
     *
     * @param placeholderCommaList
     * @param projectIdArray
     * @param offset
     * @param limit
     * @return
     */
    List<String> getAllScheduleIdsByProjectIds(String placeholderCommaList, List<Integer> projectIdArray, Integer offset, Integer limit);

    /**
     *
     * @param scheduleId
     * @return
     */
    boolean existsSchedule(int scheduleId);
    
    ReindexJobProgress saveReindexJobProgress(Map<String, Object> reindexJobProgressProperties);
    
    
    List<ReindexJobProgress> getReindexJobProgress(String name, OptionalLong projectId);
    
    List<ReindexJobProgress> getReindexJobProgress(String name, List<Long> projectIdList, String placeholderCommaList);
	
	Integer getTotalSchedulesCount();

	/**
	 *
	 * @param schedules
	 * @param customFieldValueRequests
	 * @param loggedInUser
	 * @param jobProgressToken
	 * @return List of processed schedules to which custom fields are associated.
	 */
	List<Schedule> bulkAssignCustomFields(Schedule[] schedules, Map<String, CustomFieldValueResource.CustomFieldValueRequest> customFieldValueRequests, User loggedInUser, String jobProgressToken);

	List<Schedule> getSchedulesByCycleAndFolder(Long versionId, Long projectId, String[] cycleIdArr, String folders);
	
	public boolean updateModifiedDate(final Schedule schedule, final Date modifiedDate);

	void cloneCustomFields(int scheduleId, Schedule newSchedule, boolean clearCustomFields);
	
	Integer getScheduleCountByProjectIds(String placeholderCommaList, List<Integer> projectIds, Date createdDate);
	
	List<String> getAllScheduleIdsByProjectIdsByCreatedDate(String placeholderCommaList, List<Integer> projectIdArray, Integer offset, Integer limit, Date createdDate);
	
	void updateCurrentDateForAllReindexJobProgress();

	Map<Integer, Map<String, Object>> getExecutionDetailsByCycleAndStatus(Set<Entry<Integer, ExecutionStatus>> statuses, Long versionId, Long projectId, List<Cycle> cycles);

	Map<String, Map<String, Object>> getExecutionDetailsByExecutorAndStatus(Set<Entry<Integer, ExecutionStatus>> statuses, Long versionId, Long projectId, Collection<User> users);

	Map<String,Object> getExecutionStatusCountByProjectAndVersionFilterByComponent(Long projectId, Long versionId, String[] componentIdArr, Map<Integer, ExecutionStatus> executionStatuses);
}
