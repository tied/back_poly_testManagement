package com.thed.zephyr.je.operation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.thed.zephyr.util.ZCollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.atlassian.jira.bulkedit.operation.BulkOperationException;
import com.atlassian.jira.bulkedit.operation.ProgressAwareBulkOperation;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.task.context.Context;
import com.atlassian.jira.task.context.Contexts;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.collect.CollectionEnclosedIterable;
import com.atlassian.jira.util.collect.EnclosedIterable;
import com.atlassian.jira.web.bean.BulkEditBean;
import com.thed.zephyr.je.index.ScheduleIndexManager;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.permissions.model.PermissionType;
import com.thed.zephyr.je.permissions.service.ZephyrPermissionManager;
import com.thed.zephyr.je.service.ScheduleManager;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.ZephyrComponentAccessor;

public class ZephyrAddTestBulkOperation implements ProgressAwareBulkOperation {
    protected final Logger log = Logger.getLogger(ZephyrAddTestBulkOperation.class);

    public static final String NAME = "AddTestsToCycle";
    public static final String NAME_KEY = "jira.bulk.addtests.operation.name";
    private static final String DESCRIPTION_KEY = "jira.bulk.operation.key.description";
    private static final String CANNOT_PERFORM_MESSAGE_KEY = "zephyr.bulk.operation.invalid.description";

	@Override
	public String getNameKey() {
		return NAME_KEY;
	}

	@Override
	public String getDescriptionKey() {
		return ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText(DESCRIPTION_KEY);
	}

	@Override
	public String getOperationName() {
		return NAME;
	}

	@Override
	public String getCannotPerformMessageKey() {
		return  CANNOT_PERFORM_MESSAGE_KEY;
	}

	@Override
	public boolean canPerform(BulkEditBean bulkEditBean,
			ApplicationUser remoteUser) {
        // Check whether the user has the delete permission for all the selected issues
        List<Issue> selectedIssues = bulkEditBean.getSelectedIssues();
        for (Issue issue : selectedIssues) {
            String typeId = JiraUtil.getTestcaseIssueTypeId();
            boolean hasPermission = JiraUtil.hasBrowseProjectPermission(issue.getProjectObject(), ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser());
            boolean hasZephyrPermission = verifyBulkPermissions(issue.getProjectObject().getId(),remoteUser);
        	if(!hasPermission || !StringUtils.equalsIgnoreCase(issue.getIssueType().getId(),typeId) || !hasZephyrPermission) {
        		return false;
        	}
        }
        return true;
	}

	@Override
	public void perform(BulkEditBean bulkEditBean, ApplicationUser remoteUser,
			Context taskContext) throws BulkOperationException {
		BulkAddTestBean bulkAddTestBean = (BulkAddTestBean)bulkEditBean;
        List<Issue> selectedIssues = bulkEditBean.getSelectedIssues();
        ScheduleManager scheduleManager = (ScheduleManager)ZephyrComponentAccessor.getInstance().getComponent("schedule-manager");
        Collection<Schedule> indexSchedules = new ArrayList<Schedule>();
        Long folderId = Long.valueOf(bulkAddTestBean.getFolderId());

        for(Issue issue : selectedIssues) {
            Context.Task task = taskContext.start(issue);
        	//Issue needs to be belonging to the same project as the issue selected
        	if(issue.getProjectId().intValue() == bulkAddTestBean.getProjectId()) {
                String typeId = JiraUtil.getTestcaseIssueTypeId();
	            boolean hasJiraPermission = JiraUtil.hasBrowseProjectPermission(issue.getProjectObject(), ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser());
	            boolean hasZephyrPermission =  verifyBulkPermissions(issue.getProjectId().longValue(),remoteUser);
	        	if(hasJiraPermission && hasZephyrPermission && StringUtils.equalsIgnoreCase(issue.getIssueType().getId(),typeId)) {
                    List<Schedule> schedules = null;
	        		if(folderId != -1) {
                        schedules = getScheduleIfExists(bulkAddTestBean.getProjectId(), issue.getId().intValue(),bulkAddTestBean.getCycleId(),bulkAddTestBean.getVersionId(),folderId);
                    }else {
                        schedules = getScheduleIfExists(bulkAddTestBean.getProjectId(), issue.getId().intValue(),bulkAddTestBean.getCycleId(),bulkAddTestBean.getVersionId(),folderId);
                    }
	                if(schedules == null) { 
			            Map<String, Object> scheduleProperties = createSchedulePropertiesMap(bulkAddTestBean.getProjectId(), bulkAddTestBean.getVersionId(), bulkAddTestBean.getCycleId(), issue.getId(),scheduleManager.getMaxOrderId(),remoteUser,folderId);
			            Schedule schedule = scheduleManager.saveSchedule(scheduleProperties);
			            indexSchedules.add(schedule);
	                }
	        	}
        	}
        	task.complete();
        }
        if (indexSchedules.size() > 0) {
            try {
                //Need Index update on the same thread.
            	ScheduleIndexManager scheduleIndexManager = (ScheduleIndexManager)ZephyrComponentAccessor.getInstance().getComponent("schedule-index-manager");
                EnclosedIterable<Schedule> enclosedSchedules = CollectionEnclosedIterable.copy(indexSchedules);
                scheduleIndexManager.reIndexSchedule(enclosedSchedules, Contexts.nullContext());
            } catch (Exception e) {
                log.error("Error Indexing Schedule:", e);
            }
        }
	}
	@Override
	public int getNumberOfTasks(BulkEditBean bulkEditBean) {
        return bulkEditBean.getSelectedIssues().size();
	}


