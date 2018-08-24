package com.thed.zephyr.je.rest;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.rest.v2.issue.IssueAttachmentsResource.JiraAttachmentMultipartConfig;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.task.context.Contexts;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.usercompatibility.UserCompatibilityHelper;
import com.atlassian.jira.util.collect.CollectionEnclosedIterable;
import com.atlassian.jira.util.collect.EnclosedIterable;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.web.util.FileIconUtil;
import com.atlassian.plugins.rest.common.multipart.FilePart;
import com.atlassian.plugins.rest.common.multipart.MultipartConfigClass;
import com.atlassian.plugins.rest.common.multipart.MultipartFormParam;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.sun.jersey.spi.container.ResourceFilters;
import com.thed.zephyr.je.attachment.AttachmentUtils;
import com.thed.zephyr.je.attachment.ZAttachmentException;
import com.thed.zephyr.je.attachment.ZAttachmentManager;
import com.thed.zephyr.je.event.EventType;
import com.thed.zephyr.je.event.ScheduleModifyEvent;
import com.thed.zephyr.je.event.StepResultModifyEvent;
import com.thed.zephyr.je.event.TeststepModifyEvent;
import com.thed.zephyr.je.model.Attachment;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.model.StepResult;
import com.thed.zephyr.je.model.Teststep;
import com.thed.zephyr.je.permissions.model.PermissionType;
import com.thed.zephyr.je.permissions.service.ZephyrPermissionManager;
import com.thed.zephyr.je.rest.exception.RESTException;
import com.thed.zephyr.je.rest.filter.ZFJApiFilter;
import com.thed.zephyr.je.service.AttachmentManager;
import com.thed.zephyr.je.service.ScheduleManager;
import com.thed.zephyr.je.service.StepResultManager;
import com.thed.zephyr.je.service.TeststepManager;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.ZephyrCacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.ofbiz.core.util.UtilDateTime;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.util.*;

@Api(value = "Attachment Resource API(s)", description = "Following section describes the rest resources (API's) for fetching and uploading attachments.\nTo get attachment for a given entity (Execution or Its Stepresults) or upload, You would need its\nid : Unique Identifier in DB\nType : execution | stepresult")
@Path("attachment")
@Produces({ MediaType.APPLICATION_JSON })
@ResourceFilters(ZFJApiFilter.class)
public class AttachmentResource {
    private static final String SCHEDULE_ENTITY_TYPE = "SCHEDULE";
    private static final String ERROR_LOG_MESSAGE = "[Error] [Error code: %s %s Error Message : %s]";

    protected final Logger log = Logger.getLogger(AttachmentResource.class);

	private final JiraAuthenticationContext authContext;
	private final AttachmentManager attachmentManager;
	private final ZAttachmentManager zAttachmentManager;
	private final ScheduleManager scheduleManager;
	private final StepResultManager stepResultManager;
	private final ProjectManager projectManager;
	private final FileIconUtil fileIconUtil;
	private final DateTimeFormatterFactory dateTimeFormatterFactory;
	private static final String EXECUTION_ENTITY_TYPE="execution";
    private final EventPublisher eventPublisher;
	private static final String ATTACHMENT_ENTITY = "Attachment";
    private final ZephyrPermissionManager zephyrPermissionManager;
    private final TeststepManager teststepManager;
    private final IssueManager issueManager;


	public AttachmentResource(final JiraAuthenticationContext authContext,final AttachmentManager attachmentManager,
			final FileIconUtil fileIconUtil,DateTimeFormatterFactory dateTimeFormatterFactory,final ScheduleManager scheduleManager,
			final StepResultManager stepResultManager,final ProjectManager projectManager,final ZAttachmentManager zAttachmentManager,
			final EventPublisher eventPublisher,ZephyrPermissionManager zephyrPermissionManager, TeststepManager teststepManager,
            IssueManager issueManager) {
		this.authContext=authContext;
		this.attachmentManager = attachmentManager;
		this.fileIconUtil=fileIconUtil;
		this.dateTimeFormatterFactory=dateTimeFormatterFactory;
		this.scheduleManager=scheduleManager;
		this.stepResultManager=stepResultManager;
		this.projectManager=projectManager;
		this.zAttachmentManager=zAttachmentManager;
		this.eventPublisher=eventPublisher;
		this.zephyrPermissionManager=zephyrPermissionManager;
		this.teststepManager = teststepManager;
		this.issueManager = issueManager;
	}

