package com.thed.zephyr.je.rest;

import java.util.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.sun.jersey.spi.container.ResourceFilters;
import com.thed.zephyr.je.permissions.model.PermissionType;
import com.thed.zephyr.je.permissions.service.ZephyrPermissionManager;
import com.thed.zephyr.je.rest.filter.ZFJApiFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.usercompatibility.UserCompatibilityHelper;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.thed.zephyr.je.model.Cycle;
import com.thed.zephyr.je.model.Folder;
import com.thed.zephyr.je.model.FolderCycleMapping;
import com.thed.zephyr.je.rest.delegate.FolderResourceDelegate;
import com.thed.zephyr.je.service.CycleManager;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.ZephyrCacheControl;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Api(value = "Folder Resource API(s)", description = "Following section describes rest resources pertaining to Folder Resource")
@Path("folder")
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@ResourceFilters(ZFJApiFilter.class)
public class FolderResource {
	
	protected final Logger log = Logger.getLogger(FolderResource.class);
	private static final String ERROR_LOG_MESSAGE = "[Error] [Error code: %s %s Error Message : %s]";
	
	private final JiraAuthenticationContext authContext;
	private final FolderResourceDelegate folderResourceDelegate;
	private final ProjectManager projectManager;
    private final CycleManager cycleManager;
    private final VersionManager versionManager;
    private final ZephyrPermissionManager zephyrPermissionManager;
	
