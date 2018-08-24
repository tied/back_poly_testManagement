package com.thed.zephyr.je.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import com.thed.zephyr.je.service.JobProgressService;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.JiraUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.index.IndexException;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.task.context.Contexts;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.Function;
import com.atlassian.jira.util.collect.CollectionEnclosedIterable;
import com.atlassian.jira.util.collect.CollectionUtil;
import com.atlassian.jira.util.collect.EnclosedIterable;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.thed.zephyr.je.index.ScheduleIndexManager;
import com.thed.zephyr.je.model.Cycle;
import com.thed.zephyr.je.model.Folder;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.permissions.model.PermissionType;
import com.thed.zephyr.je.permissions.service.ZephyrPermissionManager;
import com.thed.zephyr.je.service.CycleManager;
import com.thed.zephyr.je.service.FolderManager;
import com.thed.zephyr.je.service.ScheduleManager;
import com.thed.zephyr.util.ZCollectionUtils;
import com.thed.zephyr.util.ZephyrCacheControl;

public class CycleResourceHelper {
	protected final Logger log = Logger.getLogger(CycleResourceHelper.class);

	private final ScheduleManager scheduleManager;
	private final JiraAuthenticationContext authContext;
	private final ProjectManager projectManager;
	private final CycleManager cycleManager;
	private final ScheduleIndexManager scheduleIndexManager;
	private final ZephyrPermissionManager zephyrPermissionManager;
	private final JobProgressService jobProgressService;
	private final FolderManager folderManager;
	public CycleResourceHelper(ScheduleManager scheduleManager,
			JiraAuthenticationContext authContext, ProjectManager projectManager, CycleManager cycleManager,
			ScheduleIndexManager scheduleIndexManager,ZephyrPermissionManager zephyrPermissionManager, JobProgressService jobProgressService, FolderManager folderManager) {
		this.scheduleManager=scheduleManager;
		this.authContext=authContext;
		this.projectManager=projectManager;
		this.cycleManager=cycleManager;
		this.scheduleIndexManager=scheduleIndexManager;
		this.zephyrPermissionManager=zephyrPermissionManager;
		this.jobProgressService = jobProgressService;
		this.folderManager = folderManager;
	}
	
	
	@SuppressWarnings("unchecked")
	public Response moveExecutionsToCycle(Long cycleId, Map<String, Object> params, String jobProgressToken) {
		List<Object> scheduleList =  (List<Object>)params.get("executions");
		if(scheduleList == null || scheduleList.size() < 1){
			log.info("Schedule list is either not passed or was empty " + scheduleList);
			return Response.ok(authContext.getI18nHelper().getText("zephyr.common.error.invalid", "input", "data")).build();
		}
        final Collection<String> schedules = CollectionUtils.collect(scheduleList, new Transformer() {
            @Override
			public String transform(final Object input) {
                if (input == null) {
                    return null;
                }
                return String.valueOf(input);
            }
        });

		Boolean clearDefectMappingFlag = (Boolean)params.get("clearDefectMappingFlag");
    	List<Schedule> successSchedules = new ArrayList<Schedule>();
    	String action = ZCollectionUtils.getAsString(params, "action");
    	Boolean clearStatusFlag = (Boolean)params.get("clearStatusFlag");
    	Boolean clearAssignmentsFlag = ZCollectionUtils.getAsBoolean(params, "clearAssignmentsFlag", false);
    	Boolean clearCustomFields = ZCollectionUtils.getAsBoolean(params, "clearCustomFields", false);
    	String projectId = ZCollectionUtils.getAsString(params, "projectId");
    	String versionId = ZCollectionUtils.getAsString(params, "versionId");
    	Long folderId = ZCollectionUtils.getAsLong(params, "folderId");
    	JSONObject jsonObjectResponse = new JSONObject();
        if(folderId != null) {
        	Folder folder = folderManager.getFolder(folderId);
        	if(folder == null) {
        		try {
					jsonObjectResponse.put("error", authContext.getI18nHelper().getText("project.folder.not.exist"));
				} catch (JSONException e) {
				}
				log.error("[Error] [Error code:"+ Response.Status.BAD_REQUEST.getStatusCode() + " " + Response.Status.BAD_REQUEST +" Error Message :"+
						authContext.getI18nHelper().getText("project.folder.not.exist"));
				return Response.status(Status.BAD_REQUEST).entity(jsonObjectResponse.toString()).build();
        	}
        }
    	Map<String,String> errorMap = performInputValidation(cycleId,projectId,versionId,folderId);
    	if(errorMap.size() > 0) {
    		return buildResponseErrorMap(errorMap);
    	}
		jobProgressService.createJobProgress(ApplicationConstants.BULK_EXECUTION_COPY_MOVE_JOB_PROGRESS,0,jobProgressToken);
		jobProgressService.addSteps(jobProgressToken,schedules.size());
		ExecutorService executor = Executors.newSingleThreadExecutor();
 		final ApplicationUser user = authContext.getLoggedInUser();
    	boolean hasZephyrPermissions = verifyBulkPermissions(Long.valueOf(projectId), authContext.getLoggedInUser(),action);
    	if(!hasZephyrPermissions) {
    	    JSONObject json = new JSONObject();
    	    try {
        		String permissionError = StringUtils.join(schedules, ",");
        		json.put("success", "-");
        		json.put("projectMismatch", "-");
        		json.put("versionMismatch", "-");
        		json.put("invalid", "-");
        		json.put("existing", "-");
        		json.put("noPermissionError", StringUtils.isNotBlank(permissionError) ? permissionError : "-");
				if(StringUtils.isNotBlank(permissionError)){
					jobProgressService.addCompletedSteps(jobProgressToken,schedules.size()+1);
				}
    		} catch (JSONException e) {
    			log.warn("Error creatinhg skipped schedules JSON result",e);
    		}
    		jobProgressService.setMessage(jobProgressToken,json.toString());
        	return Response.ok().build();
    	}
    	executor.submit(()->{
			if(authContext != null && authContext.getLoggedInUser() == null)
				authContext.setLoggedInUser(user);

			List<Schedule> sucSchedules = successSchedules;
			Map<String,List<Schedule>> resultSchedules = scheduleManager.copyOrMoveBulkSchedules(schedules,action,Integer.valueOf(projectId),Integer.valueOf(versionId),folderId,cycleId.intValue(),
					clearStatusFlag != null ? clearStatusFlag : true, clearDefectMappingFlag != null ? clearDefectMappingFlag : true, clearAssignmentsFlag, jobProgressToken,clearCustomFields != null ? clearCustomFields : true);
			if(resultSchedules != null && resultSchedules.containsKey("success")) {
				sucSchedules = resultSchedules.get("success");
				//Need Index update on the same thread for ZQL.
				EnclosedIterable<Schedule> enclosedSchedules = CollectionEnclosedIterable.copy(sucSchedules);
				try {
					scheduleIndexManager.reIndexSchedule(enclosedSchedules, Contexts.nullContext());
				} catch (IndexException e) {
					log.warn("Error Indexing schedule :",e);
				}
			}

			List<String> successfulScheduleIds = transformSchedules(sucSchedules);
			List<Schedule> projectMismatchSchedules = resultSchedules.get("project_mismatch");
			List<String> projectMismatchScheduleIds = transformSchedules(projectMismatchSchedules);

			List<Schedule> versionMismatchSchedules = resultSchedules.get("version_mismatch");
			List<String> versionMismatchScheduleIds = transformSchedules(versionMismatchSchedules);

			List<Schedule> failedSchedules = resultSchedules.get("invalid");
			List<String> failedScheduleIds = transformSchedules(failedSchedules);

			List<Schedule> alreadyExistingSchedules = resultSchedules.get("already_present");
			List<String> alreadyExistingScheduleIds = transformSchedules(alreadyExistingSchedules);

			List<Schedule> issuePermissionSchedules = resultSchedules.get("issue_permission");
			List<String> issuePermissionScheduleIds = transformSchedules(issuePermissionSchedules);

			JSONObject resultJson = new JSONObject();

			try {
				String success = StringUtils.join(successfulScheduleIds, ",");
				String projectMismatch = StringUtils.join(projectMismatchScheduleIds, ",");
				String versionMismatch = StringUtils.join(versionMismatchScheduleIds, ",");
				String invalid = StringUtils.join(failedScheduleIds, ",");
				String existing = StringUtils.join(alreadyExistingScheduleIds, ",");
				String issuePermissions = StringUtils.join(issuePermissionScheduleIds, ",");
				resultJson.put("success", success);
				resultJson.put("projectMismatch", StringUtils.isNotBlank(projectMismatch) ? projectMismatch : "-");
				resultJson.put("versionMismatch", StringUtils.isNotBlank(versionMismatch) ? versionMismatch : "-");
				resultJson.put("invalid", StringUtils.isNotBlank(invalid) ? invalid : "-");
				resultJson.put("existing", existing);
				if(JiraUtil.isIssueSecurityEnabled()) {
					resultJson.put("noPermissionError", StringUtils.isNotBlank(issuePermissions) ? issuePermissions : "-");
				}
			} catch (JSONException e) {
				log.warn("Error creating skipped schedules JSON result",e);
			}
			jobProgressService.setMessage(jobProgressToken,resultJson.toString());
			jobProgressService.addCompletedSteps(jobProgressToken,1);
		});

    	return Response.ok().build();
	}

