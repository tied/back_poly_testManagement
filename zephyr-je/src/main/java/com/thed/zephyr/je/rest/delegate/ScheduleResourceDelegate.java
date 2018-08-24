package com.thed.zephyr.je.rest.delegate;

import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.index.IndexException;
import com.atlassian.jira.project.version.Version;
import com.atlassian.query.Query;
import com.thed.zephyr.je.model.Cycle;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.permissions.aop.ValidatePermissions;
import com.thed.zephyr.je.permissions.model.PermissionType;
import com.thed.zephyr.je.rest.ScheduleResource.ExecutionReorderRequest;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * ScheduleResource delegate, which serves for actual ScheduleResource Rest along with ValidatePermissions annotation.
 */

public interface ScheduleResourceDelegate {
	
    @ValidatePermissions(permissionType = {PermissionType.ZEPHYR_BROWSE_CYCLE})
	Response getExecution(Schedule schedule, final String expandos);
	
    @ValidatePermissions(permissionType = {PermissionType.ZEPHYR_BROWSE_CYCLE})
    Response getExecutions(HttpServletRequest req, Integer issueId, Long versionId, Integer cycleId, Integer offset,
            String action, String sortQuery, final String expandos,
            final Integer limit, Long folderId);

    @ValidatePermissions(permissionType = {PermissionType.ZEPHYR_BROWSE_CYCLE})
    Response getExecutionDefects(Schedule schedule);
    
    @ValidatePermissions(permissionType = {PermissionType.ZEPHYR_BROWSE_CYCLE,PermissionType.ZEPHYR_CREATE_EXECUTION})
    Response createExecution(HttpServletRequest req, Map<String,Object> params);
    
    @ValidatePermissions(permissionType = {PermissionType.ZEPHYR_BROWSE_CYCLE})
    Response getExecutionCount(Long versionId,
            String groupBy,
            Integer cycleId,
            Integer sprintId,
            final String days,
            final String periodName,
            String graphType) ;

    @ValidatePermissions(permissionType = {PermissionType.ZEPHYR_BROWSE_CYCLE})
    Response getTopDefectsByIssueStatuses(
            Integer versionId,
            String issueStatuses,	/*open | in progress | reopen*/
            final String howManyDays); 
    
    @ValidatePermissions(permissionType = {PermissionType.ZEPHYR_BROWSE_CYCLE,PermissionType.ZEPHYR_EDIT_EXECUTION})
    Response editExecution(Schedule schedule, HttpServletRequest request, Map<String, Object> params);
    
    @ValidatePermissions(permissionType = {PermissionType.ZEPHYR_BROWSE_CYCLE})
    Response getExecutionNavigator(final Query currentQuery,
           final Integer offset, final Schedule schedule, final String expand);

    @ValidatePermissions(permissionType = {PermissionType.ZEPHYR_BROWSE_CYCLE,PermissionType.ZEPHYR_CREATE_EXECUTION})
	Response indexAll(boolean isSyncOnly, boolean isHardIndex, boolean isFromBackupJob, Date applyChangesDate);

    @ValidatePermissions(permissionType = {PermissionType.ZEPHYR_BROWSE_CYCLE,PermissionType.ZEPHYR_CREATE_EXECUTION})
	Response indexStatus(final long token);

    @ValidatePermissions(permissionType = {PermissionType.ZEPHYR_BROWSE_CYCLE,PermissionType.ZEPHYR_DELETE_EXECUTION})
	Response deleteExecution(Schedule schedule);
	
	Response addTestsToCycle(final Map<String, Object> params);
    
    @ValidatePermissions(permissionType = {PermissionType.ZEPHYR_BROWSE_CYCLE,PermissionType.ZEPHYR_EDIT_EXECUTION})
    Response reorderExecution(final ExecutionReorderRequest executionReorderRequest, Cycle cycle, Version version);

    Response getExecutionsByIssue(MutableIssue issue, Integer offset,
			Integer maxRecords, String expand);

    Response getExecutionsStatusCountForCycleByProjectIdAndVersion(Long projectId, Long versionId, String componentId, Integer offset,
                                                                   Integer limit);

    Response getExecutionsStatusCountPerAssigneeForCycle(Long projectId, Long versionId, String cycleId, Integer offset, Integer limit);

    Response getExecutionsStatusByAssignee(Long projectId, Long versionId, String cycleId);

    Response getExecutionsStatusCountPerCycleAndFolder(Long projectId, Long versionId, String cycleId, String folderId);

    @ValidatePermissions(permissionType = {PermissionType.ZEPHYR_BROWSE_CYCLE,PermissionType.ZEPHYR_CREATE_EXECUTION})
    Response reindexByProjectIds(List<String> projectIds, boolean isSyncOnly, boolean isHardIndex);

    @ValidatePermissions(permissionType = {PermissionType.ZEPHYR_BROWSE_CYCLE,PermissionType.ZEPHYR_CREATE_EXECUTION})
    Response indexCurrentNode();
    
    Integer getExecutionTotalCount(Long projectId, Long versionId, Integer cycleId, Long folderId);

    @ValidatePermissions(permissionType = {PermissionType.ZEPHYR_BROWSE_CYCLE})
    Response getPastExecutionsStatusCount(Long projectId, String dateStr, String cycleIds, String versionId);

    @ValidatePermissions(permissionType = {PermissionType.ZEPHYR_BROWSE_CYCLE})
    Response getExecutionsTimeTrackingPerCycleAndFolder(Long projectId, Long versionId, String cycles, String folders);
    
    void backUpIndexFiles() throws IOException;
    
    String getRecoveryBackUpPath() throws IOException;
    
    void applyChangesToIndexFromDate(long dateTime, java.util.OptionalLong projectId) throws IndexException;
}
