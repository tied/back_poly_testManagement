package com.thed.zephyr.je.rest;

import com.atlassian.core.util.InvalidDurationException;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.google.gson.JsonObject;
import com.sun.jersey.spi.container.ResourceFilters;
import com.thed.zephyr.je.model.ExecutionWorkflowStatus;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.permissions.model.PermissionType;
import com.thed.zephyr.je.permissions.service.ZephyrPermissionManager;
import com.thed.zephyr.je.rest.delegate.ExecutionWorkflowResourceDelegate;
import com.thed.zephyr.je.rest.delegate.ScheduleResourceDelegate;
import com.thed.zephyr.je.rest.filter.ZFJApiFilter;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.ZephyrCacheControl;
import io.swagger.annotations.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Map;
import java.util.Objects;

/**
 * @author manjunath
 *
 */
@Api(value = "Execution Workflow Resource API(s)", description = "Following section describes rest resources pertaining to Execution Workflow Resource")
@Path("execution/workflow")
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@ResourceFilters(ZFJApiFilter.class)
public class ExecutionWorkflowResource {
	
	protected final Logger log = Logger.getLogger(ExecutionWorkflowResource.class);
	private static final String ERROR_LOG_MESSAGE = "[Error] [Error code: %s %s Error Message : %s]";
	
	private final JiraAuthenticationContext authContext;
	private final ExecutionWorkflowResourceDelegate executionWorkflowResourceDelegate;
	private final ProjectManager projectManager;
	private final IssueManager issueManager;
	private final ScheduleResourceDelegate scheduleResourceDelegate;
    private final ZephyrPermissionManager zephyrPermissionManager;
	
	public ExecutionWorkflowResource(JiraAuthenticationContext authContext, ExecutionWorkflowResourceDelegate executionWorkflowResourceDelegate, 
			ProjectManager projectManager, IssueManager issueManager, ScheduleResourceDelegate scheduleResourceDelegate,
                                     ZephyrPermissionManager zephyrPermissionManager) {
		this.authContext = authContext;
		this.executionWorkflowResourceDelegate = executionWorkflowResourceDelegate;
		this.projectManager = projectManager;
		this.issueManager = issueManager;
		this.scheduleResourceDelegate = scheduleResourceDelegate;
		this.zephyrPermissionManager = zephyrPermissionManager;
	}
	
