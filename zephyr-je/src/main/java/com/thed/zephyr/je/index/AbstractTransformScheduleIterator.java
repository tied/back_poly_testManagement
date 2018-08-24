package com.thed.zephyr.je.index;

import java.util.Collection;
import java.util.Iterator;

import com.thed.zephyr.je.model.Schedule;

public abstract class AbstractTransformScheduleIterator<T> implements Iterator<Schedule>
{
    protected final Iterator<T> iterator;

    protected AbstractTransformScheduleIterator(final Collection<T> objects)
    {
        iterator = objects.iterator();
    }

    public Schedule nextSchedule()
    {
        return transform(iterator.next());
    }

    public Schedule next()
    {
        return nextSchedule();
    }

    protected abstract Schedule transform(T o);

    public boolean hasNext()
    {
        return iterator.hasNext();
    }

    public void remove()
    {
        throw new UnsupportedOperationException("Cannot remove an schedule from an Schedule Iterator");
    }
}
