package com.thed.zephyr.je.index;

import com.atlassian.jira.util.Consumer;
import com.atlassian.jira.util.collect.CollectionUtil;
import com.atlassian.jira.util.collect.EnclosedIterable;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.service.ScheduleManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class ScheduleIdsScheduleIterable implements EnclosedIterable<Schedule>
{
    private final Collection<Long> ids;
    private Collection<Schedule> allSchedules;
    private final ScheduleManager scheduleManager;

    
    
    public ScheduleIdsScheduleIterable(final Collection<Long> scheduleIds, 
    		final ScheduleManager scheduleManager,Collection<Schedule> schedules)
    {
        ids = Collections.unmodifiableCollection(new ArrayList<Long>(scheduleIds));
        allSchedules = Collections.unmodifiableCollection(schedules);
        this.scheduleManager = scheduleManager;
    }

    

	@Override
	public void foreach(Consumer<Schedule> sink) {
		if(ids != null && ids.size() > 0) {
	        CollectionUtil.foreach(new AbstractTransformScheduleIterator<Long>(ids)
	        {
	            @Override
	            protected Schedule transform(final Long scheduleId)
	            {
	                return scheduleManager.getSchedule(scheduleId.intValue());
	            }
	        }, sink);
		} else if(allSchedules != null && allSchedules.size() > 0) {
	        CollectionUtil.foreach(new AbstractTransformScheduleIterator<Schedule>(allSchedules)
	    	{
				@Override
				protected Schedule transform(Schedule schedule) {
					return schedule;
				}
		     }, sink);
		}
			
    }

    public int size()
    {
        return ids.size();
    }

    public boolean isEmpty()
    {
        return ids.isEmpty();
    }

    @Override
    public String toString()
    {
        return getClass().getName() + " (" + size() + " items): " + ids;
    }

}