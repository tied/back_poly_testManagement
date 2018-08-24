package com.thed.zephyr.je.rest;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.sun.jersey.spi.container.ResourceFilters;
import com.thed.zephyr.je.model.*;
import com.thed.zephyr.je.permissions.model.PermissionType;
import com.thed.zephyr.je.permissions.service.ZephyrPermissionManager;
import com.thed.zephyr.je.rest.delegate.CustomFieldResourceDelegate;
import com.thed.zephyr.je.rest.delegate.CustomFieldValueResourceDelegate;
import com.thed.zephyr.je.rest.delegate.ExecutionWorkflowResourceDelegate;
import com.thed.zephyr.je.rest.filter.ZFJApiFilter;
import com.thed.zephyr.je.service.CustomFieldValueManager;
import com.thed.zephyr.je.service.ScheduleManager;
import com.thed.zephyr.je.service.TeststepManager;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.ZephyrUtil;
import com.thed.zephyr.util.validator.CustomFieldValueValidationUtil;
import io.swagger.annotations.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;
import java.util.Objects;

@Api(value = "Custom Field Value Resource API(s)", description = "Following section describes the rest resources pertaining to custom field values storage based on entity type.")
@Path("customfieldvalue")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@AnonymousAllowed
@ResourceFilters(ZFJApiFilter.class)
public class CustomFieldValueResource {

    private static final Logger log = Logger.getLogger(PreferenceResource.class);
    private static final Integer STRING_VALUE_MAX_LENGTH = new Integer(255);

    private final JiraAuthenticationContext authContext;
    private final CustomFieldValueResourceDelegate customFieldValueResourceDelegate;
    private final ZephyrPermissionManager zephyrPermissionManager;
    private final ScheduleManager scheduleManager;
    private final TeststepManager teststepManager;
    private final CustomFieldValueManager customFieldValueManager;
    private final IssueManager issueManager;
    private final CustomFieldResourceDelegate customFieldResourceDelegate;
    private final ExecutionWorkflowResourceDelegate executionWorkflowResourceDelegate;


    public CustomFieldValueResource(final JiraAuthenticationContext authContext,
                                    final CustomFieldValueResourceDelegate customFieldValueResourceDelegate,
                                    final ZephyrPermissionManager zephyrPermissionManager,
                                    final ScheduleManager scheduleManager,
                                    final TeststepManager teststepManager,
                                    final CustomFieldValueManager customFieldValueManager,
                                    final IssueManager issueManager,
                                    final CustomFieldResourceDelegate customFieldResourceDelegate,
                                    final ExecutionWorkflowResourceDelegate executionWorkflowResourceDelegate) {
        this.authContext = authContext;
        this.customFieldValueResourceDelegate = customFieldValueResourceDelegate;
        this.zephyrPermissionManager = zephyrPermissionManager;
        this.scheduleManager = scheduleManager;
        this.teststepManager = teststepManager;
        this.customFieldValueManager = customFieldValueManager;
        this.issueManager = issueManager;
        this.customFieldResourceDelegate = customFieldResourceDelegate;
        this.executionWorkflowResourceDelegate = executionWorkflowResourceDelegate;
    }

