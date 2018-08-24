package com.thed.zephyr.je.rest;

import com.atlassian.beehive.compat.ClusterLock;
import com.atlassian.beehive.compat.ClusterLockService;
import com.atlassian.beehive.compat.ClusterLockServiceFactory;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.bc.issue.link.IssueLinkService;
import com.atlassian.jira.bc.issue.search.SearchService.ParseResult;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.RendererManager;
import com.atlassian.jira.issue.fields.rest.json.beans.JiraBaseUrls;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.RemoteIssueLinkManager;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.task.context.Contexts;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.usercompatibility.UserCompatibilityHelper;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.collect.CollectionEnclosedIterable;
import com.atlassian.jira.util.collect.EnclosedIterable;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.query.Query;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.websudo.WebSudoRequired;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.*;
import com.sun.jersey.spi.container.ResourceFilters;
import com.thed.zephyr.je.event.EntityType;
import com.thed.zephyr.je.event.EventType;
import com.thed.zephyr.je.event.ScheduleModifyEvent;
import com.thed.zephyr.je.event.SingleScheduleEvent;
import com.thed.zephyr.je.helper.ScheduleResourceHelper;
import com.thed.zephyr.je.helper.ScheduleSearchResourceHelper;
import com.thed.zephyr.je.index.ScheduleIndexManager;
import com.thed.zephyr.je.index.cluster.NodeStateManager;
import com.thed.zephyr.je.model.*;
import com.thed.zephyr.je.permissions.model.PermissionType;
import com.thed.zephyr.je.permissions.service.ZephyrPermissionManager;
import com.thed.zephyr.je.rest.delegate.ExecutionWorkflowResourceDelegate;
import com.thed.zephyr.je.rest.delegate.ScheduleResourceDelegate;
import com.thed.zephyr.je.rest.exception.RESTException;
import com.thed.zephyr.je.rest.filter.ZFJApiFilter;
import com.thed.zephyr.je.service.*;
import com.thed.zephyr.je.vo.ZQLScheduleBean;
import com.thed.zephyr.je.vo.ZQLSearchResultBean;
import com.thed.zephyr.je.zql.core.SearchService;
import com.thed.zephyr.je.zql.core.ZephyrClauseHandlerFactory;
import com.thed.zephyr.util.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Api(value = "Execution Resource API(s)", description = "Following section describes rest resources (API's) pertaining to ExecutionResource")
@Path("execution")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@ResourceFilters(ZFJApiFilter.class)
public class ScheduleResource {

    private static final String SCHEDULE_ENTITY = "Execution";
    private static final String ID = " ID : %s";
    private static final String ERROR_LOG_MESSAGE = "[Error] [Error code: %s %s Error Message : %s]";
    protected final Logger log = Logger.getLogger(ScheduleResource.class);

    private final JiraAuthenticationContext authContext;
    private final ScheduleManager scheduleManager;
    private final DateTimeFormatterFactory dateTimeFormatterFactory;
    private final IssueManager issueManager;
    private final CycleManager cycleManager;
    private final SearchProvider searchProvider;
    private final VersionManager versionManager;
    private final JiraBaseUrls jiraBaseURLs;
    private final RemoteIssueLinkManager rilManager;
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
    private final IssueLinkService issueLinkService;
    private final ClusterLockService clusterLockService;
	private final ZephyrClauseHandlerFactory zephyrClauseHandlerFactory;
	private final ZFJCacheService zfjCacheService;
    private final ZephyrSprintService sprintService;
    private final ScheduleResourceDelegate scheduleResourceDelegate;
    private final ZephyrPermissionManager zephyrPermissionManager;
    private final JobProgressService jobProgressService;
    private final FolderManager folderManager;
    private final ZephyrCustomFieldManager zephyrCustomFieldManager;
    private final ExecutionWorkflowResourceDelegate executionWorkflowResourceDelegate;
    private final NodeStateManager nodeStateManager;

    public ScheduleResource(JiraAuthenticationContext authContext,
            RendererManager rendererManager,
            ScheduleManager scheduleManager,
            IssueManager issueManager,
            DateTimeFormatterFactory dateTimeFormatterFactory,
            CycleManager cycleManager,
            SearchProvider searchProvider,
            VersionManager versionManager,
            JiraBaseUrls jiraBaseURLs,
            RemoteIssueLinkManager rilManager,
            ScheduleIndexManager scheduleIndexManager,
            EventPublisher eventPublisher,
            ProjectManager projectManager,
            PermissionManager permissionManager, ExportService exportService, SearchService searchService,
            TeststepManager testStepManager, StepResultManager stepResultManager, ZAPIValidationService zapiValidationService,
            IssueLinkService issueLinkService, ClusterLockServiceFactory clusterLockServiceFactory, 
            ZephyrClauseHandlerFactory zephyrClauseHandlerFactory,
            ZFJCacheService zfjCacheService, ZephyrSprintService sprintService,
            ScheduleResourceDelegate scheduleResourceDelegate,
            ZephyrPermissionManager zephyrPermissionManager,
            JobProgressService jobProgressService, FolderManager folderManager, ZephyrCustomFieldManager zephyrCustomFieldManager,
                            ExecutionWorkflowResourceDelegate executionWorkflowResourceDelegate, NodeStateManager nodeStateManager) {
        this.authContext = authContext;
        this.rendererManager = rendererManager;
        this.scheduleManager = scheduleManager;
        this.dateTimeFormatterFactory = dateTimeFormatterFactory;
        this.issueManager = issueManager;
        this.cycleManager = cycleManager;
        this.searchProvider = searchProvider;
        this.versionManager = versionManager;
        this.jiraBaseURLs = jiraBaseURLs;
        this.rilManager = rilManager;
        this.scheduleIndexManager = scheduleIndexManager;
        this.eventPublisher = eventPublisher;
        this.projectManager = projectManager;
        this.permissionManager = permissionManager;
        this.exportService = exportService;
        this.searchService = searchService;
        this.testStepManager = testStepManager;
        this.stepResultManager = stepResultManager;
        this.zapiValidationService = zapiValidationService;
        this.issueLinkService = issueLinkService;
        this.clusterLockService = clusterLockServiceFactory.getClusterLockService();
        this.zephyrClauseHandlerFactory=zephyrClauseHandlerFactory;
        this.zfjCacheService=zfjCacheService;
        this.sprintService=sprintService;
        this.scheduleResourceDelegate=scheduleResourceDelegate;
        this.zephyrPermissionManager=zephyrPermissionManager;
        this.jobProgressService=jobProgressService;
        this.folderManager = folderManager;
        this.zephyrCustomFieldManager = zephyrCustomFieldManager;
        this.executionWorkflowResourceDelegate = executionWorkflowResourceDelegate;
        this.nodeStateManager=nodeStateManager;
    }

