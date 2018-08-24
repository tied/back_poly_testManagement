package com.thed.zephyr.je.helper;

import com.atlassian.core.util.DateUtils.Duration;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.exception.CreateException;
import com.atlassian.jira.exception.UpdateException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.index.IndexException;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.RemoteIssueLink;
import com.atlassian.jira.issue.link.RemoteIssueLinkManager;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.task.context.Context;
import com.atlassian.jira.task.context.Contexts;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.Function;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.collect.CollectionUtil;
import com.atlassian.jira.util.collect.EnclosedIterable;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.atlassian.query.operator.Operator;
import com.google.common.base.Optional;
import com.thed.zephyr.je.attachment.SessionKeys;
import com.thed.zephyr.je.config.model.ExecutionStatus;
import com.thed.zephyr.je.index.DefectSummaryModel;
import com.thed.zephyr.je.index.ScheduleIdsScheduleIterable;
import com.thed.zephyr.je.index.ScheduleIndexManager;
import com.thed.zephyr.je.index.cluster.MessageHandler;
import com.thed.zephyr.je.index.cluster.ZFJClusterMessage;
import com.thed.zephyr.je.index.cluster.ZFJMessage;
import com.thed.zephyr.je.index.cluster.ZFJMessage.ZFJMessageType;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.model.ScheduleDefect;
import com.thed.zephyr.je.model.StepDefect;
import com.thed.zephyr.je.permissions.model.PermissionType;
import com.thed.zephyr.je.permissions.service.ZephyrPermissionManager;
import com.thed.zephyr.je.service.*;
import com.thed.zephyr.je.vo.*;
import com.thed.zephyr.je.zql.core.SearchService;
import com.thed.zephyr.je.zql.core.ZephyrClauseHandlerFactory;
import com.thed.zephyr.util.*;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jfree.data.time.TimePeriod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

public class ScheduleResourceHelper {
    protected final Logger log = Logger.getLogger(ScheduleResourceHelper.class);
    public static final String PROJECT_KEY = "projectKey";
    public static final String DAYS_NAME = "daysPrevious";
    public static final String PERIOD_NAME = "periodName";
    public static final String HOW_MANY = "howMany";

	private ScheduleIndexManager scheduleIndexManager;
    private IssueManager issueManager;
    private SearchProvider searchProvider;
    private JiraAuthenticationContext authContext;
    private ScheduleManager scheduleManager;
    private CycleManager cycleManager;
    private VersionManager versionManager;
    private SearchService searchService;
    private ZephyrClauseHandlerFactory zephyrClauseHandlerFactory;
    private ZFJCacheService zfjCacheService;
    private ZephyrSprintService sprintService;
	private RemoteIssueLinkManager rilManager;
    private StepResultManager stepResultManager;
    private ZephyrCustomFieldManager zephyrCustomFieldManager;
    
	private static final int MAX_RECORDS_ALLOWED = 1000;

    
	public ScheduleResourceHelper() {
	}

	public ScheduleResourceHelper(ScheduleIndexManager scheduleIndexManager) {
		this.scheduleIndexManager=scheduleIndexManager;
	}
	
    public ScheduleResourceHelper(IssueManager issueManager) {
        this.issueManager=issueManager;
    }

	public ScheduleResourceHelper(IssueManager issueManager, RemoteIssueLinkManager rilManager, ScheduleManager scheduleManager,StepResultManager stepResultManager) {
		this.issueManager=issueManager;
		this.rilManager = rilManager;
		this.scheduleManager = scheduleManager;
        this.stepResultManager = stepResultManager;
		this.authContext = ComponentAccessor.getJiraAuthenticationContext();
	}

    public ScheduleResourceHelper(IssueManager issueManager, RemoteIssueLinkManager rilManager, ScheduleManager scheduleManager,StepResultManager stepResultManager,
                                  SearchProvider searchProvider) {
        this.issueManager=issueManager;
        this.rilManager = rilManager;
        this.scheduleManager = scheduleManager;
        this.stepResultManager = stepResultManager;
        this.authContext = ComponentAccessor.getJiraAuthenticationContext();
        this.searchProvider = searchProvider;
    }

	public ScheduleResourceHelper(IssueManager issueManager,SearchProvider searchProvider,JiraAuthenticationContext authContext,
    		ScheduleManager scheduleManager,ScheduleIndexManager scheduleIndexManager,CycleManager cycleManager,VersionManager versionManager,
    		SearchService searchService,ZephyrClauseHandlerFactory zephyrClauseHandlerFactory,ZFJCacheService zfjCacheService,ZephyrCustomFieldManager zephyrCustomFieldManager) {
        this.issueManager=issueManager;
        this.searchProvider=searchProvider;
        this.authContext=authContext;
        this.scheduleManager = scheduleManager;
		this.scheduleIndexManager=scheduleIndexManager;
		this.cycleManager=cycleManager;
		this.versionManager=versionManager;
		this.searchService=searchService;
		this.zephyrClauseHandlerFactory=zephyrClauseHandlerFactory;
		this.zfjCacheService=zfjCacheService;
		this.zephyrCustomFieldManager = zephyrCustomFieldManager;
    }
    
    public ScheduleResourceHelper(IssueManager issueManager,SearchProvider searchProvider,JiraAuthenticationContext authContext,
    		ScheduleManager scheduleManager,ScheduleIndexManager scheduleIndexManager,CycleManager cycleManager,VersionManager versionManager,
    		SearchService searchService,ZephyrClauseHandlerFactory zephyrClauseHandlerFactory,ZFJCacheService zfjCacheService,ZephyrSprintService sprintService
    		, ZephyrCustomFieldManager zephyrCustomFieldManager) {
        this.issueManager=issueManager;
        this.searchProvider=searchProvider;
        this.authContext=authContext;
        this.scheduleManager = scheduleManager;
		this.scheduleIndexManager=scheduleIndexManager;
		this.cycleManager=cycleManager;
		this.versionManager=versionManager;
		this.searchService=searchService;
		this.zephyrClauseHandlerFactory=zephyrClauseHandlerFactory;
		this.zfjCacheService=zfjCacheService;
		this.sprintService=sprintService;
		this.zephyrCustomFieldManager = zephyrCustomFieldManager;
    }

	/**
	 * Reindexes all Schedules
	 * @param scheduleIds
	 * @param nullContext
	 * @param isSyncOnly
     * @return timeElapsed in seconds
	 */
	public long reIndexAll(
            ScheduleIdsScheduleIterable scheduleIds,
            Context nullContext, String jobProgressToken) {
		long result =  scheduleIndexManager.reIndexAll(scheduleIds, nullContext, jobProgressToken,true);
		return result;
	}

	//Trigger Indexing on other Nodes..
	public void sendMessageToOtherNodes(boolean isSyncOnly) {
		MessageHandler messageHandler = (MessageHandler) ZephyrComponentAccessor.getInstance().getComponent("messageHandler");
		if(!isSyncOnly) {
			messageHandler.sendMessage(ZFJMessage.fromString(ZFJMessageType.RE_INDEX_ALL.getMessageType()), null, null, true);
		} else {
			messageHandler.sendMessage(ZFJMessage.fromString(ZFJMessageType.SYNC_INDEX_ALL.getMessageType()), null, null, true);
		}
	}

	//Trigger Indexing on other Nodes..
	public void sendReIndexByProjectMessageToOtherNodes(List<String> projectIds, boolean isSyncOnly) {
		MessageHandler messageHandler = (MessageHandler) ZephyrComponentAccessor.getInstance().getComponent("messageHandler");
		if(!isSyncOnly) {
			messageHandler.sendMessage(ZFJMessage.fromString(ZFJMessageType.REINDEX_BY_PROJECT.getMessageType()), null, projectIds, true);
		} else {
			messageHandler.sendMessage(ZFJMessage.fromString(ZFJMessageType.SYNC_INDEX_BY_PROJECT.getMessageType()), null, projectIds, true);
		}
	}
	
	public ZFJClusterMessage addReindexAllOrSyncIndexAllCurrentNodeMessage(String status, boolean isSyncOnly) {
		MessageHandler messageHandler = (MessageHandler) ZephyrComponentAccessor.getInstance().getComponent("messageHandler");
		if(!isSyncOnly) {
			return messageHandler.addCurrentNodeindexMessage(ZFJMessageType.RE_INDEX_ALL.getMessageType(), status);
		} else {
			return messageHandler.addCurrentNodeindexMessage(ZFJMessageType.SYNC_INDEX_ALL.getMessageType(), status);
		}
	}
	
