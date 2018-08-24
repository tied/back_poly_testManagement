package com.thed.zephyr.je.index;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.instrumentation.operations.OpTimer;
import com.atlassian.jira.bc.issue.search.SearchService.ParseResult;
import com.atlassian.jira.charts.ChartFactory;
import com.atlassian.jira.charts.ChartFactory.PeriodName;
import com.atlassian.jira.charts.jfreechart.util.ChartUtil;
import com.atlassian.jira.charts.util.DataUtils;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.util.IndexPathManager;
import com.atlassian.jira.config.util.IndexingConfiguration;
import com.atlassian.jira.instrumentation.Instrumentation;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.index.IndexException;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.statistics.DatePeriodStatisticsMapper;
import com.atlassian.jira.issue.statistics.StatisticsMapper;
import com.atlassian.jira.jql.query.QueryCreationContext;
import com.atlassian.jira.jql.query.QueryCreationContextImpl;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.task.context.Context;
import com.atlassian.jira.task.context.Contexts;
import com.atlassian.jira.timezone.TimeZoneManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.usercompatibility.UserCompatibilityHelper;
import com.atlassian.jira.util.Consumer;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.NotNull;
import com.atlassian.jira.util.PathUtils;
import com.atlassian.jira.util.Supplier;
import com.atlassian.jira.util.collect.CollectionEnclosedIterable;
import com.atlassian.jira.util.collect.EnclosedIterable;
import com.atlassian.jira.util.dbc.Assertions;
import com.google.common.collect.Ordering;
import com.thed.zephyr.je.config.model.ExecutionStatus;
import com.thed.zephyr.je.index.Index.Result;
import com.thed.zephyr.je.index.ScheduleIndexDirectoryFactory.Mode;
import com.thed.zephyr.je.index.cluster.ClusterProperties;
import com.thed.zephyr.je.index.cluster.MessageHandler;
import com.thed.zephyr.je.index.cluster.ZFJMessage;
import com.thed.zephyr.je.index.cluster.ZFJMessage.ZFJMessageType;
import com.thed.zephyr.je.model.Cycle;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.service.CycleManager;
import com.thed.zephyr.je.service.JobProgressService;
import com.thed.zephyr.je.service.ScheduleManager;
import com.thed.zephyr.je.vo.ExecutionSummaryImpl;
import com.thed.zephyr.je.zql.core.LuceneQueryBuilder;
import com.thed.zephyr.je.zql.core.SearchService;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.IssueUtils;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.ZephyrComponentAccessor;
import com.thed.zephyr.util.ZipDir;
import com.thed.zephyr.util.collector.ExecutionsByDurationCollector;
import com.thed.zephyr.util.collector.ScheduleBurndownDurationHitCollector;
import com.thed.zephyr.util.collector.ScheduleDefectHitCollector;
import com.thed.zephyr.util.collector.UnexecutedSchedulesCollector;

import net.jcip.annotations.GuardedBy;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.*;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimePeriod;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;

import javax.annotation.Nonnull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

public class ScheduleIndexManagerImpl implements ScheduleIndexManager {
    protected final Logger log = Logger.getLogger(ScheduleIndexManagerImpl.class);
	private final ScheduleIndexer scheduleIndexer;
	private final ScheduleManager scheduleManager;
	private final UserManager userManager;
	private final CycleManager cycleManager;
    private final IndexingConfiguration indexConfig;
    private final TimeZoneManager timeZoneManager;
    private final IndexLocks indexLock = new IndexLocks();
    private final IssueManager issueManager;
    private final IndexPathManager indexPathManager;
    private final LuceneQueryBuilder luceneQueryBuilder;
    private final SearchService searchService;
    private final JiraAuthenticationContext authContext;
	private final JobProgressService jobProgressService;
	private final ClusterProperties clusterProperties;

    ScheduleIndexManagerImpl(final ScheduleIndexer scheduleIndexer, final UserManager userManager,
							 final CycleManager cycleManager, final IndexingConfiguration indexConfig,
							 TimeZoneManager timeZoneManager,
							 final IssueManager issueManager, final IndexPathManager indexPathManager,
							 final LuceneQueryBuilder luceneQueryBuilder, SearchService searchService,
							 final JiraAuthenticationContext authContext, JobProgressService jobProgressService,
							 ScheduleManager scheduleManager, ClusterProperties clusterProperties) {
		this.scheduleIndexer=scheduleIndexer;
		this.userManager=userManager;
		this.cycleManager=cycleManager;
		this.indexConfig=indexConfig;
		this.timeZoneManager=timeZoneManager;
		this.issueManager=issueManager;
		this.indexPathManager = indexPathManager;
        this.luceneQueryBuilder = luceneQueryBuilder;
        this.searchService = searchService;
        this.authContext = authContext;
		this.jobProgressService = jobProgressService;
		this.scheduleManager=scheduleManager;
		this.clusterProperties = clusterProperties;
	}
	
    // responsible for getting the actual searcher when required
    private final Supplier<IndexSearcher> scheduleSearcherSupplier = new Supplier<IndexSearcher>() {
        public IndexSearcher get() {
            try {
                return scheduleIndexer.getScheduleIndexSearcher();
            } catch (final RuntimeException e) {
                throw e;
            }
        }
    };
    
    @Override
    public boolean indexSchedules(@NotNull EnclosedIterable<Schedule> scheduleIterable, @NotNull final Context context) {
		String jobProgressToken = "";
        final OpTimer opTimer = Instrumentation.pullTimer("Execution");
        List<Schedule> scheduleList = new ArrayList<>(scheduleIterable.size());
        if (!getIndexLock())
        {
            log.error("Unable to acquire lock, could not reindex: " + scheduleIterable.toString());
            scheduleIterable.foreach(scheduleList::add);
            sendMessageToOtherNodes(scheduleList, false);
            return false;
        }
        try {			
			scheduleIterable.foreach(new Consumer<Schedule>()
			{
				public void consume(final Schedule schedule)
				{
					if(schedule != null) {
						List<Schedule> schedules = new ArrayList<Schedule>();
						schedules.add(schedule);
						scheduleList.add(schedule);
						EnclosedIterable<Schedule> tests = CollectionEnclosedIterable.copy(schedules);
						await(scheduleIndexer.indexSchedules(tests, context, jobProgressToken));
					} else {
						jobProgressService.addCompletedSteps(jobProgressToken,1);
					}
				}
			});
			sendMessageToOtherNodes(scheduleList, true);
        } catch(Exception e) {
        	log.warn("Exception");
        } finally{
            releaseIndexLock();
            try {
            	ZFJSearcherCache.getThreadLocalCache().closeSearchers();
			} catch (IOException e) {
	        	log.warn("Error closing searchers (indexSchedules) ",e);
			}
        }
		opTimer.end();
        final long totalTime = opTimer.snapshot().getMillisecondsTaken();
		log.debug(scheduleIterable.size() + "number of issues indexed in" + totalTime + "ms.");
        
        return true;			
  	
    }
    