	@ApiOperation(value = "Start Execution", notes = "Start workflow for the execution.")
	@ApiImplicitParams(@ApiImplicitParam(name = "response", value = "{\\\"execution\\\":{\\\"id\\\":10203,\\\"orderId\\\":9821,\\\"executionStatus\\\":\\\"-1\\\",\\\"comment\\\":\\\"\\\",\\\"htmlComment\\\":\\\"\\\",\\\"cycleId\\\":-1,\\\"cycleName\\\":\\\"Ad hoc\\\",\\\"versionId\\\":10401,\\\"versionName\\\":\\\"szdfxgcvhbjjjj\\\",\\\"projectId\\\":10100,\\\"createdBy\\\":\\\"vm_admin\\\",\\\"modifiedBy\\\":\\\"vm_admin\\\",\\\"issueId\\\":11725,\\\"issueKey\\\":\\\"SONY-1386\\\",\\\"summary\\\":\\\"SONY Project\\\",\\\"label\\\":\\\"\\\",\\\"component\\\":\\\"\\\",\\\"projectKey\\\":\\\"SONY\\\",\\\"executionDefectCount\\\":0,\\\"stepDefectCount\\\":0,\\\"totalDefectCount\\\":0,\\\"executionWorkflowStatus\\\":\\\"STARTED\\\", \\\"executionEstimatedTime\\\":\\\"2hr 20min\\\", \\\"executionTimeLogged\\\":\\\"0hr\\\"}}"))
	@ApiResponses({@ApiResponse(code = 400, message = "Invalid Request Parameters."),
    	@ApiResponse(code = 500, message = "Server error while processing the request."),
    	@ApiResponse(code = 401, message = "Unauthorized Request."), @ApiResponse(code = 403, message = "Permission Denied for the request"),
    	@ApiResponse(code = 200, message = "Request processed successfully", response=Map.class, responseContainer="Map", reference = "{\\\\\\\"execution\\\\\\\":{\\\\\\\"id\\\\\\\":10203,\\\\\\\"orderId\\\\\\\":9821,\\\\\\\"executionStatus\\\\\\\":\\\\\\\"-1\\\\\\\",\\\\\\\"comment\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"htmlComment\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"cycleId\\\\\\\":-1,\\\\\\\"cycleName\\\\\\\":\\\\\\\"Ad hoc\\\\\\\",\\\\\\\"versionId\\\\\\\":10401,\\\\\\\"versionName\\\\\\\":\\\\\\\"szdfxgcvhbjjjj\\\\\\\",\\\\\\\"projectId\\\\\\\":10100,\\\\\\\"createdBy\\\\\\\":\\\\\\\"vm_admin\\\\\\\",\\\\\\\"modifiedBy\\\\\\\":\\\\\\\"vm_admin\\\\\\\",\\\\\\\"issueId\\\\\\\":11725,\\\\\\\"issueKey\\\\\\\":\\\\\\\"SONY-1386\\\\\\\",\\\\\\\"summary\\\\\\\":\\\\\\\"SONY Project\\\\\\\",\\\\\\\"label\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"component\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"projectKey\\\\\\\":\\\\\\\"SONY\\\\\\\",\\\\\\\"executionDefectCount\\\\\\\":0,\\\\\\\"stepDefectCount\\\\\\\":0,\\\\\\\"totalDefectCount\\\\\\\":0,\\\\\\\"executionWorkflowStatus\\\\\\\":\\\\\\\"STARTED\\\\\\\", \\\\\\\"executionEstimatedTime\\\\\\\":\\\\\\\"2hr 20min\\\\\\\", \\\\\\\"executionTimeLogged\\\\\\\":\\\\\\\"0hr\\\\\\\"}}")})
	@PUT
	@Path("/{scheduleId}/inProgress")
	public Response startExecution(@ApiParam(value = "Schedule Id") @PathParam("scheduleId") Integer scheduleId) {
	    final ApplicationUser user = authContext.getLoggedInUser();
		JSONObject jsonObject = new JSONObject();
		Schedule schedule = null;
		try {
			schedule = executionWorkflowResourceDelegate.getExecution(scheduleId);
			if (Objects.isNull(schedule)) {
				return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Execution" + String.format(" ID : %s",scheduleId)), Status.BAD_REQUEST, null);
			}
			Response response = validatePermissionForSchedule(schedule, Boolean.FALSE);
			if(Objects.nonNull(response)) {
				return response;
			}
            Project project = projectManager.getProjectObj(schedule.getProjectId());
            JiraUtil.setProjectThreadLocal(project);

            if(!verifyExecutionLevelPermission(user,project.getId(), PermissionType.ZEPHYR_EDIT_EXECUTION.toString())) {
                String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error") + " Missing " +
                        PermissionType.ZEPHYR_EDIT_EXECUTION.toString() + " permission.";
                return constructErrorResponse(jsonObject, errorMessage, Status.FORBIDDEN, null);
            }

			executionWorkflowResourceDelegate.startExecution(schedule);
		} catch(Exception exception) {
			return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.internal.server.error"), Status.INTERNAL_SERVER_ERROR, exception);
		}
		return scheduleResourceDelegate.getExecution(schedule, null);
	}

