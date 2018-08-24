package com.thed.zephyr.je.event;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.Table;
import com.thed.zephyr.je.model.Schedule;

public class ScheduleModifyEvent extends ZephyrEvent {
	
	private Schedule schedule;
	private String userName;
	private Collection<Schedule> schedules;
	private Table<String, String, Object> changePropertyTable;
	
    public ScheduleModifyEvent(Collection<Schedule> schedules, Map<String, Object> params,
    		EventType eventType, String userName){
        super(params, eventType);
        this.schedules=schedules;
        this.userName = userName;
    }
	
	public ScheduleModifyEvent(Schedule schedule, Table<String, String, Object> changePropertyTable,
    		EventType eventType, String userName){
        super(null, eventType);
        this.schedule=schedule;
        this.changePropertyTable = changePropertyTable;
        this.userName = userName;
    }
	
    public int hashCode(){
        int result = super.hashCode();
        result = 29 * result + (eventType != null ? eventType.hashCode() : 0);
        result = 29 * result + (changePropertyTable != null ? changePropertyTable.hashCode() : 0);
        result = 29 * result + (userName != null ? userName.hashCode() : 0);
        result = 29 * result + (schedules != null ? schedules.hashCode() : 0);
        result = 29 * result + (schedule != null ? schedule.hashCode() : 0);
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

	public Collection<Schedule> getSchedules() {
		return schedules;
	}

	public void setSchedules(Collection<Schedule> schedules) {
		this.schedules = schedules;
	}

	public Table<String, String, Object> getChangePropertyTable() {
		return changePropertyTable;
	}

	public void setChangePropertyTable(
			Table<String, String, Object> changePropertyTable) {
		this.changePropertyTable = changePropertyTable;
	}    
    
}