    private void sendMessageToOtherNodes(List<Schedule> scheduleList, boolean skipCurrentNode) {
    	int count = 0;
		int limit = 999;
		while (count < scheduleList.size()) {
			if ((scheduleList.size() - count) < limit) {
				limit = scheduleList.size() - count;
			}
			List<Schedule> subList = scheduleList.subList(count, count + limit);
			MessageHandler messageHandler = (MessageHandler) ZephyrComponentAccessor.getInstance().getComponent("messageHandler");
			messageHandler.sendMessage(ZFJMessage.fromString(ZFJMessageType.INDEX_EXECUTION.getMessageType()), CollectionEnclosedIterable.copy(subList), null, skipCurrentNode);
			count += subList.size();
		}
    }
    
	@Override
	public boolean reIndexSchedule(EnclosedIterable<Schedule> scheduleIterable,
			final Context context) throws IndexException {
		String jobProgressToken = "";
        final OpTimer opTimer = Instrumentation.pullTimer("Execution");
        List<Schedule> scheduleList = new ArrayList<>(scheduleIterable.size());
        if (!getIndexLock())
        {
            log.error("Not able to acquire lock, hence not able to re-index: " + scheduleIterable.toString());
            scheduleIterable.foreach(scheduleList::add);
            sendMessageToOtherNodes(scheduleList, false);
            return false;
        }
        try {
        	scheduleIterable.foreach(new Consumer<Schedule>()
	        {
	            public void consume(final Schedule schedule)
	            {
	            	if(schedule != null) {
	            		List<Schedule> schedules = new ArrayList<Schedule>();
		            	boolean exists = scheduleManager.existsSchedule(schedule.getID());
		            	if(exists) {
							schedules.add(schedule);
							EnclosedIterable<Schedule> tests = CollectionEnclosedIterable.copy(schedules);
							await(scheduleIndexer.reIndexSchedules(tests, context, jobProgressToken)); //Trigger Indexing on other Nodes..
							scheduleList.add(schedule);
						}
	            	} else {
						jobProgressService.addCompletedSteps(jobProgressToken,1);
					}
	            }
	        });
        	sendMessageToOtherNodes(scheduleList, true);
        } catch(Exception e) {
        	log.error("Exception occurred while reIndexSchedule",e);
        } finally{
            releaseIndexLock();
            try {
            	ZFJSearcherCache.getThreadLocalCache().closeSearchers();
			} catch (IOException e) {
	        	log.warn("Error closing searchers (reIndexSchedule) ",e);
			}
        }

		opTimer.end();

        final long totalTime = opTimer.snapshot().getMillisecondsTaken();
        log.debug("["+scheduleIterable.size() + "] issues re-indexed in " + totalTime + "ms.");
        return true;			
	}


	@Override
	public boolean reIndexScheduleInBatch(EnclosedIterable<Schedule> scheduleIterable,
								   final Context context) throws IndexException {
		String jobProgressToken = "";
		final OpTimer opTimer = Instrumentation.pullTimer("Execution");
		if (!getIndexLock())
		{
			log.error("Not able to acquire lock, hence not able to re-index: " + scheduleIterable.toString());
			return false;
		}
		try {
			await(scheduleIndexer.reIndexSchedulesInBatch(scheduleIterable,context,jobProgressToken));
		} catch(Exception e) {
			log.error("Exception occurred while reIndexSchedule",e);
		} finally{
			releaseIndexLock();
			try {
				ZFJSearcherCache.getThreadLocalCache().closeSearchers();
			} catch (IOException e) {
				log.warn("Error closing searchers (reIndexSchedule) ",e);
			}
		}
		opTimer.end();
		final long totalTime = opTimer.snapshot().getMillisecondsTaken();
		log.debug("["+scheduleIterable.size() + "] issues re-indexed in " + totalTime + "ms.");
		return true;
	}

	
	@Override
	public long reIndexAll(final EnclosedIterable<Schedule> scheduleIterable, final Context context, String jobProgressToken, boolean isOptimized)    {
        Assertions.notNull("context", context);
        context.setName("Schedule");
        log.info("Reindexing all executions");
        final long startTime = System.currentTimeMillis();
        try
        {
            if (!indexLock.writeLock.tryLock())
            {
                return -1;
            }

			// do not timeout on reindexAll
			scheduleIndexer.indexSchedulesBatchMode(scheduleIterable, context, jobProgressToken).await();

			jobProgressService.setMessage(jobProgressToken,authContext.getI18nHelper().getText("zephyr.je.reindexed.success"));

			// optimise logic, passes 'true' for 'recreateIndex', which forces the optimize
			if(isOptimized) {
				optimize0();
			}
        } catch ( Exception e) {
			log.error("Error ReIndexing",e);
		} finally {
            indexLock.writeLock.unlock();
            try {
            	ZFJSearcherCache.getThreadLocalCache().closeSearchers();
			} catch (IOException e) {
	        	log.warn("Error closing searchers (reIndexAll) ",e);
			}
        }

        final long totalTime = (System.currentTimeMillis() - startTime);
        log.debug("ReindexAll Completed, took : " + totalTime + "ms");

        return totalTime;
    }

	@Override
	public long reIndexByProject(final EnclosedIterable<Schedule> scheduleIterable, final Context context, String jobProgressToken, boolean isOptimized)    {
		Assertions.notNull("context", context);
		context.setName("Schedule");
		log.info("Reindexing By Project all executions");
		final long startTime = System.currentTimeMillis();
		try
		{
			if (!indexLock.writeLock.tryLock()) {
				return -1;
			}

			// do not timeout on reindexAll
			scheduleIndexer.reIndexSchedulesInBatch(scheduleIterable, context, jobProgressToken).await();

			jobProgressService.setMessage(jobProgressToken,authContext.getI18nHelper().getText("zephyr.je.reindexed.success"));

			// optimise logic, passes 'true' for 'recreateIndex', which forces the optimize
			optimize0();
		}
		finally
		{
			indexLock.writeLock.unlock();
			try {
				ZFJSearcherCache.getThreadLocalCache().closeSearchers();
			} catch (IOException e) {
				log.warn("Error closing searchers (reIndexAll) ",e);
			}
		}

		final long totalTime = (System.currentTimeMillis() - startTime);
		log.debug("ReindexAll Completed, took : " + totalTime + "ms");

		return totalTime;
	}

