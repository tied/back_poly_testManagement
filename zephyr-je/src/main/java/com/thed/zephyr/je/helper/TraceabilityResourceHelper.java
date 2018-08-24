package com.thed.zephyr.je.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.thed.zephyr.je.config.model.ExecutionStatus;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.model.ScheduleDefect;
import com.thed.zephyr.je.model.StepDefect;
import com.thed.zephyr.je.service.ScheduleManager;
import com.thed.zephyr.je.service.StepResultManager;
import com.thed.zephyr.util.JiraUtil;

public class TraceabilityResourceHelper {
    protected final Logger log = Logger.getLogger(TraceabilityResourceHelper.class);

	private final ScheduleManager scheduleManager;
	private final StepResultManager stepResultManager;
	private final IssueManager issueManager;
	private final JiraAuthenticationContext authContext;
	private final SearchProvider searchProvider;

	public TraceabilityResourceHelper(final ScheduleManager scheduleManager, 
			final StepResultManager stepResultManager,final IssueManager issueManager, 
			final JiraAuthenticationContext authContext, final SearchProvider searchProvider) {
		this.scheduleManager = scheduleManager;
		this.stepResultManager=stepResultManager;
		this.issueManager=issueManager;
		this.authContext = authContext;
		this.searchProvider = searchProvider;
	}


	public Map<String, Object> getStatisticsForIssue (Long issueId, Long versionId) {
		Map<String, Object> response = new TreeMap<String, Object>();
		Map<String, Object> executionStat = new TreeMap<String, Object>();
		Map<String, Object> statuses = new TreeMap<String, Object>();
		List<Schedule> schedules;
		Integer offset = 0;
		Integer maxResult = 10;
		Integer totalCount;
		Set<Integer> uniqueDefectList = new TreeSet<Integer>();
		List<Map<String, String>> scheduleDefectList = new ArrayList<Map<String,String>>();
		do {
			schedules = scheduleManager.getSchedulesByIssueId(issueId.intValue(), offset, maxResult);
			for (Schedule schedule : schedules) {
				ExecutionStatus executionStatus = JiraUtil.getExecutionStatuses().get(Integer.valueOf(schedule.getStatus()));
				if (statuses.get(executionStatus.getName()) != null) {
					Integer previousCount = (Integer) statuses.get(executionStatus.getName());
					previousCount++;
					statuses.put(executionStatus.getName(), previousCount);
				} else {
					statuses.put(executionStatus.getName(), 1);
				}
                getUniqueDefectCount(schedule, uniqueDefectList,scheduleDefectList);
			}
			totalCount = scheduleManager.getSchedulesCountByIssueId(issueId.intValue());
			offset = offset + maxResult;
		} while (offset < totalCount);
		executionStat.put("total", totalCount);
		List<Map<String, Object>> statusList = new ArrayList<Map<String,Object>>();
		for(Map.Entry<String, Object> entry:statuses.entrySet()) {
			Map<String, Object> statusMap = new TreeMap<String, Object>();
			statusMap.put("status", entry.getKey());
			statusMap.put("count", entry.getValue());
			statusList.add(statusMap);
		}
		executionStat.put("statuses", statusList);
		Map<String,Object> defectStat = new TreeMap<String, Object>();
		defectStat.put("total", uniqueDefectList.size());
		response.put("defectStat", defectStat);
		response.put("executionStat", executionStat);
		response.put("defectList", uniqueDefectList);
		response.put("defects", scheduleDefectList);
		return response;
	}


	private void getUniqueDefectCount (Schedule schedule, Set<Integer> uniqueDefectList, List<Map<String, String>> scheduleDefectList) {
		List<ScheduleDefect> associatedDefects = scheduleManager.getAssociatedDefects(schedule.getID());
		for (ScheduleDefect defect: associatedDefects) {
			uniqueDefectList.add(defect.getDefectId());
		}
		
		ScheduleResourceHelper scheduleHelper = new ScheduleResourceHelper(issueManager);
    	List<Map<String, String>> scheduleDefects = scheduleHelper.convertScheduleDefectToMap(associatedDefects);
    	scheduleDefectList.addAll(scheduleDefects);

		List<StepDefect> associatedStepDefects = stepResultManager.getStepResultsWithDefectBySchedule(schedule.getID());
		List<StepDefect> uniqueStepDefectsList = new ArrayList<StepDefect>();
		for (StepDefect defect: associatedStepDefects) {
			if(!uniqueDefectList.contains(defect.getDefectId())) {
				uniqueStepDefectsList.add(defect);
			}
			uniqueDefectList.add(defect.getDefectId());
		}
		StepResultResourceHelper stepResultResourceHelper = new StepResultResourceHelper(issueManager);
    	List<Map<String, String>> stepDefects = stepResultResourceHelper.convertScheduleDefectToMap(uniqueStepDefectsList,new ArrayList<String>());
    	scheduleDefectList.addAll(stepDefects);
	}


