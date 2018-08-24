package com.thed.zephyr.je.event;

import java.util.Collection;
import java.util.Map;


import com.atlassian.event.api.AsynchronousPreferred;
import com.thed.zephyr.je.model.Schedule;

@AsynchronousPreferred
public final class SingleScheduleEvent extends ZephyrEvent {
    private Collection<Schedule> schedules;

    /**
     * Create a new IssueEvent with a given list of parameters.
     *
     * @param schedule    the schedule this event refers to
     * @param params      parameters that can be retrieved by the Listener
     * @param user        the user who has initiated this event
     * @param eventType the type ID of this event
     */
    public SingleScheduleEvent(Collection<Schedule> schedules, Map params, EventType eventType){
        super(params, eventType);
        this.schedules=schedules;
    }


    public int hashCode(){
        int result = super.hashCode();
        result = 29 * result + (schedules != null ? schedules.hashCode() : 0);
        result = 29 * result + (eventType != null ? eventType.hashCode() : 0);
        return result;
    }


	public Collection<Schedule> getSchedules() {
		return schedules;
	}


	public void setSchedules(Collection<Schedule> schedules) {
		this.schedules = schedules;
	}
}