    /**
     * Request object to create schedule.
     * @param projectId
     * @param versionId
     * @param cycleId
     * @param issueId
     * @param orderId
     * @param remoteUser
     * @param folderId
     * @return
     */
    private Map<String, Object> createSchedulePropertiesMap(Integer projectId, Integer versionId, Integer cycleId, Long issueId, Integer orderId, ApplicationUser remoteUser, Long folderId) {
        Map<String, Object> scheduleProperties = new HashMap<String, Object>();
        Date date = new Date();
        scheduleProperties.put("ISSUE_ID", issueId.intValue());
        scheduleProperties.put("PROJECT_ID", projectId.longValue());
        scheduleProperties.put("VERSION_ID", versionId.longValue());
        cycleId = cycleId != null ? cycleId : ApplicationConstants.AD_HOC_CYCLE_ID;

        if (cycleId != null && cycleId.intValue() != -1)
            scheduleProperties.put("CYCLE_ID", cycleId);

        if (folderId != null && folderId != -1L) {
            scheduleProperties.put("FOLDER_ID", folderId);
        }

		scheduleProperties.put("DATE_CREATED", date);
		scheduleProperties.put("STATUS", "-1");
		scheduleProperties.put("ORDER_ID",orderId + 1);
		scheduleProperties.put("CREATED_BY",remoteUser.getKey());
		scheduleProperties.put("MODIFIED_BY",remoteUser.getKey());
		return scheduleProperties;
	}
    
	private boolean verifyBulkPermissions(Long projectId ,ApplicationUser user) {
		//Check ZephyrPermission and update response to include execution per project permissions
		ProjectPermissionKey cyclePermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_BROWSE_CYCLE.toString());
		ProjectPermissionKey executionPermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_CREATE_EXECUTION.toString());
		Collection<ProjectPermissionKey> projectPermissionKeys = new ArrayList<ProjectPermissionKey>();
		projectPermissionKeys.add(executionPermissionKey);
		projectPermissionKeys.add(cyclePermissionKey);
        ZephyrPermissionManager zephyrPermissionManager = (ZephyrPermissionManager)ZephyrComponentAccessor.getInstance().getComponent("zephyrPermissionManager");
		boolean loggedInUserHasZephyrPermission = zephyrPermissionManager.validateUserPermissions(projectPermissionKeys, null, user ,projectId);
		return loggedInUserHasZephyrPermission;
	}
	
    private List<Schedule> getScheduleIfExists(Integer projectId, Integer issueId, Integer cycleId, Integer versionId) {
        Map<String, Object> filter = new HashMap<String, Object>();
        filter.put("pid", projectId);
        filter.put("issueId", issueId);
        filter.put("cid", new Integer[]{cycleId});
        filter.put("vid", versionId);
        ScheduleManager scheduleManager = (ScheduleManager)ZephyrComponentAccessor.getInstance().getComponent("schedule-manager");
        List<Schedule> schedules = scheduleManager.searchSchedules(filter);
        if (schedules.size() > 0)
            return schedules;
        return null;
    }

    private List<Schedule> getScheduleIfExists(Integer projectId, Integer issueId, Integer cycleId, Integer versionId, Long folderId) {
        Map<String, Object> filter = new HashMap<String, Object>();
        filter.put("pid", projectId);
        filter.put("issueId", issueId);
        filter.put("cid", new Integer[]{cycleId});
        filter.put("vid", versionId);
        if(folderId != null && folderId != -1L) {
            filter.put("folderId", folderId);
        } else {
            filter.put("folderId", null);
        }
        ScheduleManager scheduleManager = (ScheduleManager)ZephyrComponentAccessor.getInstance().getComponent("schedule-manager");
        List<Schedule> schedules = scheduleManager.searchSchedules(filter);
        if (schedules.size() > 0)
            return schedules;
        return null;
    }
}