	/**
	 * Perform Validations of Input Project,Version and Cycle
	 * @param cycleId
	 * @param projectId
	 * @param versionId
	 * @param folderId 
	 * @return
	 */
	private Map<String, String> performInputValidation(Long cycleId,
			String projectId, String versionId, Long folderId) {
		Map<String,String> errorMap = new HashMap<String, String>();
		
    	//ValidateProjectAndVersion, They cannot be null irrespective of move/copy or Adhoc
    	if(projectId == null || StringUtils.isEmpty(projectId)) {
    		errorMap.put("error", authContext.getI18nHelper().getText("project.cycle.summary.create.dialog.validationError.project"));
    		return errorMap;
    	} 
    	if(versionId == null || StringUtils.isEmpty(versionId)) {
    		errorMap.put("error", authContext.getI18nHelper().getText("project.cycle.summary.create.dialog.validationError.version.missing"));
    		return errorMap;
    	} 

		
		if(cycleId != -1) {
			Cycle cycle = cycleManager.getCycle(cycleId);
	    	if(cycle == null) {
	    		errorMap.put("error", authContext.getI18nHelper().getText("project.cycle.summary.notfound.error", cycleId));
				return errorMap;
	    	}
	    	
	    	if(folderId != null && folderId.intValue() > 0) {
	    		Folder folder = folderManager.getFolder(folderId);
	    		if(folder == null) {
	    			errorMap.put("error", authContext.getI18nHelper().getText("zephyr.common.error.invalid","FOLDER_ID",String.valueOf(folderId)));
	    			return errorMap;
	    		}
	    	}
	    	//Check if project is valid for move/copy
			Project project = projectManager.getProjectObj(Long.valueOf(projectId));
			if(project == null) {
	    		errorMap.put("error", authContext.getI18nHelper().getText("zephyr.common.error.invalid","PROJECT_ID",projectId));
	    		return errorMap;
			}
			if(project.getId().intValue() != cycle.getProjectId().intValue()) {
	    		errorMap.put("error", authContext.getI18nHelper().getText("schedule.entity.mismatch.error","Project",project.getId(),"Project","Cycle",cycle.getID()));
	    		return errorMap;
			}
			
			if(cycle.getVersionId().intValue() != Integer.valueOf(versionId).intValue()) {
	    		errorMap.put("error", authContext.getI18nHelper().getText("schedule.entity.mismatch.error","Version",versionId,"Version","Cycle",cycle.getID()));
	    		return errorMap;
			}
		}
		return errorMap;
	}


