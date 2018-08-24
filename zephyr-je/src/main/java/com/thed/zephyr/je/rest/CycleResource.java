package com.thed.zephyr.je.rest;


import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.usercompatibility.UserCompatibilityHelper;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.sun.jersey.spi.container.ResourceFilters;
import com.thed.zephyr.je.attachment.SessionKeys;
import com.thed.zephyr.je.model.Cycle;
import com.thed.zephyr.je.model.Folder;
import com.thed.zephyr.je.permissions.model.PermissionType;
import com.thed.zephyr.je.permissions.service.ZephyrPermissionManager;
import com.thed.zephyr.je.rest.delegate.CycleResourceDelegate;
import com.thed.zephyr.je.rest.filter.ZFJApiFilter;
import com.thed.zephyr.je.service.CycleManager;
import com.thed.zephyr.je.service.ZephyrSprintService;
import com.thed.zephyr.je.vo.SprintBean;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.ZCollectionUtils;
import com.thed.zephyr.util.ZephyrCacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.UnsupportedEncodingException;
import java.util.*;

@Api(value = "Cycle Resource API(s)", description = "Following section describes rest resources (API's) pertaining to CycleResource")
@Path("cycle")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@AnonymousAllowed
@ResourceFilters(ZFJApiFilter.class)
public class CycleResource {
    protected final Logger log = Logger.getLogger(CycleResource.class);
    private final JiraAuthenticationContext authContext;

    private static final String ERROR_LOG_MESSAGE = "[Error] [Error code: %s %s Error Message : %s]";

    private final CycleResourceDelegate cycleResourceDelegate;
    private final ProjectManager projectManager;
    private final CycleManager cycleManager;
    private final ZephyrSprintService sprintService;
    private final PermissionManager permissionManager;
    private final DateTimeFormatterFactory dateTimeFormatterFactory;
    private final ZephyrPermissionManager zephyrPermissionManager;
    
    public CycleResource(final JiraAuthenticationContext authContext, 
    		final CycleResourceDelegate cycleResourceDelegate,final ProjectManager projectManager, 
    		final CycleManager cycleManager, final ZephyrSprintService sprintService, final PermissionManager permissionManager,
    		final DateTimeFormatterFactory dateTimeFormatterFactory,ZephyrPermissionManager zephyrPermissionManager) {
        this.cycleResourceDelegate=cycleResourceDelegate;
        this.projectManager=projectManager;
        this.cycleManager=cycleManager;
        this.authContext=authContext;
        this.sprintService=sprintService;
        this.permissionManager=permissionManager;
        this.dateTimeFormatterFactory=dateTimeFormatterFactory;
        this.zephyrPermissionManager=zephyrPermissionManager;
    }

