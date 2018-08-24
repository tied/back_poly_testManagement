package com.thed.zephyr.je.index;

import net.jcip.annotations.GuardedBy;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.IndexSearcher;

import com.atlassian.jira.issue.index.JiraAnalyzer;
import com.atlassian.jira.task.context.Context;
import com.atlassian.jira.util.NotNull;
import com.atlassian.jira.util.collect.EnclosedIterable;
import com.thed.zephyr.je.index.ScheduleIndexDirectoryFactory.Mode;
import com.thed.zephyr.je.model.Schedule;

import java.io.IOException;

public interface ScheduleIndexer
{

    long[] getAllScheduleDocuments() throws Exception;

    long[] getAllTestcaseDocuments() throws Exception;

    public static class Analyzers
    {
        public static final Analyzer SEARCHING = JiraAnalyzer.ANALYZER_FOR_SEARCHING;
        public static final Analyzer INDEXING = JiraAnalyzer.ANALYZER_FOR_INDEXING;
    }

    /**
     * Add documents for the supplied schedules.
     * 
     * @param entities An iterable of entities to index.
     * @param context for showing the user the current status.
     */
    Index.Result indexSchedules(@NotNull EnclosedIterable<Schedule> entities, @NotNull Context context, String jobProgressToken);

    /**
     * Delete any existing documents for the supplied schedules.
     * 
     * @param entities An iterable of entities to index.
     * @param context for showing the user the current status.
     */
    Index.Result deleteScheduleIndexes(@NotNull EnclosedIterable<Schedule> entities, @NotNull Context context, String jobProgressToken);

    /**
     * Delete any existing documents for the supplied Id and Term.
     * 
     * @param scheduleIds An iterable of entities to delete.
     * @param context for showing the user the current status.
     */
    Index.Result deleteBatchIndexByTerm(@NotNull final EnclosedIterable<String> scheduleIds,@NotNull final String term, @NotNull final Context context);

     
    /**
     * Re-index the given issues, delete any existing documents and add new ones.
     * 
     * @param schedules An iterable of schedules to index.
     * @param context for showing the user the current status.
     */
    Index.Result reIndexSchedules(@NotNull EnclosedIterable<Schedule> schedules, @NotNull Context context, String jobProgressToken);

    /**
     * Index the given schedules, use whatever is in your arsenal to do it as FAST as possible.
     *
     * @param schedules An iterable of schedules to index.
     * @param context for showing the user the current status.
     */
    @GuardedBy("external indexing lock")
    Index.Result indexSchedulesBatchMode(@NotNull EnclosedIterable<Schedule> schedules, @NotNull Context context, String jobProgressToken);

    @GuardedBy("external indexing lock")
    Index.Result optimize();

    // @TODO maybe return result?
    void deleteScheduleIndexes();

    void shutdown();
    
    void setMode(Mode mode);

    IndexSearcher getScheduleIndexSearcher();

    Index.Result reIndexSchedulesInBatch(@NotNull EnclosedIterable<Schedule> schedules, Context context, String jobProgressToken);
    
    boolean isSchedulePresentInIndex(String scheduleId);

//	ExecutionsByDurationCollector searchWithDocument(Query query);
    
    void updateIndexWithNewFilesFromZip(String zipFilePath) throws IOException; 
    
    void removeDuplicateSchedules(int dbCount);
}