    @Override
	public boolean deleteScheduleIndexes() throws IndexException {
		scheduleIndexer.deleteScheduleIndexes();
		return true;
	}

	
	@Override
    public boolean deleteBatchIndexByTerm(@NotNull final EnclosedIterable<String> scheduleIds,@NotNull final String term,@NotNull final Context context) {
		if (!getIndexLock()) {
            log.error("Not able to acquire lock hence not able to reindex: " + scheduleIds.size());
            return false;
        }

		//Trigger Delete Indexing on other Nodes..
        final OpTimer opTimer = Instrumentation.pullTimer("Execution");
        try {
            await(scheduleIndexer.deleteBatchIndexByTerm(scheduleIds, term, context));
        } catch(Exception e) {
        	log.error("Error occurred while executing deleteBatchIndexByTerm method.",e);
        } finally{
            releaseIndexLock();
            try {
            	ZFJSearcherCache.getThreadLocalCache().closeSearchers();
			} catch (IOException e) {
	        	log.warn("Error closing searchers (deleteBatch) ",e);
			}
        }

		opTimer.end();
        final long totalTime = opTimer.snapshot().getMillisecondsTaken();
		log.debug("[" + scheduleIds.size() + "] issues re-indexed in " + totalTime + "ms.");
        return true;	
    }


	@Override
	public boolean deleteBatchIndexByTermWithoutMessage(@NotNull final EnclosedIterable<String> scheduleIds,@NotNull final String term,@NotNull final Context context) {
		if (!getIndexLock()) {
			log.error("Not able to acquire lock hence not able to reindex: " + scheduleIds.size());
			return false;
		}

		final OpTimer opTimer = Instrumentation.pullTimer("Execution");
		try {
			await(scheduleIndexer.deleteBatchIndexByTerm(scheduleIds, term, context));
		} catch(Exception e) {
			log.error("Error occurred while executing deleteBatchIndexByTerm method.",e);
		} finally{
			releaseIndexLock();
			try {
				ZFJSearcherCache.getThreadLocalCache().closeSearchers();
			} catch (IOException e) {
				log.warn("Error closing searchers (deleteBatch) ",e);
			}
		}

		opTimer.end();
		final long totalTime = opTimer.snapshot().getMillisecondsTaken();
		log.debug("[" + scheduleIds.size() + "] issues re-indexed in " + totalTime + "ms.");
		return true;
	}
	
	@Override
	public boolean deleteScheduleIndexes(
			EnclosedIterable<Schedule> scheduleIterable, Context context)
			throws IndexException {
		String jobProgressToken = "";
        if (!getIndexLock()) {
            log.error("Could not reindex: " + scheduleIterable.toString());
            return false;
        }

        try {
			await(scheduleIndexer.deleteScheduleIndexes(scheduleIterable, Contexts.nullContext(), jobProgressToken));
        } finally{
            releaseIndexLock();
            try {
            	ZFJSearcherCache.getThreadLocalCache().closeSearchers();
			} catch (IOException e) {
	        	log.warn("Error closing searchers (deleteSchedules) ",e);
			}
        }
		return true;
	}
	
	@Override
	public TopDocs search(org.apache.lucene.search.Query query) throws SearchException, IOException, ParseException {
        TopDocs luceneMatches = null;
        final IndexSearcher entitySearcher = getRawEntitySearcher();
        try {
	       luceneMatches = getHits(query, entitySearcher);
        } catch(Exception e) {
        	log.warn("Exception",e);
        } finally {
        	try {
        		entitySearcher.close();
			} catch (IOException e) {
	        	log.warn("Error closing searcher (search) ",e);
			}        
        }
        return luceneMatches;
    }
	
    private TopDocs getHits(final org.apache.lucene.search.Query searchQuery, IndexSearcher entitySearcher) throws SearchException, IOException, ParseException
    {
        if (searchQuery == null)
        {
            return null;
        }
       return runSearch(entitySearcher, searchQuery);
    }

    	
    private TopDocs runSearch(final IndexSearcher searcher, final org.apache.lucene.search.Query query) throws IOException, ParseException
    {
        log.debug("Query to search executions: " + query.toString());
        int  maxHits = Integer.MAX_VALUE;
        TopDocs hits = searcher.search(query, maxHits);
        return hits;
    }
	

	@Override
	public IndexSearcher getRawEntitySearcher() {
		return scheduleIndexer.getScheduleIndexSearcher();
	}
	
    public IndexSearcher getScheduleSearcher()
    {
        return ZFJSearcherCache.getThreadLocalCache().retrieveZFJIndexSearcher(scheduleSearcherSupplier);
    }

	@Override
	/**
	 * Fetches count for each user, returns null if user has no execution in given project/version
	 * @param versionId
	 * @param projectId
	 * @return
	 */
	public Set<Map<String, Object>> getExecutionSummaryGroupedByUser(final Long versionId, Long projectId) {
		Set<Map<String, Object>> statusCntByUser= new LinkedHashSet<Map<String, Object>>();
		Map<String,User> executedByUsers = scheduleManager.getExecutedByValues(projectId, versionId);

		//No Point getting all the 10k users when only few are executing it
		final Set<Entry<Integer, ExecutionStatus>> statuses = JiraUtil.getExecutionStatuses().entrySet();
		final Map<String,Map<String, Object>> countByStatus = scheduleManager.getExecutionDetailsByExecutorAndStatus(statuses, versionId, projectId, executedByUsers.values());
		if(countByStatus != null && countByStatus.size() > 0) {
			executedByUsers.values().stream().forEach(user -> {
				String keyForUser = UserCompatibilityHelper.getKeyForUser(user);
				if (countByStatus.get(keyForUser) != null) {
					Map<String, Object> userMap = getExecutionSummaryMap(keyForUser, user.getDisplayName(), countByStatus.get(keyForUser), null);
					statusCntByUser.add(userMap);
				}
			});
		}
		return statusCntByUser;
	}


	@Override
	public Set<Map<String, Object>> getExecutionSummaryGroupedByCycle(
			final Long versionId, final Long projectId) {
		Set<Map<String, Object>> statusCntByCycle= new LinkedHashSet<>();
		List<Cycle> cycles = cycleManager.getCyclesByVersion(versionId, projectId, -1);
		final Set<Entry<Integer, ExecutionStatus>> statuses = JiraUtil.getExecutionStatuses().entrySet();
		final Map<Integer,Map<String, Object>> countByStatus = scheduleManager.getExecutionDetailsByCycleAndStatus(statuses, versionId, projectId, cycles);
		if(countByStatus != null && countByStatus.size() > 0) {
			cycles.stream().forEach(cycle -> {
				if (countByStatus.get(cycle.getID()) != null) {
					Map<String, Object> cycleMap = getExecutionSummaryMap(cycle.getID(), cycle.getName(), countByStatus.get(cycle.getID()), cycle.getSprintId());
					statusCntByCycle.add(cycleMap);
				}
			});
		}
		final Map<Integer,Map<String, Object>> adhocCountByStatus  = scheduleManager.getExecutionDetailsByCycleAndStatus(statuses, versionId, projectId, null);
		if(adhocCountByStatus.get(ApplicationConstants.AD_HOC_CYCLE_ID) != null) {
			I18nHelper i18n = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
			Map<String, Object> cycleMap = getExecutionSummaryMap(-1l, i18n.getText("zephyr.je.cycle.adhoc"), adhocCountByStatus.get(ApplicationConstants.AD_HOC_CYCLE_ID), null);
			statusCntByCycle.add(cycleMap);
		}
		return statusCntByCycle;
	}
	
	
	@Override
	public boolean isIndexDirectoryPresent() {
		IndexSearcher indexSearcher = scheduleIndexer.getScheduleIndexSearcher();
		try {
			String[] dirList = indexSearcher.getIndexReader().directory().listAll();
			if(dirList != null && dirList.length > 0) {
				return true;
			}
			return false;
		} catch(Exception e) {
			log.warn("No Directory exists:",e);
			return false;
		} finally {
			try {
				indexSearcher.close();
			} catch (IOException e) {
	        	log.warn("Error closing searcher",e);
			}
		}
	}	