	public ZFJClusterMessage addReindexByPrjOrSyncIndexByPrjCurrentNodeMessage(String status, boolean isSyncOnly) {
		MessageHandler messageHandler = (MessageHandler) ZephyrComponentAccessor.getInstance().getComponent("messageHandler");
		if(!isSyncOnly) {
			return messageHandler.addCurrentNodeindexMessage(ZFJMessageType.REINDEX_BY_PROJECT.getMessageType(), status);
		} else {
			return messageHandler.addCurrentNodeindexMessage(ZFJMessageType.SYNC_INDEX_BY_PROJECT.getMessageType(), status);
		}
	}
	
	public void updateMessageForCurrentNode(ZFJClusterMessage zfjClusterMessage) {
		if(Objects.nonNull(zfjClusterMessage)) {
			MessageHandler messageHandler = (MessageHandler) ZephyrComponentAccessor.getInstance().getComponent("messageHandler");
			messageHandler.updateMessageForCurrentNode(zfjClusterMessage);
		}
	}

	/**
	 * Index Schedules
	 * @param schedules
	 * @param nullContext
	 * @return timeElapsed in seconds
	 * @throws IndexException 
	 */
	public boolean reIndexSchedule(
			EnclosedIterable<Schedule> schedules,
			com.atlassian.jira.task.context.Context nullContext) throws IndexException {
		return scheduleIndexManager.reIndexSchedule(schedules, nullContext);
	}
	
	/**
	 * Pass through for getting Execution Summary by Cycle from Index
	 * @param versionId
	 * @param projectId
	 * @return
	 */
	public Set<Map<String, Object>> getExecutionSummaryGroupedByCycle(
			Long versionId, Long projectId) {
		return scheduleIndexManager.getExecutionSummaryGroupedByCycle(versionId, projectId);
	}

	/**
	 * Pass through for getting Execution Summary by User from Index
	 * @param versionId
	 * @param projectId
	 * @return
	 */
	public Set<Map<String, Object>> getExecutionSummaryGroupedByUser(
			Long versionId, Long projectId) {
		return scheduleIndexManager.getExecutionSummaryGroupedByUser(versionId, projectId);
	}

	/**
	 * Pass through for getting Execution Summary by IssueIds from Index
	 * @param issueIds
	 * @param versionId
	 * @return
	 */
	public Map<String, Object> getExecutionSummaryByIssueIds(Collection<Long> issueIds, Long versionId) {
		return scheduleIndexManager.getExecutionSummaryByIssueIds(issueIds, versionId);
	}


	/**
	 * Pass through for getting Schedules By Project ID with Duration
	 * @param projectId
	 * @param periodName 
	 * @param days 
	 * @param showExecutionsOnly 
	 * @return
	 */
	public Map<Long,Map<String, Object>> getSchedulesByProjectIdWithDuration(Integer projectId, 
			String days, String periodName, boolean showExecutionsOnly) {
		return scheduleIndexManager.getSchedulesByProjectIdWithDuration(projectId,days,periodName,showExecutionsOnly);
	}
	
	public JSONObject getUnexecutedSchedulesByProject(Integer projectId,
																Integer versionId,
																Integer cycleId,
																Integer sprintId,
																String periodName) throws JSONException {
		
		final TreeMap<TimePeriod, Integer> rawChartDataPointMap = new TreeMap<TimePeriod, Integer>();
        final Map<String,Integer> countMap = new HashMap<String,Integer>();
        int totalDays = scheduleIndexManager.getScheduleBurndownByPeriod(projectId, versionId, cycleId, sprintId, periodName, rawChartDataPointMap, countMap);
        return preparePresentableDataSetForUnexecutedSchedules(rawChartDataPointMap, countMap, totalDays);
	}
	
	/**
	 * @param rawChartDataPointMap
	 * @param countMap
	 * @param totalDays
	 * @return
	 * @throws JSONException 
	 */
	private JSONObject preparePresentableDataSetForUnexecutedSchedules(
																final TreeMap<TimePeriod, Integer> rawChartDataPointMap,
																final Map<String, Integer> countMap, int totalDays) throws JSONException {
		Map<String,Number> resultMap= new TreeMap<String,Number>();
		float workBurnRate = 0f;
		int remainingSchedulesLog = 0;
		String allSchedulesCompletionDateValue = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("common.words.unknown");
		SimpleDateFormat dateFormatter = new SimpleDateFormat(ApplicationConstants.ZFJ_DATE_FORMAT);
		
        final TreeMap<String, Number> predictionGraphDataMap = new TreeMap<String, Number>();
        if(rawChartDataPointMap.size() == 0){
        	//It means there were no schedules created for given project, version and cycle.
        	//Return one point with zero value.
        	String todayString = String.valueOf(new Date().getTime());
        	resultMap.put(todayString, 0);
        	predictionGraphDataMap.put(todayString, 0);
        }
        else{
            log.debug("Total Schedules - " + countMap.get("CreationCount") + " Schedules Executed - " + countMap.get("ExecutionCount"));
            for(Map.Entry<TimePeriod, Integer> dataPointEntry : rawChartDataPointMap.entrySet()){
            	Date dt = dataPointEntry.getKey().getStart();
            	resultMap.put(String.valueOf(dt.getTime()), dataPointEntry.getValue());
            }

            TimePeriod lastXCoordinate4UnexecutedGraphDataMap = rawChartDataPointMap.lastEntry().getKey();
            Date lastXPointDate = lastXCoordinate4UnexecutedGraphDataMap.getStart();
            Integer lastYCoordinate4UnexecutedGraphDataMap = rawChartDataPointMap.lastEntry().getValue();
            
            predictionGraphDataMap.put(String.valueOf(lastXPointDate.getTime()), lastYCoordinate4UnexecutedGraphDataMap);
            
            int totalSchedulesCreated = countMap.get("CreationCount");
            int totalSchedulesExecuted = countMap.get("ExecutionCount");
            remainingSchedulesLog =  totalSchedulesCreated - totalSchedulesExecuted;
            //workBurnRate = Work Done / total of no. days spent i.e. executionCount / total no. of days.
            workBurnRate = (float)totalSchedulesExecuted / totalDays;
            log.debug(" Work burn rate is " + workBurnRate + " schedules/day");

            Calendar cal = GregorianCalendar.getInstance();
        	cal.setTime(lastXPointDate);
            //if( (workBurnRate > 0) && (totalSchedulesExecuted < totalSchedulesCreated) ){
            if( workBurnRate > 0 ){
            	int timeRequiredToFinishRemainingWork = Math.round(remainingSchedulesLog / workBurnRate);
            	log.debug("Time required to finish " + remainingSchedulesLog + " remaining schedules is " + timeRequiredToFinishRemainingWork + " days.");
            	
            	int totalDaysToFinishAllWork = totalDays + timeRequiredToFinishRemainingWork;            	
            	cal = GregorianCalendar.getInstance();
        		cal.add(Calendar.DATE,timeRequiredToFinishRemainingWork );
        		allSchedulesCompletionDateValue = dateFormatter.format(cal.getTime());
        		
            	cal = GregorianCalendar.getInstance();
            	//If necessary we will plot graph for additional 15 days.
            	//But if the totalDays to finish work takes more than 15 days, then we will show graph till next 15 days only.
            	if(totalDaysToFinishAllWork > (totalDays + 15)){
            		//Let's get the how many executions can be finished in 15 days.
            		int scheduleToBeFinished = Math.round(workBurnRate * 15);
            		cal.add(Calendar.DATE, new Integer(15));
            		predictionGraphDataMap.put(String.valueOf(cal.getTime().getTime()), scheduleToBeFinished);
            		log.debug("In next 15 days " + scheduleToBeFinished + " schedules will be done.");
            	}else{
            		//store the date when remaining schedules get executed
            		cal.add(Calendar.DATE, new Integer(timeRequiredToFinishRemainingWork));
            		allSchedulesCompletionDateValue= dateFormatter.format(cal.getTime());
            		predictionGraphDataMap.put(String.valueOf(cal.getTime().getTime()), 0);
            		log.debug("By " + cal.getTime() + " all remaining schedules will be executed.");
            	}
            //}else if(workBurnRate == 0 && (totalSchedulesExecuted == 0)){
            }else if(workBurnRate == 0){
            	//This is infinity condition where not a single schedule has been executed.
            	//In this case we will have one more point indicating schedules count remains same as there were on 30th day!
        		cal.add(Calendar.DATE, new Integer(15));
        		predictionGraphDataMap.put(String.valueOf(cal.getTime().getTime()), totalSchedulesCreated - totalSchedulesExecuted);
        		log.debug("By " + cal.getTime() + " schedules to remain executed will be still " + totalSchedulesExecuted + " which is same as today!");
            }
            

			for(Map.Entry<String, Number> i$ : predictionGraphDataMap.entrySet()){
				log.debug("Date in millis - "+ i$.getKey() + " unexecuted Schedules - "+ i$.getValue());
			}

        }
        
        JSONObject finalResponse = new JSONObject();
        Map<String,Map<String,Number>> graphMaps = new HashMap<String,Map<String,Number>>();
        graphMaps.put("UnexecutedGraphDataMap", resultMap);
        graphMaps.put("PredictionGraphDataMap", predictionGraphDataMap);
        
        finalResponse.put("data", graphMaps);
        finalResponse.put("executionRate", String.format("%.2g%n", workBurnRate));
        finalResponse.put("executionsRemaining", remainingSchedulesLog);
        finalResponse.put("completionDate", allSchedulesCompletionDateValue);
		return finalResponse;
	}
	
