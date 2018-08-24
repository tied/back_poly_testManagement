package com.thed.zephyr.je.rest;

//import com.atlassian.jira.gadgets.system.*;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.timezone.TimeZoneManager;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.sun.jersey.spi.container.ResourceFilters;
import com.thed.zephyr.je.rest.delegate.ChartResourceDelegate;
import com.thed.zephyr.je.rest.exception.RESTException;
import com.thed.zephyr.je.rest.filter.ZFJApiFilter;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.ZephyrCacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Api(value = "Chart Resource API(s)", description = "Following section describes the rest resources pertaining to ChartResource")
@Path ("zchart")
@Produces ({ MediaType.APPLICATION_JSON })
@AnonymousAllowed
@ResourceFilters(ZFJApiFilter.class)
public class ChartResource {//extends SearchQueryBackedResource{

    private final JiraAuthenticationContext authenticationContext;
    private final SearchProvider searchProvider;
    private final TimeZoneManager timeZoneManager;
    private final ChartResourceDelegate chartResourceDelegate;
    private final ProjectManager projectManager;

    public static final String PROJECT_KEY = "projectKey";
    public static final String DAYS_NAME = "daysPrevious";
    private static final String PERIOD_NAME = "periodName";
    public static final String VERSION_LABEL = "versionLabel";
    private static final String IS_CUMULATIVE = "isCumulative";
    private static final String SHOW_UNRESOLVED_TREND = "showUnresolvedTrend";
    private static final String RETURN_DATA = "returnData";
    private static final String PROJECT_ID = "projectId";
	private static final String ERROR_LOG_MESSAGE = "[Error] [Error code: %s %s Error Message : %s]";

    protected final Logger log = Logger.getLogger(ScheduleResource.class);
   
    public ChartResource(final JiraAuthenticationContext authenticationContext,
			final SearchProvider searchProvider,
			final TimeZoneManager timeZoneManager,final ChartResourceDelegate chartResourceDelegate,
			final ProjectManager projectManager) {
		this.authenticationContext=authenticationContext;
		this.searchProvider = searchProvider;
		this.timeZoneManager = timeZoneManager;
		this.chartResourceDelegate=chartResourceDelegate;
		this.projectManager=projectManager;
	}

	@ApiOperation(value = "Get Issue Status by Project", notes = "Get Issue Statuses by Project Id")
	@ApiImplicitParams({ @ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "{\"IssueStatusesOptionsList\":[{\"label\":\"Done\",\"value\":\"10001\"},{\"label\":\"In Progress\",\"value\":\"3\"},{\"label\":\"To Do\",\"value\":\"10000\"}]}")})
	@GET
    @Path("/issueStatuses")
    public Response getIssueStatusesByProject(@Context HttpServletRequest request,
            @QueryParam (PROJECT_ID) @DefaultValue ("10000") final Long pId){

    	List<HashMap<String,String>> mapList = new ArrayList<HashMap<String,String>>();
    	Project project = ComponentAccessor.getProjectManager().getProjectObj(pId);
    	if(project == null) {
            log.error(String.format(ERROR_LOG_MESSAGE,Response.Status.BAD_REQUEST.getStatusCode(), Response.Status.BAD_REQUEST,authenticationContext.getI18nHelper().getText("schedule.execute.update.stepresult.invalid.id",PROJECT_ID)));
			throw new RESTException(javax.ws.rs.core.Response.Status.BAD_REQUEST, authenticationContext.getI18nHelper().getText("schedule.execute.update.stepresult.invalid.id",PROJECT_ID));
    	}
    	
    	//Validate Project Permission
    	boolean hasPermission = JiraUtil.hasBrowseProjectPermission(project.getId(),authenticationContext.getLoggedInUser());
    	if(!hasPermission) {
       		String errorMessage = authenticationContext.getI18nHelper().getText("zephyr.project.permission.error","Get","IssueStatuses");
            log.error(String.format(ERROR_LOG_MESSAGE, Response.Status.FORBIDDEN.getStatusCode(),Response.Status.FORBIDDEN,errorMessage));
       		return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "Insufficient Project permissions", errorMessage, errorMessage );
		}    	
    	
    	Collection<Status> statusList = JiraUtil.getIssueStatusesForProject(pId);    	
    	for (Status gv: statusList) {
        	HashMap<String,String> statusMap = new HashMap<String,String>();
        	statusMap.put("value", gv.getId());
        	statusMap.put("label", gv.getNameTranslation());
        	mapList.add(statusMap);
    	}

    	JSONObject ob = new JSONObject();

    	try{
        	ob.put("IssueStatusesOptionsList", mapList);
    	}
    	catch(JSONException je){
    		log.error("Exception occurred while getting issue statuses by project.");
    		je.printStackTrace();
			return Response.status(Response.Status.BAD_REQUEST).build();
    	}
  
	    return Response.ok(ob.toString()).cacheControl(ZephyrCacheControl.never()).build();
    }

    @ApiOperation(value = "Generate Test Created Data", notes = "Generate Test's Created Data by Project Key")
	@ApiImplicitParams({ @ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "{\"testsCreated-OptionsList\":[{\"label\":\"Done\",\"value\":\"10001\"},{\"label\":\"In Progress\",\"value\":\"3\"},{\"label\":\"To Do\",\"value\":\"10000\"}]}")})
	@GET
    @Path ("/testsCreated")
    public Response generateTestsCreatedData(@Context HttpServletRequest request,
            @QueryParam (PROJECT_KEY) final String pkey,
            @QueryParam (DAYS_NAME) @DefaultValue ("30") final String days,
            @QueryParam (PERIOD_NAME) @DefaultValue ("daily") final String periodName) {

    	if(StringUtils.isBlank(pkey)) {
            log.error(String.format(ERROR_LOG_MESSAGE, Response.Status.BAD_REQUEST.getStatusCode(),Response.Status.BAD_REQUEST,authenticationContext.getI18nHelper().getText("schedule.execute.update.stepresult.invalid.id", "projectKey")));
    		return Response.status(Response.Status.BAD_REQUEST).entity(ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().
    				getText("schedule.execute.update.stepresult.invalid.id", "projectKey")).build();
    	}
    	Project project = projectManager.getProjectObjByKey(pkey);
        if (project == null) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "projectKey", pkey);
            log.error(String.format(ERROR_LOG_MESSAGE, Response.Status.BAD_REQUEST.getStatusCode(),Response.Status.BAD_REQUEST,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "Invalid Project", errorMessage, errorMessage);
        }
        JiraUtil.setProjectThreadLocal(project);
    	return chartResourceDelegate.generateTestsCreatedData(request, pkey, days, periodName);
    }

	@GET
	@Path ("/stats")
	public Response generateTestsCreatedData(@Context HttpServletRequest request) {
		Map<String, Integer> map = chartResourceDelegate.getStats(request);
		if(map != null && map.size()>0){
			return Response.ok(map).build();
		}else {
			return Response.ok().build();
		}
	}

}