	/**
	 * Gets Hits based on Query for Charts
	 * @param parser
	 * @param query
	 * @param countByStatus
	 * @param statusKey
	 * @return
	 */
	private Integer getHits(QueryParser parser,String query, Map<String, Object> countByStatus, String statusKey) {
		try {
	        org.apache.lucene.search.Query q = parser.parse(query);
			TopDocs topDoc = search(q);
			Integer cnt;
	        if(topDoc != null) {
				ScoreDoc[] scoreDocs = topDoc.scoreDocs;
				cnt = scoreDocs == null ? cnt = new Integer(0) : scoreDocs.length ;
				countByStatus.put(statusKey, cnt);
	        } else {
				cnt = new Integer(0);
	        }
	        countByStatus.put(statusKey, cnt);
	        return cnt;
		} catch (ParseException e) {
			log.error("Error Parsing query", e);
		} catch (IOException e) {
			log.error("IOException parsing/searching query", e);
		} catch (SearchException e) {
			log.error("SearchException searching query", e);
		}
		return 0;
	}


	/**
	 *
	 * @param id
	 * @param name
	 * @param countByStatus
	 * @param sprintId 
	 * @return
	 */
	private Map<String, Object> getExecutionSummaryMap(Object id, String name, Map<String, Object> countByStatus, Long sprintId) {
		Map<String, Object> cycleMap = new HashMap<String, Object>();
		cycleMap.put("id", id);
		cycleMap.put("name", name);
		if(sprintId != null) {
			cycleMap.put("sprintId",sprintId);
		}
		cycleMap.put("cnt", countByStatus);
		return cycleMap;
	}

	/**
	 *
	 * @param statuses
	 * @param versionId
	 * @param projectId
	 * @param cycleId
	 * @return
	 */
	private Map<String, Object> getExecutionDetailsByCycle(final Set<Entry<Integer, ExecutionStatus>> statuses, Long versionId, Long projectId, Integer cycleId) {
		Map<String, Object> countByStatus = new LinkedHashMap<String, Object>();
        int total=0;
		String[] fields = new String[] {"CYCLE_ID","VERSION_ID","PROJECT_ID","STATUS"};
		QueryParser parser = extractParser(fields);

        for(Entry<Integer, ExecutionStatus> statusEntry : statuses){
			String statusKey = statusEntry.getKey().toString();
			String query="";
			String version = String.valueOf(versionId);
			if(cycleId == null){
		        String cycleNew = "-1";
				query = "+PROJECT_ID:"+projectId +" +VERSION_ID:"+"\""+ version+ "\"" + " +CYCLE_ID:"+"\""+cycleNew + "\"" + " +STATUS:"+"\""+statusKey+ "\"";
			} else {
				String cycleNew = String.valueOf(cycleId);
			    query = "+PROJECT_ID:"+projectId +" +VERSION_ID:"+"\""+ version+ "\"" + " +CYCLE_ID:"+"\""+cycleNew + "\"" + " +STATUS:"+"\""+statusKey+ "\"";
			}
			total += getHits(parser, query, countByStatus, statusKey);
		}
		countByStatus.put("total", total);
        return countByStatus;
	}


	public Map<String, Object> getExecutionSummaryByIssueIds(Collection<Long> issueIds, Long versionId) {
		return scheduleManager.getExecutionSummaryByIssueIds(issueIds,versionId);
	}
	
	@Override
	public List<ExecutionSummaryImpl> getExecutionSummariesByIssueIds(List<Long> issueIds) {
		List<ExecutionSummaryImpl> summaryList = new ArrayList<ExecutionSummaryImpl>();
		Map<String, Object> countByStatus = new LinkedHashMap<String, Object>();
        String[] fields = new String[] {"ISSUE_ID","STATUS"};
		QueryParser parser = extractParser(fields);
		if(issueIds == null || issueIds.size() == 0 )
			return null;
		StringBuilder str = new StringBuilder();
		str.append("+(");
		for(Long issueId : issueIds) {
			str.append("ISSUE_ID:"+issueId);
			str.append(" ");
		}
		str.append(")");

		final Set<Entry<Integer, ExecutionStatus>> statuses = JiraUtil.getExecutionStatuses().entrySet();
		for(Entry<Integer, ExecutionStatus> statusEntry : statuses){
		    String query = str.toString() + " +STATUS:"+"\""+String.valueOf(statusEntry.getKey())+ "\"";
		    Integer cnt = getHits(parser, query, countByStatus, statusEntry.getKey().toString());
			if(cnt == null) cnt = new Integer(0);
			ExecutionSummaryImpl summary = new ExecutionSummaryImpl(cnt, statusEntry.getKey(),statusEntry.getValue().getName(),
					statusEntry.getValue().getDescription(),statusEntry.getValue().getColor());
			summaryList.add(summary);
		}
		return summaryList;
	}

	@Override
	public boolean reIndexSchedule(ScheduleIdsScheduleIterable schedules, Context context, String jobProgressToken) {
		final OpTimer opTimer = Instrumentation.pullTimer("Execution");
		List<Schedule> scheduleList = new ArrayList<>(schedules.size());
		if (!getIndexLock())
		{
			log.error("Not able to acquire lock, hence not able to re-index: " + schedules.toString());
			schedules.foreach(scheduleList::add);
            sendMessageToOtherNodes(scheduleList, false);
			return false;
		}
		try {
			schedules.foreach(new Consumer<Schedule>()
			{
				public void consume(final Schedule schedule)
				{
					if(schedule != null) {
						List<Schedule> schedules = new ArrayList<>();
						schedules.add(schedule);
						EnclosedIterable<Schedule> tests = CollectionEnclosedIterable.copy(schedules);
						await(scheduleIndexer.reIndexSchedules(tests, context, jobProgressToken));
						scheduleList.add(schedule);
					} else {
						jobProgressService.addCompletedSteps(jobProgressToken,1);
					}
				}
			});
			sendMessageToOtherNodes(scheduleList, true);
		} catch(Exception exception) {
			log.error("Exception occurred while reIndexSchedule",exception);
		} finally{
			releaseIndexLock();
			try {
				ZFJSearcherCache.getThreadLocalCache().closeSearchers();
			} catch (IOException ex) {
				log.warn("Error closing searchers (reIndexSchedule) ",ex);
			}
		}
		opTimer.end();

		final long totalTime = opTimer.snapshot().getMillisecondsTaken();
		log.debug("["+schedules.size() + "] issues re-indexed in " + totalTime + "ms.");
		return true;
	}
	
