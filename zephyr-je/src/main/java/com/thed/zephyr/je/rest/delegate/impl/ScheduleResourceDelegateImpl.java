package com.thed.zephyr.je.rest.delegate.impl;


import com.atlassian.beehive.compat.ClusterLock;
import com.atlassian.beehive.compat.ClusterLockService;
import com.atlassian.beehive.compat.ClusterLockServiceFactory;
import com.atlassian.core.util.DateUtils;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.bc.issue.search.SearchService.ParseResult;
import com.atlassian.jira.bc.issue.worklog.TimeTrackingConfiguration;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.bc.project.component.ProjectComponentManager;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.database.DatabaseConfig;
import com.atlassian.jira.config.database.DatabaseConfigurationManager;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.RendererManager;
import com.atlassian.jira.issue.fields.renderer.wiki.AtlassianWikiRenderer;
import com.atlassian.jira.issue.fields.rest.json.beans.JiraBaseUrls;
import com.atlassian.jira.issue.index.IndexException;
import com.atlassian.jira.issue.link.RemoteIssueLinkManager;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.search.SearchProviderFactory;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.task.context.Contexts;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.usercompatibility.UserCompatibilityHelper;
import com.atlassian.jira.util.collect.CollectionEnclosedIterable;
import com.atlassian.jira.util.collect.EnclosedIterable;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.query.Query;
import com.atlassian.util.concurrent.atomic.AtomicInteger;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.*;
import com.opensymphony.util.TextUtils;
import com.thed.zephyr.je.attachment.SessionKeys;
import com.thed.zephyr.je.audit.model.ChangeZJEGroup;
import com.thed.zephyr.je.audit.model.ChangeZJEItem;
import com.thed.zephyr.je.audit.service.AuditManager;
import com.thed.zephyr.je.config.model.ExecutionStatus;
import com.thed.zephyr.je.event.EntityType;
import com.thed.zephyr.je.event.EventType;
import com.thed.zephyr.je.event.ScheduleModifyEvent;
import com.thed.zephyr.je.event.SingleScheduleEvent;
import com.thed.zephyr.je.helper.ScheduleResourceHelper;
import com.thed.zephyr.je.helper.ScheduleSearchResourceHelper;
import com.thed.zephyr.je.index.DefectSummaryModel;
import com.thed.zephyr.je.index.ScheduleIdsScheduleIterable;
import com.thed.zephyr.je.index.ScheduleIndexManager;
import com.thed.zephyr.je.index.cluster.NodeStateManager;
import com.thed.zephyr.je.index.cluster.ZFJClusterMessage;
import com.thed.zephyr.je.index.cluster.ZFJMessage.ZFJMessageStatus;
import com.thed.zephyr.je.index.cluster.ZFJMessage.ZFJMessageType;
import com.thed.zephyr.je.model.Cycle;
import com.thed.zephyr.je.model.Folder;
import com.thed.zephyr.je.model.ReindexJobProgress;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.model.ScheduleDefect;
import com.thed.zephyr.je.model.StepDefect;
import com.thed.zephyr.je.model.*;
import com.thed.zephyr.je.permissions.model.PermissionType;
import com.thed.zephyr.je.permissions.service.ZephyrPermissionManager;
import com.thed.zephyr.je.rest.CustomFieldValueResource;
import com.thed.zephyr.je.rest.ScheduleResource.ExecutionReorder;
import com.thed.zephyr.je.rest.ScheduleResource.ExecutionReorderRequest;
import com.thed.zephyr.je.rest.delegate.CustomFieldValueResourceDelegate;
import com.thed.zephyr.je.rest.delegate.ScheduleResourceDelegate;
import com.thed.zephyr.je.service.*;
import com.thed.zephyr.je.vo.*;
import com.thed.zephyr.je.zql.core.SearchService;
import com.thed.zephyr.je.zql.core.ZephyrClauseHandlerFactory;
import com.thed.zephyr.je.zql.helper.SearchResult;
import com.thed.zephyr.util.*;
import com.thed.zephyr.util.collector.IssueIdsCollector;
import com.thed.zephyr.util.collector.IssueObjectCollector;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.search.IndexSearcher;
import org.codehaus.jackson.map.ObjectMapper;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import org.ofbiz.core.entity.jdbc.SQLProcessor;

import java.io.IOException;

import java.sql.ResultSet;
import java.text.*;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.thed.zephyr.util.ApplicationConstants.MSSQL_DB;
import static com.thed.zephyr.util.ApplicationConstants.POSTGRES_DB;

/**
 * ScheduleResource delegate, which serves for actual ScheduleResource Rest along with ValidatePermissions annotation.
 */

public class ScheduleResourceDelegateImpl implements ScheduleResourceDelegate {
    private static final String SCHEDULE_ENTITY = "Execution";
    private static final String ERROR_LOG_MESSAGE = "[Error] [Error code: %s %s Error Message : %s]";
    private static final String NO_COMPONENT = "-1";
    private static final String FOLDER_LEVEL_ESTIMATED_TIME = "folderLevelEstimatedTime";
    private static final String FOLDER_LEVEL_LOGGED_TIME = "folderLevelLoggedTime";
    private static final String FOLDER_LEVEL_EXECUTIONS_LOGGED = "folderLevelExecutionsToBeLogged";

    protected final Logger log = Logger.getLogger(ScheduleResourceDelegateImpl.class);

    private final JiraAuthenticationContext authContext;
    private final ScheduleManager scheduleManager;
    private final DateTimeFormatterFactory dateTimeFormatterFactory;
    private final IssueManager issueManager;
    private final CycleManager cycleManager;
    private final SearchProvider searchProvider;
    private final VersionManager versionManager;
    private final JiraBaseUrls jiraBaseUrls;
    private final RemoteIssueLinkManager remoteIssueLinkManager;
    private final ScheduleIndexManager scheduleIndexManager;
    private final EventPublisher eventPublisher;
    private ProjectManager projectManager;
    private PermissionManager permissionManager;
    private ExportService exportService;
    private SearchService searchService;
    private TeststepManager testStepManager;
    private StepResultManager stepResultManager;
    private ZAPIValidationService zapiValidationService;
    private final RendererManager rendererManager;
    private final AuditManager auditManager;
    private final ClusterLockService clusterLockService;
	private final ZephyrClauseHandlerFactory zephyrClauseHandlerFactory;
	private final ZFJCacheService zfjCacheService;
    private final ZephyrSprintService sprintService;
    private final JobProgressService jobProgressService;
    private FolderManager folderManager;
    private final CustomFieldValueResourceDelegate customFieldValueResourceDelegate;
    private final ZephyrCustomFieldManager zephyrCustomFieldManager;


    /*@TODO - Need to move it to better cache*/
    static Map<Long, Optional<Long>> reindexStatus = new HashMap<Long, Optional<Long>>();
    private final ZephyrPermissionManager zephyrPermissionManager;

    public ScheduleResourceDelegateImpl(JiraAuthenticationContext authContext,
                            ScheduleManager scheduleManager,
                            DateTimeFormatterFactory dateTimeFormatterFactory,
                            IssueManager issueManager,
                            CycleManager cycleManager,
                            SearchProvider searchProvider,
                            VersionManager versionManager,
                            JiraBaseUrls jiraBaseUrls,
                            RemoteIssueLinkManager remoteIssueLinkManager,
                            ScheduleIndexManager scheduleIndexManager,
                            EventPublisher eventPublisher,
                            ProjectManager projectManager,
                            PermissionManager permissionManager, 
                            ExportService exportService, 
                            SearchService searchService,
                            TeststepManager testStepManager, 
                            StepResultManager stepResultManager, 
                            ZAPIValidationService zapiValidationService,
                            RendererManager rendererManager,
                            AuditManager auditManager, 
                            ClusterLockServiceFactory clusterLockServiceFactory, 
                            ZephyrClauseHandlerFactory zephyrClauseHandlerFactory,
                            ZFJCacheService zfjCacheService,
                            JobProgressService jobProgressService,
                            ZephyrSprintService sprintService,
                            final ZephyrPermissionManager zephyrPermissionManager, FolderManager folderManager,
                                        final CustomFieldValueResourceDelegate customFieldValueResourceDelegate,
                                        final ZephyrCustomFieldManager zephyrCustomFieldManager) {
        this.authContext = authContext;
        this.rendererManager = rendererManager;
        this.scheduleManager = scheduleManager;
        this.dateTimeFormatterFactory = dateTimeFormatterFactory;
        this.issueManager = issueManager;
        this.cycleManager = cycleManager;
        this.searchProvider = searchProvider;
        this.versionManager = versionManager;
        this.jiraBaseUrls = jiraBaseUrls;
        this.remoteIssueLinkManager = remoteIssueLinkManager;
        this.scheduleIndexManager = scheduleIndexManager;
        this.eventPublisher = eventPublisher;
        this.projectManager = projectManager;
        this.permissionManager = permissionManager;
        this.exportService = exportService;
        this.searchService = searchService;
        this.testStepManager = testStepManager;
        this.stepResultManager = stepResultManager;
        this.zapiValidationService = zapiValidationService;
        this.auditManager = auditManager;
        this.clusterLockService = clusterLockServiceFactory.getClusterLockService();
        this.zephyrClauseHandlerFactory=zephyrClauseHandlerFactory;
        this.zfjCacheService=zfjCacheService;
        this.sprintService=sprintService;
        this.zephyrPermissionManager=zephyrPermissionManager;
        this.jobProgressService = jobProgressService;
        this.folderManager = folderManager;
        this.customFieldValueResourceDelegate = customFieldValueResourceDelegate;
        this.zephyrCustomFieldManager = zephyrCustomFieldManager;
    }

