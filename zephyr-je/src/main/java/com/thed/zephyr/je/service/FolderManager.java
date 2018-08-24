package com.thed.zephyr.je.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.thed.zephyr.je.model.Folder;
import com.thed.zephyr.je.model.FolderCycleMapping;

/**
 * This class acts as a service layer which interacts with active objects.
 * 
 * @author manjunath
 *
 */
public interface FolderManager {
	
    /**
     * Saves a folder information into DB.
     * 
     * @param folderProperties -- Holds the folder related information which needs to be saved.
     * @return Folder -- Returns the saved folder object with id.
     */
    Folder saveFolder(Map<String, Object> folderProperties);
    
    /**
     * Updates the folder information into DB.
     * 
	 * @param Folder -- Existing folder data.
	 * @param folderName -- Name of the folder to be created.
	 * @param folderDescription -- Description of the folder to be created.
	 * @param modifiedBy -- Modified user name.
	 * @param cacheFolderMap -- Cache Map holds folder names.
     */
    void updateFolder(Folder folder, String folderName, String folderDescription, String modifiedBy, Map<String, Boolean> cacheFolderMap);
    
    /**
     * Removes a folder from the table.
     * 
     * @param folderId -- Id of the folder which needs to be removed from DB.
     * @return -- Returns 1 if the folder is deleted successfully otherwise 0
     */
    int removeFolder(Long folderId);
    
    /**
     * Fetch the folder information for the id.
     * 
     * @param folderId -- Id of the folder which needs to be fetched.
     * @return -- Returns the fetched folder object.
     */
    Folder getFolder(Long folderId);
    
    /**
     * Method saves the folder-cycle mapping into db.
     * 
     * @param folderCycleMappingProperties -- Holds the folder-cycle mapping information which needs to be saved.
     * @return -- Returns the created FolderCycleMapping object.
     */
    FolderCycleMapping saveFolderCycleMapping(Map<String, Object> folderCycleMappingProperties);
    
    /**
     * Removes a cycle folder mapping from the table.
     * 
     * @param cycleId -- Cycle id.
     * @param folderId -- Id of the folder which needs to be removed from DB.
     * @return -- Returns 1 if the folder is deleted successfully otherwise 0
     */
    int removeCycleFolderMapping(Long projectId, Long versionId, long cycleId, Long folderId);
    
    /**
     * Method fetches the folders count for the cycle.
     * 
     * @param projectId -- Project id.
     * @param versionId -- Version id.
     * @param cycleId -- Cycle id
     * @return -- Returns the folders count for the cycle.
     */
    int getFoldersCountForCycle(Long projectId, Long versionId, Long cycleId);
    
    /**
     * Checks whether the folder name is unique within the cycle in the database;
     * 
     * @param cycleId -- Cycle id for which folder name to be checked for uniqueness.
     * @param folderName -- Folder name to be checked for uniqueness.
     * @return -- Returns true if the folder name is not present otherwise false.
     */
    boolean isFolderNameUniqueUnderCycle(Long projectId, Long versionId, Long cycleId, String folderName);
    
    /**
     * Removes a folder from the table.
     * 
     * @param folderId -- Id of the folder which needs to be removed from DB.
     * @param cacheFolderMap -- Cache Map holds folder names.
     * @return -- Returns 1 if the folder is deleted successfully otherwise 0
     */
    int removeFolder(Long folderId, Map<String, Boolean> cacheFolderMap);
    
    /**
     * Link folder to sprint for a specific cycle.
     * 
     * @param folderId -- Folder id to be linked to sprint.
     * @param cycleId -- Cycle id under which folders are created.
     * @param versionId -- Version id under which cycle is created.
     * @param projectId -- Project id.
     * @param sprintId -- Sprint for which folder to be linked.
     * @return -- Returns 1 if the folder is linked successfully otherwise 0
     */
    int updateFoldersToSprint(Long folderId, Long cycleId, Long versionId, Long projectId, Long sprintId);
    
