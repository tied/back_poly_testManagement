package com.thed.zephyr.je.service.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.database.DatabaseConfig;
import com.atlassian.jira.config.database.DatabaseConfigurationManager;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.link.RemoteIssueLinkManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.usercompatibility.UserCompatibilityHelper;
import com.atlassian.jira.util.Function;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.collect.CollectionUtil;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.base.Optional;
import com.google.common.collect.*;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.Gson;
import com.thed.zephyr.je.config.license.PluginUtils;
import com.thed.zephyr.je.config.model.ExecutionStatus;
import com.thed.zephyr.je.event.EventType;
import com.thed.zephyr.je.event.ScheduleModifyEvent;
import com.thed.zephyr.je.model.*;
import com.thed.zephyr.je.permissions.model.PermissionType;
import com.thed.zephyr.je.permissions.service.ZephyrPermissionManager;
import com.thed.zephyr.je.rest.CustomFieldValueResource.CustomFieldValueRequest;
import com.thed.zephyr.je.rest.delegate.CustomFieldValueResourceDelegate;
import com.thed.zephyr.je.service.*;
import com.thed.zephyr.je.vo.ExecutionSummaryImpl;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.ZCollectionUtils;
import com.thed.zephyr.util.ZephyrComponentAccessor;
import net.java.ao.Query;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.ofbiz.core.entity.jdbc.SQLProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.thed.zephyr.util.ApplicationConstants.MSSQL_DB;

import com.atlassian.crowd.embedded.api.User;

public class ScheduleManagerImpl extends BaseManagerImpl implements ScheduleManager {

	private static final Logger log = LoggerFactory.getLogger(ScheduleManagerImpl.class);
    private static final String DATABASE_TYPE = "Database type";
    private CycleManager cycleManager;
	private final UserManager userManager;
	private final ProjectManager projectManager;
	private final AttachmentManager attachmentManager;
	private final StepResultManager stepResultManager;
	private final TeststepManager testStepManager;
	private final IssueManager issueManager;
	private final ZephyrPermissionManager zephyrPermissionManager;
	private final JobProgressService jobProgressService;
	private final JiraAuthenticationContext authContext;
	private final FolderManager folderManager;
	private final CustomFieldValueManager customFieldValueManager;
	private final ZephyrCustomFieldManager zephyrCustomFieldManager;
	private final EventPublisher eventPublisher;

	public ScheduleManagerImpl(ActiveObjects ao, 
								CycleManager cycleManager, 
								UserManager userManager, 
								AttachmentManager attachmentManager,
								ProjectManager projectManager,
								StepResultManager stepResultManager,
								TeststepManager testStepManager,
								IssueManager issueManager,
								ZephyrPermissionManager zephyrPermissionManager,
							   JobProgressService jobProgressService,
							   JiraAuthenticationContext authContext,
							   FolderManager folderManager,
							   CustomFieldValueManager customFieldValueManager,
							   ZephyrCustomFieldManager zephyrCustomFieldManager,
							   EventPublisher eventPublisher) {
		super(checkNotNull(ao));
		this.cycleManager = cycleManager;
		this.userManager = userManager;
		this.attachmentManager = attachmentManager;
		this.projectManager=projectManager;
		this.stepResultManager = stepResultManager;
		this.testStepManager=testStepManager;
		this.issueManager=issueManager;
		this.zephyrPermissionManager=zephyrPermissionManager;
		this.jobProgressService = jobProgressService;
		this.authContext=authContext;
		this.folderManager=folderManager;
		this.customFieldValueManager=customFieldValueManager;
		this.zephyrCustomFieldManager=zephyrCustomFieldManager;
		this.eventPublisher=eventPublisher;
	}

	@Override
	public List<Schedule> getSchedules(Schedule schedule, Integer offset, Integer limit) {
		if(limit == null) limit = 50;
		if(offset == null) offset = 0;
		Query query = Query.select("MODIFIED_BY,ISSUE_ID,PROJECT_ID,COMMENT,ACTUAL_EXECUTION_TIME,STATUS,DATE_CREATED,EXECUTED_ON,CREATED_BY,ORDER_ID,ASSIGNED_TO,EXECUTED_BY,VERSION_ID");
		if(limit != -1) query.setLimit(limit);
		if(offset != -1) query.setOffset(offset);

		Schedule[] entities = ao.find(Schedule.class, query);
		return Arrays.asList(entities);
	}
	
	@Override
	public List<ScheduleDefect> getScheduleDefects(Integer scheduleId) {
		ScheduleDefect[] scheduleDefectArray = ao.find(ScheduleDefect.class, Query.select().where("SCHEDULE_ID = ?", scheduleId));
		return Arrays.asList(scheduleDefectArray);
	}

	@Override
	public List<Schedule> getSchedulesByDefectId(Integer defectId, Boolean includeStepResult) {
		ScheduleDefect[] scheduleDefectArray = ao.find(ScheduleDefect.class, Query.select("SCHEDULE_ID").where("DEFECT_ID = ?", defectId).distinct());
        StepDefect[] stepDefectArray = new StepDefect[0];
        if(includeStepResult){
            stepDefectArray = ao.find(StepDefect.class, Query.select("SCHEDULE_ID").where("DEFECT_ID = ?", defectId).distinct());
        }
        Iterable<Integer> scheduleIds = new ImmutableSet.Builder<Integer>().addAll(Iterables.transform(Arrays.asList(scheduleDefectArray), new com.google.common.base.Function<ScheduleDefect, Integer>() {
            @Override
            public Integer apply(@Nullable ScheduleDefect o) {
                return o.getScheduleId();
            }
        }))
        .addAll(Iterables.transform(Arrays.asList(stepDefectArray), new com.google.common.base.Function<StepDefect, Integer>() {
            @Override
            public Integer apply(@Nullable StepDefect o) {
                return o.getScheduleId();
            }
        }))
        .build();
		List<Schedule> schedules = new ArrayList<Schedule>();
		for(Integer schId : scheduleIds) {
			schedules.add(getSchedule(schId));
		}
		return schedules;
	}
	
	@Override
	public List<Schedule> getSchedulesByDefectId(Integer defectId, Boolean includeStepResult, Integer offset, Integer maxResult) {
		Query query = Query.select("SCHEDULE_ID").where("DEFECT_ID = ?", defectId);
		int limit = 10;
		if(maxResult != null && maxResult.intValue() != 0) {
			limit = maxResult;
		}
		//If Offset comes as -1, no pagination
		if(offset == null || offset == -1) {
			limit = -1;
			if(offset == null){
				offset = -1;
			}
		}

		query.setOffset(offset);
		query.setLimit(limit);
		ScheduleDefect[] scheduleDefectArray = ao.find(ScheduleDefect.class,query.distinct());
        StepDefect[] stepDefectArray = new StepDefect[0];
        if(includeStepResult){
            stepDefectArray = ao.find(StepDefect.class, Query.select("SCHEDULE_ID").where("DEFECT_ID = ?", defectId).distinct());
        }
        Iterable<Integer> scheduleIds = new ImmutableSet.Builder<Integer>().addAll(Iterables.transform(Arrays.asList(scheduleDefectArray), new com.google.common.base.Function<ScheduleDefect, Integer>() {
            @Override
            public Integer apply(@Nullable ScheduleDefect o) {
                return o.getScheduleId();
            }
        }))
        .addAll(Iterables.transform(Arrays.asList(stepDefectArray), new com.google.common.base.Function<StepDefect, Integer>() {
            @Override
            public Integer apply(@Nullable StepDefect o) {
                return o.getScheduleId();
            }
        }))
        .build();
		List<Schedule> schedules = new ArrayList<Schedule>();
		for(Integer schId : scheduleIds) {
			schedules.add(getSchedule(schId));
		}
		return schedules;
	}
	
	@Override
	public Map<String,Object> getTestAndStepSchedulesByDefectId(Integer defectId,
			Integer offset, Integer maxResult) {
		Map<String,Object> response = new HashMap<String, Object>();
		Query query = Query.select("SCHEDULE_ID").where("DEFECT_ID = ?", defectId);
		int limit = 10;
		if(maxResult != null && maxResult.intValue() != 0) {
			limit = maxResult;
		}
		//If Offset comes as -1, no pagination
		if(offset == null || offset == -1) {
			offset = 0;
		}
		ScheduleDefect[] scheduleDefectArray = ao.find(ScheduleDefect.class,query.distinct());
        StepDefect[] stepDefectArray = ao.find(StepDefect.class, Query.select("SCHEDULE_ID").where("DEFECT_ID = ?", defectId).distinct());
	    Set<Integer> scheduleIds = CollectionUtil.transformSet(Sets.newHashSet(scheduleDefectArray), new Function<ScheduleDefect, Integer>(){
	        @Override
			public Integer get(final ScheduleDefect scheduleDefect){
	            return scheduleDefect.getScheduleId();
	        }
	    });
	    
	    Set<Integer> stepScheduleIds =  CollectionUtil.transformSet(Sets.newHashSet(stepDefectArray), new Function<StepDefect, Integer>(){
	        @Override
			public Integer get(final StepDefect stepDefect){
	            return stepDefect.getScheduleId();
	        }
	    });

	    List<Integer> scheduleList = JiraUtil.safe(new ArrayList<Integer>(scheduleIds));
	    scheduleList.removeAll(stepScheduleIds);
	    scheduleList.addAll(JiraUtil.safe(new ArrayList<Integer>(stepScheduleIds)));
		List<Schedule> schedules = new ArrayList<Schedule>();
		int totalCount = scheduleList.size();
		try {
			if(offset > scheduleList.size()) {
				response.put("schedules", schedules);
				response.put("totalCount", scheduleList.size());
				return response;
			} else if(scheduleList.size() >= offset + limit) {
				scheduleList = scheduleList.subList(offset, offset+limit);
			} else if(scheduleList.size() < offset + limit) {
				scheduleList = scheduleList.subList(offset, scheduleList.size());
			}
			Collections.sort(scheduleList);
			for(Integer schId : scheduleList) {
				schedules.add(getSchedule(schId));
			}
		} catch (Exception e) {
			log.error("Error retrieving executionIds. Maybe the executions might need reIndexing ",e);
		}
		response.put("schedules", schedules);
		response.put("totalCount", totalCount);
		return response;
	}
	
	@Override
	public Integer getScheduleCountByDefectId(Integer defectId) {
		Integer scheduleCount =  ao.count(ScheduleDefect.class, Query.select().where("DEFECT_ID = ?", defectId));
		return scheduleCount;
	}
	
	@Override
	public Integer getScheduleCountByStepDefectId(Integer stepDefectId) {
		Integer stepDefectCount =  ao.count(StepDefect.class, Query.select().where("DEFECT_ID = ?", stepDefectId));
		return stepDefectCount;
	}
	
	@Override
	public Schedule getSchedule(Integer id) {
		Schedule [] schedules =  ao.find(Schedule.class, Query.select("MODIFIED_BY, ISSUE_ID, PROJECT_ID, COMMENT, ACTUAL_EXECUTION_TIME, STATUS, DATE_CREATED, EXECUTED_ON, CREATED_BY, CYCLE_ID, ORDER_ID, ASSIGNED_TO, EXECUTED_BY, VERSION_ID").where("ID = ?", id));
		if(schedules != null && schedules.length > 0){
			return schedules[0];
		}
		return null;
	}

	@Override
	public List<Schedule> searchSchedules(Map<String, Object> filters){
		List<String> whereClauses = new ArrayList<String>();
		if(filters == null)
			return new ArrayList<Schedule>();
		/*Prepare where clause*/
		List<Object> params = new ArrayList<Object>();
		if(filters.containsKey("cid")) {
			Integer[] cycles = (Integer[]) filters.get("cid");
			if(cycles != null){
				//if(cycles.length == 1 && Long.parseLong(cycles[0]) == CycleManager.ANY){}//Deal with ANY}
				String ques[] = new String[cycles.length];
				for(int i=0; i< cycles.length; i++){
					ques[i] = "?";
				}
				//This is a hack to get around AO's limitation of being able to pass only one object per query
				whereClauses.add(" CYCLE_ID IN ( " + StringUtils.join(ques, ',') + " ) ");
				params.addAll(Arrays.asList(cycles));
				Integer issueId = (Integer) filters.get("issueId");
				if(issueId != null) {
					whereClauses.add(" ISSUE_ID = ?");
					params.add(issueId);
				}
				Integer versionId = (Integer) filters.get("vid");
				if(versionId != null) {
					whereClauses.add(" VERSION_ID = ?");
					params.add(versionId);
				}
			} else if(filters.containsKey("pid") && filters.get("pid") != null){
				whereClauses.add(" PROJECT_ID = ?");
				whereClauses.add(" VERSION_ID = ?");
				whereClauses.add(" CYCLE_ID is NULL ");
				Integer versionId = (Integer) filters.get("vid");
				Integer projectId = (Integer) filters.get("pid");
				params.add(projectId);
				params.add(versionId);
				Integer issueId = (Integer) filters.get("issueId");
				if(issueId != null) {
					whereClauses.add(" ISSUE_ID = ?");
					params.add(issueId);
				}
			}
			if(filters.containsKey("folderId")) {
				if(filters.get("folderId") != null) {
					whereClauses.add(" FOLDER_ID = ? ");
					params.add(filters.get("folderId"));
				} else {
					whereClauses.add(" FOLDER_ID IS NULL");
				}
			}
		} else if(filters.containsKey("pid") && filters.get("pid") != null) {
			Integer projectId = (Integer) filters.get("pid");
			Integer issueId = (Integer) filters.get("issueId");
			//This is a heck to get around AO's limitation of being able to pass only one object per query
			whereClauses.add(" PROJECT_ID = ?");
			params.add(projectId);
			if(issueId != null) {
				whereClauses.add(" ISSUE_ID = ?");
				params.add(issueId);
			}
			Integer versionId = (Integer) filters.get("vid");
			if(versionId != null) {
				whereClauses.add(" VERSION_ID = ?");
				params.add(versionId);
			}
		}
		
		Schedule [] schedules = null;
		Query query = Query.select("MODIFIED_BY,ISSUE_ID,PROJECT_ID,COMMENT,ACTUAL_EXECUTION_TIME,STATUS,DATE_CREATED,EXECUTED_ON,CREATED_BY,ORDER_ID,ASSIGNED_TO,EXECUTED_BY,VERSION_ID,FOLDER_ID");
		if(filters.containsKey("size"))
			query.setLimit((Integer) filters.get("size"));
		if(filters.containsKey("offset"))
			query.setOffset((Integer) filters.get("offset"));
		if(whereClauses.size() >0){
			query = Query.select().where(StringUtils.join(whereClauses, " and "), params.toArray());
			schedules =  ao.find(Schedule.class, query);
		}else{
			schedules =  ao.find(Schedule.class, query);
		}
		return Arrays.asList(schedules);
	}

	@Override
	public List<Schedule> getSchedulesByCycleId(final Long versionId, final Long projectId, final Integer cycleId, Integer offset, final String sortQuery, final String expandos) {
		int limit = 10;
		String finalOrderQuery = getOrderQueryFroSchedule(sortQuery);

		//If Offset comes as -1, no pagination
		if(offset == null || offset == -1) {
			offset = -1;
			limit = -1;
		}
        // if expandos == reorderId, change the limit to 200
        if(StringUtils.isNotEmpty(expandos) && expandos.equals("reorderId"))
            limit = 200;

		Schedule [] schedules = null;
		Query query = Query.select("MODIFIED_BY,ISSUE_ID,PROJECT_ID,COMMENT,ACTUAL_EXECUTION_TIME,STATUS,DATE_CREATED,EXECUTED_ON,CREATED_BY,ORDER_ID,ASSIGNED_TO,EXECUTED_BY,VERSION_ID,FOLDER_ID");
		if(cycleId == null || cycleId.intValue() == -1){
			query.where("PROJECT_ID = ? AND VERSION_ID = ? AND CYCLE_ID is NULL", projectId, versionId);
		} else {
			query.where("CYCLE_ID = ?", cycleId);
		}
		schedules =  ao.find(Schedule.class, query.order(finalOrderQuery).offset(offset).limit(limit));
		return Arrays.asList(schedules);
	}
	
	@Override
	public Integer getSchedulesCount(final Long versionId, final Long projectId, final Integer cycleId, Long folderId) {
		Integer schedules = null;
		if(cycleId == null || cycleId.intValue() == -1){
			schedules =  ao.count(Schedule.class, Query.select().where("PROJECT_ID = ? AND VERSION_ID = ? AND CYCLE_ID IS NULL", projectId, versionId));
		} else {
			Query query = Query.select();
			if(folderId != null && !folderId.equals(-1l)) {
				query.where("CYCLE_ID = ? AND FOLDER_ID = ? ", cycleId, folderId);
			} else {
				query.where("CYCLE_ID = ? AND FOLDER_ID IS NULL", cycleId);
			}
			schedules =  ao.count(Schedule.class, query);
		}
		return schedules;
	}
	
	@Override
	public Integer getSchedulesCountByIssueId(final Integer issueId) {
		Integer schedules =  ao.count(Schedule.class, Query.select().where("ISSUE_ID = ?", issueId));
		return schedules;
	}

	
	@Override
	public List<Schedule> getSchedulesByIssueId(final Integer issueId, Integer offset, Integer maxResult) {
		int limit = 11;
		if(maxResult != null && maxResult != 0) {
			limit = maxResult;
		}
		//If Offset comes as -1, no pagination
		if(offset == null || offset == -1) {
			limit = -1;
			
			if(offset == null){
				offset = -1;
			}
		}

		Schedule [] schedules =  ao.find(Schedule.class, Query.select("MODIFIED_BY,ISSUE_ID,PROJECT_ID,COMMENT,ACTUAL_EXECUTION_TIME,STATUS,DATE_CREATED,EXECUTED_ON,CREATED_BY,ORDER_ID,ASSIGNED_TO,EXECUTED_BY,VERSION_ID").where("ISSUE_ID = ? and ID != -1", issueId).order(" DATE_CREATED DESC ").offset(offset).limit(limit));
		return Arrays.asList(schedules);
	}
	