    /**
     * Gets all schedules available for given Issue Id
     *
     * @param scheduleId
     * @param expandos
     * @return Response
     */
    @ApiOperation(value = "Get Execution Information", notes = "Gets all executions available for given execution Id")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"execution\":{\"id\":10203,\"orderId\":9821,\"executionStatus\":\"-1\",\"comment\":\"\",\"htmlComment\":\"\",\"cycleId\":-1,\"cycleName\":\"Ad hoc\",\"versionId\":10401,\"versionName\":\"szdfxgcvhbjjjj\",\"projectId\":10100,\"createdBy\":\"vm_admin\",\"modifiedBy\":\"vm_admin\",\"issueId\":11725,\"issueKey\":\"SONY-1386\",\"summary\":\"SONY Project\",\"label\":\"\",\"component\":\"\",\"projectKey\":\"SONY\",\"executionDefectCount\":0,\"stepDefectCount\":0,\"totalDefectCount\":0}}")})
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @AnonymousAllowed
    public Response getExecution(@PathParam("id") Integer scheduleId, @QueryParam("expand") final String expandos) {
        Schedule schedule = scheduleManager.getSchedule(scheduleId);
        Map<String, String> errorMap = new HashMap<String, String>();
        if (schedule == null) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("schedule.execute.update.stepresult.invalid.id", SCHEDULE_ENTITY + String.format(ID,scheduleId))));
            throw new RESTException(Response.Status.BAD_REQUEST, authContext.getI18nHelper().getText("schedule.execute.update.stepresult.invalid.id", SCHEDULE_ENTITY + String.format(ID,scheduleId)));
        }
    	Project project = projectManager.getProjectObj(schedule.getProjectId());
        //Response will be an error message if user does not have browse permission
        Response response = hasBrowseProjectPermission(project);
        if(response != null) {
            return response;
        }

        if (project == null) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "projectId ", "");
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "Invalid Project", errorMessage, errorMessage);
        }  
    	JiraUtil.setProjectThreadLocal(project);
    	return scheduleResourceDelegate.getExecution(schedule, expandos);
    }

    /**
     * Gets all schedules available for given Issue Id
     */
    @ApiOperation(value = "Get List of Execution", notes = "Get all execution available for given issue id")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"status\":{\"1\":{\"id\":1,\"color\":\"#75B000\",\"description\":\"Test was executed and passed successfully.(edited)\",\"name\":\"PASSED\"},\"2\":{\"id\":2,\"color\":\"#CC3300\",\"description\":\"Test was executed and failed.\",\"name\":\"FAIL\"},\"3\":{\"id\":3,\"color\":\"#F2B000\",\"description\":\"Test execution is a work-in-progress.\",\"name\":\"WIP\"},\"4\":{\"id\":4,\"color\":\"#6693B0\",\"description\":\"The test execution of this test was blocked for some reason.\",\"name\":\"BLOCKED\"},\"5\":{\"id\":5,\"color\":\"#ff33ff\",\"description\":\"It will cancel the test.\",\"name\":\"CANCEL\"},\"-1\":{\"id\":-1,\"color\":\"#A0A0A0\",\"description\":\"The test has not yet been executed.\",\"name\":\"UNEXECUTED\"}},\"executions\":[{\"id\":47,\"orderId\":19,\"executionStatus\":\"2\",\"executedOn\":\"Today 1:05 PM\",\"executedBy\":\"vm_admin\",\"executedByDisplay\":\"vm_admin\",\"comment\":\"\",\"htmlComment\":\"\",\"cycleId\":-1,\"cycleName\":\"Ad hoc\",\"versionId\":-1,\"versionName\":\"Unscheduled\",\"projectId\":10100,\"createdBy\":\"vm_admin\",\"modifiedBy\":\"vm_admin\",\"issueId\":10400,\"issueKey\":\"HTC-1\",\"summary\":\"test1\",\"label\":\"\",\"component\":\"\",\"projectKey\":\"HTC\",\"executionDefectCount\":0,\"stepDefectCount\":0,\"totalDefectCount\":0}],\"currentlySelectedExecutionId\":\"\",\"recordsCount\":1}")})
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @AnonymousAllowed
    public Response getExecutions(@Context HttpServletRequest req, @QueryParam("issueId") Integer issueId, @QueryParam("projectId") Long projectId,
                                  @QueryParam("versionId") Long versionId, @QueryParam("cycleId") Integer cycleId, @QueryParam("offset") Integer offset,
                                  @QueryParam("action") String action, @QueryParam("sorter") String sortQuery, @QueryParam("expand") final String expandos,
                                  @QueryParam("limit") final Integer limit, @DefaultValue("-1") @QueryParam("folderId") final Long folderId) {
        ScheduleResourceHelper scheduleHelper = new ScheduleResourceHelper(issueManager);
        // If no limit, return
        if (limit != null && limit == 0) {
            scheduleHelper.setCycleSummaryDetail(req, cycleId, versionId, folderId, action, offset, sortQuery);
            return Response.ok().cacheControl(ZephyrCacheControl.never()).build();
        }
        Project project = null;
        if (issueId != null && cycleId != null) {
           Issue issue = issueManager.getIssueObject(issueId != null ? issueId.longValue() : null);
           if(issue == null) {
        	   Cycle cycle = cycleManager.getCycle(cycleId != null? cycleId.longValue() : null);
        	   if(cycle != null) {
                   project = projectManager.getProjectObj(cycle.getProjectId());
               }
           } else {
               project = issue.getProjectObject();
           }
        } else if (issueId != null) {
            Issue issue = issueManager.getIssueObject(issueId != null ? issueId.longValue() : null);
            project = issue != null ? issue.getProjectObject() : null;
        } else if (cycleId != null) {
            if(cycleId != ApplicationConstants.AD_HOC_CYCLE_ID) {
                Cycle cycle = cycleManager.getCycle(cycleId != null? cycleId.longValue() : null);
                if (cycle == null) {
                    String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "cycleId ", String.valueOf(cycleId));
                    log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,errorMessage));
                    return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "Invalid Cycle", errorMessage, errorMessage);
                }
                if (cycle.getProjectId() == null) {
                    String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "projectId ", String.valueOf(projectId));
                    log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorMessage));
                    return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "Project associated with this cycle is null", errorMessage, errorMessage);
                }
                projectId = cycle.getProjectId();
            }
            if(cycleId == ApplicationConstants.AD_HOC_CYCLE_ID && projectId == null) {
                String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.update.ID.required", "projectId", "");
                log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorMessage));
                return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "ProjectId is a required parameter for Adhoc Cycle", errorMessage, errorMessage);
            }
     	   project = projectManager.getProjectObj(projectId);
        }
        if(project != null) {
            //Response will be an error message if user does not have browse permission
            Response response = hasBrowseProjectPermission(project);
            if(response != null) {
                return response;
            }
        	JiraUtil.setProjectThreadLocal(project);
        }
        JSONObject jsonObjectResponse = new JSONObject();
        if(folderId != null && !folderId.equals(-1L)) {
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
        return scheduleResourceDelegate.getExecutions(req, issueId, versionId, cycleId, offset, action, sortQuery, expandos, limit, folderId);
    }

    @ApiOperation(value = "Get Defect List", notes = "Get all defect available for given Execution Id")
    @ApiImplicitParams({ @ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"10737\":{\"SONY-2019\":{\"key\":\"SONY-2019\",\"resolution\":\"\",\"status\":\"To Do\",\"statusId\":\"10000\",\"summary\":\"xcv\"}}}")})
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}/defects")
    public Response getExecutionDefects(@PathParam("id") Integer executionId) {
        if (executionId == null || executionId <= 0) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,authContext.getI18nHelper().getText("schedule.execute.update.stepresult.invalid.id", SCHEDULE_ENTITY +  String.format(ID,executionId))));
            throw new RESTException(Response.Status.BAD_REQUEST, authContext.getI18nHelper().getText("schedule.execute.update.stepresult.invalid.id", SCHEDULE_ENTITY +  String.format(ID,executionId)));
        }
        //Add Schedule validation
        Schedule schedule = scheduleManager.getSchedule(executionId);
        if (schedule == null) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,authContext.getI18nHelper().getText("schedule.execute.update.stepresult.invalid.id", SCHEDULE_ENTITY +  String.format(ID,executionId))));
            throw new RESTException(Response.Status.BAD_REQUEST, authContext.getI18nHelper().getText("schedule.execute.update.stepresult.invalid.id", SCHEDULE_ENTITY +  String.format(ID,executionId)));
        }
        Project project = projectManager.getProjectObj(schedule.getProjectId());
        //Response will be an error message if user does not have browse permission
        Response response = hasBrowseProjectPermission(project);
        if(response != null) {
            return response;
        }
        if (project == null) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "projectId ", "");
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "Invalid Project", errorMessage, errorMessage);
        }  
    	JiraUtil.setProjectThreadLocal(project);
        boolean hasViewIssuePermission = JiraUtil.hasIssueViewPermission(Long.valueOf(schedule.getIssueId()), null, authContext.getLoggedInUser());
        if (!hasViewIssuePermission) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.issue.permission.error", "Execution");
            log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(), Status.FORBIDDEN,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "Insufficient Issue permission", errorMessage, errorMessage);
        }
        return scheduleResourceDelegate.getExecutionDefects(schedule);
    }


    @ApiOperation(value = "Add Assignee to Execution", notes = "Add Assignee to execution by Execution Id")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{ }")})
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{scheduleId}")
    public Response assignSchedule(@PathParam("scheduleId") final Integer scheduleId, @QueryParam("assignee") final String userName) {

        JSONObject jsonObject = new JSONObject();
        try {
            if (authContext.getLoggedInUser() == null) {
                jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (JSONException e) {
            log.error("Error occurred while assigning schedule.",e);
            return Response.status(Status.BAD_REQUEST).build();
        }

        Schedule schedule = scheduleManager.getSchedule(scheduleId);
        if (schedule == null) {
            log.debug("Invalid scheduleId " + scheduleId);
            /*return Response.serverError().status(Response.Status.BAD_REQUEST).build();*/
            try {
                jsonObject.put("error", authContext.getI18nHelper().getText("schedule.execute.update.stepresult.invalid.id", SCHEDULE_ENTITY +  String.format(ID,scheduleId)));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("schedule.execute.update.stepresult.invalid.id", SCHEDULE_ENTITY +  String.format(ID,scheduleId))));
                return Response.status(Status.BAD_REQUEST).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            } catch (JSONException e) {
                log.error("Invalid scheduleId ",e);
                return Response.status(Status.BAD_REQUEST).build();
            }
        }
        String userKey = ComponentAccessor.getUserKeyService().getKeyForUsername(userName);
        if (userKey == null) {
            Map<String, String> errorMap = new HashMap<String, String>();
            errorMap.put("Unable to find User", authContext.getI18nHelper().getText("zephyr.common.error.invalid", "userName ", userName));
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorMap));
            return Response.status(Status.BAD_REQUEST).cacheControl(ZephyrCacheControl.never()).entity(errorMap).build();
        }
        boolean hasViewIssuePermission = JiraUtil.hasIssueViewPermission(Long.valueOf(schedule.getIssueId()), null, authContext.getLoggedInUser());
        if (!hasViewIssuePermission) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.project.permission.error", "Execution", String.valueOf(schedule.getProjectId()));
            log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(), Status.FORBIDDEN,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "Insufficient Issue permission", errorMessage, errorMessage);
        }
        String oldUserKey = schedule.getAssignedTo();
        schedule.setAssignedTo(userKey);

        schedule.setModifiedBy(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(ComponentAccessor.getJiraAuthenticationContext())));
        //setting modified date
        schedule.setModifiedDate(new Date());

        schedule.save();
        logAuditData(schedule,userKey,oldUserKey);
        log.debug("Schedule " + scheduleId + " is successfully assigned to " + userName);
        return Response.ok(ImmutableMap.of("success","Schedule " + scheduleId + " is successfully assigned to " + userName)).build();
    }

    @ApiOperation(value = "Assign Bulk Executions", notes = "add bulk execution with assignee type <p>This API returns a jobProgressToken which should be used for making the call to /rest/zapi/latest/execution/jobProgress/:jobProgressToken?type=bulk_execution_assign_user_job_progress. Once the request is processed, the jobProgress will populate the message field with result.</p>")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{\"executions\":[\"7\",\"6\",\"5\",\"4\",\"3\"],\"assigneeType\":\"currentUser\"}"),
            @ApiImplicitParam(name = "response", value = "{\"jobProgressToken\":\"0001491865695314-242b71effff9574-0001\"}")})
    @SuppressWarnings("unchecked")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/bulkAssign")
    public Response assignBulkExecutions(final Map<String, Object> params) {
        Map<Schedule, Table<String, String, Object>> changePropertyTables = new HashMap<Schedule, Table<String, String, Object>>();

        JSONObject jsonErrObject = new JSONObject();
        try {
            if (authContext.getLoggedInUser() == null) {
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                jsonErrObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                return Response.status(Status.UNAUTHORIZED).entity(jsonErrObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (JSONException e) {
            log.error("Error occurred while bulk assign.",e);
            return Response.status(Status.BAD_REQUEST).build();
        }

        List<String> schedules = params.get("executions") != null ? (List<String>) params.get("executions") : null;
        String assigneeTypeVal = params.get("assigneeType") != null ? ZCollectionUtils.getAsString(params, "assigneeType") : null;
        String assignee = params.get("assignee") != null ? ZCollectionUtils.getAsString(params, "assignee") : null;

        Map<String, String> errorMap = new HashMap<String, String>();
        if (schedules == null || schedules.isEmpty()) {
            return buildErrorMessage(authContext.getI18nHelper().getText("schedule.update.ID.required", SCHEDULE_ENTITY + " data"));
        }

        String assigneeUserKey = null;
        ApplicationUser assigneeUser = null;
        if (StringUtils.isNotBlank(assigneeTypeVal)) {
            if (StringUtils.equalsIgnoreCase(assigneeTypeVal, "assignee") && StringUtils.isNotBlank(assignee)) {
                assigneeUser = ComponentAccessor.getUserManager().getUserByName(assignee);
            } else if (StringUtils.equalsIgnoreCase(assigneeTypeVal, "assignee") && StringUtils.isBlank(assignee)) {
                return buildErrorMessage(authContext.getI18nHelper().getText("schedule.execute.update.stepresult.invalid.id", "Assignee value"));
            } else {
                assigneeUser = authContext.getLoggedInUser();
            }
            assigneeUserKey = assigneeUser != null && assigneeUser.isActive() ? assigneeUser.getKey() : null;
            if (StringUtils.isBlank(assigneeUserKey)) {
                return buildErrorMessage(authContext.getI18nHelper().getText("schedule.execute.update.stepresult.invalid.id", "Assignee value"));
            }
        }

        final Collection<Integer> scheduleIds = CollectionUtils.collect(schedules, new Transformer() {
            @Override
            public Integer transform(final Object input) {
                if (StringUtils.isBlank(String.valueOf(input))) {
                    return null;
                }
                final Integer scheduleId = Integer.valueOf(String.valueOf(input));
                return scheduleId;
            }
        });

        removeNullSchedules(scheduleIds);

        List<Object> successfulIds = new ArrayList<Object>();
        List<Schedule> indexSchedules = new ArrayList<Schedule>();
        Collection<String> noZephyrPermissionExecutions = new ArrayList<String>();
        Collection<String> noJiraPermissions = new ArrayList<String>();
        Collection<String> workFlowCompletedExecutions = new ArrayList<>();

        String jobProgressToken = new UniqueIdGenerator().getStringId();
        jobProgressService.createJobProgress(ApplicationConstants.BULK_EXECUTION_ASSIGN_USER_JOB_PROGRESS,0,jobProgressToken);
        jobProgressService.addSteps(jobProgressToken,schedules.size());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        final ApplicationUser user = authContext.getLoggedInUser();
        final String assigneeUserKeyStr=assigneeUserKey;
        executor.submit(()->{
            if(authContext != null && authContext.getLoggedInUser() == null)
                authContext.setLoggedInUser(user);
            //For Auditing, First get all the schedules and set the changeProperty map
            Schedule[] scheduleArr = scheduleManager.getSchedules(new ArrayList<Integer>(scheduleIds));
		/* TODO - Avoid double for loop and double execution fetching */
            for (Schedule schedule : scheduleArr) {
                //Check Project Permission first for each schedule..
                boolean hasJiraPermission = JiraUtil.hasBrowseProjectPermission(schedule.getProjectId(), authContext.getLoggedInUser());
                //Check Issue Permission
                boolean hasViewIssuePermission = JiraUtil.hasIssueViewPermission(Long.valueOf(schedule.getIssueId()), null, authContext.getLoggedInUser());
                if (hasJiraPermission && hasViewIssuePermission) {
                    boolean loggedInUserHasZephyrPermission = verifyBulkPermissions(schedule,authContext.getLoggedInUser());
                    if(loggedInUserHasZephyrPermission) {
                        if(null != schedule.getExecutionWorkflowStatus() &&
                                schedule.getExecutionWorkflowStatus().name().equals(ExecutionWorkflowStatus.COMPLETED.name())) {
                            workFlowCompletedExecutions.add(schedule.getID()+StringUtils.EMPTY);
                            scheduleIds.remove(schedule.getID());
                        } else {
                            /* Only set/reset execution value if status has been updated */
                            if ((StringUtils.isBlank(schedule.getAssignedTo()) && StringUtils.isNotBlank(assigneeUserKeyStr)) ||
                                    (StringUtils.isNotBlank(schedule.getAssignedTo()) && !schedule.getAssignedTo().equalsIgnoreCase(assigneeUserKeyStr))) {
                                Table<String, String, Object> changePropertyTable = HashBasedTable.create();
                                // Saving modified STATUS, EXECUTED_BY and EXECUTED_ON for change logs
                                changePropertyTable.put("ASSIGNED_TO", ApplicationConstants.OLD, StringUtils.isEmpty(schedule.getAssignedTo()) ? ApplicationConstants.NULL : schedule.getAssignedTo());
                                changePropertyTable.put("ASSIGNED_TO", ApplicationConstants.NEW, StringUtils.isEmpty(assigneeUserKeyStr) ? ApplicationConstants.NULL : assigneeUserKeyStr);
                                changePropertyTables.put(schedule, changePropertyTable);
                            }
                        }

                    } else {
                        noZephyrPermissionExecutions.add(String.valueOf(schedule.getID()));
                        scheduleIds.remove(schedule.getID());
                    }
                } else {
                    if(!hasViewIssuePermission) {
                        noJiraPermissions.add(String.valueOf(schedule.getID()));
                    }
                    scheduleIds.remove(schedule.getID());
                }
            }

            for (Integer scheduleId : scheduleIds) {
                Schedule schedule = scheduleManager.getSchedule(((Integer) scheduleId).intValue());
                if (schedule != null) {
                    schedule.setAssignedTo(assigneeUserKeyStr);
                    schedule.setModifiedBy(authContext.getLoggedInUser().getKey());
                    //setting modified date
                    schedule.setModifiedDate(new Date());
                    schedule.save();
                    indexSchedules.add(schedule);
                    successfulIds.add(scheduleId);
                    jobProgressService.addCompletedSteps(jobProgressToken,1);
                }
            }

            //log each status
            for (Schedule schedule : changePropertyTables.keySet()) {
                Table<String, String, Object> changePropertyTable = changePropertyTables.get(schedule);
                // publishing ScheduleModifyEvent for change logs
                eventPublisher.publish(new ScheduleModifyEvent(schedule, changePropertyTable, EventType.EXECUTION_UPDATED,
                        UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));
            }

            if (successfulIds.size() > 0) {
                try {
                    //Need Index update on the same thread.
                    EnclosedIterable<Schedule> enclosedSchedules = CollectionEnclosedIterable.copy(indexSchedules);
                    scheduleIndexManager.reIndexSchedule(enclosedSchedules, Contexts.nullContext());
                } catch (Exception e) {
                    log.error("Error Indexing Schedule:", e);
                }
            }

            if(noZephyrPermissionExecutions.size()>0){
                jobProgressService.addCompletedSteps(jobProgressToken, noZephyrPermissionExecutions.size());
            }
            if(noJiraPermissions.size()>0){
                jobProgressService.addCompletedSteps(jobProgressToken,noJiraPermissions.size());
            }
            if(workFlowCompletedExecutions.size()>0){
                jobProgressService.addCompletedSteps(jobProgressToken,workFlowCompletedExecutions.size());
            }
            Collection<String> executions = transformSchedulestoString(schedules);

            ScheduleResourceHelper resourceHelper = new ScheduleResourceHelper(scheduleIndexManager);
            JSONObject jsonObject = resourceHelper.formBulkUpdateResponse(authContext.getI18nHelper(), new ArrayList(executions), indexSchedules, noZephyrPermissionExecutions, noJiraPermissions, workFlowCompletedExecutions);
            jobProgressService.setMessage(jobProgressToken,jsonObject.toString());
            jobProgressService.addCompletedSteps(jobProgressToken,1);
        });
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(ApplicationConstants.JOB_PROGRESS_TOKEN,jobProgressToken);
        } catch (JSONException e) {
            log.error("error getting job progress token",e);
        }
         return Response.ok(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
     }

    @ApiOperation(value = "Create New Execution", notes = "Use this resource to create new execution")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{\"cycleId\":\"-1\",\"issueId\":\"10013\",\"projectId\":\"10000\",\"versionId\":\"10001\",\"assigneeType\":\"assignee\",\"assignee\":\"jira_user\",\"folderId\":233}"),
    @ApiImplicitParam(name = "response", value = "{\"13377\":{\"id\":13377,\"orderId\":13377,\"executionStatus\":\"-1\",\"comment\":\"\",\"htmlComment\":\"\",\"cycleId\":-1,\"cycleName\":\"Ad hoc\",\"versionId\":10001,\"versionName\":\"Version2\",\"projectId\":10000,\"createdBy\":\"vm_admin\",\"modifiedBy\":\"vm_admin\",\"assignedTo\":\"user1\",\"assignedToDisplay\":\"user1\",\"assignedToUserName\":\"user1\",\"assigneeType\":\"assignee\",\"issueId\":10013,\"issueKey\":\"SAM-14\",\"summary\":\"Test\",\"label\":\"\",\"component\":\"\",\"projectKey\":\"SAM\", \"folderId\":233,\"folderName\":\"testfolder\"}}")})
    @SuppressWarnings("unused")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    //@Path("/create/")
    public Response createExecution(@Context HttpServletRequest req, final Map<String, Object> params) {
        log.debug("Create Execution Request Params:"+ params != null ? params.toString() : null);
        JSONObject jsonErrObject = new JSONObject();
        try {
            if (authContext.getLoggedInUser() == null) {
                jsonErrObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                return Response.status(Status.UNAUTHORIZED).entity(jsonErrObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (JSONException e) {
            log.error("Error occurred while creating execution.",e);
            return Response.status(Status.BAD_REQUEST).build();
        }
        //Method validateIdsAndRelations validates the Input fields and sets the 
        // necessary Project object into ThreadLocal.
        Map<String, String> errorMap = validateIdsAndRelations(params, false);
        if (null != errorMap && errorMap.size() > 0) {
            ResponseBuilder builder = Response.status(Response.Status.BAD_REQUEST);
            builder.type(MediaType.APPLICATION_JSON);
            builder.entity(errorMap);
            log.error(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,errorMap));
            return builder.build();
        }
        return scheduleResourceDelegate.createExecution(req, params);
    }

    @ApiOperation(value = "Get Execution Count Summary", notes = "Get Execution Count List. \n\n It takes care of 3 execution counts\n 1. execution summary - projectId + groupFld:timePeriod\n 2. test execution gadget - projectId + version + groupFld:cycle|user|component\n 3. burndown - projectId + versionId + cycleId + groupFld:timePeriod")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"statusSeries\":{\"-1\":{\"color\":\"#A0A0A0\",\"name\":\"UNEXECUTED\",\"id\":-1,\"desc\":\"The test has not yet been executed.\"},\"1\":{\"color\":\"#75B000\",\"name\":\"PASS\",\"id\":1,\"desc\":\"Test was executed and passed successfully.\"},\"2\":{\"color\":\"#CC3300\",\"name\":\"FAIL\",\"id\":2,\"desc\":\"Test was executed and failed.\"},\"3\":{\"color\":\"#F2B000\",\"name\":\"WIP\",\"id\":3,\"desc\":\"Test execution is a work-in-progress.\"},\"4\":{\"color\":\"#6693B0\",\"name\":\"BLOCKED\",\"id\":4,\"desc\":\"The test execution of this test was blocked for some reason.\"},\"5\":{\"color\":\"#990099\",\"name\":\"PENDING\",\"id\":5,\"desc\":\"\"},\"6\":{\"color\":\"#996633\",\"name\":\"APPROVED\",\"id\":6,\"desc\":\"\"},\"7\":{\"color\":\"#ff3366\",\"name\":\"12\",\"id\":7,\"desc\":\"\"}}}")})
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/count")
    @AnonymousAllowed
    public Response getExecutionCount(@QueryParam("projectId") Long projectId,
                                      @QueryParam("versionId") Long versionId,
                                      @QueryParam("groupFld") String groupBy,
                                      @QueryParam("cycleId") Integer cycleId,
                                      @QueryParam("sprintId") Integer sprintId,
                                      @QueryParam(ScheduleResourceHelper.DAYS_NAME) @DefaultValue("30") final String days,
                                      @QueryParam(ScheduleResourceHelper.PERIOD_NAME) @DefaultValue("daily") final String periodName,
                                      @QueryParam("graphType") @DefaultValue("") String graphType)  {
        final ApplicationUser user = authContext.getLoggedInUser();

        JSONObject jsonErrObject = new JSONObject();
        try {
            if (user == null && !JiraUtil.hasAnonymousPermission(user)) {
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                jsonErrObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                return Response.status(Status.UNAUTHORIZED).entity(jsonErrObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (JSONException e) {
            log.error("Error occurred while getting execution count.",e);
            return Response.status(Status.BAD_REQUEST).build();
        }
        if (projectId == null) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "projectId ", "");
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "Invalid Project", errorMessage, errorMessage);
        }
        
        Project project = projectManager.getProjectObj(projectId);
        if (project == null) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "projectId ", "");
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "Invalid Project", errorMessage, errorMessage);
        }        
        JiraUtil.setProjectThreadLocal(project);
        boolean hasPermission = JiraUtil.hasBrowseProjectPermission(project, authContext.getLoggedInUser());
        if (!hasPermission) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.project.permission.error", "Execution", String.valueOf(projectId));
            log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(), Status.FORBIDDEN,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "Insufficient Project permissions", errorMessage, errorMessage);
        }
        return scheduleResourceDelegate.getExecutionCount(versionId, groupBy, cycleId, sprintId, days, periodName, graphType);
    }

    @ApiOperation(value = "Get Top Defect By Issue Status", notes = "Get Defect List by Project Id, Version Id, Issue Status ")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"label\":\"In Progress\",\"value\":\"3\"}")})
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/topDefects")
    @AnonymousAllowed
    public Response getTopDefectsByIssueStatuses(
            @QueryParam("projectId") Integer projectId,
            @QueryParam("versionId") Integer versionId,
            @QueryParam("issueStatuses") @DefaultValue("3|1|4") String issueStatuses,	/*open | in progress | reopen*/
            @QueryParam(ScheduleResourceHelper.HOW_MANY) @DefaultValue("10") final String howManyDays) {

        final ApplicationUser user = authContext.getLoggedInUser();

        JSONObject jsonErrObject = new JSONObject();
        try {
            if (user == null && !JiraUtil.hasAnonymousPermission(user)) {
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                jsonErrObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                return Response.status(Status.UNAUTHORIZED).entity(jsonErrObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (JSONException e) {
            log.error("Error occurred while getting top defects.",e);
            return Response.status(Status.BAD_REQUEST).build();
        }
        if (projectId == null) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "projectId ", "");
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "Invalid Project", errorMessage, errorMessage);
        }
        Project project = projectManager.getProjectObj(projectId.longValue());
        if (project == null) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "projectId ", String.format(ID,projectId));
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "Invalid Project", errorMessage, errorMessage);
        }  
        	
        boolean hasPermission = JiraUtil.hasBrowseProjectPermission(projectId.longValue(), authContext.getLoggedInUser());
        if (!hasPermission) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.project.permission.error", "Execution(s)", String.valueOf(projectId));
            log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "Insufficient Project permissions", errorMessage, errorMessage);
        }
        JiraUtil.setProjectThreadLocal(project);
        return scheduleResourceDelegate.getTopDefectsByIssueStatuses(versionId, issueStatuses, howManyDays);
    }

    /*@TODO - Need to move it to better cache*/
    static Map<Long, Optional<Long>> reindexStatus = new HashMap<Long, Optional<Long>>();

    @ApiOperation(value = "Re Index All Execution", notes = "Re Index all Execution <p>This API returns a jobProgressToken which should be used for making the call to /rest/zapi/latest/execution/jobProgress/:jobProgressToken?type=reindex_job_progress. Once the request is processed, the jobProgress will populate the message field with result.</p>")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"jobProgressToken\":\"0001491865840676-242b71effff9574-0001\"}")})
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/indexAll")
    @WebSudoRequired
    public Response indexAll(@QueryParam("isHardIndex") boolean isHardIndex) {
    	boolean isJiraAdmin = ComponentAccessor.getGlobalPermissionManager().hasPermission(GlobalPermissionKey.ADMINISTER,authContext.getLoggedInUser());
    	if(!isJiraAdmin) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
            log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
            return JiraUtil.getPermissionDeniedErrorResponse(errorMessage);
    	}
    	return scheduleResourceDelegate.indexAll(false, isHardIndex, false, null);
    }

    @ApiOperation(value = "Synchronizes Execution Indexes from DB. It will only add the one which is missing.", notes = "Synchronizes Execution Indexes from DB <p>This API returns a jobProgressToken which should be used for making the call to /rest/zapi/latest/execution/jobProgress/:jobProgressToken?type=sync_reindex_job_progress. Once the request is processed, the jobProgress will populate the message field with result.</p>")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"jobProgressToken\":\"0001491865840676-242b71effff9574-0001\"}")})
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/syncIndex")
    @WebSudoRequired
    public Response syncIndex() {
        boolean isJiraAdmin = ComponentAccessor.getGlobalPermissionManager().hasPermission(GlobalPermissionKey.ADMINISTER,authContext.getLoggedInUser());
        if(!isJiraAdmin) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
            log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
            return JiraUtil.getPermissionDeniedErrorResponse(errorMessage);
        }
        return scheduleResourceDelegate.indexAll(true, false, false, null);
    }

    @ApiOperation(value = "Re-Index All Execution for Current Node. ", notes = "Re Index all Execution for Current Node <p>This API returns a jobProgressToken which should be used for making the call to /rest/zapi/latest/execution/jobProgress/:jobProgressToken?type=reindex_job_progress. Once the request is processed, the jobProgress will populate the message field with result.</p>")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"jobProgressToken\":\"0001491865840676-242b71effff9574-0001\"}")})
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/indexCurrentNode")
    @WebSudoRequired
    public Response indexCurrentNode() {
        boolean isJiraAdmin = ComponentAccessor.getGlobalPermissionManager().hasPermission(GlobalPermissionKey.ADMINISTER,authContext.getLoggedInUser());
        if(!isJiraAdmin) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
            log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
            return JiraUtil.getPermissionDeniedErrorResponse(errorMessage);
        }
        return scheduleResourceDelegate.indexCurrentNode();
    }

    @ApiOperation(value = "Re Index All Execution for given project id(s)", notes = "Re Index all Execution for given project id(s) <p>This API returns a jobProgressToken which should be used for making the call to /rest/zapi/latest/execution/jobProgress/:jobProgressToken?type=reindex_job_progress. Once the request is processed, the jobProgress will populate the message field with result.</p>")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"jobProgressToken\":\"0001491865840676-242b71effff9574-0001\"}")})
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/reindex/byProject")
    @WebSudoRequired
    public Response reIndexByProjectIds(@QueryParam("projectIds") String projectIds, @QueryParam("isHardIndex") boolean isHardIndex) {
        boolean isJiraAdmin = ComponentAccessor.getGlobalPermissionManager().hasPermission(GlobalPermissionKey.ADMINISTER,authContext.getLoggedInUser());
        if(!isJiraAdmin) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
            log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
            return JiraUtil.getPermissionDeniedErrorResponse(errorMessage);
        }
        Iterable<String> projectIdValues = Splitter.on(',').split(projectIds);
        log.info("Project Id Values:"+projectIdValues.toString());
        String[] projectIdValuesArray = Iterables.toArray(projectIdValues, String.class);
        log.info("projectIdValuesArray Id Values:"+projectIdValuesArray.length);

        if(projectIdValuesArray == null || projectIdValuesArray.length > ApplicationConstants.MAX_PROJECT_ID) {
            String errorMessage = "Max Allowed Project to be ReIndexed is " +ApplicationConstants.MAX_PROJECT_ID;
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,errorMessage));
            return JiraUtil.buildErrorResponse(Status.BAD_REQUEST,"Exceeded Max Allowed Project to be Re-Indexed",errorMessage,errorMessage);
        }
        List<String> projectIdList  = Arrays.asList(projectIdValuesArray);
        return scheduleResourceDelegate.reindexByProjectIds(projectIdList, false, isHardIndex);
    }


    @ApiOperation(value = "Sync Execution Index for given project id(s). It will only add the one which is missing", notes = "Sync Execution Index for given project id(s) <p>This API returns a jobProgressToken which should be used for making the call to /rest/zapi/latest/execution/jobProgress/:jobProgressToken?type=reindex_job_progress. Once the request is processed, the jobProgress will populate the message field with result.</p>")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"jobProgressToken\":\"0001491865840676-242b71effff9574-0001\"}")})
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/syncIndex/byProject")
    @WebSudoRequired
    public Response syncIndexByProjectIds(@QueryParam("projectIds") String projectIds) {
        boolean isJiraAdmin = ComponentAccessor.getGlobalPermissionManager().hasPermission(GlobalPermissionKey.ADMINISTER,authContext.getLoggedInUser());
        if(!isJiraAdmin) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
            log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
            return JiraUtil.getPermissionDeniedErrorResponse(errorMessage);
        }
        Iterable<String> projectIdValues = Splitter.on(',').split(projectIds);
        log.info("Project Id Values:"+projectIdValues.toString());
        String[] projectIdValuesArray = Iterables.toArray(projectIdValues, String.class);
        log.info("projectIdValuesArray Id Values:"+projectIdValuesArray.length);

        if(projectIdValuesArray == null || projectIdValuesArray.length > ApplicationConstants.MAX_PROJECT_ID) {
            String errorMessage = "Max Allowed Project to be ReIndexed is " +ApplicationConstants.MAX_PROJECT_ID;
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,errorMessage));
            return JiraUtil.buildErrorResponse(Status.BAD_REQUEST,"Exceeded Max Allowed Project to be Re-Indexed",errorMessage,errorMessage);
        }
        List<String> projectIdList  = Arrays.asList(projectIdValuesArray);
        return scheduleResourceDelegate.reindexByProjectIds(projectIdList, true, false);
    }

    @ApiOperation(value = "Get Index Status", notes = "Get Index Status by Token")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"took\":\"35 seconds\",\"status\":\"completed\"}")})
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/indexStatus/{token}")
    public Response indexStatus(@PathParam("token") final long token) {
    	boolean isJiraAdmin = ComponentAccessor.getGlobalPermissionManager().hasPermission(GlobalPermissionKey.ADMINISTER,authContext.getLoggedInUser());
    	if(!isJiraAdmin) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
            log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
            return JiraUtil.getPermissionDeniedErrorResponse(errorMessage);
    	}
    	return scheduleResourceDelegate.indexStatus(token);
    }

    @GET
    @ApiOperation(value = "Get job progress status ", notes = " Get job progress with status ")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "response", value = "{\"timeTaken\":\"0 min, 0 sec\",\"stepMessage\":\"\",\"summaryMessage\":\"\",\"errorMessage\":\"\",\"progress\":0.0,\"message\":\"\",\"stepLabel\":\"\",\"stepMessages\":[]}")
    })
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/jobProgress/{jobProgressToken}")
    public Response bulkStatusJobProgress(@PathParam("jobProgressToken") String jobProgressToken, @QueryParam("type") String type){
        try {
            JSONObject result = new JSONObject();
            Map<String, Object> progressMap;
                progressMap = jobProgressService.checkJobProgress(jobProgressToken,type);
                if (null == progressMap) {
                    log.error("Can't get JobProgress from cache jobProgressTicket: " + jobProgressToken);
                }
                return Response.ok().entity(toJson(progressMap).toString()).build();

        }catch (Exception e){
            return Response.ok().build();
        }
    }
    /**
     * Stores a single field value
     */
    @ApiOperation(value = "Update Bulk Execution Status", notes = "Update bulk Execution Status by Status <p>This API returns a jobProgressToken which should be used for making the call to /rest/zapi/latest/execution/jobProgress/:jobProgressToken?type=update_bulk_execution_status_job_progress. Once the request is processed, the jobProgress will populate the message field with result.</p>")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{\"executions\":[\"7\",\"6\",\"5\",\"4\",\"3\"],\"status\":\"4\"}"),
            @ApiImplicitParam(name = "response", value = "{\"jobProgressToken\":\"0001491865478768-242b71effff9574-0001\"}")})
    @SuppressWarnings("unchecked")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/updateBulkStatus")
    public Response updateBulkStatus(final Map<String, Object> params) {
        String jobProgressToken = new UniqueIdGenerator().getStringId();
        Map<Schedule, Table<String, String, Object>> changePropertyTables = new HashMap<Schedule, Table<String, String, Object>>();
        List<String> schedules = (List<String>) params.get("executions");
        Boolean clearDefectMappingFlag = ZCollectionUtils.getAsBoolean(params, "clearDefectMappingFlag", Boolean.FALSE);
        Collection<String> noZephyrPermissionExecutions = new ArrayList<String>();
        Collection<String> noJiraPermission = new ArrayList<>();
        Collection<String> workFlowCompletedExecutions = new ArrayList<>();

        String testStatus = ZCollectionUtils.getAsString(params, "status");

        Boolean changeStepStatus = ZCollectionUtils.getAsBoolean(params, "testStepStatusChangeFlag", Boolean.FALSE);
        String stepStatus = null;

        if (schedules == null || schedules.isEmpty()) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("schedule.update.ID.required", SCHEDULE_ENTITY + " data")));
            throw new RESTException(Status.BAD_REQUEST, authContext.getI18nHelper().getText("schedule.update.ID.required", SCHEDULE_ENTITY + " data"));
        }

        final Collection<Integer> scheduleIds = CollectionUtils.collect(schedules, new Transformer() {
            @Override
            public Integer transform(final Object input) {
                if (StringUtils.isBlank(String.valueOf(input))) {
                    return null;
                }
                final Integer scheduleId = Integer.valueOf(String.valueOf(input));
                return scheduleId;
            }
        });

        if (scheduleIds.isEmpty()) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("schedule.update.ID.required", SCHEDULE_ENTITY + " data")));
            throw new RESTException(Status.BAD_REQUEST, authContext.getI18nHelper().getText("schedule.update.ID.required", SCHEDULE_ENTITY + " data"));
        }

        //ZAPI validation
        JSONObject errorJsonObject = zapiValidationService.validateTestStatus(testStatus);
        if (errorJsonObject != null) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorJsonObject.toString()));
            return Response.status(Status.BAD_REQUEST).entity(errorJsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
        }

        if (changeStepStatus.booleanValue()) {
            stepStatus = ZCollectionUtils.getAsString(params, "stepStatus");
            errorJsonObject = zapiValidationService.validateStepStatus(stepStatus, true);
            if (errorJsonObject != null) {
                log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorJsonObject.toString()));
                return Response.status(Status.BAD_REQUEST).entity(errorJsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        }

        //For Auditing, First get all the schedules and set the changeProperty map
        Schedule[] scheduleArr = scheduleManager.getSchedules(new ArrayList<Integer>(scheduleIds));
        for (Schedule schedule : scheduleArr) {
            //Check Project Permission first for each schedule..
            boolean hasBrowsePermission = JiraUtil.hasBrowseProjectPermission(schedule.getProjectId(), authContext.getLoggedInUser());
            boolean hasViewIssuePermission = JiraUtil.hasIssueViewPermission(Long.valueOf(schedule.getIssueId()), null, authContext.getLoggedInUser());
            if (hasBrowsePermission && hasViewIssuePermission) {
               	boolean loggedInUserHasZephyrPermission = verifyBulkPermissions(schedule,authContext.getLoggedInUser());
                if(loggedInUserHasZephyrPermission) {

                    if(null != schedule.getExecutionWorkflowStatus() &&
                            schedule.getExecutionWorkflowStatus().name().equals(ExecutionWorkflowStatus.COMPLETED.name())) {
                        workFlowCompletedExecutions.add(schedule.getID()+StringUtils.EMPTY);
                        scheduleIds.remove(schedule.getID());
                    } else {
                        /* Only set/reset execution value if status has been updated */
                        if ((testStatus != null) && (testStatus != schedule.getStatus())) {
                            Table<String, String, Object> changePropertyTable = HashBasedTable.create();
                            // Saving modified STATUS, EXECUTED_BY and EXECUTED_ON for change logs
                            populateChangePropertyTable(testStatus, schedule, changePropertyTable);
                            changePropertyTables.put(schedule, changePropertyTable);
                        }
                    }
                } else {
                	noZephyrPermissionExecutions.add(String.valueOf(schedule.getID()));
                    scheduleIds.remove(schedule.getID());
                }
            } else {
                noJiraPermission.add(String.valueOf(schedule.getID()));
                scheduleIds.remove(schedule.getID());
            }
        }

        jobProgressService.createJobProgress(ApplicationConstants.UPDATE_BULK_EXECUTION_STATUS_JOB_PROGRESS,0,jobProgressToken);
        jobProgressService.addSteps(jobProgressToken,scheduleIds.size());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        String finalStepStatus = stepStatus;
        final ApplicationUser user = authContext.getLoggedInUser();
        executor.submit(()->{
            if(authContext != null && authContext.getLoggedInUser() == null)
                authContext.setLoggedInUser(user);

            List<Schedule> scheduleList = scheduleManager.updateBulkStatus(scheduleIds, testStatus, finalStepStatus, clearDefectMappingFlag, changeStepStatus, JiraUtil.getLoggedInUser(authContext), jobProgressToken);

             //log each status
             for (Schedule schedule : changePropertyTables.keySet()) {
                 Table<String, String, Object> changePropertyTable = changePropertyTables.get(schedule);
                 // publishing ScheduleModifyEvent for change logs
                 eventPublisher.publish(new ScheduleModifyEvent(schedule, changePropertyTable, EventType.EXECUTION_UPDATED,
                         UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));
             }

             if (scheduleList.size() > 0) {
                 try {
                     //Need Index update on the same thread for ZQL.
                     EnclosedIterable<Schedule> enclosedSchedules = CollectionEnclosedIterable.copy(scheduleList);
                     scheduleIndexManager.reIndexSchedule(enclosedSchedules, Contexts.nullContext());
                 } catch (Exception e) {
                     log.error("Error Indexing Schedule:", e);
                 }
                 //Publish out Schedule updates
                 //eventPublisher.publish(new SingleScheduleEvent(schedules1, new HashMap<String,Object>(), EventType.EXECUTION_UPDATED));
             }
            ScheduleResourceHelper resourceHelper = new ScheduleResourceHelper(scheduleIndexManager);
            JSONObject jsonObject = resourceHelper.formBulkUpdateResponse(authContext.getI18nHelper(), schedules, scheduleList,noZephyrPermissionExecutions,noJiraPermission,workFlowCompletedExecutions);
            jobProgressService.setMessage(jobProgressToken,jsonObject.toString());
            jobProgressService.addCompletedSteps(jobProgressToken,1);
        });
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(ApplicationConstants.JOB_PROGRESS_TOKEN,jobProgressToken);
        } catch (JSONException e) {
            log.error("error getting job progress token",e);
        }
        return Response.ok(jsonObject.toString()).build();
    }

    /**
     * Stores a single field value
     */
    @ApiOperation(value = "Bulk Assign CustomFields to Executions", notes = "Bulk Assign Custom Fields to Executions <p>This API returns a jobProgressToken which should be used for making the call to /rest/zapi/latest/execution/jobProgress/:jobProgressToken?type=update_bulk_execution_status_job_progress. Once the request is processed, the jobProgress will populate the message field with result.</p>")
    @SuppressWarnings("unchecked")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/bulkAssignCustomFields")
	public Response bulkAssignCustomFields(final Map<String, Object> params) {
        String jobProgressToken = new UniqueIdGenerator().getStringId();
        List<String> scheduleIdsInputList = (List<String>) params.get("executions");
        Map<String, CustomFieldValueResource.CustomFieldValueRequest> customFieldValueRequests = (Map<String,CustomFieldValueResource.CustomFieldValueRequest >) params.get("customFieldValues");
        Collection<String> noZephyrPermissionExecutions = new ArrayList<>();
        Collection<String> noJiraPermission = new ArrayList<>();
        Collection<String> workFlowCompletedExecutions = new ArrayList<>();

        if (CollectionUtils.isEmpty(scheduleIdsInputList) || MapUtils.isEmpty(customFieldValueRequests)) {
            log.error(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,
                    authContext.getI18nHelper().getText("schedule.update.ID.required", SCHEDULE_ENTITY + " data")));
            throw new RESTException(Status.BAD_REQUEST, authContext.getI18nHelper().getText("schedule.update.ID.required", SCHEDULE_ENTITY + " data"));
        }

        List<Integer> scheduleIds = scheduleIdsInputList.stream().map(Integer::valueOf).collect(Collectors.toList());
        removeNullSchedules(scheduleIds);

        if (CollectionUtils.isEmpty(scheduleIds)) {
            log.error(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,
                    authContext.getI18nHelper().getText("schedule.update.ID.required", SCHEDULE_ENTITY + " data")));
            throw new RESTException(Status.BAD_REQUEST, authContext.getI18nHelper().getText("schedule.update.ID.required", SCHEDULE_ENTITY + " data"));
        }

        // For Auditing, First get all the schedules and set the changeProperty map
        Schedule[] scheduleArr = scheduleManager.getSchedules(scheduleIds);
        for (Schedule schedule : scheduleArr) {
            // Check Project Permission first for each schedule..
            boolean hasBrowsePermission = JiraUtil.hasBrowseProjectPermission(schedule.getProjectId(), authContext.getLoggedInUser());
            boolean hasViewIssuePermission = JiraUtil.hasIssueViewPermission(Long.valueOf(schedule.getIssueId()), null, authContext.getLoggedInUser());
            if (hasBrowsePermission && hasViewIssuePermission) {
                boolean loggedInUserHasZephyrPermission = verifyBulkPermissions(schedule, authContext.getLoggedInUser());
                if (loggedInUserHasZephyrPermission) {
                    // Saving modified STATUS, EXECUTED_BY and EXECUTED_ON for change logs
                    if(null != schedule.getExecutionWorkflowStatus() &&
                            schedule.getExecutionWorkflowStatus().name().equals(ExecutionWorkflowStatus.COMPLETED.name())) {
                        workFlowCompletedExecutions.add(schedule.getID()+StringUtils.EMPTY);
                        scheduleIds.remove(new Integer(schedule.getID()));
                    }

                } else {
                    noZephyrPermissionExecutions.add(String.valueOf(schedule.getID()));
                    scheduleIds.remove(new Integer(schedule.getID()));
                }
            } else {
                noJiraPermission.add(String.valueOf(schedule.getID()));
                scheduleIds.remove(new Integer(schedule.getID()));
            }
        }

        jobProgressService.createJobProgress(ApplicationConstants.BULK_EXECUTION_ASSIGN_CF_JOB_PROGRESS, 0, jobProgressToken);
        jobProgressService.addSteps(jobProgressToken, scheduleIds.size());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        final ApplicationUser user = authContext.getLoggedInUser();
        executor.submit(() -> {
            if (authContext != null && authContext.getLoggedInUser() == null)
                authContext.setLoggedInUser(user);
            List<Schedule> processedSchedules = Lists.newArrayList();
            if(CollectionUtils.isNotEmpty(scheduleIds)) {
                processedSchedules = scheduleManager.bulkAssignCustomFields(scheduleManager.getSchedules(scheduleIds), customFieldValueRequests,
                        JiraUtil.getLoggedInUser(authContext), jobProgressToken);

                if (CollectionUtils.isNotEmpty(processedSchedules)) {
                    try {
                        // Need Index update on the same thread for ZQL.
                        EnclosedIterable<Schedule> enclosedSchedules = CollectionEnclosedIterable.copy(processedSchedules);
                        scheduleIndexManager.reIndexSchedule(enclosedSchedules, Contexts.nullContext());
                    } catch (Exception e) {
                        log.error("Error Indexing Schedule:", e);
                    }
                }
            }

            ScheduleResourceHelper resourceHelper = new ScheduleResourceHelper(scheduleIndexManager);
            JSONObject responseJson = resourceHelper.formBulkUpdateResponse(authContext.getI18nHelper(), scheduleIdsInputList, processedSchedules, noZephyrPermissionExecutions, noJiraPermission, workFlowCompletedExecutions);
            jobProgressService.setMessage(jobProgressToken, responseJson.toString());
            jobProgressService.addCompletedSteps(jobProgressToken, 1);
        });
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(ApplicationConstants.JOB_PROGRESS_TOKEN, jobProgressToken);
        } catch (JSONException e) {
            log.error("error getting job progress token", e);
        }
        return Response.ok(jsonObject.toString()).build();
    }

    /**
     * Stores a single field value
     */
    @ApiOperation(value = "Update Bulk Defects", notes = "Update bulk Defect by Executions , Defects <p>This API returns a jobProgressToken which should be used for making the call to /rest/zapi/latest/execution/jobProgress/:jobProgressToken?type=bulk_execution_associate_defect_job_progress. Once the request is processed, the jobProgress will populate the message field with result.</p>")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{\"executions\": [ \"67\",\"66\" ], \"defects\": [ \"ABC-41\",\"ABC-39\" ] }"),
            @ApiImplicitParam(name = "response", value = "{\"jobProgressToken\":\"0001491865740912-242b71effff9574-0001\"}")})
    @SuppressWarnings("unchecked")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/updateWithBulkDefects")
    public Response updateWithBulkDefects(@Context HttpServletRequest request, final Map<String, Object> params) {
        final ScheduleResourceHelper helper = new ScheduleResourceHelper(issueManager, rilManager, scheduleManager,stepResultManager);
        Table<String, String, Object> changePropertyTable = HashBasedTable.create();

        List<Object> scheduleList = (List<Object>) params.get("executions");
        List<String> defects = (List<String>) params.get("defects");
        String expando = ObjectUtils.identityToString(params.get("detailedResponse"));

        List<Integer> issueIds = new ArrayList<Integer>();
        //Get unique Defect Set by removing the duplicate
        Set<String> uniqueDefectSet = new HashSet<String>(defects);
        Boolean remoteIssueLinkEnabled = JiraUtil.isIssueToTestExecutionRemoteLinkingEnabled();
        Boolean issuelinkEnabled = JiraUtil.isIssueToTestLinkingEnabled();
        String contextPath = request.getContextPath();
        Collection<String> invalidDefectKeys = new ArrayList<>();
        Collection<String> jiraPermissionDefectKeys = new ArrayList<>();

        Integer schedId = 0;Schedule sched=null;Issue test=null;
        if(scheduleList.size()>0) {
            Object scheduleIdObject = scheduleList.get(0);
            Optional<Integer> scheduleId = ZCollectionUtils.getAsOptionalInteger(scheduleIdObject);
             schedId = scheduleId.get();
             sched = scheduleManager.getSchedule(schedId);
             test = issueManager.getIssueObject(sched.getIssueId().longValue());
            ProjectPermissionKey projectPermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_EDIT_EXECUTION.toString());
            boolean hasZephyrPermission = zephyrPermissionManager.validateUserPermission(projectPermissionKey, test.getProjectObject(), authContext.getLoggedInUser(), test.getProjectObject().getId());
            if(!hasZephyrPermission){
                String errMsg = authContext.getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
                return JiraUtil.getPermissionDeniedErrorResponse(errMsg);
            }
        }

        for (String issueKey : uniqueDefectSet) {
            MutableIssue issue = issueManager.getIssueObject(issueKey);
            boolean hasViewIssuePermission = JiraUtil.hasIssueViewPermission(null,issue,authContext.getLoggedInUser());
            if (issue != null && hasViewIssuePermission) {
                issueIds.add(Integer.valueOf(issue.getId().intValue()));
            } else if(issue != null & !hasViewIssuePermission) {
                log.error("User does not have access to the Defect passed in. skipping issue with key : " + issue.getKey());
                if(!jiraPermissionDefectKeys.contains(issueKey)) {
                    jiraPermissionDefectKeys.add(issueKey);
                }
            } else {
                log.error("Key passed in does not exist. skipping key");
                if(!invalidDefectKeys.contains(issueKey)) {
                    invalidDefectKeys.add(issueKey);
                }
            }
        }
        JSONObject jsonObject = new JSONObject();
        List<Schedule> schedules = new ArrayList<Schedule>();
        Map<Integer, Map<String, Object>> responseMap = scheduleManager.bulkAssociateDefectsToSchedules(scheduleList, issueIds);

        Long issueLinkTypeId = helper.getLinkTypeId();
        Long oldIssueLinkTypeId = issueLinkTypeId;

        boolean proceed = false;
        int size = 0;
        if(scheduleList != null && scheduleList.size()>0){
            for(Object scheduleIdObject : scheduleList) {
                Optional<Integer> scheduleId = ZCollectionUtils.getAsOptionalInteger(scheduleIdObject);
                Schedule schedule = scheduleManager.getSchedule(scheduleId.get());
                if(schedule != null) {
                    proceed = true;
                    size++;
                }
            }
        }
        JSONObject tokenObject = new JSONObject();
        if(!proceed){
            try {
                tokenObject.put(ApplicationConstants.ERROR,authContext.getI18nHelper().getText("zephyr.je.entity.invalid.error",ApplicationConstants.EXECUTION));
            } catch (JSONException e) {
                log.error("error getting placing error msg",e);
            }
           return Response.ok(tokenObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
        }
        String jobProgressToken = new UniqueIdGenerator().getStringId();
        jobProgressService.createJobProgress(ApplicationConstants.BULK_EXECUTION_ASSOCIATE_DEFECT_JOB_PROGRESS,0,jobProgressToken);
        jobProgressService.addSteps(jobProgressToken,size);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        final ApplicationUser user = authContext.getLoggedInUser();
        executor.submit(()->{
            if(authContext != null && authContext.getLoggedInUser() == null)
                authContext.setLoggedInUser(user);

            for (Integer scheduleId : responseMap.keySet()) {
                jobProgressService.addCompletedSteps(jobProgressToken,1);
                Collection<Integer> deletedDefectIds = new ArrayList<Integer>();
                Collection<Integer> unchangedDefectIds = new ArrayList<Integer>();
                Collection<String> sameIssueIds = new ArrayList<String>();

                Map<String, Object> associatedDefectsMap = responseMap.get(scheduleId);
                Collection<Integer> associatedAddedDefectIds = (Collection<Integer>) associatedDefectsMap.get("added");
                Collection<Integer> associatedDeletedDefectIds = (Collection<Integer>) associatedDefectsMap.get("deleted");
                Collection<Integer> unchanged = (Collection<Integer>) associatedDefectsMap.get("unchanged");
                String sameIssueError = (String) associatedDefectsMap.get("invalid");
                String missing = (String) associatedDefectsMap.get("missing");
                String noZephyrPermissionExecution = (String) associatedDefectsMap.get("noPermission");
                String workFlowCompletedExecutions = (String) associatedDefectsMap.get("workFlowCompletedExecutions");

                List<ScheduleDefect> associatedDefects = (List<ScheduleDefect>) associatedDefectsMap.get("final");
                Schedule schedule = (Schedule) associatedDefectsMap.get("schedule");

                if (StringUtils.isBlank(missing)) {
                    if(StringUtils.isBlank(noZephyrPermissionExecution) && StringUtils.isBlank(workFlowCompletedExecutions)) {
                        schedules.add(schedule);
                        deletedDefectIds.addAll(associatedDeletedDefectIds);

                        unchangedDefectIds = CollectionUtils.subtract(issueIds, unchanged);

                        String cycleBreadCrumb = helper.getCycleBreadCrumbOfSchedule(schedule);
                        Issue testcase = issueManager.getIssueObject(new Long(schedule.getIssueId()));
                        helper.addRemoteLinks(associatedAddedDefectIds, testcase, schedule, cycleBreadCrumb, contextPath, issuelinkEnabled, remoteIssueLinkEnabled, issueLinkTypeId, oldIssueLinkTypeId);
                        helper.addIssueLinks(associatedAddedDefectIds, testcase, issueLinkTypeId, oldIssueLinkTypeId, issuelinkEnabled);
                        removeRemoteLinks(associatedDeletedDefectIds, String.valueOf(schedule.getID()));
                        //removeIssueLinks(associatedDeletedDefectIds, testcase);

                        sameIssueIds.add(sameIssueError);

                        if ((deletedDefectIds!=null&&!deletedDefectIds.isEmpty())) 
                            unchanged.remove(deletedDefectIds);

                        // Saving added/deleted Schedule Defect(s) for change logs
                        if ((null != associatedAddedDefectIds && !associatedAddedDefectIds.isEmpty())) {
                            String[] scheDefs = new String[unchanged.size()];
                            int indx = 0;
                            // Saving all previous Issue Id's as ',' separated string.
                            for (Integer defectId : unchanged) {
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
                            //log each status
                            // publishing ScheduleModifyEvent for change logs
                            eventPublisher.publish(new ScheduleModifyEvent(schedule, changePropertyTable, EventType.EXECUTION_UPDATED,
                                    UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));
                        }
                    }
                }
                try {
                    JSONObject resultJson = new JSONObject();
                    if (StringUtils.equalsIgnoreCase(expando, "true")) {
                        final Collection<String> linkedDefectKeyList = transformToIssueKeys(associatedAddedDefectIds);
                        resultJson.put("linked", StringUtils.join(linkedDefectKeyList, ","));
                        final Collection<String> alreadyLinkedDefectKeyList = transformToIssueKeys(unchangedDefectIds);
                        resultJson.put("alreadylinked", StringUtils.join(alreadyLinkedDefectKeyList, ","));
                    }

                    if (StringUtils.isNotBlank(sameIssueError)) {
                        resultJson.put("invalid", sameIssueError);
                    }
                    if (StringUtils.isNotBlank(missing)) {
                        resultJson.put("error", StringUtils.join(schedules, ","));
                    }
                    if (StringUtils.isNotBlank(noZephyrPermissionExecution)) {
                        resultJson.put("noPermission", noZephyrPermissionExecution);
                    }
                    if (!invalidDefectKeys.isEmpty() && !jsonObject.has("invalidDefect")) {
                        jsonObject.put("invalidDefect", StringUtils.join(invalidDefectKeys,","));
                    }
                    if (!jiraPermissionDefectKeys.isEmpty() && !jsonObject.has("jiraPermissionDefectKeys")) {
                        jsonObject.put("noIssuePermission", StringUtils.join(jiraPermissionDefectKeys,","));
                    }
                    if (StringUtils.isNotBlank(workFlowCompletedExecutions)) {
                        resultJson.put("workFlowCompletedExecutions", workFlowCompletedExecutions);
                    }
                    jsonObject.put(String.valueOf(scheduleId), resultJson);
                } catch (JSONException e) {
                    log.warn("Error creating JSON Object", e);
                }
                jobProgressService.setMessage(jobProgressToken,jsonObject.toString());
            }

            if (!schedules.isEmpty()) {
                try {
                    //Need Index update on the same thread for ZQL.
                    EnclosedIterable<Schedule> enclosedSchedules = CollectionEnclosedIterable.copy(schedules);
                    scheduleIndexManager.reIndexSchedule(enclosedSchedules, Contexts.nullContext());
                } catch (Exception e) {
                    log.error("Error Indexing Schedule:", e);
                }
                //eventPublisher.publish(new SingleScheduleEvent(schedules, new HashMap<String,Object>(), EventType.EXECUTION_UPDATED));
            }
            jobProgressService.addCompletedSteps(jobProgressToken,1);
        });

         try {
            tokenObject.put(ApplicationConstants.JOB_PROGRESS_TOKEN,jobProgressToken);
        } catch (JSONException e) {
            log.error("error getting job progress token",e);
        }
        return Response.ok(tokenObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
    }


    @SuppressWarnings("unchecked")
    private Collection<String> transformToIssueKeys(
            Collection<Integer> defectIds) {
        final Collection<String> defectKeyList = CollectionUtils.collect(defectIds, new Transformer() {
            @Override
            public String transform(final Object input) {
                if (input == null) {
                    return null;
                }
                final String issueKey = issueManager.getIssueObject(((Integer) input).longValue()).getKey();
                return String.valueOf(issueKey);
            }
        });
        return defectKeyList;
    }


    /**
     * Perform execution on a existing Schedule
     */
    @ApiOperation(value = "Update Execution Details", notes = "Update Execution Details by Execution Id ")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{\"status\":\"3\"}"),
            @ApiImplicitParam(name = "response", value = "{\"id\":49,\"orderId\":21,\"executionStatus\":\"3\",\"executedOn\":\"Today 2:10 PM\",\"executedBy\":\"vm_admin\",\"executedByDisplay\":\"vm_admin\",\"comment\":\"\",\"htmlComment\":\"\",\"cycleId\":51,\"cycleName\":\"c1\",\"versionId\":-1,\"versionName\":\"Unscheduled\",\"projectId\":10100,\"createdBy\":\"vm_admin\",\"modifiedBy\":\"vm_admin\",\"issueId\":10400,\"issueKey\":\"HTC-1\",\"summary\":\"test1\",\"label\":\"\",\"component\":\"\",\"projectKey\":\"HTC\"}")})
    @SuppressWarnings("unchecked")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}/execute")
    public Response executeExecution(@PathParam("id") Integer scheduleId, @Context HttpServletRequest request, Map<String, Object> params) {
        try {

            JSONObject jsonObject = new JSONObject();
            try {
                if (authContext.getLoggedInUser() == null) {
                    log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                    jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                    return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
                }
            } catch (JSONException e) {
                log.error("Error occurred while executing executions.",e);
                return Response.status(Status.BAD_REQUEST).build();
            }

	        Schedule schedule = scheduleManager.getSchedule(scheduleId);
	        //If Null, return response back
	        if (schedule == null) {
	            JSONObject errorJson = new JSONObject();
	            errorJson.put("error", authContext.getI18nHelper().getText("zephyr.common.error.create", SCHEDULE_ENTITY, String.valueOf(scheduleId)));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("zephyr.common.error.create", SCHEDULE_ENTITY, String.valueOf(scheduleId))));
	            return Response.status(Status.BAD_REQUEST).entity(errorJson.toString()).build();
	        }
	        Project project = projectManager.getProjectObj(schedule.getProjectId()); 
	        boolean hasPermission = JiraUtil.hasBrowseProjectPermission(project, authContext.getLoggedInUser());
	        if (!hasPermission) {
	            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.project.permission.error", "Execution(s)", String.valueOf(schedule.getProjectId()));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(), Status.FORBIDDEN,errorMessage));
	            return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "Insufficient Project permissions", errorMessage, errorMessage);
	        }

            boolean isWorkflowDisabled = executionWorkflowResourceDelegate.isExecutionWorkflowDisabled(schedule.getProjectId());
            if(!isWorkflowDisabled && null != schedule.getExecutionWorkflowStatus() &&
                    schedule.getExecutionWorkflowStatus().name().equals(ExecutionWorkflowStatus.COMPLETED.name())) {
                JSONObject errorJson = new JSONObject();
                errorJson.put("error", authContext.getI18nHelper().getText("workflow.schedule.modify.error"));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("workflow.schedule.modify.error")));
                return Response.status(Status.BAD_REQUEST).entity(errorJson.toString()).build();
            }
	        JiraUtil.setProjectThreadLocal(project);
	        return scheduleResourceDelegate.editExecution(schedule, request, params);
        } catch (Exception e) {
	        log.error("Error Executing Test:" + e.getMessage(), e);
	        Map<String, String> errorMap = new HashMap<String, String>();
	        ResponseBuilder builder = Response.status(Response.Status.NOT_ACCEPTABLE);
	        builder.type(MediaType.APPLICATION_JSON);
	        errorMap.put("generic", ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("execute.test.generic.error"));
            errorMap.put("error", e.getMessage()); //check
	        builder.entity(errorMap);
	        return builder.build();
	   }
   }

    @ApiOperation(value = "Delete Execution", notes = "Execution with the given Id will be deleted.")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"success\": \"Successfully deleted execution(s) 54\"}")})
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    public Response deleteExecution(@PathParam("id") Integer scheduleId) {
        //Fetch the Schedule to be removed, for change log info
        Schedule schedule = scheduleManager.getSchedule(scheduleId);

        if (schedule == null) {
            JSONObject jsonObject = new JSONObject();
            try {
                log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,authContext.getI18nHelper().getText("schedule.operation.delete.error.noID", scheduleId)));
                jsonObject.put("error", authContext.getI18nHelper().getText("schedule.operation.delete.error.noID", scheduleId));
            } catch (JSONException e) {
                log.warn("Error creating JSON Object", e);
            }
            return Response.ok().entity(jsonObject.toString()).build();
        }

        //Project BROWSE permission validation
        Project project = projectManager.getProjectObj(schedule.getProjectId());
        boolean hasPermission = JiraUtil.hasBrowseProjectPermission(project, authContext.getLoggedInUser());
        if (!hasPermission) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.project.permission.error", "Execution", String.valueOf(schedule.getProjectId()));
            log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(), Status.FORBIDDEN,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "Insufficient Project permissions", errorMessage, errorMessage);
        }
        JiraUtil.setProjectThreadLocal(project);
        boolean hasViewIssuePermission = JiraUtil.hasIssueViewPermission(Long.valueOf(schedule.getIssueId()),null, authContext.getLoggedInUser());
        if (!hasViewIssuePermission) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.issue.permission.error", "Execution");
            log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(), Status.FORBIDDEN,errorMessage));
            return JiraUtil.getPermissionDeniedErrorResponse("Insufficient Issue permissions." + errorMessage);
        }
        return scheduleResourceDelegate.deleteExecution(schedule);
    }

    @ApiOperation(value = "Delete bulk Execution", notes = "Delete bulk Execution by Execution Id <p>This API returns a jobProgressToken which should be used for making the call to /rest/zapi/latest/execution/jobProgress/:jobProgressToken?type=bulk_executions_delete_job_progress. Once the request is processed, the jobProgress will populate the message field with result.</p>")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{\"executions\":[\"1\",\"2\"]}"),
            @ApiImplicitParam(name = "response", value = "{\"jobProgressToken\":\"0001491865789474-242b71effff9574-0001\"}")})
    @SuppressWarnings("unchecked")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/deleteExecutions")
    public Response deleteExecutions(final Map<String, Object> params) {
        String jobProgressToken = new UniqueIdGenerator().getStringId();
        jobProgressService.createJobProgress(ApplicationConstants.BULK_EXECUTIONS_DELETE_JOB_PROGRESS,0,jobProgressToken);
         List<Integer> scheduleIds = (List<Integer>) params.get("executions");
        if (scheduleIds == null || scheduleIds.size() == 0) {
            JSONObject jsonObject = new JSONObject();
            try {
                log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(), Status.FORBIDDEN,authContext.getI18nHelper().getText("zephyr.common.error.delete", SCHEDULE_ENTITY, "scheduleIds:null")));
                jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.error.delete", SCHEDULE_ENTITY, "scheduleIds:null"));
            } catch (JSONException e) {
                log.warn("Error creating JSON Object", e);
            }
            return Response.ok().entity(jsonObject.toString()).build();
        }


        Schedule[] schedules = scheduleManager.getSchedules(scheduleIds);
        jobProgressService.addSteps(jobProgressToken,schedules.length-1);
        List<Schedule> successful = new ArrayList<Schedule>();
        List<Integer> noPermissionExecutions = new ArrayList<Integer>();
        Collection<String> noJiraPermission = new ArrayList<>();
        List<Schedule> scheduleStreamList = Arrays.asList(schedules);

        List<Integer> cycleList = scheduleStreamList.stream().map((schedule)-> null != schedule.getCycle() ? schedule.getCycle().getID() : null).collect(Collectors.toList());
        if(cycleList != null) {
            boolean isMatch = cycleList.stream().anyMatch(cycleId -> zfjCacheService.getCacheByWildCardKey("CLONE_CYCLE_PROGRESS_CHK" + "_" + String.valueOf(cycleId)));
            if (isMatch) {
                String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.je.admin.clone.in.progress", ApplicationConstants.CYCLE_ENTITY, ApplicationConstants.CYCLE_ENTITY, ApplicationConstants.CYCLE_ENTITY);
                Response responseJsonObject = getErrorResponse(jobProgressToken, errorMessage);
                if (responseJsonObject != null) return responseJsonObject;
            }
        }

        List<Integer> folderList = scheduleStreamList.stream().map((schedule)->  schedule.getFolder() != null ? schedule.getFolder().getID() : null).collect(Collectors.toList());
        if(folderList != null) {
            folderList.removeAll(Collections.singleton(null));
            boolean isFolderMatch = folderList.stream().anyMatch(folderId -> zfjCacheService.getCacheByWildCardKey("CLONE_FOLDER_PROGRESS_CHK" + "_" + String.valueOf(folderId)));
            if (isFolderMatch) {
                String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.je.admin.clone.in.progress", ApplicationConstants.FOLDER_ENTITY, ApplicationConstants.FOLDER_ENTITY, ApplicationConstants.FOLDER_ENTITY);
                Response responseJsonObject = getErrorResponse(jobProgressToken, errorMessage);
                if (responseJsonObject != null) return responseJsonObject;
            }
        }

        scheduleStreamList.stream().forEach(schedule -> {
            Project project = projectManager.getProjectObj(schedule.getProjectId());
            boolean hasPermission = JiraUtil.hasBrowseProjectPermission(project, authContext.getLoggedInUser());
            boolean hasViewIssuePermission = JiraUtil.hasIssueViewPermission(Long.valueOf(schedule.getIssueId()), null, authContext.getLoggedInUser());

            ProjectPermissionKey cycleBrowsePermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_BROWSE_CYCLE.toString());
            boolean hasBrowseCyclePermission = zephyrPermissionManager.validateUserPermission(cycleBrowsePermissionKey, project, authContext.getLoggedInUser(), null);

            //Need to handle ZFJ Permission for bulk here.
            ProjectPermissionKey projectPermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_DELETE_EXECUTION.toString());
            boolean hasExecutionDeletePermission = zephyrPermissionManager.validateUserPermission(projectPermissionKey, project, authContext.getLoggedInUser(), null);
            if (hasPermission && hasBrowseCyclePermission && hasExecutionDeletePermission && hasViewIssuePermission) {
                successful.add(schedule);
            }
            if(!hasBrowseCyclePermission || !hasExecutionDeletePermission) {
                noPermissionExecutions.add(schedule.getID());
            }
            if(!hasViewIssuePermission) {
                noJiraPermission.add(String.valueOf(schedule.getID()));
            }
        });
        int rows = scheduleManager.deleteSchedules(successful.toArray(new Schedule[successful.size()]),jobProgressToken);

        // publishing ScheduleModifyEvent for change logs
        eventPublisher.publish(new ScheduleModifyEvent(successful, null, EventType.EXECUTION_DELETED,
                UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));


        final Collection<String> scheduleList = CollectionUtils.collect(successful, new Transformer() {
            @Override
            public String transform(final Object input) {
                if (input == null) {
                    return null;
                }
                Schedule tempSchedule = (Schedule) input;
                return String.valueOf(tempSchedule.getID());
            }
        });

        if(noPermissionExecutions.size() > 0) {
            jobProgressService.addCompletedSteps(jobProgressToken, noPermissionExecutions.size());
        }
        if(noJiraPermission.size() > 0 ) {
            jobProgressService.addCompletedSteps(jobProgressToken, noJiraPermission.size());
        }
        //Delete  Index using Schedule Event
        EnclosedIterable<String> entityIdIterables = CollectionEnclosedIterable.copy(scheduleList);
        scheduleIndexManager.deleteBatchIndexByTerm(entityIdIterables, "schedule_id", Contexts.nullContext());
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("ENTITY_TYPE", "schedule_id");
        param.put("ENTITY_VALUE", scheduleList);
        eventPublisher.publish(new SingleScheduleEvent(null, param, com.thed.zephyr.je.event.EventType.EXECUTION_DELETED));
        Response response = formBulkDeleteResponse(scheduleIds, noPermissionExecutions, noJiraPermission, successful.toArray(new Schedule[successful.size()]), rows);
        jobProgressService.setMessage(jobProgressToken, response.getEntity().toString());

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(ApplicationConstants.JOB_PROGRESS_TOKEN,jobProgressToken);
        } catch (JSONException e) {
            log.error("error getting job progress token",e);
        }
        return Response.ok(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
    }


    @ApiOperation(value = "Add Test's to Cycle", notes = "Add Test to Cycle \n\n <table><tr><td colspan=2>This API will execute based on following conditions:<tr><td>1<td>From individual test required following params:<br>(assigneeType, cycleId, issues, method = 1, projectId, versionId)<tr><td>2<td>From search filter required following params:<br>(assigneeType, cycleId, issues, method = 2, projectId, versionId, searchId)<tr><td>3<td>From another cycle required following params:<br>(assigneeType, cycleId, issues, method = 3, projectId, versionId, components, fromCycleId, fromVersionId, hasDefects, labels, priorities, statuses)</table><p>This API returns a jobProgressToken which should be used for making the call to /rest/zapi/latest/execution/jobProgress/:jobProgressToken?type=add_tests_to_cycle_job_progress. Once the request is processed, the jobProgress will populate the message field with result.</p>")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{\"components\":\"\",\"cycleId\":\"-1\",\"fromCycleId\":\"3\",\"fromVersionId\":\"10003\",\"hasDefects\":false,\"labels\":\"\",\"method\":\"3\",\"priorities\":\"\",\"projectId\":\"10000\",\"statuses\":\"\",\"versionId\":\"10003\",\"folderId\":12313}"),
            @ApiImplicitParam(name = "response", value = "{\"jobProgressToken\":\"0001491864384175-f6f74daa3cce-0001\"}")})
    @SuppressWarnings("unchecked")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/addTestsToCycle/")
    public Response addTestsToCycle(final Map<String, Object> params) {

        JSONObject jsonObject = new JSONObject();
        try {
            if (authContext.getLoggedInUser() == null) {
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(), Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (JSONException e) {
            log.error("Error occurred while adding tests to the cycle.",e);
            return Response.status(Status.BAD_REQUEST).build();
        }

        Map<String, String> errorMap = validateIdsAndRelations(params, true);
        if (null != errorMap && errorMap.size() > 0 ) {
            ResponseBuilder builder = Response.status(Response.Status.BAD_REQUEST);
            builder.type(MediaType.APPLICATION_JSON);
            builder.entity(errorMap);
            log.error(String.format(
                    ERROR_LOG_MESSAGE,
                    Status.BAD_REQUEST.getStatusCode(),
                    Status.BAD_REQUEST,errorMap));
            return builder.build();
        }
        return scheduleResourceDelegate.addTestsToCycle(params);
    }


    @SuppressWarnings("unchecked")
    private Map<String, String> validateIdsAndRelations(final Map<String, Object> params, boolean skipIssueValidation) {
        Map<String, String> errorMap = new HashMap<String, String>();
        final I18nHelper i18n = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
        Integer projectId = ZCollectionUtils.getAsInteger(params, "projectId");
        Integer issueId = ZCollectionUtils.getAsInteger(params, "issueId");
        Integer versionId = ZCollectionUtils.getAsInteger(params, "versionId");
        Integer cycleId = ZCollectionUtils.getAsInteger(params, "cycleId");
        final Collection<Object> issues = params.get("issues") != null ? (Collection<Object>) params.get("issues") : new ArrayList<Object>();
        if (null == projectId) {
            errorMap.put("Invalid Project", i18n.getText("zephyr.common.error.invalid", "projectId ", String.valueOf(params.get("projectId"))));
            return errorMap;
        }
        Project project = projectManager.getProjectObj(projectId.longValue());
        // validate for a valid Project
        if (null == project) {
            errorMap.put("Invalid Project", i18n.getText("zephyr.common.error.invalid", "projectId ", String.valueOf(params.get("projectId"))));
            return errorMap;
        }
        //Put the Project in ThreadLocal
        JiraUtil.setProjectThreadLocal(project);
        boolean hasPermission = JiraUtil.hasBrowseProjectPermission(project, authContext.getLoggedInUser());
        if (!hasPermission) {
            errorMap.put("Insufficient Project permissions", i18n.getText("schedule.project.permission.error", "Execution(s)", String.valueOf(project.getId())));
            return errorMap;
        }
        String methodType = (String) params.get("method");

        if (StringUtils.isNotBlank(methodType) && methodType.equals("1") && issues.isEmpty()) {
            errorMap.put("Invalid Issue", i18n.getText("zephyr.common.error.invalid", "issueId ", String.valueOf(params.get("issueId"))));
            return errorMap;
        }

        if (StringUtils.isNotBlank(methodType) && !(methodType.equals("1") || methodType.equals("2") || methodType.equals("3"))) {
            errorMap.put("Invalid method type", i18n.getText("zephyr.common.error.invalid", "method type ", methodType));
            return errorMap;
        }

        if (!skipIssueValidation) {
            if (null == issueId) {
                errorMap.put("Invalid Issue", i18n.getText("zephyr.common.error.invalid", "issueId ", String.valueOf(params.get("issueId"))));
                return errorMap;
            }
            Issue test = issueManager.getIssueObject(issueId.longValue());
            // validate for a valid test
            if (null == test) {
                errorMap.put("Invalid Issue", i18n.getText("zephyr.common.error.invalid", "issueId ", String.valueOf(params.get("issueId"))));
                return errorMap;
            }


            if (!StringUtils.equalsIgnoreCase(test.getIssueTypeObject().getId(), JiraUtil.getTestcaseIssueTypeId())) {
                errorMap.put("Invalid Issue", authContext.getI18nHelper().getText("zapi.cycle.get.issue.bytype.test.error", String.valueOf(test.getId()), test.getKey()));
                return errorMap;
            }

            // validate if the test belong to the right project
            if (test != null && test.getProjectObject().getId().longValue() != projectId.longValue()) {
                String issueIdStr = String.valueOf(params.get("issueId"));
                String projectIdStr = String.valueOf(projectId);
                errorMap.put("Invalid Issue", authContext.getI18nHelper().getText("zapi.schedule.project.issue.mismatch", issueIdStr, projectIdStr));
                return errorMap;
            }
        }

        //If the execution is Adhoc, we need to verify that passed in version belongs to that project
        if ((null != cycleId && cycleId.equals(ApplicationConstants.AD_HOC_CYCLE_ID)) || (null == cycleId && versionId != null)) {
            //Defensive Check for Invalid Version ID passed from ZAPI
            if (versionId != null && versionId.intValue() != ApplicationConstants.UNSCHEDULED_VERSION_ID) {
                Version version = versionManager.getVersion(versionId.longValue());
                if (null == version) {
                    errorMap.put("Invalid Version", i18n.getText("zephyr.common.error.invalid", "versionId", String.valueOf(params.get("versionId"))));
                    return errorMap;
                }
                if (!ObjectUtils.equals(version.getProjectObject().getId(), project.getId())) {
                    errorMap.put("Invalid Version", authContext.getI18nHelper().getText("schedule.entity.mismatch.error", "Version", versionId, "Version", "Project", String.valueOf(project.getId())));
                    return errorMap;
                }
            }
        }

        if (null != cycleId && !cycleId.equals(ApplicationConstants.AD_HOC_CYCLE_ID)) {
            Cycle cycle = cycleManager.getCycle(Long.valueOf(cycleId));
            // validating if the cycle belongs to right project
            if (null == cycle || cycle.getProjectId() != projectId.longValue()) {
                errorMap.put("Invalid Cycle", i18n.getText("zapi.schedule.project.cycle.mismatch", String.valueOf(params.get("cycleId")), String.valueOf(params.get("projectId"))));
                return errorMap;
            }
        }
        return errorMap;
    }
    

    /*@TODO - Need to move it to better cache - assumes sessions are sticky*/
    static Map<Long, Optional<Long>> linkRefreshStatus = new ConcurrentHashMap<>(1);

    @ApiOperation(value = "Refresh Issue/Remote Link", notes = "Refresh Issue to Test/Step Link or Remote Link")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"success\": \"refreshRemoteLinks  successfully.\"}")})
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/refreshRemoteLinks")
    @WebSudoRequired
    public Response refreshRemoteLinks(@Context final HttpServletRequest request, @QueryParam("issueLinkTypeId") final Long issueLinkTypeId) {
        final Boolean remoteIssueLinkEnabled = JiraUtil.isIssueToTestExecutionRemoteLinkingEnabled();
        final Boolean issuelinkEnabled = JiraUtil.isIssueToTestLinkingEnabled();

        final Boolean issueToTestStepLink = JiraUtil.isIssueToTestStepLinkingEnabled();;
        final Boolean remoteIssueLinkTestStepExecution = JiraUtil.isIssueToTestStepExecutionRemoteLinkingEnabled();

    	boolean isJiraAdmin = ComponentAccessor.getGlobalPermissionManager().hasPermission(GlobalPermissionKey.ADMINISTER,authContext.getLoggedInUser());
    	if(!isJiraAdmin) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
            log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(), Status.FORBIDDEN,errorMessage));
            return JiraUtil.getPermissionDeniedErrorResponse(errorMessage);
    	}
        
    	if (!issuelinkEnabled && !remoteIssueLinkEnabled && !issueToTestStepLink && !remoteIssueLinkTestStepExecution) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.NOT_ACCEPTABLE.getStatusCode(), Status.NOT_ACCEPTABLE, ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.je.admin.plugin.test.section.item.zephyr.configuration.remoteissuelink.reset.ril.disabled.error")));
            return Response.status(Status.NOT_ACCEPTABLE).entity(
                    ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.je.admin.plugin.test.section.item.zephyr.configuration.remoteissuelink.reset.ril.disabled.error")
            ).build();
        }

        // check if issue link type exists
        Collection<IssueLinkType> issueLinkTypes = Collections2.filter(issueLinkService.getIssueLinkTypes(), new Predicate<IssueLinkType>() {
            @Override
            public boolean apply(IssueLinkType issueLinkType) {
                return issueLinkTypeId.equals(issueLinkType.getId());
            }
        });

        if (null == issueLinkTypes || issueLinkTypes.size() == 0) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.NOT_ACCEPTABLE.getStatusCode(), Status.NOT_ACCEPTABLE,ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.je.admin.plugin.test.section.item.zephyr.configuration.issuelink.reset.link.not.found")));
            return Response.status(Status.NOT_ACCEPTABLE).entity(
                    ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.je.admin.plugin.test.section.item.zephyr.configuration.issuelink.reset.link.not.found")
            ).build();
        }
        final ScheduleResourceHelper helper = new ScheduleResourceHelper(issueManager, rilManager, scheduleManager,stepResultManager);
        // put a lock on bulk reset remote/issue Links
        final String lockName = "zephyr-reset-issueLinks";
        final ClusterLock lock = clusterLockService.getLockForName(lockName);
        try {
            final String contextPath = request.getContextPath();
            final ApplicationUser user = authContext.getLoggedInUser();
            final JSONObject finalResponse = new JSONObject();
            final Long token = System.currentTimeMillis();

            Future<Response> responseFuture = Executors.newSingleThreadExecutor().submit(new Callable<Response>() {
                @Override
                public Response call() throws Exception {
                    try {
                        if (lock.tryLock(0, TimeUnit.SECONDS)) {
                            try {
                                // setting user in auth context as it wouldn't be available on default auth context
                                if(authContext != null && authContext.getLoggedInUser() == null)
                                    authContext.setLoggedInUser(user);
                                long timeTaken = helper.performLinksRefreshAsync(contextPath, issueLinkTypeId, remoteIssueLinkEnabled, issuelinkEnabled,issueToTestStepLink,remoteIssueLinkTestStepExecution);
                                linkRefreshStatus.put(token, Optional.of(timeTaken));
                            } finally {
                                // perform clean up
                                lock.unlock(); // release lock
                                authContext.setLoggedInUser(null);
                            }
                            return null; // return nothing here
                        } else {
                            // if lock is already taken, return error message to client
                            String relinkingAlreadyInProgressMsg = authContext.getI18nHelper().getText("zephyr.je.admin.relinking.already.in.progress");
                            return JiraUtil.buildErrorResponse(Status.FORBIDDEN, "403", relinkingAlreadyInProgressMsg, relinkingAlreadyInProgressMsg);
                        }
                    } catch (InterruptedException e) {
                        /*Setting timeTaken to negative, will make UI get the error*/
                        linkRefreshStatus.put(token, Optional.of(-1l));
                        log.error("refreshRemoteLinks(): Issue links reset operation interrupted: ", e);
                        String relinkingFailedMsg = authContext.getI18nHelper().getText("zephyr.je.admin.relinking.error");
                        return JiraUtil.buildErrorResponse(Status.INTERNAL_SERVER_ERROR, "500", relinkingFailedMsg, relinkingFailedMsg);
                    }
                }
            });
		/* Retrieve future result. If the lock is acquired successfully and relinking started, this future will timeout.
               Hence we will treat timeout as success case. For other cases, future will be redeemed quickly and we will return the response.
            */
			try {
				return responseFuture.get(500, TimeUnit.MILLISECONDS);
			} catch (TimeoutException te) {
				linkRefreshStatus.put(token, Optional.<Long>absent());
				finalResponse.put("token", token);
				return Response.ok(finalResponse.toString()).build();
			}
		} catch (Exception e) {
			// send error message back to client
			log.error("refreshRemoteLinks(): error resetting issue links relation: ", e);
			String relinkingFailedMsg = authContext.getI18nHelper().getText("zephyr.je.admin.relinking.error");
			return JiraUtil.buildErrorResponse(Status.INTERNAL_SERVER_ERROR, "500", relinkingFailedMsg, relinkingFailedMsg);
		}
	}

    @ApiOperation(value = "Refresh Issue Link Status", notes = "Refresh Link Status(not found/in progress/completed)")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"status\":\"notfound\"}")})
    @GET
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/refreshLinksStatus/{token}")
	public Response refreshLinksStatus(@PathParam("token") final long token) {
		JSONObject finalResponse = new JSONObject();
    	boolean isJiraAdmin = ComponentAccessor.getGlobalPermissionManager().hasPermission(GlobalPermissionKey.ADMINISTER,authContext.getLoggedInUser());
    	if(!isJiraAdmin) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
            log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(), Status.FORBIDDEN,errorMessage));
            return JiraUtil.getPermissionDeniedErrorResponse(errorMessage);
    	}
		Optional<Long> timeTaken = linkRefreshStatus.get(token);
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
                log.error(String.format(ERROR_LOG_MESSAGE, Status.INTERNAL_SERVER_ERROR,"Unable to acquire lock on issue relinking, Perhaps issue re-linking already in progress."));
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Unable to acquire lock on issue relinking, Perhaps issue re-linking already in progress.").build();
			}

            /*if (timeTaken.get() < 0) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Unable to acquire lock on issue relinking, Perhaps issue re-linking already in progress...").build();
            }*/

            if (timeTaken.get() > 0) {
                long timeTakenLong = timeTaken.get() / 1000;
                finalResponse.put("took", timeTakenLong + " seconds");
            }
            finalResponse.put("status", "completed");
            linkRefreshStatus.remove(token);
        } catch (JSONException e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("error:" + e.getMessage()).build();
        }
        return Response.ok(finalResponse.toString()).cacheControl(ZephyrCacheControl.never()).build();
    }


