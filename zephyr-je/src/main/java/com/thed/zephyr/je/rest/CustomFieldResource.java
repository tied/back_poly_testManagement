package com.thed.zephyr.je.rest;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sun.jersey.spi.container.ResourceFilters;
import com.thed.zephyr.je.model.CustomField;
import com.thed.zephyr.je.model.CustomFieldOption;
import com.thed.zephyr.je.model.CustomFieldsMeta;
import com.thed.zephyr.je.rest.delegate.CustomFieldResourceDelegate;
import com.thed.zephyr.je.rest.filter.ZFJApiFilter;
import com.thed.zephyr.je.zql.core.SearchHandlerManager;
import com.thed.zephyr.je.zql.core.SystemSearchConstant;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.ZephyrCacheControl;
import com.thed.zephyr.util.ZephyrUtil;
import io.swagger.annotations.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Api(value = "CustomField Resource API(s)", description = "Following section describes rest resources pertaining to CustomField Resource")
@Path("customfield")
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@ResourceFilters(ZFJApiFilter.class)
public class CustomFieldResource {

    protected final Logger log = Logger.getLogger(CustomFieldResource.class);
	private static final String ERROR_LOG_MESSAGE = "[Error] [Error code: %s %s Error Message : %s]";
    private static final String CUSTOM_FIELD_ID = "customFieldId";
    private static final String OPTION_VALUE = "optionValue";
    private static final String IS_DISABLED = "isDisabled";
    private static final String SEQUENCE = "sequence";
    private static final String CUSTOM_FIELD_OPTION_ID = "optionId";
    private static final String COMMA = ",";
    private static final Integer STRING_VALUE_MAX_LENGTH = new Integer(255);


	private final JiraAuthenticationContext authContext;
	private final CustomFieldResourceDelegate customFieldResourceDelegate;
	private final IssueManager issueManager;
	private final ProjectManager projectManager;
    private final SearchHandlerManager searchHandlerManager;

	public CustomFieldResource(JiraAuthenticationContext authContext,
			CustomFieldResourceDelegate customFieldResourceDelegate, IssueManager issueManager,ProjectManager projectManager,
                               SearchHandlerManager searchHandlerManager) {
		this.authContext = authContext;
		this.customFieldResourceDelegate = customFieldResourceDelegate;
		this.issueManager = issueManager;
		this.projectManager = projectManager;
		this.searchHandlerManager=searchHandlerManager;
	}

	@GET
	@Path("/metadata")
	@AnonymousAllowed
	public Response getCustomFieldsMetaInfo() {

        final ApplicationUser user = authContext.getLoggedInUser();
        JSONObject jsonObject = ZephyrUtil.validateUser(user, authContext);
        if (jsonObject != null) {
            return ZephyrUtil.constructErrorResponse(jsonObject, Response.Status.UNAUTHORIZED, null);
        }

		CustomFieldsMeta[] customFieldsMetaData = customFieldResourceDelegate.getCustomFieldsMeta();
        JSONObject jsonResponse = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		try {
			for (CustomFieldsMeta customField : customFieldsMetaData) {
                JSONObject jsonObj = new JSONObject();
                jsonObj.put("label", ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText(customField.getLabel()));
                jsonObj.put("type", customField.getType());
                jsonObj.put("imageClass", customField.getImage());
                jsonObj.put("description", ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText(customField.getDescription()));
                jsonObj.put("options", customField.isOptions());
				jsonArray.put(jsonObj);
			}
            jsonResponse.put("customFields",jsonArray);
        } catch (JSONException jsonException) {
			log.error("Error occurred while creating CustomFields metadata.",jsonException);
        }
		return Response.ok(jsonResponse.toString()).cacheControl(ZephyrCacheControl.never()).build();
	}