	/**
	 * Pass through for getting SchedulesWith Maximun Defects by Duration
	 * @param projectId
	 * @param versionId 
	 * @param days 
	 * @param statuses 
	 * @return
	 */
	public SortedSet<DefectSummaryModel> getTopSchedulesWithDefectsByDuration(
			Integer projectId, Integer versionId, String days, String statuses) {
		return 	scheduleIndexManager.getTopSchedulesWithDefectsByDuration(projectId, versionId, days,statuses);
	}
	
	public void indexSchedule(ScheduleIdsScheduleIterable scheduleIdsScheduleIterable) throws IndexException {
		scheduleIndexManager.reIndexSchedule(scheduleIdsScheduleIterable, Contexts.nullContext());
	}

	
	/**
	 * 
	 * @param groupFldId
	 * @param groupFldName
	 * @param countByStatus
	 * @return
	 */
	public Map<String, Object> populateScheduleSummary(Long groupFldId, String groupFldName, Map<String, Object> countByStatus) {
        Map<String, Object> componentSummary = new LinkedHashMap<>();
        componentSummary.put("id", groupFldId);
        componentSummary.put("name", groupFldName);
		componentSummary.put("cnt", countByStatus != null && countByStatus.size() > 0 ? countByStatus : new HashMap<String, Object>(){{put("-1", 0);put("total", 0);}});
        return componentSummary;
	}
	
	
	/**
	 * Populates sorted Execution Status Map - specially important for FF or IE
	 * @return
	 */
	public Map<String, Object> populateStatusMap() {
		Map<String, Object> statusesMap = new LinkedHashMap<String, Object>();
		ExecutionStatus unexecuted = null;
		for(ExecutionStatus execStatus : JiraUtil.getExecutionStatuses().values()) {
			if(execStatus.getId().intValue() == ApplicationConstants.UNEXECUTED_STATUS){
				unexecuted = execStatus;
				continue;
			}
			statusesMap.put(String.valueOf(execStatus.getId()), execStatus.toMap());
		}
		if(unexecuted != null)
			statusesMap.put(String.valueOf(unexecuted.getId()), unexecuted.toMap());
		return statusesMap;
	}
	
	/**
	 * Populates Execution Status Map 
	 * @return
	 */
	public List<Map<String, Object>> populateStatusList() {
		List<Map<String, Object>> statusesList = new ArrayList<Map<String, Object>>();
		for(ExecutionStatus execStatus : JiraUtil.getExecutionStatuses().values()) {
			Map<String, Object> statusMap = new HashMap<String, Object>();
			statusMap.put("id", execStatus.getId());
			statusMap.put("color", execStatus.getColor());
			statusMap.put("desc", execStatus.getDescription());
			statusMap.put("name", execStatus.getName().toUpperCase());
			statusesList.add(statusMap);
		}
		return statusesList;
	}

    /**
     * Returns comma separated values for Issues Added, Skipped or Already Existing
     * @param resultMap
     * @param messageKey
     * @return
     */
	public String getInfoMessage(Map<String, List<String>> resultMap, String messageKey) {
		if(StringUtils.isNotBlank(messageKey) && resultMap.containsKey(messageKey)) {
			return StringUtils.join(resultMap.get(messageKey), ",");
		} else {
			return "-";
		}
	}

	/**
	 * Sets Cycle Summary Details which is good for current session
	 * @param req
	 * @param cycleId
	 * @param versionId
	 * @param action
	 * @param offset
	 * @param sortQuery
	 */
	@SuppressWarnings("unchecked")
	public void setCycleSummaryDetail(final HttpServletRequest req,final Integer cycleId,final Long versionId, final Long folderId, String action, Integer offset, String sortQuery) {
		HttpSession session = req.getSession(false);
		if(session == null){
			log.warn("No Session found, unable to recover cycle expand settings.");
			return;
		}
		Map<String,String> cycleActionMap = (HashMap<String, String>)session.getAttribute(SessionKeys.CYCLE_SUMMARY_DETAIL);
		if(cycleActionMap == null) {
			cycleActionMap = new HashMap<String, String>();
			session.setAttribute(SessionKeys.CYCLE_SUMMARY_DETAIL, cycleActionMap);
		}
		StringBuilder sbuilder = new StringBuilder();
		sbuilder.append("action="+action);
		sbuilder.append(",soffset="+offset);
		sbuilder.append(",sortQuery="+sortQuery);
		String compositekey = String.valueOf(cycleId)+":"+ String.valueOf(versionId);
		if(folderId != null) compositekey = String.valueOf(folderId) + ":" + compositekey; 
		cycleActionMap.put(compositekey, sbuilder.toString());
	}

	/**
	 * Converts Exeuction Summary for Duration to JSON Structure
	 * @param data
	 * @return
	 * @throws JSONException
	 */
	public JSONObject executionDurationToJSON(
			Map<Long, Map<String, Object>> data) throws JSONException {
		JSONObject jsonObject = new JSONObject();
		for(Long dateKey : data.keySet()){
			jsonObject.put(dateKey.toString(), data.get(dateKey));
		}
		return jsonObject;
	}

	/**
	 * Check to see if Index Directory Present or if its first time so that we can manually kick off Index Creation.
	 */
	public boolean isIndexDirectoryPresent() {
		return scheduleIndexManager.isIndexDirectoryPresent();
	}


	/**
	 * Forms Response for Bulk Status Update
	 * @param i18NHelper
	 * @param schedules
	 * @param scheduleList
	 * @param noZephyrPermissionExecutions
	 * @param noJiraPermissions
	 * @param workFlowCompletedExecutions
	 * @return
	 */
	public JSONObject formBulkUpdateResponse(I18nHelper i18NHelper, List<String> schedules, List<Schedule> scheduleList,
											 Collection<String> noZephyrPermissionExecutions, Collection<String> noJiraPermissions, Collection<String> workFlowCompletedExecutions) {
		JSONObject jsonObject = new JSONObject();
       	try {
       		jsonObject.put("error","-");
       		jsonObject.put("success","-");
       		jsonObject.put("noPermission","-");

	       	if(schedules.size() == scheduleList.size()) {
	       		jsonObject.put("success", i18NHelper.getText("enav.bulk.result"));
	       	} else {
	        	Collection<String> successfulSchedules = transformScheduleObjectToID(scheduleList);
        		jsonObject.put("success", StringUtils.join(successfulSchedules,","));
	        	schedules.removeAll(successfulSchedules);
	        	schedules.removeAll(noZephyrPermissionExecutions);
        		if(!noZephyrPermissionExecutions.isEmpty()) {
		        	jsonObject.put("error", StringUtils.join(schedules,","));
		        	jsonObject.put("noZephyrPermission", StringUtils.join(noZephyrPermissionExecutions,","));
		        	if(!noJiraPermissions.isEmpty()) {
						jsonObject.put("noIssuePermission", StringUtils.join(noJiraPermissions,","));
					}
                    if(CollectionUtils.isNotEmpty(workFlowCompletedExecutions)) {
                        jsonObject.put("workFlowCompletedExecutions", StringUtils.join(workFlowCompletedExecutions,","));
                    }
	        	} else if(!noJiraPermissions.isEmpty()) {
					jsonObject.put("error", StringUtils.join(schedules,","));
					jsonObject.put("noIssuePermission", StringUtils.join(noJiraPermissions,","));

                    if(CollectionUtils.isNotEmpty(workFlowCompletedExecutions)) {
                        jsonObject.put("workFlowCompletedExecutions", StringUtils.join(workFlowCompletedExecutions,","));
                    }
				} else if(CollectionUtils.isNotEmpty(workFlowCompletedExecutions)) {
					jsonObject.put("error", StringUtils.join(schedules,","));
					jsonObject.put("workFlowCompletedExecutions", StringUtils.join(workFlowCompletedExecutions,","));
				} else {
	        		jsonObject.put("error", StringUtils.join(schedules,","));
	        	}
	        }
       	} catch(JSONException e) {
    		log.warn("Error creating JSON Object",e);
    	}
		return jsonObject;
	}


