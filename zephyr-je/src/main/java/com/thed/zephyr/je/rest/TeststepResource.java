package com.thed.zephyr.je.rest;

import com.atlassian.beehive.compat.ClusterLock;
import com.atlassian.beehive.compat.ClusterLockService;
import com.atlassian.beehive.compat.ClusterLockServiceFactory;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.usercompatibility.UserCompatibilityHelper;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.web.util.FileIconBean;
import com.atlassian.jira.web.util.FileIconUtil;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.sun.jersey.spi.container.ResourceFilters;
import com.thed.zephyr.je.event.EventType;
import com.thed.zephyr.je.event.SingleScheduleEvent;
import com.thed.zephyr.je.event.TeststepModifyEvent;
import com.thed.zephyr.je.helper.TestStepResourceHelper;
import com.thed.zephyr.je.model.Attachment;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.model.Teststep;
import com.thed.zephyr.je.rest.delegate.CustomFieldResourceDelegate;
import com.thed.zephyr.je.rest.delegate.CustomFieldValueResourceDelegate;
import com.thed.zephyr.je.rest.exception.RESTException;
import com.thed.zephyr.je.rest.filter.ZFJApiFilter;
import com.thed.zephyr.je.service.*;
import com.thed.zephyr.je.vo.TeststepBean;
import com.thed.zephyr.je.vo.TeststepBeanWrapper;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.UniqueIdGenerator;
import com.thed.zephyr.util.ZephyrCacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

@Api(value = "TestStep Resource API(s)", description = "Following section describes the rest resources pertaining to TestStepResource")
@Path("teststep/{issueId}")
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@AnonymousAllowed
@ResourceFilters(ZFJApiFilter.class)
public class TeststepResource {
	private static final String ERROR_LOG_MESSAGE = "[Error] [Error code: %s %s Error Message : %s]";

    private final TeststepManager testStepManager;
	private final JiraAuthenticationContext authContext;
	private final EventPublisher eventPublisher;
	private final IssueManager issueManager;
	private final ScheduleManager scheduleManager;
	private final ZAPIValidationService zapiValidationService;
    private ClusterLockService clusterLockService;
    private final JobProgressService jobProgressService;
    private final AttachmentManager attachmentManager;
    private final FileIconUtil fileIconUtil;
    private final DateTimeFormatterFactory dateTimeFormatterFactory;
	private final CustomFieldValueResourceDelegate customFieldValueResourceDelegate;
	private final CustomFieldResourceDelegate customFieldResourceDelegate;

    protected final Logger log = Logger.getLogger(TeststepResource.class);

	public TeststepResource(JiraAuthenticationContext authContext, TeststepManager testStepManager,
			final EventPublisher eventPublisher,final IssueManager issueManager,final ZAPIValidationService zapiValidationService, ClusterLockServiceFactory clusterLockServiceFactory,
            final ScheduleManager scheduleManager, JobProgressService jobProgressService, AttachmentManager attachmentManager,
                            final FileIconUtil fileIconUtil,final DateTimeFormatterFactory dateTimeFormatterFactory,
                            final CustomFieldValueResourceDelegate customFieldValueResourceDelegate,
                            final CustomFieldResourceDelegate customFieldResourceDelegate) {
		super();
		this.authContext = authContext;
		this.testStepManager = testStepManager;
		this.eventPublisher = eventPublisher;
		this.issueManager=issueManager;
		this.zapiValidationService=zapiValidationService;
		this.scheduleManager=scheduleManager;
        this.clusterLockService =   clusterLockServiceFactory.getClusterLockService();
        this.jobProgressService = jobProgressService;
        this.attachmentManager = attachmentManager;
        this.fileIconUtil = fileIconUtil;
        this.dateTimeFormatterFactory = dateTimeFormatterFactory;
        this.customFieldValueResourceDelegate = customFieldValueResourceDelegate;
        this.customFieldResourceDelegate = customFieldResourceDelegate;
	}

