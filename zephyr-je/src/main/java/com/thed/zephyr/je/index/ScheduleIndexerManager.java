package com.thed.zephyr.je.index;

import static com.atlassian.jira.util.dbc.Assertions.notNull;
import static com.atlassian.util.concurrent.ManagedLocks.weakManagedLockFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.IndexSearcher;
import com.atlassian.jira.index.Index.UpdateMode;
import com.atlassian.jira.index.MultiThreadedIndexingConfiguration;
import com.atlassian.jira.task.context.Context;
import com.atlassian.jira.util.Consumer;
import com.atlassian.jira.util.Function;
import com.atlassian.jira.util.NotNull;
import com.atlassian.jira.util.collect.EnclosedIterable;
import com.atlassian.util.concurrent.ManagedLock;
import com.thed.zephyr.je.index.Index.Operation;
import com.thed.zephyr.je.index.Index.Result;
import com.thed.zephyr.je.index.ScheduleIndexDirectoryFactory.EntityName;
import com.thed.zephyr.je.index.ScheduleIndexDirectoryFactory.Mode;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.service.JobProgressService;

import net.jcip.annotations.GuardedBy;

public class ScheduleIndexerManager implements ScheduleIndexer
{
    protected final Logger log = Logger.getLogger(ScheduleIndexerManager.class);

    private static final MultiThreadedIndexingConfiguration multiThreadedIndexingConfiguration = new MultiThreadedIndexingConfiguration()
    {
        public int minimumBatchSize()
        {
            return 50;
        }

        public int maximumQueueSize()
        {
            return 1000;
        }

        public int noOfThreads()
        {
            return 10;
        }
    };


    private final ZephyrIndexProvider lifecycle;
    private final IndexingStrategy simpleIndexingStrategy = new SimpleIndexingStrategy();
    private final DocumentCreationStrategy documentCreationStrategy = new EntityLockDocumentCreationStrategy();
    private final JobProgressService jobProgressService;


    public ScheduleIndexerManager(@NotNull final ScheduleIndexDirectoryFactory jeEntityIndexDirectoryFactory, JobProgressService jobProgressService)
    {
        this.lifecycle = new ZephyrIndexProvider(jeEntityIndexDirectoryFactory);
        this.jobProgressService = jobProgressService;
    }


    @GuardedBy("external index read lock")
    public Index.Result deleteScheduleIndexes(@NotNull final EnclosedIterable<Schedule> schedules,
                                              @NotNull final Context context, String jobProgressToken)
    {
         return perform(schedules, simpleIndexingStrategy, context, new IndexOperation()
        {
            public Index.Result perform(final Schedule schedule, final Context.Task task)
            {
                final Term issueTerm = getNewIdentifyingTerm(schedule);
                final Operation delete = Operations.newDelete(issueTerm, UpdateMode.INTERACTIVE);
                final Operation onCompletion = Operations.newCompletionDelegate(delete, new TaskCompleter(task));
                final AccumulatingResultBuilder results = new AccumulatingResultBuilder();
                results.add(lifecycle.getEntityIndex().perform(onCompletion));
                return results.toResult();
            }
        }, jobProgressToken);
    }

    
    @GuardedBy("external index read lock")
    public Index.Result deleteBatchIndexByTerm(@NotNull final EnclosedIterable<String> entityIds, @NotNull final String term,@NotNull final Context context)
    {
        final AccumulatingResultBuilder results = new AccumulatingResultBuilder();
    	entityIds.foreach(new Consumer<String>()
        {
            public void consume(final String entityId)
            {
                // wrap the updater task in a Job and give it a Context.Task so we can tell the user what's happening
            	Long id =Long.valueOf(entityId);
                final Context.Task task = context.start(id);
                final Term issueTerm = getEntityIdentifyingTerm(term,id);
                final Operation delete = Operations.newDelete(issueTerm, UpdateMode.INTERACTIVE);
                final Operation onCompletion = Operations.newCompletionDelegate(delete, new TaskCompleter(task));
                results.add(lifecycle.getEntityIndex().perform(onCompletion));
            }
        });
    	return results.toResult();
    }

    
    @GuardedBy("external index read lock")
    public Index.Result indexSchedules(@NotNull final EnclosedIterable<Schedule> schedules, @NotNull final Context context, String jobProgressToken)
    {
        return perform(schedules, simpleIndexingStrategy, context, new IndexEntityOperation(UpdateMode.INTERACTIVE), jobProgressToken);
    }