	@Override
	public boolean reIndexScheduleWithOutMessage(ScheduleIdsScheduleIterable schedules, Context context, String jobProgressToken) {
		final OpTimer opTimer = Instrumentation.pullTimer("Execution");
		List<Schedule> scheduleList = new ArrayList<>(schedules.size());
		if (!getIndexLock())
		{
			return false;
		}
		try {
			schedules.foreach(new Consumer<Schedule>()
			{
				public void consume(final Schedule schedule)
				{
					if(schedule != null) {
						List<Schedule> schedules = new ArrayList<>();
						schedules.add(schedule);
						EnclosedIterable<Schedule> tests = CollectionEnclosedIterable.copy(schedules);
						await(scheduleIndexer.reIndexSchedules(tests, context, jobProgressToken));
						scheduleList.add(schedule);
					} else {
						jobProgressService.addCompletedSteps(jobProgressToken,1);
					}
				}
			});
		} catch(Exception exception) {
			log.error("Exception occurred while reIndexSchedule",exception);
		} finally{
			releaseIndexLock();
			try {
				ZFJSearcherCache.getThreadLocalCache().closeSearchers();
			} catch (IOException ex) {
				log.warn("Error closing searchers (reIndexSchedule) ",ex);
			}
		}
		opTimer.end();

		final long totalTime = opTimer.snapshot().getMillisecondsTaken();
		log.debug("["+schedules.size() + "] issues re-indexed in " + totalTime + "ms.");
		return true;
	}

	@Override
	public long[] getAllScheduleDocuments() throws Exception {
		return scheduleIndexer.getAllScheduleDocuments();
	}

	@Override
	public long[] getAllTestcaseDocuments() throws Exception {
		return scheduleIndexer.getAllTestcaseDocuments();
	}

	/**
	 * Sets up the Parser for query
	 * @param fields
	 * @return
	 */
	private QueryParser extractParser(String[] fields) {
		org.apache.lucene.util.Version v = org.apache.lucene.util.Version.LUCENE_30;
        QueryParser parser 
            = new MultiFieldQueryParser(v,fields, new KeywordAnalyzer());
        parser.setLowercaseExpandedTerms(false);
        parser.setAutoGeneratePhraseQueries(false);
        parser.enable_tracing();
		return parser;
	}

	
	/**
	 * Fetches Schedules for given project/schedule/duration
	 * @param projectId
	 * @param days
	 * @param periodName
	 * @return
	 */
	public Map<Long,Map<String, Object>> getSchedulesByProjectIdWithDuration(Integer projectId, 
			String days, String periodName,boolean showExecutionsOnly) {
    	PeriodName period = ChartFactory.PeriodName.valueOf(periodName);
    	int daysInInt = 30;
    	try{
    		daysInInt = Integer.valueOf(days);
    	}
    	catch(Exception e){
    		log.info("As a default we will get data for 30 days", e);
    	}
    	
        int totalDays = DataUtils.normalizeDaysValue( daysInInt , period);
        
		Map<Object,Map<String, Object>> statusByDuration = new HashMap<Object,Map<String, Object>>();
		Map<String,Map<String, Object>> statusByDurationMap = new TreeMap<String,Map<String, Object>>();
		Map<String,Number> resultMap= new TreeMap<String,Number>();
		
		final StatisticsMapper createdMapper = new DatePeriodStatisticsMapper(ChartUtil.getTimePeriodClass(PeriodName.valueOf(periodName)), 
        		"EXECUTED_ON", timeZoneManager.getLoggedInUserTimeZone());
        final Map<RegularTimePeriod, Number> result = new TreeMap<RegularTimePeriod, Number>();
        ExecutionsByDurationCollector hitCollector = new ExecutionsByDurationCollector(createdMapper,result, 
        		getRawEntitySearcher(),statusByDuration);
        
		StringBuffer zql = new StringBuffer("project = ").append(projectId).append(" and executionDate > ").append("\"-").append(totalDays).append("d\"");
		if(showExecutionsOnly) {
//			zql = "+PROJECT_ID:"+projectId +" +EXECUTED_ON:[" + date + " TO " + currDate +"]" + " -STATUS:"+"\""+ -1 + "\"";
			zql.append(" and executionStatus != ").append(ApplicationConstants.UNSCHEDULED_VERSION_ID_AS_STRING);
		}
/*        else {
			zql = "+PROJECT_ID:"+projectId +" +EXECUTED_ON:[" + date + " TO " + currDate +"]";
		}
*/
		log.debug("ZQL for " + totalDays + " days chart " + zql.toString());
        ParseResult parseResult = searchService.parseQuery(authContext.getLoggedInUser(), zql.toString());
        try {
			search(createLuceneQuery(parseResult.getQuery(), null, authContext.getLoggedInUser(), true), hitCollector);
		} catch (IOException e) {
			log.error("Error Retrieving data from index",e);
		} catch (SearchException e) {
            log.error("Error Retrieving data from index",e);
        }

        for (Iterator<RegularTimePeriod> i$ = result.keySet().iterator(); i$.hasNext(); ) {
	    	 RegularTimePeriod key = i$.next();
	    	 Number number = result.get(key);
	    	 resultMap.put(key.toString(), number);
	    }

	    for (Iterator<Object> i$ = statusByDuration.keySet().iterator(); i$.hasNext(); ) { 
	    	 RegularTimePeriod key = (RegularTimePeriod) i$.next();
	    	 Map<String,Object> statusMap = statusByDuration.get(key);
	    	 statusByDurationMap.put(key.toString(), statusMap);
	    }

	    
        Map<Long, Map<String, Object>> completeRangeDataMap = 
        	generateCompleteStatusData(statusByDurationMap, resultMap, period, daysInInt);

		return completeRangeDataMap;
	}