	@Override
	public Response getExecution(Schedule schedule, final String expandos) {
        ScheduleResourceHelper scheduleHelper = new ScheduleResourceHelper(issueManager);
        Project project = JiraUtil.getProjectThreadLocal();
        boolean hasPermission = JiraUtil.hasBrowseProjectPermission(project, authContext.getLoggedInUser());
        if (!hasPermission) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.project.permission.error", "Execution", String.valueOf(schedule.getProjectId()));
            log.error(String.format(ERROR_LOG_MESSAGE, Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "Insufficient Project permissions", errorMessage, errorMessage);
        }
        Map<String, Object> scheduleMap = new HashMap<String, Object>();
        if (schedule != null) {
            Issue issue = issueManager.getIssueObject(Long.valueOf(schedule.getIssueId()));
            boolean hasViewIssuePermission = JiraUtil.hasIssueViewPermission(null, issue, authContext.getLoggedInUser());
            if (!hasViewIssuePermission) {
                String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.project.permission.error", "Execution", String.valueOf(schedule.getProjectId()));
                log.error(String.format(ERROR_LOG_MESSAGE, Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
                return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "Insufficient Issue permissions", errorMessage, errorMessage);
            }
            List<ScheduleDefect> associatedDefects = scheduleManager.getAssociatedDefects(schedule.getID());
            List<Map<String, String>> scheduleDefectList = scheduleHelper.convertScheduleDefectToMap(associatedDefects);
            scheduleMap = getSerializeSchedule(schedule, scheduleDefectList, issue);

            //getDefect Counts for Steps
            ScheduleSearchResourceHelper searchResourceHelper = new ScheduleSearchResourceHelper(authContext.getLoggedInUser(), searchService, exportService, issueManager, cycleManager, versionManager, testStepManager, stepResultManager);
            String zqlQuery = "execution=" + schedule.getID();
            Map<String, Integer> defectCounts = searchResourceHelper.getStepDefectCountBySchedule(zqlQuery);

            //Add defect count
            scheduleMap.putAll(defectCounts);

            try {
                scheduleMap.put("customFields", new ObjectMapper().writeValueAsString(getCustomFieldsValue(schedule.getID(), issue)));
            } catch (IOException e) {
                log.error("exception occurred", e);
            }

            // Create StepResults if needed. This will void the need of calling the ExecuteTest page for ZAPI.
            if (StringUtils.isNotBlank(expandos) &&
                    StringUtils.containsIgnoreCase(expandos, "checksteps")) {
                boolean isSuccess = testStepManager.verifyAndAddStepResult(schedule);
                if (!isSuccess) {
                    log.debug("Failed to verify and add step result");
                }
            }
        }
        JSONObject ob = new JSONObject();
        try {
            //Execution Status if Present in expandos
            if (StringUtils.isNotBlank(expandos) &&
                    StringUtils.containsIgnoreCase(expandos, "executionStatus")) {
                Map<String, Object> statusesMap = scheduleHelper.populateStatusMap();
                ob.put("status", statusesMap);
            }
            ob.put("execution", scheduleMap);
        } catch (JSONException e1) {
            log.fatal("", e1);
        }
        return Response.ok(ob.toString()).cacheControl(ZephyrCacheControl.never()).build();
	}
	
	@Override
	public Response getExecutions(HttpServletRequest req, Integer issueId,
			Long versionId, Integer cycleId, Integer offset,
			String action, String sortQuery, String expandos, Integer limit, Long folderId) {
    	ScheduleResourceHelper scheduleHelper = new ScheduleResourceHelper(issueManager);
        //Set the Action. Moved it to Helper Class
        scheduleHelper.setCycleSummaryDetail(req, cycleId, versionId, folderId, action, offset, sortQuery);
        Project project = JiraUtil.getProjectThreadLocal();
        Long totalExecutionEstimatedTime = new Long(0);
        Long totalExecutionLoggedTime = new Long(0);
        AtomicLong executionsToBeLogged = new AtomicLong(0);

        List<Schedule> schedules = null;
        Map<String,Long> folderLevelExecutionEstimationData;
        Integer recordCount = null;
        if (issueId != null && cycleId != null) {
            schedules = new ArrayList<Schedule>();
            Schedule schedule = scheduleManager.getSchedulesByIssueIdAndCycleId(issueId, cycleId, offset);
            if (schedule != null)
                schedules.add(schedule);
            else
                log.debug("No schedules found matching this criteria.");
        } else if (issueId != null) {
            schedules = scheduleManager.getSchedulesByIssueId(issueId, offset, null != limit ? limit : null);
        }
        else if (cycleId != null) {
        	schedules = scheduleManager.getSchedules(versionId, project.getId(), cycleId, offset, sortQuery, expandos, folderId,limit);
            recordCount = scheduleManager.getSchedulesCount(versionId, project.getId(), cycleId, folderId);
        }

        //Moved the Code to Helper
        Map<String, Object> statusesMap = scheduleHelper.populateStatusMap();
        Set<Map<String, Object>> scheduleSet = new LinkedHashSet<Map<String, Object>>();
        if (schedules != null) {
            for (Schedule schedule : schedules) {
                Issue issue = issueManager.getIssueObject(issueId != null ? issueId.longValue() : Long.valueOf(schedule.getIssueId()));
                List<ScheduleDefect> associatedDefects = scheduleManager.getAssociatedDefects(schedule.getID());
                boolean hasIssueViewPermission = JiraUtil.hasIssueViewPermission(Long.valueOf(schedule.getIssueId()),null,authContext.getLoggedInUser());
                List<Map<String, String>> scheduleDefectList = scheduleHelper.convertScheduleDefectToMapMasked(associatedDefects, hasIssueViewPermission);
                //List<StepDefect> stepDefects = stepResultManager.getStepResultsWithDefectBySchedule(schedule.getID());
                Map<String, Object> scheduleMap = getSerializeSchedule(schedule, scheduleDefectList, issue);

                //getDefect Counts for Steps
                ScheduleSearchResourceHelper searchResourceHelper = new ScheduleSearchResourceHelper(authContext.getLoggedInUser(), searchService, exportService, issueManager, cycleManager, versionManager, testStepManager, stepResultManager);
                String zqlQuery = "execution=" + schedule.getID();
                Map<String, Integer> defectCounts = searchResourceHelper.getStepDefectCountBySchedule(zqlQuery);

                //Add defect count
                scheduleMap.putAll(defectCounts);

                try {
                    scheduleMap.put("customFields", new ObjectMapper().writeValueAsString(getCustomFieldsValue(schedule.getID(), issue)));
                } catch (IOException e) {
                    log.error("exception occurred");
                }
                scheduleSet.add(scheduleMap);
            }
        }

        String currentlySelectedExecutionId = (String) req.getSession(false).getAttribute(SessionKeys.CURRENT_SELECTED_SCHEDULE_ID);
        if (currentlySelectedExecutionId == null) {
            currentlySelectedExecutionId = "";
        }

        JSONObject executionSummary = getExecutionSummaryForCycleAndProjectAndVersionAndFolder(cycleId, project.getId(),versionId,
                null != folderId ? folderId.intValue() : null);

        JSONObject ob = new JSONObject();
        try {
            ob.put("status", statusesMap);
            ob.put("issueId", issueId);
            ob.put("executions", scheduleSet);
            ob.put("currentlySelectedExecutionId", currentlySelectedExecutionId);
            if(null != executionSummary && executionSummary.length() > 0) {
                ob.put("executionSummaries",executionSummary.get("executionSummaries"));
                ob.put("totalExecutions",executionSummary.get("totalExecutions"));
                ob.put("totalExecuted",executionSummary.get("totalExecuted"));
            }

            if (recordCount != null) {
                ob.put("recordsCount", recordCount);
            } else if (schedules != null) {
                //Reduce the Non permission Schedules from total Count
                ob.put("recordsCount", schedules.size());
            }

            if(Objects.nonNull(folderId) && Objects.nonNull(cycleId)) {
                folderLevelExecutionEstimationData = scheduleManager.getExecutionEstimationData(project.getId(),versionId,cycleId.longValue(),folderId);

                if(MapUtils.isNotEmpty(folderLevelExecutionEstimationData)) {
                    totalExecutionEstimatedTime += folderLevelExecutionEstimationData.get(FOLDER_LEVEL_ESTIMATED_TIME);
                    totalExecutionLoggedTime += folderLevelExecutionEstimationData.get(FOLDER_LEVEL_LOGGED_TIME);
                    executionsToBeLogged.addAndGet(folderLevelExecutionEstimationData.get(FOLDER_LEVEL_EXECUTIONS_LOGGED));
                }
            }
            ob.put("totalExecutionEstimatedTime",getDateStringPretty(totalExecutionEstimatedTime));
            ob.put("totalExecutionLoggedTime",getDateStringPretty(totalExecutionLoggedTime));
            ob.put("executionsToBeLogged",executionsToBeLogged.longValue());
            ob.put("isExecutionWorkflowEnabledForProject",JiraUtil.getExecutionWorkflowEnabled(project.getId()));
            ob.put("isTimeTrackingEnabled",ComponentAccessor.getComponent(TimeTrackingConfiguration.class).enabled());

        } catch (JSONException e1) {
            log.error("", e1);
        }
        return Response.ok(ob.toString()).cacheControl(ZephyrCacheControl.never()).build();
	}

    @Override
	public Response getExecutionDefects(Schedule schedule) {
		Project project = JiraUtil.getProjectThreadLocal();
        boolean hasPermission = JiraUtil.hasBrowseProjectPermission(project, authContext.getLoggedInUser());
        if (!hasPermission) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.project.permission.error", "Execution", String.valueOf(schedule.getProjectId()));
            log.error(String.format(ERROR_LOG_MESSAGE, Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "Insufficient Project permissions", errorMessage, errorMessage);
        }

        List<ScheduleDefect> associatedDefects = scheduleManager.getAssociatedDefects(schedule.getID());
        JSONObject schedulesJSonObj = new JSONObject();
        Map<String, Map<String, String>> scheduleMap = new TreeMap<String, Map<String, String>>();
        for (ScheduleDefect sd : associatedDefects) {
            Issue issue = issueManager.getIssueObject(new Long(sd.getDefectId()));
            if (issue == null) {
                log.debug("Issue not found from the schedule defect object." + sd.getDefectId());
                continue;
            }
            scheduleMap.put(issue.getKey(), IssueUtils.convertDefectToMap(issue));
        }
        try {
            schedulesJSonObj.put(String.valueOf(schedule.getID()), scheduleMap);
        } catch (JSONException e) {
            log.fatal("", e);
        }
        return Response.ok(schedulesJSonObj.toString()).cacheControl(ZephyrCacheControl.never()).build();
	}
	
	@Override
	public Response createExecution(HttpServletRequest req,
			Map<String, Object> params) {
        ScheduleResourceHelper scheduleHelper = new ScheduleResourceHelper(issueManager);
        Integer projectId = ZCollectionUtils.getAsInteger(params, "projectId");
        Integer issueId = ZCollectionUtils.getAsInteger(params, "issueId");
        Integer versionId = ZCollectionUtils.getAsInteger(params, "versionId");
        Integer cId = ZCollectionUtils.getAsInteger(params, "cycleId");
        Long folderId = ZCollectionUtils.getAsLong(params, "folderId");
        String assigneeTypeVal = params.get("assigneeType") != null ? ZCollectionUtils.getAsString(params, "assigneeType") : null;
        ApplicationUser appUser = null;
        JSONObject jsonObjectResponse = new JSONObject();
        boolean isReturnSchedule = Boolean.FALSE;

        if(folderId != null) {
        	Folder folder = folderManager.getFolder(folderId);
        	if(folder == null) {
        		try {
					jsonObjectResponse.put("error", authContext.getI18nHelper().getText("project.folder.not.exist"));
				} catch (JSONException e) {
				}
				log.error("[Error] [Error code:"+ Response.Status.BAD_REQUEST.getStatusCode() + " " + Response.Status.BAD_REQUEST +" Error Message :"+
						authContext.getI18nHelper().getText("project.folder.not.exist"));
				return Response.status(Status.BAD_REQUEST).entity(jsonObjectResponse.toString()).build();
        	}
        }
        if (StringUtils.isNotBlank(assigneeTypeVal)) {
            String assignee = params.get("assignee") != null ? ZCollectionUtils.getAsString(params, "assignee") : null;
            appUser = StringUtils.equalsIgnoreCase(assigneeTypeVal, "currentUser") ? authContext.getLoggedInUser() : assignee != null ? ComponentAccessor.getUserManager().getUserByName(assignee) : null;
            if (appUser == null || !appUser.isActive()) {
                return buildErrorMessage(authContext.getI18nHelper().getText("schedule.execute.update.stepresult.invalid.id", "Assignee value"));
            }
        }

        if (cId == null) {
            cId = ApplicationConstants.AD_HOC_CYCLE_ID;
        } else if (cId != ApplicationConstants.AD_HOC_CYCLE_ID) {
            Cycle cycle = cycleManager.getCycle(Long.valueOf(cId));
            versionId = cycle.getVersionId().intValue();
        }
        Schedule returnSchedule = null;
        MutableIssue issue =  null;
        //Cycle is Not Adhoc, Schedule is Not UnScheduled
        if ((cId != null) && !cId.equals(ApplicationConstants.AD_HOC_CYCLE_ID) &&
                (versionId != null && ApplicationConstants.UNSCHEDULED_VERSION_ID != versionId.intValue())) {
            //If User has already executed once as New, but later decided to add this Test to Cycle, fetch the Existing Schedule and associate it
            Cycle cycle = cycleManager.getCycle(Long.valueOf(cId));
            //If the CycleID is not Adhoc than th gbe
            if (versionId == null) {
                versionId = cycle.getVersionId().intValue();
            }
            List<Schedule> schedules = getScheduleIfExists(projectId, issueId, cId, versionId, folderId);
            if (schedules != null && schedules.size() > 0) {
                returnSchedule = schedules.get(0);
                issue = issueManager.getIssueObject(Long.valueOf(returnSchedule.getIssueId()));
                boolean hasViewIssuePermission = JiraUtil.hasIssueViewPermission(null, issue, authContext.getLoggedInUser());
                if (!hasViewIssuePermission) {
                    String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.project.permission.error", "Execution", String.valueOf(returnSchedule.getProjectId()));
                    log.error(String.format(ERROR_LOG_MESSAGE, Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
                    return JiraUtil.buildErrorResponse(Status.FORBIDDEN, "Insufficient Issue permission", errorMessage, errorMessage);
                }
                returnSchedule.setCycle(cycle);
                setFolderToSchedule(returnSchedule, ZCollectionUtils.getAsLong(params, "folderId"));
                returnSchedule.setVersionId(versionId.longValue());
                returnSchedule.setModifiedBy(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
                returnSchedule.setModifiedDate(new Date());
                returnSchedule.save();
                isReturnSchedule = Boolean.TRUE;
            }
        }

        //Cycle is Not Adhoc, Schedule is unScheduled we need to retrieve the same execution and update
        if ((cId != null) && !cId.equals(ApplicationConstants.AD_HOC_CYCLE_ID) &&
                (versionId != null && ApplicationConstants.UNSCHEDULED_VERSION_ID == versionId.intValue())) {
            //If User has already executed once as New, but later decided to add this Test to Cycle, fetch the Existing Schedule and associate it
            Cycle cycle = cycleManager.getCycle(Long.valueOf(cId));
            log.debug("Create Execution cycle retrieved is "+ cycle != null ? cycle.getID() : null);

//    		//If the CycleID is not Adhoc than use the versionId from Cycle
            if (versionId == null) {
                versionId = cycle.getVersionId().intValue();
            }
            List<Schedule> schedules = getScheduleIfExists(projectId, issueId, cId, versionId, folderId);
            if (schedules != null && schedules.size() > 0) {
                returnSchedule = schedules.get(0);
                issue = issueManager.getIssueObject(Long.valueOf(returnSchedule.getIssueId()));
                boolean hasViewIssuePermission = JiraUtil.hasIssueViewPermission(null, issue, authContext.getLoggedInUser());
                //Issue Security
                if (!hasViewIssuePermission) {
                    String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.project.permission.error", "Execution", String.valueOf(returnSchedule.getProjectId()));
                    log.error(String.format(ERROR_LOG_MESSAGE, Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
                    return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "Insufficient Issue permission", errorMessage, errorMessage);
                }
                returnSchedule.setCycle(cycle);
                setFolderToSchedule(returnSchedule, ZCollectionUtils.getAsLong(params, "folderId"));
                returnSchedule.setVersionId(versionId.longValue());
                returnSchedule.setModifiedBy(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
                //setting modified date on schedule update.
                returnSchedule.setModifiedDate(new Date());
                returnSchedule.save();
                isReturnSchedule = Boolean.TRUE;
            }
        }

        if (returnSchedule == null) {
            if (cId == ApplicationConstants.AD_HOC_CYCLE_ID && versionId == null) {
                versionId = ApplicationConstants.UNSCHEDULED_VERSION_ID;
            }
            issue = issueManager.getIssueObject(Long.valueOf(issueId));
            log.debug("Creating new Execution for Test: " + issue != null ? issue.getKey() : null);

            //Issue Security
            boolean hasViewIssuePermission = JiraUtil.hasIssueViewPermission(null, issue, authContext.getLoggedInUser());
            log.debug("Issue Permission is Set to : "+ hasViewIssuePermission);

            if (!hasViewIssuePermission) {
                String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.project.permission.error", "Execution", String.valueOf(projectId));
                log.error(String.format(ERROR_LOG_MESSAGE, Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
                return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "Insufficient Issue permission", errorMessage, errorMessage);
            }
            Map<String, Object> scheduleProperties = createSchedulePropertiesMap(params, projectId, versionId, issueId);
            log.debug("Creating new Execution with parameters : "+ scheduleProperties.toString());

            try {
                String assignee = params.get("assignee") != null ? (String) params.get("assignee") : null;
                String assigneeUserKey = StringUtils.equalsIgnoreCase(assigneeTypeVal, "currentUser") ? authContext.getLoggedInUser().getKey() : assignee != null ? ComponentAccessor.getUserManager().getUserByName(assignee).getKey() : null;
                log.debug("Assignee assigned during Execution : "+ assigneeUserKey);
                if (StringUtils.isNotBlank(assigneeUserKey)) {
                    scheduleProperties.put("ASSIGNED_TO", assigneeUserKey);
                }
                returnSchedule = scheduleManager.saveSchedule(scheduleProperties);
                log.debug("Execution created successfully with ID: " + returnSchedule.getID());
            } catch (Exception e) {
                log.error("Error creating Execution:", e);
                return Response.status(Status.BAD_REQUEST).entity(authContext.getI18nHelper().getText("zephyr.common.error.create", SCHEDULE_ENTITY, "cycleId, projectId, versionId, issueId") + "\n" + e.getMessage()).build();
            }
            boolean isSuccess = testStepManager.verifyAndAddStepResult(returnSchedule);
            if (!isSuccess) {
                log.debug("Failed to verify and add step result.");
            }
        }

        if (returnSchedule != null) {
            try {
                Collection<Schedule> schedules = new ArrayList<Schedule>();
                schedules.add(returnSchedule);
                //eventPublisher.publish(new SingleScheduleEvent(schedules, new HashMap<String,Object>(), EventType.EXECUTION_ADDED));
                EnclosedIterable<Schedule> enclosedSchedules = CollectionEnclosedIterable.copy(schedules);
                scheduleIndexManager.reIndexSchedule(enclosedSchedules, Contexts.nullContext());

            } catch (Exception e) {
                log.error("Error Indexing Schedule:", e);
            }
        }

        setSelectedVersion(req, versionId == null ? "0" : versionId.toString());

        JSONObject schedulesJSonObj = new JSONObject();

        Map<String, Object> scheduleMap = getSerializeSchedule(returnSchedule, null, issue);
        if (scheduleMap != null) {
            try {
                if(isReturnSchedule) {
                    scheduleMap.put("isReturnSchedule",Boolean.TRUE.toString());
                }
                schedulesJSonObj.put(String.valueOf(returnSchedule.getID()), scheduleMap);
            } catch (JSONException e) {
                log.warn("Error building JSONObject", e);
            }
        }

        Table<String, String, Object> changePropertyTable = HashBasedTable.create();
        changePropertyTable.put("STATUS", ApplicationConstants.OLD, ApplicationConstants.NULL);
        changePropertyTable.put("STATUS", ApplicationConstants.NEW, returnSchedule.getStatus());
        changePropertyTable.put("DATE_CREATED", ApplicationConstants.OLD, ApplicationConstants.NULL);
        changePropertyTable.put("DATE_CREATED", ApplicationConstants.NEW, String.valueOf(returnSchedule.getDateCreated().getTime()));
        if (StringUtils.isNotBlank(returnSchedule.getAssignedTo())) {
            changePropertyTable.put("ASSIGNED_TO", ApplicationConstants.OLD, ApplicationConstants.NULL);
            changePropertyTable.put("ASSIGNED_TO", ApplicationConstants.NEW, StringUtils.isEmpty(returnSchedule.getAssignedTo()) ? ApplicationConstants.NULL : returnSchedule.getAssignedTo());
        }

        eventPublisher.publish(new ScheduleModifyEvent(returnSchedule, changePropertyTable, EventType.EXECUTION_ADDED,
                UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));


        return Response.ok(schedulesJSonObj.toString()).cacheControl(ZephyrCacheControl.never()).build();
	}


	@Override
	public Response getExecutionCount(Long versionId, String groupBy,
			Integer cycleId, Integer sprintId, String days, String periodName,
			String graphType) {
        if(cycleId != null && cycleId == ApplicationConstants.ALL_CYCLES_ID) {
            cycleId = null;
        }
        Project project = JiraUtil.getProjectThreadLocal();
        Long projectId = project.getId();
        JSONObject finalResponse = new JSONObject();
        try {
            //Handle First time Indexes
            checkIndexDirectoryExists();
            /*Periodic charts, Burndown charts*/
            if (StringUtils.equalsIgnoreCase(groupBy, "timePeriod")) {
                Integer vid = versionId != null ? versionId.intValue() : null;
                if(sprintId != null) {
                	Optional<SprintBean> sprintBean = sprintService.getSprint(sprintId.longValue()); 
                	if(!sprintBean.isPresent()) {
                		return buildErrorMessage(Status.BAD_REQUEST,authContext.getI18nHelper().getText("zephyr.common.error.invalid", "SprintId", String.valueOf(sprintId)));
                	}
                }
                if(cycleId != null && cycleId.intValue() != ApplicationConstants.AD_HOC_CYCLE_ID) {
                	Cycle cycle = cycleManager.getCycle(cycleId.longValue()); 
                	if(cycle == null) {
                		return buildErrorMessage(Status.BAD_REQUEST,authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Cycle", String.valueOf(cycleId)));
                	}
                }
                finalResponse = getExecutionCountByStatus(projectId.intValue(), vid, cycleId, sprintId, days, periodName, graphType);
            }/*All other Charts*/ else {
                final String issueType = JiraUtil.getTestcaseIssueTypeId();
                Set<Map<String, Object>> data = null;
                if (StringUtils.equalsIgnoreCase(groupBy, "cycle") || StringUtils.equalsIgnoreCase(groupBy, "sprint-cycle")) {
                    finalResponse.put("urlBase", "TBD");
                    ScheduleResourceHelper scheduleHelper = new ScheduleResourceHelper(scheduleIndexManager);
                    data = scheduleHelper.getExecutionSummaryGroupedByCycle(versionId, projectId);
                } else if (StringUtils.equalsIgnoreCase(groupBy, "label")) {
                    finalResponse.put("urlBase", "TBD");
                    //data = ?
                } else if (StringUtils.equalsIgnoreCase(groupBy, "user")) {
                    finalResponse.put("urlBase", "TBD");
                    ScheduleResourceHelper scheduleHelper = new ScheduleResourceHelper(scheduleIndexManager);
                    data = scheduleHelper.getExecutionSummaryGroupedByUser(versionId, projectId);
                } else if (StringUtils.equalsIgnoreCase(groupBy, "component")) {
                    ProjectComponentManager compManager = ComponentAccessor.getProjectComponentManager();
                    Collection<ProjectComponent> pComponents = compManager.findAllForProject(projectId);

                    SearchProviderFactory searchProviderFactory = ComponentManager.getComponentInstanceOfType(SearchProviderFactory.class);
                    IndexSearcher searcher = searchProviderFactory.getSearcher(SearchProviderFactory.ISSUE_INDEX);
                    data = new LinkedHashSet<Map<String, Object>>();
                    for (ProjectComponent pComp : pComponents) {
                        Collection<Long> issueIds = searchIssuesByComponent(projectId, issueType, searcher, pComp);
                        Map<String, Object> componentSummary = prepareScheduleSummary(issueIds, versionId, pComp.getId(), pComp.getName());
                        data.add(componentSummary);
                    }
                    /* For issue having no components */
                    Collection<Long> issueIds = searchIssuesByComponent(projectId, issueType, searcher, null);
                    String noComponentName = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.je.component.nocomponent");
                    Map<String, Object> componentSummary = prepareScheduleSummary(issueIds, versionId, -1l, noComponentName);
                    data.add(componentSummary);
                }
                finalResponse.put("groupFld", groupBy);
                finalResponse.put("statusSeries", getExecutionStatusMap());
                finalResponse.put("data", data);
            }
        } catch (JSONException e) {
            log.fatal("Error in preparing JSON response for schedules count ", e);
        } catch (SearchException e) {
            log.fatal("Unable to perform search ", e);
        }
        return Response.ok(finalResponse.toString()).cacheControl(ZephyrCacheControl.never()).build();
	}
	
	

	@Override
	public Response getTopDefectsByIssueStatuses(Integer versionId,
			String issueStatuses, String howManyDays) {
		Project project = JiraUtil.getProjectThreadLocal();
        //Handle First time Indexes
        checkIndexDirectoryExists();
        JSONObject finalResponse = null;
        try {
            //Add Lucene call
            ScheduleResourceHelper scheduleHelper = new ScheduleResourceHelper(scheduleIndexManager);
            if (!StringUtils.isBlank(issueStatuses)) {
                finalResponse = new JSONObject();
                SortedSet<DefectSummaryModel> data = null;
                data = scheduleHelper.getTopSchedulesWithDefectsByDuration(project.getId() != null ? project.getId().intValue() : null, versionId, howManyDays, issueStatuses);
                JSONArray defectSummary = new JSONArray();
                for (DefectSummaryModel defectSummaryModel : data) {
                    defectSummary.put(IssueUtils.defectSummaryToJSON(defectSummaryModel));
                }
                finalResponse.put("data", defectSummary);
            }
        } catch (JSONException e) {
            log.fatal("Error in preparing JSON response for schedules with criterion ", e);
        } catch (Exception e) {
            log.fatal("Unable to perform search ", e);
        }
        return Response.ok(finalResponse.toString()).cacheControl(ZephyrCacheControl.never()).build();
	}
	
	@Override
	public Response editExecution(Schedule schedule, HttpServletRequest request, Map<String, Object> params) {
        final ScheduleResourceHelper helper = new ScheduleResourceHelper(issueManager, remoteIssueLinkManager, scheduleManager,stepResultManager);
        // Table to keep track of modified properties of given Schedule, for change logs
        Table<String, String, Object> changePropertyTable = HashBasedTable.create();

        Boolean remoteIssueLinkEnabled = JiraUtil.isIssueToTestExecutionRemoteLinkingEnabled();
        Boolean issuelinkEnabled = JiraUtil.isIssueToTestLinkingEnabled();
        Boolean issueToTestStepLink = JiraUtil.isIssueToTestStepLinkingEnabled();;
        Boolean remoteIssueLinkStepExecution = JiraUtil.isIssueToTestStepExecutionRemoteLinkingEnabled();

        String comment = (String) params.get("comment");
        String execStatusStr = ZCollectionUtils.getAsString(params, "status");
        int newExecutionStatus = -1;
        if (execStatusStr != null) {
            //ZAPI validation
            JSONObject errorJsonObject = zapiValidationService.validateTestStatus(execStatusStr);
            if (errorJsonObject != null) {
                log.error(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,errorJsonObject.toString()));
                return Response.status(Status.BAD_REQUEST).entity(errorJsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
            newExecutionStatus = Integer.parseInt(execStatusStr);
        }

        String contextPath = request.getContextPath();

        try {
            int currentExecutionStatus = schedule.getStatus() != null ? Integer.parseInt(schedule.getStatus()) : ApplicationConstants.UNEXECUTED_STATUS;
            String cycleBreadCrumb = helper.getCycleBreadCrumbOfSchedule(schedule);

            // Fix for ZFJ-1574, Saving modified COMMENT for change logs
            if(!params.containsKey("updateDefectList") && params.containsKey("comment") && !StringUtils.equalsIgnoreCase(schedule.getComment(),comment)) {
                changePropertyTable.put("COMMENT", ApplicationConstants.OLD, StringUtils.isEmpty(schedule.getComment()) ? ApplicationConstants.NULL : schedule.getComment());
                changePropertyTable.put("COMMENT", ApplicationConstants.NEW, StringUtils.isEmpty(comment) ? ApplicationConstants.NULL : comment);
            }

            if (comment != null) {
                schedule.setComment(comment);
            }

            Boolean changeAssignee = params.get("changeAssignee") != null ? ZCollectionUtils.getAsBoolean(params, "changeAssignee", false) : false;
            boolean loggedInUserHasZephyrPermission = verifyBulkEditPermissions(schedule,authContext.getLoggedInUser());
            if(!loggedInUserHasZephyrPermission) {
                String errorMessage = authContext.getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
                return getPermissionDeniedErrorResponse(errorMessage);
            }
            if (changeAssignee) {
                String assigneeTypeVal = params.get("assigneeType") != null ? ZCollectionUtils.getAsString(params, "assigneeType") : null;
                String assignee = params.get("assignee") != null ? ZCollectionUtils.getAsString(params, "assignee") : null;
                ApplicationUser appUser = StringUtils.equalsIgnoreCase(assigneeTypeVal, "currentUser") ? authContext.getLoggedInUser() : assignee != null ? ComponentAccessor.getUserManager().getUserByName(assignee) : null;
                String assigneeUserKey = appUser != null ? appUser.getKey() : null;
                String currentUserDisplayName = appUser != null ? appUser.getDisplayName() : StringUtils.EMPTY;
                String assignedUserDisplayName = getAssignedUserDisplayName(schedule.getAssignedTo());

                if(loggedInUserHasZephyrPermission) {
	                if (StringUtils.isBlank(assigneeTypeVal) && StringUtils.isNotEmpty(schedule.getAssignedTo())) {
	                    changePropertyTable.put("ASSIGNED_TO", ApplicationConstants.OLD, StringUtils.isEmpty(schedule.getAssignedTo()) ? ApplicationConstants.NULL : assignedUserDisplayName);
	                    changePropertyTable.put("ASSIGNED_TO", ApplicationConstants.NEW, ApplicationConstants.NULL);
	                    schedule.setAssignedTo(null);
	                } else if (StringUtils.isNotBlank(assigneeTypeVal)) {
	                    if (appUser != null && appUser.isActive()) {
	                        // Fix for ZFJ-1574, Saving modified ASSIGNEE for change logs
	                        if ((StringUtils.isBlank(schedule.getAssignedTo()) && StringUtils.isNotBlank(assigneeUserKey)) ||
	                                (StringUtils.isNotBlank(schedule.getAssignedTo()) && !schedule.getAssignedTo().equalsIgnoreCase(assigneeUserKey))) {
	                            changePropertyTable.put("ASSIGNED_TO", ApplicationConstants.OLD, StringUtils.isEmpty(schedule.getAssignedTo()) ? ApplicationConstants.NULL : assignedUserDisplayName);
	                            changePropertyTable.put("ASSIGNED_TO", ApplicationConstants.NEW, StringUtils.isEmpty(assigneeUserKey) ? ApplicationConstants.NULL : currentUserDisplayName);
	                        }
	                        schedule.setAssignedTo(assigneeUserKey);
	                    } else {
	                        return buildErrorMessage(authContext.getI18nHelper().getText("schedule.execute.update.stepresult.invalid.id", "Assignee"));
	                    }
	                } else {
	                    schedule.setAssignedTo(null);
	                }
                } else {
                    String errorMessage = authContext.getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
                    return getPermissionDeniedErrorResponse(errorMessage);
                }
            } 

            schedule.setModifiedBy(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));

            //setting modified date.
            schedule.setModifiedDate(new Date());

            Issue testcase = issueManager.getIssueObject(new Long(schedule.getIssueId()));

            boolean hasViewIssuePermission = JiraUtil.hasIssueViewPermission(null,testcase, authContext.getLoggedInUser());
            if (!hasViewIssuePermission) {
                String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.project.permission.error", "Execution", String.valueOf(schedule.getProjectId()));
                return getPermissionDeniedErrorResponse("Insufficient Issue permissions." + errorMessage);
            }

            // ScheduleDefect(s) associated with original Schedule
            List<ScheduleDefect> defects = scheduleManager.getAssociatedDefects(schedule.getID());
            // temp collection to keep the original defects ID's

            List<Integer> defectsIds = new ArrayList<Integer>(defects.size());
            for (ScheduleDefect defect : defects) {
                defectsIds.add(defect.getDefectId());
            }

            //If user has changed associated defect list, we get the latest one or we update the one which is already associated.
            Collection<Integer> associatedDefectIdsCollection = null;
            List<ScheduleDefect> associatedDefects = null;

            //If this flag is true that means we are coming from "Execute Test" Page and hence update defect list.
            //Otherwise we are doing quick schedule execution and don't bother to update defect list for given schedule.
            String updateFlag = ZCollectionUtils.getAsString(params, "updateDefectList");
            boolean updateDefectListFlag = updateFlag != null && updateFlag.equals("true") ? true : false;

            List<String> defectList = (List<String>) params.get("defectList");
            if ((updateFlag != null) && updateDefectListFlag) {
                Map<String, Object> associatedDefectsMap = persistRelatedDefects(schedule, defectList, new Long(schedule.getIssueId()));

                associatedDefects = (List<ScheduleDefect>) associatedDefectsMap.get("final");
                associatedDefectIdsCollection = (Collection<Integer>) associatedDefectsMap.get("unchanged");

                Collection<Integer> associatedAddedDefectIds = (Collection<Integer>) associatedDefectsMap.get("added");
                Collection<Integer> associatedDeletedDefectIds = (Collection<Integer>) associatedDefectsMap.get("deleted");
                Long issueLinkTypeId = ScheduleResourceHelper.getLinkTypeId();
                Long oldIssueLinkTypeId = issueLinkTypeId;
                helper.addRemoteLinks(associatedAddedDefectIds, testcase, schedule, cycleBreadCrumb, contextPath, issuelinkEnabled, remoteIssueLinkEnabled, issueLinkTypeId, oldIssueLinkTypeId);
                helper.addIssueLinks(associatedAddedDefectIds, testcase, issueLinkTypeId, oldIssueLinkTypeId, issuelinkEnabled);
                removeRemoteLinks(associatedDeletedDefectIds, String.valueOf(schedule.getID()));
                //removeIssueLinks(associatedDeletedDefectIds, testcase);

                // Saving added/deleted Schedule Defect(s) for change logs
                if ((null != associatedAddedDefectIds && associatedAddedDefectIds.size() > 0) ||
                        (null != associatedDeletedDefectIds && associatedDeletedDefectIds.size() > 0)) {
                    String[] scheDefs = new String[defects.size()];
                    int indx = 0;
                    // Saving all previous Issue Id's as ',' separated string.
                    for (Integer defectId : defectsIds) {
                        scheDefs[indx] = defectId.toString();
                        indx++;
                    }
                    changePropertyTable.put(EntityType.SCHEDULE_DEFECT.getEntityType(), ApplicationConstants.OLD, StringUtils.join(scheDefs, ','));
                    // Saving modified Issue Id's as ',' separated string.
                    scheDefs = new String[associatedDefects.size()];
                    indx = 0;
                    for (ScheduleDefect defect : associatedDefects) {
                        scheDefs[indx] = defect.getDefectId().toString();
                        indx++;
                    }
                    changePropertyTable.put(EntityType.SCHEDULE_DEFECT.getEntityType(), ApplicationConstants.NEW, StringUtils.join(scheDefs, ','));
                }


            }

            //Check if step level defect link update enabled
            if(issueToTestStepLink && remoteIssueLinkStepExecution) {
                //StepDefect(s) associated with original Schedule
                List<StepDefect> stepDefects = stepResultManager.getStepResultsWithDefectBySchedule(schedule.getID());
                List<Integer> stepDefectsIds = new ArrayList<Integer>(stepDefects.size());
                for (StepDefect stepDefect : stepDefects) {
                    stepDefectsIds.add(stepDefect.getDefectId());
                }

                if (null != stepDefects && stepDefects.size() > 0) {
                    helper.updateRemoteLinks(stepDefectsIds, testcase, schedule, cycleBreadCrumb, contextPath, issueToTestStepLink, remoteIssueLinkStepExecution);
                }
            }

	    	/* Only set/reset execution value if status has been updated */
            if ((execStatusStr != null) && (newExecutionStatus != currentExecutionStatus)) {
                schedule.setStatus(execStatusStr);
                // Saving modified STATUS, EXECUTED_BY and EXECUTED_ON for change logs
                changePropertyTable.put("STATUS", ApplicationConstants.OLD, String.valueOf(currentExecutionStatus));
                changePropertyTable.put("STATUS", ApplicationConstants.NEW, execStatusStr);
                changePropertyTable.put("EXECUTED_ON", ApplicationConstants.OLD, null != schedule.getExecutedOn() ? schedule.getExecutedOn().toString() : ApplicationConstants.NULL);
                changePropertyTable.put("EXECUTED_ON", ApplicationConstants.NEW, String.valueOf(System.currentTimeMillis()));
                changePropertyTable.put("EXECUTED_BY", ApplicationConstants.OLD, StringUtils.isEmpty(schedule.getExecutedBy()) ? ApplicationConstants.NULL : schedule.getExecutedBy());
                changePropertyTable.put("EXECUTED_BY", ApplicationConstants.NEW, UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
		    	/*Unexecute*/
                if (ApplicationConstants.UNEXECUTED_STATUS == newExecutionStatus) {
                    schedule.setExecutedBy(null);
                    schedule.setExecutedOn(null);
                } else {
                    schedule.setExecutedBy(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
                    schedule.setExecutedOn(System.currentTimeMillis());
                }
                associatedDefectIdsCollection = new ArrayList<Integer>();
                for (ScheduleDefect defect : defects) {
                    associatedDefectIdsCollection.add(defect.getDefectId());
                }
                //execution status changed, so lets update the remote links
                helper.updateRemoteLinks(associatedDefectIdsCollection, testcase, schedule, cycleBreadCrumb, contextPath, issuelinkEnabled, remoteIssueLinkEnabled);
            }else if(StringUtils.isNotBlank(execStatusStr) && (newExecutionStatus == currentExecutionStatus)
                    && JiraUtil.getZephyrUpdateExecutionExecutedonFlag()) {
                schedule.setStatus(execStatusStr);
                // Saving modified STATUS, EXECUTED_BY and EXECUTED_ON for change logs
                changePropertyTable.put("STATUS", ApplicationConstants.OLD, String.valueOf(currentExecutionStatus));
                changePropertyTable.put("STATUS", ApplicationConstants.NEW, execStatusStr);
                changePropertyTable.put("EXECUTED_ON", ApplicationConstants.OLD, null != schedule.getExecutedOn() ? schedule.getExecutedOn().toString() : ApplicationConstants.NULL);
                changePropertyTable.put("EXECUTED_ON", ApplicationConstants.NEW, String.valueOf(System.currentTimeMillis()));
                changePropertyTable.put("EXECUTED_BY", ApplicationConstants.OLD, StringUtils.isEmpty(schedule.getExecutedBy()) ? ApplicationConstants.NULL : schedule.getExecutedBy());
                changePropertyTable.put("EXECUTED_BY", ApplicationConstants.NEW, UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
		    	/*Unexecute*/
                if (ApplicationConstants.UNEXECUTED_STATUS == newExecutionStatus) {
                    schedule.setExecutedBy(null);
                    schedule.setExecutedOn(null);
                } else {
                    schedule.setExecutedBy(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
                    schedule.setExecutedOn(System.currentTimeMillis());
                }
            }

            schedule.save();
            // publishing ScheduleModifyEvent for change logs
            eventPublisher.publish(new ScheduleModifyEvent(schedule, changePropertyTable, EventType.EXECUTION_UPDATED,
                    UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));
            try {
                Collection<Schedule> schedules = new ArrayList<Schedule>(1);
                schedules.add(schedule);
                //Need Index update on the same thread for ZQL.
                EnclosedIterable<Schedule> enclosedSchedules = CollectionEnclosedIterable.copy(schedules);
                scheduleIndexManager.reIndexSchedule(enclosedSchedules, Contexts.nullContext());
            } catch (Exception e) {
                log.error("Error Indexing Schedule:", e);
            }

            ScheduleResourceHelper scheduleHelper = new ScheduleResourceHelper(issueManager);
            if(CollectionUtils.isEmpty(associatedDefects)) {
                associatedDefects = scheduleManager.getAssociatedDefects(schedule.getID());
            }
            List<Map<String, String>> scheduleDefectList = scheduleHelper.convertScheduleDefectToMap(associatedDefects);
            Map<String, Object> scheduleMap = getSerializeSchedule(schedule, scheduleDefectList, testcase);

            // Added response for execution defect counts.            
            ScheduleSearchResourceHelper searchResourceHelper = new ScheduleSearchResourceHelper(authContext.getLoggedInUser(), searchService, exportService, issueManager, cycleManager, versionManager, testStepManager, stepResultManager);            
            String zqlQuery = "execution=" + schedule.getID();            
            Map<String,Integer> defectCounts = searchResourceHelper.getStepDefectCountBySchedule(zqlQuery);            
            scheduleMap.putAll(defectCounts);

            JSONObject executionSummary = getExecutionSummaryForCycleAndProjectAndVersionAndFolder(null != schedule.getCycle() ?
                            schedule.getCycle().getID() : null, schedule.getProjectId(),schedule.getVersionId(),
                    null != schedule.getFolder() ? schedule.getFolder().getID(): null);

            if(null != executionSummary && executionSummary.length() > 0 ) {
                JSONObject summaries = (JSONObject) executionSummary.get("executionSummaries");
                scheduleMap.put("executionSummaries",null != summaries ? summaries.toString() : StringUtils.EMPTY);
                scheduleMap.put("totalExecutions",executionSummary.get("totalExecutions"));
                scheduleMap.put("totalExecuted",executionSummary.get("totalExecuted"));
            }
            return Response.ok(scheduleMap).cacheControl(ZephyrCacheControl.never()).build();
        } catch (Exception e) {
            log.error("Error Executing Test:" + e.getMessage(), e);
            Map<String, String> errorMap = new HashMap<String, String>();
            ResponseBuilder builder = Response.status(Status.BAD_REQUEST);
            builder.type(MediaType.APPLICATION_JSON);
            errorMap.put("generic", ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("execute.test.generic.error"));
            builder.entity(errorMap);
            return builder.build();
        }
	}

    @Override
	public Response getExecutionNavigator(Query currentQuery, Integer offset, Schedule schedule, String expand) {
        JSONObject ob = new JSONObject();
        Integer executionId = schedule.getID();
		try {
	        ScheduleSearchResourceHelper searchResourceHelper = new ScheduleSearchResourceHelper(authContext.getLoggedInUser(), searchService);
            Query executionQuery = searchResourceHelper.getNewSearchQuery("execution = " + executionId).getQuery();
            Map<String, Object> scheduleDocuments = searchService.search(currentQuery, executionQuery, authContext.getLoggedInUser(), null, false, offset);

            //If No scheduleDocuments are returned, we should assume there is no data present.
            // Execution check against DB is done above to ensure the execution exists, so the below would mean either the zql which was passed in did
            // not meet the criteria of having the passed in executionId in it or indexing was triggered Async and hence is not present before this call was made.
            if (scheduleDocuments == null || scheduleDocuments.size() == 0 || !scheduleDocuments.containsKey(String.valueOf(executionId))) {
                String errorMessage = authContext.getI18nHelper().getText("zephyr.common.error.invalid", new Object[]{SCHEDULE_ENTITY, executionId});
                return populateErrorMsg(errorMessage);
            }

            Issue issue = issueManager.getIssueObject(Long.valueOf(schedule.getIssueId()));
            ScheduleResourceHelper scheduleHelper = new ScheduleResourceHelper(issueManager);

            //Create StepResults if not present already
            if (StringUtils.isNotBlank(expand) &&
                    StringUtils.containsIgnoreCase(expand, "checksteps")) {
                boolean isSuccess = testStepManager.verifyAndAddStepResult(schedule);
                if (!isSuccess) {
                    log.error(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,"Failed to verify and add step result"));
                    log.debug("Failed to verify and add step result");
                }
            }

            List<ScheduleDefect> associatedDefects = scheduleManager.getAssociatedDefects(schedule.getID());
            List<Map<String, String>> scheduleDefectList = scheduleHelper.convertScheduleDefectToMap(associatedDefects);
            Map<String, Object> scheduleMap = getSerializeSchedule(schedule, scheduleDefectList, issue);

			//getDefect Counts for Steps
	    	String defectZqlQuery = "execution=" + schedule.getID();
	    	Map<String, Integer> defectCounts = searchResourceHelper.getStepDefectCountBySchedule(defectZqlQuery);
	    	scheduleMap.putAll(defectCounts);

            try {
                scheduleMap.put("customFields", new ObjectMapper().writeValueAsString(getCustomFieldsValue(schedule.getID(),issue)));
            } catch (IOException e) {
                log.error("exception occured", e);
            }

            //Add Project Specific Data for Standalone Execution
            Project project = projectManager.getProjectObj(schedule.getProjectId());
            scheduleMap.put("projectName", project.getName());
            scheduleMap.put("projectKey", project.getKey());
            scheduleMap.put("projectAvatarId", project.getAvatar().getId());

            Map<String, Object> statusesMap = scheduleHelper.populateStatusMap();
            ob.put("status", statusesMap);
            ob.put("execution", scheduleMap);
            ob.put("offset", ZCollectionUtils.getAsString(scheduleDocuments, "offset"));
            ob.put("prevExecutionId", scheduleDocuments.get("prevExecutionId") != null ? scheduleDocuments.get("prevExecutionId") : 0);
            ob.put("nextExecutionId", scheduleDocuments.get("nextExecutionId") != null ? scheduleDocuments.get("nextExecutionId") : 0);
        } catch (JSONException e) {
            log.error("Error building response:", e);
            String errorMessage = authContext.getI18nHelper().getText("zephyr.common.error.invalid", new Object[]{SCHEDULE_ENTITY, executionId});
            return populateErrorMsg(errorMessage);
        } catch (Exception e) {
            log.error("Error Searching Query:", e);
            String errorMessage = authContext.getI18nHelper().getText("zephyr.common.error.invalid", new Object[]{SCHEDULE_ENTITY, executionId});
            return populateErrorMsg(errorMessage);
        }
        return Response.ok(ob.toString()).cacheControl(ZephyrCacheControl.never()).build();
	}


    @SuppressWarnings("deprecation")
	@Override
	public Response indexAll(boolean isSyncOnly, boolean isHardIndex, boolean isFromBackupJob, Date applyChangesDate) {
    	Date currentDate = Calendar.getInstance().getTime();
        String jobProgressToken = new UniqueIdGenerator().getStringId();
        jobProgressService.createJobProgress(ApplicationConstants.REINDEX_JOB_PROGRESS,0,jobProgressToken);
        final Integer totalSchedule = scheduleManager.getScheduleCount(java.util.Optional.of(currentDate), java.util.OptionalLong.empty(), java.util.Optional.of(Boolean.FALSE));
        jobProgressService.setTotalSteps(jobProgressToken,totalSchedule);
        NodeStateManager nodeStateManager = (NodeStateManager) ZephyrComponentAccessor.getInstance().getComponent("nodeStateManager");
        int clusterNodesCount = nodeStateManager.getAllActiveNodes().size();
        final String lockName = "zephyr-reindexAll";
        final ClusterLock lock = clusterLockService.getLockForName(lockName);
        try {
            log.info("Sync Flag Set to :" + isSyncOnly);
            long[] allScheduleDocumentsArray = new long[0];
            if(isSyncOnly) {
                allScheduleDocumentsArray = scheduleIndexManager.getAllScheduleDocuments();
                Arrays.sort(allScheduleDocumentsArray);
            }
            JSONObject finalResponse = new JSONObject();
            final ScheduleResourceHelper scheduleHelper = new ScheduleResourceHelper(scheduleIndexManager);
            final ApplicationUser user = authContext.getLoggedInUser();
            final long[] finalAllScheduleDocuments = allScheduleDocumentsArray;
            allScheduleDocumentsArray = null;
            CompletableFuture.runAsync(() -> {
            	try {
                    if (lock.tryLock(0, TimeUnit.SECONDS)) {
                    	ZFJClusterMessage zfjClusterMessage = null;
                        try {
                            // setting user in auth context as it wouldn't be available on default auth context
                            if(authContext != null && authContext.getLoggedInUser() == null) authContext.setLoggedInUser(user);
                            //This is to avoid job to pick the messages while reindexing happening on the same node.
                            zfjClusterMessage = scheduleHelper.addReindexAllOrSyncIndexAllCurrentNodeMessage(ZFJMessageStatus.WORK_IN_PROGRESS.getMessageStatus(), isSyncOnly);
                            Integer offset = 0;
                            Integer limit = ApplicationConstants.REINDEX_BATCH_SIZE;
                           //Fetch previous reindex job progress if it is present to fetch the schedules based on last indexed time otherwise do the reindex all for the schedules.
                            List<ReindexJobProgress> reindexJobProgressList = scheduleManager.getReindexJobProgress(ZFJMessageType.RE_INDEX_ALL.toString(), java.util.OptionalLong.empty());
                            String nodeId = nodeStateManager.getNode().getNodeId();
                            String successMessage = authContext.getI18nHelper().getText("zephyr.je.reindexed.success");
                            checkAndAddStepMessageForNode(jobProgressToken, nodeId, "zephyr.je.reindex.nodes.sync");
                            if(reindexJobProgressList.size() > 0 && !isSyncOnly && !isHardIndex) {
                            	ReindexJobProgress reindexJobProgress = reindexJobProgressList.get(0);
                            	//Changing the time to 2hr back to sync for any changes, just to avoid sync issues.
                        		Date indexedDate = reindexJobProgress.getDateIndexed();
                        		reindexJobProgress.setDateIndexed(currentDate);                       		
                        		Integer scheduleCount = scheduleManager.getScheduleCount(java.util.Optional.of(applyChangesDate != null ? applyChangesDate : indexedDate), java.util.OptionalLong.empty(), java.util.Optional.of(Boolean.TRUE));                                	                               		
                                if(scheduleCount > 0) {
                                	do {                                
                                        List<Long> schedulesArr = scheduleManager.getScheduleIdsByPagination(java.util.Optional.of(applyChangesDate != null ? applyChangesDate : indexedDate), java.util.OptionalLong.empty(), offset, limit, java.util.Optional.of(Boolean.TRUE));                                  		
                                        scheduleIndexManager.reIndexScheduleWithOutMessage(new ScheduleIdsScheduleIterable(schedulesArr, scheduleManager, new ArrayList<>()), Contexts.nullContext(), jobProgressToken);
                                        offset += limit;
                                    } while(offset <= scheduleCount);
                                }
                                jobProgressService.addCompletedSteps(jobProgressToken, totalSchedule/2);
                            	//Updating the index for updated schedules, deleted schedules, deleted cycles, deleted folder, deleted versions
                            	//deleted projects which we can't get these changes from schedule table.
                                applyChangesToIndexFromDate(applyChangesDate != null ? applyChangesDate.getTime() : indexedDate.getTime(), java.util.OptionalLong.empty());                                    	                                   
                                updateReindexJobProgress(reindexJobProgress, jobProgressToken, clusterNodesCount, totalSchedule, currentDate, 1);
                                scheduleIndexManager.removeDuplicateSchedules(totalSchedule);
                            	jobProgressService.addCompletedSteps(jobProgressToken, (totalSchedule - (totalSchedule/2) - scheduleCount));
                            } else {
                            	if(!isSyncOnly && isHardIndex) {
                                    scheduleIndexManager.deleteScheduleIndexes();
                                }
                            	List<Long> filteredSchedulesList = new ArrayList<>();
                            	do {
                            		filteredSchedulesList.clear();
                                    List<Long> scheduleIds = scheduleManager.getScheduleIdsByPagination(java.util.Optional.of(currentDate), java.util.OptionalLong.empty(), offset, limit, java.util.Optional.of(Boolean.FALSE));
                                    if(isSyncOnly && !isHardIndex) {                                        
                                        scheduleIds.stream().forEach(scheduleId -> {
                                        	Long filteredSchedule = filterListWithExistence(finalAllScheduleDocuments, scheduleId);
                                            if(filteredSchedule != null) {
                                                filteredSchedulesList.add(filteredSchedule);
                                            }
                                        });
                                        log.info("Sync Flag set to true:"+filteredSchedulesList.size());
                                        if (filteredSchedulesList.size() > 0) {
                                        	scheduleIndexManager.reIndexScheduleWithOutMessage(new ScheduleIdsScheduleIterable(filteredSchedulesList, scheduleManager, new ArrayList<>()), Contexts.nullContext(), jobProgressToken);
                                            if (scheduleIds.size() - filteredSchedulesList.size() != 0)
                                                jobProgressService.addCompletedSteps(jobProgressToken, scheduleIds.size() - filteredSchedulesList.size());
                                        } else {
                                            jobProgressService.addCompletedSteps(jobProgressToken, scheduleIds.size());
                                        }
                                    } else {
                                        scheduleHelper.reIndexAll(new ScheduleIdsScheduleIterable(scheduleIds, scheduleManager, new ArrayList<>()), Contexts.nullContext(), jobProgressToken);
                                    }
                                    offset += limit;
                                } while(offset <= totalSchedule);                            	
                            	checkAndAddStepMessageForNode(jobProgressToken, nodeId, "zephyr.je.reindex.nodes.sync.completed");
                            	if(reindexJobProgressList.size() == 0) {
                            		Map<String, Object> reindexJobProgressProperties = getReindexJobProgressProperties(jobProgressToken, ZFJMessageType.RE_INDEX_ALL.toString(),
                                			new Integer(1), new Long(0), new Long(totalSchedule), currentDate, clusterNodesCount, null);
                                	scheduleManager.saveReindexJobProgress(reindexJobProgressProperties); 
                            	} else if(reindexJobProgressList.size() > 0) {
                            		ReindexJobProgress reindexJobProgress = reindexJobProgressList.get(0);
                                	updateReindexJobProgress(reindexJobProgress, jobProgressToken, clusterNodesCount, totalSchedule, currentDate, 1);
                            	}
                            }
                            jobProgressService.setMessage(jobProgressToken, successMessage);
                            if(isFromBackupJob) {
                            	backUpIndexFiles();
                            	scheduleManager.updateCurrentDateForAllReindexJobProgress();
                            }
                            scheduleHelper.sendMessageToOtherNodes(isSyncOnly);
                        } catch(Throwable ex) {
                        	ex.printStackTrace();
                            log.error("reindexAll or syncIndexAll:", ex);
                        } finally {
                            lock.unlock();
                            scheduleHelper.updateMessageForCurrentNode(zfjClusterMessage);                            
                            authContext.setLoggedInUser(null);
                        }
                    } else {
                        String inProgressMsg = authContext.getI18nHelper().getText("zephyr.je.admin.reIndex.already.in.progress");
                        jobProgressService.setMessage(jobProgressToken,inProgressMsg);
                    }
                } catch (InterruptedException e) {
                    String error = "reindex all operation interrupted";
                    log.error("reindexAll(): " + error, e);
                }
            });
            finalResponse.put(ApplicationConstants.JOB_PROGRESS_TOKEN,jobProgressToken);
            return Response.ok(finalResponse.toString()).build();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error Indexing Schedules:", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    private void updateReindexJobProgress(ReindexJobProgress reindexJobProgress, String jobProgressToken, int clusterNodesCount, int totalSchedule, 
    		Date currentDate, int completedNodeCount) {
    	reindexJobProgress.setJobProgressId(jobProgressToken);
		reindexJobProgress.setCompletedNodeCount(completedNodeCount);
		reindexJobProgress.setNodeCount(clusterNodesCount);
		reindexJobProgress.setPreviousIndexedCount(reindexJobProgress.getCurrentIndexedCount());
    	reindexJobProgress.setCurrentIndexedCount(new Long(totalSchedule));
		reindexJobProgress.setDateIndexed(currentDate);
		reindexJobProgress.save();
    }
    
    private void checkAndAddStepMessageForNode(String jobProgressToken, String nodeId, String key) {
    	if(Objects.nonNull(nodeId)) {
    		String stepMessage = authContext.getI18nHelper().getText(key, nodeId);
        	jobProgressService.setStepMessage(jobProgressToken, stepMessage);
    	}
    }

    @Override
    public Response indexCurrentNode() {
        String jobProgressToken = new UniqueIdGenerator().getStringId();
        jobProgressService.createJobProgress(ApplicationConstants.REINDEX_JOB_PROGRESS,0,jobProgressToken);
        final Integer totalSchedule = scheduleManager.getScheduleCount(java.util.Optional.empty(), java.util.OptionalLong.empty(), java.util.Optional.empty());
        jobProgressService.setTotalSteps(jobProgressToken,totalSchedule);
        final String lockName = "zephyr-reindexAll";
        final ClusterLock lock = clusterLockService.getLockForName(lockName);
        try {
            JSONObject finalResponse = new JSONObject();
            final ApplicationUser user = authContext.getLoggedInUser();
            scheduleIndexManager.deleteScheduleIndexes();
            CompletableFuture.runAsync(() -> {
            	try {
                    if (lock.tryLock(0, TimeUnit.SECONDS)) {
                        try {
                            // setting user in auth context as it wouldn't be available on default auth context
                            if(authContext != null && authContext.getLoggedInUser() == null) authContext.setLoggedInUser(user);

                            Integer offset = 0;
                            do {
                            	Integer limit = ApplicationConstants.REINDEX_BATCH_SIZE;
                                List<Long> scheduleIds = scheduleManager.getScheduleIdsByPagination(java.util.Optional.empty(), OptionalLong.empty(), offset, limit, java.util.Optional.empty());
                                scheduleIndexManager.reIndexAll(new ScheduleIdsScheduleIterable(scheduleIds, scheduleManager, new ArrayList<>()), Contexts.nullContext(), jobProgressToken,false);
                                offset += limit;
                            } while(offset <= totalSchedule);
                        } finally {
                            lock.unlock();
                            authContext.setLoggedInUser(null);
                        }
                    } else {
                        String inProgressMsg = authContext.getI18nHelper().getText("zephyr.je.admin.reIndex.already.in.progress");
                        jobProgressService.setMessage(jobProgressToken,inProgressMsg);
                    }
                } catch (InterruptedException e) {
                    String error = "reindex all operation interrupted";
                    log.error("reindexAll(): " + error, e);
                }
            });
            finalResponse.put(ApplicationConstants.JOB_PROGRESS_TOKEN,jobProgressToken);
            return Response.ok(finalResponse.toString()).build();
        } catch (Exception e) {
            log.error("Error Indexing Schedules:", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Override
    public Response indexStatus(final long token) {
	    JSONObject finalResponse = new JSONObject();
	    Optional<Long> timeTaken = reindexStatus.get(token);
	    try {
	        if (timeTaken == null) {
	            finalResponse.put("status", "notfound");
	            return Response.ok(finalResponse.toString()).build();
	        }
	        if (!timeTaken.isPresent()) {
	            finalResponse.put("status", "inprogress");
	            return Response.ok(finalResponse.toString()).build();
	        }
	
	        if (timeTaken.get() < 0) {
                log.error(String.format(ERROR_LOG_MESSAGE, Status.INTERNAL_SERVER_ERROR.getStatusCode(),Status.INTERNAL_SERVER_ERROR,"Unable to acquire lock on index, Perhaps re-indexing already in place"));
	            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Unable to acquire lock on index, Perhaps re-indexing already in place").build();
	        }
	
	        if (timeTaken.get() > 0) {
	            long timeTakenLong = timeTaken.get() / 1000;
	            finalResponse.put("took", timeTakenLong + " seconds");
	        }
	        finalResponse.put("status", "completed");
	        reindexStatus.remove(token);
	    } catch (JSONException e) {
	        log.error("Error occurred while preparing the response.",e);
	        return Response.status(Status.INTERNAL_SERVER_ERROR).entity("error:" + e.getMessage()).build();
	    }
	    return Response.ok(finalResponse.toString()).cacheControl(ZephyrCacheControl.never()).build();
    }
    
    
    @Override
    public Response deleteExecution(Schedule schedule) {
        if(null != schedule.getCycle()) {
            if(zfjCacheService.getCacheByWildCardKey("CLONE_CYCLE_PROGRESS_CHK" + "_" + String.valueOf(schedule.getCycle().getID()))) {
                String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.je.admin.clone.in.progress",ApplicationConstants.CYCLE_ENTITY,ApplicationConstants.CYCLE_ENTITY,ApplicationConstants.CYCLE_ENTITY);
                return JiraUtil.buildErrorResponse(Status.CONFLICT, "Clone in Progress", errorMessage, errorMessage);
            }
        }

        if(schedule.getFolder() != null) {
            if (zfjCacheService.getCacheByWildCardKey("CLONE_FOLDER_PROGRESS_CHK" + "_" + String.valueOf(schedule.getFolder().getID()))) {
                String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.je.admin.clone.in.progress", ApplicationConstants.FOLDER_ENTITY, ApplicationConstants.FOLDER_ENTITY, ApplicationConstants.FOLDER_ENTITY);
                return JiraUtil.buildErrorResponse(Status.CONFLICT, "Clone in Progress", errorMessage, errorMessage);
            }
        }

        int rows = scheduleManager.removeSchedule(schedule.getID());
        // publishing ScheduleModifyEvent for change logs
        eventPublisher.publish(new ScheduleModifyEvent(Lists.newArrayList(schedule), null, EventType.EXECUTION_DELETED,
                UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));

        //Delete  Index using Schedule Event
        List<String> scheduleIds = new ArrayList<String>();
        scheduleIds.add(String.valueOf(schedule.getID()));
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("ENTITY_TYPE", "schedule_id");
        param.put("ENTITY_VALUE", scheduleIds);
        eventPublisher.publish(new SingleScheduleEvent(null, param, com.thed.zephyr.je.event.EventType.EXECUTION_DELETED));

        if (rows > 0) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(authContext.getI18nHelper().getText("schedule.response.success.label"), authContext.getI18nHelper().getText("schedule.delete.success", StringUtils.join(scheduleIds, ",")));
                return Response.ok(jsonObject.toString()).build();
            } catch (JSONException e) {
                log.error("Error Constructing JSON Response", e);
                return Response.ok(authContext.getI18nHelper().getText("zephyr.common.retrieve.error")).build();
            }
        } else {
            log.error(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("schedule.operation.delete.error.noID", schedule.getID())));
            return Response.status(Status.BAD_REQUEST).entity(authContext.getI18nHelper().getText("schedule.operation.delete.error.noID", schedule.getID())).build();
        }    	
    }
    
    @Override
    public Response addTestsToCycle(final Map<String, Object> params) {
        final String typeId = JiraUtil.getTestcaseIssueTypeId();
        Long folderId = ZCollectionUtils.getAsLong(params, "folderId");
        Long fromFolderId = ZCollectionUtils.getAsLong(params, "fromFolderId");
        final String cIdString = params.get("cycleId") != null ? params.get("cycleId").toString() : "";
        final Collection<Object> issues = params.get("issues") != null ? (Collection<Object>) params.get("issues") : new ArrayList<Object>();
        String jobProgressTicket = new UniqueIdGenerator().getStringId();
        jobProgressService.createJobProgress(ApplicationConstants.ADD_TESTS_TO_CYCLE_JOB_PROGRESS,0,jobProgressTicket);
        String method = ZCollectionUtils.getAsString(params, "method");
        String assigneeTypeVal = params.get("assigneeType") != null ? ZCollectionUtils.getAsString(params, "assigneeType") : null;
        Boolean addCustomFields = ZCollectionUtils.getAsBoolean(params,"addCustomFields",Boolean.FALSE);
        ApplicationUser appUser = null;
        if(folderId != null) {
        	isFolderExist(folderId);
        }
        if(fromFolderId != null) {
            isFolderExist(fromFolderId);
        }
        if (!StringUtils.isBlank(assigneeTypeVal)) {
            String assignee = params.get("assignee") != null ? (String) params.get("assignee") : null;
            appUser = StringUtils.equalsIgnoreCase(assigneeTypeVal, "currentUser") ? authContext.getLoggedInUser() : assignee != null ? ComponentAccessor.getUserManager().getUserByName(assignee) : null;
            if (appUser == null || !appUser.isActive()) {
                log.error(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("schedule.execute.update.stepresult.invalid.id", "Assignee value")));
                return buildErrorMessage(authContext.getI18nHelper().getText("schedule.execute.update.stepresult.invalid.id", "Assignee value"));
            }
        }

        final ApplicationUser assignedUser = appUser;
        Collection<Long> issueIds = null;
        try {
            if (StringUtils.isBlank(cIdString)) {
                log.error(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("schedule.update.ID.required", "cycleId")));
                JSONObject jsonObject = new JSONObject();
                String errorMessage = authContext.getI18nHelper().getText("schedule.update.ID.required", "cycleId");
                jsonObject.put("error",errorMessage);
                return Response.status(Status.BAD_REQUEST).entity(jsonObject.toString()).build();
                //throw new RESTException(Status.BAD_REQUEST, authContext.getI18nHelper().getText("schedule.update.ID.required", "cycleId"));
            }

            if (StringUtils.equals("2", method)) {
                String searchId = ZCollectionUtils.getAsString(params, "searchId");
                try {
                    if (StringUtils.isBlank(searchId)) {
                        log.error(String.format(ERROR_LOG_MESSAGE, Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,"No Saved search selected, Please pick a saved search."));
                        JSONObject jsonObject = new JSONObject();
                        String errorMessage = "No Saved search selected, Please pick a saved search.";
                        jsonObject.put("error",errorMessage);
                        return Response.status(Status.BAD_REQUEST).entity(jsonObject.toString()).build();
                    }
                    issueIds = populateIssueIdsFromSavedSearch(typeId, params);
                } catch (SearchException e) {
                    log.error("Unable to add test cases based on saved search with ID '" + searchId + "'");
                    JSONObject jsonObject = new JSONObject();
                    String errorMessage = "Unable to add test cases based on saved search with ID '" + searchId + "' " + e.getMessage();
                    jsonObject.put("error",errorMessage);
                    return Response.status(Status.BAD_REQUEST).entity(jsonObject.toString()).build();
                }
            } else if (StringUtils.equals("3", method)) {
                try {
                    issueIds = populateIssueIdsFromPreviousCycle(params);
                } catch (Exception e) {
                    log.error("Unable to add testcases based on existing cycles with params " + params);
                    JSONObject jsonObject = new JSONObject();
                    String errorMessage = "Unable to add testcases based on existing cycle " + e.getMessage();
                    jsonObject.put("error",errorMessage);
                    return Response.status(Status.INTERNAL_SERVER_ERROR).entity(jsonObject.toString()).build();
                }
            }else {
                if (issues.size() == 0) {
                    return Response.ok().cacheControl(ZephyrCacheControl.never()).build();
                }
            }
        }catch (JSONException jsonEx) {
            log.error("Error occurred while creating error json object.");
        }

        if(issueIds != null && issueIds.size()>0){
            issues.addAll(issueIds);
        }

        Map<String, String> responseMap = new HashMap<String, String>();
        final Integer projectId = ZCollectionUtils.getAsInteger(params, "projectId");
        Integer versionId = ZCollectionUtils.getAsInteger(params, "versionId");
        jobProgressService.addSteps(jobProgressTicket,issues.size()-1);
        Map<String, List<String>> resultMap = new HashMap<String, List<String>>();
        List<Long> returnScheduleIds = new ArrayList<Long>();
        List<Schedule> indexScheduleList = new ArrayList<Schedule>();

        //If CycleId is passed, we will use Cycle's Version and Ignore User input on version
        if (StringUtils.isNotBlank(cIdString) && !StringUtils.equalsIgnoreCase(cIdString, ApplicationConstants.AD_HOC_CYCLE_ID_AS_STRING)) {
            Cycle cycle = cycleManager.getCycle(Integer.valueOf(cIdString).longValue());
            versionId = cycle.getVersionId().intValue();
        }

        if (StringUtils.equalsIgnoreCase(cIdString, ApplicationConstants.AD_HOC_CYCLE_ID_AS_STRING) && versionId == null) {
            versionId = ApplicationConstants.UNSCHEDULED_VERSION_ID;
        }
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Integer finalVersionId = versionId;
        final ApplicationUser user = authContext.getLoggedInUser();
        String lockName = ApplicationConstants.CYCLE_ENTITY+"_"+cIdString;
        if(!Objects.isNull(folderId)) {
            lockName = ApplicationConstants.FOLDER_ENTITY+"_"+cIdString+"_"+String.valueOf(folderId);
        }
        final ClusterLock lock = clusterLockService.getLockForName(lockName);
        executor.submit(()->{
            try {
                if (lock.tryLock(0, TimeUnit.SECONDS)) {
                    try {
                        if(!Objects.isNull(folderId)) {
                            zfjCacheService.createOrUpdateCache("FOLDER_ID_PROGRESS_CHK" + "_" + cIdString + "_" + String.valueOf(folderId), cIdString+ "_" + String.valueOf(folderId));
                        }
                        if (authContext != null && authContext.getLoggedInUser() == null)
                            authContext.setLoggedInUser(user);
                        Integer localVersionId = finalVersionId;
                        if ((cIdString != null) && (localVersionId != null)) {
                            Integer cId = new Integer(cIdString);

                            for (Object issueKey : issues) {
                                //If User has already executed once as New, but later decided to add this Test to Cycle, fetch the Existing Schedule and associate it
                                MutableIssue issue = null;
                                try {
                                    if (issueKey instanceof String)
                                        issue = issueManager.getIssueObject((String) issueKey);
                                    else if (issueKey instanceof Long)
                                        issue = issueManager.getIssueObject((Long) issueKey);
                                } catch (NullPointerException e) {
                                    jobProgressService.addCompletedSteps(jobProgressTicket, 1);
                                }

                                boolean hasZephyrPermission = verifyBulkPermissions(projectId.longValue(), authContext.getLoggedInUser());
                                boolean hasIssueViewPermission = true;
                                if (null != issue)
                                    hasIssueViewPermission = JiraUtil.hasIssueViewPermission(null, issue, authContext.getLoggedInUser());

                                if (issue != null && issue.getProjectObject().getId().intValue() == projectId.intValue() && hasZephyrPermission && hasIssueViewPermission) {
                                    //Safety Condition to ignore Non test Issue that comes in selection
                                    if (StringUtils.equalsIgnoreCase(issue.getIssueTypeObject().getId(), typeId)) {
                                        if (cId == ApplicationConstants.AD_HOC_CYCLE_ID && localVersionId == null) {
                                            localVersionId = ApplicationConstants.UNSCHEDULED_VERSION_ID;
                                        }
                                        List<Schedule> schedules = getScheduleIfExists(projectId, issue.getId().intValue(), cId, localVersionId, folderId);
                                        //We are interested only if schedules does not exist, ignore if it exists
                                        if (schedules == null) {
                                            Map<String, Object> scheduleProperties = createSchedulePropertiesMap(params, projectId, localVersionId, issue.getId().intValue());
                                            if (!StringUtils.isBlank(assigneeTypeVal) && assignedUser != null) {
                                                scheduleProperties.put("ASSIGNED_TO", assignedUser.getKey());
                                            }

                                            Schedule schedule = scheduleManager.saveSchedule(scheduleProperties);
                                            // Get old schedule if the process is to copy the schedules from other cycle
                                            if(StringUtils.equals("3", method)){
                                            	Schedule existingSchedule = findSchedule(params, issue.getId().intValue());
                                                if(existingSchedule != null && addCustomFields){
                                                    scheduleManager.cloneCustomFields(existingSchedule.getID(), schedule, false);
                                                }
                                            }
                                            Table<String, String, Object> changePropertyTable = HashBasedTable.create();
                                            changePropertyTable.put("STATUS", ApplicationConstants.OLD, ApplicationConstants.NULL);
                                            changePropertyTable.put("STATUS", ApplicationConstants.NEW, schedule.getStatus());
                                            changePropertyTable.put("DATE_CREATED", ApplicationConstants.OLD, ApplicationConstants.NULL);
                                            changePropertyTable.put("DATE_CREATED", ApplicationConstants.NEW, String.valueOf(schedule.getDateCreated().getTime()));
                                            if (StringUtils.isNotBlank(schedule.getAssignedTo())) {
                                                changePropertyTable.put("ASSIGNED_TO", ApplicationConstants.OLD, ApplicationConstants.NULL);
                                                changePropertyTable.put("ASSIGNED_TO", ApplicationConstants.NEW, StringUtils.isEmpty(schedule.getAssignedTo()) ? ApplicationConstants.NULL : schedule.getAssignedTo());
                                            }
                                            eventPublisher.publish(new ScheduleModifyEvent(schedule, changePropertyTable, EventType.EXECUTION_ADDED,
                                                    UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));
                                            addToResult(resultMap, "success", issue.getKey());
                                            returnScheduleIds.add(Long.valueOf(schedule.getID()));
                                            indexScheduleList.add(schedule);
                                        } else {
                                            addToResult(resultMap, "existing", issue.getKey());
                                        }
                                    } else {
                                        addToResult(resultMap, "issuetypeinvalid", issue.getKey());
                                    }
                                } else {
                                    //Invalid Issue will not have an Issue Object return the same Key back
                                    if (!hasZephyrPermission || !hasIssueViewPermission) {
                                        addToResult(resultMap, "noPermission", issue != null ? issue.getKey() : issueKey.toString());
                                    } else if (issue == null) {
                                        addToResult(resultMap, "invalid", issue != null ? issue.getKey() : issueKey.toString());
                                    } else {
                                        if (!StringUtils.equalsIgnoreCase(issue.getIssueType().getId(), typeId)) {
                                            addToResult(resultMap, "issuetypeinvalid", issue != null ? issue.getKey() : issueKey.toString());
                                        } else {
                                            addToResult(resultMap, "mismatch", issue != null ? issue.getKey() : issueKey.toString());
                                        }
                                    }
                                }
                                jobProgressService.addCompletedSteps(jobProgressTicket, 1);
                            }
                        }

                        //Index All the Schedules
                        if (indexScheduleList != null && indexScheduleList.size() > 0) {
                            //Index Schedules (Added Temporarily. Once the Event Listener is working, we can take this out)
                            try {
                                EnclosedIterable<Schedule> enclosedSchedules = CollectionEnclosedIterable.copy(indexScheduleList);
                                //scheduleHelper.indexSchedule(new ScheduleIdsScheduleIterable(returnScheduleIds, scheduleManager, new ArrayList<Schedule>()));
                                ExecutorService executorService = Executors.newSingleThreadExecutor();
                                Future<String> callableFuture = executorService.submit(() -> {
                                    if (authContext != null && authContext.getLoggedInUser() == null)
                                        authContext.setLoggedInUser(user);
                                    scheduleIndexManager.reIndexSchedule(enclosedSchedules, Contexts.nullContext());
                                    return "";
                                });
                                executorService.shutdown();
                            } catch (Exception e) {
                                log.error("Error Indexing Schedule. Manual ZFJ Re-Index must be performed to fix this.", e);
                            }
                        }
                        if(!Objects.isNull(folderId)) {
                            zfjCacheService.removeCacheByKey("FOLDER_ID_PROGRESS_CHK" + "_" + cIdString + "_" + String.valueOf(folderId));
                        }
                        Map<String, String> statusMap = new HashMap<String, String>();

                        ScheduleResourceHelper scheduleHelper = new ScheduleResourceHelper();
                        String labelKey = (folderId != null ? "folder.operation.add.tests.warn" : "cycle.operation.add.tests.warn");
                        statusMap.put("warn", ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText(labelKey, scheduleHelper.getInfoMessage(resultMap, "success"),
                                scheduleHelper.getInfoMessage(resultMap, "invalid"), scheduleHelper.getInfoMessage(resultMap, "mismatch"), scheduleHelper.getInfoMessage(resultMap, "existing"),
                                scheduleHelper.getInfoMessage(resultMap, "issuetypeinvalid"), scheduleHelper.getInfoMessage(resultMap, "noPermission")));
                        jobProgressService.setMessage(jobProgressTicket, statusMap.get("warn").toString());
                    } catch(Exception e){
                    	log.error("Exception while performing the operation:" + e);
                    }finally {
                        lock.unlock();
                        authContext.setLoggedInUser(null);
                    }
                }else{
                    String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.je.bulk.cycle.delete.in.progress");
                    log.warn(errorMessage);
                    jobProgressService.addCompletedSteps(jobProgressTicket, 1);
                    jobProgressService.setErrorMessage(jobProgressTicket, errorMessage);
                    return;
                }
            } catch (InterruptedException e) {
                log.error("error during getting lock ");
            }

        });
            responseMap.put("jobProgressToken",jobProgressTicket);
            return Response.ok().type(MediaType.APPLICATION_JSON).entity(responseMap).build();
    }

    private Schedule findSchedule(Map<String, Object> params, int issueId) {
    	Integer projectId = ZCollectionUtils.getAsInteger(params, "projectId");
        String versionId = ZCollectionUtils.getAsString(params, "fromVersionId");
        String fromCycleId = ZCollectionUtils.getAsString(params, "fromCycleId");
        Long fromFolderId = ZCollectionUtils.getAsLong(params, "fromFolderId");
        if(fromCycleId != null && fromCycleId.equals(ApplicationConstants.AD_HOC_CYCLE_ID_AS_STRING) && StringUtils.isEmpty(versionId)){
        	versionId = ApplicationConstants.UNSCHEDULED_VERSION_ID_AS_STRING;
        }
        Map<String, Object> filter = new HashMap<String, Object>();
        if(!fromCycleId.equals(ApplicationConstants.AD_HOC_CYCLE_ID_AS_STRING)){
        	filter.put("cid", new Integer[]{Integer.parseInt(fromCycleId)});
        }
    	filter.put("pid", ZCollectionUtils.getAsInteger(params, "projectId"));
    	filter.put("issueId", issueId);
    	filter.put("vid", Integer.getInteger(versionId));

        if(fromFolderId != null && fromFolderId.longValue() != ApplicationConstants.ADHOC_SYSTEM_FOLDER_ID) {
        	filter.put("folderId", fromFolderId);
        } else {
        	filter.put("folderId", null);
        }
        List<Schedule> schedules = scheduleManager.searchSchedules(filter);
        if (schedules.size() > 0)
        	return (schedules != null && schedules.size() > 0) ? schedules.get(0) : null;
        return null;
	}

	@Override
    public Response reorderExecution(final ExecutionReorderRequest executionReorderRequest, Cycle cycle, Version version) {
        JSONObject jsonResponse = new JSONObject();
        ScheduleSearchResourceHelper searchResourceHelper = new ScheduleSearchResourceHelper(authContext.getLoggedInUser(), searchService);
        Collection<Integer> successfulExecutions = new ArrayList<Integer>(0);
        Collection<Integer> failedExecutions = new ArrayList<Integer>(0);
        if (executionReorderRequest != null && executionReorderRequest.executionReorders != null
                && executionReorderRequest.executionReorders.size() > 0) {
            //we will iterate twice through reorder, first iteration will validate the orderIds through intersection.If the request's oldOrderId and newOrderId don't match in the list,
            //Its an invalid request and we will not proceed further. This will avoid any bad data to be processed.
            try {
                List<Integer> oldOrderIds = new ArrayList<Integer>();
                List<Integer> newOrderIds = new ArrayList<Integer>();
                for (ExecutionReorder executionReorder : executionReorderRequest.executionReorders) {
                    if (executionReorder.oldOrderId == executionReorder.newOrderId) {
                        return extractReorderErrorMessage();
                    }

                    oldOrderIds.add(executionReorder.oldOrderId);
                    newOrderIds.add(executionReorder.newOrderId);
                    if (executionReorder.newOrderId == null) {
                        return extractReorderErrorMessage();
                    } else {
                        Schedule newOrderSchedule = getScheduleByOrderId(executionReorder.newOrderId, cycle != null ? cycle.getID() : -1, executionReorderRequest.versionId);
                        if (newOrderSchedule == null) {
                            return extractReorderErrorMessage();
                        }
                    }
                }

                //Check for duplicates in the request
                Set<Integer> uniqueOldOrderIds = new HashSet<Integer>(0);
                uniqueOldOrderIds.addAll(oldOrderIds);

                Set<Integer> uniqueNewOrderIds = new HashSet<Integer>(0);
                uniqueNewOrderIds.addAll(newOrderIds);

                if ((uniqueOldOrderIds.size() != oldOrderIds.size()) || (uniqueNewOrderIds.size() != newOrderIds.size())) {
                    return extractReorderErrorMessage();
                }

                if (!oldOrderIds.containsAll(newOrderIds)) {
                    return extractReorderErrorMessage();
                }
            } catch (JSONException e) {
                log.warn("Error creating JSON response", e);
            }

            Collection<Schedule> schedules = new ArrayList<Schedule>(0);
            String userName = UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(ComponentAccessor.getJiraAuthenticationContext()));
            for (ExecutionReorder executionReorder : executionReorderRequest.executionReorders) {
                Schedule schedule = scheduleManager.getSchedule(executionReorder.executionId);
                boolean isValidExecution = false;
                if (schedule != null && schedule.getOrderId().intValue() == executionReorder.oldOrderId.intValue()) {
                    boolean hasPermission = JiraUtil.hasBrowseProjectPermission(schedule.getProjectId(), authContext.getLoggedInUser());
                    if (hasPermission) {
                        log.debug("reordering executionId=" + executionReorder.executionId + " with old OrderId=" + executionReorder.oldOrderId + " to newOrderId=" + executionReorder.newOrderId);
                        if ((null != cycle && (cycle.getID() == schedule.getCycle().getID())) ||
                                (cycle == null && version != null && version.getId().intValue() == schedule.getVersionId().intValue()) ||
                                (executionReorderRequest.versionId == -1 && executionReorderRequest.cycleId == -1) &&
                                        executionReorder.oldOrderId == schedule.getOrderId().intValue()) {
                            schedule.setOrderId(executionReorder.newOrderId);
                            try {
                                schedule.setModifiedBy(userName);
                                //setting modified date
                                schedule.setModifiedDate(new Date());
                                schedule.save();
                                schedules.add(schedule);
                                successfulExecutions.add(schedule.getID());
                            } catch (Exception e) {
                                failedExecutions.add(schedule.getID());
                                log.error("Error saving schedule", e);
                            }
                            isValidExecution = true;
                        }
                    }
                }
                if (!isValidExecution) {
                    failedExecutions.add(executionReorder.executionId);
                }
            }
            eventPublisher.publish(new SingleScheduleEvent(schedules, new HashMap<String, Object>(), EventType.EXECUTION_UPDATED));
        }
        try {
            if (successfulExecutions.size() == executionReorderRequest.executionReorders.size()) {
                String message = authContext.getI18nHelper().getText("enav.bulk.result");
                jsonResponse.put("success", message);
                log.info(jsonResponse.toString());
                return Response.ok().entity(jsonResponse.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }

            if (failedExecutions.size() > 0) {
                jsonResponse.put("failedExecutions", StringUtils.join(failedExecutions, ","));
                log.info(jsonResponse.toString());
                return Response.ok().entity(jsonResponse.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }

            if (failedExecutions.size() > 0 && successfulExecutions.size() > 0) {
                jsonResponse.put("successfulExecutions", StringUtils.join(successfulExecutions, ","));
                jsonResponse.put("failedExecutions", StringUtils.join(failedExecutions, ","));
                return Response.ok().entity(jsonResponse.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (JSONException e) {
            log.error("Error forming response", e);
            return Response.status(Status.BAD_REQUEST).cacheControl(ZephyrCacheControl.never()).build();
        }
        return Response.ok().cacheControl(ZephyrCacheControl.never()).build();
    }
	
	@Override
	public Response getExecutionsByIssue(MutableIssue issue, Integer offset,
			Integer maxRecords, String expand) {
        ScheduleResourceHelper scheduleHelper = new ScheduleResourceHelper(issueManager,searchProvider,authContext,scheduleManager,scheduleIndexManager,
        		cycleManager,versionManager,searchService,zephyrClauseHandlerFactory,zfjCacheService,zephyrCustomFieldManager);
        return scheduleHelper.getExecutionsByIssue(issue,offset,maxRecords,expand);
	}

    @Override
    public Response getExecutionsStatusCountForCycleByProjectIdAndVersion(Long projectId, Long versionId, String components, Integer offset, Integer limit) {
        List<Long> projectIdList = new ArrayList<Long>();
        projectIdList.add(Long.valueOf(projectId));
        String[] versionIds = new String[]{versionId+""};
        String[] componentIdArr = StringUtils.split(components,"|");
        Map<String, Object> executionStatusCountMap = scheduleManager.getExecutionStatusCountByProjectAndVersionFilterByComponent(projectId,versionId,componentIdArr,JiraUtil.getExecutionStatuses());
        return Response.ok(executionStatusCountMap).cacheControl(ZephyrCacheControl.never()).build();
    }

    @Override
    public Response getExecutionsStatusCountPerAssigneeForCycle(Long projectId, Long versionId, String cycleId, Integer offset, Integer limit) {
        String[] cycleIdArr = null;
        String zqlQuery=null;
        Map<Integer, String> statusesMap = new HashMap<>();
        for (ExecutionStatus execStatus : JiraUtil.getExecutionStatuses().values()) {
            statusesMap.putIfAbsent(execStatus.getId(), execStatus.getName());
        }
        if(StringUtils.isNotBlank(cycleId)) {
            cycleIdArr = StringUtils.split(cycleId,"|");
            zqlQuery = "project =" + projectId + " AND fixVersion=" + versionId + " AND cycleId IN (" + StringUtils.join(cycleIdArr,",") + ")";
        }else {
            zqlQuery = "project =" + projectId + " AND fixVersion=" + versionId;
        }

        ScheduleSearchResourceHelper searchResourceHelper = new ScheduleSearchResourceHelper(authContext.getLoggedInUser(),searchService,exportService,issueManager,
                cycleManager,versionManager,testStepManager,stepResultManager,folderManager,zephyrCustomFieldManager);
        ParseResult parseResult = searchResourceHelper.getNewSearchQuery(zqlQuery);
        Map<String, Map<String, Integer>> executionAssigneeStatusCountMap = new HashMap<>();
        try {
            SearchResult searchResults = searchService.searchMax(authContext.getLoggedInUser(), parseResult.getQuery(), false, true);
            executionAssigneeStatusCountMap = prepareExecutionStatusCountMapPerAssignee(searchResourceHelper.convertLuceneDocumentToJson(searchResults,0,Integer.MAX_VALUE,"executionStatus"), statusesMap);
        } catch(Exception e) {
            log.error("Error retrieving data from ExecutionsStatus Assignee breakdown from Zephyr Indexes:",e);
        }
        return Response.ok(executionAssigneeStatusCountMap).cacheControl(ZephyrCacheControl.never()).build();
    }

    @Override
    public Response getExecutionsStatusByAssignee(Long projectId, Long versionId, String cycleId) {
        Map<String,Map<String, Integer>> resultMap = new HashMap<>();
        String[] cycleIdArr = null;
        final String[] zqlQuery = {null};
        Map<Integer, String> statusMap = new HashMap<>();
        UserManager userManager = ComponentAccessor.getUserManager();
        for (ExecutionStatus execStatus : JiraUtil.getExecutionStatuses().values()) {
            statusMap.putIfAbsent(execStatus.getId(), execStatus.getName());
        }
        if(projectId != null) {
            DatabaseConfig dbConfig = ComponentAccessor.getComponent(DatabaseConfigurationManager.class).getDatabaseConfiguration();
            String commonSqlStr = "SELECT ASSIGNED_TO AS assTo, STATUS AS statusId, COUNT(ID) as execCount FROM AO_7DEABF_SCHEDULE WHERE PROJECT_ID =" + projectId;
            String cycleSql = null;
            if (versionId != null) { commonSqlStr += " AND VERSION_ID=" + versionId; }

            if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), POSTGRES_DB)) {
                commonSqlStr = "SELECT \"ASSIGNED_TO\" AS assTo, \"STATUS\" AS statusId, COUNT(\"ID\") AS execCount FROM \"AO_7DEABF_SCHEDULE\" WHERE \"PROJECT_ID\" =" + projectId;
                if (dbConfig.getSchemaName() != null && !StringUtils.equalsIgnoreCase(dbConfig.getSchemaName(), "public")) {
                    commonSqlStr = "SELECT \"ASSIGNED_TO\" AS assTo, \"STATUS\" AS statusId, COUNT(\"ID\") AS execCount FROM  " +  dbConfig.getSchemaName() + "."  + "\"AO_7DEABF_SCHEDULE\" WHERE \"PROJECT_ID\" = " + projectId;
                }
                if (versionId != null) { commonSqlStr += " AND \"VERSION_ID\" =" + versionId; }
            }else if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), MSSQL_DB)) {
                if(dbConfig.getSchemaName() != null && !StringUtils.equalsIgnoreCase(dbConfig.getSchemaName(),"public")) {
                    commonSqlStr = " SELECT ASSIGNED_TO AS assTo, STATUS AS statusId, COUNT(ID) AS execCount FROM " + dbConfig.getSchemaName() + "." +  "AO_7DEABF_SCHEDULE WHERE PROJECT_ID = " + projectId ;
                    if (versionId != null) { commonSqlStr += " AND VERSION_ID=" + versionId; }
                }
            }
            if (StringUtils.isNotBlank(cycleId)) {
                cycleIdArr = StringUtils.split(cycleId, "|");
                if (Arrays.asList(cycleIdArr).contains("-1")) {
                    if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), POSTGRES_DB)) {
                        cycleSql = " \"CYCLE_ID\" IS NULL ";
                    }else{
                        cycleSql = " CYCLE_ID IS NULL ";
                    }
                } //cause in db cycle_ID is null for adHoc cycle
                String[] cycleIdArrs = (String[]) ArrayUtils.removeElement(cycleIdArr, "-1");
                if (cycleIdArrs != null && cycleIdArrs.length > 0) {
                    if (StringUtils.isNotEmpty(cycleSql)) {
                        if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), POSTGRES_DB)) {
                            commonSqlStr += " AND ( \"CYCLE_ID\" IN (" + StringUtils.join(cycleIdArrs, ",") + ") OR " + cycleSql + " ) ";
                        }else {
                            commonSqlStr += " AND ( CYCLE_ID IN (" + StringUtils.join(cycleIdArrs, ",") + ") OR " + cycleSql + " ) ";
                        }
                    } else {
                        if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), POSTGRES_DB)) {
                            commonSqlStr += " AND  \"CYCLE_ID\" IN (" + StringUtils.join(cycleIdArrs, ",") + ") ";
                        }else {
                            commonSqlStr += " AND  CYCLE_ID IN (" + StringUtils.join(cycleIdArrs, ",") + ") ";
                        }
                    }
                } else if (StringUtils.isNotEmpty(cycleSql)) {
                    commonSqlStr += " AND " + cycleSql;
                }
            }
            String finalCommonSqlStr = commonSqlStr;

            int count = 0;
            SQLProcessor sqlProcessor = null;

            String rawSQLStr = finalCommonSqlStr + " GROUP BY STATUS, ASSIGNED_TO ";
            if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), POSTGRES_DB)) {
                rawSQLStr = finalCommonSqlStr + " GROUP BY \"STATUS\", \"ASSIGNED_TO\" ";
            }
            log.info("Query being executed for(getExecutionsStatusByAssignee) : "+rawSQLStr);
            try {
                sqlProcessor = new SQLProcessor("defaultDS");
                ResultSet resultSet = sqlProcessor.executeQuery(rawSQLStr);
                while (resultSet.next()) {
                    Map<String, Integer> countMap = new HashMap<>();
                    String assignTo = resultSet.getString(1);
                    Integer statusId = resultSet.getInt(2);
                    Integer execCount = resultSet.getInt(3);
                    if(StringUtils.isNotEmpty(assignTo)){

                        statusMap.forEach((stKey, stValue) -> {
                            if(statusId != null && execCount != null && statusId.equals(stKey)) {
                                countMap.put(stValue, execCount);
                            }
                            //add 0 exec count for others
                            if(countMap.get(stValue) == null){
                                countMap.put(stValue,0);
                            }

                        });

                        ApplicationUser assigneeUser = userManager.getUserByName(assignTo);
                        if(assigneeUser != null) {
                            Map<String, Integer> exMap = resultMap.get(assigneeUser.getDisplayName());
                            if(MapUtils.isEmpty(exMap)) {
                                resultMap.put(assigneeUser.getDisplayName(), countMap);
                            }else{
                                Map<String, Integer> latestMap = Stream.of(exMap,countMap)
                                        .map(Map::entrySet)
                                        .flatMap(Collection::stream)
                                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Integer::max));
                                resultMap.put(assigneeUser.getDisplayName(), latestMap);
                            }
                        }
                    }
                }
                resultSet.close();
            } catch (Exception ex) {
                log.error("Error while executing the query - " + zqlQuery);
                throw new RuntimeException("Error while executing the query - " + zqlQuery);
            } finally {
                try {
                    if (sqlProcessor != null)
                        sqlProcessor.close();
                } catch (Exception ex) {
                    log.error("Error while closing the sql processor connection ");
                }
            }

            return Response.ok(resultMap).build();
        }else{
            return Response.ok().build();
        }
    }

    @Override
    public Response getExecutionsStatusCountPerCycleAndFolder(Long projectId, Long versionId, String cycleId, String folderId) {

        String[] cycleIdArr = null;
        final String[] zqlQuery = {null};
        String[] folderIdArr = null;
        Map<String, Integer> executionStatusCountMap = new HashMap<>();
        Map<Integer, String> statusMap = new HashMap<>();
        Map<String, String> statusColorMap = new HashMap<>();
        for (ExecutionStatus execStatus : JiraUtil.getExecutionStatuses().values()) {
            statusMap.putIfAbsent(execStatus.getId(), execStatus.getName());
            statusColorMap.putIfAbsent(execStatus.getName(), execStatus.getColor());
        }
        if(projectId != null) {
            DatabaseConfig dbConfig = ComponentAccessor.getComponent(DatabaseConfigurationManager.class).getDatabaseConfiguration();
            String commonSqlStr = "SELECT STATUS AS statusId, COUNT(ID) as execCount FROM AO_7DEABF_SCHEDULE WHERE PROJECT_ID =" + projectId;
            String cycleSql = null;
            if (versionId != null) { commonSqlStr += " AND VERSION_ID=" + versionId; }

            if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), POSTGRES_DB)) {
                commonSqlStr = "SELECT \"STATUS\" AS statusId, COUNT(\"ID\") AS execCount FROM \"AO_7DEABF_SCHEDULE\" WHERE \"PROJECT_ID\" =" + projectId;
                if (dbConfig.getSchemaName() != null && !StringUtils.equalsIgnoreCase(dbConfig.getSchemaName(), "public")) {
                    commonSqlStr = "SELECT \"STATUS\" AS statusId, COUNT(\"ID\") AS execCount FROM  " +  dbConfig.getSchemaName() + "."  + "\"AO_7DEABF_SCHEDULE\" WHERE \"PROJECT_ID\" = " + projectId;
                }
                if (versionId != null) { commonSqlStr += " AND \"VERSION_ID\" =" + versionId; }
            }else if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), MSSQL_DB)) {
                if(dbConfig.getSchemaName() != null && !StringUtils.equalsIgnoreCase(dbConfig.getSchemaName(),"public")) {
                    commonSqlStr = " SELECT STATUS AS statusId, COUNT(ID) AS execCount FROM " + dbConfig.getSchemaName() + "." +  "AO_7DEABF_SCHEDULE WHERE PROJECT_ID = " + projectId ;
                    if (versionId != null) { commonSqlStr += " AND VERSION_ID=" + versionId; }
                }
            }
            if (StringUtils.isNotBlank(cycleId)) {
                cycleIdArr = StringUtils.split(cycleId, "|");
                if (Arrays.asList(cycleIdArr).contains("-1")) {
                    if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), POSTGRES_DB)) {
                        cycleSql = " \"CYCLE_ID\" IS NULL ";
                    }else{
                        cycleSql = " CYCLE_ID IS NULL ";
                    }
                } //cause in db cycle_ID is null for adHoc cycle
                String[] cycleIdArrs = (String[]) ArrayUtils.removeElement(cycleIdArr, "-1");
                if (StringUtils.isNotBlank(folderId) && !folderId.equals("-1")) {
                    folderIdArr = StringUtils.split(folderId, "|");
                    if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), POSTGRES_DB)) {
                        commonSqlStr +=  " AND \"FOLDER_ID\" IN(" + StringUtils.join(folderIdArr, ", ") + ") ";
                    }else{
                        commonSqlStr +=  " AND FOLDER_ID IN(" + StringUtils.join(folderIdArr, ", ") + ") ";
                    }
                }
                if (cycleIdArrs != null && cycleIdArrs.length > 0) {
                    if (StringUtils.isNotEmpty(cycleSql)) {
                        if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), POSTGRES_DB)) {
                            commonSqlStr += " AND ( \"CYCLE_ID\" IN (" + StringUtils.join(cycleIdArrs, ",") + ") OR " + cycleSql + " ) ";
                        }else {
                            commonSqlStr += " AND ( CYCLE_ID IN (" + StringUtils.join(cycleIdArrs, ",") + ") OR " + cycleSql + " ) ";
                        }
                    } else {
                        if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), POSTGRES_DB)) {
                            commonSqlStr += " AND  \"CYCLE_ID\" IN (" + StringUtils.join(cycleIdArrs, ",") + ") ";
                        }else {
                            commonSqlStr += " AND  CYCLE_ID IN (" + StringUtils.join(cycleIdArrs, ",") + ") ";
                        }
                    }
                } else if (StringUtils.isNotEmpty(cycleSql)) {
                    commonSqlStr += " AND " + cycleSql;
                }
            }
            String finalCommonSqlStr = commonSqlStr;

            int count = 0;
            SQLProcessor sqlProcessor = null;

            String rawSQLStr = finalCommonSqlStr + " GROUP BY STATUS";
            if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), POSTGRES_DB)) {
                rawSQLStr = finalCommonSqlStr + " GROUP BY \"STATUS\" ";
            }
            log.info("Query being executed for(getExecutionsStatusCountPerCycleAndFolder) : "+rawSQLStr);
            try {
                sqlProcessor = new SQLProcessor("defaultDS");
                ResultSet resultSet = sqlProcessor.executeQuery(rawSQLStr);
                while (resultSet.next()) {
                    Integer statusId = resultSet.getInt(1);
                    Integer execCount = resultSet.getInt(2);
                    statusMap.forEach((stKey, stValue) -> {
                        if(statusId != null && execCount != null && statusId.equals(stKey)) {
                            executionStatusCountMap.put(stValue, execCount);
                        }
                        //add 0 exec count for others
                        if(executionStatusCountMap.get(stValue) == null){
                            executionStatusCountMap.put(stValue,0);
                        }

                    });
                }
                resultSet.close();
            } catch (Exception ex) {
                log.error("Error while executing the query - " + zqlQuery);
                throw new RuntimeException("Error while executing the query - " + zqlQuery);
            } finally {
                try {
                    if (sqlProcessor != null)
                        sqlProcessor.close();
                } catch (Exception ex) {
                    log.error("Error while closing the sql processor connection ");
                }
            }

            List<ExecutionStatusCountBean> response = prepareExecutionsStatusCountPerCycle(executionStatusCountMap, statusColorMap);
            return Response.ok(response).build();
        }else{
            return Response.ok().build();
        }
    }

    @Override
    public Response reindexByProjectIds(List<String> projectIds, boolean isSyncOnly, boolean isHardIndex) {
		Date currentDate = Calendar.getInstance().getTime();
        log.info("reindexByProjectIds : Projects For ReIndexing:" + projectIds != null ? projectIds.size() : null);
        String placeholderCommaList = Joiner.on(",").join(
                Iterables.transform(projectIds, Functions.constant("?")));
        List<Integer> projectIdsList = convertStringListToIntegerList(projectIds);
        Integer scheduleCount = scheduleManager.getScheduleCountByProjectIds(placeholderCommaList,projectIdsList, currentDate);
        log.info("Total Execution Count:"+scheduleCount);
        String jobProgressToken = new UniqueIdGenerator().getStringId();
        jobProgressService.createJobProgress(ApplicationConstants.REINDEX_JOB_PROGRESS,0,jobProgressToken);
        jobProgressService.setTotalSteps(jobProgressToken,scheduleCount);
        NodeStateManager nodeStateManager = (NodeStateManager) ZephyrComponentAccessor.getInstance().getComponent("nodeStateManager");
        int clusterNodesCount = nodeStateManager.getAllActiveNodes().size();
        final String lockName = "zephyr-reindexAll";
        final ClusterLock lock = clusterLockService.getLockForName(lockName);
        try {
            long[] allScheduleDocumentsArray = new long[0];
            if(isSyncOnly) {
                allScheduleDocumentsArray = scheduleIndexManager.getAllScheduleDocuments();
                Arrays.sort(allScheduleDocumentsArray);
            }
            JSONObject finalResponse = new JSONObject();
            final ScheduleResourceHelper scheduleHelper = new ScheduleResourceHelper(scheduleIndexManager);
            final ApplicationUser user = authContext.getLoggedInUser();
            final long[] finalAllScheduleDocuments = allScheduleDocumentsArray;
            allScheduleDocumentsArray = null;
            CompletableFuture.runAsync(() -> {
                try {
                    if (lock.tryLock(0, TimeUnit.SECONDS)) {
                    	ZFJClusterMessage zfjClusterMessage = null;
                        try {
                            // setting user in auth context as it wouldn't be available on default auth context
                            if(authContext != null && authContext.getLoggedInUser() == null) {
                                authContext.setLoggedInUser(user);
                            }
                            //This is to avoid job to pick the messages while reindexing happening on the same node.
                            zfjClusterMessage = scheduleHelper.addReindexByPrjOrSyncIndexByPrjCurrentNodeMessage(ZFJMessageStatus.WORK_IN_PROGRESS.getMessageStatus(), isSyncOnly);
                            Integer limit = ApplicationConstants.REINDEX_BATCH_SIZE;
                            String nodeId = nodeStateManager.getNode().getNodeId();
                            String successMessage = authContext.getI18nHelper().getText("zephyr.je.reindexed.success");
                            ScheduleSearchResourceHelper searchResourceHelper = new ScheduleSearchResourceHelper(authContext.getLoggedInUser(), searchService);                           
                            projectIdsList.forEach(projectId -> {
                            	try {
                            		Integer totalSchedule = scheduleManager.getScheduleCount(java.util.Optional.of(currentDate), java.util.OptionalLong.of(projectId), java.util.Optional.of(Boolean.FALSE));
                                	List<ReindexJobProgress> reindexJobProgressList = scheduleManager.getReindexJobProgress(ZFJMessageType.REINDEX_BY_PROJECT.toString(), java.util.OptionalLong.of(projectId));
                                	Integer offset = 0;
                                	checkAndAddStepMessageForNode(jobProgressToken, nodeId, "zephyr.je.reindex.nodes.sync");
                                	Query countQuery = searchResourceHelper.getNewSearchQuery("project = " + projectId).getQuery();
                                    long searchScheduledCount = searchService.searchCount(authContext.getLoggedInUser(), countQuery);
                                	if(reindexJobProgressList.size() > 0 && !isSyncOnly && !isHardIndex) {
                                		ReindexJobProgress reindexJobProgress = reindexJobProgressList.get(0);
                                		//Changing the time to 2hr back to sync for any changes.
                            			Date indexedDate = reindexJobProgress.getDateIndexed();
                                		reindexJobProgress.setDateIndexed(currentDate);
                                		indexedDate.setHours(indexedDate.getHours()-2);
                                		Integer newScheduleCount = scheduleManager.getScheduleCount(java.util.Optional.of(indexedDate), java.util.OptionalLong.of(projectId), java.util.Optional.of(Boolean.TRUE));
                                		if(newScheduleCount > 0) {
                                			do {                                
                                				List<Long> schedulesArr = scheduleManager.getScheduleIdsByPagination(java.util.Optional.of(indexedDate), java.util.OptionalLong.empty(), offset, limit, java.util.Optional.of(Boolean.TRUE));                                  		
                                                scheduleIndexManager.reIndexScheduleWithOutMessage(new ScheduleIdsScheduleIterable(schedulesArr, scheduleManager, new ArrayList<>()), Contexts.nullContext(), jobProgressToken);
                                                offset += limit;
                                            } while(offset <= newScheduleCount);
                                		}
                                		jobProgressService.addCompletedSteps(jobProgressToken, totalSchedule /2);
                                		//Updating the index for updated schedules, deleted schedules, deleted cycles, deleted folder, deleted versions
                                    	//deleted projects which we can't get these changes from schedule table based on the project id.
                                		applyChangesToIndexFromDate(currentDate.getTime(), java.util.OptionalLong.of(projectId));                                    	                                   
                                    	updateReindexJobProgress(reindexJobProgress, jobProgressToken, clusterNodesCount, totalSchedule, currentDate, 1);
                                    	jobProgressService.addCompletedSteps(jobProgressToken, (totalSchedule - (totalSchedule /2) - newScheduleCount));
                                	} else {
                                		if(!new Long(searchScheduledCount).equals(new Long(totalSchedule)) || isHardIndex) {
                                			do {
                                    			List<Integer> projectIdList = new ArrayList<>(1);
                                    			projectIdList.add(projectId);
                                    			String projectHolderCommaList = Joiner.on(",").join(
                                    	                Iterables.transform(projectIdList, Functions.constant("?")));
                                                List<String> schedules = scheduleManager.getAllScheduleIdsByProjectIdsByCreatedDate(projectHolderCommaList, projectIdList, offset, limit, currentDate);
                                                // If its Full ReIndex, we go through the current flow and delete all Indexes based on project else we will just do the filtered comparison
                                                if(!isSyncOnly && isHardIndex) {
                                                    EnclosedIterable<String> enclosedIterable = CollectionEnclosedIterable.copy(schedules);
                                                    scheduleIndexManager.deleteBatchIndexByTermWithoutMessage(enclosedIterable, "schedule_id", Contexts.nullContext());
                                                    log.info("Retrieved Execution Size : "+schedules.size());
                                                    List<Long> scheduleIds = schedules.stream().map(Long::parseLong).collect(Collectors.toList());
                                                    scheduleHelper.reIndexSchedulesInBatch(new ScheduleIdsScheduleIterable(scheduleIds,scheduleManager,new ArrayList<>()),Contexts.nullContext(),jobProgressToken);
                                                } else {
                                                    List<Long> filteredSchedulesList = new ArrayList<>();
                                                    schedules.stream().forEach(scheduleId -> {
                                                    	Long filteredSchedule = filterListWithExistence(finalAllScheduleDocuments, Long.parseLong(scheduleId));
                                                        if(filteredSchedule != null) {
                                                            filteredSchedulesList.add(filteredSchedule);
                                                        }
                                                    });
                                                    log.info("Filtered Execution Size : "+filteredSchedulesList.size());
                                                    if (filteredSchedulesList.size() > 0) {
                                                        scheduleHelper.reIndexSchedulesInBatch(new ScheduleIdsScheduleIterable(filteredSchedulesList,scheduleManager,new ArrayList<>()),Contexts.nullContext(),jobProgressToken);
                                                        if (schedules.size() - filteredSchedulesList.size() != 0)
                                                            jobProgressService.addCompletedSteps(jobProgressToken, schedules.size() - filteredSchedulesList.size());
                                                    } else {
                                                        jobProgressService.addCompletedSteps(jobProgressToken, schedules.size());
                                                    }
                                                }
                                                offset += limit;
                                            } while (offset <= scheduleCount);
                                		} else {
                                   		 	jobProgressService.addCompletedSteps(jobProgressToken, totalSchedule);
                                    	}                                		
                                		checkAndAddStepMessageForNode(jobProgressToken, nodeId, "zephyr.je.reindex.nodes.sync.completed");
                                    	if(reindexJobProgressList.size()  == 0) {
                                    		Map<String, Object> reindexJobProgressProperties = getReindexJobProgressProperties(jobProgressToken, ZFJMessageType.REINDEX_BY_PROJECT.toString(),
                                        			new Integer(1), new Long(0), new Long(totalSchedule), currentDate, clusterNodesCount, new Long(projectId));
                                        	scheduleManager.saveReindexJobProgress(reindexJobProgressProperties);
                                    	} else if(reindexJobProgressList.size() > 0) {
                                    		ReindexJobProgress reindexJobProgress = reindexJobProgressList.get(0);
                                    		updateReindexJobProgress(reindexJobProgress, jobProgressToken, clusterNodesCount, totalSchedule, currentDate, 1);
                                    	}
                                	}
                            	} catch(Exception ex) {
                            		log.error("Error during reindex by project -> " + projectId, ex);
                            	}
                            });                            
                            jobProgressService.setMessage(jobProgressToken, successMessage);
                            scheduleHelper.sendReIndexByProjectMessageToOtherNodes(projectIds,isSyncOnly);
                        } catch (Throwable tr) {
                            tr.printStackTrace();
                            log.error("Error occurred while reindexing the schedules by project ids, please trigger the reindex all.", tr);
                        } finally {
                            lock.unlock();
                            scheduleHelper.updateMessageForCurrentNode(zfjClusterMessage);                              
                            authContext.setLoggedInUser(null);
                        }
                    } else {
                        String inProgressMsg = authContext.getI18nHelper().getText("zephyr.je.admin.reIndex.already.in.progress");
                        jobProgressService.setMessage(jobProgressToken,inProgressMsg);
                    }
                } catch (InterruptedException ex) {
                    log.error("reindex by project ids operation interrupted ", ex);
                }
            });
            log.info("REINDEXING BY PROJECT SCHEDULE COMPLETE::::" + scheduleCount);
            finalResponse.put(ApplicationConstants.JOB_PROGRESS_TOKEN, jobProgressToken);
            return Response.ok(finalResponse.toString()).build();
        } catch (Exception e) {
            log.error("Error Indexing Schedules:", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }


/**	################################ PRIVATE METHODS BELOW ########################################*/
    /**
     * Serializes AO Schedule to JSON
     *
     * @param schedule
     * @param scheduleDefectList
     * @param issue
     * @return
     */
    private Map<String, Object> getSerializeSchedule(Schedule schedule, List<Map<String, String>> scheduleDefectList, Issue issue) {
        if (schedule.getID() == -1) {
            return null;
        }
        Map<String, Object> scheduleMap = new LinkedHashMap<String, Object>();
        boolean hasViewIssuePermission = JiraUtil.hasIssueViewPermission(null,issue,authContext.getLoggedInUser());
        log.debug("Populating Executions for Execution:"+schedule.getID());
		/*Just a safeguard, can happen when issue gets deleted */
        if (issue != null && hasViewIssuePermission) {
            Cycle cycle = schedule.getCycle();
            Folder folder = schedule.getFolder();
            scheduleMap.put("id", schedule.getID());
            scheduleMap.put("orderId", schedule.getOrderId());
            scheduleMap.put("executionStatus", schedule.getStatus());
            scheduleMap.put("executionWorkflowStatus", schedule.getExecutionWorkflowStatus());

            if (schedule.getStatus() != null && !"-1".equals(schedule.getStatus())) {
                Long scheduleExecutedOnTime = schedule.getExecutedOn();
                if (scheduleExecutedOnTime != null) {
                    scheduleMap.put("executedOn", dateTimeFormatterFactory.formatter().forLoggedInUser().format(new Date(scheduleExecutedOnTime)));
                    scheduleMap.put("executedOnVal", scheduleExecutedOnTime);
                }

                User executor = UserCompatibilityHelper.getUserForKey(schedule.getExecutedBy());
                scheduleMap.put("executedBy", executor != null ? executor.getName() : schedule.getExecutedBy());
                scheduleMap.put("executedByDisplay", executor != null ? executor.isActive() ? executor.getDisplayName() : executor.getDisplayName() + " (Inactive)" : "");
            }
            if (scheduleDefectList != null && scheduleDefectList.size() > 0) {
                scheduleMap.put("defects", scheduleDefectList);
            }
            //scheduleMap.put( "comment", schedule.getComment() != null ? StringUtils.abbreviate(schedule.getComment(), 100) : fetchDefaultStatus(schedule.getStatus()));
            scheduleMap.put("comment", schedule.getComment() != null ? schedule.getComment() : "");
            scheduleMap.put("htmlComment", schedule.getComment() != null ? TextUtils.plainTextToHtml(schedule.getComment(), "_blank", true) : "");

            if (cycle != null) {
                scheduleMap.put("cycleId", cycle.getID());
                scheduleMap.put("cycleName", cycle.getName());
            } else {
                scheduleMap.put("cycleId", ApplicationConstants.AD_HOC_CYCLE_ID);
                scheduleMap.put("cycleName", ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.je.cycle.adhoc"));
            }
            
            if(folder != null) {
            	 scheduleMap.put("folderId", folder.getID());
                 scheduleMap.put("folderName", folder.getName());
            }

            long versionId = schedule.getVersionId();
            scheduleMap.put("versionId", versionId);
            String versionName = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.je.version.unscheduled");
            if (versionId != -1) {
                Version version = versionManager.getVersion(versionId);
                if (version != null) {
                    versionName = version.getName();
                }
            }
            scheduleMap.put("versionName", versionName);

            scheduleMap.put("projectId", schedule.getProjectId());

            //creator and modified by
            scheduleMap.put("createdBy", StringUtils.isNotBlank(schedule.getCreatedBy()) ? schedule.getCreatedBy() : StringUtils.EMPTY);

            User creator = UserCompatibilityHelper.getUserForKey(schedule.getCreatedBy());
            scheduleMap.put("createdByDisplay", creator != null ? creator.isActive() ? creator.getDisplayName() : creator.getDisplayName() + " (Inactive)" : "");
            String createdByUserName = creator != null ? creator.getName() : schedule.getCreatedBy();
            scheduleMap.put("createdByUserName", createdByUserName);

            scheduleMap.put("modifiedBy", schedule.getModifiedBy());

            if(Objects.nonNull(schedule.getDateCreated())) {
                scheduleMap.put("createdOn", dateTimeFormatterFactory.formatter().forLoggedInUser().format(schedule.getDateCreated()));
                scheduleMap.put("createdOnVal", schedule.getDateCreated().getTime());
            }

            //assignedTo
            if (schedule.getAssignedTo() != null) {
                User assignedToUser = UserCompatibilityHelper.getUserForKey(schedule.getAssignedTo());
                String assignedTo = assignedToUser != null ? assignedToUser.getDisplayName() : schedule.getAssignedTo();
                String assigneeDisplay = (assignedToUser != null && assignedToUser.isActive()) ? assignedTo : assignedTo + " (Inactive)";
                String assigneeUserName = assignedToUser != null ? assignedToUser.getName() : assignedTo;
                scheduleMap.put("assignedTo", assignedTo);
                scheduleMap.put("assignedToDisplay", assigneeDisplay);
                scheduleMap.put("assignedToUserName", assigneeUserName);

                ApplicationUser appUser = authContext.getLoggedInUser();
                if(appUser != null) {
                    User loggedInUser = UserCompatibilityHelper.getUserForKey(appUser.getKey());
                    //Fix for ZFJ-1549: if user is deleted, setting assigneeType -> assignee
                    if (assignedToUser == null || (!StringUtils.equals(assignedToUser.getName(), loggedInUser.getName()))) {
                        scheduleMap.put("assigneeType", "assignee");
                    } else if (StringUtils.equals(assignedToUser.getName(), loggedInUser.getName())) {
                        scheduleMap.put("assigneeType", "currentUser");
                    }
                } else {
                    scheduleMap.put("assigneeType", "-");
                }
            }


		/*Very Inefficient - use Collector */
		/*Just a safeguard, can happen when issue gets deleted */
            scheduleMap.put("issueId", schedule.getIssueId());
            scheduleMap.put("issueId", issue.getId());
            scheduleMap.put("issueKey", issue.getKey());
            if (issue.getSummary() != null) {
                scheduleMap.put("summary", issue.getSummary());
            }
            if (issue.getDescription() != null) {
                scheduleMap.put("issueDescription", rendererManager.getRenderedContent(AtlassianWikiRenderer.RENDERER_TYPE, issue.getDescription(), issue.getIssueRenderContext()));
            }
            if (issue.getLabels() != null) {
                scheduleMap.put("label", StringUtils.join(issue.getLabels(), ", "));
            }
            if (issue.getComponents() != null) {
                ArrayList<String> componentList = new ArrayList<String>();
                for (ProjectComponent comp : issue.getComponents()) {
                    componentList.add(comp.getName());
                }
                String componentStr = StringUtils.join(componentList, ", ");
                scheduleMap.put("component", componentStr);
            }
            scheduleMap.put("projectKey", issue.getProjectObject().getKey());
            scheduleMap.put("canViewIssue", true);

            if(Objects.nonNull(schedule.getEstimatedTime())) {
                long issueEstimateTime = schedule.getEstimatedTime();
                long loggedTime = null != schedule.getLoggedTime() ? schedule.getLoggedTime() : 0L;

                double workflowCompletePercentage = (loggedTime * 100.0) / issueEstimateTime;
                double workflowLoggedTimedIncreasePercentage = (issueEstimateTime * 100.0) / loggedTime;
                NumberFormat formatter = new DecimalFormat("#0.00");

                scheduleMap.put("executionEstimatedTime",getDateStringPretty(issueEstimateTime));
                scheduleMap.put("workflowCompletePercentage",formatter.format(workflowCompletePercentage));
                scheduleMap.put("workflowLoggedTimedIncreasePercentage",formatter.format(workflowLoggedTimedIncreasePercentage));
            }

            if(Objects.nonNull(schedule.getLoggedTime())) {
                scheduleMap.put("executionTimeLogged",getDateStringPretty(schedule.getLoggedTime().longValue()));
            }

            if(Objects.nonNull(issue.getOriginalEstimate()) && issue.getOriginalEstimate() > 0L) {
                scheduleMap.put("issueOriginalEstimate",getDateStringPretty(issue.getOriginalEstimate().longValue()));
                scheduleMap.put("isIssueEstimateNil",Boolean.FALSE);
            }else {
                scheduleMap.put("isIssueEstimateNil",Boolean.TRUE);
            }

            scheduleMap.put("isExecutionWorkflowEnabled",JiraUtil.getExecutionWorkflowEnabled(schedule.getProjectId()));
            scheduleMap.put("isTimeTrackingEnabled",ComponentAccessor.getComponent(TimeTrackingConfiguration.class).enabled());

        } else {
            //Mask the data
            maskCycleData(issue,schedule,scheduleDefectList,scheduleMap);
        }
        return scheduleMap;
    }


    private List<Schedule> getScheduleIfExists(Integer projectId, Integer issueId, Integer cycleId, Integer versionId, Long folderId) {
        Map<String, Object> filter = new HashMap<String, Object>();
        filter.put("pid", projectId);
        filter.put("issueId", issueId);
        filter.put("cid", new Integer[]{cycleId});
        filter.put("vid", versionId);
        if(folderId != null && !folderId.equals(ApplicationConstants.ADHOC_SYSTEM_FOLDER_ID)) {
        	filter.put("folderId", folderId);
        } else {
        	filter.put("folderId", null);
        }
        List<Schedule> schedules = scheduleManager.searchSchedules(filter);
        if (schedules.size() > 0)
            return schedules;
        return null;
    }
    
    /**
     * @param params
     * @param projectId
     * @param versionId
     * @param issueId
     * @return
     */
    private Map<String, Object> createSchedulePropertiesMap(final Map<String, Object> params, Integer projectId, Integer versionId, Integer issueId) {
        Map<String, Object> scheduleProperties = new HashMap<String, Object>();
        Date date = new Date();
        Long folderId = ZCollectionUtils.getAsLong(params, "folderId");
        scheduleProperties.put("ISSUE_ID", issueId);
        scheduleProperties.put("PROJECT_ID", Long.valueOf(projectId));
        scheduleProperties.put("VERSION_ID", Long.valueOf(versionId));
        Integer cycleId = params.get("cycleId") != null ? Integer.parseInt(params.get("cycleId").toString()) : null;

        if (cycleId != null && cycleId.intValue() != -1)
            scheduleProperties.put("CYCLE_ID", cycleId);
        
        if (folderId != null && !folderId.equals(ApplicationConstants.ADHOC_SYSTEM_FOLDER_ID)) {
        	scheduleProperties.put("FOLDER_ID", ZCollectionUtils.getAsLong(params, "folderId"));
        }

		scheduleProperties.put("DATE_CREATED", date);
		scheduleProperties.put("STATUS", "-1");
		scheduleProperties.put("ORDER_ID",(scheduleManager.getMaxOrderId()) + 1);
		scheduleProperties.put("CREATED_BY",UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
		scheduleProperties.put("MODIFIED_BY",UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
		scheduleProperties.put("EXECUTION_WORKFLOW_STATUS", ExecutionWorkflowStatus.CREATED);
		return scheduleProperties;
	}

    /**
     * Sets VersionSelected which is good for current session
     * @param selectedVersion
     */
    private void setSelectedVersion(final HttpServletRequest req, final String selectedVersion) {
        req.getSession(false).getServletContext().setAttribute(SessionKeys.CYCLE_SUMMARY_VERSION + authContext.getLoggedInUser(), selectedVersion);
    }

    /**
     * Checks if directory exists for Index else Indexes all
     */
    private void checkIndexDirectoryExists() {
        ScheduleResourceHelper scheduleHelper = new ScheduleResourceHelper(scheduleIndexManager);
        if (!scheduleHelper.isIndexDirectoryPresent()) {
            indexAll(true, false, false, null);
        }
    }
    
    /**
     * Used by both burndown and last xDays chart.
     * For burndown: Either cycleId or sprintId should be set or graphType should overridden to burndown
     * @param projectId
     * @param versionId
     * @param cycleId
     * @param sprintId
     * @param days
     * @param periodName
     * @param graphType ["burndown" | "periodic"]
     * @return
     */
    private JSONObject getExecutionCountByStatus(Integer projectId, Integer versionId, Integer cycleId, Integer sprintId, final String days, final String periodName, String graphType) {
        JSONObject finalResponse = null;
        try {
            //Add Lucene call
            ScheduleResourceHelper scheduleHelper = new ScheduleResourceHelper(scheduleIndexManager);
            //Rather than having another call to get Executed/Unexecuted, pass an hardcoding
            boolean showExecutionsOnly = true;
            if (null != cycleId || sprintId != null || StringUtils.equals(graphType, "burndown")) {
                finalResponse = scheduleHelper.getUnexecutedSchedulesByProject(projectId, versionId, cycleId, sprintId, periodName);
            } else {
                Map<Long, Map<String, Object>> data = null;
                finalResponse = new JSONObject();
                data = scheduleHelper.getSchedulesByProjectIdWithDuration(projectId, days, periodName, showExecutionsOnly);
                JSONObject summary = scheduleHelper.executionDurationToJSON(data);
                finalResponse.put("data", summary);
            }
        } catch (JSONException e) {
            log.fatal("Error in preparing JSON response for schedules with criterion ", e);
        } catch (Exception e) {
            log.fatal("Unable to perform search ", e);
        }
        return finalResponse;
    }
    
    /**
     * @param projectId
     * @param issueType
     * @param searcher
     * @param pComp     - ProjectComponent, if null is passed, search is done for issues with no component
     * @return
     * @throws SearchException
     */
    private Collection<Long> searchIssuesByComponent(Long projectId, String issueType, IndexSearcher searcher, ProjectComponent pComp) throws SearchException {
        JqlClauseBuilder builder = JqlQueryBuilder.newClauseBuilder();
        if (pComp != null)
            builder = builder.project(projectId).and().component(pComp.getId()).and().issueType(issueType);
        else
            builder = builder.project(projectId).and().componentIsEmpty().and().issueType(issueType);
        Query query = builder.buildQuery();
        log.debug("Query is" + query.toString());

        IssueIdsCollector collector = new IssueIdsCollector(searcher);
        searchProvider.search(query, authContext.getLoggedInUser(), collector);
        Collection<Long> issueIds = (Collection<Long>) collector.getValue();
        return issueIds;
    }
    
    /**
     * Prepares Schedule Summary. Checks the Index First , if does not return anything, makes a backend call
     *
     * @param issueIds
     * @param groupFldId
     * @param groupFldName
     * @return
     */
    private Map<String, Object> prepareScheduleSummary(Collection<Long> issueIds, Long versionId, Long groupFldId, String groupFldName) {
        ScheduleResourceHelper scheduleHelper = new ScheduleResourceHelper(scheduleIndexManager);
        Map<String, Object> countByStatus = scheduleHelper.getExecutionSummaryByIssueIds(issueIds, versionId);
        return scheduleHelper.populateScheduleSummary(groupFldId, groupFldName, countByStatus);
    }
    
    /**
     * @return
     */
    private Map<String, Map<String, Object>> getExecutionStatusMap() {
        final Set<Entry<Integer, ExecutionStatus>> statuses = JiraUtil.getExecutionStatuses().entrySet();
        Map<String, Map<String, Object>> statusesMap = new LinkedHashMap<String, Map<String, Object>>();
        for (Entry<Integer, ExecutionStatus> status : statuses) {
            Map<String, Object> statusMap = new HashMap<String, Object>();
            statusMap.put("id", status.getKey());
            statusMap.put("name", status.getValue().getName());
            statusMap.put("color", status.getValue().getColor());
            statusMap.put("desc", status.getValue().getDescription());
            statusesMap.put(status.getKey().toString(), statusMap);
        }
        return statusesMap;
    }

    /**
     * @param schedule
     * @param defectsKeys
     * @param associatedIssueId
     */
    private Map<String, Object> persistRelatedDefects(Schedule schedule, List<String> defectsKeys, Long associatedIssueId) {
        List<Integer> issueIds = new ArrayList<Integer>();

        if (defectsKeys != null) {

            //Get unique Defect Set by removing the duplicate
            Set<String> uniqueDefectSet = new HashSet<String>(defectsKeys);

            for (String issueKey : uniqueDefectSet) {
                MutableIssue issue = issueManager.getIssueObject(issueKey);
                if (issue != null && (!issue.getId().equals(associatedIssueId))) {
                    issueIds.add(Integer.valueOf(issue.getId().intValue()));
                } else {
                    log.info("Key passed in does not exist. skipping key");
                }
            }
        }
        Map<String, Object> associatedDefects = scheduleManager.saveAssociatedDefects(schedule, issueIds);
        return associatedDefects;
    }

    private void removeRemoteLinks(Collection<Integer> deletedDefects, String scheduleId) {
        for (Integer issueId : deletedDefects) {
            Issue issue = issueManager.getIssueObject(new Long(issueId));
            if (null != issue)
                remoteIssueLinkManager.removeRemoteIssueLinkByGlobalId(issue, scheduleId, authContext.getLoggedInUser());
        }
    }
    
    private Response buildErrorMessage(String errorMessage) {
        log.error(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,errorMessage));
        return buildErrorMessage(Status.BAD_REQUEST, errorMessage);
    }

	private Response buildErrorMessage(Status status, String errorMessage) {
		JSONObject errorJsonObject = new JSONObject();
		try {
			errorJsonObject.put("error", errorMessage);
		} catch (JSONException e) {
			log.error("Error constructing JSON", e);
		}
        log.error(String.format(ERROR_LOG_MESSAGE, status.BAD_REQUEST.getStatusCode(),status.BAD_REQUEST,errorMessage));
		return Response.status(status).entity(errorJsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
	}


    private Response populateErrorMsg(String errorMessage) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("error", errorMessage);
        } catch (JSONException e) {
            log.warn("Error creating JSON Object", e);
        }
        log.error(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,errorMessage));
        return Response.status(Status.BAD_REQUEST).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
    }
    

    /**
     * Add key to result Map for Add Test
     *
     * @param resultMap
     * @param key
     * @param issueKey
     */
    private void addToResult(Map<String, List<String>> resultMap, String key, String issueKey) {
        List<String> successList = resultMap.get(key);
        if (successList == null) {
            successList = new ArrayList<String>();
        }
        if (StringUtils.equalsIgnoreCase(key, "invalid")) {
            successList.add(issueKey);
        } else {
            String formattedMessage = "<a href='" + jiraBaseUrls.baseUrl() + "/browse/" + issueKey + "'>" + issueKey + "</a>";
            successList.add(formattedMessage);
        }
        resultMap.put(key, successList);
    }
    
    /**
     * Modifies savedSearchQuery and adds issueType test check. Redundant check shouldnt hurt.
     *
     * @param testTypeId
     * @param params
     * @throws SearchException
     */
    private Collection<Long> populateIssueIdsFromSavedSearch(String testTypeId, Map<String, Object> params) throws SearchException {
        SearchRequestService searchRequestService = ComponentAccessor.getComponentOfType(SearchRequestService.class);

        String[] searchIds = ZCollectionUtils.getAsString(params, "searchId").split(",");
        Collection<Long> issueIds = new HashSet<Long>();
        params.put("issues", issueIds);
        for (String sidString : searchIds) {
            Long searchId = new Long(sidString);
            SearchRequest sr = searchRequestService.getFilter(new JiraServiceContextImpl(authContext.getLoggedInUser()), searchId);
            if (sr == null) {
                log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("project.cycle.addTests.filter.notfound", searchId)));
                throw new SearchException(ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("project.cycle.addTests.filter.notfound", searchId));
            }
            Query query = JqlQueryBuilder.newBuilder(sr.getQuery()).where().and().issueType(testTypeId).buildQuery();

            SearchProviderFactory searchProviderFactory = ComponentManager.getComponentInstanceOfType(SearchProviderFactory.class);
            IndexSearcher searcher = searchProviderFactory.getSearcher(SearchProviderFactory.ISSUE_INDEX);
            log.debug("Query to search issues ids from saved search: " + query.getQueryString());
            IssueIdsCollector collector = new IssueIdsCollector(searcher);
            searchProvider.search(query, authContext.getLoggedInUser(), collector);
            if (collector.getValue() != null)
                issueIds.addAll((Set<Long>) collector.getValue());
                log.debug("Number of test cases fetched from saved search " + searchId + ": " + (issueIds != null ? issueIds.size() : 0));
        }
        return issueIds;
    }

    /**
     * retrieves IssueIds from Another Cycle 
     * @param params
     * @throws Exception
     */
    private Collection<Long> populateIssueIdsFromPreviousCycle(Map<String, Object> params) throws Exception {
        String projectId = ZCollectionUtils.getAsString(params, "projectId");
        String versionId = ZCollectionUtils.getAsString(params, "fromVersionId");
        String fromCycleId = ZCollectionUtils.getAsString(params, "fromCycleId");
        String priorityIds = ZCollectionUtils.getAsString(params, "priorities");
        String statusIds = ZCollectionUtils.getAsString(params, "statuses");
        String componentIds = ZCollectionUtils.getAsString(params, "components");
        String labels = (String) params.get("labels");
        Boolean hasDefects = (Boolean) params.get("hasDefects");
        String defectStatuses = ZCollectionUtils.getAsString(params, "withStatuses");
        Long fromFolderId = ZCollectionUtils.getAsLong(params, "fromFolderId");

        StringBuffer luceneQueryString = new StringBuffer("+(CYCLE_ID:\"").append(fromCycleId).append("\")");
        luceneQueryString.append(" +(PROJECT_ID:\"").append(projectId).append("\")");
        luceneQueryString.append(" +(VERSION_ID:\"").append(versionId).append("\")");
        appendClause("PRIORITY_ID", priorityIds, luceneQueryString);
        appendClause("COMPONENT_ID", componentIds, luceneQueryString);
        appendClause("STATUS", statusIds, luceneQueryString);
        appendClause("LABEL", labels, luceneQueryString);
        if (hasDefects != null && hasDefects == true) {
            luceneQueryString.append(" -(SCHEDULE_DEFECT_ID:\"-1\")");
            appendClause("SCHEDULE_DEFECT_STATUS", defectStatuses, luceneQueryString);
        }
        if(null != fromFolderId) {
            luceneQueryString.append(" +(FOLDER_ID:").append(fromFolderId).append(")");
        }else {
            String currentZFJVersion = ComponentAccessor.getPluginAccessor().getPlugin(ConfigurationConstants.PLUGIN_KEY).getPluginInformation().getVersion();
            if(validateLuceneIndex(fromCycleId,projectId,versionId) || (null == fromFolderId && JiraUtil.isZFJVersionGreaterThanEqualsTo(currentZFJVersion,3,6))) {
                luceneQueryString.append(" +(FOLDER_ID:\"-1\"").append(")");
            }
        }
        log.debug("Query to search issue ids from previous cycle " + luceneQueryString);

        IssueIdsCollector issueIdsCollector = new IssueIdsCollector("ISSUE_ID", scheduleIndexManager.getRawEntitySearcher());
        scheduleIndexManager.search(new String[]{"ISSUE_ID"}, luceneQueryString.toString(), issueIdsCollector);
        Collection<Long> issueIds = (Set<Long>) issueIdsCollector.getValue();
        params.put("issues", issueIds);
        log.debug("Number of test cases fetched from previous cycle search: " + (issueIds != null ? issueIds.size() : 0));
        return issueIds;
    }

    /**
     * @param luceneField
     * @param values
     * @param luceneQueryString
     */
    private void appendClause(String luceneField, String values, StringBuffer luceneQueryString) {
        if (StringUtils.isNotBlank(values)) {
            luceneQueryString.append(" +(");
            for (String value : values.split(",")) {
                luceneQueryString.append(luceneField).append(":\"").append(value).append("\" ");
            }
            luceneQueryString.append(")");
        }
    }
    
    /**
     * Gets Schedule by new OrderId for reOrdering execution
     * @param newOrderId
     * @param cycleId
     * @param versionId
     * @return
     */
    private Schedule getScheduleByOrderId(Integer newOrderId, int cycleId, Integer versionId) {
        List<Schedule> schedules = scheduleManager.getScheduleByOrderId(newOrderId.intValue(), cycleId, versionId.intValue());
        return schedules != null ? schedules.get(0) : null;
    }
    
    private Response extractReorderErrorMessage() throws JSONException {
        JSONObject jsonErrorObject = new JSONObject();
        String message = authContext.getI18nHelper().getText("cycle.reorder.executions.general.error");
        jsonErrorObject.put("errorMessages", message);
        log.error(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,message));
        return Response.status(Status.BAD_REQUEST).entity(jsonErrorObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
    }
    
	private boolean verifyBulkPermissions(Long projectId ,ApplicationUser user) {
		//Check ZephyrPermission and update response to include execution per project permissions
		ProjectPermissionKey cyclePermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_BROWSE_CYCLE.toString());
		ProjectPermissionKey executionPermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_CREATE_EXECUTION.toString());
		Collection<ProjectPermissionKey> projectPermissionKeys = new ArrayList<ProjectPermissionKey>();
		projectPermissionKeys.add(executionPermissionKey);
		projectPermissionKeys.add(cyclePermissionKey);
		boolean loggedInUserHasZephyrPermission = zephyrPermissionManager.validateUserPermissions(projectPermissionKeys, null, user ,projectId);
		return loggedInUserHasZephyrPermission;
	}
	
	private boolean verifyBulkEditPermissions(Schedule schedule,ApplicationUser user) {
		//Check ZephyrPermission and update response to include execution per project permissions
		ProjectPermissionKey cyclePermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_BROWSE_CYCLE.toString());
		ProjectPermissionKey executionPermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_EDIT_EXECUTION.toString());
		Collection<ProjectPermissionKey> projectPermissionKeys = new ArrayList<ProjectPermissionKey>();
		projectPermissionKeys.add(executionPermissionKey);
		projectPermissionKeys.add(cyclePermissionKey);
		boolean loggedInUserHasZephyrPermission = zephyrPermissionManager.validateUserPermissions(projectPermissionKeys, null, user ,schedule.getProjectId());
		return loggedInUserHasZephyrPermission;
	}
	
	/**
	 * @return 
	 */
	private Response getPermissionDeniedErrorResponse(String errorMessage) {
		JSONObject errorJsonObject = null;
		try {
			errorJsonObject = new JSONObject();
            // build error map
            errorJsonObject.put("PERM_DENIED", errorMessage);
            Response.ResponseBuilder builder = Response.status(Response.Status.FORBIDDEN);
            builder.type(MediaType.APPLICATION_JSON);
            builder.entity(errorJsonObject.toString());
            return builder.build();
		} catch(JSONException e) {
			log.error("Error creating JSON Object",e);
		}
		return Response.status(Status.BAD_REQUEST).entity(errorJsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
	}

    private void maskCycleData(Issue issue, Schedule schedule, List<Map<String, String>> scheduleDefectList, Map<String, Object> scheduleMap) {
	    log.debug("Masking Cycle Data");
        Cycle cycle = schedule.getCycle();
        scheduleMap.put("id", schedule.getID());
        scheduleMap.put("orderId", schedule.getOrderId());
        scheduleMap.put("executionStatus", ApplicationConstants.MASKED_DATA);
        if (schedule.getStatus() != null && !"-1".equals(schedule.getStatus())) {
            Long scheduleExecutedOnTime = schedule.getExecutedOn();
            if (scheduleExecutedOnTime != null)
                scheduleMap.put("executedOn", ApplicationConstants.MASKED_DATA);

            User executor = UserCompatibilityHelper.getUserForKey(schedule.getExecutedBy());
            scheduleMap.put("executedBy", ApplicationConstants.MASKED_DATA);
            scheduleMap.put("executedByDisplay", executor != null ? ApplicationConstants.MASKED_DATA : "");
        }
        if (scheduleDefectList != null && scheduleDefectList.size() > 0) {
            scheduleMap.put("defects", scheduleDefectList);
        }
        //scheduleMap.put( "comment", schedule.getComment() != null ? StringUtils.abbreviate(schedule.getComment(), 100) : fetchDefaultStatus(schedule.getStatus()));
        scheduleMap.put("comment", schedule.getComment() != null ? ApplicationConstants.MASKED_DATA : "");
        scheduleMap.put("htmlComment", schedule.getComment() != null ? ApplicationConstants.MASKED_DATA : "");

        if (cycle != null) {
            scheduleMap.put("cycleId", cycle.getID());
            scheduleMap.put("cycleName", cycle.getName());
        } else {
            scheduleMap.put("cycleId", ApplicationConstants.AD_HOC_CYCLE_ID);
            scheduleMap.put("cycleName", ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.je.cycle.adhoc"));
        }

        long versionId = schedule.getVersionId();
        scheduleMap.put("versionId", versionId);
        scheduleMap.put("versionName", ApplicationConstants.MASKED_DATA);

        scheduleMap.put("projectId", schedule.getProjectId());

        //creator and modified by
        scheduleMap.put("createdBy", ApplicationConstants.MASKED_DATA);
        scheduleMap.put("modifiedBy", ApplicationConstants.MASKED_DATA);

        //assignedTo
        if (schedule.getAssignedTo() != null) {
            User assignedToUser = UserCompatibilityHelper.getUserForKey(schedule.getAssignedTo());
            String assignedTo = assignedToUser != null ? assignedToUser.getDisplayName() : schedule.getAssignedTo();
            String assigneeDisplay = (assignedToUser != null && assignedToUser.isActive()) ? assignedTo : assignedTo + " (Inactive)";
            String assigneeUserName = assignedToUser != null ? assignedToUser.getName() : assignedTo;
            scheduleMap.put("assignedTo", ApplicationConstants.MASKED_DATA);
            scheduleMap.put("assignedToDisplay", ApplicationConstants.MASKED_DATA);
            scheduleMap.put("assignedToUserName", ApplicationConstants.MASKED_DATA);

            ApplicationUser appUser = authContext.getLoggedInUser();
            if(appUser != null) {
                User loggedInUser = UserCompatibilityHelper.getUserForKey(appUser.getKey());
                //Fix for ZFJ-1549: if user is deleted, setting assigneeType -> assignee
                if (assignedToUser == null || (!StringUtils.equals(assignedToUser.getName(), loggedInUser.getName()))) {
                    scheduleMap.put("assigneeType", "assignee");
                } else if (StringUtils.equals(assignedToUser.getName(), loggedInUser.getName())) {
                    scheduleMap.put("assigneeType", "currentUser");
                }
            } else {
                scheduleMap.put("assigneeType", "-");
            }
        }


		/*Very Inefficient - use Collector */
		/*Just a safeguard, can happen when issue gets deleted */
        scheduleMap.put("issueId", schedule.getIssueId());
        if (issue.getSummary() != null) {
            scheduleMap.put("summary", ApplicationConstants.MASKED_DATA);
        }
        if (issue.getDescription() != null) {
            scheduleMap.put("issueDescription", ApplicationConstants.MASKED_DATA);
        }
        if (issue.getLabels() != null) {
            scheduleMap.put("label", ApplicationConstants.MASKED_DATA);
        }
        if (issue.getComponents() != null) {
            ArrayList<String> componentList = new ArrayList<String>();
            for (ProjectComponent comp : issue.getComponents()) {
                componentList.add(ApplicationConstants.MASKED_DATA);
            }
            String componentStr = StringUtils.join(componentList, ", ");
            scheduleMap.put("component", componentStr);
        }
        scheduleMap.put("projectKey", ApplicationConstants.MASKED_DATA);
        String[] issueKeyArr = StringUtils.split(issue.getKey(),"-");
        String maskedKey = issueKeyArr.length > 0 ? issueKeyArr[0] : "";
        scheduleMap.put("issueKey", maskedKey + "-" + ApplicationConstants.MASKED_DATA);
        scheduleMap.put("canViewIssue", false);
    }

    /**
     *
     * @param zqlSearchResultBean
     * @param componentIdArr
     * @param statusMap
     * @return
     */
    private Map<String,Integer> prepareExecutionStatusCountMap(ZQLSearchResultBean zqlSearchResultBean, String componentIdArr, Map<Integer, String> statusMap) {

        Map<String, Integer> executionStatusMap = populateExecutionStatusMap(statusMap);
        if(zqlSearchResultBean != null && zqlSearchResultBean.getExecutions() != null) {
            for (ZQLScheduleBean zqlScheduleBean : zqlSearchResultBean.getExecutions()) {
                updateExecutionStatusMap(executionStatusMap, zqlScheduleBean.getStatus() != null ? zqlScheduleBean.getStatus().getId() : ApplicationConstants.UNEXECUTED_STATUS, statusMap);
            }
        }
        return executionStatusMap;
    }

    /**
     *
     * @param zqlSearchResultBean
     * @param statusesMap
     * @return
     */
    private Map<String,Map<String,Integer>> prepareExecutionStatusCountMapPerAssignee(ZQLSearchResultBean zqlSearchResultBean, Map<Integer, String> statusesMap) {

        Map<String, Map<String, Integer>> assigneeExecutionStatusMap = new HashMap<>();
        Map<String, Integer> statusMap;
        if(zqlSearchResultBean != null && zqlSearchResultBean.getExecutions() != null) {
            for (ZQLScheduleBean zqlScheduleBean : zqlSearchResultBean.getExecutions()) {
                Map<String, Integer> executionStatusMap = populateExecutionStatusMap(statusesMap);

                String assigneeName = null;
                if (StringUtils.isNotBlank(zqlScheduleBean.getAssignee())) {
                    assigneeName = zqlScheduleBean.getAssigneeDisplay();
                }

                if (StringUtils.isNotBlank(assigneeName)) {
                    assigneeExecutionStatusMap.putIfAbsent(assigneeName, executionStatusMap);

                    //String executionStatus = statusesMap.get(zqlScheduleBean.getStatus().getId());
                    String executionStatus = statusesMap.get(zqlScheduleBean.getStatus() != null ? zqlScheduleBean.getStatus().getId() : zqlScheduleBean.getStatusId());
                    if (null != executionStatus) {
                        statusMap = assigneeExecutionStatusMap.get(assigneeName);
                        statusMap.put(executionStatus, statusMap.get(executionStatus) + 1);
                        assigneeExecutionStatusMap.put(assigneeName, statusMap);
                    }
                }
            }
        }
        return assigneeExecutionStatusMap;
    }

    /**
     *
     * @param executionStatusCountMap
     * @param statusColorMap
     * @return
     */
    private List<ExecutionStatusCountBean> prepareExecutionsStatusCountPerCycle(Map<String, Integer> executionStatusCountMap,
                                                                                Map<String, String> statusColorMap) {
        List<ExecutionStatusCountBean> response = new ArrayList<>();
        ExecutionStatusCountBean bean;
        for (Map.Entry<String, Integer> entry : executionStatusCountMap.entrySet()) {
            bean = new ExecutionStatusCountBean();
            bean.setStatusName(entry.getKey());
            bean.setStatusCount(entry.getValue());
            bean.setStatusColor(statusColorMap.get(entry.getKey()));
            response.add(bean);
        }
        return response;
    }


    /**
     *  @param executionStatusMap
     * @param status
     * @param statusMap
     */
    private void updateExecutionStatusMap(Map<String, Integer> executionStatusMap, Integer status, Map<Integer, String> statusMap) {

        String executionStatus = statusMap.get(status);
        if(null != executionStatus) {
            executionStatusMap.put(executionStatus,executionStatusMap.get(executionStatus)+1);
        }
    }

    /**
     *
     * @param statusMap
     * @return
     */
    private Map<String,Integer> populateExecutionStatusMap(Map<Integer, String> statusMap) {
        Map<String, Integer> executionStatusMap = new LinkedHashMap<>();
        for(Map.Entry<Integer, String> entry : statusMap.entrySet()) {
            executionStatusMap.put(entry.getValue(), 0);
        }
        return executionStatusMap;
    }

    private void setFolderToSchedule(Schedule schedule, Long folderId) {
    	if(folderId != null && folderId.intValue() > 0) {
    		Folder folder = folderManager.getFolder((Long)folderId);
    		schedule.setFolder(folder);
    	}
    }

    private List<Integer> convertStringListToIntegerList(List<String> projectIds) {
        if(CollectionUtils.isEmpty(projectIds))
            return Collections.EMPTY_LIST;

        return projectIds.stream().map(Integer::valueOf).collect(Collectors.toList());
    }

    private Response isFolderExist(Long folderId) {
        JSONObject jsonObjectResponse = new JSONObject();
        Folder folder = folderManager.getFolder(folderId);
        if(folder == null) {
            try {
                jsonObjectResponse.put("error", authContext.getI18nHelper().getText("project.folder.not.exist"));
            } catch (JSONException e) {
            }
            log.error("[Error] [Error code:"+ Response.Status.BAD_REQUEST.getStatusCode() + " " + Response.Status.BAD_REQUEST +" Error Message :"+
                    authContext.getI18nHelper().getText("project.folder.not.exist"));
            return Response.status(Status.BAD_REQUEST).entity(jsonObjectResponse.toString()).build();
        }
        return null;
    }

    /**
     *
     * @param cycleId
     * @param projectId
     * @param versionId
     * @param folderId
     * @return
     */
    private JSONObject getExecutionSummaryForCycleAndProjectAndVersionAndFolder(Integer cycleId, Long projectId, Long versionId, Integer folderId) {

        int totalExecuted = 0;
        int totalExecutions = 0;
        JSONObject jsonResponse = new JSONObject();
        JSONArray expectedOperators = new JSONArray();
        JSONObject jsonObject = new JSONObject();

        if(null == cycleId) {
            cycleId = ApplicationConstants.AD_HOC_CYCLE_ID;
        }

        if(null != projectId && null !=cycleId && null !=versionId) {
            try {
                List<Long> projectIdList = new ArrayList<>();
                projectIdList.add(projectId);
                String[] versionIds = {versionId+""};
                Long inputFolderId = null;
                if(null != folderId && folderId != -1) {
                    inputFolderId = Long.valueOf(folderId);
                }

                List<ExecutionSummaryImpl> allSummaries = scheduleManager.getExecutionDetailsByCycleAndFolder(projectIdList, versionIds,
                        Long.valueOf(cycleId), inputFolderId, null);
                for (ExecutionSummaryImpl executionSummary : allSummaries) {
                    if (executionSummary.getExecutionStatusKey().intValue() != -1 &&
                            !org.apache.commons.lang.StringUtils.equalsIgnoreCase(executionSummary.getExecutionStatusName(), "Unexecuted")) {
                        totalExecuted += executionSummary.getCount();
                    }
                    totalExecutions += executionSummary.getCount();
                    expectedOperators.put(executionSummaryToJSON(executionSummary));
                }

                jsonObject.put("executionSummary", expectedOperators);
                jsonResponse.put("executionSummaries", jsonObject);
                jsonResponse.put("totalExecutions", totalExecutions);
                jsonResponse.put("totalExecuted", totalExecuted);
            } catch (JSONException e) {
                log.error("Exception occur while setting the execution response.");
            }
        }

        return jsonResponse;
    }

    private JSONObject executionSummaryToJSON(ExecutionSummaryImpl executionSummary) throws JSONException {
        JSONObject object = new JSONObject();
        object.put("count", executionSummary.getCount());
        object.put("statusKey", executionSummary.getExecutionStatusKey());
        object.put("statusName", executionSummary.getExecutionStatusName());
        object.put("statusColor", executionSummary.getExecutionStatusColor());
        return object;
    }

    /**
     * Api to get custom field values for execution
     * @param executionId
     * @param issue
     * @return
     */
    private Map<String, CustomFieldValueResource.CustomFieldValueResponse> getCustomFieldsValue(Integer executionId, Issue issue) {
        return customFieldValueResourceDelegate.getCustomFieldValuesByEntityId(executionId, ApplicationConstants.ENTITY_TYPE.EXECUTION.name(), issue);
    }

    /**
     *
     * @param estimateDateValue
     * @return
     */
    private String getDateStringPretty(long estimateDateValue) {
        int hoursPerDay = Integer.parseInt(ComponentAccessor.getApplicationProperties().getDefaultBackedString(APKeys.JIRA_TIMETRACKING_HOURS_PER_DAY));
        int daysPerWeek = Integer.parseInt(ComponentAccessor.getApplicationProperties().getDefaultBackedString(APKeys.JIRA_TIMETRACKING_DAYS_PER_WEEK));
        return DateUtils.getDurationString(estimateDateValue,hoursPerDay,daysPerWeek);
    }

    @Override
	public Integer getExecutionTotalCount(Long projectId, Long versionId, Integer cycleId, Long folderId) {
		return scheduleManager.getSchedulesCount(versionId, projectId, cycleId, folderId);
	}
    
    private Map<String, Object> getReindexJobProgressProperties(String jobProgressToken, String name, Integer completedNodeCount, Long prevCount, Long currCount, Date date, Integer nodeCount, Long projectId) {
    	Map<String, Object> reindexJobProgressProperties = new HashMap<>();
    	reindexJobProgressProperties.put("NAME", name);
    	reindexJobProgressProperties.put("JOB_PROGRESS_ID", jobProgressToken);
    	reindexJobProgressProperties.put("PREVIOUS_INDEXED_COUNT", prevCount);
    	reindexJobProgressProperties.put("CURRENT_INDEXED_COUNT", currCount);
    	reindexJobProgressProperties.put("COMPLETED_NODE_COUNT", completedNodeCount);
    	reindexJobProgressProperties.put("DATE_INDEXED", date);
    	reindexJobProgressProperties.put("NODE_COUNT", nodeCount);
    	reindexJobProgressProperties.put("PROJECT_ID", projectId);
    	return reindexJobProgressProperties;
    }
    
    public void applyChangesToIndexFromDate(long dateTime, java.util.OptionalLong projectId) throws IndexException {
    	//Applying all the executions updated to reindex.
    	ChangeZJEGroup[] changeZJEGroupArr =  auditManager.getZFJChangeLogsByDate(dateTime, com.thed.zephyr.je.event.EventType.EXECUTION_UPDATED.toString(), projectId);
    	List<Long> exeucutionsUpdatedList =Stream.of(changeZJEGroupArr).map(changeZJEGroup -> Long.valueOf(changeZJEGroup.getZephyrEntityId())).collect(Collectors.toList());
    	if(Objects.nonNull(exeucutionsUpdatedList) && exeucutionsUpdatedList.size() > 0) {
        	scheduleIndexManager.reIndexScheduleWithOutMessage(new ScheduleIdsScheduleIterable(exeucutionsUpdatedList, scheduleManager, new ArrayList<>()), Contexts.nullContext(), null);
    	}
    	
    	//Applying all the cycle version updated to reindex.
    	changeZJEGroupArr =  auditManager.getZFJChangeLogsByDate(dateTime, com.thed.zephyr.je.event.EventType.CYCLE_UPDATED.toString(), projectId);
    	List<Integer> cyclesUpdatedList =Stream.of(changeZJEGroupArr).map(changeZJEGroup -> changeZJEGroup.getZephyrEntityId()).collect(Collectors.toList());    	
    	if(Objects.nonNull(cyclesUpdatedList) && cyclesUpdatedList.size() > 0) {
    		for(Integer cycleId : cyclesUpdatedList) {
    			Schedule[] schedulesArr = scheduleManager.getSchedulesByCycleId(cycleId);
    			EnclosedIterable<Schedule> schedules = CollectionEnclosedIterable.copy(Arrays.asList(schedulesArr));
        		scheduleIndexManager.reIndexSchedule(schedules, Contexts.nullContext());
    		}      	
    	}
    	
		//Applying all the executions deleted to reindex.
    	processChangesByEventType(dateTime, com.thed.zephyr.je.event.EventType.EXECUTION_DELETED.toString(), "schedule_id", projectId);
		
		//Applying all the folders deleted to reindex.
    	processChangesByEventType(dateTime, com.thed.zephyr.je.event.EventType.FOLDER_DELETED.toString(), ApplicationConstants.FOLDER_IDX, projectId);
		
		//Applying all the cycles deleted to reindex.
    	processChangesByEventType(dateTime, com.thed.zephyr.je.event.EventType.CYCLE_DELETED.toString(), ApplicationConstants.CYCLE_IDX, projectId);
    	
    	//Applying all the issues deleted to reindex.
    	processChangesByEventType(dateTime, com.thed.zephyr.je.event.EventType.ISSUE_DELETED.toString(), "ISSUE_ID", projectId);
    	
    	//Applying all the projects deleted to reindex.
    	processChangesByEventType(dateTime, com.thed.zephyr.je.event.EventType.PROJECT_DELETED.toString(), ApplicationConstants.PROJECT_ID_IDX, projectId);
    	
    }
    
    private void processChangesByEventType(long dateTime, String subEventType, String termName, java.util.OptionalLong projectId) {
    	ChangeZJEGroup[] changeZJEGroupArr = auditManager.getZFJChangeLogsByDate(dateTime, subEventType, projectId);
    	List<String> entityIds = Stream.of(changeZJEGroupArr).map(changeZJEGroup -> String.valueOf(changeZJEGroup.getZephyrEntityId())).collect(Collectors.toList());
    	if(Objects.nonNull(entityIds) && entityIds.size() > 0) {
    		EnclosedIterable<String> entityIdIterables = CollectionEnclosedIterable.copy(entityIds);
    		scheduleIndexManager.deleteBatchIndexByTerm(entityIdIterables, termName, Contexts.nullContext());		
    	}
    }

	@Override
   	public Response getPastExecutionsStatusCount(Long projectId,String howMany, String cycleIds, String versionId) {
           JSONObject finalResponse = new JSONObject();
           Map<String, Map<String, Integer>> dateExecutionStatusMap = new   HashMap<String, Map<String, Integer>>();
           try {
               //Handle First time Indexes
           	checkIndexDirectoryExists();
            Date date = new Date();
   			if(howMany == null  || StringUtils.isEmpty(howMany))
                howMany = new String("5");
   			//Find Execution status for the past 10 days created and executed on particular date
            Calendar calendar = GregorianCalendar.getInstance();
            calendar.add(Calendar.DATE,0-Integer.valueOf(howMany).intValue()+1);
   			Map<String, HashSet<String>>  dateIssueIdsMap = searchTestCasesByDate(projectId, calendar.getTime(),Integer.valueOf(howMany).intValue());
   			int count = 1;
   			while (count <= Integer.valueOf(howMany).intValue()) {
                calendar.setTime(date);
                calendar.add(Calendar.DATE, count-Integer.valueOf(howMany).intValue());
                calendar.set(Calendar.HOUR_OF_DAY,0);
                calendar.set(Calendar.MINUTE,0);
                calendar.set(Calendar.SECOND,0);
                HashSet<String> issueIds = dateIssueIdsMap.get(calendar.getTime().toString());
   				if (issueIds == null || issueIds.size() == 0) {
   					Map<Integer, String> statusesMap = new HashMap<>();
   					String creationDate = formatDate(calendar.getTime());
   					preparePastExecutionStatusResponse(statusesMap , dateExecutionStatusMap, creationDate);
   				} else {
                    int execCount = 1;
   				    while(execCount <= Integer.valueOf(howMany).intValue()) {
                        Calendar executionCalendar = GregorianCalendar.getInstance();
                        executionCalendar.setTime(date);
                        executionCalendar.add(Calendar.DATE, execCount-Integer.valueOf(howMany).intValue());
                        executionCalendar.set(Calendar.HOUR_OF_DAY,0);
                        executionCalendar.set(Calendar.MINUTE,0);
                        executionCalendar.set(Calendar.SECOND,0);
                        searchExecutionStatusForIssueIdsPerDateInAudit(cycleIds, versionId, calendar.getTime(), executionCalendar.getTime(), issueIds, dateExecutionStatusMap);
                        execCount++;
   				    }
                }
                count++;
            }
           finalResponse.put("statusSeries", getExecutionStatusMap());
           finalResponse.put("data", dateExecutionStatusMap);
        } catch (IOException e) {
           	log.fatal("Exception occured ", e);
   	    }  catch (JSONException e) {
           	log.fatal("Error in preparing JSON response for schedules count ", e);
   	    } catch (SearchException e) {
   	        log.fatal("Unable to perform search ", e);
   	    } catch (ParseException e) {
   	        log.fatal("Unable to perform parse of Date ", e);
   	    } catch (Exception e) {
               e.printStackTrace();;
   	        log.fatal("Exception occured ", e);
   	    }
       return Response.ok(finalResponse.toString()).cacheControl(ZephyrCacheControl.never()).build();
    }

    @Override
    public Response getExecutionsTimeTrackingPerCycleAndFolder(Long projectId, Long versionId, String cycles,
                                                               String folders) {
        String[] cycleIdArr = null;
        HashMap<String, Map<String, Map<String, ExecutionTimeTrackingStatusBean>>> response = new HashMap<>();
        Boolean executionWorkflowEnabled = JiraUtil.getExecutionWorkflowEnabled(projectId);
        Map<String, Map<String, ExecutionTimeTrackingStatusBean>> executionsTimeTrackingMap;
        /**
         * If execution workflow is enabled then fetch the executions time tracking.
         */
        if (executionWorkflowEnabled) {
            /* If cycleIds are present, fetch schedules from DB */
            if (StringUtils.isNotBlank(cycles) && cycles != null) {
                cycleIdArr = StringUtils.split(cycles, "|");
                cycleIdArr = (String[]) ArrayUtils.removeElement(cycleIdArr, String.valueOf(ApplicationConstants.AD_HOC_CYCLE_ID));
            }

            if (cycleIdArr != null && cycleIdArr.length > 0) {
                executionsTimeTrackingMap = prepareExecutionTimeTrackingPerCycleMap(folders, projectId, versionId, cycleIdArr);
                response.put("data", executionsTimeTrackingMap);
            } else {
                /**
                 * If cycle ids are not present then throw error since it can cause performance degrade.
                 */
                JSONObject jsonResponse = new JSONObject();
                try {
                    log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("zephyr.common.error.required",ApplicationConstants.CYCLE_ENTITY)));
                    jsonResponse.put("error", authContext.getI18nHelper().getText("zephyr.common.error.required",ApplicationConstants.CYCLE_ENTITY));
                    return Response.status(Status.BAD_REQUEST).entity(jsonResponse.toString()).cacheControl(ZephyrCacheControl.never()).build();
                } catch (JSONException e) {
                    log.error("Error occurred during retrieving gadget ", e);
                    return Response.status(Status.BAD_REQUEST).build();
                }
            }
        }else {
            Project project = projectManager.getProjectObj(projectId);
            String errorMessage = authContext.getI18nHelper().getText("workflow.project.disabled.error.label", project.getKey());
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,errorMessage));
            Map<String, Object> map = new HashMap<>();
            map.put("error", errorMessage);
            JSONObject jsonResponse = new JSONObject(map);
            return Response.status(Status.BAD_REQUEST).entity(jsonResponse.toString()).cacheControl(ZephyrCacheControl.never()).build();
        }
        return Response.ok(response).cacheControl(ZephyrCacheControl.never()).build();
    }

    @Override
    public void backUpIndexFiles() throws IOException {
        scheduleIndexManager.copyBackupfilesScheduleDirectory(
                ApplicationConstants.ZFJ_SHARED_HOME_PATH + ApplicationConstants.INDEX_BACKUP_FOLDER_NAME);
    }

    @Override
    public String getRecoveryBackUpPath() throws IOException {
        return scheduleIndexManager.getRecoveryPath(
                ApplicationConstants.ZFJ_SHARED_HOME_PATH + ApplicationConstants.INDEX_BACKUP_FOLDER_NAME);
    }

    /**
     *
     * @param projectId
     * @param date
     * @param howMany
     * @return
     * @throws SearchException
     * @throws ParseException
     * @throws IOException
     */
   	private Map<String, HashSet<String>> searchTestCasesByDate(Long projectId, Date date, int howMany)
   			throws SearchException, ParseException, IOException {
   		Map<String, HashSet<String>> dateIssueIdsMap = null;
   		SearchProviderFactory searchProviderFactory = ComponentManager.getComponentInstanceOfType(SearchProviderFactory.class);
   		IndexSearcher searcher = searchProviderFactory.getSearcher(SearchProviderFactory.ISSUE_INDEX);
   		dateIssueIdsMap = searchIssuesByDate(projectId, date, searcher,howMany);
   		return dateIssueIdsMap;

   	}

    /**
     *
     * @param projectId
     * @param inputDate
     * @param searcher
     * @param howMany
     * @return
     * @throws SearchException
     * @throws ParseException
     * @throws IOException
     */
   	private Map<String, HashSet<String>> searchIssuesByDate(Long projectId, Date inputDate, IndexSearcher searcher,int howMany) throws SearchException, ParseException, IOException {
   		
   		JqlClauseBuilder builder = JqlQueryBuilder.newClauseBuilder();
   		builder = builder.project(projectId);		
   		Query query = builder.buildQuery();
   		
   		JqlQueryBuilder jqlQueryBuilder = JqlQueryBuilder.newBuilder(query);
        JqlClauseBuilder whereClauseBuilder = jqlQueryBuilder.where().defaultAnd();
        Calendar betweenDate = Calendar.getInstance();
        betweenDate.add(Calendar.DATE,howMany);
        String inDate = formatDate(inputDate);
        String outDate = formatDate(betweenDate.getTime());
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        whereClauseBuilder.createdBetween(df.parse(inDate),df.parse(outDate));
        whereClauseBuilder.defaultAnd().issueType(JiraUtil.getTestcaseIssueTypeId());

   		log.debug("Query is" + whereClauseBuilder.buildQuery());
   		IssueObjectCollector collector = new IssueObjectCollector(new HashMap<>(), searcher);
   		searchProvider.search(whereClauseBuilder.buildQuery(), authContext.getLoggedInUser(), collector);
   		return collector.getIssueObject();
   	}

   	/**
   	 * 
   	 * @param curDate
   	 * @return
   	 * @throws ParseException
   	 */
   	private String formatDate(Date curDate) throws ParseException {
   		

   		DateFormat formatter = new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy");
   		Date date =  formatter.parse(curDate.toString());
   		Calendar cal = Calendar.getInstance();
   		cal.setTime(date);
   		String formatedDate = cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-"
   				+ cal.get(Calendar.DATE);
   		return formatedDate;
   		
   	}

    /**
     *
     * @param cycleIds
     * @param versionId
     * @param issueCreatedDate
     * @param curDate
     * @param issueIds
     * @param dateExecutionStatusMap
     * @return
     * @throws ParseException Prepares Schedule Summary.
     * Checks the Index First , if
     * does not return anything, makes a backend call
     */
    private Map<String, Map<String, Integer>> searchExecutionStatusForIssueIdsPerDateInAudit(String cycleIds,
                                                                                             String versionId, Date issueCreatedDate, Date curDate, HashSet<String> issueIds,
                                                                                             Map<String, Map<String, Integer>> dateExecutionStatusMap) throws ParseException {

        String formattedDate = formatDate(curDate);
        Map<String, Map<String, Integer>> executionStatusCountPerDateMap = new HashMap<>();
        String[] cycleIdArr = null;
        Calendar chartDate = Calendar.getInstance();
        chartDate.setTime(curDate);
        chartDate.add(Calendar.DATE,1);
        String zqlQuery = "creationDate >=" + formattedDate + " AND creationDate <" + formatDate(chartDate.getTime()) + " AND ISSUE IN (" + StringUtils.join(issueIds, ",") + ")";

        if (versionId != null && StringUtils.isNotEmpty(versionId)) {
            zqlQuery += " AND fixVersion =" + Long.valueOf(versionId);
        }

        if (StringUtils.isNotBlank(cycleIds)) {
            cycleIdArr = StringUtils.split(cycleIds, "|");
            zqlQuery += " AND cycleId IN (" + StringUtils.join(cycleIdArr, ",") + ")";
        }
        zqlQuery+= " ORDER BY creationDate desc";
        ScheduleSearchResourceHelper searchResourceHelper = new ScheduleSearchResourceHelper(
            authContext.getLoggedInUser(), searchService, zephyrClauseHandlerFactory, issueManager, cycleManager,
            versionManager, authContext, scheduleIndexManager,zephyrCustomFieldManager);
        ParseResult parseResult = searchResourceHelper.getNewSearchQuery(zqlQuery);
        getExecutionsFromIndex(parseResult,executionStatusCountPerDateMap,0,cycleIds,issueIds,issueCreatedDate, curDate,dateExecutionStatusMap);
   		return executionStatusCountPerDateMap;
   	}

    /**
     *
     * @param cycleId
     * @param issueIds
     * @param issueCreatedDate
     * @param curDate
     * @param dateExecutionStatusMap
     * @return
     * @throws ParseException
     */
   	private Map<String, Map<String, Integer>> prepareExecutionStatusCountForPastExecutions(String cycleId,
                       HashSet<String> issueIds, Date issueCreatedDate, Date curDate,
                       Map<String, Map<String, Integer>> dateExecutionStatusMap) throws ParseException {
        Map<Integer, String> statusesMap = new HashMap<>();
        Map<String, Integer> statusMap = new HashMap<>();
   		String creationDate = formatDate(curDate);
   		preparePastExecutionStatusResponse(statusesMap, dateExecutionStatusMap, creationDate);
   		String[] cycleIdArr = StringUtils.split(cycleId,"|");
        List<ChangeZJEItem> executionFromAudit = getExecutionStatusFromAudit(cycleIdArr,issueIds,curDate.getTime());
        List<String> foundIssueIds = new ArrayList<>();
        foundIssueIds.addAll(issueIds);
        if(executionFromAudit != null && executionFromAudit.size() > 0) {
            executionFromAudit.removeAll(Collections.singleton(null));
            Map<Integer,Date> lastUpdatedStatus = new LinkedHashMap<>();
            for (ChangeZJEItem changeZJEItem : executionFromAudit) {
                final Integer execStatus = !StringUtils.equalsIgnoreCase(changeZJEItem.getOldValue(), changeZJEItem.getNewValue()) ? Integer.valueOf(changeZJEItem.getNewValue()) : null;
                String executionStatus = statusesMap.get(execStatus);
                if (StringUtils.isNotBlank(executionStatus)) {
                    if(!lastUpdatedStatus.containsKey(changeZJEItem.getChangeZJEGroup().getScheduleId())) {
                        statusMap = dateExecutionStatusMap.get(creationDate);
                        statusMap.put(executionStatus, statusMap.get(executionStatus) + 1);
                        statusMap.put("executed", statusMap.get("executed") + 1);
                        foundIssueIds.remove(String.valueOf(changeZJEItem.getChangeZJEGroup().getIssueId()));
                        lastUpdatedStatus.put(changeZJEItem.getChangeZJEGroup().getScheduleId(),new Date(changeZJEItem.getChangeZJEGroup().getCreated().longValue()));
                    } else {
                        Date previousSetDate = lastUpdatedStatus.get(changeZJEItem.getChangeZJEGroup().getScheduleId());
                        Date lastDate = new Date(changeZJEItem.getChangeZJEGroup().getCreated().longValue());
                        if(lastDate.after(previousSetDate)) {
                            statusMap = dateExecutionStatusMap.get(creationDate);
                            statusMap.put(executionStatus, statusMap.get(executionStatus) + 1);
                            lastUpdatedStatus.put(changeZJEItem.getChangeZJEGroup().getScheduleId(),lastDate);
                        }
                    }
                }
            }
            statusMap = dateExecutionStatusMap.get(creationDate);
        }

        if (issueCreatedDate.equals(curDate) ) {
            preparePastExecutionStatusResponse(statusesMap, dateExecutionStatusMap, creationDate);
            statusMap = dateExecutionStatusMap.get(creationDate);
            statusMap.put("unscheduled", foundIssueIds.size());
        }
        int executedCount = statusMap.get("executed") != null ? statusMap.get("executed") : 0;
        int unscheduledCount = statusMap.get("unscheduled") != null ? statusMap.get("unscheduled") : 0;
        statusMap.put("totalTests", unscheduledCount + executedCount);
        return dateExecutionStatusMap;
   	}


    /**
     * Retrieve Executions from Audit
     * @param parseResult
     * @param executionStatusCountPerDateMap
     * @param offset
     * @param cycleIds
     * @param issueIds
     * @param issueCreatedDate
     * @param curDate
     * @param dateExecutionStatusMap
     */
    private void getExecutionsFromIndex(ParseResult parseResult, Map<String, Map<String, Integer>> executionStatusCountPerDateMap,
                                        int offset, String cycleIds, HashSet<String> issueIds, Date issueCreatedDate, Date curDate, Map<String, Map<String, Integer>> dateExecutionStatusMap) {

        try {
            SearchResult searchResults = searchService.search(authContext.getLoggedInUser(), parseResult.getQuery(), offset,true,
                    ApplicationConstants.MAX_LIMIT, false);
            executionStatusCountPerDateMap = prepareExecutionStatusCountForPastExecutions(cycleIds, issueIds, issueCreatedDate, curDate, dateExecutionStatusMap);
            if (searchResults.getTotal() > ApplicationConstants.MAX_LIMIT && searchResults.getTotal() > offset) {
                offset = searchResults.getDocuments().size() + 1;
                getExecutionsFromIndex(parseResult, executionStatusCountPerDateMap, offset, cycleIds, issueIds, issueCreatedDate, curDate, dateExecutionStatusMap);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error retrieving data from ExecutionsStatus Per Date breakdown from Zephyr Indexes:", e);
        }
    }


    /**
   	 * 
   	 * @param statusesMap
   	 * @param dateExecutionStatusMap
   	 * @param creationDate
   	 * @throws ParseException 
   	 */
   	private void preparePastExecutionStatusResponse(Map<Integer, String> statusesMap,
   			Map<String, Map<String, Integer>> dateExecutionStatusMap, String creationDate) throws ParseException {

   		for (ExecutionStatus execStatus : JiraUtil.getExecutionStatuses().values()) {
   			statusesMap.putIfAbsent(execStatus.getId(), execStatus.getName());
   		}
   		Map<String, Integer> executionStatusMap = populateExecutionStatusMap(statusesMap);

   		executionStatusMap.put("executed", new Integer(0));
   		executionStatusMap.put("unscheduled", new Integer(0));
   		executionStatusMap.put("totalTests", new Integer(0));
   		if (StringUtils.isNotBlank(creationDate)) {
   			dateExecutionStatusMap.putIfAbsent(creationDate, executionStatusMap);
   		}
	}

    private Long filterListWithExistence(long[] allScheduleDocumentsArray, Long scheduleId) {
        try {
            int i = Arrays.binarySearch(allScheduleDocumentsArray, scheduleId);
            if(i < 0) {
                return scheduleId;
            }
        } catch(Exception e) {
            log.error("Error filtering Executions",e);
        }
        return null;
    }
    /**
     * retrieve data from Audit history
     * @param cycleIds
     * @param issueIds
     * @param curDateInLong
     * @return
     */
    private List<ChangeZJEItem> getExecutionStatusFromAudit(String[] cycleIds,
                                                            HashSet<String> issueIds, Long curDateInLong) {
        int limit = ApplicationConstants.MAX_LIMIT;
        List<ChangeZJEItem> response = new ArrayList<>();
        Map<String, Object> filterMap = new LinkedHashMap<>();
        if (StringUtils.isNotBlank(ApplicationConstants.ENTITY_TYPE.EXECUTION.name())) {
            filterMap.put("ZEPHYR_ENTITY_TYPE", ApplicationConstants.ENTITY_TYPE.EXECUTION.name());
        }
        List<String> events = new ArrayList<>();
        events.add(EventType.EXECUTION_UPDATED.name());
        events.add(EventType.EXECUTION_ADDED.name());
        filterMap.put("ZEPHYR_ENTITY_EVENT", events);
        if (null != cycleIds && cycleIds.length > 0) {
            filterMap.put("CYCLE", cycleIds);
        }
        if (issueIds != null && issueIds.size() > 0) {
            filterMap.put("ISSUE_ID", issueIds);
        }
        if (curDateInLong != null) {
            filterMap.put("CREATED", curDateInLong);
        }
        filterMap.put("ZEPHYR_FIELD", "STATUS");
        filterMap.put("GADGET", true);
        List<ChangeZJEItem> items = auditManager.getZephyrChangeLogs(filterMap, 0, limit);
        if (items != null && items.size() > 0) {
            response.addAll(items);
        }
        return response;
    }

    /**
     *
     * @param versionId
     * @param projectId
     * @return
     */
    private ZQLSearchResultBean getSchedulesByProjectAndVersion(Long versionId, Long projectId) {

		String zqlQuery = "project =" + projectId + " AND fixVersion=" + versionId;

		ZQLSearchResultBean zqlSearchResultBean = new ZQLSearchResultBean();
		ScheduleSearchResourceHelper searchResourceHelper = new ScheduleSearchResourceHelper(
				authContext.getLoggedInUser(), searchService, exportService, issueManager, cycleManager, versionManager,
				testStepManager, stepResultManager, folderManager,zephyrCustomFieldManager);
		ParseResult parseResult = searchResourceHelper.getNewSearchQuery(zqlQuery);
		try {
			SearchResult searchResults = searchService.searchMax(authContext.getLoggedInUser(), parseResult.getQuery(),
					false, true);
			zqlSearchResultBean = searchResourceHelper.convertLuceneDocumentToJson(searchResults, 0, Integer.MAX_VALUE,
					"executionStatus");
		} catch (Exception e) {
			log.error("Error retrieving data from ExecutionsStatus Assignee breakdown from Zephyr Indexes:", e);
		}
		return zqlSearchResultBean;

	}

    /**
     *
     * @param folders
     * @param projectId
     * @param versionId
     * @param cycleIdArr
     * @return
     */
	private Map<String,Map<String,ExecutionTimeTrackingStatusBean>> prepareExecutionTimeTrackingPerCycleMap(String folders, Long projectId, Long versionId, String[] cycleIdArr) {

        Map<String,Map<String,ExecutionTimeTrackingStatusBean>> executionWorkflowTimePerCycleMap = new HashMap<>();

		if(StringUtils.isNotBlank(folders)) {
            List<Long> folderList = getFolderList(folders);
            for(String cycleId  : cycleIdArr) {
                Long parsedCycleId = Long.valueOf(cycleId);
                Cycle cycle = cycleManager.getCycle(parsedCycleId);

                if(Objects.nonNull(cycle)) {
                    folderList.stream().forEach(folderId -> {
                        FolderCycleMapping folderCycleMapping = folderManager.getFolderCycleMapping(folderId,parsedCycleId,versionId,projectId);
                        if(Objects.nonNull(folderCycleMapping)) {
                            Map<String,Long> executionEstimationData = scheduleManager.getExecutionEstimationData(projectId,versionId,parsedCycleId,folderId);

                            String cycleNameIdKey = cycle.getName() + "-"+cycle.getID();

                            if(executionWorkflowTimePerCycleMap.containsKey(cycleNameIdKey)) {
                                Map<String, ExecutionTimeTrackingStatusBean> timeTrackingStatusMap = executionWorkflowTimePerCycleMap.get(cycleNameIdKey);
                                String folderNameKey = "F-"+ folderCycleMapping.getFolder().getName();

                                if(timeTrackingStatusMap.containsKey(folderNameKey)) {
                                    ExecutionTimeTrackingStatusBean timeTrackingStatusBean = timeTrackingStatusMap.get(folderNameKey);
                                    timeTrackingStatusMap.put(folderNameKey,getUpdatedTimeTrackingStatusBean(timeTrackingStatusBean,executionEstimationData.get(FOLDER_LEVEL_ESTIMATED_TIME),executionEstimationData.get(FOLDER_LEVEL_LOGGED_TIME)));
                                }else {
                                    ExecutionTimeTrackingStatusBean timeTrackingStatusBean = new ExecutionTimeTrackingStatusBean();
                                    timeTrackingStatusMap.put(folderNameKey, getUpdatedTimeTrackingStatusBean(timeTrackingStatusBean, executionEstimationData.get(FOLDER_LEVEL_ESTIMATED_TIME),executionEstimationData.get(FOLDER_LEVEL_LOGGED_TIME)));
                                }

                            }else {
                                // new folder level data.
                                Map<String, ExecutionTimeTrackingStatusBean> timeTrackingStatusMap = new HashMap<>();
                                String folderNameKey = "F-"+ folderCycleMapping.getFolder().getName();
                                ExecutionTimeTrackingStatusBean timeTrackingStatusBean = new ExecutionTimeTrackingStatusBean();
                                timeTrackingStatusMap.put(folderNameKey,getUpdatedTimeTrackingStatusBean(timeTrackingStatusBean,executionEstimationData.get(FOLDER_LEVEL_ESTIMATED_TIME),executionEstimationData.get(FOLDER_LEVEL_LOGGED_TIME)));
                                executionWorkflowTimePerCycleMap.put(cycleNameIdKey,timeTrackingStatusMap);
                            }

                        }
                    });
                }
            }

        }else {
		    for(String cycleId  : cycleIdArr) {
		        Long parsedCycleId = Long.valueOf(cycleId);
		        Cycle cycle = cycleManager.getCycle(parsedCycleId);

		        if(Objects.nonNull(cycle)) {
                    Map<String,Long> executionEstimationData = scheduleManager.getExecutionEstimationData(projectId,versionId,parsedCycleId,null);

                    String cycleNameIdKey = cycle.getName() + "-"+cycle.getID();

                    if(executionWorkflowTimePerCycleMap.containsKey(cycleNameIdKey)) {
                        Map<String, ExecutionTimeTrackingStatusBean> timeTrackingStatusMap = executionWorkflowTimePerCycleMap.get(cycleNameIdKey);
                        if(MapUtils.isNotEmpty(executionEstimationData)) {
                            // its an existing cycle data
                            String cycleNameKey = "C-"+ cycle.getName();
                            if(timeTrackingStatusMap.containsKey(cycleNameKey)) {
                                ExecutionTimeTrackingStatusBean timeTrackingStatusBean = timeTrackingStatusMap.get(cycleNameKey);
                                timeTrackingStatusMap.put(cycleNameKey,getUpdatedTimeTrackingStatusBean(timeTrackingStatusBean,executionEstimationData.get(FOLDER_LEVEL_ESTIMATED_TIME),executionEstimationData.get(FOLDER_LEVEL_LOGGED_TIME)));
                            }else {
                                ExecutionTimeTrackingStatusBean timeTrackingStatusBean = new ExecutionTimeTrackingStatusBean();
                                timeTrackingStatusMap.put(cycleNameKey,getUpdatedTimeTrackingStatusBean(timeTrackingStatusBean,executionEstimationData.get(FOLDER_LEVEL_ESTIMATED_TIME),executionEstimationData.get(FOLDER_LEVEL_LOGGED_TIME)));
                            }

                        }
                    }else {
                        // new cycle level data.
                        Map<String, ExecutionTimeTrackingStatusBean> timeTrackingStatusMap = new HashMap<>();
                        String cycleNameKey = "C-"+ cycle.getName();
                        ExecutionTimeTrackingStatusBean timeTrackingStatusBean = new ExecutionTimeTrackingStatusBean();
                        timeTrackingStatusMap.put(cycleNameKey,getUpdatedTimeTrackingStatusBean(timeTrackingStatusBean,executionEstimationData.get(FOLDER_LEVEL_ESTIMATED_TIME),executionEstimationData.get(FOLDER_LEVEL_LOGGED_TIME)));
                        executionWorkflowTimePerCycleMap.put(cycleNameIdKey,timeTrackingStatusMap);
                    }
                }
            }
        }

		return executionWorkflowTimePerCycleMap;
	}

    /**
     *
     * @param timeTrackingStatusBean
     * @param estimatedTime
     * @param loggedTime
     * @return
     */
    private ExecutionTimeTrackingStatusBean getUpdatedTimeTrackingStatusBean(ExecutionTimeTrackingStatusBean timeTrackingStatusBean, Long estimatedTime, Long loggedTime) {

        if (Objects.nonNull(estimatedTime)) {
            timeTrackingStatusBean.setTotalExecutionEstimatedTime(timeTrackingStatusBean.getTotalExecutionEstimatedTime() +
                    estimatedTime);
        }

        if (Objects.nonNull(loggedTime)) {
            timeTrackingStatusBean.setTotalExecutionLoggedTime(timeTrackingStatusBean.getTotalExecutionLoggedTime() +
                    loggedTime);
        }
        timeTrackingStatusBean.setTotalExecutionEstimatedTimeDurationStr(DateUtils.getDurationStringSeconds(timeTrackingStatusBean.getTotalExecutionEstimatedTime(), Long.MAX_VALUE, Long.MAX_VALUE));
        timeTrackingStatusBean.setTotalExecutionLoggedTimeDurationStr(DateUtils.getDurationStringSeconds(timeTrackingStatusBean.getTotalExecutionLoggedTime(), Long.MAX_VALUE, Long.MAX_VALUE));
        return timeTrackingStatusBean;
    }


    /**
     *
     * @param zqlSearchResultBean
     * @return
     */
    private Map<String, Map<String, ExecutionTimeTrackingStatusBean>> prepareExecutionTimeTrackingPerCycleMapFromZqlSchedule(ZQLSearchResultBean zqlSearchResultBean) {
        Map<String,Map<String,ExecutionTimeTrackingStatusBean>> executionWorkflowTimePerCycleMap = new HashMap<>();
        if(CollectionUtils.isNotEmpty(zqlSearchResultBean.getExecutions())) {

            zqlSearchResultBean.getExecutions().forEach(zqlScheduleBean -> {
                if(!zqlScheduleBean.getCycleId().equals(ApplicationConstants.AD_HOC_CYCLE_ID)) {

                    String cycleNameIdKey = zqlScheduleBean.getCycleName() + "-"+zqlScheduleBean.getCycleId();

                    if(executionWorkflowTimePerCycleMap.containsKey(cycleNameIdKey)) {
                        Map<String, ExecutionTimeTrackingStatusBean> timeTrackingStatusMap = executionWorkflowTimePerCycleMap.get(cycleNameIdKey);

                        // capture aggregated cycle data
                        String cycleNameKey = "C-" + zqlScheduleBean.getCycleName();
                        if (timeTrackingStatusMap.containsKey(cycleNameKey)) {
                            ExecutionTimeTrackingStatusBean timeTrackingStatusBean = timeTrackingStatusMap.get(cycleNameKey);
                            timeTrackingStatusMap.put(cycleNameKey, getUpdatedTimeTrackingStatusBeanForZqlSchedule(timeTrackingStatusBean, zqlScheduleBean));
                        } else {
                            ExecutionTimeTrackingStatusBean timeTrackingStatusBean = new ExecutionTimeTrackingStatusBean();
                            timeTrackingStatusMap.put(cycleNameKey, getUpdatedTimeTrackingStatusBeanForZqlSchedule(timeTrackingStatusBean, zqlScheduleBean));
                        }

                    }else {
                        // new cycle level data.
                        // capture aggregated cycle data
                        Map<String, ExecutionTimeTrackingStatusBean> timeTrackingStatusMap = new HashMap<>();

                        String cycleNameKey = "C-" + zqlScheduleBean.getCycleName();
                        ExecutionTimeTrackingStatusBean timeTrackingStatusBean = new ExecutionTimeTrackingStatusBean();
                        timeTrackingStatusMap.put(cycleNameKey, getUpdatedTimeTrackingStatusBeanForZqlSchedule(timeTrackingStatusBean, zqlScheduleBean));

                        executionWorkflowTimePerCycleMap.put(cycleNameIdKey, timeTrackingStatusMap);
                    }
                }
            });

        }

        return executionWorkflowTimePerCycleMap;
    }

    /**
     *
     * @param timeTrackingStatusBean
     * @param zqlScheduleBean
     * @return
     */
    private ExecutionTimeTrackingStatusBean getUpdatedTimeTrackingStatusBeanForZqlSchedule(ExecutionTimeTrackingStatusBean timeTrackingStatusBean, ZQLScheduleBean zqlScheduleBean) {
        if (Objects.nonNull(zqlScheduleBean.getEstimatedTime())) {
            timeTrackingStatusBean.setTotalExecutionEstimatedTime(timeTrackingStatusBean.getTotalExecutionEstimatedTime() +
                    zqlScheduleBean.getEstimatedTime());
        }

        if (Objects.nonNull(zqlScheduleBean.getLoggedTime())) {
            timeTrackingStatusBean.setTotalExecutionLoggedTime(timeTrackingStatusBean.getTotalExecutionLoggedTime() +
                    zqlScheduleBean.getLoggedTime());
        }

        timeTrackingStatusBean.setTotalExecutionEstimatedTimeDurationStr(DateUtils.getDurationStringSeconds(timeTrackingStatusBean.getTotalExecutionEstimatedTime(),
                Long.MAX_VALUE, Long.MAX_VALUE));
        timeTrackingStatusBean.setTotalExecutionLoggedTimeDurationStr(DateUtils.getDurationStringSeconds(timeTrackingStatusBean.getTotalExecutionLoggedTime(),
                Long.MAX_VALUE, Long.MAX_VALUE));
        return timeTrackingStatusBean;
    }


    /**
     *
     * @param fromCycleId
     * @param projectId
     * @param versionId
     * @return
     * @throws Exception
     */
    private boolean validateLuceneIndex(String fromCycleId, String projectId, String versionId) throws Exception {
        StringBuffer luceneQueryString = new StringBuffer("+(CYCLE_ID:\"").append(fromCycleId).append("\")");
        luceneQueryString.append(" +(PROJECT_ID:\"").append(projectId).append("\")");
        luceneQueryString.append(" +(VERSION_ID:\"").append(versionId).append("\")");
        luceneQueryString.append(" +(FOLDER_ID:\"-1\"").append(")");

        IssueIdsCollector issueIdsCollector = new IssueIdsCollector("ISSUE_ID", scheduleIndexManager.getRawEntitySearcher());
        scheduleIndexManager.search(new String[]{"ISSUE_ID"}, luceneQueryString.toString(), issueIdsCollector);
        Collection<Long> issueIds = (Set<Long>) issueIdsCollector.getValue();
        if(null != issueIds && issueIds.size() > 0) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    /**
     *
     * @param folders
     * @return
     */
    private List<Long> getFolderList(String folders) {

        String[] folderArr = StringUtils.split(folders, "|");
        List<Long> folderList = Lists.newArrayList();
        if (Objects.nonNull(folderArr) && folderArr.length > 0) {
            folderList = Arrays.stream(folderArr).map(Long::parseLong).collect(Collectors.toList());
        }
        return folderList;
    }

    /**
     *
     * @param assignedTo
     * @return
     */
    private String getAssignedUserDisplayName(String assignedTo) {

        if (StringUtils.isNotBlank(assignedTo)) {
            User assignedToUser = UserCompatibilityHelper.getUserForKey(assignedTo);
            return assignedToUser != null ? assignedToUser.getDisplayName() : assignedTo;

        }
        return StringUtils.EMPTY;
    }
    
}
