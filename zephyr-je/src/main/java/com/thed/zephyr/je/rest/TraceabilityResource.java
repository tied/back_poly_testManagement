package com.thed.zephyr.je.rest;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.sal.api.ApplicationProperties;
import com.sun.jersey.spi.container.ResourceFilters;
import com.thed.zephyr.je.config.model.ExecutionStatus;
import com.thed.zephyr.je.helper.ScheduleResourceHelper;
import com.thed.zephyr.je.helper.StepResultResourceHelper;
import com.thed.zephyr.je.helper.TraceabilityResourceHelper;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.model.ScheduleDefect;
import com.thed.zephyr.je.model.StepDefect;
import com.thed.zephyr.je.permissions.model.PermissionType;
import com.thed.zephyr.je.permissions.service.ZephyrPermissionManager;
import com.thed.zephyr.je.rest.filter.ZFJApiFilter;
import com.thed.zephyr.je.service.ExportService;
import com.thed.zephyr.je.service.ScheduleManager;
import com.thed.zephyr.je.service.StepResultManager;
import com.thed.zephyr.je.service.ZAPIValidationService;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.ZCollectionUtils;
import com.thed.zephyr.util.ZephyrCacheControl;
import com.thed.zephyr.util.ZephyrComponentAccessor;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

@Api(value = "Rest end point for Traceability", description = "Rest end point for Traceability")
@Path("traceability")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@AnonymousAllowed
@ResourceFilters(ZFJApiFilter.class)
public class TraceabilityResource {
    protected final Logger log = Logger.getLogger(TraceabilityResource.class);
    private static final String ERROR_LOG_MESSAGE = "[Error] [Error code: %s %s Error Message : %s]";

    private final JiraAuthenticationContext authContext;

    private final SearchProvider searchProvider;

    private final ScheduleManager scheduleManager;

    private final IssueManager issueManager;

    private final StepResultManager stepResultManager;

    private final ZAPIValidationService zapiValidationService;

    private final ExportService exportService;

    private final PermissionManager permissionManager;

    private final ZephyrPermissionManager zephyrPermissionManager;

    
    public TraceabilityResource(final JiraAuthenticationContext authContext,
                                final SearchProvider searchProvider, ScheduleManager scheduleManager, IssueManager issueManager,
                                StepResultManager stepResultManager, ZAPIValidationService zapiValidationService, ExportService exportService, 
                                PermissionManager permissionManager,ZephyrPermissionManager zephyrPermissionManager) {
        this.authContext = authContext;
        this.searchProvider = searchProvider;
        this.scheduleManager = scheduleManager;
        this.issueManager = issueManager;
        this.stepResultManager = stepResultManager;
        this.zapiValidationService = zapiValidationService;
        this.exportService = exportService;
        this.permissionManager = permissionManager;
        this.zephyrPermissionManager=zephyrPermissionManager;
    }

