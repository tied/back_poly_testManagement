package com.thed.zephyr.je.rest.delegate.impl;

import com.atlassian.beehive.compat.ClusterLock;
import com.atlassian.beehive.compat.ClusterLockService;
import com.atlassian.beehive.compat.ClusterLockServiceFactory;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.rest.json.beans.JiraBaseUrls;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.task.context.Contexts;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.usercompatibility.UserCompatibilityHelper;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.collect.CollectionEnclosedIterable;
import com.atlassian.jira.util.collect.EnclosedIterable;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.ApplicationProperties;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.thed.zephyr.je.attachment.AttachmentUtils;
import com.thed.zephyr.je.attachment.SessionKeys;
import com.thed.zephyr.je.event.CycleModifyEvent;
import com.thed.zephyr.je.event.EventType;
import com.thed.zephyr.je.event.ScheduleModifyEvent;
import com.thed.zephyr.je.event.SingleScheduleEvent;
import com.thed.zephyr.je.helper.CycleResourceHelper;
import com.thed.zephyr.je.index.ScheduleIndexManager;
import com.thed.zephyr.je.model.Cycle;
import com.thed.zephyr.je.model.ExecutionCf;
import com.thed.zephyr.je.model.ExecutionWorkflowStatus;
import com.thed.zephyr.je.model.Folder;
import com.thed.zephyr.je.model.FolderCycleMapping;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.permissions.model.PermissionType;
import com.thed.zephyr.je.permissions.service.ZephyrPermissionManager;
import com.thed.zephyr.je.rest.CycleResource;
import com.thed.zephyr.je.rest.CycleResource.CycleRequest;
import com.thed.zephyr.je.rest.CycleResource.CycleResponse;
import com.thed.zephyr.je.rest.delegate.CycleResourceDelegate;
import com.thed.zephyr.je.service.*;
import com.thed.zephyr.je.vo.ExecutionSummaryImpl;
import com.thed.zephyr.je.vo.SprintBean;
import com.thed.zephyr.util.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * CycleResource delegate, which serves for actual CycleResource Rest along with ValidatePermissions annotation.
 */

public class CycleResourceDelegateImpl implements CycleResourceDelegate {

    protected final Logger log = Logger.getLogger(CycleResourceDelegateImpl.class);

    private final JiraAuthenticationContext authContext;
    private final CycleManager cycleManager;
    private final ScheduleManager scheduleManager;
    private final ScheduleIndexManager scheduleIndexManager;
    private final DateTimeFormatterFactory dateTimeFormatterFactory;
    private final EventPublisher eventPublisher;
    private final ExportService exportService;
    private final ProjectManager projectManager;
    private final ZAPIValidationService zapiValidationService;
    private final PermissionManager permissionManager;
    private final ZFJCacheService zfjCacheService;
    private final VersionManager versionManager;
    private final ZephyrSprintService sprintService;
    private final ZephyrPermissionManager zephyrPermissionManager;
    private final JobProgressService jobProgressService;
    private final ClusterLockService clusterLockService;
    private final FolderManager folderManager;
    private final JiraBaseUrls jiraBaseUrls;
    
    public CycleResourceDelegateImpl(JiraAuthenticationContext authContext,
                                     CycleManager cycleManager, ScheduleManager scheduleManager, DateTimeFormatterFactory dateTimeFormatterFactory,
                                     final EventPublisher eventPublisher, final ExportService exportService, final ProjectManager projectManager,
                                     final ZAPIValidationService zapiValidationService, final PermissionManager permissionManager,
                                     final ScheduleIndexManager scheduleIndexManager, final VersionManager versionManager, final ZephyrSprintService sprintService,
                                     final ZephyrPermissionManager zephyrPermissionManager,
                                     final JobProgressService jobProgressService,
                                     ClusterLockServiceFactory clusterLockServiceFactory, FolderManager folderManager, JiraBaseUrls jiraBaseUrls,
                                     ZFJCacheService zfjCacheService) {
        this.authContext = authContext;
        this.cycleManager = cycleManager;
        this.scheduleManager = scheduleManager;
        this.dateTimeFormatterFactory = dateTimeFormatterFactory;
        this.eventPublisher = eventPublisher;
        this.exportService = exportService;
        this.projectManager = projectManager;
        this.zapiValidationService = zapiValidationService;
        this.permissionManager = permissionManager;
        this.scheduleIndexManager = scheduleIndexManager;
        this.versionManager = versionManager;
        this.sprintService = sprintService;
        this.zephyrPermissionManager = zephyrPermissionManager;
        this.jobProgressService = jobProgressService;
        this.clusterLockService = clusterLockServiceFactory.getClusterLockService();
        this.folderManager = folderManager;
        this.jiraBaseUrls = jiraBaseUrls;
        this.zfjCacheService=zfjCacheService;
    }


    @Override
    public Map<String, Object> getCycle(Long cycleId, Cycle cycle, ApplicationUser user) {
        Map<String, Object> cycleMap = convertToResponseMap(cycle);
        return cycleMap;
    }

