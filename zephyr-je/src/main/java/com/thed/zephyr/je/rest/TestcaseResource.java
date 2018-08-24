package com.thed.zephyr.je.rest;

import com.atlassian.beehive.compat.ClusterLock;
import com.atlassian.beehive.compat.ClusterLockService;
import com.atlassian.beehive.compat.ClusterLockServiceFactory;
import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.bc.issue.link.IssueLinkService;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.bc.project.component.ProjectComponentManager;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.label.LabelManager;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.RemoteIssueLinkManager;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.sharing.SharedEntityColumn;
import com.atlassian.jira.sharing.search.ShareTypeSearchParameter;
import com.atlassian.jira.sharing.search.SharedEntitySearchContext;
import com.atlassian.jira.sharing.search.SharedEntitySearchParameters;
import com.atlassian.jira.sharing.search.SharedEntitySearchResult;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.query.Query;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.sun.jersey.spi.container.ResourceFilters;
import com.thed.zephyr.je.helper.ScheduleResourceHelper;
import com.thed.zephyr.je.panel.ZephyrProjectNavContextProvider;
import com.thed.zephyr.je.rest.filter.ZFJApiFilter;
import com.thed.zephyr.je.service.ScheduleManager;
import com.thed.zephyr.je.service.StepResultManager;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.ConfigurationConstants;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.ZephyrCacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import com.atlassian.jira.project.version.VersionManager;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.jql.util.JqlStringSupportImpl;
import com.google.common.base.Charsets;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Api(value = "Testcase Resource API(s)", description = "Following section describes rest resources pertaining to TestcaseResource")
@Path("test")
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@AnonymousAllowed
@ResourceFilters(ZFJApiFilter.class)
public class TestcaseResource {
	protected final Logger log = Logger.getLogger(TestcaseResource.class);
	private static final String ERROR_LOG_MESSAGE = "[Error] [Error code: %s %s Error Message : %s]";
    static Map<Long, com.google.common.base.Optional<Long>> zephyrLinkResetStatus = new ConcurrentHashMap<>(1);

	private final JiraAuthenticationContext authContext;
	private final SearchProvider searchProvider;
	private ProjectManager projectManager;
	private IssueManager issueManager;
	private PermissionManager permissionManager;
	private RemoteIssueLinkManager rilManager;
	private ScheduleManager scheduleManager;
	private StepResultManager stepResultManager;
    private final ClusterLockService clusterLockService;
	private VersionManager versionManager;
	private final LabelManager labelManager;
	private final I18nHelper i18n;

	public TestcaseResource(JiraAuthenticationContext authContext, 
							SearchProvider searchProvider,
							ProjectManager projectManager,
							PermissionManager permissionManager,
							IssueManager issueManager,
							RemoteIssueLinkManager rilManager,
							StepResultManager stepResultManager,
                            ClusterLockServiceFactory clusterLockServiceFactory,
							VersionManager versionManager,
							final LabelManager lm) {
		super();
		this.authContext = authContext;
		this.searchProvider = searchProvider;
		this.projectManager = projectManager;
		this.permissionManager = permissionManager;
		this.issueManager=issueManager;
		this.rilManager=rilManager;
		this.stepResultManager=stepResultManager;
        this.clusterLockService = clusterLockServiceFactory.getClusterLockService();
		this.versionManager = versionManager;
		this.labelManager = lm;
		this.i18n = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
	}

