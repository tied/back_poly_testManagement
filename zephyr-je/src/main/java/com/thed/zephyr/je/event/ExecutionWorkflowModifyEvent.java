package com.thed.zephyr.je.event;

import com.atlassian.event.api.AsynchronousPreferred;
import com.google.common.collect.Table;
import com.thed.zephyr.je.model.Schedule;

/**
 * @author manjunath
 *
 */
@AsynchronousPreferred
public class ExecutionWorkflowModifyEvent extends ZephyrEvent {
	
	private Schedule schedule;
	private String userName;
	private Table<String, String, Object> changePropertyTable;
	
	public ExecutionWorkflowModifyEvent(Schedule schedule, Table<String, String, Object> changePropertyTable, EventType eventType, String userName) {
        super(null, eventType);
        this.schedule = schedule;
        this.changePropertyTable = changePropertyTable;
        this.userName = userName;
    }
	
	public int hashCode(){
        int result = super.hashCode();
        result = 29 * result + (schedule != null ? schedule.hashCode() : 0);
        result = 29 * result + (eventType != null ? eventType.hashCode() : 0);
        result = 29 * result + (changePropertyTable != null ? changePropertyTable.hashCode() : 0);
        result = 29 * result + (userName != null ? userName.hashCode() : 0);
        return result;
    }

	public Schedule getSchedule() {
		return schedule;
	}

	public void setSchedule(Schedule schedule) {
		this.schedule = schedule;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public Table<String, String, Object> getChangePropertyTable() {
		return changePropertyTable;
	}

	public void setChangePropertyTable(Table<String, String, Object> changePropertyTable) {
		this.changePropertyTable = changePropertyTable;
	}

}