    /**
     * No other index operations should be called while this method is being called
     */
    @GuardedBy("external index write lock")
    public Index.Result indexSchedulesBatchMode(@NotNull final EnclosedIterable<Schedule> entities, @NotNull final Context context, String jobProgressToken)
    {
        if (entities.size() < multiThreadedIndexingConfiguration.minimumBatchSize()) {
            return indexSchedules(entities, context, jobProgressToken);
        }

        lifecycle.close();
        lifecycle.setMode(Mode.DIRECT);
        try {
            return perform(entities, new MultiThreadedIndexingStrategy(simpleIndexingStrategy, multiThreadedIndexingConfiguration, "EntityIndexer"),
                context, new IndexEntityOperation(UpdateMode.BATCH), jobProgressToken);
        } finally {
            lifecycle.close();
            lifecycle.setMode(Mode.QUEUED);
        }
    }

    @GuardedBy("external index read lock")
    public Index.Result reIndexSchedules(@NotNull final EnclosedIterable<Schedule> entities,
                                         @NotNull final Context context, String jobProgressToken)
    {
        return perform(entities, simpleIndexingStrategy, context, (schedule, task) -> {
            try {
                final UpdateMode mode = UpdateMode.INTERACTIVE;
                final Documents documents = documentCreationStrategy.get(schedule);
                final Term issueTerm = documents.getIdentifyingTerm();
                final Operation update = Operations.newUpdate(issueTerm, documents.getEntityDocument(), mode);
                final Operation onCompletion = Operations.newCompletionDelegate(update, new TaskCompleter(task));
                final AccumulatingResultBuilder results = new AccumulatingResultBuilder();
                results.add(lifecycle.getEntityIndex().perform(onCompletion));
                return results.toResult();
            } catch (final Exception ex) {
                return new ZFJDefaultIndex.Failure(ex);
            }
        }, jobProgressToken);
    }


    @GuardedBy("external index read lock")
    public Index.Result reIndexSchedulesInBatch(@NotNull final EnclosedIterable<Schedule> entities,
                                         @NotNull final Context context, String jobProgressToken)
    {
        lifecycle.close();
        lifecycle.setMode(Mode.DIRECT);
        try
        {
            return performInBatch(entities, new MultiThreadedIndexingStrategy(simpleIndexingStrategy, multiThreadedIndexingConfiguration, "EntityIndexer"),
                    context, new IndexEntityOperation(UpdateMode.BATCH), jobProgressToken);
        }
        finally
        {
            lifecycle.close();
            lifecycle.setMode(Mode.QUEUED);
        }
    }

    

    @Override
    public void deleteScheduleIndexes()
    {
        for (final Index.Manager manager : lifecycle)
        {
            manager.deleteIndexDirectory();
        }
    }

    public IndexSearcher getScheduleIndexSearcher()
    {
        return lifecycle.get(EntityName.JEENTITY).getSearcher();
    }
    
    private Term getNewIdentifyingTerm(Schedule schedule) {
		return new Term("schedule_id", String.valueOf(schedule.getID()));
	}
    
    
    private Term getEntityIdentifyingTerm(String term, Long entityId) {
		return new Term(term, String.valueOf(entityId));
	}

    @Override
    public long[] getAllScheduleDocuments() throws Exception {
        IndexSearcher indexSearcher = getScheduleIndexSearcher();
        IndexReader indexReader = indexSearcher.getIndexReader();
        // We know implicitly that the there is exactly one and only one schedule Id per document
        TermEnum termEnum = indexReader.terms(new Term("schedule_id", ""));
        long[] scheduleIds = new long[indexReader.numDocs()];
        Set<Integer> scheduleSet = new HashSet<>();
        try {
            int i = 0;
            do {
                Term term = termEnum.term();
                // Lucene terms are interned so the != comparison is safe.
                if (term == null || term.field() != "schedule_id") {
                    // No issues. May happen
                    break;
                }
                String scheduleId = term.text();
                if(!scheduleSet.contains(Integer.valueOf(scheduleId))) {
                    scheduleSet.add(Integer.valueOf(scheduleId));
                    scheduleIds = ensureCapacity(scheduleIds, i + 1);
                    scheduleIds[i] = Long.valueOf(scheduleId);
                    i++;
                }
            }
            while (termEnum.next());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error getting Schedule",e);
            return new long[0];
        } finally {
            try {
                termEnum.close();
                indexSearcher.close();
            } catch (Exception e) {
               log.error("Error Closing Term",e);
            }
        }
        return scheduleIds;
    }
    
