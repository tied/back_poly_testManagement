package com.thed.zephyr.je.index;

import com.atlassian.jira.util.NotNull;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

/**
 * Created by niravshah on 5/1/18.
 */
public class ZephyrIndexProvider implements Iterable<Index.Manager> {
    private final AtomicReference<Map<ScheduleIndexDirectoryFactory.EntityName, Index.Manager>> ref = new AtomicReference<Map<ScheduleIndexDirectoryFactory.EntityName, Index.Manager>>();
    private final ScheduleIndexDirectoryFactory factory;

    public ZephyrIndexProvider(@NotNull final ScheduleIndexDirectoryFactory factory)
    {
        this.factory = notNull("factory", factory);
    }

    public Iterator<Index.Manager> iterator()
    {
        return open().values().iterator();
    }

    void close()
    {
        final Map<ScheduleIndexDirectoryFactory.EntityName, Index.Manager> indexes = ref.getAndSet(null);
        if (indexes == null)
        {
            return;
        }
        for (final Index.Manager manager : indexes.values())
        {
            manager.close();
        }
    }

    Map<ScheduleIndexDirectoryFactory.EntityName, Index.Manager> open()
    {
        Map<ScheduleIndexDirectoryFactory.EntityName, Index.Manager> result = ref.get();
        while (result == null)
        {
            ref.compareAndSet(null, factory.get());
            result = ref.get();
        }
        return result;
    }

    Index getEntityIndex()
    {
        return get(ScheduleIndexDirectoryFactory.EntityName.JEENTITY).getIndex();
    }

    Index.Manager get(final ScheduleIndexDirectoryFactory.EntityName key)
    {
        return open().get(key);
    }

    void setMode(final ScheduleIndexDirectoryFactory.Mode type)
    {
        factory.setIndexingMode(type);
    }
}