/*    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/export")
	public Response exportExecution(@Context final HttpServletResponse response, @QueryParam ("exportType") final String exportType,
			@QueryParam ("zqlQuery") final String zqlQuery,@QueryParam ("startIndex") final Integer startIndex,@QueryParam ("expand") final String expand,
			@QueryParam ("maxAllowedResult") final boolean maxAllowedResult) {
    	if(authContext.getLoggedInUser() == null) {
    		return Response.status(Status.FORBIDDEN).build();
    	}

      	//Refactored and Moved code to Helper
    	ScheduleSearchResourceHelper searchResourceHelper = new ScheduleSearchResourceHelper(authContext.getLoggedInUser(),searchService,exportService,issueManager,cycleManager,versionManager,testStepManager,stepResultManager);
    	return searchResourceHelper.exportExecutions(exportType, zqlQuery, startIndex,expand,maxAllowedResult);
    }*/

    @ApiOperation(value = "Export Execution", notes = "Export Selected Execution by Selected Export Format(RSS/HTML/XLS/CSV/XML)")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{\"exportType\":\"xls\",\"maxAllowedResult\":\"true\",\"expand\":\"teststeps\",\"startIndex\":\"0\",\"zqlQuery\":\"executionStatus != UNEXECUTED AND executedBy = vm_admin\"}"),
            @ApiImplicitParam(name = "response", value = "{\"url\":\"http://localhost:8722/plugins/servlet/export/exportAttachment?fileName=ZFJ-Executions-04-11-2016.xls\"}")})
    @SuppressWarnings("unchecked")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/export")
    public Response exportExecution(@Context final HttpServletResponse response, final Map<String, Object> params) {

        JSONObject jsonObject = new JSONObject();
        try {
            if (authContext.getLoggedInUser() == null) {
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (JSONException e) {
            log.error("Error occurred while exporting the data.",e);
            return Response.status(Status.BAD_REQUEST).build();
        }
        String exportType = (String) params.get("exportType");
        String zqlQuery = (String) params.get("zqlQuery");
        Integer startIndex = params.get("offset") != null ? Integer.parseInt((String) params.get("offset")) : 0;
        String expand = (String) params.get("expand");
        Boolean maxAllowedResult = Boolean.valueOf((String) params.get("maxAllowedResult"));
        List<String> scheduleList = (List<String>) params.get("executions");

        //If its RSS, than return with zqlQuery
        if (StringUtils.equalsIgnoreCase(exportType, "rss")) {
            JSONObject ob = new JSONObject();
            try {
                ApplicationProperties applicationProperties = (ApplicationProperties) ZephyrComponentAccessor.getInstance().getComponent("applicationProperties");
                String fileUrl = applicationProperties.getBaseUrl() + "/plugins/servlet/export/exportAttachment?exportType=rss&zql=" + zqlQuery;
                ob.put("url", fileUrl);
                return Response.ok(ob.toString()).build();
            } catch (JSONException e) {
                log.warn("Error exporting file", e);
                return Response.status(Status.SERVICE_UNAVAILABLE).build();
            }
        }
        String queryForSelectedSchedules = formQueryForSelectedSchedules(scheduleList);
        if (StringUtils.isNotBlank(queryForSelectedSchedules)) {
            int orderByIndex = zqlQuery.indexOf("ORDER BY");
            String updatedZqlQuery = StringUtils.substringBefore(zqlQuery, "ORDER BY");
            if (orderByIndex != -1) {
                if (StringUtils.isNotBlank(updatedZqlQuery)) {
                    updatedZqlQuery += " AND ";
                }
                updatedZqlQuery += queryForSelectedSchedules;
                updatedZqlQuery += StringUtils.substring(zqlQuery, orderByIndex, zqlQuery.length());
            } else {
                if (StringUtils.isNotBlank(zqlQuery)) {
                    updatedZqlQuery += " AND ";
                }
                updatedZqlQuery += queryForSelectedSchedules;
            }
            zqlQuery = updatedZqlQuery;
        }
        //Refactored and Moved code to Helper
        ScheduleSearchResourceHelper searchResourceHelper = new ScheduleSearchResourceHelper(authContext.getLoggedInUser(), searchService, exportService, issueManager, cycleManager, versionManager, testStepManager, stepResultManager,folderManager,zephyrCustomFieldManager);
        return searchResourceHelper.exportExecutions(exportType, zqlQuery, startIndex, expand, maxAllowedResult);
    }


    /**
     * validates and executes search against zephyr indexes. offset and limit provides a way to define the beginning and the max limit allowed
     *
     * @param zqlQuery
     * @param offset
     * @param executionId
     * @param expand
     * @return
     */
    @ApiOperation(value = "Navigate Execution", notes = "Validates and executes search against zephyr indexes. offset and limit provides a way to define the beginning and the max limit allowed")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"status\":{\"1\":{\"id\":1,\"color\":\"#75B000\",\"description\":\"Test was executed and passed successfully.\",\"name\":\"PASS\"},\"2\":{\"id\":2,\"color\":\"#CC3300\",\"description\":\"Test was executed and failed.\",\"name\":\"FAIL\"},\"3\":{\"id\":3,\"color\":\"#F2B000\",\"description\":\"Test execution is a work-in-progress.\",\"name\":\"WIP\"},\"4\":{\"id\":4,\"color\":\"#6693B0\",\"description\":\"The test execution of this test was blocked for some reason.\",\"name\":\"BLOCKED\"},\"5\":{\"id\":5,\"color\":\"#990099\",\"description\":\"\",\"name\":\"PENDING\"},\"6\":{\"id\":6,\"color\":\"#996633\",\"description\":\"\",\"name\":\"APPROVED\"},\"7\":{\"id\":7,\"color\":\"#ff3366\",\"description\":\"\",\"name\":\"12\"},\"-1\":{\"id\":-1,\"color\":\"#A0A0A0\",\"description\":\"The test has not yet been executed.\",\"name\":\"UNEXECUTED\"}},\"execution\":{\"id\":10373,\"orderId\":9991,\"executionStatus\":\"-1\",\"comment\":\"\",\"htmlComment\":\"\",\"cycleId\":-1,\"cycleName\":\"Ad hoc\",\"versionId\":10401,\"versionName\":\"szdfxgcvhbjjjj\",\"projectId\":10100,\"createdBy\":\"vm_admin\",\"modifiedBy\":\"vm_admin\",\"issueId\":12158,\"issueKey\":\"SONY-1819\",\"summary\":\"SONY Project\",\"label\":\"\",\"component\":\"\",\"projectKey\":\"SONY\",\"executionDefectCount\":0,\"stepDefectCount\":0,\"totalDefectCount\":0,\"projectName\":\"SONY\",\"projectAvatarId\":10011},\"offset\":\"139\",\"prevExecutionId\":\"10374\",\"nextExecutionId\":\"10372\"}")})
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/navigator/{id}")
    public Response getExecutionNavigator(@QueryParam("zql") final String zqlQuery,
                                          @QueryParam("offset") final Integer offset, @PathParam("id") final Integer executionId,
                                          @QueryParam("expand") final String expand) {

        JSONObject jsonObject = new JSONObject();
        try {
            if (authContext.getLoggedInUser() == null) {
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (JSONException e) {
            log.error("Error occurred while exporting the data using navigator by ID.",e);
            return Response.status(Status.BAD_REQUEST).build();
        }
        ScheduleSearchResourceHelper searchResourceHelper = new ScheduleSearchResourceHelper(authContext.getLoggedInUser(), searchService);
        ParseResult parseResult = searchResourceHelper.getNewSearchQuery(zqlQuery);
        Query currentQuery = null;
        if (parseResult != null && parseResult.isValid()) {
            currentQuery = parseResult.getQuery();
        } else {
            if (parseResult.getErrors() != null && !parseResult.getErrors().getErrorMessages().isEmpty()) {
                return searchResourceHelper.parseQuery(parseResult);
            }
        }
        log.debug("Search query for get executions navigator: " + currentQuery);

        //Refactored and Moved code to Helper
        Schedule schedule = scheduleManager.getSchedule(executionId);
        if (schedule == null) {
            String errorMessage = authContext.getI18nHelper().getText("zephyr.common.error.invalid", new Object[]{SCHEDULE_ENTITY, executionId});
            return populateErrorMsg(errorMessage);
        }
        Project project = projectManager.getProjectObj(schedule.getProjectId());
        if (project == null) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "projectId ", String.format(ID,schedule.getProjectId()));
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "Invalid Project", errorMessage, errorMessage);
        }  
        boolean hasPermission = JiraUtil.hasBrowseProjectPermission(project, authContext.getLoggedInUser());
        if (!hasPermission) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.project.permission.error", "Execution", String.valueOf(schedule.getProjectId()));
            log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(), Status.FORBIDDEN,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "Insufficient Project permissions", errorMessage, errorMessage);
        }
        JiraUtil.setProjectThreadLocal(project);
        return scheduleResourceDelegate.getExecutionNavigator(currentQuery, offset, schedule, expand);
    }


    /**
     * validates and executes search against zephyr indexes. offset and limit provides a way to define the beginning and the max limit allowed
     *
     * @param executionReorderRequest
     * @return
     */
    @ApiOperation(value = "Re Order Execution", notes = "Re Order Execution ")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "request", value = "{\"cycleId\":\"1\",\"executionReorders\":[{\"executionId\":2,\"oldOrderId\":2,\"newOrderId\":1},{\"executionId\":1,\"oldOrderId\":1,\"newOrderId\":2}],\"versionId\":-1}"),
            @ApiImplicitParam(name = "response", value = "{\"success\":\"All execution(s) were successfully updated.\"}")
    })
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/reorder")
    public Response reorderExecution(final ExecutionReorderRequest executionReorderRequest) {
        JSONObject jsonResponse = new JSONObject();

        try {
            if (authContext.getLoggedInUser() == null) {
                jsonResponse.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                return Response.status(Status.UNAUTHORIZED).entity(jsonResponse.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (JSONException e) {
            log.error("Error occurred while reorder.",e);
            return Response.status(Status.BAD_REQUEST).build();
        }

        JSONObject jsonErrorObject = zapiValidationService.validateEntity(executionReorderRequest.cycleId, "CYCLE_ID");
        if (jsonErrorObject != null) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,jsonErrorObject.toString()));
            return Response.status(Status.BAD_REQUEST).entity(jsonErrorObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
        }

        jsonErrorObject = zapiValidationService.validateEntity(executionReorderRequest.versionId, "VERSION_ID");
        if (jsonErrorObject != null) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,jsonErrorObject.toString()));
            return Response.status(Status.BAD_REQUEST).entity(jsonErrorObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
        }

        Cycle cycle = null;
        Version version = null;
        Project project = null;
        if (ApplicationConstants.AD_HOC_CYCLE_ID != executionReorderRequest.cycleId.intValue()) {
            cycle = cycleManager.getCycle(executionReorderRequest.cycleId.longValue());
            jsonErrorObject = zapiValidationService.validateEntity(cycle, "CYCLE");
            if (jsonErrorObject != null) {
                log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,jsonErrorObject.toString()));
                return Response.status(Status.BAD_REQUEST).entity(jsonErrorObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
            project = projectManager.getProjectObj(cycle.getProjectId());
        } else {
            if (ApplicationConstants.UNSCHEDULED_VERSION_ID != executionReorderRequest.versionId.intValue()) {
                version = ComponentAccessor.getVersionManager().getVersion(executionReorderRequest.versionId.longValue());

                jsonErrorObject = zapiValidationService.validateEntity(version, "VERSION");
                if (jsonErrorObject != null) {
                    log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,jsonErrorObject.toString()));
                    return Response.status(Status.BAD_REQUEST).entity(jsonErrorObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
                }
                project = version.getProject();
            }
        }
        if(project != null)
        	JiraUtil.setProjectThreadLocal(project);
        return scheduleResourceDelegate.reorderExecution(executionReorderRequest,cycle,version);
    }

    /**
     * Gets all schedules available for given Issue Id
     * @param params
     * @return response
     */
    @ApiOperation(value = "Get Execution Summaries By Sprint And Issue", notes = "Gets all execution available for given Issue Id")
    @ApiImplicitParams({ @ApiImplicitParam(name = "request", value = "{\"sprintId\":1}"),
            @ApiImplicitParam(name = "response", value = "{\"successful\":{\"10008\":{\"totalExecutions\":2,\"action\":\"collapse\",\"totalExecuted\":1,\"totalOpenDefectCount\":0,\"executionSummaries\":{\"executionSummary\":[{\"count\":1,\"statusKey\":-1,\"statusName\":\"UNEXECUTED\",\"statusColor\":\"#A0A0A0\",\"statusDescription\":\"The test has not yet been executed.\"},{\"count\":0,\"statusKey\":1,\"statusName\":\"PASSED\",\"statusColor\":\"#75B000\",\"statusDescription\":\"Test was executed and passed successfully.(edited)\"},{\"count\":0,\"statusKey\":2,\"statusName\":\"FAIL\",\"statusColor\":\"#CC3300\",\"statusDescription\":\"Test was executed and failed.\"},{\"count\":1,\"statusKey\":3,\"statusName\":\"WIP\",\"statusColor\":\"#F2B000\",\"statusDescription\":\"Test execution is a work-in-progress.\"},{\"count\":0,\"statusKey\":4,\"statusName\":\"BLOCKED\",\"statusColor\":\"#6693B0\",\"statusDescription\":\"The test execution of this test was blocked for some reason.\"},{\"count\":0,\"statusKey\":5,\"statusName\":\"CANCEL\",\"statusColor\":\"#ff33ff\",\"statusDescription\":\"It will cancel the test.\"}]},\"totalDefectCount\":0}}}")})
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/executionSummariesBySprintAndIssue")
    @AnonymousAllowed
    public Response getExecutionSummariesBySprintAndIssue(final Map<String,Object> params)  {

        JSONObject jsonResponse = new JSONObject();
        try {
            if (authContext.getLoggedInUser() == null) {
                jsonResponse.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                return Response.status(Status.UNAUTHORIZED).entity(jsonResponse.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (JSONException e) {
            log.error("Error occurred while getting execution summary by sprint and issue.",e);
            return Response.status(Status.BAD_REQUEST).build();
        }
        
        String issueIdOrKey = params.get("issueIdOrKeys") != null ? String.valueOf(params.get("issueIdOrKeys")) : null;
        String inputSprintId = params.get("sprintId") != null ? String.valueOf(params.get("sprintId")) : null;
        if(StringUtils.isBlank(inputSprintId)) {
        	inputSprintId = null;
            Map<String, String> errorMap = new HashMap<String, String>();
            errorMap.put("Invalid Sprint", authContext.getI18nHelper().getText("zephyr.common.error.invalid", "SprintId", inputSprintId));
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("zephyr.common.error.invalid", "SprintId", inputSprintId)));
            return Response.status(Status.BAD_REQUEST).entity(errorMap).cacheControl(ZephyrCacheControl.never()).build();
        }

        String[] issueIdOrKeys = issueIdOrKey != null ? issueIdOrKey.split(",") : new String[0];
        String[] sprintIds = inputSprintId.split(",");
        
        ScheduleResourceHelper scheduleHelper = new ScheduleResourceHelper(issueManager,searchProvider,authContext,scheduleManager,scheduleIndexManager,
        		cycleManager,versionManager,searchService,zephyrClauseHandlerFactory,zfjCacheService,sprintService, zephyrCustomFieldManager);
       try {
	        return scheduleHelper.getExecutionSummariesBySprintAndIssue(issueIdOrKeys,sprintIds);
       } catch(Exception e) {
           log.error("Error retrieving Execution Summary",e);
           return buildErrorMessage(authContext.getI18nHelper().getText("execute.test.execution.navigator.error.label"));
       }
    }


    /**
     * Gets all schedules available for given Issue Id
     * @param issueIdOrKey
     * @param action
     * @param offset
     * @param maxRecords
     * @param expand
     * @return Response
     */
    @ApiOperation(value = "Get Execution Summary by Issue", notes = "Gets all execution available for given Issue Id")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "[{\"id\":127,\"orderId\":127,\"cycleId\":3,\"cycleName\":\"cycle\\\\\\\\\", \"issueId\": \"10027\", \"issueKey\": \"SAM-28\", \"issueSummary\": \"Test\", \"labels\": [\"123\"], \"issueDescription\": \"\", \"projectKey\": \"SAM\", \"projectId\": 10000, \"project\": \"Samsung\", \"projectAvatarId\": 10324, \"priority\": \"Medium\", \"components\": [ { \"name\": \"Component2\", \"id\": 10001 }]}]")})
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/executionsByIssue")
    @AnonymousAllowed
    public Response getExecutionsByIssue(@QueryParam("issueIdOrKey") String issueIdOrKey,
            @QueryParam("action") String action,
            @QueryParam("offset") Integer offset,
            @QueryParam("maxRecords") Integer maxRecords,
            @QueryParam("expand") String expand)  {

        JSONObject jsonResponse = new JSONObject();

        try {
            if (authContext.getLoggedInUser() == null) {
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                jsonResponse.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                return Response.status(Status.UNAUTHORIZED).entity(jsonResponse.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (JSONException e) {
            log.error("Error occurred while getting executions by issue.",e);
            return Response.status(Status.BAD_REQUEST).build();
        }

        JSONObject errorJsonObject = zapiValidationService.validateEntityStr(issueIdOrKey, "Input Parameter");
        if (errorJsonObject != null) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,errorJsonObject.toString()));
            return Response.status(Status.BAD_REQUEST).entity(errorJsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
        }

        MutableIssue issue = null;
        if (Pattern.matches(".*[a-zA-Z]+.*", issueIdOrKey)) {
        	issue = issueManager.getIssueObject(issueIdOrKey);
        } else {
        	issue = issueManager.getIssueObject(Long.valueOf(issueIdOrKey));
        }

        if (issue == null) {
            errorJsonObject = zapiValidationService.validateEntity(issue, "Story/Issue");
            if (errorJsonObject != null) {
                log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,errorJsonObject.toString()));
                return Response.status(Status.BAD_REQUEST).entity(errorJsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        }

        JiraUtil.setProjectThreadLocal(issue.getProjectObject());
        String cacheKey = "ExecutionsByIssue" + ":" + authContext.getLoggedInUser().getKey()+ ":" + issue.getKey();
        action = StringUtils.isBlank(action) ? "expand" : action;
        //If Action is Collapse, Just update the cache and return.
    	zfjCacheService.createOrUpdateCache("ExecutionsByIssue" + ":" + authContext.getLoggedInUser().getKey()+ ":" + issue.getKey(),action);
        
        //return if action is collapse without any data fetch
        if(StringUtils.equals(action, "collapse")) {
        	ZQLSearchResultBean resultBean = new ZQLSearchResultBean();
	    	List<ZQLScheduleBean> schedules = new ArrayList<ZQLScheduleBean>();
	    	resultBean.setExecutions(schedules);
			return Response.ok(resultBean).cacheControl(ZephyrCacheControl.never()).build();
        }
        return scheduleResourceDelegate.getExecutionsByIssue(issue,offset,maxRecords,expand);
    }

    @ApiOperation(value = "Get Executions count for cycles by given project id and version id", notes = "Get Executions count for cycles by given project id and version id")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"Ad hoc:--1\":{\"UNEXECUTED\":0,\"PASS\":0,\"FAIL\":0,\"WIP\":0,\"BLOCKED\":0},\"Another:-12\":{\"UNEXECUTED\":0,\"PASS\":0,\"FAIL\":0,\"WIP\":0,\"BLOCKED\":0},\"Simon:-13\":{\"UNEXECUTED\":0,\"PASS\":0,\"FAIL\":0,\"WIP\":0,\"BLOCKED\":0}}")})
    @GET
    @Path("/executionsStatusCountForCycleByProjectIdAndVersion")
    @Produces(MediaType.APPLICATION_JSON)
    @AnonymousAllowed
    public Response getExecutionsStatusCountForCycleByProjectIdAndVersion(@QueryParam("projectId") Long projectId,
                                                                          @QueryParam("versionId") Long versionId, @QueryParam("components") String components,
                                                                          @QueryParam("offset") Integer offset, @QueryParam("limit") final Integer limit)  {

        if(null == components) {
            components = "-1";
        }
        return scheduleResourceDelegate.getExecutionsStatusCountForCycleByProjectIdAndVersion(projectId,versionId,components,offset,limit);
    }

    @ApiOperation(value = "Get Executions count for given cycle", notes = "Get Executions count for given cycle")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "[{\"statusName\":\"UNEXECUTED\",\"statusCount\":1,\"statusColor\":\"#A0A0A0\"},{\"statusName\":\"PASS\",\"statusCount\":4,\"statusColor\":\"#75B000\"},{\"statusName\":\"FAIL\",\"statusCount\":1,\"statusColor\":\"#CC3300\"},{\"statusName\":\"WIP\",\"statusCount\":1,\"statusColor\":\"#F2B000\"},{\"statusName\":\"BLOCKED\",\"statusCount\":1,\"statusColor\":\"#6693B0\"}]")})
    @GET
    @Path("/executionsStatusCountByCycle")
    @Produces(MediaType.APPLICATION_JSON)
    @AnonymousAllowed
    public Response getExecutionsStatusCountPerCycle(@QueryParam("projectId") Long projectId,
                                                     @QueryParam("versionId") Long versionId,
                                                     @QueryParam("cycles") String cycles,@QueryParam("folders") String folders,@QueryParam("offset") Integer offset,
                                                     @QueryParam("limit") final Integer limit)  {

        return scheduleResourceDelegate.getExecutionsStatusCountPerCycleAndFolder(projectId,versionId,cycles,folders);
    }

    @ApiOperation(value = "Get Executions count per assignee for given cycle", notes = "Get Executions count per assignee for given cycle")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"Admin\":{\"UNEXECUTED\":0,\"PASS\":2,\"FAIL\":0,\"WIP\":0,\"BLOCKED\":1}}")})
    @GET
    @Path("/executionsStatusCountPerAssigneeForCycle")
    @Produces(MediaType.APPLICATION_JSON)
    @AnonymousAllowed
    public Response getExecutionsStatusCountPerAssigneeForCycle(@QueryParam("projectId") Long projectId,
                                                                @QueryParam("versionId") Long versionId, @QueryParam("cycles") String cycles)  {
        JSONObject jsonResponse = new JSONObject();
        try {
            if (authContext.getLoggedInUser() == null) {
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                jsonResponse.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                return Response.status(Status.UNAUTHORIZED).entity(jsonResponse.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (JSONException e) {
            log.error("Error occurred while getting execution status count by assignee.", e);
            return Response.status(Status.BAD_REQUEST).build();
        }
        return scheduleResourceDelegate.getExecutionsStatusByAssignee(projectId,versionId,cycles);
    }
    
    @ApiOperation(value = "Get execution total count", notes = "Get execution total count")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"totalCount\":\"30\"}")})
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/totalCount")
    public Response getExecutionTotalCount(@QueryParam("projectId") Long projectId,
                                      @QueryParam("versionId") Long versionId,
                                      @QueryParam("cycleId") Integer cycleId,
                                      @QueryParam("folderId") Long folderId)  {
        final ApplicationUser user = authContext.getLoggedInUser();
        JSONObject jsonObject = new JSONObject();
        try {
            if (user == null && !JiraUtil.hasAnonymousPermission(user)) {
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (JSONException e) {
            log.error("Error occurred while getting execution count.",e);
            return Response.status(Status.BAD_REQUEST).build();
        }
        
        Project project = projectManager.getProjectObj(projectId);
        if (project == null) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "projectId ", "");
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "Invalid Project", errorMessage, errorMessage);
        }        
        JiraUtil.setProjectThreadLocal(project);
        boolean hasPermission = JiraUtil.hasBrowseProjectPermission(project, authContext.getLoggedInUser());
        if (!hasPermission) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.project.permission.error", "Execution", String.valueOf(projectId));
            log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(), Status.FORBIDDEN,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "Insufficient Project permissions", errorMessage, errorMessage);
        }
        if(!(versionId.intValue() == ApplicationConstants.UNSCHEDULED_VERSION_ID)) {
        	Version version = versionManager.getVersion(versionId);
			if(Objects.isNull(version)) {
				return buildErrorMessage(Status.BAD_REQUEST,authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Version", String.valueOf(versionId)));
			}
        }        
        if(cycleId != null && cycleId.intValue() != ApplicationConstants.AD_HOC_CYCLE_ID) {
        	Cycle cycle = cycleManager.getCycle(cycleId.longValue()); 
        	if(cycle == null) {
        		return buildErrorMessage(Status.BAD_REQUEST,authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Cycle", String.valueOf(cycleId)));
        	}
        }
        if(folderId != null) {
        	Folder folder = folderManager.getFolder(folderId);
        	if(folder == null) {
        		return buildErrorMessage(Status.BAD_REQUEST,authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Folder", String.valueOf(folderId)));
        	}
        }
        try {
        	Integer totalCount =  scheduleResourceDelegate.getExecutionTotalCount(projectId, versionId, cycleId, folderId);
        	jsonObject.put("totalCount", totalCount);
            return Response.ok().entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
        } catch (Exception ex) {
        	log.error(String.format(ERROR_LOG_MESSAGE,Status.INTERNAL_SERVER_ERROR.getStatusCode(),Status.INTERNAL_SERVER_ERROR, ex.toString()));
            return Response.status(Status.INTERNAL_SERVER_ERROR).cacheControl(ZephyrCacheControl.never()).build();
        }
    }
        	
    
    @ApiOperation(value = "Get Executions count for past 5 days from current system date", notes = "Get Executions count for past 5 days from system date. User can configure it for 5/10/15 days.")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"Admin\":{\"UNEXECUTED\":0,\"PASS\":2,\"FAIL\":0,\"WIP\":0,\"BLOCKED\":1,\"_EX_NOCYCLE\":0,_EX_CYCLE\":2,\"_EX_TOTAL\":2}}")})
    @GET
    @Path("/pastExecutionsStatusCount")
    @Produces(MediaType.APPLICATION_JSON)
    @AnonymousAllowed
    public Response getPastExecutionsStatusCount(@QueryParam("projectId") Long projectId,
															@QueryParam("howMany") String howMany,
												            @QueryParam("versionId") String versionId,
												            @QueryParam("cycleIds") String cycleIds)  {
        JSONObject jsonResponse = new JSONObject();
        try {
            if (authContext.getLoggedInUser() == null) {
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                jsonResponse.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                return Response.status(Status.UNAUTHORIZED).entity(jsonResponse.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (JSONException e) {
            log.error("Error occurred while getting execution status count for past 10 dates.", e);
            return Response.status(Status.BAD_REQUEST).build();
        }
        Project project = projectManager.getProjectObj(projectId);
        //Response will be an error message if user does not have browse permission
        Response response = hasBrowseProjectPermission(project);
        if(response != null) {
            return response;
        }

        if (project == null) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "projectId ", "");
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "Invalid Project", errorMessage, errorMessage);
        }
        JiraUtil.setProjectThreadLocal(project);
        return scheduleResourceDelegate.getPastExecutionsStatusCount(projectId,howMany,cycleIds,versionId);
    }
    
    @ApiOperation(value = "Get Execution Workflow Time Tracking per cycle", notes = "Get Execution Workflow Time Tracking per cycle")
    @GET
    @Path("/executionsTimeTrackingByCycle")
    @Produces(MediaType.APPLICATION_JSON)
    @AnonymousAllowed
    public Response getExecutionsWorkflowTimePerCycle(@QueryParam("projectId") Long projectId,
                                                     @QueryParam("versionId") Long versionId,
                                                     @QueryParam("cycles") String cycles,@QueryParam("folders") String folders,@QueryParam("offset") Integer offset,
                                                     @QueryParam("limit") final Integer limit)  {

        JSONObject jsonResponse = new JSONObject();
        try {
            if (authContext.getLoggedInUser() == null) {
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                jsonResponse.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                return Response.status(Status.UNAUTHORIZED).entity(jsonResponse.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (JSONException e) {
            log.error("Error occurred while getting execution status count for past 10 dates.", e);
            return Response.status(Status.BAD_REQUEST).build();
        }
        Project project = projectManager.getProjectObj(projectId);
        //Response will be an error message if user does not have browse permission
        Response response = hasBrowseProjectPermission(project);
        if(response != null) {
            return response;
        }

        if (project == null) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "projectId ", "");
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "Invalid Project", errorMessage, errorMessage);
        }
        JiraUtil.setProjectThreadLocal(project);
        return scheduleResourceDelegate.getExecutionsTimeTrackingPerCycleAndFolder(projectId,versionId,cycles,folders);
    }

    @ApiOperation(value = "Get Index Count on Request Receiving Node", notes = "Get Index Count on Request Receiving Node")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"indexCount\":\"30\",\"databaseCount\":\"30\"}")})
    @GET
    @Path("/indexCount")
    @Produces(MediaType.APPLICATION_JSON)
    @AnonymousAllowed
    public Response getIndexCount()  {
        JSONObject resultMap  = new JSONObject();
        try {
            int count = scheduleManager.getTotalSchedulesCount();
            resultMap.put(ApplicationConstants.DATABASE_COUNT, count);
            ScheduleSearchResourceHelper searchResourceHelper = new ScheduleSearchResourceHelper(authContext.getLoggedInUser(), searchService);
            Query countQuery = searchResourceHelper.getNewSearchQuery("").getQuery();
            long searchScheduledCount = searchService.searchCountByPassSecurity(countQuery,authContext.getLoggedInUser());
            resultMap.put(ApplicationConstants.INDEX_COUNT, searchScheduledCount);
            if(nodeStateManager.isClustered()){
                resultMap.put(ApplicationConstants.CURRENT_NODE_ID, nodeStateManager.getNode().getNodeId());
                resultMap.put(ApplicationConstants.CURRENT_NODE_IP, nodeStateManager.getNode().getIp());
            }
            return Response.ok().entity(resultMap.toString()).cacheControl(ZephyrCacheControl.never()).build();
        } catch (Exception e) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.INTERNAL_SERVER_ERROR.getStatusCode(),Status.INTERNAL_SERVER_ERROR, e.toString()));
            return Response.status(Status.INTERNAL_SERVER_ERROR).cacheControl(ZephyrCacheControl.never()).build();
        }
    }
    
    private Response populateErrorMsg(String errorMessage) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("error", errorMessage);
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorMessage));
        } catch (JSONException e) {
            log.warn("Error creating JSON Object", e);
        }
        return Response.status(Status.BAD_REQUEST).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
    }


    private void removeRemoteLinks(Collection<Integer> deletedDefects, String scheduleId) {
        for (Integer issueId : deletedDefects) {
            Issue issue = issueManager.getIssueObject(new Long(issueId));
            if (null != issue)
                rilManager.removeRemoteIssueLinkByGlobalId(issue, scheduleId, authContext.getLoggedInUser());
        }
    }

    /**
     * Utility method to form ZQLQuery for selected schedules.
     *
     * @param schedules : selected schedules in GUI
     * @return query : for instance for given selected schedules [11, 12, 13, 14],
     * resultant query would be <code>" AND SCHEDULE IN ( 11, 12, 13, 14)"</code>
     */
    private String formQueryForSelectedSchedules(List<String> schedules) {
        if (null != schedules && !schedules.isEmpty()) {

            return new StringBuilder(" EXECUTION IN (")
                    .append(StringUtils.join(schedules, " , ")).append(")").toString();
        }
        return null;
    }


    /**
     * To handle empty strings sent by ZAPI API
     *
     * @param scheduleIds
     * @return
     */
    private boolean removeNullSchedules(
            Collection<Integer> scheduleIds) {
        boolean nullPresent = scheduleIds.removeAll(Collections.singleton(null));
        return nullPresent;
    }

    /**
     * Populate Change Property Table for Execution
     *
     * @param testStatus
     * @param schedule
     * @param changePropertyTable
     */
    private void populateChangePropertyTable(String testStatus,
                                             Schedule schedule, Table<String, String, Object> changePropertyTable) {
        changePropertyTable.put("STATUS", ApplicationConstants.OLD, String.valueOf(schedule.getStatus()));
        changePropertyTable.put("STATUS", ApplicationConstants.NEW, testStatus);
        changePropertyTable.put("EXECUTED_ON", ApplicationConstants.OLD, null != schedule.getExecutedOn() ? schedule.getExecutedOn().toString() : ApplicationConstants.NULL);
        changePropertyTable.put("EXECUTED_ON", ApplicationConstants.NEW, String.valueOf(System.currentTimeMillis()));
        changePropertyTable.put("EXECUTED_BY", ApplicationConstants.OLD, StringUtils.isEmpty(schedule.getExecutedBy()) ? ApplicationConstants.NULL : schedule.getExecutedBy());
        changePropertyTable.put("EXECUTED_BY", ApplicationConstants.NEW, UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
    }


    @SuppressWarnings("unchecked")
    private Response formBulkDeleteResponse(List<Integer> scheduleIds, List<Integer> noPermissionExecutions,
                                            Collection<String> noJiraPermission, Schedule[] schedules, int rows) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("success", "-");
            jsonObject.put("error", "-");
            final Collection<String> schedList = CollectionUtils.collect(Arrays.asList(schedules), new Transformer() {
                @Override
                public String transform(final Object input) {
                    if (input == null) {
                        return null;
                    }
                    return String.valueOf(((Schedule) input).getID());
                }
            });
            if (rows > 0 && rows == scheduleIds.size()) {
                jsonObject.put("success", StringUtils.join(Arrays.asList(schedList), ","));
            }
            if (rows != scheduleIds.size()) {
                scheduleIds.removeAll(schedList);
                Collections.sort(noPermissionExecutions);
                scheduleIds.removeAll(noPermissionExecutions);
                jsonObject.put("success", StringUtils.join(schedList, ","));
                jsonObject.put("error", StringUtils.join(scheduleIds, ","));
                jsonObject.put("noPermission", StringUtils.join(noPermissionExecutions, ","));
                jsonObject.put("noIssuePermission", StringUtils.join(noJiraPermission, ","));
            }
            return Response.ok().entity(jsonObject.toString()).build();
        } catch (JSONException e) {
            log.error("Error Constructing JSON Response", e);
        }
        return Response.ok().build();
    }

    private Response buildLoginErrorResponse() {
        String loginUrl = ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL) + "/login.jsp";
        StringBuilder sb = new StringBuilder(authContext.getI18nHelper().getText("login.required.notloggedin.permissionviolation"));
        sb.append("<a href=\"");
        sb.append(loginUrl);
        sb.append("\"> ");
        sb.append(authContext.getI18nHelper().getText("login.required.title"));
        sb.append("<a>.");
		return buildErrorMessage(Status.FORBIDDEN, sb.toString());
    }

    private Response buildErrorMessage(String errorMessage) {
        log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,errorMessage));
        return buildErrorMessage(Status.BAD_REQUEST, errorMessage);
    }

	private Response buildErrorMessage(Status status, String errorMessage) {
		JSONObject errorJsonObject = new JSONObject();
		try {
			errorJsonObject.put("error", errorMessage);
		} catch (JSONException e) {
			log.error("Error constructing JSON", e);
		}
		return Response.status(status).entity(errorJsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
	}

    @SuppressWarnings("unchecked")
    private Collection<String> transformSchedulestoString(
            Collection<String> schedules) {
        final Collection<String> scheduleIds = CollectionUtils.collect(schedules, new Transformer() {
            @Override
            public String transform(final Object input) {
                if (StringUtils.isBlank(String.valueOf(input))) {
                    return null;
                }
                final String scheduleId = String.valueOf(input);
                return scheduleId;
            }
        });
        return scheduleIds;
    }


	private boolean verifyBulkPermissions(Schedule schedule,ApplicationUser user) {
		//Check ZephyrPermission and update response to include execution per project permissions
		ProjectPermissionKey cyclePermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_BROWSE_CYCLE.toString());
		ProjectPermissionKey executionPermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_EDIT_EXECUTION.toString());
		Collection<ProjectPermissionKey> projectPermissionKeys = new ArrayList<ProjectPermissionKey>();
		projectPermissionKeys.add(executionPermissionKey);
		projectPermissionKeys.add(cyclePermissionKey);
		boolean loggedInUserHasZephyrPermission = zephyrPermissionManager.validateUserPermissions(projectPermissionKeys, null, user ,schedule.getProjectId());
		return loggedInUserHasZephyrPermission;
	}

    private Response hasBrowseProjectPermission(Project project) {
        boolean hasPermission = JiraUtil.hasBrowseProjectPermission(project, authContext.getLoggedInUser());
        if (!hasPermission) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.project.permission.error", "Execution", String.valueOf(project.getName()));
            log.error(String.format(ERROR_LOG_MESSAGE, Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "Insufficient Project permissions", errorMessage, errorMessage);
        }
        return null;
    }

    @XmlRootElement
    public static class ExecutionReorderRequest {
        @XmlElement(nillable = false)
        public Integer cycleId;

        @XmlElement
        public Integer versionId;

        @XmlElement(nillable = false)
        public List<ExecutionReorder> executionReorders;
    }

    public static class ExecutionReorder {
        @XmlElement(nillable = false)
        public Integer executionId;

        @XmlElement(nillable = false)
        public Integer oldOrderId;

        @XmlElement(nillable = false)
        public Integer newOrderId;
    }

    /**
     * Object to jsonNode
     * @param object
     * @return
     */
    private JsonNode  toJson(Object object){
        org.codehaus.jackson.map.ObjectMapper mapper = new org.codehaus.jackson.map.ObjectMapper();
        JsonNode jsonNode = mapper.convertValue(object, JsonNode.class);
        return jsonNode;
    }

    /**
     *
     * @param schedule
     * @param oldUserKey
     */
    private void logAuditData(Schedule schedule, String assigneeUserKey, String oldUserKey) {
        Table<String, String, Object> changePropertyTable = HashBasedTable.create();
        changePropertyTable.put("ASSIGNED_TO", ApplicationConstants.OLD, StringUtils.isEmpty(oldUserKey) ? ApplicationConstants.NULL : oldUserKey);
        changePropertyTable.put("ASSIGNED_TO", ApplicationConstants.NEW, StringUtils.isEmpty(assigneeUserKey) ? ApplicationConstants.NULL : assigneeUserKey);
        eventPublisher.publish(new ScheduleModifyEvent(schedule, changePropertyTable, EventType.EXECUTION_UPDATED,
                UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));
    }


    /**
     * Return Error Response
     * @param jobProgressToken
     * @param errorMessage
     * @return
     */
    private Response getErrorResponse(String jobProgressToken, String errorMessage) {
        jobProgressService.setErrorMessage(jobProgressToken, errorMessage);
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("conflict",errorMessage);
            Response response = Response.ok().entity(jsonObject.toString()).build();
            jobProgressService.setMessage(jobProgressToken, response.getEntity().toString());
            JSONObject responseJsonObject = new JSONObject();
            responseJsonObject.put(ApplicationConstants.JOB_PROGRESS_TOKEN,jobProgressToken);
            return Response.ok(responseJsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
        } catch (JSONException e) {
            e.printStackTrace();
            log.error("Error Constructing JSON Response",e);
        }
        return null;
    }
}