package com.thed.zephyr.je.rest;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.search.SearchService.ParseResult;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserUtils;
import com.atlassian.jira.usercompatibility.UserCompatibilityHelper;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.query.Query;
import com.atlassian.util.concurrent.NotNull;
import com.google.common.collect.ImmutableMap;
import com.sun.jersey.spi.container.ResourceFilters;
import com.thed.zephyr.je.config.model.ExecutionStatus;
import com.thed.zephyr.je.event.ZQLFilterShareType;
import com.thed.zephyr.je.helper.ScheduleSearchResourceHelper;
import com.thed.zephyr.je.rest.filter.ZFJApiFilter;
import com.thed.zephyr.je.zql.core.SearchService;
import com.thed.zephyr.je.zql.model.ZQLFavoriteAsoc;
import com.thed.zephyr.je.zql.model.ZQLFilter;
import com.thed.zephyr.je.zql.model.ZQLSharePermissions;
import com.thed.zephyr.je.zql.service.ZQLFilterManager;
import com.thed.zephyr.util.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.*;

@Api(value = "Execution Filter Resource API(s)", description = "Following section describes the rest resources (API's) pertaining to ExecutionFilterResource")
@Path ("zql/executionFilter")
@Produces ({ MediaType.APPLICATION_JSON})
@Consumes ({ MediaType.APPLICATION_JSON})
@AnonymousAllowed
@ResourceFilters(ZFJApiFilter.class)
public class ZQLFilterResource {
	
    protected final Logger log = Logger.getLogger(ScheduleSearchResource.class);
	private static final String ERROR_LOG_MESSAGE = "[Error] [Error code: %s %s Error Message : %s]";

	private final JiraAuthenticationContext authenticationContext;
	private final ZQLFilterManager zqlFilterManager;
	private final DateTimeFormatterFactory dateTimeFormatterFactory;
	private final SearchService searchService;
	
	public ZQLFilterResource(JiraAuthenticationContext authenticationContext,
			ZQLFilterManager zqlFilterManager,
			DateTimeFormatterFactory dateTimeFormatterFactory,SearchService searchService) {
		this.authenticationContext = authenticationContext;
		this.zqlFilterManager = zqlFilterManager;
		this.dateTimeFormatterFactory = dateTimeFormatterFactory;
		this.searchService=searchService;
	}
	
