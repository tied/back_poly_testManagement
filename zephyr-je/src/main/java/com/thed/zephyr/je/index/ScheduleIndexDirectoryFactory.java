package com.thed.zephyr.je.index;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.util.IndexPathManager;
import com.atlassian.jira.index.Configuration;
import com.atlassian.jira.util.LuceneDirectoryUtils;
import com.atlassian.jira.util.NotNull;
import com.atlassian.jira.util.PathUtils;
import com.atlassian.jira.util.Supplier;
import com.thed.zephyr.je.index.Index.Manager;
import org.apache.lucene.store.Directory;

import java.io.File;
import java.util.List;
import java.util.Map;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

/**
 * Responsible for creating the {@link Directory directories} required for schedule indexing.
 *
 */
public interface ScheduleIndexDirectoryFactory extends Supplier<Map<ScheduleIndexDirectoryFactory.EntityName, Index.Manager>>
{
    enum Mode
    {
        DIRECT
        {
            @Override
            Manager createIndexManager(final String name, final Configuration configuration)
            {
                return ZFJIndexes.createSimpleIndexManager(configuration);
            }
        },
        QUEUED
        {
            @Override
            Manager createIndexManager(final String name, final Configuration configuration)
            {
                //long maxQueueSize = PropertiesUtil.getIntProperty(applicationProperties, APKeys.JiraIndexConfiguration.Issue.MAX_QUEUE_SIZE, 1000);
            	return ZFJIndexes.createQueuedIndexManager(name, configuration,1000);
            }
        };

        abstract Index.Manager createIndexManager(String name, Configuration configuration);
    }

    enum EntityName
    {
        JEENTITY
        {
            @Override
            @NotNull
            String getPath(final IndexPathManager indexPathManager)
            {
                String path = PathUtils.appendFileSeparator(indexPathManager.getIndexRootPath()) + "JEEntity/schedule";
                return verify(indexPathManager, path);
            }
        };


        @NotNull
        public Directory directory(@NotNull final IndexPathManager indexPathManager)
        {
            LuceneDirectoryUtils luceneDirectoryUtils = ComponentAccessor.getComponent(LuceneDirectoryUtils.class);
            return luceneDirectoryUtils.getDirectory(new File(getPath(indexPathManager)));
        }

        final @NotNull
        String verify(final IndexPathManager indexPathManager, final String path) throws IllegalStateException
        {
            if (indexPathManager.getMode() == IndexPathManager.Mode.DISABLED)
            {
                throw new IllegalStateException("Indexing is disabled.");
            }
            return notNull("Index path is null: " + this, path);
        }

        abstract @NotNull
        String getPath(@NotNull IndexPathManager indexPathManager);
    }

    String getIndexRootPath();

    List<String> getIndexPaths();

    /**
     * Sets the Indexing Mode - one of either DIRECT or QUEUED.
     *
     * @param mode the indexing mode.
     */
    void setIndexingMode(@NotNull Mode mode);
}