    private org.apache.lucene.search.Query createLuceneQuery(com.atlassian.query.Query searchQuery, org.apache.lucene.search.Query andQuery, ApplicationUser searchUser, boolean overrideSecurity)
            throws SearchException {
        final String jqlSearchQuery = searchQuery.toString();
        org.apache.lucene.search.Query finalQuery = andQuery;

        if (searchQuery.getWhereClause() != null) {
            final QueryCreationContext context = new QueryCreationContextImpl(searchUser, overrideSecurity);
            final org.apache.lucene.search.Query query = luceneQueryBuilder.createLuceneQuery(context, searchQuery.getWhereClause());
            if (query != null) {
                log.debug("ZQL query to search executions:"+query);
                if (finalQuery != null) {
                    BooleanQuery join = new BooleanQuery();
                    join.add(finalQuery, BooleanClause.Occur.MUST);
                    join.add(query, BooleanClause.Occur.MUST);
                    finalQuery = join;
                } else {
                    finalQuery = query;
                }
            } else {
				log.info("Got a null query from the ZQL Query.");
            }
        }
        log.debug("Generated final lucene query (might have characters translated into lucene format) is::"+finalQuery);
        // NOTE: we do this because when you are searching for everything the query is null
        if (finalQuery == null) {
            finalQuery = new MatchAllDocsQuery();
        }
        log.debug("ZQL lucene query (might have characters translated into lucene format): " + finalQuery);
        return finalQuery;
    }

	/**
	 *
	 * @param projectId
	 * @param versionId
	 * @param cycleId
	 * @param periodName
	 * @param rawChartDataPointMap
	 * @param countMap
	 * @return
	 */
	public int getScheduleBurndownByPeriod(Integer projectId,
											Integer versionId,
											Integer cycleId,
											Integer sprintId, 
											String periodName,
											TreeMap<TimePeriod, Integer> rawChartDataPointMap,
											Map<String,Integer> countMap
											) {
		
    	PeriodName period = ChartFactory.PeriodName.valueOf(periodName); 
//    	int periodInt = period.ordinal();

		final StatisticsMapper<TimePeriod> periodMapper = new DatePeriodStatisticsMapper(ChartUtil.getTimePeriodClass(PeriodName.valueOf(periodName)), 
        		"EXECUTED_ON", timeZoneManager.getLoggedInUserTimeZone());
		//Initialize countMap
        countMap.put("ExecutionCount", 0);
        countMap.put("CreationCount", 0);
        
		int totalDays = 0;
		String query = "+PROJECT_ID:"+projectId +" +VERSION_ID:"+ "\"" + String.valueOf(versionId) +"\"";
		if(sprintId != null && cycleId == null) {
			query += " +SPRINT_ID:" + "\"" + String.valueOf(sprintId) +"\"";
		} else if(sprintId == null && cycleId != null) {
			query += " +CYCLE_ID:" + "\"" + String.valueOf(cycleId) +"\""; 
		} else if(sprintId != null && cycleId != null) {
			query += " +SPRINT_ID:" + "\"" + String.valueOf(sprintId) +"\"" + " +CYCLE_ID:" + "\"" + String.valueOf(cycleId) +"\""; 
		}
		log.debug("Query - " + query);
		String[] fields = new String[] {"PROJECT_ID","schedule_id"};
		QueryParser parser = extractParser(fields);
        try {
        	/*** Query Graph Start and End Dates ***/
    		ScheduleBurndownDurationHitCollector dateHitCollector = new ScheduleBurndownDurationHitCollector(getRawEntitySearcher());
			search(parser.parse(query), dateHitCollector);
			
			Date startDate = null;
			Date endDate = null;
			try{
				final Map<String,Object> dateMapper = dateHitCollector.getDates();
				startDate = (Date) dateMapper.get("START_DATE");
				endDate = (Date) dateMapper.get("END_DATE");
			}
			catch(RuntimeException re){
				re.printStackTrace();
				return totalDays;
			}
			log.debug("Start Date - " + startDate + " End Date - "+ endDate);

			/*** Find Chart Data Points between start and end dates ***/
			Days d = Days.daysBetween(new DateTime(startDate), new DateTime(endDate));
			totalDays = d.getDays();
			if(startDate != null && endDate != null){
	        	UnexecutedSchedulesCollector hitCollector = new UnexecutedSchedulesCollector(rawChartDataPointMap, getRawEntitySearcher(), periodMapper, startDate, endDate, countMap);
				search(parser.parse(query), hitCollector);
			}
		} catch (ParseException e) {
			log.error("Error Parsing Query:", e);
		} catch (IOException e) {
			log.error("Error Retrieving data from index", e);
		}
		
        //We are adding 1 to get count of day on which graph is displayed to user!.
        //This will also cover scenario when user is creating schedules first time for given cycle and also views them on same day.
        return (totalDays + 1);
	}

	/**
	 * Gets Top Defects Occuring in Max Executions for a given Project/Version
	 */
	public SortedSet<DefectSummaryModel> getTopSchedulesWithDefectsByDuration(
			Integer projectId, Integer versionId, String days, String statuses) {
        String query = "+PROJECT_ID:"+projectId +" +VERSION_ID:"+ "\"" + String.valueOf(versionId) +"\"" + buildStatusClause(statuses);
        String[] fields = new String[] {"PROJECT_ID","VERSION_ID","SCHEDULE_DEFECT_STATUS"};
		QueryParser parser = extractParser(fields);
		Map<Integer,Integer> result = new HashMap<Integer, Integer>();
		Map<Integer,List<String>> testIds = new HashMap<Integer, List<String>>();
		
		ScheduleDefectHitCollector defectCollector = new ScheduleDefectHitCollector(result,testIds,getRawEntitySearcher());
        try {
			search(parser.parse(query),defectCollector);
		} catch (ParseException e) {
			log.error("Error Parsing Query:",e);
		} catch (IOException e) {
			log.error("Error Retrieving data from index",e);
		}
        SortedSet<DefectSummaryModel> defectMap = fetchTopDefects(result,testIds,days,statuses);
		return defectMap;		
	}


	/**
	 * Retrieves Top n Defects based on days. defaulted to 10
	 * @param defectMap
	 * @param testIds
	 * @param days
	 * @param statuses
	 * @return
	 */
	private SortedSet<DefectSummaryModel> fetchTopDefects(
			Map<Integer, Integer> defectMap, Map<Integer, List<String>> testIds, String days, String statuses) {
		SortedSet<DefectSummaryModel> responseSet = new TreeSet<DefectSummaryModel>();
//		Map<Integer,String> priorityDefectMap = new TreeMap<Integer, String>();
		for(Integer defectId : defectMap.keySet()) {
			try {
				Issue issue = issueManager.getIssueObject(defectId.longValue());
				if(issue != null && issue.getStatus() != null) {
					//Indexing seems to be adding Mutiple DefectIds in a line along with its status
					String[] statusArray = StringUtils.split(statuses,"|"); //statuses.split("|");
					List<String> statusList = Arrays.asList(statusArray);
					if(statusList.contains(issue.getStatus().getId())) {
						DefectSummaryModel defectSummaryModel = IssueUtils.convertIssueToDefectSummaryModel(defectMap, testIds.get(defectId), defectId, issue);
                        responseSet.add(defectSummaryModel);
					}
				}
			} catch (Exception e) {
				log.error("Error Retrieving Issue",e);
			}
		}
		
		//Now that the data is sorted, we will grab the top n records based on input
		SortedSet<DefectSummaryModel> targetSet = new TreeSet<DefectSummaryModel>();
//		int count=0;
//		Map<DefectSummaryModel,String> defectMapNew = new HashMap<DefectSummaryModel,String>();
//		while(count < Integer.parseInt(days)) {
//			if(count < responseSet.size()) {
//				DefectSummaryModel summaryModel = (DefectSummaryModel) CollectionUtils.get(responseSet, count);
//				//targetSet.add(summaryModel);
//				for(Integer defectId : priorityDefectMap.keySet()) {
//					if(defectId.intValue() == summaryModel.getDefectId().intValue()) {
//						defectMapNew.put(summaryModel, priorityDefectMap.get(defectId));
//					}
//				}
//				count++;
//			} else {
//				targetSet.addAll(sortByValue(defectMapNew).keySet());
//				return targetSet;
//			}
//		}
		targetSet.addAll(Ordering.natural().greatestOf(responseSet, Integer.parseInt(days)));
		return targetSet;
	}