    @ApiOperation(value = "Pause Execution", notes = "Pause workflow for the execution.")
	@ApiImplicitParams(@ApiImplicitParam(name = "response", value = "{\\\"execution\\\":{\\\"id\\\":10203,\\\"orderId\\\":9821,\\\"executionStatus\\\":\\\"-1\\\",\\\"comment\\\":\\\"\\\",\\\"htmlComment\\\":\\\"\\\",\\\"cycleId\\\":-1,\\\"cycleName\\\":\\\"Ad hoc\\\",\\\"versionId\\\":10401,\\\"versionName\\\":\\\"szdfxgcvhbjjjj\\\",\\\"projectId\\\":10100,\\\"createdBy\\\":\\\"vm_admin\\\",\\\"modifiedBy\\\":\\\"vm_admin\\\",\\\"issueId\\\":11725,\\\"issueKey\\\":\\\"SONY-1386\\\",\\\"summary\\\":\\\"SONY Project\\\",\\\"label\\\":\\\"\\\",\\\"component\\\":\\\"\\\",\\\"projectKey\\\":\\\"SONY\\\",\\\"executionDefectCount\\\":0,\\\"stepDefectCount\\\":0,\\\"totalDefectCount\\\":0,\\\"executionWorkflowStatus\\\":\\\"PAUSED\\\", \\\"executionEstimatedTime\\\":\\\"2hr 20min\\\", \\\"executionTimeLogged\\\":\\\"2hr\\\"}}"))
	@ApiResponses({@ApiResponse(code = 400, message = "Invalid Request Parameters."),
    	@ApiResponse(code = 500, message = "Server error while processing the request."),
    	@ApiResponse(code = 401, message = "Unauthorized Request."), @ApiResponse(code = 403, message = "Permission Denied for the request"),
    	@ApiResponse(code = 200, message = "Request processed successfully", response=Map.class, responseContainer="Map", reference = "{\\\\\\\"execution\\\\\\\":{\\\\\\\"id\\\\\\\":10203,\\\\\\\"orderId\\\\\\\":9821,\\\\\\\"executionStatus\\\\\\\":\\\\\\\"-1\\\\\\\",\\\\\\\"comment\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"htmlComment\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"cycleId\\\\\\\":-1,\\\\\\\"cycleName\\\\\\\":\\\\\\\"Ad hoc\\\\\\\",\\\\\\\"versionId\\\\\\\":10401,\\\\\\\"versionName\\\\\\\":\\\\\\\"szdfxgcvhbjjjj\\\\\\\",\\\\\\\"projectId\\\\\\\":10100,\\\\\\\"createdBy\\\\\\\":\\\\\\\"vm_admin\\\\\\\",\\\\\\\"modifiedBy\\\\\\\":\\\\\\\"vm_admin\\\\\\\",\\\\\\\"issueId\\\\\\\":11725,\\\\\\\"issueKey\\\\\\\":\\\\\\\"SONY-1386\\\\\\\",\\\\\\\"summary\\\\\\\":\\\\\\\"SONY Project\\\\\\\",\\\\\\\"label\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"component\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"projectKey\\\\\\\":\\\\\\\"SONY\\\\\\\",\\\\\\\"executionDefectCount\\\\\\\":0,\\\\\\\"stepDefectCount\\\\\\\":0,\\\\\\\"totalDefectCount\\\\\\\":0,\\\\\\\"executionWorkflowStatus\\\\\\\":\\\\\\\"PAUSED\\\\\\\", \\\\\\\"executionEstimatedTime\\\\\\\":\\\\\\\"2hr 20min\\\\\\\", \\\\\\\"executionTimeLogged\\\\\\\":\\\\\\\"2hr\\\\\\\"}}")})
	@PUT
	@Path("/{scheduleId}/pause")
	public Response pauseExecution(@ApiParam(value = "Schedule Id") @PathParam("scheduleId") Integer scheduleId) {
		JSONObject jsonObject = new JSONObject();
		Schedule schedule = null;
		try {
			schedule = executionWorkflowResourceDelegate.getExecution(scheduleId);
			if (Objects.isNull(schedule)) {
				return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Execution" + String.format(" ID : %s",scheduleId)), Status.BAD_REQUEST, null);
			}
			Response response = validatePermissionForSchedule(schedule, Boolean.FALSE);
			if(Objects.nonNull(response)) {
				return response;
			}
            Project project = projectManager.getProjectObj(schedule.getProjectId());
            JiraUtil.setProjectThreadLocal(project);

            if(!verifyExecutionLevelPermission(authContext.getLoggedInUser(),project.getId(), PermissionType.ZEPHYR_EDIT_EXECUTION.toString())) {
                String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error") + " Missing " +
                        PermissionType.ZEPHYR_EDIT_EXECUTION.toString() + " permission.";
                return constructErrorResponse(jsonObject, errorMessage, Status.FORBIDDEN, null);
            }

			executionWorkflowResourceDelegate.pauseExecution(schedule);
		} catch(Exception exception) {
			return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.internal.server.error"), Status.INTERNAL_SERVER_ERROR, exception);
		}
		return scheduleResourceDelegate.getExecution(schedule, null);		
	}
	
