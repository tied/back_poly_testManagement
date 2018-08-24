package com.thed.zephyr.je.index;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.jfree.data.time.TimePeriod;

import com.atlassian.jira.issue.index.IndexException;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.task.context.Context;
import com.atlassian.jira.util.NotNull;
import com.atlassian.jira.util.collect.EnclosedIterable;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.vo.ExecutionSummaryImpl;

public interface ScheduleIndexManager {
	boolean indexSchedules(@NotNull EnclosedIterable<Schedule> entities, @NotNull Context context);
    /**
     * Reindex a list of JE Entities, passing an optional event that will be set progress
     * 
     * @param entityIterable JEEntityIterable
     * @param context used to report progress back to the user or to the logs. Must not be null.
     * @return boolean if Index Successful.
     */
    //Handles both New/Update
    boolean reIndexSchedule(EnclosedIterable<Schedule> scheduleIterable, Context context) throws IndexException;

    //Bulk reindexing
	long reIndexAll(EnclosedIterable<Schedule> schedule, Context context, String jobProgressToken, boolean isOptimized);

	long reIndexByProject(final EnclosedIterable<Schedule> scheduleIterable, final Context context, String jobProgressToken, boolean isOptimized);

	boolean deleteBatchIndexByTermWithoutMessage(@NotNull EnclosedIterable<String> scheduleIds, @NotNull String term, @NotNull Context context);

	//Handles Delete
    boolean deleteScheduleIndexes(EnclosedIterable<Schedule> scheduleIterable, Context context) throws IndexException;

	//Handles Delete
	boolean deleteScheduleIndexes() throws IndexException;

    //Get Searcher
    IndexSearcher getRawEntitySearcher();
    
    IndexSearcher getScheduleSearcher();
    
    //Search
    TopDocs search(final Query query) throws SearchException, IOException, ParseException;

    //Execution Summary by User
	Set<Map<String, Object>> getExecutionSummaryGroupedByUser(Long versionId,
			Long projectId);

	//Execution Summary by Cycle
	Set<Map<String, Object>> getExecutionSummaryGroupedByCycle(Long versionId,
			Long projectId);

	//Execution Summary by IssueIds
	Map<String, Object> getExecutionSummaryByIssueIds(Collection<Long> issueIds, Long versionId);

	/**
	 * Gets remaining executions per day
	 * @param projectId
	 * @param versionId
	 * @param cycleId
	 * @param sprintId
	 * @param periodName
	 * @param rawChartDataPointMap
	 * @param countMap
	 * @return Effective No of days for which schedule burndown is calculated 
	 */
	int getScheduleBurndownByPeriod(Integer projectId, Integer versionId, Integer cycleId, Integer sprintId, String periodName, 
																TreeMap<TimePeriod, Integer> rawChartDataPointMap, Map<String,Integer> countMap);
	
	
	Map<Long, Map<String, Object>> getSchedulesByProjectIdWithDuration(
			Integer projectId,String days, String periodName, boolean showExecutionsOnly);

	SortedSet<DefectSummaryModel> getTopSchedulesWithDefectsByDuration(
			Integer projectId, Integer versionId, String days, String statuses);
	
	void search(String[] fields, String luceneQueryString, Collector hitCollector) throws Exception;

    boolean deleteBatchIndexByTerm(final EnclosedIterable<String> entityIds, final String term,final Context context);

	boolean isIndexDirectoryPresent();
	
	List<ExecutionSummaryImpl> getExecutionSummariesByIssueIds(List<Long> issueIds);

    boolean reIndexSchedule(ScheduleIdsScheduleIterable schedules, Context context, String jobProgressToken);

	boolean reIndexScheduleInBatch(EnclosedIterable<Schedule> enclosedSchedules, Context context) throws IndexException;

	/**
	 * Method zip the schedule folder from local and dump it into shared file system path.
	 * Once customer invokes reindex all and once it is completed in any one machine, then it zip the schedule folder
	 * so that it will used for extracting and dumping into other machines in the cluster.
	 */
	void zipScheduleDirectory(String indexTypeFileName) throws IOException;
	    
	/**
	 * Method unzip the schedule folder present in shared file system path to local.
	 */
	void unZipScheduleDirectory(String indexTypeFileName) throws IOException;

	long[] getAllScheduleDocuments() throws Exception;

	long[] getAllTestcaseDocuments() throws Exception;

	void copyBackupfilesScheduleDirectory(String backupFileName) throws IOException;
	
	String getRecoveryPath(String backupFileName) throws IOException;
	
	void isZipFilePresentInIndexBackup(String indexTypeFileName) throws FileNotFoundException;
	
	boolean reIndexScheduleWithOutMessage(ScheduleIdsScheduleIterable schedules, Context context, String jobProgressToken);
	
	void removeDuplicateSchedules(int dbCount);
}