	@Override
	public Schedule getSchedulesByIssueIdAndCycleId(final Integer issueId, final Integer cycleId, Integer offset){
		int limit = 11;

		//If Offset comes as -1, no pagination
		if(offset == null || offset == -1) {
			limit = -1;
			
			if(offset == null){
				offset = -1;
			}
		}

		Schedule [] schedules =  ao.find(Schedule.class, Query.select("MODIFIED_BY,ISSUE_ID,PROJECT_ID,COMMENT,ACTUAL_EXECUTION_TIME,STATUS,DATE_CREATED,EXECUTED_ON,CREATED_BY,ORDER_ID,ASSIGNED_TO,EXECUTED_BY,VERSION_ID").where("ISSUE_ID = ? and CYCLE_ID = ?", issueId, cycleId).order(" DATE_CREATED DESC ").offset(offset).limit(limit) );
		if(schedules != null && schedules.length >0){
			return schedules[0];
		}
		return null;
	}
	public Schedule getSchedulesByIssueIdAndCycleIdAndFolderId(final Integer projectId, final Integer versionId, final Integer issueId, final Integer cycleId, final Long folderId,
			Integer offset) {
		int limit = 11;
		/*
		* Commented for ZFJ-2678
		if(folderId==null||folderId<1) {
			return getSchedulesByIssueIdAndCycleId(issueId, cycleId, offset);
		}*/

		// If Offset comes as -1, no pagination
		if (offset == null || offset == -1) {
			limit = -1;

			if (offset == null) {
				offset = -1;
			}
		}
		Query query = Query.select();
		if(cycleId != null && !cycleId.equals(ApplicationConstants.AD_HOC_CYCLE_ID_LONG)) {
			if(folderId != null && !folderId.equals(ApplicationConstants.ADHOC_SYSTEM_FOLDER_ID)) {
				query.where("ISSUE_ID = ? and CYCLE_ID = ? and FOLDER_ID = ?", issueId, cycleId, folderId);
			} else {
				query.where("ISSUE_ID = ? and CYCLE_ID = ? and FOLDER_ID IS NULL", issueId, cycleId);
			}
		} else {
			if(folderId != null && !folderId.equals(ApplicationConstants.ADHOC_SYSTEM_FOLDER_ID)) {
				query.where("PROJECT_ID = ? AND VERSION_ID = ? AND ISSUE_ID = ? and CYCLE_ID IS NULL and FOLDER_ID = ?", projectId, versionId, issueId, cycleId, folderId);
			} else {
				query.where("PROJECT_ID = ? AND VERSION_ID = ? AND ISSUE_ID = ? and CYCLE_ID IS NULL and FOLDER_ID IS NULL", projectId, versionId, issueId, cycleId);
			}
		}		
		query.order(" DATE_CREATED DESC ").offset(offset).limit(limit);
		Schedule[] schedules = ao.find(Schedule.class, query);
		if (schedules != null && schedules.length > 0) {
			return schedules[0];
		}
		return null;
	}
	@Override
	public Schedule getSchedulesByIssueIdAndProjectId(final Integer issueId, final Integer projectId){
		Schedule [] schedules =  ao.find(Schedule.class, Query.select("MODIFIED_BY,ISSUE_ID,PROJECT_ID,COMMENT,ACTUAL_EXECUTION_TIME,STATUS,DATE_CREATED,EXECUTED_ON,CREATED_BY,ORDER_ID,ASSIGNED_TO,EXECUTED_BY,VERSION_ID").where("ISSUE_ID = ? and PROJECT_ID = ?", issueId, projectId));
		if(schedules != null && schedules.length >0){
			return schedules[0];
		}
		return null;
	}
	

	@Override
	public List<Schedule> getSchedulesByExecutionStatus(String statusId) {
		Schedule [] schedules =  ao.find(Schedule.class, Query.select("MODIFIED_BY,ISSUE_ID,PROJECT_ID,COMMENT,ACTUAL_EXECUTION_TIME,STATUS,DATE_CREATED,EXECUTED_ON,CREATED_BY,ORDER_ID,ASSIGNED_TO,EXECUTED_BY,VERSION_ID").where("STATUS = ?", String.valueOf(statusId)));
		return Arrays.asList(schedules);
	}
	
	@Override
	public Integer getTestcaseExecutionCount(Long versionId, Long projectId){
		Query query = null;
		if(versionId == null){
			query = Query.select("ID").where("PROJECT_ID = ? AND STATUS IS NOT NULL AND STATUS != ?", projectId, String.valueOf(ApplicationConstants.UNEXECUTED_STATUS));
		}else{
			query = Query.select("ID").where("VERSION_ID = ? AND PROJECT_ID = ? AND STATUS IS NOT NULL AND STATUS != ?", versionId, projectId, String.valueOf(ApplicationConstants.UNEXECUTED_STATUS));
		}
		return ao.count(Schedule.class, query);
	}

	
	@Override
	public Set<Map<String, Object>>  getExecutionSummaryGroupedByCycle(Long versionId, Long projectId) {
		Set<Map<String, Object>> statusCntByCycle= new LinkedHashSet<Map<String, Object>>();
		List<Cycle> cycles = cycleManager.getCyclesByVersion(versionId, projectId, -1);
		final Set<Entry<Integer, ExecutionStatus>> statuses = JiraUtil.getExecutionStatuses().entrySet();
		for(Cycle cycle : cycles){
			Map<String, Object> countByStatus = getExecutionDetailsByCycle(statuses, versionId, projectId, cycle.getID());
			Map<String, Object> cycleMap = getExecutionSummaryMap(cycle.getID(), cycle.getName(), countByStatus);
			statusCntByCycle.add(cycleMap);
		}
		//Lets count for AdHoc Cycle (either Project or Version)
		Map<String, Object> countByStatus = getExecutionDetailsByCycle(statuses, versionId, projectId, null);
		/*TODO Adhoc - Move it to User Preference*/
		I18nHelper i18n = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
		Map<String, Object> cycleMap = getExecutionSummaryMap(-1l, i18n.getText("zephyr.je.cycle.adhoc"), countByStatus);

		statusCntByCycle.add(cycleMap);
		return statusCntByCycle;
	}

	/**
	 * @param countByStatus
	 * @return
	 */
	public Map<String, Object> getExecutionSummaryMap(Object id, String name, Map<String, Object> countByStatus) {
		Map<String, Object> cycleMap = new HashMap<String, Object>();
		cycleMap.put("id", id);
		cycleMap.put("name", name);
		cycleMap.put("cnt", countByStatus);
		return cycleMap;
	}

