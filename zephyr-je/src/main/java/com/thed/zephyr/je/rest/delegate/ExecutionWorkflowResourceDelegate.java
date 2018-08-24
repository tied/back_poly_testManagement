package com.thed.zephyr.je.rest.delegate;

import com.atlassian.core.util.InvalidDurationException;
import com.thed.zephyr.je.model.Schedule;

/**
 * @author manjunath
 *
 */
public interface ExecutionWorkflowResourceDelegate {
	
	/**
	 * Fetch test execution from the database.
	 * 
	 * @param executionId -- Execution id to which execution details need to be fetched.
	 * @return -- Returns the fetched execution details.
	 */
	Schedule getExecution(Integer executionId);
	
	/**
	 * Starts the execution workflow by putting the status into STARTED.
	 * Also, changes the execution status to WIP.
	 * 
	 * @param schedule -- Execution to which workflow to be started.
	 */
	void startExecution(Schedule schedule);
	
	/**
	 * Pause the execution workflow by putting the status into PAUSED.
	 * In this scenario execution status won't be changed. 
	 * 
	 * @param schedule -- Execution to which workflow to be paused.
	 */
	void pauseExecution(Schedule schedule);
	
	/**
	 * Resume the execution workflow by putting the status into STARTED
	 * In this scenario execution status won't be changed.
	 * 
	 * @param schedule -- Execution to which workflow to be resumed.
	 */
	void resumeExecution(Schedule schedule);
	
	/**
	 * Complete the execution workflow by putting the status into COMPLETED.
	 * In this scenario, update the user logged time.
	 * 
	 * @param schedule -- Execution to which workflow to be completed.
	 * @param timeLogged
	 */
	void completeExecution(Schedule schedule, String timeLogged) throws InvalidDurationException;

	/**
	 * To check whether workflow is disabled for the project.
	 * 
	 * @param projectId -- Project id to which workflow is enabled or not.
	 * @return -- Returns the true value if workflow is enabled otherwise false.
	 */
	boolean isExecutionWorkflowDisabled(Long projectId);

	/**
	 * Updates the logged in time for execution workflow provided by the user.
	 * @param schedule
	 * @param timeLogged
	 */
	void modifyLoggedTimeByUser(Schedule schedule, String timeLogged) throws InvalidDurationException;

	/**
	 * Reopen the execution workflow by putting the status into REOPENED.
	 * Also, changes the execution status to REOPENED.
	 *
	 * @param schedule -- Execution to which workflow to be reopened.
	 */
    void reopenExecution(Schedule schedule);
}
