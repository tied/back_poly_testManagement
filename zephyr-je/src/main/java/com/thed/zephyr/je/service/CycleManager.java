package com.thed.zephyr.je.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.thed.zephyr.je.model.Cycle;
import com.thed.zephyr.je.model.Schedule;
import net.java.ao.EntityStreamCallback;


public interface CycleManager {
	static final Long ANY = -1l;
	  /**
     * Retrieves all of the cycles
     */
    List getCycles(Cycle cycle);

    /**
     * Gets cycle's information based on id.
     * @param id the cycle's id
     * @return cycle populated cycle object
     */
    Cycle getCycle(final Long id);
    
    /**
     * Gets List of Cycle information based on version id. For adhoc cycles,
     * versionId is null and hence search is done via projectId
     * @param id the version
     * @param offset (-1 indicates no limit, return all data, else max of 10 records to be returned
     * @return Cycle populated Cycle object
     */
	List<Cycle> getCyclesByVersion(final Long id, final Long projectId, Integer offset);

    /**
     * Gets List of Cycle information based on issue id.
     * @param id the issue
     * @return Cycle populated Cycle object
     */
	 List<Cycle> getCyclesByIssueId(Integer issueId);

	
    /**
     * Saves a cycle's information
     * @param cycle the object to be saved
     * @return 
     */
    Cycle saveCycle(Map<String, Object> cycleProperties);
	
    /**
     * Removes a cycle from the database by id
     * @param id the cycle's id
     */
    void removeCycle(final Long id, String jobProgressToken);
    
    /**
     * Gets All Cycles based on passed in criteria
     * @param searchExpression
     * @param maxAllowedRecord
     * @return
     */
    public List<Cycle> getCyclesByCriteria(String searchExpression,int maxAllowedRecord);

    /**
     * Removes All Cycles belonging to a Project/Project or Version
     * @param projectId
     * @param versionId
     * @return
     */
    public Integer removeBulkCycle(Long projectId, Long versionId);
    
    /**
     * Checks and skips if issue is already added to cycle 
     * @param cycleId
     * @param issuesIds
     * @return schedules newly created schedules
     */
    List<Schedule> addIssuesToCycle(Long cycleId, List<Long> issuesIds);
    
	Integer getCycleCountByVersionId(final Long id, final Long projectId);

    /**
     * Swaps Version and deletes the Source Version 
     * @param sourceVersionId
     * @param targetVersionId
     * @param projectId
     */
	 Integer swapVersion(Long sourceVersionId, Long targetVersionId, Long projectId);
	  
	 /**
	  * Gets cycle's information based on key and value.
	  * @param value of the key
	  * @return values populated object
	  */
	 List<Cycle> getValuesByKey(final String clause,final List<String> values);	

	 /**
	  * Gets All Cycles, these cycles cant be updated
	  * @param name
	  * @return
	  */
	  public List<Cycle> getCycles(String whereClause);

    /**
     * Can iterate over cycles via callback
     * @param whereClause
     * @param entityStreamCallback
     */
    public void getCycles(String whereClause, EntityStreamCallback<Cycle, Integer> entityStreamCallback);

	  public List<String> getValues(final String fieldName,final String key);
	  
	  /**
	   * Retrieves List of Cycle By Project IDs
	   * @param projectIds
	   * @param valuePrefix
	   * @return
	   */
	  List<Cycle> getCyclesByProjectId(Collection<Integer> projectIds,String clauseName,String valuePrefix);
	  
	    
    /**
     * Get distinct versions for a given project
     * @param versionId
     * @return ProjectId
     */
    Long getDistinctProjectIdByVersionId(Long versionId);

    /**
     * Get All Cycles grouped By SprintId
     * @param projectIdList
     * @param versionIds
     * @param sprintIds 
     * @param offset 
     * @param maxRecords 
     * @return
     */
    List<Cycle> getCyclesByProjectsAndVersions(List<Long> projectIdList, String[] versionIds, String[] sprintIds, Integer offset, Integer maxRecords);
    
    /**
     * Removes the cycle and its associated folders & executions.
     * 
     * @param id -- cycle id
     * @param jobProgressToken -- Token generated for this job to track.
     */
    public void removeCycleAndFolder(Long projectId, Long versionId, Long id, String jobProgressToken);
    
    Integer getTotalCyclesCount();
}
