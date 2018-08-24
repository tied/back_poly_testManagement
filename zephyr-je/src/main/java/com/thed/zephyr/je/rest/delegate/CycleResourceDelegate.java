package com.thed.zephyr.je.rest.delegate;

import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.thed.zephyr.je.model.Cycle;
import com.thed.zephyr.je.model.Folder;
import com.thed.zephyr.je.permissions.aop.ValidatePermissions;
import com.thed.zephyr.je.permissions.model.PermissionType;
import com.thed.zephyr.je.rest.CycleResource;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

/**
 * CycleResource delegate, which serves for actual CycleResource Rest along with ValidatePermissions annotation.
 */

public interface CycleResourceDelegate {
    @ValidatePermissions(permissionType = {PermissionType.ZEPHYR_BROWSE_CYCLE})
    Map<String,Object> getCycle(Long cycleId, Cycle cycle,ApplicationUser user);

    @ValidatePermissions(permissionType = {PermissionType.ZEPHYR_BROWSE_CYCLE})
    Response exportCycleOrFolder(final Integer cycleId, final Long versionId, final Project project, final Long folderId, final String sortQuery) throws UnsupportedEncodingException;

    @ValidatePermissions(permissionType = {PermissionType.ZEPHYR_CREATE_CYCLE})
    Response createCycle(CycleResource.CycleRequest cycleRequest);

    @ValidatePermissions(permissionType = {PermissionType.ZEPHYR_BROWSE_CYCLE,PermissionType.ZEPHYR_EDIT_CYCLE})
    Response updateCycle(CycleResource.CycleRequest cycleRequest);

    @ValidatePermissions(permissionType = {PermissionType.ZEPHYR_BROWSE_CYCLE, PermissionType.ZEPHYR_DELETE_CYCLE})
    Response deleteCycle(final Cycle cycle, String isFolderCycleDelete);

    @ValidatePermissions(permissionType = {PermissionType.ZEPHYR_BROWSE_CYCLE})
    Response getCycles(HttpServletRequest req, Long versionId, Long cycleId, Integer offset, String issueId, String expand);

    Response moveExecutionsToCycle(final Long cycleId, final Map<String, Object> params);

    Response copyExecutionsToCycle(final Long cycleId, final Map<String, Object> params);

    Response getCyclesByVersionsAndSprint(final Map<String, Object> params, List<Long> projectIdList);

    Response cleanupSprintFromCycle();
    
    /**
	 * Method fetches all the folders created under cycle.
	 * 
	 * @param projectId - project id
	 * @param versionId - version id
	 * @param cycleId -- Cycle id
	 * @param limit -- Number of records to limit.
     * @param offset -- Position from which the records will be fetched.
	 * @return -- Returns the list of folders created under cycle.
	 */
	List<Folder> fetchAllFoldersforCycle(Long projectId, Long versionId, Long cycleId, Integer limit, Integer offset);
	
	/**
	 * Populates the executions summaries for cycle and folder.
	 * 
	 * @param ob -- Response Object
	 * @param projectIdList -- List of projects.
	 * @param versionIds -- List of versions.
	 * @param cycleId -- Cycle id
	 * @param folderId -- Folder for which execution summaries to be fetched.
	 * @param cycleActionMap -- Holds the user actions on the executions like pagination, sort query etc.
	 * @throws JSONException -- Thrown while converting object to json.
	 */
	void populateFolderExecutionSummaries(JSONObject ob, List<Long> projectIdList, String[] versionIds, Long cycleId, Long folderId, Map<String, String> cycleActionMap) throws JSONException;
	
	/**
	 * Move executions from cycle to folder.
	 * 
	 * @param projectId -- Project id
	 * @param verisonId -- Verison id
	 * @param cycleId -- Cycle id
	 * @param folderId -- Folder id
	 * @param userName - User who has initiated this process.
	 * @param cycleName - Cycle Name
	 * @param schedulesList -- List of selected schedules to move.
	 * @return -- Returns the response object.
	 */
	Response moveExecutionsFromCycleToFolder(Long projectId, Long versionId, Long cycleId, Long folderId, String userName, String cycleName, List<Integer> schedulesList);

	/**
	 *get the spring id for folder and cycle
	 *
	 * @param folderId
	 * @param cycleId
	 * @return
	 */
	Long getSprintIDForFolder(Long folderId, Long cycleId);

	/**
	 * Clears Cycle Cache incase of Clustered wide cache causing issue or due to stickiness
	 * @return
	 */
    Response cleanupCacheForCycle();
}
