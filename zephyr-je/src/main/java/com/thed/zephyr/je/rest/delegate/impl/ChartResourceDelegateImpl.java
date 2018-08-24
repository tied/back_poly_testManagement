/**
 * 
 */
package com.thed.zephyr.je.rest.delegate.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.charts.ChartFactory;
import com.atlassian.jira.charts.ChartFactory.PeriodName;
import com.atlassian.jira.charts.jfreechart.util.ChartUtil;
import com.atlassian.jira.charts.util.DataUtils;
import com.atlassian.jira.issue.index.DocumentConstants;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.search.SearchProviderFactory;
import com.atlassian.jira.issue.statistics.DatePeriodStatisticsMapper;
import com.atlassian.jira.issue.statistics.StatisticsMapper;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.timezone.TimeZoneManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.query.Query;
import com.thed.zephyr.je.index.ScheduleIndexer;
import com.thed.zephyr.je.model.*;
import com.thed.zephyr.je.rest.delegate.ChartResourceDelegate;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.ZephyrCacheControl;
import com.thed.zephyr.util.collector.IssueIdsCollector;
import com.thed.zephyr.util.collector.OneDimensionalObjectHitCollector;
import com.thed.zephyr.util.collector.ProjectIdsCollector;
import org.apache.log4j.Logger;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.jfree.data.time.RegularTimePeriod;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;

/**
 * @author niravshah
 *
 */
public class ChartResourceDelegateImpl implements ChartResourceDelegate {
    protected final Logger log = Logger.getLogger(ChartResourceDelegateImpl.class);
    private final JiraAuthenticationContext authContext;
    private final SearchProvider searchProvider;
	private final ScheduleIndexer scheduleIndexer;
    private final TimeZoneManager timeZoneManager;
	private final ActiveObjects activeObjects;

    public ChartResourceDelegateImpl(final JiraAuthenticationContext authContext,
									 final SearchProvider searchProvider,
									 ScheduleIndexer scheduleIndexer, final TimeZoneManager timeZoneManager, ActiveObjects activeObjects) {
    	this.authContext=authContext;
    	this.searchProvider=searchProvider;
		this.scheduleIndexer = scheduleIndexer;
		this.timeZoneManager=timeZoneManager;
		this.activeObjects = activeObjects;
	}

	@Override
	public Response generateTestsCreatedData(HttpServletRequest request,
			String projectKey, String days, String periodName) {
    	PeriodName period = ChartFactory.PeriodName.valueOf(periodName);    	
    	int daysInInt = 30;
    	try{
    		daysInInt = Integer.valueOf(days);
    	}
    	catch(Exception e){
    		log.info("As a default we will get data for 30 days",e );
    	}
    	
        int totalDays = DataUtils.normalizeDaysValue( daysInInt , period);

		JqlClauseBuilder builder = JqlQueryBuilder.newClauseBuilder();
		builder = builder.project(projectKey);
		
		Query query = builder.buildQuery();

		JqlQueryBuilder jqlQueryBuilder = JqlQueryBuilder.newBuilder(query);
        JqlClauseBuilder whereClauseBuilder = jqlQueryBuilder.where().defaultAnd();
        whereClauseBuilder.createdAfter("-" + totalDays + "d");
        whereClauseBuilder.defaultAnd().issueType(JiraUtil.getTestcaseIssueTypeId());
        
		log.debug("Query is - " + whereClauseBuilder.buildQuery());
	
		Map<RegularTimePeriod, Number> createdIssuesMap = new TreeMap<RegularTimePeriod, Number>();
		Map<String,Number> issuesMap = new TreeMap<String,Number>();
		int createdIssuesCount = 0;
        try {
        	
			createdIssuesMap = getCreatedIssues(whereClauseBuilder.buildQuery(), authContext.getLoggedInUser(), period);
			createdIssuesCount = DataUtils.getTotalNumber(createdIssuesMap);
			
		    for (Iterator<RegularTimePeriod> i$ = createdIssuesMap.keySet().iterator(); i$.hasNext(); ) { 
		    	 RegularTimePeriod key = i$.next();
		    	 Number number = createdIssuesMap.get(key);
		    	 issuesMap.put(key.toString(), number);
		    	 //System.out.println(key + " - " + createdIssuesMap.get(key));
		    }
		} catch (SearchException e) {
			log.error("Error Retrieving Data from Index:",e);
		} catch (IOException e) {
			log.error("IO Error Retrieving Data from Index:",e);
		}

    	JSONObject ob = new JSONObject();
        Map<String, Number>completeRangeDataMap = generateCreatedIssuesChartData(issuesMap, period, daysInInt);
    	
    	try{
    		ob.put("TestsCreationMap", completeRangeDataMap);
    		ob.put("TestsCreationCount", createdIssuesCount);
    		ob.put("TestsCreationPeriod", daysInInt);
    	}
    	catch(JSONException je) {
			log.error("Error forming JSON Response :",je);
    	}
	    return Response.ok(ob.toString()).cacheControl(ZephyrCacheControl.never()).build();
	}