	/**
	 * Get a ZQL filter by it's Id
	 */
	@ApiOperation(value = "Get ZQL filter", notes = "Get ZQL filter by it's Id")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "{\"createdBy\":\"vm_admin\",\"query\":\"labels = 1\",\"popularity\":1,\"filterName\":\"labels filter1\",\"description\":\"\",\"executionCount\":20,\"id\":1,\"creationDate\":1458315028093,\"sharePerm\":2,\"isFavorite\":true}")})
	@GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path ("/{id}")
    @AnonymousAllowed
   	public Response getExecutionFilter(@PathParam("id") final Integer id ) {
    	final I18nHelper i18nHelper = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();

    	if(null == authenticationContext.getLoggedInUser() && !JiraUtil.hasAnonymousPermission(authenticationContext.getLoggedInUser()))
            return buildLoginErrorResponse();

    	if(id <0){
    		return getSystemZQLs(id);
    	}
    	// validating zqlId
    	if(null == id || id <= 0){
			return getErrorResponse(ImmutableMap.of("Error", i18nHelper.getText("zql.filter.getbyid.invalid.id.error")));
    	}

		Map<String, Object> zqlFilterMap = null;
		ZQLFilter zqlFilter = zqlFilterManager.getZQLFilter(id);

		if(null == zqlFilter){
           return getErrorResponse(ImmutableMap.of("Error", i18nHelper.getText("admin.errors.filters.not.saved")));
           // return getErrorResponse(ImmutableMap.of("Error", i18nHelper.getText("zephyr.common.retrieve.error"))); verify above key else use this key.
		} else {
            if(ZQLFilterUtils.isFilterAccessibleByUser(zqlFilter, authenticationContext.getLoggedInUser())){
                zqlFilterMap = convertToMap(zqlFilter);
            }else{
                return getErrorResponse(ImmutableMap.of("Error", i18nHelper.getText("admin.errors.filters.nonexistent")));
                // return getErrorResponse(ImmutableMap.of("Error", i18nHelper.getText("zephyr.common.retrieve.error"))); verify above key else use this key.
            }
		}
		return Response.ok(zqlFilterMap).build();
    }

    private Response getErrorResponse(Map<String, String> errorMap){
        ResponseBuilder builder = Response.status(Response.Status.NOT_ACCEPTABLE);
        builder.type(MediaType.APPLICATION_JSON);
        builder.entity(errorMap);
        log.error(String.format(ERROR_LOG_MESSAGE,Status.NOT_ACCEPTABLE.getStatusCode(), Status.NOT_ACCEPTABLE,errorMap));
        return builder.build();
    }

    /**
     * System ZQLs
     * @param id
     * @return
     */
	private Response getSystemZQLs(Integer id) {
		String filterQuery = "";
		Map<Integer, ExecutionStatus> executionStatuses = JiraUtil.getExecutionStatuses();
		final String unexecutedStatus = "\"" + executionStatuses.get(ApplicationConstants.UNEXECUTED_STATUS).getName() + "\"";
		final String failedStatus = "\"" + executionStatuses.get(2).getName() + "\"";
		String userName = !StringUtils.containsAny(authenticationContext.getLoggedInUser().getName(), new char[]{'.', ' '}) ? "\""+authenticationContext.getLoggedInUser().getName()+"\"" :
			"\""+authenticationContext.getLoggedInUser().getName()+"\"";
		switch (id){
			case -1:
				filterQuery = "executionStatus != " + unexecutedStatus + " AND executedBy = " + userName ; break;
			case -2:
				filterQuery = "executionStatus = " + failedStatus + " AND executedBy = "+ userName; break;
			case -3:
				filterQuery = "executionStatus = "+ unexecutedStatus ; break;
			case -4:
				filterQuery = "executionStatus != "+ unexecutedStatus ; break;
			case -5:
				filterQuery = "executionStatus = "+ failedStatus ; break;
			case -6:
				filterQuery = "assignee = "+ userName ; break;
		}
		return Response.ok(ImmutableMap.of("id", id, "query", filterQuery)).build();
	}

	/**
	 * Get logged in user
	 */
	@ApiOperation(value = "Get LoggedIn User", notes = "Get LoggedIn User")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "{\"LOGGED_IN_USER\": \"vm_admin\"}")})
	@GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path ("/user")
   	public Response getLoggedInUser() {

        if(null == authenticationContext.getLoggedInUser() && !JiraUtil.hasAnonymousPermission(authenticationContext.getLoggedInUser()))
            return buildLoginErrorResponse();

    	Map<String, Object> zqlFilterMap = new HashMap<String, Object>();
		zqlFilterMap.put("LOGGED_IN_USER", authenticationContext.getLoggedInUser() !=  null ?  authenticationContext.getLoggedInUser().getName() : null);
		return Response.ok(zqlFilterMap).build();    	
    } 
    
	/**
	 * Get all ZQL filters
	 */
	@ApiOperation(value = "Get All Execution Filters")
	@ApiImplicitParams({ @ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "[{\"query\":\"fixVersion = \\\"Version 1.0\\\"\",\"filterName\":\"ExecFilter1\",\"description\":\"\",\"totalCount\":1,\"creationDate\":1463393450081,\"currentIndex\":1,\"createdBy\":\"vm_admin\",\"popularity\":1,\"maxResultAllowed\":20,\"executionCount\":17,\"id\":1,\"linksNew\":[1],\"sharePerm\":1,\"isFavorite\":true}]")})
	@SuppressWarnings("unchecked")
	@GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @AnonymousAllowed
	public Response getExecutionFilters(
			@QueryParam("byUser") final String byUser,
			@QueryParam("fav") final String fav,
			@QueryParam("offset") Integer offset,
			@QueryParam("maxRecords") Integer maxRecords) {

        if(null == authenticationContext.getLoggedInUser() && !JiraUtil.hasAnonymousPermission(authenticationContext.getLoggedInUser()))
            return buildLoginErrorResponse();

    	List<Map<String, Object>> zqlFiltersAsList = null;    	
    	String userName = UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authenticationContext));
    	 Map<String, Object> filtersMap = null; 
    	if(offset == null || offset.intValue() <= -1)
    		offset = -1;
    	Integer startIndex = offset != null ? offset : 0;		
    	Integer maxResultHit = (maxRecords != null && (maxRecords == -1 || maxRecords > 0)) ? maxRecords : Integer.valueOf(JiraUtil.getSimpleDBProperty(ConfigurationConstants.ZEPHYR_ZQL_RESULT_MAX_ON_PAGE, "20").toString());
    	
    	
    	// if null == fav && byUser == true, fetch all filters create by this user
    	if(null == fav && Boolean.valueOf(byUser))
    		filtersMap = zqlFilterManager.getAllZQLFiltersByUser(userName, offset, maxResultHit);
    	// if null == fav && byUser == false, fetch all global filters
    	else if(null == fav && !Boolean.valueOf(byUser))
    		filtersMap = zqlFilterManager.getAllGlobalZQLFilters(offset, maxResultHit);
    	// if null == byUser && fav == true, fetch all Popular( global n favorites) filters
    	else if(null == byUser && Boolean.valueOf(fav))
    		filtersMap = zqlFilterManager.getPopularZQLFilters(offset, maxResultHit, true);      	
    	// if null == byUser && fav == false, fetch all NON-FAVORITE filters
    	else if(null == byUser && !Boolean.valueOf(fav))
    		filtersMap = zqlFilterManager.getPopularZQLFilters(offset, maxResultHit, false); 
    	// If byUser == false && fav == true, fetch all Favorite ZQL Filters
       	else if(!Boolean.valueOf(byUser) && Boolean.valueOf(fav))
    		filtersMap = zqlFilterManager.getPopularZQLFilters(offset, maxResultHit, true);      	
    	// If byUser == true && fav == true, fetch all Favorite ZQL Filters  for the given user
    	else if(Boolean.valueOf(byUser) && Boolean.valueOf(fav))
    		filtersMap = zqlFilterManager.getAllFaoritesZQLFiltersByUser(userName, offset, maxResultHit);
    	// If byUser == true && fav == false, fetch all non-favorite ZQL Filters for the given user
    	else if(Boolean.valueOf(byUser) && !Boolean.valueOf(fav))
    		filtersMap = zqlFilterManager.getAllNonFaoritesZQLFiltersByUser(userName, offset, maxResultHit);
    	// If byUser == false && fav == false, fetch all NON-FAVORITE ZQL Filters
    	else if(!Boolean.valueOf(byUser) && !Boolean.valueOf(fav))
    		filtersMap = zqlFilterManager.getPopularZQLFilters(offset, maxResultHit, false);    	
    	else{    		
    		final I18nHelper i18nHelper = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
    		ResponseBuilder builder = Response.status(Response.Status.NOT_ACCEPTABLE);
			builder.type(MediaType.APPLICATION_JSON);
			//builder.entity(ImmutableMap.of("Error", i18nHelper.getText("admin.errors.filters.nonexistent")));
            builder.entity(ImmutableMap.of("Error", i18nHelper.getText("Unable to retrieve the data.")));
            log.error(String.format(ERROR_LOG_MESSAGE,Status.NOT_ACCEPTABLE.getStatusCode(), Status.NOT_ACCEPTABLE,i18nHelper.getText("Unable to retrieve the data.")));
			return builder.build();
    	}
    	
		zqlFiltersAsList = convertToMap((List<ZQLFilter>)filtersMap.get("zqlFilters"), (Integer)filtersMap.get("totalCount"), startIndex, maxResultHit);		
		return Response.ok(zqlFiltersAsList).build();    	
    }  
    
	/**
	 * Search ZQL filters
	 */
	@ApiOperation(value = "Search Execution Filters", notes = "Search Execution Filters by Filter Name")
    @SuppressWarnings("unchecked")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "{\"query\":\"executionStatus = PASS\",\"filterName\":\"executed by jira user2\",\"description\":\"1executed from 2016-4-18 to 2016-10-19\\nin all projects\",\"totalCount\":4,\"creationDate\":1461098076774,\"currentIndex\":1,\"createdBy\":\"vm_admin\",\"popularity\":1,\"maxResultAllowed\":20,\"executionCount\":7555,\"id\":1,\"linksNew\":[1],\"sharePerm\":1,\"isFavorite\":true}")})
	@GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/search")
    @AnonymousAllowed
	public Response searchExecutionFilters(
			@QueryParam("filterName") final String filterName,
			@QueryParam("owner") final String owner,
			@QueryParam("sharePerm") Integer sharePerm) {

        if(null == authenticationContext.getLoggedInUser() && !JiraUtil.hasAnonymousPermission(authenticationContext.getLoggedInUser()))
            return buildLoginErrorResponse();

        List<Map<String, Object>> zqlFiltersAsList = null;
    	Map<String, Object> filtersMap = null;
    	String ownerKey = null;
    	Integer startIndex = 0;    	
    	Integer maxResultHit = Integer.valueOf(JiraUtil.getSimpleDBProperty(ConfigurationConstants.ZEPHYR_ZQL_RESULT_MAX_ON_PAGE, "20").toString());
    	sharePerm = (null == sharePerm || sharePerm <= 0 ) ? -1 : sharePerm; // -1 pertains to all global and private filters
    	if(null != owner){
    		ownerKey = getUserKeyFromUserName(owner);
    	}
  		filtersMap = zqlFilterManager.searchZQLFilters(filterName, ownerKey, UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authenticationContext)), sharePerm);
		zqlFiltersAsList = convertToMap((List<ZQLFilter>)filtersMap.get("zqlFilters"), (Integer)filtersMap.get("totalCount"), startIndex, maxResultHit);		
		return Response.ok(zqlFiltersAsList).build();    	
    }
    
    /**
     * For filter search by owner name, gets the userKey for the given userName
     * @param userName
     * @return userKey
     */
    private String getUserKeyFromUserName(String userName){
    	String userKey = null;
    	ApplicationUser user = UserUtils.getUser(userName);
    	userKey = UserCompatibilityHelper.isUserObject(user) ? UserCompatibilityHelper.convertUserObject(user).getKey() : userName;
    	return userKey;
    }
    
	/**
	 * Quick Search ZQL filters
	 */
	@ApiOperation(value = "Quick Search ZQL Filters", notes = "Quick Search Execution Filters by Query")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "[{\"createdBy\":\"vm_admin\",\"query\":\"fixVersion = \\\"Version 1.0\\\"\",\"filterName\":\"ExecFilter1\",\"description\":\"\",\"id\":1,\"creationDate\":1463393450081}]")})
	@GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/quickSearch")
	public Response quickSearchExecutionFilters(@QueryParam("query") final String query) {

        if(null == authenticationContext.getLoggedInUser())
            return buildLoginErrorResponse();

    	List<Map<String, Object>> zqlFiltersAsList = null;
    	List<ZQLFilter> zqlFilters = null;
    	if(StringUtils.isNotBlank(query)){
    		zqlFilters = zqlFilterManager.quickSearchZQLFilters(query, UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authenticationContext)));
    		zqlFiltersAsList = convertToMap(zqlFilters);
    		return Response.ok(zqlFiltersAsList).build();
    	}else{
    		return Response.ok().build();
    	}		    	
    }          
    
    /**
     * Copy a ZQL filter
     */
	@ApiOperation(value = "Copy a ZQL Filter", notes = "Copy Execution Filter by Filter Name")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{\"id\":1010,\"filterName\":\"executed by jira user2\"}"),
			@ApiImplicitParam(name = "response", value = "{\"success\":\"Execution Filter copied successfully.\"}")})
	@PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/copy")
    public Response copyExecutionFilter(final Map<String,Object> params) {
    	final I18nHelper i18nHelper = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
		ResponseBuilder builder = Response.status(Response.Status.NOT_ACCEPTABLE);
		builder.type(MediaType.APPLICATION_JSON);
    	// Validating Logged in user

        if(null == authenticationContext.getLoggedInUser())
            return buildLoginErrorResponse();

    	// Validating Filter Name and FilterID
    	Map<String,String> errorMap = new HashMap<String,String>(); 
    	errorMap = inputZQLFilterDataValidation(params.get("id"), null); 
    	validateForNullFilterName(params.get("filterName"), errorMap, i18nHelper);
    	// check for duplicate FilterName
    	if(null != params.get("filterName") && StringUtils.isNotBlank(params.get("filterName").toString()) && checkForDuplicateFilterName(params.get("filterName").toString()))
    		errorMap.put("Duplicate FilterName", i18nHelper.getText("admin.errors.filters.same.name"));
    	
		if(errorMap.size() > 0 ){
			builder.entity(errorMap);
            log.error(String.format(ERROR_LOG_MESSAGE,Status.NOT_ACCEPTABLE.getStatusCode(), Status.NOT_ACCEPTABLE,errorMap));
			return builder.build();
		}
		
		Integer filterId = Integer.parseInt(params.get("id").toString());
		//get the filter to be copied from
		ZQLFilter zqlFilter = zqlFilterManager.getZQLFilter(filterId);
		if(null == zqlFilter){			
			builder.entity(ImmutableMap.of("Error", i18nHelper.getText("admin.errors.filters.not.saved")));
            log.error(String.format(ERROR_LOG_MESSAGE,Status.NOT_ACCEPTABLE.getStatusCode(), Status.NOT_ACCEPTABLE,i18nHelper.getText("admin.errors.filters.not.saved")));
			return builder.build();				
		} else if (null != zqlFilter && !zqlFilter.getCreatedBy().equalsIgnoreCase(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authenticationContext))) &&
				(null == zqlFilter.getZQLFilterSharePermissions() || zqlFilter.getZQLFilterSharePermissions().length <= 0 || ZQLFilterShareType.PRIVATE.getShareType().equals(
								zqlFilter.getZQLFilterSharePermissions()[0].getShareType()))) {
            builder.entity(ImmutableMap.of("Error", i18nHelper.getText("zql.filter.copy.private.error")));
            log.error(String.format(ERROR_LOG_MESSAGE,Status.NOT_ACCEPTABLE.getStatusCode(), Status.NOT_ACCEPTABLE, i18nHelper.getText("zql.filter.copy.private.error")));
            return builder.build();
        } else {
			// creating a property map to save new Filter
			Map<String, Object> zqlFilterMap = new HashMap<String, Object>();
			zqlFilterMap.put("FILTER_NAME", params.get("filterName").toString());
			zqlFilterMap.put("DESCRIPTION", zqlFilter.getDescription());
			zqlFilterMap.put("ZQL_QUERY", zqlFilter.getZqlQuery());
			zqlFilterMap.put("CREATED_ON", new Date().getTime());
			zqlFilterMap.put("UPDATED_ON", new Date().getTime());
			zqlFilterMap.put("FAVORITE", true);
			zqlFilterMap.put("CREATED_BY", UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authenticationContext)));
			String sharePerm = null;
			if (zqlFilter.getZQLFilterSharePermissions() != null
					&& zqlFilter.getZQLFilterSharePermissions().length > 0)
				sharePerm = zqlFilter.getZQLFilterSharePermissions()[0]
						.getShareType() != null ? zqlFilter
						.getZQLFilterSharePermissions()[0].getShareType()
						: ZQLFilterShareType.GLOBAL.getShareType();
			else
				sharePerm = ZQLFilterShareType.GLOBAL.getShareType();
			
			zqlFilterMap.put("SHARE_TYPE", sharePerm);
			zqlFilterMap.put("PARAM1", null);
			zqlFilterMap.put("PARAM2", null);	 	    	
    		try {
    			zqlFilterManager.saveZQLFilter(zqlFilterMap);
    			// return the entry  
       			return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(ImmutableMap.of("success",
    					i18nHelper.getText("zql.filter.copy.success"))).cacheControl(ZephyrCacheControl.never()).build();    			
    		} catch(Exception e) {
    			log.error("Error Copying ZQL Filter ",e);
        		errorMap.put("generic", i18nHelper.getText("zql.filter.copy.error") );
    			builder.entity(errorMap);
    			return builder.build();
    		}	
		}	
    } 
    
	/**
	 * Stores a ZQL filter
	 */
	@ApiOperation(value = "Create Execution Filter", notes = "Create new execution filter")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{\"query\":\"executionstatus=PASS\",\"filterName\":\"executed to PASS\",\"description\":\"default\\nfilter\",\"isFavorite\":false,\"sharePerm\":2}"),
			@ApiImplicitParam(name = "response", value = "{\"id\":3,\"responseMessage\":\"Execution Filter created successfully.\"}")})
	@POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
   	public Response createExecutionFilter(ZQLFilterRequest zqlFilterRequest) {
    	final I18nHelper i18nHelper = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();

        if(null == authenticationContext.getLoggedInUser())
            return buildLoginErrorResponse();

    	ZQLFilterResponse response = new ZQLFilterResponse();    	
		//validate input.
		Map<String,String> errorMap = inputZQLFilterDataValidation(zqlFilterRequest);
		if(errorMap.size() > 0 ){
			ResponseBuilder builder = Response.status(Response.Status.NOT_ACCEPTABLE);
			builder.type(MediaType.APPLICATION_JSON);
			builder.entity(errorMap);
            log.error(String.format(ERROR_LOG_MESSAGE,Status.NOT_ACCEPTABLE.getStatusCode(), Status.NOT_ACCEPTABLE,errorMap));
			return builder.build();
		}
		
    	Map<String,Object> map = convertToMap(zqlFilterRequest);
    	if(map.size() > 0) {
    		try {
                long currentTime = new Date().getTime();
                map.put("CREATED_ON", currentTime);
    			map.put("UPDATED_ON", currentTime);
    			ZQLFilter filter = zqlFilterManager.saveZQLFilter(map);  
    			response.id = filter.getID();
    			response.responseMessage = i18nHelper.getText("zql.filter.save.success");
    			// return the entry
    			return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(response).cacheControl(ZephyrCacheControl.never()).build();
    		} catch(Exception e) {
    			log.error("Error Creating ZQL Filter ",e);
    			ResponseBuilder builder = Response.status(Response.Status.NOT_ACCEPTABLE);
    			builder.type(MediaType.APPLICATION_JSON);
        		errorMap.put("generic", i18nHelper.getText("zql.filter.save.error"));
    			builder.entity(errorMap);
                log.error(String.format(ERROR_LOG_MESSAGE,Status.NOT_ACCEPTABLE.getStatusCode(), Status.NOT_ACCEPTABLE,i18nHelper.getText("zql.filter.save.error")));
    			return builder.build();
    		}
    	} 
    	response.responseMessage=i18nHelper.getText("zql.filter.save.error");
		return Response.serverError().entity(response).build();
	} 
    
    
    /**
     * Updates the ZQL filter
     */
	@ApiOperation(value = "Update The ZQL Filter", notes = "Update Execution Filter" )
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{\"id\":\"1010\",\"description\":\"1executed from 2016-4-18 to 2016-10-19\\nin all projects\"}"),
			@ApiImplicitParam(name = "response", value = "{\"success\":\"Execution Filter updated successfully.\"}")})
	@PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/update")
    public Response updateExecutionFilter(final ZQLFilterRequest zqlFilterRequest) {    
    	if(zqlFilterRequest.id == null || (zqlFilterRequest.id != null && Integer.valueOf(zqlFilterRequest.id).intValue() < 0)) {
    		try {
	    		JSONObject jsonObject = new JSONObject();
	    		jsonObject.put("error", authenticationContext.getI18nHelper().getText("zql.system.filter.update.error"));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,authenticationContext.getI18nHelper().getText("zql.system.filter.update.error")));
	    		return Response.status(Status.BAD_REQUEST).entity(jsonObject.toString()).build();
    		} catch(JSONException e) {
    			log.error("Error constructing JSONObject",e);
	    		return Response.status(Status.BAD_REQUEST).build();
    		}
    	} else {
	    	final I18nHelper i18nHelper = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
	    	// Validating Logged in user

            if(null == authenticationContext.getLoggedInUser())
                return buildLoginErrorResponse();

            // Validating Filter Name and FilterID
	    	Map<String,String> errorMap = new HashMap<String,String>(); 
			ResponseBuilder builder = Response.status(Response.Status.NOT_ACCEPTABLE);
			builder.type(MediaType.APPLICATION_JSON);    	
	    	errorMap = inputZQLFilterDataValidation(zqlFilterRequest.id, zqlFilterRequest.description);
            validateForEmptyFilterName(zqlFilterRequest.filterName, errorMap, i18nHelper);
            //validateForNullFilterName(zqlFilterRequest.filterName, errorMap, i18nHelper);

			if(errorMap.size() > 0 ){
				builder.entity(errorMap);
                log.error(String.format(ERROR_LOG_MESSAGE,Status.NOT_ACCEPTABLE.getStatusCode(), Status.NOT_ACCEPTABLE,errorMap));
				return builder.build();
			}
			
			// Updating Filter Name 
			Integer filterId = Integer.parseInt(zqlFilterRequest.id);
			ZQLFilter zqlFilter = zqlFilterManager.getZQLFilter(filterId);	
			if(zqlFilter == null) {
				builder.entity(ImmutableMap.of("Error", i18nHelper.getText("admin.errors.filters.not.saved")));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.NOT_ACCEPTABLE.getStatusCode(), Status.NOT_ACCEPTABLE,i18nHelper.getText("admin.errors.filters.not.saved")));
				return builder.build();	    		
			} else if (null != zqlFilter && (null == zqlFilter.getCreatedBy() || 
					!zqlFilter.getCreatedBy().equalsIgnoreCase(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authenticationContext))))){
				builder.entity(ImmutableMap.of("Error", i18nHelper.getText("admin.errors.filters.not.owner")));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.NOT_ACCEPTABLE.getStatusCode(), Status.NOT_ACCEPTABLE,i18nHelper.getText("admin.errors.filters.not.owner")));
				return builder.build();				
			}
			// Updating Filter
			try {
				if(StringUtils.isNotBlank(zqlFilterRequest.query)) {
					zqlFilter.setZqlQuery(zqlFilterRequest.query);
				}
	   			if(StringUtils.isNotBlank(zqlFilterRequest.filterName)) {
					zqlFilter.setFilterName(zqlFilterRequest.filterName);
				}
                zqlFilter.setDescription(zqlFilterRequest.description);

	   			zqlFilter.setUpdatedBy(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authenticationContext)));
				zqlFilter.setUpdatedOn(new Date().getTime());
				zqlFilter.save();
				// Save any Share Permission 
				if(StringUtils.isNotBlank(zqlFilterRequest.sharePerm)) {
					ZQLSharePermissions zqlSharePermissions = zqlFilterManager.getZQLSharePermissions(zqlFilter);
					Integer shareTypePrevVal = ZQLFilterShareType.valueOf(zqlSharePermissions.getShareType().toUpperCase()).getShareTypeIntVal();				
					Integer shareTypeVal = 1; // global by default
					try{
						shareTypeVal = Integer.parseInt(null != zqlFilterRequest.sharePerm ? zqlFilterRequest.sharePerm : "1");
					}catch (NumberFormatException nfe) {
						shareTypeVal = 1; // global by default
					}
					if(shareTypeVal <= 0 || shareTypeVal > 2) // if neither global nor private, set default value to global
						shareTypeVal = 1;

                    ZQLFilterShareType zqlFilterShareType = ZQLFilterShareType.getZQLFilterShareType(shareTypeVal);
                    String shareTypeString = ZQLFilterShareType.GLOBAL.getShareType();
                    if(zqlFilterShareType != null){
                        shareTypeString = zqlFilterShareType.getShareType();
                    }

                    zqlSharePermissions.setShareType(shareTypeString);
					zqlSharePermissions.save();
					// if shared permission is changed from global to private, remove any favorite mark from other user(s)
					if(shareTypePrevVal == 1 && shareTypeVal == 2){
						int removedFavCnt = zqlFilterManager.removeFavoriteFromPrivateFilters(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authenticationContext)),
								zqlFilter.getID());
						zqlFilter.setFavCount(Math.abs(zqlFilter.getFavCount()-removedFavCnt));
						zqlFilter.save();
					}
				}
				// Update any "Favorite Associations"
				if(StringUtils.isNotBlank(zqlFilterRequest.isFavorite)){				
					Integer favCountPrevVal = zqlFilter.getFavCount();	
					Boolean	favorite = BooleanUtils.toBoolean(zqlFilterRequest.isFavorite);
					
					ZQLFavoriteAsoc[] zqlFavoriteAsocArr =  zqlFilterManager.getZQLFavoriteAsoc(zqlFilter, UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authenticationContext)));
					if(favorite && zqlFavoriteAsocArr.length == 0){		
						zqlFilterManager.createZQLFavoriteAsoc(zqlFilter, UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authenticationContext)));
						// increase FAV_COUNT				
						zqlFilter.setFavCount(++favCountPrevVal);
						zqlFilter.save();    					
					} else if(!favorite && zqlFavoriteAsocArr.length > 0){
						zqlFilterManager.deleteZQLFavoriteAsoc(zqlFavoriteAsocArr);
						// decrease FAV_COUNT				
						zqlFilter.setFavCount(--favCountPrevVal);
						zqlFilter.save();    					
					} else {} // do nothing  
				}
				
				return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(ImmutableMap.of("success",
						i18nHelper.getText("zql.filter.update.success"))).cacheControl(ZephyrCacheControl.never()).build();    			
			} catch(Exception e) {
                log.error("Error Updating ZQL Filter ", e);
                errorMap.put("generic", i18nHelper.getText("zql.filter.update.error"));
                builder.entity(errorMap);
                log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST, i18nHelper.getText("zql.filter.update.error")));
                return builder.build();
            }
        }
    }  
    
    /**
     * Rename a ZQL filter
     */
	@ApiOperation(value = "Rename a ZQL Filter", notes = "Rename an Execution Filter")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{\"id\":1010,\"filterName\":\"executed by jira user2\"}"),
			@ApiImplicitParam(name = "response", value = "{\"success\":\"Execution Filter Renamed successfully.\"}")})
	@PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/rename")
    public Response renameExecutionFilter(final Map<String,Object> params) {
    	final I18nHelper i18nHelper = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
		ResponseBuilder builder = Response.status(Response.Status.NOT_ACCEPTABLE);
		builder.type(MediaType.APPLICATION_JSON);
    	// Validating Logged in user

        if(null == authenticationContext.getLoggedInUser())
            return buildLoginErrorResponse() ;
        // Validating Filter Name and FilterID
    	Map<String,String> errorMap = new HashMap<String,String>();
    	errorMap = inputZQLFilterDataValidation(params.get("id"), null);
    	validateForNullFilterName(params.get("filterName"), errorMap, i18nHelper);
    	// check for duplicate FilterName
    	if(null != params.get("filterName") && StringUtils.isNotBlank(params.get("filterName").toString()) && checkForDuplicateFilterName(params.get("filterName").toString()))
    		errorMap.put("Duplicate FilterName", i18nHelper.getText("admin.errors.filters.same.name"));
    	
    	if(errorMap.size() > 0 ){
			builder.entity(errorMap);
            log.error(String.format(ERROR_LOG_MESSAGE,Status.NOT_ACCEPTABLE.getStatusCode(), Status.NOT_ACCEPTABLE,errorMap));
			return builder.build();
		}
		
		// validating permissions to rename
		Integer filterId = Integer.parseInt(params.get("id").toString());
		ZQLFilter zqlFilter = zqlFilterManager.getZQLFilter(filterId);	
		if(zqlFilter == null) {
			builder.entity(ImmutableMap.of("Error", i18nHelper.getText("admin.errors.filters.not.saved")));
            log.error(String.format(ERROR_LOG_MESSAGE,Status.NOT_ACCEPTABLE.getStatusCode(), Status.NOT_ACCEPTABLE,i18nHelper.getText("admin.errors.filters.not.saved")));
			return builder.build();	    		
		} else if (null != zqlFilter && (null == zqlFilter.getCreatedBy() || 
				!zqlFilter.getCreatedBy().equalsIgnoreCase(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authenticationContext))))){
			builder.entity(ImmutableMap.of("Error", i18nHelper.getText("admin.errors.filters.not.owner")));
            log.error(String.format(ERROR_LOG_MESSAGE,Status.NOT_ACCEPTABLE.getStatusCode(), Status.NOT_ACCEPTABLE, i18nHelper.getText("admin.errors.filters.not.owner")));
			return builder.build();				
		}    	
    	// Renaming Filter Name
    	ZQLFilterResponse response = new ZQLFilterResponse();    
		Map<String, Object> zqlFilterMap = new HashMap<String, Object>();
		zqlFilterMap.put("ID", Integer.parseInt(params.get("id").toString()));
		zqlFilterMap.put("FILTER_NAME", params.get("filterName").toString());				
		zqlFilterMap.put("UPDATED_BY", UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authenticationContext)));
    	if(zqlFilterMap.size() > 0) {
    		try {
    			zqlFilterMap.put("UPDATED_ON", new Date().getTime());
    			zqlFilterManager.updateZQLFilter(zqlFilterMap);    			
    			// return the entry
       			return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(ImmutableMap.of("success",
    					i18nHelper.getText("zql.filter.rename.success"))).cacheControl(ZephyrCacheControl.never()).build();    			
    		} catch(Exception e) {
                log.error("Error Renaming ZQL Filter ", e);
                errorMap.put("generic", i18nHelper.getText("zql.filter.rename.error"));
                builder.entity(errorMap);
                log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST, i18nHelper.getText("zql.filter.rename.error")));
                return builder.build();
            }
        }
		response.responseMessage=i18nHelper.getText("zql.filter.rename.error");
		return Response.serverError().entity(response).build();	
    } 
    
    /**
     * Toggle ZQL filter "isFavorites"
     */
	@ApiOperation(value = "Toggle ZQL filter \"isFavorites\" ", notes = "Toggle ZQL filter")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{\"id\":1010,\"isFavorite\":true}"),
			@ApiImplicitParam(name = "response", value = "{\"success\":\"Favorite status toggled successfully.\"}")})
	@PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/toggleFav")
    public Response toggleFavorite(final Map<String,Object> params) {
    	final I18nHelper i18nHelper = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
		ResponseBuilder builder = Response.status(Response.Status.NOT_ACCEPTABLE);
		builder.type(MediaType.APPLICATION_JSON);
    	// Validating Logged in user

        if(null == authenticationContext.getLoggedInUser())
            return buildLoginErrorResponse();
    	// Validating FilterID
    	Map<String,String> errorMap = new HashMap<String,String>();
     	Integer filterId = null;
		try {
			filterId = Integer.parseInt(params.get("id").toString());
	      	if(null == filterId || filterId <= 0)
	    		errorMap.put("FilterId", i18nHelper.getText("zql.filter.getbyid.invalid.id.error")); 			
		} catch (NumberFormatException nfe) {
			errorMap.put("FilterId",i18nHelper.getText("zql.filter.getbyid.invalid.id.error"));
		} catch (NullPointerException nfe) {
			errorMap.put("FilterId",i18nHelper.getText("zql.filter.getbyid.invalid.id.error"));
		}  
		if(null == params.get("isFavorite"))
			errorMap.put("Invalid isFavorite flag",i18nHelper.getText("zql.filter.toggle.favorite.input.flag.invalid"));
		if(errorMap.size() > 0 ){
			builder.entity(errorMap);
            log.error(String.format(ERROR_LOG_MESSAGE,Status.NOT_ACCEPTABLE.getStatusCode(), Status.NOT_ACCEPTABLE,errorMap));
			return builder.build();
		}

		// Update any "Favorite Associations"
		try {
	    	ZQLFilter zqlFilter = zqlFilterManager.getZQLFilter(filterId);	
			if(null == zqlFilter){			
				builder.entity(ImmutableMap.of("Error", i18nHelper.getText("admin.errors.filters.not.saved")));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.NOT_ACCEPTABLE.getStatusCode(), Status.NOT_ACCEPTABLE,i18nHelper.getText("admin.errors.filters.not.saved")));
				return builder.build();				
			} else{
		    	ZQLSharePermissions zqlSharePermissions = zqlFilterManager.getZQLSharePermissions(zqlFilter);			
		    	// check if the logged in user have right permissions to toggle the favorite status
		    	if(!UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authenticationContext)).equalsIgnoreCase(zqlFilter.getCreatedBy()) &&
		    			ZQLFilterShareType.PRIVATE.getShareType().equalsIgnoreCase(zqlSharePermissions.getShareType())){
					builder.entity(ImmutableMap.of("Error", i18nHelper.getText("admin.errors.filters.not.owner")));
                    log.error(String.format(ERROR_LOG_MESSAGE,Status.NOT_ACCEPTABLE.getStatusCode(), Status.NOT_ACCEPTABLE,i18nHelper.getText("admin.errors.filters.not.owner")));
					return builder.build();		    		
		    	} else{
					Boolean favorite = BooleanUtils.toBoolean(params.get("isFavorite").toString());
					Integer favCountPrevVal = zqlFilter.getFavCount();
					if(null != favorite){    				
						ZQLFavoriteAsoc[] zqlFavoriteAsocArr =  zqlFilterManager.getZQLFavoriteAsoc(zqlFilter, UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authenticationContext)));
						if(favorite && zqlFavoriteAsocArr.length == 0){		
							zqlFilterManager.createZQLFavoriteAsoc(zqlFilter, UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authenticationContext)));
							// increase FAV_COUNT				
							zqlFilter.setFavCount(++favCountPrevVal);
							zqlFilter.save();    					
						} else if(!favorite && zqlFavoriteAsocArr.length > 0){
							zqlFilterManager.deleteZQLFavoriteAsoc(zqlFavoriteAsocArr);
							// decrease FAV_COUNT				
							zqlFilter.setFavCount(--favCountPrevVal);
							zqlFilter.save();    					
						} else {
							builder.entity(ImmutableMap.of("Error", i18nHelper.getText("zql.filter.toggle.favorite.status.invalid")));
							return builder.build();								
						}		
					}
		   			return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(ImmutableMap.of("success",
	    					i18nHelper.getText("zql.filter.unmark.favorite.success"))).cacheControl(ZephyrCacheControl.never()).build();  		
		    	}
			}
		} catch(Exception e) {
            log.error("Error marking ZQL Filter Unfavorite", e);
            errorMap.put("generic", i18nHelper.getText("zql.filter.unmark.favorite.error"));
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST, i18nHelper.getText("zql.filter.unmark.favorite.error")));
            builder.entity(errorMap);
            return builder.build();
        }
    }     
    
    /**
     * Deletes a ZQL filter by id.
     * @param id
     * @return
     */
	@ApiOperation(value = "Deletes a ZQL filter", notes = "Deletes a ZQL filter by id")
	@ApiImplicitParams({ @ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "{\"success\": \"Execution Filter deleted successfully.\"}")})
	@DELETE
    @Path ("/{id}")
    public Response deleteExecutionFilter(@PathParam("id") Integer id){   
    	final I18nHelper i18nHelper = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
    	// Validating Logged in user

        if(null == authenticationContext.getLoggedInUser())
            return buildLoginErrorResponse();

        // validating FilterID
    	ResponseBuilder builder = Response.status(Response.Status.NOT_ACCEPTABLE);
    	builder.type(MediaType.APPLICATION_JSON);
    	if(null == id || id <= 0){ 
			builder.entity(ImmutableMap.of("Error", i18nHelper.getText("zql.filter.getbyid.invalid.id.error")));
            log.error(String.format(ERROR_LOG_MESSAGE,Status.NOT_ACCEPTABLE.getStatusCode(), Status.NOT_ACCEPTABLE,i18nHelper.getText("zql.filter.getbyid.invalid.id.error")));
			return builder.build();
    	}
    	
    	ZQLFilter zqlFilter = zqlFilterManager.getZQLFilter(id);
    	// validating permission to delete this filter
		if(null == zqlFilter){			
			builder.entity(ImmutableMap.of("Error", i18nHelper.getText("admin.errors.filters.not.saved")));
            log.error(String.format(ERROR_LOG_MESSAGE,Status.NOT_ACCEPTABLE.getStatusCode(), Status.NOT_ACCEPTABLE,i18nHelper.getText("admin.errors.filters.not.saved")));
			return builder.build();				
		} else if (null != zqlFilter && (null == zqlFilter.getCreatedBy() || 
				!zqlFilter.getCreatedBy().equalsIgnoreCase(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authenticationContext))))){
			builder.entity(ImmutableMap.of("Error", i18nHelper.getText("admin.errors.filters.cannot.delete.filter")));
            log.error(String.format(ERROR_LOG_MESSAGE,Status.NOT_ACCEPTABLE.getStatusCode(), Status.NOT_ACCEPTABLE,i18nHelper.getText("admin.errors.filters.cannot.delete.filter")));
			return builder.build();
		} 
		// deleting Filter
		else{	    	
	    	try {
	    		zqlFilterManager.removeZQLFilter(zqlFilter);
       			return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(ImmutableMap.of("success",
    					i18nHelper.getText("zql.filter.delete.success"))).cacheControl(ZephyrCacheControl.never()).build();
	    	} catch (Exception e) {
				log.error("Error Deleting ZQL Filter:",e);				
				builder.entity(ImmutableMap.of("Error",i18nHelper.getText("admin.errors.filters.exception.occured.deleting")));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,i18nHelper.getText("admin.errors.filters.exception.occured.deleting")));
				return builder.build();			
			}		
		}
    }   
    
 
    private Map<String,String> inputZQLFilterDataValidation(ZQLFilterRequest zqlFilterRequest){
    	Map<String,String> errorMap = new HashMap<String,String>();    	
    	final I18nHelper i18n = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
    	if(StringUtils.isBlank(zqlFilterRequest.query))
    		errorMap.put("ZQLQuery", i18n.getText("zql.filter.create.dialog.validationError.query") );
    	if(StringUtils.isBlank(zqlFilterRequest.filterName))
    		errorMap.put("FilterName", i18n.getText("zql.filter.create.dialog.validationError.name") );    	
    	if(StringUtils.isNotBlank(zqlFilterRequest.filterName) 
    			&& (zqlFilterRequest.filterName.length() > 255))
    		errorMap.put("FilterName length", i18n.getText("zql.filter.validationError.filtername", 255));
    	// check for duplicate FilterName
    	if(StringUtils.isNotBlank(zqlFilterRequest.filterName) && checkForDuplicateFilterName(zqlFilterRequest.filterName))
    		errorMap.put("Duplicate FilterName", i18n.getText("admin.errors.filters.same.name"));  	
    	DateTimeFormatter formatter = dateTimeFormatterFactory.formatter().forLoggedInUser().withSystemZone().withStyle(DateTimeStyle.DATE_PICKER);    	
    	try{
    		Date filterCreateDate = convertToDate(zqlFilterRequest.creationDate, formatter);
    	}catch(Exception ex){
    		log.error("Error in converting to date ", ex);
    		errorMap.put("Date", i18n.getText("fields.validation.data.format", ComponentAccessor.getApplicationProperties().getDefaultBackedString(APKeys.JIRA_DATE_PICKER_JAVA_FORMAT)) );
    	}   	
 	
    	if(StringUtils.isNotBlank(zqlFilterRequest.description) && (zqlFilterRequest.description.length() > 255) )
    		errorMap.put("Description", i18n.getText("zql.filter.create.dialog.validationError.filterdescription", 255));
    	
    	return errorMap;
    } 
    
    private Map<String,String> inputZQLFilterDataValidation(Object filterId, Object description){
    	Map<String,String> errorMap = new HashMap<String,String>();    	
    	final I18nHelper i18n = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();    	
    	try {
			Integer filterIdInt = Integer.parseInt(filterId.toString());
	      	if(null == filterIdInt || filterIdInt <= 0)
	    		errorMap.put("FilterId", i18n.getText("zql.filter.getbyid.invalid.id.error")); 			
		} catch (NumberFormatException nfe) {
			errorMap.put("FilterId",i18n.getText("zql.filter.getbyid.invalid.id.error"));
		} catch (NullPointerException nfe) {
			errorMap.put("FilterId",i18n.getText("zql.filter.getbyid.invalid.id.error"));
		}
    	
    	if(null != description && StringUtils.isNotBlank(description.toString()) && (description.toString().length() > 255) )
    		errorMap.put("Description", i18n.getText("zql.filter.create.dialog.validationError.filterdescription", 255));
    	
    	return errorMap;
    } 
    
    private void validateForNullFilterName(Object filterName, Map<String,String> errorMap, I18nHelper i18n){
    	if(null == filterName || StringUtils.isBlank(filterName.toString()))
    		errorMap.put("FilterName", i18n.getText("zql.filter.create.dialog.validationError.name"));    	
    	if(null != filterName && StringUtils.isNotBlank(filterName.toString()) 
    			&& (filterName.toString().length() > 255))
    		errorMap.put("FilterName length", i18n.getText("zql.filter.validationError.filtername", 255));     	
    }
    
    private void validateForEmptyFilterName(String filterName, Map<String,String> errorMap, I18nHelper i18n){    	
    	if(null != filterName && StringUtils.isEmpty(filterName))
    		errorMap.put("FilterName", i18n.getText("zql.filter.create.dialog.validationError.name"));    	
    	if(null != filterName && StringUtils.isNotEmpty(filterName) && (filterName.length() > 255))
    		errorMap.put("FilterName length", i18n.getText("zql.filter.validationError.filtername", 255));     	
    }    
    
    private Boolean checkForDuplicateFilterName(String filterNameToBeCreated){
    	Boolean isDupe = true;
    	ZQLFilter zqlFilter = zqlFilterManager.getZQLFilter(filterNameToBeCreated);
    	if(null == zqlFilter)
    		isDupe = false;
    	return isDupe;
    }
    
    /**
     * Parse the REST input and converts it to a Map, 
     * which is then passed to activeobjects to persist in database.
     * @param zqlFilterRequest
     * @return
     */
	private Map<String,Object> convertToMap(ZQLFilterRequest zqlFilterRequest) {
		Map<String, Object> zqlFilterMap = new HashMap<String, Object>();		
		if(zqlFilterRequest.id != null) {
			zqlFilterMap.put("ID", zqlFilterRequest.id);
		}
		zqlFilterMap.put("CREATED_BY", UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authenticationContext)));
		zqlFilterMap.put("DESCRIPTION", null != zqlFilterRequest.description ? zqlFilterRequest.description : "" );
		zqlFilterMap.put("ZQL_QUERY", zqlFilterRequest.query);
		zqlFilterMap.put("FILTER_NAME", zqlFilterRequest.filterName);		
		zqlFilterMap.put("FAVORITE", null != zqlFilterRequest.isFavorite ? BooleanUtils.toBoolean(zqlFilterRequest.isFavorite) : true);
		//TODO this piece of code has to be modified. Sharing all ZQLFilters globally by default for this release.
		Integer shareTypeVal = 1; // global by default
		try{
			shareTypeVal = Integer.parseInt(null != zqlFilterRequest.sharePerm ? zqlFilterRequest.sharePerm : "1");
		}catch (NumberFormatException nfe) {
			shareTypeVal = 1; // global by default
		}
		if(shareTypeVal <= 0 || shareTypeVal > 2) // if neither global nor private, set default value to global
			shareTypeVal = 1;
		
		zqlFilterMap.put("SHARE_TYPE", shareTypeVal == ZQLFilterShareType.GLOBAL.getShareTypeIntVal() ? ZQLFilterShareType.GLOBAL.getShareType() : 
			shareTypeVal == ZQLFilterShareType.PRIVATE.getShareTypeIntVal() ? ZQLFilterShareType.PRIVATE.getShareType() : "global");
		zqlFilterMap.put("PARAM1", null);
		zqlFilterMap.put("PARAM2", null);
		return zqlFilterMap;
	}    
    
	/**
	 * @param dateString
	 * @param formatter
	 * @return java.util.Date
	 */
	private Date convertToDate(String dateString, DateTimeFormatter formatter) {
		if(!StringUtils.isBlank(dateString)) {
			Date  date = formatter.parse(dateString);
			return date;
		}
		return null;
	}   
	
	/**
	 * Parse ZQLFilter and converts it to a Map, which is then passed to GUI.
	 * @param zqlFilter
	 * @return Map<String, Object>
	 */
	private Map<String, Object> convertToMap(ZQLFilter zqlFilter) {
		Map<String, Object> zqlFilterMap = new HashMap<String, Object>();
		zqlFilterMap.put("id", zqlFilter.getID());
		zqlFilterMap.put("filterName", zqlFilter.getFilterName());
        zqlFilterMap.put("description", null != zqlFilter.getDescription() ? zqlFilter.getDescription() : "");
		zqlFilterMap.put("query", zqlFilter.getZqlQuery());
		zqlFilterMap.put("creationDate", zqlFilter.getCreatedOn());
		zqlFilterMap.put("createdBy", UserCompatibilityHelper.getUserForKey(zqlFilter.getCreatedBy()).getName());
		zqlFilterMap.put("popularity", zqlFilter.getFavCount());
		// To set share permissions
		String sharePerm = null;
		if(zqlFilter.getZQLFilterSharePermissions() != null && 
				zqlFilter.getZQLFilterSharePermissions().length > 0) {
			sharePerm = zqlFilter.getZQLFilterSharePermissions()[0].getShareType();
		}
		if(null != sharePerm && ZQLFilterShareType.GLOBAL.getShareType().equals(sharePerm))
			zqlFilterMap.put("sharePerm", ZQLFilterShareType.GLOBAL.getShareTypeIntVal());
		else if (null != sharePerm && ZQLFilterShareType.PRIVATE.getShareType().equals(sharePerm))
			zqlFilterMap.put("sharePerm", ZQLFilterShareType.PRIVATE.getShareTypeIntVal());	
		// by default set it to "global" 
		else
			zqlFilterMap.put("sharePerm", ZQLFilterShareType.GLOBAL.getShareTypeIntVal());
		// To set "Favorite"
		Boolean favorite = false;
		ZQLFavoriteAsoc[] zqlFavoriteAsocArr = zqlFilterManager.getZQLFavoriteAsoc(zqlFilter, UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authenticationContext)));
		if(null != zqlFavoriteAsocArr && zqlFavoriteAsocArr.length > 0)
			favorite = true;
		zqlFilterMap.put("isFavorite", favorite);	
		
		Object scheduleCount = getScheduleCount(zqlFilter.getZqlQuery());
		zqlFilterMap.put("executionCount", scheduleCount);
		return zqlFilterMap;
	}	
	
	/**
	 * Parse List<ZQLFilter> and converts it to a Map, which is then passed to GUI.
	 * @param zqlFilters
	 * @param totalCount
	 * @param startIndex
	 * @param maxResultHit
	 * @return Map<String, Object>
	 */
	private List<Map<String, Object>> convertToMap(List<ZQLFilter> zqlFilters, Integer totalCount, Integer startIndex, Integer maxResultHit) {
		List<Map<String, Object>> zqlFiltersAsList = new ArrayList<Map<String,Object>>();
		Map<String, Object> zqlFilterMap = null;	
		Integer current = startIndex != null && startIndex != 0 ? (Math.round(startIndex/maxResultHit)+1)  : 1;  
		
		for (ZQLFilter zqlFilter : zqlFilters) {
			zqlFilterMap = new HashMap<String, Object>();
			zqlFilterMap.put("totalCount", totalCount);
			zqlFilterMap.put("currentIndex", current);
			zqlFilterMap.put("maxResultAllowed", maxResultHit);
			zqlFilterMap.put("linksNew", generateNewLinks(totalCount, current, maxResultHit));			
			zqlFilterMap.put("id", zqlFilter.getID());
			zqlFilterMap.put("filterName", zqlFilter.getFilterName());
			zqlFilterMap.put("description", null != zqlFilter.getDescription() ? zqlFilter.getDescription() : "");
			zqlFilterMap.put("query", zqlFilter.getZqlQuery());
			zqlFilterMap.put("creationDate", zqlFilter.getCreatedOn());
			User user = UserCompatibilityHelper.getUserForKey(zqlFilter.getCreatedBy());
			if(user != null) {
				zqlFilterMap.put("createdBy", UserCompatibilityHelper.getUserForKey(zqlFilter.getCreatedBy()).getName());
			} else {
				zqlFilterMap.put("createdBy", zqlFilter.getCreatedBy() + "- Deleted/InActive");
			}
			zqlFilterMap.put("popularity", zqlFilter.getFavCount());
			// To set "share permission"
			String sharePerm = null;
			if(zqlFilter.getZQLFilterSharePermissions() != null && 
					zqlFilter.getZQLFilterSharePermissions().length > 0) {
				sharePerm = zqlFilter.getZQLFilterSharePermissions()[0].getShareType();
			}			
			if(null != sharePerm && ZQLFilterShareType.GLOBAL.getShareType().equals(sharePerm))
				zqlFilterMap.put("sharePerm", ZQLFilterShareType.GLOBAL.getShareTypeIntVal());
			else if (null != sharePerm && ZQLFilterShareType.PRIVATE.getShareType().equals(sharePerm))
				zqlFilterMap.put("sharePerm", ZQLFilterShareType.PRIVATE.getShareTypeIntVal());
			// by default set it to "global" 
			else
				zqlFilterMap.put("sharePerm", ZQLFilterShareType.GLOBAL.getShareTypeIntVal());
			// To set "Favorite"
			Boolean favorite = false;
			ZQLFavoriteAsoc[] zqlFavoriteAsocArr = zqlFilterManager.getZQLFavoriteAsoc(zqlFilter, UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authenticationContext)));
			//ZQLFavoriteAsoc[] zqlFavoriteAsocArr = zqlFilter.getZQLFilterFavoriteAsoc();
			if(null != zqlFavoriteAsocArr && zqlFavoriteAsocArr.length > 0)
				favorite = true;
			zqlFilterMap.put("isFavorite", favorite);
			
			Object scheduleCount = getScheduleCount(zqlFilter.getZqlQuery());
			zqlFilterMap.put("executionCount", scheduleCount);
			zqlFiltersAsList.add(zqlFilterMap);
		}
		return zqlFiltersAsList;
	}	
	
	/**
	 * Parse List<ZQLFilter> and converts it to a Map, which is then passed to GUI.
	 * @param zqlFilters
	 * @return Map<String, Object>
	 */
	private List<Map<String, Object>> convertToMap(List<ZQLFilter> zqlFilters) {
		List<Map<String, Object>> zqlFiltersAsList = new ArrayList<Map<String,Object>>();
		Map<String, Object> zqlFilterMap = null;
		
		for (ZQLFilter zqlFilter : zqlFilters) {
			zqlFilterMap = new HashMap<String, Object>();		
			zqlFilterMap.put("id", zqlFilter.getID());
			zqlFilterMap.put("filterName", zqlFilter.getFilterName());
            zqlFilterMap.put("description", null != zqlFilter.getDescription() ? zqlFilter.getDescription() : "");
			zqlFilterMap.put("query", zqlFilter.getZqlQuery());
			zqlFilterMap.put("creationDate", zqlFilter.getCreatedOn());
			User user = UserCompatibilityHelper.getUserForKey(zqlFilter.getCreatedBy());
			if(user != null) {
				zqlFilterMap.put("createdBy", UserCompatibilityHelper.getUserForKey(zqlFilter.getCreatedBy()).getName());
			} else {
				zqlFilterMap.put("createdBy", zqlFilter.getCreatedBy() + "- Deleted/InActive");
			}
			zqlFiltersAsList.add(zqlFilterMap);
		}
		return zqlFiltersAsList;
	}		
	
    /**
     * If a User goes n clicking after 9, we need to increment one link forward and decrement one link from backward. i.e. when a 
     * user clicks on 9, the ending link will be 10 and start will be 2
     * @param totalFilters
     * @param currentIndex 
     * @param maxResultHit
     */
 	private List<Integer> generateNewLinks(Integer totalFilters,Integer currentIndex, Integer maxResultHit) {
		List<Integer> pageList = new ArrayList<Integer>();
        int pageNumber = 1;
        int endIndex = Math.round(totalFilters/maxResultHit);
        int iterateIndex=0;
        //If currentIndex >= 9 and less than endIndex + 4, than we increment by 4 . At all point, we keep the pages total displayed as 9
        if(currentIndex >= 9) { //1
        	if(endIndex >= (currentIndex+4)) {
            	pageNumber = currentIndex - 4;
        	} else {
                if(totalFilters % maxResultHit != 0) {
               		pageNumber = (endIndex +1) - 9 + 1; 
                } else {
                	pageNumber = (endIndex+1) - 9;
                }
        	}
        }
        //If the Total/Max hit is not equal to 0, we will just add one to it as Round will round it off to previous value
        if(totalFilters % maxResultHit != 0) {
            //If End Index >= 9, than we need to only show 9 links
            if(endIndex >= 9) {
            	endIndex = 8;
            }
        	iterateIndex = endIndex + 1;
        } else {
            //If End Index >= 9, than we need to only show 9 links
            if(endIndex >= 9) {
            	endIndex = 9;
            }
        	iterateIndex = endIndex;
        }
        for (int index = 0; index < iterateIndex; index++) {
        	pageList.add(pageNumber++);
        }
        return pageList;
	}	
    
	/**
	 * Retrieve execution count for each zql filter
	 * @param zqlQuery
	 * @return
	 */
	private Long getScheduleCount(String zqlQuery) {
		ScheduleSearchResourceHelper searchResourceHelper = new ScheduleSearchResourceHelper(authenticationContext.getLoggedInUser(),searchService);
		ParseResult parseResult = searchResourceHelper.getNewSearchQuery(zqlQuery);
        Query currentQuery=null;
		Long scheduleCount = 0l;
        if (parseResult.isValid())
        {
			currentQuery = parseResult.getQuery();
        } else {
        	if(parseResult.getErrors() != null && !parseResult.getErrors().getErrorMessages().isEmpty()) {
        		return new Long(-1);
        	}
        }
		try {
			scheduleCount = searchService.searchCount(authenticationContext.getLoggedInUser(), currentQuery);
			
		} catch (Exception e) {
			log.warn("Error retrieving count from index",e);
		}
		return scheduleCount;		
	}

    /**
     * Common login error response.
     * @return
     */
	private Response buildLoginErrorResponse() {

        JSONObject jsonObject = new JSONObject();
        try {
            String errorMessage = authenticationContext.getI18nHelper().getText("zephyr.common.logged.user.error");
            log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(), Status.UNAUTHORIZED,errorMessage));
            jsonObject.put("error", errorMessage);
        } catch (JSONException e) {
            log.error("Error occurred during response object creation.", e);
            return Response.status(Status.BAD_REQUEST).build();
        }
        return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
    }
    
    @XmlRootElement
    public static class ZQLFilterRequest
    {
    	@XmlElement(nillable=true)
        public String id;
    	
    	@XmlElement(nillable=true)
    	public String createdBy;
    	
    	@XmlElement(nillable=true)
    	public String filterName;    	

    	@XmlElement(nillable=true)
        public String updatedBy;

    	@XmlElement(nillable=true)
        public String creationDate;

    	@XmlElement(nillable=true)
        public String updatedOn;
        
    	@XmlElement(nillable=true)
        public String query;

    	@XmlElement(nillable=true)
        public String description; 
    	
       	@XmlElement(nillable=true)
        public String isFavorite;   
       	
       	@XmlElement(nillable=true)
        public String sharePerm;   
       	
       	@XmlElement(nillable=true)
        public String param1;   
       	
       	@XmlElement(nillable=true)
        public String param2;          	

        public ZQLFilterRequest()
        {    
        }        
    } 
    
    @XmlRootElement
    public static class ZQLFilterResponse
    {
        @XmlElement
        public Integer id;
        
        @XmlElement
        public String responseMessage;
        
        public ZQLFilterResponse()
        {    
        }        
    }        
}
	