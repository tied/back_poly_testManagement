package com.thed.zephyr.je.rest.delegate.impl;

import com.atlassian.core.util.DateUtils;
import com.atlassian.core.util.InvalidDurationException;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.task.context.Contexts;
import com.atlassian.jira.usercompatibility.UserCompatibilityHelper;
import com.atlassian.jira.util.collect.CollectionEnclosedIterable;
import com.atlassian.jira.util.collect.EnclosedIterable;
import com.atlassian.jira.util.json.JSONArray;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.thed.zephyr.je.event.EventType;
import com.thed.zephyr.je.event.ExecutionWorkflowModifyEvent;
import com.thed.zephyr.je.event.ScheduleModifyEvent;
import com.thed.zephyr.je.index.ScheduleIndexManager;
import com.thed.zephyr.je.model.ExecutionWorkflowStatus;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.rest.delegate.ExecutionWorkflowResourceDelegate;
import com.thed.zephyr.je.service.ScheduleManager;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.ConfigurationConstants;
import com.thed.zephyr.util.JiraUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * @author manjunath
 * @see com.thed.zephyr.je.rest.delegate.ExecutionWorkflowResourceDelegate
 *
 */
public class ExecutionWorkflowResourceDelegateImpl implements ExecutionWorkflowResourceDelegate {
	
	protected final Logger log = Logger.getLogger(ExecutionWorkflowResourceDelegateImpl.class);
	
	private final JiraAuthenticationContext authContext;
	private final ScheduleManager scheduleManager;	
	private final EventPublisher eventPublisher;
	private final IssueManager issueManager;
    private final ScheduleIndexManager scheduleIndexManager;
	
	public ExecutionWorkflowResourceDelegateImpl(JiraAuthenticationContext authContext, ScheduleManager scheduleManager, EventPublisher eventPublisher,
                                                 IssueManager issueManager, ScheduleIndexManager scheduleIndexManager) {
		this.authContext = authContext;
		this.scheduleManager = scheduleManager;
		this.eventPublisher = eventPublisher;
		this.issueManager = issueManager;
		this.scheduleIndexManager = scheduleIndexManager;
	}

	@Override
	public Schedule getExecution(Integer executionId) {
		return scheduleManager.getSchedule(executionId);
	}

