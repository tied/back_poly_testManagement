package com.thed.zephyr.je.rest;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.util.I18nHelper;
import com.google.common.collect.ImmutableMap;
import com.sun.jersey.spi.container.ResourceFilters;
import com.thed.zephyr.je.rest.exception.RESTException;
import com.thed.zephyr.je.rest.filter.ZFJApiFilter;
import com.thed.zephyr.je.service.ZephyrCustomFieldManager;
import com.thed.zephyr.je.zql.model.ZQLFilter;
import com.thed.zephyr.je.zql.service.ZQLFilterManager;
import com.thed.zephyr.util.ZQLFilterUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.log4j.Logger;
import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.google.common.collect.ImmutableMap;
import com.thed.zephyr.je.helper.ScheduleSearchResourceHelper;
import com.thed.zephyr.je.rest.exception.RESTException;
import com.thed.zephyr.je.service.CycleManager;
import com.thed.zephyr.je.service.TeststepManager;
import com.thed.zephyr.je.zql.core.AutoCompleteJsonGenerator;
import com.thed.zephyr.je.zql.core.SearchService;
import com.thed.zephyr.je.zql.core.ZephyrClauseHandlerFactory;
import com.thed.zephyr.je.zql.model.ZQLFilter;
import com.thed.zephyr.je.zql.service.ZQLFilterManager;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.ZQLFilterUtils;
import com.thed.zephyr.util.ZephyrCacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import java.util.HashMap;
import java.util.Map;

@Api(value = "Search Resource API(s)", description = "Following section describes the rest resources (API's) pertaining to SearchResource")
@Path ("zql")
@Produces ({ MediaType.APPLICATION_JSON})
@Consumes ({ MediaType.APPLICATION_JSON})
@ResourceFilters(ZFJApiFilter.class)
public class ScheduleSearchResource {
    protected final Logger log = Logger.getLogger(ScheduleSearchResource.class);
	private static final String ERROR_LOG_MESSAGE = "[Error] [Error code: %s %s Error Message : %s]";

	private final JiraAuthenticationContext authContext;
    private final ZQLFilterManager zqlFilterManager;
    private SearchService searchService;
    private final IssueManager issueManager;
    private final CycleManager cycleManager;
    private final VersionManager versionManager;
	private final ZephyrClauseHandlerFactory zephyrClauseHandlerFactory;
	private final AutoCompleteJsonGenerator autocompleteJsonGenerator;
	private final TeststepManager teststepManager;
	private final ZephyrCustomFieldManager zephyrCustomFieldManager;

