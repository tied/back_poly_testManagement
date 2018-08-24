package com.thed.zephyr.je.rest;


import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.sun.jersey.spi.container.ResourceFilters;
import com.thed.zephyr.je.operation.JobProgress;
import com.thed.zephyr.je.rest.filter.ZFJApiFilter;
import com.thed.zephyr.je.service.JobProgressService;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.ZephyrCacheControl;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Api(value = "Job Progress API(s)", description = "Following section describes rest resources (API's) pertaining to JobProgressResource")
@Path("jobProgress")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@AnonymousAllowed
@ResourceFilters(ZFJApiFilter.class)
public class JobProgressResource {
    protected final Logger log = Logger.getLogger(JobProgressResource.class);
    private final JiraAuthenticationContext authContext;

    private static final String ERROR_LOG_MESSAGE = "[Error] [Error code: %s %s Error Message : %s]";
    private final JobProgressService jobProgressService;

    public JobProgressResource(final JiraAuthenticationContext authContext, final JobProgressService jobProgressService) {
        this.authContext=authContext;
        this.jobProgressService = jobProgressService;
    }

    @GET
    @ApiOperation(value = "Get job progress status ", notes = "Get job progress with status ")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "response", value = "{\"timeTaken\":\"0 min, 0 sec\",\"stepMessage\":\"\",\"summaryMessage\":\"\",\"errorMessage\":\"\",\"progress\":0.0,\"message\":\"\",\"stepLabel\":\"\",\"stepMessages\":[]}")
    })
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{jobProgressToken}")
    public Response bulkStatusJobProgress(@PathParam("jobProgressToken") String jobProgressToken, @QueryParam("type") String type){
        try {
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

    @ApiOperation(value = "Mark the Job to STOP", notes = "Mark the Job to STOP")
    @ApiImplicitParams({
    	@ApiImplicitParam(name = "response", value = "{\"timeTaken\":\"0 min, 0 sec\",\"stepMessage\":\"\",\"summaryMessage\":\"\",\"errorMessage\":\"\",\"progress\":0.0,\"message\":\"\",\"stepLabel\":\"\",\"stepMessages\":[]}")
    	})
    @PUT()
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{jobProgressToken}")
    public Response updateJobProgressStatus(@PathParam("jobProgressToken") String jobProgressToken, @QueryParam("status") String status) {
        final ApplicationUser user = authContext.getLoggedInUser();

        JSONObject jsonObject = new JSONObject();
        try {
            if (user == null) {
                jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        	/*boolean isJiraAdmin = ComponentAccessor.getGlobalPermissionManager().hasPermission(GlobalPermissionKey.ADMINISTER, user);
        	if(!isJiraAdmin) {
                String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,errorMessage));
                return JiraUtil.getPermissionDeniedErrorResponse(errorMessage);
        	}*/
        } catch (JSONException e) {
            log.error("Error occurred while updating the cycle data.",e);
            return Response.status(Status.BAD_REQUEST).build();
        }
        if(StringUtils.isEmpty(jobProgressToken)){
        	return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, null, "Job details can't be found for empty id", null);
        }
        if(StringUtils.isEmpty(status) || !ApplicationConstants.JOB_STATUS_STOP_STRING.equalsIgnoreCase(status)){
        	return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, null, "Job progress status is not STOP, can't process.", null);
        }
        JobProgress jobProgress = jobProgressService.getJobProgress(jobProgressToken);
        Map<String, Object> response = jobProgressService.completedWithStatusStop(ApplicationConstants.JOB_STATUS_STOPPED, jobProgressToken, user.getUsername());
        if(jobProgress != null) {
            jobProgressService.setCompletedSteps(jobProgressToken, jobProgress.getTotalSteps());
            jobProgressService.setMessage(jobProgressToken,authContext.getI18nHelper().getText("zephyr-je.pdb.importer.init.job.stopped.successfully"));
        }
        if(jobProgress == null){
        	return JiraUtil.buildErrorResponse(Response.Status.NOT_FOUND, null, "Job details not found with key:" + jobProgressToken, null);
        }else if (jobProgress.getCanceledJob()){
        	return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, null, "Job has already been completed:" + jobProgressToken, null);
        }

        return Response.ok(toJson(response)).build();
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

}
