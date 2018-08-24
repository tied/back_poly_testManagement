package com.thed.zephyr.je.rest;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.link.RemoteIssueLinkManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.task.context.Contexts;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.usercompatibility.UserCompatibilityHelper;
import com.atlassian.jira.util.collect.CollectionEnclosedIterable;
import com.atlassian.jira.util.collect.EnclosedIterable;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.web.util.FileIconBean;
import com.atlassian.jira.web.util.FileIconUtil;
import com.google.common.collect.*;
import com.opensymphony.util.TextUtils;
import com.sun.jersey.spi.container.ResourceFilters;
import com.thed.zephyr.je.config.model.ExecutionStatus;
import com.thed.zephyr.je.event.EntityType;
import com.thed.zephyr.je.event.EventType;
import com.thed.zephyr.je.event.StepResultModifyEvent;
import com.thed.zephyr.je.helper.ScheduleResourceHelper;
import com.thed.zephyr.je.helper.ScheduleSearchResourceHelper;
import com.thed.zephyr.je.helper.StepResultResourceHelper;
import com.thed.zephyr.je.index.ScheduleIndexManager;
import com.thed.zephyr.je.model.*;
import com.thed.zephyr.je.permissions.model.PermissionType;
import com.thed.zephyr.je.permissions.service.ZephyrPermissionManager;
import com.thed.zephyr.je.rest.delegate.CustomFieldValueResourceDelegate;
import com.thed.zephyr.je.rest.delegate.ExecutionWorkflowResourceDelegate;
import com.thed.zephyr.je.rest.exception.RESTException;
import com.thed.zephyr.je.rest.filter.ZFJApiFilter;
import com.thed.zephyr.je.service.*;
import com.thed.zephyr.je.vo.StepResultBean;
import com.thed.zephyr.je.zql.core.SearchService;
import com.thed.zephyr.util.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Api(value = "StepResult Resource API(s)", description = "Following section describes the rest resources pertaining to StepResultResource")
@Path("stepResult")
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@ResourceFilters(ZFJApiFilter.class)
public class StepResultResource{
    private static final String STEP_RESULT_ENTITY = "StepResult";
    private static final String ID = " ID : %s";
	private static final String ERROR_LOG_MESSAGE = "[Error] [Error code: %s %s Error Message : %s]";

	private static final String TEST_STEP = "testStep";
    private static final String TEST_DATA = "testData";
    private static final String TEST_RESULT = "testResult";
    private static final String STATUS = "status";
    private static final String COMMENT = "comment";
    private static final String DEFAULT_SEARCH = "default";


    protected final Logger log = Logger.getLogger(StepResultResource.class);
	private final TeststepManager testStepManager;
	private final StepResultManager stepResultManager;
	private final JiraAuthenticationContext authContext;
	private final IssueManager issueManager;
	private final DateTimeFormatterFactory dateTimeFormatterFactory;
	private final EventPublisher eventPublisher;
    private ZAPIValidationService zapiValidationService;
	private final ScheduleManager scheduleManager;
	private final RemoteIssueLinkManager rilManager;
	private final ZephyrPermissionManager zephyrPermissionManager;
    private final SearchService searchService;
    private final ScheduleIndexManager scheduleIndexManager;
    private final AttachmentManager attachmentManager;
    private final FileIconUtil fileIconUtil;
    private final CustomFieldValueResourceDelegate customFieldValueResourceDelegate;
    private final ExecutionWorkflowResourceDelegate executionWorkflowResourceDelegate;