    public ScheduleSearchResource(JiraAuthenticationContext authContext,
    		SearchService searchService,
    		IssueManager issueManager,CycleManager cycleManager,
    		VersionManager versionManager,
            ZQLFilterManager zqlFilterManager,
    		ZephyrClauseHandlerFactory zephyrClauseHandlerFactory,
    		AutoCompleteJsonGenerator autocompleteJsonGenerator,TeststepManager teststepManager,ZephyrCustomFieldManager zephyrCustomFieldManager) {
    	this.authContext=authContext;
    	this.searchService=searchService;
    	this.issueManager=issueManager;
    	this.cycleManager=cycleManager;
    	this.versionManager=versionManager;
        this.zqlFilterManager = zqlFilterManager;
    	this.zephyrClauseHandlerFactory=zephyrClauseHandlerFactory;
    	this.autocompleteJsonGenerator=autocompleteJsonGenerator;
    	this.teststepManager=teststepManager;
    	this.zephyrCustomFieldManager=zephyrCustomFieldManager;
    }

    
    /**
     * validates and executes search against zephyr indexes. offset and limit provides a way to define the beginning and the max limit allowed 
     * @param zqlQuery Optional (if filterId is populated, preferred over filterId)
     * @param filterId Optional (if query is populated)
     * @param offset
     * @param maxRecords
     * @param expand
     * @return
     */
	@ApiOperation(value = "Execute Search to Get Search Result", notes = "Execute Search to Get ZQL Search Result by zqlQuery")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "{\"executions\":[{\"id\":409,\"orderId\":1,\"cycleId\":-1,\"cycleName\":\"Ad hoc\",\"issueId\":\"10125\",\"issueKey\":\"IE-1\",\"issueSummary\":\"cx\",\"labels\":[],\"issueDescription\":\"\",\"projectKey\":\"IE\",\"projectId\":10201,\"project\":\"IE\",\"projectAvatarId\":10224,\"priority\":\"Major\",\"components\":[],\"versionId\":-1,\"versionName\":\"Unscheduled\",\"status\":{\"id\":-1,\"name\":\"UNEXECUTED\",\"description\":\"The test has not yet been executed.\",\"color\":\"#A0A0A0\",\"type\":0},\"executedOn\":\"\",\"creationDate\":\"27/Apr/16\",\"comment\":\"\",\"htmlComment\":\"\",\"executedBy\":\"\",\"executedByUserName\":\"\",\"executionDefects\":[],\"stepDefects\":[],\"executionDefectCount\":0,\"stepDefectCount\":0,\"totalDefectCount\":0,\"executedByDisplay\":\"\",\"assignee\":\"\",\"assigneeUserName\":\"\",\"assigneeDisplay\":\"\"}],\"currentIndex\":1,\"maxResultAllowed\":20,\"linksNew\":[1],\"totalCount\":1,\"executionIds\":[]}")})
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/executeSearch")
    @AnonymousAllowed
    public Response executeSearch(@QueryParam ("zqlQuery") String zqlQuery, @QueryParam("filterId") final Integer filterId,
    		@QueryParam ("offset") Integer offset,@QueryParam ("maxRecords") final Integer maxRecords,@QueryParam ("expand") final String expand) {
		JSONObject jsonResponse = new JSONObject();
		try {
			if(authContext.getLoggedInUser() == null && !JiraUtil.hasAnonymousPermission(authContext.getLoggedInUser())) {
				jsonResponse.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
				return Response.status(Status.UNAUTHORIZED).entity(jsonResponse.toString()).cacheControl(ZephyrCacheControl.never()).build();
			}
		} catch (JSONException e) {
			log.error("Error occurred while execute search.",e);
			return Response.status(Status.BAD_REQUEST).build();
		}
    	//Handle JIRA ZFJ-1789. If offset is -ve, change it to 0
    	if(offset != null) {
    		offset =  Integer.max(offset, 0);
    	}
    	
    	Integer maxResultToHit = maxRecords;
    	if(null == maxResultToHit || maxResultToHit < 0)
    		maxResultToHit = 0;
    	//Refactored and Moved code to Helper
    	ScheduleSearchResourceHelper searchResourceHelper = new ScheduleSearchResourceHelper(authContext.getLoggedInUser(),
    			searchService,zephyrClauseHandlerFactory,issueManager,cycleManager,teststepManager,versionManager,zephyrCustomFieldManager);

        if(filterId != null && zqlQuery == null){
            ZQLFilter zqlFilter = zqlFilterManager.getZQLFilter(filterId);
            if(zqlFilter != null) {
                /*If filter is specified, we must make sure user has permission to access the filter*/
                if(!ZQLFilterUtils.isFilterAccessibleByUser(zqlFilter, authContext.getLoggedInUser())) {
                    log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("zephyr.common.retrieve.error")));
                    throw new RESTException(Response.Status.BAD_REQUEST, ImmutableMap.of("Error", ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.retrieve.error")));
                }
                zqlQuery = zqlFilter.getZqlQuery();
            }
        }

    	return searchResourceHelper.performZQLSearch(zqlQuery, offset, maxResultToHit,expand,
				searchResourceHelper,null);
    }

    
    /**
     * validates and executes search against zephyr indexes. offset and limit provides a way to define the beginning and the max limit allowed 
     * @return
     */
	@ApiOperation(value = "Get Search Clauses", notes = "Get List of Search Clauses")
	@ApiImplicitParams({ @ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "{\"clauses\":[\"issue\",\"component\",\"priority\",\"project\",\"executionStatus\",\"executedBy\",\"cycleName\",\"cycleId\",\"executionDate\",\"creationDate\",\"execution\",\"fixVersion\",\"summary\",\"executionDefectKey\",\"labels\",\"assignee\"]}")})
	@GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/clauses")
    public Response getClauses() {

        JSONObject jsonResponse = new JSONObject();
        try {
            if(authContext.getLoggedInUser() == null) {
                jsonResponse.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                return Response.status(Status.UNAUTHORIZED).entity(jsonResponse.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (JSONException e) {
            log.error("Error occurred while get clauses.",e);
            return Response.status(Status.BAD_REQUEST).build();
        }
    	//Refactored and Moved code to Helper
    	ScheduleSearchResourceHelper searchResourceHelper = new ScheduleSearchResourceHelper(authContext.getLoggedInUser(),
    			searchService,zephyrClauseHandlerFactory,issueManager,versionManager,cycleManager);
    	JSONObject jsonObject = searchResourceHelper.getClauses();
    	if(jsonObject == null) {
    		Map<String,String> errorMap = new HashMap<String, String>(); 
			ResponseBuilder builder = Response.status(Response.Status.NOT_ACCEPTABLE);
			builder.type(MediaType.APPLICATION_JSON);
    		errorMap.put("warning", "No clauses found.");
			builder.entity(errorMap);
            log.error(String.format(ERROR_LOG_MESSAGE, Status.NOT_ACCEPTABLE,errorMap));
			return builder.build();    		
    	} else {
			return Response.ok(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
    	}

    }

    
    
    /**
     * validates and executes search against zephyr indexes. offset and limit provides a way to define the beginning and the max limit allowed
     * @return
     */
	@ApiOperation(value = "Get AutoComplete ZQL Json", notes = "Get AutoComplete JSON Execution")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "{\"jqlFieldZ\":\"[{\\\"value\\\":\\\"assignee\\\",\\\"displayName\\\":\\\"assignee\\\",\\\"auto\\\":\\\"true\\\",\\\"orderable\\\":\\\"true\\\",\\\"searchable\\\":\\\"true\\\",\\\"operators\\\":[\\\"=\\\",\\\"!=\\\",\\\"in\\\",\\\"not in\\\",\\\"is\\\",\\\"is not\\\"],\\\"types\\\":[\\\"java.lang.String\\\"]},{\\\"value\\\":\\\"component\\\",\\\"displayName\\\":\\\"component\\\",\\\"auto\\\":\\\"true\\\",\\\"orderable\\\":\\\"true\\\",\\\"searchable\\\":\\\"true\\\",\\\"operators\\\":[\\\"=\\\",\\\"!=\\\",\\\"in\\\",\\\"not in\\\",\\\"is\\\",\\\"is not\\\"],\\\"types\\\":[\\\"com.atlassian.jira.bc.project.component.ProjectComponent\\\"]},{\\\"value\\\":\\\"creationDate\\\",\\\"displayName\\\":\\\"creationDate\\\",\\\"orderable\\\":\\\"true\\\",\\\"searchable\\\":\\\"true\\\",\\\"operators\\\":[\\\"=\\\",\\\"!=\\\",\\\"in\\\",\\\"not in\\\",\\\"is\\\",\\\"is not\\\",\\\"<\\\",\\\"<=\\\",\\\">\\\",\\\">=\\\"],\\\"types\\\":[\\\"java.util.Date\\\"]},{\\\"value\\\":\\\"cycleName\\\",\\\"displayName\\\":\\\"cycleName\\\",\\\"auto\\\":\\\"true\\\",\\\"orderable\\\":\\\"true\\\",\\\"searchable\\\":\\\"true\\\",\\\"operators\\\":[\\\"=\\\",\\\"is\\\",\\\"is not\\\",\\\"~\\\",\\\"!=\\\",\\\"not in\\\",\\\"!~\\\",\\\"in\\\"],\\\"types\\\":[\\\"com.thed.zephyr.je.model.Cycle\\\"]},{\\\"value\\\":\\\"executedBy\\\",\\\"displayName\\\":\\\"executedBy\\\",\\\"auto\\\":\\\"true\\\",\\\"orderable\\\":\\\"true\\\",\\\"searchable\\\":\\\"true\\\",\\\"operators\\\":[\\\"=\\\",\\\"!=\\\",\\\"in\\\",\\\"not in\\\",\\\"is\\\",\\\"is not\\\"],\\\"types\\\":[\\\"java.lang.String\\\"]},{\\\"value\\\":\\\"execution\\\",\\\"displayName\\\":\\\"execution\\\",\\\"orderable\\\":\\\"true\\\",\\\"searchable\\\":\\\"true\\\",\\\"operators\\\":[\\\"=\\\",\\\"!=\\\",\\\"in\\\",\\\"not in\\\",\\\"<\\\",\\\"<=\\\",\\\">\\\",\\\">=\\\"],\\\"types\\\":[\\\"com.thed.zephyr.je.model.Schedule\\\"]},{\\\"value\\\":\\\"executionDate\\\",\\\"displayName\\\":\\\"executionDate\\\",\\\"orderable\\\":\\\"true\\\",\\\"searchable\\\":\\\"true\\\",\\\"operators\\\":[\\\"=\\\",\\\"!=\\\",\\\"in\\\",\\\"not in\\\",\\\"is\\\",\\\"is not\\\",\\\"<\\\",\\\"<=\\\",\\\">\\\",\\\">=\\\"],\\\"types\\\":[\\\"java.util.Date\\\"]},{\\\"value\\\":\\\"executionDefectKey\\\",\\\"displayName\\\":\\\"executionDefectKey\\\",\\\"orderable\\\":\\\"true\\\",\\\"searchable\\\":\\\"true\\\",\\\"operators\\\":[\\\"=\\\",\\\"!=\\\",\\\"in\\\",\\\"not in\\\",\\\"is\\\",\\\"is not\\\",\\\"<\\\",\\\"<=\\\",\\\">\\\",\\\">=\\\"],\\\"types\\\":[\\\"com.atlassian.jira.issue.Issue\\\"]},{\\\"value\\\":\\\"executionStatus\\\",\\\"displayName\\\":\\\"executionStatus\\\",\\\"auto\\\":\\\"true\\\",\\\"orderable\\\":\\\"true\\\",\\\"searchable\\\":\\\"true\\\",\\\"operators\\\":[\\\"=\\\",\\\"!=\\\",\\\"in\\\",\\\"not in\\\",\\\"is\\\",\\\"is not\\\"],\\\"types\\\":[\\\"com.thed.zephyr.je.config.model.ExecutionStatus\\\"]},{\\\"value\\\":\\\"fixVersion\\\",\\\"displayName\\\":\\\"fixVersion\\\",\\\"auto\\\":\\\"true\\\",\\\"orderable\\\":\\\"true\\\",\\\"searchable\\\":\\\"true\\\",\\\"operators\\\":[\\\"is\\\",\\\">\\\",\\\"was\\\",\\\"<=\\\",\\\"was not in\\\",\\\"was not\\\",\\\">=\\\",\\\"was in\\\",\\\"!=\\\",\\\"in\\\",\\\"changed\\\",\\\"=\\\",\\\"is not\\\",\\\"<\\\",\\\"not in\\\"],\\\"types\\\":[\\\"com.atlassian.jira.project.version.Version\\\"]},{\\\"value\\\":\\\"issue\\\",\\\"displayName\\\":\\\"issue\\\",\\\"orderable\\\":\\\"true\\\",\\\"searchable\\\":\\\"true\\\",\\\"operators\\\":[\\\"=\\\",\\\"!=\\\",\\\"in\\\",\\\"not in\\\",\\\"<\\\",\\\"<=\\\",\\\">\\\",\\\">=\\\"],\\\"types\\\":[\\\"com.atlassian.jira.issue.Issue\\\"]},{\\\"value\\\":\\\"labels\\\",\\\"displayName\\\":\\\"labels\\\",\\\"auto\\\":\\\"true\\\",\\\"orderable\\\":\\\"true\\\",\\\"searchable\\\":\\\"true\\\",\\\"operators\\\":[\\\"=\\\",\\\"!=\\\",\\\"in\\\",\\\"not in\\\",\\\"is\\\",\\\"is not\\\"],\\\"types\\\":[\\\"java.lang.String\\\"]},{\\\"value\\\":\\\"priority\\\",\\\"displayName\\\":\\\"priority\\\",\\\"auto\\\":\\\"true\\\",\\\"orderable\\\":\\\"true\\\",\\\"searchable\\\":\\\"true\\\",\\\"operators\\\":[\\\"=\\\",\\\"!=\\\",\\\"in\\\",\\\"not in\\\",\\\"is\\\",\\\"is not\\\",\\\"<\\\",\\\"<=\\\",\\\">\\\",\\\">=\\\"],\\\"types\\\":[\\\"com.atlassian.jira.issue.priority.Priority\\\"]},{\\\"value\\\":\\\"project\\\",\\\"displayName\\\":\\\"project\\\",\\\"auto\\\":\\\"true\\\",\\\"orderable\\\":\\\"true\\\",\\\"searchable\\\":\\\"true\\\",\\\"operators\\\":[\\\"=\\\",\\\"!=\\\",\\\"in\\\",\\\"not in\\\",\\\"is\\\",\\\"is not\\\"],\\\"types\\\":[\\\"com.atlassian.jira.project.Project\\\"]},{\\\"value\\\":\\\"summary\\\",\\\"displayName\\\":\\\"summary\\\",\\\"orderable\\\":\\\"true\\\",\\\"searchable\\\":\\\"true\\\",\\\"operators\\\":[\\\"~\\\",\\\"!~\\\",\\\"is\\\",\\\"is not\\\"],\\\"types\\\":[\\\"java.lang.String\\\"]}]\",\"reservedWords\":\"[\\\"explain\\\",\\\"select\\\",\\\"isnull\\\",\\\"commit\\\",\\\"isempty\\\",\\\"when\\\",\\\"rowid\\\",\\\"output\\\",\\\"number\\\",\\\"character\\\",\\\"identified\\\",\\\"sqrt\\\",\\\"delimiter\\\",\\\"else\\\",\\\"exclusive\\\",\\\"lock\\\",\\\"catch\\\",\\\"join\\\",\\\"strict\\\",\\\"greater\\\",\\\"if\\\",\\\"between\\\",\\\"order\\\",\\\"having\\\",\\\"in\\\",\\\"byte\\\",\\\"double\\\",\\\"subtract\\\",\\\"outer\\\",\\\"index\\\",\\\"raw\\\",\\\"is\\\",\\\"then\\\",\\\"execute\\\",\\\"input\\\",\\\"as\\\",\\\"defaults\\\",\\\"field\\\",\\\"size\\\",\\\"left\\\",\\\"unique\\\",\\\"difference\\\",\\\"returns\\\",\\\"begin\\\",\\\"modulo\\\",\\\"object\\\",\\\"trans\\\",\\\"minus\\\",\\\"access\\\",\\\"increment\\\",\\\"sum\\\",\\\"long\\\",\\\"into\\\",\\\"uid\\\",\\\"current\\\",\\\"default\\\",\\\"file\\\",\\\"goto\\\",\\\"min\\\",\\\"audit\\\",\\\"by\\\",\\\"share\\\",\\\"where\\\",\\\"after\\\",\\\"power\\\",\\\"escape\\\",\\\"connect\\\",\\\"noaudit\\\",\\\"table\\\",\\\"validate\\\",\\\"cf\\\",\\\"set\\\",\\\"break\\\",\\\"initial\\\",\\\"max\\\",\\\"more\\\",\\\"column\\\",\\\"right\\\",\\\"trigger\\\",\\\"union\\\",\\\"asc\\\",\\\"rename\\\",\\\"decrement\\\",\\\"equals\\\",\\\"fetch\\\",\\\"char\\\",\\\"exists\\\",\\\"notin\\\",\\\"to\\\",\\\"first\\\",\\\"return\\\",\\\"transaction\\\",\\\"checkpoint\\\",\\\"date\\\",\\\"privileges\\\",\\\"declare\\\",\\\"before\\\",\\\"do\\\",\\\"integer\\\",\\\"float\\\",\\\"while\\\",\\\"empty\\\",\\\"mode\\\",\\\"view\\\",\\\"whenever\\\",\\\"prior\\\",\\\"continue\\\",\\\"function\\\",\\\"intersection\\\",\\\"limit\\\",\\\"raise\\\",\\\"create\\\",\\\"from\\\",\\\"collation\\\",\\\"alter\\\",\\\"group\\\",\\\"add\\\",\\\"all\\\",\\\"last\\\",\\\"like\\\",\\\"resource\\\",\\\"count\\\",\\\"check\\\",\\\"less\\\",\\\"encoding\\\",\\\"inner\\\",\\\"rownum\\\",\\\"collate\\\",\\\"null\\\",\\\"abort\\\",\\\"immediate\\\",\\\"true\\\",\\\"decimal\\\",\\\"exec\\\",\\\"nowait\\\",\\\"changed\\\",\\\"desc\\\",\\\"option\\\",\\\"drop\\\",\\\"next\\\",\\\"string\\\",\\\"session\\\",\\\"values\\\",\\\"for\\\",\\\"distinct\\\",\\\"insert\\\",\\\"revoke\\\",\\\"update\\\",\\\"delete\\\",\\\"not\\\",\\\"synonym\\\",\\\"avg\\\",\\\"public\\\",\\\"and\\\",\\\"of\\\",\\\"define\\\",\\\"alias\\\",\\\"divide\\\",\\\"end\\\",\\\"row\\\",\\\"multiply\\\",\\\"on\\\",\\\"or\\\",\\\"intersect\\\",\\\"previous\\\",\\\"false\\\",\\\"go\\\",\\\"start\\\",\\\"was\\\",\\\"rows\\\",\\\"any\\\",\\\"int\\\",\\\"modify\\\",\\\"with\\\",\\\"inout\\\",\\\"boolean\\\",\\\"grant\\\",\\\"remainder\\\",\\\"user\\\"]\",\"functionZ\":\"[]\"}")})
	@GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/autocompleteZQLJson")
    @AnonymousAllowed
    public Response getAutocompleteJSON() {

    	JSONObject jsonObject = new JSONObject();
    	try {

            if(authContext.getLoggedInUser() == null && !JiraUtil.hasAnonymousPermission(authContext.getLoggedInUser())) {
                jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
			String json = autocompleteJsonGenerator.getVisibleFieldNamesJson(authContext.getLoggedInUser(),authContext.getI18nHelper().getLocale());
			jsonObject.put("jqlFieldZ",json);
	
			String reservedWords = autocompleteJsonGenerator.getJqlReservedWordsJson();
			jsonObject.put("reservedWords",reservedWords);
	
			String functionZ = autocompleteJsonGenerator.getVisibleFunctionNamesJson(authContext.getLoggedInUser(),authContext.getI18nHelper().getLocale());
			jsonObject.put("functionZ",functionZ);
    	} catch(JSONException e) {
    		log.error("Error generating autocompletejson for zql:",e);
            return Response.status(Status.BAD_REQUEST).build();
    	}

    	return Response.ok(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
     }

    /**
     * Provides a service context with the current user which contains this action as its {@link
     * com.atlassian.jira.util.ErrorCollection}.
     *
     * @return the JiraServiceContext.
     */
//    public JiraServiceContext getJiraServiceContext() {
//        return new JiraServiceContextImpl(authContext.getLoggedInUser());
//    }
}
