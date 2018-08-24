package com.thed.zephyr.je.rest.delegate.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.atlassian.jira.component.ComponentAccessor;

import org.apache.log4j.Logger;

import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.beehive.compat.ClusterLock;
import com.atlassian.beehive.compat.ClusterLockService;
import com.atlassian.beehive.compat.ClusterLockServiceFactory;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.task.context.Contexts;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.usercompatibility.UserCompatibilityHelper;
import com.atlassian.jira.util.collect.CollectionEnclosedIterable;
import com.atlassian.jira.util.collect.EnclosedIterable;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.thed.zephyr.je.event.CycleModifyEvent;
import com.thed.zephyr.je.event.EventType;
import com.thed.zephyr.je.event.FolderModifyEvent;
import com.thed.zephyr.je.event.SingleScheduleEvent;
import com.thed.zephyr.je.index.ScheduleIndexManager;
import com.thed.zephyr.je.model.Folder;
import com.thed.zephyr.je.model.Cycle;
import com.thed.zephyr.je.model.ExecutionWorkflowStatus;
import com.thed.zephyr.je.model.FolderCycleMapping;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.rest.FolderResource.FolderRequest;
import com.thed.zephyr.je.rest.FolderResource.FolderResponse;
import com.thed.zephyr.je.rest.delegate.FolderResourceDelegate;
import com.thed.zephyr.je.service.CycleManager;
import com.thed.zephyr.je.service.FolderManager;
import com.thed.zephyr.je.service.JobProgressService;
import com.thed.zephyr.je.service.ScheduleManager;
import com.thed.zephyr.je.service.ZFJCacheService;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.UniqueIdGenerator;
import com.thed.zephyr.util.ZephyrCacheControl;

/**
 * Implementation of Folder resource delegate.
 * 
 * @author manjunath
 * @see com.thed.zephyr.je.rest.delegate.FolderResourceDelegate
 *
 */
public class FolderResourceDelegateImpl implements FolderResourceDelegate {
	
	protected final Logger log = Logger.getLogger(FolderResourceDelegateImpl.class);
	
	private FolderManager folderManager;
	
	private JiraAuthenticationContext authContext;
	
	private ClusterLockService clusterLockService;
	
	private JobProgressService jobProgressService;
	
	private ScheduleManager scheduleManager;
	
	private ZFJCacheService zfjCacheService;
	
	private final ScheduleIndexManager scheduleIndexManager;
	
	private final EventPublisher eventPublisher;
	
	private CycleManager cycleManager;
	
	public FolderResourceDelegateImpl(FolderManager folderManager, JiraAuthenticationContext authContext, ClusterLockServiceFactory clusterLockServiceFactory,
			JobProgressService jobProgressService, ScheduleManager scheduleManager, ZFJCacheService zfjCacheService, ScheduleIndexManager scheduleIndexManager,
			EventPublisher eventPublisher, CycleManager cycleManager) {
		this.folderManager = folderManager;
		this.authContext = authContext;
		this.clusterLockService = clusterLockServiceFactory.getClusterLockService();
		this.jobProgressService = jobProgressService;
		this.scheduleManager = scheduleManager;
		this.zfjCacheService = zfjCacheService;
		this.scheduleIndexManager = scheduleIndexManager;
		this.eventPublisher = eventPublisher;
		this.cycleManager = cycleManager;
	}