	/**
	 * Search Documents with HitCollecor
	 * @param fields
	 * @param luceneQueryString
	 * @param hitCollector
	 * @throws Exception
	 */
	public void search(String[] fields, String luceneQueryString, Collector hitCollector) throws Exception {
		QueryParser parser = extractParser(fields);
		try {
			//luceneQueryString = QueryParser.escape(luceneQueryString);
			Query luceneQuery = parser.parse(luceneQueryString);
			search(luceneQuery, hitCollector);
		} catch (Exception e) {
			log.error("Error Retrieving data from index", e);
			throw e;
		}
	}
	
	/**
	 * Complete the Data Generation for the entire range, if missing, generate a structure with 0-count and 0-executed,0-unexecuted
	 * @param statusByDurationMap
	 * @param resultMap
	 * @param period
	 * @param days
	 * @return
	 */
	private Map<Long, Map<String, Object>> generateCompleteStatusData(
			Map<String, Map<String, Object>> statusByDurationMap,
			Map<String, Number> resultMap, PeriodName period,
			int days) {
    	Map<Long, Map<String,Object>> completeDataMap = new TreeMap<Long,Map<String,Object>>();
    	
		DateTime endDate = new DateTime();
		DateTime startDate = endDate.minusDays(days);
    	
		while( startDate.isBefore(endDate) || startDate.isEqual(endDate)){
			String formattedDate = startDate.toString(DateTimeFormat.forPattern("dd-MMM-yyyy"));
			
			switch(period){
			case hourly: formattedDate = startDate.toString(DateTimeFormat.forPattern("[k,dd/M/yyyy]"));
    				break;
    		case daily: formattedDate = startDate.toString(DateTimeFormat.forPattern("dd-MMMMM-yyyy"));
    				break;
    		case monthly: formattedDate = startDate.toString(DateTimeFormat.forPattern("MMMMM yyyy"));
    				break;
    		case yearly: formattedDate = startDate.toString(DateTimeFormat.forPattern("yyyy"));
					break;
	    	}
			if(formattedDate.startsWith("0")) {
				formattedDate = StringUtils.substring(formattedDate, 1);
			}
			
			Map<String, Object> statusMap = statusByDurationMap.get(formattedDate);
	    	long timestamp = startDate.getMillis();
	    	if(statusMap != null && statusMap.size() > 0){
	    		statusMap.put("total", resultMap.get((Object)formattedDate));
	    		//Let's store the date in milliseconds format.
	    		completeDataMap.put(timestamp, statusMap);
	    	} else {
	    		statusMap = new HashMap<String, Object>();
	    		statusMap.put("executed", new Integer(0));
	    		//statusMap.put("unexecuted", new Integer(0));
	    		statusMap.put("total", new Integer(0));
	    		completeDataMap.put(timestamp, populateDefault(statusMap));
	    	}
	    	
			switch(period){
	    		case hourly: startDate = startDate.plusHours(1);
	    				break;
	    		case daily: startDate = startDate.plusDays(1);
	    				break;
	    		case monthly: startDate = startDate.plusWeeks(1);
	    				break;
	    		case yearly: startDate = startDate.plusYears(1);
						break;
	    	}
		}
    	return completeDataMap;
    }

	/**
	 * Populate Default Values for Duration which does not get any Hit and are within the range
	 * @param statusMap
	 * @return
	 */
	private Map<String, Object> populateDefault(Map<String, Object> statusMap) {
        for(ExecutionStatus executionStatus : JiraUtil.getExecutionStatuses().values()) {
        	statusMap.put(executionStatus.getId().toString(),new Integer(0));
        }
    	return statusMap;
	}

	/**
	 * Search Documents with HitCollecor
	 * @param query
	 * @param hitCollector
	 * @throws IOException
	 */
	private void search(org.apache.lucene.search.Query query, Collector hitCollector) throws IOException {
        final IndexSearcher entitySearcher = getRawEntitySearcher();
        try {
            entitySearcher.search(query, hitCollector);
        } catch(Exception e) {
        	log.warn("Exception",e);
        } finally {
        	try {
        		entitySearcher.close();
			} catch (IOException e) {
	        	log.warn("Error closing searchers",e);
			}        
        }    
   }

	
	private boolean obtain(final Awaitable waitFor) {
		try {
			if (waitFor.await(indexConfig.getIndexLockWaitTime(),
					TimeUnit.MILLISECONDS)) {
				return true;
			}
		} catch (final InterruptedException ie) {
			log.error("Wait attempt interrupted.", new IndexException(
					"Wait attempt interrupted.", ie));
			return false;
		}
		// We failed to acquire a lock after waiting the configured time
		// (default=30s), so give up.
		final String errorMessage = "Wait attempt timed out - waited "
				+ indexConfig.getIndexLockWaitTime() + " milliseconds";
		log.error(errorMessage, new IndexException(errorMessage));
		return false;
	}

	/**
	 * Optimizes the index and resets the dirtyness count. Should only be called
	 * if the index read lock is obtained.
	 * 
	 * @return optimization time in milliseconds
	 */
	@GuardedBy("index read lock")
	private long optimize0() {
        final long startTime = System.currentTimeMillis();
        // do not timeout on optimize
        scheduleIndexer.optimize().await();
        return System.currentTimeMillis() - startTime;
	}

	 private interface Awaitable
	    {
	        /**
	         * See if we can wait successfully for this thing.
	         * @param time how long to wait
	         * @param unit the unit in which time is specified
	         * @return true if the thing was obtained.
	         * @throws InterruptedException if someone hits the interrupt button
	         */
	        boolean await(long time, TimeUnit unit) throws InterruptedException;
	    }

