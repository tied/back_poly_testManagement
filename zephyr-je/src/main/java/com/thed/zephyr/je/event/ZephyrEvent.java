package com.thed.zephyr.je.event;

import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.atlassian.jira.event.AbstractEvent;

public abstract class ZephyrEvent extends AbstractEvent {

	protected EventType eventType;

	public ZephyrEvent() {
		super();
	}

	public ZephyrEvent(Map<String, Object> params, EventType eventType) {
		super(params);
		this.eventType = eventType;
	}

	public String toString() {
	    return new ToStringBuilder(this).
	            append("eventType", getEventType()).
	            append("params", getParams()).
	            toString();
	}

	/**
	 * Note: this will not compare the time stamps of two events - only everything else.
	 */
	public boolean equals(Object o) {
	    if (this == o){
	        return true;
	    }
	    if (!(o instanceof SingleScheduleEvent)){
	        return false;
	    }
	
	    final SingleScheduleEvent event = (SingleScheduleEvent) o;
	
	    if (getParams() != null ? !getParams().equals(event.getParams()) : event.getParams() != null){
	        return false;
	    }
	    if (eventType != null ? !eventType.equals(event.eventType) : event.eventType != null){
	        return false;
	    }
	    return true;
	}

	public int hashCode() {
	    int result = super.hashCode();
	    result = 29 * result + (eventType != null ? eventType.hashCode() : 0);
	    return result;
	}

	public EventType getEventType() {
		return eventType;
	}

	public void setEventType(EventType eventTypeId) {
		this.eventType = eventTypeId;
	}

}