    @ApiOperation(value = "Get custom fields.", notes = "Get custom fields.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "response", value = "")})
    @ApiResponses({@ApiResponse(code = 400, message = "Invalid Request Parameters."),
            @ApiResponse(code = 500, message = "Server error while processing the request."),
            @ApiResponse(code = 401, message = "Unauthorized Request."), @ApiResponse(code = 403, message = "Permission Denied for the request"),
            @ApiResponse(code = 200, message = "Request processed successfully")})
	@GET
	@AnonymousAllowed
	public Response getCustomFields(@ApiParam(value = "Project Id") @QueryParam("projectId") Long projectId) {
        final ApplicationUser user = authContext.getLoggedInUser();
        JSONObject jsonObject = ZephyrUtil.validateUser(user, authContext);
        if (jsonObject != null) {
            return ZephyrUtil.constructErrorResponse(jsonObject, Response.Status.UNAUTHORIZED, null);
        }

        if(null != projectId) {
            Response response = validateProjectPermission(user, projectId);
            if(response != null) return response;
        }

		CustomField[] customFields = customFieldResourceDelegate.getCustomFields(projectId);

		List<CustomFieldResponse> customFieldResponses = buildResponse(customFields);
		return Response.ok().entity(customFieldResponses).build();
	}

    @ApiOperation(value = "Get custom field by id.", notes = "Get custom field by id.")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = ""),
            @ApiImplicitParam(name = "response", value = "")})
    @ApiResponses({@ApiResponse(code = 400, message = "Invalid Request Parameters."),
            @ApiResponse(code = 500, message = "Server error while processing the request."),
            @ApiResponse(code = 401, message = "Unauthorized Request."), @ApiResponse(code = 403, message = "Permission Denied for the request"),
            @ApiResponse(code = 200, message = "Request processed successfully")})
	@GET
	@Path("/{id}")
	@AnonymousAllowed
	public Response getCustomFieldById(@ApiParam(value = "CustomField Id") @PathParam("id") Long customFieldId) {

        final ApplicationUser user = authContext.getLoggedInUser();
        JSONObject jsonObject = ZephyrUtil.validateUser(user, authContext);
        if (jsonObject != null) {
            return ZephyrUtil.constructErrorResponse(jsonObject, Response.Status.UNAUTHORIZED, null);
        }

        Response response = validateSystemAdministrator(user);

        if(null !=response) {
            return response;
        }

		CustomField customField = customFieldResourceDelegate.getCustomFieldById(customFieldId);
		CustomField[] customFields = { customField };


		List<CustomFieldResponse> customFieldResponses = buildResponse(customFields);
		return Response.ok().entity(customFieldResponses).build();
	}

    @ApiOperation(value = "Get custom fields by given entity type (EXECUTION | TESTSTEP).", notes = "Get custom fields by given entity type (EXECUTION | TESTSTEP).")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "response", value = "[{\"name\":\"Custom_Field_Number\",\"description\":\"loreum ipsum\",\"defaultValue\":\"\",\"isActive\":false,\"fieldType\":\"NUMBER\",\"aliasName\":\"\",\"projectId\":\"\",\"displayName\":\"\",\"createdOn\":1525689773853,\"createdBy\":\"admin\",\"entityType\":\"EXECUTION\",\"fieldOptions\":\"\",\"displayFieldType\":\"Number Field\",\"id\":3,\"customFieldOptionValues\":\"\",\"active\":false},{\"name\":\"Custo_Field_Name_Update\",\"description\":\"loreum ipsum updated.\",\"defaultValue\":\"\",\"isActive\":false,\"fieldType\":\"CHECKBOX\",\"aliasName\":\"\",\"projectId\":\"\",\"displayName\":\"\",\"createdOn\":1525688574630,\"createdBy\":\"admin\",\"entityType\":\"EXECUTION\",\"fieldOptions\":\"\",\"displayFieldType\":\"Checkboxes\",\"id\":2,\"customFieldOptionValues\":{\"5\":\"option1\",\"6\":\"option2\",\"7\":\"option3\",\"8\":\"option4\"},\"active\":false}]")})
    @ApiResponses({@ApiResponse(code = 400, message = "Invalid Request Parameters."),
            @ApiResponse(code = 500, message = "Server error while processing the request."),
            @ApiResponse(code = 401, message = "Unauthorized Request."), @ApiResponse(code = 403, message = "Permission Denied for the request"),
            @ApiResponse(code = 200, message = "Request processed successfully")})
	@GET
	@Path("/entity")
	@AnonymousAllowed
	public Response getCustomFieldsByEntity(
			@ApiParam(value = "Entity Type") @QueryParam("entityType") String entityType,
            @ApiParam(value = "Project Id") @QueryParam("projectId") Long projectId,
            @ApiParam(value = "Issue Id") @QueryParam("issueId") Long issueId,
            @ApiParam(value = "Is Global", defaultValue = "true") @QueryParam("isGlobal") Boolean isGlobal) {

        if(null == projectId && null!=issueId) {
            Issue issue = issueManager.getIssueObject(issueId);
            projectId = issue.getProjectId();
        }

        final ApplicationUser user = authContext.getLoggedInUser();
        JSONObject jsonObject = ZephyrUtil.validateUser(user, authContext);
        if (jsonObject != null) {
            return ZephyrUtil.constructErrorResponse(jsonObject, Response.Status.UNAUTHORIZED, null);
        }

        if (!ApplicationConstants.ENTITY_TYPE.TESTSTEP.toString().equalsIgnoreCase(entityType) &&
                !ApplicationConstants.ENTITY_TYPE.EXECUTION.toString().equalsIgnoreCase(entityType)) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "Entity type"),
                    Response.Status.BAD_REQUEST, null);
        }
		CustomField[] customFields = customFieldResourceDelegate.getCustomFieldsByEntityType(entityType,projectId,isGlobal);
		List<CustomFieldResponse> customFieldResponses = buildResponse(customFields);
		return Response.ok().entity(customFieldResponses).build();
	}

    @ApiOperation(value = "Get custom fields by given entity type (EXECUTION | TESTSTEP) and project id/issue id.", notes = "Get custom fields by given entity type (EXECUTION | TESTSTEP) and project id/issue id.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "response", value = "[{\"name\":\"Custom_Field_Number\",\"description\":\"loreum ipsum\",\"defaultValue\":\"\",\"isActive\":false,\"fieldType\":\"NUMBER\",\"aliasName\":\"\",\"projectId\":\"\",\"displayName\":\"\",\"createdOn\":1525689773853,\"createdBy\":\"admin\",\"entityType\":\"EXECUTION\",\"fieldOptions\":\"\",\"displayFieldType\":\"Number Field\",\"id\":3,\"customFieldOptionValues\":\"\",\"active\":false},{\"name\":\"Custo_Field_Name_Update\",\"description\":\"loreum ipsum updated.\",\"defaultValue\":\"\",\"isActive\":false,\"fieldType\":\"CHECKBOX\",\"aliasName\":\"\",\"projectId\":\"\",\"displayName\":\"\",\"createdOn\":1525688574630,\"createdBy\":\"admin\",\"entityType\":\"EXECUTION\",\"fieldOptions\":\"\",\"displayFieldType\":\"Checkboxes\",\"id\":2,\"customFieldOptionValues\":{\"5\":\"option1\",\"6\":\"option2\",\"7\":\"option3\",\"8\":\"option4\"},\"active\":false}]")})
    @ApiResponses({@ApiResponse(code = 400, message = "Invalid Request Parameters."),
            @ApiResponse(code = 500, message = "Server error while processing the request."),
            @ApiResponse(code = 401, message = "Unauthorized Request."), @ApiResponse(code = 403, message = "Permission Denied for the request"),
            @ApiResponse(code = 200, message = "Request processed successfully")})
	@GET
	@Path("/byEntityTypeAndProject")
	@AnonymousAllowed
	public Response getCustomFieldsByEntityTypeAndProjectId(
			@ApiParam(value = "Entity Type") @QueryParam("entityType") String entityType,
            @ApiParam(value = "Project Id") @QueryParam("projectId") Long projectId,
			@ApiParam(value = "Issue Id") @QueryParam("issueId") Long issueId) {
	    if(null == projectId) {
            Issue issue = issueManager.getIssueObject(issueId);
            projectId = issue.getProjectId();
        }

        final ApplicationUser user = authContext.getLoggedInUser();
        JSONObject jsonObject = ZephyrUtil.validateUser(user, authContext);
        if (jsonObject != null) {
            return ZephyrUtil.constructErrorResponse(jsonObject, Response.Status.UNAUTHORIZED, null);
        }

        if (!ApplicationConstants.ENTITY_TYPE.TESTSTEP.toString().equalsIgnoreCase(entityType) &&
                !ApplicationConstants.ENTITY_TYPE.EXECUTION.toString().equalsIgnoreCase(entityType)) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "Entity type"),
                    Response.Status.BAD_REQUEST, null);
        }

        if(null != projectId) {
            Response response = validateProjectPermission(user, projectId);
            if(response != null) return response;
        }

		List<CustomField> customFields = customFieldResourceDelegate.getCustomFieldsByEntityTypeAndProject(entityType,projectId);
		List<CustomFieldResponse> customFieldResponses = buildResponse(customFields != null ? customFields.toArray(new CustomField[customFields.size()]) : null);
		return Response.ok().entity(customFieldResponses).build();
	}

	@ApiOperation(value = "Create a CustomField", notes = "Create a CustomField. Name and Description cannot exceed 255 characters.")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{\n" +
            "  \"name\": \"Custo_Field_Name\",\n" +
            "  \"description\": \"loreum ipsum.\",\n" +
            "  \"defaultValue\": \"\",\n" +
            "  \"isActive\": true,\n" +
            "  \"fieldType\": \"CHECKBOX\",\n" +
            "  \"aliasName\": \"\",\n" +
            "  \"projectId\": \"\",\n" +
            "  \"displayName\": \"\",\n" +
            "  \"displayFieldType\": \"Checkboxes\",\n" +
            "  \"entityType\": \"EXECUTION\",\n" +
            "  \"fieldOptions\": [\n" +
            "    \"option1\",\n" +
            "    \"option2\",\n" +
            "    \"option3\",\n" +
            "    \"option4\"\n" +
            "  ]\n" +
            "}"),
            @ApiImplicitParam(name = "response", value = "{\n" +
                    "  \"name\": \"Custo_Field_Name\",\n" +
                    "  \"description\": \"loreum ipsum.\",\n" +
                    "  \"defaultValue\": \"\",\n" +
                    "  \"isActive\": false,\n" +
                    "  \"fieldType\": \"CHECKBOX\",\n" +
                    "  \"aliasName\": \"\",\n" +
                    "  \"projectId\": \"\",\n" +
                    "  \"displayName\": \"\",\n" +
                    "  \"createdOn\": 1525688574630,\n" +
                    "  \"createdBy\": \"admin\",\n" +
                    "  \"entityType\": \"EXECUTION\",\n" +
                    "  \"fieldOptions\": \"\",\n" +
                    "  \"displayFieldType\": \"\",\n" +
                    "  \"id\": 2,\n" +
                    "  \"responseMessage\": \"Custom Field Created Successfully.\",\n" +
                    "  \"customFieldOptionValues\": {\n" +
                    "    \"5\": \"option1\",\n" +
                    "    \"6\": \"option2\",\n" +
                    "    \"7\": \"option3\",\n" +
                    "    \"8\": \"option4\"\n" +
                    "  },\n" +
                    "  \"modifiedBy\": \"\",\n" +
                    "  \"active\": false\n" +
                    "}")})
	@ApiResponses({ @ApiResponse(code = 400, message = "Invalid Request Parameters."),
			@ApiResponse(code = 500, message = "Server error while processing the request."),
            @ApiResponse(code = 400, message = "Bad Request."),
            @ApiResponse(code = 401, message = "Unauthorized Request."),
			@ApiResponse(code = 403, message = "Permission Denied for the request"),
			@ApiResponse(code = 200, message = "Request processed successfully", response = CustomFieldResponse.class, reference = "{\"id\":123456,\"responseMessage\":\"CustomField created successfully\"}") })
	@POST
	@Path("/create")
	@AnonymousAllowed
	public Response createCustomField(CustomFieldRequest customFieldRequest) throws JSONException, UnsupportedEncodingException {
		final ApplicationUser user = authContext.getLoggedInUser();
        Response response;
        CustomFieldResponse customFieldResponse;
		// User permission validation.
        JSONObject jsonObject = ZephyrUtil.validateUser(user, authContext);
        if (jsonObject != null) {
            return ZephyrUtil.constructErrorResponse(jsonObject, Response.Status.UNAUTHORIZED, null);
        }

        response = validateCustomFieldRequest(customFieldRequest);
        if(Objects.nonNull(response)) {
            return response;
        }

        response = validateSystemAdministrator(user);
        if(null !=response) {
            return response;
        }


        // Custom Field Name cannot be same as System Level Fields
        if(SystemSearchConstant.getSystemNames().contains(customFieldRequest.getName())) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.custom.field.system.name.unique",StringUtils.join(SystemSearchConstant.getSystemNames(),", ")),
                    Response.Status.BAD_REQUEST, null);
        }
        boolean isCustomFieldNameUnique = checkCustomFieldNameUniqueness(customFieldRequest.getEntityType(),null,customFieldRequest.getName());
        if(checkForCustomFieldCreationLimit(customFieldRequest.getEntityType()) && isCustomFieldNameUnique) {
            customFieldResponse = customFieldResourceDelegate.createCustomField(customFieldRequest, null);
            searchHandlerManager.refresh();
            return Response.ok(customFieldResponse).build();
        }else {
            jsonObject = new JSONObject();
            if(!isCustomFieldNameUnique) {
                return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.custom.field.name.unique"),
                        Response.Status.BAD_REQUEST, null);
            }
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.custom.field.creation.limit"),
                    Response.Status.BAD_REQUEST, null);
        }
	}

    @ApiOperation(value = "Updates the CustomField information for given custom field id.", notes = "Update the CustomField information")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "request", value = "{\n" +
                    "  \"name\": \"Custo_Field_Name_Update\",\n" +
                    "  \"description\": \"loreum ipsum updated.\"\n" +
                    "}"),
			@ApiImplicitParam(name = "response", value = "{\n" +
                    "  \"name\": \"Custo_Field_Name_Update\",\n" +
                    "  \"description\": \"loreum ipsum updated.\",\n" +
                    "  \"defaultValue\": \"\",\n" +
                    "  \"isActive\": false,\n" +
                    "  \"fieldType\": \"CHECKBOX\",\n" +
                    "  \"aliasName\": \"\",\n" +
                    "  \"projectId\": \"\",\n" +
                    "  \"displayName\": \"\",\n" +
                    "  \"createdOn\": 1525688574630,\n" +
                    "  \"createdBy\": \"admin\",\n" +
                    "  \"entityType\": \"EXECUTION\",\n" +
                    "  \"fieldOptions\": \"\",\n" +
                    "  \"displayFieldType\": \"\",\n" +
                    "  \"id\": 2,\n" +
                    "  \"responseMessage\": \"Custom Field updated successfully.\",\n" +
                    "  \"customFieldOptionValues\": {\n" +
                    "    \"5\": \"option1\",\n" +
                    "    \"6\": \"option2\",\n" +
                    "    \"7\": \"option3\",\n" +
                    "    \"8\": \"option4\"\n" +
                    "  },\n" +
                    "  \"modifiedDate\": 1525689121827,\n" +
                    "  \"modifiedBy\": \"admin\",\n" +
                    "  \"active\": false\n" +
                    "}") })
	@ApiResponses({ @ApiResponse(code = 400, message = "Invalid Request Parameters."),
			@ApiResponse(code = 500, message = "Server error while processing the request."),
			@ApiResponse(code = 401, message = "Unauthorized Request."),
			@ApiResponse(code = 200, message = "Request processed successfully", response = CustomFieldResponse.class) })
	@PUT
	@Path("/{customfieldId}")
	@AnonymousAllowed
	public Response updateCustomField(
			@ApiParam(value = "CustomField Id") @PathParam("customfieldId") Long customFieldId,
            CustomFieldRequest updateCustomFieldRequest) {
		final ApplicationUser user = authContext.getLoggedInUser();
        JSONObject jsonObject = ZephyrUtil.validateUser(user, authContext);
        if (jsonObject != null) {
            return ZephyrUtil.constructErrorResponse(jsonObject, Response.Status.UNAUTHORIZED, null);
        }

        if (StringUtils.isBlank(updateCustomFieldRequest.getName())) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("schedule.update.ID.required", "Name"),
                    Response.Status.BAD_REQUEST, null);
        }

        if (StringUtils.isNotBlank(updateCustomFieldRequest.getName()) && StringUtils.length(updateCustomFieldRequest.getName().trim()) == 0) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("schedule.update.ID.required", "Name"),
                    Response.Status.BAD_REQUEST, null);
        }

        if (StringUtils.isNotBlank(updateCustomFieldRequest.getName()) && StringUtils.length(updateCustomFieldRequest.getName()) > STRING_VALUE_MAX_LENGTH) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("field.limit.exceed.validationError.description", "Name","255"),
                    Response.Status.BAD_REQUEST, null);
        }

        if (StringUtils.isNotBlank(updateCustomFieldRequest.getDescription()) && StringUtils.length(updateCustomFieldRequest.getDescription()) > STRING_VALUE_MAX_LENGTH) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("field.limit.exceed.validationError.description", "Description","255"),
                    Response.Status.BAD_REQUEST, null);
        }

        // Custom Field Name cannot be same as System Level Fields
        if(SystemSearchConstant.getSystemNames().contains(updateCustomFieldRequest.getName())) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.custom.field.system.name.unique",StringUtils.join(SystemSearchConstant.getSystemNames(),", ")),
                    Response.Status.BAD_REQUEST, null);
        }

        if(Objects.isNull(customFieldId)) {
            jsonObject = new JSONObject();
            return constructResponseObject(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "customFieldId"), Status.BAD_REQUEST, null);
        }

        String customFieldName = updateCustomFieldRequest.getName();
        if(StringUtils.isNotBlank(customFieldName) && (StringUtils.contains(customFieldName,"," ) ||
                StringUtils.contains(customFieldName,":" ) ||
                StringUtils.contains(customFieldName,"\"" ) ||
                StringUtils.contains(customFieldName,"\\" ) ||
                StringUtils.contains(customFieldName,"/" ))) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "Custom field name contains special character"),
                    Response.Status.BAD_REQUEST, null);
        }

        CustomField customField = customFieldResourceDelegate.getCustomFieldById(customFieldId);

        if(Objects.isNull(customField)) {
            jsonObject = new JSONObject();
            return constructResponseObject(jsonObject, authContext.getI18nHelper().getText("zephyr.customfield.not.exist"), Status.BAD_REQUEST, null);
        }

        if(null != customField) {
            Response response = validateSystemAdministrator(user);

            if(null !=response) {
                return response;
            }
            if(StringUtils.isNotBlank(updateCustomFieldRequest.getName()) && !updateCustomFieldRequest.getName().equalsIgnoreCase(customField.getName())) {
                if(!checkCustomFieldNameUniqueness(customField.getZFJEntityType(),null,updateCustomFieldRequest.getName())) {
                    jsonObject = new JSONObject();
                    return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.custom.field.name.unique"),
                            Response.Status.BAD_REQUEST, null);
                }
            }
        }

		try {
			CustomFieldResponse customFieldResponse = customFieldResourceDelegate.updateCustomField(customFieldId, updateCustomFieldRequest);
            searchHandlerManager.refresh();
			return Response.ok(customFieldResponse).build();
		} catch (Exception exception) {
			return constructResponseObject(jsonObject,
					authContext.getI18nHelper().getText("zephyr.common.internal.server.error"),
					Status.INTERNAL_SERVER_ERROR, exception);
		}
	}


    @ApiOperation(value = "Delete a CustomField by given id.", notes = "Delete a CustomField ")
    @ApiResponses({ @ApiResponse(code = 400, message = "Invalid Request Parameters."),
            @ApiResponse(code = 500, message = "Server error while processing the request."),
            @ApiResponse(code = 401, message = "Unauthorized Request."),
            @ApiResponse(code = 403, message = "Permission Denied for the request"),
            @ApiResponse(code = 200, message = "Request processed successfully - {custom field id.}") })
    @DELETE
    @Path("/{customFieldId}")
    @AnonymousAllowed
    public Response deleteCustomField(@PathParam("customFieldId") Long customFieldId) {
        final ApplicationUser user = authContext.getLoggedInUser();
        JSONObject jsonObject = ZephyrUtil.validateUser(user, authContext);
        if (jsonObject != null) {
            return ZephyrUtil.constructErrorResponse(jsonObject, Response.Status.UNAUTHORIZED, null);
        }

        if(Objects.isNull(customFieldId)) {
            jsonObject = new JSONObject();
            return constructResponseObject(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "customFieldId"), Status.BAD_REQUEST, null);
        }
        CustomField customField = customFieldResourceDelegate.getCustomFieldById(customFieldId);

        if(Objects.isNull(customField)) {
            jsonObject = new JSONObject();
            return constructResponseObject(jsonObject, authContext.getI18nHelper().getText("zephyr.customfield.not.exist"), Status.BAD_REQUEST, null);
        }

        Response response = validateSystemAdministrator(user);
        if(null !=response) {
            return response;
        }

        customFieldResourceDelegate.deleteCustomField(customFieldId);
        searchHandlerManager.refresh();
        jsonObject = new JSONObject();
        return constructResponseObject(jsonObject, authContext.getI18nHelper().getText("zephyr.customfield.deleted.label",customFieldId), Status.OK, null, "message");
    }

	@ApiOperation(value = "Create a CustomFieldOption for given custom field id.", notes = "Create a CustomFieldOption.")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{\"optionValue\":\"option5\"}"),
            @ApiImplicitParam(name = "response", value = "{\"sequence\":\"0\",\"optionValue\":\"option5\",\"optionId\":\"9\",\"customFieldId\":\"2\",\"isDisabled\":\"false\"}")})
	@ApiResponses({ @ApiResponse(code = 400, message = "Invalid Request Parameters."),
			@ApiResponse(code = 500, message = "Server error while processing the request."),
			@ApiResponse(code = 401, message = "Unauthorized Request."),
			@ApiResponse(code = 403, message = "Permission Denied for the request"),
			@ApiResponse(code = 200, message = "Request processed successfully") })
	@POST
	@Path("/{customFieldId}/customfieldOption")
	@AnonymousAllowed
	public Response createCustomFieldOption(@PathParam("customFieldId") Integer customFieldId, Map<String, String> params) {
		final ApplicationUser user = authContext.getLoggedInUser();
		JSONObject jsonObject;
		// User permission validation.
		Response response = validateUser(user);
		if (response != null)
			return response;

        if(Objects.isNull(customFieldId)) {
            jsonObject = new JSONObject();
            return constructResponseObject(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "customFieldId"), Status.BAD_REQUEST, null);
        }
        CustomField customField = customFieldResourceDelegate.getCustomFieldById(customFieldId.longValue());

        if(Objects.isNull(customField)) {
            jsonObject = new JSONObject();
            return constructResponseObject(jsonObject, authContext.getI18nHelper().getText("zephyr.customfield.not.exist"), Status.BAD_REQUEST, null);
        }

        response = validateSystemAdministrator(user);
        if(null != response) {
            return response;
        }

        String fieldOptionValue = params.get("optionValue") + StringUtils.EMPTY;
        if (StringUtils.isBlank(fieldOptionValue) || StringUtils.length(fieldOptionValue.trim()) == 0) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("schedule.update.ID.required", "Value"),
                    Response.Status.BAD_REQUEST, null);
        }

        if (StringUtils.isNotBlank(fieldOptionValue) && StringUtils.length(fieldOptionValue) > STRING_VALUE_MAX_LENGTH) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("field.limit.exceed.validationError.description", "Option value","255"),
                    Response.Status.BAD_REQUEST, null);
        }
		CustomFieldOption customFieldOption = customFieldResourceDelegate.createCustomFieldOption(customFieldId,params);
		return Response.ok().entity(prepareResponseForCustomFieldOption(customFieldOption)).build();
	}


    @ApiOperation(value = "Update custom field option for given custom field id & customFieldOptionId.", notes = "Update CustomFieldOption value.")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{\"optionValue\":\"option1-updated\"}"),
            @ApiImplicitParam(name = "response", value = "{\"sequence\":\"0\",\"optionValue\":\"option1-updated\",\"optionId\":\"5\",\"customFieldId\":\"2\",\"isDisabled\":\"false\"}")})
    @ApiResponses({ @ApiResponse(code = 400, message = "Invalid Request Parameters."),
            @ApiResponse(code = 500, message = "Server error while processing the request."),
            @ApiResponse(code = 401, message = "Unauthorized Request."),
            @ApiResponse(code = 403, message = "Permission Denied for the request"),
            @ApiResponse(code = 200, message = "Request processed successfully") })
    @PUT
	@Path("/customfieldOption/{customFieldOptionId}")
	@AnonymousAllowed
	public Response updateCustomFieldOption(@PathParam(value = "customFieldOptionId") Integer customFieldOptionId,
                                            Map<String, String> params) {
		final ApplicationUser user = authContext.getLoggedInUser();
		JSONObject jsonObject;
		// User permission validation.
		Response response = validateUser(user);
		if (response != null)
			return response;

        if(Objects.isNull(customFieldOptionId)) {
            jsonObject = new JSONObject();
            return constructResponseObject(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "customFieldOptionId"), Status.BAD_REQUEST, null);
        }
        String fieldOptionValue = params.get("optionValue") + StringUtils.EMPTY;
        if (StringUtils.isBlank(fieldOptionValue) || StringUtils.length(fieldOptionValue.trim()) == 0) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("schedule.update.ID.required", "Value"),
                    Response.Status.BAD_REQUEST, null);
        }

        if (StringUtils.isNotBlank(fieldOptionValue) && StringUtils.length(fieldOptionValue.trim()) > STRING_VALUE_MAX_LENGTH) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("field.limit.exceed.validationError.description", "Option value","255"),
                    Response.Status.BAD_REQUEST, null);
        }

        CustomFieldOption customFieldOption = customFieldResourceDelegate.getCustomFieldOptionById(customFieldOptionId);

        if(Objects.isNull(customFieldOption)) {
            jsonObject = new JSONObject();
            return constructResponseObject(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "customFieldOption"), Status.BAD_REQUEST, null);
        }

        response = validateSystemAdministrator(user);
        if(null != response) {
            return response;
        }

		CustomFieldOption cfResponse = customFieldResourceDelegate.updateCustomFieldOption(customFieldOptionId, params);
		return Response.ok().entity(prepareResponseForCustomFieldOption(cfResponse)).build();
	}

    @ApiOperation(value = "Delete a CustomFieldOption by given id.", notes = "Delete a CustomFieldOption.")
    @ApiResponses({ @ApiResponse(code = 400, message = "Invalid Request Parameters."),
            @ApiResponse(code = 500, message = "Server error while processing the request."),
            @ApiResponse(code = 401, message = "Unauthorized Request."),
            @ApiResponse(code = 403, message = "Permission Denied for the request"),
            @ApiResponse(code = 200, message = "Request processed successfully - {custom field option id.}") })
    @DELETE
    @Path("/customfieldOption/{customFieldOptionId}")
    @AnonymousAllowed
    public Response deleteCustomFieldOption(@PathParam(value = "customFieldOptionId") Integer customFieldOptionId) {
        final ApplicationUser user = authContext.getLoggedInUser();
        JSONObject jsonObject;
        // User permission validation.
        Response response = validateUser(user);
        if (response != null)
            return response;

        if(Objects.isNull(customFieldOptionId)) {
            jsonObject = new JSONObject();
            return constructResponseObject(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "customFieldOptionId"), Status.BAD_REQUEST, null);
        }

        CustomFieldOption customFieldOption = customFieldResourceDelegate.getCustomFieldOptionById(customFieldOptionId);

        if(Objects.isNull(customFieldOption)) {
            jsonObject = new JSONObject();
            return constructResponseObject(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "customFieldOptionId"), Status.BAD_REQUEST, null);
        }

        response = validateSystemAdministrator(user);
        if(null != response) {
            return response;
        }

        if(isLastCustomFieldOptionForCustomField(customFieldOption)) {
            jsonObject = new JSONObject();
            return constructResponseObject(jsonObject, "Unable to delete the last custom field option value. Atleast one option value is required.", Status.BAD_REQUEST, null);
        }
        customFieldResourceDelegate.deleteCustomFieldOption(customFieldOptionId);
        searchHandlerManager.refresh();
        jsonObject = new JSONObject();
        return constructResponseObject(jsonObject, authContext.getI18nHelper().getText("zephyr.customfield.option.deleted.label", customFieldOptionId), Status.OK, null,"message");
    }


    @ApiOperation(value = "Enable or Disable customField for a project", notes = "Disable customField for a project")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{\"optionValue\":\"option1-updated\"}"),
            @ApiImplicitParam(name = "response", value = "{\"message\":\"Custom Field enabled successfully - {custom field name}.\"}")})
    @ApiResponses({ @ApiResponse(code = 400, message = "Invalid Request Parameters."),
            @ApiResponse(code = 500, message = "Server error while processing the request."),
            @ApiResponse(code = 401, message = "Unauthorized Request."),
            @ApiResponse(code = 403, message = "Permission Denied for the request"),
            @ApiResponse(code = 200, message = "Request processed successfully") })
    @PUT
    @Path("/{customFieldId}/{projectId}")
    @AnonymousAllowed
    public Response disableCustomField(@PathParam("customFieldId") Long customFieldId,
                                       @PathParam("projectId") Long projectId,
                                       @ApiParam(value = "enable", defaultValue = "true") @QueryParam("enable") String enable) {
        final ApplicationUser user = authContext.getLoggedInUser();
        JSONObject jsonObject = ZephyrUtil.validateUser(user, authContext);
        if (jsonObject != null) {
            return ZephyrUtil.constructErrorResponse(jsonObject, Response.Status.UNAUTHORIZED, null);
        }

        if(Objects.isNull(customFieldId)) {
            jsonObject = new JSONObject();
            return constructResponseObject(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "customFieldId"), Status.BAD_REQUEST, null);
        }

        if(StringUtils.isBlank(enable)) {
            jsonObject = new JSONObject();
            return constructResponseObject(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "enable query param is missing."), Status.BAD_REQUEST, null);
        }

        if(StringUtils.isNotBlank(enable) && !validateEnableFlagStatus(enable)) {
            jsonObject = new JSONObject();
            return constructResponseObject(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "enable query param value."), Status.BAD_REQUEST, null);
        }

        if(Objects.isNull(projectId)) {
            jsonObject = new JSONObject();
            return constructResponseObject(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "projectId"), Status.BAD_REQUEST, null);
        }

        CustomField customField = customFieldResourceDelegate.getCustomFieldById(customFieldId);

        if(Objects.isNull(customField)) {
            jsonObject = new JSONObject();
            return constructResponseObject(jsonObject, authContext.getI18nHelper().getText("zephyr.customfield.not.exist"), Status.BAD_REQUEST, null);
        }

        Project project = projectManager.getProjectObj(projectId);

        if(Objects.isNull(project)) {
            jsonObject = new JSONObject();
            return constructResponseObject(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "project"), Status.BAD_REQUEST, null);
        }

        Response response = validateProjectPermission(user, projectId);
        if(response != null) return response;

        response = validateProjectAdministrator(user);
        if( response != null) {
            return response;
        }

        Boolean enableDisableFlag = Boolean.valueOf(enable);
        customFieldResourceDelegate.enableOrDisableCustomFieldForProject(projectId,enableDisableFlag,customField);
        searchHandlerManager.refresh();
        jsonObject = new JSONObject();
        String errorMessage;
        if(enableDisableFlag) {
            errorMessage = authContext.getI18nHelper().getText("zephyr.customfield.enabled.label",customField.getName());
        }else {
            errorMessage = authContext.getI18nHelper().getText("zephyr.customfield.disabled.label",customField.getName());
        }
        searchHandlerManager.refresh();
        return constructResponseObject(jsonObject, errorMessage, Status.OK, null, "message");
    }

    @ApiOperation(value = "Get customFields for given project & entity type (EXECUTION | TESTSTEP)", notes = "Get customFields for given project & entity type (EXECUTION | TESTSTEP)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "response", value = "[{\"name\":\"Custo_Field_Name_Update\",\"description\":\"loreum ipsum updated.\",\"defaultValue\":\"\",\"isActive\":true,\"fieldType\":\"CHECKBOX\",\"aliasName\":\"\",\"projectId\":\"\",\"displayName\":\"\",\"createdOn\":1525688574630,\"createdBy\":\"admin\",\"entityType\":\"EXECUTION\",\"fieldOptions\":\"\",\"displayFieldType\":\"Checkboxes\",\"id\":2,\"customFieldOptionValues\":{\"5\":\"option1-updated\",\"6\":\"option2\",\"7\":\"option3\",\"8\":\"option4\",\"9\":\"option5\"},\"active\":true}]")})
    @ApiResponses({ @ApiResponse(code = 400, message = "Invalid Request Parameters."),
            @ApiResponse(code = 500, message = "Server error while processing the request."),
            @ApiResponse(code = 401, message = "Unauthorized Request."),
            @ApiResponse(code = 403, message = "Permission Denied for the request"),
            @ApiResponse(code = 200, message = "Request processed successfully") })
    @GET
    @Path("/globalCustomFieldsByEntityTypeAndProject")
    @AnonymousAllowed
    public Response getGlobalCustomFieldsByEntityTypeAndProjectId(
            @ApiParam(value = "Entity Type") @QueryParam("entityType") String entityType,
            @ApiParam(value = "Project Id") @QueryParam("projectId") Long projectId) {
        final ApplicationUser user = authContext.getLoggedInUser();
        JSONObject jsonObject = ZephyrUtil.validateUser(user, authContext);
        if (jsonObject != null) {
            return ZephyrUtil.constructErrorResponse(jsonObject, Response.Status.UNAUTHORIZED, null);
        }

        if (!ApplicationConstants.ENTITY_TYPE.TESTSTEP.toString().equalsIgnoreCase(entityType) &&
                !ApplicationConstants.ENTITY_TYPE.EXECUTION.toString().equalsIgnoreCase(entityType)) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "Entity type"),
                    Response.Status.BAD_REQUEST, null);
        }

        if(null != projectId) {
            Response response = validateProjectPermission(user, projectId);
            if(response != null) return response;
        }

        CustomField[] customFields = customFieldResourceDelegate.getGlobalCustomFieldsByEntityTypeAndProjectId(entityType,projectId);
        List<CustomFieldResponse> customFieldResponses = buildResponse(customFields);
        return Response.ok().entity(customFieldResponses).build();
    }

    /**
	 * Prepare the user response for the request.
	 * @param customFields
	 * @return
	 */
	private List<CustomFieldResponse> buildResponse(CustomField[] customFields) {
		List<CustomFieldResponse> customFieldResponses = new ArrayList<CustomFieldResponse>();
		try {
			for (CustomField customField : customFields) {
				CustomFieldResponse customFieldResponse = new CustomFieldResponse();
				customFieldResponse.setId(customField.getID());
				customFieldResponse.setName(customField.getName());
				customFieldResponse.setDescription(null != customField.getDescription() ? customField.getDescription() :
                        StringUtils.EMPTY);
				customFieldResponse.setDefaultValue(null != customField.getDefaultValue() ? customField.getDefaultValue() :
                        StringUtils.EMPTY);
				customFieldResponse.setActive(customField.getIsActive());
                customFieldResponse.setEntityType(customField.getZFJEntityType());
				customFieldResponse.setFieldType(customField.getCustomFieldType());
				customFieldResponse.setDisplayName(customField.getDisplayName());
				CustomFieldOption[] options = customFieldResourceDelegate.getCustomFieldOptions(customField.getID());
				if (options != null && options.length > 0) {
					Map<Integer,String> optionValues = new TreeMap<>();
					for (CustomFieldOption customFieldOption : options) {
						optionValues.put(customFieldOption.getID(),customFieldOption.getOptionValue());
					}
					customFieldResponse.setCustomFieldOptionValues(optionValues);
				}
				customFieldResponse.setAliasName(null != customField.getAliasName() ? customField.getAliasName() :
                        StringUtils.EMPTY);
				customFieldResponse.setCreatedOn(customField.getCreatedOn());
				customFieldResponse.setCreatedBy(customField.getCreatedBy());
				customFieldResponse.setDisplayFieldType(StringUtils.isNotBlank(customField.getDisplayFieldType()) ? customField.getDisplayFieldType() : StringUtils.EMPTY);
				customFieldResponses.add(customFieldResponse);
			}
		} catch (Exception e) {
		}
		return customFieldResponses;
	}

    /**
     * Prepare the error response object and sends to the upstream.
     * @param jsonObject
     * @param errorMessage
     * @param status
     * @param exception
     * @return
     */
	private Response constructResponseObject(JSONObject jsonObject, String errorMessage, Status status,
                                             Exception exception) {
		try {
			String finalErrorMessage = String.format(ERROR_LOG_MESSAGE, status.getStatusCode(), status, errorMessage);
			log.error(finalErrorMessage);
			jsonObject.put("error", errorMessage);
			return Response.status(status).entity(jsonObject != null ? jsonObject.toString() : finalErrorMessage)
					.cacheControl(ZephyrCacheControl.never()).build();
		} catch (JSONException e) {
			log.error("Error while constructing the error response");
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

    /**
     *
     * @param jsonObject
     * @param errorMessage
     * @param status
     * @param exception
     * @param messageKey
     * @return
     */
    private Response constructResponseObject(JSONObject jsonObject, String errorMessage, Status status,
                                             Exception exception, String messageKey) {
        try {
            String finalErrorMessage = String.format(ERROR_LOG_MESSAGE, status.getStatusCode(), status, errorMessage);
            log.error(finalErrorMessage, exception);
            jsonObject.put(messageKey, errorMessage);
            return Response.status(status).entity(jsonObject != null ? jsonObject.toString() : finalErrorMessage)
                    .cacheControl(ZephyrCacheControl.never()).build();
        } catch (JSONException e) {
            log.error("Error while constructing the error response");
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Validate the logged in user & prepare response for the upstream.
     * @param user
     * @return
     */
	private Response validateUser(final ApplicationUser user) {
		JSONObject jsonObject = new JSONObject();
		try {
			if (user == null && !JiraUtil.hasAnonymousPermission(user)) {
				jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
				return constructResponseObject(jsonObject,
						authContext.getI18nHelper().getText("zephyr.common.logged.user.error"), Status.UNAUTHORIZED,
						null);
			}
		} catch (JSONException e) {
			log.error("Error occurred while getting count.", e);
			return Response.status(Status.BAD_REQUEST).build();
		}
		return null;
	}

    /**
     * Prepare the response object and sends to the upstream.
     * @param customFieldOption
     * @return
     */
    private Map<String, String> prepareResponseForCustomFieldOption(CustomFieldOption customFieldOption) {
        Map<String,String> response = new HashMap<>();
        response.put(CUSTOM_FIELD_OPTION_ID, customFieldOption.getID() + StringUtils.EMPTY);
        response.put(CUSTOM_FIELD_ID, customFieldOption.getCustomField().getID() + StringUtils.EMPTY);
        response.put(OPTION_VALUE, customFieldOption.getOptionValue());
        response.put(IS_DISABLED,customFieldOption.getIsDisabled() + StringUtils.EMPTY);
        response.put(SEQUENCE,customFieldOption.getSequence() + StringUtils.EMPTY);

        return response;
    }

    /**
     * validate project level permission.
     * @param user
     * @param projectId
     * @return
     */
    private Response validateProjectPermission(ApplicationUser user,  Long projectId) {
        JSONObject jsonObject = new JSONObject();

        Project project = projectManager.getProjectObj(projectId);
        // validate for a valid Project
        if (Objects.isNull(project)) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "projectId ", "");
            log.error(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,errorMessage));
            return constructResponseObject(jsonObject, errorMessage, Status.BAD_REQUEST, null);
        }
        // checking the project browse permissions for the user
        if (!JiraUtil.hasBrowseProjectPermission(project, user)) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.project.permission.error", "Project", String.valueOf(project.getName()));
            log.error(String.format(ERROR_LOG_MESSAGE, Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
            return constructResponseObject(jsonObject, errorMessage, Status.FORBIDDEN, null);
        }

        return null;
    }

    /**
     *
     * @param user
     * @return
     */
    private Response validateSystemAdministrator(ApplicationUser user) {
        if(!ComponentAccessor.getGlobalPermissionManager().hasPermission(GlobalPermissionKey.ADMINISTER,user) &&
                !ComponentAccessor.getGlobalPermissionManager().hasPermission(GlobalPermissionKey.SYSTEM_ADMIN,user)) {
            JSONObject jsonObject = new JSONObject();
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.project.permission.error", "Project", user.getDisplayName());
            log.error(String.format(ERROR_LOG_MESSAGE, Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
            return constructResponseObject(jsonObject, errorMessage, Status.FORBIDDEN, null);
        }
        return null;
    }


    /**
     *
     * @param user
     * @return
     */
    private Response validateProjectAdministrator(ApplicationUser user) {

        if(!ComponentAccessor.getGlobalPermissionManager().hasPermission(GlobalPermissionKey.ADMINISTER,user) &&
                !ComponentAccessor.getGlobalPermissionManager().hasPermission(Permissions.PROJECT_ADMIN,user)) {
            JSONObject jsonObject = new JSONObject();
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.project.permission.error", "Project", user.getDisplayName());
            log.error(String.format(ERROR_LOG_MESSAGE, Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
            return constructResponseObject(jsonObject, errorMessage, Status.FORBIDDEN, null);
        }
        return null;
    }

    /**
     * This method will check the existing custom field count & will validate with the specified custom field creation limit.
     * @param entityType
     * @return
     */
    private boolean checkForCustomFieldCreationLimit(String entityType) {
        Integer count = customFieldResourceDelegate.getCustomFieldCount(entityType);

        if(ApplicationConstants.ENTITY_TYPE.EXECUTION.name().equalsIgnoreCase(entityType)) {
            if(count < ApplicationConstants.EXECUTION_CUSTOM_FIELD_GLOBAL_LEVEL) {
                return true;
            } else {
                return false;
            }
        } else if(ApplicationConstants.ENTITY_TYPE.TESTSTEP.name().equalsIgnoreCase(entityType)) {
            if(count < ApplicationConstants.TEST_STEP_CUSTOM_FIELD_GLOBAL_LEVEL) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }


    /**
     *
     * @param customFieldOption
     * @return
     */
    private boolean isLastCustomFieldOptionForCustomField(CustomFieldOption customFieldOption) {
        CustomFieldOption[] customFieldOptions = customFieldResourceDelegate.getCustomFieldOptions(customFieldOption.getCustomField().getID());

        if(null != customFieldOptions && customFieldOptions.length > 0) {
            if(customFieldOptions.length == 1 && customFieldOption.getID() == customFieldOptions[0].getID()) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    /**
     * This method will validate the custom field name uniqueness.
     * If project id is provided then the check will done across project.
     * If project id is null then the check will done across global level.
     * @param entityType
     * @param projectId
     * @param customFieldName
     * @return
     */
    private boolean checkCustomFieldNameUniqueness(String entityType, Long projectId, String customFieldName) {
       return customFieldResourceDelegate.checkCustomFieldNameUniqueness(entityType,projectId,customFieldName);
    }

    /**
     * This method will prepare the response for custom field created at the project level.
     * @param successList
     * @param projectPermissionFailedList
     * @param customFieldCreationLimitExceedList
     * @param customFieldValidationFailedList
     * @return
     */
    private JSONObject prepareResponseForProjectLevelCustomField(List<String> successList, List<String> projectPermissionFailedList, List<String> customFieldCreationLimitExceedList, List<String> customFieldValidationFailedList) {

        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("Successfully created for project(s) : ", StringUtils.join(successList,COMMA));
            jsonObject.put("No zephyr permission for project(s) / project doesn't exist : ", StringUtils.join(projectPermissionFailedList,COMMA));
            jsonObject.put("Project level custom field creation limit exceeded for project(s) : ", StringUtils.join(customFieldCreationLimitExceedList,COMMA));
            jsonObject.put("Custom field validation error for project(s) : ", StringUtils.join(customFieldValidationFailedList,COMMA));
        } catch (JSONException exception) {
            log.error("Error occurred while preparing the response for custom fields.");
        }
        return jsonObject;
    }

    /**
     * Validation for custom field.
     * @param customFieldRequest
     * @return
     */
    private Response validateCustomFieldRequest(CustomFieldRequest customFieldRequest) {

        JSONObject jsonObject;

        if (!ApplicationConstants.ENTITY_TYPE.TESTSTEP.toString().equalsIgnoreCase(customFieldRequest.getEntityType()) &&
                !ApplicationConstants.ENTITY_TYPE.EXECUTION.toString().equalsIgnoreCase(customFieldRequest.getEntityType())) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "Entity type"),
                    Response.Status.BAD_REQUEST, null);
        }

        if (StringUtils.isBlank(customFieldRequest.getName()) || StringUtils.length(customFieldRequest.getName().trim()) == 0) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("schedule.update.ID.required", "Name"),
                    Response.Status.BAD_REQUEST, null);
        }

        if (StringUtils.length(customFieldRequest.getName()) > STRING_VALUE_MAX_LENGTH) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("field.limit.exceed.validationError.description", "Name","255"),
                    Response.Status.BAD_REQUEST, null);
        }
        String customFieldName = customFieldRequest.getName();
        if(StringUtils.isNotBlank(customFieldName) && (StringUtils.contains(customFieldName,"," ) ||
                StringUtils.contains(customFieldName,":" ) ||
                StringUtils.contains(customFieldName,"\"" ) ||
                StringUtils.contains(customFieldName,"\\" ) ||
                StringUtils.contains(customFieldName,"/" ))) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "Custom field name contains special character"),
                    Response.Status.BAD_REQUEST, null);
        }

        if (StringUtils.isNotBlank(customFieldRequest.getDescription()) && StringUtils.length(customFieldRequest.getDescription()) > STRING_VALUE_MAX_LENGTH) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("field.limit.exceed.validationError.description", "Description","255"),
                    Response.Status.BAD_REQUEST, null);
        }

        if(CollectionUtils.isNotEmpty(customFieldRequest.getFieldOptions())) {

            for(String fieldOptionValue : customFieldRequest.getFieldOptions()) {
                if (StringUtils.isNotBlank(fieldOptionValue) && StringUtils.length(fieldOptionValue) > STRING_VALUE_MAX_LENGTH) {
                    jsonObject = new JSONObject();
                    return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("field.limit.exceed.validationError.description", "Option value","255"),
                            Response.Status.BAD_REQUEST, null);
                }else if(StringUtils.isBlank(fieldOptionValue)) {
                    jsonObject = new JSONObject();
                    return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "Option value is empty or blank"),
                            Response.Status.BAD_REQUEST, null);
                }
             }
        }
        if (StringUtils.isBlank(customFieldRequest.getFieldType()) ) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("schedule.update.ID.required", "Custom Field Type"),
                    Response.Status.BAD_REQUEST, null);
        }

        if (!ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.containsKey(StringUtils.upperCase(customFieldRequest.getFieldType()))) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "Custom Field Type"),
                    Response.Status.BAD_REQUEST, null);
        }

        if(ApplicationConstants.CUSTOM_FIELD_TYPE_MAP.get(StringUtils.upperCase(customFieldRequest.getFieldType())).equalsIgnoreCase(ApplicationConstants.RADIO_BUTTON) ||
                ApplicationConstants.CUSTOM_FIELD_TYPE_MAP.get(StringUtils.upperCase(customFieldRequest.getFieldType())).equalsIgnoreCase(ApplicationConstants.CHECKBOX) ||
                ApplicationConstants.CUSTOM_FIELD_TYPE_MAP.get(StringUtils.upperCase(customFieldRequest.getFieldType())).equalsIgnoreCase(ApplicationConstants.SINGLE_SELECT) ||
                ApplicationConstants.CUSTOM_FIELD_TYPE_MAP.get(StringUtils.upperCase(customFieldRequest.getFieldType())).equalsIgnoreCase(ApplicationConstants.MULTI_SELECT)
                ) {
            if(CollectionUtils.isEmpty(customFieldRequest.getFieldOptions())) {
                jsonObject = new JSONObject();
                return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("schedule.update.ID.required", "Atleast one option value is required for custom field type" +
                                " (RADIO_BUTTON | CHECKBOX | SINGLE_SELECT | MULTI_SELECT)"),
                        Response.Status.BAD_REQUEST, null);
            }
        }
        return null;
    }


    /**
     *
     * @param enable
     * @return
     */
    private boolean validateEnableFlagStatus(String enable) {
        Pattern queryLangPattern = Pattern.compile("true|false", Pattern.CASE_INSENSITIVE);
        Matcher matcher = queryLangPattern.matcher(enable);
        return matcher.matches();
    }


    @XmlRootElement
	@ApiModel("customFieldResponse")
    @JsonInclude(JsonInclude.Include.NON_NULL)
	public static class CustomFieldResponse extends CustomFieldRequest {
		@XmlElement
		@ApiModelProperty
		private long id;

		@XmlElement
		@ApiModelProperty
		private String responseMessage;

        @XmlElement(nillable = true)
        @ApiModelProperty(required = false)
        private Map<Integer,String> customFieldOptionValues;

        @XmlElement
        private Date modifiedDate;

        @XmlElement
        private String modifiedBy;

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public String getResponseMessage() {
			return responseMessage;
		}

		public void setResponseMessage(String responseMessage) {
			this.responseMessage = responseMessage;
		}

        public Map<Integer, String> getCustomFieldOptionValues() {
            return customFieldOptionValues;
        }

        public void setCustomFieldOptionValues(Map<Integer, String> customFieldOptionValues) {
            this.customFieldOptionValues = customFieldOptionValues;
        }

        public Date getModifiedDate() {
            return modifiedDate;
        }

        public void setModifiedDate(Date modifiedDate) {
            this.modifiedDate = modifiedDate;
        }

        public String getModifiedBy() {
            return modifiedBy;
        }

        public void setModifiedBy(String modifiedBy) {
            this.modifiedBy = modifiedBy;
        }
    }

	@XmlRootElement
	@ApiModel("customFieldRequest")
	public static class CustomFieldRequest {

		@XmlElement(nillable = false)
		@ApiModelProperty(required = true)
		private String name;

		@XmlElement(nillable = true)
		@ApiModelProperty(required = false)
		private String description;

		@XmlElement(nillable = true)
		@ApiModelProperty(required = false)
		private String defaultValue;

		@XmlElement(nillable = true)
		@ApiModelProperty(required = false)
		private boolean isActive;

		@XmlElement(nillable = true)
		@ApiModelProperty(required = false)
		private String fieldType;

		@XmlElement(nillable = true)
		@ApiModelProperty(required = false)
		private String aliasName;

		@XmlElement(nillable = true)
		@ApiModelProperty(required = false)
		private String projectId;

		@XmlElement(nillable = true)
		@ApiModelProperty(required = false)
		private String displayName;

		@XmlElement(nillable = true)
		@ApiModelProperty(required = false)
		private Date createdOn;

		@XmlElement(nillable = true)
		@ApiModelProperty(required = false)
		private String createdBy;

		@XmlElement(nillable = true)
		@ApiModelProperty(required = false)
		private String entityType;

		@XmlElement(nillable = true)
		@ApiModelProperty(required = false)
		private List<String> fieldOptions;

        @XmlElement(nillable = true)
        @ApiModelProperty(required = false)
        private String displayFieldType;

		CustomFieldRequest() {
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getDefaultValue() {
			return defaultValue;
		}

		public void setDefaultValue(String defaultValue) {
			this.defaultValue = defaultValue;
		}

		public boolean isActive() {
			return isActive;
		}

		public void setActive(boolean isActive) {
			this.isActive = isActive;
		}

		public String getFieldType() {
			return fieldType;
		}

		public void setFieldType(String fieldType) {
			this.fieldType = fieldType;
		}

		public String getAliasName() {
			return aliasName;
		}

		public void setAliasName(String aliasName) {
			this.aliasName = aliasName;
		}

		public String getProjectId() {
			return projectId;
		}

		public void setProjectId(String projectId) {
			this.projectId = projectId;
		}

		public String getDisplayName() {
			return displayName;
		}

		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}

		public Date getCreatedOn() {
			return createdOn;
		}

		public void setCreatedOn(Date createdOn) {
			this.createdOn = createdOn;
		}

		public String getCreatedBy() {
			return createdBy;
		}

		public void setCreatedBy(String createdBy) {
			this.createdBy = createdBy;
		}

		public String getEntityType() {
			return entityType;
		}

		public void setEntityType(String entityType) {
			this.entityType = entityType;
		}

		public List<String> getFieldOptions() {
			return fieldOptions;
		}

		public void setFieldOptions(List<String> fieldOptions) {
			this.fieldOptions = fieldOptions;
		}

        public String getDisplayFieldType() {
            return displayFieldType;
        }

        public void setDisplayFieldType(String displayFieldType) {
            this.displayFieldType = displayFieldType;
        }
    }

}