	@Override
	public Map<String, Object> getExecutionSummaryByIssueIds(Collection<Long> issueIds, Long versionId){
		DatabaseConfig dbConfig = ComponentAccessor.getComponent(DatabaseConfigurationManager.class).getDatabaseConfiguration();
		List<String> statusList = JiraUtil.getExecutionStatuses().entrySet().stream().map(status -> String.valueOf(status.getKey())).collect(Collectors.toList());
		SQLProcessor sqlProcessor = null;
		String sqlQuery = null;
		int count = 0;
		StringBuffer innerSQL = new StringBuffer();
		Map<String, Object> countByStatus = new LinkedHashMap<String, Object>();
		final AtomicInteger statCount = new AtomicInteger(statusList.size());
		if(issueIds == null || issueIds.size() <= 0 || versionId == null) {
			return countByStatus;
		}
		List<Long> issueList = issueIds.stream().collect(Collectors.toList());
		buildSumOfExecutionStatusQuery(dbConfig, statusList, innerSQL, statCount);
		try {
			sqlProcessor = new SQLProcessor("defaultDS");
			do {
				List<Long> subList = issueList.size() > count + ApplicationConstants.MAX_IN_QUERY ? issueList.subList(count, count + ApplicationConstants.MAX_IN_QUERY) : issueList;

				sqlQuery = "SELECT ISSUE_ID," + innerSQL.toString() + " from AO_7DEABF_SCHEDULE WHERE VERSION_ID = " + versionId + " AND ISSUE_ID IN (" + StringUtils.join(subList, ",") + " ) group by ISSUE_ID";
				if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), ApplicationConstants.POSTGRES_DB)) {
					sqlQuery = "SELECT \"ISSUE_ID\"," + innerSQL.toString() + " from \"AO_7DEABF_SCHEDULE\" WHERE \"VERSION_ID\" = " + versionId + " AND \"ISSUE_ID\" IN (" + StringUtils.join(subList, ",") + " ) group by \"ISSUE_ID\"";
				} else if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), ApplicationConstants.MSSQL_DB)) {
					if(dbConfig.getSchemaName() != null && !StringUtils.equalsIgnoreCase(dbConfig.getSchemaName(),"public")) {
						sqlQuery = "SELECT ISSUE_ID," + innerSQL.toString() + " from " + dbConfig.getSchemaName() + "." + "AO_7DEABF_SCHEDULE WHERE VERSION_ID = " + versionId + " AND ISSUE_ID IN (" + StringUtils.join(subList, ",") + " ) group by ISSUE_ID";
					}
				}
				ResultSet resultSet = null;
				executeIssueGroupBySqlQuery(statusList, sqlProcessor, sqlQuery, countByStatus, resultSet);
				count += subList.size();
			} while (count < issueList.size());
		} catch (Exception ex) {
			log.error("Error while executing the query - " + sqlQuery);
		} finally {
			try {
				if (sqlProcessor != null)
					sqlProcessor.close();
			} catch (Exception ex) {
				log.error("Error while closing the sql processor connection ");
			}
		}
		return countByStatus;
	}
	
	@Override
	public List<ExecutionSummaryImpl> getExecutionDetailsByCycle(Long versionId, Long projectId, Integer cycleId, String userName) {
		final Set<Entry<Integer, ExecutionStatus>> statuses = JiraUtil.getExecutionStatuses().entrySet();
		return getExecutionDetailsByCurrentUserAndCycle(statuses, versionId, projectId, cycleId, userName);
	}

	/**
	 * Fetches count for each user, returns null if user has no execution in given project/version
	 * @param statuses
	 * @param userName
	 * @return
	 */
	private Map<String, Object> getExecutionDetailsByExecutor(final Set<Entry<Integer, ExecutionStatus>> statuses, Long versionId, Long projectId, String userName) {
		Map<String, Object> countByStatus = new HashMap<String, Object>();
		int total = 0;
		for(Entry<Integer, ExecutionStatus> statusEntry : statuses){
			/*Not counting unexecuted ones*/
			if(statusEntry.getKey() == -1) 
				continue;
			String statusKey = statusEntry.getKey().toString();
			Integer cnt = ao.count(Schedule.class, Query.select().where("PROJECT_ID = ? AND VERSION_ID = ? AND EXECUTED_BY = ? AND STATUS = ?",
					projectId, versionId, userName, statusKey));
			if(cnt == null) cnt = new Integer(0);
			countByStatus.put(statusKey, cnt);
			total += cnt;
		}
		if(total > 0)
			return countByStatus;
		else
			return null;
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
		for(Entry<Integer, ExecutionStatus> statusEntry : statuses){
			Integer cnt = 0;
			String statusKey = statusEntry.getKey().toString();
			if(cycleId == null){
				cnt = ao.count(Schedule.class, Query.select().where("PROJECT_ID = ? AND VERSION_ID = ? AND CYCLE_ID IS NULL AND STATUS = ?", 
						projectId, versionId, statusKey));
			}else
				cnt = ao.count(Schedule.class, Query.select().where("PROJECT_ID = ? AND VERSION_ID = ? AND CYCLE_ID = ? AND STATUS = ?", 
					projectId, versionId, cycleId, statusKey));
			if(cnt == null) cnt = new Integer(0);
			countByStatus.put(statusKey, cnt);
		}
		return countByStatus;
	}
	
	/**
	 * returns all Execution Details. If UserName is null return all ExecutionDetails 
	 * @param statuses
	 * @param cycleId
	 * @param userName
	 * @return
	 */
	
	private List<ExecutionSummaryImpl> getExecutionDetailsByCurrentUserAndCycle(Set<Entry<Integer, ExecutionStatus>> statuses,
    		Long versionId, Long projectId, Integer cycleId, String userName) {
		List<ExecutionSummaryImpl> summaryList = new ArrayList<ExecutionSummaryImpl>();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<ExecutionSummaryImpl>> responseList = new ArrayList<>();

        for(Entry<Integer, ExecutionStatus> statusEntry : statuses) {
            Integer key = statusEntry.getKey();
            ExecutionStatus executionStatus = statusEntry.getValue();
            Future<ExecutionSummaryImpl> executionSummary = executor.submit(new ProcessExecutionSummaryCount(key+"",executionStatus,userName,
                    projectId,versionId,cycleId));
            responseList.add(executionSummary);
        }
        executor.shutdown();

        try {
            while (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.debug("Awaiting completion of threads.");
            }
        } catch (InterruptedException ex) {
            log.error("Interrupted exception occurred.", ex);
        }

        responseList.forEach(future -> {
            try {
                summaryList.add(future.get());
            } catch (InterruptedException e) {

            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        });

		return summaryList;		
	}


	@Override
	public Schedule saveSchedule(Map<String, Object> scheduleProperties) {
		if(scheduleProperties == null){
			throw new RuntimeException("Unable to create schedule with empty data");
		}
		if(!scheduleProperties.containsKey("PROJECT_ID")){
			/*Shall we add a default cycle?*/
			throw new RuntimeException("Unable to create schedule without project");
		}
		return ao.create(Schedule.class, scheduleProperties);
	}

	@Override
	public int removeSchedule(Integer id) {
		String jobProgressToken="";
		Schedule sch = ao.get(Schedule.class, id);
		if(sch != null){
			return deleteSchedules(new Schedule[]{sch}, jobProgressToken);
		}
		return 0;
	}
	
	@Override
	public int removeSchedules(final  Long testcaseId) {
		String jobProgressToken = "";
		final Schedule[] schedules = ao.find(Schedule.class, Query.select().where("ISSUE_ID = ?", testcaseId));
		Integer noOfSchedules = deleteSchedules(schedules, jobProgressToken);
		return noOfSchedules;
	}

	@Override
	public int removeSchedulesByCycleId(Long cycleId, String jobProgressToken){
		if(StringUtils.isNotEmpty(jobProgressToken)) {
			jobProgressService.addSteps(jobProgressToken, ao.count(Schedule.class, Query.select().where("CYCLE_ID = ?", cycleId)));
		}
		final Integer[] noOfSchedules = {0};
		ExecutorService cachedPool = Executors.newFixedThreadPool(5);
		final ApplicationUser user = authContext.getLoggedInUser();
		Future<String> callableFuture = cachedPool.submit(() -> {
			if(authContext != null && authContext.getLoggedInUser() == null)
				authContext.setLoggedInUser(user);

			final Schedule[] schedules = ao.find(Schedule.class, Query.select().where("CYCLE_ID = ?", cycleId));
			noOfSchedules[0] = deleteSchedules(schedules, jobProgressToken);
			return "";
		});
		cachedPool.shutdown();

		return noOfSchedules[0];
	}


	@Override
	public int removeSchedulesByCycleIdAndProjectId(Long cycleId,Long projectId){
		String jobProgressToken = "";
		if(cycleId != null) {
			return removeSchedulesByCycleId(cycleId, jobProgressToken);
		}
		Query query = Query.select().where("CYCLE_ID IS NULL AND PROJECT_ID = ?", projectId);
		final Schedule[] schedules = ao.find(Schedule.class, query);
		Integer noOfSchedules = deleteSchedules(schedules, jobProgressToken);
		return noOfSchedules;
	}

	/**
	 * Private utility method to delete schedules and their mappings. 
	 * @param schedules
	 * @return
	 */
	public Integer deleteSchedules(final Schedule[] schedules, String jobProgressToken) {
 		if(schedules == null || schedules.length < 1)
			return 0;
		Integer noOfSchedules = ao.executeInTransaction(new TransactionCallback<Integer>() {
			@Override
			public Integer doInTransaction() {
				List<Integer> scheduleIds = new ArrayList<Integer>();
				Project project = null;
				Query query = Query.select();
				for (Schedule sch : schedules) {
					project = projectManager.getProjectObj(sch.getProjectId());
					scheduleIds.add(sch.getID());
					query.where("EXECUTION_ID = ?", sch.getID());
					ExecutionCf[] executions = ao.find(ExecutionCf.class, query);
					if(Objects.nonNull(executions) && executions.length>0) {
						ao.delete(executions);
					}
					stepResultManager.removeStepResultsBySchedule(sch.getID());
				}
				/*delete defects in bulk*/
				deleteScheduleDefects(scheduleIds);
				/*delete attachments in bulk*/
				attachmentManager.removeAttachmentsInBulk(ApplicationConstants.SCHEDULE_TYPE, scheduleIds, project);

				deleteSchedulesInChunk(schedules, jobProgressToken);

//				ao.delete(schedules);
                ApplicationUser currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
                log.info("Schedules with IDS " + scheduleIds + " are deleted by the user " + (currentUser != null ? currentUser.getName() : " no user"));

				return scheduleIds.size();
			}
		});
		return noOfSchedules;
	}

	/**
	 * Delete schedules in chunk and in transaction
	 * @param schedules
	 */
	private void deleteSchedulesInChunk(Schedule[] schedules, String jobProgressToken) {
		int fromIndex = 0;
		int toIndex = 0;
		boolean stopSublist=false;
		final List<Schedule> scheduleList = Arrays.asList(schedules);
		while(!stopSublist) {
			toIndex = scheduleList.size() > fromIndex && 
			scheduleList.size() > (fromIndex + ApplicationConstants.MAX_IN_QUERY) ? 
							(fromIndex + ApplicationConstants.MAX_IN_QUERY) : scheduleList.size();
			List<Schedule> subList = scheduleList.subList(fromIndex, toIndex);
			log.info("Deleting " + subList.size() + " schedules");
			ao.delete((Schedule[])subList.toArray(new Schedule[subList.size()]));
			jobProgressService.addCompletedSteps(jobProgressToken,subList.size());
			fromIndex += ApplicationConstants.MAX_IN_QUERY;
			if(toIndex == scheduleList.size()) {
				stopSublist = true;
			}
		}				
	}


	private void deleteScheduleDefects(List<Integer> scheduleIds) {
		if (scheduleIds == null || scheduleIds.size() < 1)
			return;
		log.info("Finding schedules : " + scheduleIds.size() + " found.");
		int fromIndex = 0;
		int toIndex = 0;
		boolean stopSublist = false;
		while (!stopSublist) {
			toIndex = scheduleIds.size() > fromIndex && scheduleIds.size() > (fromIndex + ApplicationConstants.MAX_IN_QUERY) ?
					(fromIndex + ApplicationConstants.MAX_IN_QUERY) : scheduleIds.size();
			List<Integer> subList = scheduleIds.subList(fromIndex, toIndex);
			String ques = StringUtils.repeat("?", ", ", subList.size());
			ScheduleDefect[] schDefects = ao.find(ScheduleDefect.class, Query.select().where("SCHEDULE_ID IN (" + ques + ")", subList.toArray()));
			deleteScheduleDefectsInChunk(schDefects);

			fromIndex += ApplicationConstants.MAX_IN_QUERY;
			if (toIndex == scheduleIds.size()) {
				stopSublist = true;
			}
		}
	}

	private void deleteScheduleDefectsInChunk(ScheduleDefect[] schDefects) {
		if (schDefects == null || schDefects.length < 1)
			return;
		log.info("Deleting associated " + schDefects.length + " schedule defects.");
		int fromIndex = 0;
		int toIndex = 0;
		boolean stopSubList = false;

		List<ScheduleDefect> schDefectList = Arrays.asList(schDefects);
		while (!stopSubList) {
			toIndex = schDefectList.size() > fromIndex && schDefectList.size() > (fromIndex + ApplicationConstants.MAX_IN_QUERY) ?
					(fromIndex + ApplicationConstants.MAX_IN_QUERY) : schDefectList.size();

			List<ScheduleDefect> subList = schDefectList.subList(fromIndex, toIndex);
			ScheduleDefect[] subArray = subList.toArray(new ScheduleDefect[0]);

			removeRemoteLinks(subArray);
			log.info("Deleting " + subArray.length + " attachments for " + subList.size() + " schedules");
			ao.delete(subArray);

			fromIndex += ApplicationConstants.MAX_IN_QUERY;
			if (toIndex == schDefectList.size()) {
				stopSubList = true;
			}
		}
	}
	
	/**
	 * Deletes all the remote links for associated defects
	 * @param scheduleDefects
	 */
	private void removeRemoteLinks(ScheduleDefect []scheduleDefects) {
		RemoteIssueLinkManager rilManager = ComponentAccessor.getComponentOfType(RemoteIssueLinkManager.class);
		ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
		for(ScheduleDefect sd : scheduleDefects) {
			try {
				rilManager.removeRemoteIssueLinkByGlobalId(issueManager.getIssueObject(new Long(sd.getDefectId())), 
														   String.valueOf(sd.getScheduleId()), user);
			} catch(Exception e) {
				log.info("Error finding remote link for defect : " + sd.getDefectId(),e);
			}
		}
	}

	@Override
	public List<Schedule> getSchedulesByCriteria(String searchExpression, int maxAllowedRecord) {
		return null;
	}

	@Override
	public List<Schedule> createBulkSchedule(List<Integer> issuesIds, Integer cycleId, Integer versionId) {
		List<Schedule> schedules = new ArrayList<Schedule>();
		Cycle cycle = ao.get(Cycle.class, cycleId);
		final Date today = new Date();
		for(Integer issueId : issuesIds){
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("dateCreated", today);
			params.put("cycle", cycle);
			params.put("issueId", issueId);
			Schedule sch = ao.create(Schedule.class, params);
			schedules.add(sch);
		}
		return schedules;
	}

	
	/******* SCHEDULEDEFECT ********/
	
	public List<ScheduleDefect> removeScheduleDefectsAssociation(Long defectId){
		List<ScheduleDefect> scheduleDefects = null;
		
		if(defectId != null){
			
			String ques = StringUtils.repeat("?", ", ", 1);
			ScheduleDefect[] schDefects = ao.find(ScheduleDefect.class, Query.select().where("DEFECT_ID IN (" + ques + ")", defectId));
			log.info("Deleting " + schDefects.length + " defects from ScheduleDefects Table ");
			ao.delete(schDefects);

			scheduleDefects = Arrays.asList(schDefects);
		}
		
		return scheduleDefects;
	}
	
	@Override
	public List<ScheduleDefect> getAssociatedDefects(Integer scheduleId) {
		Query query = Query.select().where("SCHEDULE_ID = ?", scheduleId.intValue());
		ScheduleDefect[] scheduleDefect = ao.find(ScheduleDefect.class, query);
		return Arrays.asList(scheduleDefect);
	}

	@Override
	public Map<String, Object> saveAssociatedDefects(final Schedule schedule, List<Integer> defectsToPersist) {
		ScheduleDefect[] scheduleDefectArray = ao.find(ScheduleDefect.class, Query.select().where("SCHEDULE_ID = ?", schedule.getID()));
		final Map<Integer, ScheduleDefect> existingDefectMap = new HashMap<Integer, ScheduleDefect>();
		for(ScheduleDefect sd : scheduleDefectArray){
			existingDefectMap.put(sd.getDefectId(), sd);
		}
		Set<Integer> existingDefects = existingDefectMap.keySet();
		if(defectsToPersist == null){
			defectsToPersist = new ArrayList<Integer>();
		}
		
		final Collection<Integer> defectsToAdd = CollectionUtils.subtract(defectsToPersist, existingDefects);
		final Collection<Integer> defectsToRemove = CollectionUtils.subtract(existingDefects, defectsToPersist);
		final Collection<Integer> defectsUnchanged = CollectionUtils.intersection(existingDefects, defectsToPersist);
		
		ao.executeInTransaction(new TransactionCallback<ScheduleDefect>() {
			Map<String, Object> params = new HashMap<String, Object>();
			@Override
			public ScheduleDefect doInTransaction(){
				for(Integer defectId : defectsToAdd){
					params.clear();
					params.put("SCHEDULE_ID", schedule.getID());
					params.put("DEFECT_ID", defectId);
					ao.create(ScheduleDefect.class, params);
				}
				
				for(Integer defectId : defectsToRemove){
					ao.delete(existingDefectMap.get(defectId));
				}
				return null;
			}
		});
		
		scheduleDefectArray = ao.find(ScheduleDefect.class, Query.select().where("SCHEDULE_ID = ?", schedule.getID()));
		Map<String, Object> associatedDefects = new HashMap<String, Object>(); 
		associatedDefects.put("added", defectsToAdd);
		associatedDefects.put("deleted", defectsToRemove);
		associatedDefects.put("unchanged", defectsUnchanged);
		if(scheduleDefectArray != null){
			associatedDefects.put("final", Arrays.asList(scheduleDefectArray));
		}
		
        //if finalDefects Added to schedule > 0, change the modified_by on schedule
        if(defectsToAdd.size() > 0) {
			schedule.setModifiedBy(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(ComponentAccessor.getJiraAuthenticationContext())));
            //setting modified date
            schedule.setModifiedDate(new Date());
			schedule.save();
        }
		
		return associatedDefects;
	}

	@Override
	public Integer removeAssociatedDefects(Integer scheduleId, Integer[] issueIds) {
		List<ScheduleDefect> scheduleDefects = null;
		if(issueIds == null || issueIds.length <1){
			ScheduleDefect[] scheduleDefectArray = ao.find(ScheduleDefect.class, Query.select().where("SCHEDULE_ID = ?", scheduleId));
		}else{
			for(int defectId : issueIds){
				ScheduleDefect[] scheduleDefectArray = ao.find(ScheduleDefect.class, Query.select().where("SCHEDULE_ID = ? and DEFECT_ID = ?", scheduleId, defectId));
				scheduleDefects.addAll(Arrays.asList(scheduleDefectArray));
			}
		}
		for(ScheduleDefect sd : scheduleDefects){
			ao.delete(sd);
		}
		return null;
	}
	/******* SCHEDULEDEFECT ********/
	
	@Override
	public List<Long> getAllScheduleIds() {
		Schedule[] schedules =  ao.find(Schedule.class,Query.select().distinct());
		List<Long> allScheduleIds = new ArrayList<Long>();
		for(Schedule schedule : schedules) {
			allScheduleIds.add(Long.valueOf(schedule.getID()));
		}
		return allScheduleIds;
	}


	/**
	 * Fetches Schedules for given project/schedule/duration
	 * @param statuses
	 * @param projectId
	 * @param duration
	 * @return
	 */
	public Map<String, Object> getSchedulesByProjectIdWithDuration(Set<Entry<Integer, ExecutionStatus>> statuses,Integer projectId, String duration) {
		//If daily, we will use this date
		DateFormat df = new SimpleDateFormat(ApplicationConstants.ZFJ_DATE_FORMAT);
		Date createdOn = new Date();
		if(StringUtils.equalsIgnoreCase(duration, "weekly")) {
			Calendar cal = GregorianCalendar.getInstance();
			cal.setTime(createdOn);
			cal.add(Calendar.DATE, -7);
			createdOn = cal.getTime();
		} else if(StringUtils.equalsIgnoreCase(duration, "monthly")) {
			Calendar cal = GregorianCalendar.getInstance();
			cal.setTime(createdOn);
			cal.add(Calendar.MONTH, -1);
			cal.add(Calendar.DAY_OF_MONTH, 1);
			createdOn = cal.getTime();
		}
		
		String date = df.format(createdOn);
		try {
			createdOn = df.parse(date);
		} catch(Exception e) {
			log.error("Error Parsing date:",e);
		}
		Map<String, Object> countByStatus = new HashMap<String, Object>();
		Integer total=0;
		for(Entry<Integer, ExecutionStatus> statusEntry : statuses){
			Integer cnt = 0;
			String statusKey = statusEntry.getKey().toString();
			cnt = ao.count(Schedule.class, Query.select().where("PROJECT_ID = ? and DATE_CREATED >= ? and STATUS=?", projectId,createdOn,statusKey).order(" DATE_CREATED DESC "));
			if(cnt == null) cnt = new Integer(0);
			total += cnt;
			countByStatus.put(statusKey, cnt);
		}
		countByStatus.put("total", total);
		return countByStatus;
	}
	
	//Move the call to Index when we have time. 
	public List<Long> getScheduleIdsByIssueId(Integer issueId) {
		Schedule[] schedules =  ao.find(Schedule.class,Query.select().where("ISSUE_ID=?", issueId).distinct());
		List<Long> allScheduleIds = new ArrayList<Long>();
		for(Schedule schedule : schedules) {
			allScheduleIds.add(Long.valueOf(schedule.getID()));
		}
		return allScheduleIds;
	}
	
	public Map<String, User> getExecutedByValues(Long projectId, Long versionId) {
		Map<String, User> allUsers = new LinkedHashMap<String, User>();
		DatabaseConfig dbConfig = ComponentAccessor.getComponent(DatabaseConfigurationManager.class).getDatabaseConfiguration();
		SQLProcessor sqlProcessor = null;
		String sqlQuery = null;
		int totalCount = 0;
		ResultSet resultSet = null;
		try {
			sqlProcessor = new SQLProcessor("defaultDS");
			if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), ApplicationConstants.POSTGRES_DB)) {
				sqlQuery = "SELECT DISTINCT \"EXECUTED_BY\" from \"AO_7DEABF_SCHEDULE\"  WHERE \"EXECUTED_BY\" IS NOT NULL AND \"PROJECT_ID\" = " + projectId + " AND \"VERSION_ID\" = " + versionId;
				if(projectId == null)
					sqlQuery = "SELECT DISTINCT \"EXECUTED_BY\" from \"AO_7DEABF_SCHEDULE\"  WHERE \"EXECUTED_BY\" IS NOT NULL";
				else if(versionId == ApplicationConstants.UNSCHEDULED_VERSION_ID)
					sqlQuery = "SELECT DISTINCT \"EXECUTED_BY\" from \"AO_7DEABF_SCHEDULE\"  WHERE \"EXECUTED_BY\" IS NOT NULL AND \"PROJECT_ID\" = " + projectId ;
			} else {
				sqlQuery = "SELECT DISTINCT EXECUTED_BY from AO_7DEABF_SCHEDULE WHERE EXECUTED_BY IS NOT NULL AND PROJECT_ID = " + projectId + " AND VERSION_ID = " + versionId;
				if(projectId == null)
					sqlQuery = "SELECT DISTINCT EXECUTED_BY from AO_7DEABF_SCHEDULE WHERE EXECUTED_BY IS NOT NULL";
				else if(versionId == ApplicationConstants.UNSCHEDULED_VERSION_ID)
					sqlQuery = "SELECT DISTINCT EXECUTED_BY from AO_7DEABF_SCHEDULE WHERE EXECUTED_BY IS NOT NULL AND PROJECT_ID = " + projectId;
			}
			resultSet =  sqlProcessor.executeQuery(sqlQuery);
			while (resultSet.next()) {
				String executedBy = resultSet.getString("EXECUTED_BY");
				User user = UserCompatibilityHelper.getUserForKey(executedBy);
				if(user != null) {
					allUsers.put(executedBy, user);
				}
			}
		} catch (Exception ex) {
			log.error("Error while executing the query - " + sqlQuery);
		} finally {
			closeConnections(sqlProcessor, resultSet);
		}
		return allUsers;
	}


	@Override
	public List<Schedule> getSchedulesInBatch(List<Long> scheduleIds) {
		String ques = StringUtils.repeat("?", ", ", scheduleIds.size());
		Query query = Query.select("ID").where("ID IN (" + ques + ")", scheduleIds.toArray());
		Schedule[] allSchedules = ao.find(Schedule.class,query);
		return Arrays.asList(allSchedules);
	}

	@Override
	public Schedule[] getSchedulesByCycleId(int id) {
		final Schedule[] schedules = ao.find(Schedule.class, Query.select().where("CYCLE_ID = ?", id));
		return schedules;
	}

	public Map<String, User> getAssigneeValues() {
		Map<String, User> allUsers = new LinkedHashMap<String, User>();
		DatabaseConfig dbConfig = ComponentAccessor.getComponent(DatabaseConfigurationManager.class).getDatabaseConfiguration();
		SQLProcessor sqlProcessor = null;
		String sqlQuery = null;
		int totalCount = 0;
		ResultSet resultSet = null;
		try {
			sqlProcessor = new SQLProcessor("defaultDS");
			if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), ApplicationConstants.POSTGRES_DB)) {
				sqlQuery = "SELECT DISTINCT \"ASSIGNED_TO\" from \"AO_7DEABF_SCHEDULE\"  WHERE \"ASSIGNED_TO\" IS NOT NULL";
			} else {
				sqlQuery = "SELECT DISTINCT ASSIGNED_TO from AO_7DEABF_SCHEDULE WHERE ASSIGNED_TO IS NOT NULL ";
			}
			resultSet =  sqlProcessor.executeQuery(sqlQuery);
			while (resultSet.next()) {
				String assignedTo = resultSet.getString("ASSIGNED_TO");
				User user = UserCompatibilityHelper.getUserForKey(assignedTo);
				if(user != null) {
					allUsers.put(assignedTo, user);
				}
			}
		} catch (Exception ex) {
			log.error("Error while executing the query - " + sqlQuery);
		} finally {
			closeConnections(sqlProcessor, resultSet);
		}
		return allUsers;
	}
	
	@Override
	public int removeSchedulesByUserId(String userId){
		String jobProgressToken = "";
		final Schedule[] schedules = ao.find(Schedule.class, Query.select().where("EXECUTED_BY = ?", userId));
		Integer noOfSchedules = deleteSchedules(schedules, jobProgressToken);
		return noOfSchedules;
	}	
	
	public boolean updateModifiedDate(final Schedule schedule, final Date modifiedDate){
		//Schedule schedule = getSchedule(Integer.valueOf(scheduleId));
		if(schedule != null){
			schedule.setModifiedDate(modifiedDate);
			schedule.save();
			return true;
		}
		return false;
	}

	/**
	 * Updates Bulk Status for Schedules
	 */
	public List<Schedule> updateBulkStatus(final Collection<Integer> scheduleIds,final String status,final String stepStatus,final boolean clearDefectAssociation,final boolean changeStepStatus,final User executedByUser, String jobProgressToken) {
		List<Schedule> noOfSchedules = ao.executeInTransaction(new TransactionCallback<List<Schedule>>() {
			@Override
			public List<Schedule> doInTransaction() {
				List<Schedule> schedules = new ArrayList<Schedule>();
				List<Integer> scheduleList = new ArrayList<Integer>();
				
				for(Integer scheduleId : scheduleIds) {
					Schedule schedule = getSchedule(Integer.valueOf(scheduleId));
					if(schedule != null) {
						schedule.setStatus(status);
						schedule.setModifiedBy(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(ComponentAccessor.getJiraAuthenticationContext())));

						//setting modified date
                        schedule.setModifiedDate(new Date());

						//reset executedBy if changed to Unexecuted Status
						final String userKey = UserCompatibilityHelper.getKeyForUser(executedByUser);
						final long currentTimeMillis = System.currentTimeMillis();
						if(JiraUtil.getExecutionStatuses().get(Integer.valueOf(status)).getId() == -1) {
							schedule.setExecutedBy(null);
							schedule.setExecutedOn(null);
						} else {
				    		schedule.setExecutedBy(userKey);
				    		schedule.setExecutedOn(currentTimeMillis);
						}
						schedule.save();
						scheduleList.add(scheduleId);
						schedules.add(schedule);
						List<Integer> stepResultIds = new ArrayList<Integer>();
						if(changeStepStatus) {
							Map<Integer,StepResult> testStepResults = new HashMap<Integer,StepResult>();
							List<StepResult> stepResults = stepResultManager.getStepResultsBySchedule(scheduleId);
							String stepResultStatusId = String.valueOf(JiraUtil.getStepExecutionStatuses().get(Integer.valueOf(stepStatus)).getId());
							if(stepResults != null && stepResults.size() > 0) {
								for(StepResult stepResult  : stepResults) {
									stepResult.setStatus(stepResultStatusId);
									//reset executedBy if changd to Unexecuted Status
									if(stepResultStatusId != null && stepResultStatusId.equals("-1")){
										stepResult.setExecutedBy(null);
										stepResult.setExecutedOn(null);
									} else {
										stepResult.setExecutedBy(userKey);
										stepResult.setExecutedOn(currentTimeMillis);
									}
									stepResult.save();
									testStepResults.put(stepResult.getStep().getID(),stepResult);
									stepResultIds.add(stepResult.getID());
								}
							}

							List<Teststep> testSteps = testStepManager.getTeststeps(Long.valueOf(schedule.getIssueId()), java.util.Optional.empty(), java.util.Optional.empty());
							for(Teststep testStep : testSteps) {
								if(!testStepResults.containsKey(testStep.getID())) {
									Map<String,Object> resultProperties = new HashMap<String,Object>();
									resultProperties.put("SCHEDULE_ID", scheduleId);
									resultProperties.put("STEP_ID", testStep.getID());
									resultProperties.put("PROJECT_ID", schedule.getProjectId());
									resultProperties.put("STATUS", stepResultStatusId);
									resultProperties.put("CREATED_BY",UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(ComponentAccessor.getJiraAuthenticationContext())));
									resultProperties.put("MODIFIED_BY",UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(ComponentAccessor.getJiraAuthenticationContext())));

									if(stepResultStatusId != null && !stepResultStatusId.equals("-1")){
										resultProperties.put("EXECUTED_BY", userKey);
										resultProperties.put("EXECUTED_ON", currentTimeMillis);
									}
									StepResult sResult = stepResultManager.addStepResult(resultProperties);	
									stepResultIds.add(sResult.getID());
								} 
							}
						}
						if(clearDefectAssociation && JiraUtil.getExecutionStatuses().get(Integer.valueOf(status)).getId() == ApplicationConstants.UNEXECUTED_STATUS) {
							deleteScheduleDefects(scheduleList);
						}
						
						if(clearDefectAssociation && changeStepStatus && JiraUtil.getStepExecutionStatuses().get(Integer.valueOf(stepStatus)).getId() == ApplicationConstants.UNEXECUTED_STATUS) {
							stepResultManager.deleteStepDefects(stepResultIds);
						}
					}
					jobProgressService.addCompletedSteps(jobProgressToken,1);
				}
				return schedules;
			}
		});	
		return noOfSchedules;
	}
	
	
	public Map<String,List<Schedule>> copyOrMoveBulkSchedules(final Collection<String> scheduleIds,final String action,final Integer projectId,final Integer versionId,final Long folderId, 
			final Integer cycleId,final boolean clearStatusFlag,final boolean clearDefectAssociation, final boolean clearAssignmentsFlag, String jobProgressToken, final boolean clearCustomFields) {
		final Map<String,List<Schedule>> result = new HashMap<String, List<Schedule>>();
		List<Schedule> noOfSchedules = ao.executeInTransaction(new TransactionCallback<List<Schedule>>() {
			@Override
			public List<Schedule> doInTransaction() {
				List<Schedule> schedules = new ArrayList<Schedule>();
				List<Integer> scheduleList = new ArrayList<Integer>();
				List<StepResult> stepResults = new ArrayList<StepResult>();
				for(String scheduleId : scheduleIds) {
					if(StringUtils.equalsIgnoreCase(action, "move")) {
						if(StringUtils.isNotBlank(scheduleId)) {
							Schedule schedule = getSchedule(Integer.valueOf(scheduleId));
							if(schedule != null) {
								if(schedule.getProjectId().intValue() == projectId.intValue()) {
									if(versionId == ApplicationConstants.UNSCHEDULED_VERSION_ID) {
										Schedule existingSchedule = getSchedulesByIssueIdAndCycleIdAndFolderId(projectId, versionId, schedule.getIssueId(),cycleId,folderId,null);
										stepResults = validateAndPerformBulkMove(
												versionId, cycleId,
												clearStatusFlag,
												clearDefectAssociation,
												result, schedules,
												scheduleList, stepResults,
												scheduleId, schedule,
												existingSchedule, folderId, clearCustomFields);
									} else {
										Schedule existingSchedule = getSchedulesByIssueIdAndCycleIdAndFolderId(projectId, versionId, schedule.getIssueId(),cycleId,folderId,null);
										boolean isValid = isVersionCorrect(projectManager.getProjectObj(projectId.longValue()),versionId);
										if(!isValid) {
											updateBulkOperationResult("version_mismatch",result, schedule);
										} else {
											stepResults = validateAndPerformBulkMove(
													versionId, cycleId,
													clearStatusFlag,
													clearDefectAssociation,
													result, schedules,
													scheduleList, stepResults,
													scheduleId, schedule,
													existingSchedule, folderId, clearCustomFields);
										}
									}
								} else {
									updateBulkOperationResult("project_mismatch",result, schedule);
								}
							}
							jobProgressService.addCompletedSteps(jobProgressToken,1);
						}
 					} else if(StringUtils.equalsIgnoreCase(action, "copy")) {
						if(StringUtils.isNotBlank(scheduleId)) {
							Schedule schedule = getSchedule(Integer.valueOf(scheduleId));
							if(schedule != null) {
								Schedule existingSchedule = getSchedulesByIssueIdAndCycleIdAndFolderId(projectId, versionId, schedule.getIssueId(),cycleId,folderId,null);
								if(schedule.getProjectId().intValue() == projectId.intValue()) {
									if(versionId == ApplicationConstants.UNSCHEDULED_VERSION_ID) {
										stepResults = validateAndPerformBulkCopy(
												versionId, cycleId,
												clearStatusFlag,
												clearDefectAssociation,
												clearAssignmentsFlag,
												result, schedules,
												stepResults, schedule,
												existingSchedule, folderId, clearCustomFields);
									} else {
										boolean isValid = isVersionCorrect(projectManager.getProjectObj(projectId.longValue()),versionId);
										if(!isValid) {
											updateBulkOperationResult("version_mismatch",result, schedule);
										} else {
											stepResults = validateAndPerformBulkCopy(
													versionId, cycleId,
													clearStatusFlag,
													clearDefectAssociation,
													clearAssignmentsFlag,
													result, schedules,
													stepResults, schedule,
													existingSchedule, folderId, clearCustomFields);
										}
									}
								} else {
									updateBulkOperationResult("project_mismatch",result, schedule);
								}
							}
							jobProgressService.addCompletedSteps(jobProgressToken,1);
						}
					}
				}
				return schedules;
			}

			private List<StepResult> validateAndPerformBulkCopy(
					final Integer versionId, final Integer cycleId,
					final boolean clearStatusFlag,
					final boolean clearDefectAssociation,
					boolean clearAssignmentsFlag, final Map<String, List<Schedule>> result,
					List<Schedule> schedules, List<StepResult> stepResults,
					Schedule schedule, Schedule existingSchedule, Long folderId, boolean clearCustomFields) {
				if(existingSchedule != null && (schedule.getCycle() != null && schedule.getCycle().getID() == cycleId)) {
					updateBulkOperationResult("invalid",result, schedule);
				} else if(existingSchedule != null && (existingSchedule.getCycle() != null && existingSchedule.getCycle().getID() == cycleId)) {
					updateBulkOperationResult("already_present",result, schedule);
				} else {
					boolean hasIssueViewPermission = JiraUtil.hasIssueViewPermission(Long.valueOf(schedule.getIssueId()),null,authContext.getLoggedInUser());
					if(!hasIssueViewPermission) {
						updateBulkOperationResult("issue_permission",result, schedule);
					} else {
                        stepResults = performBulkCopy(versionId,
                                cycleId, clearStatusFlag,
                                clearDefectAssociation, clearAssignmentsFlag, schedules, schedule, folderId, clearCustomFields);
                        updateBulkOperationResult("success",result, schedule);
				    }
				}
				return stepResults;
			}

			private List<StepResult> validateAndPerformBulkMove(
					final Integer versionId, final Integer cycleId,
					final boolean clearStatusFlag,
					final boolean clearDefectAssociation,
					final Map<String, List<Schedule>> result,
					List<Schedule> schedules, List<Integer> scheduleList,
					List<StepResult> stepResults, String scheduleId,
					Schedule schedule, Schedule existingSchedule, Long folderId, boolean clearCustomFields) {
				if(existingSchedule != null && (existingSchedule.getCycle() != null && existingSchedule.getCycle().getID() == cycleId)) { 
						updateBulkOperationResult("already_present",result, schedule);
				} else if(existingSchedule != null && (schedule.getCycle() != null && schedule.getCycle().getID() == cycleId)) { 
						updateBulkOperationResult("invalid",result, schedule);
				} else {
					boolean hasIssueViewPermission = JiraUtil.hasIssueViewPermission(Long.valueOf(schedule.getIssueId()),null,authContext.getLoggedInUser());
					if(!hasIssueViewPermission) {
						updateBulkOperationResult("issue_permission",result, schedule);
                    } else {
                        stepResults = performBulkMove(versionId,
                                cycleId, clearStatusFlag,
                                clearDefectAssociation, schedules,
                                scheduleList, stepResults, scheduleId,
                                schedule, folderId, clearCustomFields);
                        updateBulkOperationResult("success",result, schedule);
                    }
				}
				return stepResults;
			}

			/**
			 * Version needs to be belong to the project passed in
			 * @param projectObj
			 * @param versionId
			 * @return
			 */
			private boolean isVersionCorrect(Project projectObj,Integer versionId) {
				for(Version version : projectObj.getVersions()) {
					if(version.getId().intValue() == versionId.intValue()) {
						return true;
					}
				}
				return false;
			}


			private List<StepResult> performBulkCopy(final Integer versionId,
					final Integer cycleId, final boolean clearStatusFlag,
					final boolean clearDefectAssociation, boolean clearAssignmentsFlag, List<Schedule> schedules, Schedule schedule, Long folderId, boolean clearCustomFields) {

                Map<String, Object> schedMap = new HashMap<String, Object>();
				schedMap.put("DATE_CREATED", new Date());
				schedMap.put("VERSION_ID", Long.valueOf(versionId));
				schedMap.put("PROJECT_ID", schedule.getProjectId());
				if(cycleId != -1) {
					schedMap.put("CYCLE_ID",Long.valueOf(cycleId));
				}
				/*As per requirement, we need to reset comments*/
				schedMap.put("ISSUE_ID", schedule.getIssueId());
				
				//add order_id to newly created execution
				schedMap.put("ORDER_ID", getMaxOrderId() + 1);
				schedMap.put("CREATED_BY", UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(ComponentAccessor.getJiraAuthenticationContext())));
				schedMap.put("MODIFIED_BY", UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(ComponentAccessor.getJiraAuthenticationContext())));

				if(clearStatusFlag) {
					schedMap.put("STATUS", String.valueOf(ApplicationConstants.UNEXECUTED_STATUS));
					schedMap.put("EXECUTED_ON",null);
					schedMap.put("EXECUTED_BY",null);
					schedMap.put("COMMENT", "");
					/*Not cloning the steps as the result is cleared, stepResult will autogenerated when user first visits this execution*/
				} else {
					schedMap.put("STATUS", schedule.getStatus());
					schedMap.put("EXECUTED_ON", schedule.getExecutedOn());
					schedMap.put("EXECUTED_BY", schedule.getExecutedBy());
					schedMap.put("COMMENT", schedule.getComment());
				}
				if(!clearAssignmentsFlag) {
					schedMap.put("ASSIGNED_TO", schedule.getAssignedTo());
				}
				schedMap.put("FOLDER_ID", folderId);
				schedMap.put("EXECUTION_WORKFLOW_STATUS", ExecutionWorkflowStatus.CREATED);
				Schedule newSchedule = saveSchedule(schedMap);
				Table<String, String, Object> changePropertyTable = HashBasedTable.create();
				changePropertyTable.put("STATUS", ApplicationConstants.OLD, ApplicationConstants.NULL);
				changePropertyTable.put("STATUS", ApplicationConstants.NEW, newSchedule.getStatus());
				changePropertyTable.put("DATE_CREATED", ApplicationConstants.OLD, ApplicationConstants.NULL);
				changePropertyTable.put("DATE_CREATED", ApplicationConstants.NEW, String.valueOf(newSchedule.getDateCreated().getTime()));
				if(StringUtils.isNotBlank(schedule.getAssignedTo())) {
					changePropertyTable.put("ASSIGNED_TO", ApplicationConstants.OLD, ApplicationConstants.NULL);
					changePropertyTable.put("ASSIGNED_TO", ApplicationConstants.NEW, StringUtils.isEmpty(newSchedule.getAssignedTo()) ? ApplicationConstants.NULL : newSchedule.getAssignedTo());
				}
				eventPublisher.publish(new ScheduleModifyEvent(schedule, changePropertyTable, EventType.EXECUTION_ADDED,
						UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));

				schedules.add(newSchedule);
				List<StepResult> newStepResults =  cloneStepResults(schedule.getID(), newSchedule.getID(),clearStatusFlag);
				//associate defects to new schedule
				if(!clearDefectAssociation) {
					List<ScheduleDefect> scheduleDefects = getAssociatedDefects(schedule.getID());
				    List<Integer> defectsToPersist = CollectionUtil.transform(scheduleDefects, new Function<ScheduleDefect, Integer>(){
				        @Override
						public Integer get(final ScheduleDefect scheduleDefect){
				            return scheduleDefect.getDefectId();
				        }
				    });	
				    saveAssociatedDefects(newSchedule, defectsToPersist);
					List<StepResult> oldStepResults = stepResultManager.getStepResultsBySchedule(schedule.getID());

				    if(newStepResults != null && newStepResults.size() > 0) {
						for(StepResult stepResult : oldStepResults) {
							List<StepDefect> stepDefects = stepResultManager.getAssociatedDefects(stepResult.getID());
							List<Integer> stepDefectsToPersist = CollectionUtil.transform(stepDefects, new Function<StepDefect, Integer>(){
						        @Override
								public Integer get(final StepDefect stepDefect){
						            return stepDefect.getDefectId();
						        }
						    });	
							cloneStepResultDefect(newSchedule.getID(),stepResult,newStepResults,stepDefectsToPersist);
						}
				    }
				}
				if(!clearCustomFields){
					List<ExecutionCf> allCFs = customFieldValueManager.getCustomFieldValuesForExecution(schedule.getID());
					CustomFieldProject[] allActiveCustomFieldProjects = zephyrCustomFieldManager.getAllActiveCustomFieldsProject();
					Map<Integer,List<Long>> allActiveCustomFieldsProject = new LinkedHashMap<>();
					if(allActiveCustomFieldProjects != null) {
						allActiveCustomFieldsProject = Arrays.stream(allActiveCustomFieldProjects).collect(Collectors.groupingBy(CustomFieldProject::getCustomFieldId,
								Collectors.mapping(f -> f.getProjectId(),
										Collectors.toList())));
					}
					Map<Integer, List<Long>> finalAllActiveCustomFieldsProject = allActiveCustomFieldsProject;
					allActiveCustomFieldsProject = null;
					List<ExecutionCf> newCFs = new ArrayList<>();
					if(allCFs != null){
						allCFs.stream().forEach(executionCf -> {
							if(finalAllActiveCustomFieldsProject != null && finalAllActiveCustomFieldsProject.get(executionCf.getCustomField().getID()) != null &&
									finalAllActiveCustomFieldsProject.get(executionCf.getCustomField().getID()).contains(schedule.getProjectId())) {
								Map<String, Object> customFieldValueProperties = prepareRequestForCustomFieldValue(executionCf);
								customFieldValueProperties.put("EXECUTION_ID", newSchedule.getID());
								ExecutionCf newExecutionCf = customFieldValueManager.saveExecutionCustomFieldValue(customFieldValueProperties);
								newCFs.add(newExecutionCf);
							}
						});
					}
				}
				return newStepResults;
			}

			/**
			 * Clone Associated Defects 
			 * @param newScheduleId
			 * @param oldStepResult
			 * @param newStepResults
			 * @param stepDefectsToPersist
			 */
			private void cloneStepResultDefect(int newScheduleId, StepResult oldStepResult,
					List<StepResult> newStepResults,
					List<Integer> stepDefectsToPersist) {
				for(StepResult stepResult : newStepResults) {
					if(stepResult.getStep().getID() ==  oldStepResult.getStep().getID()) {
						stepResultManager.saveAssociatedDefects(stepResult.getStep().getID(),newScheduleId,stepResult.getID(),stepDefectsToPersist);
					}
				}
			}


			/**
			 * @param existingScheduleId ID of schedule to be cloned
			 * @param newScheduleId
			 * @param clearStatusFlag
			 * @return
			 */
			private List<StepResult> cloneStepResults(Integer existingScheduleId, Integer newScheduleId,boolean clearStatusFlag) {
				List<StepResult> stepResults;
				//Step Results to clone
				stepResults = stepResultManager.getStepResultsBySchedule(existingScheduleId);
				if(stepResults == null || stepResults.size() < 1){
					 return null;
				}
				List<StepResult> newStepResults = new ArrayList<StepResult>();
				for(StepResult stepResult : stepResults){
					Builder<String, Object> stepResultCloneBuilder = new ImmutableMap.Builder<String, Object>()
						.put("SCHEDULE_ID", newScheduleId)
						.put("PROJECT_ID", stepResult.getProjectId())
						.put("STEP_ID", stepResult.getStep().getID())
						.put("CREATED_BY",UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(ComponentAccessor.getJiraAuthenticationContext())))
						.put("MODIFIED_BY",UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(ComponentAccessor.getJiraAuthenticationContext())));

						if(!clearStatusFlag) {
							/*Comments are not cloned*/
							if(stepResult.getExecutedBy() != null)
								stepResultCloneBuilder.put("EXECUTED_BY", stepResult.getExecutedBy());
							if(stepResult.getExecutedOn() != null)
								stepResultCloneBuilder.put("EXECUTED_ON", stepResult.getExecutedOn());
							if(stepResult.getStatus() != null)
								stepResultCloneBuilder.put("STATUS", stepResult.getStatus());
							if(stepResult.getComment() != null) 
								stepResultCloneBuilder.put("COMMENT", stepResult.getComment());
						} else {
							stepResultCloneBuilder.put("STATUS", String.valueOf(ApplicationConstants.UNEXECUTED_STATUS));
						}
					newStepResults.add(stepResultManager.addStepResult(stepResultCloneBuilder.build()));
				}
				return newStepResults;
			}
			

			private List<StepResult> performBulkMove(
					final Integer versionId, final Integer cycleId,
					final boolean clearStatusFlag,
					final boolean clearDefectAssociation,
					List<Schedule> schedules, List<Integer> scheduleList,
					List<StepResult> stepResults, String scheduleId,
					Schedule schedule, Long folderId, boolean clearCustomFields) {
				//clearCustomFields is not required for the move operation.
                // if workflow is completed then do not clear status & defect association.
                String workflowStatus = null != schedule.getExecutionWorkflowStatus() ?  schedule.getExecutionWorkflowStatus().getName() : StringUtils.EMPTY;
                boolean isWorkFlowCompleted = (StringUtils.isNotBlank(workflowStatus) && workflowStatus.equalsIgnoreCase(ExecutionWorkflowStatus.COMPLETED.name())) ? Boolean.TRUE : Boolean.FALSE;

				if(clearStatusFlag) {
				    if((null != cycleId && cycleId == -1) || (null != cycleId && !isWorkFlowCompleted)) {
                        schedule.setStatus(String.valueOf(ApplicationConstants.UNEXECUTED_STATUS));
                        schedule.setExecutedOn(null);
                        schedule.setExecutedBy(null);
                        //Clear Step Results
                        stepResults = stepResultManager.getStepResultsBySchedule(Integer.valueOf(scheduleId));
                        for(StepResult stepResult  : stepResults) {
                            stepResult.setStatus(String.valueOf(ApplicationConstants.UNEXECUTED_STATUS));
                            stepResult.setExecutedBy(null);
                            stepResult.setExecutedOn(null);
                            stepResult.save();
                        }
                    }
				} else {
					schedule.setStatus(schedule.getStatus());
					schedule.setExecutedOn(schedule.getExecutedOn());
					schedule.setExecutedBy(schedule.getExecutedBy());
				}
				schedule.setProjectId(schedule.getProjectId());
				schedule.setVersionId(Long.valueOf(versionId));
				schedule.setModifiedBy(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(ComponentAccessor.getJiraAuthenticationContext())));
				schedule.setModifiedDate(new Date());
				schedule.setFolder(folderManager.getFolder(folderId));
				if(cycleId == -1) {
					schedule.setExecutionWorkflowStatus(null);
					schedule.setEstimatedTime(null);
					schedule.setLoggedTime(null);
					schedule.setCycle(null);
				} else {
					schedule.setCycle(cycleManager.getCycle(Long.valueOf(cycleId)));
				}
				schedule.save();
				scheduleList.add(Integer.valueOf(scheduleId));
				schedules.add(schedule);
				if(clearDefectAssociation && !isWorkFlowCompleted) {
					deleteScheduleDefects(scheduleList);
					if(stepResults != null ) {
						stepResults = stepResultManager.getStepResultsBySchedule(Integer.valueOf(scheduleId));
					}
					List<Integer> stepResultIds = new ArrayList<Integer>();
					for(StepResult stepResult  : stepResults) {
						stepResultIds.add(stepResult.getID());
					}
					stepResultManager.deleteStepDefects(stepResultIds);
				}
				return stepResults;
			}
		});	
		result.put("success", noOfSchedules);
		return result;
	}
	
	private Map<String, Object> prepareRequestForCustomFieldValue(ExecutionCf executionCf){
        Map<String, Object> customFieldValueProperties = new HashMap<>();

        customFieldValueProperties.put("CUSTOM_FIELD_ID", executionCf.getCustomField().getID());
        //customFieldValueProperties.put("EXECUTION_ID", executionCf.getExecutionId());
        CustomField cf = executionCf.getCustomField();
        if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(cf.getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.STRING_VALUE) ||
                ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(cf.getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.LIST_VALUE)) {
            customFieldValueProperties.put(ApplicationConstants.STRING_VALUE, executionCf.getStringValue());
        } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(cf.getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.LARGE_VALUE)) {
            customFieldValueProperties.put(ApplicationConstants.LARGE_VALUE, executionCf.getLargeValue());
        } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(cf.getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.NUMBER_VALUE)) {
            if(executionCf.getNumberValue() == null) {
                customFieldValueProperties.put(ApplicationConstants.NUMBER_VALUE, null);
            }else {
                customFieldValueProperties.put(ApplicationConstants.NUMBER_VALUE, executionCf.getNumberValue());
            }
        } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(cf.getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.DATE_VALUE)
                || ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(cf.getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.DATE_TIME_VALUE)) {
            try {
                if(executionCf.getDateValue() != null) {
                    customFieldValueProperties.put(ApplicationConstants.DATE_VALUE, executionCf.getDateValue());
                } else {
                    customFieldValueProperties.put(ApplicationConstants.DATE_VALUE, null);
                }
            } catch (Exception e) {
                log.error("Exception occurred while parsing the date", e);
            }
        }
        customFieldValueProperties.put("SELECTED_OPTIONS", executionCf.getSelectedOptions());

        return customFieldValueProperties;
	}


	public Map<Integer,Map<String, Object>> bulkAssociateDefectsToSchedules(final List<Object> scheduleIds,final List<Integer> defects) {
		Map<Integer,Map<String, Object>> responseMap = new HashMap<Integer, Map<String,Object>>();
		for(Object scheduleIdObject : scheduleIds) {
			Optional<Integer> scheduleId = ZCollectionUtils.getAsOptionalInteger(scheduleIdObject);
            if(!scheduleId.isPresent())
                continue;
			Schedule schedule = getSchedule(scheduleId.get());
			if(schedule != null) {
              	//Check ZephyrPermission and update response to include execution per project permissions
            	ProjectPermissionKey cyclePermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_BROWSE_CYCLE.toString());
            	ProjectPermissionKey executionPermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_EDIT_EXECUTION.toString());
            	Collection<ProjectPermissionKey> projectPermissionKeys = new ArrayList<ProjectPermissionKey>();
            	projectPermissionKeys.add(executionPermissionKey);
            	projectPermissionKeys.add(cyclePermissionKey);
				boolean loggedInUserHasZephyrPermission = zephyrPermissionManager.validateUserPermissions(projectPermissionKeys, null, ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser(), schedule.getProjectId());
                boolean hasIssueViewPermission = JiraUtil.hasIssueViewPermission(Long.valueOf(schedule.getIssueId()),null,authContext.getLoggedInUser());
				if(loggedInUserHasZephyrPermission) {
					Map<String, Object> result = new HashMap<>();
                    if(null != schedule.getExecutionWorkflowStatus() &&
                            schedule.getExecutionWorkflowStatus().name().equals(ExecutionWorkflowStatus.COMPLETED.name())) {
                        result.put("workFlowCompletedExecutions", String.valueOf(scheduleId.get()));
                        result.put("schedule", schedule);
                    }else {
                        List<Integer> tempDefects = new ArrayList<Integer>(defects);
                        List<ScheduleDefect> scheduleDefects = getAssociatedDefects(schedule.getID());
                        List<Integer> defectsToPersist = CollectionUtil.transform(scheduleDefects, new Function<ScheduleDefect, Integer>()
                        {
                            @Override
                            public Integer get(final ScheduleDefect scheduleDefect)
                            {
                                return scheduleDefect.getDefectId();
                            }
                        });

                        boolean exists = tempDefects.remove(schedule.getIssueId());

                        final Collection<Integer> defectsToAdd = CollectionUtils.union(tempDefects,defectsToPersist);
                        List<Integer> finalDefectsToAdd = new ArrayList<Integer>(defectsToAdd); //One thats passed in

                        if(hasIssueViewPermission) {
                            result = saveAssociatedDefects(schedule, finalDefectsToAdd);
                        }
                        result.put("schedule", schedule);
                        if(exists) {
                            result.put("invalid", issueManager.getIssueObject(schedule.getIssueId().longValue()).getKey());
                        }
                    }
			        responseMap.put(scheduleId.get(), result);
                } else {
                	Map<String, Object> result = new HashMap<String, Object>();
			        result.put("noPermission", String.valueOf(scheduleId.get()));
			        result.put("schedule", schedule);
			        responseMap.put(scheduleId.get(), result);
                }
				if(!hasIssueViewPermission) {
					Map<String, Object> result = new HashMap<String, Object>();
					result.put("noPermission", String.valueOf(scheduleId.get()));
					result.put("schedule", schedule);
					responseMap.put(scheduleId.get(), result);
				}
			} else {
				Map<String, Object> result = new HashMap<String, Object>();
				result.put("missing", scheduleId);
		        responseMap.put(scheduleId.get(), result);
			}
		}
		return responseMap;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Schedule[] getSchedules(List<Integer> scheduleIds) {
		List<Object> params = new ArrayList<Object>();
		String ques[] = new String[scheduleIds.size()];
		for(int i=0; i< scheduleIds.size(); i++){
			ques[i] = "?";
		}
		// This is to deal with PostgreSql DB ( plz refer to ZFJ-601)
		// scheduleIds which is a List<Integer>, yet it contains string inside this.
		// MySql DB driver takes care of integer = string conversion and works fine, 
		// wherein PostgreSql doesn't take auto care of integer = string and throws exception 
		Iterator<Integer> itr = scheduleIds.listIterator();
		while (itr.hasNext()) {
			Integer intVal = 0;
			Object val = itr.next();
			if(val instanceof Number)
				intVal = ((Number)val).intValue();
			if(val instanceof String)
				intVal = Integer.parseInt((String)val);
			params.add(intVal);
		}
		//This is a hack to get around AO's limitation of being able to pass only one object per query
		String whereClause =" ID IN ( " + StringUtils.join(ques, ',') + " ) ";
        Query query = Query.select("MODIFIED_BY,ISSUE_ID,PROJECT_ID,COMMENT,ACTUAL_EXECUTION_TIME,STATUS,DATE_CREATED,EXECUTED_ON,CREATED_BY,ORDER_ID,ASSIGNED_TO,EXECUTED_BY,VERSION_ID").where(whereClause,params.toArray());
        Schedule[] schedules = ao.find(Schedule.class, query);
        return schedules;
	}
	
	private void updateBulkOperationResult(String status,
			final Map<String, List<Schedule>> result, Schedule schedule) {
		List<Schedule> scheduleSkipped = result.get(status);
		if(scheduleSkipped == null) {
			scheduleSkipped = new ArrayList<Schedule>();
		}
		scheduleSkipped.add(schedule);
		result.put(status, scheduleSkipped);
	}
	
	@Override
	public Set<Long> getDistinctVersionsByProjectId(Long projectId) {
		Set<Long> versionSet = new TreeSet<Long>();
		Schedule[] schedules = ao.find(Schedule.class, Query.select("VERSION_ID").distinct().where("PROJECT_ID = ? ", projectId));
		for (Schedule schedule : schedules) {
			versionSet.add(schedule.getVersionId());
		}
		return versionSet;
	}
	
	@Override
	public Long getDistinctProjectIdByVersionId(Long versionId) {
		Long projectId = null;
		Query query = Query.select("PROJECT_ID").distinct();
		query.setLimit(1);
		Schedule[] schedules = ao.find(Schedule.class, query.where("VERSION_ID = ? ", versionId));
		if(schedules != null && schedules.length > 0) {
			projectId = schedules[0].getProjectId(); 
		}
		return projectId;
	}
	
	@Override
	public Integer getScheduleDefectCountByScheduleId(final Integer scheduleId) {
		Integer scheduleDefects =  ao.count(ScheduleDefect.class, Query.select().where("SCHEDULE_ID = ?", scheduleId));
		return scheduleDefects;
	}
	
	@Override
	public Integer getMaxOrderId() {
		Schedule[] schedules =  ao.find(Schedule.class, Query.select("ORDER_ID").where("ORDER_ID IS NOT NULL").distinct().order("ORDER_ID DESC").limit(1));
		for(Schedule schedule : schedules) {
			return schedule.getOrderId();
		}
		return 0;
	}
	
	@Override
	public List<Schedule> getScheduleByOrderId(Integer newOrderId, int cycleId, Integer versionId) {
		StringBuffer whereClauses = new StringBuffer();
		List<Integer> params = new ArrayList<Integer>();
		if(cycleId == -1) {
			whereClauses.append("ORDER_ID = ? AND CYCLE_ID IS NULL ");
			params.add(newOrderId.intValue());
			if(versionId != null) {
				whereClauses.append(" AND VERSION_ID = ?");
				params.add(versionId.intValue());
			} else {
				whereClauses.append(" AND VERSION_ID IS NULL");
			}
		} else {
			whereClauses.append("ORDER_ID = ? AND CYCLE_ID = ? ");
			params.add(newOrderId.intValue());
			params.add(cycleId);
			if(versionId != null) {
				whereClauses.append(" AND VERSION_ID = ?");
				params.add(versionId.intValue());
			} else {
				whereClauses.append(" AND VERSION_ID IS NULL");
			}
		}
		
		Schedule[] schedules =  ao.find(Schedule.class, Query.select("MODIFIED_BY,ISSUE_ID,PROJECT_ID,COMMENT,ACTUAL_EXECUTION_TIME,STATUS,DATE_CREATED,EXECUTED_ON,CREATED_BY,ORDER_ID,ASSIGNED_TO,EXECUTED_BY,VERSION_ID").where(whereClauses.toString(),params.toArray()));
		if(schedules != null && schedules.length > 0){
			return Arrays.asList(schedules);
		}
		return null;
	}

	@Override
	public Integer getScheduleCountByProjectIdAndGroupby(Integer projectId, boolean onlyUnexecuted) {
        try {

            if (!onlyUnexecuted) {
                Schedule[] scheduleCount = null;
				DatabaseConfig dbConfig = ComponentAccessor.getComponent(DatabaseConfigurationManager.class).getDatabaseConfiguration();
				log.debug("getScheduleCountByProjectIdAndGroupby Database Config is :"+ dbConfig != null ? dbConfig.getDatabaseType() : "Unavailable");
				try {
					if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), ApplicationConstants.POSTGRES_DB)) {
						log.debug("getScheduleCountByProjectId Schema name is :"+ dbConfig.getSchemaName());
						String query = "SELECT DISTINCT \"ISSUE_ID\" from \"AO_7DEABF_SCHEDULE\" where \"PROJECT_ID\" = ?";
						if(dbConfig.getSchemaName() != null && !StringUtils.equalsIgnoreCase(dbConfig.getSchemaName(),"public")) {
							query = "SELECT DISTINCT \"ISSUE_ID\" from " + dbConfig.getSchemaName() + "." + "\"AO_7DEABF_SCHEDULE\" where \"PROJECT_ID\" = ?";
						}
						log.debug("getScheduleCountByProjectId Query to be executed is :"+ query);
						scheduleCount = ao.findWithSQL(Schedule.class, "ISSUE_ID", query , projectId.intValue());
					} else if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), ApplicationConstants.MSSQL_DB)) {
						log.debug("getScheduleCountByProjectId Schema name  is :"+ dbConfig.getSchemaName());
						String query = "SELECT DISTINCT ISSUE_ID from AO_7DEABF_SCHEDULE where PROJECT_ID = ?";
						if(dbConfig.getSchemaName() != null && !StringUtils.equalsIgnoreCase(dbConfig.getSchemaName(),"public")) {
							query = "SELECT DISTINCT ISSUE_ID from " + dbConfig.getSchemaName() + "." + "AO_7DEABF_SCHEDULE where PROJECT_ID = ?";
						}
						log.debug("getScheduleCountByProjectId Query to be executed is :"+ query);
						scheduleCount = ao.findWithSQL(Schedule.class, "ISSUE_ID", query , projectId.intValue());
					} else {
						String query = "SELECT DISTINCT ISSUE_ID from AO_7DEABF_SCHEDULE where PROJECT_ID = ?";
						log.debug("getScheduleCountByProjectId Query to be executed is :"+ query);
						scheduleCount = ao.findWithSQL(Schedule.class, "ISSUE_ID",query , projectId.intValue());
					}
				} catch(Exception e) {
					e.printStackTrace();
					log.error("Error running SQL query getScheduleCountByProjectIdAndGroupby:",e);
				}
                int execCount = scheduleCount != null ? scheduleCount.length : 0;
                return execCount;
            } else {

                Query query = Query.select("ID").where("PROJECT_ID = ? and STATUS = ?", projectId.intValue(), "-1");
                Schedule[] scheduleCount = ao.find(Schedule.class, query);
                int execCount = scheduleCount != null ? scheduleCount.length : 0;
                return execCount > 0 ? execCount : 0;
            }
        } catch (Exception e) {
            log.error("Error retrieving Execution count:", e);
            return 0;
        }
    }

	@Override
	public Future<Boolean> removeSchedulesByCycleIdPromise(Long cycleId, String jobProgressToken) {
		if(StringUtils.isNotEmpty(jobProgressToken)) {
			jobProgressService.addSteps(jobProgressToken, ao.count(Schedule.class, Query.select().where("CYCLE_ID = ?", cycleId)));
		}
		final Integer[] noOfSchedules = {0};
		ExecutorService cachedPool = Executors.newCachedThreadPool();
		final ApplicationUser user = authContext.getLoggedInUser();
		Future<Boolean> callableFuture = cachedPool.submit(() -> {
			if(authContext != null && authContext.getLoggedInUser() == null) {
				authContext.setLoggedInUser(user);
			}
			final Schedule[] schedules = ao.find(Schedule.class, Query.select().where("CYCLE_ID = ?", cycleId));
			noOfSchedules[0] = deleteSchedules(schedules, jobProgressToken);
			return true;
		});
		cachedPool.shutdown();

 		return callableFuture;
	}

	@Override
	public Future<Boolean> removeSchedulesByFolderIdAndCycleIdPromise(Long projectId, Long versionId, Long cycleId, Long folderId, String jobProgressToken) {
		if(StringUtils.isNotEmpty(jobProgressToken)) {
			Query query = Query.select();
			if(cycleId != null && cycleId.equals(ApplicationConstants.AD_HOC_CYCLE_ID_LONG)) {
				if(folderId != null && !folderId.equals(ApplicationConstants.ADHOC_SYSTEM_FOLDER_ID)) {
					query.where("PROJECT_ID = ? AND VERSION_ID = ? AND CYCLE_ID IS NULL AND FOLDER_ID = ?",  projectId, versionId, folderId);
				} else {
					query.where("PROJECT_ID = ? AND VERSION_ID = ? AND CYCLE_ID IS NULL AND FOLDER_ID IS NULL",  projectId, versionId);
				}
			} else {
				if(folderId != null && !folderId.equals(ApplicationConstants.ADHOC_SYSTEM_FOLDER_ID)) {
					query.where("CYCLE_ID = ? AND FOLDER_ID = ?", cycleId, folderId);
				} else {
					query.where("CYCLE_ID = ? AND FOLDER_ID is null", cycleId);
				}
			}
			jobProgressService.addSteps(jobProgressToken, ao.count(Schedule.class, query));
		}
		final Integer[] noOfSchedules = {0};
		ExecutorService executor = Executors.newSingleThreadExecutor();
		final ApplicationUser user = authContext.getLoggedInUser();
		Future<Boolean> callableFuture = executor.submit(() -> {
			if(authContext != null && authContext.getLoggedInUser() == null) {
				authContext.setLoggedInUser(user);
			}
			Query query = Query.select();
			if(cycleId != null && cycleId.equals(ApplicationConstants.AD_HOC_CYCLE_ID_LONG)) {
				if(folderId != null && !folderId.equals(ApplicationConstants.ADHOC_SYSTEM_FOLDER_ID)) {
					query.where("PROJECT_ID = ? AND VERSION_ID = ? AND CYCLE_ID IS NULL AND FOLDER_ID = ?",  projectId, versionId, folderId);
				} else {
					query.where("PROJECT_ID = ? AND VERSION_ID = ? AND CYCLE_ID IS NULL AND FOLDER_ID IS NULL",  projectId, versionId);
				}
			} else {
				if(folderId != null && !folderId.equals(ApplicationConstants.ADHOC_SYSTEM_FOLDER_ID)) {
					query.where("CYCLE_ID = ? AND FOLDER_ID = ?", cycleId, folderId);
				} else {
					query.where("CYCLE_ID = ? AND FOLDER_ID is null", cycleId);
				}
			}
			final Schedule[] schedules = ao.find(Schedule.class, query);
			noOfSchedules[0] = deleteSchedules(schedules, jobProgressToken);
			return true;
		});
		executor.shutdown();

 		return callableFuture;
	}
	
	@Override
	public List<Schedule> getSchedulesByCycleAndFolder(Long projectId, Long versionId, Long cycleId, Integer offset, final String sortQuery, final String expandos, final Long folderId) {
		int limit = 10;
		
		String finalOrderQuery = getOrderQueryFroSchedule(sortQuery);

		//If Offset comes as -1, no pagination
		if(offset == null || offset == -1) {
			offset = -1;
			limit = -1;
		}
        // if expandos == reorderId, change the limit to 200
        if(StringUtils.isNotEmpty(expandos) && expandos.equals("reorderId"))
            limit = 200;

		Schedule [] schedules = null;
		Query query = Query.select("MODIFIED_BY,ISSUE_ID,PROJECT_ID,COMMENT,ACTUAL_EXECUTION_TIME,STATUS,DATE_CREATED,EXECUTED_ON,CREATED_BY,ORDER_ID,ASSIGNED_TO,EXECUTED_BY,VERSION_ID,FOLDER_ID");
		if(cycleId != null && cycleId.equals(ApplicationConstants.AD_HOC_CYCLE_ID_LONG)){
			if(folderId != null && !folderId.equals(ApplicationConstants.ADHOC_SYSTEM_FOLDER_ID)) {
				query.where("PROJECT_ID = ? AND VERSION_ID = ? AND CYCLE_ID IS NULL AND FOLDER_ID = ?", projectId, versionId, folderId);
			} else {
				query.where("PROJECT_ID = ? AND VERSION_ID = ? AND CYCLE_ID IS NULL AND FOLDER_ID IS NULL", projectId, versionId);
			}
		} else {
			if(folderId != null && !folderId.equals(ApplicationConstants.ADHOC_SYSTEM_FOLDER_ID)) {
				query.where("CYCLE_ID = ? AND FOLDER_ID = ?", cycleId, folderId);
			} else {
				query.where("CYCLE_ID = ? AND FOLDER_ID IS NULL", cycleId);
			}
		}
		schedules =  ao.find(Schedule.class, query.order(finalOrderQuery).offset(offset).limit(limit));
		return Arrays.asList(schedules);
	}

	@Override
	public Map<String,Long> getExecutionEstimationData(Long projectId, Long versionId, Long cycleId, final Long folderId) {
		Map<String,Long> resultMap = new HashMap<>();
		if(projectId != null) {
            DatabaseConfig dbConfig = ComponentAccessor.getComponent(DatabaseConfigurationManager.class).getDatabaseConfiguration();
			String commonSqlStr = " FROM AO_7DEABF_SCHEDULE WHERE PROJECT_ID = " + projectId ;
            String sqlQuery, sqlQueryForCount;

            if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), ApplicationConstants.POSTGRES_DB)) {
                log.debug("getExecutionEstimationData Schema name  :" + dbConfig.getSchemaName());
                commonSqlStr = " FROM \"AO_7DEABF_SCHEDULE\" WHERE \"PROJECT_ID\" = " + projectId;

                if (dbConfig.getSchemaName() != null && !StringUtils.equalsIgnoreCase(dbConfig.getSchemaName(), "public")) {
                    commonSqlStr = " FROM " +  dbConfig.getSchemaName() + "."  + "\"AO_7DEABF_SCHEDULE\" WHERE \"PROJECT_ID\" = " + projectId;
                }
            }else if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), ApplicationConstants.MSSQL_DB)) {
                log.debug("getExecutionEstimationData Schema name :"+ dbConfig.getSchemaName());
                commonSqlStr = "FROM AO_7DEABF_SCHEDULE WHERE PROJECT_ID = " + projectId ;
                if(dbConfig.getSchemaName() != null && !StringUtils.equalsIgnoreCase(dbConfig.getSchemaName(),"public")) {
                    commonSqlStr = " FROM " + dbConfig.getSchemaName() + "." +  "AO_7DEABF_SCHEDULE WHERE PROJECT_ID = " + projectId ;
                }
            }

            if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), ApplicationConstants.POSTGRES_DB)) {
                //commonSqlStr = "FROM AO_7DEABF_SCHEDULE WHERE AND \"PROJECT_ID\" = " + projectId;
                if (versionId != null){ commonSqlStr += " AND \"VERSION_ID\" = " + versionId; }
                if (cycleId != null){ commonSqlStr += " AND \"CYCLE_ID\" = " + cycleId; }
                if (folderId != null && !folderId.equals(-1L)) {
                    commonSqlStr += " AND \"FOLDER_ID\" = " + folderId;
                }
                sqlQuery = "SELECT SUM(\"ESTIMATED_TIME\") AS estimatedTime,SUM(\"LOGGED_TIME\") AS loggedTime " + commonSqlStr;
				sqlQueryForCount = "SELECT  COUNT(\"ID\") " + commonSqlStr + " AND \"ESTIMATED_TIME\" IS NULL AND \"LOGGED_TIME\" IS NULL";
            }else {
                if (versionId != null){ commonSqlStr += " AND VERSION_ID = " + versionId; }
                if (cycleId != null){ commonSqlStr += " AND CYCLE_ID = " + cycleId; }

                if (folderId != null && !folderId.equals(-1L)) {
                    commonSqlStr += " AND FOLDER_ID = " + folderId;
                }

                sqlQuery = "SELECT SUM(ESTIMATED_TIME) AS estimatedTime,SUM(LOGGED_TIME) AS loggedTime " +
                        commonSqlStr;
                sqlQueryForCount =  "SELECT  COUNT(ID) " + commonSqlStr + " AND ESTIMATED_TIME IS NULL AND LOGGED_TIME IS NULL";
            }

			SQLProcessor sqlProcessor = null;
			try {
				sqlProcessor = new SQLProcessor("defaultDS");
				ResultSet resultSet = sqlProcessor.executeQuery(sqlQuery);
				while (resultSet.next()) {
					resultMap.put(ApplicationConstants.FOLDER_LEVEL_ESTIMATED_TIME, resultSet.getLong(1));
					resultMap.put(ApplicationConstants.FOLDER_LEVEL_LOGGED_TIME, resultSet.getLong(2));
				}

                resultSet = sqlProcessor.executeQuery(sqlQueryForCount);
                while (resultSet.next()) {
                    resultMap.put(ApplicationConstants.FOLDER_LEVEL_EXECUTIONS_LOGGED, resultSet.getLong(1));
                }
				resultSet.close();

			} catch (Exception ex) {
				log.error("Error while executing the query - " + sqlQuery);
				throw new RuntimeException("Error while executing the query - " + sqlQuery);
			} finally {
				try {
					if (sqlProcessor != null)
						sqlProcessor.close();
				} catch (Exception ex) {
					log.error("Error while closing the sql processor connection ");
				}
			}
		}
		return resultMap;
	}


	private String getOrderQueryFroSchedule(String sortQuery) {
		String secondarySortColumn = null;
		//Default Second Level Sort to ID DESC
		secondarySortColumn = "ORDER_ID";
		String finalOrderQuery = null;
		if(!StringUtils.isBlank(sortQuery)) {
			String[] sorter = StringUtils.split(sortQuery,":");
			if(sorter.length == 2) {
				if(StringUtils.equalsIgnoreCase(sorter[0],"ID")){
					 sorter[0] = "ISSUE_ID";
				} else if(StringUtils.equalsIgnoreCase(sorter[0],"ExecutionStatus")){
				 	 sorter[0] = "STATUS";
				} else if(StringUtils.equalsIgnoreCase(sorter[0],"ExecutionDate")){
				 	 sorter[0] = "EXECUTED_ON";
				} else if(StringUtils.equalsIgnoreCase(sorter[0],"ExecutedBy")){
				 	 sorter[0] = "EXECUTED_BY";
				} else if(StringUtils.equalsIgnoreCase(sorter[0],"OrderId")){
                    sorter[0] = "ORDER_ID";
                }
                String secondaryColumnSortSQLClause = null;
                /*To detect and avoid duplication of sql column - mssql doesnt like duplicate columns in order By*/
                if(!StringUtils.equalsIgnoreCase(sorter[0], secondarySortColumn)){
                    DatabaseConfig dbConfig = ComponentAccessor.getComponent(DatabaseConfigurationManager.class).getDatabaseConfiguration();
                    if(StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), "postgres") && PluginUtils.isAOVersionLessThan23()){
                        secondaryColumnSortSQLClause = "\"" + secondarySortColumn + "\" DESC";
                    }else{
                        secondaryColumnSortSQLClause = secondarySortColumn + " " + sorter[1];
                    }
                }

				/*quoting second order By column, workaround for http://bit.ly/zqNxsI*/

                if(secondaryColumnSortSQLClause != null)
					finalOrderQuery = sorter[0] + " " + sorter[1] + ", " + secondaryColumnSortSQLClause;
				else
					finalOrderQuery = sorter[0] + " " + sorter[1];
			}
		}
		if(finalOrderQuery == null){
			finalOrderQuery = secondarySortColumn + " DESC";
		}
		return finalOrderQuery;
	}

    @Override
    public List<Schedule> getAllSchedulesByProjectIds(String placeholderCommaList, List<Integer> projectIdArray, Integer offset, Integer limit) {
		try {
			if(offset == null)
				offset = 0;
			if(limit==null)
				limit = ApplicationConstants.REINDEX_BATCH_SIZE;

			log.debug("getAllSchedulesByProjectIds:::"+projectIdArray.toString());
			Schedule[] schedules = ao.find(Schedule.class, Query.select().where("PROJECT_ID IN (" + placeholderCommaList + ")", projectIdArray.toArray()).offset(offset).limit(limit));
			log.debug("getAllSchedulesByProjectIds:::"+schedules.length);
			return Arrays.asList(schedules);
		} catch(Exception e) {
			e.printStackTrace();
			log.error("Error Retrieving Schedules by Project ID:",e);
		}
        return new ArrayList<>();
    }

    @Override
    public Integer getScheduleCountByProjectIds(String placeholderCommaList, List<Integer> projectIds) {
		return ao.count(Schedule.class,Query.select().where("PROJECT_ID IN (" + placeholderCommaList + ")", projectIds.toArray(new Integer[projectIds.size()])));
    }
    
    @Override
    public List<Schedule> getSchedules(final Long versionId, final Long projectId, final Integer cycleId, Integer offset, final String sortQuery, final String expandos, Long folderId, Integer limit) {
		String finalOrderQuery = getOrderQueryFroSchedule(sortQuery);

        if(null == limit || limit == -1) {
            limit = 10;
        }

		//If Offset comes as -1, no pagination
		if(offset == null || offset == -1) {
			offset = -1;
			limit = -1;
		}
        // if expandos == reorderId, change the limit to 200
        if(StringUtils.isNotEmpty(expandos) && expandos.equals("reorderId"))
            limit = 200;

		Schedule [] schedules = null;
		Query query = Query.select("MODIFIED_BY,ISSUE_ID,PROJECT_ID,COMMENT,ACTUAL_EXECUTION_TIME,STATUS,DATE_CREATED,EXECUTED_ON,CREATED_BY,ORDER_ID,ASSIGNED_TO,EXECUTED_BY,VERSION_ID,FOLDER_ID");
		if(cycleId == null || cycleId.intValue() == ApplicationConstants.AD_HOC_CYCLE_ID){
			if(folderId != null && !folderId.equals(-1l) && !folderId.equals(ApplicationConstants.ADHOC_SYSTEM_FOLDER_ID)) {
				 query.where("PROJECT_ID = ? AND VERSION_ID = ? AND CYCLE_ID is NULL AND FOLDER_ID = ?", projectId, versionId, folderId);
			} else {
				 query.where("PROJECT_ID = ? AND VERSION_ID = ? AND CYCLE_ID is NULL AND FOLDER_ID IS NULL", projectId, versionId);
			}
		} else {
			if(folderId != null && !folderId.equals(-1l) && !folderId.equals(ApplicationConstants.ADHOC_SYSTEM_FOLDER_ID)) {
				query.where("CYCLE_ID = ? AND FOLDER_ID = ?", cycleId, folderId);
			} else {
				query.where("CYCLE_ID = ? AND FOLDER_ID IS NULL", cycleId);
			}		
		}
		schedules =  ao.find(Schedule.class, query.order(finalOrderQuery).offset(offset).limit(limit));
		return Arrays.asList(schedules);
	}
    
    @Override
	public List<ExecutionSummaryImpl> getExecutionDetailsByCycleAndFolder(List<Long> projectIdList, String[] versionIds, Long cycleId, Long folderId, String userName) {
		final Set<Entry<Integer, ExecutionStatus>> statuses = JiraUtil.getExecutionStatuses().entrySet();
		return getExecutionDetailsByCurrentUserAndCycleAndFolder(statuses, projectIdList, versionIds, cycleId, folderId, userName);
	}
    
    private List<ExecutionSummaryImpl> getExecutionDetailsByCurrentUserAndCycleAndFolder(Set<Entry<Integer, ExecutionStatus>> statuses, List<Long> projectIdList, String[] versionIds, Long cycleId, Long folderId, String userName) {
		List<ExecutionSummaryImpl> summaryList = new ArrayList<ExecutionSummaryImpl>(statuses.size());
		StringBuilder whereClause = new StringBuilder();
		List<Object> params = new ArrayList<Object>();
		String ques[] = new String[versionIds.length];
		for(int i=0; i< versionIds.length; i++){
			ques[i] = "?";
		}
		String quesProj[] = new String[projectIdList.size()];
		if(projectIdList != null) {
			for(int i=0; i< projectIdList.size(); i++){
				quesProj[i] = "?";
			}			
		}
		if(quesProj.length > 0) {
			whereClause.append("PROJECT_ID IN ( " + StringUtils.join(quesProj, ',') + " )");
		}
		if(ques.length > 0) {
			whereClause.append(" AND VERSION_ID IN ( " + StringUtils.join(ques, ',') + " )");
		}
		
		Iterator<Long> itrProj = projectIdList.iterator();
		while (itrProj.hasNext()) {
			Long longVal = 0l;
			Object val = itrProj.next();
			if(val instanceof Number)
				longVal = ((Number)val).longValue();
			if(val instanceof String)
				longVal = Long.parseLong((String)val);
			params.add(longVal);
		}
		
		List<String> versionList = Arrays.asList(versionIds);
		Iterator<String> itr = versionList.listIterator();
		while (itr.hasNext()) {
			Long longVal = 0l;
			Object val = itr.next();
			if(val instanceof Number)
				longVal = ((Number)val).longValue();
			if(val instanceof String)
				longVal = Long.parseLong((String)val);
			params.add(longVal);
		}
		
		for(Entry<Integer, ExecutionStatus> statusEntry : statuses) {
			Integer cnt = 0;
			String statusKey = statusEntry.getKey().toString();
			Query query = Query.select();
			List<Object> clonedParams = new ArrayList<>(params);
			if(StringUtils.isBlank(userName)) {
				if(cycleId != null && cycleId.equals(ApplicationConstants.AD_HOC_CYCLE_ID_LONG)) {
					clonedParams.add(statusKey);
					if(folderId != null && !folderId.equals(ApplicationConstants.ADHOC_SYSTEM_FOLDER_ID)) {
						clonedParams.add(folderId);
						query.where(whereClause.toString() + " AND CYCLE_ID IS NULL AND STATUS = ? AND FOLDER_ID = ?", clonedParams.toArray());	
					} else {
						query.where(whereClause.toString() + " AND CYCLE_ID IS NULL AND FOLDER_ID IS NULL AND STATUS = ?", clonedParams.toArray());
					}					
				} else {					
					if(folderId != null && !folderId.equals(ApplicationConstants.ADHOC_SYSTEM_FOLDER_ID)) {
						query.where("CYCLE_ID = ? AND FOLDER_ID = ? AND  STATUS = ?", cycleId, folderId, statusKey);	
					} else {
						query.where("CYCLE_ID = ? AND FOLDER_ID IS NULL AND  STATUS = ?", cycleId, statusKey);
					}
				}
			} else {
				if(cycleId != null && cycleId.equals(ApplicationConstants.AD_HOC_CYCLE_ID_LONG)) {
					clonedParams.add(statusKey);clonedParams.add(userName);
					if(folderId != null && !folderId.equals(ApplicationConstants.ADHOC_SYSTEM_FOLDER_ID)) {
						clonedParams.add(folderId);
						query.where(whereClause.toString() + " AND CYCLE_ID IS NULL AND STATUS = ? AND EXECUTED_BY = ? AND FOLDER_ID = ?", clonedParams.toArray());	
					} else {
						query.where(whereClause.toString() + " AND CYCLE_ID IS NULL AND FOLDER_ID IS NULL AND STATUS = ? AND EXECUTED_BY = ?",  clonedParams.toArray());
					}					
				} else {					
					if(folderId != null && !folderId.equals(ApplicationConstants.ADHOC_SYSTEM_FOLDER_ID)) {
						query.where("CYCLE_ID = ? AND FOLDER_ID = ? AND STATUS = ? AND EXECUTED_BY = ?", cycleId, folderId, statusKey, userName);	
					}  else {
						query.where("CYCLE_ID = ? AND FOLDER_ID IS NULL AND  STATUS = ? AND EXECUTED_BY = ?", cycleId, statusKey, userName);
					}
				}
			}
			cnt = ao.count(Schedule.class, query);
			ExecutionSummaryImpl summary = new ExecutionSummaryImpl(cnt, statusEntry.getKey(),statusEntry.getValue().getName(),
					statusEntry.getValue().getDescription(),statusEntry.getValue().getColor());
			summaryList.add(summary);
		}
		return summaryList;		
	}

	@Override
	public Integer getTotalDefectsCountByCycle(Long cycleId, Long projectId, Long versionId) {
		Query query = Query.select();
		query.alias(Schedule.class, "schedule");
		query.alias(ScheduleDefect.class, "defect");
		query.join(ScheduleDefect.class, "schedule.ID = defect.SCHEDULE_ID");
		if (cycleId != null && cycleId.equals(ApplicationConstants.AD_HOC_CYCLE_ID_LONG)) {
			query.where("schedule.CYCLE_ID IS NULL AND schedule.PROJECT_ID = ? AND schedule.VERSION_ID", cycleId, projectId, versionId);
		} else {
			query.where("schedule.CYCLE_ID = ? ", cycleId);
		}
		return ao.count(Schedule.class, query);
	}
	
	@Override
	public Integer getScheduleCount(java.util.Optional<Date> dateOptional, java.util.OptionalLong projectId, java.util.Optional<Boolean> shouldBeGreater) {
		Query query = Query.select();
		if(dateOptional.isPresent() && projectId.isPresent() && shouldBeGreater.isPresent()) {
			if(shouldBeGreater.get()) {
				query.where("DATE_CREATED >= ? AND PROJECT_ID = ?", dateOptional.get(), projectId.getAsLong());
			} else {
				query.where("DATE_CREATED < ? AND PROJECT_ID = ?", dateOptional.get(), projectId.getAsLong());
			}
		} else if(projectId.isPresent())  {
			query.where("PROJECT_ID = ?", projectId.getAsLong());
		} else if(dateOptional.isPresent() && shouldBeGreater.isPresent()){
			if(shouldBeGreater.get()) {
				query.where("DATE_CREATED >= ?", dateOptional.get());
			} else {
				query.where("DATE_CREATED < ?", dateOptional.get());
			}			
		}
		return ao.count(Schedule.class, query);
	}

	@Override
	public List<Schedule> getSchedulesByPagination(Integer offset, Integer limit) {
		if(offset==null) offset = 0; if(limit==null) limit = ApplicationConstants.REINDEX_BATCH_SIZE;
		Schedule[] schedules =  ao.find(Schedule.class,Query.select()
				.distinct()
				.offset(offset)
				.limit(limit)
		);
		if(schedules != null) {
			return Arrays.asList(schedules);
		}
		return new ArrayList<Schedule>(0);
	}
	
	public List<Schedule> getSchedulesByCycle(Long projectId, Long versionId, Long cycleId, Integer offset, final String sortQuery, final String expandos) {
		int limit = 10;
		
		String finalOrderQuery = getOrderQueryFroSchedule(sortQuery);

		//If Offset comes as -1, no pagination
		if(offset == null || offset == -1) {
			offset = -1;
			limit = -1;
		}
        // if expandos == reorderId, change the limit to 200
        if(StringUtils.isNotEmpty(expandos) && expandos.equals("reorderId"))
            limit = 200;

		Schedule [] schedules = null;
		Query query = Query.select();
		if(cycleId != null && cycleId.equals(ApplicationConstants.AD_HOC_CYCLE_ID_LONG)){
			query.where("PROJECT_ID = ? AND VERSION_ID = ? AND CYCLE_ID IS NULL", projectId, versionId);
		} else {
			query.where("CYCLE_ID = ?", cycleId);
		}
		schedules =  ao.find(Schedule.class, query.order(finalOrderQuery).offset(offset).limit(limit));
		return Arrays.asList(schedules);
	}

    private class ProcessExecutionSummaryCount implements Callable<ExecutionSummaryImpl> {

	    private String statusKey;
	    private ExecutionStatus executionStatus;
	    private String userName;
	    private Long projectId;
	    private Long versionId;
	    private Integer cycleId;


        public ProcessExecutionSummaryCount(String statusKey, ExecutionStatus executionStatus, String userName,
                                            Long projectId, Long versionId, Integer cycleId) {
            this.statusKey = statusKey;
            this.executionStatus = executionStatus;
            this.userName = userName;
            this.projectId = projectId;
            this.versionId = versionId;
            this.cycleId = cycleId;
        }

        @Override
        public ExecutionSummaryImpl call() throws Exception {

            Integer count;
            if(StringUtils.isBlank(userName)) {
                if(cycleId == null || cycleId == -1)
                    count = ao.count(Schedule.class, Query.select().where("PROJECT_ID = ? AND VERSION_ID = ? AND CYCLE_ID IS NULL AND STATUS = ?", projectId, versionId, statusKey));
                else
                    count = ao.count(Schedule.class, Query.select().where("CYCLE_ID = ? AND STATUS = ?", cycleId, statusKey));
            } else {
                if(cycleId == null || cycleId == -1)
                    count = ao.count(Schedule.class, Query.select().where("PROJECT_ID = ? AND VERSION_ID = ? AND CYCLE_ID IS NULL AND STATUS = ? AND EXECUTED_BY = ?", projectId, versionId, statusKey, userName));
                else
                    count = ao.count(Schedule.class, Query.select().where("CYCLE_ID = ? AND STATUS = ? AND EXECUTED_BY = ?", cycleId, statusKey, userName));
            }
            if(count == null) count = new Integer(0);
            ExecutionSummaryImpl summary = new ExecutionSummaryImpl(count, Integer.valueOf(statusKey),executionStatus.getName(),
                    executionStatus.getDescription(),executionStatus.getColor());
            return summary;
        }
    }

    @Override
    public List<Long> getScheduleIdsByPagination(java.util.Optional<Date> dateOptional, java.util.OptionalLong projectId, Integer offset, Integer limit, java.util.Optional<Boolean> shouldBeGreater) {
    	if (offset == null)
            offset = 0;
        if (limit == null)
            limit = ApplicationConstants.REINDEX_BATCH_SIZE;
        Query query = Query.select();
        if(dateOptional.isPresent() && shouldBeGreater.isPresent() && projectId.isPresent()) {
        	if(shouldBeGreater.get()) {
        		query.where("DATE_CREATED >= ? AND PROJECT_ID = ?", dateOptional.get(), projectId.getAsLong());
        	} else {
        		query.where("DATE_CREATED < ? AND PROJECT_ID = ?", dateOptional.get(), projectId.getAsLong());
        	}
        } else if(dateOptional.isPresent() && shouldBeGreater.isPresent()) {
        	if(shouldBeGreater.get()) {
        		query.where("DATE_CREATED >= ?", dateOptional.get());
        	} else {
        		query.where("DATE_CREATED < ?", dateOptional.get());
        	}
        }
        Schedule[] schedules = ao.find(Schedule.class, query.distinct().offset(offset).limit(limit));
        if(schedules != null && schedules.length > 0) {
        	List<Long> allScheduleIds = Stream.of(schedules).map(schedule -> Long.valueOf(schedule.getID())).collect(Collectors.toList());
            return allScheduleIds;
        } else {
        	return new ArrayList<>(0);
        }
    }

    @Override
    public List<String> getAllScheduleIdsByProjectIds(String placeholderCommaList, List<Integer> projectIdArray, Integer offset, Integer limit) {
        return getAllScheduleIdsByProjectIds(placeholderCommaList, projectIdArray, offset, limit, java.util.Optional.empty());
    }
    
    @Override
    public List<String> getAllScheduleIdsByProjectIdsByCreatedDate(String placeholderCommaList, List<Integer> projectIdArray, Integer offset, Integer limit, Date createdDate) {
        return getAllScheduleIdsByProjectIds(placeholderCommaList, projectIdArray, offset, limit, java.util.Optional.of(createdDate));
    }
    
    private List<String> getAllScheduleIdsByProjectIds(String placeholderCommaList, List<Integer> projectIdArray, Integer offset, Integer limit, java.util.Optional<Date> createdDate) {
        try {
        	List<Object> dataList = new ArrayList<>(projectIdArray);
            if (offset == null)
                offset = 0;
            if (limit == null)
                limit = ApplicationConstants.REINDEX_BATCH_SIZE;
            Query query = Query.select();
            if(createdDate.isPresent()) {
            	dataList.add(createdDate.get());
            	query.where("PROJECT_ID IN (" + placeholderCommaList + ") AND  DATE_CREATED <= ?", dataList.toArray());
            } else {
            	query.where("PROJECT_ID IN (" + placeholderCommaList + ")", dataList.toArray());
            }

            log.debug("getAllScheduleIdsByProjectIds :::" + projectIdArray.toString());
            Schedule[] schedules = ao.find(Schedule.class, query.offset(offset).limit(limit));
            if(schedules != null && schedules.length > 0) {
            	List<String> allScheduleIds = Stream.of(schedules).map(schedule -> String.valueOf(schedule.getID())).collect(Collectors.toList());
                return allScheduleIds;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error Retrieving Schedules by Project ID:", e);
        }
        return new ArrayList<>(0);
    }

	@Override
	public boolean existsSchedule(int scheduleId) {
		int scheduleCount =  ao.count(Schedule.class, Query.select().where("ID = ?", scheduleId));
		if(scheduleCount > 0) {
			return true;
		}
		return false;
	}

	@Override
	public ReindexJobProgress saveReindexJobProgress(Map<String, Object> reindexJobProgressProperties) {
		if(reindexJobProgressProperties == null || reindexJobProgressProperties.size() == 0){
			throw new RuntimeException("Unable to create reindex job progress with empty data");
		}
		return ao.create(ReindexJobProgress.class, reindexJobProgressProperties);
	}

	@Override
	public List<ReindexJobProgress> getReindexJobProgress(String name, java.util.OptionalLong projectId) {
		Query query = Query.select();
		if(projectId.isPresent()) {
			query.where("NAME = ? AND PROJECT_ID = ?", name, projectId.getAsLong());
		} else {
			query.where("NAME = ?", name);
		}
		ReindexJobProgress[] reindexJobProgresses = ao.find(ReindexJobProgress.class, query);
		if(reindexJobProgresses == null || reindexJobProgresses.length == 0) {
			return new ArrayList<>(0);
		}
		return Arrays.asList(reindexJobProgresses);
	}
	
	@Override
	public List<ReindexJobProgress> getReindexJobProgress(String name, List<Long> projectIdList, String placeholderCommaList) {
		ReindexJobProgress[] reindexJobProgresses = ao.find(ReindexJobProgress.class, Query.select().where("PROJECT_ID IN (" + placeholderCommaList + ")", projectIdList.toArray()));
		if(reindexJobProgresses == null || reindexJobProgresses.length == 0) {
			return new ArrayList<>(0);
		}
		return Arrays.asList(reindexJobProgresses);
	}

	@Override
	public Integer getTotalSchedulesCount() {
		return ao.count(Schedule.class, Query.select());
	}
	

	public List<Schedule> bulkAssignCustomFields(Schedule[] schedules, Map<String, CustomFieldValueRequest> customFieldRequests, User loggedInUser, String jobProgressToken) {
		Gson gson = new Gson();
		List<Schedule> processedList = new ArrayList<>();
        Map<Long, CustomField> customFields = new HashMap<>();
        Map<Long, List<Long>> customFieldsProjectMapping = Maps.newHashMap();
        String userName = UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(ComponentAccessor.getJiraAuthenticationContext()));
        customFieldRequests.entrySet().forEach(customFieldRequest -> {
            Long customFieldId = Long.valueOf(customFieldRequest.getKey());
            CustomField customField = zephyrCustomFieldManager.getCustomFieldById(customFieldId);
            if(Objects.nonNull(customField)) {
                customFields.putIfAbsent(customFieldId,customField);
                CustomFieldProject[] customFieldProjects = zephyrCustomFieldManager.getActiveCustomFieldProjects(customFieldId);
                List<Long> associatedProjectList;
                if(Objects.nonNull(customFieldProjects) && customFieldProjects.length > 0) {
                    associatedProjectList = getAssociatedProjectList(customFieldProjects);
                    if(CollectionUtils.isNotEmpty(associatedProjectList)) {
                        customFieldsProjectMapping.put(customFieldId,associatedProjectList);
                    }
                }
            }
        });

        CustomFieldValueResourceDelegate customFieldValueResourceDelegate = (CustomFieldValueResourceDelegate) ZephyrComponentAccessor.getInstance().getComponent("customFieldValueResourceDelegate");
        for (Schedule schedule : schedules) {
            Table<String, String, Object> bulkAssignChangePropertyTable = HashBasedTable.create();

            AtomicInteger recordCreated = new AtomicInteger(0);
            customFieldRequests.entrySet().forEach(customFieldRequest -> {
                String json = gson.toJson(customFieldRequest.getValue());
                CustomFieldValueRequest customFieldValueRequest = gson.fromJson(json,CustomFieldValueRequest.class);
                Map<String, Object> customFieldValueProperties = prepareRequestForCustomFieldValue(customFieldValueRequest,customFields,schedule,customFieldsProjectMapping);
                if(MapUtils.isNotEmpty(customFieldValueProperties)) {
                    try {
                        CustomField customField = customFields.get(customFieldValueRequest.getCustomFieldId());
                        Table<String, String, Object> changePropertyTable = customFieldValueResourceDelegate.createOrUpdateCustomFieldRecordForBulkAssign(customFieldValueProperties,customField.getName());
                        bulkAssignChangePropertyTable.putAll(changePropertyTable);
                        customFieldValueManager.saveOrUpdateExecutionCustomFieldValue(customFieldValueProperties);
                        recordCreated.addAndGet(1);
                    } catch (Exception exception) {
                        log.error("Error occurred while associating custom fields in bulk for schedule ID:" + schedule.getID(), exception);
                    }
                }
            });

            schedule.setModifiedBy(userName);
            //setting modified date
            schedule.setModifiedDate(new Date());
            schedule.save();

            if(null != bulkAssignChangePropertyTable) {
                eventPublisher.publish(new ScheduleModifyEvent(schedule, bulkAssignChangePropertyTable, EventType.EXECUTION_UPDATED,
                        UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));
            }

            if(recordCreated.get() > 0) {
                processedList.add(schedule);
            }
            jobProgressService.addCompletedSteps(jobProgressToken, 1);
        }
        return processedList;
    }

    @Override
	public List<Schedule> getSchedulesByCycleAndFolder(final Long versionId, final Long projectId, final String[] cycleIdArr,
			final String folders) {

		Schedule[] schedules = null;
		List<String> whereClauses = new ArrayList<>();

		/* Prepare where clause */
		List<Object> params = new ArrayList<>();
		whereClauses.add(" PROJECT_ID = " + projectId);
		whereClauses.add(" VERSION_ID = " + versionId);

		/* Prepare cycleIdArray */

		String ques[] = preparePlaceHoldersAndParamForWhereClause(cycleIdArr,params);
		whereClauses.add(" CYCLE_ID IN ( " + StringUtils.join(ques, ',') + " ) ");
		/* Prepare folderIdArray */
		if (StringUtils.isNotBlank(folders) && !folders.equals("-1")) {
			String[] folderIdArr = StringUtils.split(folders, "|");
			folderIdArr = (String[]) ArrayUtils.removeElement(folderIdArr,
					String.valueOf(ApplicationConstants.ADHOC_SYSTEM_FOLDER_ID));
			if (folderIdArr != null) {
				String quesFolders[] = preparePlaceHoldersAndParamForWhereClause(folderIdArr,params);
				whereClauses.add(" FOLDER_ID IN ( " + StringUtils.join(quesFolders, ',') + " ) ");				
			}
		}
		Query query = Query.select("CYCLE_ID,ESTIMATED_TIME,LOGGED_TIME");
		if (whereClauses.size() > 0) {
			query = Query.select().where(StringUtils.join(whereClauses, " and "), params.toArray()).order("CYCLE_ID ASC");
			schedules = ao.find(Schedule.class, query);
		} else {
			schedules = ao.find(Schedule.class, query);
		}
		return Arrays.asList(schedules);
	}

    @Override
    public void cloneCustomFields(int scheduleId, Schedule newSchedule, boolean clearCustomFields) {
        if(!clearCustomFields){
            List<ExecutionCf> allCFs = customFieldValueManager.getCustomFieldValuesForExecution(scheduleId);
            if(allCFs != null){
                for(ExecutionCf executionCf : allCFs ){
                	if(null != zephyrCustomFieldManager.getCustomFieldProjectByCustomFieldAndProjectId(executionCf.getCustomField().getID(), newSchedule.getProjectId())) {
						Map<String, Object> customFieldValueProperties = prepareRequestForCustomFieldValue(executionCf);
						customFieldValueProperties.put("EXECUTION_ID", newSchedule.getID());
						customFieldValueManager.saveExecutionCustomFieldValue(customFieldValueProperties);
					}
                }
            }
        }
    }
    
	private String[] preparePlaceHoldersAndParamForWhereClause(String [] arr, List<Object> params) {
		String ques[] = new String[arr.length];
		Integer[] ids = new Integer[arr.length];
		for (int i = 0; i < arr.length; i++) {
			ques[i] = "?";
			ids[i] = Integer.valueOf(arr[i]);
		}
		params.addAll(Arrays.asList(ids));
		return ques;
	}

    /**
     *
     * @param customField
     * @param projectId
     * @param disableCustomFieldsProjectMapping
     * @return
     */
    private boolean isCustomFieldAssociatedForProject(CustomField customField, Long projectId, Map<Long, List<Long>> disableCustomFieldsProjectMapping) {
        List<Long> associatedProjectList = disableCustomFieldsProjectMapping.get(Long.valueOf(customField.getID()));

        if(CollectionUtils.isNotEmpty(associatedProjectList)) {
            return associatedProjectList.contains(projectId);
        }
        return Boolean.FALSE;
    }

    private List<Long> getAssociatedProjectList(CustomFieldProject[] customFieldProjects) {
        return Arrays.stream(customFieldProjects).map(CustomFieldProject::getProjectId).collect(Collectors.toList());
    }

    /**
     *
     * @param request
     * @param customFields
     * @param schedule
     * @param customFieldsProjectMapping
     * @return
     */
    private Map<String, Object> prepareRequestForCustomFieldValue(CustomFieldValueRequest request, Map<Long, CustomField> customFields, Schedule schedule, Map<Long, List<Long>> customFieldsProjectMapping) {

        CustomField customField = customFields.get(request.getCustomFieldId());
        if(isCustomFieldAssociatedForProject(customField,schedule.getProjectId(),customFieldsProjectMapping)) {
            // create custom field value record if custom field is associated for the project to which schedule belongs.
            Map<String, Object> customFieldValueProperties = new HashMap<>();
            customFieldValueProperties.put("CUSTOM_FIELD_ID", request.getCustomFieldId());
            if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customField.getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.STRING_VALUE) ||
                    ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customField.getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.LIST_VALUE)) {
                customFieldValueProperties.put(ApplicationConstants.STRING_VALUE, request.getValue());
            } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customField.getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.LARGE_VALUE)) {
                customFieldValueProperties.put(ApplicationConstants.LARGE_VALUE, request.getValue());
            } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customField.getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.NUMBER_VALUE)) {
                customFieldValueProperties.put(ApplicationConstants.NUMBER_VALUE, NumberUtils.toDouble(request.getValue()));
            } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customField.getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.DATE_VALUE)
                    || ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customField.getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.DATE_TIME_VALUE)) {
                try {
                    if (StringUtils.isEmpty(request.getValue())) {
                        Date inputDate = null;
                        customFieldValueProperties.put(ApplicationConstants.DATE_VALUE, inputDate);
                    } else {
                        Date userInputDate = new Date(Long.parseLong(request.getValue()) * 1000);
                        customFieldValueProperties.put(ApplicationConstants.DATE_VALUE, userInputDate);
                    }
                } catch (Exception e) {
                    log.error("Exception occurred while parsing the date", e);
                }
            }
            if (StringUtils.isNotBlank(request.getSelectedOptions())) {
                customFieldValueProperties.put("SELECTED_OPTIONS", request.getSelectedOptions());
            }
            customFieldValueProperties.put("EXECUTION_ID", schedule.getID());
            return customFieldValueProperties;
        }
        return null;
    }
	
	@Override
    public Integer getScheduleCountByProjectIds(String placeholderCommaList, List<Integer> projectIds, Date createdDate) {
    	List<Object> dataList = new ArrayList<>(projectIds);
    	dataList.add(createdDate);
		return ao.count(Schedule.class,Query.select().where("PROJECT_ID IN (" + placeholderCommaList + ") AND DATE_CREATED < ?", dataList.toArray()));
    }
	
	@Override
	public void updateCurrentDateForAllReindexJobProgress() {
		Date currentDate = Calendar.getInstance().getTime();
		ReindexJobProgress[] reindexJobProgresses = ao.find(ReindexJobProgress.class, Query.select());
		if(reindexJobProgresses != null && reindexJobProgresses.length > 0) {
			Stream.of(reindexJobProgresses).forEach(reindexJobProgress -> {
				reindexJobProgress.setDateIndexed(currentDate);
        		reindexJobProgress.save();
			});
		}
	}

	/**
	 *
	 * @param cycleId
	 * @param statuses
	 * @param versionId
	 * @param projectId
	 * @return
	 */
	@Override
	public Map<Integer, Map<String, Object>> getExecutionDetailsByCycleAndStatus(final Set<Entry<Integer, ExecutionStatus>> statuses, Long versionId, Long projectId, List<Cycle> cycles) {
		DatabaseConfig dbConfig = ComponentAccessor.getComponent(DatabaseConfigurationManager.class).getDatabaseConfiguration();
		List<Integer> cycleIds = new ArrayList<>();
		if(cycles != null) {
			cycleIds = cycles.stream().map(cycle -> cycle.getID()).collect(Collectors.toList());
		}
		List<String> statusList = statuses.stream().map(status -> String.valueOf(status.getKey())).collect(Collectors.toList());
		SQLProcessor sqlProcessor = null;
		String sqlQuery = null;
		int count = 0;
		StringBuffer innerSQL = new StringBuffer();
		final Map<Integer,Map<String, Object>> cycleCountByStatus = new LinkedHashMap<>();
		final AtomicInteger statCount = new AtomicInteger(statusList.size());
		buildSumOfExecutionStatusQuery(dbConfig, statusList, innerSQL, statCount);
		try {
			sqlProcessor = new SQLProcessor("defaultDS");
			if(cycleIds != null && cycleIds.size() > 0) {
				do {
					List<Integer> subList = cycleIds != null && cycleIds.size() > count + ApplicationConstants.MAX_IN_QUERY ? cycleIds.subList(count, count + ApplicationConstants.MAX_IN_QUERY) : cycleIds;

					sqlQuery = "SELECT CYCLE_ID," + innerSQL.toString() + " from AO_7DEABF_SCHEDULE WHERE PROJECT_ID = " + projectId + " AND VERSION_ID = " + versionId + " AND CYCLE_ID IN (" + StringUtils.join(subList, ",") + " ) group by CYCLE_ID";
					if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), ApplicationConstants.POSTGRES_DB)) {
						sqlQuery = "SELECT \"CYCLE_ID\"," + innerSQL.toString() + " from \"AO_7DEABF_SCHEDULE\" WHERE \"PROJECT_ID\" = " + projectId + " AND \"VERSION_ID\" = " + versionId + " AND \"CYCLE_ID\" IN (" + StringUtils.join(subList, ",") + " ) group by \"CYCLE_ID\"";
					} else if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), ApplicationConstants.MSSQL_DB)) {
						if(dbConfig.getSchemaName() != null && !StringUtils.equalsIgnoreCase(dbConfig.getSchemaName(),"public")) {
							sqlQuery = "SELECT CYCLE_ID," + innerSQL.toString() + " from " + dbConfig.getSchemaName() + "." + "AO_7DEABF_SCHEDULE WHERE PROJECT_ID = " + projectId + " AND VERSION_ID = " + versionId + " AND CYCLE_ID IN (" + StringUtils.join(subList, ",") + " ) group by CYCLE_ID";
						}
					}
					ResultSet resultSet = null;
					executeCycleGroupBySqlQuery(statusList, sqlProcessor, sqlQuery, cycleCountByStatus, resultSet);
					count += subList.size();
				} while (count < cycleIds.size());
			} else {
				sqlQuery = "SELECT CYCLE_ID," + innerSQL.toString() + " from AO_7DEABF_SCHEDULE WHERE PROJECT_ID = " + projectId + " AND VERSION_ID = " + versionId + " AND CYCLE_ID IS NULL group by CYCLE_ID";
				if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), ApplicationConstants.POSTGRES_DB)) {
					sqlQuery = "SELECT \"CYCLE_ID\"," + innerSQL.toString() + " from \"AO_7DEABF_SCHEDULE\" WHERE \"PROJECT_ID\" = " + projectId + " AND \"VERSION_ID\" = " + versionId + " AND \"CYCLE_ID\" IS NULL group by \"CYCLE_ID\"";
				} else if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), ApplicationConstants.MSSQL_DB)) {
					if(dbConfig.getSchemaName() != null && !StringUtils.equalsIgnoreCase(dbConfig.getSchemaName(),"public")) {
						sqlQuery = "SELECT CYCLE_ID," + innerSQL.toString() + " from " + dbConfig.getSchemaName() + "." + "AO_7DEABF_SCHEDULE WHERE PROJECT_ID = " + projectId + " AND VERSION_ID = " + versionId + " AND CYCLE_ID IS NULL group by CYCLE_ID";
					}
				}
				ResultSet resultSet = null;
				executeCycleGroupBySqlQuery(statusList, sqlProcessor, sqlQuery, cycleCountByStatus, resultSet);
			}
		} catch (Exception ex) {
			log.error("Error while executing the query - " + sqlQuery);
		} finally {
			try {
				if (sqlProcessor != null)
					sqlProcessor.close();
			} catch (Exception ex) {
				log.error("Error while closing the sql processor connection ");
			}
		}
		return cycleCountByStatus;
	}


	/**
	 *
	 * @param cycleId
	 * @param statuses
	 * @param versionId
	 * @param projectId
	 * @param users
	 * @return
	 */
	@Override
	public Map<String, Map<String, Object>> getExecutionDetailsByExecutorAndStatus(final Set<Entry<Integer, ExecutionStatus>> statuses, Long versionId, Long projectId, Collection<User> users) {
		DatabaseConfig dbConfig = ComponentAccessor.getComponent(DatabaseConfigurationManager.class).getDatabaseConfiguration();
		List<String> userKeys = new ArrayList<>();
		if(users != null) {
			userKeys = users.stream().map(user -> UserCompatibilityHelper.getKeyForUser(user)).collect(Collectors.toList());
		}
		List<String> statusList = statuses.stream().map(status -> String.valueOf(status.getKey())).collect(Collectors.toList());
		SQLProcessor sqlProcessor = null;
		String sqlQuery = null;
		int count = 0;
		StringBuffer innerSQL = new StringBuffer();
		final Map<String,Map<String, Object>> userCountByStatus = new LinkedHashMap<>();
		final AtomicInteger statCount = new AtomicInteger(statusList.size());
		buildSumOfExecutionStatusQuery(dbConfig, statusList, innerSQL, statCount);
		try {
			sqlProcessor = new SQLProcessor("defaultDS");
			if(userKeys != null && userKeys.size() > 0) {
				do {
					List<String> subList = userKeys != null && userKeys.size() > count + ApplicationConstants.MAX_IN_QUERY ? userKeys.subList(count, count + ApplicationConstants.MAX_IN_QUERY) : userKeys;
					String executedBy = subList.stream()
							.map(s -> "\'" + s + "\'")
							.collect(Collectors.joining(", "));
					sqlQuery = "SELECT EXECUTED_BY," + innerSQL.toString() + " from AO_7DEABF_SCHEDULE WHERE PROJECT_ID = " + projectId + " AND VERSION_ID = " + versionId + " AND EXECUTED_BY IN (" + executedBy + " ) group by EXECUTED_BY";
					if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), ApplicationConstants.POSTGRES_DB)) {
						sqlQuery = "SELECT \"EXECUTED_BY\"," + innerSQL.toString() + " from \"AO_7DEABF_SCHEDULE\" WHERE \"PROJECT_ID\" = " + projectId + " AND \"VERSION_ID\" = " + versionId + " AND \"EXECUTED_BY\" IN (" + executedBy + " ) group by \"EXECUTED_BY\"";
					} else if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), ApplicationConstants.MSSQL_DB)) {
						if(dbConfig.getSchemaName() != null && !StringUtils.equalsIgnoreCase(dbConfig.getSchemaName(),"public")) {
							sqlQuery = "SELECT EXECUTED_BY," + innerSQL.toString() + " from " + dbConfig.getSchemaName() + "." + "AO_7DEABF_SCHEDULE WHERE PROJECT_ID = " + projectId + " AND VERSION_ID = " + versionId + " AND EXECUTED_BY IN (" + executedBy + " ) group by EXECUTED_BY";
						}
					}
					ResultSet resultSet = null;
					executeUserGroupBySqlQuery(statusList, sqlProcessor, sqlQuery, userCountByStatus, resultSet);
					count += subList.size();
				} while (count < userKeys.size());
			}
		} catch (Exception ex) {
			log.error("Error while executing the query - " + sqlQuery);
		} finally {
			try {
				if (sqlProcessor != null)
					sqlProcessor.close();
			} catch (Exception ex) {
				log.error("Error while closing the sql processor connection ");
			}
		}
		return userCountByStatus;
	}



	@Override
	public Map<String,Object> getExecutionStatusCountByProjectAndVersionFilterByComponent(Long projectId, Long versionId, String[] componentIdArr, Map<Integer, ExecutionStatus> executionStatuses) {
		DatabaseConfig dbConfig = ComponentAccessor.getComponent(DatabaseConfigurationManager.class).getDatabaseConfiguration();
		List<String> statusList = executionStatuses.entrySet().stream().map(status -> String.valueOf(status.getKey())).collect(Collectors.toList());
		SQLProcessor sqlProcessor = null;
		String sqlQuery = null;
		String adhocSqlQuery = null;
		int count = 0;
		StringBuffer innerSQL = new StringBuffer();
		final Map<String, Object> cycleCountByStatus = new LinkedHashMap<>();
		final AtomicInteger statCount = new AtomicInteger(statusList.size());
		String components = null;
		if(componentIdArr != null) {
			components = Arrays.asList(componentIdArr).stream()
					.map(s -> "\'" + s + "\'")
					.collect(Collectors.joining(", "));
		}

		buildSumOfExecutionStatusQuery(dbConfig, statusList, innerSQL, statCount);
		try {
			sqlProcessor = new SQLProcessor("defaultDS");
			String andQuery = null;
			if(StringUtils.isNotBlank(components)) {
				sqlQuery = "SELECT sched.CYCLE_ID,cyc.NAME," + innerSQL.toString() + " from AO_7DEABF_SCHEDULE sched, AO_7DEABF_CYCLE cyc WHERE sched.PROJECT_ID = " + projectId + " AND sched.VERSION_ID = " + versionId + " AND sched.ISSUE_ID IN " +
						" (select noassoc.SOURCE_NODE_ID from component comp, nodeassociation noassoc, jiraissue issue  where noassoc.SINK_NODE_ENTITY='Component' AND noassoc.SINK_NODE_ID = comp.ID AND " +
						" noassoc.SOURCE_NODE_ID = issue.ID AND noassoc.SINK_NODE_ID IN (" + components + ")) " +
						" AND sched.CYCLE_ID = cyc.ID group by sched.CYCLE_ID, cyc.NAME";

				adhocSqlQuery = "SELECT sched.CYCLE_ID," + innerSQL.toString() + " from AO_7DEABF_SCHEDULE sched WHERE sched.PROJECT_ID = " + projectId + " AND sched.VERSION_ID = " + versionId + " AND sched.ISSUE_ID IN " +
						" (select noassoc.SOURCE_NODE_ID from component comp, nodeassociation noassoc, jiraissue issue  where noassoc.SINK_NODE_ENTITY='Component' AND noassoc.SINK_NODE_ID = comp.ID AND " +
						" noassoc.SOURCE_NODE_ID = issue.ID AND noassoc.SINK_NODE_ID IN (" + components + ") " +
						" AND sched.CYCLE_ID IS NULL group by sched.CYCLE_ID";

				if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), ApplicationConstants.POSTGRES_DB)) {
					sqlQuery = "SELECT sched.\"CYCLE_ID\", cyc.\"NAME\"," + innerSQL.toString() + " from \"AO_7DEABF_SCHEDULE\" sched, \"AO_7DEABF_CYCLE\" cyc WHERE sched.\"PROJECT_ID\" = " + projectId + " AND sched.\"VERSION_ID\" = " + versionId + " AND sched.\"ISSUE_ID\" " +
							" IN (select noassoc.SOURCE_NODE_ID from component comp, nodeassociation noassoc, \"jiraissue\" issue  where noassoc.SINK_NODE_ENTITY='Component' AND noassoc.SINK_NODE_ID = comp.ID and " +
							" noassoc.SOURCE_NODE_ID = issue.ID AND noassoc.SINK_NODE_ID IN (" + components + ")) AND " +
							"sched.\"CYCLE_ID\" = cyc.\"ID\" group by sched.\"CYCLE_ID\", cyc.\"NAME\"";

					adhocSqlQuery = "SELECT sched.\"CYCLE_ID\"," + innerSQL.toString() + " from \"AO_7DEABF_SCHEDULE\" sched WHERE sched.\"PROJECT_ID\" = " + projectId + " AND sched.\"VERSION_ID\" = " + versionId + " AND sched.\"ISSUE_ID\" " +
							" IN (select noassoc.SOURCE_NODE_ID from component comp, nodeassociation noassoc, jiraissue issue  where noassoc.SINK_NODE_ENTITY='Component' AND noassoc.SINK_NODE_ID = comp.ID AND " +
							" noassoc.SOURCE_NODE_ID = issue.ID AND noassoc.SINK_NODE_ID IN (" + components + ")) " +
							" AND sched.\"CYCLE_ID\" IS NULL group by sched.\"CYCLE_ID\"";

				} else if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), ApplicationConstants.MSSQL_DB)) {
					if(dbConfig.getSchemaName() != null && !StringUtils.equalsIgnoreCase(dbConfig.getSchemaName(),"public")) {
						sqlQuery = "SELECT sched.CYCLE_ID,cyc.NAME," + innerSQL.toString() + " from " + dbConfig.getSchemaName() + "." + "AO_7DEABF_SCHEDULE sched, " + dbConfig.getSchemaName() + "." + "AO_7DEABF_CYCLE cyc " +
								"WHERE sched.PROJECT_ID = " + projectId + " AND sched.VERSION_ID = " + versionId + " AND sched.ISSUE_ID IN" +
								" (select noassoc.SOURCE_NODE_ID from " + dbConfig.getSchemaName() + "." + "component comp, " + dbConfig.getSchemaName() + "." + "nodeassociation noassoc, " + dbConfig.getSchemaName() + "." +
								"jiraissue issue  where noassoc.SINK_NODE_ENTITY='Component' AND noassoc.SINK_NODE_ID = comp.ID AND " +
								" noassoc.SOURCE_NODE_ID = issue.ID AND noassoc.SINK_NODE_ID IN (" + components + ")) " +
								"AND sched.CYCLE_ID = cyc.ID group by sched.CYCLE_ID, cyc.NAME";


						adhocSqlQuery = "SELECT sched.CYCLE_ID," + innerSQL.toString() + " from " + dbConfig.getSchemaName() + "." + "AO_7DEABF_SCHEDULE sched" +
								"WHERE sched.PROJECT_ID = " + projectId + " AND sched.VERSION_ID = " + versionId + " AND sched.ISSUE_ID IN" +
								" (select noassoc.SOURCE_NODE_ID from " + dbConfig.getSchemaName() + "." + "component comp, " + dbConfig.getSchemaName() + "." + "nodeassociation noassoc, " + dbConfig.getSchemaName() + "." +
								"jiraissue issue  where noassoc.SINK_NODE_ENTITY='Component' AND noassoc.SINK_NODE_ID = comp.ID AND " +
								" noassoc.SOURCE_NODE_ID = issue.ID AND noassoc.SINK_NODE_ID IN (" + components + ")) " +
								" AND sched.CYCLE_ID IS NULL group by sched.CYCLE_ID";
					}
				}
			} else {
				sqlQuery = "SELECT sched.CYCLE_ID,cyc.NAME," + innerSQL.toString() + " from AO_7DEABF_SCHEDULE sched, AO_7DEABF_CYCLE cyc WHERE sched.PROJECT_ID = " + projectId + " AND sched.VERSION_ID = " + versionId + " AND " +
						"sched.PROJECT_ID = cyc.PROJECT_ID AND sched.CYCLE_ID = cyc.ID group by sched.CYCLE_ID, cyc.NAME";

				adhocSqlQuery = "SELECT sched.CYCLE_ID," + innerSQL.toString() + " from AO_7DEABF_SCHEDULE sched WHERE sched.PROJECT_ID = " + projectId + " AND sched.VERSION_ID = " + versionId + " AND " +
						"sched.CYCLE_ID IS NULL group by sched.CYCLE_ID";

				if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), ApplicationConstants.POSTGRES_DB)) {
					sqlQuery = "SELECT sched.\"CYCLE_ID\",cyc.\"NAME\"," + innerSQL.toString() + " from \"AO_7DEABF_SCHEDULE\" sched, \"AO_7DEABF_CYCLE\" cyc WHERE sched.\"PROJECT_ID\" = " + projectId + " AND sched.\"VERSION_ID\" = " + versionId + " AND " +
							"sched.\"PROJECT_ID\" = cyc.\"PROJECT_ID\" AND " +
							"sched.\"CYCLE_ID\" = cyc.\"ID\" group by sched.\"CYCLE_ID\", cyc.\"NAME\"";


					adhocSqlQuery = "SELECT sched.\"CYCLE_ID\"," + innerSQL.toString() + " from \"AO_7DEABF_SCHEDULE\" sched WHERE sched.\"PROJECT_ID\" = " + projectId + " AND sched.\"VERSION_ID\" = " + versionId + " AND " +
							"sched.\"CYCLE_ID\" IS NULL group by sched.\"CYCLE_ID\"";


				} else if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), ApplicationConstants.MSSQL_DB)) {
					if(dbConfig.getSchemaName() != null && !StringUtils.equalsIgnoreCase(dbConfig.getSchemaName(),"public")) {
						sqlQuery = "SELECT sched.CYCLE_ID,cyc.NAME," + innerSQL.toString() + " from " + dbConfig.getSchemaName() + "." + "AO_7DEABF_SCHEDULE sched, " + dbConfig.getSchemaName() + "." + "AO_7DEABF_CYCLE cyc " +
								"WHERE sched.PROJECT_ID = " + projectId + " AND sched.VERSION_ID = " + versionId + " AND " +
								"sched.PROJECT_ID = cyc.PROJECT_ID AND " +
								"sched.CYCLE_ID = cyc.ID group by sched.CYCLE_ID, cyc.NAME";

						adhocSqlQuery = "SELECT sched.CYCLE_ID," + innerSQL.toString() + " from " + dbConfig.getSchemaName() + "." + "AO_7DEABF_SCHEDULE sched " +
								"WHERE sched.PROJECT_ID = " + projectId + " AND sched.VERSION_ID = " + versionId + " AND " +
								"sched.CYCLE_ID IS NULL group by sched.CYCLE_ID";
					}
				}
			}
			ResultSet resultSet = null;
			executeCycleComponentGroupBySqlQuery(statusList, sqlProcessor, sqlQuery , cycleCountByStatus, resultSet);
			executeCycleComponentGroupBySqlQuery(statusList, sqlProcessor, adhocSqlQuery, cycleCountByStatus, resultSet);
		} catch (Exception ex) {
			log.error("Error while executing the query - " + sqlQuery);
		} finally {
			try {
				if (sqlProcessor != null)
					sqlProcessor.close();
			} catch (Exception ex) {
				log.error("Error while closing the sql processor connection ");
			}
		}
		return cycleCountByStatus;
	}

	private void buildSumOfExecutionStatusQuery(DatabaseConfig dbConfig, List<String> statusList, StringBuffer innerSQL, AtomicInteger statCount) {
		statusList.stream().forEach(statusKey -> {
			if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), ApplicationConstants.POSTGRES_DB)) {
				innerSQL.append("SUM(case when \"STATUS\" = '" + statusKey + "' then 1 end) as \"" + statusKey + "\"");
				if(statCount.getAndDecrement() > 1)
					innerSQL.append(",\n");
			} else {
				innerSQL.append("SUM(case when STATUS = " + statusKey + " then 1 end) as \"" + statusKey+ "\"");
				if(statCount.getAndDecrement() > 1)
					innerSQL.append(",\n");
			}
		});
	}

	private void executeCycleGroupBySqlQuery(List<String> statusList, SQLProcessor sqlProcessor, String sqlQuery, Map<Integer, Map<String, Object>> cycleCountByStatus, ResultSet resultSet) {
		try {
			resultSet = sqlProcessor.executeQuery(sqlQuery);
			while (resultSet.next()) {
				AtomicInteger totalCount = new AtomicInteger(0);
				final Map<String, Object> countByStatus = new LinkedHashMap<String, Object>();
				Integer cycleId = resultSet.getInt(1);
				getExecutionStatusBreakDown(statusList, resultSet, totalCount, countByStatus);
				countByStatus.put("total", totalCount);
				if(cycleId == null || cycleId == 0) {
					cycleCountByStatus.put(ApplicationConstants.AD_HOC_CYCLE_ID, countByStatus);
				} else {
					cycleCountByStatus.put(cycleId, countByStatus);
				}
			}
		} catch (Exception e) {
			log.error("Error retrieving data from DB", e);
		} finally {
			try {
				if (resultSet != null)
					resultSet.close();
			} catch (Exception ex) {
				log.error("Error while closing the sql processor connection ");
			}
		}
	}


	private void executeCycleComponentGroupBySqlQuery(List<String> statusList, SQLProcessor sqlProcessor, String sqlQuery, Map<String, Object> cycleCountByStatus, ResultSet resultSet) {
		try {
			resultSet = sqlProcessor.executeQuery(sqlQuery);
			while (resultSet.next()) {
				AtomicInteger totalCount = new AtomicInteger(0);
				final Map<String, Object> countByStatus = new LinkedHashMap<String, Object>();
				Integer cycleId = resultSet.getInt(1);
				getExecutionStatusBreakDownWithStatusName(statusList, resultSet, totalCount, countByStatus);
				if(cycleId == null || cycleId == 0) {
					cycleCountByStatus.put(ApplicationConstants.AD_HOC_CYCLE_NAME+":-"+String.valueOf(ApplicationConstants.AD_HOC_CYCLE_ID), countByStatus);
				} else {
					String cycleName = resultSet.getString(2);
					cycleCountByStatus.put(cycleName+":-"+cycleId, countByStatus);
				}
			}
		} catch (Exception e) {
			log.error("Error retrieving data from DB", e);
		} finally {
			try {
				if (resultSet != null)
					resultSet.close();
			} catch (Exception ex) {
				log.error("Error while closing the sql processor connection ");
			}
		}
	}


	private void executeIssueGroupBySqlQuery(List<String> statusList, SQLProcessor sqlProcessor, String sqlQuery,Map<String, Object> issueCountByStatus, ResultSet resultSet) {
		try {
			resultSet = sqlProcessor.executeQuery(sqlQuery);
			AtomicInteger totalCount = new AtomicInteger(0);
			while (resultSet.next()) {
				getExecutionStatusBreakDownForIssue(statusList, resultSet, totalCount, issueCountByStatus);
				issueCountByStatus.put("total", totalCount);
			}
		} catch (Exception e) {
			log.error("Error retrieving data from DB", e);
		} finally {
			try {
				if (resultSet != null)
					resultSet.close();
			} catch (Exception ex) {
				log.error("Error while closing the sql processor connection ");
			}
		}
	}


	private void executeUserGroupBySqlQuery(List<String> statusList, SQLProcessor sqlProcessor, String sqlQuery, Map<String, Map<String, Object>> userCountByStatus, ResultSet resultSet) {
		try {
			resultSet = sqlProcessor.executeQuery(sqlQuery);
			while (resultSet.next()) {
				AtomicInteger totalCount = new AtomicInteger(0);
				final Map<String, Object> countByStatus = new LinkedHashMap<String, Object>();
				String executedBy = resultSet.getString(1);
				getExecutionStatusBreakDown(statusList, resultSet, totalCount, countByStatus);
				countByStatus.put("total", totalCount);
				if(StringUtils.isNotBlank(executedBy)) {
					userCountByStatus.put(executedBy, countByStatus);
				}
			}
		} catch (Exception e) {
			log.error("Error retrieving data from DB", e);
		} finally {
			try {
				if (resultSet != null)
					resultSet.close();
			} catch (Exception ex) {
				log.error("Error while closing the sql processor connection ");
			}
		}
	}

	private void getExecutionStatusBreakDown(List<String> statusList, ResultSet resultSet, AtomicInteger totalCount, Map<String, Object> countByStatus) {
		for (String statusKey : statusList) {
			try {
				int statusCount = resultSet.getInt(statusKey);
				totalCount.set(totalCount.get() + statusCount);
				if(countByStatus.containsKey(statusKey)) {
					Integer previousCount = (Integer)countByStatus.get(statusKey);
					statusCount += previousCount;
				}
				countByStatus.put(String.valueOf(statusKey), statusCount);
			} catch (SQLException e) {
				log.error("Error retrieving data from resultset",e);
				countByStatus.put(String.valueOf(statusKey), 0);
			}
		}
	}


	private void getExecutionStatusBreakDownForIssue(List<String> statusList, ResultSet resultSet, AtomicInteger totalCount, Map<String, Object> countByStatus) {
		for (String statusKey : statusList) {
			try {
				int statusCount = resultSet.getInt(statusKey);
				totalCount.set(totalCount.get() + statusCount);
				if(countByStatus.containsKey(statusKey)) {
					Integer previousCount = (Integer)countByStatus.get(statusKey);
					statusCount += previousCount;
				}
				countByStatus.put(statusKey, statusCount);
			} catch (SQLException e) {
				log.error("Error retrieving data from resultset",e);
				countByStatus.put(String.valueOf(statusKey), 0);
			}
		}
	}


	private void getExecutionStatusBreakDownWithStatusName(List<String> statusList, ResultSet resultSet, AtomicInteger totalCount, Map<String, Object> countByStatus) {
		for (String statusKey : statusList) {
			try {
				int statusCount = resultSet.getInt(statusKey);
				totalCount.set(totalCount.get() + statusCount);
				if(countByStatus.containsKey(statusKey)) {
					Integer previousCount = (Integer)countByStatus.get(statusKey);
					statusCount += previousCount;
				}
				if(JiraUtil.getExecutionStatuses().containsKey(Integer.parseInt(statusKey))){
					countByStatus.put(JiraUtil.getExecutionStatuses().get(Integer.parseInt(statusKey)).getName(), statusCount);
				}
			} catch (SQLException e) {
				log.error("Error retrieving data from resultset",e);
				countByStatus.put(String.valueOf(statusKey), 0);
			}
		}
	}


	private void closeConnections(SQLProcessor sqlProcessor, ResultSet resultSet) {
		try {
			if(resultSet != null) {
				resultSet.close();
			}
			if (sqlProcessor != null)
				sqlProcessor.close();
		} catch (Exception ex) {
			log.error("Error while closing the sql processor connection ");
		}
	}
}
