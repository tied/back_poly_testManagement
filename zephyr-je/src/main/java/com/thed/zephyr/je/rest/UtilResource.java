package com.thed.zephyr.je.rest;

import com.atlassian.jira.application.ApplicationAuthorizationService;
import com.atlassian.jira.application.ApplicationKeys;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.label.LabelManager;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.portal.PortalPage;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.sharing.search.SharedEntitySearchParameters;
import com.atlassian.jira.sharing.search.SharedEntitySearchParametersBuilder;
import com.atlassian.jira.sharing.search.SharedEntitySearchResult;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.google.common.collect.ImmutableMap;
import com.sun.jersey.spi.container.ResourceFilters;
import com.thed.zephyr.je.config.model.ExecutionStatus;
import com.thed.zephyr.je.helper.ScheduleResourceHelper;
import com.thed.zephyr.je.model.Cycle;
import com.thed.zephyr.je.rest.filter.ZFJApiFilter;
import com.thed.zephyr.je.service.CycleManager;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.ZephyrCacheControl;
import com.thed.zephyr.util.ZephyrWikiParser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Api(value = "UTIL Resource API(s)", description = "Following section describes the rest resources related to common utility API(s)")
@Path("util")
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@AnonymousAllowed
@ResourceFilters(ZFJApiFilter.class)
public class UtilResource {
    private static final String VERSION_ENTITY_TYPE = "version";

	private static final String PROJECT_ENTITY_TYPE = "project";
	
	private static final String PRIORITIES_ENTITY_TYPE = "Priorities";
	private static final String COMPONENT_ENTITY_TYPE = "Components";
	private static final String LABELS_ENTITY_TYPE = "Labels";
	private static final String STATUSES_ENTITY_TYPE = "Statuses";
	private static final String ZEPHYR_RENDERER_TYPE_WIKI = "zephyr-wiki-renderer";
	private static final String ERROR_LOG_MESSAGE = "[Error] [Error code: %s %s Error Message : %s]";

	protected final Logger log = Logger.getLogger(UtilResource.class);

	private final JiraAuthenticationContext authContext;
	private final ApplicationAuthorizationService applicationAuthorizationService;

	private ProjectManager projectManager;
	private VersionManager versionManager;
	private PermissionManager permissionManager;
	private CycleManager cycleManager;
	
	public UtilResource(JiraAuthenticationContext authContext,ProjectManager projectManager, VersionManager versionManager, 
			PermissionManager permissionManager,CycleManager cycleManager, ApplicationAuthorizationService applicationAuthorizationService) {
		this.authContext = authContext;
		this.projectManager = projectManager;
		this.versionManager = versionManager;
		this.permissionManager = permissionManager;
		this.cycleManager=cycleManager;
		this.applicationAuthorizationService = applicationAuthorizationService;
	}

