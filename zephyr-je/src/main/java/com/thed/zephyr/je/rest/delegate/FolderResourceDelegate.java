package com.thed.zephyr.je.rest.delegate;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import com.thed.zephyr.je.model.Folder;
import com.thed.zephyr.je.model.FolderCycleMapping;
import com.thed.zephyr.je.rest.FolderResource.FolderRequest;
import com.thed.zephyr.je.rest.FolderResource.FolderResponse;

/**
 * Folder resource delegate, which serves for actual FolderResource Rest along with ValidatePermissions annotation.
 * 
 * @author manjunath
 *
 */
public interface FolderResourceDelegate {
	
	/**
	 * Method invokes the folder manager api to create a folder under the cycle.
	 * 
	 * @param folderRequest -- Holds the user posted folder creation request data.
	 * @param loggedInUser -- User name
	 * @param valueHolder -- Holds the values in a key value pair.
	 * @return -- Returns the response object with folder id and response message.
	 */
	Response createFolder(FolderRequest folderRequest, String loggedInUser, Map<String, String> valueHolder);
	
	/**
	 * Method updates the folder information.
	 * 
	 * @param projectId - Project id.
	 * @param versionId - version id.
	 * @param cycleId -- Cycle Id
	 * @param folderId -- Id of the folder.
	 * @param folderName -- Name of the folder to be created.
	 * @param folderDescription -- Description of the folder to be created.
	 * @param loggedInUser -- Logged in user name.
	 * @param valueHolder -- Holds the values in a key value pair.
	 * @return -- Returns the folder response object with folder id and response message.
	 */
	FolderResponse updateFolder(Long projectId, Long versionId, Long cycleId, Folder folder, String folderName, String folderDescription, String loggedInUser, Map<String, String> valueHolder);
	
	/**
	 * Method deletes the folder information as well as cycle folder mapping.
	 * 
	 * @param projectId - Project id.
	 * @param versionId - version id.
	 * @param cycleId -- Cycle Id
	 * @param folderId -- Folder id to be deleted.
	 * @return -- Returns the json response object with job progress token id. Use this token id to check the progress of deletion.
	 */
	Response deleteFolder(Long projectId, Long versionId, Long cycleId, Long folderId);
	
	/**
	 * First check whether the folder name is already user for the cycle in the cycle.
	 * If cache does not have folder name for the cycle then it checks against the database.
	 * 
	 * Cache check is for reducing the database call.
	 * 
	 * @param projectId -- Project id.
	 * @param versionId - Version id.
	 * @param cycleId -- Cycle id for which folder name uniqueness should be checked.
	 * @param folderName -- Folder name to be checked for uniqueness.
	 * @return -- Returns true if the folder name is not present otherwise false.
	 */
	boolean isFolderUniqueForCycle(Long projectId, Long versionId, Long cycleId, String folderName);
	
	/**
	 * Removes the folder name for the cycle from the cache due to some issue
	 * in creating a folder.
	 * 
	 * @param projectId -- Project id.
	 * @param versionId - Version id.
	 * @param cycleId -- Cycle id
	 * @param folderName -- Folder name which needs to be removed from the cache.
	 */
	void rollbackFolderNameFromCache(Long projectId, Long versionId, Long cycleId, String folderName);
	
	/**
     * Link folders to sprint for a specific cycle.
     * 
     * @param projectId -- Project id.
     * @param versionId -- Version id.
     * @param folderId -- Folder to be linked to sprint.
     * @param cycleId -- Cycle under which folder is created.
     * @param sprintId -- Sprint for which folder to be linked.
     */
    void updateFoldersToSprint(Long projectId, Long versionId, Long folderId, Long cycleId, Long sprintId);
    
    /**
     * Fetch folder cycle mapping object for the folder id.
     * 
     * @param projectId -- Project id.
     * @param versionId -- Version id.
     * @param cycleId -- Cycle id.
	 * @param folderId -- Folder id.
     * @return -- Returns the fetched folder cycle mapping object for the folder.
     */
    FolderCycleMapping getFolderCycleMapping(Long projectId, Long versionId, Long cycleId, Long folderId);
    
    /**
     * Fetch the folder
     * 
     * @param folderId -- Folder id
     * @return -- Return the folder information.
     */
    Folder getFolder(Long folderId);
    
    /**
	 * Method fetches all the folders.
	 * 
	 * @param projectId - project id
	 * @param versionId - version id
	 * @param cycleIds -- List of Cycle ids
	 * @param limit -- Number of records to limit.
     * @param offset -- Position from which the records will be fetched.
	 * @return -- Returns the list of folders created under cycle.
	 */
	List<Folder> fetchFolders(Long projectId, Long versionId, List<Long> cycleIds, Integer limit, Integer offset);
	

}