	@Override
	public void startExecution(Schedule schedule) {
		String workflowStatusOldValue = schedule.getExecutionWorkflowStatus() != null ? schedule.getExecutionWorkflowStatus().getName() : null;
		Issue issue = issueManager.getIssueObject(schedule.getIssueId().longValue());

        if(Objects.nonNull(issue.getOriginalEstimate())) {
            long issueEstimateTime = issue.getOriginalEstimate().longValue();
            schedule.setEstimatedTime(issueEstimateTime);
        }

		schedule.setExecutionWorkflowStatus(ExecutionWorkflowStatus.STARTED);
		schedule.setModifiedBy(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
		schedule.setModifiedDate(new Date());
		schedule.save();

		// Reindex execution after execution workflow status update.
		reindexScheduleOnExecutionWorkflowStatusUpdate(schedule);
		//Audit execution workflow status change.
		auditWorkflowChange(schedule, workflowStatusOldValue);
	}

	@Override
	public void pauseExecution(Schedule schedule) {
		String workflowStatusOldValue = schedule.getExecutionWorkflowStatus() != null ? schedule.getExecutionWorkflowStatus().getName() : null;
		schedule.setExecutionWorkflowStatus(ExecutionWorkflowStatus.PAUSED);
        schedule.setModifiedBy(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
        schedule.setModifiedDate(new Date());
		schedule.save();

        // Reindex execution after execution workflow status update.
        reindexScheduleOnExecutionWorkflowStatusUpdate(schedule);
		//Audit execution workflow status change.
		auditWorkflowChange(schedule, workflowStatusOldValue);
	}
									
	@Override
	public void resumeExecution(Schedule schedule) {
		String workflowStatusOldValue = schedule.getExecutionWorkflowStatus() != null ? schedule.getExecutionWorkflowStatus().getName() : null;
		schedule.setExecutionWorkflowStatus(ExecutionWorkflowStatus.STARTED);
        schedule.setModifiedBy(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
        schedule.setModifiedDate(new Date());
		schedule.save();

        // Reindex execution after execution workflow status update.
        reindexScheduleOnExecutionWorkflowStatusUpdate(schedule);

		//Audit execution workflow status change.
		auditWorkflowChange(schedule, workflowStatusOldValue);
	}

	@Override
	public void completeExecution(Schedule schedule, String timeLogged) throws InvalidDurationException {

	    validateLoggedTimeFormat(timeLogged);

        String currentLoggedTime = null != schedule.getLoggedTime() ? getDateStringPretty(schedule.getLoggedTime()) : StringUtils.EMPTY;
		String workflowStatusOldValue = schedule.getExecutionWorkflowStatus() != null ? schedule.getExecutionWorkflowStatus().getName() : null;
		schedule.setLoggedTime(getAndValidateTimeSpent(timeLogged));
		schedule.setExecutionWorkflowStatus(ExecutionWorkflowStatus.COMPLETED);
        schedule.setModifiedBy(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
        schedule.setModifiedDate(new Date());
		schedule.save();

        // Reindex execution after execution workflow status update.
        reindexScheduleOnExecutionWorkflowStatusUpdate(schedule);

		//Audit execution workflow status change.
		auditWorkflowChange(schedule, workflowStatusOldValue);
        String updatedLoggedTime = null != schedule.getLoggedTime() ? getDateStringPretty(schedule.getLoggedTime()) : StringUtils.EMPTY;
        auditWorkflowChangeOnModifyTime(schedule, currentLoggedTime,updatedLoggedTime);
	}

    @Override
	public boolean isExecutionWorkflowDisabled(Long projectId){
		String projectIdsJson = JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
				.getText(ConfigurationConstants.ZEPHYR_DISABLE_PROJECT_EXEC_WORKFLOW);
		List<String> disabledProjectIdsList = new ArrayList<>();
		Boolean isExecutionWorkflowDisabled = Boolean.FALSE;
        if(StringUtils.isNotBlank(projectIdsJson)){
        	try{
	        	JSONArray ja = new JSONArray(projectIdsJson);
	    		for(int i = 0 ; i < ja.length(); i++){
                    disabledProjectIdsList.add(ja.get(i).toString());
	    		}
        	}catch(Exception e){
        		log.error("Exception while parsing JSON for the disabled project ids for execution workflow", e);
        	}
        }
        if(CollectionUtils.isNotEmpty(disabledProjectIdsList)) {
            if(disabledProjectIdsList.contains(String.valueOf(projectId))) {
                isExecutionWorkflowDisabled = Boolean.TRUE;
            }
        }
		return isExecutionWorkflowDisabled;
	}

    @Override
    public void modifyLoggedTimeByUser(Schedule schedule, String timeLogged) throws InvalidDurationException {

		validateLoggedTimeFormat(timeLogged);
	    String currentLoggedTime = null != schedule.getLoggedTime() ? getDateStringPretty(schedule.getLoggedTime()) : StringUtils.EMPTY;
        schedule.setLoggedTime(getAndValidateTimeSpent(timeLogged));
        schedule.setModifiedBy(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
        schedule.setModifiedDate(new Date());
        schedule.save();

        // Reindex execution after execution workflow status update.
        reindexScheduleOnExecutionWorkflowStatusUpdate(schedule);

        String updatedLoggedTime = null != schedule.getLoggedTime() ? getDateStringPretty(schedule.getLoggedTime()) : StringUtils.EMPTY;
        auditWorkflowChangeOnModifyTime(schedule, currentLoggedTime,updatedLoggedTime);
    }


    @Override
	public void reopenExecution(Schedule schedule) {
		String workflowStatusOldValue = schedule.getExecutionWorkflowStatus() != null ? schedule.getExecutionWorkflowStatus().getName() : null;

		schedule.setExecutionWorkflowStatus(ExecutionWorkflowStatus.REOPEN);
        schedule.setModifiedBy(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
        schedule.setModifiedDate(new Date());
		schedule.save();

        // Reindex execution after execution workflow status update.
        reindexScheduleOnExecutionWorkflowStatusUpdate(schedule);

		//Audit execution workflow status change.
		auditWorkflowChange(schedule, workflowStatusOldValue);
	}

	/**
     * Logs the audit data.
     * @param schedule
     * @param workflowStatusOldValue
     */
	private void auditWorkflowChange(Schedule schedule, String workflowStatusOldValue) {
		Table<String, String, Object> changePropertyTable = changePropertyTable(schedule, workflowStatusOldValue);
		if(Objects.nonNull(changePropertyTable)) {
			eventPublisher.publish(new ExecutionWorkflowModifyEvent(schedule, changePropertyTable, EventType.EXECUTION_WORKFLOW_UPDATED,
	                UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));
		}
	}

    /**
     *
     * @param schedule
     * @param existingLoggedTime
     * @param updatedLoggedTime
     */
    private void auditWorkflowChangeOnModifyTime(Schedule schedule, String existingLoggedTime, String updatedLoggedTime) {
        Table<String, String, Object> changePropertyTable = HashBasedTable.create();

            changePropertyTable.put("ExecutionWorkflowLoggedTime", ApplicationConstants.OLD, StringUtils.isNotBlank(existingLoggedTime) ? existingLoggedTime : StringUtils.EMPTY );
            changePropertyTable.put("ExecutionWorkflowLoggedTime", ApplicationConstants.NEW, updatedLoggedTime);

        if(Objects.nonNull(changePropertyTable)) {
            eventPublisher.publish(new ExecutionWorkflowModifyEvent(schedule, changePropertyTable, EventType.EXECUTION_WORKFLOW_UPDATED,
                    UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));
        }
    }

    /**
     * Prepares the request data for audit.
     * @param schedule
     * @param oldValue
     * @return
     */
	private Table<String, String, Object> changePropertyTable(Schedule schedule, String oldValue) {
        Table<String, String, Object> changePropertyTable = null;
        if (Objects.isNull(schedule))
            return null;

        changePropertyTable = HashBasedTable.create();

        if ( null != schedule.getExecutionWorkflowStatus() && !schedule.getExecutionWorkflowStatus().getName().equalsIgnoreCase(oldValue)) {
            changePropertyTable.put("ExecutionWorkflowStatus", ApplicationConstants.OLD, StringUtils.isNotBlank(oldValue) ? oldValue : StringUtils.EMPTY );
            changePropertyTable.put("ExecutionWorkflowStatus", ApplicationConstants.NEW, schedule.getExecutionWorkflowStatus().getName());
        }
        return changePropertyTable;
    }

    /**
     *
     * @param timeSpent
     * @return
     * @throws InvalidDurationException
     */
	private Long getAndValidateTimeSpent(String timeSpent) throws InvalidDurationException {
        Long millisecondsDuration = 0L;
        if (StringUtils.isNotBlank(timeSpent)) {
        	 // Need to multiply by 1000 as jiraDurationUtils returns duration in seconds
            millisecondsDuration = DateUtils.getDuration(timeSpent,getHoursPerDay(),getDaysPerWeek());
        	//millisecondsDuration = 1000 * DateUtils.getDuration(timeSpent,getHoursPerDay(),getDaysPerWeek());
        }
        return millisecondsDuration;
    }

    private String getDateStringPretty(long estimateDateValue) {
        return DateUtils.getDurationString(estimateDateValue,getHoursPerDay(),getDaysPerWeek());
    }

    private int getHoursPerDay()
    {
        return Integer.parseInt(ComponentAccessor.getApplicationProperties().getDefaultBackedString(APKeys.JIRA_TIMETRACKING_HOURS_PER_DAY));
    }

    private int getDaysPerWeek()
    {
        return Integer.parseInt(ComponentAccessor.getApplicationProperties().getDefaultBackedString(APKeys.JIRA_TIMETRACKING_DAYS_PER_WEEK));
    }

    private void reindexScheduleOnExecutionWorkflowStatusUpdate(Schedule schedule) {
        if(Objects.nonNull(schedule)) {
            try {
                log.debug("Indexing Schedule while adding custom field :");
                Collection<Schedule> schedules = new ArrayList<>();
                schedules.add(schedule);
                EnclosedIterable<Schedule> enclosedSchedules = CollectionEnclosedIterable.copy(schedules);
                scheduleIndexManager.reIndexSchedule(enclosedSchedules, Contexts.nullContext());
            } catch (Exception e) {
                log.error("Error Indexing Schedule while adding custom field :", e);
            }
        }
    }

    private void validateLoggedTimeFormat(String timeLogged) throws InvalidDurationException {

        if (StringUtils.isBlank(timeLogged)) {
            throw new InvalidDurationException();
        }
        timeLogged = timeLogged.toLowerCase();
        if (StringUtils.isNotBlank(timeLogged) && (StringUtils.contains(timeLogged, "day") ||
                StringUtils.contains(timeLogged, "hour") || StringUtils.contains(timeLogged, "minute") ||
                StringUtils.contains(timeLogged, "week") ||
                StringUtils.contains(timeLogged, "year"))) {
            throw new InvalidDurationException();
        }
    }
}