	/**
	 * Gets all projects
	 */
	@ApiOperation(value = "Get All Projects", notes = "Get List of Projects")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "{\"options\":[{\"hasAccessToSoftware\":\"true\",\"label\":\"abc\",\"type\":\"software\",\"value\":\"10001\"},{\"hasAccessToSoftware\":\"true\",\"label\":\"abccore\",\"type\":\"business\",\"value\":\"10201\"},{\"hasAccessToSoftware\":\"true\",\"label\":\"Apple\",\"type\":\"software\",\"value\":\"10400\"},{\"hasAccessToSoftware\":\"true\",\"label\":\"asd\",\"type\":\"software\",\"value\":\"10401\"},{\"hasAccessToSoftware\":\"true\",\"label\":\"memo\",\"type\":\"software\",\"value\":\"10000\"},{\"hasAccessToSoftware\":\"true\",\"label\":\"nokia\",\"type\":\"business\",\"value\":\"10202\"},{\"hasAccessToSoftware\":\"true\",\"label\":\"sam\",\"type\":\"software\",\"value\":\"10200\"},{\"hasAccessToSoftware\":\"true\",\"label\":\"slk\",\"type\":\"service_desk\",\"value\":\"10300\"},{\"hasAccessToSoftware\":\"true\",\"label\":\"SONY\",\"type\":\"software\",\"value\":\"10100\"},{\"hasAccessToSoftware\":\"true\",\"label\":\"test\",\"type\":\"software\",\"value\":\"10203\"}]}")})
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/project-list")
    @AnonymousAllowed
   	public Response getProjects() {
    	final ApplicationUser user = authContext.getLoggedInUser();

        if( user == null && !JiraUtil.hasAnonymousPermission(user))
            return buildLoginErrorResponse();

    	Collection<Project> projects = getProjects(user);//projectManager.getProjectObjects();
    	List<Map<String, String>> projectList = new ArrayList<Map<String, String>>();
    	for(Project project : projects){
    		Map<String, String> projectMap = new HashMap<String, String>(projects.size());
    		projectMap.put("label", project.getName());
    		projectMap.put("value", String.valueOf(project.getId()));
    		projectMap.put("type", String.valueOf(project.getProjectTypeKey() != null ? project.getProjectTypeKey().getKey() : ""));
			projectMap.put("hasAccessToSoftware", String.valueOf(applicationAuthorizationService.canUseApplication(user, ApplicationKeys.SOFTWARE)));
			projectList.add(projectMap);
    	}
    	JSONObject ob = new JSONObject();
    	try {
			ob.put("options", projectList);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return Response.ok(ob.toString()).build();
	}

    @ApiOperation(value = "Get All Versions Text")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/allversionstext")
    public Response getAllVersionsText() {
        final ApplicationUser user = authContext.getLoggedInUser();

        if( user == null && !JiraUtil.hasAnonymousPermission(user))
            return buildLoginErrorResponse();

        JSONObject ob = new JSONObject();
        try {
            ob.put("text", "All Releases");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return Response.ok(ob.toString()).build();
    }
    
    /**
     * Gets all versions
     */
	@ApiOperation(value = "Get All Versions", notes = "Get List of Versions")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "{\"type\":\"software\",\"hasAccessToSoftware\":\"true\",\"unreleasedVersions\":[{\"value\":\"-1\",\"archived\":false,\"label\":\"Unscheduled\"},{\"value\":\"10000\",\"archived\":false,\"label\":\"version 1\"},{\"value\":\"10001\",\"archived\":false,\"label\":\"version 2\"},{\"value\":\"10002\",\"archived\":false,\"label\":\"version 3\"},{\"value\":\"10003\",\"archived\":false,\"label\":\"version 4\"},{\"value\":\"10004\",\"archived\":false,\"label\":\"version 5\"}],\"releasedVersions\":[]}")})
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/versionBoard-list")
    @AnonymousAllowed
    public Response getVersions(@QueryParam("projectId") String projectId, @QueryParam("versionId") String versionId) {
    	final ApplicationUser user = authContext.getLoggedInUser();

        if(user == null && !JiraUtil.hasAnonymousPermission(user))
            return buildLoginErrorResponse();
    	
    	if(!StringUtils.isBlank(versionId)){
    		return getVersionInfo(projectId, versionId);
    	}
    	
//    	Collection<Version> versions = versionManager.getVersionsUnreleased(new Long(projectId), false);
    	List<Map<String, Object>> unreleasedVersionedList = new ArrayList<Map<String,Object>>();
    	List<Map<String, Object>> releasedVersionedList = new ArrayList<Map<String,Object>>();
    	unreleasedVersionedList.add(getVersionAsMap(ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.je.version.unscheduled"), String.valueOf(ApplicationConstants.UNSCHEDULED_VERSION_ID),false));
    	Collection<Version> unreleasedVersions = versionManager.getVersionsUnreleased(new Long(projectId), true);
    	Collection<Version> releasedVersions = versionManager.getVersionsReleasedDesc(new Long(projectId), true);
    	
    	//Unreleased Versions with no archive
		unreleasedVersions.stream().forEach(version -> {
			unreleasedVersionedList.add(getVersionAsMap(version.getName(), String.valueOf(version.getId()),version.isArchived()));
		});

		releasedVersions.stream().forEach(version -> {
			releasedVersionedList.add(getVersionAsMap(version.getName(), String.valueOf(version.getId()),version.isArchived()));
		});
    	
    	/*Lets add unscheduled version (no version) */
    	JSONObject ob = new JSONObject();
    	try {
    		if(!StringUtils.isBlank(projectId)) {
	    		Project project = projectManager.getProjectObj(Long.valueOf(projectId));
	    		ob.put("type", String.valueOf(project.getProjectTypeKey() != null ? project.getProjectTypeKey().getKey() : ""));
	    		ob.put("hasAccessToSoftware", String.valueOf(applicationAuthorizationService.canUseApplication(user, ApplicationKeys.SOFTWARE)));
    		} else {
	    		ob.put("hasAccessToSoftware", String.valueOf(false));
    		}
    		ob.put("unreleasedVersions", unreleasedVersionedList);
    		ob.put("releasedVersions", releasedVersionedList);
    	} catch (JSONException e) {
    		e.printStackTrace();
    	}
    	return Response.ok(ob.toString()).build();
    }
    
    
	/**
	 * Gets all projects
	 */
	@ApiOperation(value = "Get List of Sprints ", notes = "Get List of Sprints by Project Id and Version Id")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "{\"sprintIds\":[1]}")})
	@GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/sprintsByProjectAndVersion")
    @AnonymousAllowed
   	public Response getSprintsByProjectAndVersion(@QueryParam("projectId") String projectId, @QueryParam("versionId") String versionId) {
    	final ApplicationUser user = authContext.getLoggedInUser();

        if( user == null && !JiraUtil.hasAnonymousPermission(user))
            return buildLoginErrorResponse();
    	
    	if(StringUtils.isBlank(versionId) || StringUtils.equalsIgnoreCase(versionId, "null")) {
    		String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "versionId ", "");
            log.error(String.format(ERROR_LOG_MESSAGE,Response.Status.BAD_REQUEST.getStatusCode(), Response.Status.BAD_REQUEST,errorMessage));
    		return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "Invalid Version", errorMessage, errorMessage );
    	}
    	
    	if(StringUtils.isBlank(projectId) || StringUtils.equalsIgnoreCase(projectId, "null")) {
    		String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "projectId ", "");
            log.error(String.format(ERROR_LOG_MESSAGE,Response.Status.BAD_REQUEST.getStatusCode(), Response.Status.BAD_REQUEST,errorMessage));
    		return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "Invalid Project", errorMessage, errorMessage );
    	}
    	
    	List<Long> projectIdList = new ArrayList<Long>();
    	projectIdList.add(Long.valueOf(projectId));
    	List<String> versionIds = new ArrayList<String>();
    	versionIds.add(versionId);
    	
    	List<Cycle> cycles = cycleManager.getCyclesByProjectsAndVersions(projectIdList, versionIds.toArray(new String[versionIds.size()]), new String[0], 0, -1);
    	List<Long> sprintList = new ArrayList<Long>();
		cycles.stream().forEach(cycle-> {
			if(cycle.getSprintId() != null && !sprintList.contains(cycle.getSprintId())) {
				sprintList.add(cycle.getSprintId());
			}
		});
    	JSONObject ob = new JSONObject();
    	try {
			ob.put("sprintIds", sprintList);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return Response.ok(ob.toString()).build();
	}
    
    /**
     * Gets all versions
     * @param projectId
     * @param versionId
     * @return Response
     */