	public FolderResource(JiraAuthenticationContext authContext, FolderResourceDelegate folderResourceDelegate, ProjectManager projectManager, CycleManager cycleManager,
                          VersionManager versionManager,ZephyrPermissionManager zephyrPermissionManager) {
		this.authContext = authContext;
		this.folderResourceDelegate = folderResourceDelegate;
		this.projectManager = projectManager;
		this.cycleManager = cycleManager;
		this.versionManager = versionManager;
		this.zephyrPermissionManager = zephyrPermissionManager;
	}

	
	@ApiOperation(value = "Create a folder under cycle", notes = "Create a folder under cycle")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "request", value = "{\"cycleId\":123456,\"name\":\"testfolder\",\"description\":\"created test folder for this cycle\",\"projectId\":10000,\"versionId\":-1,\"clonedFolderId\":1}"),
			@ApiImplicitParam(name = "response", value = "{\"id\":123456,\"responseMessage\":\"Folder testfolder created successfully\"} OR {\"jobProgressToken\":\"34234-324234-2342342131\"}")})
	@ApiResponses({@ApiResponse(code = 400, message = "Invalid Request Parameters."),
    	@ApiResponse(code = 500, message = "Server error while processing the request."),
    	@ApiResponse(code = 401, message = "Unauthorized Request."), @ApiResponse(code = 403, message = "Permission Denied for the request"),
    	@ApiResponse(code = 200, message = "Request processed successfully", response=FolderResponse.class, reference="{\"id\":123456,\"responseMessage\":\"Folder testfolder created successfully\"}")})
	@POST
	@Path("/create")
	public Response createFolder(FolderRequest folderRequest) {
		final ApplicationUser user = authContext.getLoggedInUser();
		JSONObject jsonObject = new JSONObject();
		Boolean isFolderNameUpdated = false;
		Map<String, String> valueHolder = new HashMap<>();
		//User permission validation.
		Response response = validateUser(user);
		if(response != null) return response;
		
		Long cycleId = folderRequest.getCycleId();
		String folderName = folderRequest.getName();
		Long projectId = folderRequest.getProjectId() != null ? folderRequest.getProjectId() : 0L;
   		Long versionId = folderRequest.getVersionId() != null ? folderRequest.getVersionId() : 0L;
		
		try {			
			if(Objects.isNull(cycleId) || cycleId.equals(0L)) {
	    		return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.cycle.folder.invalid.cycleId", cycleId), Status.BAD_REQUEST, null);
	    	}
			if(cycleId.equals(ApplicationConstants.AD_HOC_CYCLE_ID_LONG)) {
	    		return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("folder.adhoc.cycle.error.message"), Status.BAD_REQUEST, null);
	    	}
			if(StringUtils.isBlank(folderName) || !folderName.matches("^(.){1,210}$")) {
	    		return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.cycle.folder.empty.folderName"), Status.BAD_REQUEST, null);
	    	}
			
			//project permission validation for the user
			response = validateProjectPermission(user, Long.valueOf(cycleId), projectId, valueHolder);
			if(response != null) return response;
			if(!Objects.isNull(versionId) && !(versionId.intValue() == ApplicationConstants.UNSCHEDULED_VERSION_ID)) { //Version validation
				Version version = versionManager.getVersion(versionId);
				if(Objects.isNull(version)) {
					return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("project.version.not.exist"), Status.BAD_REQUEST, null);
				}
				valueHolder.put("versionName", version.getName());
			} else if(Objects.isNull(versionId)) {
				return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "VersionId"), Status.BAD_REQUEST, null);
			} else {
				String versionName = authContext.getI18nHelper().getText("zephyr.je.version.unscheduled");
				valueHolder.put("versionName", versionName);
			}

            String loggedInUser = UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext));
            if(! verifyFolderLevelOperationPermission(user,projectId, PermissionType.ZEPHYR_CREATE_CYCLE.toString())) {

                String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
                JSONObject errorObject = new JSONObject();
                return constructErrorResponse(errorObject, errorMessage, Status.FORBIDDEN, null);

            }
            //validating clone folder permission check
            if(null != folderRequest.getClonedFolderId()) {
                String errorMessage;
                JSONObject errorObject;

                if(! verifyFolderLevelOperationPermission(user,projectId, PermissionType.ZEPHYR_CREATE_EXECUTION.toString())) {
                    errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error") + " Missing " +
                            PermissionType.ZEPHYR_CREATE_EXECUTION.toString() + " permission.";
                    errorObject = new JSONObject();
                    return constructErrorResponse(errorObject, errorMessage, Status.FORBIDDEN, null);
                }
            }

			//Validation to check whether folder name in unique within the cyle.
			response = folderNameUniquenessValidation(projectId, versionId, Long.valueOf(cycleId), folderName);
			if(response != null) return response;
			isFolderNameUpdated = true;
			
            //Delegating to create a folder.
			return folderResourceDelegate.createFolder(folderRequest, loggedInUser, valueHolder);
		} catch(Exception exception) {
			if(isFolderNameUpdated)
				folderResourceDelegate.rollbackFolderNameFromCache(projectId, versionId, Long.valueOf(cycleId), folderName);
			return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.internal.server.error"), Status.INTERNAL_SERVER_ERROR, exception);
		}
	}


    @ApiOperation(value = "Update a folder information", notes = "Update a folder information")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "request", value = "{\"folderId\":123456,\"name\":\"testfolder1\",\"description\":\"updated test folder for this cycle\"}"),
		@ApiImplicitParam(name = "response", value = "{\"id\":123456,\"responseMessage\":\"Folder testfolder1 updated successfully\"}")})
	@ApiResponses({@ApiResponse(code = 400, message = "Invalid Request Parameters."),
    	@ApiResponse(code = 500, message = "Server error while processing the request."),
    	@ApiResponse(code = 401, message = "Unauthorized Request."),
    	@ApiResponse(code = 200, message = "Request processed successfully", response=FolderResponse.class, reference="{\"id\":123456,\"responseMessage\":\"Folder testfolder1 updated successfully\"}")})
	@PUT
	@Path("/{folderId}")
	public Response updateFolder(@ApiParam(value="Folder Id") @PathParam("folderId") Long folderId, Map<String,String> params) {
		final ApplicationUser user = authContext.getLoggedInUser();	
		JSONObject jsonObject = new JSONObject();
		Map<String, String> valueHolder = new HashMap<>();
		Boolean isFolderNameUpdated = false;
		//User permission validation.
		Response response = validateUser(user);
		if(response != null) return response;
		//Parameters empty validation.
		response = validateIsParamsEmpty(params);
		if(response != null) return response;
		String folderName = params.get("name");
		String folderDescription = params.get("description");
		String cycleId = params.get("cycleId");
		Long projectId = !StringUtils.isBlank(params.get("projectId")) ? Long.valueOf(params.get("projectId")) : 0L;
   		Long versionId = !StringUtils.isBlank(params.get("versionId")) ? Long.valueOf(params.get("versionId")) : 0L;
		try {			
			if(Objects.isNull(folderId) || folderId.equals(0L) || folderId.equals(ApplicationConstants.ADHOC_SYSTEM_FOLDER_ID)) {
	    		return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.cycle.folder.invalid.folderId"), Status.BAD_REQUEST, null);
	    	}
			if(StringUtils.isBlank(folderName) || !folderName.matches("^(.){1,210}$")) {
	    		return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.cycle.folder.empty.folderName"), Status.BAD_REQUEST, null);
	    	}
			if(StringUtils.isBlank(cycleId) || cycleId.equals("0")) {
	    		return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.cycle.folder.invalid.cycleId", cycleId), Status.BAD_REQUEST, null);
	    	}
			if(cycleId.equals(String.valueOf(ApplicationConstants.AD_HOC_CYCLE_ID_LONG))) {
	    		return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("folder.adhoc.cycle.error.message"), Status.BAD_REQUEST, null);
	    	}
			
			//project permission validation for the user
			response = validateProjectPermission(user, Long.valueOf(cycleId), projectId, valueHolder);
			if(response != null) return response;
			if(!Objects.isNull(versionId) && !(versionId.intValue() == ApplicationConstants.UNSCHEDULED_VERSION_ID)) { //Version validation
				Version version = versionManager.getVersion(versionId);
				if(Objects.isNull(version)) {
					return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("project.version.not.exist"), Status.BAD_REQUEST, null);
				}
				valueHolder.put("versionName", version.getName());
			} else if(Objects.isNull(versionId)) {
				return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "VersionId"), Status.BAD_REQUEST, null);
			}  else {
				String versionName = authContext.getI18nHelper().getText("zephyr.je.version.unscheduled");
				valueHolder.put("versionName", versionName);
			}

            if(! verifyFolderLevelOperationPermission(user,projectId, PermissionType.ZEPHYR_EDIT_CYCLE.toString())) {

                String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
                JSONObject errorObject = new JSONObject();
                return constructErrorResponse(errorObject,errorMessage, Status.FORBIDDEN,null);
            }
			
			FolderCycleMapping folderCycleMapping = folderResourceDelegate.getFolderCycleMapping(projectId, versionId, Long.valueOf(cycleId), folderId);
			Folder existingfolder = folderResourceDelegate.getFolder(folderId);
			if(folderCycleMapping == null || existingfolder == null) {
				return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("project.folder.not.exist"), Status.BAD_REQUEST, null);
			}
			if(!Objects.isNull(folderCycleMapping.getProjectId()) && !folderCycleMapping.getProjectId().equals(projectId)) {
				return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("folder.update.project.error.message", "ProjectId"), Status.BAD_REQUEST, null);
			}
			if(!Objects.isNull(folderCycleMapping.getVersionId()) && !folderCycleMapping.getVersionId().equals(versionId)) {
				return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("folder.update.project.error.message", "VersionId"), Status.BAD_REQUEST, null);
			}
			if(!Objects.isNull(folderCycleMapping.getCycle().getID()) && !(folderCycleMapping.getCycle().getID() == Integer.valueOf(cycleId))) {
				return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("folder.update.project.error.message", "CycleId"), Status.BAD_REQUEST, null);
			}
			
			if(!existingfolder.getName().equals(folderName)) {				
				//Validation to check whether folder name is unique within the cycle.
				response = folderNameUniquenessValidation(projectId, versionId, Long.valueOf(cycleId), folderName);
				if(response != null) return response;
				isFolderNameUpdated = true;
			}
			
			String loggedInUser = UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext));
			//Delegating to update a folder.
			FolderResponse folderResponse = folderResourceDelegate.updateFolder(projectId, versionId, Long.valueOf(cycleId), existingfolder, folderName, folderDescription, loggedInUser, valueHolder);
			return Response.ok(folderResponse).build();
		} catch(Exception exception) {
			if(isFolderNameUpdated)
				folderResourceDelegate.rollbackFolderNameFromCache(projectId, versionId, Long.valueOf(cycleId), folderName);
			return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.internal.server.error"), Status.INTERNAL_SERVER_ERROR, exception);
		}
	}
	
	@ApiOperation(value = "Delete a folder under a cycle", notes = "Delete a folder under a cycle")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "request", value = "{}"),
		@ApiImplicitParam(name = "response", value = "{\"jobProgressToken\":\"34234-324234-2342342132\"}")})
	@ApiResponses({@ApiResponse(code = 400, message = "Invalid Request Parameters."),
    	@ApiResponse(code = 500, message = "Server error while processing the request."),
    	@ApiResponse(code = 401, message = "Unauthorized Request."),@ApiResponse(code = 403, message = "Permission Denied for the request"),
    	@ApiResponse(code = 200, message = "Request processed successfully", reference="{\"jobProgressToken\":\"34234-324234-2342342132\"}", response=JSONObject.class)})
	@DELETE
	@Path("/{folderId}")
	public Response deleteFolder(@ApiParam(value = "Folder Id") @PathParam("folderId") Long folderId, @ApiParam(value = "Project Id") @QueryParam("projectId") Long projectId, @ApiParam(value = "Version Id") @QueryParam("versionId") Long versionId, @ApiParam(value = "Cycle Id") @QueryParam("cycleId") Long cycleId) {
		JSONObject jsonObject = new JSONObject();
		try {
			final ApplicationUser user = authContext.getLoggedInUser();
			Map<String, String> valueHolder = new HashMap<>();
			//User permission validation.
			Response response = validateUser(user);
			if(response != null) return response;
			Long paramProjectId = projectId != null ? projectId : 0L;
	   		Long paramVersionId = versionId != null ? versionId : 0L;
			if(Objects.isNull(folderId) || folderId.equals(0L) || folderId.equals(ApplicationConstants.ADHOC_SYSTEM_FOLDER_ID)) {
	    		return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.cycle.folder.invalid.folderId"), Status.BAD_REQUEST, null);
	    	}
			if(Objects.isNull(cycleId) || cycleId.equals(0L)) {
	    		return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.cycle.folder.invalid.cycleId", cycleId), Status.BAD_REQUEST, null);
	    	}
			if(cycleId.equals(ApplicationConstants.AD_HOC_CYCLE_ID_LONG)) {
	    		return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("folder.adhoc.cycle.error.message"), Status.BAD_REQUEST, null);
	    	}
			
			//project permission validation for the user
			response = validateProjectPermission(user, Long.valueOf(cycleId), paramProjectId, valueHolder);
			if(response != null) return response;
			if(!Objects.isNull(paramVersionId) && !(paramVersionId.intValue() == ApplicationConstants.UNSCHEDULED_VERSION_ID)) { //Version validation
				Version version = versionManager.getVersion(paramVersionId);
				if(Objects.isNull(version)) {
					return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("project.version.not.exist"), Status.BAD_REQUEST, null);
				}
			} else if(Objects.isNull(paramVersionId)) {
				return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "VersionId"), Status.BAD_REQUEST, null);
			}

            if(! verifyFolderLevelOperationPermission(user,projectId, PermissionType.ZEPHYR_DELETE_CYCLE.toString())) {

                String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
                JSONObject errorObject = new JSONObject();
                return constructErrorResponse(errorObject,errorMessage, Status.FORBIDDEN,null);
            }
			FolderCycleMapping folderCycleMapping = folderResourceDelegate.getFolderCycleMapping(projectId, versionId, Long.valueOf(cycleId), folderId);
			if(folderCycleMapping == null) {
				return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("project.folder.not.exist"), Status.BAD_REQUEST, null);
			}
			if(!Objects.isNull(folderCycleMapping.getProjectId()) && !folderCycleMapping.getProjectId().equals(projectId)) {
				return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("folder.update.project.error.message", "ProjectId"), Status.BAD_REQUEST, null);
			}
			if(!Objects.isNull(folderCycleMapping.getVersionId()) && !folderCycleMapping.getVersionId().equals(versionId)) {
				return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("folder.update.project.error.message", "VersionId"), Status.BAD_REQUEST, null);
			}
			if(!Objects.isNull(folderCycleMapping.getCycle().getID()) && !(folderCycleMapping.getCycle().getID() == cycleId.intValue())) {
				return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("folder.update.project.error.message", "CycleId"), Status.BAD_REQUEST, null);
			}
			//Delegating to delete a folder.
			return folderResourceDelegate.deleteFolder(projectId, versionId, cycleId, folderId);
		} catch(Exception exception) {
			return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.internal.server.error"), Status.INTERNAL_SERVER_ERROR, exception);
		}
	}

	@GET
	public Response fetchFolders(@ApiParam(value = "Project Id") @QueryParam("projectId") Long projectId, @ApiParam(value = "Version Id") @QueryParam("versionId") Long versionId, @ApiParam(value = "Maximum number of records to fetch") @QueryParam("limit") Integer limit, @ApiParam(value = "Start position of the records") @QueryParam("offset") Integer offset,
			@ApiParam(value = "Cycle Ids") @QueryParam("cycleIds") String cycleIds) {
		final ApplicationUser user = authContext.getLoggedInUser();
		JSONObject jsonObject = new JSONObject();
		try {
            if(Objects.isNull(user) && !JiraUtil.hasAnonymousPermission(user)) {
				jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
				String errorMessage = String.format(ERROR_LOG_MESSAGE, Status.UNAUTHORIZED.getStatusCode(), Status.UNAUTHORIZED, authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
				log.error(errorMessage);
				return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
			}
		} catch (JSONException e) {
			log.error("Error occurred while validating user.",e);
			return Response.status(Status.BAD_REQUEST).build();
		}
		try {
			if(Objects.isNull(projectId) || projectId.equals(0L)) {
	    		return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "ProjectId"), Status.BAD_REQUEST, null);
	    	}
			// checking the project browse permissions
			Project project = projectManager.getProjectObj(projectId);
			if (Objects.isNull(project)) {
	            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "projectId ", "");
	            log.error(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,errorMessage));
	            return constructErrorResponse(jsonObject, errorMessage, Status.BAD_REQUEST, null);
	        }
			if(!Objects.isNull(versionId) && !(versionId.intValue() == ApplicationConstants.UNSCHEDULED_VERSION_ID)) { //Version validation
				Version version = ComponentAccessor.getVersionManager().getVersion(versionId);
				if(Objects.isNull(version)) {
					return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("project.version.not.exist"), Status.BAD_REQUEST, null);
				}
			} else if(Objects.isNull(versionId)) {
				return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "VersionId"), Status.BAD_REQUEST, null);
			}
			List<Long> cycleIdsList = new ArrayList<>();
			if(!StringUtils.isBlank(cycleIds)) {
				for(String cyleID : cycleIds.split(",")) {
					Long cycleId = Long.valueOf(cyleID);
					if(!cycleId.equals(ApplicationConstants.AD_HOC_CYCLE_ID_LONG)) {
						Cycle cycle = cycleManager.getCycle(cycleId);
						if(cycle == null) {
							log.error("[Error] [Error code:"+ Response.Status.BAD_REQUEST.getStatusCode() + " " + Response.Status.BAD_REQUEST +" Error Message :"+
									authContext.getI18nHelper().getText("zephyr.common.invalid.parameter"));
							return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("project.cycle.not.exist"), Status.NOT_ACCEPTABLE, null);
						}
					}
					cycleIdsList.add(cycleId);
				}
			} else {
				cycleIdsList.add(-1L);
			}
			List<Folder> foldersList = folderResourceDelegate.fetchFolders(projectId, versionId, cycleIdsList, limit, offset);
			JSONArray jsonArray = new JSONArray();
			JSONObject jsonObj = null;
			for(Folder folder : foldersList) {
                jsonObj = new JSONObject();
                jsonObj.put("folderId", folder.getID());
                jsonObj.put("folderName", folder.getName());
                jsonObj.put("folderDescription", folder.getDescription());
                jsonObj.put("isExecutionWorkflowEnabledForProject",JiraUtil.getExecutionWorkflowEnabled(project.getId()));
                jsonArray.put(jsonObj);
            }
			return Response.ok().entity(jsonArray.toString()).build();
		} catch(Exception exception) {
			return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.internal.server.error"), Status.INTERNAL_SERVER_ERROR, null);
		}
	}
	
	private Response constructErrorResponse(JSONObject jsonObject, String errorMessage, Status status, Exception exception) {
        try {
        	String finalErrorMessage = String.format(ERROR_LOG_MESSAGE, status.getStatusCode(), status, errorMessage);
            log.error(finalErrorMessage, exception);
			jsonObject.put("error", errorMessage);
			return Response.status(status).entity(jsonObject != null ? jsonObject.toString() : finalErrorMessage).cacheControl(ZephyrCacheControl.never()).build();
		} catch (JSONException e) {
			log.error("Eror while constructing the error response");
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	private Response validateUser(final ApplicationUser user) {
		JSONObject jsonObject = new JSONObject();
		try {
            if(user == null && !JiraUtil.hasAnonymousPermission(user)) {
				jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
				return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.logged.user.error"), Status.UNAUTHORIZED, null);
			}
		} catch (JSONException e) {
			log.error("Error occurred while getting count.",e);
			return Response.status(Status.BAD_REQUEST).build();
		}
		return null;
	}
	
	private Response folderNameUniquenessValidation(Long projectId, Long versionId, Long cycleId, String folderName) {
		JSONObject jsonObject = new JSONObject();
		try {
            if(!folderResourceDelegate.isFolderUniqueForCycle(projectId, versionId, cycleId, folderName)) {
            	jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.folder.name.unique", folderName));
				return constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.folder.name.unique", folderName), Status.BAD_REQUEST, null);
			}
		} catch (JSONException | RuntimeException e) {
			log.error("Error occurred while checking folder name uniquness. Folder Name : " + folderName, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		return null;
	}
	
	private Response validateProjectPermission(ApplicationUser user, Long cycleId, Long projectId, Map<String, String> valueHolder) {
		JSONObject jsonObject = new JSONObject();
		if(!cycleId.equals(ApplicationConstants.AD_HOC_CYCLE_ID_LONG)) {
			Cycle cycle = cycleManager.getCycle(cycleId);
	        if (null == cycle) {
	            final I18nHelper i18nHelper = authContext.getI18nHelper();
	            log.error(String.format(ERROR_LOG_MESSAGE,Response.Status.NOT_ACCEPTABLE.getStatusCode(),Status.NOT_ACCEPTABLE,i18nHelper.getText("project.cycle.not.exist")));
	            return constructErrorResponse(jsonObject, i18nHelper.getText("project.cycle.not.exist"), Status.NOT_ACCEPTABLE, null);
	        }
	        valueHolder.put("cycleName", cycle.getName());
		}
		Project project = projectManager.getProjectObj(projectId);
        // validate for a valid Project
        if (project == null) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.error.invalid", "projectId ", "");
            log.error(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,errorMessage));
            return constructErrorResponse(jsonObject, errorMessage, Status.BAD_REQUEST, null);
        }
        valueHolder.put("projectKey", project.getKey());
        // checking the project browse permissions for the user
        if (!JiraUtil.hasBrowseProjectPermission(project, user)) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.project.permission.error", "Cycle", String.valueOf(project.getName()));
            log.error(String.format(ERROR_LOG_MESSAGE, Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
            return constructErrorResponse(jsonObject, errorMessage, Status.FORBIDDEN, null);
        }
        return null;
	}
	
	private Response validateIsParamsEmpty(Map<String,String> params) {
		if(params == null || params.size() == 0) {
			return constructErrorResponse(null, authContext.getI18nHelper().getText("zephyr.folder.params.empty"), Status.BAD_REQUEST, null);
		}
		return null;
	}

    /**
     * Validate folder level operation permission.
     * @param user
     * @param projectId
     * @param permissionType
     */
    private boolean verifyFolderLevelOperationPermission(ApplicationUser user, Long projectId, String permissionType) {
        ProjectPermissionKey projectPermissionKey = new ProjectPermissionKey(permissionType);
        return zephyrPermissionManager.validateUserPermission(projectPermissionKey, null, user, projectId);
    }

	
	@XmlRootElement
	@ApiModel("folderResponse")
	public static class FolderResponse {
		
	    @XmlElement
	    @ApiModelProperty
	    private long id;
	    
	    @XmlElement
	    @ApiModelProperty
	    private String responseMessage;
	    
	    @XmlElement
	    @ApiModelProperty
	    private Long projectId;
	    
	    @XmlElement
	    @ApiModelProperty
	    private Long versionId;
	    
	    @XmlElement
	    @ApiModelProperty
	    private String projectKey;
	    
	    @XmlElement
	    @ApiModelProperty
	    private String versionName;
	    
	    @XmlElement
	    @ApiModelProperty
	    private Long cycleId;
	    
	    @XmlElement
	    @ApiModelProperty
	    private String cycleName;

	    public FolderResponse() {
	    }

		public Long getId() {
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

		public Long getProjectId() {
			return projectId;
		}

		public void setProjectId(Long projectId) {
			this.projectId = projectId;
		}

		public Long getVersionId() {
			return versionId;
		}

		public void setVersionId(Long versionId) {
			this.versionId = versionId;
		}

		public String getProjectKey() {
			return projectKey;
		}

		public void setProjectKey(String projectKey) {
			this.projectKey = projectKey;
		}

		public Long getCycleId() {
			return cycleId;
		}

		public void setCycleId(Long cycleId) {
			this.cycleId = cycleId;
		}

		public String getCycleName() {
			return cycleName;
		}

		public void setCycleName(String cycleName) {
			this.cycleName = cycleName;
		}

		public String getVersionName() {
			return versionName;
		}

		public void setVersionName(String versionName) {
			this.versionName = versionName;
		}		
	}
	
	@XmlRootElement
	@ApiModel("folderRequest")
    public static class FolderRequest {

        @XmlElement(nillable = false)
        @ApiModelProperty(required=false)
        private Long clonedFolderId;

        @XmlElement(nillable = false)
        @ApiModelProperty(required=true)
        private String name;

        @XmlElement(nillable = true)
        @ApiModelProperty(required=false)
        private String description;

        @XmlElement(nillable = false)
        @ApiModelProperty(required=true)
        private Long cycleId;
        
        @XmlElement(nillable = false)
        @ApiModelProperty(required=true)
        private Long projectId;
        
        @XmlElement(nillable = false)
        @ApiModelProperty(required=true)
        private Long versionId;

		@XmlElement(nillable = true)
		public Boolean cloneCustomFields;

        public FolderRequest() {
        }

		public Long getClonedFolderId() {
			return clonedFolderId;
		}

		public void setClonedFolderId(Long clonedFolderId) {
			this.clonedFolderId = clonedFolderId;
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

		public Long getCycleId() {
			return cycleId;
		}

		public void setCycleId(Long cycleId) {
			this.cycleId = cycleId;
		}

		public Long getProjectId() {
			return projectId;
		}

		public void setProjectId(Long projectId) {
			this.projectId = projectId;
		}

		public Long getVersionId() {
			return versionId;
		}

		public void setVersionId(Long versionId) {
			this.versionId = versionId;
		}

        public Boolean getCloneCustomFields() {
            return cloneCustomFields;
        }

        public void setCloneCustomFields(Boolean cloneCustomFields) {
            this.cloneCustomFields = cloneCustomFields;
        }
    }
	
	@XmlRootElement
	public static class LinkFoldersRequest {
		
	    @XmlElement
	    private Long cycleId;
	    
	    @XmlElement
	    private Long folderId;
	    
	    private Long sprintId;

	    public LinkFoldersRequest() {
	    }

		public Long getCycleId() {
			return cycleId;
		}

		public void setCycleId(Long cycleId) {
			this.cycleId = cycleId;
		}

		public Long getFolderId() {
			return folderId;
		}

		public void setFolderId(Long folderId) {
			this.folderId = folderId;
		}

		public Long getSprintId() {
			return sprintId;
		}

		public void setSprintId(Long sprintId) {
			this.sprintId = sprintId;
		}
	    
	}
}