	private String buildStatusClause(String statuses) {
		StringBuilder str = new StringBuilder();
		if(StringUtils.isNotBlank(statuses)) {
			String[] statusArray = StringUtils.split(statuses,"|"); //statuses.split("|");
			str.append(" +(");
			for(String status : statusArray) {
				str.append("SCHEDULE_DEFECT_STATUS:"+ "\"" + status+"\"");
				str.append(" ");
			}
			str.append(")");	
			return str.toString();
		}
		return str.toString();
	}	
	
	public <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		List<Map.Entry<K, V>> list =
			new LinkedList<Map.Entry<K, V>>( map.entrySet() );
		Collections.sort( list, new Comparator<Map.Entry<K, V>>()
				{
			public int compare( Map.Entry<K, V> o1, Map.Entry<K, V> o2 )
			{
				return (o1.getValue()).compareTo( o2.getValue() );
			}
				} );

		Map<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list)
		{
			result.put( entry.getKey(), entry.getValue() );
		}
		return result;
	}	
	
	private void await(final Result result)
    {
        obtain(new Awaitable()
        {
            public boolean await(final long time, final TimeUnit unit) throws InterruptedException
            {
                return result.await(time, unit);
            }
        });
    }

    private void releaseIndexLock()
    {
        indexLock.readLock.unlock();
    }
    
    /**
     * @return true if got the lock, false otherwise
     */
    boolean getIndexLock()
    {
        try {
        	if (StringUtils.isBlank(indexPathManager.getIndexRootPath()))
            {
                log.error("File path not set - not indexing");
                return false;
            }

            // Attempt to acquire read lock on index operations.
            return indexLock.readLock.tryLock();
        } catch(Exception ex) {
        	return false;
        }
    }

    /**
     * Holds the index read/write locks.
     */
    private class IndexLocks
    {
        /**
         * Internal lock. Not to be used by clients.
         */
        private final ReadWriteLock indexLock = new ReentrantReadWriteLock();

        /**
         * The index read lock. This lock needs to be acquired when updating the index (i.e. adding to it or updating
         * existing documents in the index).
         */
        @Nonnull
        final IndexLock readLock = new IndexLock(indexLock.readLock());

        /**
         * The index write lock. This lock needs to be acquired only when a "stop the world" reindex is taking place and
         * the entire index is being deleted and re-created.
         */
        @Nonnull
        final IndexLock writeLock = new IndexLock(indexLock.writeLock());
    }

    /**
     * An index lock that can be acquired using a configurable time out.
     */
    private final class IndexLock
    {
        @Nonnull
        private final Lock lock;

        private IndexLock(Lock lock)
        {
            this.lock = notNull("lock", lock);
        }

        /**
         * Tries to acquire this lock using a timeout of {@link IndexingConfiguration#getIndexLockWaitTime()}
         * milliseconds.
         *
         * @return a boolean indicating whether the lock was acquired within the timeout
         */
        public boolean tryLock()
        {
            return obtain(new Awaitable()
            {
                public boolean await(final long time, final TimeUnit unit) throws InterruptedException
                {
                    return lock.tryLock(time, unit);
                }
            });
        }

        /**
         * Unlocks this lock.
         */
        public void unlock()
        {
            lock.unlock();
        }
    }

	@Override
	public void zipScheduleDirectory(String indexTypeFileName) throws IOException {
		try {
			if (!indexLock.writeLock.tryLock())
            {
                return;
            }
			optimize0();
			String schedulePath = PathUtils.appendFileSeparator(indexPathManager.getIndexRootPath()) + "JEEntity/schedule";
	        String destZipPath = clusterProperties.getSharedHome() + indexTypeFileName ;
	        ZipDir zipDir = new ZipDir(Paths.get(schedulePath), Paths.get(destZipPath));
	        zipDir.zipDir();
		} finally {
			indexLock.writeLock.unlock();
        }
	}

	@Override
	public void unZipScheduleDirectory(String indexTypeFileName) throws IOException {
		try {
			if (!indexLock.writeLock.tryLock()) {
	            log.error("Not able to acquire lock, hence not able to sync the schedule index folder from snapshot: ");
	            return;
	        }
			scheduleIndexer.shutdown();
			scheduleIndexer.setMode(Mode.DIRECT);
			String zipFilePath = clusterProperties.getSharedHome() +  indexTypeFileName;
			File file = new File(zipFilePath);
			if(!file.exists()) {
				throw new FileNotFoundException("File doesn't exist - " + file.getName());
			}
			scheduleIndexer.updateIndexWithNewFilesFromZip(zipFilePath);
		} finally { 
			scheduleIndexer.shutdown();
			scheduleIndexer.setMode(Mode.QUEUED);
			indexLock.writeLock.unlock();
        }
	}

	@Override
	public void copyBackupfilesScheduleDirectory(String backupFileName) throws IOException {
		try {
			if (!indexLock.writeLock.tryLock()) {
				return;
			}
			optimize0();
			String schedulePath = PathUtils.appendFileSeparator(indexPathManager.getIndexRootPath())
					+ "JEEntity/schedule";
			String destZipPath = clusterProperties.getSharedHome() + backupFileName;
			Date date = new Date();
			SimpleDateFormat sm = new SimpleDateFormat("yyyy-MMM-dd-HH.mm");
			String strDate = sm.format(date);
			String destZipFilePath = destZipPath + File.separator +  ApplicationConstants.INDEX_SNAPSHOT + strDate + ApplicationConstants.ZIP_EXTENSION;
			File[] files = Paths.get(destZipPath).toFile().listFiles();
			if(files != null && files.length > 0) {
				Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
				if(files.length > 4) {
					files[4].delete();
				}
			}			
			ZipDir zipDir = new ZipDir(Paths.get(schedulePath), Paths.get(destZipFilePath));
			zipDir.zipDir();
		} finally {
			indexLock.writeLock.unlock();
		}
	}

	@Override
	public String getRecoveryPath(String backupFileName) throws IOException {
		String destZipPath = clusterProperties.getSharedHome() + backupFileName;
		Path path = Paths.get(destZipPath);
		if (!Files.exists(Paths.get(destZipPath))) {
			path = Files.createDirectories(Paths.get(destZipPath));
		}
		return path.toString();
	}	
	
	@Override
	public void isZipFilePresentInIndexBackup(String indexTypeFileName) throws FileNotFoundException {
		String zipFilePath = clusterProperties.getSharedHome() +  indexTypeFileName;
		File file = new File(zipFilePath);
		if(!file.exists()) {
			throw new FileNotFoundException("File doesn't exist - " + file.getName());
		}
	}
	
	@Override
	public void removeDuplicateSchedules(int dbCount) {
		try {
			if (!getIndexLock()) {
				return;
			}
			scheduleIndexer.shutdown();
			scheduleIndexer.setMode(Mode.DIRECT);
			scheduleIndexer.removeDuplicateSchedules(dbCount);
		} finally {
			releaseIndexLock();
			scheduleIndexer.shutdown();
			scheduleIndexer.setMode(Mode.QUEUED);
		}
    }
}