    /**
     * Read of CRUD. If cycleId -1 is passed, system returns hardcoded value
     */
    @ApiOperation(value = "Get Cycle Information", notes = "Get Cycle data by Cycle Id. If cycleId -1 is passed, system returns hardcoded cycle")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"endDate\":\"30/Jan/14\",\"description\":\"Released Cycle1\",\"versionName\":\"v1\",\"sprintId\":null,\"versionId\":10207,\"environment\":\"\",\"build\":\"\",\"createdBy\":\"vm_admin\",\"name\":\"RC1\",\"modifiedBy\":\"vm_admin\",\"id\":54,\"projectId\":10203,\"startDate\":\"04/Dec/12\"}")})
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    public Response getCycle(@PathParam("id") long cycleId) {
        final ApplicationUser user = authContext.getLoggedInUser();
        //Anonymous User pass thru
        boolean isPermissionEnabled = JiraUtil.getPermissionSchemeFlag();

        JSONObject jsonObject = new JSONObject();
        try {
            if (user == null && !JiraUtil.hasAnonymousPermission(user) && !isPermissionEnabled) {
                jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (JSONException e) {
            log.error("Error Retrieving cycle data.",e);
            return Response.status(Status.BAD_REQUEST).build();
        }
        Map<String, Object> cycleMap = new HashMap<String, Object>();
        if (cycleId == ApplicationConstants.AD_HOC_CYCLE_ID) {
            cycleMap.put("id", String.valueOf(ApplicationConstants.AD_HOC_CYCLE_ID));
            cycleMap.put("name", ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.je.cycle.adhoc"));
        } else if (cycleId == ApplicationConstants.ALL_CYCLES_ID) {
            cycleMap.put("id", String.valueOf(ApplicationConstants.ALL_CYCLES_ID));
            cycleMap.put("name", ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("je.gadget.common.cycles.all.label"));
        } else {
        	Cycle cycle = cycleManager.getCycle(cycleId);
	        if (null == cycle) {
	            final I18nHelper i18nHelper = authContext.getI18nHelper();
	            ResponseBuilder builder = Response.status(Response.Status.NOT_ACCEPTABLE);
	            builder.type(MediaType.APPLICATION_JSON);
	            builder.entity(ImmutableMap.of("Error", i18nHelper.getText("project.cycle.not.exist")));
	            log.error(String.format(ERROR_LOG_MESSAGE,Response.Status.NOT_ACCEPTABLE.getStatusCode(),Status.NOT_ACCEPTABLE,i18nHelper.getText("project.cycle.not.exist")));
	            return builder.build();
	        }
	        Project project = projectManager.getProjectObj(cycle.getProjectId());
	        // validate for a valid Project
	        if (project == null) {
	            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "projectId ", "");
                log.error(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,errorMessage));
	            return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "Invalid Project", errorMessage, errorMessage);
	        }  
	        // checking the project browse permissions
            if (!JiraUtil.hasBrowseProjectPermission(project, user)) {
                String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.project.permission.error", "Cycle", String.valueOf(project.getName()));
                log.error(String.format(ERROR_LOG_MESSAGE, Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
                return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "Insufficient Project permissions", errorMessage, errorMessage);
            }
	    	JiraUtil.setProjectThreadLocal(project);
	    	cycleMap = cycleResourceDelegate.getCycle(cycleId, cycle,authContext.getLoggedInUser());
        }
	    return Response.ok(cycleMap).build();
    }

    /**
     * Export Cycle as CSV, file is generated on fly and streamed to client
     *
     * @throws UnsupportedEncodingException
     */
    @ApiOperation(value = "Export Cycle Data", notes = "Export Cycle by Cycle Id, file is generated on fly and streamed to client ")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"url\": \"http://localhost:8722/plugins/servlet/export/exportAttachment?fileName=Cycle-RC1.csv\"}")})
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{id}/export")
    public Response exportCycleOrFolder(@PathParam("id") final Integer cycleId, @QueryParam("versionId") final Long versionId, @QueryParam("projectId") final Long projectId,
    		@DefaultValue("-1") @QueryParam("folderId") final Long folderId, @QueryParam("sorter") String sortQuery) throws UnsupportedEncodingException {
        final ApplicationUser user = authContext.getLoggedInUser();

        JSONObject jsonObject = new JSONObject();
        try {
            if (user == null) {
                jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (JSONException e) {
            log.error("Error exporting cycle data.",e);
            return Response.status(Status.BAD_REQUEST).build();
        }
    	Project project = projectManager.getProjectObj(projectId);
    	// validate for a valid Project
        if (project == null) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "projectId ", "");
            log.error(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "Invalid Project", errorMessage, errorMessage);
        }  
    	JiraUtil.setProjectThreadLocal(project);
    	return cycleResourceDelegate.exportCycleOrFolder(cycleId, versionId, project, folderId, sortQuery);
    }

    @ApiOperation(value = "Create New Cycle", notes = "Create New Cycle by given Cycle Information")
    @ApiImplicitParams({@ApiImplicitParam(name = "request",value = "{\"clonedCycleId\":\"\",\"name\":\"Create cycle unscheduled version with sprint\",\"build\":\"\",\"environment\":\"\",\"description\":\"Create cycle with sprint\",\"startDate\":\"4/Dec/12\",\"endDate\":\"30/Dec/15\",\"projectId\":\"10000\",\"versionId\":\"-1\",\"sprintId\":1}"),
    @ApiImplicitParam(name = "response", value = "{\"id\":\"54\",\"responseMessage\":\"Cycle 54 created successfully.\"}")})
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createCycle(CycleRequest cycleRequest) {
        final ApplicationUser user = authContext.getLoggedInUser();

        JSONObject jsonObject = new JSONObject();
        try {
            if (user == null) {
                jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (JSONException e) {
            log.error("Error occurred while creating new cycle.",e);
            return Response.status(Status.BAD_REQUEST).build();
        }

        //validate input.
        Map<String, String> errorMap = inputCycleDataValidation(cycleRequest, "create");
        if (errorMap.size() > 0) {
            log.error(String.format(ERROR_LOG_MESSAGE, Status.NOT_ACCEPTABLE.getStatusCode(),Status.NOT_ACCEPTABLE,errorMap));
            return buildResponseErrorMap(errorMap);
        }
        if (!StringUtils.isBlank(cycleRequest.clonedCycleId) && StringUtils.equals(cycleRequest.clonedCycleId, ApplicationConstants.AD_HOC_CYCLE_ID_AS_STRING)) {
            log.error(String.format(ERROR_LOG_MESSAGE, Status.NOT_ACCEPTABLE.getStatusCode(),Status.NOT_ACCEPTABLE,ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("project.cycle.clone.adhoc.error")));
            return buildResponseErrorMap(ImmutableMap.of("error", ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("project.cycle.clone.adhoc.error")));
        }
        // validate create clone cycle when create execution permission is missing.
        if(StringUtils.isNotBlank(cycleRequest.clonedCycleId)) {
            String errorMessage;
            JSONObject errorObject;

            if(! verifyCycleLevelExecutionOperationPermission(user,Long.valueOf(cycleRequest.projectId), PermissionType.ZEPHYR_CREATE_EXECUTION.toString())) {
                errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
                errorObject = new JSONObject();
                return constructErrorResponse(errorObject, errorMessage, Status.FORBIDDEN, null);
            }
        }

    	return cycleResourceDelegate.createCycle(cycleRequest);
    }

    @ApiOperation(value = "Update Cycle Information", notes = "Update Cycle Information")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{\"id\":\"15\",\"name\":\"Updated Cycle-1\",\"build\":\"updated build:900\",\"environment\":\"linux\",\"description\":\"this is just a test\\nand another line here \\none more\",\"startDate\":\"8/Aug/13\",\"endDate\":\"8/Aug/14\",\"versionId\":\"10000\", \"folderId\":123}"),
    @ApiImplicitParam(name = "response", value = "{\"error\":\"-\",\"success\":\"cycle  were successfully updated.\",\"noPermission\":\"-\"}")})
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateCycle(CycleRequest cycleRequest) {
        final ApplicationUser user = authContext.getLoggedInUser();

        JSONObject jsonObject = new JSONObject();
        try {
            if (user == null) {
                jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (JSONException e) {
            log.error("Error occurred while updating the cycle data.",e);
            return Response.status(Status.BAD_REQUEST).build();
        }
        //validate input.
        Map<String, String> errorMap = inputCycleDataValidation(cycleRequest, "update");
        if (errorMap.size() > 0) {
            log.error(String.format(ERROR_LOG_MESSAGE, Status.NOT_ACCEPTABLE.getStatusCode(),Status.NOT_ACCEPTABLE,errorMap));
            return buildResponseErrorMap(errorMap);
        }
        
        Cycle cycle = cycleManager.getCycle(Long.valueOf(cycleRequest.id));
        // validate for a valid cycle
        if (cycle == null) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "cycle", "");
            log.error(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,errorMessage));
            //Fix for ZFJ-2295
            errorMap = new HashMap<>();
            errorMap.put("Invalid Cycle",errorMessage);
            return Response.status(Status.BAD_REQUEST).entity(errorMap).cacheControl(ZephyrCacheControl.never()).build();
            //return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "Invalid Cycle", errorMessage, errorMessage);
        }          
    	Project project = projectManager.getProjectObj(cycle.getProjectId());
        // validate for a valid Project
        if (project == null) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "projectId", "");
            log.error(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "Invalid Project", errorMessage, errorMessage);
        }  
    	JiraUtil.setProjectThreadLocal(project);
    	return cycleResourceDelegate.updateCycle(cycleRequest);
    }

    @ApiOperation(value = "Delete Cycle", notes = "Delete Cycle by Cycle Id <p>This API returns a jobProgressToken which should be used for making the call to /rest/zapi/latest/execution/jobProgress/:jobProgressToken?type=cycle_delete_job_progress. Once the request is processed, the jobProgress will populate the message field with result.</p>")
    @ApiImplicitParams({ @ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"jobProgressToken\":\"0001491865989042-242b71effff9574-0001\"}")})
    @DELETE
    @Path("/{id}")
    public Response deleteCycle(@PathParam("id") Long cycleId, @DefaultValue("true") @QueryParam("isFolderCycleDelete") String isFolderCycleDelete) {
        final ApplicationUser user = authContext.getLoggedInUser();

        JSONObject jsonObject = new JSONObject();
        try {
            if (user == null) {
                jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (JSONException e) {
            log.error("Error occurred while deleting the cycle data.",e);
            return Response.status(Status.BAD_REQUEST).build();
        }

        try {
	        Cycle cycle = cycleManager.getCycle(cycleId);
	        if (null == cycle) {
	            jsonObject.put("error", authContext.getI18nHelper().getText("project.cycle.summary.notfound.error", cycleId));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("project.cycle.summary.notfound.error", cycleId)));
	            return Response.status(Status.BAD_REQUEST).entity(jsonObject.toString()).build();
	        }
	    	Project project = projectManager.getProjectObj(cycle.getProjectId());
	        // validate for a valid Project
	        if (project == null) {
	            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "projectId ", "");
                log.error(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,errorMessage));
	            return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "Invalid Project", errorMessage, errorMessage);
	        }  
	    	JiraUtil.setProjectThreadLocal(project);
            Response response = cycleResourceDelegate.deleteCycle(cycle, isFolderCycleDelete);
            return Response.ok(response.getEntity().toString()).cacheControl(ZephyrCacheControl.never()).build();
        } catch (JSONException e) {
            log.error("JSONException on Cycle Delete:", e);
        } catch (Exception e) {
            log.error("Error on Cycle Delete:", e);
        }
        return Response.ok().build();
    }

    @ApiOperation(value = "Get List of Cycle", notes = "Get List of Cycle by Project Id")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"-1\":[{\"-1\":{\"totalExecutions\":2,\"endDate\":\"\",\"description\":\"\",\"totalExecuted\":1,\"started\":\"\",\"versionName\":\"Unscheduled\",\"expand\":\"executionSummaries\",\"projectKey\":\"TEST\",\"versionId\":-1,\"environment\":\"\",\"build\":\"\",\"ended\":\"\",\"name\":\"Ad hoc\",\"modifiedBy\":\"\",\"projectId\":10203,\"startDate\":\"\",\"executionSummaries\":{\"executionSummary\":[]}},\"recordsCount\":1}],\"10208\":[{\"-1\":{\"totalExecutions\":1,\"endDate\":\"\",\"description\":\"\",\"totalExecuted\":0,\"started\":\"\",\"versionName\":\"v2\",\"expand\":\"executionSummaries\",\"projectKey\":\"TEST\",\"versionId\":10208,\"environment\":\"\",\"build\":\"\",\"ended\":\"\",\"name\":\"Ad hoc\",\"modifiedBy\":\"\",\"projectId\":10203,\"startDate\":\"\",\"executionSummaries\":{\"executionSummary\":[]}},\"recordsCount\":1}]}")})
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON})
    public Response getCycles(@Context HttpServletRequest req, @QueryParam("projectId") Long projectId, @QueryParam("versionId") Long versionId,
                              @QueryParam("id") Long cycleId, @QueryParam("offset") Integer offset, @QueryParam("issueId") String issueId, @QueryParam("expand") String expand) {
        final ApplicationUser user = authContext.getLoggedInUser();
        //Anonymous User pass thru
        boolean isPermissionEnabled = JiraUtil.getPermissionSchemeFlag();

        JSONObject jsonObject = new JSONObject();
        try {
            if (user == null && !JiraUtil.hasAnonymousPermission(user) && !isPermissionEnabled) {
                jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                log.error(String.format(ERROR_LOG_MESSAGE, Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (JSONException e) {
            log.error("Error occurred while getting the cycle data.",e);
            return Response.status(Status.BAD_REQUEST).build();
        }

        if ((projectId == null || projectId == 0l) && versionId != null) {
            Version version = ComponentAccessor.getVersionManager().getVersion(versionId);
            if (version != null)
                projectId = version.getProjectId();
            else
                log.error("Unable to determine PROJECT, please check input data, version:" + versionId + ", project:" + projectId);
        }
    	Project project = projectManager.getProjectObj(projectId);
        // validate for a valid Project
        if (project == null) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "projectId ", "");
            log.error(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "Invalid Project", errorMessage, errorMessage);
        } else {
            // checking the project browse permissions
            if (!JiraUtil.hasBrowseProjectPermission(project, user)) {
                String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.project.permission.error", "Cycle", String.valueOf(project.getName()));
                log.error(String.format(ERROR_LOG_MESSAGE, Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
                return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "Insufficient Project permissions", errorMessage, errorMessage);
            }
        }
    	JiraUtil.setProjectThreadLocal(project);
    	return cycleResourceDelegate.getCycles(req, versionId, cycleId, offset, issueId, expand);
    }

    @ApiOperation(value = "Move Executions to Cycle", notes = "Move Executions to Cycle by Cycle Id <p>This API returns a jobProgressToken which should be used for making the call to /rest/zapi/latest/execution/jobProgress/:jobProgressToken?type=bulk_execution_copy_move_job_progress. Once the request is processed, the jobProgress will populate the message field with result.</p>")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{\"executions\":[\"17\",\"18\"],\"projectId\":\"10000\",\"versionId\":\"10000\",\"clearStatusFlag\":false,\"clearDefectMappingFlag\":false,\"folderId\":123}"),
    @ApiImplicitParam(name = "response", value = "{\"jobProgressToken\":\"0001491865556044-242b71effff9574-0001\"}")})
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/{id}/move")
    public Response moveExecutionsToCycle(@PathParam("id") final Long cycleId, final Map<String, Object> params) {

        JSONObject jsonObject = new JSONObject();
        try {
            if (authContext.getLoggedInUser() == null) {
                jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (JSONException e) {
            log.error("Error occurred while moving executions to the cycle.",e);
            return Response.status(Status.BAD_REQUEST).build();
        }
        // checking for valid projectId
        Long projectId = ZCollectionUtils.getAsLong(params, "projectId");
    	Project project = projectManager.getProjectObj(projectId);
        boolean hasPermission = JiraUtil.hasBrowseProjectPermission(project, authContext.getLoggedInUser());
        if (!hasPermission) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zapi.execution.move.invalid.projectid");
            log.error(String.format(ERROR_LOG_MESSAGE, Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "NullProjectId", errorMessage, errorMessage);
        }
        String errorMessage;
        JSONObject errorObject;

        List<String> projectPermissionKeys = new ArrayList<String>(){{
            add(PermissionType.ZEPHYR_EDIT_CYCLE.toString());
            add(PermissionType.ZEPHYR_EDIT_EXECUTION.toString());
        }};

        if(! verifyFolderLevelOperationPermission(authContext.getLoggedInUser(),projectId, projectPermissionKeys)) {
            errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
            errorObject = new JSONObject();
            return constructErrorResponse(errorObject, errorMessage, Status.FORBIDDEN, null);
        }
    	JiraUtil.setProjectThreadLocal(project);
        Response response = cycleResourceDelegate.moveExecutionsToCycle(cycleId, params);
        return Response.status(response.getStatus()).entity(response.getEntity().toString()).cacheControl(ZephyrCacheControl.never()).build();
     }


    @ApiOperation(value = "Copy Executions to Cycle", notes = "Copy Executions to Cycle By Cycle Id <p>This API returns a jobProgressToken which should be used for making the call to /rest/zapi/latest/execution/jobProgress/:jobProgressToken?type=bulk_execution_copy_move_job_progress. Once the request is processed, the jobProgress will populate the message field with result.</p>")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{\"executions\":[\"18\",\"17\"],\"projectId\":\"10000\",\"versionId\":\"-1\",\"clearStatusFlag\":true,\"clearDefectMappingFlag\":true}"),
            @ApiImplicitParam(name = "response", value = "{\"jobProgressToken\":\"0001491865646411-242b71effff9574-0001\"}")})
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/{id}/copy")
    public Response copyExecutionsToCycle(@PathParam("id") final Long cycleId, final Map<String, Object> params) {

        JSONObject jsonObject = new JSONObject();
        try {
            if (authContext.getLoggedInUser() == null) {
                jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (JSONException e) {
            log.error("Error occurred while copying executions to the cycle.",e);
            return Response.status(Status.BAD_REQUEST).build();
        }
        // checking for valid projectId
        Long projectId = ZCollectionUtils.getAsLong(params, "projectId");
    	Project project = projectManager.getProjectObj(projectId);
    	boolean hasPermission = JiraUtil.hasBrowseProjectPermission(project, authContext.getLoggedInUser());
        if (!hasPermission) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zapi.execution.move.invalid.projectid");
            log.error(String.format(ERROR_LOG_MESSAGE, Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "NullProjectId", errorMessage, errorMessage);
        }
        String errorMessage;
        JSONObject errorObject;

        List<String> projectPermissionKeys = new ArrayList<String>(){{
            add(PermissionType.ZEPHYR_EDIT_CYCLE.toString());
            add(PermissionType.ZEPHYR_EDIT_EXECUTION.toString());
        }};

        boolean cycleLevelExecutionPermission = verifyCycleLevelExecutionOperationPermission(authContext.getLoggedInUser(),projectId,
                PermissionType.ZEPHYR_CREATE_EXECUTION.toString());
        boolean folderLevelExecutionPermission = verifyFolderLevelOperationPermission(authContext.getLoggedInUser(),projectId, projectPermissionKeys);

        if(!cycleLevelExecutionPermission && !folderLevelExecutionPermission) {
            errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
            errorObject = new JSONObject();
            return constructErrorResponse(errorObject, errorMessage, Status.FORBIDDEN, null);
        }
    	JiraUtil.setProjectThreadLocal(project);
        Response response = cycleResourceDelegate.copyExecutionsToCycle(cycleId, params);
        return Response.status(response.getStatus()).entity(response.getEntity().toString()).cacheControl(ZephyrCacheControl.never()).build();
    }

    @ApiOperation(value = "Get Cycles By Versions/Sprint", notes = "Get Cycles by Version Id, Project Id")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{\"expand\":\"executionSummaries\",\"offset\":0,\"projectId\":10000,\"sprintId\":1,\"versionId\":\"-1,10000,10001,10002,10003,10004\"}"),
    @ApiImplicitParam(name = "response",value = "[{\"id\":\"3\",\"cycles\":[{\"totalExecutions\":1,\"endDate\":\"30/Jan/14\",\"description\":\"Released Cycle1\",\"totalExecuted\":1,\"started\":\"true\",\"versionName\":\"v1\",\"sprintId\":3,\"expand\":\"executionSummaries\",\"projectKey\":\"TEST\",\"versionId\":10207,\"environment\":\"\",\"build\":\"\",\"createdBy\":\"vm_admin\",\"ended\":\"true\",\"name\":\"RC1\",\"modifiedBy\":\"vm_admin\",\"id\":54,\"projectId\":10203,\"createdByDisplay\":\"vm_admin\",\"startDate\":\"4/Dec/12\",\"executionSummaries\":{\"executionSummary\":[{\"count\":0,\"statusKey\":-1,\"statusName\":\"UNEXECUTED\",\"statusColor\":\"#A0A0A0\",\"statusDescription\":\"The test has not yet been executed.\"},{\"count\":0,\"statusKey\":1,\"statusName\":\"PASS\",\"statusColor\":\"#75B000\",\"statusDescription\":\"Test was executed and passed successfully.\"},{\"count\":0,\"statusKey\":2,\"statusName\":\"FAIL\",\"statusColor\":\"#CC3300\",\"statusDescription\":\"Test was executed and failed.\"},{\"count\":0,\"statusKey\":3,\"statusName\":\"WIP\",\"statusColor\":\"#F2B000\",\"statusDescription\":\"Test execution is a work-in-progress.\"},{\"count\":0,\"statusKey\":4,\"statusName\":\"BLOCKED\",\"statusColor\":\"#6693B0\",\"statusDescription\":\"The test execution of this test was blocked for some reason.\"},{\"count\":0,\"statusKey\":5,\"statusName\":\"PENDING\",\"statusColor\":\"#990099\",\"statusDescription\":\"\"},{\"count\":0,\"statusKey\":6,\"statusName\":\"APPROVED\",\"statusColor\":\"#996633\",\"statusDescription\":\"\"},{\"count\":1,\"statusKey\":7,\"statusName\":\"12\",\"statusColor\":\"#ff3366\",\"statusDescription\":\"\"}]}}]}]")})
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/cyclesByVersionsAndSprint")
    public Response getCyclesByVersionsAndSprint(final Map<String, Object> params) {
        final ApplicationUser user = authContext.getLoggedInUser();
        //Anonymous User pass thru
        boolean isPermissionEnabled = JiraUtil.getPermissionSchemeFlag();

        JSONObject jsonObject = new JSONObject();
        try {
            if (user == null && !JiraUtil.hasAnonymousPermission(user) && !isPermissionEnabled) {
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(), Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (JSONException e) {
            log.error("Error occurred while getting cycles by versions and sprints.",e);
            return Response.status(Status.BAD_REQUEST).build();
        }

        // checking for valid projectId
        String projectId = params.get("projectId") != null ? String.valueOf(params.get("projectId")) : null;
        String[] projectIds = projectId != null ? projectId.split(",") : new String[0];
        List<Long> projectIdList = new ArrayList<Long>();
        List<Long> zephyrPermissionErrors = new ArrayList<Long>();
        for(String project : projectIds) {
        	if(StringUtils.isNotBlank(project)) {
		        boolean hasPermission = JiraUtil.hasBrowseProjectPermission(Long.valueOf(project), authContext.getLoggedInUser());
            	ProjectPermissionKey projectPermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_BROWSE_CYCLE.toString());
            	boolean hasZephyrPermission = zephyrPermissionManager.validateUserPermission(projectPermissionKey, null, authContext.getLoggedInUser(),Long.valueOf(project));
		        if (hasPermission && hasZephyrPermission) {
		        	projectIdList.add(Long.valueOf(project));
		        } else if(!hasZephyrPermission) {
		        	zephyrPermissionErrors.add(Long.valueOf(project));
		        }
        	}
        }
        if(projectIdList.isEmpty() && !zephyrPermissionErrors.isEmpty()) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
            log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
            return JiraUtil.getPermissionDeniedErrorResponse(errorMessage);
        }
        
    	return cycleResourceDelegate.getCyclesByVersionsAndSprint(params,projectIdList);
    }

    @ApiOperation(value = "Clean Up Sprint From Cycle", notes = "Cleanup sprint data from cycle")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"took\": 0}")})
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/cleanupSprints")
    public Response cleanupSprintFromCycle() {
    	boolean isJiraAdmin = ComponentAccessor.getGlobalPermissionManager().hasPermission(GlobalPermissionKey.ADMINISTER,authContext.getLoggedInUser());
    	if(!isJiraAdmin) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
            log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
            return JiraUtil.getPermissionDeniedErrorResponse(errorMessage);
    	}
    	return cycleResourceDelegate.cleanupSprintFromCycle();
    }

    @ApiOperation(value = "Clean Up Cycle Cache", notes = "Cleanup cycle cache data for cycle/folder")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"took\": 0}")})
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/cleanupCycleCache")
    public Response cleanupCycleCache() {
        boolean isJiraAdmin = ComponentAccessor.getGlobalPermissionManager().hasPermission(GlobalPermissionKey.ADMINISTER,authContext.getLoggedInUser());
        if(!isJiraAdmin) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
            log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
            return JiraUtil.getPermissionDeniedErrorResponse(errorMessage);
        }
        return cycleResourceDelegate.cleanupCacheForCycle();
    }
    
    @SuppressWarnings("unchecked")
	@ApiOperation(value = "Get the list of folder for a cyle", notes = "Get the list of folder for a cyle")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "request", value = "{}"),
		@ApiImplicitParam(name = "response", value = "[{\"ID\":123456,\"folderName\":\"testfolder\",\"folderDescription\":\"created test folder for this cycle\",\"sprintId\":123,\"createdBy\":\"dummyUser\",\"executionSummaries\":{\"executionSummary\":[{\"count\": 0, \"statusKey\": -1, \"statusName\": \"UNEXECUTED\", \"statusColor\": \"#A0A0A0\"}]}}]")})
    @ApiResponses({@ApiResponse(code = 400, message = "Invalid Request Parameters."),
    	@ApiResponse(code = 500, message = "Server error while processing the request."),
    	@ApiResponse(code = 401, message = "Unauthorized Request."),
    	@ApiResponse(code = 200, message = "Request processed successfully", response=List.class, reference="[{\"ID\":123456,\"folderName\":\"testfolder\",\"folderDescription\":\"created test folder for this cycle\",\"sprintId\":123,\"createdBy\":\"dummyUser\",\"executionSummaries\":{\"executionSummary\":[{\"count\": 0, \"statusKey\": -1, \"statusName\": \"UNEXECUTED\", \"statusColor\": \"#A0A0A0\"}]}}]")})
	@GET
	@Path("{cycleId}/folders")
	public Response listAllFolderForCycle(@Context HttpServletRequest req, @ApiParam(value = "Cycle Id") @PathParam("cycleId") Long cycleId, @ApiParam(value = "Project Id") @QueryParam("projectId") Long projectId, @ApiParam(value = "Version Id") @QueryParam("versionId") Long versionId, @ApiParam(value = "Maximum number of records to fetch") @QueryParam("limit") Integer limit, @ApiParam(value = "Start position of the records") @QueryParam("offset") Integer offset) {
		final ApplicationUser user = authContext.getLoggedInUser();
		JSONObject jsonObject = new JSONObject();
		Map<String, String> valueHolder = new HashMap<>();
		try {
            if(Objects.isNull(user) && !JiraUtil.hasAnonymousPermission(user)) {
				jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
				String errorMessage = String.format(ERROR_LOG_MESSAGE, Status.UNAUTHORIZED.getStatusCode(), Status.UNAUTHORIZED, authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
				log.error(errorMessage);
				return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
			}
		} catch (JSONException e) {
			log.error("Error occurred while validating user.",e);
			return Response.status(Status.BAD_REQUEST).build();
		}
		try {
			if(Objects.isNull(cycleId) || cycleId.equals(0L)) {
				String errorMessage = String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST, authContext.getI18nHelper().getText("zephyr.cycle.folder.invalid.cycleId", cycleId));
				log.error(errorMessage);
				return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.cycle.folder.invalid.cycleId", cycleId), Status.BAD_REQUEST, null);
			}
			if(!cycleId.equals(ApplicationConstants.AD_HOC_CYCLE_ID_LONG)) {
				Cycle cycle = cycleManager.getCycle(cycleId);
				if(cycle == null) {
					log.error("[Error] [Error code:"+ Response.Status.BAD_REQUEST.getStatusCode() + " " + Response.Status.BAD_REQUEST +" Error Message :"+
							authContext.getI18nHelper().getText("zephyr.common.invalid.parameter"));
					return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("project.cycle.not.exist"), Status.NOT_ACCEPTABLE, null);
				}
				valueHolder.put("cycleName", cycle.getName());
			} else {
				log.error("[Error] [Error code:"+ Response.Status.BAD_REQUEST.getStatusCode() + " " + Response.Status.BAD_REQUEST +" Error Message :"+
						authContext.getI18nHelper().getText("folder.adhoc.cycle.error.message"));
				return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("folder.adhoc.cycle.error.message"), Status.NOT_ACCEPTABLE, null);
			}
			if(Objects.isNull(projectId) || projectId.equals(0L)) {
	    		return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "ProjectId"), Status.BAD_REQUEST, null);
	    	}
			// checking the project browse permissions
			Project project = projectManager.getProjectObj(projectId);
			Response response = validateProjectPermission(project, user);
			if(response != null) return response;
			valueHolder.put("projectKey", project.getKey());
			if(!Objects.isNull(versionId) && !(versionId.intValue() == ApplicationConstants.UNSCHEDULED_VERSION_ID)) { //Version validation
				Version version = ComponentAccessor.getVersionManager().getVersion(versionId);
				if(Objects.isNull(version)) {
					return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("project.version.not.exist"), Status.BAD_REQUEST, null);
				}
				valueHolder.put("versionName", version.getName());
			} else if(Objects.isNull(versionId)) {
				return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "VersionId"), Status.BAD_REQUEST, null);
			} else {
				String versionName = authContext.getI18nHelper().getText("zephyr.je.version.unscheduled");
				valueHolder.put("versionName", versionName);
			}
			//Delegate updating of a folder.
			List<Folder> foldersList = cycleResourceDelegate.fetchAllFoldersforCycle(projectId, versionId, cycleId, limit, offset);
			JSONArray jsonArray = new JSONArray();
			JSONObject jsonObj = null;
			List<Long> projectIds = new ArrayList<>();
			projectIds.add(projectId);
			String[] versionIds = new String[]{versionId+""};
			Map<String, String> cycleActionMap = null;
	        if (req.getSession(false) != null)
	            cycleActionMap = (HashMap<String, String>) req.getSession(false).getAttribute(SessionKeys.CYCLE_SUMMARY_DETAIL);
			for(Folder folder : foldersList) {
                jsonObj = new JSONObject();
                jsonObj.put("folderId", folder.getID());
                jsonObj.put("folderName", folder.getName());
                jsonObj.put("folderDescription", folder.getDescription());
                jsonObj.put("cycleId", cycleId);
                jsonObj.put("cycleName", valueHolder.get("cycleName"));
                jsonObj.put("projectId", projectId);
                jsonObj.put("projectKey", valueHolder.get("projectKey"));
                jsonObj.put("versionName", valueHolder.get("versionName"));
                jsonObj.put("versionId", versionId);
                Long sprintID = cycleResourceDelegate.getSprintIDForFolder(Long.valueOf(folder.getID()), cycleId);
                if (sprintID != null) {
                    jsonObj.put("sprintId", sprintID);
                }

                cycleResourceDelegate.populateFolderExecutionSummaries(jsonObj, projectIds, versionIds, cycleId, Long.valueOf(folder.getID() + ""), cycleActionMap);
                jsonArray.put(jsonObj);
            }
			return Response.ok().entity(jsonArray.toString()).build();
		} catch(Exception exception) {
			return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.internal.server.error"), Status.INTERNAL_SERVER_ERROR, null);
		}
	}
    
    @SuppressWarnings("unchecked")
	@ApiOperation(value = "Move selected executions or all executions from cycle to folder", notes = "Move selected executions or all executions from cycle to folder")
   	@ApiImplicitParams({
   		@ApiImplicitParam(name = "request", value = "{\"projectId\":123456,\"versionId\":435345, \"schedulesList\":[3,4,5,6]}"),
   		@ApiImplicitParam(name = "response", value = "{\"jobProgressToken\":\"34234-324234-2342342132\"}")})
       @ApiResponses({@ApiResponse(code = 400, message = "Invalid Request Parameters."),
       	@ApiResponse(code = 500, message = "Server error while processing the request."),
       	@ApiResponse(code = 401, message = "Unauthorized Request."),
       	@ApiResponse(code = 200, message = "Request processed successfully",  reference="{\"jobProgressToken\":\"34234-324234-2342342132\"}", response=JSONObject.class)})
   	@PUT
   	@Path("{cycleId}/move/executions/folder/{folderId}")
   	public Response moveExecutionsFromCycleToFolder(@ApiParam(value = "Cycle Id") @PathParam("cycleId") Long cycleId, @ApiParam(value = "Folder Id") @PathParam("folderId") Long folderId, Map<String, Object> params) {
   		final ApplicationUser user = authContext.getLoggedInUser();
   		JSONObject jsonObject = new JSONObject();
   		try {
             if(user == null && !JiraUtil.hasAnonymousPermission(user)) {
   				jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
   				String errorMessage = String.format(ERROR_LOG_MESSAGE, Status.UNAUTHORIZED.getStatusCode(), Status.UNAUTHORIZED, authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
   				log.error(errorMessage);
   				return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
   			}
   		} catch (JSONException e) {
   			log.error("Error occurred while getting count.",e);
   			return Response.status(Status.BAD_REQUEST).build();
   		}
   		try {
   	   		Long projectId = new Long(params.get("projectId") instanceof String ? StringUtils.isBlank((String)params.get("projectId")) ? 0 : Integer.parseInt((String)params.get("projectId"))  : (Integer)params.get("projectId"));
   	   		Long versionId = new Long(params.get("versionId") instanceof String ? StringUtils.isBlank((String)params.get("versionId")) ? 0 : Integer.parseInt((String)params.get("versionId"))  : (Integer)params.get("versionId"));
   	   		List<Integer> schedulesList = (List<Integer>) params.get("schedulesList");
   	   		if(Objects.isNull(projectId)|| projectId.equals(0L)) {
   	    		return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "ProjectId"), Status.BAD_REQUEST, null);
   	    	}
   	   		if(Objects.isNull(cycleId) || cycleId.equals(0L)) {
   	   			return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "CycleId"), Status.BAD_REQUEST, null);
   			}
   	   		if(cycleId.equals(ApplicationConstants.AD_HOC_CYCLE_ID_LONG)) {
   	    		return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("folder.adhoc.cycle.error.message"), Status.BAD_REQUEST, null);
   	    	}
   	   		if(Objects.isNull(folderId) || folderId.equals(0L)) {
   	    		return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.cycle.folder.invalid.folderId"), Status.BAD_REQUEST, null);
   	    	}
   	   		Project project = projectManager.getProjectObj(projectId);
   	   		// checking the project browse permissions
   			Response response = validateProjectPermission(project, user);
   			if(response != null) return response;
   			if(!Objects.isNull(versionId) && !(versionId.intValue() == ApplicationConstants.UNSCHEDULED_VERSION_ID)) { //Version validation
				Version version = ComponentAccessor.getVersionManager().getVersion(versionId);
				if(Objects.isNull(version)) {
					return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("project.version.not.exist"), Status.BAD_REQUEST, null);
				}
			} else if(Objects.isNull(versionId)) {
				return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "VersionId"), Status.BAD_REQUEST, null);
			}
   			Cycle cycle = cycleManager.getCycle(cycleId);
   			if((cycle == null && !cycleId.equals(ApplicationConstants.AD_HOC_CYCLE_ID_LONG))) {
   				log.error("[Error] [Error code:"+ Response.Status.BAD_REQUEST.getStatusCode() + " " + Response.Status.BAD_REQUEST +" Error Message :"+
   						authContext.getI18nHelper().getText("zephyr.common.invalid.parameter"));
   				return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "CycleId"), Status.NOT_ACCEPTABLE, null);
   			}
   	   		String loggedInUser = UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext));

            /**
             * Verify whether the user has permission to move executions from cycle to folder.
             */
            String errorMessage;
            JSONObject errorObject;

            List<String> projectPermissionKeys = new ArrayList<String>(){{
                add(PermissionType.ZEPHYR_EDIT_CYCLE.toString());
                add(PermissionType.ZEPHYR_EDIT_EXECUTION.toString());
            }};

            if(! verifyFolderLevelOperationPermission(user,projectId, projectPermissionKeys)) {
                errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
                errorObject = new JSONObject();
                return constructErrorResponse(errorObject, errorMessage, Status.FORBIDDEN, null);
            }

   	   		response = cycleResourceDelegate.moveExecutionsFromCycleToFolder(projectId, versionId, cycleId, folderId, loggedInUser, cycle.getName(), schedulesList);
   			return response;
   		} catch(Exception exception) {
   			return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.internal.server.error"), Status.INTERNAL_SERVER_ERROR, null);
   		}
   	}
    
    
    private Response validateProjectPermission(Project project, ApplicationUser user) {
    	JSONObject jsonObject = new JSONObject();
    	 if (project == null) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "projectId ", "");
            log.error(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,errorMessage));
            return constructErrorResponse(jsonObject, errorMessage, Status.BAD_REQUEST, null);
        } 
        if (!JiraUtil.hasBrowseProjectPermission(project, user)) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.project.permission.error", "Cycle", String.valueOf(project.getName()));
            log.error(String.format(ERROR_LOG_MESSAGE, Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
            return constructErrorResponse(jsonObject, errorMessage, Status.FORBIDDEN, null);            
        }
    	return null;
    }
    
    /**
     * Validates Input request for Cycle creation and updates
     *
     * @param cycleRequest
     * @param action
     * @return
     */
    private Map<String, String> inputCycleDataValidation(CycleRequest cycleRequest, String action) {
        Map<String, String> errorMap = new LinkedHashMap<>();
        final I18nHelper i18n = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();

        if (StringUtils.equalsIgnoreCase(action, "update")) {
            if (StringUtils.isBlank(cycleRequest.id)) {
                errorMap.put("id", i18n.getText("project.cycle.summary.missingid.error"));
            }
            if (cycleRequest.name != null) {
                if (StringUtils.isBlank(cycleRequest.name)) {
                    errorMap.put("name", i18n.getText("project.cycle.name.invalid.error"));
                }
                if(StringUtils.isNotBlank(cycleRequest.name) && !cycleRequest.name.matches("^(.){1,255}$")) {
                    errorMap.put("name", "Cycle name length exceeded allowed characters limit.");
                }
            }
            validateCycleStartAndEndDate(cycleRequest.startDate, cycleRequest.endDate, errorMap, i18n);
        }

        //ZAPI validation
        if (StringUtils.equalsIgnoreCase(action, "create")) {
            if (StringUtils.isBlank(cycleRequest.name)) {
                errorMap.put("name", i18n.getText("project.cycle.summary.create.dialog.validationError.cyclename"));
            }

            if(StringUtils.isNotBlank(cycleRequest.name) && !cycleRequest.name.matches("^(.){1,255}$")) {
                errorMap.put("name", "Cycle name length exceeded allowed characters limit.");
            }
            validateCycleStartAndEndDate(cycleRequest.startDate, cycleRequest.endDate, errorMap, i18n);

            //validation if the projectId is valid
            if (StringUtils.isBlank(cycleRequest.projectId)) {
                errorMap.put("projectId", i18n.getText("project.cycle.summary.create.dialog.validationError.project"));
                return errorMap;
            }
            Long projectId = null;
            try {
                projectId = Long.valueOf(cycleRequest.projectId);
            } catch (NumberFormatException nfe) {
                errorMap.put("projectId", i18n.getText("project.cycle.summary.create.dialog.validationError.project"));

            }
            Project project = projectManager.getProjectObj(projectId);
            if (project == null) {
                errorMap.put("projectId", i18n.getText("project.cycle.summary.create.dialog.validationError.project"));
                return errorMap;
            }
            JiraUtil.setProjectThreadLocal(project);
            //Verify Cloned Cycle
            if (null != cycleRequest.clonedCycleId && !StringUtils.equalsIgnoreCase(cycleRequest.clonedCycleId, "")) {
                if (StringUtils.equalsIgnoreCase(cycleRequest.clonedCycleId, "0")) {
                    errorMap.put("clonedCycleId", i18n.getText("schedule.execute.update.stepresult.invalid.id", "clonedCycleId"));
                    return errorMap;
                } else {
                    Cycle cycle = cycleManager.getCycle(Long.valueOf(cycleRequest.clonedCycleId));
                    if (cycle == null) {
                        errorMap.put("clonedCycleId", i18n.getText("zephyr.common.error.invalid", "clonedCycleId", cycleRequest.clonedCycleId));
                        return errorMap;
                    }
                    if (cycle.getProjectId().intValue() != Integer.valueOf(cycleRequest.projectId)) {
                        errorMap.put("projectId", i18n.getText("schedule.entity.mismatch.error", "ProjectId", cycleRequest.projectId, "Project", "Cycle", cycleRequest.clonedCycleId));
                        return errorMap;
                    }
                }
            }

            // checking the project browse permissions
            if (!permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, project, authContext.getLoggedInUser())) {
                errorMap.put("ProjectBrowsePermissions", i18n.getText("zapi.execution.move.invalid.projectid"));
                return errorMap;
            }

            if (StringUtils.isBlank(cycleRequest.versionId) || StringUtils.equalsIgnoreCase(cycleRequest.versionId, "0")) {
                errorMap.put("versionId", i18n.getText("project.cycle.summary.create.dialog.validationError.version.missing"));
                return errorMap;
            }
            //Unscheduled is a valid version. skip verification in that case
            if (Integer.valueOf(cycleRequest.versionId).intValue() != -1) {
                boolean isVersionMatch = false;
                for (Version version : project.getVersions()) {
                    if (version.getId().intValue() == Integer.valueOf(cycleRequest.versionId).intValue()) {
                        isVersionMatch = true;
                    }
                }
                if (!isVersionMatch) {
                    errorMap.put("versionId", i18n.getText("project.cycle.summary.create.dialog.validationError.version.mismatch", cycleRequest.versionId));
                }
            }
        }
        if (StringUtils.isNotEmpty(cycleRequest.build) && (cycleRequest.build.length() > 255))
            errorMap.put("build", String.format("Cycle build name should be equal or less than %s characters.",255));

        if (StringUtils.isNotEmpty(cycleRequest.environment) && (cycleRequest.environment.length() > 255))
            errorMap.put("environment", String.format("Cycle environment name should be equal or less than %s characters.",255));


        /*Validate Sprint if present */
        if (StringUtils.isNotBlank(cycleRequest.sprintId)) {
            Optional<SprintBean> sprint = sprintService.getSprint(Long.parseLong(cycleRequest.sprintId));
            if (!sprint.isPresent()) {
                errorMap.put("sprintId", i18n.getText("project.cycle.validationError.sprintNotFound", cycleRequest.sprintId));
            } else if (sprint.get().getState() == SprintBean.State.CLOSED.name()) {
                log.warn(i18n.getText("project.cycle.validationError.invalidSprintState", sprint.get().getName()));
            }
        }

        return errorMap;
    }
    
    /**
     * Validates Cycle Start and End Date
     *
     * @param startDate
     * @param endDate
     * @param errorMap
     * @param i18n
     */
    private void validateCycleStartAndEndDate(String startDate, String endDate, Map<String, String> errorMap, final I18nHelper i18n) {
        DateTimeFormatter formatter = dateTimeFormatterFactory.formatter().forLoggedInUser().withSystemZone().withStyle(DateTimeStyle.DATE_PICKER);
        Date cycleStartDate = null;
        Date cycleEndDate = null;
        try {
            cycleStartDate = convertToDate(startDate, formatter);
            cycleEndDate = convertToDate(endDate, formatter);
        } catch (Exception ex) {
            log.error("Error in converting to date "+ ex.getMessage());
            errorMap.put("date", i18n.getText("fields.validation.data.format", ComponentAccessor.getApplicationProperties().getDefaultBackedString(APKeys.JIRA_DATE_PICKER_JAVA_FORMAT)));
        }

        if (!isDateSequenceValid(cycleStartDate, cycleEndDate))
            errorMap.put("dateSequenceError", i18n.getText("project.cycle.summary.create.dialog.validationError.datesequenceerror"));
    }

    private boolean isDateSequenceValid(Date cycleStartDate, Date cycleEndDate) {
        if (cycleStartDate == null || cycleEndDate == null)
            return true;
        return (cycleEndDate.after(cycleStartDate) || DateUtils.isSameDay(cycleStartDate, cycleEndDate));
    }
	
    /**
     * @param dateString
     * @param formatter
     * @return Date
     */
    private Date convertToDate(String dateString, DateTimeFormatter formatter) {
        if (!StringUtils.isBlank(dateString)) {
            Date date = formatter.parse(dateString);
            return date;
        }
        return null;
    }
    
    /**
     * Build Error Map
     *
     * @param errorMap
     * @return
     */
    private Response buildResponseErrorMap(Map<String, String> errorMap) {
        Response.ResponseBuilder builder = Response.status(Response.Status.NOT_ACCEPTABLE);
        builder.type(MediaType.APPLICATION_JSON);
        builder.entity(errorMap);
        return builder.build();
    }
    
    private Response constructErrorResponse(JSONObject jsonObject, String errorMessage, Status status, Exception exception) {
        try {
    		String finalErrorMessage = String.format(ERROR_LOG_MESSAGE, status.getStatusCode(), status, errorMessage);
            log.error(finalErrorMessage, exception);
			jsonObject.put("error", errorMessage);
			return Response.status(status).entity(jsonObject != null ? jsonObject.toString() : finalErrorMessage).cacheControl(ZephyrCacheControl.never()).build();
		} catch (JSONException e) {
			log.error("Eror while constructing the error response");
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}		
	}

    private boolean verifyFolderLevelOperationPermission(ApplicationUser user, Long projectId, List<String> permissionTypeList) {
        ProjectPermissionKey projectPermissionKey;
        for(String permissionType : permissionTypeList) {
            projectPermissionKey = new ProjectPermissionKey(permissionType);
            boolean permission = zephyrPermissionManager.validateUserPermission(projectPermissionKey, null, user, projectId);
            if(!permission) {
                return permission;
            }
        }
        return true;
    }

    /**
     * Validate folder level operation permission.
     * @param user
     * @param projectId
     * @param permissionType
     */
    private boolean verifyCycleLevelExecutionOperationPermission(ApplicationUser user, Long projectId, String permissionType) {
        ProjectPermissionKey projectPermissionKey = new ProjectPermissionKey(permissionType);
        return zephyrPermissionManager.validateUserPermission(projectPermissionKey, null, user, projectId);
    }
    
    @XmlRootElement
    public static class CycleRequest {
        @XmlElement(nillable = true)
        public String id;

        @XmlElement(nillable = true)
        public String clonedCycleId;

        @XmlElement(nillable = false)
        public String name;

        @XmlElement(nillable = false)
        public String build;

        @XmlElement(nillable = true)
        public String description;

        @XmlElement(nillable = true)
        public String environment;

        @XmlElement(nillable = true)
        public String versionId;

        @XmlElement(nillable = true)
        public String projectId;

        @XmlElement(nillable = true)
        public String startDate;

        @XmlElement(nillable = true)
        public String endDate;

        @XmlElement(nillable = true)
        public String issueId;

        @XmlElement(nillable = true)
        public String sprintId;
        
        @XmlElement(nillable = true)
        public Long folderId;

        @XmlElement(nillable = true)
        public Boolean cloneCustomFields;

        public CycleRequest() {
        }
    }

    @XmlRootElement
    public static class CycleResponse {
        @XmlElement
        public String id;
        @XmlElement
        public String responseMessage;

        public CycleResponse() {
        }
    }
}