    @Override
    public Response exportCycleOrFolder(final Integer cycleId, final Long versionId, final Project project, final Long folderId, final String sortQuery) throws UnsupportedEncodingException {
        final ApplicationUser user = authContext.getLoggedInUser();
        Long projectId = project.getId();
        JSONObject jsonObjectResponse = new JSONObject();
        final I18nHelper i18nHelper = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
        final Cycle cycle = cycleManager.getCycle(new Long(cycleId));
        ResponseBuilder builder = Response.status(Response.Status.NOT_ACCEPTABLE);
        builder.type(MediaType.APPLICATION_JSON);
        if (cycleId == null || (cycleId != ApplicationConstants.AD_HOC_CYCLE_ID && cycle == null)) {
            builder.entity(i18nHelper.getText("project.cycle.summary.notfound.error", cycleId));
            log.error("[Error] [Error code:"+ Response.Status.NOT_ACCEPTABLE.getStatusCode()+" "+Response.Status.NOT_ACCEPTABLE+" Error Message :"+
                    i18nHelper.getText("project.cycle.summary.notfound.error", cycleId));
            return builder.build();
        }
        if ((cycleId == ApplicationConstants.AD_HOC_CYCLE_ID && projectId == null && versionId == null)) {
            builder.entity(i18nHelper.getText("project.cycle.summary.missingpidvid.error", cycleId));
            log.error("[Error] [Error code:"+ Response.Status.NOT_ACCEPTABLE.getStatusCode()+" "+Response.Status.NOT_ACCEPTABLE+" Error Message :"+
                    i18nHelper.getText("project.cycle.summary.missingpidvid.error", cycleId));
            return builder.build();
        }
        /*To avoid nullPointer due to cycle could possibly being null, lets precalc all cycle attributes*/
        final Long projectIdVal = (projectId == null && cycle != null) ? cycle.getProjectId() : projectId;
        final Long versionIdVal = (versionId == null && cycle != null) ? cycle.getVersionId() : versionId;
        final Long startDate = (cycle != null) ? cycle.getStartDate() : null;
        final Long endDate = (cycle != null) ? cycle.getEndDate() : null;
        String cycleName = (cycle != null) ? cycle.getName() : i18nHelper.getText("zephyr.je.cycle.adhoc");
        final String build = (cycle != null) ? cycle.getBuild() : null;
        final String env = (cycle != null) ? cycle.getEnvironment() : null;

        // checking the project browse permissions
        if (!JiraUtil.hasBrowseProjectPermission(project, user)) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zapi.execution.move.invalid.projectid");
            log.error("[Error] [Error code:"+ Status.FORBIDDEN.getStatusCode()+" "+ Status.FORBIDDEN+" Error Message :"+errorMessage);
            return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "NullVersion", errorMessage, errorMessage);
        }
        StreamingOutput so = null; String fileName = null;
        if(folderId != null && !folderId.equals(-1l) && !folderId.equals(ApplicationConstants.ADHOC_SYSTEM_FOLDER_ID)) {
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
        	so = exportService.exportCycleOrFolder(cycleId, i18nHelper, projectIdVal,
                    versionIdVal, startDate, endDate, cycleName, build, env, folderId, sortQuery);
        	fileName = "Folder-" + URLEncoder.encode(folder.getName().length()>15?folder.getName().substring(0,15):folder.getName(), "UTF-8") + ".csv";
        } else {
        	so = exportService.exportCycleOrFolder(cycleId, i18nHelper, projectIdVal,
                    versionIdVal, startDate, endDate, cycleName, build, env, folderId, sortQuery);
        	fileName = "Cycle-" + URLEncoder.encode(cycleName.length()>15?cycleName.substring(0, 15):cycleName, "UTF-8") + ".csv";
        }

        /**
         * Fix for ZFJ-4415, * symbol doesn't get encoded.
         * If it exists then replacing it with empty string due to windows OS limitation.
         * */
        if(StringUtils.isNotBlank(fileName) && StringUtils.contains(fileName,"*")) {
            fileName = fileName.replaceAll("\\*", StringUtils.EMPTY);
        }

        final File tmpDir = AttachmentUtils.getTemporaryAttachmentDirectory();
        File tempAttachmentFile = new File(tmpDir, fileName);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(tempAttachmentFile);
            so.write(out);
        } catch (FileNotFoundException e) {
            log.error("exportCycle() : FileNotFoundException", e);
        } catch (WebApplicationException e) {
            log.error("exportCycle() : WebApplicationException", e);
        } catch (IOException e) {
            log.error("exportCycle() : IOException", e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    log.error("createAttachment() : IOException closing stream", e);
                    return null;
                }
            }
        }
        JSONObject ob = new JSONObject();
        try {
            ApplicationProperties applicationProperties = (ApplicationProperties) ZephyrComponentAccessor.getInstance().getComponent("applicationProperties");
            String fileUrl = applicationProperties.getBaseUrl() + "/plugins/servlet/export/exportAttachment?fileName=" + tempAttachmentFile.getName();
            ob.put("url", fileUrl);
        } catch (JSONException e) {
            log.warn("Error exporting file", e);
        }
        return Response.ok(ob.toString()).build();
    }

    @Override
    public Response createCycle(CycleRequest cycleRequest) {
        Map<String, String> errorMap = new HashMap<String, String>();
        CycleResponse response = new CycleResponse();
        Map<String, Object> map = convertToMap(cycleRequest);
        if (map.size() > 0) {
            try {
                Cycle newCycle = cycleManager.saveCycle(map);
                Date today = new Date();
                if (!StringUtils.isBlank(cycleRequest.clonedCycleId)) {
                    Cycle oldCycle = cycleManager.getCycle(new Long(cycleRequest.clonedCycleId));
                    if (null == oldCycle) {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.cycle.invalid.cloneCycleId", cycleRequest.clonedCycleId));
                        log.error("[Error] [Error code:" + Response.Status.BAD_REQUEST.getStatusCode() + " " + Response.Status.BAD_REQUEST + " Error Message :" +
                                authContext.getI18nHelper().getText("zephyr.cycle.invalid.cloneCycleId", cycleRequest.clonedCycleId));
                        return Response.status(Status.BAD_REQUEST).entity(jsonObject.toString()).build();
                    }
                    String jobProgressToken = new UniqueIdGenerator().getStringId();
                    JSONObject jsonObjectResponse = new JSONObject();
                    try {
                        jsonObjectResponse.put(ApplicationConstants.JOB_PROGRESS_TOKEN, jobProgressToken);
                    } catch (JSONException e) {
                        log.error("error getting job progress token", e);
                    }
                    jobProgressService.createJobProgress(ApplicationConstants.CYCLE_DELETE_JOB_PROGRESS, 0, jobProgressToken);
                    jobProgressService.setEntityWithId(jobProgressToken, ApplicationConstants.CYCLE_ID_ENTITY, String.valueOf(newCycle.getID()));

                    List<Schedule> schedules = scheduleManager.getSchedulesByCycleId(new Long(cycleRequest.versionId),
                            new Long(cycleRequest.projectId),
                            new Integer(cycleRequest.clonedCycleId), -1, "OrderId:ASC", null); // fix for ZFJ-1086: reorder chaos on cloning cycle*/

                    List<Schedule> scheduleList = new ArrayList<Schedule>();
                    jobProgressService.addSteps(jobProgressToken, schedules.size());
                    final String lockName = ApplicationConstants.CYCLE_ENTITY + "_" + cycleRequest.clonedCycleId;
                    final ClusterLock lock = clusterLockService.getLockForName(lockName);
                    List<Long> cycleIds = new ArrayList<>();
                    cycleIds.add(new Long(cycleRequest.clonedCycleId));
                    List<Folder> folders = folderManager.fetchFolders(oldCycle.getProjectId(), oldCycle.getVersionId(),
                            cycleIds, -1, 0);
                    Collections.reverse(folders);
                    String userName = UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext));
                    Boolean cloneCustomFields = Objects.nonNull(cycleRequest.cloneCustomFields) ? cycleRequest.cloneCustomFields : Boolean.FALSE;
                    Executors.newSingleThreadExecutor().submit(() -> {
                        try {
                            if (lock.tryLock(0, TimeUnit.SECONDS)) {
                                try {
                                    zfjCacheService.createOrUpdateCache("CLONE_CYCLE_PROGRESS_CHK" + "_" + String.valueOf(cycleRequest.clonedCycleId), cycleRequest.clonedCycleId);
                                    Map<String, Long> folderIdsMap = new HashMap<>();
                                    List<Schedule> folderLevelSchedules;
                                    for (Folder folder : folders) {
                                        folderLevelSchedules = scheduleManager.getSchedulesByCycleAndFolder(oldCycle.getProjectId(), oldCycle.getVersionId(),
                                                new Long(oldCycle.getID()), -1, "OrderId:ASC", null, new Long(folder.getID()));

                                        Folder clonedFolder = folderManager.cloneFolderToCycle(newCycle.getProjectId(), newCycle.getVersionId(),
                                                Long.valueOf(folder.getID() + ""), Long.valueOf(newCycle.getID() + ""), Long.valueOf(cycleRequest.clonedCycleId), userName);
                                        if (clonedFolder == null) {
                                            throw new RuntimeException("Error while creating cloned folders.");
                                        }
                                        folderIdsMap.put(folder.getID() + StringUtils.EMPTY, Long.valueOf(clonedFolder.getID() +  StringUtils.EMPTY));
                                        //Do it in transaction?
                                        if (CollectionUtils.isNotEmpty(folderLevelSchedules)) {
                                            for (Schedule schedule : folderLevelSchedules) {
                                                Schedule newSchedule = scheduleManager.saveSchedule(createScheduleProperties(schedule, newCycle.getID(),today,newCycle.getVersionId(),
                                                        folderIdsMap.get(schedule.getFolder().getID() + ""),userName));
                                                if(cloneCustomFields) {
                                                    scheduleManager.cloneCustomFields(schedule.getID(), newSchedule, false);
                                                }
                                                scheduleList.add(newSchedule);
                                                //Cycle Clone - Add Execution Changelog
                                                Table<String, String, Object> changePropertyTable = HashBasedTable.create();
                                                changePropertyTable.put("STATUS", ApplicationConstants.OLD, ApplicationConstants.NULL);
                                                changePropertyTable.put("STATUS", ApplicationConstants.NEW, newSchedule.getStatus());
                                                changePropertyTable.put("DATE_CREATED", ApplicationConstants.OLD, ApplicationConstants.NULL);
                                                changePropertyTable.put("DATE_CREATED", ApplicationConstants.NEW, newSchedule.getDateCreated());

                                                eventPublisher.publish(new ScheduleModifyEvent(newSchedule, changePropertyTable, EventType.EXECUTION_ADDED,
                                                        UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));
                                                jobProgressService.addCompletedSteps(jobProgressToken, 1);
                                                //Clone attachments
                                                //Clone issues
                                            }
                                        }
                                    }

                                    List<Schedule> cycleLevelSchedules = scheduleManager.getSchedulesByCycleAndFolder(new Long(cycleRequest.projectId), new Long(cycleRequest.versionId),
                                            new Long(cycleRequest.clonedCycleId), -1, "OrderId:ASC", null, null);
                                    if (CollectionUtils.isNotEmpty(cycleLevelSchedules)) {
                                        for (Schedule schedule : cycleLevelSchedules) {
                                            Schedule newSchedule = scheduleManager.saveSchedule(createScheduleProperties(schedule, newCycle.getID(),today,newCycle.getVersionId(),null, userName));
                                            if(cloneCustomFields) {
                                                scheduleManager.cloneCustomFields(schedule.getID(), newSchedule, false);
                                            }
                                            scheduleList.add(newSchedule);
                                            //Cycle Clone - Add Execution Changelog
                                            Table<String, String, Object> changePropertyTable = HashBasedTable.create();
                                            changePropertyTable.put("STATUS", ApplicationConstants.OLD, ApplicationConstants.NULL);
                                            changePropertyTable.put("STATUS", ApplicationConstants.NEW, newSchedule.getStatus());
                                            changePropertyTable.put("DATE_CREATED", ApplicationConstants.OLD, ApplicationConstants.NULL);
                                            changePropertyTable.put("DATE_CREATED", ApplicationConstants.NEW, String.valueOf(newSchedule.getDateCreated().getTime()));
                                            if (StringUtils.isNotBlank(newSchedule.getAssignedTo())) {
                                                changePropertyTable.put("ASSIGNED_TO", ApplicationConstants.OLD, ApplicationConstants.NULL);
                                                changePropertyTable.put("ASSIGNED_TO", ApplicationConstants.NEW, StringUtils.isEmpty(newSchedule.getAssignedTo()) ? ApplicationConstants.NULL : newSchedule.getAssignedTo());
                                            }
                                            eventPublisher.publish(new ScheduleModifyEvent(newSchedule, changePropertyTable, EventType.EXECUTION_ADDED,
                                                    UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));
                                            jobProgressService.addCompletedSteps(jobProgressToken, 1);
                                            //Clone attachments
                                            //Clone issues
                                        }
                                    }
                                    //Update Index for Cloned Cycles.
                                    try {
                                        //Need Index update on the same thread for ZQL.
                                        EnclosedIterable<Schedule> enclosedSchedules = CollectionEnclosedIterable.copy(scheduleList);
                                        scheduleIndexManager.indexSchedules(enclosedSchedules, Contexts.nullContext());
                                        //eventPublisher.publish(new SingleScheduleEvent(scheduleList, new HashMap<String,Object>(), EventType.EXECUTION_ADDED));
                                        response.id = String.valueOf(newCycle.getID());
                                        response.responseMessage = authContext.getI18nHelper().getText("cycle.creation.label", newCycle.getName(), "created");
                                        jobProgressService.addCompletedSteps(jobProgressToken, 1);
                                        jobProgressService.setMessage(jobProgressToken, response.responseMessage);

                                    } catch (Exception e) {
                                        log.error("Error Indexing Schedules for Cloned Cycle:", e);
                                    }
                                } finally {
                                    lock.unlock();
                                    zfjCacheService.removeCacheByKey("CLONE_CYCLE_PROGRESS_CHK" + "_" + String.valueOf(cycleRequest.clonedCycleId));
                                    authContext.setLoggedInUser(null);
                                }
                                return null;
                            } else {
                                String inProgressMsg = authContext.getI18nHelper().getText("zephyr.je.admin.clone.cycle.already.in.progress");
                                log.warn(inProgressMsg);
                                jobProgressService.setMessage(jobProgressToken, inProgressMsg);
                                return null;
                            }
                        } catch (InterruptedException e) {
                            String error = "Clone cycle operation interrupted";
                            log.error("cloneCycle(): " + error, e);
                            return null;
                        }
                    });
                    return Response.ok().entity(jsonObjectResponse.toString()).build();
                }
                response.id = String.valueOf(newCycle.getID());
                response.responseMessage = authContext.getI18nHelper().getText("cycle.creation.label", newCycle.getName(), "created");
                // return the entry
                log.debug(response.responseMessage);
                return Response.ok().entity(response).build();
            } catch (Exception e) {
                log.error("Error Creating Cycle=", e);
                ResponseBuilder builder = Response.status(Response.Status.NOT_ACCEPTABLE);
                builder.type(MediaType.APPLICATION_JSON);
                errorMap.put("generic", ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("project.cycle.summary.create.dialog.generic.error"));
                builder.entity(errorMap);
                return builder.build();
            }
        }
        response.responseMessage = "Error occurred while creating new cycle.";
        log.debug(response.responseMessage);
        return Response.ok(response.toString()).build();
    }

	@Override
    public Response updateCycle(CycleResource.CycleRequest cycleRequest) {
        //validate input.
        Map<String, String> errorMap = new HashMap<String, String>();
        Cycle cycle = cycleManager.getCycle(Long.valueOf(cycleRequest.id));
        //Cycle does not exist, return graceful error
        if (cycle == null) {
            errorMap.put("generic", ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("project.cycle.summary.notfound.error", cycleRequest.id));
            return buildResponseErrorMap(errorMap);
        }
        Long oldVersionID = cycle.getVersionId();
        //Version ID check for ZAPI
        if (StringUtils.isNotBlank(cycleRequest.versionId)) {
            long newVersionId = Long.valueOf(cycleRequest.versionId);
            Version version = ComponentAccessor.getVersionManager().getVersion(newVersionId);
            if (version == null && newVersionId != -1) {
                errorMap.put("versionId", authContext.getI18nHelper().getText("project.cycle.summary.create.dialog.validationError.version.mismatch", cycleRequest.versionId));
                return buildResponseErrorMap(errorMap);
            } else {
                if (newVersionId != -1) {
                    if (version.getProjectId().longValue() != cycle.getProjectId().longValue()) {
                        errorMap.put("versionId", authContext.getI18nHelper().getText("project.cycle.summary.create.dialog.validationError.version.mismatch", cycleRequest.versionId));
                        return buildResponseErrorMap(errorMap);
                    }
                }
            }
        }

        //Start Date and End Date check ZAPI
        DateTimeFormatter formatter = dateTimeFormatterFactory.formatter().forLoggedInUser().withSystemZone().withStyle(DateTimeStyle.DATE_PICKER);
        if (StringUtils.isNotEmpty(cycleRequest.endDate) && StringUtils.isNotEmpty(cycleRequest.startDate)) {
            validateCycleStartAndEndDate(cycleRequest.startDate, cycleRequest.endDate, errorMap, authContext.getI18nHelper());
        } else if (StringUtils.isNotEmpty(cycleRequest.startDate) && null == cycleRequest.endDate) {
            if (cycle.getEndDate() != null) {
                String formattedCycleEndDate = formatter.format(new Date(cycle.getEndDate()));
                validateCycleStartAndEndDate(cycleRequest.startDate, formattedCycleEndDate, errorMap, authContext.getI18nHelper());
            }
        } else if (null == cycleRequest.startDate && StringUtils.isNotEmpty(cycleRequest.endDate)) {
            if (cycle.getStartDate() != null) {
                String formattedCycleStartDate = formatter.format(new Date(cycle.getStartDate()));
                validateCycleStartAndEndDate(formattedCycleStartDate, cycleRequest.endDate, errorMap, authContext.getI18nHelper());
            }
        }

        if (errorMap.size() > 0) {
            return buildResponseErrorMap(errorMap);
        }

        Date startDate = convertToDate(cycleRequest.startDate, formatter);
        Date endDate = convertToDate(cycleRequest.endDate, formatter);
        //fetch the modified properties for change logs
        Table<String, String, Object> changePropertyTable = changePropertyTable(cycle, cycleRequest, startDate, endDate, "update");
        if (cycleRequest.build != null)
            cycle.setBuild(cycleRequest.build);

        if (cycleRequest.description != null)
            cycle.setDescription(cycleRequest.description);

        if (cycleRequest.environment != null)
            cycle.setEnvironment(cycleRequest.environment);

        if (cycleRequest.startDate != null) {
            if (startDate != null)
                cycle.setStartDate(startDate.getTime());
            else
                cycle.setStartDate(null);
        }

        if (cycleRequest.endDate != null) {
            if (endDate != null)
                cycle.setEndDate(endDate.getTime());
            else
                cycle.setEndDate(null);
        }
        List<Schedule> schedules = new ArrayList<Schedule>();
        long newVersionId = cycleRequest.versionId != null ? Long.valueOf(cycleRequest.versionId) : 0;
        Boolean fullCycleReindexNeeded = false;
        if (newVersionId != 0 && cycle.getVersionId().longValue() != newVersionId) {
            fullCycleReindexNeeded = true;
        }
        if (!StringUtils.equals(cycle.getName(), cycleRequest.name)) {
            fullCycleReindexNeeded = true;
        }
        if (cycleRequest.sprintId != null && (cycle.getSprintId() != null && !StringUtils.equals(String.valueOf(cycle.getSprintId()), cycleRequest.sprintId))) {
            fullCycleReindexNeeded = true;
        }
        if (fullCycleReindexNeeded) {
        	if(newVersionId != 0) { //In case of new version id change, change all the schedules only at cycle but also at folder.
        		schedules = scheduleManager.getSchedulesByCycle(cycle.getProjectId(), cycle.getVersionId(), Long.valueOf(cycle.getID()+""), -1, null, null);
        	} else {
        		schedules = scheduleManager.getSchedules(cycle.getVersionId(), cycle.getProjectId(), cycle.getID(), -1, null, null, cycleRequest.folderId,null);
        	}
        }

        if (cycleRequest.versionId != null) {
            cycle.setVersionId(Long.valueOf(cycleRequest.versionId));
        }

        if (cycleRequest.name != null) {
            cycle.setName(cycleRequest.name);
        }
        Long sprintId = StringUtils.isNotBlank(cycleRequest.sprintId) ? Long.valueOf(cycleRequest.sprintId) : null;
        if ((cycleRequest.sprintId != null && (cycleRequest.folderId != null)) || cycleRequest.folderId != null) {
        	if(!cycleRequest.folderId.equals(ApplicationConstants.ADHOC_SYSTEM_FOLDER_ID)) {
    			Folder folder = folderManager.getFolder(cycleRequest.folderId);
    			if(folder == null) {
    				log.error("[Error] [Error code:"+ Response.Status.BAD_REQUEST.getStatusCode() + " " + Response.Status.BAD_REQUEST +" Error Message :"+
    						authContext.getI18nHelper().getText("zephyr.common.invalid.parameter"));
    				return Response.status(Status.NOT_ACCEPTABLE).entity(authContext.getI18nHelper().getText("project.folder.not.exist")).build();
    			}
    		}
            folderManager.updateFoldersToSprint(cycleRequest.folderId, Long.valueOf(cycle.getID()+""), Long.valueOf(cycleRequest.versionId), Long.valueOf(cycleRequest.projectId), sprintId);
        } else {
        	cycle.setSprintId(sprintId);
        }

        //update modified by
        cycle.setModifiedBy(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));

        //set modified date.
        cycle.setModifiedDate(new Date());

		/*Not allowing users to update project*/
        //cycle.setProjectId(Long.valueOf(cycleRequest.PROJECT_ID));
        try {
            cycle.save();
            // publishing CycleModifyEvent
            eventPublisher.publish(new CycleModifyEvent(cycle, changePropertyTable, EventType.CYCLE_UPDATED,
                    UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));

            //Defensive check to avoid saving executions when ZAPI sends no versionId. Either we mandate the versionId on update or we ignore updating the executions.
            if (newVersionId != 0) {
                for (Schedule schedule : schedules) {
                    log.debug("Changing Schedule for Version Change:" + schedule.getVersionId());
                    schedule.setVersionId(newVersionId);
                    schedule.save();
                }
                if(fullCycleReindexNeeded) {
                	folderManager.updateDeletedVersionId(cycle.getProjectId(), oldVersionID, Long.valueOf(cycle.getID()+""), newVersionId);//update new version in folder cycle mapping table.
                }
            }
            //ReIndex Schedules if schedules.size > 0 (Version was changed)
            if (fullCycleReindexNeeded && schedules != null && schedules.size() > 0) {
                final EnclosedIterable<Schedule> enclosedSchedules = CollectionEnclosedIterable.copy(schedules);
                new Thread(() -> {
                    try {
                        //Need Index update on the same thread for ZQL.
                        scheduleIndexManager.reIndexSchedule(enclosedSchedules, Contexts.nullContext());
                        log.debug("Reindex after cycle update is completed");
                        //eventPublisher.publish(new SingleScheduleEvent(schedules, new HashMap<String,Object>(), EventType.CYCLE_MOVED));
                    } catch (Exception e) {
                        log.error("Error Indexing Schedule on Version Update in Cycle:", e);
                    }
                }).start();
            }
            CycleResponse response = new CycleResponse();
            response.id = cycleRequest.id;
            response.responseMessage = authContext.getI18nHelper().getText("cycle.creation.label", cycleRequest.name, "updated");
            log.debug(response.responseMessage);
            return Response.ok().entity(response).build();
        } catch (Exception e) {
            log.error("Error Updating Cycle=", e);
            ResponseBuilder builder = Response.status(Response.Status.NOT_ACCEPTABLE);
            builder.type(MediaType.APPLICATION_JSON);
            errorMap.put("generic", ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("project.cycle.summary.create.dialog.generic.error"));
            builder.entity(errorMap);
            return builder.build();
        }
    }

    @Override
    public Response deleteCycle(final Cycle cycle, String isFolderCycleDelete) {
        final ApplicationUser user = authContext.getLoggedInUser();

        String jobProgressToken = new UniqueIdGenerator().getStringId();
        JSONObject jsonObjectResponse = new JSONObject();
        try {
            jsonObjectResponse.put(ApplicationConstants.JOB_PROGRESS_TOKEN, jobProgressToken);
        } catch (JSONException e) {
            log.error("error getting job progress token", e);
        }
        jobProgressService.createJobProgress(ApplicationConstants.CYCLE_DELETE_JOB_PROGRESS, 0, jobProgressToken);
        jobProgressService.setEntityWithId(jobProgressToken, ApplicationConstants.CYCLE_ENTITY, String.valueOf(cycle.getID()));
        Project project = JiraUtil.getProjectThreadLocal();
        if(zfjCacheService.getCacheByWildCardKey("FOLDER_ID_PROGRESS_CHK" + "_" + String.valueOf(cycle.getID())+"_")) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.je.bulk.cycle.delete.in.progress");
            jobProgressService.addCompletedSteps(jobProgressToken, 1);
            jobProgressService.setErrorMessage(jobProgressToken, errorMessage);
            return Response.ok(jsonObjectResponse.toString()).build();
        }
        final String lockName = ApplicationConstants.CYCLE_ENTITY+"_"+cycle.getID();
        final ClusterLock lock = clusterLockService.getLockForName(lockName);
         Executors.newSingleThreadExecutor().submit(() -> {
            try {
                if (lock.tryLock(0, TimeUnit.SECONDS)) {
                    try {
                        if (authContext != null && authContext.getLoggedInUser() == null)
                            authContext.setLoggedInUser(user);
                        // checking the project browse permissions
                        if (!JiraUtil.hasBrowseProjectPermission(project, user)) {
                            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zapi.execution.move.invalid.projectid");
                            jobProgressService.addCompletedSteps(jobProgressToken, 1);
                            jobProgressService.setMessage(jobProgressToken, errorMessage);
                            return;
                        }
                        //Checking is the request from folder-cycle deletion or normal cycle deletion.
                        if("true".equalsIgnoreCase(isFolderCycleDelete)) {
                        	cycleManager.removeCycleAndFolder(cycle.getProjectId(), cycle.getVersionId(), Long.valueOf(cycle.getID()), jobProgressToken);
                        } else {
                        	cycleManager.removeCycle(Long.valueOf(cycle.getID()), jobProgressToken);
                        } 
                        // publishing CycleModifyEvent
                        eventPublisher.publish(new CycleModifyEvent(Lists.newArrayList(cycle), null, EventType.CYCLE_DELETED,
                                user.getName()));
                        Map<String, Object> params = new HashMap<String, Object>();
                        params.put("ENTITY_TYPE", "CYCLE_ID");
                        params.put("ENTITY_VALUE", Lists.newArrayList(String.valueOf(cycle.getID())));
                        eventPublisher.publish(new SingleScheduleEvent(null, params, EventType.EXECUTION_DELETED));
                        zfjCacheService.removeCacheByKey(String.valueOf(cycle.getID()));
                    } finally {
                        lock.unlock(); // release lock
                        authContext.setLoggedInUser(null);
                    }
                }else{
                    String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.je.bulk.cycle.delete.in.progress");
                    log.debug(errorMessage);
                    jobProgressService.addCompletedSteps(jobProgressToken, 1);
                    jobProgressService.setErrorMessage(jobProgressToken, errorMessage);
                    return;
                }

            } catch (Exception e) {
                log.error("Error on Cycle Delete:", e);
            }
        });
        return Response.ok(jsonObjectResponse.toString()).build();
    }

    @Override
    public Response getCycles(HttpServletRequest req, Long versionId, Long cycleId, Integer offset, String issueId, String expand) {
        final ApplicationUser user = authContext.getLoggedInUser();
        Project project = JiraUtil.getProjectThreadLocal();
        JSONObject ob = null;
        //Turning off Pagination. easiest way to do is out here, ignore the offset sent
        offset = -1;
        try {
            if (null != project && null == versionId && null == cycleId && StringUtils.isBlank(issueId)) {
                // checking the project browse permissions
                if (!JiraUtil.hasBrowseProjectPermission(project, user)) {
                    String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zapi.execution.move.invalid.projectid");
                    log.error("[Error] [Error code:"+ Status.FORBIDDEN.getStatusCode()+" "+Status.FORBIDDEN+" Error Message :"+errorMessage);
                    return JiraUtil.buildErrorResponse(Response.Status.FORBIDDEN, "NullVersion", errorMessage, errorMessage);
                }

                Set<Long> versionSet = scheduleManager.getDistinctVersionsByProjectId(project.getId());
                ob = new JSONObject();
                for (Long version : versionSet) {
                    ob.append(String.valueOf(version), getExecutionStatusByCycleId(req, project.getId(), version, null, null, offset, expand));
                }
            } else if (StringUtils.isNotBlank(issueId)) {
                if (StringUtils.isNotBlank(issueId) && versionId == null) {
                    Map<String, String> errorMap = zapiValidationService.validateIssueById(issueId);
                    if (errorMap != null && !errorMap.isEmpty()){
                        log.error("[Error] [Error code:"+ Status.BAD_REQUEST.getStatusCode()+" "+Status.BAD_REQUEST+" Error Message :"+errorMap);
                        return Response.status(Response.Status.BAD_REQUEST).entity(errorMap).cacheControl(ZephyrCacheControl.never()).build();
                    }
                }
                ob = getCycles(req, issueId, versionId, project.getId(), offset, expand);
            } else {
                ob = getExecutionStatusByCycleId(req, project.getId(), versionId, null, cycleId, offset, expand);
                setSelectedVersion(req, versionId == null ? "0" : versionId.toString());
            }
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ImmutableMap.of("error", ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().
                    getText("zapi.get.cycles.invalid.input.error"))).cacheControl(ZephyrCacheControl.never()).build();
        }

        return Response.ok(ob.toString()).cacheControl(ZephyrCacheControl.never()).build();
    }

    @Override
    public Response moveExecutionsToCycle(Long cycleId, Map<String, Object> params) {
        CycleResourceHelper cycleResourceHelper = new CycleResourceHelper(scheduleManager, authContext, projectManager, cycleManager, scheduleIndexManager, zephyrPermissionManager, jobProgressService,folderManager);
        params.put("action", "move");
        String jobProgressToken = new UniqueIdGenerator().getStringId();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(ApplicationConstants.JOB_PROGRESS_TOKEN,jobProgressToken);
        } catch (JSONException e) {
            log.error("error getting job progress token",e);
        }
        Response response = cycleResourceHelper.moveExecutionsToCycle(cycleId, params, jobProgressToken);
        if(response.getStatus() != Status.OK.getStatusCode()) {
        	return response;
        }
        return Response.ok(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
    }

    @Override
    public Response copyExecutionsToCycle(Long cycleId, Map<String, Object> params) {
        CycleResourceHelper cycleResourceHelper = new CycleResourceHelper(scheduleManager, authContext, projectManager, cycleManager, scheduleIndexManager, zephyrPermissionManager, jobProgressService,folderManager);
        params.put("action", "copy");
        String jobProgressToken = new UniqueIdGenerator().getStringId();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(ApplicationConstants.JOB_PROGRESS_TOKEN,jobProgressToken);
        } catch (JSONException e) {
            log.error("error getting job progress token",e);
        }
        Response response = cycleResourceHelper.moveExecutionsToCycle(cycleId, params, jobProgressToken);
        if(response.getStatus() != Status.OK.getStatusCode()) {
        	return response;
        }
        return Response.ok(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
    }


    /**
     * @param cycle
     * @return
     */
    private Map<String, Object> convertToResponseMap(Cycle cycle) {
        Map<String, Object> cycleMap = new HashMap<String, Object>();
        cycleMap.put("id", cycle.getID());
        cycleMap.put("build", cycle.getBuild() != null ? cycle.getBuild() : "");
        cycleMap.put("name", cycle.getName());
        cycleMap.put("description", cycle.getDescription() != null ? cycle.getDescription() : "");
        cycleMap.put("projectId", cycle.getProjectId());
        cycleMap.put("versionId", cycle.getVersionId());
        String versionName = authContext.getI18nHelper().getText("zephyr.je.version.unscheduled");        // ApplicationConstants.UNSCHEDULED_VERSION_NAME;
        if (cycle.getVersionId() != ApplicationConstants.UNSCHEDULED_VERSION_ID) {
            Version version = versionManager.getVersion(cycle.getVersionId());
            if (version != null) {
                versionName = version.getName();
            }
        }
        cycleMap.put("versionName", versionName);
        cycleMap.put("sprintId", cycle.getSprintId());
        cycleMap.put("environment", cycle.getEnvironment() != null ? cycle.getEnvironment() : "");
        SimpleDateFormat dateFormatter = new SimpleDateFormat(ApplicationConstants.DATE_FORMAT_SHORT, authContext.getLocale());
        if (cycle.getStartDate() != null) {
            String formattedCycleStartDate = dateFormatter.format(new Date(cycle.getStartDate()));
            cycleMap.put("startDate", formattedCycleStartDate);
        }
        if (cycle.getEndDate() != null) {
            String formattedCycleEndDate = dateFormatter.format(new Date(cycle.getEndDate()));
            cycleMap.put("endDate", formattedCycleEndDate);
        }
        cycleMap.put("createdBy", cycle.getCreatedBy() != null ? cycle.getCreatedBy() : "");
        cycleMap.put("modifiedBy", cycle.getModifiedBy() != null ? cycle.getModifiedBy() : "");
        return cycleMap;
    }


    /**
     * Build Error Map
     *
     * @param errorMap
     * @return
     */
    private Response buildResponseErrorMap(Map<String, String> errorMap) {
        Response.ResponseBuilder builder = Response.status(Response.Status.NOT_ACCEPTABLE);
        builder.type(MediaType.APPLICATION_JSON);
        builder.entity(errorMap);
        log.error("[Error] [Error code:"+ Status.NOT_ACCEPTABLE.getStatusCode()+" "+Status.NOT_ACCEPTABLE+" Error Message :"+errorMap);
        return builder.build();
    }

    private Map<String, Object> convertToMap(CycleRequest cycleRequest) {
        Map<String, Object> cycleMap = new HashMap<String, Object>();

        if (cycleRequest.id != null) {
            cycleMap.put("ID", cycleRequest.id);
        }

        cycleMap.put("BUILD", cycleRequest.build);
        cycleMap.put("NAME", cycleRequest.name);
        cycleMap.put("DESCRIPTION", cycleRequest.description);
        cycleMap.put("ENVIRONMENT", cycleRequest.environment);
        cycleMap.put("PROJECT_ID", cycleRequest.projectId != null ? Long.valueOf(cycleRequest.projectId) : null);
        cycleMap.put("VERSION_ID", cycleRequest.versionId != null ? Long.valueOf(cycleRequest.versionId) : null);
        cycleMap.put("SPRINT_ID", cycleRequest.sprintId != null ? Long.valueOf(cycleRequest.sprintId) : null);
        String userKey = UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext));
        cycleMap.put("MODIFIED_BY", userKey);
        cycleMap.put("CREATED_BY", userKey);

        DateTimeFormatter formatter = dateTimeFormatterFactory.formatter().forLoggedInUser().withSystemZone().withStyle(DateTimeStyle.DATE_PICKER);
        Date startDate = convertToDate(cycleRequest.startDate, formatter);
        if (startDate != null)
            cycleMap.put("START_DATE", startDate.getTime());

        Date endDate = convertToDate(cycleRequest.endDate, formatter);
        if (endDate != null)
            cycleMap.put("END_DATE", endDate.getTime());

        // Created date for the cycle.
        cycleMap.put("DATE_CREATED", new Date());
        return cycleMap;
    }

    /**
     * @param dateString
     * @param formatter
     * @return Date
     */
    private Date convertToDate(String dateString, DateTimeFormatter formatter) {
        if (!StringUtils.isBlank(dateString)) {
            Date date = formatter.parse(dateString);
            return date;
        }
        return null;
    }

    /**
     * To gather the changes to be saved for audit logs. Typically, which field/property underwent change,
     * what was the old value and what is the new value.
     *
     * @param cycle
     * @param cycleRequest
     * @param startDate
     * @param endDate
     * @return changePropertyTable
     */
    private Table<String, String, Object> changePropertyTable(Cycle cycle, CycleRequest cycleRequest, Date startDate, Date endDate, String action) {
        Table<String, String, Object> changePropertyTable = null;
        if (cycle == null || cycleRequest == null)
            return null;

        changePropertyTable = HashBasedTable.create();

        if (StringUtils.equalsIgnoreCase(action, "create") || (StringUtils.equalsIgnoreCase(action, "update") && cycleRequest.name != null)) {
            if (!(cycle.getName().equalsIgnoreCase(cycleRequest.name))) {
                changePropertyTable.put("NAME", ApplicationConstants.OLD, StringUtils.isEmpty(cycle.getName()) ? ApplicationConstants.NULL : cycle.getName());
                changePropertyTable.put("NAME", ApplicationConstants.NEW, StringUtils.isEmpty(cycleRequest.name) ? ApplicationConstants.NULL : cycleRequest.name);
            }
        }

        if (StringUtils.equalsIgnoreCase(action, "create") || (StringUtils.equalsIgnoreCase(action, "update") && cycleRequest.description != null)) {
            if (!StringUtils.equalsIgnoreCase(cycle.getDescription(), cycleRequest.description)) {
                changePropertyTable.put("DESCRIPTION", ApplicationConstants.OLD, StringUtils.isEmpty(cycle.getDescription()) ? ApplicationConstants.NULL : cycle.getDescription());
                changePropertyTable.put("DESCRIPTION", ApplicationConstants.NEW, StringUtils.isEmpty(cycleRequest.description) ? ApplicationConstants.NULL : cycleRequest.description);
            }
        }

        if (StringUtils.equalsIgnoreCase(action, "create") || (StringUtils.equalsIgnoreCase(action, "update") && cycleRequest.environment != null)) {
            if (!StringUtils.equalsIgnoreCase(cycle.getEnvironment(), cycleRequest.environment)) {
                changePropertyTable.put("ENVIRONMENT", ApplicationConstants.OLD, StringUtils.isEmpty(cycle.getEnvironment()) ? ApplicationConstants.NULL : cycle.getEnvironment());
                changePropertyTable.put("ENVIRONMENT", ApplicationConstants.NEW, StringUtils.isEmpty(cycleRequest.environment) ? ApplicationConstants.NULL : cycleRequest.environment);
            }
        }

        if (StringUtils.equalsIgnoreCase(action, "create") || (StringUtils.equalsIgnoreCase(action, "update") && cycleRequest.versionId != null)) {
            if (!ObjectUtils.equals(cycle.getVersionId(), Long.valueOf(cycleRequest.versionId))) {
                changePropertyTable.put("VERSION_ID", ApplicationConstants.OLD, String.valueOf(cycle.getVersionId()));
                changePropertyTable.put("VERSION_ID", ApplicationConstants.NEW, cycleRequest.versionId);
            }
        }

        if (StringUtils.equalsIgnoreCase(action, "create") || (StringUtils.equalsIgnoreCase(action, "update") && cycleRequest.sprintId != null)) {
            Long sprintId = null;
            if (StringUtils.isNotBlank(cycleRequest.sprintId)) {
                sprintId = Long.parseLong(cycleRequest.sprintId);
            }
            if (!ObjectUtils.equals(cycle.getSprintId(), sprintId)) {
                changePropertyTable.put("SPRINT_ID", ApplicationConstants.OLD, String.valueOf(cycle.getSprintId()));
                changePropertyTable.put("SPRINT_ID", ApplicationConstants.NEW, cycleRequest.sprintId);
            }
        }

        if (StringUtils.equalsIgnoreCase(action, "create") || (StringUtils.equalsIgnoreCase(action, "update") && cycleRequest.build != null)) {
            if (!StringUtils.equalsIgnoreCase(cycle.getBuild(), cycleRequest.build)) {
                changePropertyTable.put("BUILD", ApplicationConstants.OLD, StringUtils.isEmpty(cycle.getBuild()) ? ApplicationConstants.NULL : cycle.getBuild());
                changePropertyTable.put("BUILD", ApplicationConstants.NEW, StringUtils.isEmpty(cycleRequest.build) ? ApplicationConstants.NULL : cycleRequest.build);
            }
        }

        String newStartDateStr = null == startDate ? ApplicationConstants.NULL : String.valueOf(startDate.getTime());
        String newEndDateStr = null == endDate ? ApplicationConstants.NULL : String.valueOf(endDate.getTime());
        String oldStartDateStr = null == cycle.getStartDate() ? ApplicationConstants.NULL : cycle.getStartDate().toString();
        String oldEndDateStr = null == cycle.getEndDate() ? ApplicationConstants.NULL : cycle.getEndDate().toString();

        if (StringUtils.equalsIgnoreCase(action, "create") || (StringUtils.equalsIgnoreCase(action, "update") && cycleRequest.startDate != null)) {
            if (!(oldStartDateStr.equals(newStartDateStr))) {
                // Table doesn't permit 'null' values, hence passing in String "NULL".
                changePropertyTable.put("START_DATE", ApplicationConstants.OLD, oldStartDateStr);
                changePropertyTable.put("START_DATE", ApplicationConstants.NEW, newStartDateStr);
            }
        }

        if (StringUtils.equalsIgnoreCase(action, "create") || (StringUtils.equalsIgnoreCase(action, "update") && cycleRequest.endDate != null)) {
            if (!(oldEndDateStr.equals(newEndDateStr))) {
                // Table doesn't permit 'null' values, hence passing in String "NULL".
                changePropertyTable.put("END_DATE", ApplicationConstants.OLD, oldEndDateStr);
                changePropertyTable.put("END_DATE", ApplicationConstants.NEW, newEndDateStr);
            }
        }
        return changePropertyTable;
    }


    /**
     * Temporary method to determine issue version
     *
     * @param issue
     * @return
     */
    private Long getIssueVersion(Issue issue) {
        Collection<Version> versions = issue.getFixVersions();
        if (versions != null && versions.size() > 0) {
            /*@TODO We are only taking first version into considerations, this will change in final version*/
            return versions.iterator().next().getId();
        } else {
            versions = issue.getAffectedVersions();
            if (versions != null && versions.size() > 0) {
				/*@TODO We are only taking first version into considerations, this will change in final version*/
                return versions.iterator().next().getId();
            }
        }
        return new Long(ApplicationConstants.UNSCHEDULED_VERSION_ID);
    }

    /**
     * Sets VersionSelected which is good for current session
     *
     * @param selectedVersion
     */
    private void setSelectedVersion(final HttpServletRequest req, final String selectedVersion) {
        req.getSession(false).getServletContext().setAttribute(SessionKeys.CYCLE_SUMMARY_VERSION + authContext.getLoggedInUser(), selectedVersion);
    }


    /**
     * @param req
     * @param projectId
     * @param versionId
     * @param userName
     * @param cycleId
     * @param offset
     * @param expand
     * @return JSONObject
     */
    @SuppressWarnings("unchecked")
    private JSONObject getExecutionStatusByCycleId(final HttpServletRequest req, Long projectId, Long versionId, String userName, Long cycleId, Integer offset, String expand) {
        List<Cycle> cycles = null;
        if (cycleId == null || cycleId == 0) {
            cycles = cycleManager.getCyclesByVersion(versionId, projectId, offset);
        } else if (cycleId != ApplicationConstants.AD_HOC_CYCLE_ID) {    //No need to lookup for ADHoc, we will do it by default anyways
            cycles = Lists.newArrayList(cycleManager.getCycle(cycleId));
        }
        Integer recordCount = cycleManager.getCycleCountByVersionId(versionId, projectId);
        if (recordCount == null) {
            recordCount = cycles.size();
        }
        Map<String, String> cycleActionMap = null;
        if (req.getSession(false) != null)
            cycleActionMap = (HashMap<String, String>) req.getSession(false).getAttribute(SessionKeys.CYCLE_SUMMARY_DETAIL);

        JSONObject ob = new JSONObject();
        LocalDate today = new DateTime(System.currentTimeMillis()).toLocalDate();

        try {
    		/*Adhoc cycle. Add only if its not a paginated page or if requested for a single cycle*/
            boolean shouldIncludeAdhoc = (cycleId == null || cycleId == 0 || cycleId == ApplicationConstants.AD_HOC_CYCLE_ID);
            if ((offset == null || offset.intValue() == 0 || offset.intValue() == -1) && shouldIncludeAdhoc) {
                populateCycleSummaryInJson(ob, versionId, projectId, null, ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.je.cycle.adhoc"),
                        "", null, null, today, null, null, null, null, null, userName, cycleActionMap, expand);
            }
            if (cycles != null) {
                for (Cycle cycle : cycles) {
                    if (null != cycle) {
                        populateCycleSummaryInJson(ob, cycle.getVersionId(), cycle.getProjectId(), cycle.getID(), cycle.getName(), cycle.getDescription(), cycle.getStartDate(), cycle.getEndDate(),
                                today, cycle.getBuild(), cycle.getEnvironment(), cycle.getCreatedBy(), cycle.getModifiedBy(), cycle.getSprintId(), userName, cycleActionMap, expand);
                    }
                }
                //ob.put("offsetCount", offset + (cycles.size() + 1));
            }
            ob.put("recordsCount", (recordCount + 1));
        } catch (JSONException e) {
            log.error("Failed Creating JSON Data", e);
            return null;
        }

        return ob;
    }


    private Map<String, Object> populateCycleSummaryInJson(JSONObject ob, Long versionId, Long projectId, Integer cycleId, String cycleName, String cycleDesc,
                                                           Long cycleStartDateLong, Long cycleEndDate, LocalDate today, String build, String environment, String createdBy, String modifiedBy, Long sprintId, String userName, Map<String, String> cycleActionMap, String expand) throws JSONException {
        int totalExecuted = 0;
        int totalExecutions = 0;
        Map<String, Object> cntByCycleIdAndStatus = new HashMap<String, Object>();
        JSONObject jsonObject = new JSONObject();
        if (StringUtils.equalsIgnoreCase(expand, "executionSummaries")) {
            List<ExecutionSummaryImpl> allSummaries = scheduleManager.getExecutionDetailsByCycle(versionId, projectId, cycleId, userName);
            JSONArray expectedOperators = new JSONArray();
            for (ExecutionSummaryImpl executionSummary : allSummaries) {
                if (executionSummary.getExecutionStatusKey().intValue() != -1 &&
                        !StringUtils.equalsIgnoreCase(executionSummary.getExecutionStatusName(), "Unexecuted")) {
                    totalExecuted += executionSummary.getCount();
                }
                totalExecutions += executionSummary.getCount();
                expectedOperators.put(executionSummaryToJSON(executionSummary));
            }
            jsonObject.put("executionSummary", expectedOperators);
            jsonObject.put("executionSummary", expectedOperators);
        } else {
            jsonObject.put("executionSummary", new JSONArray());
        }

        cntByCycleIdAndStatus.put("executionSummaries", jsonObject);
        cntByCycleIdAndStatus.put("expand", "executionSummaries");
        cntByCycleIdAndStatus.put("totalExecutions", totalExecutions);
        cntByCycleIdAndStatus.put("totalExecuted", totalExecuted);
        Integer totalCycleExecutions = scheduleManager.getSchedulesCount(versionId, projectId, cycleId, null);
        cntByCycleIdAndStatus.put("totalCycleExecutions", totalCycleExecutions);
        
        if(cycleId != null) {
        	Integer foldersCount = folderManager.getFoldersCountForCycle(projectId, versionId, Long.valueOf(cycleId+""));
        	cntByCycleIdAndStatus.put(ApplicationConstants.TOTAL_FOLDERS, foldersCount);
        	Integer cycleExecutionDefectsCount = scheduleManager.getTotalDefectsCountByCycle(Long.valueOf(cycleId+""), projectId, versionId);
        	cntByCycleIdAndStatus.put(ApplicationConstants.TOTAL_DEFECTS, cycleExecutionDefectsCount);
        }

        String isCycleStarted = "";
        String formattedCycleStartDate = "";
        DateTimeFormatter dateFormatter = dateTimeFormatterFactory.formatter().forLoggedInUser().withSystemZone().withStyle(DateTimeStyle.DATE_PICKER);
        if (cycleStartDateLong != null) {
            LocalDate cycleStartDate = new DateTime(cycleStartDateLong).toLocalDate();
            formattedCycleStartDate = dateFormatter.format(new Date(cycleStartDateLong));
            isCycleStarted = String.valueOf(cycleStartDate.isBefore(today));
        }
        Project project = projectManager.getProjectObj(projectId);
        cntByCycleIdAndStatus.put("projectKey", project.getKey());
        cntByCycleIdAndStatus.put("projectId", projectId);
        cntByCycleIdAndStatus.put("started", isCycleStarted);
        cntByCycleIdAndStatus.put("startDate", formattedCycleStartDate);
        if (sprintId != null) {
            cntByCycleIdAndStatus.put("sprintId", sprintId);
        }
        String versionName = authContext.getI18nHelper().getText("zephyr.je.version.unscheduled");        // ApplicationConstants.UNSCHEDULED_VERSION_NAME;
        if (versionId != ApplicationConstants.UNSCHEDULED_VERSION_ID) {
            Version version = versionManager.getVersion(versionId);
            if (version != null) {
                versionName = version.getName();
            }
        }
        cntByCycleIdAndStatus.put("versionName", versionName);
        cntByCycleIdAndStatus.put("versionId", versionId);
        cntByCycleIdAndStatus.put("build", build != null ? build : "");
        cntByCycleIdAndStatus.put("environment", StringUtils.isNotBlank(environment) ? environment : "");

        String isCycleEnded = "";
        String formattedCycleEndDate = "";
        if (cycleEndDate != null) {
            LocalDate cycleEndDateLocal = new DateTime(cycleEndDate).toLocalDate();
            formattedCycleEndDate = dateFormatter.format(new Date(cycleEndDate));
            isCycleEnded = String.valueOf(cycleEndDateLocal.isBefore(today));
        }
        cntByCycleIdAndStatus.put("ended", isCycleEnded);
        cntByCycleIdAndStatus.put("endDate", formattedCycleEndDate);

        cntByCycleIdAndStatus.put("name", cycleName);
        cntByCycleIdAndStatus.put("description", (cycleDesc != null) ? cycleDesc : "");

        cntByCycleIdAndStatus.put("modifiedBy", (modifiedBy != null) ? modifiedBy : "");
        if (createdBy != null) {
            User user = UserCompatibilityHelper.getUserForKey(createdBy);
            cntByCycleIdAndStatus.put("createdBy", (user != null && user.isActive()) ? user.getName() : createdBy);
            cntByCycleIdAndStatus.put("createdByDisplay", (user != null && user.isActive()) ? user.getDisplayName() : createdBy);
        }
        if (cycleId == null) {
            cycleId = -1;
        }
        String compositekey = String.valueOf(cycleId) + ":" + String.valueOf(versionId);
        if (cycleActionMap != null && cycleActionMap.containsKey(compositekey)) {
            String cycleSummaryDetail = cycleActionMap.get(compositekey);
            String[] keyValues = StringUtils.split(cycleSummaryDetail, ",");
            for (String keyValue : keyValues) {
                String[] properties = StringUtils.split(keyValue, "=");
                if (properties.length == 2) {
                    cntByCycleIdAndStatus.put(properties[0], properties[1]);
                } else {
                    cntByCycleIdAndStatus.put(properties[0], "");
                }
            }
        }
        cntByCycleIdAndStatus.put("isExecutionWorkflowEnabledForProject",JiraUtil.getExecutionWorkflowEnabled(project.getId()));
        ob.put(String.valueOf(cycleId != null ? cycleId : -1), cntByCycleIdAndStatus);
        return cntByCycleIdAndStatus;
    }


    /**
     * Creates JSON Structure for Execution Summary
     *
     * @param executionSummary
     * @return
     * @throws JSONException
     */
    private JSONObject executionSummaryToJSON(ExecutionSummaryImpl executionSummary) throws JSONException {
        JSONObject object = new JSONObject();
        object.put("count", executionSummary.getCount());
        object.put("statusKey", executionSummary.getExecutionStatusKey());
        object.put("statusName", executionSummary.getExecutionStatusName());
        object.put("statusColor", executionSummary.getExecutionStatusColor());
        object.put("statusDescription", executionSummary.getExecutionStatusDescription());
        return object;
    }

    /**
     * Retrieves list of cycle
     * if versionId/Pid is provided, cycle list is retrieved for that version
     * if versionId is not provided, cycles are retrieved for the fixed version of issue.
     *
     * @param req
     * @param issueId
     * @param versionId
     * @param projectId
     * @param offset
     * @param expand
     * @return JSONObject
     */
    private JSONObject getCycles(final HttpServletRequest req, String issueId, Long versionId, Long projectId, Integer offset, String expand) {
        if ((projectId == null || versionId == null) && StringUtils.isNotBlank(issueId)) {
            MutableIssue issue = ComponentAccessor.getIssueManager().getIssueObject(new Long(issueId));
            versionId = getIssueVersion(issue);
            projectId = issue.getProjectObject().getId();
        }
        JSONObject ob = getExecutionStatusByCycleId(req, projectId, versionId, null, null, offset, expand);
        return ob;
    }

    @SuppressWarnings("unchecked")
	@Override
    public Response getCyclesByVersionsAndSprint(Map<String, Object> params, List<Long> projectIdList) {
        String versionId = params.get("versionId") != null ? String.valueOf(params.get("versionId")) : null;
        String[] versionIds = versionId != null ? versionId.split(",") : new String[0];
        String inputSprintId = params.get("sprintId") != null ? String.valueOf(params.get("sprintId")) : "";
        String[] sprintIds = inputSprintId.split(",");

        Integer offset = ZCollectionUtils.getAsInteger(params, "offset");
        String expand = ZCollectionUtils.getAsString(params, "expand");
        Integer maxRecords = ZCollectionUtils.getAsInteger(params, "maxRecords");
        if (offset == null) {
            offset = 0;
        }
        maxRecords = maxRecords != null ? maxRecords : -1;
        CycleResourceHelper cycleResourceHelper = new CycleResourceHelper(scheduleManager, authContext, projectManager, cycleManager, scheduleIndexManager, zephyrPermissionManager, jobProgressService,folderManager);
        List<Cycle> cycles = cycleResourceHelper.getCyclesByVersions(projectIdList, versionIds, sprintIds, offset, maxRecords);
        JSONObject jsonIDArray = new JSONObject();
        Collection<Long> zephyrPermissionError = new ArrayList<>();
        try {
            LocalDate today = new DateTime(System.currentTimeMillis()).toLocalDate();
            for (Cycle cycle : cycles) {
                ProjectPermissionKey projectPermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_BROWSE_CYCLE.toString());
                boolean hasZephyrPermission = zephyrPermissionManager.validateUserPermission(projectPermissionKey, null, authContext.getLoggedInUser(), cycle.getProjectId());
                if (hasZephyrPermission) {
                    String sprintId = cycle.getSprintId() != null ? String.valueOf(cycle.getSprintId()) : "-1";
                    if (jsonIDArray.has(sprintId)) {
                        JSONObject ob = new JSONObject();
                        Map<String, Object> cntByCycleIdAndStatus = populateCycleSummaryInJson(ob, cycle.getVersionId(), cycle.getProjectId(), cycle.getID(), cycle.getName(), cycle.getDescription(), cycle.getStartDate(), cycle.getEndDate(),
                                today, cycle.getBuild(), cycle.getEnvironment(), cycle.getCreatedBy(), cycle.getModifiedBy(), cycle.getSprintId(), null, null, expand);
                        cntByCycleIdAndStatus.put("id", cycle.getID());
                        JSONObject jsonObject = (JSONObject) jsonIDArray.get(sprintId);
                        if (jsonObject != null) {
                            List<Map<String, Object>> cyclesBySprintList = (List<Map<String, Object>>) jsonObject.get("cycles");
                            cyclesBySprintList.add(cntByCycleIdAndStatus);
                            jsonObject.put("id", sprintId);
                            jsonObject.put("cycles", cyclesBySprintList);
                        }
                    } else {
                        List<Map<String, Object>> cyclesBySprintList = new ArrayList<Map<String, Object>>();
                        JSONObject ob = new JSONObject();
                        Map<String, Object> cntByCycleIdAndStatus = populateCycleSummaryInJson(ob, cycle.getVersionId(), cycle.getProjectId(), cycle.getID(), cycle.getName(), cycle.getDescription(), cycle.getStartDate(), cycle.getEndDate(),
                                today, cycle.getBuild(), cycle.getEnvironment(), cycle.getCreatedBy(), cycle.getModifiedBy(), cycle.getSprintId(), null, null, expand);
                        cntByCycleIdAndStatus.put("id", cycle.getID());
                        cyclesBySprintList.add(cntByCycleIdAndStatus);
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("id", sprintId);
                        jsonObject.put("cycles", cyclesBySprintList);
                        jsonIDArray.put(sprintId, jsonObject);
                    }
                } else {
                    zephyrPermissionError.add(cycle.getProjectId());
                }
            }
        } catch (Exception e) {
            return Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().
                    getText("zapi.get.cycles.invalid.input.error"))).cacheControl(ZephyrCacheControl.never()).build();
        }
        
        try {
        	//Append Folders test summaries information which are linked to sprint.
            appendFoldersLinkedToSprint(projectIdList, versionIds, sprintIds, offset, maxRecords, jsonIDArray, expand);
        } catch(RuntimeException e) {
        	String errorMessgae = "[Error] [Error code:"+ Status.INTERNAL_SERVER_ERROR.getStatusCode() + " " + Status.INTERNAL_SERVER_ERROR +" Error Message :"+ e.getMessage();
        	log.error(errorMessgae);
        	return Response.status(Status.INTERNAL_SERVER_ERROR).entity(errorMessgae).cacheControl(ZephyrCacheControl.never()).build();
        }

        JSONArray jsonArray = new JSONArray();
        Iterator<String> keys = jsonIDArray.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONObject json;
            try {
                json = (JSONObject) jsonIDArray.get(key);
                jsonArray.put(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (!zephyrPermissionError.isEmpty()) {
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
            log.error("[Error] [Error code:"+ Status.FORBIDDEN.getStatusCode()+" "+Status.FORBIDDEN+" Error Message :"+errorMessage);
            log.error("List of zephyr permission error projects while calling getCyclesByVersionsAndSprint :"+zephyrPermissionError);
            return JiraUtil.getPermissionDeniedErrorResponse(errorMessage);
        }

        return Response.status(Status.OK).entity(jsonArray.toString()).cacheControl(ZephyrCacheControl.never()).build();
    }
    
    @SuppressWarnings("unchecked")
    private void appendFoldersLinkedToSprint(List<Long> projectIdList, String[] versionIds, String[] sprintIds, Integer offset, Integer maxRecords, JSONObject jsonIDArray, String expand) {
    	List<FolderCycleMapping> folderCycleMappingList = folderManager.getFoldersForSprint(projectIdList, versionIds, sprintIds, offset, maxRecords);
        JSONObject folderMap = null;
        List<JSONObject> foldersBySprintList = null;
        List<Map<String, Object>> cyclesBySprintList = null;
        LocalDate today = new DateTime(System.currentTimeMillis()).toLocalDate();
        for(FolderCycleMapping folderCycleMapping : folderCycleMappingList) {
        	try {
        		String sprintID = String.valueOf(folderCycleMapping.getSprintId());
        		folderMap = new JSONObject();
        		folderMap.put("folderId", folderCycleMapping.getFolder().getID());
            	folderMap.put("folderName", folderCycleMapping.getFolder().getName());            	
				populateFolderExecutionSummaries(folderMap, projectIdList, versionIds, Long.valueOf(folderCycleMapping.getCycle().getID()+""), Long.valueOf(folderCycleMapping.getFolder().getID()+""), null);
				if (jsonIDArray.has(sprintID)) {
					JSONObject jsonObject = (JSONObject) jsonIDArray.get(sprintID);
					if (jsonObject != null) {
						List<Map<String, Object>> currCyclesList = (List<Map<String, Object>>) jsonObject.get("cycles");
                        Map<Integer,Map<String, Object>> currCyclesIdList = getCycleIdListFromCurrentList(currCyclesList);
						if(null != currCyclesIdList.get(folderCycleMapping.getCycle().getID())) {
                            Map<String, Object> mapObj = currCyclesIdList.get(folderCycleMapping.getCycle().getID());
                            if(mapObj.get("folders") != null) {
                                foldersBySprintList = (List<JSONObject>) mapObj.get("folders");
                                foldersBySprintList.add(folderMap);
                            } else {
                                foldersBySprintList = new ArrayList<>();
                                foldersBySprintList.add(folderMap);
                                mapObj.put("folders", foldersBySprintList);
                            }
                        }else {
                            foldersBySprintList = new ArrayList<>();
                            foldersBySprintList.add(folderMap);
                            Cycle cycle = folderCycleMapping.getCycle();
                            Map<String, Object> cntByCycleIdAndStatus = populateCycleSummaryInJson(new JSONObject(), cycle.getVersionId(), cycle.getProjectId(), cycle.getID(), cycle.getName(), cycle.getDescription(), cycle.getStartDate(), cycle.getEndDate(),
                                    today, cycle.getBuild(), cycle.getEnvironment(), cycle.getCreatedBy(), cycle.getModifiedBy(), cycle.getSprintId(), null, null, expand);
                            cntByCycleIdAndStatus.put("id", cycle.getID());
                            cntByCycleIdAndStatus.put("folders", foldersBySprintList);
                            currCyclesList.add(cntByCycleIdAndStatus);

                            jsonObject.put("id", sprintID);
                            jsonObject.put("cycles", currCyclesList);
                            jsonIDArray.put(sprintID, jsonObject);
                        }
					}
				} else {
					foldersBySprintList = new ArrayList<>();
					foldersBySprintList.add(folderMap);
					cyclesBySprintList = new ArrayList<>();
					Cycle cycle = folderCycleMapping.getCycle();
					Map<String, Object> cntByCycleIdAndStatus = populateCycleSummaryInJson(new JSONObject(), cycle.getVersionId(), cycle.getProjectId(), cycle.getID(), cycle.getName(), cycle.getDescription(), cycle.getStartDate(), cycle.getEndDate(),
                            today, cycle.getBuild(), cycle.getEnvironment(), cycle.getCreatedBy(), cycle.getModifiedBy(), cycle.getSprintId(), null, null, expand);
                    cntByCycleIdAndStatus.put("id", cycle.getID());
                    cntByCycleIdAndStatus.put("folders", foldersBySprintList);
                    cyclesBySprintList.add(cntByCycleIdAndStatus);
					JSONObject jsonObject = new JSONObject();
                    jsonObject.put("id", sprintID);
                    jsonObject.put("cycles", cyclesBySprintList);
                    jsonIDArray.put(sprintID, jsonObject);
				}
			} catch (NumberFormatException | JSONException e) {
				log.error("Error while processing linked folders to sprint");
				throw new RuntimeException("Error while processing linked folders to sprint", e);
			}        	
        }
    }

    private Map<Integer,Map<String, Object>> getCycleIdListFromCurrentList(List<Map<String, Object>> currCyclesList) {
        if(CollectionUtils.isNotEmpty(currCyclesList)) {
            Map<Integer,Map<String, Object>> cycleIdListMap = new HashMap<>();
            currCyclesList.forEach(
                    cycleMapObject -> {
                        cycleIdListMap.put((Integer) cycleMapObject.get("id"), cycleMapObject);
                    }
            );
            return cycleIdListMap;
        }
        return Collections.EMPTY_MAP;
    }


    public Response cleanupSprintFromCycle() {
        long start = System.currentTimeMillis();
        List<Cycle> cycles = cycleManager.getCyclesByCriteria("SPRINT_ID IS NOT NULL", -1);
        final LoadingCache<Long, Optional<SprintBean>> sprintCache = CacheBuilder.newBuilder()
                .maximumSize(100)
                .build(
                        new CacheLoader<Long, Optional<SprintBean>>() {
                            public Optional<SprintBean> load(Long springId) {
                                return sprintService.getSprint(springId);
                            }
                        });
        Predicate<Cycle> invalidSprintFilter = cycle -> !sprintCache.getUnchecked(cycle.getSprintId()).isPresent();
        cycles.stream().filter(invalidSprintFilter).forEach(cycle -> {
            cycle.setSprintId(null);
            cycle.save();
        });

        long end = System.currentTimeMillis();
        long tookSec = (end - start) / 1000;
        log.info("Sprint cleanup completed in " + tookSec+" ms");
        JSONObject json = new JSONObject(ImmutableMap.of("took", tookSec));
        return Response.ok(json.toString()).build();
    }


    public Response cleanupCacheForCycle() {
        long start = System.currentTimeMillis();
        zfjCacheService.removeAllCycleCacheByWildCard();
        long end = System.currentTimeMillis();
        long tookSec = (end - start) / 1000;
        log.info("Cache cleanup completed in " + tookSec+" ms");
        JSONObject json = new JSONObject(ImmutableMap.of("took", tookSec));
        return Response.ok(json.toString()).build();
    }

    /**
     * Validates Cycle Start and End Date
     *
     * @param startDate
     * @param endDate
     * @param errorMap
     * @param i18n
     */
    private void validateCycleStartAndEndDate(String startDate, String endDate, Map<String, String> errorMap, final I18nHelper i18n) {
        DateTimeFormatter formatter = dateTimeFormatterFactory.formatter().forLoggedInUser().withSystemZone().withStyle(DateTimeStyle.DATE_PICKER);
        Date cycleStartDate = null;
        Date cycleEndDate = null;
        try {
            cycleStartDate = convertToDate(startDate, formatter);
            cycleEndDate = convertToDate(endDate, formatter);
        } catch (Exception ex) {
            log.error("Error in converting to date ", ex);
            errorMap.put("date", i18n.getText("fields.validation.data.format", ComponentAccessor.getApplicationProperties().getDefaultBackedString(APKeys.JIRA_DATE_PICKER_JAVA_FORMAT)));
        }

        if (!isDateSequenceValid(cycleStartDate, cycleEndDate))
            errorMap.put("dateSequenceError", i18n.getText("project.cycle.summary.create.dialog.validationError.datesequenceerror"));
    }

    private boolean isDateSequenceValid(Date cycleStartDate, Date cycleEndDate) {
        if (cycleStartDate == null || cycleEndDate == null)
            return true;
        return (cycleEndDate.after(cycleStartDate) || DateUtils.isSameDay(cycleStartDate, cycleEndDate));
    }
    
    @Override
	public List<Folder> fetchAllFoldersforCycle(Long projectId, Long versionId, Long cycleId, Integer limit, Integer offset) {
    	List<Long> cycleIds = new ArrayList<>(1);
		cycleIds.add(cycleId);
		return folderManager.fetchFolders(projectId, versionId, cycleIds, limit, offset);
	}
    
    @Override
    public void populateFolderExecutionSummaries(JSONObject ob, List<Long> projectIdList, String[] versionIds, Long cycleId, Long folderId, Map<String, String> cycleActionMap) throws JSONException {
    	int totalExecuted = 0;
        int totalExecutions = 0;
        JSONArray expectedOperators = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        List<ExecutionSummaryImpl> allSummaries = scheduleManager.getExecutionDetailsByCycleAndFolder(projectIdList, versionIds, cycleId, folderId, null);
        for (ExecutionSummaryImpl executionSummary : allSummaries) {
            if (executionSummary.getExecutionStatusKey().intValue() != -1 &&
                    !StringUtils.equalsIgnoreCase(executionSummary.getExecutionStatusName(), "Unexecuted")) {
                totalExecuted += executionSummary.getCount();
            }
            totalExecutions += executionSummary.getCount();
            expectedOperators.put(executionSummaryToJSON(executionSummary));
        }
        jsonObject.put("executionSummary", expectedOperators);
        ob.put("executionSummaries", jsonObject);
        ob.put("totalExecutions", totalExecutions);
        ob.put("totalExecuted", totalExecuted);
        String compositekey = String.valueOf(folderId) + ":" + String.valueOf(cycleId) + ":";
        if(cycleActionMap != null) {
        	for(String versionId : versionIds) {
            	compositekey = compositekey + versionId;
            	if (cycleActionMap.containsKey(compositekey)) {
                    String cycleSummaryDetail = cycleActionMap.get(compositekey);
                    String[] keyValues = StringUtils.split(cycleSummaryDetail, ",");
                    for (String keyValue : keyValues) {
                        String[] properties = StringUtils.split(keyValue, "=");
                        if (properties.length == 2) {
                        	ob.put(properties[0], properties[1]);
                        } else {
                        	ob.put(properties[0], "");
                        }
                    }
                    break;
                }
            }
        }
    }

	@Override
	public Response moveExecutionsFromCycleToFolder(Long projectId, Long versionId, Long cycleId, Long folderId, String userName, String cycleName, List<Integer> schedulesList) {
		Folder folder = null;
		JSONObject jsonObjectResponse = new JSONObject();
		String jobProgressToken = new UniqueIdGenerator().getStringId();
        try {
        	if(!folderId.equals(ApplicationConstants.ADHOC_SYSTEM_FOLDER_ID)) {
    			folder = folderManager.getFolder(folderId);
    			if(folder == null) {
    				jsonObjectResponse.put("error", authContext.getI18nHelper().getText("project.folder.not.exist"));
    				log.error("[Error] [Error code:"+ Response.Status.BAD_REQUEST.getStatusCode() + " " + Response.Status.BAD_REQUEST +" Error Message :"+
    						authContext.getI18nHelper().getText("project.folder.not.exist"));
    				return Response.status(Status.BAD_REQUEST).entity(jsonObjectResponse.toString()).build();
    			}
    		}
            jsonObjectResponse.put(ApplicationConstants.JOB_PROGRESS_TOKEN, jobProgressToken);
        } catch (JSONException e) {
            log.error("error getting job progress token", e);
        }
        jobProgressService.createJobProgress(ApplicationConstants.MOVE_EXECUTIONS_FROM_CYCLE_TO_FOLDER_JOB_PROGRESS, 0, jobProgressToken);
        jobProgressService.setEntityWithId(jobProgressToken, ApplicationConstants.CYCLE_ENTITY, String.valueOf(cycleId));
        final List<Schedule> schedules;
        if(schedulesList != null && schedulesList.size() > 0) {
        	schedules = Arrays.asList(scheduleManager.getSchedules(schedulesList));
        	jobProgressService.addSteps(jobProgressToken, schedules.size());
        } else {
        	schedules = scheduleManager.getSchedules(versionId, projectId, cycleId.intValue(), -1, null, null, null,null);
            jobProgressService.addSteps(jobProgressToken, schedules.size());
        } 
        final String lockName = "zephyr-move-executions-cycle-folder_" + cycleId;
        final ClusterLock lock = clusterLockService.getLockForName(lockName);
        final Folder finalFolder = folder;
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
            	boolean flag = true;
            	List<Schedule> finalSchedulesList = new ArrayList<>(schedules.size());
            	StringBuffer sb = new StringBuffer();
                if (lock.tryLock(0, TimeUnit.SECONDS)) {
                	for (Schedule schedule : schedules) {
                		if(!checkIssueExistsForFolder(projectId.intValue(), schedule.getIssueId(), cycleId.intValue(), versionId.intValue(), folderId)) {
                			schedule.setFolder(finalFolder);
                        	schedule.setModifiedBy(userName);
                            schedule.setModifiedDate(new Date());
                        	schedule.save();
                        	finalSchedulesList.add(schedule);
                        	flag = false;
                		} else {
                			MutableIssue issue = ComponentAccessor.getIssueManager().getIssueObject(new Long(schedule.getIssueId()));
                			String formattedMessage = "<a href='" + jiraBaseUrls.baseUrl() + "/browse/" + issue.getKey() + "'>" + issue.getKey() + "</a>,";
                			sb.append(formattedMessage);
                		}
                        jobProgressService.addCompletedSteps(jobProgressToken,1);   
                    }
                	jobProgressService.addCompletedSteps(jobProgressToken, ApplicationConstants.JOB_STATUS_COMPLETED);
 	                if(!flag && sb.length() > 0) {
 	                	jobProgressService.setMessage(jobProgressToken, authContext.getI18nHelper().getText("zephyr.move.executions.cycle.folder.error.message", sb.toString().trim(), finalFolder.getName()));
                 	} else if(flag && sb.length() > 0) {
                 		jobProgressService.setMessage(jobProgressToken, authContext.getI18nHelper().getText("zephyr.move.executions.cycle.folder.error1.message", sb.toString().trim(), finalFolder.getName()));
                 	} else {
                 		if(schedules.size() > 0) {
                 			jobProgressService.setMessage(jobProgressToken, authContext.getI18nHelper().getText("zephyr.move.executions.cycle.folder.message", cycleName, finalFolder.getName()));
                 		} else {
                 			jobProgressService.setMessage(jobProgressToken, authContext.getI18nHelper().getText("zephyr.move.executions.cycle.folder.invalid.message", cycleName, finalFolder.getName()));
                 		}
                 	}
	                try {
	                    //Need re-Index hence publishing event to do it.
	                    Map<String, Object> params = new HashMap<String, Object>();
                        params.put("ENTITY_TYPE", "SCHEDULE_ID");
                        eventPublisher.publish(new SingleScheduleEvent(finalSchedulesList, params, EventType.EXECUTION_UPDATED));
	                } catch (Exception e) {
	                    log.error("Error Indexing Schedules for Move executions from cycle to folder:", e);
	                }
                } else {
	                String inProgressMsg = authContext.getI18nHelper().getText("zephyr.je.admin.move.exeuctions.cycle.folder.already.in.progress");
	                jobProgressService.setMessage(jobProgressToken, inProgressMsg);
                }
	        } catch (InterruptedException e) {
	            String error = "Move executions from cycle to folder operation interrupted";
	            log.error("moveExecutionsFromCycleToFolder(): " + error, e);
	            jobProgressService.addCompletedSteps(jobProgressToken, ApplicationConstants.JOB_STATUS_FAILED);
	        } finally {
	            lock.unlock();
	            authContext.setLoggedInUser(null);
	        }
        });
		return Response.ok().entity(jsonObjectResponse.toString()).build();
	}

    @Override
    public Long getSprintIDForFolder(Long folderId, Long cycleId) {
        return folderManager.getSprintIDForFolder(folderId, cycleId);
    }
    
    private boolean checkIssueExistsForFolder(Integer projectId, Integer issueId, Integer cycleId, Integer versionId, Long folderId) {
        Map<String, Object> filter = new HashMap<String, Object>();
        filter.put("pid", projectId);
        filter.put("issueId", issueId);
        if(cycleId.intValue() != ApplicationConstants.AD_HOC_CYCLE_ID) {
        	filter.put("cid", new Integer[]{cycleId});
        } else {
        	filter.put("cid", null);
        }
        filter.put("vid", versionId);
        if(folderId != null && !folderId.equals(ApplicationConstants.ADHOC_SYSTEM_FOLDER_ID)) {
        	filter.put("folderId", folderId);
        } else {
        	filter.put("folderId", null);
        }
        List<Schedule> schedules = scheduleManager.searchSchedules(filter);
        if (schedules.size() > 0)
            return true;
        return false;
    }

    /**
     * Create schedule properties map.
     * @param schedule
     * @param newCycleId
     * @param today
     * @param versionId
     * @param folderId
     * @param userName
     * @return
     */
    private Map<String, Object> createScheduleProperties(Schedule schedule, int newCycleId, Date today, Long versionId, Long folderId, String userName) {
        Map<String, Object> scheduleMap = new HashMap<>();
        scheduleMap.put("CYCLE_ID", newCycleId);
        scheduleMap.put("DATE_CREATED", today);
        scheduleMap.put("VERSION_ID", versionId);
        scheduleMap.put("PROJECT_ID", schedule.getProjectId());
        /*As per requirement, we need to reset comments*/
        scheduleMap.put("COMMENT", StringUtils.EMPTY);
        scheduleMap.put("ISSUE_ID", schedule.getIssueId());
        scheduleMap.put("STATUS", String.valueOf(ApplicationConstants.UNEXECUTED_STATUS));
        scheduleMap.put("CREATED_BY", userName);
        scheduleMap.put("MODIFIED_BY", userName);
        scheduleMap.put("ORDER_ID", scheduleManager.getMaxOrderId() + 1);
        scheduleMap.put("EXECUTION_WORKFLOW_STATUS", ExecutionWorkflowStatus.CREATED);

        if(Objects.nonNull(folderId)) {
            scheduleMap.put("FOLDER_ID", folderId);
        }
        return scheduleMap;
    }


}