//    @GET
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces(MediaType.APPLICATION_JSON)
//    @Path("/versionInfo")
    private Response getVersionInfo(String projectId, String versionId) {
    	final ApplicationUser user = authContext.getLoggedInUser();

        if(user == null)
            return buildLoginErrorResponse();

        if(!permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, projectManager.getProjectObj(new Long(projectId).longValue()), user)){
    		String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("gadget.project.no.browseable.projects");
            log.error(String.format(ERROR_LOG_MESSAGE,Response.Status.FORBIDDEN.getStatusCode(), Response.Status.FORBIDDEN,errorMessage));
    		return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "NullVersion", errorMessage, errorMessage );
        }
    	
    	String entityType = null;
    	Project project = null;
    	JSONObject ob = new JSONObject();
    	try {
	    	if(versionId != null && !StringUtils.equalsIgnoreCase(versionId, "-1")){
	    		entityType = VERSION_ENTITY_TYPE;
	    		Version version = versionManager.getVersion(Long.parseLong(versionId));
	    		if(version == null){
	        		String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.je.entity.notfound", VERSION_ENTITY_TYPE );
                    log.error(String.format(ERROR_LOG_MESSAGE,Response.Status.NOT_FOUND.getStatusCode(), Response.Status.NOT_FOUND,errorMessage));
	        		return JiraUtil.buildErrorResponse(Response.Status.NOT_FOUND, "NullVersion", errorMessage, errorMessage );
	    		}
	    		ob.put(VERSION_ENTITY_TYPE, getObjectAsMap(version.getId(), version.getName(), version.getDescription()));
	    		project = version.getProjectObject();
	    	}else{
	    		entityType = PROJECT_ENTITY_TYPE;
	    		String versionName = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.je.version.unscheduled");
	    		ob.put(VERSION_ENTITY_TYPE, getObjectAsMap(-1, versionName, ""));
	    		project = projectManager.getProjectObj(Long.parseLong(projectId));
	    		if(project == null){
	        		String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.je.entity.notfound", PROJECT_ENTITY_TYPE );
                    log.error(String.format(ERROR_LOG_MESSAGE,Response.Status.NOT_FOUND.getStatusCode(), Response.Status.NOT_FOUND,errorMessage));
	        		return JiraUtil.buildErrorResponse(Response.Status.NOT_FOUND, "NullProject", errorMessage, errorMessage );
	    		}
	    	}
	    	
	    	Map<String, Object> projectMap = getObjectAsMap(project.getId(), project.getName(), project.getDescription());
	    	projectMap.put("key", project.getKey());
			ob.put(PROJECT_ENTITY_TYPE, projectMap);
			if(project != null) {
	    		ob.put("type", String.valueOf(project.getProjectTypeKey() != null ? project.getProjectTypeKey().getKey() : ""));
	    		ob.put("hasAccessToSoftware", String.valueOf(applicationAuthorizationService.canUseApplication(user, ApplicationKeys.SOFTWARE)));
    		} else {
	    		ob.put("hasAccessToSoftware", String.valueOf(false));
    		}
    	} catch (Exception e) {
    		log.fatal("Unable to fetch Version/Project data \n", e);
    		String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.je.entity.notfound", entityType);
            log.error(String.format(ERROR_LOG_MESSAGE,Response.Status.BAD_REQUEST.getStatusCode(), Response.Status.BAD_REQUEST,errorMessage));
    		return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "NullProject", errorMessage, errorMessage);
    	}
    	return Response.ok(ob.toString()).build();
    }
    
    /**
     * Gets Execution Statuses, Priorities, Components, Labels
     */
	@ApiOperation(value = "Get Cycle Criteria Info", notes = "Get Cycle Criteria Information")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "{\"components\":[{\"name\":\"comp1\",\"id\":10100},{\"name\":\"comp2\",\"id\":10101},{\"name\":\"comp3\",\"id\":10102},{\"name\":\"comp4\",\"id\":10103},{\"name\":\"component11111111111111\",\"id\":10400},{\"name\":\"component444444444444444444\",\"id\":10401},{\"name\":\"sxc\",\"id\":10300}],\"priorities\":[{\"name\":\"Highest\",\"id\":\"1\",\"desc\":\"This problem will block progress.\"},{\"name\":\"High\",\"id\":\"2\",\"desc\":\"Serious problem that could block progress.\"},{\"name\":\"Medium\",\"id\":\"3\",\"desc\":\"Has the potential to affect progress.\"},{\"name\":\"Low\",\"id\":\"4\",\"desc\":\"Minor problem or easily worked around.\"},{\"name\":\"Lowest\",\"id\":\"5\",\"desc\":\"Trivial problem with little or no impact on progress.\"}],\"labels\":[{\"name\":\"asdfghjkl\",\"id\":\"asdfghjkl\"},{\"name\":\"ASDFGHJK\",\"id\":\"ASDFGHJK\"},{\"name\":\"1\",\"id\":\"1\"},{\"name\":\"'''..,,,\",\"id\":\"'''..,,,\"},{\"name\":\"!@#$%^&*\",\"id\":\"!@#$%^&*\"},{\"name\":\"xcfvgbhnjm\",\"id\":\"xcfvgbhnjm\"},{\"name\":\"twyewgd\",\"id\":\"twyewgd\"},{\"name\":\"sdf\\\\\\\\\",          \"id\": \"sdf\\\\\\\\\"       },             {          \"name\": \"sdcfvghj\",          \"id\": \"sdcfvghj\"       },             {          \"name\": \"hqwgqg\",          \"id\": \"hqwgqg\"       },             {          \"name\": \"e4t\",          \"id\": \"e4t\"       },             {          \"name\": \"bzcggap\\\\\",          \"id\": \"bzcggap\\\\\"       },             {          \"name\": \"bvc\",          \"id\": \"bvc\"       },             {          \"name\": \"as/\",          \"id\": \"as/\"       },             {          \"name\": \"aas\",          \"id\": \"aas\"       },             {          \"name\": \"X\",          \"id\": \"X\"       },             {          \"name\": \"HDHFGSHG\",          \"id\": \"HDHFGSHG\"       },             {          \"name\": \"789\",          \"id\": \"789\"       },             {          \"name\": \"6\",          \"id\": \"6\"       },             {          \"name\": \"2345678\",          \"id\": \"2345678\"       },             {          \"name\": \"14514351\",          \"id\": \"14514351\"       },             {          \"name\": \"12131\",          \"id\": \"12131\"       },             {          \"name\": \"!@$$\",          \"id\": \"!@$$\"       },             {          \"name\": \"!@#$%^&*()\",          \"id\": \"!@#$%^&*()\"       },             {          \"name\": \"!@#$%^&*(\",          \"id\": \"!@#$%^&*(\"       }    ],    \"issueStatuses\":    [             {          \"name\": \"Done\",          \"id\": \"10001\"       },             {          \"name\": \"InProgress\",          \"id\": \"3\"       },             {          \"name\": \"ToDo\",          \"id\": \"10000\"       }    ],    \"executionStatuses\":    [             {          \"color\": \"#A0A0A0\",          \"name\": \"UNEXECUTED\",          \"id\": -1,          \"desc\": \"Thetesthasnotyetbeenexecuted.\"       },             {          \"color\": \"#75B000\",          \"name\": \"PASS\",          \"id\": 1,          \"desc\": \"Testwasexecutedandpassedsuccessfully.\"       },             {          \"color\": \"#CC3300\",          \"name\": \"FAIL\",          \"id\": 2,          \"desc\": \"Testwasexecutedandfailed.\"       },             {          \"color\": \"#F2B000\",          \"name\": \"WIP\",          \"id\": 3,          \"desc\": \"Testexecutionisawork-in-progress.\"       },             {          \"color\": \"#6693B0\",          \"name\": \"BLOCKED\",          \"id\": 4,          \"desc\": \"Thetestexecutionofthistestwasblockedforsomereason.\"       },             {          \"color\": \"#990099\",          \"name\": \"PENDING\",          \"id\": 5,          \"desc\": \"\"       },             {          \"color\": \"#996633\",          \"name\": \"APPROVED\",          \"id\": 6,          \"desc\": \"\"       },             {          \"color\": \"#ff3366\",          \"name\": \"12\",          \"id\": 7,          \"desc\": \"\"       }    ] }")})
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/cycleCriteriaInfo")
    public Response getMiscellaneousData(@QueryParam("projectId") String projectId) {
    	String entityType = null;
    	Project project = null;
    	JSONObject ob = new JSONObject();
    	try {
    		entityType = PROJECT_ENTITY_TYPE;
    		//Safety Check for Null projectId 0r 0
        	if(StringUtils.isBlank(projectId) || StringUtils.equals(projectId, "0")) {
        		String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "projectId ", "");
                log.error(String.format(ERROR_LOG_MESSAGE,Response.Status.BAD_REQUEST.getStatusCode(), Response.Status.BAD_REQUEST,errorMessage));
        		return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "Invalid Project", errorMessage, errorMessage );
            }
    		
    		Long pid = Long.parseLong(projectId);
    		project = projectManager.getProjectObj(pid);
    		
        	if(project == null){
        		String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "projectId ", projectId+"");
                log.error(String.format(ERROR_LOG_MESSAGE,Response.Status.BAD_REQUEST.getStatusCode(), Response.Status.BAD_REQUEST,errorMessage));
        		return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "Invalid Project", errorMessage, errorMessage );
            }
        	
        	boolean hasPermission = JiraUtil.hasBrowseProjectPermission(project.getId(),authContext.getLoggedInUser());
        	if(!hasPermission) {
           		String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.project.permission.error","cycle",String.valueOf(project.getId()));
                log.error(String.format(ERROR_LOG_MESSAGE,Response.Status.BAD_REQUEST.getStatusCode(), Response.Status.BAD_REQUEST,errorMessage));
           		return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "Insufficient Project permissions", errorMessage, errorMessage );
    		}

    		
    		entityType = COMPONENT_ENTITY_TYPE;
    		List<Map<String, Object>> componentsCollection = new ArrayList<Map<String, Object>>();
    		for(ProjectComponent comp : project.getProjectComponents()){
    			componentsCollection.add(getObjectAsMap(comp.getId(), comp.getName(), comp.getDescription()));
    		}
    		
    		entityType = PRIORITIES_ENTITY_TYPE;
    		List<Map<String, Object>> prioritiesCollection = new ArrayList<Map<String, Object>>();
    		for(Priority prio : ComponentAccessor.getConstantsManager().getPriorityObjects()){
    			prioritiesCollection.add(getObjectAsMap(prio.getId(), prio.getNameTranslation(), prio.getDescription()));
    		}
    		
    		entityType = LABELS_ENTITY_TYPE;
    		List<Map<String, Object>> labelsCollection = new ArrayList<Map<String, Object>>();
    		for (String lbl : ComponentAccessor.getComponentOfType(LabelManager.class).getSuggestedLabels(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser(), null, "")){
    			labelsCollection.add(getObjectAsMap(lbl, lbl, null));
    		}
    		
    		entityType = STATUSES_ENTITY_TYPE;
    		List<Map<String, Object>> statusCollection = new ArrayList<Map<String, Object>>();
    		Collection<Status> statusList = JiraUtil.getIssueStatusesForProject(pid);    	
        	for (Status gv: statusList) {
        		statusCollection.add(getObjectAsMap(gv.getId(), gv.getNameTranslation(), null));
        	}

    		
    		List<Map<String, Object>> executionStatusesCollection = new ScheduleResourceHelper().populateStatusList();
    		
    		ob.put("components", componentsCollection);
    		ob.put("priorities", prioritiesCollection);
    		ob.put("labels", labelsCollection);
    		ob.put("issueStatuses", statusCollection);
    		ob.put("executionStatuses", executionStatusesCollection);
    		
    	} catch (Exception e) {
    		log.fatal("Unable to fetch Version/Project data \n", e);
    		e.printStackTrace();
    		String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.je.entity.notfound", entityType);
    		return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "NullProject", errorMessage, errorMessage);
    	}
    	return Response.ok(ob.toString()).build();
    }
    
    /**
     * Gets Execution Statuses, Priorities, Components, Labels
     */
	@ApiOperation(value = "Get Execution Statuses")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "[{\"id\":-1,\"name\":\"UNEXECUTED\",\"description\":\"The test has not yet been executed.\",\"color\":\"#A0A0A0\",\"type\":0},{\"id\":1,\"name\":\"PASS\",\"description\":\"Test was executed and passed successfully.\",\"color\":\"#75B000\",\"type\":0},{\"id\":2,\"name\":\"FAIL\",\"description\":\"Test was executed and failed.\",\"color\":\"#CC3300\",\"type\":0},{\"id\":3,\"name\":\"WIP\",\"description\":\"Test execution is a work-in-progress.\",\"color\":\"#F2B000\",\"type\":0},{\"id\":4,\"name\":\"BLOCKED\",\"description\":\"The test execution of this test was blocked for some reason.\",\"color\":\"#6693B0\",\"type\":0},{\"id\":5,\"name\":\"PENDING\",\"description\":\"\",\"color\":\"#990099\",\"type\":1},{\"id\":6,\"name\":\"APPROVED\",\"description\":\"\",\"color\":\"#996633\",\"type\":1},{\"id\":7,\"name\":\"12\",\"description\":\"\",\"color\":\"#ff3366\",\"type\":1}]")})
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/testExecutionStatus")
    public Response getExecutionStatus() {
    	List<ExecutionStatus> executionStatuses = JiraUtil.getExecutionStatusList();
    	return Response.ok(executionStatuses).cacheControl(ZephyrCacheControl.never()).build();
    }
    
    /**
     * Gets Execution Statuses, Priorities, Components, Labels
     */
	@ApiOperation(value = "Get Test Step Execution Statuses")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "[{\"id\":-1,\"name\":\"UNEXECUTED\",\"description\":\"The Test step has not yet been executed.\",\"color\":\"#A0A0A0\",\"type\":0},{\"id\":1,\"name\":\"PASS\",\"description\":\"Test step was executed and passed successfully\",\"color\":\"#75B000\",\"type\":0},{\"id\":2,\"name\":\"FAIL\",\"description\":\"Test step was executed and failed.\",\"color\":\"#CC3300\",\"type\":0},{\"id\":3,\"name\":\"WIP\",\"description\":\"Test step execution is a work-in-progress.\",\"color\":\"#F2B000\",\"type\":0},{\"id\":4,\"name\":\"BLOCKED\",\"description\":\"The Test step execution of this test was blocked for some reason.\",\"color\":\"#6693B0\",\"type\":0},{\"id\":5,\"name\":\"APPROVED\",\"description\":\"\",\"color\":\"#ff6699\",\"type\":1}]")})
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/teststepExecutionStatus")
    public Response getTestStepExecutionStatus() {
    	Collection<ExecutionStatus> executionStatuses = JiraUtil.getStepExecutionStatusList();
    	return Response.ok(executionStatuses).cacheControl(ZephyrCacheControl.never()).build();
    }

	@ApiOperation(value = "Get Dashboard Summary", notes = "Get Dashboard information by Query")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "[{\"id\":10000,\"name\":\"System Dashboard\"}]")})
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/dashboard")
    public Response getDashboardByName(@QueryParam("query") String dashboardName, @QueryParam("maxRecords") @DefaultValue("15") Integer limit){
		int pageWidth = limit;
		int pagePostion = 0;
		PortalPage selectedPortal = JiraUtil.getTestMetricsDashboard();
		/*Add wildcard to the query if its not already there*/
		if(StringUtils.isNotBlank(dashboardName) && !StringUtils.endsWith(dashboardName, "*")){
			dashboardName = dashboardName + "*";
		}
		SharedEntitySearchParameters paramSharedEntitySearchParameters = new SharedEntitySearchParametersBuilder().setName(dashboardName).toSearchParameters();
		SharedEntitySearchResult<PortalPage> dashboards = JiraUtil.getPortalPageManager().search(paramSharedEntitySearchParameters, authContext.getLoggedInUser(), pagePostion, pageWidth);
		List<Map<String, ? extends Object>> dashboardObj = new ArrayList<Map<String, ? extends Object>>(limit); 
		for(PortalPage page : dashboards.getResults()){
			if(selectedPortal != null && ObjectUtils.equals(selectedPortal.getId(), page.getId()))
				continue;
			Map<String, ? extends Object> map = ImmutableMap.of("id", page.getId(), "name", page.getName());
			dashboardObj.add(map);
		}
		return Response.ok(dashboardObj).cacheControl(ZephyrCacheControl.never()).build();
    }

	@ApiOperation(value = "Convert Markup to HTML", notes = "Convert Wiki Markup to HTML via ZephyrWikiParser")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "{\"renderedHTML\":\"<p>$markup<\\/p>\"}")})
	@POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
	@Path("/render")
	public Response convertMarkupToHTML(Map<String,String> params) {
    	JSONObject ob = new JSONObject();
		if(params != null || params.size() >= 0) {
			String renderedHTML = null;
			String rendererType = params.get("rendererType");
			if(StringUtils.equals(rendererType, ZEPHYR_RENDERER_TYPE_WIKI)) {
				String unrenderedMarkup = params.get("unrenderedMarkup");
				String issueKey = params.get("issueKey");
				Issue issue = null;
				if(issueKey != null){
					issue = ComponentAccessor.getIssueManager().getIssueObject(issueKey);
				}
				renderedHTML = ZephyrWikiParser.WIKIPARSER.convertWikiToHTML(unrenderedMarkup, issue);
			}
			try {
				ob.put("renderedHTML", renderedHTML);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}		
		
		return Response.ok(ob.toString()).cacheControl(ZephyrCacheControl.never()).build();
	}

    /**
     * Gets all component for a project.
     */
    @ApiOperation(value = "Get Components", notes = "Gets all component for a project")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{ }")})
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/component-list")
    @AnonymousAllowed
    public Response getComponentsForProject(@QueryParam("projectId") Long projectId) {
        final ApplicationUser user = authContext.getLoggedInUser();

        if( user == null && !JiraUtil.hasAnonymousPermission(user))
            return buildLoginErrorResponse();

        Project project = projectManager.getProjectObj(projectId);
        Map<Long,String> componentMap = new HashMap<>();

        if (project == null) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "projectId ", "");
            log.error(String.format(ERROR_LOG_MESSAGE, Response.Status.BAD_REQUEST.getStatusCode(), Response.Status.BAD_REQUEST,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, "Invalid Project", errorMessage, errorMessage);
        }else {
            Collection<ProjectComponent> components =  project.getComponents();
			components.stream().forEach(projectComponent -> {
				componentMap.putIfAbsent(projectComponent.getId(),projectComponent.getName());
			});
        }

        return Response.ok(componentMap).build();
    }

	@ApiOperation(value = "Get Execution Status Color", notes = "Get Execution Status Color")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "{ }")})
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/teststatus-list")
	@AnonymousAllowed
	public Response getTestStatus() {
		final ApplicationUser user = authContext.getLoggedInUser();

		if( user == null && !JiraUtil.hasAnonymousPermission(user))
			return buildLoginErrorResponse();

        Map<String, String> statusesMap = new LinkedHashMap<>();
        for (ExecutionStatus execStatus : JiraUtil.getExecutionStatuses().values()) {
            statusesMap.putIfAbsent(execStatus.getName(),execStatus.getColor());
        }
		return Response.ok(statusesMap).build();
	}

	@ApiOperation(value = "Get Zephyr IssueType", notes = "Get Zephyr Issue Type")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"options\":[{\"hasAccessToSoftware\":\"true\",\"label\":\"abc\",\"type\":\"software\",\"value\":\"10001\"},{\"hasAccessToSoftware\":\"true\",\"label\":\"abccore\",\"type\":\"business\",\"value\":\"10201\"},{\"hasAccessToSoftware\":\"true\",\"label\":\"Apple\",\"type\":\"software\",\"value\":\"10400\"},{\"hasAccessToSoftware\":\"true\",\"label\":\"asd\",\"type\":\"software\",\"value\":\"10401\"},{\"hasAccessToSoftware\":\"true\",\"label\":\"memo\",\"type\":\"software\",\"value\":\"10000\"},{\"hasAccessToSoftware\":\"true\",\"label\":\"nokia\",\"type\":\"business\",\"value\":\"10202\"},{\"hasAccessToSoftware\":\"true\",\"label\":\"sam\",\"type\":\"software\",\"value\":\"10200\"},{\"hasAccessToSoftware\":\"true\",\"label\":\"slk\",\"type\":\"service_desk\",\"value\":\"10300\"},{\"hasAccessToSoftware\":\"true\",\"label\":\"SONY\",\"type\":\"software\",\"value\":\"10100\"},{\"hasAccessToSoftware\":\"true\",\"label\":\"test\",\"type\":\"software\",\"value\":\"10203\"}]}")})
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/zephyrTestIssueType")
    public Response getTestcaseIssueTypeId() {
        String testcaseIssueTypeId = JiraUtil.getTestcaseIssueTypeId();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("testcaseIssueTypeId", testcaseIssueTypeId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return Response.ok(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
    }

    private Map<String, Object> getObjectAsMap(Object id, Object name, String desc){
    	Map<String, Object> objectMap = new HashMap<String, Object>(2);
		objectMap.put("id", id);
		objectMap.put("name", name);
		if(desc != null)
			objectMap.put("desc", desc);
		return objectMap;
    }

    private Collection<Project> getProjects(ApplicationUser user)
    {
        return permissionManager.getProjects(ProjectPermissions.BROWSE_PROJECTS, user);
    }

    /**
	 *
	 * @param label
	 * @param value
	 * @param isArchived
	 * @return
	 */
	private Map<String, Object> getVersionAsMap(String label, String value,boolean isArchived) {
		Map<String, Object> versionMap = new HashMap<String, Object>(3);
		versionMap.put("label", label);
		versionMap.put("value", value);
		versionMap.put("archived", isArchived);
		return versionMap;
	}

    /**
     * Common logged in user error response.
     * @return
     */
    private Response buildLoginErrorResponse() {

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
            log.error(String.format(ERROR_LOG_MESSAGE,Response.Status.UNAUTHORIZED.getStatusCode(), Response.Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
        } catch (JSONException e) {
            log.error("Error occurred during response object creation.",e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        return Response.status(Response.Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
    }

}