	@Override
	@Transactional
	public Response createFolder(FolderRequest folderRequest, String loggedInUser, Map<String, String> valueHolder) {
		FolderResponse response = new FolderResponse();
		Map<String, Object> folderProperties = createFolderProperties(folderRequest, loggedInUser);
		//Saving the folder information into folder table
		Folder folder = folderManager.saveFolder(folderProperties);
		Map<String, Object> folderCycleMappingProperties = createFolderCycleMappingProperties(folderRequest, folder.getID(), loggedInUser);
		//Mapping the folder to cycle in the mapping table.
		folderManager.saveFolderCycleMapping(folderCycleMappingProperties);
		//Update modified_time for the cycle object.
		Cycle cycle = cycleManager.getCycle(folderRequest.getCycleId());
		if(cycle != null){
			cycle.setModifiedDate(new Date());
            cycle.save();
            Table<String, String, Object> changePropertyTable = HashBasedTable.create();
            changePropertyTable.put("FOLDER", ApplicationConstants.OLD, ApplicationConstants.NULL );
            changePropertyTable.put("FOLDER", ApplicationConstants.NEW, folderRequest.getName());
            // publishing CycleModifyEvent
            eventPublisher.publish(new CycleModifyEvent(cycle, changePropertyTable, EventType.CYCLE_UPDATED,
                    UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));
		}
		if(folderRequest.getClonedFolderId() != null) {
			return cloneSchedulesToFolder(folder, folderRequest.getClonedFolderId(), folderRequest.getProjectId(), folderRequest.getVersionId(), folderRequest.getCycleId(),folderRequest.getCloneCustomFields(), response);
		}
		response.setId(folder.getID());
		response.setResponseMessage(authContext.getI18nHelper().getText("zephyr.folder.creation.label", folder.getName(), "created"));
		response.setCycleId(folderRequest.getCycleId());
		response.setProjectId(folderRequest.getProjectId());
		response.setVersionId(folderRequest.getVersionId());
		response.setCycleName(valueHolder.get("cycleName"));
		response.setProjectKey(valueHolder.get("projectKey"));
		response.setVersionName(valueHolder.get("versionName"));
		return Response.ok(response).build();
	}
	