	@Override
	public Map<String, Integer> getStats(HttpServletRequest request) {
		Map<String, Integer> results = new LinkedHashMap<>();
		JqlClauseBuilder builder = JqlQueryBuilder.newClauseBuilder();
		builder.issueType(JiraUtil.getTestcaseIssueTypeId());
		Query query = builder.buildQuery();
		SearchProviderFactory searchProviderFactory = ComponentManager.getComponentInstanceOfType(SearchProviderFactory.class);
		IndexSearcher searcher = searchProviderFactory.getSearcher(SearchProviderFactory.ISSUE_INDEX);
		log.debug("Query to search issues ids from saved search: " + query.getQueryString());
		ProjectIdsCollector collProj = new ProjectIdsCollector(searcher);
		IssueIdsCollector collIss = new IssueIdsCollector(searcher);
		try {
			searchProvider.search(query, authContext.getLoggedInUser(), collProj);
			Set<Long> projects = null;
			if(collProj.getValue() != null) {
				projects = (Set<Long>) collProj.getValue();
				results.put(ApplicationConstants.PROJECTS, projects.size());
			}

			Schedule[] schedules =  activeObjects.find(Schedule.class,
					net.java.ao.Query.select());
			Set<Long> versionCounts = new HashSet<>();
			for(Schedule schedule: schedules){
				versionCounts.add(schedule.getVersionId());
			}
			results.put(ApplicationConstants.VERSIONS,versionCounts.size());

			Integer cycleCount =  activeObjects.count(Cycle.class,
					net.java.ao.Query.select());
			results.put(ApplicationConstants.CYCLES,cycleCount);

			Integer schedDefects =  activeObjects.count(ScheduleDefect.class,
					net.java.ao.Query.select());
			Integer stepDefects =  activeObjects.count(StepDefect.class,
					net.java.ao.Query.select());
			results.put(ApplicationConstants.DEFECTS,schedDefects+stepDefects);

			searchProvider.search(query, authContext.getLoggedInUser(), collIss);
			if(collIss.getValue() != null) {
				Set<Long> issues = (Set<Long>) collIss.getValue();
				results.put(ApplicationConstants.TESTCASES, issues.size());
			}

			Integer executions =  activeObjects.count(Schedule.class,
					net.java.ao.Query.select());
			results.put(ApplicationConstants.EXECUTIONS, executions);

			Integer attachments =  activeObjects.count(Attachment.class,
					net.java.ao.Query.select());
			results.put(ApplicationConstants.ATTACHMENTS, attachments);

		}catch (Exception e){
			e.printStackTrace();
		}
		return results;
	}

	/**
     * Searches and Fetches Created Issues for a Given Duration/Term
     * @param query
     * @param remoteUser
     * @param periodName
     * @return
     * @throws IOException
     * @throws SearchException
     */
    private Map<RegularTimePeriod, Number> getCreatedIssues(Query query, ApplicationUser remoteUser, final ChartFactory.PeriodName periodName)
            throws IOException, SearchException{

        final StatisticsMapper createdMapper = new DatePeriodStatisticsMapper(ChartUtil.getTimePeriodClass(periodName), DocumentConstants.ISSUE_CREATED, timeZoneManager.getLoggedInUserTimeZone());
        final Map<RegularTimePeriod, Number> result = new TreeMap<RegularTimePeriod, Number>();
        //Collector hitCollector = new OneDimensionalObjectHitCollector(createdMapper, result);
        Collector hitCollector = new OneDimensionalObjectHitCollector(createdMapper, result);
        searchProvider.search(query, remoteUser, hitCollector);
        return result;
    }
    
    /**
     * Generates Created Issue Data For Charts
     * @param issuesMap
     * @param period
     * @param days
     * @return
     */
    private Map<String,Number> generateCreatedIssuesChartData(Map<String,Number> issuesMap, PeriodName period, int days){
    	Map<String, Number> completeDataMap = new TreeMap<String,Number>();
    	
		DateTime endDate = new DateTime();
		DateTime startDate = endDate.minusDays(days);
		String formattedDate = null;
		while( startDate.isBefore(endDate) || startDate.isEqual(endDate)){

	    	switch(period){
    		case hourly: formattedDate = startDate.toString(DateTimeFormat.forPattern("[k,dd/M/yyyy]"));
    				break;
    		case daily: formattedDate = startDate.toString(DateTimeFormat.forPattern("d-MMMMM-yyyy"));
    				break;
    		case monthly: formattedDate = startDate.toString(DateTimeFormat.forPattern("MMMMM yyyy"));
    				break;
    		case yearly: formattedDate = startDate.toString(DateTimeFormat.forPattern("yyyy"));
					break;
	    	}
	    	
	    	Number number = issuesMap.get(formattedDate);
	    	long timestamp = startDate.getMillis();
	    	if(number != null){
	    		//Stores the formatted date
	    		//Let's store the date in milliseconds format.
	    		completeDataMap.put(String.valueOf(timestamp), number);
	    	}
	    	else{
	    		completeDataMap.put(String.valueOf(timestamp), 0);
	    	}
	    	
			switch(period){
	    		case hourly: startDate = startDate.plusHours(1);
	    				break;
	    		case daily: startDate = startDate.plusDays(1);
	    				break;
	    		case monthly: startDate = startDate.plusMonths(1);
	    				break;
	    		case yearly: startDate = startDate.plusYears(1);
						break;
	    	}
		}
    	return completeDataMap;
    }
}