    @ApiOperation(value = "Get List of Search Execution By Test", notes = "Get Search Execution List by Test Id/Key")
    @ApiImplicitParams({ @ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "[{\"Executions\":{\"id\":12420,\"key\":\"TEST-1\",\"status\":\"To Do\",\"statusId\":\"10000\",\"summary\":\"test\"},\"tests\":[],\"totalDefects\":0}]")})
    @GET
    @Path("executionsByTest")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @AnonymousAllowed
    public Response searchExecutionsByTest(@QueryParam("testIdOrKey") String testIdOrKey, @QueryParam("maxRecords") Integer maxRecords, @QueryParam("offset") Integer offset) {
        Map<String, Object> response = new TreeMap<String, Object>();
        if (authContext.getLoggedInUser() == null) {
            return buildLoginErrorResponse();
        }

        JSONObject errorJsonObject = zapiValidationService.validateEntityStr(testIdOrKey, "Input Parameter");
        if (errorJsonObject != null) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorJsonObject.toString()));
            return Response.status(Status.BAD_REQUEST).entity(errorJsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
        }

        MutableIssue testIssue = null;
        if (Pattern.matches(".*[a-zA-Z]+.*", testIdOrKey)) {
            testIssue = issueManager.getIssueObject(testIdOrKey);
        } else {
            testIssue = issueManager.getIssueObject(Long.valueOf(testIdOrKey));
        }

        if (testIssue == null) {
            errorJsonObject = zapiValidationService.validateEntity(testIssue, "Test/Issue");
            if (errorJsonObject != null) {
                log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorJsonObject.toString()));
                return Response.status(Status.BAD_REQUEST).entity(errorJsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        }

        boolean hasTestViewPermission = JiraUtil.hasIssueViewPermission(null,testIssue,authContext.getLoggedInUser());
        if (!hasTestViewPermission) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.project.permission.error", "Testcase", String.valueOf(testIssue.getProjectId()));
            log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(), Status.FORBIDDEN,errorMessage));
            return JiraUtil.buildErrorResponse(Status.FORBIDDEN, "Insufficient Issue Permission", errorMessage, errorMessage);
        }

        // checking the project browse permissions
        Project project = testIssue.getProjectObject();
        if (!permissionManager.hasPermission(Permissions.BROWSE, project, authContext.getLoggedInUser())) {
            String errorMessage = authContext.getI18nHelper().getText("zapi.execution.move.invalid.projectid");
            log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(), Status.FORBIDDEN,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "NullVersion", errorMessage, errorMessage);
        }

        ProjectPermissionKey projectPermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_BROWSE_CYCLE.toString());
        boolean hasZephyrPermission = zephyrPermissionManager.validateUserPermission(projectPermissionKey, project, authContext.getLoggedInUser(), project.getId());
        if(hasZephyrPermission) {
	        final Integer testId = testIssue.getId().intValue();
	        List<Schedule> schedules = scheduleManager.getSchedulesByIssueId(testId, offset, maxRecords);
	        ScheduleResourceHelper scheduleHelper = new ScheduleResourceHelper(issueManager);
	        StepResultResourceHelper stepResourceHelper = new StepResultResourceHelper(issueManager);
	        List<Map<String, Object>> executions = new ArrayList<Map<String, Object>>();
	        for (Schedule schedule : schedules) {
                if(JiraUtil.isIssueSecurityEnabled()) {
                    Issue issue = issueManager.getIssueObject(Long.valueOf(schedule.getIssueId()));
                    if(issue != null) {
                        boolean hasIssueViewPermission = JiraUtil.hasIssueViewPermission(null, issue, authContext.getLoggedInUser());
                        if (!hasIssueViewPermission) {
                            log.info("User does not have Permission to the execution belonging to Test:" + issue.getKey());
                            continue;
                        }
                    }
                }
	            Map<String, Object> scheduleData = new TreeMap<String, Object>();
	            scheduleData.put("execution", scheduleHelper.convertScheduleToMap(schedule));
	            List<ScheduleDefect> associatedDefects = scheduleManager.getAssociatedDefects(schedule.getID());
	            List<Map<String, String>> scheduleDefectList = scheduleHelper.convertScheduleDefectToMap(associatedDefects);
	            scheduleData.put("defects", scheduleDefectList);
	
	            List<StepDefect> associatedStepDefects = stepResultManager.getStepResultsWithDefectBySchedule(schedule.getID());
	            List<Map<String, String>> stepDefectsList = stepResourceHelper.convertScheduleDefectToMap(associatedStepDefects, null);
	            scheduleData.put("stepDefects", stepDefectsList);
	            executions.add(scheduleData);
	        }
	        response.put("executions", executions);
	        response.put("totalCount", scheduleManager.getSchedulesCountByIssueId(testId));
        } else {
	        response.put("executions", new ArrayList<Map<String, Object>>());
	        response.put("totalCount", 0);
        	response.put("PERM_DENIED", authContext.getI18nHelper().getText("zephyr.plugin.permissions.invalid.error"));
            log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(), Status.FORBIDDEN,authContext.getI18nHelper().getText("zephyr.plugin.permissions.invalid.error")));
        }
        return Response.ok(response).cacheControl(ZephyrCacheControl.never()).build();
    }

    @ApiOperation(value = "Get List of Search Test by Requirement", notes = "Get Search Test by Requirement Id/Key")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "[{\"requirement\":{\"id\":12420,\"key\":\"TEST-1\",\"status\":\"To Do\",\"statusId\":\"10000\",\"summary\":\"test\"},\"tests\":[],\"totalDefects\":0}]")})
    @SuppressWarnings("unchecked")
    @GET
    @Path("testsByRequirement")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @AnonymousAllowed
    public Response searchTestsByRequirement(@QueryParam("requirementIdOrKeyList") String requirementIdOrKeyList) {
        if (authContext.getLoggedInUser() == null) {
            return buildLoginErrorResponse();
        }
        JSONObject errorJsonObject = zapiValidationService.validateEntityStr(requirementIdOrKeyList, "Input Parameter");
        if (errorJsonObject != null) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorJsonObject.toString()));
            return Response.status(Status.BAD_REQUEST).entity(errorJsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
        }

        String[] requirementIdArray = requirementIdOrKeyList.split(",");
        List<Map<String, Object>> response = new ArrayList<Map<String, Object>>();
        ScheduleResourceHelper scheduleHelper = new ScheduleResourceHelper(issueManager);
        TraceabilityResourceHelper traceabilityResourceHelper =
                new TraceabilityResourceHelper(scheduleManager, stepResultManager, issueManager, authContext, searchProvider);
        for (String requirementIdOrKey : requirementIdArray) {
            if (requirementIdOrKey != null) {
                Map<String, Object> responseData = new TreeMap<String, Object>();
                Set<Object> uniqueDefectList = new TreeSet<Object>();
                try {
                    MutableIssue requirement = null;
                    if (Pattern.matches(".*[a-zA-Z]+.*", requirementIdOrKey)) {
                        requirement = issueManager.getIssueObject(requirementIdOrKey);
                    } else {
                        requirement = issueManager.getIssueObject(Long.valueOf(requirementIdOrKey));
                    }

                    if (null == requirement)
                        continue;

                    // checking the project browse permissions. Skip the issues for those permission is invalid
                    Project project = requirement.getProjectObject();
                    if (!permissionManager.hasPermission(Permissions.BROWSE, project, authContext.getLoggedInUser())) {
                        log.debug("You don't have permission to browse JIRA REQUIREMENT: " + requirementIdOrKey);
                        continue;
                    }
                    
                    responseData.put("requirement", scheduleHelper.convertIssueToMap(requirement));
                    List<Issue> issues = traceabilityResourceHelper.findLinkedTestsWithIssue(String.valueOf(requirementIdOrKey));
                    List<Map<String, Object>> tests = new ArrayList<Map<String, Object>>();
                    for (Issue issue : issues) {
                        Map<String, Object> testData = new TreeMap<String, Object>();
                        testData.put("test", scheduleHelper.convertIssueToMap(issue));
                        ProjectPermissionKey projectPermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_BROWSE_CYCLE.toString());
                        boolean hasZephyrPermission = zephyrPermissionManager.validateUserPermission(projectPermissionKey, project, authContext.getLoggedInUser(), project.getId());
                        if(hasZephyrPermission) {
	                        Map<String, Object> statMap = traceabilityResourceHelper.getStatisticsForIssue(issue.getId(), null);
	                        uniqueDefectList.addAll((Set<Object>) statMap.get("defectList"));
	                        testData.put("executionStat", statMap.get("executionStat"));
	                        testData.put("defectStat", statMap.get("defectStat"));
	                        testData.put("defects", statMap.get("defects") != null ? (List<Map<String, Object>>) statMap.get("defects") : new ArrayList<Map<String, Object>>());
	                        tests.add(testData);
                        } else {
                            log.debug(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(), Status.FORBIDDEN,authContext.getI18nHelper().getText("zephyr.plugin.permissions.invalid.error")));
                        	testData.put("PERM_DENIED", authContext.getI18nHelper().getText("zephyr.plugin.permissions.invalid.error"));
	                        tests.add(testData);
                        }
                    }
                    responseData.put("tests", tests);
                    responseData.put("totalDefects", uniqueDefectList.size());
                } catch (Exception exception) {
                    log.error("Error getting issue, probably this issue doesn't exist.", exception);
                }
                response.add(responseData);
            }
        }
        return Response.ok(response).cacheControl(ZephyrCacheControl.never()).build();
    }

    @ApiOperation(value = "Search Defect Statistics ", notes = "Search Defect Statistics by Defect Id/Key List")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "[{\"defect\":{\"id\":12420,\"key\":\"TEST-1\",\"status\":\"To Do\",\"statusId\":\"10000\",\"summary\":\"test\"},\"executionStat\":{\"statuses\":[],\"total\":0},\"reqStat\":{\"count\":0},\"requirements\":[],\"testStat\":{\"count\":0}}]")})
    @GET
    @Path("defectStatistics")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @AnonymousAllowed
    public Response searchDefectStatistics(@QueryParam("defectIdOrKeyList") String defectIdOrKeyList) {
        if (authContext.getLoggedInUser() == null) {
            return buildLoginErrorResponse();
        }

        JSONObject errorJsonObject = zapiValidationService.validateEntityStr(defectIdOrKeyList, "Input Parameter");
        if (errorJsonObject != null) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorJsonObject.toString()));
            return Response.status(Status.BAD_REQUEST).entity(errorJsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
        }

        List<Map<String, Object>> response = new ArrayList<Map<String, Object>>();
        String[] defectIdOrKeyArray = defectIdOrKeyList.split(",");
        ScheduleResourceHelper scheduleHelper = new ScheduleResourceHelper(issueManager);
        TraceabilityResourceHelper traceabilityResourceHelper =
                new TraceabilityResourceHelper(scheduleManager, stepResultManager, issueManager, authContext, searchProvider);
        for (String defectIdOrKey : defectIdOrKeyArray) {
            Map<String, Object> defectStatsMap = new TreeMap<String, Object>();
            try {
                Issue defect = null;
                if (Pattern.matches(".*[a-zA-Z]+.*", defectIdOrKey)) {
                    defect = issueManager.getIssueObject(defectIdOrKey);
                } else {
                    defect = issueManager.getIssueObject(Long.valueOf(defectIdOrKey));
                }

                //If issue doesn't exists or not found, skip this issue and continue
                boolean hasIssueViewPermission = JiraUtil.hasIssueViewPermission(null,defect,authContext.getLoggedInUser());
                if (null == defect || !hasIssueViewPermission)
                    continue;

                // checking the project browse permissions. Skip the issues for those permission is invalid
                Project project = defect.getProjectObject();
                if (!permissionManager.hasPermission(Permissions.BROWSE, project, authContext.getLoggedInUser())) {
                    log.debug("You don't have permission to browse JIRA DEFECT: " + defectIdOrKey);
                    continue;
                }

                defectStatsMap.put("defect", scheduleHelper.convertIssueToMap(defect));
                ProjectPermissionKey projectPermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_BROWSE_CYCLE.toString());
                boolean hasZephyrPermission = zephyrPermissionManager.validateUserPermission(projectPermissionKey, project, authContext.getLoggedInUser(), project.getId());
                if(hasZephyrPermission) {
	                List<Schedule> allSchedules = scheduleManager.getSchedulesByDefectId(defect.getId().intValue(), true);
	                Map<String, Object> execStatMap = new TreeMap<String, Object>();
	                execStatMap.put("total", allSchedules.size());
	                List<Map<String, Object>> statuses = new ArrayList<Map<String, Object>>();
	                Map<String, Object> statusesMap = new TreeMap<String, Object>();
	                Set<Integer> uniqueTestList = new TreeSet<Integer>();
	                for (Schedule schedule : allSchedules) {
	                    uniqueTestList.add(schedule.getIssueId());
	                    ExecutionStatus executionStatus = JiraUtil.getExecutionStatuses().get(Integer.parseInt(schedule.getStatus()));
	                    if (statusesMap.get(executionStatus.getName()) != null) {
	                        Integer previousCount = (Integer) statusesMap.get(executionStatus.getName());
	                        previousCount++;
	                        statusesMap.put(executionStatus.getName(), previousCount);
	                    } else {
	                        statusesMap.put(executionStatus.getName(), 1);
	                    }
	                }
	                for (Map.Entry<String, Object> entry : statusesMap.entrySet()) {
	                    Map<String, Object> statusMap = new TreeMap<String, Object>();
	                    statusMap.put("status", entry.getKey());
	                    statusMap.put("count", entry.getValue());
	                    statuses.add(statusMap);
	                }
	                execStatMap.put("statuses", statuses);
	                Map<String, Object> testStat = new TreeMap<String, Object>();
	                testStat.put("count", uniqueTestList.size());
	                Set<Long> uniqueReqList = new TreeSet<Long>();
	                List<Map<String, Object>> requirementMap = new ArrayList<Map<String, Object>>();
	                for (Integer test : uniqueTestList) {
	                    List<Issue> requirementList = traceabilityResourceHelper.findLinkedIssuesWithTest(String.valueOf(test), defect.getIssueTypeObject().getId());
	                    for (Issue requirement : requirementList) {
	                        Map<String, Object> reqMap = new HashMap<String, Object>();
	                        reqMap = scheduleHelper.convertIssueToMap(requirement);
	                        requirementMap.add(reqMap);
	                        uniqueReqList.add(requirement.getId());
	                    }
	                }
	                Map<String, Object> reqStat = new TreeMap<String, Object>();
	                reqStat.put("count", uniqueReqList.size());
	                defectStatsMap.put("executionStat", execStatMap);
	                defectStatsMap.put("testStat", testStat);
	                defectStatsMap.put("reqStat", reqStat);
	                defectStatsMap.put("requirements", requirementMap);
                } else {
                    log.debug(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(), Status.FORBIDDEN,authContext.getI18nHelper().getText("zephyr.plugin.permissions.invalid.error")));
                	defectStatsMap.put("PERM_DENIED", authContext.getI18nHelper().getText("zephyr.plugin.permissions.invalid.error"));
                }
            } catch (Exception e) {
                log.error("Error retrieving issue, this issue probably doesn't exist.", e);
            }
            response.add(defectStatsMap);
        }
        return Response.ok(response).cacheControl(ZephyrCacheControl.never()).build();
    }

    @ApiOperation(value="Search Execution by Defect", notes = "Get Execution by Defect Id/Key")
    @ApiImplicitParams({ @ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"executions\":[{\"defects\":[{\"key\":\"AB-7\",\"resolution\":\"\",\"status\":\"To Do\",\"statusId\":\"10000\",\"summary\":\"qwert\"}],\"execution\":{\"id\":\"2687\",\"status\":\"FAIL\",\"statusId\":\"2\",\"testCycle\":\"Ad hoc\"},\"stepDefects\":[]}],\"totalCount\":1}")})
    @SuppressWarnings({"rawtypes", "unchecked"})
    @GET
    @Path("executionsByDefect")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @AnonymousAllowed
    public Response searchExecutionsByDefect(@QueryParam("defectIdOrKey") final String defectIdOrKey, @QueryParam("maxRecords") Integer maxRecords,
                                             @QueryParam("offset") Integer offset) {
        if (authContext.getLoggedInUser() == null) {
            return buildLoginErrorResponse();
        }

        JSONObject errorJsonObject = zapiValidationService.validateEntityStr(defectIdOrKey, "Input Parameter");
        if (errorJsonObject != null) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorJsonObject.toString()));
            return Response.status(Status.BAD_REQUEST).entity(errorJsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
        }

        MutableIssue defectIssue = null;
        if (Pattern.matches(".*[a-zA-Z]+.*", defectIdOrKey)) {
            defectIssue = issueManager.getIssueObject(defectIdOrKey);
        } else {
            defectIssue = issueManager.getIssueObject(Long.valueOf(defectIdOrKey));
        }

        if (defectIssue == null) {
            errorJsonObject = zapiValidationService.validateEntity(defectIssue, "Defect/Issue");
            if (errorJsonObject != null) {
                log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorJsonObject.toString()));
                return Response.status(Status.BAD_REQUEST).entity(errorJsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        }

        boolean hasDefectViewPermission = JiraUtil.hasIssueViewPermission(null,defectIssue,authContext.getLoggedInUser());
        if (!hasDefectViewPermission) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.project.permission.error", "Issue", String.valueOf(defectIssue.getProjectId()));
            log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(), Status.FORBIDDEN,errorMessage));
            return JiraUtil.buildErrorResponse(Status.FORBIDDEN, "Insufficient Issue Permission", errorMessage, errorMessage);
        }

        // checking the project browse permissions
        Project project = defectIssue.getProjectObject();
        if (!permissionManager.hasPermission(Permissions.BROWSE, project, authContext.getLoggedInUser())) {
            String errorMessage = authContext.getI18nHelper().getText("zapi.execution.move.invalid.projectid");
            log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(), Status.FORBIDDEN,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "NullVersion", errorMessage, errorMessage);
        }
        
        ProjectPermissionKey projectPermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_BROWSE_CYCLE.toString());
        boolean hasZephyrPermission = zephyrPermissionManager.validateUserPermission(projectPermissionKey, project, authContext.getLoggedInUser(), project.getId());
        Map<String, Object> response = new TreeMap<String, Object>();
        List<Map<String, Object>> executions = new ArrayList<Map<String, Object>>();
        if(hasZephyrPermission) {
	        final Integer defectId = defectIssue.getId().intValue();
	        Map<String,Object> schedulesMap = scheduleManager.getTestAndStepSchedulesByDefectId(defectId, offset, maxRecords);
	        List<Schedule> schedules = schedulesMap.get("schedules") != null ? (ArrayList)schedulesMap.get("schedules") : new ArrayList<Schedule>();
	        ScheduleResourceHelper scheduleHelper = new ScheduleResourceHelper(issueManager);
	        TraceabilityResourceHelper traceabilityResourceHelper =
	                new TraceabilityResourceHelper(scheduleManager, stepResultManager, issueManager, authContext, searchProvider);
	        for (Schedule schedule : schedules) {
	            Map<String, Object> execTestReqSet = new TreeMap<String, Object>();
	            Map executionMap = scheduleHelper.convertScheduleToMap(schedule);
	            List<ScheduleDefect> scheduleDefects = scheduleManager.getAssociatedDefects(schedule.getID());
	            Boolean executionDefect = CollectionUtils.exists(scheduleDefects, new Predicate() {
	                @Override
	                public boolean evaluate(Object object) {
	                    ScheduleDefect defect = (ScheduleDefect) object;
	                    return defect.getDefectId().equals(defectId);
	                }
	            });
	            if (executionDefect) {
	                executionMap.put("stepLevel", false);
	            } else {
	                executionMap.put("stepLevel", true);
	            }

                execTestReqSet.put("execution", executionMap);
                traceabilityResourceHelper.createTestReqSet(String.valueOf(defectId), schedule, execTestReqSet);
                executions.add(execTestReqSet);
	        }
	        int totalDefect = schedulesMap.get("totalCount") != null ? (Integer)schedulesMap.get("totalCount") : 0;
	        response.put("executions", executions);
	        response.put("totalCount", totalDefect);
        } else {
        	response.put("PERM_DENIED", authContext.getI18nHelper().getText("zephyr.plugin.permissions.invalid.error"));
            response.put("executions", new ArrayList<Map<String, Object>>());
        	response.put("totalCount", 0);
            log.debug(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(), Status.FORBIDDEN,"Get Execution by Defect Id/Key "+authContext.getI18nHelper().getText("zephyr.plugin.permissions.invalid.error")));
        }
        return Response.ok(response).cacheControl(ZephyrCacheControl.never()).build();
    }

    /**
     * Exports Executions based on ZQL Search
     *
     * @param request
     * @param params
     * @return
     */
    @ApiOperation(value = "Export Traceability Report", notes = "Export Traceability Report by Defect Id List")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{\"exportType\":\"HTML\",\"requirementIdList\":[10002],\"versionId\":0}"),
            @ApiImplicitParam(name = "response", value = "{\"url\":\"http://localhost:8712/jira/plugins/servlet/export/exportAttachment?fileName=ZFJ-ReqDefectReport-1461831474904.html\"}")})
    @SuppressWarnings({"unchecked"})
    @POST
    @Path("export")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @AnonymousAllowed
    public Response exportTraceabilityReport(@Context HttpServletRequest request, Map<String, Object> params) {
        if (authContext.getLoggedInUser() == null) {
            return buildLoginErrorResponse();
        }

        if (params == null || params.isEmpty()) {
            return buildErrorMessage(authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Export", "Request [params are null/empty]"));
        }

        String exportType = ZCollectionUtils.getAsString(params, "exportType") != null ? ZCollectionUtils.getAsString(params, "exportType") : "html";
        Collection<Long> defectIds = new ArrayList<Long>();
        if (params.get("defectIdList") != null) {
            defectIds = CollectionUtils.collect((Collection<Object>) params.get("defectIdList"), new Transformer() {
                @Override
                public Long transform(final Object input) {
                    if (StringUtils.isBlank(String.valueOf(input))) {
                        return null;
                    }
                    final Long defectId = Long.valueOf(input.toString());

                    // checking the project browse permissions. Skip the issues for those permission is invalid
                    Issue issue = issueManager.getIssueObject(defectId);
                    Project project = issue.getProjectObject();
                    if (!permissionManager.hasPermission(Permissions.BROWSE, project, authContext.getLoggedInUser())) {
                        log.debug("You do not have permission to browse Jira Defect: " + defectId);
                        return null;
                    }

                    boolean hasIssueViewPermission = JiraUtil.hasIssueViewPermission(null,issue,authContext.getLoggedInUser());
                    if (!hasIssueViewPermission) {
                        log.info("You do not have permission to browse Jira Defect: " + issue != null ? issue.getKey() : defectId);
                        return null;
                    }

                    return defectId;
                }
            });
        }
        Set<Long> uniqueDefectIdList = new HashSet<Long>(defectIds);

        Collection<Long> requirementIds = new ArrayList<Long>();
        if (params.get("requirementIdList") != null) {
            requirementIds = CollectionUtils.collect((Collection<Object>) params.get("requirementIdList"), new Transformer() {
                @Override
                public Long transform(final Object input) {
                    if (StringUtils.isBlank(String.valueOf(input))) {
                        return null;
                    }
                    final Long requirementId = Long.valueOf(input.toString());

                    // checking the project browse permissions. Skip the issues for those permission is invalid
                    Issue issue = issueManager.getIssueObject(requirementId);
                    Project project = issue.getProjectObject();
                    if (!permissionManager.hasPermission(Permissions.BROWSE, project, authContext.getLoggedInUser())) {
                        log.debug("You don't have permission to browse JIRA REQUIREMENT: " + requirementId);
                        return null;
                    }
                    return requirementId;
                }
            });
        }
        Set<Long> requirementIdList = new HashSet<Long>(requirementIds);
        File file = null;
        try {
            if (defectIds != null && !defectIds.isEmpty()) {
                file = exportService.createDefectRequirementReport(exportType, uniqueDefectIdList, null);
            } else if (requirementIds != null && !requirementIds.isEmpty()) {
                file = exportService.createRequirementDefectReport(exportType, requirementIdList, null);
            } else {
                return Response.status(Status.BAD_REQUEST).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (Exception e) {
            log.error("Error exporting report", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).cacheControl(ZephyrCacheControl.never()).build();
        }
        return buildExportResponse(file);
    }

    /**
     * Build export response in the same lines as other export.
     *
     * @param file
     * @return
     */
    private Response buildExportResponse(File file) {
        if (file != null) {
            JSONObject ob = new JSONObject();
            try {
                ApplicationProperties applicationProperties = (ApplicationProperties) ZephyrComponentAccessor.getInstance().getComponent("applicationProperties");
                String fileUrl = applicationProperties.getBaseUrl() + "/plugins/servlet/export/exportAttachment?fileName=" + file.getName();
                ob.put("url", fileUrl);
                return Response.ok(ob.toString()).build();
            } catch (JSONException e) {
                log.warn("Error exporting traceability file", e);
                return Response.status(Status.SERVICE_UNAVAILABLE).build();
            }
        } else {
            log.info(String.format(ERROR_LOG_MESSAGE,Status.SERVICE_UNAVAILABLE.getStatusCode(),Status.SERVICE_UNAVAILABLE,"Export data service is unavailable."));
            return Response.status(Status.SERVICE_UNAVAILABLE).build();
        }
    }

    private Response buildLoginErrorResponse() {
        /*
        * commented for consistent error message.
        String loginUrl = ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL) + "/login.jsp";
        StringBuilder sb = new StringBuilder(authContext.getI18nHelper().getText("login.required.notloggedin.permissionviolation"));
        sb.append("<a href=\"");
        sb.append(loginUrl);
        sb.append("\"> ");
        sb.append(authContext.getI18nHelper().getText("login.required.title"));
        sb.append("<a>.");
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("error", sb);
        } catch (JSONException e) {
            log.warn("Error creating JSON Object", e);
        }
        return Response.status(Status.FORBIDDEN).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
        */

        JSONObject jsonObject = new JSONObject();
        try {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(), Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
            jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
        } catch (JSONException e) {
            log.error("Error occurred while creating the response object.", e);
            return Response.status(Status.BAD_REQUEST).build();
        }
        return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
    }

    private Response buildErrorMessage(String errorMessage) {
        JSONObject errorJsonObject = new JSONObject();
        try {
            errorJsonObject.put("error", errorMessage);
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorMessage));
        } catch (JSONException e) {
            log.error("Error constructing JSON", e);
        }
        return Response.status(Status.BAD_REQUEST).entity(errorJsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
    }
}