    @Override
    public boolean isSchedulePresentInIndex(String scheduleId) {
        IndexReader indexReader = getScheduleIndexSearcher().getIndexReader();
        try {
            TermEnum termEnum = indexReader.terms(new Term("schedule_id", scheduleId));
            if(termEnum.docFreq() > 0) {
            	return true;
            }
        } catch(IOException io) {
        	log.error("Error in searching the term for schedule id - " + scheduleId, io);
        	return false;
        }
        return false;
    }


    @Override
    public long[] getAllTestcaseDocuments() throws Exception {
    	IndexSearcher indexSearcher = getScheduleIndexSearcher();
        IndexReader indexReader = indexSearcher.getIndexReader();

        TermEnum termEnum = indexReader.terms(new Term("ISSUE_ID", ""));
        long[] issueIds = new long[indexReader.numDocs()];
        Set<Long> issueSet = new HashSet<>();
        try {
            int i = 0;
            do {
                Term term = termEnum.term();
                // Lucene terms are interned so the != comparison is safe.
                if (term == null || term.field() != "ISSUE_ID") {
                    // No issues. May happen
                    break;
                }
                String issueId = term.text();
                if(!issueSet.contains(Long.valueOf(issueId))) {
                    issueSet.add(Long.valueOf(issueId));
                    issueIds = ensureCapacity(issueIds, i + 1);
                    issueIds[i] = Long.valueOf(issueId);
                    i++;
                }
            }
            while (termEnum.next());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error getting Issue/Testcase document",e);
            return new long[0];
        } finally {
            try {
                termEnum.close();
                indexSearcher.close();
            } catch (Exception e) {
                log.error("Error Closing Term",e);
            }
        }
        return issueIds;
    }


    /**
     * Ensure the array has at least i elements.
     *
     * @param issueIds     Array to test.
     * @param requiredSize required Size.
     */
    protected long[] ensureCapacity(final long[] scheduleIds, final int requiredSize) {
        if (scheduleIds.length < requiredSize) {
            // Expand the array.  This should occur rarely if ever so we only add a small increment
            int newSize = Math.max(requiredSize, scheduleIds.length + scheduleIds.length / 10);
            return Arrays.copyOf(scheduleIds, newSize);
        }
        return scheduleIds;
    }
    public Index.Result optimize()
    {
        final AccumulatingResultBuilder builder = new AccumulatingResultBuilder();
        for (final Index.Manager manager : lifecycle)
        {
            builder.add(manager.getIndex().perform(Operations.newOptimize()));
        }
        return builder.toResult();
    }

    public void shutdown()
    {
        lifecycle.close();
    }
    
    public void setMode(Mode mode)
    {
        lifecycle.setMode(mode);
    }


  
    /**
     * Perform an {@link IndexOperation} on some {@link EnclosedIterable Schedule} using a particular 
     * {@link IndexingStrategy strategy}. There is a {@link Context task context} that must be
     * updated to provide feedback to the user.
     * <p>
     * The implementation needs to be thread-safe, as it may be run in parallel and maintain a 
     * composite result to return to the caller.
     * 
     * @param rawEntities the entities to index/deindex/reindex
     * @param strategy single or multi-threaded
     * @param context task context for status feedback
     * @param operation deindex/reindex/index etc.
     * @return the {@link Result} may waited on or not.
     */
    private Index.Result perform(final EnclosedIterable<Schedule> entities, final IndexingStrategy strategy,
                                 final Context context, final IndexOperation operation, final String jobProgressToken)
    {
        try
        {
            notNull("entities", entities);
            // thread-safe handler for the asynchronous Result 
            final AccumulatingResultBuilder builder = new AccumulatingResultBuilder();
            // perform the operation for every issue in the collection
            entities.foreach(inputSchedule -> {
                if(inputSchedule != null) {
                	 // wrap the updater task in a Job and give it a Context.Task so we can tell the user what's happening
                    final Context.Task task = context.start(inputSchedule);
                    // ask the Strategy for the Result, this may be performed on a thread-pool
                    // the result may be a future if asynchronous
                    final Result result = strategy.get(() -> {
                        // the actual index operation
                        return operation.perform(inputSchedule, task);
                    });
                    builder.add(result);
                }
                jobProgressService.addCompletedSteps(jobProgressToken,1);
             });
            return builder.toResult();
        }
        finally
        {
            strategy.close();
        }
    }