	@ApiOperation(value = "Get Test Count List", notes = "Get Count List of Tests by Project Id, Version Id")
	@ApiImplicitParams({ @ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "{\"groupFld\":\"user\",\"urlBase\":\"TBD\",\"data\":[{\"name\":\"vm_admin\",\"cnt\":1,\"id\":\"vm_admin\"}]}")})
	@GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/count")
    @AnonymousAllowed
	public Response getTestCount(@QueryParam("projectId") Long projectId, @QueryParam("versionId") Long versionId, @QueryParam("groupFld") String groupBy){
    	final ApplicationUser user = authContext.getLoggedInUser();

		JSONObject jsonObject = new JSONObject();
		try {
            if(user == null && !JiraUtil.hasAnonymousPermission(user)) {
				jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(), Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
				return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
			}
		} catch (JSONException e) {
			log.error("Error occurred while getting count.",e);
			return Response.status(Status.BAD_REQUEST).build();
		}
    	if(StringUtils.isBlank(groupBy)){
    		groupBy = "user";
    	}
    	
    	if(projectId == null || projectId == 0) {
       		String errorMessage = authContext.getI18nHelper().getText("zapi.execution.move.invalid.projectid");
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorMessage));
    		return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "NullProjectId", errorMessage, errorMessage );
    	}
    	
    	Project project = projectManager.getProjectObj(Long.valueOf(projectId));
    	if(project == null) {
       		String errorMessage = authContext.getI18nHelper().getText("zapi.execution.move.invalid.projectid");
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorMessage));
    		return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "NullProjectId", errorMessage, errorMessage );
    	}
    	
    	if(!permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, project, user)){
       		String errorMessage = authContext.getI18nHelper().getText("zapi.execution.move.invalid.projectid");
            log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(), Status.FORBIDDEN,errorMessage));
    		return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "NullProjectId", errorMessage, errorMessage );
        }
        
    	JSONObject finalResponse = new JSONObject();
    	String issueType = JiraUtil.getTestcaseIssueTypeId();
    	try {
    		finalResponse.put("groupFld", groupBy);
    		Set<Map<String, Object>> data = new LinkedHashSet<Map<String,Object>>();;
			if(StringUtils.equalsIgnoreCase(groupBy, "label")){
				finalResponse.put("urlBase", "TBD");
				//data = ?
			}else if(StringUtils.equalsIgnoreCase(groupBy, "user")){
				finalResponse.put("urlBase", "TBD");
				UserManager userManager = ComponentAccessor.getUserManager();
				Map<String, Object> tcMap = searchIssuesByCreator(projectId,versionId,issueType);
				if(MapUtils.isNotEmpty(tcMap)) {
					tcMap.forEach((k, v) -> {
						if(k != null && v != null){
							ApplicationUser tcUser = userManager.getUserByKey(k);
							Long count = Long.valueOf(String.valueOf(v));
							if(tcUser != null) {
								Map<String, Object> userSummary = prepareCreationSummary(count, tcUser.getName(), tcUser.getDisplayName());
								data.add(userSummary);
							}
						}
					});
				}

			//TBI
			}else if(StringUtils.equalsIgnoreCase(groupBy, "component")){
				finalResponse.put("urlBase", "TBD");
				ProjectComponentManager componentManager = ComponentAccessor.getProjectComponentManager();
				Map<Long, Object> tcMap = searchIssuesByComponent(projectId,versionId,issueType);
				String noComponentName = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.je.component.nocomponent");
				if(MapUtils.isNotEmpty(tcMap)) {
					tcMap.forEach((k, v) -> {
						if(k != null && v != null) {
							Map<String, Object> componentSummary = new HashMap<>();
							Long count = Long.valueOf(String.valueOf(v));
							if (k != -1) {
								ProjectComponent projectComponent = componentManager.getProjectComponent(k);
								if(projectComponent != null) {
									componentSummary = prepareCreationSummary(count, projectComponent.getId(), projectComponent.getName());
								}
							} else {
								componentSummary = prepareCreationSummary(count, -1l, noComponentName);
							}
							data.add(componentSummary);
						}
					});

				}


			}
			finalResponse.put("data", data);
		} catch (JSONException e) {
			log.fatal("Error in preparing JSON response for schedules count " , e);
		} catch (SearchException e) {
			log.fatal("Unable to perform search " , e);
		}
    	return Response.ok(finalResponse.toString()).build();
    }
	
	@ApiOperation(value = "Get List of Saved Searches", notes = "Get List of Saved Searches by SaveSearch Id")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "{\"count\": 4}")})
	@GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    //@Path("/mySearches")
	@Path("/mySearches/{id}/")
	public Response getSavedSearches(@PathParam("id") String savedSearchId){
		JSONObject finalResponse = new JSONObject();
		List<Map<String, Object>> mySavedSearches = new ArrayList<Map<String,Object>>();
		try {
			if(!StringUtils.isBlank(savedSearchId)){
				return getSavedSearchCount(savedSearchId);
			}
			
			SearchRequestService searchRequestService = ComponentAccessor.getComponentOfType(SearchRequestService.class);
			Set<SearchRequest> allFilters = new HashSet<SearchRequest>();
	        final JiraServiceContext serviceContext = new JiraServiceContextImpl(authContext.getLoggedInUser());
            SharedEntitySearchResult<SearchRequest> sesr = searchRequestService.search(serviceContext, new NullSharedEntitySearchParameters(), 0, 400 );
            final Collection<SearchRequest> allFiltersCollection = sesr.getResults();
            allFilters.addAll(allFiltersCollection);

			for (SearchRequest sr : allFilters){
				Map<String, Object> savedSearchMap = new HashMap<String, Object>();
				savedSearchMap.put("id", sr.getId());
				if(StringUtils.equals(authContext.getLoggedInUser().getName(), sr.getOwnerUserName())){
					savedSearchMap.put("name",  sr.getName() + " (my)");
				}else{
					savedSearchMap.put("name",  sr.getName() + " (" + sr.getOwnerUserName() + ")");
				}
					
				savedSearchMap.put("desc", sr.getDescription());
				mySavedSearches.add(savedSearchMap);
			}
			
			finalResponse.put("savedsearches", mySavedSearches);
		} catch (JSONException e) {
			log.fatal("Unable to run query");
		}
		return Response.ok(finalResponse.toString()).build();
	}


	@ApiOperation(value = "Add's Issue Link", notes = "Add Issue Link from Issue to Zephyr Test")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "{\"success\": \"Issue Link added successfully.\"}")})
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/addIssueLink")
	public Response addIssueLink(@QueryParam("parentIssueId") final Long parentIssueId, @QueryParam("testcaseId") final Long testcaseId) {
		final ScheduleResourceHelper helper = new ScheduleResourceHelper(issueManager, rilManager, scheduleManager, stepResultManager);
		Long issueLinkTypeId = new Long(0);
		if(StringUtils.isNotBlank(JiraUtil.getTestcaseToRequirementLinkType())) {
			issueLinkTypeId = Long.parseLong(JiraUtil.getTestcaseToRequirementLinkType());
			resetTestcaseToRequirementLinkTypeIfNotExist(issueLinkTypeId);
            issueLinkTypeId = Long.parseLong(JiraUtil.getTestcaseToRequirementLinkType());
		}
		Issue testcase = issueManager.getIssueObject(testcaseId);
		helper.addIssueLinks(testcase, parentIssueId, issueLinkTypeId, issueLinkTypeId, true);
		JSONObject response = new JSONObject();
		try {
			response.put("success", "Issue Link added successfully");
		} catch (JSONException e) {
			log.error("Error building JSON response", e);
		}
		return Response.ok(response.toString()).cacheControl(ZephyrCacheControl.never()).build();
	}

    /*
    * commented as part of ZFJ-2445 bug.

    @ApiOperation(value = "Refresh Issue Link", notes = "Refresh Issue Link of Zephyr Test from old link type to new link type")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"success\": \"Issue Link added successfully.\"}")})
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/resetIssueLink")
    public Response resetIssueLink(@QueryParam("issueLinkTypeId") final Long issueLinkTypeId, Map<String,String> issueTypeMap) {
        final ScheduleResourceHelper helper = new ScheduleResourceHelper(issueManager, rilManager, scheduleManager, stepResultManager, searchProvider);

        boolean isJiraAdmin = ComponentAccessor.getGlobalPermissionManager().hasPermission(GlobalPermissionKey.ADMINISTER, authContext.getLoggedInUser());
        if (!isJiraAdmin) {
            String errorMessage = authContext.getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
            log.error(String.format(ERROR_LOG_MESSAGE, Status.FORBIDDEN.getStatusCode(), Status.FORBIDDEN, errorMessage));
            return JiraUtil.getPermissionDeniedErrorResponse(errorMessage);
        }
        final String lockName = "zephyr-test-reset-links";
        final ClusterLock lock = clusterLockService.getLockForName(lockName);
        try {
            final ApplicationUser user = authContext.getLoggedInUser();
            final JSONObject finalResponse = new JSONObject();
            final Long token = System.currentTimeMillis();

            Future<Response> responseFuture = Executors.newSingleThreadExecutor().submit(new Callable<Response>() {
                @Override
                public Response call() throws Exception {
                    try {
                        if (lock.tryLock(0, TimeUnit.SECONDS)) {
                            try {
                                if (authContext != null && authContext.getLoggedInUser() == null)
                                    authContext.setLoggedInUser(user);
                                Long oldIssueLinkTypeId = new Long(0);
                                if (StringUtils.isNotBlank(JiraUtil.getTestcaseToRequirementOldLinkType())) {
                                    oldIssueLinkTypeId = Long.parseLong(JiraUtil.getTestcaseToRequirementOldLinkType());
                                }
                                long timeTaken = helper.performZephyrTestCaseLinksResetAsync(issueLinkTypeId, oldIssueLinkTypeId,issueTypeMap);
                                zephyrLinkResetStatus.put(token, com.google.common.base.Optional.of(timeTaken));
                            } finally {
                                lock.unlock(); // release lock
                                authContext.setLoggedInUser(null);
                            }
                            return null;
                        } else {
                            // if lock is already taken, return error message to client
                            String resettingAlreadyInProgressMsg = authContext.getI18nHelper().getText("zephyr.je.admin.reset.already.in.progress");
                            return JiraUtil.buildErrorResponse(Status.FORBIDDEN, "403", resettingAlreadyInProgressMsg, resettingAlreadyInProgressMsg);
                        }
                    } catch (InterruptedException e) {
                        zephyrLinkResetStatus.put(token, com.google.common.base.Optional.of(-1l));
                        log.error("resetIssueLink(): Issue links reset operation interrupted: ", e);
                        String resetFailedMsg = authContext.getI18nHelper().getText("zephyr.je.admin.reset.error");
                        return JiraUtil.buildErrorResponse(Status.INTERNAL_SERVER_ERROR, "500", resetFailedMsg, resetFailedMsg);
                    }
                }
            });

            try {
                return responseFuture.get(500, TimeUnit.MILLISECONDS);
            } catch (TimeoutException te) {
                zephyrLinkResetStatus.put(token, com.google.common.base.Optional.<Long>absent());
                finalResponse.put("token", token);
                return Response.ok(finalResponse.toString()).build();
            }

        } catch (Exception e) {
            // send error message back to client
            log.error("resetIssueLinks(): error resetting zephyr links relation: ", e);
            String resetFailedMsg = authContext.getI18nHelper().getText("zephyr.je.admin.reset.error");
            return JiraUtil.buildErrorResponse(Status.INTERNAL_SERVER_ERROR, "500", resetFailedMsg, resetFailedMsg);
        }
    }

    @ApiOperation(value = "Refresh Zephyr test Link Status", notes = "Refresh Link Status(not found/in progress/completed)")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"status\":\"notfound\"}")})
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/resetIssueLinkStatus/{token}")
    public Response refreshLinksStatus(@PathParam("token") final long token) {
        Map<String,String> finalResponse = new HashMap<>();
        boolean isJiraAdmin = ComponentAccessor.getGlobalPermissionManager().hasPermission(GlobalPermissionKey.ADMINISTER,authContext.getLoggedInUser());
        if(!isJiraAdmin) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
            log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(), Status.FORBIDDEN,errorMessage));
            return JiraUtil.getPermissionDeniedErrorResponse(errorMessage);
        }
        com.google.common.base.Optional<Long> timeTaken = zephyrLinkResetStatus.get(token);
        try {
            if (timeTaken == null) {
                finalResponse.put("status", "notfound");
                return Response.ok(finalResponse).build();
            }
            if (!timeTaken.isPresent()) {
                finalResponse.put("status", "inprogress");
                return Response.ok(finalResponse).build();
            }

            if (timeTaken.get() < 0) {
                log.error(String.format(ERROR_LOG_MESSAGE, Status.INTERNAL_SERVER_ERROR,"Unable to acquire lock on zephyr test resetting, Perhaps zephyr test reset already in progress."));
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Unable to acquire lock on zephyr test resetting, Perhaps zephyr test reset already in progress.").build();
            }

            if (timeTaken.get() > 0) {
                long timeTakenLong = timeTaken.get() / 1000;
                finalResponse.put("took", timeTakenLong + " seconds");
            }
            finalResponse.put("status", "completed");
            zephyrLinkResetStatus.remove(token);
        } catch (Exception e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("error:" + e.getMessage()).build();
        }
        return Response.ok(finalResponse).cacheControl(ZephyrCacheControl.never()).build();
    }
*/
    private static class NullSharedEntitySearchParameters implements SharedEntitySearchParameters {
        public String getDescription() { return null; }
        public Boolean getFavourite() { return null; }
        public String getName() { return null; }
        public ShareTypeSearchParameter getShareTypeParameter() { return null; }
        public SharedEntityColumn getSortColumn() { return SharedEntityColumn.NAME; }
        public SharedEntitySearchParameters.TextSearchMode getTextSearchMode() { return null; }
        public String getUserName() { return null; }
        public boolean isAscendingSort() { return true; }
		@Override
		public SharedEntitySearchContext getEntitySearchContext() {
			return SharedEntitySearchContext.USE;
		}
    }
    
	/*
	 * @param savedSearchId
	 * @return Response
	 */
	private Response getSavedSearchCount(String savedSearchId){
		JSONObject finalResponse = new JSONObject();
		try {
			SearchRequestService searchRequestService = ComponentAccessor.getComponentOfType(SearchRequestService.class);
			StringTokenizer tokenizer = new StringTokenizer(savedSearchId,",");
	    	int totalCount = 0;
	    	String strQuery = "";
			while (tokenizer.hasMoreElements()) {
	    		final String token = tokenizer.nextToken();
				final SearchRequest myFilter = searchRequestService.getFilter(new JiraServiceContextImpl(authContext.getLoggedInUser()), Long.valueOf(token));
				if(myFilter == null){
					log.warn("Saved filter not found, check filterId or permissions " + token);
					continue;
				}
				final String testTypeId = JiraUtil.getTestcaseIssueTypeId();
				Query query = JqlQueryBuilder.newBuilder(myFilter.getQuery()).where().and().issueType(testTypeId).buildQuery();
				log.debug("final Savedsearch query is " + query.getQueryString());
				finalResponse.put("desc", myFilter.getDescription());
				totalCount += searchProvider.searchCount(query, authContext.getLoggedInUser());
				strQuery += myFilter.getQuery().toString() + "\n";
	    	}
			finalResponse.put("count",totalCount);
			finalResponse.put("query",strQuery);
		} catch (SearchException e) {
			log.error(Status.INTERNAL_SERVER_ERROR +" Unable to perform search " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Unable to perform search " + e.getMessage()).build();
		} catch (JSONException e) {
            log.error(Status.INTERNAL_SERVER_ERROR +" Unable to perform search " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Unable to create response " + e.getMessage()).build();
		}
		return Response.ok(finalResponse.toString()).build();
	}    
	
/*	@GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/mySearches/{id}/count")
	public Response getSavedSearchCount(@PathParam("id") String savedSearchId){
		JSONObject finalResponse = new JSONObject();
		try {
			SearchRequestService searchRequestService = ComponentAccessor.getComponentOfType(SearchRequestService.class);
			StringTokenizer tokenizer = new StringTokenizer(savedSearchId,",");
	    	int totalCount = 0;
			while (tokenizer.hasMoreElements()) {
	    		final String token = tokenizer.nextToken();
				final SearchRequest myFilter = searchRequestService.getFilter(new JiraServiceContextImpl(authContext.getLoggedInUser()), Long.valueOf(token));
				final String testTypeId = JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, 1l).getString(ConfigurationConstants.ZEPHYR_ISSUETYPE_KEY);
				Query query = JqlQueryBuilder.newBuilder(myFilter.getQuery()).where().and().issueType(testTypeId).buildQuery();
				log.debug("final Savedsearch query is " + query.getQueryString());
				finalResponse.put("desc", myFilter.getDescription());
				totalCount += searchProvider.searchCount(query, authContext.getLoggedInUser());
	    	}
			finalResponse.put("count",totalCount);
		} catch (SearchException e) {
			log.fatal("", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Unable to perform search " + e.getMessage()).build();
		} catch (JSONException e) {
			log.fatal("", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Unable to create response " + e.getMessage()).build();
		}
		return Response.ok(finalResponse.toString()).build();
	}*/
	
	private Map<String, Object> prepareCreationSummary(long cnt, Object groupFldId, String groupFldName) {
        Map<String, Object> componentSummary = new HashMap<String, Object>();
        componentSummary.put("id", groupFldId);
        componentSummary.put("name", groupFldName);
        componentSummary.put("cnt", cnt);
        return componentSummary;
	}

	/**
	 *
	 * @param projectId
	 * @param versionId
	 * @param issueType
	 * @param pComp - ProjectComponent, if null is passed, search is done for issues with no component
	 * @return
	 * @throws SearchException
	 */
	public long searchIssuesByComponent(Long projectId, Long versionId, String issueType, ProjectComponent pComp) throws SearchException {
		JqlClauseBuilder builder = JqlQueryBuilder.newClauseBuilder();
		if(pComp != null)
			builder = builder.project(projectId).and().component(pComp.getId()).and().issueType(issueType);
		else
			builder = builder.project(projectId).and().componentIsEmpty().and().issueType(issueType);
		
		if(versionId != null && versionId.longValue() != -1)
			builder.and().fixVersion(versionId);
		else
			builder.and().fixVersionIsEmpty();
		
		return searchCountUsingJQL(builder);
	}

	/**
	 *
	 * @param projectId
	 * @param issueType
	 * @param pComp - ProjectComponent, if null is passed, search is done for issues with no component
	 * @return
	 * @throws SearchException
	 */
	public long searchIssuesByComponentForTestSummary(Long projectId, String issueType, ProjectComponent pComp) throws SearchException {
		JqlClauseBuilder builder = JqlQueryBuilder.newClauseBuilder();
		if(pComp != null)
			builder = builder.project(projectId).and().component(pComp.getId()).and().issueType(issueType);
		else
			builder = builder.project(projectId).and().componentIsEmpty().and().issueType(issueType);
		
		return searchCountUsingJQL(builder);
	}
	
	public long searchIssuesByCreator(Long projectId, Long versionId, String issueType, ApplicationUser reporter) throws SearchException {
		Long issueCount  = ComponentAccessor.getIssueManager().getIssueCountForProject(projectId);
		if(issueCount == 0l) {
			return issueCount;
		}
		JqlClauseBuilder builder = JqlQueryBuilder.newClauseBuilder();
		builder = builder.project(projectId).and().reporterUser(reporter.getName()).and().issueType(issueType);
		if(versionId != null && versionId.longValue() != -1)
			builder.and().fixVersion(versionId);
		else
			builder.and().fixVersionIsEmpty();
		
		return searchCountUsingJQL(builder);
	}

	/**
	 * Paginated searchIssuesByReporter using JQL
	 * @param projectId
	 * @param versionId
	 * @param issueType
	 * @return
	 * @throws SearchException
	 */
	public Map<String, Object> searchIssuesByCreator(Long projectId, Long versionId, String issueType) throws SearchException {
		Map<String, Object> resultMap = new HashMap<>();

		int offset = 0;
		JqlClauseBuilder builder = JqlQueryBuilder.newClauseBuilder();
		builder = builder.project(projectId).and().issueType(issueType);
		if(versionId != null && versionId.longValue() != -1)
			builder.and().fixVersion(versionId);
		else
			builder.and().fixVersionIsEmpty();
		int total = (int) searchCountUsingJQL(builder);
		do{
			Query query = builder.buildQuery();
			PagerFilter pageFilter = new PagerFilter(offset, ApplicationConstants.MAX_IN_QUERY);
			SearchResults searchResults = searchProvider.search(query, authContext.getLoggedInUser(), pageFilter);
			if(searchResults != null){
				List<Issue> issueList = searchResults.getIssues();
				if(issueList != null && issueList.size() > 0) {
					searchResults.getIssues().forEach(issue -> {
						String reporter = issue.getReporter() != null ? issue.getReporter().getName() : null;
						if (StringUtils.isNotEmpty(reporter)) {
							Integer count = (Integer) resultMap.get(reporter);
							if (count != null) {
								count = count + 1;
							} else {
								count = 1;
							}
							resultMap.put(reporter, count);
						}
					});
				}

			}
			offset += ApplicationConstants.MAX_IN_QUERY;
		}while (offset <= total);

		return resultMap;
	}

	/**
	 * Paginated searchIssuesByComponent using JQL
	 * @param projectId
	 * @param versionId
	 * @param issueType
	 * @return
	 * @throws SearchException
	 */
	public Map<Long, Object> searchIssuesByComponent(Long projectId, Long versionId, String issueType) throws SearchException {
		Map<Long, Object> resultMap = new HashMap<>();
		int offset = 0;
		JqlClauseBuilder builder = JqlQueryBuilder.newClauseBuilder();
		builder = builder.project(projectId).and().issueType(issueType);
		if(versionId != null && versionId.longValue() != -1)
			builder.and().fixVersion(versionId);
		else
			builder.and().fixVersionIsEmpty();
		int total = (int) searchCountUsingJQL(builder);
		do{
			Query query = builder.buildQuery();
			PagerFilter pageFilter = new PagerFilter(offset, ApplicationConstants.MAX_IN_QUERY);
			SearchResults searchResults = searchProvider.search(query, authContext.getLoggedInUser(), pageFilter);
			if(searchResults != null){
				List<Issue> issueList = searchResults.getIssues();
				if(issueList != null && issueList.size() > 0) {
					issueList.forEach(issue -> {
						Collection<ProjectComponent> components = issue.getComponents() != null ? issue.getComponents() : Collections.EMPTY_LIST;
						if (components != null && components.size() > 0) {
							Iterator<ProjectComponent> projComps = components.iterator();
							while(projComps.hasNext()) {
								ProjectComponent projComp = projComps.next();
								Integer count = (Integer) resultMap.get(projComp.getId());
								if (count != null) {
									count = count + 1;
								} else {
									count = 1;
								}
								resultMap.put(projComp.getId(), count);
							}
						}else{
							//for no component
							Integer count = (Integer) resultMap.get(-1l);
							if (count != null) {
								count = count + 1;
							} else {
								count = 1;
							}
							resultMap.put(-1l,count);
						}
					});
				}

			}
			offset += ApplicationConstants.MAX_IN_QUERY;
		}while (offset <= total);

		return resultMap;
	}

    public long searchIssuesByVersion(Long projectId, Long versionId, String issueType) throws SearchException {
        JqlClauseBuilder builder = JqlQueryBuilder.newClauseBuilder();
        builder = builder.project(projectId).and().issueType(issueType);
        if(versionId != null && versionId.longValue() != -1)
            builder.and().fixVersion(versionId);
        else
            builder.and().fixVersionIsEmpty();

        return searchCountUsingJQL(builder);
    }

	public long searchIssuesByLabel(Long projectId, String labelName, String issueType) throws SearchException {
		JqlClauseBuilder builder = JqlQueryBuilder.newClauseBuilder();
		builder = builder.project(projectId).and().issueType(issueType);
		if(labelName != null)
			builder.and().labels(labelName);
		else
			builder.and().labelsIsEmpty();

		return searchCountUsingJQL(builder);
	}

	/**
	 * @param builder
	 * @return
	 * @throws SearchException
	 */
	public long searchCountUsingJQL(JqlClauseBuilder builder)
			throws SearchException {
		Query query = builder.buildQuery();
		log.debug("Query is" + query.toString());
		
		long noOfIssues = searchProvider.searchCount(query, authContext.getLoggedInUser());
		return noOfIssues;
	}


    /**
     * This method will reset the requiremnt type link id if admin deletes all the issue link types.
     * @param issueLinkTypeId
     */
    private void resetTestcaseToRequirementLinkTypeIfNotExist(Long issueLinkTypeId) {
        try {
            IssueLinkService issueLinkService = ComponentAccessor.getComponentOfType(IssueLinkService.class);
            Collection<IssueLinkType> issueLinkTypes = issueLinkService.getIssueLinkTypes();
            TreeMap<String,Long> issueLinkTypeMap = new TreeMap<>();
            Collection<IssueLinkType> issueLinkTypeList = Collections2.filter(issueLinkTypes, new Predicate<IssueLinkType>() {
                @Override
                public boolean apply(IssueLinkType issueLinkType) {
                    return issueLinkTypeId.equals(issueLinkType.getId());
                }
            });

            if(CollectionUtils.isEmpty(issueLinkTypeList) && CollectionUtils.isNotEmpty(issueLinkTypes)) {
                for(IssueLinkType issueLinkType : issueLinkTypes) {
                    issueLinkTypeMap.put(issueLinkType.getName(),issueLinkType.getId());
                }
                if(MapUtils.isNotEmpty(issueLinkTypeMap)) {
                    JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                            .setLong(ConfigurationConstants.ZEPHYR_REQ_TO_TEST_LINK_RELATION, issueLinkTypeMap.firstEntry().getValue());
                    JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                            .setLong(ConfigurationConstants.ZEPHYR_REQ_TO_TEST_LINK_OLD_RELATION, issueLinkTypeMap.firstEntry().getValue());
                }
            }
        } catch (Exception e) {
            log.error("Error updating DB property : " + ConfigurationConstants.ZEPHYR_REQ_TO_TEST_LINK_RELATION + " : " + e.getMessage());
        }
    }

	@ApiOperation(value = "Fetch Tests By Label", notes = "Fetch Tests By Label")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "request", value = "{}"), 
		@ApiImplicitParam(name = "response", value = "{'values':[{'testCount':11,'name':'No Label','url':'/secure/IssueNavigator.jspa?reset=true&jqlQuery=project='ZFJ' AND issuetype=10002 AND labels is EMPTY'}],'totalCount':1}")})
	@ApiResponses({@ApiResponse(code = 400, message = "Invalid Request Parameters."),
    	@ApiResponse(code = 500, message = "Server error while processing the request."),
    	@ApiResponse(code = 401, message = "Unauthorized Request."),@ApiResponse(code = 403, message = "Permission Denied for the request"),
    	@ApiResponse(code = 200, message = "Request processed successfully", reference="{'values':[{'testCount':11,'name':'No Label','url':'/secure/IssueNavigator.jspa?reset=true&jqlQuery=project='ZFJ' AND issuetype=10002 AND labels is EMPTY'}],'totalCount':1}", response=JSONObject.class)})
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/summary/testsbylabel")
	public Response getTestByLabel(@ApiParam(value="Project Id") @QueryParam("projectId") String projectId, @ApiParam(value="Label Name") @QueryParam("labelName") String labelName, @ApiParam(value="Offset") @QueryParam("offset") String offset, @ApiParam(value="Maximum records") @QueryParam("maxRecords") String maxRecords) {
		final ApplicationUser user = authContext.getLoggedInUser();

    	if(StringUtils.isBlank(projectId) || StringUtils.equals(projectId, "0")) {
			String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "projectId ", "");
			log.error(String.format(ERROR_LOG_MESSAGE,Response.Status.BAD_REQUEST.getStatusCode(), Response.Status.BAD_REQUEST,errorMessage));
			return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "Invalid Project", errorMessage, errorMessage );
		}
		JSONObject ob = new JSONObject();
		if(!JiraUtil.isTestSummaryAllFiltersDisabled() &&  !JiraUtil.isTestSummaryLabelsFilterDisabled()) {
			Project project = projectManager.getProjectObj(Long.valueOf(projectId));

			if (project == null) {
				String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "projectId ", "");
				log.error(String.format(ERROR_LOG_MESSAGE, Response.Status.BAD_REQUEST.getStatusCode(), Response.Status.BAD_REQUEST, errorMessage));
				return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "Invalid Project", errorMessage, errorMessage);
			}

			List<String> labelList = getLabelList(labelManager.getSuggestedLabels(user, null, ""));

			List<String> filteredList = new ArrayList<>();
			if (labelName == null || labelName.trim().isEmpty()) {
				filteredList = labelList;
			} else {
				for (String label : labelList) {
					if (label.toLowerCase().contains(labelName.toLowerCase())) {
						filteredList.add(label);
					}
				}
			}

			Integer totalLabels = 1;        //1 for No Label
			String issueType = JiraUtil.getTestcaseIssueTypeId();
			List<Map<String, Object>> listWithTestCount = new ArrayList<>();


			if (offset == null || offset.trim().isEmpty())
				offset = "0";
			if (maxRecords == null || maxRecords.trim().isEmpty())
				maxRecords = "10";

			try {
				Map<String, Long> nameCountMap = new HashMap<>();
				List<String> finalFilteredList = new ArrayList<>();
				for (String label : filteredList) {
					Long testCount = searchIssuesByLabel(Long.valueOf(projectId), label, issueType);
					if (testCount > 0) {
						finalFilteredList.add(label);
						nameCountMap.put(label, testCount);
						++totalLabels;
					}
				}

				Integer startIndex = Integer.valueOf(offset) == 0 ? 0 : Integer.valueOf(offset) - 1;
				if (startIndex > finalFilteredList.size()) {
					startIndex = 0;
				}
				Integer endIndex = startIndex + Integer.valueOf(maxRecords) - 1;

				if (endIndex > finalFilteredList.size()) {
					finalFilteredList = finalFilteredList.subList(startIndex, finalFilteredList.size());
				} else {
					finalFilteredList = finalFilteredList.subList(startIndex, endIndex);
				}

				//Add No Label
				Map<String, Long> labelCountMap = new HashMap<>();
				Map<String, Map<String, Object>> labelDataMap = new HashMap<>();
				Map<String, Object> noLabelMap = new HashMap<String, Object>();
				noLabelMap.put("name", i18n.getText("project.testcase.by.label.noLabel.label"));
				noLabelMap.put("testCount", searchIssuesByLabel(Long.valueOf(projectId), null, issueType));
				String urlFrag = "/secure/IssueNavigator.jspa?reset=true&jqlQuery=project='" + project.getKey() + "' AND issuetype=" + issueType +
						" AND labels is EMPTY";
				noLabelMap.put("url", urlFrag);
				listWithTestCount.add(noLabelMap);

				for (String label : finalFilteredList) {
					Map<String, Object> testCountMap = new HashMap<String, Object>(2);
					testCountMap.put("name", label);
					testCountMap.put("testCount", nameCountMap.get(label));
					String jql = "project='" + project.getKey()
							+ "' AND issuetype=" + issueType
							+ (label == null ? " AND labels is EMPTY" : " AND labels=" + URLEncoder.encode(JqlStringSupportImpl.encodeAsQuotedString(label), Charsets.UTF_8.name()));
					String urlFragment = "/secure/IssueNavigator.jspa?" + ("reset=true&jqlQuery=" + jql);
					testCountMap.put("url", urlFragment);
					labelCountMap.put(label, nameCountMap.get(label));
					labelDataMap.put(label, testCountMap);
				}
				ob.put("values", getReverseSortedList(listWithTestCount, labelCountMap, labelDataMap));
				ob.put("totalCount", totalLabels);
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (IndexOutOfBoundsException e) {
				e.printStackTrace();
			} catch (SearchException e) {
				log.fatal("Unable to perform search ", e);
			} catch (UnsupportedEncodingException e) {
				log.fatal("Error in populating label Map", e);
			}
		}
		return Response.ok(ob.toString()).build();
	}

    @ApiOperation(value = "Fetch Tests By Component", notes = "Fetch Tests By Component")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "request", value = "{}"), 
		@ApiImplicitParam(name = "response", value = "{'values':[{'testCount':11,'name':'No Component','url':'/secure/IssueNavigator.jspa?reset=true&jqlQuery=project='ZFJ' AND issuetype=10002 AND component is EMPTY'}],'totalCount':1}")})
	@ApiResponses({@ApiResponse(code = 400, message = "Invalid Request Parameters."),
    	@ApiResponse(code = 500, message = "Server error while processing the request."),
    	@ApiResponse(code = 401, message = "Unauthorized Request."),@ApiResponse(code = 403, message = "Permission Denied for the request"),
    	@ApiResponse(code = 200, message = "Request processed successfully", reference="{'values':[{'testCount':11,'name':'No Component','url':'/secure/IssueNavigator.jspa?reset=true&jqlQuery=project='ZFJ' AND issuetype=10002 AND component is EMPTY'}],'totalCount':1}", response=JSONObject.class)})
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/summary/testsbycomponent")
	public Response getTestByComponent(@ApiParam(value="Project Id") @QueryParam("projectId") String projectId, @ApiParam(value="Component Name") @QueryParam("componentName") String componentName, @ApiParam(value="Offset") @QueryParam("offset") String offset, @ApiParam(value="Maximum records") @QueryParam("maxRecords") String maxRecords) {
		if(StringUtils.isBlank(projectId) || StringUtils.equals(projectId, "0")) {
			String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "projectId ", "");
			log.error(String.format(ERROR_LOG_MESSAGE,Response.Status.BAD_REQUEST.getStatusCode(), Response.Status.BAD_REQUEST,errorMessage));
			return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "Invalid Project", errorMessage, errorMessage );
		}
		JSONObject ob = new JSONObject();
		if(!JiraUtil.isTestSummaryAllFiltersDisabled()) {
			Project project = projectManager.getProjectObj(Long.valueOf(projectId));

			if (project == null) {
				String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "projectId ", "");
				log.error(String.format(ERROR_LOG_MESSAGE, Response.Status.BAD_REQUEST.getStatusCode(), Response.Status.BAD_REQUEST, errorMessage));
				return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "Invalid Project", errorMessage, errorMessage);
			}

			Collection<ProjectComponent> componentList = project.getComponents();

			List<ProjectComponent> filteredList = new ArrayList<>();
			if (componentName == null || componentName.trim().isEmpty()) {
				filteredList = (List<ProjectComponent>) componentList;
			} else {
				for (ProjectComponent componentObj : componentList) {
					if (componentObj.getName().toLowerCase().contains(componentName.toLowerCase())) {
						filteredList.add(componentObj);
					}
				}
			}

			Integer totalComponents = 1;    //+1 for No Component
			String issueType = JiraUtil.getTestcaseIssueTypeId();
			List<Map<String, Object>> listWithTestCount = new ArrayList<Map<String, Object>>();

			if (offset == null || offset.trim().isEmpty())
				offset = "0";
			if (maxRecords == null || maxRecords.trim().isEmpty())
				maxRecords = "10";

			try {
				Map<String, Long> nameCountMap = new HashMap<>();
				List<ProjectComponent> finalFilteredList = new ArrayList<>();
				for (ProjectComponent componentObj : filteredList) {
					Long testCount = searchIssuesByComponentForTestSummary(Long.valueOf(projectId), issueType, componentObj);
					if (testCount > 0) {
						finalFilteredList.add(componentObj);
						nameCountMap.put(componentObj.getName(), testCount);
						++totalComponents;
					}
				}

				Integer startIndex = Integer.valueOf(offset) == 0 ? 0 : Integer.valueOf(offset) - 1;
				if (startIndex > finalFilteredList.size()) {
					startIndex = 0;
				}
				Integer endIndex = startIndex + Integer.valueOf(maxRecords) - 1;

				if (endIndex > finalFilteredList.size()) {
					finalFilteredList = finalFilteredList.subList(startIndex, finalFilteredList.size());
				} else {
					finalFilteredList = finalFilteredList.subList(startIndex, endIndex);
				}

				Map<String, Long> componentCountMap = new HashMap<>();
				Map<String, Map<String, Object>> componentDataMap = new HashMap<>();

				//Add No Component
				Map<String, Object> noComponentMap = new HashMap<String, Object>();
				noComponentMap.put("name", i18n.getText("zephyr.je.component.nocomponent"));
				noComponentMap.put("testCount", searchIssuesByComponentForTestSummary(Long.valueOf(projectId), issueType, null));
				String urlFrag = "/secure/IssueNavigator.jspa?reset=true&jqlQuery=project='" + project.getKey() + "' AND issuetype=" + issueType +
						" AND component is EMPTY";
				noComponentMap.put("url", urlFrag);
				listWithTestCount.add(noComponentMap);

				for (ProjectComponent componentObj : finalFilteredList) {
					Map<String, Object> testCountMap = new HashMap<>(2);
					String cmptName = componentObj.getName();
					testCountMap.put("name", cmptName);
					testCountMap.put("testCount", nameCountMap.get(cmptName));
					String urlFragment = "/secure/IssueNavigator.jspa?reset=true&jqlQuery=project='" + project.getKey() + "' AND issuetype=" + issueType +
							(componentObj.getId() == null ? " AND component is EMPTY" : " AND component=" + componentObj.getId());
					testCountMap.put("url", urlFragment);
					componentCountMap.put(cmptName, nameCountMap.get(cmptName));
					componentDataMap.put(cmptName, testCountMap);
				}

				ob.put("values", getReverseSortedList(listWithTestCount, componentCountMap, componentDataMap));
				ob.put("totalCount", totalComponents);
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (IndexOutOfBoundsException e) {
				e.printStackTrace();
			} catch (SearchException e) {
				log.fatal("Unable to perform search ", e);
			}
		}
		return Response.ok(ob.toString()).build();
	}

	@ApiOperation(value = "Fetch Tests By Version", notes = "Fetch Tests By Version")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "request", value = "{}"), 
		@ApiImplicitParam(name = "response", value = "{'values':[{'testCount':11,'name':'Unscheduled','url':'/secure/IssueNavigator.jspa?reset=true&jqlQuery=project='ZFJ' AND issuetype=10002 AND fixVersion is EMPTY'}],'totalCount':1}")})
	@ApiResponses({@ApiResponse(code = 400, message = "Invalid Request Parameters."),
    	@ApiResponse(code = 500, message = "Server error while processing the request."),
    	@ApiResponse(code = 401, message = "Unauthorized Request."),@ApiResponse(code = 403, message = "Permission Denied for the request"),
    	@ApiResponse(code = 200, message = "Request processed successfully", reference="{'values':[{'testCount':11,'name':'Unscheduled','url':'/secure/IssueNavigator.jspa?reset=true&jqlQuery=project='ZFJ' AND issuetype=10002 AND fixVersion is EMPTY'}],'totalCount':1}", response=JSONObject.class)})
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/summary/testsbyversion")
	public Response getTestByVersion(@ApiParam(value="Project Id") @QueryParam("projectId") String projectId, @ApiParam(value="Version Name") @QueryParam("versionName") String versionName, @ApiParam(value="offset") @QueryParam("offset") String offset, @ApiParam(value="Maximum records") @QueryParam("maxRecords") String maxRecords) {
		if(StringUtils.isBlank(projectId) || StringUtils.equals(projectId, "0")) {
			String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "projectId ", "");
			log.error(String.format(ERROR_LOG_MESSAGE,Response.Status.BAD_REQUEST.getStatusCode(), Response.Status.BAD_REQUEST,errorMessage));
			return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "Invalid Project", errorMessage, errorMessage );
		}

		JSONObject ob = new JSONObject();
		if(!JiraUtil.isTestSummaryAllFiltersDisabled()) {
			Project project = projectManager.getProjectObj(Long.valueOf(projectId));

			if (project == null) {
				String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "projectId ", "");
				log.error(String.format(ERROR_LOG_MESSAGE, Response.Status.BAD_REQUEST.getStatusCode(), Response.Status.BAD_REQUEST, errorMessage));
				return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "Invalid Project", errorMessage, errorMessage);
			}

			List<Map<String, Object>> versionList = this.getAllVersions(projectId);

			List<Map<String, Object>> filteredList = new ArrayList<Map<String, Object>>();
			if (versionName == null || versionName.trim().isEmpty()) {
				filteredList = versionList;
			} else {
				for (Map<String, Object> versionObj : versionList) {
					if (versionObj.get("name").toString().toLowerCase().contains(versionName.toLowerCase())) {
						filteredList.add(versionObj);
					}
				}
			}

			Integer totalVersions = 1;    //+1 for Unscheduled
			String issueType = JiraUtil.getTestcaseIssueTypeId();
			List<Map<String, Object>> listWithTestCount = new ArrayList<Map<String, Object>>();

			if (offset == null || offset.trim().isEmpty())
				offset = "0";
			if (maxRecords == null || maxRecords.trim().isEmpty())
				maxRecords = "10";
			Integer startIndex = Integer.valueOf(offset) == 0 ? 0 : Integer.valueOf(offset) - 1;

			try {
				Map<String, Long> nameCountMap = new HashMap<>();
				List<Map<String, Object>> finalFilteredList = new ArrayList<Map<String, Object>>();
				for (Map<String, Object> versionObj : filteredList) {
					Long testCount = searchIssuesByVersion(Long.valueOf(projectId), Long.valueOf(versionObj.get("id").toString()), issueType);
					if (testCount > 0) {
						finalFilteredList.add(versionObj);
						nameCountMap.put((String) versionObj.get("name"), testCount);
						++totalVersions;
					}
				}

				if (startIndex > finalFilteredList.size()) {
					startIndex = 0;
				}
				Integer endIndex = startIndex + Integer.valueOf(maxRecords) - 1;

				if (endIndex > finalFilteredList.size()) {
					finalFilteredList = finalFilteredList.subList(startIndex, finalFilteredList.size());
				} else {
					finalFilteredList = finalFilteredList.subList(startIndex, endIndex);
				}

				//Add Unscheduled
				Map<String, Object> unscheduledMap = new HashMap<String, Object>();
				unscheduledMap.put("name", i18n.getText("zephyr.je.version.unscheduled"));
				unscheduledMap.put("testCount", searchIssuesByVersion(Long.valueOf(projectId), -1L, issueType));
				String urlFrag = "/secure/IssueNavigator.jspa?reset=true&jqlQuery=project='" + project.getKey() + "' AND issuetype=" + issueType +
						" AND fixVersion is EMPTY";
				unscheduledMap.put("url", urlFrag);
				listWithTestCount.add(unscheduledMap);

				Map<String, Long> versionsCountMap = new HashMap<>();
				Map<String, Map<String, Object>> versionDataMap = new HashMap<>();

				for (Map<String, Object> versionObj : finalFilteredList) {
					Map<String, Object> testCountMap = new HashMap<>(3);
					String name = (String) versionObj.get("name");
					testCountMap.put("name", name);
					testCountMap.put("testCount", nameCountMap.get(name));
					String urlFragment = "/secure/IssueNavigator.jspa?reset=true&jqlQuery=project='" + project.getKey() + "' AND issuetype=" + issueType +
							(versionObj.get("id") == null ? " AND fixVersion is EMPTY" : " AND fixVersion=" + versionObj.get("id"));
					testCountMap.put("url", urlFragment);
					versionsCountMap.put(name, nameCountMap.get(name));
					versionDataMap.put(name, testCountMap);
				}
				ob.put("values", getReverseSortedList(listWithTestCount, versionsCountMap, versionDataMap));
				ob.put("totalCount", totalVersions);
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (IndexOutOfBoundsException e) {
				e.printStackTrace();
			} catch (SearchException e) {
				log.fatal("Unable to perform search ", e);
			}
		}
		return Response.ok(ob.toString()).build();
	}

	private Map<String, Object> getVersionAsMap(String label, String value,boolean isArchived) {
		Map<String, Object> versionMap = new HashMap<String, Object>(3);
		versionMap.put("name", label);
		versionMap.put("id", value);
		versionMap.put("archived", isArchived);
		return versionMap;
	}

	private List<Map<String, Object>> getAllVersions(String projectId){
		List<Map<String, Object>> versionList = new ArrayList<Map<String,Object>>();

		Collection<Version> unreleasedVersions = versionManager.getVersionsUnreleased(new Long(projectId), true);
		Collection<Version> releasedVersions = versionManager.getVersionsReleasedDesc(new Long(projectId), true);

		//Unreleased Versions with no archive
		for(Version version : unreleasedVersions){
			versionList.add(getVersionAsMap(version.getName(), String.valueOf(version.getId()),version.isArchived()));
		}

		for(Version version : releasedVersions){
			versionList.add(getVersionAsMap(version.getName(), String.valueOf(version.getId()),version.isArchived()));
		}
		return versionList;
	}

    private List<String> getLabelList(Set<String> labels) {
        if(CollectionUtils.isEmpty(labels))
            return Collections.EMPTY_LIST;

        /* Ignoring text case for labels */
        labels = ZephyrProjectNavContextProvider.labelsIgnoreCase(labels);

        return labels.stream().collect(Collectors.toList());
    }

    private List<Map<String, Object>> getReverseSortedList(List<Map<String, Object>> listWithTestCount,
                 Map<String, Long> entityCountMap, Map<String, Map<String, Object>> entityDataMap) {

        List<String> entityValue = entityCountMap.entrySet().stream()
                .sorted(Map.Entry.<String,Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());


        entityValue.forEach(
                key -> {
                    if(null != entityDataMap.get(key)) {
                        listWithTestCount.add(entityDataMap.get(key));
                    }
                }
        );
	    return listWithTestCount;
    }
}