	/**
	 * Build Error Map 
	 * @param errorMap
	 * @return
	 */
	private Response buildResponseErrorMap(Map<String, String> errorMap) {
		ResponseBuilder builder = Response.status(Response.Status.NOT_ACCEPTABLE);
		builder.type(MediaType.APPLICATION_JSON);
		builder.entity(errorMap);
		log.error("[Error] [Error code:"+ Response.Status.NOT_ACCEPTABLE.getStatusCode()+" "+Response.Status.NOT_ACCEPTABLE+" Error Message :"+errorMap);
		return builder.build();
	}
	
	/**
	 * Transforms Schedules to Int
	 * @param successSchedules
	 * @return
	 */
	private List<String> transformSchedules(List<Schedule> successSchedules) {
		List<String> successfulScheduleIds = CollectionUtil.transform(successSchedules, new Function<Schedule, String>()
		{
	        @Override
			public String get(final Schedule schedule)
	        {
	            return ComponentAccessor.getIssueManager().getIssueObject(new Long(schedule.getIssueId())).getKey();
	        }
	    });
		if(successSchedules == null) {
			successSchedules = new ArrayList<Schedule>();
		}
		return successfulScheduleIds;
	}


	public List<Cycle> getCyclesByVersions(List<Long> projectIdList,
			String[] versionIds, String[] sprintIds, Integer offset, Integer maxRecords) {
		List<Cycle> allCycles = cycleManager.getCyclesByProjectsAndVersions(projectIdList,versionIds,sprintIds,offset,maxRecords);
		return allCycles;
	}
	
	private boolean verifyBulkPermissions(Long projectId ,ApplicationUser user, String action) {
		//Check ZephyrPermission and update response to include execution per project permissions
		Collection<ProjectPermissionKey> projectPermissionKeys = new ArrayList<ProjectPermissionKey>();
		ProjectPermissionKey cyclePermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_BROWSE_CYCLE.toString());
		projectPermissionKeys.add(cyclePermissionKey);
		if(StringUtils.equalsIgnoreCase(action, "move")) {
			ProjectPermissionKey executionPermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_EDIT_EXECUTION.toString());
			projectPermissionKeys.add(executionPermissionKey);
		}
		if(StringUtils.equalsIgnoreCase(action, "copy")) {
			ProjectPermissionKey executionPermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_CREATE_EXECUTION.toString());
			projectPermissionKeys.add(executionPermissionKey);
		}
		boolean loggedInUserHasZephyrPermission = zephyrPermissionManager.validateUserPermissions(projectPermissionKeys, null, user,projectId);
		return loggedInUserHasZephyrPermission;
	}
}
