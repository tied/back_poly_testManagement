package com.thed.zephyr.je.rest;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.usercompatibility.UserCompatibilityHelper;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.google.common.collect.ImmutableMap;
import com.sun.jersey.spi.container.ResourceFilters;
import com.thed.zephyr.je.helper.ZNavResourceHelper;
import com.thed.zephyr.je.rest.exception.RESTException;
import com.thed.zephyr.je.rest.filter.ZFJApiFilter;
import com.thed.zephyr.je.service.ColumnLayoutItemManager;
import com.thed.zephyr.je.service.ZFJCacheService;
import com.thed.zephyr.je.service.ZephyrCustomFieldManager;
import com.thed.zephyr.je.vo.ColumnLayoutBean;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.ZephyrCacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Api(value = "Rest end point for Zephyr Navigation(znav)", description = "Rest end point for Zephyr Navigation(znav)")
@Path ("znav")
@Produces ({ MediaType.APPLICATION_JSON})
@Consumes ({ MediaType.APPLICATION_JSON})
@ResourceFilters(ZFJApiFilter.class)
public class ZNavResource {
    protected final Logger log = Logger.getLogger(ZNavResource.class);
    private static final String ERROR_LOG_MESSAGE = "[Error] [Error code: %s %s Error Message : %s]";

	private final JiraAuthenticationContext authContext;
	private ColumnLayoutItemManager columnLayoutItemManager;
	private ZephyrCustomFieldManager zephyrCustomFieldManager;
    private final ZFJCacheService zfjCacheService;

    public ZNavResource(JiraAuthenticationContext authContext,
    		ColumnLayoutItemManager columnLayoutItemManager, ZephyrCustomFieldManager zephyrCustomFieldManager,
                        ZFJCacheService zfjCacheService) {
		this.authContext = authContext;
		this.columnLayoutItemManager=columnLayoutItemManager;
		this.zephyrCustomFieldManager = zephyrCustomFieldManager;
		this.zfjCacheService = zfjCacheService;
	}
    