    /**
     * clone the folder to new cycle.
     * 
     * @param folderId -- Folder id to be cloned to new cycle.
     * @param newCycleId -- New cycle id to which folders to be cloned.
     * @param oldCycleId -- Cloned cycled id from which folders to be cloned.
     * @param loggedInUser -- Logged In user.
     * @return -- Returns newly created folder.
     */
    Folder cloneFolderToCycle(Long projectId, Long versionId, Long folderId, Long newCycleId, Long oldCycleId, String loggedInUser);
    
    /**
     * Fetches folders for sprint.
     * 
     * @param projectIdList -- List of project ids.
     * @param versionIds -- List of version ids.
     * @param sprintIds -- List of sprint ids.
     * @param offset -- Starting position to fetch the records.
     * @param maxRecords -- Maximum number of records to fetch.
     * @return -- Returns the list of folder-cycle mappings for a sprint.
     */
    List<FolderCycleMapping> getFoldersForSprint(List<Long> projectIdList, String[] versionIds, String[] sprintIds, Integer offset, Integer maxRecords);
    
    /**
     * Clones the empty folders from one cycle to another.
     * 
     * @param projectId -- Project id.
     * @param versionId -- Version id.
     * @param clonedCycleId -- Cycle from which empty folders will be cloned.
     * @param newCycleId - Cycle to which empty folders will be cloned.
     * @param loggedInUser -- User name
     * @param clonedFoldersMap -- Map holds the folder ids which are mapped to executions.
     */
    void cloneEmptyFoldersFromCyle(Long oldProjectId, Long oldVersionId, Long clonedCycleId, 
			Long newProjectId, Long newVersionId, Long newCycleId, String loggedInUser, Map<String, Long> clonedFoldersMap);
    
    /**
     * Fetches the folders based on values and the clause.
     * 
     * @param clause -- Clause to be used.
     * @param values -- List of values to be searched.
     * @return -- Returns the list of matched folders.
     */
    List<Folder> getValuesByKey(final String clause, final List<String> values);
    
    /**
     * Fetches the folder based on fieldName.
     * 
     * @param fieldName -- Field Name to be searched.
     * @param value -- Value of the field name.
     * @return -- Returns the list of matched folder names.
     */
    List<String> getValues(final String fieldName, final String value);
    
    /**
     * Gets List of folder information based on cycle id. For adhoc cycles,
     * versionId is null and hence search is done via projectId.
     * 
     * @param projectIds -- List of project ids.
     * @param clauseName -- Clause name
     * @param valuePrefix -- Value prefix
     * @return -- Returns the list of folders which are matched.
     */
    List<Folder> getFoldersByProjectId(final Collection<Integer> projectIds, String clauseName, String valuePrefix);

    /**
     * Get the sprint ID for the Folder
     *
     * @param folderId
     * @param cycleId
     * @return
     */
    Long getSprintIDForFolder(Long folderId, Long cycleId);
    
    /**
     * Updates the deleted version id to unscheduled version id in folder cycle mapping table.
     * 
     * @param projectId -- Project Id
     * @param srcVersionId -- Deleted Version Id
     * @param cycleId -- Cycle ID
     * @param targetVersionId -- Target Version Id to update.
     */
    void updateDeletedVersionId(Long projectId, Long srcVersionId, Long cycleId, Long targetVersionId);
    
    /**
     * Fetch folder cycle mapping object for the folder id.
     * 
     * @param folderId -- Folder id.
     * @param cycleId -- Cycle id.
     * @param versionId -- Version id.
     * @param projectId -- Project id.
     * @return -- Returns the fetched folder cycle mapping object for the folder.
     */
    FolderCycleMapping getFolderCycleMapping(Long folderId, Long cycleId, Long versionId, Long projectId);
    
    /**
     * Fetches the list of folders.
     * 
     * @param projectId -- project id
     * @param versionId -- version id
     * @param cycleId -- Id of the cycle for which list of folders to be fetched.
     * @param limit -- Number of records to limit.
     * @param offset -- Position from which the records will be fetched.
     * @return -- Returns the list of folders under the cycle.
     */
    List<Folder> fetchFolders(Long projectId, Long versionId, List<Long> cycleIds, Integer limit, Integer offset);
}