    /**
     * @param associatedDefects
     * @return
     */
    public List<Map<String, String>> convertScheduleDefectToMap(List<ScheduleDefect> associatedDefects) {
        if(associatedDefects == null) {
            associatedDefects = new ArrayList<ScheduleDefect>(0);
        }
        List<Map<String, String>> scheduleDefectList = new ArrayList<Map<String, String>>(associatedDefects.size());
        if(associatedDefects != null && associatedDefects.size() > 0){
            for(ScheduleDefect sd : associatedDefects){
                MutableIssue issue = issueManager.getIssueObject(new Long(sd.getDefectId()));
                if(issue == null)
                    continue;
                final Map<String, String> scheduleDefectMap = IssueUtils.convertDefectToMap(issue);
                scheduleDefectList.add(scheduleDefectMap);
            }
        }
        Collections.sort(scheduleDefectList, new Comparator<Map<String, String>>() {
            @Override
            public int compare(Map<String, String> first, Map<String, String> second) {
                return first.get("key").compareTo(second.get("key"));
            }
        });
        return scheduleDefectList;
    }


	public List<Map<String, String>> convertScheduleDefectToMapMasked(List<ScheduleDefect> associatedDefects, Boolean masked) {
		if(associatedDefects == null) {
			associatedDefects = new ArrayList<ScheduleDefect>(0);
		}
		List<Map<String, String>> scheduleDefectList = new ArrayList<Map<String, String>>(associatedDefects.size());
		if(associatedDefects != null && associatedDefects.size() > 0){
			for(ScheduleDefect sd : associatedDefects){
				MutableIssue issue = issueManager.getIssueObject(new Long(sd.getDefectId()));
				if(issue == null)
					continue;
				if(masked != null && !masked) {
					final Map<String, String> scheduleDefectMap = IssueUtils.convertDefectToMapMasked(issue);
					scheduleDefectList.add(scheduleDefectMap);
				}else{
					final Map<String, String> scheduleDefectMap = IssueUtils.convertDefectToMap(issue);
					scheduleDefectList.add(scheduleDefectMap);
				}
			}
		}
		Collections.sort(scheduleDefectList, new Comparator<Map<String, String>>() {
			@Override
			public int compare(Map<String, String> first, Map<String, String> second) {
				return first.get("key").compareTo(second.get("key"));
			}
		});
		return scheduleDefectList;
	}
    
    
    public Map<String,String> convertScheduleToMap (Schedule schedule) {
        Map<String,String> executionField = new TreeMap<String, String>();
        ExecutionStatus executionStatus = JiraUtil.getExecutionStatuses().get(new Integer(schedule.getStatus()));
		boolean hasIssueViewPermission = JiraUtil.hasIssueViewPermission(Long.valueOf(schedule.getIssueId()),null,ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser());
        executionField.put("id", String.valueOf(schedule.getID()));

		if(hasIssueViewPermission) {
            executionField.put("testCycle", schedule.getCycle() == null ? ApplicationConstants.AD_HOC_CYCLE_NAME : schedule.getCycle().getName());
            executionField.put("status", executionStatus.getName());
            executionField.put("statusId", executionStatus.getId().toString());
		} else {
			executionField.put("testCycle", ApplicationConstants.MASKED_DATA);
			executionField.put("status", ApplicationConstants.MASKED_DATA);
			executionField.put("statusId", ApplicationConstants.MASKED_DATA);
		}
		if(schedule.getFolder()!=null){
            executionField.put("folderName", schedule.getFolder().getName());
            }
        return executionField;
    }

    
	/**
	 * Converts IssueObject to Map
	 * @param issue
	 * @return
	 */
	public Map<String, Object> convertIssueToMap (Issue issue) {
        Map<String,Object> testData = new TreeMap<String, Object>();
        if(issue != null) {
			if(JiraUtil.hasIssueViewPermission(null,issue,ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser())) {
                testData.put("id", issue.getId());
                testData.put("key", issue.getKey());
                testData.put("summary", issue.getSummary());
                testData.put("status", issue.getStatusObject().getName());
                testData.put("statusId", issue.getStatusObject().getId());
			} else {
				testData.put("id", issue.getId());
				testData.put("key", issue.getKey());
				testData.put("maskedIssueKey", ApplicationConstants.MASKED_DATA);
				testData.put("summary", ApplicationConstants.MASKED_DATA);
				testData.put("status", ApplicationConstants.MASKED_DATA);
				testData.put("statusId", ApplicationConstants.MASKED_DATA);
			}
        }
	    return testData;
	}
    
	
	/**
	 * Transforms Schedule Object to Ids
	 * @param scheduleList
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Collection<String> transformScheduleObjectToID(List<Schedule> scheduleList) {
		final Collection<String> successfulSchedules = CollectionUtils.collect(scheduleList, new Transformer() {
		    @Override
			public String transform(final Object input) {
		        if (input == null) {
		            return null;
		        }
		        final String scheduleId = String.valueOf(((Schedule)input).getID());
		        return scheduleId;
		    }
		});
		return successfulSchedules;
	}

	public Response getExecutionSummariesBySprintAndIssue(String[] issueIdOrKeys,
			String[] sprintIds) throws Exception {
        List<Long> issueList = new ArrayList<Long>(); 
        List<Issue> issues = new ArrayList<Issue>();
    	JSONObject jsonObject = new JSONObject();
    	JSONObject successJsonObject = new JSONObject();
    	//iterate and filter our invalid Sprints
		List<Long> validSprints = new ArrayList<Long>();
		List<Long> inValidSprints = new ArrayList<Long>();

    	//GetAllStory for Sprint
        //for All Issues belonging to a Sprint which is not a Test, get Linked Test and Summary
    	try {
    		JqlClauseBuilder jqlClauseBuilder = JqlQueryBuilder.newClauseBuilder();
    		Long[] sprintIdArray = (Long[])  ConvertUtils.convert(sprintIds, Long[].class);
    		stripInvalidSprints(sprintIdArray,validSprints,inValidSprints);
    		if((validSprints.isEmpty() && !inValidSprints.isEmpty()) || (validSprints.isEmpty() && inValidSprints.isEmpty())) {
    			return createInvalidSprintError(inValidSprints);
    		}
			jqlClauseBuilder.addNumberCondition("Sprint", Operator.IN, validSprints);
			jqlClauseBuilder.defaultAnd().issueType().notEq().string(JiraUtil.getTestcaseIssueTypeId());
			Query query = jqlClauseBuilder.buildQuery();
	        SearchResults searchResults = searchProvider.search(query, authContext.getLoggedInUser(), new PagerFilter().getUnlimitedFilter());
	        issues = searchResults.getIssues();
    	} catch(Exception e) {
    		log.error("Error retrieving Stories for a sprint",e);
        	jsonObject.put("errorMessages", authContext.getI18nHelper().getText("schedule.execute.update.stepresult.invalid.id", "Sprint/Issue"));
    		return Response.status(Status.NOT_FOUND).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
    	}
        List<Issue> validIssuesBySprint = new ArrayList<Issue>();
		for(Issue issue : issues) {
			if (issueIdOrKeys != null && issueIdOrKeys.length != 0) {
				for (String issueIdOrKey : issueIdOrKeys) {
					if (Pattern.matches(".*[a-zA-Z]+.*", issueIdOrKey)) {
						if (StringUtils.equalsIgnoreCase(issue.getKey(), issueIdOrKey)) {
							validIssuesBySprint.add(issue);
						}
					} else {
						if (issue.getId().longValue() == Long.valueOf(issueIdOrKey).longValue()) {
							validIssuesBySprint.add(issue);
						}
					}
				}
			} else {
				validIssuesBySprint.add(issue);
			}
		}
        List<Long> zephyrPermissionErrors = new ArrayList<Long>();
        //Get LinkedTest for each Story
        for(Issue issue : validIssuesBySprint) {
        	if(issue != null && !StringUtils.equalsIgnoreCase(issue.getIssueType().getId(),JiraUtil.getTestcaseIssueTypeId())) {
            	try {
                	ProjectPermissionKey projectPermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_BROWSE_CYCLE.toString());
                    ZephyrPermissionManager zephyrPermissionManager = (ZephyrPermissionManager)ZephyrComponentAccessor.getInstance().getComponent("zephyrPermissionManager");
                	boolean hasZephyrPermission = zephyrPermissionManager.validateUserPermission(projectPermissionKey, issue.getProjectObject(), authContext.getLoggedInUser(),issue.getProjectObject().getId());
    		        if (hasZephyrPermission) {
	   	        		JqlClauseBuilder jqlClauseBuilder = JqlQueryBuilder.newClauseBuilder();
		        		jqlClauseBuilder.and().issueType().eq().string(JiraUtil.getTestcaseIssueTypeId());
		    			jqlClauseBuilder.and().issue().in().functionLinkedIssues(String.valueOf(issue.getId()));
		    			Query query = jqlClauseBuilder.buildQuery();
		    	        SearchResults searchResults = searchProvider.search(query, authContext.getLoggedInUser(), new PagerFilter().getUnlimitedFilter()); 
		    	        //based on test, get the count
		    			//getExecutionSummary (Not by Project or Version. By Issue)
		    			Map<String,Object> execSummary = getExecutionSummaryWithDefectCount(searchResults.getIssues());
		            	Object action =  zfjCacheService.getCacheByKey("ExecutionsByIssue" + ":" + authContext.getLoggedInUser().getKey()+ ":" + issue.getKey(),"collapse");
		            	execSummary.put("action",action != null? (String)action : "collapse");
		            	successJsonObject.put(String.valueOf(issue.getId()), execSummary);
    		        } else {
    		        	zephyrPermissionErrors.add(issue.getId());
    		        }
            	} catch(Exception e) {
            		log.error("Error retrieving Execution Summary",e);
            	}
        	}
        }
        if(inValidSprints.size() > 0) {
        	jsonObject.put("failed", authContext.getI18nHelper().getText("zephyr.common.error.invalid", "SprintId(s)",StringUtils.join(inValidSprints,",")));
			log.info(jsonObject.toString());
        }
        if((!zephyrPermissionErrors.isEmpty() && inValidSprints.isEmpty())
				|| (validIssuesBySprint.size() == zephyrPermissionErrors.size() && zephyrPermissionErrors.size() > 0)) {
			String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
			log.error("[Error] [Error code:"+ Status.FORBIDDEN.getStatusCode()+" "+ Status.FORBIDDEN+" Error Message :"+errorMessage);
			return JiraUtil.getPermissionDeniedErrorResponse(errorMessage);
        }
        jsonObject.put("successful", successJsonObject);
        return Response.ok().entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
	}

	private void stripInvalidSprints(Long[] sprintIdArray, List<Long> validSprints, List<Long> inValidSprints) {
		for(Long sprintId : sprintIdArray) {
			Optional<SprintBean> sprint = sprintService.getSprint(sprintId);
			if(sprint.isPresent()) {
				SprintBean sprintBean = sprint.get();
				validSprints.add(sprintBean.getId());
			} else {
				inValidSprints.add(sprintId);
			}
		}
	}

	public Response getExecutionsByIssue(MutableIssue issue,Integer offset,Integer maxRecords, 
			String expand) {
      //Get LinkedTest for each Story
    	try {
    		JqlClauseBuilder jqlClauseBuilder = JqlQueryBuilder.newClauseBuilder();
			jqlClauseBuilder.and().issueType().eq().string(JiraUtil.getTestcaseIssueTypeId());
			jqlClauseBuilder.and().issue().in().functionLinkedIssues(String.valueOf(issue.getId()));
			Query query = jqlClauseBuilder.buildQuery();
	        SearchResults searchResults = searchProvider.search(query, authContext.getLoggedInUser(), new PagerFilter().getUnlimitedFilter());
	        return getExecutionsByIssues(searchResults.getIssues(),offset,maxRecords,expand);
    	} catch(Exception e) {
    		log.error("Error retrieving Tests by Story Issue",e);
    		return returnNotFound(issue.getKey());
    	}
 	}

	private Map<String, Object> getExecutionSummaryWithDefectCount(List<Issue> issues) throws Exception {
        int totalExecuted = 0;
        int totalExecutions = 0;
        List<Long> issueIds = CollectionUtil.transform(issues, new Function<Issue, Long>() {
            @Override
            public Long get(final Issue issue) {
                return issue.getId();
            }
        });
        Map<String, Object> cntByIssueIdAndStatus = new HashMap<String, Object>();
		if(issueIds != null) {
	        final Set<Entry<Integer, ExecutionStatus>> statuses = JiraUtil.getExecutionStatuses().entrySet();
	        List<ExecutionSummaryImpl> allSummaries = scheduleIndexManager.getExecutionSummariesByIssueIds(issueIds);
	        JSONObject jsonObject = new JSONObject();
	        JSONArray expectedOperators = new JSONArray();
	        if(allSummaries != null) {
		        for (ExecutionSummaryImpl executionSummary : allSummaries) {
		            if (executionSummary.getExecutionStatusKey().intValue() != -1 &&
		                    !StringUtils.equalsIgnoreCase(executionSummary.getExecutionStatusName(), "Unexecuted")) {
		                totalExecuted += executionSummary.getCount();
		            }
		            totalExecutions += executionSummary.getCount();
		            expectedOperators.put(executionSummaryToJSON(executionSummary));
		        }
	        }
	        jsonObject.put("executionSummary", expectedOperators);
	        cntByIssueIdAndStatus.put("executionSummaries", jsonObject);
		}
        cntByIssueIdAndStatus.put("totalExecutions", totalExecutions);
        cntByIssueIdAndStatus.put("totalExecuted", totalExecuted);
    	int totalDefectCount = 0;
    	int totalOpenDefectCount = 0;
    	int totalResolvedDefectCount = 0;
    	JSONArray defectSummariesArray = new JSONArray();
        String defaultMax = ComponentAccessor.getApplicationProperties().getDefaultBackedString("jira.search.stable.max.results");
        //For version below 6.0 will be null
        if(StringUtils.isBlank(defaultMax)) {
        	defaultMax = ComponentAccessor.getApplicationProperties().getDefaultBackedString(APKeys.JIRA_SEARCH_VIEWS_DEFAULT_MAX);
        }
        Integer allowedRecordLimit = defaultMax != null ? Integer.valueOf(defaultMax.trim()) : MAX_RECORDS_ALLOWED;
    	if(!issueIds.isEmpty()) {
	    	String zqlQuery = "ISSUE IN (" + StringUtils.join(issueIds,",") + ")";
	    	ScheduleSearchResourceHelper searchResourceHelper = new ScheduleSearchResourceHelper(authContext.getLoggedInUser(),
	    			searchService,zephyrClauseHandlerFactory,issueManager,cycleManager,versionManager, zephyrCustomFieldManager);
	    	ZQLSearchResultBean zqlSearchResultBean = searchResourceHelper.performRawZQLSearch(zqlQuery, 0, allowedRecordLimit, searchResourceHelper);
	    	List<ZQLScheduleBean> zqlScheduleBeans = zqlSearchResultBean.getExecutions();
	    	Map<String,ExecutionDefectBean> uniqueDefectBean = new HashMap<String, ExecutionDefectBean>();
	    	for(ZQLScheduleBean zqlScheduleBean : zqlScheduleBeans) {
				if(zqlScheduleBean.getTestDefectsUnMasked() != null
						&& zqlScheduleBean.getTestDefectsUnMasked().size() > 0) {
					for (String testDefect : zqlScheduleBean.getTestDefectsUnMasked()) {
						ExecutionDefectBean testDefectBean = getConvertedExecutionDefectBean(testDefect);
						if (!uniqueDefectBean.containsKey(testDefectBean.getDefectKey())) {
							uniqueDefectBean.put(testDefectBean.getDefectKey(), testDefectBean);
							if (StringUtils.isNotBlank(testDefectBean.getDefectResolutionId())) {
								totalResolvedDefectCount += 1;
							}
						}
					}
				}
	    		
	    		//Now for Step Defect
				if(zqlScheduleBean.getStepDefectsUnMasked() != null
						&& zqlScheduleBean.getStepDefectsUnMasked().size() > 0) {
					for (String stepDefect : zqlScheduleBean.getStepDefectsUnMasked()) {
						ExecutionDefectBean stepDefectBean = getConvertedExecutionDefectBean(stepDefect);
						if (!uniqueDefectBean.containsKey(stepDefectBean.getDefectKey())) {
							uniqueDefectBean.put(stepDefectBean.getDefectKey(), stepDefectBean);
							if (StringUtils.isNotBlank(stepDefectBean.getDefectResolutionId())) {
								totalResolvedDefectCount += 1;
							}
						}
					}
				}
			}
	    	totalOpenDefectCount = uniqueDefectBean.size() > 0 ? uniqueDefectBean.size() - totalResolvedDefectCount : 0;
	    	totalDefectCount = uniqueDefectBean.size();
    	}
    	cntByIssueIdAndStatus.put("totalDefectCount", totalDefectCount);
    	cntByIssueIdAndStatus.put("totalOpenDefectCount", totalOpenDefectCount);
    	return cntByIssueIdAndStatus;
	}

	/**
	 * Get ConvertedTestExecutionDefectBean
	 * @param defectKey
	 * @return
	 */
	private ExecutionDefectBean getConvertedExecutionDefectBean(String defectKey) {
		ExecutionDefectBean executionDefect = new ExecutionDefectBean();
		Issue defectIssue = ComponentAccessor.getIssueManager().getIssueObject(defectKey);
		if(null != defectIssue) {
			executionDefect.setDefectId(defectIssue.getId().intValue());
				executionDefect.setDefectKey(defectKey);
				executionDefect.setDefectStatus(defectIssue.getStatus().getNameTranslation());
				executionDefect.setDefectSummary(defectIssue.getSummary());
				if (defectIssue.getResolutionId() != null)
					executionDefect.setDefectResolutionId(defectIssue.getResolution().getName());
				else
					executionDefect.setDefectResolutionId("");
		}
		return  executionDefect;
	}