	public void createTestReqSet (String defectId, Schedule schedule, Map<String,Object> execTestReqSet) {
		try {
			ScheduleResourceHelper scheduleHelper = new ScheduleResourceHelper(issueManager);
			Issue defect = issueManager.getIssueObject(Long.valueOf(defectId));
			IssueType defectType = defect.getIssueTypeObject();
			Issue test = issueManager.getIssueObject(Long.valueOf(schedule.getIssueId()));
					execTestReqSet.put("test", scheduleHelper.convertIssueToMap(test));
    		List<String> excludedDefectTypes = new ArrayList<String>();
    		excludedDefectTypes.add(JiraUtil.getTestcaseIssueTypeId());
	        if (defectType != null && StringUtils.isNotBlank(defectType.getId())) {
	        	excludedDefectTypes.add(defectType.getId());
	        }
			
			JqlClauseBuilder jqlClauseBuilder = JqlQueryBuilder.newClauseBuilder();
    		jqlClauseBuilder.issue().in().functionLinkedIssues(String.valueOf(test.getId()));
    		jqlClauseBuilder.and().issueType().notIn(excludedDefectTypes.toArray(new String[excludedDefectTypes.size()]));
    		Query query = jqlClauseBuilder.buildQuery();
            SearchResults searchResults = searchProvider.search(query, authContext.getLoggedInUser(), new PagerFilter().getUnlimitedFilter());
            List<Map<String,Object>> requirementListMap = new ArrayList<Map<String,Object>>();
            if(searchResults != null) {
				for (Issue requirement:searchResults.getIssues()) {
						requirementListMap.add(scheduleHelper.convertIssueToMap(requirement));
				}
			}
			execTestReqSet.put("requirement", requirementListMap);
		} catch (Exception exception) {
			log.error("Error retrieving issue, this issue probably doesn't exist.", exception);
		}
	}
	
	
	   /**
     * Finds LinkedIssues using Jql
     * @param issueIdOrKey
     * @return
     * @throws SearchException
     */
	@SuppressWarnings("rawtypes")
	public List<Issue> findLinkedTestsWithIssue(String issueIdOrKey)
			throws SearchException {
        List<Issue> issues = new ArrayList<Issue>();
        Integer maxResult = 200;
        Integer offset = 0;
        Integer totalCount = 0;
        do {
			JqlClauseBuilder jqlClauseBuilder = JqlQueryBuilder.newClauseBuilder();
			jqlClauseBuilder.issue().in().functionLinkedIssues(issueIdOrKey);
			jqlClauseBuilder.and().issueType().eq(JiraUtil.getTestcaseIssueTypeId());
			Query query = jqlClauseBuilder.buildQuery();
			PagerFilter pageFilter = new PagerFilter(offset, maxResult);
			SearchResults searchResults = searchProvider.search(query, authContext.getLoggedInUser(), pageFilter);
			if(searchResults != null) {
	            issues.addAll(searchResults.getIssues());
	            totalCount = searchResults.getTotal();
	            offset += maxResult;
			} else {
				totalCount = offset;
			}
        } while (offset < totalCount);
		return issues;
	}

	/**
	 * Finds linkedIssues with Test using Jql
	 * @param testIdOrKey
	 * @param excludeIssueTypeId
	 * @return
	 * @throws SearchException
	 */
	@SuppressWarnings("rawtypes")
	public List<Issue> findLinkedIssuesWithTest(String testIdOrKey, String excludeIssueTypeId)
			throws SearchException {
		List<String> excludeIssueTypes = new ArrayList<String>();
		excludeIssueTypes.add(JiraUtil.getTestcaseIssueTypeId());
        if (StringUtils.isNotBlank(excludeIssueTypeId)) {
    		excludeIssueTypes.add(excludeIssueTypeId);
        }
        List<Issue> issues = new ArrayList<Issue>();
        Integer maxResult = 200;
        Integer offset = 0;
        Integer totalCount = 0;
        do {
			JqlClauseBuilder jqlClauseBuilder = JqlQueryBuilder.newClauseBuilder();
			jqlClauseBuilder.issue().in().functionLinkedIssues(testIdOrKey);
			jqlClauseBuilder.and().issueType().notIn(excludeIssueTypes.toArray(new String[excludeIssueTypes.size()]));
			Query query = jqlClauseBuilder.buildQuery();
			PagerFilter pageFilter = new PagerFilter(offset, maxResult);
			SearchResults searchResults = searchProvider.search(query, authContext.getLoggedInUser(), pageFilter);
			if(searchResults != null) {
	            issues.addAll(searchResults.getIssues());
	            totalCount = searchResults.getTotal();
	            offset += maxResult;
			} else {
				totalCount = offset;
			}
	    } while (offset < totalCount);
		return issues;
	}
}