	@ApiOperation(value = "Get List of TestSteps", notes = "Get List of TestSteps by Issue Id")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "[{\"id\":2097,\"orderId\":1,\"step\":\"Check for schedule count\",\"data\":\"filter id\",\"result\":\"count should be equal to schedules returned by this filter.\",\"createdBy\":\"vm_admin\",\"modifiedBy\":\"vm_admin\",\"htmlStep\":\"<p>Check for schedule count<\\/p>\",\"htmlData\":\"<p>filter id<\\/p>\",\"htmlResult\":\"<p>count should be equal to schedules returned by this filter.<\\/p>\"},{\"id\":2098,\"orderId\":2,\"step\":\"*strong*\",\"data\":\"filter id\",\"result\":\"count should be equal to schedules returned by this filter.\",\"createdBy\":\"vm_admin\",\"modifiedBy\":\"vm_admin\",\"htmlStep\":\"<p><b>strong<\\/b><\\/p>\",\"htmlData\":\"<p>filter id<\\/p>\",\"htmlResult\":\"<p>count should be equal to schedules returned by this filter.<\\/p>\"}]")})
	@GET
    @AnonymousAllowed
	public Response getTeststeps(@PathParam ("issueId") final Long issueId, @QueryParam("offset") Integer offset, @QueryParam("limit") Integer limit){
		Issue issue = ComponentAccessor.getIssueManager().getIssueObject(issueId);
		Map<String, String> errorMap = zapiValidationService.validateIssueById(String.valueOf(issueId));
		if(errorMap.size() > 0) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorMap));
			return Response.status(Status.BAD_REQUEST).entity(errorMap).build();
		}
		boolean hasIssueViewPermission = JiraUtil.hasIssueViewPermission(null,issue,authContext.getLoggedInUser());
		if (!hasIssueViewPermission) {
			String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.project.permission.error", "TestStep", String.valueOf(issue.getProjectId()));
            log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(), Status.FORBIDDEN,errorMessage));
			return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "Insufficient Issue permission", errorMessage, errorMessage);
		}

		Integer totalStepCounts = testStepManager.getTotalStepCount(issueId);

		Optional<Integer> offsetValue = offset != null ? Optional.of(offset) : Optional.empty();
		Optional<Integer> limitValue = limit != null ? Optional.of(limit) : Optional.empty();

		List<Teststep> steps = testStepManager.getTeststeps(issueId, offsetValue, limitValue);
		List<TeststepBean> stepBeanCollection = new ArrayList<>();
		for(Teststep step : steps){
			TeststepBean teststepBean = new TeststepBean(step, issue,getAttachmentsMap(step.getID()), getCustomFieldsValue(step.getID(), issue));
			teststepBean.setTotalStepCount(totalStepCounts);
			stepBeanCollection.add(teststepBean);
		}

        TeststepBeanWrapper teststepBeanWrapper = new TeststepBeanWrapper();
		teststepBeanWrapper.setStepBeanCollection(stepBeanCollection);

		updateTeststepResponseForPreviousNextTeststep(issueId, issue, teststepBeanWrapper, offsetValue, limitValue);
		return Response.ok(teststepBeanWrapper).cacheControl(ZephyrCacheControl.never()).build();
	}

    @ApiOperation(value = "Get TestStep Information", notes = "Get TestStep Information by TestStep Id, IssueId")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "{\"id\":76,\"orderId\":1,\"step\":\"step1\",\"data\":\"\",\"result\":\"\",\"createdBy\":\"vm_admin\",\"modifiedBy\":\"vm_admin\",\"htmlStep\":\"<p>step1<\\/p>\",\"htmlData\":\"\",\"htmlResult\":\"\"}")})
	@GET
	@Path ("{id}")
    @AnonymousAllowed
	public TeststepBean getTeststep(@PathParam ("issueId") final Long issueId, @PathParam ("id") final Integer id) {
		Map<String, String> errorMap = zapiValidationService.validateIssueById(String.valueOf(issueId));
		if(errorMap.size() > 0) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorMap));
			throw new RESTException(Status.BAD_REQUEST, errorMap);
		}
		Teststep step = testStepManager.getTeststep(id);
		try {
			JSONObject errorJsonObject = zapiValidationService.validateIssueAndStepId(issueId,step,id);
			if(errorJsonObject.length() > 0) {
                log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorJsonObject.toString()));
				throw new RESTException(Status.BAD_REQUEST, errorJsonObject);
			}
		} catch (JSONException e) {
			log.warn("Error constructing JSON:",e);
			throw new RESTException(Status.BAD_REQUEST);
		}
		Issue issue = issueManager.getIssueObject(issueId);
		boolean hasIssueViewPermission = JiraUtil.hasIssueViewPermission(null,issue,authContext.getLoggedInUser());
		checkIssueSecurityPermission(issue, hasIssueViewPermission);

		return new TeststepBean(step, issue,getAttachmentsMap(step.getID()), getCustomFieldsValue(step.getID(), issue));
	}

	@ApiOperation(value = "Update TestStep Data", notes = "Update TestStep Information by TestStep Id, Issue Id")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "request", value = "{\"step\":\"Add a ZQL stmt.\",\"data\":\"required data for zql stmt\"}"),
			@ApiImplicitParam(name = "response", value = "{\"id\":26,\"orderId\":6,\"step\":\"Add a ZQL stmt.\",\"data\":\"required data for zql stmt\",\"createdBy\":\"vm_admin\",\"modifiedBy\":\"vm_admin\",\"htmlStep\":\"<p>Add a ZQL stmt.<\\/p>\",\"htmlData\":\"<p>required data for zql stmt<\\/p>\",\"htmlResult\":\"\"}")})
	@PUT
	@Path ("{id}")
	public Response updateTeststep(@PathParam ("issueId") final Long issueId,
			@PathParam ("id") final Integer id, final TeststepBean stepBean) {
		//Validate issue
		Map<String, String> errorMap = zapiValidationService.validateIssueById(String.valueOf(issueId));
		if(errorMap.size() > 0) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorMap));
			throw new RESTException(Status.BAD_REQUEST, errorMap);
		}

		//Validate Test Step
		JSONObject jsonObject = zapiValidationService.validateTestStepId(id,stepBean);
		if(jsonObject.length() > 0) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,jsonObject.toString()));
			throw new RESTException(Status.BAD_REQUEST, jsonObject);
		}
		Teststep step = testStepManager.getTeststep(id);

		try {
			//Validate IssueId passed in and Step IssueId
			JSONObject errorJsonObject = zapiValidationService.validateIssueAndStepId(issueId,step,id);
			if(errorJsonObject.length() > 0) {
                log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorJsonObject.toString()));
				throw new RESTException(Status.BAD_REQUEST, errorJsonObject);
			}
		} catch (JSONException e) {
			log.warn("Error constructing JSON:",e);
			return Response.status(Status.BAD_REQUEST).build();
		}

		TestStepResourceHelper testStepResourceHelper = new TestStepResourceHelper(testStepManager,authContext,customFieldValueResourceDelegate);
		Issue issue = issueManager.getIssueObject(issueId);
		JSONObject errorObject = testStepResourceHelper.verifyPermissions(issue, ProjectPermissions.EDIT_ISSUES);
		if(errorObject != null) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorObject.toString()));
			throw new RESTException(Status.BAD_REQUEST, errorObject);
		}

		boolean hasIssueViewPermission = JiraUtil.hasIssueViewPermission(null,issue,authContext.getLoggedInUser());
		checkIssueSecurityPermission(issue, hasIssueViewPermission);

		//fetch the modified properties for change logs
		Table<String, String, Object> changePropertyTable = TeststepUtils.changePropertyTable(step, stepBean);
		if(stepBean.step != null) step.setStep(stepBean.step);
		if(stepBean.data != null) step.setData(stepBean.data);
		if(stepBean.result != null) step.setResult(stepBean.result);
		step.setModifiedBy(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));

		step.save();

        if(MapUtils.isNotEmpty(stepBean.getCustomFieldValues())) {
            Response res = testStepResourceHelper.validateCustomFieldValueRequest(stepBean.getCustomFieldValues(),customFieldResourceDelegate,issue,step.getID());
            if(res != null) {
                return res;
            }else {
                customFieldValueResourceDelegate.updateCustomFieldValues(stepBean.getCustomFieldValues(), ApplicationConstants.ENTITY_TYPE.TESTSTEP.name(), step.getID());
            }
        }

		TeststepBean teststepBean = new TeststepBean(step, issue, getAttachmentsMap(step.getID()), getCustomFieldsValue(step.getID(),issue));
		Integer totalStepCounts = testStepManager.getTotalStepCount(issueId);
		teststepBean.setTotalStepCount(totalStepCounts);

		// publishing TeststepModifyEvent
		eventPublisher.publish(new TeststepModifyEvent(step, changePropertyTable, EventType.TESTSTEP_UPDATED,
				UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));

		return Response.ok(teststepBean).cacheControl(ZephyrCacheControl.never()).build();
	}

	@ApiOperation(value = "Create New TestStep", notes = "Create New TestStep by Issue Id")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{\"step\":\"Check for schedule count\",\"data\":\"filter id\",\"result\":\"count should be equal to schedules returned by this filter.\"}"),
	@ApiImplicitParam(name = "response", value = "{\"id\":2097,\"orderId\":1,\"step\":\"Check for schedule count\",\"data\":\"filter id\",\"result\":\"count should be equal to schedules returned by this filter.\",\"createdBy\":\"vm_admin\",\"modifiedBy\":\"vm_admin\",\"htmlStep\":\"<p>Check for schedule count<\\/p>\",\"htmlData\":\"<p>filter id<\\/p>\",\"htmlResult\":\"<p>count should be equal to schedules returned by this filter.<\\/p>\"}")})
	@POST
	public Response createTeststep(@PathParam ("issueId") final Long issueId, final TeststepBean stepBean) {
		Map<String,String> errorMap = zapiValidationService.validateIssueByIdAndType(String.valueOf(issueId));
		if(errorMap.size() > 0) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorMap));
			return Response.status(Status.BAD_REQUEST).entity(errorMap).build();
		}
		TestStepResourceHelper testStepResourceHelper = new TestStepResourceHelper(testStepManager,authContext,customFieldValueResourceDelegate);
		Issue issue = issueManager.getIssueObject(issueId);
		JSONObject errorObject = testStepResourceHelper.verifyPermissions(issue, ProjectPermissions.CREATE_ISSUES);
		if(errorObject != null) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorObject.toString()));
			throw new RESTException(Status.BAD_REQUEST, errorObject);
		}
		boolean hasIssueViewPermission = JiraUtil.hasIssueViewPermission(null,issue,authContext.getLoggedInUser());
		checkIssueSecurityPermission(issue, hasIssueViewPermission);

		String lockName = "zephyr-tc-" + issueId.toString();//StringUtils.substring(issueId.toString(), -2, 0);
        ClusterLock lock = clusterLockService.getLockForName(lockName);
		Response res = null;
		try {
            lock.lock();
			Teststep step = testStepManager.createTeststep(stepBean, issueId);
			TeststepBean teststepBean = new TeststepBean(step, issue);
			if(MapUtils.isNotEmpty(stepBean.getCustomFieldValues())) {
				res = testStepResourceHelper.validateCustomFieldValueRequest(stepBean.getCustomFieldValues(),customFieldResourceDelegate,issue,step.getID());
				if(res != null) {
				    return res;
                }else {
                    customFieldValueResourceDelegate.createCustomFieldValues(stepBean.getCustomFieldValues(), ApplicationConstants.ENTITY_TYPE.TESTSTEP.name(), step.getID());
                    teststepBean.setCustomFields(getCustomFieldsValue(step.getID(), issue));
                }
			}
			Integer totalStepCounts = testStepManager.getTotalStepCount(issueId);
			teststepBean.setTotalStepCount(totalStepCounts);
			res = Response.ok(teststepBean).cacheControl(ZephyrCacheControl.never()).build();
		} catch (Exception e) {
			log.fatal("", e);
			res = Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally{
            lock.unlock();
		}
		return res;
	}

	@ApiOperation(value = "Move TestStep to Issue")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "[{\"id\":57,\"orderId\":1,\"step\":\"Add a ZQL stmt.\",\"data\":\"required data for zql stmt\",\"result\":\"ilk,j\",\"htmlStep\":\"Add a ZQL stmt.\",\"htmlData\":\"required data for zql stmt\",\"htmlResult\":\"ilk,j\"}]")})
	@POST
	@Path("{id}/move")
	public Response moveTeststep(@PathParam ("issueId") final Long issueId, @PathParam ("id") final int id, Map<String, Object> params, @QueryParam("offset") Integer offset, @QueryParam("limit") Integer limit) {
		Map<String,String> errorMap = zapiValidationService.validateIssueByIdAndType(String.valueOf(issueId));
		if(errorMap.size() > 0) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorMap));
			return Response.status(Status.BAD_REQUEST).entity(errorMap).build();
		}
		
		TestStepResourceHelper testStepResourceHelper = new TestStepResourceHelper(testStepManager,authContext,customFieldValueResourceDelegate);
		Issue issue = issueManager.getIssueObject(issueId);
		JSONObject errorObject = testStepResourceHelper.verifyPermissions(issue, ProjectPermissions.EDIT_ISSUES);
		if(errorObject != null) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorObject.toString()));
			throw new RESTException(Status.BAD_REQUEST, errorObject);
		}
		boolean hasIssueViewPermission = JiraUtil.hasIssueViewPermission(null,issue,authContext.getLoggedInUser());
		checkIssueSecurityPermission(issue, hasIssueViewPermission);

		List<Teststep> steps = testStepManager.getTeststeps(issueId, Optional.empty(), Optional.empty());
		
		int stepAheadId = -1;
		int newPosition = -1;
		int currentPosition = -1;
		Teststep stepBeingMoved = null;
		Teststep stepBelowStepBeingMoved = null;
		
		Object afterElementUrl = params.get("after");
		if(afterElementUrl != null){
			stepAheadId = Integer.parseInt(StringUtils.substringAfterLast(afterElementUrl.toString(), "/"));
		}else{
			newPosition = steps.size() - 1;
		}
		/*Determine the order of current step and destination step*/
		for(int i=0 ; i< steps.size(); i++){
			Teststep step = steps.get(i);

			if(step.getID() == id) {
				step.setModifiedBy(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
				stepBeingMoved = step;
				currentPosition = i;
				if(newPosition != -1) break;
			}
			
			if(stepAheadId != -1 && step.getID() == stepAheadId ){
				step.setModifiedBy(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
				stepBelowStepBeingMoved = step;
				newPosition = i;
				if(currentPosition != -1) break;
			}
		}
		/*Adjust the new position - if item is dropped from higher order to lower order*/
		if(stepBelowStepBeingMoved != null && stepBeingMoved != null){
			if(stepBeingMoved.getOrderId() < stepBelowStepBeingMoved.getOrderId()){
				newPosition -= 1;
			}
		}
		/*Swap with row below till we reach target*/
		if(newPosition > currentPosition){
			for(int i=currentPosition ; i < newPosition ; i++){
				Collections.swap(steps, i, i+1);
			}
		}
		/*Swap with row above till we reach target*/
		else{
			for(int i=currentPosition ; i > newPosition ; i--){
				Collections.swap(steps, i, i-1);
			}
		}
		/*generate new OrderIds*/
		testStepManager.updateOrderId(steps);

        Integer totalStepCounts = testStepManager.getTotalStepCount(issueId);

        Optional<Integer> offsetValue = offset != null ? Optional.of(offset) : Optional.empty();
        Optional<Integer> limitValue = limit != null ? Optional.of(limit) : Optional.empty();

        steps = testStepManager.getTeststeps(issueId, offsetValue, limitValue);
        List<TeststepBean> stepBeanCollection = new ArrayList<>();
        for(Teststep step : steps){
            TeststepBean teststepBean = new TeststepBean(step, issue,getAttachmentsMap(step.getID()), getCustomFieldsValue(step.getID(), issue));
            teststepBean.setTotalStepCount(totalStepCounts);
            stepBeanCollection.add(teststepBean);
        }

        TeststepBeanWrapper teststepBeanWrapper = new TeststepBeanWrapper();
        teststepBeanWrapper.setStepBeanCollection(stepBeanCollection);

        updateTeststepResponseForPreviousNextTeststep(issueId, issue, teststepBeanWrapper, offsetValue, limitValue);

        return Response.ok(teststepBeanWrapper).cacheControl(ZephyrCacheControl.never()).build();
	}

	@ApiOperation(value = "Delete TestStep", notes = "Delete TestStep by TestStep Id, Issue Id")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "[{\"id\":2101,\"orderId\":1,\"step\":\"CLONE - Check for schedule count\",\"data\":\"filter id\",\"result\":\"count should be equal to schedules returned by this filter.\",\"createdBy\":\"vm_admin\",\"modifiedBy\":\"vm_admin\",\"htmlStep\":\"<p>CLONE - Check for schedule count<\\/p>\",\"htmlData\":\"<p>filter id<\\/p>\",\"htmlResult\":\"<p>count should be equal to schedules returned by this filter.<\\/p>\"},{\"id\":2098,\"orderId\":2,\"step\":\"*strong*\",\"data\":\"filter id\",\"result\":\"count should be equal to schedules returned by this filter.\",\"createdBy\":\"vm_admin\",\"modifiedBy\":\"vm_admin\",\"htmlStep\":\"<p><b>strong<\\/b><\\/p>\",\"htmlData\":\"<p>filter id<\\/p>\",\"htmlResult\":\"<p>count should be equal to schedules returned by this filter.<\\/p>\"},{\"id\":2102,\"orderId\":3,\"htmlStep\":\"\",\"htmlData\":\"\",\"htmlResult\":\"\"}]")})
	@DELETE
	@Path ("{id}")
	public Response delete(@PathParam ("issueId") final Long issueId,@PathParam ("id") final Integer id, @QueryParam("offset") Integer offset, @QueryParam("limit") Integer limit) {
		Map<String, String> errorMap = zapiValidationService.validateIssueById(String.valueOf(issueId));
		if(errorMap.size() > 0) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorMap));
			return Response.status(Status.BAD_REQUEST).entity(errorMap).build();
		}
		
		TestStepResourceHelper testStepResourceHelper = new TestStepResourceHelper(testStepManager,authContext,customFieldValueResourceDelegate);
		Issue issue = issueManager.getIssueObject(issueId);
		JSONObject errorObject = testStepResourceHelper.verifyPermissions(issue, ProjectPermissions.EDIT_ISSUES);
		if(errorObject != null) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorObject.toString()));
			throw new RESTException(Status.BAD_REQUEST, errorObject);
		}
		boolean hasIssueViewPermission = JiraUtil.hasIssueViewPermission(null,issue,authContext.getLoggedInUser());
		checkIssueSecurityPermission(issue, hasIssueViewPermission);

    	//Fetch the Teststep to be removed, for change log info
    	Teststep stepToBeRemoved = testStepManager.getTeststep(id);
		try {
			JSONObject errorJsonObject = zapiValidationService.validateIssueAndStepId(issueId,stepToBeRemoved,id);
			if(errorJsonObject.length() > 0) {
                log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorJsonObject.toString()));
				return Response.status(Status.BAD_REQUEST).entity(errorJsonObject.toString()).type(MediaType.APPLICATION_JSON).cacheControl(ZephyrCacheControl.never()).build();
			}
		} catch (JSONException e) {
			log.warn("Error constructing JSON:",e);
			return Response.status(Status.BAD_REQUEST).build();
		}
    	deleteAttachmentForTeststep(stepToBeRemoved);
        testStepManager.removeTeststep(id);
		// publishing TeststepModifyEvent
	   	eventPublisher.publish(new TeststepModifyEvent(Lists.newArrayList(stepToBeRemoved), null, EventType.TESTSTEP_DELETED,
	   			UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));
	   	
    	//ReIndex schedule to delete the Linked Defect keys from all the schedules that has the step
	   	List<Schedule> schedules = scheduleManager.getSchedulesByIssueId(issueId.intValue(), null, null);
		eventPublisher.publish(new SingleScheduleEvent(schedules, new HashMap<String,Object>(), EventType.TESTSTEP_DELETED));

        Integer totalStepCounts = testStepManager.getTotalStepCount(issueId);

        Optional<Integer> offsetValue = offset != null ? Optional.of(offset) : Optional.empty();
        Optional<Integer> limitValue = limit != null ? Optional.of(limit) : Optional.empty();

        List<Teststep> steps = testStepManager.getTeststeps(issueId, offsetValue, limitValue);
        List<TeststepBean> stepBeanCollection = new ArrayList<>();
        for(Teststep step : steps){
            TeststepBean teststepBean = new TeststepBean(step, issue,getAttachmentsMap(step.getID()), getCustomFieldsValue(step.getID(), issue));
            teststepBean.setTotalStepCount(totalStepCounts);
            stepBeanCollection.add(teststepBean);
        }

        TeststepBeanWrapper teststepBeanWrapper = new TeststepBeanWrapper();
        teststepBeanWrapper.setStepBeanCollection(stepBeanCollection);

        updateTeststepResponseForPreviousNextTeststep(issueId, issue, teststepBeanWrapper, offsetValue, limitValue);

        return Response.ok(teststepBeanWrapper).cacheControl(ZephyrCacheControl.never()).build();
	}

    @ApiOperation(value = "Clone TestStep", notes = "Clone TestStep by from TestStep Id, Issue Id")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{\"position\":\"-1\"}"),
	@ApiImplicitParam(name = "response", value = "[{\"id\":76,\"orderId\":1,\"step\":\"Add a ZQL stmt.\",\"data\":\"required data for zql stmt\",\"result\":\"\",\"createdBy\":\"vm_admin\",\"modifiedBy\":\"vm_admin\",\"htmlStep\":\"<p>Add a ZQL stmt.<\\/p>\",\"htmlData\":\"<p>required data for zql stmt<\\/p>\",\"htmlResult\":\"\"},{\"id\":78,\"orderId\":2,\"step\":\"CLONE - Add a ZQL stmt.\",\"data\":\"required data for zql stmt\",\"result\":\"\",\"createdBy\":\"vm_admin\",\"modifiedBy\":\"vm_admin\",\"htmlStep\":\"<p>CLONE - Add a ZQL stmt.<\\/p>\",\"htmlData\":\"<p>required data for zql stmt<\\/p>\",\"htmlResult\":\"\"},{\"id\":77,\"orderId\":3,\"step\":\"h1.xzz\",\"createdBy\":\"vm_admin\",\"modifiedBy\":\"vm_admin\",\"htmlStep\":\"<h1><a name=\\\"xzz\\\"><\\/a>xzz<\\/h1>\",\"htmlData\":\"\",\"htmlResult\":\"\"}]")})
	@POST
	@Path("clone/{fromStepId}")
    @AnonymousAllowed
	public Response cloneTeststep(@PathParam ("fromStepId") final Long fromStepId,@PathParam ("issueId") final Long issueId,
			Map<String,String> params, @QueryParam("offset") Integer offset, @QueryParam("limit") Integer limit) {
		Map<String,String> errorMap = zapiValidationService.validateIssueByIdAndType(String.valueOf(issueId));
		if(errorMap.size() > 0) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorMap));
			throw new RESTException(Status.BAD_REQUEST, errorMap);
		}
		
		TestStepResourceHelper testStepResourceHelper = new TestStepResourceHelper(testStepManager,authContext,customFieldValueResourceDelegate);
		Issue issue = issueManager.getIssueObject(issueId);
		JSONObject errorJsonObject = testStepResourceHelper.verifyPermissions(issue,ProjectPermissions.CREATE_ISSUES);
		if(errorJsonObject != null) {
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorJsonObject.toString()));
			throw new RESTException(Status.BAD_REQUEST, errorJsonObject);
		}
		boolean hasIssueViewPermission = JiraUtil.hasIssueViewPermission(null,issue,authContext.getLoggedInUser());
		checkIssueSecurityPermission(issue, hasIssueViewPermission);
		
		int position=2;
		String step = null;
		if(params != null || params.size() >= 0) {
			position = params.containsKey("position") ? Integer.valueOf(params.get("position")).intValue() : position;
			step = params.containsKey("step") ? params.get("step") : null;
		}
		
		//If null StepID
		if(fromStepId == null || fromStepId == 0) {
			errorMap = new HashMap<String, String>();
			errorMap.put("errorMessages", authContext.getI18nHelper().getText("schedule.update.ID.required", "fromStepId"));
			errorMap.put("errors", new String(""));
            log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST,errorMap));
			throw new RESTException(Status.BAD_REQUEST, errorMap);
		}

        testStepResourceHelper.cloneTestStep(fromStepId,issueId, step, position);

        Integer totalStepCounts = testStepManager.getTotalStepCount(issueId);

        Optional<Integer> offsetValue = offset != null ? Optional.of(offset) : Optional.empty();
        Optional<Integer> limitValue = limit != null ? Optional.of(limit) : Optional.empty();

        List<Teststep> steps = testStepManager.getTeststeps(issueId, offsetValue, limitValue);
        List<TeststepBean> stepBeanCollection = new ArrayList<>();
        for(Teststep teststep : steps){
            TeststepBean teststepBean = new TeststepBean(teststep, issue,getAttachmentsMap(teststep.getID()), getCustomFieldsValue(teststep.getID(), issue));
            teststepBean.setTotalStepCount(totalStepCounts);
            stepBeanCollection.add(teststepBean);
        }

        TeststepBeanWrapper teststepBeanWrapper = new TeststepBeanWrapper();
        teststepBeanWrapper.setStepBeanCollection(stepBeanCollection);

        updateTeststepResponseForPreviousNextTeststep(issueId, issue, teststepBeanWrapper, offsetValue, limitValue);
        return Response.ok(teststepBeanWrapper).cacheControl(ZephyrCacheControl.never()).build();
	}

    private void checkIssueSecurityPermission(Issue issue, boolean hasIssueViewPermission) {
		if (!hasIssueViewPermission) {
			String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.project.permission.error", "TestStep", String.valueOf(issue.getProjectId()));
			JSONObject errorJsonObject = new JSONObject();
			try {
				errorJsonObject.put("errorMessages", errorMessage);
				errorJsonObject.put("errors", "Insufficient Issue Permission");
			} catch (JSONException e) {
				e.printStackTrace();
			}
            log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(), Status.FORBIDDEN,errorMessage));
			throw new RESTException(Status.FORBIDDEN,errorJsonObject);
		}
	}

	/**
	 * Teststep utility methods for reuse
	 */
	public static class TeststepUtils{

		/**
         * To gather the changes to be saved for audit logs. Typically, which field/property underwent change,
         * what was the old value and what is the new value.
         * @param testStep
         * @param testStepBean
         * @return changePropertyTable
         *
         */
        public static Table<String, String, Object> changePropertyTable(Teststep testStep, TeststepBean testStepBean){
            Table<String, String, Object> changePropertyTable = null;
            if(testStep == null || testStepBean == null)
                return null;

            changePropertyTable = HashBasedTable.create();
            if(checkForPropsModification(testStep.getStep(), testStepBean.step)){
                changePropertyTable.put("STEP", ApplicationConstants.OLD, StringUtils.isEmpty(testStep.getStep()) ? ApplicationConstants.NULL : testStep.getStep());
                changePropertyTable.put("STEP", ApplicationConstants.NEW, StringUtils.isEmpty(testStepBean.step) ? ApplicationConstants.NULL : testStepBean.step);
            }
            if(checkForPropsModification(testStep.getData(), testStepBean.data)){
                changePropertyTable.put("DATA", ApplicationConstants.OLD, StringUtils.isEmpty(testStep.getData()) ? ApplicationConstants.NULL : testStep.getData());
                changePropertyTable.put("DATA", ApplicationConstants.NEW, StringUtils.isEmpty(testStepBean.data) ? ApplicationConstants.NULL : testStepBean.data);
            }
               if(checkForPropsModification(testStep.getResult(), testStepBean.result)){
                changePropertyTable.put("RESULT", ApplicationConstants.OLD, StringUtils.isEmpty(testStep.getResult()) ? ApplicationConstants.NULL : testStep.getResult());
                changePropertyTable.put("RESULT", ApplicationConstants.NEW, StringUtils.isEmpty(testStepBean.result) ? ApplicationConstants.NULL : testStepBean.result);
            }
            return changePropertyTable;
        }

		private static Boolean checkForPropsModification(String oldVal, String newVal){
            if(null == newVal || (StringUtils.isBlank(oldVal) && StringUtils.isBlank(newVal)))
                return false;
            Boolean isPropModifiedCheck = (StringUtils.isEmpty(oldVal) && StringUtils
                    .isNotEmpty(newVal))
                    || (StringUtils.isEmpty(newVal) && StringUtils
                            .isNotEmpty(oldVal))
                    || !(oldVal.equalsIgnoreCase(newVal));
            return isPropModifiedCheck;
        }
	}
	
	@ApiOperation(value = "Copy Test steps from source to destination issues", notes = "Copy Test steps from source to destination issues \nThis API returns a jobProgressToken which should be used for making the call to /rest/zapi/latest/execution/jobProgress/:jobProgressToken?type=copy_test_step_job_progress. Once the request is processed, the jobProgress will populate the message field with result.")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "request", value = "{\"destinationIssues\":\"1000,1001\", \"isJql\":\"false\"}"),
			@ApiImplicitParam(name = "response", value = "{\"jobProgressToken\":\"0001499757895101-d605d530ffff9386-0001\"}")
	})
	@POST
	@Path("copyteststeps")
    @AnonymousAllowed
	public Response copyteststeps(@PathParam ("issueId") final String sourceIssueKey, Map<String,String> params) {
		log.debug("copy test steps params : " + params.toString() + " sourceIssueKey : " + sourceIssueKey);
		Issue sourceIssue = issueManager.getIssueByKeyIgnoreCase(sourceIssueKey);
		String destinationIssueKeys = params.get("destinationIssues");
		String isJql = params.get("isJql");
		if(sourceIssue == null || params == null || destinationIssueKeys == null || isJql == null) {
            log.error(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST, sourceIssueKey));
			throw new RESTException(Status.BAD_REQUEST, sourceIssueKey);
		}
		String copyCustomFields = params.getOrDefault("copyCustomField", Boolean.FALSE.toString());
		JSONObject jsonObject = new JSONObject();
		//Checking whether user is allowed to do the copy test steps.
		TestStepResourceHelper testStepResourceHelper = new TestStepResourceHelper(testStepManager,authContext,customFieldValueResourceDelegate);
		JSONObject errorJsonObject = testStepResourceHelper.verifyPermissions(sourceIssue, ProjectPermissions.CREATE_ISSUES);
		if(errorJsonObject != null) {
            log.error(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST, errorJsonObject.toString()));
			throw new RESTException(Status.BAD_REQUEST, errorJsonObject);
		}
		ApplicationUser loggedInUser = authContext.getLoggedInUser();
		boolean hasIssueViewPermission = JiraUtil.hasIssueViewPermission(null, sourceIssue, loggedInUser);
		checkIssueSecurityPermission(sourceIssue, hasIssueViewPermission);
		//Progress bar logic
		String jobProgressToken = new UniqueIdGenerator().getStringId();
	    jobProgressService.createJobProgress(ApplicationConstants.COPY_TESTSTEPS_FROM_SOURCE_TO_DESTINATION, 0, jobProgressToken);
	    Map<String, String> result = new HashMap<>();
	    result.put("copiedIssues", 0+"");
        Boolean copyCustomFieldFlag = Boolean.valueOf(copyCustomFields);
		try {
			Runnable runnable = () -> {
				testStepResourceHelper.copyTestStepsFromSrcToDst(jobProgressService, isJql, copyCustomFieldFlag, sourceIssue, destinationIssueKeys, jobProgressToken, result
						, loggedInUser);
			};
			Executors.newSingleThreadExecutor().submit(runnable);		
			jsonObject.put(ApplicationConstants.JOB_PROGRESS_TOKEN, jobProgressToken);
		} catch(Exception e) {
			log.error("Error while copying Test Steps",e);
			jobProgressService.addCompletedSteps(jobProgressToken, ApplicationConstants.JOB_STATUS_FAILED);
			return Response.status(Status.INTERNAL_SERVER_ERROR).cacheControl(ZephyrCacheControl.never()).build();
		}
		return Response.ok().entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
	}

    /**
     *
     * @param teststep
     */
    private void deleteAttachmentForTeststep(Teststep teststep) {
        List<Attachment> attachmentList = attachmentManager.getAttachmentsByEntityIdAndType(teststep.getID(), ApplicationConstants.TEST_STEP_TYPE);
        if(CollectionUtils.isNotEmpty(attachmentList)) {
            attachmentList.forEach(attachment -> {
                attachmentManager.removeAttachment(attachment);
            });
        }
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

    private void updateTeststepResponseForPreviousNextTeststep(Long issueId, Issue issue, TeststepBeanWrapper teststepBeanWrapper, Optional<Integer> offsetValue, Optional<Integer> limitValue) {
        if(offsetValue.isPresent() && offsetValue.get().intValue() > 0) {
            Integer prevOffsetValue = offsetValue.get().intValue() - 1 ;
            Teststep prevStep = testStepManager.getPrevTeststep(issueId, prevOffsetValue, 1);

            TeststepBean prevTestStepBean = new TeststepBean(prevStep, issue,getAttachmentsMap(prevStep.getID()), getCustomFieldsValue(prevStep.getID(), issue));
            teststepBeanWrapper.setPrevTestStepBean(prevTestStepBean);

        }


        if(offsetValue.isPresent() && limitValue.isPresent()) {
            Integer nextOffsetValue = offsetValue.get().intValue() + limitValue.get().intValue();
            Teststep[] teststeps = testStepManager.getNextTeststep(issueId, nextOffsetValue, 2);
            boolean isLastElementOnPage = Boolean.FALSE;
            if(Objects.nonNull(teststeps) && teststeps.length > 0) {
                Teststep nextStep = null;
                Teststep firstElementOnNextPage = null;
                if(teststeps.length == 1) {
                    isLastElementOnPage = Boolean.TRUE;
                    nextStep = teststeps[0];
                }else if (teststeps.length >= 2) {
                    firstElementOnNextPage = teststeps[0];
                    nextStep = teststeps[1];
                }

                if(Objects.nonNull(nextStep)) {
                    TeststepBean teststepBean = new TeststepBean(nextStep, issue,getAttachmentsMap(nextStep.getID()), getCustomFieldsValue(nextStep.getID(), issue));
                    teststepBeanWrapper.setNextTestStepBean(teststepBean);
                }

                if(Objects.nonNull(firstElementOnNextPage)) {
                    TeststepBean teststepBean = new TeststepBean(firstElementOnNextPage, issue,getAttachmentsMap(firstElementOnNextPage.getID()), getCustomFieldsValue(firstElementOnNextPage.getID(), issue));
                    teststepBeanWrapper.setFirstElementOnNextPage(teststepBean);
                }
            }

            teststepBeanWrapper.setLastElementOnPage(isLastElementOnPage);
        }
    }
}
