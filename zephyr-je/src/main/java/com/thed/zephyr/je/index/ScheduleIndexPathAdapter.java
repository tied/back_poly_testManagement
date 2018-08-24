package com.thed.zephyr.je.index;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.atlassian.jira.config.util.IndexPathManager;
import com.atlassian.jira.config.util.IndexWriterConfiguration;
import com.atlassian.jira.index.DefaultConfiguration;
import com.atlassian.jira.issue.index.IssueIndexer;
import com.atlassian.jira.util.NotNull;

public class ScheduleIndexPathAdapter implements ScheduleIndexDirectoryFactory
{
    private final IndexPathManager indexPathManager;
    private final IndexWriterConfiguration writerConfiguration;
    private volatile Mode strategy = Mode.QUEUED;

    public ScheduleIndexPathAdapter(final @NotNull IndexPathManager indexPathManager, final IndexWriterConfiguration writerConfiguration)
    {
        this.indexPathManager = notNull("indexPathManager", indexPathManager);
        this.writerConfiguration = notNull("writerConfiguration", writerConfiguration);
    }

    public Map<EntityName, Index.Manager> get()
    {
        final Mode strategy = this.strategy;
        final EnumMap<EntityName, Index.Manager> indexes = new EnumMap<EntityName, Index.Manager>(EntityName.class);
        for (final EntityName type : EntityName.values())
        {
            indexes.put(type, strategy.createIndexManager(type.name(), new DefaultConfiguration(type.directory(indexPathManager),
                IssueIndexer.Analyzers.INDEXING, writerConfiguration)));
        }
        return Collections.unmodifiableMap(indexes);
    }

    public String getIndexRootPath()
    {
        return indexPathManager.getIndexRootPath();
    }

    public List<String> getIndexPaths()
    {
        final List<String> result = new ArrayList<String>(EntityName.values().length);
        for (final EntityName indexType : EntityName.values())
        {
            try
            {
                result.add(indexType.getPath(indexPathManager));
            }
            catch (final RuntimeException ignore)
            {
                //probable not setup
            }
        }
        return Collections.unmodifiableList(result);
    }

    public void setIndexingMode(final Mode strategy)
    {
        this.strategy = strategy;
    }
}