	@ApiOperation(value = "Complete Execution", notes = "Complete workflow for the execution.")
	@ApiImplicitParams(@ApiImplicitParam(name = "response", value = "{\\\"execution\\\":{\\\"id\\\":10203,\\\"orderId\\\":9821,\\\"executionStatus\\\":\\\"-1\\\",\\\"comment\\\":\\\"\\\",\\\"htmlComment\\\":\\\"\\\",\\\"cycleId\\\":-1,\\\"cycleName\\\":\\\"Ad hoc\\\",\\\"versionId\\\":10401,\\\"versionName\\\":\\\"szdfxgcvhbjjjj\\\",\\\"projectId\\\":10100,\\\"createdBy\\\":\\\"vm_admin\\\",\\\"modifiedBy\\\":\\\"vm_admin\\\",\\\"issueId\\\":11725,\\\"issueKey\\\":\\\"SONY-1386\\\",\\\"summary\\\":\\\"SONY Project\\\",\\\"label\\\":\\\"\\\",\\\"component\\\":\\\"\\\",\\\"projectKey\\\":\\\"SONY\\\",\\\"executionDefectCount\\\":0,\\\"stepDefectCount\\\":0,\\\"totalDefectCount\\\":0,\\\"executionWorkflowStatus\\\":\\\"COMPLETED\\\", \\\"executionEstimatedTime\\\":\\\"2hr 20min\\\", \\\"executionTimeLogged\\\":\\\"3hr\\\"}}"))
	@ApiResponses({@ApiResponse(code = 400, message = "Invalid Request Parameters."),
    	@ApiResponse(code = 500, message = "Server error while processing the request."),
    	@ApiResponse(code = 401, message = "Unauthorized Request."), @ApiResponse(code = 403, message = "Permission Denied for the request"),
    	@ApiResponse(code = 200, message = "Request processed successfully", response=Map.class, responseContainer="Map",reference = "{\\\\\\\"execution\\\\\\\":{\\\\\\\"id\\\\\\\":10203,\\\\\\\"orderId\\\\\\\":9821,\\\\\\\"executionStatus\\\\\\\":\\\\\\\"-1\\\\\\\",\\\\\\\"comment\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"htmlComment\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"cycleId\\\\\\\":-1,\\\\\\\"cycleName\\\\\\\":\\\\\\\"Ad hoc\\\\\\\",\\\\\\\"versionId\\\\\\\":10401,\\\\\\\"versionName\\\\\\\":\\\\\\\"szdfxgcvhbjjjj\\\\\\\",\\\\\\\"projectId\\\\\\\":10100,\\\\\\\"createdBy\\\\\\\":\\\\\\\"vm_admin\\\\\\\",\\\\\\\"modifiedBy\\\\\\\":\\\\\\\"vm_admin\\\\\\\",\\\\\\\"issueId\\\\\\\":11725,\\\\\\\"issueKey\\\\\\\":\\\\\\\"SONY-1386\\\\\\\",\\\\\\\"summary\\\\\\\":\\\\\\\"SONY Project\\\\\\\",\\\\\\\"label\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"component\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"projectKey\\\\\\\":\\\\\\\"SONY\\\\\\\",\\\\\\\"executionDefectCount\\\\\\\":0,\\\\\\\"stepDefectCount\\\\\\\":0,\\\\\\\"totalDefectCount\\\\\\\":0,\\\\\\\"executionWorkflowStatus\\\\\\\":\\\\\\\"COMPLETED\\\\\\\", \\\\\\\"executionEstimatedTime\\\\\\\":\\\\\\\"2hr 20min\\\\\\\", \\\\\\\"executionTimeLogged\\\\\\\":\\\\\\\"3hr\\\\\\\"}}")})
	@PUT
	@Path("/{scheduleId}/complete")
	public Response completeExecution(@ApiParam(value = "Schedule Id") @PathParam("scheduleId") Integer scheduleId, 
			@ApiParam(value = "Time Logged By User") @QueryParam("timeLogged") String timeLogged) {
		JSONObject jsonObject = new JSONObject();
		Schedule schedule = null;
		try {
			schedule = executionWorkflowResourceDelegate.getExecution(scheduleId);
			if (Objects.isNull(schedule)) {	        
	            return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Execution" + String.format(" ID : %s",scheduleId)), Status.BAD_REQUEST, null);
			}
			Response response = validatePermissionForSchedule(schedule, Boolean.FALSE);
			if(Objects.nonNull(response)) {
				return response;
			}
            Project project = projectManager.getProjectObj(schedule.getProjectId());
            JiraUtil.setProjectThreadLocal(project);

            if(!verifyExecutionLevelPermission(authContext.getLoggedInUser(),project.getId(), PermissionType.ZEPHYR_EDIT_EXECUTION.toString())) {
                String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error") + " Missing " +
                        PermissionType.ZEPHYR_EDIT_EXECUTION.toString() + " permission.";
                return constructErrorResponse(jsonObject, errorMessage, Status.FORBIDDEN, null);
            }

			executionWorkflowResourceDelegate.completeExecution(schedule,timeLogged);
		} catch(InvalidDurationException e) {
			return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("workflow.invalid.time.logged.label", timeLogged), Status.BAD_REQUEST, e);
		} catch(Exception exception) {
			return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.internal.server.error"), Status.INTERNAL_SERVER_ERROR, exception);
		}
		return scheduleResourceDelegate.getExecution(schedule, null);		
	}
	
	
	@ApiOperation(value = "Resume Execution", notes = "Resume workflow for the execution.")
	@ApiImplicitParams(@ApiImplicitParam(name = "response", value = "{\\\"execution\\\":{\\\"id\\\":10203,\\\"orderId\\\":9821,\\\"executionStatus\\\":\\\"-1\\\",\\\"comment\\\":\\\"\\\",\\\"htmlComment\\\":\\\"\\\",\\\"cycleId\\\":-1,\\\"cycleName\\\":\\\"Ad hoc\\\",\\\"versionId\\\":10401,\\\"versionName\\\":\\\"szdfxgcvhbjjjj\\\",\\\"projectId\\\":10100,\\\"createdBy\\\":\\\"vm_admin\\\",\\\"modifiedBy\\\":\\\"vm_admin\\\",\\\"issueId\\\":11725,\\\"issueKey\\\":\\\"SONY-1386\\\",\\\"summary\\\":\\\"SONY Project\\\",\\\"label\\\":\\\"\\\",\\\"component\\\":\\\"\\\",\\\"projectKey\\\":\\\"SONY\\\",\\\"executionDefectCount\\\":0,\\\"stepDefectCount\\\":0,\\\"totalDefectCount\\\":0,\\\"executionWorkflowStatus\\\":\\\"STARTED\\\", \\\"executionEstimatedTime\\\":\\\"2hr 20min\\\", \\\"executionTimeLogged\\\":\\\"2hr\\\"}}"))
	@ApiResponses({@ApiResponse(code = 400, message = "Invalid Request Parameters."),
    	@ApiResponse(code = 500, message = "Server error while processing the request."),
    	@ApiResponse(code = 401, message = "Unauthorized Request."), @ApiResponse(code = 403, message = "Permission Denied for the request"),
    	@ApiResponse(code = 200, message = "Request processed successfully", response=Map.class, responseContainer="Map", reference = "{\\\\\\\"execution\\\\\\\":{\\\\\\\"id\\\\\\\":10203,\\\\\\\"orderId\\\\\\\":9821,\\\\\\\"executionStatus\\\\\\\":\\\\\\\"-1\\\\\\\",\\\\\\\"comment\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"htmlComment\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"cycleId\\\\\\\":-1,\\\\\\\"cycleName\\\\\\\":\\\\\\\"Ad hoc\\\\\\\",\\\\\\\"versionId\\\\\\\":10401,\\\\\\\"versionName\\\\\\\":\\\\\\\"szdfxgcvhbjjjj\\\\\\\",\\\\\\\"projectId\\\\\\\":10100,\\\\\\\"createdBy\\\\\\\":\\\\\\\"vm_admin\\\\\\\",\\\\\\\"modifiedBy\\\\\\\":\\\\\\\"vm_admin\\\\\\\",\\\\\\\"issueId\\\\\\\":11725,\\\\\\\"issueKey\\\\\\\":\\\\\\\"SONY-1386\\\\\\\",\\\\\\\"summary\\\\\\\":\\\\\\\"SONY Project\\\\\\\",\\\\\\\"label\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"component\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"projectKey\\\\\\\":\\\\\\\"SONY\\\\\\\",\\\\\\\"executionDefectCount\\\\\\\":0,\\\\\\\"stepDefectCount\\\\\\\":0,\\\\\\\"totalDefectCount\\\\\\\":0,\\\\\\\"executionWorkflowStatus\\\\\\\":\\\\\\\"STARTED\\\\\\\", \\\\\\\"executionEstimatedTime\\\\\\\":\\\\\\\"2hr 20min\\\\\\\", \\\\\\\"executionTimeLogged\\\\\\\":\\\\\\\"2hr\\\\\\\"}}")})
	@PUT
	@Path("/{scheduleId}/resume")
	public Response resumeExecution(@ApiParam(value = "Schedule Id") @PathParam("scheduleId") Integer scheduleId) {
		JSONObject jsonObject = new JSONObject();
		Schedule schedule = null;
		try {
			schedule = executionWorkflowResourceDelegate.getExecution(scheduleId);
			if (Objects.isNull(schedule)) {
				return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Execution" + String.format(" ID : %s",scheduleId)), Status.BAD_REQUEST, null);
			}
			Response response = validatePermissionForSchedule(schedule, Boolean.FALSE);
			if(Objects.nonNull(response)) {
				return response;
			}

            if(!verifyExecutionLevelPermission(authContext.getLoggedInUser(),schedule.getProjectId(), PermissionType.ZEPHYR_EDIT_EXECUTION.toString())) {
                String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error") + " Missing " +
                        PermissionType.ZEPHYR_EDIT_EXECUTION.toString() + " permission.";
                return constructErrorResponse(jsonObject, errorMessage, Status.FORBIDDEN, null);
            }

			executionWorkflowResourceDelegate.resumeExecution(schedule);
		} catch(Exception exception) {
			return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.internal.server.error"), Status.INTERNAL_SERVER_ERROR, exception);
		}
		return scheduleResourceDelegate.getExecution(schedule, null);		
	}
	
	@ApiOperation(value = "Execution Workflow Check for project", notes = "Check whether the Execution Workflow is enabled")
	@ApiResponses({@ApiResponse(code = 400, message = "Invalid Request Parameters."),
    	@ApiResponse(code = 500, message = "Server error while processing the request."),
    	@ApiResponse(code = 401, message = "Unauthorized Request."),
    	@ApiResponse(code = 403, message = "Permission Denied for the request"),
    	@ApiResponse(code = 404, message = "Execution Workflow is disabled for the project"),
    	@ApiResponse(code = 200, message = "Request processed successfully")})
	@PUT
	@Path("/disable/status")
	public Response executionWorkflowStatus(@ApiParam(value = "Project Id") @QueryParam("projectId") Long projectId) {
		JSONObject jsonObject = new JSONObject();
		boolean result = false;
		try {
			Project project = projectManager.getProjectObj(projectId);
			if (Objects.isNull(project)) {
	            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "projectId ", projectId+"");
	            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,errorMessage));
	            return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "Invalid Project", errorMessage, errorMessage);
			}
			result = executionWorkflowResourceDelegate.isExecutionWorkflowDisabled(projectId);
		} catch(Exception exception) {
			return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.internal.server.error"), Status.INTERNAL_SERVER_ERROR, exception);
		}
		JsonObject response = new JsonObject();
		response.addProperty("isWorkflowDisabled", result);
		return Response.ok(response.toString()).build();
	}

    @ApiOperation(value = "Complete Execution", notes = "Complete workflow for the execution.")
    @ApiImplicitParams(@ApiImplicitParam(name = "response", value = "{\\\"execution\\\":{\\\"id\\\":10203,\\\"orderId\\\":9821,\\\"executionStatus\\\":\\\"-1\\\",\\\"comment\\\":\\\"\\\",\\\"htmlComment\\\":\\\"\\\",\\\"cycleId\\\":-1,\\\"cycleName\\\":\\\"Ad hoc\\\",\\\"versionId\\\":10401,\\\"versionName\\\":\\\"szdfxgcvhbjjjj\\\",\\\"projectId\\\":10100,\\\"createdBy\\\":\\\"vm_admin\\\",\\\"modifiedBy\\\":\\\"vm_admin\\\",\\\"issueId\\\":11725,\\\"issueKey\\\":\\\"SONY-1386\\\",\\\"summary\\\":\\\"SONY Project\\\",\\\"label\\\":\\\"\\\",\\\"component\\\":\\\"\\\",\\\"projectKey\\\":\\\"SONY\\\",\\\"executionDefectCount\\\":0,\\\"stepDefectCount\\\":0,\\\"totalDefectCount\\\":0,\\\"executionWorkflowStatus\\\":\\\"COMPLETED\\\", \\\"executionEstimatedTime\\\":\\\"2hr 20min\\\", \\\"executionTimeLogged\\\":\\\"3hr\\\"}}"))
    @ApiResponses({@ApiResponse(code = 400, message = "Invalid Request Parameters."),
            @ApiResponse(code = 500, message = "Server error while processing the request."),
            @ApiResponse(code = 401, message = "Unauthorized Request."), @ApiResponse(code = 403, message = "Permission Denied for the request"),
            @ApiResponse(code = 200, message = "Request processed successfully", response=Map.class, responseContainer="Map",reference = "{\\\\\\\"execution\\\\\\\":{\\\\\\\"id\\\\\\\":10203,\\\\\\\"orderId\\\\\\\":9821,\\\\\\\"executionStatus\\\\\\\":\\\\\\\"-1\\\\\\\",\\\\\\\"comment\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"htmlComment\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"cycleId\\\\\\\":-1,\\\\\\\"cycleName\\\\\\\":\\\\\\\"Ad hoc\\\\\\\",\\\\\\\"versionId\\\\\\\":10401,\\\\\\\"versionName\\\\\\\":\\\\\\\"szdfxgcvhbjjjj\\\\\\\",\\\\\\\"projectId\\\\\\\":10100,\\\\\\\"createdBy\\\\\\\":\\\\\\\"vm_admin\\\\\\\",\\\\\\\"modifiedBy\\\\\\\":\\\\\\\"vm_admin\\\\\\\",\\\\\\\"issueId\\\\\\\":11725,\\\\\\\"issueKey\\\\\\\":\\\\\\\"SONY-1386\\\\\\\",\\\\\\\"summary\\\\\\\":\\\\\\\"SONY Project\\\\\\\",\\\\\\\"label\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"component\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"projectKey\\\\\\\":\\\\\\\"SONY\\\\\\\",\\\\\\\"executionDefectCount\\\\\\\":0,\\\\\\\"stepDefectCount\\\\\\\":0,\\\\\\\"totalDefectCount\\\\\\\":0,\\\\\\\"executionWorkflowStatus\\\\\\\":\\\\\\\"COMPLETED\\\\\\\", \\\\\\\"executionEstimatedTime\\\\\\\":\\\\\\\"2hr 20min\\\\\\\", \\\\\\\"executionTimeLogged\\\\\\\":\\\\\\\"3hr\\\\\\\"}}")})
    @PUT
    @Path("/{scheduleId}/loggedTime/modify")
    public Response modifyLoggedTime(@ApiParam(value = "Schedule Id") @PathParam("scheduleId") Integer scheduleId,
                                      @ApiParam(value = "Time Logged By User") @QueryParam("timeLogged") String timeLogged) {
        JSONObject jsonObject = new JSONObject();
        Schedule schedule = null;
        try {
            schedule = executionWorkflowResourceDelegate.getExecution(scheduleId);
            if (Objects.isNull(schedule)) {
				return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Execution" + String.format(" ID : %s",scheduleId)), Status.BAD_REQUEST, null);
            }
            Response response = validatePermissionForSchedule(schedule, Boolean.FALSE);
            if(Objects.nonNull(response)) {
                return response;
            }
            Project project = projectManager.getProjectObj(schedule.getProjectId());
            JiraUtil.setProjectThreadLocal(project);

            if(Objects.isNull(schedule.getExecutionWorkflowStatus())) {
                String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("workflow.schedule.not.started.error");
                log.error(String.format(ERROR_LOG_MESSAGE, Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
                return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("workflow.schedule.not.started.error"), Status.BAD_REQUEST, null);
            }

            if(!verifyExecutionLevelPermission(authContext.getLoggedInUser(),project.getId(), PermissionType.ZEPHYR_EDIT_EXECUTION.toString())) {
                String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error") + " Missing " +
                        PermissionType.ZEPHYR_EDIT_EXECUTION.toString() + " permission.";
                return constructErrorResponse(jsonObject, errorMessage, Status.FORBIDDEN, null);
            }

            executionWorkflowResourceDelegate.modifyLoggedTimeByUser(schedule,timeLogged);
        } catch(InvalidDurationException e) {
            return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("workflow.invalid.time.logged.label", timeLogged), Status.BAD_REQUEST, e);
        } catch(Exception exception) {
            return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.internal.server.error"), Status.INTERNAL_SERVER_ERROR, exception);
        }
        return scheduleResourceDelegate.getExecution(schedule, null);
    }

	@ApiOperation(value = "Reopen Execution", notes = "Reopen workflow for the execution.")
	@ApiImplicitParams(@ApiImplicitParam(name = "response", value = "{\\\"execution\\\":{\\\"id\\\":10203,\\\"orderId\\\":9821,\\\"executionStatus\\\":\\\"-1\\\",\\\"comment\\\":\\\"\\\",\\\"htmlComment\\\":\\\"\\\",\\\"cycleId\\\":-1,\\\"cycleName\\\":\\\"Ad hoc\\\",\\\"versionId\\\":10401,\\\"versionName\\\":\\\"szdfxgcvhbjjjj\\\",\\\"projectId\\\":10100,\\\"createdBy\\\":\\\"vm_admin\\\",\\\"modifiedBy\\\":\\\"vm_admin\\\",\\\"issueId\\\":11725,\\\"issueKey\\\":\\\"SONY-1386\\\",\\\"summary\\\":\\\"SONY Project\\\",\\\"label\\\":\\\"\\\",\\\"component\\\":\\\"\\\",\\\"projectKey\\\":\\\"SONY\\\",\\\"executionDefectCount\\\":0,\\\"stepDefectCount\\\":0,\\\"totalDefectCount\\\":0,\\\"executionWorkflowStatus\\\":\\\"STARTED\\\", \\\"executionEstimatedTime\\\":\\\"2hr 20min\\\", \\\"executionTimeLogged\\\":\\\"0hr\\\"}}"))
	@ApiResponses({@ApiResponse(code = 400, message = "Invalid Request Parameters."),
			@ApiResponse(code = 500, message = "Server error while processing the request."),
			@ApiResponse(code = 401, message = "Unauthorized Request."), @ApiResponse(code = 403, message = "Permission Denied for the request"),
			@ApiResponse(code = 200, message = "Request processed successfully", response=Map.class, responseContainer="Map", reference = "{\\\\\\\"execution\\\\\\\":{\\\\\\\"id\\\\\\\":10203,\\\\\\\"orderId\\\\\\\":9821,\\\\\\\"executionStatus\\\\\\\":\\\\\\\"-1\\\\\\\",\\\\\\\"comment\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"htmlComment\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"cycleId\\\\\\\":-1,\\\\\\\"cycleName\\\\\\\":\\\\\\\"Ad hoc\\\\\\\",\\\\\\\"versionId\\\\\\\":10401,\\\\\\\"versionName\\\\\\\":\\\\\\\"szdfxgcvhbjjjj\\\\\\\",\\\\\\\"projectId\\\\\\\":10100,\\\\\\\"createdBy\\\\\\\":\\\\\\\"vm_admin\\\\\\\",\\\\\\\"modifiedBy\\\\\\\":\\\\\\\"vm_admin\\\\\\\",\\\\\\\"issueId\\\\\\\":11725,\\\\\\\"issueKey\\\\\\\":\\\\\\\"SONY-1386\\\\\\\",\\\\\\\"summary\\\\\\\":\\\\\\\"SONY Project\\\\\\\",\\\\\\\"label\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"component\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"projectKey\\\\\\\":\\\\\\\"SONY\\\\\\\",\\\\\\\"executionDefectCount\\\\\\\":0,\\\\\\\"stepDefectCount\\\\\\\":0,\\\\\\\"totalDefectCount\\\\\\\":0,\\\\\\\"executionWorkflowStatus\\\\\\\":\\\\\\\"STARTED\\\\\\\", \\\\\\\"executionEstimatedTime\\\\\\\":\\\\\\\"2hr 20min\\\\\\\", \\\\\\\"executionTimeLogged\\\\\\\":\\\\\\\"0hr\\\\\\\"}}")})
	@PUT
	@Path("/{scheduleId}/reopen")
	public Response reopenExecution(@ApiParam(value = "Schedule Id") @PathParam("scheduleId") Integer scheduleId) {
		JSONObject jsonObject = new JSONObject();
		Schedule schedule = null;
		try {
			schedule = executionWorkflowResourceDelegate.getExecution(scheduleId);
			if (Objects.isNull(schedule)) {
				return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Execution" + String.format(" ID : %s",scheduleId)), Status.BAD_REQUEST, null);
			}
			Response response = validatePermissionForSchedule(schedule, Boolean.TRUE);
			if(Objects.nonNull(response)) {
				return response;
			}
			Project project = projectManager.getProjectObj(schedule.getProjectId());
			JiraUtil.setProjectThreadLocal(project);

            if(!verifyExecutionLevelPermission(authContext.getLoggedInUser(),project.getId(), PermissionType.ZEPHYR_EDIT_EXECUTION.toString())) {
                String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error") + " Missing " +
                        PermissionType.ZEPHYR_EDIT_EXECUTION.toString() + " permission.";
                return constructErrorResponse(jsonObject, errorMessage, Status.FORBIDDEN, null);
            }

			executionWorkflowResourceDelegate.reopenExecution(schedule);
		} catch(Exception exception) {
			return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.internal.server.error"), Status.INTERNAL_SERVER_ERROR, exception);
		}
		return scheduleResourceDelegate.getExecution(schedule, null);
	}

    /**
     * Constructs error response.
     * @param jsonObject
     * @param errorMessage
     * @param status
     * @param exception
     * @return
     */
	private Response constructErrorResponse(JSONObject jsonObject, String errorMessage, Status status, Exception exception) {
        try {
        	String finalErrorMessage = String.format(ERROR_LOG_MESSAGE, status.getStatusCode(), status, errorMessage);
            log.error(finalErrorMessage);
			jsonObject.put("error", errorMessage);
			return Response.status(status).entity(jsonObject != null ? jsonObject.toString() : finalErrorMessage).cacheControl(ZephyrCacheControl.never()).build();
		} catch (JSONException e) {
			log.error("Error while constructing the error response");
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

    /**
     * validation for schedule.
     * @param schedule
     * @param isWorkFlowReopen
     * @return
     */
	private Response validatePermissionForSchedule(Schedule schedule, Boolean isWorkFlowReopen) {
		Project project = projectManager.getProjectObj(schedule.getProjectId());
		if (project == null) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "projectId ", "");
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "Invalid Project", errorMessage, errorMessage);
        }
        boolean hasPermission = JiraUtil.hasBrowseProjectPermission(project, authContext.getLoggedInUser());
        if (!hasPermission) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.project.permission.error", "Execution", String.valueOf(project.getName()));
            log.error(String.format(ERROR_LOG_MESSAGE, Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "Insufficient Project permissions", errorMessage, errorMessage);
        }
        
        Issue issue = issueManager.getIssueObject(Long.valueOf(schedule.getIssueId()));
        boolean hasViewIssuePermission = JiraUtil.hasIssueViewPermission(null, issue, authContext.getLoggedInUser());
        if (!hasViewIssuePermission) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.project.permission.error", "Execution", String.valueOf(schedule.getProjectId()));
            log.error(String.format(ERROR_LOG_MESSAGE, Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "Insufficient Issue permissions", errorMessage, errorMessage);
        } 
        boolean isWorkflowDisabled = executionWorkflowResourceDelegate.isExecutionWorkflowDisabled(project.getId());
        if(isWorkflowDisabled) {
        	 String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("workflow.project.disabled.error.label", project.getName());
             log.error(String.format(ERROR_LOG_MESSAGE, Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
             return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "Workflow not enabled", errorMessage, errorMessage);
        }

        String workflowStatus = null != schedule.getExecutionWorkflowStatus() ?  schedule.getExecutionWorkflowStatus().getName() : StringUtils.EMPTY;

        if(StringUtils.isNotBlank(workflowStatus) && workflowStatus.equalsIgnoreCase(ExecutionWorkflowStatus.COMPLETED.name()) && !isWorkFlowReopen) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("workflow.schedule.modify.error");
            log.error(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, Response.Status.BAD_REQUEST.getStatusCode()+StringUtils.EMPTY, errorMessage, errorMessage);
        }

        if (Objects.isNull(schedule.getCycle())) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("workflow.schedule.adhoc.cycle.error");
            log.error(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, Response.Status.BAD_REQUEST.getStatusCode()+StringUtils.EMPTY, errorMessage, errorMessage);
        }

        return null;
    }

    /**
     * Verify edit execution permission.
     * @param user
     * @param projectId
     * @param permissionType
     * @return
     */
    private boolean verifyExecutionLevelPermission(ApplicationUser user, Long projectId, String permissionType) {
        ProjectPermissionKey projectPermissionKey = new ProjectPermissionKey(permissionType);
        return zephyrPermissionManager.validateUserPermission(projectPermissionKey, null, user, projectId);
    }

}