	@ApiOperation(value = "Delete Attachment", notes = "Delete Attachment by Attachment Id")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "{\"success\": \"Attachment je-3.2.0.32002508-local.obr successfully deleted\"}")})
	@DELETE
    @Path("/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({ MediaType.APPLICATION_JSON })
    public Response deleteAttachment(@PathParam("id") Long attachmentId) {

    	JSONObject jsonObject = new JSONObject();
    	try {
			if(authContext.getLoggedInUser() == null) {
			    log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
				jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
				return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
			}
    		Attachment attachment = attachmentManager.getAttachment(attachmentId);
    		Long projectId = null;
    		if(attachment != null) {
    	    	projectId = validateAttachmentAndGetProject(attachmentId,attachment, projectId);
    	    	
    	    	//Validate Project Permission
    	    	boolean hasPermission = JiraUtil.hasBrowseProjectPermission(projectId, authContext.getLoggedInUser());
    	    	if(!hasPermission) {
    	       		String errorMessage = authContext.getI18nHelper().getText("zephyr.project.permission.error","Add/Get/Delete",ATTACHMENT_ENTITY);
                    log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(),Response.Status.FORBIDDEN,errorMessage));
    	       		return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "Insufficient Project permissions", errorMessage, errorMessage );
    			}

    	    	//Adding Permission Check. If user has edit execution/delete execution permission than allow user to delete attachment
    	    	boolean hasZephyrPermissions = verifyBulkEditPermissions(projectId,authContext.getLoggedInUser());
    	    	if(!hasZephyrPermissions) {
					String errorMessage = authContext.getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
                    log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(),Response.Status.FORBIDDEN,errorMessage));
					return JiraUtil.getPermissionDeniedErrorResponse(errorMessage);
    	    	} else {
    	    		//Check if the attachment is at Execution/Execution-TestStep, update the modified_date if yes
    	    		Schedule schedule = null;
    	    		StepResult stepResult = null;
    	    		if(SCHEDULE_ENTITY_TYPE.equals(attachment.getType())){
    	    			schedule = scheduleManager.getSchedule(attachment.getEntityId().intValue());
    	    		}else if("TESTSTEPRESULT".equals(attachment.getType())){
    		    		stepResult = stepResultManager.getStepResult(attachment.getEntityId().intValue());
    		    		if(stepResult != null)
    		    			schedule = scheduleManager.getSchedule(stepResult.getScheduleId());
    	    		}
    	    		if(schedule != null){
    	    			//scheduleManager.updateSchedule();//TODO update only the modified_date
    	                // Table to keep track of modified attachments of given StepResult for change logs
    	            	Table<String, String, Object> changePropertyTable =  HashBasedTable.create();
    	        		changePropertyTable.put("ATTACHMENT", ApplicationConstants.OLD, attachment.getFileName());
    	        		changePropertyTable.put("ATTACHMENT", ApplicationConstants.NEW, ApplicationConstants.NULL);
    	                //if entityType == STEPRESULT, save schange logs for attachment
    	        		if(stepResult != null) {    	    		// get the StepResult by given Id
    	    				// publishing ScheduleModifyEvent for change logs
    	    				eventPublisher.publish(new StepResultModifyEvent(stepResult, changePropertyTable, EventType.STEPRESULT_ATTACHMENT_DELETED,
    	    						UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));
    	                } else if(schedule != null){
    	    				// publishing ScheduleModifyEvent for change logs
    	    				eventPublisher.publish(new ScheduleModifyEvent(schedule, changePropertyTable, EventType.EXECUTION_ATTACHMENT_DELETED,
    	    						UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));
    	                }
    	        		scheduleManager.updateModifiedDate(schedule, new Date());
    	    		}
					Response response = attachmentManager.removeAttachment(attachment);
			    	if(response != null)  return response;
			    	jsonObject.put("success", authContext.getI18nHelper().getText("zephyr.common.success.delete", ATTACHMENT_ENTITY,attachment.getFileName()));
			    	return Response.ok(jsonObject.toString()).build();
    	    	}
    		} else {
				jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.error.invalid","AttachmentId",String.valueOf(attachmentId)));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Response.Status.BAD_REQUEST,authContext.getI18nHelper().getText("zephyr.common.error.invalid","AttachmentId",String.valueOf(attachmentId))));
				return Response.status(Status.BAD_REQUEST).entity(jsonObject.toString()).build();
    		}
    	} catch (JSONException e) {
			log.error("Error creating JSON response",e);
			throw new RESTException(Status.BAD_REQUEST, authContext.getI18nHelper().getText("schedule.execute.update.stepresult.invalid.id", ATTACHMENT_ENTITY));
    	} 
    }
    
    @ApiOperation(value = "Add Attachment into Entity", notes = "Add Attachment into Entity by Entity Id, Entity Type")
	@POST
    @Consumes (MediaType.MULTIPART_FORM_DATA)
    @MultipartConfigClass(JiraAttachmentMultipartConfig.class)
    public Response addAttachment(@QueryParam("entityId")final Integer entityId,
    		@QueryParam("entityType")final String entityType,@MultipartFormParam ("file") Collection<FilePart> fileParts) {

    	JSONObject jsonObject = new JSONObject();
    	String type=null;
    	try {
            if(authContext.getLoggedInUser() == null) {
                jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
	    	if(entityId == null || entityId.intValue() == 0) {
	    		jsonObject.put("error", authContext.getI18nHelper().getText("schedule.update.ID.required", "entityId"));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("schedule.update.ID.required", "entityId")));
	    		return Response.status(Status.BAD_REQUEST).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
	    	}
	
	    	if(StringUtils.isBlank(entityType)) {
	    		jsonObject.put("error", authContext.getI18nHelper().getText("schedule.update.ID.required", "entityType"));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("schedule.update.ID.required", "entityType")));
	    		return Response.status(Status.BAD_REQUEST).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
	    	}
    	} catch(JSONException e) {
    		log.error("createAttachment failed",e);
    		return Response.status(Status.BAD_REQUEST).build();
    	}
		try {
			Project project = null;
			Schedule schedule = null;
			StepResult stepResult = null;
			Integer issueId = null;
			Teststep teststep = null;
	    	if(StringUtils.equalsIgnoreCase(entityType, "execution") || StringUtils.equalsIgnoreCase(entityType, "schedule")) {
	    		type = SCHEDULE_ENTITY_TYPE;
	    		schedule = scheduleManager.getSchedule(entityId);
	    		if(schedule == null) {
					jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.error.invalid", entityType,String.valueOf(entityId)));
                    log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("zephyr.common.error.invalid", entityType,String.valueOf(entityId))));
		    		return Response.status(Status.BAD_REQUEST).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
	    		}
                issueId = schedule.getIssueId();
	    		project = projectManager.getProjectObj(schedule.getProjectId());
	    	} else if(StringUtils.equalsIgnoreCase(entityType, "TESTSTEPRESULT") || StringUtils.equalsIgnoreCase(entityType, "stepresult")) {
	    		type = "TESTSTEPRESULT";
	    		stepResult = stepResultManager.getStepResult(entityId);
				if(stepResult == null) {
					jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.error.invalid", entityType,String.valueOf(entityId)));
                    log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("zephyr.common.error.invalid", entityType,String.valueOf(entityId))));
		    		return Response.status(Status.BAD_REQUEST).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
	    		}
                schedule = scheduleManager.getSchedule(stepResult.getScheduleId());
                issueId = schedule.getIssueId();
	    		project = projectManager.getProjectObj(stepResult.getProjectId());
	    	}else if(StringUtils.equalsIgnoreCase(entityType, ApplicationConstants.TEST_STEP_TYPE) || StringUtils.equalsIgnoreCase(entityType, StringUtils.lowerCase(ApplicationConstants.TEST_STEP_TYPE))) {
				type = ApplicationConstants.TEST_STEP_TYPE;
				teststep = teststepManager.getTeststep(entityId);
                issueId = teststep.getIssueId().intValue();

				if(teststep == null) {
					jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.error.invalid", entityType,String.valueOf(entityId)));
					log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("zephyr.common.error.invalid", entityType,String.valueOf(entityId))));
					return Response.status(Status.BAD_REQUEST).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
				}
				project = issueManager.getIssueObject(teststep.getIssueId()).getProjectObject();
			} else {
				jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.error.invalid", "entityType",entityType));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("zephyr.common.error.invalid", "entityType",entityType)));
	    		return Response.status(Status.BAD_REQUEST).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build(); 
	    	}


			Response response = checkIssueSecurityPermission(issueId,project.getId(),entityType);
			if (response != null) return response;

			//Validate Project Permission
	    	boolean hasPermission = JiraUtil.hasBrowseProjectPermission(project.getId(),authContext.getLoggedInUser());
	    	if(!hasPermission) {
	       		String errorMessage = authContext.getI18nHelper().getText("zephyr.project.permission.error","Add/Get/Delete",ATTACHMENT_ENTITY);
	       		log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
	       		return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "Insufficient Project permissions", errorMessage, errorMessage );
			}
	    	
	    	//Adding Permission Check. If user has edit execution/delete execution permission than allow user to delete attachment
	    	boolean hasZephyrPermissions = verifyBulkEditPermissions(project.getId(),authContext.getLoggedInUser());
	    	if(!hasZephyrPermissions) {
				String errorMessage = authContext.getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
                log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
				return JiraUtil.getPermissionDeniedErrorResponse(errorMessage);
	    	}

	    	final List<String> retSuccess = new ArrayList<String>();
	        final List<String> failedFiles = new ArrayList<String>();

	        for (FilePart filePart : fileParts)
            {
                final ChangeItemBean cib = zAttachmentManager.createAttachment(getFileFromFilePart(filePart), filePart.getName(), filePart.getContentType(), 
                		authContext.getLoggedInUser(), entityId.longValue(),type,null,Collections.<String, Object>emptyMap(), UtilDateTime.nowTimestamp(),project);
                if(cib == null) {
                	failedFiles.add(filePart.getName());
                }
                retSuccess.add(filePart.getName());
                if(retSuccess.size() > 0) {
                	jsonObject.put("success", authContext.getI18nHelper().getText("attachment.operation.upload.message",StringUtils.join(retSuccess, ",")));	
                }
                if(failedFiles.size() > 0) {
                    log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,authContext.getI18nHelper().getText("attachment.operation.upload.error",StringUtils.join(failedFiles, ","))));
                	jsonObject.put("error", authContext.getI18nHelper().getText("attachment.operation.upload.error",StringUtils.join(failedFiles, ",")));
                }

                // Table to keep track of modified attachments of given StepResult for change logs
            	Table<String, String, Object> changePropertyTable =  HashBasedTable.create();
        		changePropertyTable.put("ATTACHMENT", ApplicationConstants.OLD, ApplicationConstants.NULL);
        		changePropertyTable.put("ATTACHMENT", ApplicationConstants.NEW, filePart.getName());	            
                //if entityType == STEPRESULT, save schange logs for attachment
        		if(stepResult != null) {    	    		// get the StepResult by given Id
    				// publishing ScheduleModifyEvent for change logs
    				eventPublisher.publish(new StepResultModifyEvent(stepResult, changePropertyTable, EventType.STEPRESULT_ATTACHMENT_ADDED,
    						UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));
                } else if(schedule != null){
    				// publishing ScheduleModifyEvent for change logs
    				eventPublisher.publish(new ScheduleModifyEvent(schedule, changePropertyTable, EventType.EXECUTION_ATTACHMENT_ADDED,
    						UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));
                } else if (null != teststep) {
                    eventPublisher.publish(new TeststepModifyEvent(teststep, changePropertyTable, EventType.TESTSTEP_UPDATED,
                            UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));
				}
            }
		} catch (JSONException e) {
			log.error("Error creating JSON response",e);
		} catch (IOException e) {
			log.error("IO Error adding attachment",e);
    		return Response.status(Status.UNSUPPORTED_MEDIA_TYPE).cacheControl(ZephyrCacheControl.never()).build(); 
		} catch (ZAttachmentException e) {
			log.error("Attachment Exception",e);
    		return Response.status(Status.UNSUPPORTED_MEDIA_TYPE).cacheControl(ZephyrCacheControl.never()).build(); 
		} catch (Exception e) {
			log.error("Error creating JSON response",e);
    		return Response.status(Status.UNSUPPORTED_MEDIA_TYPE).cacheControl(ZephyrCacheControl.never()).build(); 
		}
    	return Response.ok().entity(jsonObject.toString()).build();
    }


	@ApiOperation(value = "Get Single Attachment", notes = "Get Attachment Details by Attachment Id")
	@ApiImplicitParams({ @ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "{\"data\":{\"fileName\":\"je-3.2.0.32002508-local.obr\",\"dateCreated\":\"28/Mar/16 10:57 AM\",\"fileSize\":\"3064089\",\"fileIcon\":\"file.gif\",\"author\":\"vm_admin\",\"fileIconAltText\":\"File\",\"comment\":\"\",\"fileId\":\"1\"}}")})
	@GET
	@Produces({MediaType.APPLICATION_JSON})
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/{id}")
	public Response getAttachment(@PathParam("id") Long attachmentId) {

		if(attachmentId == null || attachmentId <= 0) {
			JSONObject jsonObject = new JSONObject();
			try {
                if(authContext.getLoggedInUser() == null) {
                    jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                    log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                    return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
                }
				jsonObject.put("error", authContext.getI18nHelper().getText("schedule.update.ID.required","ID"));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("schedule.update.ID.required","ID")));
				return Response.status(Status.BAD_REQUEST).entity(jsonObject.toString()).build();
			} catch (JSONException e) {
				log.error("Error Retrieving Attachments",e);
				return Response.status(Status.BAD_REQUEST).build();
			}
		}

		Attachment attachment = attachmentManager.getAttachment(attachmentId);

		if(attachment == null || attachment.getEntityId() == null || attachment.getType() == null){
			log.warn("Invalid attachment object, must be cleaned up in DB");
			JSONObject jsonObject = new JSONObject();
			try {
                log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("zephyr.common.error.invalid", "attachment ID",attachmentId+"")));
                jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.error.invalid", "attachment ID",attachmentId+""));
				return Response.status(Status.BAD_REQUEST).entity(jsonObject.toString()).build();
			} catch (JSONException e) {
				log.error("Invalid Attachment ID", e);
				return Response.status(Status.BAD_REQUEST).build();
			}
		}
		Long projectId = validateEntityAndGetProject(attachment.getEntityId().intValue(), attachment.getType());
		//Validate Project Permission
		boolean hasPermission = JiraUtil.hasBrowseProjectPermission(projectId,authContext.getLoggedInUser());

		if(!hasPermission) {
			String errorMessage = authContext.getI18nHelper().getText("zephyr.project.permission.error","Add/Get/Delete",ATTACHMENT_ENTITY);
            log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
			return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "Insufficient Project permissions", errorMessage, errorMessage );
		}

		Response response = getAndCheckIssueSecurityPermission(attachment.getEntityId().intValue(),projectId, attachment.getType());
		if (response != null) return response;

    	//Adding Permission Check. If user has browse_cycle permission than allow user to get attachment
    	ProjectPermissionKey projectPermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_BROWSE_CYCLE.toString());
    	boolean hasZephyrPermisison = zephyrPermissionManager.validateUserPermission(projectPermissionKey, null, authContext.getLoggedInUser(), projectId);
    	if(!hasZephyrPermisison) {
			String errorMessage = authContext.getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
            log.error(String.format(ERROR_LOG_MESSAGE, Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
			return JiraUtil.getPermissionDeniedErrorResponse(errorMessage);
    	}

		Map<String, String> attachmentMap = attachmentToMap(attachment);
		JSONObject attachmentJSON = new JSONObject();
		try {
			attachmentJSON.put("data", attachmentMap);
		} catch (JSONException e) {
			log.error("Error creating attachment JSON",e);
		}
		return Response.ok(attachmentJSON.toString()).build();
	}


	@ApiOperation(value = "Get Attachment By Entity", notes = "Get Attachments by Entity Id, Entity Type")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "{\"data\":[{\"fileName\":\"ABC-60 (1) (1).doc\",\"dateCreated\":\"Today 5:15 PM\",\"fileSize\":\"7736\",\"fileIcon\":\"word.gif\",\"author\":\"vm_admin\",\"fileIconAltText\":\"Microsoft Word\",\"comment\":\"\",\"fileId\":\"20\"},{\"fileName\":\"ABC-60 (1).doc\",\"dateCreated\":\"Today 5:18 PM\",\"fileSize\":\"7736\",\"fileIcon\":\"word.gif\",\"author\":\"vm_admin\",\"fileIconAltText\":\"Microsoft Word\",\"comment\":\"\",\"fileId\":\"22\"}]}")})
	@GET
	@Produces({MediaType.APPLICATION_JSON})
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/attachmentsByEntity")
	public Response getAttachmentByEntity(@QueryParam("entityId") Integer entityId,
								  @QueryParam("entityType") String entityType) {

		//dirty fix, but for ZAPI we need to standardize on using execution
		if(StringUtils.equalsIgnoreCase(entityType, EXECUTION_ENTITY_TYPE)) {
			entityType = SCHEDULE_ENTITY_TYPE;
		}
		if(entityId == null || entityId <= 0) {
			JSONObject jsonObject = new JSONObject();
			try {
                if(authContext.getLoggedInUser() == null) {
                    jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                    log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                    return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
                }

                jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.error.invalid", "entity ID",entityId+""));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("zephyr.common.error.invalid", "entity ID",entityId+"")));
				return Response.status(Status.BAD_REQUEST).entity(jsonObject.toString()).build();
			} catch (JSONException e) {
				log.error("Error Retrieving Attachments",e);
				return Response.status(Status.BAD_REQUEST).build();
			}
		}

		Long projectId = validateEntityAndGetProject(entityId,entityType);
    	//Validate Project Permission
    	boolean hasPermission = JiraUtil.hasBrowseProjectPermission(projectId,authContext.getLoggedInUser());
    	if(!hasPermission) {
       		String errorMessage = authContext.getI18nHelper().getText("zephyr.project.permission.error","Add/Get/Delete",ATTACHMENT_ENTITY);
            log.error(String.format(ERROR_LOG_MESSAGE, Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
       		return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "Insufficient Project permissions", errorMessage, errorMessage );
		}
    	
    	//Adding Permission Check. If user has browse_cycle permission than allow user to get attachment
    	ProjectPermissionKey projectPermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_BROWSE_CYCLE.toString());
    	boolean hasZephyrPermisison = zephyrPermissionManager.validateUserPermission(projectPermissionKey, null, authContext.getLoggedInUser(), projectId);
    	if(!hasZephyrPermisison) {
			String errorMessage = authContext.getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
            log.error(String.format(ERROR_LOG_MESSAGE, Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
			return JiraUtil.getPermissionDeniedErrorResponse(errorMessage);
    	}

		Response response = getAndCheckIssueSecurityPermission(entityId, projectId, entityType);
		if (response != null) return response;
    	
    	List<Attachment> attachments = attachmentManager.getAttachmentsByEntityIdAndType(entityId, entityType);
		List<Map<String,String>> attachmentsMap = convertAttachmentsToMap(attachments);
        JSONObject jsonObject = new JSONObject();
        try {
			jsonObject.put("data", attachmentsMap);
		} catch (JSONException e) {
			log.error("Error Retrieving Attachments",e);
		}
    	return Response.ok(jsonObject.toString()).build();
    }

    @ApiOperation(value = "Get Attachment File", notes = "Get Attachment file by Attachment file Id")
    @ApiImplicitParams({ @ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{ }")})
    @GET
    @Path("/{fileid}/file")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getAttachmentFileByFileId(@PathParam("fileid") Long attachmentFileId) {

        if(attachmentFileId == null || attachmentFileId <= 0) {
            JSONObject jsonObject = new JSONObject();
            try {
                if(authContext.getLoggedInUser() == null) {
                    jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                    log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                    return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
                }
                jsonObject.put("error", "Invalid Attachment ID");
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("schedule.update.ID.required","ID")));
                return Response.status(Status.BAD_REQUEST).entity(jsonObject.toString()).build();
            } catch (JSONException e) {
                log.error("Error Retrieving Attachments",e);
                return Response.status(Status.BAD_REQUEST).build();
            }
        }

        Attachment attachment = attachmentManager.getAttachment(attachmentFileId);

        if(attachment == null || attachment.getEntityId() == null || attachment.getType() == null){
            log.warn("Invalid attachment object, must be cleaned up in DB");
            JSONObject jsonObject = new JSONObject();
            try {
                log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("zephyr.common.error.invalid", "attachment file ID",attachmentFileId+"")));
                jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.error.invalid", "attachment file ID",attachmentFileId+""));
                return Response.status(Status.BAD_REQUEST).entity(jsonObject.toString()).build();
            } catch (JSONException e) {
                log.error("Invalid Attachment ID", e);
                return Response.status(Status.BAD_REQUEST).build();
            }
        }
        Long projectId = validateEntityAndGetProject(attachment.getEntityId().intValue(), attachment.getType());
        //Validate Project Permission
        boolean hasPermission = JiraUtil.hasBrowseProjectPermission(projectId,authContext.getLoggedInUser());

        if(!hasPermission) {
            String errorMessage = authContext.getI18nHelper().getText("zephyr.project.permission.error","Add/Get/Delete",ATTACHMENT_ENTITY);
            log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
            return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "Insufficient Project permissions", errorMessage, errorMessage );
        }

		//Adding Permission Check. If user has browse_cycle permission than allow user to get attachment
		ProjectPermissionKey projectPermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_BROWSE_CYCLE.toString());
		boolean hasZephyrPermisison = zephyrPermissionManager.validateUserPermission(projectPermissionKey, null, authContext.getLoggedInUser(), projectId);
		if(!hasZephyrPermisison) {
			String errorMessage = authContext.getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
			log.error(String.format(ERROR_LOG_MESSAGE, Status.FORBIDDEN.getStatusCode(),Status.FORBIDDEN,errorMessage));
			return JiraUtil.getPermissionDeniedErrorResponse(errorMessage);
		}

        File file = AttachmentUtils.getAttachmentFile(attachment,projectManager.getProjectObj(projectId));

        StreamingOutput stream = null;
        try {
            final InputStream in = new FileInputStream(file);
            stream = new StreamingOutput() {
                public void write(OutputStream out) throws IOException, WebApplicationException {
                    try {
                        int read = 0;
                        byte[] bytes = new byte[1024];

                        while ((read = in.read(bytes)) != -1) {
                            out.write(bytes, 0, read);
                        }
                    } catch (Exception e) {
                        throw new WebApplicationException(e);
                    }
                }
            };
        } catch (FileNotFoundException ex) {
            log.error("File not found.",ex);
        }
        return Response.ok(stream).header("content-disposition","attachment; filename = "+attachment.getFileName()).build();
    }
    
    /**
     * Converts Attachment Object to Map and adds Icons to it
     * @param attachments
     * @return
     */
	private List<Map<String,String>> convertAttachmentsToMap(List<Attachment> attachments) {
		List<Map<String,String>> responseAttachments = new ArrayList<Map<String,String>>();
		if(attachments == null)
			return responseAttachments;
		attachments.forEach(attachment -> responseAttachments.add(attachmentToMap(attachment)));
		return responseAttachments;
	}

	private Map<String, String> attachmentToMap(Attachment attachment) {
		Map<String,String> attachmentMap = new HashMap<String,String>();
		final com.atlassian.jira.web.util.FileIconBean.FileIcon fileIcon = fileIconUtil.getFileIcon(attachment.getFileName(), attachment.getMimetype());
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
	 * Creates temp file for attachment
	 * @param filePart
	 * @return
	 * @throws IOException
	 */
    private File getFileFromFilePart(FilePart filePart) throws IOException {
        File file = File.createTempFile("attachment-", ".tmp");
        file.deleteOnExit();
        filePart.write(file);
        return file;
    }
    


	/**
	 * Validates Entity passed in for deleting Attachment
	 * @param attachmentId
	 * @param attachment
	 * @param projectId
	 * @return
	 */
	private Long validateAttachmentAndGetProject(Long attachmentId,
			Attachment attachment, Long projectId) {
        if (StringUtils.equalsIgnoreCase(attachment.getType(), "execution") ||
                StringUtils.equalsIgnoreCase(attachment.getType(), "schedule")) {
            Schedule schedule = scheduleManager.getSchedule(attachment.getEntityId().intValue());
            if (schedule == null) {
                log.error(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST, authContext.getI18nHelper().getText("zephyr.common.error.invalid", ATTACHMENT_ENTITY, String.valueOf(attachmentId))));
                throw new RESTException(Status.BAD_REQUEST, authContext.getI18nHelper().getText("zephyr.common.error.invalid", ATTACHMENT_ENTITY, String.valueOf(attachmentId)));
            }
            projectId = schedule.getProjectId();
        } else if (StringUtils.equalsIgnoreCase(attachment.getType(), "TESTSTEPRESULT") || StringUtils.equalsIgnoreCase(attachment.getType(), "stepresult")) {
            StepResult stepResult = stepResultManager.getStepResult(attachment.getEntityId().intValue());
            if (stepResult == null) {
                log.error(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST, authContext.getI18nHelper().getText("zephyr.common.error.invalid", ATTACHMENT_ENTITY, String.valueOf(attachmentId))));
                throw new RESTException(Status.BAD_REQUEST, authContext.getI18nHelper().getText("zephyr.common.error.invalid", ATTACHMENT_ENTITY, String.valueOf(attachmentId)));
            }
            projectId = stepResult.getProjectId();
        } else if (StringUtils.equalsIgnoreCase(attachment.getType(), ApplicationConstants.TEST_STEP_TYPE) || StringUtils.equalsIgnoreCase(attachment.getType(), StringUtils.lowerCase(ApplicationConstants.TEST_STEP_TYPE))) {
			Teststep teststep = teststepManager.getTeststep(attachment.getEntityId().intValue());
			if (null == teststep) {
				log.error(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST, authContext.getI18nHelper().getText("zephyr.common.error.invalid", ATTACHMENT_ENTITY, String.valueOf(attachmentId))));
				throw new RESTException(Status.BAD_REQUEST, authContext.getI18nHelper().getText("zephyr.common.error.invalid", ATTACHMENT_ENTITY, String.valueOf(attachmentId)));
			}
            projectId = issueManager.getIssueObject(teststep.getIssueId()).getProjectObject().getId();
		}
        return projectId;
    }


	/**
	 * Validates Entity passed in for deleting Attachment
	 * @param entityId
	 * @param entityType
	 * @return
	 */
	private Long validateEntityAndGetProject(Integer entityId,String entityType) {
		Long projectId =  null;
		if(StringUtils.equalsIgnoreCase(entityType, "execution") ||
				StringUtils.equalsIgnoreCase(entityType, "schedule")) {
			Schedule schedule = scheduleManager.getSchedule(entityId);
			if(schedule == null) {
                log.error(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("zephyr.common.error.invalid", entityType,String.valueOf(entityId))));
				throw new RESTException(Status.BAD_REQUEST, authContext.getI18nHelper().getText("zephyr.common.error.invalid", entityType,String.valueOf(entityId)));
			}
			projectId = schedule.getProjectId();
		} else if(StringUtils.equalsIgnoreCase(entityType, "TESTSTEPRESULT") || StringUtils.equalsIgnoreCase(entityType, "stepresult")) {
			StepResult stepResult = stepResultManager.getStepResult(entityId);
			if(stepResult == null) {
                log.error(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("zephyr.common.error.invalid", ATTACHMENT_ENTITY,String.valueOf(entityId))));
				throw new RESTException(Status.BAD_REQUEST, authContext.getI18nHelper().getText("zephyr.common.error.invalid", ATTACHMENT_ENTITY,String.valueOf(entityId)));
			}
			projectId = stepResult.getProjectId();
		} if(StringUtils.equalsIgnoreCase(entityType, "TESTSTEP") ||
				StringUtils.equalsIgnoreCase(entityType, "teststep")) {
            Teststep testStep = teststepManager.getTeststep(Integer.valueOf(entityId));

			if(testStep == null) {
				log.error(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("zephyr.common.error.invalid", entityType,String.valueOf(entityId))));
				throw new RESTException(Status.BAD_REQUEST, authContext.getI18nHelper().getText("zephyr.common.error.invalid", entityType,String.valueOf(entityId)));
			}
            MutableIssue issue = issueManager.getIssueObject(testStep.getIssueId());
			projectId = issue.getProjectId();
		}
		return projectId;
	}
	
	private boolean verifyBulkEditPermissions(Long projectId,ApplicationUser user) {
		//Check ZephyrPermission and update response to include execution per project permissions
		ProjectPermissionKey cyclePermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_BROWSE_CYCLE.toString());
		ProjectPermissionKey executionPermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_EDIT_EXECUTION.toString());
		Collection<ProjectPermissionKey> projectPermissionKeys = new ArrayList<ProjectPermissionKey>();
		projectPermissionKeys.add(executionPermissionKey);
		projectPermissionKeys.add(cyclePermissionKey);
		boolean hasZephyrPermissions = zephyrPermissionManager.validateUserPermissions(projectPermissionKeys, null, user ,projectId);
		return hasZephyrPermissions;
	}


	private Response checkIssueSecurityPermission(Integer issueId, Long projectId, String entityType) {
		if(issueId != null) {
			boolean hasViewIssuePermission = JiraUtil.hasIssueViewPermission(Long.valueOf(issueId),null,authContext.getLoggedInUser());
			if (!hasViewIssuePermission) {
				String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.project.permission.error", "Attachment for EntityType  " + entityType, String.valueOf(projectId));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(),Response.Status.FORBIDDEN,errorMessage));
				return JiraUtil.getPermissionDeniedErrorResponse("Insufficient Issue permissions." + errorMessage);
			}
		}
		return null;
	}

	private Response getAndCheckIssueSecurityPermission(Integer entityId, Long projectId, String entityType) {
		StepResult result = null;
		Schedule schedule = null;
		Integer issueId = null;
		if(ApplicationConstants.TESTSTEPRESULT_TYPE.equals(entityType)){
			result = stepResultManager.getStepResult(entityId.intValue());
			schedule = scheduleManager.getSchedule(result.getScheduleId());
			issueId = schedule.getIssueId();
		}
		else if(ApplicationConstants.SCHEDULE_TYPE.equals(entityType)){
			schedule = scheduleManager.getSchedule(entityId.intValue());
			issueId = schedule.getIssueId();
		}

		if(issueId != null) {
			boolean hasViewIssuePermission = JiraUtil.hasIssueViewPermission(Long.valueOf(issueId),null,authContext.getLoggedInUser());
			if (!hasViewIssuePermission) {
				String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.project.permission.error", "Attachment for EntityType  " + entityType, String.valueOf(projectId));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(),Response.Status.FORBIDDEN,errorMessage));
				return JiraUtil.getPermissionDeniedErrorResponse("Insufficient Issue permissions." + errorMessage);
			}
		}
		return null;
	}

}