    /**
     * validates and executes search against zephyr indexes. offset and limit provides a way to define the beginning and the max limit allowed
     * @param zqlFilterId
     * @return
     */
    @ApiOperation(value = "Get Available Columns", notes = "Get Available Columns by Execution Filter Id")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"id\":5,\"userName\":\"vm_admin\",\"executionFilterId\":204,\"columnItemBean\":[{\"id\":63,\"filterIdentifier\":\"Issue Key\",\"orderId\":1,\"visible\":true},{\"id\":64,\"filterIdentifier\":\"Test Summary\",\"orderId\":2,\"visible\":true},{\"id\":65,\"filterIdentifier\":\"Project Name\",\"orderId\":3,\"visible\":true},{\"id\":66,\"filterIdentifier\":\"Priority\",\"orderId\":4,\"visible\":true},{\"id\":67,\"filterIdentifier\":\"Component\",\"orderId\":5,\"visible\":true},{\"id\":68,\"filterIdentifier\":\"Version\",\"orderId\":6,\"visible\":true},{\"id\":69,\"filterIdentifier\":\"Execution Status\",\"orderId\":7,\"visible\":true},{\"id\":70,\"filterIdentifier\":\"Executed By\",\"orderId\":8,\"visible\":true},{\"filterIdentifier\":\"Cycle Name\",\"visible\":false},{\"filterIdentifier\":\"Labels\",\"visible\":false},{\"filterIdentifier\":\"Executed On\",\"visible\":false},{\"filterIdentifier\":\"Creation Date\",\"visible\":false},{\"filterIdentifier\":\"Execution Defect(s)\",\"visible\":false},{\"filterIdentifier\":\"Assignee\",\"visible\":false}]}")})
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/availableColumns")
    public Response getAvailableColumns(@QueryParam("executionFilterId") final Integer zqlFilterId) {
        ApplicationUser loggedInUser = authContext.getLoggedInUser();
        if(loggedInUser ==  null) {
    		return buildLoginErrorResponse();
    	}
    	//Refactored and Moved code to Helper
    	ZNavResourceHelper znavResourceHelper = new ZNavResourceHelper(columnLayoutItemManager, zephyrCustomFieldManager,zfjCacheService,authContext);
        // check if user have right permission on given ZQLFilter.
        if(znavResourceHelper.havePermissionOnZqlFilter(zqlFilterId, UserCompatibilityHelper.getKeyForUser(loggedInUser.getDirectoryUser()))){
            ColumnLayoutBean columnLayoutBean = znavResourceHelper.findAvailableColumns(loggedInUser.getDirectoryUser(),0,zqlFilterId);
            return Response.ok().entity(columnLayoutBean).cacheControl(ZephyrCacheControl.never()).build();
        } else{
            return buildNoPermissionOnZQLFilterErrorResponse();
        }
    }

    private Response buildNoPermissionOnZQLFilterErrorResponse(){
        log.error("Operation not permitted, no right permissions on given ZQLFilter!");
        final I18nHelper i18nHelper = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
        Response.ResponseBuilder builder = Response.status(Response.Status.NOT_ACCEPTABLE);
        builder.type(MediaType.APPLICATION_JSON);
        builder.entity(ImmutableMap.of("Error", i18nHelper.getText("zephyr.je.znav.zql.permission.error")));
        log.error(String.format(ERROR_LOG_MESSAGE,Status.NOT_ACCEPTABLE.getStatusCode(), Status.NOT_ACCEPTABLE,i18nHelper.getText("zephyr.je.znav.zql.permission.error")));
        return builder.build();
    }
    

    /**
     * validates and executes search against zephyr indexes. offset and limit provides a way to define the beginning and the max limit allowed 
     * @param columnLayoutBean
     * @return
     */
    @ApiOperation(value = "Create Column Selection", notes = "Create/Save Column Selection")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{\"userName\":\"vm_admin\",\"executionFilterId\":\"204\",\"columnItemBean\":[{\"filterIdentifier\":\"Cycle Name\",\"visible\":false,\"orderId\":0},{\"filterIdentifier\":\"Issue Key\",\"visible\":true,\"orderId\":1},{\"filterIdentifier\":\"Test Summary\",\"visible\":true,\"orderId\":2},{\"filterIdentifier\":\"Project Name\",\"visible\":true,\"orderId\":3},{\"filterIdentifier\":\"Priority\",\"visible\":true,\"orderId\":4},{\"filterIdentifier\":\"Component\",\"visible\":true,\"orderId\":5},{\"filterIdentifier\":\"Version\",\"visible\":true,\"orderId\":6},{\"filterIdentifier\":\"Execution Status\",\"visible\":true,\"orderId\":7},{\"filterIdentifier\":\"Executed By\",\"visible\":true,\"orderId\":8},{\"filterIdentifier\":\"Creation Date\",\"visible\":false,\"orderId\":9},{\"filterIdentifier\":\"Executed On\",\"visible\":false,\"orderId\":10}]}"),
            @ApiImplicitParam(name = "response", value = "{\"id\":5,\"userName\":\"vm_admin\",\"executionFilterId\":204,\"columnItemBean\":[{\"id\":63,\"filterIdentifier\":\"Issue Key\",\"orderId\":1,\"visible\":true},{\"id\":64,\"filterIdentifier\":\"Test Summary\",\"orderId\":2,\"visible\":true},{\"id\":65,\"filterIdentifier\":\"Project Name\",\"orderId\":3,\"visible\":true},{\"id\":66,\"filterIdentifier\":\"Priority\",\"orderId\":4,\"visible\":true},{\"id\":67,\"filterIdentifier\":\"Component\",\"orderId\":5,\"visible\":true},{\"id\":68,\"filterIdentifier\":\"Version\",\"orderId\":6,\"visible\":true},{\"id\":69,\"filterIdentifier\":\"Execution Status\",\"orderId\":7,\"visible\":true},{\"id\":70,\"filterIdentifier\":\"Executed By\",\"orderId\":8,\"visible\":true},{\"filterIdentifier\":\"Cycle Name\",\"visible\":false},{\"filterIdentifier\":\"Labels\",\"visible\":false},{\"filterIdentifier\":\"Executed On\",\"visible\":false},{\"filterIdentifier\":\"Creation Date\",\"visible\":false},{\"filterIdentifier\":\"Execution Defect(s)\",\"visible\":false},{\"filterIdentifier\":\"Assignee\",\"visible\":false}]}")})
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/createColumnSelection")
    public Response saveColumnSelector(final ColumnLayoutBean columnLayoutBean) {
        ApplicationUser loggedInUser = authContext.getLoggedInUser();
    	if(loggedInUser == null) {
            return buildLoginErrorResponse();
    	}
    	if(columnLayoutBean == null) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,authContext.getI18nHelper().getText("schedule.status.null.error", "columnLayoutBean")));
			throw new RESTException(Status.BAD_REQUEST, authContext.getI18nHelper().getText("schedule.status.null.error", "columnLayoutBean"));
    	}
    	//Refactored and Moved code to Helper
    	ZNavResourceHelper znavResourceHelper = new ZNavResourceHelper(columnLayoutItemManager, zephyrCustomFieldManager,zfjCacheService,authContext);
        // check if user have right permission on given ZQLFilter.
        if(znavResourceHelper.havePermissionOnZqlFilter(columnLayoutBean.getExecutionFilterId(), UserCompatibilityHelper.getKeyForUser(loggedInUser.getDirectoryUser()))){
            Response response = znavResourceHelper.saveAvailableColumns(JiraUtil.getLoggedInUser(authContext), columnLayoutBean);
            return response;
        } else{
            return buildNoPermissionOnZQLFilterErrorResponse();
        }
    }
    
    /**
     * Updates the Column  
     * @param columnLayoutId
     * @param columnLayoutBean
     * @return
     */
    @ApiOperation(value = "Update Column Selection", notes = "Update Column Selection by Column Layout Id")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{\"id\":5,\"userName\":\"vm_admin\",\"executionFilterId\":204,\"columnItemBean\":[{\"id\":\"63\",\"filterIdentifier\":\"Issue Key\",\"orderId\":\"1\",\"visible\":true},{\"id\":\"64\",\"filterIdentifier\":\"Test Summary\",\"orderId\":\"2\",\"visible\":true},{\"id\":\"65\",\"filterIdentifier\":\"Project Name\",\"orderId\":\"3\",\"visible\":true},{\"id\":\"66\",\"filterIdentifier\":\"Priority\",\"orderId\":\"4\",\"visible\":true},{\"id\":\"67\",\"filterIdentifier\":\"Component\",\"orderId\":\"5\",\"visible\":true},{\"id\":\"68\",\"filterIdentifier\":\"Version\",\"orderId\":\"6\",\"visible\":true},{\"filterIdentifier\":\"Cycle Name\",\"orderId\":9,\"visible\":true},{\"filterIdentifier\":\"Executed On\",\"orderId\":10,\"visible\":true},{\"id\":\"68\",\"filterIdentifier\":\"Version\",\"orderId\":\"6\",\"visible\":false}]}"),
            @ApiImplicitParam(name = "response", value = "{\"id\":5,\"userName\":\"vm_admin\",\"executionFilterId\":204,\"columnItemBean\":[{\"id\":63,\"filterIdentifier\":\"Issue Key\",\"orderId\":1,\"visible\":true},{\"id\":64,\"filterIdentifier\":\"Test Summary\",\"orderId\":2,\"visible\":true},{\"id\":65,\"filterIdentifier\":\"Project Name\",\"orderId\":3,\"visible\":true},{\"id\":66,\"filterIdentifier\":\"Priority\",\"orderId\":4,\"visible\":true},{\"id\":67,\"filterIdentifier\":\"Component\",\"orderId\":5,\"visible\":true},{\"id\":69,\"filterIdentifier\":\"Execution Status\",\"orderId\":7,\"visible\":true},{\"id\":71,\"filterIdentifier\":\"Cycle Name\",\"orderId\":9,\"visible\":true},{\"id\":72,\"filterIdentifier\":\"Executed On\",\"orderId\":10,\"visible\":true},{\"filterIdentifier\":\"Labels\",\"visible\":false},{\"filterIdentifier\":\"Version\",\"visible\":false},{\"filterIdentifier\":\"Executed By\",\"visible\":false},{\"filterIdentifier\":\"Creation Date\",\"visible\":false},{\"filterIdentifier\":\"Execution Defect(s)\",\"visible\":false},{\"filterIdentifier\":\"Assignee\",\"visible\":false}]}")})
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/updateColumnSelection/{id}")
    public Response updateColumnSelector(@PathParam("id") final Integer columnLayoutId, final ColumnLayoutBean columnLayoutBean) {
        ApplicationUser loggedInUser = authContext.getLoggedInUser();
    	if(loggedInUser == null) {
            return buildLoginErrorResponse();
    	}
    	if(columnLayoutId == null || columnLayoutId == 0) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,authContext.getI18nHelper().getText("schedule.status.null.error", "id")));
			throw new RESTException(Status.BAD_REQUEST, authContext.getI18nHelper().getText("schedule.status.null.error", "id"));
    	}
    	
    	if(columnLayoutBean == null) {
            log.error(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST,authContext.getI18nHelper().getText("schedule.status.null.error", "columnLayoutBean")));
			throw new RESTException(Status.BAD_REQUEST, authContext.getI18nHelper().getText("schedule.status.null.error", "columnLayoutBean"));
    	}
    	
    	//Refactored and Moved code to Helper
    	ZNavResourceHelper znavResourceHelper = new ZNavResourceHelper(columnLayoutItemManager, zephyrCustomFieldManager,zfjCacheService,authContext);
        // check if user have right permission on given ZQLFilter.
        if(znavResourceHelper.havePermissionOnZqlFilter(columnLayoutBean.getExecutionFilterId(), UserCompatibilityHelper.getKeyForUser(loggedInUser.getDirectoryUser()))){
            Response response = znavResourceHelper.updateAvailableColumns(columnLayoutId,JiraUtil.getLoggedInUser(authContext),columnLayoutBean);
            return response;
        } else{
            return buildNoPermissionOnZQLFilterErrorResponse();
        }
    }

    /**
     * Common logged in user error response.
     * @return
     */
    private Response buildLoginErrorResponse() {

        JSONObject jsonObject = new JSONObject();
        try {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(), Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
            jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
        } catch (JSONException e) {
            log.error("Error occurred during response object creation.",e);
            return Response.status(Status.BAD_REQUEST).build();
        }
        return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
    }
}