	public StepResultResource(StepResultManager stepResultManager,
							  JiraAuthenticationContext authContext,
							  DateTimeFormatterFactory dateTimeFormatterFactory,
							  IssueManager issueManager, EventPublisher eventPublisher,
							  ZAPIValidationService zapiValidationService,
							  ScheduleManager scheduleManager,
							  TeststepManager testStepManager, RemoteIssueLinkManager rilManager,
							  ZephyrPermissionManager zephyrPermissionManager,
                              SearchService searchService,
                              ScheduleIndexManager scheduleIndexManager,
                              AttachmentManager attachmentManager,
                              final FileIconUtil fileIconUtil,
                              final CustomFieldValueResourceDelegate customFieldValueResourceDelegate,
                              ExecutionWorkflowResourceDelegate executionWorkflowResourceDelegate) {
		super();
		this.stepResultManager = stepResultManager;
		this.authContext = authContext;
		this.issueManager = issueManager;
		this.dateTimeFormatterFactory = dateTimeFormatterFactory;
		this.eventPublisher = eventPublisher;
		this.zapiValidationService=zapiValidationService;
		this.scheduleManager=scheduleManager;
		this.testStepManager=testStepManager;
		this.rilManager = rilManager;
		this.zephyrPermissionManager=zephyrPermissionManager;
		this.searchService = searchService;
		this.scheduleIndexManager = scheduleIndexManager;
		this.attachmentManager = attachmentManager;
		this.fileIconUtil = fileIconUtil;
		this.customFieldValueResourceDelegate = customFieldValueResourceDelegate;
		this.executionWorkflowResourceDelegate = executionWorkflowResourceDelegate;
	}
	@ApiOperation(value = "Get list of Step Result", notes = "Get List of Step Result by Execution Id")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "{\"id\":124,\"status\":\"-1\",\"comment\":\"\",\"htmlComment\":\"\",\"executionId\":409,\"stepId\":77,\"defects\":[],\"modifiedBy\":\"vm_admin\"}")})
	@GET
	public Response getStepResults(@QueryParam ("executionId") final String executionId, @QueryParam ("expand") final String expandos,
								   @QueryParam("offset") Integer offset, @QueryParam("limit") final Integer limit){
		JSONObject errorJsonObject = zapiValidationService.validateExecutionId(executionId);
		if(null != errorJsonObject) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorJsonObject.toString()));
			return Response.status(Status.BAD_REQUEST).entity(errorJsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
		}

        Issue issue = null;
		Schedule schedule = scheduleManager.getSchedule(Integer.parseInt(executionId));
		Response errorMessage = checkIssueSecurityPermission(schedule);
		if (errorMessage != null) return errorMessage;

        Integer totalStepResultsCount = stepResultManager.getStepResultsCount(Integer.parseInt(executionId));
        List<StepResult> steps = stepResultManager.getStepResultsByScheduleByPagination(Integer.parseInt(executionId),offset,limit);
		List<StepResultBean> stepResultsBeanCollection = new ArrayList<>();
		StepResultResourceHelper stepResourceHelper = new StepResultResourceHelper(issueManager);

		List<ExecutionStatus> executionStatusList;
        if(StringUtils.isNotBlank(expandos) &&
                StringUtils.containsIgnoreCase(expandos, "executionStatus")) {
            executionStatusList = JiraUtil.getStepExecutionStatusList();
        }else {
            executionStatusList = Lists.newArrayList();
        }

        if(null != schedule) {
            issue = issueManager.getIssueObject(new Long(schedule.getIssueId()));
        }
        if(CollectionUtils.isNotEmpty(steps)) {
            for(StepResult stepResult : steps){
                StepResultBean stepResultBean = new StepResultBean(stepResult);
                List<StepDefect> defects = stepResultManager.getAssociatedDefects(stepResult.getID());
                List<Map<String, String>> defectsMap = stepResourceHelper.convertScheduleDefectToMap(defects, null);
                stepResultBean.setDefects(defectsMap);

                stepResultBean.setStepResultAttachmentCount(attachmentManager.getAttachmentsByEntityIdAndType(stepResult.getID(),ApplicationConstants.TESTSTEPRESULT_TYPE).size());
                stepResultBean.setStepResultsCount(totalStepResultsCount);
                stepResultBean.setExecutionStatus(executionStatusList);
                stepResultBean = setTeststepValueResponse(stepResultBean,stepResult.getStep(),issue);
                stepResultsBeanCollection.add(stepResultBean);
            }
        }

		return Response.ok(stepResultsBeanCollection).cacheControl(ZephyrCacheControl.never()).build();
	}

    @ApiOperation(value = "Get StepResult Information", notes = "Get Single Step Result Information by StepResult Id")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "{\"executionId\":1011,\"comment\":\"no comment status is pass\",\"stepId\":123,\"status\":pass}")})
	@GET
    @Path("/{id}")
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	public Response getStepResult(@PathParam ("id") final Integer stepResultId, @QueryParam ("expand") final String expandos){
		JSONObject errorJsonObject = zapiValidationService.validateId(stepResultId,"stepResultId");
		if(null != errorJsonObject) 
			return parseErrorJSONObject(errorJsonObject);	
		StepResult stepResult = stepResultManager.getStepResult(stepResultId);
		//Step Result is Null
		if(stepResult == null) {
			String errorMessage = authContext.getI18nHelper().getText("zephyr.common.error.invalid", "StepResultId",String.valueOf(stepResultId));
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorMessage));
			throw new RESTException(Status.BAD_REQUEST,errorMessage);
		}
		if(JiraUtil.isIssueSecurityEnabled()) {
			Schedule schedule = scheduleManager.getSchedule(stepResult.getScheduleId());
			Response errorMessage = checkIssueSecurityPermission(schedule);
			if (errorMessage != null) return errorMessage;
		}
		StepResultResourceHelper stepResourceHelper = new StepResultResourceHelper(issueManager);
		StepResultBean stepResultBean = new StepResultBean(stepResult);
		List<StepDefect> defects = stepResultManager.getAssociatedDefects(stepResult.getID());
		List<Map<String, String>> defectsMap = stepResourceHelper.convertScheduleDefectToMap(defects, null);
		stepResultBean.setDefects(defectsMap);

		//Execution Status if Present in expandos
		if(StringUtils.isNotBlank(expandos) && 
				StringUtils.containsIgnoreCase(expandos, "executionStatus")) {
			stepResultBean.setExecutionStatus(JiraUtil.getStepExecutionStatusList());
		}	
		return Response.ok(stepResultBean).cacheControl(ZephyrCacheControl.never()).build();
	}

	/**
	 * Not currently in use as stepResult gets created as soon as schedule is fetched.
	 * @param stepResultBean
	 * @return
	 */
	@ApiOperation(value = "Create New StepResult", notes = "Create New StepResult, StepResult gets created as soon as execution is fetched")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{\"stepId\":57,\"issueId\":\"12000\",\"executionId\":148,\"status\":\"1\",}"),
			@ApiImplicitParam(name = "response", value = "{\"id\":16,\"executedOn\":1463161435446,\"status\":\"1\",\"htmlComment\":\"\",\"executedBy\":\"vm_admin\",\"executionId\":148,\"stepId\":57,\"createdBy\":\"vm_admin\",\"modifiedBy\":\"vm_admin\"}")})
	@POST
	public Response createStepResult(final StepResultBean stepResultBean) {
		//ZAPI Validation
		JSONObject errorJsonObject = zapiValidationService.validateStepResultBean(stepResultBean);
		if(errorJsonObject != null) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorJsonObject.toString()));
			return Response.status(Status.BAD_REQUEST).entity(errorJsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
		}
		
		if(stepResultManager.stepResultExists(stepResultBean.getExecutionId(), stepResultBean.getStepId())){
			return getErrorResponse(stepResultBean, "stepId:"+stepResultBean.getStepId() + " (stepResult already exists)");
		}
		
		Schedule schedule = scheduleManager.getSchedule(stepResultBean.getExecutionId());
		if(schedule == null) {
			log.error("Failed to create stepResult, schedule not found" + schedule);
			return getErrorResponse(stepResultBean, "executionId:"+stepResultBean.getExecutionId());
		}
		Response errorMessage = checkIssueSecurityPermission(schedule);
		if (errorMessage != null) return errorMessage;

        boolean isWorkflowDisabled = executionWorkflowResourceDelegate.isExecutionWorkflowDisabled(schedule.getProjectId());
        if(!isWorkflowDisabled && null != schedule.getExecutionWorkflowStatus() &&
                schedule.getExecutionWorkflowStatus().name().equals(ExecutionWorkflowStatus.COMPLETED.name())) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("workflow.schedule.modify.error")));
            return getErrorResponse(stepResultBean, authContext.getI18nHelper().getText("workflow.schedule.modify.error"));
        }
		//ZAPI Verify Step IssueId matches the TestStep IssueId
		Teststep testStep = testStepManager.getTeststep(stepResultBean.getStepId());
		if(testStep == null || schedule.getIssueId().intValue() != testStep.getIssueId().intValue()) {
			log.error("Failed to create stepResult, either testStep doesn't exist or issueId doesn't match with schedule" + testStep);
			return getErrorResponse(stepResultBean, "stepId:"+stepResultBean.getStepId());
		}
		
		boolean hasZephyrPermission = verifyBulkPermissions(schedule.getProjectId(), authContext.getLoggedInUser());
		if(hasZephyrPermission) {
			Map<String,Object> resultProperties = new HashMap<String,Object>();
			resultProperties.put("SCHEDULE_ID", stepResultBean.getExecutionId());
			resultProperties.put("COMMENT", stepResultBean.getComment());
			resultProperties.put("STEP_ID", stepResultBean.getStepId());
			resultProperties.put("PROJECT_ID", schedule.getProjectId());
			
			String status = stepResultBean.getStatus();
			resultProperties.put("STATUS", String.valueOf(ApplicationConstants.UNEXECUTED_STATUS));
			if(StringUtils.isNotBlank(status)){
				if(JiraUtil.getStepExecutionStatuses().containsKey(new Integer(status))){
					resultProperties.put("STATUS", status);
				}
			
				if(status != null && !status.equals(String.valueOf(ApplicationConstants.UNEXECUTED_STATUS))){
					resultProperties.put("EXECUTED_ON", System.currentTimeMillis());
					resultProperties.put("EXECUTED_BY",UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
				}
			}
	
			resultProperties.put("CREATED_BY", UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
			resultProperties.put("MODIFIED_BY", UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
	
			StepResult sResult = stepResultManager.addStepResult(resultProperties);
			persistRelatedDefects(stepResultBean.getStepId(), stepResultBean.getExecutionId(), sResult.getID(), stepResultBean.getDefectList(), schedule.getIssueId().longValue());
			updateScheduleProperties(schedule);
			return Response.ok(new StepResultBean(sResult)).cacheControl(ZephyrCacheControl.never()).build();
		} else {
			String error = authContext.getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
			return getPermissionDeniedErrorResponse(error);
		}
	}

    /**
	 * @param stepResultBean
	 * @return 
	 */
	private Response getErrorResponse(final StepResultBean stepResultBean, String errMsgFragment) {
		JSONObject errorJsonObject = null;
		try {
			errorJsonObject = new JSONObject();
			errorJsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.error.create",STEP_RESULT_ENTITY, errMsgFragment));
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorJsonObject.toString()));
			throw new RESTException(Status.BAD_REQUEST, errorJsonObject);
		} catch(JSONException e) {
			log.error("Error creating JSON Object",e);
		}
		return Response.status(Status.BAD_REQUEST).entity(errorJsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
	}

	@ApiOperation(value = "Update StepResult Information", notes = "Update StepResult Information by StepResult Id")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{\"status\":\"2\"}"),
			@ApiImplicitParam(name = "response", value = "{\"executedBy\":\"vm_admin\",\"executionId\":409,\"htmlComment\":\"\",\"stepId\":77,\"comment\":\"\",\"id\":124,\"status\":\"2\"}")})
	@SuppressWarnings("unchecked")
	@PUT
	@Path("/{id}")
	@Consumes({MediaType.APPLICATION_JSON})
	public Response updateStepResult(@PathParam ("id") final Integer stepResultId,
									 final StepResultBean stepResultBean,@Context HttpServletRequest request){
		final ScheduleResourceHelper helper = new ScheduleResourceHelper(issueManager, rilManager, scheduleManager, stepResultManager);

		try{

			StepResult stepResult = stepResultManager.getStepResult(stepResultId);
			Schedule schedule = scheduleManager.getSchedule(stepResult.getScheduleId());
			Boolean issueToTestStepLink = JiraUtil.isIssueToTestStepLinkingEnabled();;
			Boolean remoteIssueLinkStepExecution = JiraUtil.isIssueToTestStepExecutionRemoteLinkingEnabled();
			String cycleBreadCrumb = helper.getCycleBreadCrumbOfSchedule(schedule);
			Issue testcase = issueManager.getIssueObject(new Long(schedule.getIssueId()));
			boolean hasIssueViewPermission = JiraUtil.hasIssueViewPermission(null, testcase, authContext.getLoggedInUser());
			if (!hasIssueViewPermission) {
				String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.project.permission.error", "StepResult", String.valueOf(schedule.getProjectId()));
				return getPermissionDeniedErrorResponse("Insufficient Issue permissions." + errorMessage);
			}

            boolean isWorkflowDisabled = executionWorkflowResourceDelegate.isExecutionWorkflowDisabled(schedule.getProjectId());
            if(!isWorkflowDisabled && null != schedule.getExecutionWorkflowStatus() &&
                    schedule.getExecutionWorkflowStatus().name().equals(ExecutionWorkflowStatus.COMPLETED.name())) {
                JSONObject errorJson = new JSONObject();
                errorJson.put("error", authContext.getI18nHelper().getText("workflow.schedule.modify.error"));
                log.warn(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("workflow.schedule.modify.error")));
                return Response.status(Status.BAD_REQUEST).entity(errorJson.toString()).build();
            }

			String contextPath = request.getContextPath();
			JSONObject errorJsonObject = zapiValidationService.validateStepStatus(stepResultBean.getStatus(),true);
			if(null != errorJsonObject)
				return parseErrorJSONObject(errorJsonObject);
			// validate various id's needed to update step result
			errorJsonObject = zapiValidationService.validateId(stepResultId, "stepResultId");
			if(null != errorJsonObject)
				return parseErrorJSONObject(errorJsonObject);

			if(stepResult == null){
				Map<String, Object> errorMap = new HashMap<String, Object>();
				errorMap.put("errorMessages", new String[]{ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("view.issues.steps.notfound.error", stepResultId)});
				errorMap.put("errors", null);
                log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("view.issues.steps.notfound.error", stepResultId)));
				return Response.status(Response.Status.BAD_REQUEST).entity(errorMap).type(MediaType.APPLICATION_JSON).build();
			}
			// validate stepId
			errorJsonObject = zapiValidationService.validateId(stepResult.getStep().getID(), "stepId");
			if(null != errorJsonObject)
				return parseErrorJSONObject(errorJsonObject);
			boolean hasZephyrPermission = verifyBulkPermissions(schedule.getProjectId(), authContext.getLoggedInUser());
			if(hasZephyrPermission) {
				// Table to keep track of modified properties of given StepResult, for change logs
				Table<String, String, Object> changePropertyTable =  changePropertyTable(stepResult, stepResultBean, StringUtils.equalsIgnoreCase(stepResult.getStatus(), stepResultBean.getStatus()));
	
				//Should we verify if user has entered valid step status or not?
				String newStatus = stepResultBean.getStatus();
				if(newStatus != null) {
					if( !newStatus.equals("-1") ){
						stepResult.setStatus(newStatus);
						stepResult.setExecutedBy(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
						stepResult.setExecutedOn(System.currentTimeMillis());
					} else{
						//User has set the new status to -1, so remove execution details.
						stepResult.setStatus(String.valueOf(ApplicationConstants.UNEXECUTED_STATUS));
						stepResult.setExecutedBy(null);
						stepResult.setExecutedOn(null);
					}
					stepResult.setModifiedBy(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
				}
	
				String comment = stepResultBean.getComment();
				if (comment != null) {
					stepResult.setComment(comment);
					stepResult.setModifiedBy(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
				}
	
				//If this flag is true that means we are coming from "Execute Test" Page and hence update defect list.
				//Otherwise we are doing quick schedule execution and don't bother to update defect list for given schedule.
				String updateFlag = stepResultBean.getUpdateDefectList();
				boolean updateDefectListFlag = updateFlag != null && updateFlag.equals("true") ? true : false;
	
				List<StepDefect> associatedDefects = null;
				List<String> defectList = stepResultBean.getDefectList();
				List<StepDefect> previousAssociatedDefects= stepResultManager.getAssociatedDefects(stepResultId);
				if (updateDefectListFlag){
	
					/*@TODO: change getSchedule with count*/
					Map<String, Object> associatedDefectsMap = persistRelatedDefects(stepResult.getStep().getID(), stepResult.getScheduleId(), stepResultId, defectList, schedule.getIssueId().longValue());
					associatedDefects = (List<StepDefect>) associatedDefectsMap.get("final");
	
					Collection<Integer> associatedAddedDefectIds = (Collection<Integer>)associatedDefectsMap.get("added");
					Collection<Integer> associatedDeletedDefectIds = (Collection<Integer>)associatedDefectsMap.get("deleted");
					Long issueLinkTypeId = ScheduleResourceHelper.getLinkTypeId();
					Long oldIssueLinkTypeId = issueLinkTypeId;
	
					// Saving added/deleted Schedule Defect(s) for change logs
					if((null!= associatedAddedDefectIds && associatedAddedDefectIds.size() > 0) ||
							(null!= associatedDeletedDefectIds && associatedDeletedDefectIds.size() > 0)){
						String[] scheDefs = new String[previousAssociatedDefects.size()];
						int indx = 0;
						// Saving all previous Issue Id's as ',' separated string.
						for (StepDefect defect : previousAssociatedDefects) {
							scheDefs[indx] = defect.getDefectId().toString();
							indx++;
						}
						changePropertyTable.put(EntityType.STEP_DEFECT.toString(), ApplicationConstants.OLD, StringUtils.join(scheDefs, ','));
						// Saving modified Issue Id's as ',' separated string.
						scheDefs = new String[associatedDefects.size()];
						indx = 0;
						for (StepDefect defect : associatedDefects) {
							scheDefs[indx] = defect.getDefectId().toString();
							indx++;
						}
						changePropertyTable.put(EntityType.STEP_DEFECT.toString(), ApplicationConstants.NEW, StringUtils.join(scheDefs, ','));
	
						//Check if issue to test step linking enable then update link.
						if(issueToTestStepLink) {
							helper.addIssueLinks(associatedAddedDefectIds, testcase, issueLinkTypeId, oldIssueLinkTypeId, issueToTestStepLink);
							//we dont know who created this link so will not be removed.
							//helper.removeIssueLinks(associatedDeletedDefectIds, testcase, oldIssueLinkTypeId);
	
						}
	
						//Check if remote link for step execution enable then update remote link.
						if(remoteIssueLinkStepExecution) {
							helper.addRemoteLinks(associatedAddedDefectIds, testcase, schedule, cycleBreadCrumb, contextPath, issueToTestStepLink, remoteIssueLinkStepExecution, issueLinkTypeId, oldIssueLinkTypeId);
							helper.removeRemoteLinks(associatedDeletedDefectIds, String.valueOf(schedule.getID()));
							helper.updateRemoteLinks(associatedAddedDefectIds, testcase, schedule, cycleBreadCrumb, contextPath, issueToTestStepLink, remoteIssueLinkStepExecution);
						}
	
					}
                    updateScheduleProperties(schedule);
					//ReIndex schedule to update the Defect keys
					Collection<Schedule> schedules = new ArrayList<Schedule>();
					schedules.add(schedule);
                    //Need Index update on the same thread.
                    EnclosedIterable<Schedule> enclosedSchedules = CollectionEnclosedIterable.copy(schedules);
                    scheduleIndexManager.reIndexSchedule(enclosedSchedules, Contexts.nullContext());
					/*commented as part of ZFJ-2908
					eventPublisher.publish(new SingleScheduleEvent(schedules, new HashMap<String,Object>(), EventType.EXECUTION_UPDATED));*/
				}
	
				stepResult.save();
				// publishing ScheduleModifyEvent for change logs
				eventPublisher.publish(new StepResultModifyEvent(stepResult, changePropertyTable, EventType.STEPRESULT_UPDATED,
						UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));
	
				List<Map<String, String>> stepExecutionDefectList = convertStepDefectToMap(associatedDefects);
				Map<String, Object> stepExecutionMap = getSerializeStepExecution(stepResult,stepExecutionDefectList);

                ScheduleSearchResourceHelper searchResourceHelper = new ScheduleSearchResourceHelper(authContext.getLoggedInUser(), searchService);
                String zqlQuery = "execution=" + schedule.getID();
                Map<String,Integer> defectCounts = searchResourceHelper.getStepDefectCountBySchedule(zqlQuery);

                stepExecutionMap.putAll(defectCounts);

                Integer totalStepResultsCount = stepResultManager.getStepResultsCount(schedule.getID());
				Map<String,String> stepResultExecutionStatusMap = getStepResultExecutionStatusCountMap(schedule,totalStepResultsCount);
                updateScheduleProperties(schedule);

                stepExecutionMap.put("totalStepResultsCount",totalStepResultsCount);
                if(MapUtils.isNotEmpty(stepResultExecutionStatusMap)) {
                    stepExecutionMap.putAll(stepResultExecutionStatusMap);
                }

				return Response.ok(stepExecutionMap).cacheControl(ZephyrCacheControl.never()).build();
			} else {
				String errorMessage = authContext.getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
				return getPermissionDeniedErrorResponse(errorMessage);
			}

		} catch(Exception e) {
			log.error("Error during updating Step Result: " + e.getMessage(), e);
			Map<String,String> errorMap = ImmutableMap.of("generic", ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("execute.test.generic.error") );
			return Response.status(Response.Status.NOT_ACCEPTABLE).type(MediaType.APPLICATION_JSON).entity(errorMap).build();
		}

	}

    private Response parseErrorJSONObject(JSONObject errorJsonObject){
        log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorJsonObject.toString()));
		return Response.status(Status.BAD_REQUEST).entity(errorJsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
	}

	@ApiOperation(value = "Get List of StepDefect", notes = "Get List of StepDefect by StepResult Id")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "{\"124\":{\"IE-2\":{\"key\":\"IE-2\",\"resolution\":\"\",\"status\":\"To Do\",\"statusId\":\"10000\",\"summary\":\"step defect linked\"}}}")})
	@GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}/defects")
    public Response getStepDefects(@PathParam("id") Integer stepResultId){
		StepResult stepResult = stepResultManager.getStepResult(stepResultId);
		if(stepResult != null){
			boolean hasViewIssuePermission = JiraUtil.hasIssueViewPermission(Long.valueOf(stepResult.getStep().getIssueId()), null, authContext.getLoggedInUser());
			if (!hasViewIssuePermission) {
				String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.issue.permission.error", "StepResult");
				log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(), Status.FORBIDDEN,errorMessage));
				return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "Insufficient Issue permission", errorMessage, errorMessage);
			}
		}

    	List<StepDefect> associatedDefects = stepResultManager.getAssociatedDefects(stepResultId);
    	JSONObject stepExecutionJSonObj = new JSONObject();
		Map<String,Map<String,String>> stepExecutionDefectMap = new HashMap<String, Map<String, String>>();
    	for(StepDefect sd : associatedDefects){
    		Issue issue = issueManager.getIssueObject(new Long(sd.getDefectId()));
    		if(issue == null){
				log.fatal("Issue not found, " + sd.getDefectId()); 
				continue;
    		}
			stepExecutionDefectMap.put(issue.getKey(), IssueUtils.convertDefectToMap(issue));
    	}
    	
    	try {
			stepExecutionJSonObj.put(stepResultId.toString(), stepExecutionDefectMap);
		} catch (JSONException e) {
			log.fatal("", e);
			return Response.status(Status.BAD_REQUEST).build();
		}
    	return Response.ok(stepExecutionJSonObj.toString()).build();
    }
    
    @ApiOperation(value = "Get List of StepDefect by Execution", notes = "Get List of StepDefect by Execution Id")
	@ApiImplicitParams({ @ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "{\"stepDefects\":{\"2\":{\"currentStepExecutionStatus\":\"2\",\"stepDefects\":[{\"key\":\"IE-2\",\"resolution\":\"\",\"status\":\"To Do\",\"statusId\":\"10000\",\"summary\":\"step defect linked\"}],\"stepResultId\":124}},\"stepDefectCount\":1,\"executionDefectCount\":0,\"totalDefectCount\":1}")})
	@GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("stepDefects")
	public Response getStepDefectsByExecution(@QueryParam("executionId")final Integer executionId, @QueryParam ("expand") final String expandos){
    	if(executionId == null || executionId <= 0) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,authContext.getI18nHelper().getText("schedule.execute.update.stepresult.invalid.id", "Execution" + String.format(ID,executionId))));
    		throw new RESTException(Response.Status.BAD_REQUEST,authContext.getI18nHelper().getText("schedule.execute.update.stepresult.invalid.id", "Execution" + String.format(ID,executionId)));
    	}  	
    	//Add Schedule validation
    	Schedule schedule = scheduleManager.getSchedule(executionId);
    	boolean hasPermission = JiraUtil.hasBrowseProjectPermission(schedule.getProjectId(),authContext.getLoggedInUser());
    	if(!hasPermission) {
       		String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.project.permission.error","Execution",String.format(ID,schedule.getProjectId()));
            log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(), Status.FORBIDDEN,errorMessage));
       		return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "Insufficient Project permissions", errorMessage, errorMessage );
		}
		boolean hasViewIssuePermission = JiraUtil.hasIssueViewPermission(Long.valueOf(schedule.getIssueId()), null, authContext.getLoggedInUser());
		if (!hasViewIssuePermission) {
			String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.issue.permission.error", "StepResult");
			log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(), Status.FORBIDDEN,errorMessage));
			return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "Insufficient Issue permission", errorMessage, errorMessage);
		}
    	Integer scheduleDefectCount = scheduleManager.getScheduleDefectCountByScheduleId(executionId);
    	
    	List<StepDefect> associatedDefects = stepResultManager.getStepResultsWithDefectBySchedule(executionId);
    	JSONObject schedulesJSonObj = new JSONObject();
		Map<String,Map<String, Object>> stepDefectsMap = new TreeMap<String,Map<String,Object>>();
		for(StepDefect sd : associatedDefects) {
			Map<String, Object> scheduleStepDefectMap = null;
            final String stepOrderId = String.valueOf(sd.getStepResult().getStep().getOrderId());
            if(stepDefectsMap.containsKey(stepOrderId)) {
				scheduleStepDefectMap = stepDefectsMap.get(stepOrderId);
			}else{
                scheduleStepDefectMap = new TreeMap<String, Object>();
                scheduleStepDefectMap.put("stepDefects", new LinkedHashSet<Map<String, String>>());
                scheduleStepDefectMap.put("currentStepExecutionStatus", sd.getStepResult().getStatus());
                scheduleStepDefectMap.put("stepResultId", sd.getStepResult().getID());
            }
    		Issue issue = issueManager.getIssueObject(new Long(sd.getDefectId()));
    		if(issue == null){
    			log.fatal("Issue not found, " + sd.getDefectId());
    			continue;
    		}
    		Map<String,String> issueMap = IssueUtils.convertDefectToMap(issue);
            ((Set<Map<String, String>>)(scheduleStepDefectMap.get("stepDefects"))).add(issueMap);
    		stepDefectsMap.put(stepOrderId, scheduleStepDefectMap);
    	}
    	try {
    		int stepDefectCount = associatedDefects != null ? associatedDefects.size() : 0;
    				
			schedulesJSonObj.put("stepDefects", stepDefectsMap);
			schedulesJSonObj.put("stepDefectCount",stepDefectCount);
			schedulesJSonObj.put("executionDefectCount",scheduleDefectCount);
			schedulesJSonObj.put("totalDefectCount",scheduleDefectCount + stepDefectCount);

    		if(StringUtils.isNotBlank(expandos) && 
    				StringUtils.containsIgnoreCase(expandos, "executionStatus")) {
    			schedulesJSonObj.put("executionStatus", populateStatusMap());
    		} 
		} catch (JSONException e) {
			log.fatal("", e);
		}
		return Response.ok(schedulesJSonObj.toString()).cacheControl(ZephyrCacheControl.never()).build();
	}

    @ApiOperation(value = "Get list of Step Result", notes = "Get List of Step Result by Execution Id")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"id\":124,\"status\":\"-1\",\"comment\":\"\",\"htmlComment\":\"\",\"executionId\":409,\"stepId\":77,\"defects\":[],\"modifiedBy\":\"vm_admin\"}")})
    @GET
    @Path("byFilter")
    public Response getStepResultsByFilterKey(@QueryParam ("executionId") final String executionId,
                                              @QueryParam ("filterKey") final String filterKey,
                                              @QueryParam ("searchKey") final String searchKey,
                                              @QueryParam ("expand") final String expandos){
        JSONObject errorJsonObject = zapiValidationService.validateExecutionId(executionId);
        if(null != errorJsonObject) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorJsonObject.toString()));
            return Response.status(Status.BAD_REQUEST).entity(errorJsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
        }

        Issue issue = null;
        Schedule schedule = scheduleManager.getSchedule(Integer.parseInt(executionId));
        Response errorMessage = checkIssueSecurityPermission(schedule);
        if (errorMessage != null) return errorMessage;

        Integer totalStepResultsCount = stepResultManager.getStepResultsCount(Integer.parseInt(executionId));
        List<ExecutionStatus> executionStatusList;
        if(StringUtils.isNotBlank(expandos) &&
                StringUtils.containsIgnoreCase(expandos, "executionStatus")) {
            executionStatusList = JiraUtil.getStepExecutionStatusList();
        }else {
            executionStatusList = Lists.newArrayList();
        }
        if(null != schedule) {
            issue = issueManager.getIssueObject(new Long(schedule.getIssueId()));
        }
        StepResultResourceHelper stepResourceHelper = new StepResultResourceHelper(issueManager);
        Integer offset = new Integer(0);
        Integer limit = new Integer(100);

        List<StepResult> filteredStepResults = Lists.newArrayList();
        Integer parsedExecutionId = Integer.parseInt(executionId);

        final AtomicReference<Issue> issueAtomicReference = new AtomicReference<>();
		issueAtomicReference.set(issue);

        ExecutorService executorService = createThreadPool(totalStepResultsCount);
        List<Future<List<StepResult>>> responseList = Lists.newArrayList();
        List<StepResultBean> stepResultsBeanCollection = new ArrayList<>();

        if(totalStepResultsCount > 0) {
            while(offset <= totalStepResultsCount){
                List<StepResult> chunkedStepResults = stepResultManager.getStepResultsByScheduleByPagination(parsedExecutionId,offset,limit);

                Future<List<StepResult>> filteredList = executorService.submit(new ProcessFilterStepResult(chunkedStepResults,filterKey,searchKey));
                responseList.add(filteredList);
                offset += limit;
            }
            executorService.shutdown();
            try {
                while (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.debug("Awaiting completion of threads.");
                }
            } catch (InterruptedException ex) {
                log.error("Interrupted exception occurred.", ex);
            }

            responseList.stream().forEach(future -> {
                try {
                    filteredStepResults.addAll(future.get());
                } catch (InterruptedException e) {
                    log.error("InterruptedException occurred while filtering test step results.");
                } catch (ExecutionException e) {
                    log.error("ExecutionException occurred while filtering test step results.");
                }
            });

            if(CollectionUtils.isNotEmpty(filteredStepResults)) {
                prepareResponseForStepResults(stepResultsBeanCollection, filteredStepResults, stepResultManager, stepResourceHelper, totalStepResultsCount, executionStatusList, issueAtomicReference);
            }
        }

        return Response.ok(stepResultsBeanCollection).cacheControl(ZephyrCacheControl.never()).build();
    }

	/**
     *
     * @param stepId
     * @param scheduleId
     * @param stepResultId
     * @param defectsKeys
     * @param associatedIssueId
     * @return
     */
	private Map<String, Object> persistRelatedDefects(Integer stepId, Integer scheduleId, Integer stepResultId, List<String> defectsKeys, Long associatedIssueId) {
		List<Integer> issueIds = new ArrayList<Integer>();
		
		if(defectsKeys != null){
			
			//Get unique Defect Set by removing the duplicate
			Set<String> uniqueDefectSet = new HashSet<String>(defectsKeys);

	    	for(String issueKey : uniqueDefectSet){
	    		MutableIssue issue = issueManager.getIssueObject(issueKey);
	    		if(issue != null && (!issue.getId().equals(associatedIssueId))) {
	    			issueIds.add(Integer.valueOf(issue.getId().intValue()));
	    		} else {
	    			log.error("Issue Key passed in does not exist or same test issue key is used for associating as defect. skipping key");
	    		}
	    	}
		}
		
		Map<String, Object> associatedDefects = stepResultManager.saveAssociatedDefects(stepId, scheduleId, stepResultId, issueIds);
    	return associatedDefects;
	}

	/**
	 * @param associatedDefects
	 * @return
	 */
	private List<Map<String, String>> convertStepDefectToMap(List<StepDefect> associatedDefects) {
		if(associatedDefects == null) {
			associatedDefects = new ArrayList<StepDefect>(0);
		}
		
		List<Map<String, String>> stepExecutionDefectList = new ArrayList<Map<String, String>>(associatedDefects.size());
		if(associatedDefects != null && associatedDefects.size() > 0){
			for(StepDefect sd : associatedDefects){
				MutableIssue issue = issueManager.getIssueObject(new Long(sd.getDefectId()));
				if(issue == null)
					continue;
				stepExecutionDefectList.add(IssueUtils.convertDefectToMap(issue));
			}
		}
		Collections.sort(stepExecutionDefectList, new Comparator<Map<String, String>>() {
			@Override
			public int compare(Map<String, String> first, Map<String, String> second) {
				return first.get("key").compareTo(second.get("key"));
			}
		});
		return stepExecutionDefectList;
	}

    /**
     * Serializes AO Schedule to JSON
     * @param stepResult
     * @param stepExecutionDefectList
     * @return
     */

	private Map<String, Object> getSerializeStepExecution(StepResult stepResult, List<Map<String, String>> stepExecutionDefectList) {
		Map<String, Object> stepExecutionMap = new HashMap<String, Object>();
		stepExecutionMap.put("id", stepResult.getID() );
		stepExecutionMap.put("executionId", stepResult.getScheduleId());
		stepExecutionMap.put("stepId", stepResult.getStep().getID());
		stepExecutionMap.put("status", stepResult.getStatus());

		if(stepResult.getStatus() != null && !"-1".equals(stepResult.getStatus())){
			ApplicationUser executor = JiraUtil.getUserManager().getUser(stepResult.getExecutedBy());
			stepExecutionMap.put("executedBy", (executor == null) ? stepResult.getExecutedBy() : executor.getDisplayName());
			
			//Temporarily disabling it (as it causes Backbone models resubmit human readable info)
			/*Long stepExecutedOnTime = stepResult.getExecutedOn();					
			if(stepExecutedOnTime != null)
				stepExecutionMap.put("executedOn", dateTimeFormatterFactory.formatter().forLoggedInUser().format(new Date(stepExecutedOnTime)));
			*/
		}
		
		stepExecutionMap.put( "comment", stepResult.getComment() != null ? stepResult.getComment() : "");
		stepExecutionMap.put( "htmlComment", stepResult.getComment() != null ? TextUtils.plainTextToHtml(stepResult.getComment(), "_blank", true) : "");
		if(stepExecutionDefectList != null && stepExecutionDefectList.size() > 0) {
			stepExecutionMap.put("defectList", stepExecutionDefectList);
		}
		
	  return stepExecutionMap;
	}
	
    /**
     * To gather the changes to be saved for audit logs. Typically, which field/property underwent change, 
     * what was the old value and what is the new value.
     * @param stepResult
     * @param stepResultBean
     * @param updateExecutionDetail 
     * @return changePropertyTable
     */        
	private Table<String, String, Object> changePropertyTable(StepResult stepResult, StepResultBean stepResultBean, boolean updateExecutionDetail){
		Table<String, String, Object> changePropertyTable = null;
    	if(stepResult == null || stepResultBean == null)
    		return null;

    	changePropertyTable = HashBasedTable.create();
    	
		String oldComment = null == stepResult.getComment() ? ApplicationConstants.NULL
				: StringUtils.isEmpty(stepResult.getComment()) ? ApplicationConstants.NULL
						: stepResult.getComment();
		String newComment = null == stepResultBean ? ApplicationConstants.NULL
				: null == stepResultBean.getComment() ? ApplicationConstants.NULL
						: StringUtils.isEmpty(stepResultBean.getComment()) ? ApplicationConstants.NULL
								: stepResultBean.getComment();
       	       	
    	if(null != stepResultBean.getComment() && !(oldComment.equalsIgnoreCase(newComment))){
    		changePropertyTable.put("COMMENT", ApplicationConstants.OLD, oldComment);
    		changePropertyTable.put("COMMENT", ApplicationConstants.NEW, newComment);
    	}
  	
       	if(null != stepResultBean.getStatus() && !(stepResult.getStatus().equalsIgnoreCase(stepResultBean.getStatus()))) {
    		changePropertyTable.put("STATUS", ApplicationConstants.OLD, StringUtils.isEmpty(stepResult.getStatus()) ? ApplicationConstants.NULL : stepResult.getStatus());
    		changePropertyTable.put("STATUS", ApplicationConstants.NEW, StringUtils.isEmpty(stepResultBean.getStatus()) ? ApplicationConstants.NULL : stepResultBean.getStatus());
    		
    		changePropertyTable.put("EXECUTED_BY", ApplicationConstants.OLD, StringUtils.isEmpty(stepResult.getExecutedBy()) ? ApplicationConstants.NULL : stepResult.getExecutedBy());
    		changePropertyTable.put("EXECUTED_BY", ApplicationConstants.NEW, UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
       		changePropertyTable.put("EXECUTED_ON", ApplicationConstants.OLD, stepResult.getExecutedOn() == null ? ApplicationConstants.NULL : String.valueOf(stepResult.getExecutedOn()));
    		changePropertyTable.put("EXECUTED_ON", ApplicationConstants.NEW, String.valueOf(System.currentTimeMillis()));
       	
       	}  
    	return changePropertyTable;
    }		
	
	
	/**
	 * Populates sorted Execution Status Map - specially important for FF or IE
	 * @return
	 */
	public JSONObject populateStatusMap() {
		JSONObject statusesJSON = new JSONObject();
		try {
			ExecutionStatus unexecuted = null;
			for(ExecutionStatus execStatus : JiraUtil.getStepExecutionStatuses().values()) {
				if(execStatus.getId().intValue() == ApplicationConstants.UNEXECUTED_STATUS){
					unexecuted = execStatus;
					continue;
				}
				statusesJSON.put(String.valueOf(execStatus.getId()), execStatus.toMap());
			}
			if(unexecuted != null)
				statusesJSON.put(String.valueOf(unexecuted.getId()), unexecuted.toMap());
		} catch(JSONException e) {
			log.warn("Error building Status Map:",e);
		}
		return statusesJSON;
	}

	/**
	 * Verify Browse and create Exec permission
	 * @param projectId
	 * @param user
	 * @return
	 */
	private boolean verifyBulkPermissions(Long projectId,ApplicationUser user) {
		//Check ZephyrPermission and update response to include execution per project permissions
		ProjectPermissionKey cyclePermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_BROWSE_CYCLE.toString());
		ProjectPermissionKey executionPermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_EDIT_EXECUTION.toString());
		Collection<ProjectPermissionKey> projectPermissionKeys = new ArrayList<ProjectPermissionKey>();
		projectPermissionKeys.add(executionPermissionKey);
		projectPermissionKeys.add(cyclePermissionKey);
		boolean loggedInUserHasZephyrPermission = zephyrPermissionManager.validateUserPermissions(projectPermissionKeys, null, user , projectId);
		return loggedInUserHasZephyrPermission;
	}

	/**
	 * @return
	 */
	private Response getPermissionDeniedErrorResponse(String errorMessage) {
		JSONObject errorJsonObject = null;
		try {
			errorJsonObject = new JSONObject();
			// build error map
			errorJsonObject.put("PERM_DENIED", errorMessage);
			Response.ResponseBuilder builder = Response.status(Response.Status.FORBIDDEN);
			builder.type(MediaType.APPLICATION_JSON);
			builder.entity(errorJsonObject.toString());
            log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(), Status.FORBIDDEN,errorJsonObject.toString()));
			return builder.build();
		} catch(JSONException e) {
			log.error("Error creating JSON Object",e);
		}
		return Response.status(Status.BAD_REQUEST).entity(errorJsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
	}

	private Response checkIssueSecurityPermission(Schedule schedule) {
		if(schedule != null) {
			boolean hasIssueViewPermission = JiraUtil.hasIssueViewPermission(Long.valueOf(schedule.getIssueId()), null, authContext.getLoggedInUser());
			if (!hasIssueViewPermission) {
				String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.project.permission.error", "StepResult", String.valueOf(schedule.getProjectId()));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(), Status.FORBIDDEN,errorMessage));
				return JiraUtil.buildErrorResponse(Status.FORBIDDEN, "Insufficient Issue permission", errorMessage, errorMessage);
			}
		}
		return null;
	}

    /**
     * This method updates the schedule property based on step result update.
     * @param schedule
     */
    private void updateScheduleProperties(Schedule schedule) {
        schedule.setModifiedBy(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
        schedule.setModifiedDate(new Date());
        schedule.save();
    }

    /**
     * Get Attachment details map for test step id.
     * @param stepId
     * @return
     */
    private List<Map<String,String>> getAttachmentsMap(Integer stepId) {
        List<Attachment> attachmentList = attachmentManager.getAttachmentsByEntityIdAndType(stepId, ApplicationConstants.TEST_STEP_TYPE);
        return convertAttachmentListDataToMap(attachmentList);
    }

    /**
     *
     * @param attachmentList
     * @return
     */
    private List<Map<String,String>> convertAttachmentListDataToMap(List<Attachment> attachmentList) {
        List<Map<String,String>> responseMap = new ArrayList<Map<String,String>>();
        if(CollectionUtils.isEmpty(attachmentList)) return responseMap;
        attachmentList.forEach(attachment -> responseMap.add(attachmentObjectToMap(attachment)));
        return responseMap;
    }


    /**
     *
     * @param attachment
     * @return
     */
    private Map<String, String> attachmentObjectToMap(Attachment attachment) {
        Map<String,String> attachmentMap = new HashMap<String,String>();
        FileIconBean.FileIcon fileIcon = fileIconUtil.getFileIcon(attachment.getFileName(), attachment.getMimetype());
        attachmentMap.put("fileId", String.valueOf(attachment.getID()));
        attachmentMap.put("fileIcon",fileIcon == null ? "file.gif" : fileIcon.getIcon());
        attachmentMap.put("fileIconAltText",fileIcon == null ? "File" : fileIcon.getAltText());
        attachmentMap.put("fileName", attachment.getFileName());
        attachmentMap.put("fileSize", String.valueOf(attachment.getFilesize()));
        attachmentMap.put("comment", attachment.getComment() != null ? attachment.getComment() : "");
        attachmentMap.put("dateCreated", dateTimeFormatterFactory.formatter().forLoggedInUser().format(attachment.getDateCreated()));
        attachmentMap.put("author", attachment.getAuthor());
        return attachmentMap;
    }

    /**
     * Get Custom fields details map for test step id.
     * @param stepId
     * @param issue
     * @return
     */
    private Map<String, CustomFieldValueResource.CustomFieldValueResponse> getCustomFieldsValue(Integer stepId, Issue issue) {
        return customFieldValueResourceDelegate.getCustomFieldValuesByEntityId(stepId, ApplicationConstants.ENTITY_TYPE.TESTSTEP.name(),issue);
    }

    /**
     *
     * @param stepResultBean
     * @param step
     * @param issue
     * @return
     */
    private StepResultBean setTeststepValueResponse(StepResultBean stepResultBean, Teststep step, Issue issue) {

        stepResultBean.setTestStepId(step.getID());
        stepResultBean.setOrderId(step.getOrderId());
        stepResultBean.setStep(step.getStep() != null ? step.getStep() : StringUtils.EMPTY);
        stepResultBean.setData(step.getData() != null ? step.getData() : StringUtils.EMPTY);
        stepResultBean.setResult(step.getResult() != null ? step.getResult() : StringUtils.EMPTY);
        stepResultBean.setHtmlStep(ZephyrWikiParser.WIKIPARSER.convertWikiToHTML(stepResultBean.getStep(), issue));
        stepResultBean.setHtmlData(ZephyrWikiParser.WIKIPARSER.convertWikiToHTML(stepResultBean.getData(), issue));
        stepResultBean.setHtmlResult(ZephyrWikiParser.WIKIPARSER.convertWikiToHTML(stepResultBean.getResult(), issue));

        stepResultBean.setAttachmentsMap(getAttachmentsMap(step.getID()));
        stepResultBean.setCustomFieldValues(getCustomFieldsValue(step.getID(), issue));

        return stepResultBean;
    }

    /**
     *
     * @param schedule
     * @param totalStepResultsCount
     * @return
     */
    private Map<String, String> getStepResultExecutionStatusCountMap(Schedule schedule, Integer totalStepResultsCount) {
        Map<Integer, ExecutionStatus> stepExecutionStatus = JiraUtil.getStepExecutionStatuses();
        Map<String,String> stepResultExecutionStatusMap = Maps.newHashMap();

        for(Map.Entry<Integer,ExecutionStatus> executionStatusEntry : stepExecutionStatus.entrySet()) {
            ExecutionStatus executionStatus = executionStatusEntry.getValue();
            Integer stepsExecutedCount = stepResultManager.getStepResultsCountByExecutionStatus(schedule.getID(), executionStatus.getId());
            if(stepsExecutedCount.equals(totalStepResultsCount)) {
                stepResultExecutionStatusMap.put("updateStatus", Boolean.TRUE.toString());
                stepResultExecutionStatusMap.put("stepExecutedStatus", executionStatus.getName());
                break;
            }
        }

        return stepResultExecutionStatusMap;
    }

    /**
     *
     * @param stepResultsBeanCollection
     * @param stepResults
     * @param stepResultManager
     * @param stepResourceHelper
     * @param totalStepResultsCount
     * @param executionStatusList
     * @param issue
     * @return
     */
    private List<StepResultBean> prepareResponseForStepResults(List<StepResultBean> stepResultsBeanCollection, List<StepResult> stepResults,
                                                               StepResultManager stepResultManager, StepResultResourceHelper stepResourceHelper, Integer totalStepResultsCount,
                                                               List<ExecutionStatus> executionStatusList, AtomicReference<Issue> issue) {
        if(CollectionUtils.isNotEmpty(stepResults)) {
            stepResults.stream().forEach(stepResult -> {
                StepResultBean stepResultBean = new StepResultBean(stepResult);
                List<StepDefect> defects = stepResultManager.getAssociatedDefects(stepResult.getID());
                List<Map<String, String>> defectsMap = stepResourceHelper.convertScheduleDefectToMap(defects, null);
                stepResultBean.setDefects(defectsMap);
                stepResultBean.setStepResultAttachmentCount(attachmentManager.getAttachmentsByEntityIdAndType(stepResult.getID(),ApplicationConstants.TESTSTEPRESULT_TYPE).size());
                stepResultBean.setStepResultsCount(totalStepResultsCount);
                stepResultBean.setExecutionStatus(executionStatusList);
                stepResultBean = setTeststepValueResponse(stepResultBean,stepResult.getStep(),issue.get());
                stepResultsBeanCollection.add(stepResultBean);
            });
        }

        return stepResultsBeanCollection;
    }

    /**
     *
     */
    private class ProcessFilterStepResult implements Callable<List<StepResult>> {

        private List<StepResult> stepResults;
        private String filterKey;
        private String searchKey;

        public ProcessFilterStepResult(List<StepResult> chunkedStepResults, String filterKey, String searchKey) {
            this.stepResults = chunkedStepResults;
            this.filterKey = filterKey;
            this.searchKey = searchKey;
        }

        @Override
        public List<StepResult> call() throws Exception {
            List<StepResult> filteredStepResults = Lists.newArrayList();
            if(StringUtils.isNotBlank(filterKey) && StringUtils.isNotBlank(searchKey)) {
                searchKey = StringUtils.lowerCase(searchKey);
                switch (filterKey) {
                    case TEST_STEP:
                        filteredStepResults = stepResults.stream().filter(stepResult ->
                                null != stepResult.getStep() && (StringUtils.isNotBlank(stepResult.getStep().getStep())
                                && StringUtils.contains(StringUtils.lowerCase(stepResult.getStep().getStep()), searchKey))
                        ).collect(Collectors.toList());
                        break;
                    case TEST_DATA:
                        filteredStepResults = stepResults.stream().filter(stepResult ->
                                null != stepResult.getStep() && (StringUtils.isNotBlank(stepResult.getStep().getData())
                                        && StringUtils.contains(StringUtils.lowerCase(stepResult.getStep().getData()), searchKey))
                        ).collect(Collectors.toList());
                        break;
                    case TEST_RESULT:
                        filteredStepResults = stepResults.stream().filter(stepResult ->
                                null != stepResult.getStep() && (StringUtils.isNotBlank(stepResult.getStep().getResult())
                                        && StringUtils.contains(StringUtils.lowerCase(stepResult.getStep().getResult()), searchKey))
                        ).collect(Collectors.toList());
                        break;

                    case STATUS:
                        filteredStepResults = stepResults.stream().filter(stepResult ->
                                null != stepResult.getStep() && (StringUtils.equalsIgnoreCase(stepResult.getStatus(), searchKey))
                        ).collect(Collectors.toList());
                        break;
                    case COMMENT:
                        filteredStepResults = stepResults.stream().filter(stepResult ->
                                null != stepResult.getComment() && (StringUtils.isNotBlank(stepResult.getComment())
                                        && StringUtils.contains(StringUtils.lowerCase(stepResult.getComment()), searchKey))
                        ).collect(Collectors.toList());
                        break;
                    case DEFAULT_SEARCH:
                        filteredStepResults = stepResults.stream().filter(stepResult ->
                                null != stepResult.getStep() &&
                                        ((StringUtils.isNotBlank(stepResult.getStep().getStep())
                                                && StringUtils.contains(StringUtils.lowerCase(stepResult.getStep().getStep()), searchKey))
                                                || (StringUtils.isNotBlank(stepResult.getStep().getData())
                                                && StringUtils.contains(StringUtils.lowerCase(stepResult.getStep().getData()), searchKey))
                                                || (StringUtils.isNotBlank(stepResult.getStep().getResult())
                                                && StringUtils.contains(StringUtils.lowerCase(stepResult.getStep().getResult()), searchKey))
                                        )
                        ).collect(Collectors.toList());
                        break;
                }
                return filteredStepResults;
            }
            return stepResults;
        }
    }

    /**
     * Create fixed thread pool executor based on total steps count.
     * @param totalStepResultsCount
     * @return
     */
    private ExecutorService createThreadPool(Integer totalStepResultsCount) {

        if (totalStepResultsCount > 100) {
            return Executors.newFixedThreadPool(2);
        }
        return  Executors.newFixedThreadPool(1);
    }
}