	private Response cloneSchedulesToFolder(Folder folder, Long clonedFolderId, Long projectId, Long versionId, Long cycleId, Boolean cloneCustomFields, FolderResponse response) {
		JSONObject jsonObject = new JSONObject();
		String errorMessage = null;
		if(Objects.isNull(clonedFolderId) && clonedFolderId.equals(-1L)) {
			errorMessage = authContext.getI18nHelper().getText("folder.clone.empty.error.message");
			getResponse(errorMessage, jsonObject, Status.NOT_ACCEPTABLE);
		}
		Folder clonedFolder = folderManager.getFolder(clonedFolderId);
		if(Objects.isNull(clonedFolder)) {
			errorMessage = authContext.getI18nHelper().getText("folder.clone.invalid.error.message");
			getResponse(errorMessage, jsonObject, Status.NOT_ACCEPTABLE);
		}
		String jobProgressToken = new UniqueIdGenerator().getStringId();
        JSONObject jsonObjectResponse = new JSONObject();
        try {
            jsonObjectResponse.put(ApplicationConstants.JOB_PROGRESS_TOKEN, jobProgressToken);
        } catch (JSONException e) {
            log.error("error getting job progress token", e);
        }
        jobProgressService.createJobProgress(ApplicationConstants.FOLDER_CLONE_JOB_PROGRESS, 0, jobProgressToken);
        jobProgressService.setEntityWithId(jobProgressToken, ApplicationConstants.FOLDER_ID_ENTITY, String.valueOf(folder.getID()));
		if(zfjCacheService.getCacheByWildCardKey("FOLDER_ID_PROGRESS_CHK" + "_" + String.valueOf(cycleId) + "_" + String.valueOf(folder.getID()))) {
			errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.je.add.tests.to.folder.in.progress");
			jobProgressService.addCompletedSteps(jobProgressToken, 1);
			jobProgressService.setErrorMessage(jobProgressToken, errorMessage);
			return Response.ok(jsonObjectResponse.toString()).build();
		}
        List<Schedule> schedules = scheduleManager.getSchedulesByCycleAndFolder(projectId, versionId, cycleId, -1, "OrderId:ASC", null, clonedFolderId);
        jobProgressService.addSteps(jobProgressToken, schedules.size());
		// final String lockName = ApplicationConstants.FOLDER_ENTITY + "_" + String.valueOf(cycleId) + "_" + folder.getID();
		final String lockName = ApplicationConstants.FOLDER_ENTITY + "_" + String.valueOf(cycleId) + "_" + clonedFolder.getID();
        final ClusterLock lock = clusterLockService.getLockForName(lockName);
        List<Schedule> scheduleList = new ArrayList<Schedule>();
        String loggedInUser = UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext));
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                if (lock.tryLock(0, TimeUnit.SECONDS)) {
					zfjCacheService.createOrUpdateCache("CLONE_FOLDER_PROGRESS_CHK" + "_" + String.valueOf(clonedFolder.getID()), String.valueOf(clonedFolder.getID()));
                	Date today = new Date();
                    for (Schedule schedule : schedules) {
                        Map<String, Object> schedMap = new HashMap<String, Object>();
                        schedMap.put("CYCLE_ID", schedule.getCycle().getID());
                        schedMap.put("DATE_CREATED", today);
                        schedMap.put("VERSION_ID", schedule.getVersionId());
                        schedMap.put("PROJECT_ID", schedule.getProjectId());
                        /*As per requirement, we need to reset comments*/
                        schedMap.put("COMMENT", "");
                        schedMap.put("ISSUE_ID", schedule.getIssueId());
                        schedMap.put("STATUS", String.valueOf(ApplicationConstants.UNEXECUTED_STATUS));
                        schedMap.put("CREATED_BY", loggedInUser);
                        schedMap.put("MODIFIED_BY", loggedInUser);
                        schedMap.put("ORDER_ID", scheduleManager.getMaxOrderId() + 1);
                        schedMap.put("EXECUTION_WORKFLOW_STATUS", ExecutionWorkflowStatus.CREATED);
                        schedMap.put("FOLDER_ID", folder.getID());
                        Schedule newSchedule = scheduleManager.saveSchedule(schedMap);
                        if (Objects.nonNull(cloneCustomFields) && cloneCustomFields) {
                            scheduleManager.cloneCustomFields(schedule.getID(), newSchedule, false);
                        }
                        scheduleList.add(newSchedule);
                        jobProgressService.addCompletedSteps(jobProgressToken,1);
                    }
                    try {
						//Need re-Index hence publishing event to do it.
						Map<String, Object> params = new HashMap<>();
						params.put("ENTITY_TYPE", "SCHEDULE_ID");
						eventPublisher.publish(new SingleScheduleEvent(scheduleList, params, EventType.EXECUTION_ADDED));

	                    response.setId(folder.getID());
	                    response.setResponseMessage(authContext.getI18nHelper().getText("zephyr.folder.creation.label", folder.getName(), "created"));		
	                } catch (Exception e) {
	                    log.error("Error Indexing Schedules for Clone schedules to folder:", e);
	                }
                    jobProgressService.addCompletedSteps(jobProgressToken, ApplicationConstants.JOB_STATUS_COMPLETED);
                    jobProgressService.setMessage(jobProgressToken,response.getResponseMessage());
            } else {
                String inProgressMsg = authContext.getI18nHelper().getText("zephyr.je.admin.clone.folder.already.in.progress");
                log.warn(inProgressMsg);                
                jobProgressService.setErrorMessage(jobProgressToken, inProgressMsg);
                jobProgressService.addCompletedSteps(jobProgressToken, 1);
            }
        } catch (InterruptedException e) {
            String error = "Clone schedules to folder operation interrupted";
            log.error("cloneSchedulesToFolder(): " + error, e);
            jobProgressService.addCompletedSteps(jobProgressToken, ApplicationConstants.JOB_STATUS_FAILED);
        } finally {
            lock.unlock();
            zfjCacheService.removeCacheByKey("CLONE_FOLDER_PROGRESS_CHK" + "_" + String.valueOf(clonedFolder.getID()));
            authContext.setLoggedInUser(null);
        }
      });
      return Response.ok().entity(jsonObjectResponse.toString()).build();
	}
	
	/**
	 * Method creates the map object which holds the column name and value to that specific column name for the table(folder).
	 *
	 * @param loggedInUser -- Logged In user name
	 * @return -- Returns the created map object which holds the column name and value to that specific column name for the table(folder).
	 */
	private Map<String, Object> createFolderProperties(FolderRequest folderRequest, String loggedInUser) {
		Map<String, Object> folderProperties = new HashMap<>();
		Date date = new Date();
		folderProperties.put("NAME", folderRequest.getName());
		folderProperties.put("DESCRIPTION", folderRequest.getDescription() == null ? "" : folderRequest.getDescription());
		folderProperties.put("DATE_CREATED", date);
		folderProperties.put("CREATED_BY", loggedInUser);
		return folderProperties;
	}
	
	/**
	 * Method creates the map object which holds the column name and value to that specific column name for the table(cycle folder mapping)
	 *
	 * @param folderId -- Folder id to be mapped to cycle.
	 * @param loggedInUser -- Logged In user name.
	 * @return -- Returns the created map object which holds the column name and value to that specific column name for the table(cycle folder mapping).
	 */
	private Map<String, Object> createFolderCycleMappingProperties(FolderRequest folderRequest, Integer folderId, String loggedInUser) {
		Map<String, Object> folderCycleMappingProperties = new HashMap<>();
		Date date = new Date();
		folderCycleMappingProperties.put("CYCLE_ID", (folderRequest.getCycleId() != null && !folderRequest.getCycleId().equals(ApplicationConstants.AD_HOC_CYCLE_ID_LONG)) ? folderRequest.getCycleId() : null);
		folderCycleMappingProperties.put("FOLDER_ID", folderId);
		folderCycleMappingProperties.put("DATE_CREATED", date);
		folderCycleMappingProperties.put("PROJECT_ID", folderRequest.getProjectId());
		folderCycleMappingProperties.put("VERSION_ID", folderRequest.getVersionId());
		folderCycleMappingProperties.put("CREATED_BY", loggedInUser);
		return folderCycleMappingProperties;
	}

	@SuppressWarnings("unchecked")
	@Override
	public FolderResponse updateFolder(Long projectId, Long versionId, Long cycleId, Folder existingFolder, String folderName, String folderDescription, String loggedInUser, Map<String, String> valueHolder) {
		folderManager.updateFolder(existingFolder, folderName, folderDescription, loggedInUser, (Map<String, Boolean>) zfjCacheService.getCacheByKey(projectId+""+versionId+""+cycleId, null));
		FolderResponse response = new FolderResponse();
		response.setId(existingFolder.getID());
		response.setResponseMessage(authContext.getI18nHelper().getText("zephyr.folder.creation.label", folderName, "updated"));
		response.setCycleId(cycleId);
		response.setProjectId(projectId);
		response.setVersionId(versionId);
		response.setCycleName(valueHolder.get("cycleName"));
		response.setProjectKey(valueHolder.get("projectKey"));
		response.setVersionName(valueHolder.get("versionName"));
		reindexSchedulesOnChange(projectId,versionId,cycleId,Long.valueOf(existingFolder.getID()));
		return response;
	}

	@SuppressWarnings("unchecked")
	@Override
	@Transactional
	public Response deleteFolder(Long projectId, Long versionId, Long cycleId, Long folderId) {
		final ApplicationUser user = authContext.getLoggedInUser();
		Folder folder = folderManager.getFolder(folderId);
		if(folder == null) {
			log.error("[Error] [Error code:"+ Response.Status.BAD_REQUEST.getStatusCode() + " " + Response.Status.BAD_REQUEST +" Error Message :"+
					authContext.getI18nHelper().getText("project.folder.not.exist"));
			return Response.status(Status.NOT_ACCEPTABLE).entity(authContext.getI18nHelper().getText("zephyr.common.invalid.parameter")).build();
		}
		String jobProgressToken = new UniqueIdGenerator().getStringId();
	    jobProgressService.createJobProgress(ApplicationConstants.COPY_TESTSTEPS_FROM_SOURCE_TO_DESTINATION, 0, jobProgressToken);
	    JSONObject jsonObjectResponse = new JSONObject();
        try {
            jsonObjectResponse.put(ApplicationConstants.JOB_PROGRESS_TOKEN, jobProgressToken);
        } catch (JSONException e) {
            log.error("Error in putting job progress token", e);
        }
		if(zfjCacheService.getCacheByWildCardKey("FOLDER_ID_PROGRESS_CHK" + "_" + String.valueOf(cycleId) + "_" + String.valueOf(folder.getID()))) {
			String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.je.add.tests.to.folder.in.progress");
			jobProgressService.addCompletedSteps(jobProgressToken, 1);
			jobProgressService.setErrorMessage(jobProgressToken, errorMessage);
			return Response.ok(jsonObjectResponse.toString()).build();
		}
        final String lockName = ApplicationConstants.FOLDER_ENTITY + "_" + String.valueOf(cycleId) + "_" + folderId;
        final ClusterLock lock = clusterLockService.getLockForName(lockName);
		Executors.newSingleThreadExecutor().submit(() -> {
	        try {
	        	if(lock.tryLock(0, TimeUnit.SECONDS)) {
	        		Future<Boolean> promise = scheduleManager.removeSchedulesByFolderIdAndCycleIdPromise(projectId, versionId, cycleId, folderId, jobProgressToken);
	        		promise.get();
	        		jobProgressService.addSteps(jobProgressToken, 2);
	        		String folderName = folder != null ? folder.getName() : "";
		    		int mappingResult = folderManager.removeCycleFolderMapping(projectId, versionId, cycleId, folderId);
		    		if(mappingResult == 1) {
		    			jobProgressService.addCompletedSteps(jobProgressToken, 1);
		    			mappingResult =  folderManager.removeFolder(folderId, (Map<String, Boolean>) zfjCacheService.getCacheByKey(projectId+""+versionId+""+cycleId, null));
		    			if(mappingResult == 0) {
		    				throw new RuntimeException("Error while deleting the folder");
		    			}
		    			//Update modified_time for the cycle object.
		    			Cycle cycle = cycleManager.getCycle(cycleId);
		    			if(cycle != null){
		    				cycle.setModifiedDate(new Date());
		    	            cycle.save();
		    	            Table<String, String, Object> changePropertyTable = HashBasedTable.create();
		    	            changePropertyTable.put("FOLDER", ApplicationConstants.OLD, folderName);
		    	            changePropertyTable.put("FOLDER", ApplicationConstants.NEW, ApplicationConstants.NULL);
		    	            // publishing CycleModifyEvent
		    	            eventPublisher.publish(new CycleModifyEvent(cycle, changePropertyTable, EventType.CYCLE_UPDATED,
		    	                    UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));
		    			}
		    			jobProgressService.addCompletedSteps(jobProgressToken, 1);
		    			jobProgressService.addCompletedSteps(jobProgressToken, ApplicationConstants.JOB_STATUS_COMPLETED);
		    			JSONObject jsonObject = new JSONObject();
		    			jsonObject.put("success", authContext.getI18nHelper().getText("zephyr.folder.creation.label", folderName, "deleted"));
			        	jobProgressService.setMessage(jobProgressToken, jsonObject.toString());
			        	// publishing FolderModifyEvent
                        eventPublisher.publish(new FolderModifyEvent(Lists.newArrayList(folder), null, EventType.FOLDER_DELETED,
                                user.getName(), cycleId, projectId));
			        	Map<String, Object> params = new HashMap<String, Object>();
                        params.put("ENTITY_TYPE", "FOLDER_ID");
                        params.put("ENTITY_VALUE", Lists.newArrayList(String.valueOf(folderId)));
                        eventPublisher.publish(new SingleScheduleEvent(null, params, com.thed.zephyr.je.event.EventType.FOLDER_DELETED));
					} else {
						throw new RuntimeException("Error while deleting the folder");
		    		}
		        } else {
	                String inProgressMsg = authContext.getI18nHelper().getText("zephyr.je.admin.delete.folder.already.in.progress");
	                jobProgressService.setErrorMessage(jobProgressToken, inProgressMsg);
	                jobProgressService.addCompletedSteps(jobProgressToken, 1);
		        }
	        } catch(Exception e) {
	        	log.error("Error : ", e);
	        	jobProgressService.addCompletedSteps(jobProgressToken, ApplicationConstants.JOB_STATUS_FAILED);
	        } finally {
                lock.unlock();
                authContext.setLoggedInUser(null);
            }
		});
		return Response.ok(jsonObjectResponse.toString()).build();
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean isFolderUniqueForCycle(Long projectId, Long versionId, Long cycleId, String folderName) {
		//cache is for reducing the database call.
		String key = projectId + "" + versionId + "" + cycleId;
		Object cacheValue = zfjCacheService.getCacheByKey(key, null);
		if(cacheValue != null){ 
			if(cacheValue instanceof Map) {
				Map<String, Boolean> cacheFolderNameMap = (Map<String, Boolean>) cacheValue;
				//checking against database if cache does not have folder name. 
				if((cacheFolderNameMap.containsKey(folderName) && cacheFolderNameMap.get(folderName)) || !folderManager.isFolderNameUniqueUnderCycle(projectId, versionId, cycleId, folderName)) {
					return false;
				}
				cacheFolderNameMap.put(folderName, true);
			}
		} else {
			if(!folderManager.isFolderNameUniqueUnderCycle(projectId, versionId, cycleId, folderName)) { //check is required if cache is deleted.
				return false;
			}
			Map<String, Boolean> cacheFolderNameMap = new ConcurrentHashMap<>();
			cacheFolderNameMap.put(folderName, true);
			zfjCacheService.createOrUpdateCache(key, cacheFolderNameMap);
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void rollbackFolderNameFromCache(Long projectId, Long versionId, Long cycleId, String folderName) {
		String key = projectId + "" + versionId + "" + cycleId;
		Object cacheValue = zfjCacheService.getCacheByKey(key, null);
		if(cacheValue != null) { 
			if(cacheValue instanceof Map) {
				Map<String, Boolean> cacheFolderNameMap = (Map<String, Boolean>) cacheValue;
				cacheFolderNameMap.put(folderName, false);
			}
		}
	}

	@Override
	@Transactional
	public void updateFoldersToSprint(Long projectId, Long versionId, Long folderId, Long cycleId, Long sprintId) {
		if(sprintId != null) {
			folderManager.updateFoldersToSprint(folderId, cycleId, versionId, projectId, sprintId);
            reindexSchedulesOnChange(projectId,versionId,cycleId,folderId);
		}
	}

	@Override
	public FolderCycleMapping getFolderCycleMapping(Long projectId, Long versionId, Long cycleId, Long folderId) {
		return folderManager.getFolderCycleMapping(folderId, cycleId, versionId, projectId);
	}
	
	@Override
	public Folder getFolder(Long folderId) {
		return folderManager.getFolder(folderId);
	}
	
	private Response getResponse(String errorMessage, JSONObject jsonObject, Status status) {
		try {
			jsonObject.put("error", errorMessage);
		} catch (JSONException e) {
		}
		return Response.status(status).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
	}
	
	@Override
	public List<Folder> fetchFolders(Long projectId, Long versionId, List<Long> cycleIds, Integer limit, Integer offset) {
		return folderManager.fetchFolders(projectId, versionId, cycleIds, limit, offset);
	}

    private void reindexSchedulesOnChange(Long projectId, Long versionId, Long cycleId, Long folderId) {
        List<Schedule> schedules = scheduleManager.getSchedulesByCycleAndFolder(projectId, versionId, cycleId, -1, null, null, folderId);
        if (schedules != null && schedules.size() > 0) {
            final EnclosedIterable<Schedule> enclosedSchedules = CollectionEnclosedIterable.copy(schedules);
            new Thread(() -> {
                try {
                    //Need Index update on the same thread for ZQL.
                    scheduleIndexManager.reIndexSchedule(enclosedSchedules, Contexts.nullContext());
                    log.debug("Reindex after cycle update is completed");
                } catch (Exception e) {
                    log.error("Error Indexing Schedule on Version Update in Cycle:", e);
                }
            }).start();
        }
    }

}