	/**
     * Creates JSON Structure for Execution Summary
     *
     * @param executionSummary
     * @return
     * @throws JSONException
     */
    public static JSONObject executionSummaryToJSON(ExecutionSummaryImpl executionSummary) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("count", executionSummary.getCount());
        jsonObject.put("statusKey", executionSummary.getExecutionStatusKey());
        jsonObject.put("statusName", executionSummary.getExecutionStatusName());
        jsonObject.put("statusColor", executionSummary.getExecutionStatusColor());
        jsonObject.put("statusDescription", executionSummary.getExecutionStatusDescription());
        return jsonObject;
    }
    
    
    /**
     * Creates JSON Structure for Execution/Step Defect
     *
     * @param executionDefect
     * @return
     * @throws JSONException
     */
    private JSONObject executionDefectToJSON(ExecutionDefectBean executionDefect) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", executionDefect.getDefectId());
        jsonObject.put("key", executionDefect.getDefectKey());
        jsonObject.put("status", executionDefect.getDefectStatus());
        jsonObject.put("summary", executionDefect.getDefectSummary());
        jsonObject.put("resolution", executionDefect.getDefectResolutionId());
        return jsonObject;
    }
    
	private Response getExecutionsByIssues(List<Issue> issues,
			Integer offset, Integer maxRecords, String expand) {
        List<Long> issueIds = CollectionUtil.transform(issues, new Function<Issue, Long>() {
            @Override
            public Long get(final Issue issue) {
                return issue.getId();
            }
        });
		if(issueIds != null && !issueIds.isEmpty()) {
        	ScheduleSearchResourceHelper searchResourceHelper = new ScheduleSearchResourceHelper(authContext.getLoggedInUser(),
        			searchService,zephyrClauseHandlerFactory,issueManager,cycleManager,versionManager,authContext,scheduleIndexManager,zephyrCustomFieldManager);
        	String zqlQuery = "ISSUE IN (" + StringUtils.join(issueIds,",") + ")";
        	return searchResourceHelper.performZQLSearch(zqlQuery, offset, maxRecords, expand,searchResourceHelper,issues);
		} else {
	    	ZQLSearchResultBean resultBean = new ZQLSearchResultBean();
	    	List<ZQLScheduleBean> schedules = new ArrayList<ZQLScheduleBean>();
	    	resultBean.setExecutions(schedules);
			return Response.ok(resultBean).cacheControl(ZephyrCacheControl.never()).build();
		}
	}
	
	private Response returnNotFound(String issueKeyOrId) {
		JSONObject errorJsonObject = null;
		try {			
			errorJsonObject = new JSONObject();
			errorJsonObject.put("errorMessages", authContext.getI18nHelper().getText("schedule.execute.update.stepresult.invalid.id", issueKeyOrId));
		} catch (JSONException ex) {
			log.error("Error constructing JSON",ex);
		}		
		return Response.status(Status.NOT_FOUND).entity(errorJsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
	}
	
	private Response createInvalidSprintError(List<Long> inValidSprints) {
        Map<String, String> errorMap = new HashMap<String, String>();
        errorMap.put("Invalid Sprint(s)", authContext.getI18nHelper().getText("zephyr.common.error.invalid", "SprintId(s) ", StringUtils.join(inValidSprints,",")));
        return Response.status(Status.BAD_REQUEST).entity(errorMap).cacheControl(ZephyrCacheControl.never()).build();
	}

	/*********************************************/
	/******** Refresh Linktype relation **********/
	/*********************************************/
	public Long performLinksRefreshAsync(String contextPath, Long issueLinkTypeId, Boolean remoteIssueLinkEnabled, Boolean issuelinkEnabled,Boolean issueToTestStepLink, Boolean remoteIssueLinkStepExecution) {
		assert issueManager != null && scheduleManager != null && rilManager != null;

		final long startTime = System.currentTimeMillis();
		Long oldIssueLinkTypeId = getLinkTypeId();
		// update IssueLinkType property
		updateDBConfigProperty(ConfigurationConstants.ZEPHYR_ISSUE_LINK_RELATION, issueLinkTypeId);

		int offset = 0;
		int limit = 50;

		List<Schedule> schedules = scheduleManager.getSchedules(null, offset, limit);
		while (schedules != null && schedules.size() != 0) {
			for (Schedule schedule : schedules) {
				try {
					String cycleBreadCrumb = getCycleBreadCrumbOfSchedule(schedule);
					Issue testcase = issueManager.getIssueObject(new Long(schedule.getIssueId()));
					//ScheduleDefect remote link update if they are enable
					if(issuelinkEnabled){
						List<ScheduleDefect> sdefects = scheduleManager.getScheduleDefects(schedule.getID());
						for (ScheduleDefect sd : sdefects) {
							try {
								if (testcase != null) {
									updateIndividualRemoteLink(testcase, schedule, contextPath, cycleBreadCrumb, sd.getDefectId(), issuelinkEnabled, remoteIssueLinkEnabled, issueLinkTypeId, oldIssueLinkTypeId);
								} else {
									log.error("Unable to find IssueId " + schedule.getIssueId() + " in the DB for the Schedule " + schedule.getID());
								}
							} catch (UpdateException e) {
								log.error("Error in updating remote link", e);
							}
						}
					}
					//StepDefect remote link update if they are enable
					if(issueToTestStepLink) {
						List<StepDefect> stepDefects = stepResultManager.getStepResultsWithDefectBySchedule(schedule.getID());
						for (StepDefect stepDefect : stepDefects) {
							try {
								if (testcase != null) {
									updateIndividualRemoteLink(testcase, schedule, contextPath, cycleBreadCrumb, stepDefect.getDefectId(), issueToTestStepLink, remoteIssueLinkStepExecution, issueLinkTypeId, oldIssueLinkTypeId);
								} else {
									log.error("Unable to find Step " + stepDefect.getStepId() + " in the DB for the Step Defect " + stepDefect.getDefectId());
								}
							} catch (UpdateException e) {
								log.error("Error in updating remote link", e);
							}
						}
					}
                } catch (Exception ex) {
					log.error("Error in updating links for schedule " + schedule.getID(), ex);
				}
			}
			offset += limit;
			schedules = scheduleManager.getSchedules(null, offset, limit);
		}
		return (System.currentTimeMillis() - startTime);
	}

	/**
	 * Creates a bread crumb of cycle in form of (project/version/cycle)
	 *
	 * @param schedule
	 * @return
	 */
	public String getCycleBreadCrumbOfSchedule(Schedule schedule) {
		String pName = "";
		StringBuffer sb;
		if ((schedule.getProjectId() != null)) {
			Project proj = ComponentAccessor.getProjectManager().getProjectObj(schedule.getProjectId());
			if (proj != null)
				pName = proj.getName();
			else
				pName = "ProjectId : " + schedule.getProjectId();
		}
		String vName = "";
		if (schedule.getVersionId() == null || ObjectUtils.equals(schedule.getVersionId(), new Long(ApplicationConstants.UNSCHEDULED_VERSION_ID)))
			vName = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.je.version.unscheduled");
		else {
			Version ver = ComponentAccessor.getVersionManager().getVersion(schedule.getVersionId());
			if (ver != null)
				vName = ver.getName();
		}

		String cName = (schedule.getCycle() == null) ? ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.je.cycle.adhoc") : schedule.getCycle().getName();

        sb = new StringBuffer(pName).append('/').append(vName).append('/').append(cName);

        String folderName = (null == schedule.getFolder()) ? StringUtils.EMPTY : schedule.getFolder().getName();

        if(StringUtils.isNotBlank(folderName)) {
            sb.append("/").append(folderName);
        }

		return sb.toString();
	}

	/**
	 * @param testcase
	 * @param schedule
	 * @param contextPath
	 * @param issueId
	 * @param remoteIssueLinkEnabled
	 * @param issuelinkEnabled
	 * @throws UpdateException
	 * @throws CreateException
	 */
	private void updateIndividualRemoteLink(Issue testcase, Schedule schedule, String contextPath, String cycleBreadCrumb, Integer issueId,
											Boolean issuelinkEnabled, Boolean remoteIssueLinkEnabled, Long issueLinkTypeId, Long oldIssueLinkTypeId) throws UpdateException {
		RemoteIssueLink currentRil = null;
		//Fix for http://bugzilla.yourzephyr.com/show_bug.cgi?id=4429
		if (issuelinkEnabled)
			createRemoteLinkFromDefectBacktoTestcase(testcase.getId(), issueId.longValue(), issueLinkTypeId, oldIssueLinkTypeId);
		if (remoteIssueLinkEnabled) {
			try {
				currentRil = rilManager.getRemoteIssueLinkByGlobalId(issueManager.getIssueObject(new Long(issueId)), String.valueOf(schedule.getID()));
			} catch (Exception e) {
				log.error("unable to fetch RIL, Error " + e.getMessage() + ". We will attempt to create one.");
			}
			if (currentRil == null) {
				addIndividualLink(testcase, schedule, contextPath, cycleBreadCrumb, issueId, issuelinkEnabled, remoteIssueLinkEnabled, issueLinkTypeId, oldIssueLinkTypeId);
			} else {
				RemoteIssueLink ril = createRemoteLinkObjectFromMap(currentRil.getId(), testcase, schedule, new Long(issueId), cycleBreadCrumb, contextPath);
				rilManager.updateRemoteIssueLink(ril, authContext.getLoggedInUser());
			}
		}
	}

	/**
	 * @param testCaseId - testcase to which link will point
	 * @param issueId    - Defect ID on which link need to create
	 */
	private void createRemoteLinkFromDefectBacktoTestcase(Long testCaseId, Long issueId, Long issueLinkTypeId, Long oldIssueLinkTypeId) {
		final IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager();
		if (!issueLinkManager.isLinkingEnabled()) {
			log.warn("Linking is disabled, skipping linking defect with Zephyr testcase");
			return;
		}
		try {
			//Long linkTypeId = getLinkTypeId();
			if (issueLinkTypeId == null) {
				log.warn("Linking is disabled, skipping linking defect with Zephyr testcase");
				return;
			}
			if (JiraUtil.isTestToIssueLinkingEnabled()) {
				//createInverseIssueLink(issueId, testCaseId, oldIssueLinkTypeId, issueLinkTypeId);
				removeIssueLinkIfAny(issueId, testCaseId, oldIssueLinkTypeId); // for reset
				removeIssueLinkIfAny(testCaseId, issueId, oldIssueLinkTypeId); // for reset
				issueLinkManager.createIssueLink(testCaseId, issueId, issueLinkTypeId, 0l, authContext.getLoggedInUser());
			} else {
				removeIssueLinkIfAny(issueId, testCaseId, oldIssueLinkTypeId); // for reset
				removeIssueLinkIfAny(testCaseId, issueId, oldIssueLinkTypeId); // for inversed links
				issueLinkManager.createIssueLink(issueId, testCaseId, issueLinkTypeId, 0l, authContext.getLoggedInUser());
			}
		} catch (Exception e1) {
			log.error("Error in linking defect with Zephyr testcase. " + e1.getMessage() + "[If there is an error when creating the \"Change Item\" for this operation. Note that the Link itself has most likely been created]");
		}
	}

	public void addRemoteLinks(Collection<Integer> addedDefects, Issue testcase, Schedule schedule, String cycleBreadCrumb, String contextPath, Boolean issuelinkEnabled, Boolean remoteIssueLinkEnabled, Long issueLinkTypeId, Long oldIssueLinkTypeId) {
		assert rilManager != null;
		for (Integer issueId : addedDefects) {
			addIndividualLink(testcase, schedule, contextPath, cycleBreadCrumb, issueId, issuelinkEnabled, remoteIssueLinkEnabled, issueLinkTypeId, oldIssueLinkTypeId);
		}
	}

	public void addIssueLinks(Collection<Integer> addedDefects, Issue testcase, Long issueLinkTypeId, Long oldIssueLinkTypeId, Boolean issuelinkEnabled) {
		if (issuelinkEnabled) {
			for (Integer issueId : addedDefects) {
				createRemoteLinkFromDefectBacktoTestcase(testcase.getId(), issueId.longValue(), issueLinkTypeId, oldIssueLinkTypeId);
			}
		}
	}

	public void addIssueLinks(Issue testcase, Long parentIssueId, Long issueLinkTypeId, Long oldIssueLinkTypeId, Boolean issuelinkEnabled) {
		createRemoteLinkFromDefectBacktoTestcase(testcase.getId(), parentIssueId, issueLinkTypeId, oldIssueLinkTypeId);
    }

	public void updateRemoteLinks(Collection<Integer> defectsToBeUpdate, Issue testcase, Schedule schedule, String cycleBreadCrumb, String contextPath, Boolean issuelinkEnabled, Boolean remoteIssueLinkEnabled) {
		if (defectsToBeUpdate == null)
			return;
		Long issueLinkTypeId = getLinkTypeId();
		Long oldIssueLinkTypeId = issueLinkTypeId;

		ExecutorService cachedPool = Executors.newCachedThreadPool();
		final ApplicationUser user = authContext.getLoggedInUser();
		Future<String> callableFuture = cachedPool.submit(() -> {
			if(authContext != null && authContext.getLoggedInUser() == null)
				authContext.setLoggedInUser(user);
			for (Integer issueId : defectsToBeUpdate) {
				try {
					updateIndividualRemoteLink(testcase, schedule, contextPath, cycleBreadCrumb, issueId, issuelinkEnabled, remoteIssueLinkEnabled, issueLinkTypeId, oldIssueLinkTypeId);
				} catch (UpdateException e) {
					log.fatal("Error in creating remote link for issueId " + issueId, e);
				}
			}
			return "";
		});
		cachedPool.shutdown();

	}

	private void removeIssueLinkIfAny(Long issueId, Long testCaseId, Long linkTypeId) {
		final IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager();
		try {
			IssueLink issueLink = issueLinkManager.getIssueLink(issueId, testCaseId, linkTypeId);
			if (null != issueLink)
				issueLinkManager.removeIssueLink(issueLink, authContext.getLoggedInUser());
		} catch (Exception e) {
			log.error("Error in removing defect link with Zephyr testcase. " + e.getMessage());
		}
	}

	private RemoteIssueLink createRemoteLinkObjectFromMap(Long remoteLinkId, Issue testcase, Schedule schedule, Long issueId, String cycleBreadCrumb, String contextPath) {
		String remoteLinkTitle = testcase.getKey() + " ";
		String status = null;
		if (StringUtils.isBlank(schedule.getStatus())) {
			status = JiraUtil.getExecutionStatuses().get(ApplicationConstants.UNEXECUTED_STATUS).getName();
		} else {
			status = JiraUtil.getExecutionStatuses().get(new Integer(schedule.getStatus())).getName();
		}
		RemoteIssueLink ril = new RemoteIssueLink(remoteLinkId, issueId,
				String.valueOf(schedule.getID()), //GlobalId
				remoteLinkTitle,
				(testcase.getSummary() + " (" + cycleBreadCrumb + ") " + StringUtils.upperCase(status)),
				contextPath + "/secure/ExecuteTest!default.jspa?scheduleId=" + schedule.getID(),
				contextPath + JiraUtil.buildIconURL("ico_zephyr_issuetype", ".png"),
				String.valueOf(ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("execute.test.execution.header.label")),
				"Affects test execution of",
				false/*resolved*/,
				null /*statusIconUrl*/,
				null /*"statusIconTitle"))*/,
				null /*statusIconLink*/,
				String.valueOf(ApplicationConstants.ZFJ_PLUGIN_KEY),
				ComponentAccessor.getPluginAccessor().getPlugin(ApplicationConstants.ZFJ_PLUGIN_KEY).getName());
		return ril;
	}

	/**
	 * @param testcase
	 * @param schedule
	 * @param contextPath
	 * @param issueId
	 */
	private void addIndividualLink(Issue testcase, Schedule schedule, String contextPath, String cycleBreadCrumb, Integer issueId,
								   Boolean issuelinkEnabled, Boolean remoteIssueLinkEnabled, Long issueLinkTypeId, Long oldIssueLinkTypeId) {
		try {
			if (issuelinkEnabled)
				createRemoteLinkFromDefectBacktoTestcase(testcase.getId(), issueId.longValue(), issueLinkTypeId, oldIssueLinkTypeId);
			if (remoteIssueLinkEnabled) {
				RemoteIssueLink ril = createRemoteLinkObjectFromMap(null, testcase, schedule, new Long(issueId), cycleBreadCrumb, contextPath);
				rilManager.createRemoteIssueLink(ril, authContext.getLoggedInUser());
			}

		} catch (CreateException e) {
			log.fatal("Error in creating remote link for issueId " + issueId + "\n" + e.getMessage());
		}
	}

	/**
	 * Updates JIRA Property DB
	 * @param propertyName
	 * @param propertyValue
	 */
	private void updateDBConfigProperty(String propertyName, Long propertyValue) {
		try {
			JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
					.setLong(propertyName, propertyValue);
		} catch (Exception e) {
			log.error("Error updating DB property : " + propertyName + " : " + e.getMessage());
		}
	}


	public static Long getLinkTypeId() {
		return (Long) JiraUtil.getSimpleDBProperty(ConfigurationConstants.ZEPHYR_ISSUE_LINK_RELATION, 0L);
	}

	/**
	 * Remove defects from schedule.
	 * @param deletedDefects
	 * @param scheduleId
     */
	public void removeRemoteLinks(Collection<Integer> deletedDefects, String scheduleId) {
		for (Integer issueId : deletedDefects) {
			Issue issue = issueManager.getIssueObject(new Long(issueId));
			if (null != issue)
				rilManager.removeRemoteIssueLinkByGlobalId(issue, scheduleId, authContext.getLoggedInUser());
		}
	}

	/**
	 * Remove Issue Linkinking
	 * @param deletedDefects
	 * @param testcase
	 * @param linkTypeId
     */
	public void removeIssueLinks(Collection<Integer> deletedDefects, Issue testcase, Long linkTypeId) {
		for (Integer issueId : deletedDefects) {
			Issue issue = issueManager.getIssueObject(new Long(issueId));
			if (null != issue)
				removeIssueLinkIfAny(issue.getId(), testcase.getId(), linkTypeId);

		}
	}

	public void reIndexSchedule(ScheduleIdsScheduleIterable schedules, Context context, String jobProgressToken) throws IndexException {
        scheduleIndexManager.reIndexSchedule(schedules, context,jobProgressToken);
	}

	public long reIndexSchedulesInBatch(ScheduleIdsScheduleIterable scheduleIds, Context context, String jobProgressToken) {
		long result =  scheduleIndexManager.reIndexByProject(scheduleIds, context, jobProgressToken,true);
		return result;
	}

    /*
    * Commented as part of ZFJ-2445
    public long performZephyrTestCaseLinksResetAsync(Long issueLinkTypeId, Long oldIssueLinkTypeId, Map<String, String> issueTypeMap) {
        final long startTime = System.currentTimeMillis();
        log.debug("Request received for reset zephyr issue links.");
        log.debug("Resetting zephyr issue links from oldIssueLinkTypeId : " + oldIssueLinkTypeId + " to " + issueLinkTypeId);

        final IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager();
        JqlClauseBuilder builder = JqlQueryBuilder.newClauseBuilder();
        List<Long> list = new ArrayList<>();


        if (MapUtils.isNotEmpty(issueTypeMap)) {
            for (String string : issueTypeMap.keySet()) {
                list.add(Long.parseLong(string));
            }
            builder.addNumberCondition("issuetype", Operator.IN, list);
            findIssueIdsAndResetZephyrTestLinks(builder.buildQuery(), issueLinkTypeId, oldIssueLinkTypeId, issueLinkManager);
        } else {
            log.error("No selected value received for Non Zephyr IssueType Test to reset issue links with Zephyr test.");
        }

        return System.currentTimeMillis() - startTime;
    }

    private void findIssueIdsAndResetZephyrTestLinks(Query query, Long issueLinkTypeId, Long oldIssueLinkTypeId, IssueLinkManager issueLinkManager) {
        List<Long> issueIds = null;
        try {
            issueIds = getIssueIdsByJQLQuery(query);
            for (Long issueId : issueIds) {
                JqlClauseBuilder jql = JqlQueryBuilder.newClauseBuilder();
                jql.issueType().eq().string(JiraUtil.getTestcaseIssueTypeId()).and().issue().in().functionLinkedIssues(String.valueOf(issueId));
                List<Long> linkedIssueIds = getIssueIdsByJQLQuery(jql.buildQuery());
                resetZephyrTestIssueLink(issueId, linkedIssueIds, issueLinkTypeId, oldIssueLinkTypeId, issueLinkManager);
            }
        } catch (SearchException e) {
            log.error("Error occurred while searching for non issue type test.");
        } catch (CreateException e) {
            log.error("Error occurred while searching for zephyr test links.");
        }
    }

    private List<Long> getIssueIdsByJQLQuery(Query query) throws SearchException {
        SearchResults searchResults = searchProvider.search(query, authContext.getLoggedInUser(), new PagerFilter().getUnlimitedFilter());
        List<Issue> issues = searchResults.getIssues();
        List<Long> issueIds = CollectionUtil.transform(issues, new Function<Issue, Long>() {
            @Override
            public Long get(final Issue issue) {
                return issue.getId();
            }
        });
        return issueIds;
    }

    private void resetZephyrTestIssueLink(Long issueId, List<Long> linkedIssueIds, Long issueLinkTypeId, Long oldIssueLinkTypeId, IssueLinkManager issueLinkManager) throws CreateException {
	    boolean flag = JiraUtil.isTestToIssueLinkingEnabled();
	    if(CollectionUtils.isNotEmpty(linkedIssueIds)) {
            for (Long testCaseId : linkedIssueIds) {
                IssueLink issueLink = issueLinkManager.getIssueLink(issueId, testCaseId, oldIssueLinkTypeId);
                if (null != issueLink) {
                    issueLinkManager.removeIssueLink(issueLink, authContext.getLoggedInUser());
                    if (flag) {
                        removeIssueLinkIfAny(testCaseId, issueId, oldIssueLinkTypeId); // for reset
                        issueLinkManager.createIssueLink(testCaseId, issueId, issueLinkTypeId, 0l, authContext.getLoggedInUser());
                    } else {
                        removeIssueLinkIfAny(testCaseId, issueId, oldIssueLinkTypeId); // for inversed links
                        issueLinkManager.createIssueLink(issueId, testCaseId, issueLinkTypeId, 0l, authContext.getLoggedInUser());
                    }
                }

            }
        }
    }*/


	public static String getDurationStringSecondsInHours(long l) {
		if (l == 0) {
			return "0h";
		}

		StringBuilder result = new StringBuilder();

		if (l >= Duration.HOUR.getSeconds()) {
			result.append((l / Duration.HOUR.getSeconds()));
			result.append("h ");
			l = l % Duration.HOUR.getSeconds();
		}

		return result.toString().trim();
	}

}