    /**
     * Perform an {@link IndexOperation} on some {@link EnclosedIterable Schedule} using a particular
     * {@link IndexingStrategy strategy}. There is a {@link Context task context} that must be
     * updated to provide feedback to the user.
     * <p>
     * The implementation needs to be thread-safe, as it may be run in parallel and maintain a
     * composite result to return to the caller.
     *
     * @param rawEntities the entities to index/deindex/reindex
     * @param strategy single or multi-threaded
     * @param context task context for status feedback
     * @param operation deindex/reindex/index etc.
     * @return the {@link Result} may waited on or not.
     */
    private Index.Result performInBatch(final EnclosedIterable<Schedule> entities, final IndexingStrategy strategy,
                                 final Context context, final IndexOperation operation, final String jobProgressToken)
    {
        try
        {
            notNull("entities", entities);
            // thread-safe handler for the asynchronous Result
            final AccumulatingResultBuilder builder = new AccumulatingResultBuilder();
            // perform the operation for every issue in the collection
            entities.foreach(inputSchedule -> {
                if(inputSchedule != null) {
                	// wrap the updater task in a Job and give it a Context.Task so we can tell the user what's happening
                    final Context.Task task = context.start(inputSchedule);
                    // ask the Strategy for the Result, this may be performed on a thread-pool
                    // the result may be a future if asynchronous
                    final Result result = strategy.get(() -> {
                        return operation.perform(inputSchedule, task);
                    });
                    builder.add(result);
                }
                jobProgressService.addCompletedSteps(jobProgressToken,1);
            });
            return builder.toResult();
        }
        finally
        {
            strategy.close();
        }
    }

    //
    // inner classes
    //

    /**
     * Used when indexing to do the actual indexing of an issue.
     */
    private class IndexEntityOperation implements IndexOperation
    {
        final UpdateMode mode;

        IndexEntityOperation(final UpdateMode mode)
        {
            this.mode = mode;
        }

        public Index.Result perform(final Schedule schedule, final Context.Task task)
        {
            try {

                final Documents documents = documentCreationStrategy.get(schedule);
                final Operation scheduleCreate = Operations.newCreate(documents.getEntityDocument(), mode);
                final Operation onCompletion = Operations.newCompletionDelegate(scheduleCreate, new TaskCompleter(task));
                final AccumulatingResultBuilder results = new AccumulatingResultBuilder();
                results.add(lifecycle.getEntityIndex().perform(onCompletion));
                return results.toResult();
            } catch(Exception e) {
                return new ZFJDefaultIndex.Failure(e);
            }
        }
    }

    private interface IndexOperation extends EntityOperation<Schedule> {
    }


    private interface EntityOperation<T> {
        Index.Result perform(T entity, Context.Task task);
    }

    private static class TaskCompleter implements Runnable
    {
        private final Context.Task task;

        public TaskCompleter(final Context.Task task)
        {
            this.task = task;
        }

        public void run()
        {
            task.complete();
        }
    }

    interface DocumentCreationStrategy extends Function<Schedule, Documents>
    {}

    class Documents{
        private final Document entityDocument;
        private final Term term;

        Documents(final Schedule schedule, final Document entityDocument)
        {
            this.entityDocument = entityDocument;
            term = getNewIdentifyingTerm(schedule);
        }

        Document getEntityDocument()
        {
            return entityDocument;
        }

        Term getIdentifyingTerm()
        {
            return term;
        }
    }

    /**
     * Get the documents (issue and comments) for the issue under a lock per issue.
     */
    class EntityLockDocumentCreationStrategy implements DocumentCreationStrategy{
    	private final com.atlassian.util.concurrent.Function<Schedule, ManagedLock> lockManager = weakManagedLockFactory(new com.atlassian.util.concurrent.Function<Schedule, Integer>()        {
            public Integer get(final Schedule schedule)
            {
           		return schedule.getID();
            }
        });

        public Documents get(final Schedule schedule){
            return lockManager.get(schedule).withLock(new com.atlassian.util.concurrent.Supplier<Documents>()
            {
                public Documents get()
                {
                    return new Documents(schedule, ScheduleDocument.getDocument(schedule));
                }
            });
        }
    }

	@Override
	public void updateIndexWithNewFilesFromZip(String zipFilePath) throws IOException {
		for (final Index.Manager manager : lifecycle)
        {
            manager.updateIndexWithNewFilesFromZip(zipFilePath);
        }
	}
	
	@Override
	public void removeDuplicateSchedules(int dbCount) {
		for (final Index.Manager manager : lifecycle)
        {
            manager.removeDuplicateSchedules(dbCount);
        }
	}
}