    @ApiOperation(value = "Create custom field value entry based on entity type.", notes = "Create custom field value entry based on entity type.")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{\"customFieldId\":\"8\",\"customFieldValueId\":\"102\",\"customFieldType\":\"SINGLE_SELECT\",\"value\":\"Sports\",\"entityId\":\"72\",\"entityType\":\"EXECUTION\",\"selectedOptions\":\"6\"}"),
            @ApiImplicitParam(name = "response", value = "{\"8\":{\"customFieldValueId\":102,\"responseMessage\":\"\",\"customFieldId\":8,\"customFieldName\":\"Hobbies\",\"entityId\":72,\"customFieldType\":\"SINGLE_SELECT\",\"value\":\"Sports\",\"selectedOptions\":\"6\"},\"9\":{\"customFieldValueId\":103,\"responseMessage\":\"\",\"customFieldId\":9,\"customFieldName\":\"Workflow-StartDate\",\"entityId\":72,\"customFieldType\":\"DATE\",\"value\":\"1525851054000\",\"selectedOptions\":\"\"}}")})
    @ApiResponses({@ApiResponse(code = 400, message = "Invalid Request Parameters."),
            @ApiResponse(code = 500, message = "Server error while processing the request."),
            @ApiResponse(code = 401, message = "Unauthorized Request."), @ApiResponse(code = 403, message = "Permission Denied for the request"),
            @ApiResponse(code = 200, message = "Request processed successfully")})

    @PUT
    public Response createCustomFieldValueByEntityType(CustomFieldValueRequest customFieldValueRequest) {

        final ApplicationUser user = authContext.getLoggedInUser();
        JSONObject jsonObject = ZephyrUtil.validateUser(user, authContext);
        Issue issue = null;
        if (jsonObject != null) {
            return ZephyrUtil.constructErrorResponse(jsonObject, Response.Status.UNAUTHORIZED, null);
        }

        if (!CustomFieldValueValidationUtil.validateEntityType(customFieldValueRequest.getEntityType())) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "Entity type"),
                    Response.Status.BAD_REQUEST, null);
        }

        if (CustomFieldValueValidationUtil.isObjectNull(customFieldValueRequest.getCustomFieldId())) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "Custom Field Id"),
                    Response.Status.BAD_REQUEST, null);
        }else {
            CustomField customField = customFieldResourceDelegate.getCustomFieldById(customFieldValueRequest.getCustomFieldId());
            if(CustomFieldValueValidationUtil.isObjectNull(customField)) {
                jsonObject = new JSONObject();
                return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Custom Field Id",customFieldValueRequest.getCustomFieldId()+ StringUtils.EMPTY ), Response.Status.BAD_REQUEST, null);
            }

            if(CustomFieldValueValidationUtil.validateCustomFieldType(customFieldValueRequest.getCustomFieldType())) {
                jsonObject = new JSONObject();
                return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "Custom Field Type"),
                        Response.Status.BAD_REQUEST, null);
            }

            if(CustomFieldValueValidationUtil.validateCustomFieldTypeWithGivenCustomField(customFieldValueRequest.getCustomFieldType(), customField.getCustomFieldType())) {
                jsonObject = new JSONObject();
                return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Given custom field type doesn't match with provided custom field ID.", customFieldValueRequest.getCustomFieldType()),
                        Response.Status.BAD_REQUEST, null);
            }

            if(CustomFieldValueValidationUtil.validateEntityTypeWithGivenCustomField(customFieldValueRequest.getEntityType(),customField.getZFJEntityType())) {
                jsonObject = new JSONObject();
                return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Given custom field entity type doesn't match with provided custom field ID entity type.", customFieldValueRequest.getCustomFieldType()),
                        Response.Status.BAD_REQUEST, null);
            }
        }

        if(CustomFieldValueValidationUtil.isStringBlank(customFieldValueRequest.getCustomFieldType())) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "Custom Field Type"),
                    Response.Status.BAD_REQUEST, null);
        }

        if (CustomFieldValueValidationUtil.isObjectNull(customFieldValueRequest.getEntityId())) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "Entity Id"),
                    Response.Status.BAD_REQUEST, null);
        }

        if(customFieldValueRequest.getEntityType().equalsIgnoreCase(ApplicationConstants.ENTITY_TYPE.EXECUTION.name())) {

            Schedule schedule = scheduleManager.getSchedule(customFieldValueRequest.getEntityId());
            if(CustomFieldValueValidationUtil.isObjectNull(schedule)) {
                jsonObject = new JSONObject();
                return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("schedule.does.not.exist.title"), Response.Status.BAD_REQUEST, null);
            }

            boolean isWorkflowDisabled = executionWorkflowResourceDelegate.isExecutionWorkflowDisabled(schedule.getProjectId());
            if(!isWorkflowDisabled && null != schedule.getExecutionWorkflowStatus() &&
                    schedule.getExecutionWorkflowStatus().name().equals(ExecutionWorkflowStatus.COMPLETED.name())) {
                try {
                    JSONObject errorJson = new JSONObject();
                    errorJson.put("error", authContext.getI18nHelper().getText("workflow.schedule.modify.error"));
                    log.error(authContext.getI18nHelper().getText("workflow.schedule.modify.error"));
                    return Response.status(Response.Status.BAD_REQUEST).entity(errorJson.toString()).build();
                } catch (JSONException e) {
                    log.error("Exception occurred while creating error response object.");
                }
            }

            issue = issueManager.getIssueObject(new Long(schedule.getIssueId()));
            if(! verifyExecutionEditOperationPermission(user,schedule.getProjectId(), PermissionType.ZEPHYR_EDIT_EXECUTION.toString())) {

                String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
                JSONObject errorObject = new JSONObject();
                try {
                    errorObject.put("PERM_DENIED",errorMessage);
                } catch (JSONException e) {
                   log.error("Exception occurred while creating error response object.");
                }
                return ZephyrUtil.constructErrorResponse(errorObject,errorMessage, Response.Status.FORBIDDEN,null);
            }

            if(!validateDisableCustomFieldForProjectAndCustomField(customFieldValueRequest.getCustomFieldId(),schedule.getProjectId())) {
                jsonObject = new JSONObject();
                return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Given custom field is disabled for the project to which given entity ID belongs.", customFieldValueRequest.getCustomFieldId()+StringUtils.EMPTY),
                        Response.Status.BAD_REQUEST, null);
            }

            if(Objects.nonNull(customFieldValueRequest.getCustomFieldValueId())) {
                ExecutionCf executionCf = customFieldValueResourceDelegate.getExecutionCustomFieldValue(customFieldValueRequest.getCustomFieldValueId());
                if(CustomFieldValueValidationUtil.isObjectNull(executionCf)) {
                    jsonObject = new JSONObject();
                    return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Custom Field Value Id",customFieldValueRequest.getCustomFieldValueId()+ StringUtils.EMPTY ), Response.Status.BAD_REQUEST, null);
                }else {

                    if(Objects.nonNull(executionCf.getID()) && CustomFieldValueValidationUtil.validateCustomFieldValueIdWithGivenId(Long.valueOf(executionCf.getID()), customFieldValueRequest.getCustomFieldValueId())) {
                        jsonObject = new JSONObject();
                        return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Custom field value ID doesn't belong to provided entity ID.", customFieldValueRequest.getCustomFieldValueId()+StringUtils.EMPTY),
                                Response.Status.BAD_REQUEST, null);
                    }

                    if(Objects.nonNull(executionCf.getCustomField()) && CustomFieldValueValidationUtil.validateCustomFieldIdWithGivenId(Long.valueOf(executionCf.getCustomField().getID()), customFieldValueRequest.getCustomFieldId())) {
                        jsonObject = new JSONObject();
                        return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Custom field associated with given custom field value ID doesn't match with provided custom field ID.", customFieldValueRequest.getCustomFieldId()+StringUtils.EMPTY),
                                Response.Status.BAD_REQUEST, null);
                    }

                    if(Objects.nonNull(executionCf.getExecutionId()) && CustomFieldValueValidationUtil.validateEntityIdWithGivenId(Integer.valueOf(executionCf.getExecutionId()), customFieldValueRequest.getEntityId())) {
                        jsonObject = new JSONObject();
                        return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Custom field value ID doesn't belong to provided entity ID.", customFieldValueRequest.getCustomFieldValueId()+StringUtils.EMPTY),
                                Response.Status.BAD_REQUEST, null);
                    }
                }
            }

        } else if(customFieldValueRequest.getEntityType().equalsIgnoreCase(ApplicationConstants.ENTITY_TYPE.TESTSTEP.name())) {
            Teststep teststep = teststepManager.getTeststep(customFieldValueRequest.getEntityId());
            if(CustomFieldValueValidationUtil.isObjectNull(teststep)) {
                jsonObject = new JSONObject();
                return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("schedule.does.not.exist.title"), Response.Status.BAD_REQUEST, null);
            }
            issue = issueManager.getIssueObject(teststep.getIssueId());
            if(! JiraUtil.hasIssueViewPermission(issue.getId(), issue, user)) {
                String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
                JSONObject errorObject = new JSONObject();
                return ZephyrUtil.constructErrorResponse(errorObject,errorMessage, Response.Status.FORBIDDEN,null);
            }

            if(!validateDisableCustomFieldForProjectAndCustomField(customFieldValueRequest.getCustomFieldId(),issue.getProjectId())) {
                jsonObject = new JSONObject();
                return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Given custom field is disabled for the project to which given entity ID belongs.", customFieldValueRequest.getCustomFieldId()+StringUtils.EMPTY),
                        Response.Status.BAD_REQUEST, null);
            }
            if(Objects.nonNull(customFieldValueRequest.getCustomFieldValueId())) {
                TestStepCf testStepCf = customFieldValueResourceDelegate.getTeststepCustomFieldValue(customFieldValueRequest.getCustomFieldValueId());
                if(CustomFieldValueValidationUtil.isObjectNull(testStepCf)) {
                    jsonObject = new JSONObject();
                    return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Custom Field Value Id",customFieldValueRequest.getCustomFieldValueId()+ StringUtils.EMPTY), Response.Status.BAD_REQUEST, null);
                }else {
                    if(Objects.nonNull(testStepCf.getID()) && CustomFieldValueValidationUtil.validateCustomFieldValueIdWithGivenId(Long.valueOf(testStepCf.getID()), customFieldValueRequest.getCustomFieldValueId())) {
                        jsonObject = new JSONObject();
                        return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Custom field value ID doesn't belong to provided entity ID.", customFieldValueRequest.getCustomFieldValueId()+StringUtils.EMPTY),
                                Response.Status.BAD_REQUEST, null);
                    }

                    if(Objects.nonNull(testStepCf.getCustomField()) && CustomFieldValueValidationUtil.validateCustomFieldIdWithGivenId(Long.valueOf(testStepCf.getCustomField().getID()), customFieldValueRequest.getCustomFieldId())) {
                        jsonObject = new JSONObject();
                        return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Custom field associated with given custom field value ID doesn't match with provided custom field ID.", customFieldValueRequest.getCustomFieldId()+StringUtils.EMPTY),
                                Response.Status.BAD_REQUEST, null);
                    }

                    if(Objects.nonNull(testStepCf.getTestStepId()) && CustomFieldValueValidationUtil.validateEntityIdWithGivenId(Integer.valueOf(testStepCf.getTestStepId()), customFieldValueRequest.getEntityId())) {
                        jsonObject = new JSONObject();
                        return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Custom field value ID doesn't belong to provided entity ID.", customFieldValueRequest.getCustomFieldValueId()+StringUtils.EMPTY),
                                Response.Status.BAD_REQUEST, null);
                    }
                }
            }
        }

        if(StringUtils.isNotBlank(customFieldValueRequest.getSelectedOptions())) {
            if(validateCustomFieldOptionIds(customFieldValueRequest.getSelectedOptions(), customFieldValueRequest.getCustomFieldId())) {
                jsonObject = new JSONObject();
                return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Either Custom Field Options Value ID doesn't exist or doesn't belong to provided custom field ID.",customFieldValueRequest.getSelectedOptions()+ StringUtils.EMPTY), Response.Status.BAD_REQUEST, null);
            }
        }

        if(Objects.isNull(customFieldValueRequest.getCustomFieldValueId())) {
            if(isCustomFieldAndEntityRecordExist(customFieldValueRequest)) {
                jsonObject = new JSONObject();
                return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Custom Field Value Id",customFieldValueRequest.getCustomFieldValueId()+ StringUtils.EMPTY), Response.Status.BAD_REQUEST, null);
            }
        }

        if(CustomFieldValueValidationUtil.validateCustomFieldValueWithType(customFieldValueRequest.getCustomFieldType(),customFieldValueRequest.getValue())) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Custom Field Value",customFieldValueRequest.getValue()), Response.Status.BAD_REQUEST, null);
        }

        if(CustomFieldValueValidationUtil.validateNumberValueMaxLength(customFieldValueRequest.getCustomFieldType(), customFieldValueRequest.getValue())) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Custom field value is too large, max allowed is +/-10^14. Custom field value ",customFieldValueRequest.getValue()), Response.Status.BAD_REQUEST, null);
        }

        if(CustomFieldValueValidationUtil.validateTextValueMaxLength(customFieldValueRequest.getCustomFieldType(),customFieldValueRequest.getValue(), STRING_VALUE_MAX_LENGTH)) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("field.limit.exceed.validationError.description", "custom field value","255"),
                    Response.Status.BAD_REQUEST, null);
        }

        if(Objects.isNull(customFieldValueRequest.getCustomFieldValueId())) {
            customFieldValueResourceDelegate.createCustomFieldValue(customFieldValueRequest, customFieldValueRequest.getEntityType());
        }else {
            customFieldValueResourceDelegate.updateCustomFieldValue(customFieldValueRequest, customFieldValueRequest.getEntityType(),
                    customFieldValueRequest.getCustomFieldValueId());
        }

        Map<String, CustomFieldValueResource.CustomFieldValueResponse> response = customFieldValueResourceDelegate.getCustomFieldValuesByEntityId(customFieldValueRequest.getEntityId(),
                customFieldValueRequest.getEntityType(), issue);
        return Response.ok(response).build();
    }

    @ApiOperation(value = "Create custom field values entry based on entity type in bulk.", notes = "Create custom field values entry based on entity type.")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{\"2\":{\"customFieldId\":2,\"customFieldType\":\"DATE\",\"entityType\":\"TESTSTEP\",\"value\":\"1525804200\",\"selectedOptions\":\"\"},\"3\":{\"customFieldId\":3,\"customFieldType\":\"TEXT\",\"entityType\":\"TESTSTEP\",\"value\":\"loreum ipsum.\",\"selectedOptions\":\"\"}}"),
            @ApiImplicitParam(name = "response", value = "{\"2\":{\"customFieldValueId\":34,\"responseMessage\":\"\",\"customFieldId\":2,\"customFieldName\":\"StepCreateDate-updated\",\"entityId\":35,\"customFieldType\":\"DATE\",\"value\":\"1525804200000\",\"selectedOptions\":\"\"},\"3\":{\"customFieldValueId\":35,\"responseMessage\":\"\",\"customFieldId\":3,\"customFieldName\":\"TextValue\",\"entityId\":35,\"customFieldType\":\"TEXT\",\"value\":\"loreum ipsum.\",\"htmlValue\":\"<p>loreum ipsum.</p>\",\"selectedOptions\":\"\"}}")})
    @ApiResponses({@ApiResponse(code = 400, message = "Invalid Request Parameters."),
            @ApiResponse(code = 500, message = "Server error while processing the request."),
            @ApiResponse(code = 401, message = "Unauthorized Request."), @ApiResponse(code = 403, message = "Permission Denied for the request"),
            @ApiResponse(code = 200, message = "Request processed successfully")})

    @POST
    @Path("/{entityType}/{entityId}")
    public Response createCustomFieldValuesByEntityType(Map<String, CustomFieldValueRequest> customFieldValueRequests, @PathParam("entityType") String entityType,
                                                        @PathParam("entityId") Integer entityId) {

        final ApplicationUser user = authContext.getLoggedInUser();
        JSONObject jsonObject = ZephyrUtil.validateUser(user, authContext);
        if (jsonObject != null) {
            return ZephyrUtil.constructErrorResponse(jsonObject, Response.Status.UNAUTHORIZED, null);
        }

        if (!CustomFieldValueValidationUtil.validateEntityType(entityType)) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "Entity type"),
                    Response.Status.BAD_REQUEST, null);
        }

        if(Objects.isNull(entityId)) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "entityId"), Response.Status.BAD_REQUEST, null);
        }

        if(entityType.equalsIgnoreCase(ApplicationConstants.ENTITY_TYPE.EXECUTION.name())) {

            Schedule schedule = scheduleManager.getSchedule(entityId);
            if(Objects.isNull(schedule)) {
                jsonObject = new JSONObject();
                return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("schedule.does.not.exist.title"), Response.Status.BAD_REQUEST, null);
            }

            boolean isWorkflowDisabled = executionWorkflowResourceDelegate.isExecutionWorkflowDisabled(schedule.getProjectId());
            if(!isWorkflowDisabled && null != schedule.getExecutionWorkflowStatus() &&
                    schedule.getExecutionWorkflowStatus().name().equals(ExecutionWorkflowStatus.COMPLETED.name())) {
                try {
                    JSONObject errorJson = new JSONObject();
                    errorJson.put("error", authContext.getI18nHelper().getText("workflow.schedule.modify.error"));
                    return Response.status(Response.Status.BAD_REQUEST).entity(errorJson.toString()).build();
                } catch (JSONException e) {
                    log.error("Exception occurred while creating error response object.");
                }
            }

            if(! verifyExecutionEditOperationPermission(user,schedule.getProjectId(), PermissionType.ZEPHYR_EDIT_EXECUTION.toString())) {

                String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
                JSONObject errorObject = new JSONObject();
                return ZephyrUtil.constructErrorResponse(errorObject,errorMessage, Response.Status.FORBIDDEN,null);
            }

        } else if(entityType.equalsIgnoreCase(ApplicationConstants.ENTITY_TYPE.TESTSTEP.name())) {
            Teststep teststep = teststepManager.getTeststep(entityId);
            if(Objects.isNull(teststep)) {
                jsonObject = new JSONObject();
                return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("schedule.does.not.exist.title"), Response.Status.BAD_REQUEST, null);
            }
            MutableIssue issue = issueManager.getIssueObject(teststep.getIssueId());
            if(! JiraUtil.hasIssueViewPermission(issue.getId(), issue, user)) {
                String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
                JSONObject errorObject = new JSONObject();
                return ZephyrUtil.constructErrorResponse(errorObject,errorMessage, Response.Status.FORBIDDEN,null);
            }
        }

        customFieldValueResourceDelegate.createCustomFieldValues(customFieldValueRequests, entityType, entityId);

        Map<String, CustomFieldValueResource.CustomFieldValueResponse> response = customFieldValueResourceDelegate.getCustomFieldValuesByEntityId(entityId,entityType, null);
        return Response.ok(response).build();
    }

    @ApiOperation(value = "Get custom field value data by entity type & entity id.", notes = "Get custom field value data by entity type & entity id.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "response", value = "{\"8\":{\"customFieldValueId\":102,\"responseMessage\":\"\",\"customFieldId\":8,\"customFieldName\":\"Hobbies\",\"entityId\":72,\"customFieldType\":\"SINGLE_SELECT\",\"value\":\"Sports\",\"selectedOptions\":\"6\"},\"9\":{\"customFieldValueId\":103,\"responseMessage\":\"\",\"customFieldId\":9,\"customFieldName\":\"Workflow-StartDate\",\"entityId\":72,\"customFieldType\":\"DATE\",\"value\":\"1525851054000\",\"selectedOptions\":\"\"}}")})
    @ApiResponses({@ApiResponse(code = 400, message = "Invalid Request Parameters."),
            @ApiResponse(code = 500, message = "Server error while processing the request."),
            @ApiResponse(code = 401, message = "Unauthorized Request."), @ApiResponse(code = 403, message = "Permission Denied for the request"),
            @ApiResponse(code = 200, message = "Request processed successfully")})

    @GET
    @Path("/{entityType}/{entityId}")
    public Response getCustomFieldValuesByEntityTypeAndEntityId(@PathParam("entityType") String entityType,
                                                               @PathParam("entityId") Integer entityId) {
        final ApplicationUser user = authContext.getLoggedInUser();
        JSONObject jsonObject = ZephyrUtil.validateUser(user, authContext);
        if (jsonObject != null) {
            return ZephyrUtil.constructErrorResponse(jsonObject, Response.Status.UNAUTHORIZED, null);
        }

        if (!CustomFieldValueValidationUtil.validateEntityType(entityType)) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "Entity type"),
                    Response.Status.BAD_REQUEST, null);
        }

        if(Objects.isNull(entityId)) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "entityId"), Response.Status.BAD_REQUEST, null);
        }

        return Response.ok(customFieldValueResourceDelegate.getCustomFieldValuesByEntityId(entityId,entityType, null)).build();
    }

    @ApiOperation(value = "Create custom field value entry based on entity type.", notes = "Create custom field value entry based on entity type.")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{\"customFieldId\":\"9\",\"customFieldValueId\":\"103\",\"customFieldType\":\"DATE\",\"value\":\"1525717800\",\"entityId\":\"72\",\"entityType\":\"EXECUTION\",\"selectedOptions\":\"\"}"),
            @ApiImplicitParam(name = "response", value = "{\"8\":{\"customFieldValueId\":102,\"responseMessage\":\"\",\"customFieldId\":8,\"customFieldName\":\"Hobbies\",\"entityId\":72,\"customFieldType\":\"SINGLE_SELECT\",\"value\":\"Sports\",\"selectedOptions\":\"6\"},\"9\":{\"customFieldValueId\":103,\"responseMessage\":\"\",\"customFieldId\":9,\"customFieldName\":\"Workflow-StartDate\",\"entityId\":72,\"customFieldType\":\"DATE\",\"value\":\"1525717800000\",\"selectedOptions\":\"\"}}")})
    @ApiResponses({@ApiResponse(code = 400, message = "Invalid Request Parameters."),
            @ApiResponse(code = 500, message = "Server error while processing the request."),
            @ApiResponse(code = 401, message = "Unauthorized Request."), @ApiResponse(code = 403, message = "Permission Denied for the request"),
            @ApiResponse(code = 200, message = "Request processed successfully")})

    @PUT
    @Path("/{customFieldValueId}")
    public Response updateCustomFieldValueByEntityType(CustomFieldValueRequest customFieldValueRequest,
                                                       @PathParam("customFieldValueId") Long customFieldValueId) {

        final ApplicationUser user = authContext.getLoggedInUser();
        JSONObject jsonObject = ZephyrUtil.validateUser(user, authContext);
        if (jsonObject != null) {
            return ZephyrUtil.constructErrorResponse(jsonObject, Response.Status.UNAUTHORIZED, null);
        }

        if (!CustomFieldValueValidationUtil.validateEntityType(customFieldValueRequest.getEntityType())) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "Entity type"),
                    Response.Status.BAD_REQUEST, null);
        }

        if(Objects.isNull(customFieldValueId)) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "customFieldValueId"), Response.Status.BAD_REQUEST, null);
        }

        if (ApplicationConstants.ENTITY_TYPE.TESTSTEP.name().equalsIgnoreCase(customFieldValueRequest.getEntityType())) {
            TestStepCf testStepCf = customFieldValueManager.getTeststepCustomFieldValue(customFieldValueId);

            if(Objects.isNull(testStepCf)) {
                jsonObject = new JSONObject();
                return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.custom.field.value.does.not.exist"), Response.Status.BAD_REQUEST, null);
            }
        } else if (ApplicationConstants.ENTITY_TYPE.EXECUTION.name().equalsIgnoreCase(customFieldValueRequest.getEntityType())) {
            ExecutionCf executionCf = customFieldValueManager.getExecutionCustomFieldValue(customFieldValueId);
            if (Objects.isNull(executionCf)) {
                jsonObject = new JSONObject();
                return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.custom.field.value.does.not.exist"), Response.Status.BAD_REQUEST, null);
            }
        }

        customFieldValueResourceDelegate.updateCustomFieldValue(customFieldValueRequest, customFieldValueRequest.getEntityType(),
                                            customFieldValueId);

        Map<String, CustomFieldValueResource.CustomFieldValueResponse> response = customFieldValueResourceDelegate.getCustomFieldValuesByEntityId(customFieldValueRequest.getEntityId(),
                customFieldValueRequest.getEntityType(), null);
        return Response.ok(response).build();
    }

    /**
     * Validate execution edit permission to associate custom field.
     * @param user
     * @param projectId
     * @param permissionType
     */
    private boolean verifyExecutionEditOperationPermission(ApplicationUser user, Long projectId, String permissionType) {
        ProjectPermissionKey projectPermissionKey = new ProjectPermissionKey(permissionType);
        return zephyrPermissionManager.validateUserPermission(projectPermissionKey, null, user, projectId);
    }

    /**
     *
     * @param selectedOptions
     * @return
     */
    private boolean validateCustomFieldOptionIds(String selectedOptions, Long inputCustomFieldId) {

        if(StringUtils.isNotBlank(selectedOptions)) {
            String[] options = StringUtils.split(selectedOptions, ",");

            for (String option : options) {
                try{
                    CustomFieldOption customFieldOption = customFieldResourceDelegate.getCustomFieldOptionById(Integer.valueOf(option));
                    if(Objects.isNull(customFieldOption)) {
                        return Boolean.TRUE;
                    }else {
                        // validate whether it belongs to provided custom field id.
                        Long customFieldId = Long.valueOf(customFieldOption.getCustomField().getID());
                        if( !customFieldId.equals(inputCustomFieldId)) {
                            return Boolean.TRUE;
                        }
                    }
                }catch (NumberFormatException exception) {
                    log.error("Error occurred while parsing the option id from selected options.");
                    return Boolean.TRUE;
                }
            }
        }
        return Boolean.FALSE;
    }

    private boolean isCustomFieldAndEntityRecordExist(CustomFieldValueRequest customFieldValueRequest) {
        return customFieldValueResourceDelegate.getCustomFieldAndEntityRecord(customFieldValueRequest);
    }

    private boolean validateDisableCustomFieldForProjectAndCustomField(Long customFieldId, Long projectId) {
        return customFieldResourceDelegate.getDisableCustomFieldForProjectAndCustomField(customFieldId,projectId);
    }

    @XmlRootElement
    @ApiModel("customFieldValueBean")
    public static class CustomFieldValueRequest {

        @XmlElement(nillable = true)
        @ApiModelProperty
        private Long customFieldValueId;

        @XmlElement(nillable = true)
        @ApiModelProperty(required = true)
        private Long customFieldId;

        @XmlElement(nillable = true)
        @ApiModelProperty
        private Integer entityId;

        @XmlElement(nillable = true)
        @ApiModelProperty(required = true)
        private String customFieldType;

        @XmlElement(nillable = true)
        @ApiModelProperty(required = true)
        private String value;

        @XmlElement(nillable = true)
        @ApiModelProperty(required = true)
        private String entityType;

        @XmlElement(nillable = true)
        @ApiModelProperty(required = true)
        private String selectedOptions;

        public Long getCustomFieldId() {
            return customFieldId;
        }

        public void setCustomFieldId(Long customFieldId) {
            this.customFieldId = customFieldId;
        }

        public Long getCustomFieldValueId() {
            return customFieldValueId;
        }

        public void setCustomFieldValueId(Long customFieldValueId) {
            this.customFieldValueId = customFieldValueId;
        }

        public Integer getEntityId() {
            return entityId;
        }

        public void setEntityId(Integer entityId) {
            this.entityId = entityId;
        }

        public String getCustomFieldType() {
            return customFieldType;
        }

        public void setCustomFieldType(String customFieldType) {
            this.customFieldType = customFieldType;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getEntityType() {
            return entityType;
        }

        public void setEntityType(String entityType) {
            this.entityType = entityType;
        }

        public String getSelectedOptions() {
            return selectedOptions;
        }

        public void setSelectedOptions(String selectedOptions) {
            this.selectedOptions = selectedOptions;
        }

        public CustomFieldValueRequest() {

        }
    }

    @XmlRootElement
    @ApiModel("customFieldValueResponse")
    public static class CustomFieldValueResponse {

        @XmlElement
        @ApiModelProperty
        private long customFieldValueId;

        @XmlElement
        @ApiModelProperty
        private String responseMessage;

        @XmlElement
        @ApiModelProperty
        private Long customFieldId;

        @XmlElement
        @ApiModelProperty
        private String customFieldName;

        @XmlElement
        @ApiModelProperty
        private String customFieldDisplayName;

        @XmlElement
        @ApiModelProperty
        private Integer entityId;

        @XmlElement
        @ApiModelProperty
        private String customFieldType;

        @XmlElement
        @ApiModelProperty
        private String value;

        @XmlElement
        @ApiModelProperty
        private String htmlValue;

        @XmlElement
        @ApiModelProperty
        private String entityType;

        @XmlElement(nillable = true)
        @ApiModelProperty(required = true)
        private String selectedOptions;

        @XmlElement
        @ApiModelProperty
        private Long projectId;


        public long getCustomFieldValueId() {
            return customFieldValueId;
        }

        public void setCustomFieldValueId(long customFieldValueId) {
            this.customFieldValueId = customFieldValueId;
        }

        public String getResponseMessage() {
            return responseMessage;
        }

        public void setResponseMessage(String responseMessage) {
            this.responseMessage = responseMessage;
        }

        public Long getCustomFieldId() {
            return customFieldId;
        }

        public void setCustomFieldId(Long customFieldId) {
            this.customFieldId = customFieldId;
        }

        public String getCustomFieldName() {
            return customFieldName;
        }

        public void setCustomFieldName(String customFieldName) {
            this.customFieldName = customFieldName;
        }

        public String getCustomFieldDisplayName() {
            return customFieldDisplayName;
        }

        public void setCustomFieldDisplayName(String customFieldDisplayName) {
            this.customFieldDisplayName = customFieldDisplayName;
        }

        public Integer getEntityId() {
            return entityId;
        }

        public void setEntityId(Integer entityId) {
            this.entityId = entityId;
        }

        public String getCustomFieldType() {
            return customFieldType;
        }

        public void setCustomFieldType(String customFieldType) {
            this.customFieldType = customFieldType;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getEntityType() {
            return entityType;
        }

        public void setEntityType(String entityType) {
            this.entityType = entityType;
        }

        public String getSelectedOptions() {
            return selectedOptions;
        }

        public void setSelectedOptions(String selectedOptions) {
            this.selectedOptions = selectedOptions;
        }

        public Long getProjectId() {
            return projectId;
        }

        public void setProjectId(Long projectId) {
            this.projectId = projectId;
        }

        public String getHtmlValue() {
            return htmlValue;
        }

        public void setHtmlValue(String htmlValue) {
            this.htmlValue = htmlValue;
        }

        public CustomFieldValueResponse() {

        }
    }

}
