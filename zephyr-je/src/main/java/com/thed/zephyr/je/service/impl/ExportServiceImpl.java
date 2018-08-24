package com.thed.zephyr.je.service.impl;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.gzipfilter.org.apache.commons.lang.StringUtils;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.usercompatibility.UserCompatibilityHelper;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.JiraVelocityUtils;
import com.atlassian.velocity.VelocityManager;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.googlecode.jatl.Html;
import com.thed.zephyr.je.attachment.AttachmentUtils;
import com.thed.zephyr.je.config.model.ExecutionStatus;
import com.thed.zephyr.je.helper.TraceabilityResourceHelper;
import com.thed.zephyr.je.model.*;
import com.thed.zephyr.je.permissions.model.PermissionType;
import com.thed.zephyr.je.permissions.service.ZephyrPermissionManager;
import com.thed.zephyr.je.rest.CustomFieldValueResource;
import com.thed.zephyr.je.rest.delegate.CustomFieldValueResourceDelegate;
import com.thed.zephyr.je.service.*;
import com.thed.zephyr.je.vo.ExecutionDefectBean;
import com.thed.zephyr.je.vo.TeststepBean;
import com.thed.zephyr.je.vo.ZQLScheduleBean;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.CSVWriter;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.ZephyrComponentAccessor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.apache.velocity.tools.generic.EscapeTool;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


public class ExportServiceImpl implements ExportService {
    protected final Logger log = Logger.getLogger(ExportServiceImpl.class);
	private final JiraAuthenticationContext authContext;
    private final ScheduleManager scheduleManager;
    private final DateTimeFormatterFactory dateTimeFormatterFactory;
	private final IssueManager issueManager;
    private final StepResultManager stepResultManager;
    private final SearchProvider searchProvider;
    private final ZephyrPermissionManager zephyrPermissionManager;
    private final FolderManager folderManager;
    private final ZephyrCustomFieldManager zephyrCustomFieldManager;
    private final CustomFieldValueResourceDelegate customFieldValueResourceDelegate;
	
    private static final String EMPTY_VALUE = "";
    private static final String COLON = ":";
    private static final String PIPE = "|";

    private static final DateFormat DATE_TIME_FORMAT = new SimpleDateFormat("MM/dd/yyyy hh:mm a");
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy");

    public ExportServiceImpl(ScheduleManager scheduleManager, DateTimeFormatterFactory dateTimeFormatterFactory,
    		JiraAuthenticationContext authContext,IssueManager issueManager,StepResultManager stepResultManager,SearchProvider searchProvider,ZephyrPermissionManager zephyrPermissionManager, FolderManager folderManager,
            ZephyrCustomFieldManager zephyrCustomFieldManager, CustomFieldValueResourceDelegate customFieldValueResourceDelegate) {
    	this.authContext=authContext;
    	this.scheduleManager=scheduleManager;
    	this.dateTimeFormatterFactory=dateTimeFormatterFactory;
    	this.issueManager=issueManager;
    	this.stepResultManager=stepResultManager;
    	this.searchProvider=searchProvider;
    	this.zephyrPermissionManager=zephyrPermissionManager;
    	this.folderManager = folderManager;
    	this.zephyrCustomFieldManager = zephyrCustomFieldManager;
    	this.customFieldValueResourceDelegate = customFieldValueResourceDelegate;
    }
    
    @Override
    public StreamingOutput generate(List<ZQLScheduleBean> schedules,String exportType) {
        Map<String, Object> bodyParams = JiraVelocityUtils.getDefaultVelocityParams(authContext);
        bodyParams.put("schedules", schedules);
        bodyParams.put("i18n", authContext.getI18nHelper());

        CustomField[] customFields = zephyrCustomFieldManager.getAllCustomFieldsByEntityType(ApplicationConstants.ENTITY_TYPE.EXECUTION.name());
        Map<String,String> customFieldNameMapping = new HashMap<>();
        if(Objects.nonNull(customFields)){
            Arrays.stream(customFields).forEach(customField -> {
                customFieldNameMapping.putIfAbsent(customField.getID()+StringUtils.EMPTY,customField.getName().replaceAll("\\s+", StringUtils.EMPTY));
            });
        }

        bodyParams.put("customFieldNameMapping", customFieldNameMapping);

        VelocityManager velocityManager = ComponentAccessor.getVelocityManager();
		final String response = velocityManager.getEncodedBody("templates/zephyr/schedule/export/","searchrequest-" + exportType +".vm","UTF-8", bodyParams);
		final StreamingOutput out = new StreamingOutput() {
			@Override
			public void write(OutputStream output) throws IOException,
					WebApplicationException {
				PrintWriter out = new PrintWriter(new OutputStreamWriter(output, "UTF-8"), false);
				out.write(response);
				out.flush();
			}
		};
	    return out;
    }
    
	@Override
	public StreamingOutput generateCSV(final List<ZQLScheduleBean> schedules) {
        CustomField[] customFields = zephyrCustomFieldManager.getAllCustomFieldsByEntityType(ApplicationConstants.ENTITY_TYPE.EXECUTION.name());
        Map<String,String> customFieldNameMapping = new HashMap<>();
        if(Objects.nonNull(customFields)){
            Arrays.stream(customFields).forEach(customField -> {
                customFieldNameMapping.putIfAbsent(customField.getID()+StringUtils.EMPTY,customField.getName());
            });
        }

		StreamingOutput sOutput = new StreamingOutput() {
			public void write(OutputStream output) throws IOException,
					WebApplicationException {
				CSVWriter writer;
				PrintWriter pw = null;
				try {
					pw = new PrintWriter(output);
					writer = new CSVWriter(pw);
					writer.put("ExecutionId", "CycleName", "Issue Key", "Test Summary", "Labels", "Project",
							"Component", "Version", "Priority", "Assigned To", "Executed By",
							"Executed On", "ExecutionStatus", "Comment", "ExecutionDefects", "CreationDate","Folder Name","Custom Fields",
                            "StepId", "OrderId", "Step", "Test Data", "Expected Result",
							"Step Result", "Comments", "Test Step Custom Fields");

					writer.nl();
					for (ZQLScheduleBean zqlScheduleBean : schedules) {
						List<TeststepBean> testStepBeans = zqlScheduleBean.getTestStepBean();
						List<Map<String,Object>> componentList = zqlScheduleBean.getComponents();
						List<String> componentNames = new ArrayList<String>();
						Iterator<Map<String,Object>> componentIterator = componentList.iterator();
						while(componentIterator.hasNext()) {
							Map<String,Object> component = componentIterator.next();
							componentNames.add((String)component.get("name"));
						}

						if(null != testStepBeans && !testStepBeans.isEmpty()){
							Iterator<TeststepBean> testStepBeansItr = testStepBeans.listIterator();
							while (testStepBeansItr.hasNext()) {
								TeststepBean testStepBean = testStepBeansItr.next();
								String status = "";
								if(zqlScheduleBean.getStatus() != null && zqlScheduleBean.getStatus().getName() != null){
									status = zqlScheduleBean.getStatus().getName();
								}
								writer.put(zqlScheduleBean.getId())
										.put(zqlScheduleBean.getCycleName())
										.put(zqlScheduleBean.getIssueKey())
                                        .put(zqlScheduleBean.getIssueSummary())
										.put(StringUtils.join(zqlScheduleBean.getLabels(), ","))
										.put(zqlScheduleBean.getProject())
										.put(StringUtils.join(componentNames, ","))
										.put(zqlScheduleBean.getVersionName())
										.put(zqlScheduleBean.getPriority())
										.put(zqlScheduleBean.getAssignee())
										.put(zqlScheduleBean.getExecutedBy())
										.put(zqlScheduleBean.getExecutedOn())
										.put(status)
										.put(zqlScheduleBean.getComment())
										.put(getDefectsAsString(zqlScheduleBean.getExecutionDefects(), zqlScheduleBean.getStepDefects()))
										.put(zqlScheduleBean.getCreationDate())
                                        .put(StringUtils.isBlank(zqlScheduleBean.getFolderName()) ? StringUtils.EMPTY : zqlScheduleBean.getFolderName());
										StringJoiner sb = new StringJoiner("\n");
										if(MapUtils.isNotEmpty(zqlScheduleBean.getCustomFieldsValueMap())) {
			                                Map<String,String> customFieldValues = zqlScheduleBean.getCustomFieldsValueMap();
			                                customFieldValues.entrySet().forEach(customFieldValueEntry -> {
			                                    String customFieldName = customFieldNameMapping.get(customFieldValueEntry.getKey());
			                                    sb.add(customFieldName+COLON+customFieldValueEntry.getValue());
			                                });
										}
										writer.put(sb.toString())
										.put(testStepBean.getId())
										.put(testStepBean.getOrderId())
										.put(StringUtils.isBlank(testStepBean.getStep()) ? StringUtils.EMPTY : testStepBean.getStep())
										.put(StringUtils.isBlank(testStepBean.getData()) ? "" : testStepBean.getData())
										.put(StringUtils.isBlank(testStepBean.getResult()) ? "" : testStepBean.getResult())
										.put(StringUtils.isBlank(testStepBean.getStepExecutionStatus()) ? "" : testStepBean.getStepExecutionStatus())
										.put(StringUtils.isBlank(testStepBean.getStepComment()) ? "" : testStepBean.getStepComment());

                                        StringJoiner joiner = new StringJoiner(PIPE);
                                        if(MapUtils.isNotEmpty(testStepBean.getCustomFieldValuesMap())) {
                                            Map<String,String> customFieldValues = testStepBean.getCustomFieldValuesMap();
                                            customFieldValues.entrySet().forEach(customFieldValueEntry -> {
                                            joiner.add(customFieldValueEntry.getKey()+COLON+customFieldValueEntry.getValue());
                                            });
                                        }
                                        writer.put(joiner.toString());
										 
                                writer.nl();
                                pw.flush();
							}
						}else{
							writer.put(zqlScheduleBean.getId())
							.put(zqlScheduleBean.getCycleName())
							.put(zqlScheduleBean.getIssueKey())
                            .put(zqlScheduleBean.getIssueSummary())
							.put(StringUtils.join(zqlScheduleBean.getLabels(), ","))
							.put(zqlScheduleBean.getProject())
							.put(StringUtils.join(componentNames, ","))
							.put(zqlScheduleBean.getVersionName())
							.put(zqlScheduleBean.getPriority())
							.put(zqlScheduleBean.getAssignee())
							.put(zqlScheduleBean.getExecutedBy())
							.put(zqlScheduleBean.getExecutedOn())
							.put(zqlScheduleBean.getStatus() != null ? zqlScheduleBean.getStatus().getName() : EMPTY_VALUE)
							.put(zqlScheduleBean.getComment())
							.put(getDefectsAsString(zqlScheduleBean.getExecutionDefects(), zqlScheduleBean.getStepDefects()))
							.put(zqlScheduleBean.getCreationDate())
                                    .put(StringUtils.isBlank(zqlScheduleBean.getFolderName()) ? StringUtils.EMPTY : zqlScheduleBean.getFolderName());
							StringJoiner sb = new StringJoiner(PIPE);
                            if(MapUtils.isNotEmpty(zqlScheduleBean.getCustomFieldsValueMap())) {
                                Map<String,String> customFieldValues = zqlScheduleBean.getCustomFieldsValueMap();
                                customFieldValues.entrySet().forEach(customFieldValueEntry -> {
                                    String customFieldName = customFieldNameMapping.get(customFieldValueEntry.getKey());
                                    sb.add(customFieldName+COLON+customFieldValueEntry.getValue());
                                });
                            }
                            writer.put(sb.toString());
                            writer.nl();
							pw.flush();
						}
					}					
				} catch (Exception e) {
					log.error("Error exporting to csv file", e);
				}
				pw.flush();
			}
		};
		return sOutput;
	}  
	
	/**
	 * Export Cycles
	 */
	public StreamingOutput exportCycleOrFolder(final Integer cycleId,
			final I18nHelper i18nHelper, final Long projectId,
			final Long versionId, final Long startDate, final Long endDate, final String cycleName,
			final String build, final String env, final Long folderId, final String sortQuery) {
		StreamingOutput so = new StreamingOutput() {
			public void write(OutputStream output) throws IOException, WebApplicationException {
				//File file = new File("Cycle " + cycleId + ".csv");
				CSVWriter writer;
				PrintWriter pw = null;
				try {
					pw = new PrintWriter(output);
					writer = new CSVWriter(pw);
					
					int offset = 0;
					Project project = ComponentAccessor.getProjectManager().getProjectObj(projectId);
					String versionName = i18nHelper.getText("zephyr.je.version.unscheduled"); 
					if(ApplicationConstants.UNSCHEDULED_VERSION_ID != versionId.intValue()){
						Version version = ComponentAccessor.getVersionManager().getVersion(versionId);
						versionName = version.getName();
					}
					List<Schedule> schedules = null;
					if(folderId != null && !folderId.equals(-1L) && !folderId.equals(ApplicationConstants.ADHOC_SYSTEM_FOLDER_ID)) {
						schedules = scheduleManager.getSchedulesByCycleAndFolder(projectId, versionId, new Long(cycleId), offset, sortQuery, null, folderId);
					} else if(folderId != null && folderId.equals(ApplicationConstants.ADHOC_SYSTEM_FOLDER_ID)) {
						schedules = scheduleManager.getSchedulesByCycleAndFolder(projectId, versionId, new Long(cycleId), offset, sortQuery, null, null);
					} else {
						schedules = scheduleManager.getSchedulesByCycleId(versionId, projectId, cycleId, offset, sortQuery, null);
					}
					DateTimeFormatter formatter = dateTimeFormatterFactory.formatter().forLoggedInUser().withStyle(DateTimeStyle.DATE_PICKER);
					Joiner joiner = Joiner.on(", ");
					writer.put(i18nHelper.getText("common.concepts.project") + ": " + project.getName()).nl();
					writer.put(i18nHelper.getText("common.concepts.version") + ": " + versionName).nl();
					writer.put(i18nHelper.getText("je.gadget.common.cycle.label") + ": " + StringEscapeUtils.unescapeHtml(cycleName));
					if(startDate != null){
						Long today = System.currentTimeMillis();
						if(today > startDate){
							writer.put(i18nHelper.getText("cycle.startedon.label")+ ": " + formatter.format(new Date(startDate)));
						}else{
							writer.put(i18nHelper.getText("cycle.starton.label")+ ": " + formatter.format(new Date(startDate)));
						}
						
					}else{
						writer.put(i18nHelper.getText("cycle.no.start.date.label"));
					}
					if(endDate != null){
						Long today = System.currentTimeMillis();
						if(today > endDate){
							writer.put(i18nHelper.getText("cycle.endedon.label")+ ": " + formatter.format(new Date(endDate)));
						}else{
							writer.put(i18nHelper.getText("cycle.endon.label")+ ": " + formatter.format(new Date(endDate)));
						}

					}else{
						writer.put(i18nHelper.getText("cycle.no.end.date.label"));
					}
					writer.put(i18nHelper.getText("cycle.build.label") + ": " + (build != null ? build : EMPTY_VALUE));
					writer.put(i18nHelper.getText("cycle.environment.label") + ": " + (env != null ? env : EMPTY_VALUE));
					writer.nl();
					//ID, Status, Summary, Defect(s), Component, Label, Executed By, Executed On
					writer.put("ID", "Status", "Summary", "Defects", "Component", "Folder", "Label", "Executed By", "Executed On", "Comment", "Custom Fields").nl();
					String status, summary, defectString, components, labels, executedBy, executedOn, folderName;
					while(schedules != null && schedules.size() > 0){
						for(Schedule schedule : schedules) {
							status = summary = defectString = components = labels = executedBy = executedOn = folderName = EMPTY_VALUE;
							/*Calculation*/
							Issue issueUnderTest = ComponentAccessor.getIssueManager().getIssueObject(schedule.getIssueId().longValue());
							if(issueUnderTest == null){
								writer.put("Error in exporting Tests, underlying issue with id not found, issueId: " + schedule.getIssueId()).nl();
								continue;
							}
							boolean hasViewIssuePermission =  JiraUtil.hasIssueViewPermission(null,issueUnderTest,authContext.getLoggedInUser());

							if(hasViewIssuePermission) {
								summary = issueUnderTest.getSummary();
								components = joiner.join(Iterables.transform(issueUnderTest.getComponentObjects(), new Function<ProjectComponent, String>() {
									@Override
									public String apply(ProjectComponent input) {
										return input.getName();
									}
								}));
								labels = joiner.join(Iterables.transform(issueUnderTest.getLabels(), new Function<Label, String>() {
									@Override
									public String apply(Label input) {
										return input.getLabel();
									}
								}));
								if (schedule.getStatus() != null) {
									ExecutionStatus statusObj = JiraUtil.getExecutionStatuses().get(new Integer(schedule.getStatus()));
									if (statusObj != null) status = statusObj.getName();

									if (schedule.getExecutedBy() != null) {
										User user = UserCompatibilityHelper.getUserForKey(schedule.getExecutedBy());
										executedBy = user != null ? user.getDisplayName() : schedule.getExecutedBy();
									}
									if (schedule.getExecutedOn() != null)
										executedOn = formatter.format(new Date(schedule.getExecutedOn()));
								}
								List<ScheduleDefect> associatedDefects = scheduleManager.getAssociatedDefects(schedule.getID());
								if (associatedDefects != null && associatedDefects.size() > 0) {

									List<Long> defectIds = Lists.transform(associatedDefects, new Function<ScheduleDefect, Long>() {
										@Override
										public Long apply(ScheduleDefect input) {
											return new Long(input.getDefectId());
										}
									});
									List<Issue> issues = ComponentAccessor.getIssueManager().getIssueObjects(defectIds);
									List<String> defectsKeys = new ArrayList<>();
									for(Issue issue : issues) {
										boolean hasIssuePermission =  JiraUtil.hasIssueViewPermission(null,issue,authContext.getLoggedInUser());
										if(hasIssuePermission) {
											defectsKeys.add(issue.getKey());
										} else {
											defectsKeys.add(ApplicationConstants.MASKED_DATA);
										}
									}
									defectString = joiner.join(Iterables.transform(defectsKeys, new Function<String, String>() {
										@Override
										public String apply(String input) {
											return input;
										}
									}));
								}

								//Get Step Defects
								StepResultManager stepResultManager = (StepResultManager) ZephyrComponentAccessor.getInstance().getComponent("stepresult-manager");
								List<StepDefect> associatedStepDefects = stepResultManager.getStepResultsWithDefectBySchedule(schedule.getID());
								List<String> stepDefectKey = new ArrayList<String>();
								IssueManager issueManager = ComponentAccessor.getIssueManager();
								for (StepDefect sd : associatedStepDefects) {
									Issue issue = issueManager.getIssueObject(new Long(sd.getDefectId()));
									if (issue == null) {
										log.fatal("Issue not found, " + sd.getDefectId());
										continue;
									}
									boolean hasIssuePermission =  JiraUtil.hasIssueViewPermission(null,issue,authContext.getLoggedInUser());
									if(hasIssuePermission) {
										stepDefectKey.add(issue.getKey());
									} else {
										stepDefectKey.add(ApplicationConstants.MASKED_DATA);
									}
								}
								if (stepDefectKey != null && stepDefectKey.size() > 0) {
									String stepDefectStr = StringUtils.join(stepDefectKey, ",");
									defectString = defectString + " | " + stepDefectStr;
								}
								
								if (schedule.getFolder() != null) {
									folderName = folderManager.getFolder(Long.valueOf(schedule.getFolder().getID()+"")).getName();							
								}  

								/*Writing*/
								writer.put(issueUnderTest.getKey()).put(status).put(summary).put(defectString).put(components).put(StringEscapeUtils.unescapeHtml(folderName)).put(labels).put(executedBy).put(executedOn);
								if (schedule.getComment() != null) {
									String[] comments = StringUtils.split(schedule.getComment(), "\n");
									StringBuilder commentBuilder = new StringBuilder();
									for (String comment : comments) {
										commentBuilder.append(comment + " \r");
									}
									writer.put(commentBuilder.toString());
								} else {
								    writer.put(EMPTY_VALUE);
                                }

								//Execution level custom Fields
                                String customFieldValueDataString = getCustomFieldsValueDataAsString(schedule.getID(),schedule.getProjectId());
                                writer.put(customFieldValueDataString);

								writer.nl();
							} else {
								summary = ApplicationConstants.MASKED_DATA;
								components = joiner.join(Iterables.transform(issueUnderTest.getComponentObjects(), new Function<ProjectComponent, String>() {
									@Override
									public String apply(ProjectComponent input) {
										return ApplicationConstants.MASKED_DATA;
									}
								}));
								labels = joiner.join(Iterables.transform(issueUnderTest.getLabels(), new Function<Label, String>() {
									@Override
									public String apply(Label input) {
										return ApplicationConstants.MASKED_DATA;
									}
								}));
								if (schedule.getStatus() != null) {
									status = ApplicationConstants.MASKED_DATA;

									if (schedule.getExecutedBy() != null) {
										executedBy = ApplicationConstants.MASKED_DATA;
									}
									if (schedule.getExecutedOn() != null)
										executedOn = ApplicationConstants.MASKED_DATA;
								}
								List<ScheduleDefect> associatedDefects = scheduleManager.getAssociatedDefects(schedule.getID());
								if (associatedDefects != null && associatedDefects.size() > 0) {
									List<Long> defectIds = Lists.transform(associatedDefects, new Function<ScheduleDefect, Long>() {
										@Override
										public Long apply(ScheduleDefect input) {
											return new Long(input.getDefectId());
										}
									});
									List<Issue> issues = ComponentAccessor.getIssueManager().getIssueObjects(defectIds);
									defectString = joiner.join(Iterables.transform(issues, new Function<Issue, String>() {
										@Override
										public String apply(Issue input) {
											return ApplicationConstants.MASKED_DATA;
										}
									}));
								}

								//Get Step Defects
								StepResultManager stepResultManager = (StepResultManager) ZephyrComponentAccessor.getInstance().getComponent("stepresult-manager");
								List<StepDefect> associatedStepDefects = stepResultManager.getStepResultsWithDefectBySchedule(schedule.getID());
								List<String> stepDefectKey = new ArrayList<String>();
								IssueManager issueManager = ComponentAccessor.getIssueManager();
								for (StepDefect sd : associatedStepDefects) {
									Issue issue = issueManager.getIssueObject(new Long(sd.getDefectId()));
									if (issue == null) {
										log.fatal("Issue not found, " + sd.getDefectId());
										continue;
									}
									stepDefectKey.add(ApplicationConstants.MASKED_DATA);
								}
								if (stepDefectKey != null && stepDefectKey.size() > 0) {
									String stepDefectStr = StringUtils.join(stepDefectKey, ",");
									defectString = defectString + " | " + stepDefectStr;
								}
								
								if(schedule.getFolder() == null && cycleId.intValue() == ApplicationConstants.AD_HOC_CYCLE_ID) {
									folderName = ApplicationConstants.MASKED_DATA;
								} else if (schedule.getFolder() != null) {
									folderName = ApplicationConstants.MASKED_DATA;						
								}

								/*Writing*/
								writer.put(ApplicationConstants.MASKED_DATA).put(status).put(summary).put(defectString).put(components).put(folderName).put(labels).put(executedBy).put(executedOn);
								if (schedule.getComment() != null) {
									String[] comments = StringUtils.split(schedule.getComment(), "\n");
									StringBuilder commentBuilder = new StringBuilder();
									for (String comment : comments) {
										commentBuilder.append(ApplicationConstants.MASKED_DATA + " \r");
									}
									writer.put(commentBuilder.toString());
								}
								String customFieldsValueDataString = ApplicationConstants.MASKED_DATA;
                                writer.put(customFieldsValueDataString);
								writer.nl();
							}
						}
						offset += schedules.size();
						pw.flush();
						if(folderId != null && !folderId.equals(-1L) && !folderId.equals(ApplicationConstants.ADHOC_SYSTEM_FOLDER_ID)) {
							schedules = scheduleManager.getSchedulesByCycleAndFolder(projectId, versionId, new Long(cycleId), offset, sortQuery, null, folderId);
						} else if(folderId != null && folderId.equals(ApplicationConstants.ADHOC_SYSTEM_FOLDER_ID)) {
							schedules = scheduleManager.getSchedulesByCycleAndFolder(projectId, versionId, new Long(cycleId), offset, sortQuery, null, null);
						} else {
							schedules = scheduleManager.getSchedulesByCycleId(versionId, projectId, cycleId, offset, sortQuery, null);
						}
					}
				} catch (Exception e) {
					e.printStackTrace(pw);
				}
				pw.flush();
			}
		};
		return so;
	}

    @SuppressWarnings("unchecked")
	private String getDefectsAsString(List<ExecutionDefectBean> execDefects, List<String> stepDefects){
        String defectsAsString = "";
        if(null != execDefects && execDefects.size() > 0) {
        	final Collection<String> execDefectKeys = CollectionUtils.collect(execDefects, new Transformer() {
                @Override
    			public String transform(final Object input) {
                    if (StringUtils.isBlank(String.valueOf(input))) {
                        return null;
                    }
                    final String defectKey = ((ExecutionDefectBean)input).getDefectKey();
                    return defectKey;
                }
            });
        	defectsAsString += StringUtils.join(execDefectKeys, ",");
        }
        if(null != stepDefects && stepDefects.size() > 0)
            defectsAsString += " | " + StringUtils.join(stepDefects, ",");

        return defectsAsString;
    }

	@Override
	public File createDefectRequirementReport(String exportType,
			Collection<Long> defectIds, Long versionId) throws Exception {
		final Long fileNameTs = new Date().getTime();
		final File tmpDir = AttachmentUtils.getTemporaryAttachmentDirectory();
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("i18n", authContext.getI18nHelper());
        String extension = "html";
        if(StringUtils.equalsIgnoreCase(exportType, "html")) {
        	params.put("excelContentType", false);
        } else if(StringUtils.equalsIgnoreCase(exportType, "excel")) {
        	params.put("excelContentType", true);
        	extension = "xls";
        }
        String fileName = "ZFJ-DefectReqReport-" + fileNameTs + "." +extension;
		File tempAttachmentFile = new File(tmpDir,fileName);

        FileOutputStream fos = new FileOutputStream(tempAttachmentFile);
        VelocityManager velocityManager = ComponentAccessor.getVelocityManager();
        String content = velocityManager.getEncodedBody("templates/zephyr/schedule/export/","defectrequirement-traceability.vm","UTF-8",params);
        createHTMLReportHeader(fos, content);
        createHTMLDefectRequirementRows(fos, defectIds, versionId);
        createHTMLReportFooter(fos, content);
		fos.close();
		return tempAttachmentFile;
	}

	@Override
	public File createRequirementDefectReport(String exportType,
			Collection<Long> requirementIds, Long versionId) throws Exception {
		final Long fileNameTs = new Date().getTime();
		final File tmpDir = AttachmentUtils.getTemporaryAttachmentDirectory();
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("i18n", authContext.getI18nHelper());
        String extension = "html";
        if(StringUtils.equalsIgnoreCase(exportType, "html")) {
        	params.put("excelContentType", false);
        } else if(StringUtils.equalsIgnoreCase(exportType, "excel")) {
        	params.put("excelContentType", true);
        	extension = "xls";
        }
        String fileName = "ZFJ-ReqDefectReport-" + fileNameTs + "." +extension;
		File tempAttachmentFile = new File(tmpDir,fileName);

        FileOutputStream fos = new FileOutputStream(tempAttachmentFile);
        VelocityManager velocityManager = ComponentAccessor.getVelocityManager();
        String content = velocityManager.getEncodedBody("templates/zephyr/schedule/export/","requirementdefect-traceability.vm","UTF-8",params);
        createHTMLReportHeader(fos, content);
        createHTMLRequirementDefectRows(fos, requirementIds, versionId);
        createHTMLReportFooter(fos, content);
		fos.close();
		return tempAttachmentFile;
	}

    
    

    private void createHTMLReportHeader (FileOutputStream fos, String content) throws Exception {
        content = StringUtils.substringBefore(content, "<!-- report body -->");
        try {
            PrintWriter out = new PrintWriter(new OutputStreamWriter(fos, "UTF-8"), false);
            out.write(content);
            out.flush();
        } catch (UnsupportedEncodingException e) {
            log.error("Error create html file.", e);
            throw new Exception(e);
        }
    }

    private void createHTMLReportFooter (FileOutputStream fos, String content) throws Exception {
        content = StringUtils.substringAfter(content, "<!-- report body -->");
        try {
            PrintWriter out = new PrintWriter(new OutputStreamWriter(fos, "UTF-8"), false);
            out.write(content);
            out.flush();
        } catch (UnsupportedEncodingException e) {
            log.error("Error create html file.", e);
            throw new Exception(e);
        }
    }

	
    private void getUniqueDefects (Set<Object> uniqueDefectList, List<Schedule> schedules) {
        for (Schedule schedule : schedules) {
	    	List<ScheduleDefect> associatedDefects = scheduleManager.getAssociatedDefects(schedule.getID());
            if (associatedDefects != null) {
                for (ScheduleDefect scheduleDefect: associatedDefects) {
                    uniqueDefectList.add(scheduleDefect.getDefectId());
                }
            }
            List<StepDefect> associatedStepDefects = stepResultManager.getStepResultsWithDefectBySchedule(schedule.getID());
            if (associatedStepDefects != null) {
                for (StepDefect stepDefect : associatedStepDefects) {
                    uniqueDefectList.add(stepDefect.getDefectId());
                }
            }
        }
    }
    
    private void createHTMLRequirementDefectRows (FileOutputStream fos, Collection<Long> requirementIds, Long versionId) throws Exception{
    	PrintWriter out = null;
    	try {
            out = new PrintWriter(new OutputStreamWriter(fos, "UTF-8"), false);
            TraceabilityResourceHelper traceabilityResourceHelper 
    			= new TraceabilityResourceHelper(scheduleManager, stepResultManager, issueManager, authContext, searchProvider);
            Html html = new Html(out);
            for (Long requirementId : requirementIds) {
            	Integer totalRowCount = 0;
                try {
    	            Issue requirement = issueManager.getIssueObject(requirementId.longValue());
    	            List<Issue> testList = traceabilityResourceHelper.findLinkedTestsWithIssue(requirementId.toString());
                    Map<Long, List<Schedule>> testExecutionsMap = new TreeMap<Long, List<Schedule>>();
                    Map<Long, Issue> testMap = new TreeMap<Long, Issue>();
                    Set<Object> uniqueDefectList = new TreeSet<Object>();
                    List<Long> invalidPermissionTests = new ArrayList<Long>();

                    for (Issue test : testList) {
    	            	ProjectPermissionKey projectPermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_BROWSE_CYCLE.toString());
    	            	boolean hasZephyrPermission = zephyrPermissionManager.validateUserPermission(projectPermissionKey, test.getProjectObject(), authContext.getLoggedInUser(), test.getProjectObject().getId());
    	            	testMap.put(test.getId(), test);
    	                Integer offset = 0;
    	                Integer maxResult = 11;
    	                Integer totalCount = 0;
    	                List<Schedule> executionList = new ArrayList<Schedule>();
    	                do {
    	                    List<Schedule> schedules = scheduleManager.getSchedulesByIssueId(test.getId().intValue(), offset,null);
    	                    executionList.addAll(schedules);
    	                    getUniqueDefects(uniqueDefectList, schedules);
    	                    totalCount = scheduleManager.getSchedulesCountByIssueId(test.getId().intValue());
    	                    offset = offset + maxResult ;
    	                } while (offset < totalCount);
    	                totalRowCount += totalCount;
    	                if (totalCount == 0) {
    	                    totalRowCount++;
    	                }
    	                testExecutionsMap.put(test.getId(), executionList);
    	                if(!hasZephyrPermission) {
    	                	invalidPermissionTests.add(test.getId());
    	                }
    	            }
                    Issue requirementCell = requirement;
                    if (testExecutionsMap.size() == 0) {
                        html.tr();
                        createReqHtmlEl(html ,requirementCell, 0, 0);
                        createTestHtmlEl(html, null, null, true);
                        if(invalidPermissionTests.isEmpty()) {
                            createExecutionHtmlEl(html, null, false,false,true);
                            createDefectHtmlEl(html, null,false);
                        } else {
                        	createExecutionHtmlEl(html, null, false,true,true);
                            createDefectHtmlEl(html, null,true);
                        }
                        html.end();
                        out.flush();
                    }
                    for(Map.Entry<Long,List<Schedule>> entry:testExecutionsMap.entrySet()) {
                        Issue testCell = testMap.get(entry.getKey());
                        if (entry.getValue().size() == 0) {
                            html.tr();
                            createReqHtmlEl(html ,requirementCell, totalRowCount.intValue(), 0);
                            createTestHtmlEl(html, testCell, entry.getValue().size(), true);
                            if(invalidPermissionTests.isEmpty()) {
                            	createExecutionHtmlEl(html, null, false,false, true);
                                createDefectHtmlEl(html, null,false);
                            }  else {
                            	createExecutionHtmlEl(html, null, false,true, true);
                                createDefectHtmlEl(html, null,true);
                            }
                            html.end();
                            out.flush();
                            requirementCell = null;
                        }
                        for (Schedule schedule:entry.getValue()) {
                            html.tr();
                            createReqHtmlEl(html ,requirementCell, totalRowCount.intValue(), uniqueDefectList.size());
                            createTestHtmlEl(html, testCell, entry.getValue().size(), false);
                            if(invalidPermissionTests.isEmpty()) {
                            	createExecutionHtmlEl(html, schedule, false,false, true);
                                createDefectHtmlEl(html, schedule,false);
                            } else {
                            	createExecutionHtmlEl(html, schedule, false,true, true);
                                createDefectHtmlEl(html, schedule,true);
                            }
                            html.end();
                            out.flush();
                            testCell = null;
                            requirementCell = null;
                        }
                    }
                } catch (Exception exception) {
                    log.error("Error getting issue, probably this issue doesn't exist.", exception);
                }
            }
        } catch (UnsupportedEncodingException e) {
            log.error("Error create Requirement-Defect file.", e);
            throw new Exception(e);
        } finally {
        	if(out != null) {
        		out.close();
        	}
        }
    }
    
    
    private void createHTMLDefectRequirementRows (FileOutputStream fos, Collection<Long> defectIds, Long versionId) throws Exception {
        PrintWriter out = null;
        try {
            out = new PrintWriter(new OutputStreamWriter(fos, "UTF-8"), false);
	        TraceabilityResourceHelper traceabilityResourceHelper 
				= new TraceabilityResourceHelper(scheduleManager, stepResultManager, issueManager, authContext, searchProvider);
	        Html html = new Html(out);
	        for (Long defectId : defectIds) {
	            try {
	                Issue defect = issueManager.getIssueObject(defectId);
	                Issue defectCell = defect;
	                if(defectCell == null) {
	                	html.tr();
                        createReqHtmlEl(html , defectCell, 0, 0);
                        createTestHtmlEl(html, null, null, true);
                        createExecutionHtmlEl(html, null, false,false, true);
                        createDefectHtmlEl(html, null,false);
                        html.end();
                        out.flush();
	                } else {
		            	ProjectPermissionKey projectPermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_BROWSE_CYCLE.toString());
		            	boolean hasZephyrPermission = zephyrPermissionManager.validateUserPermission(projectPermissionKey, defect.getProjectObject(), authContext.getLoggedInUser(), defect.getProjectObject().getId());
    	            	if(hasZephyrPermission) {
			                List<Schedule> schedules = scheduleManager.getSchedulesByDefectId(defectId.intValue(),true);
			                if (schedules.size() == 0) {
		                        html.tr();
		                        createReqHtmlEl(html ,defectCell, 0, 0);
		                        createTestHtmlEl(html, null, null, true);
		                        createExecutionHtmlEl(html, null, false,false, true);
		                        createDefectHtmlEl(html, null,false);
		                        html.end();
		                        out.flush();
		                    }
			                for (Schedule schedule : schedules) {
			                    Issue test = issueManager.getIssueObject(schedule.getIssueId().longValue());
								boolean hasViewIssuePermission = JiraUtil.hasIssueViewPermission(null,test,authContext.getLoggedInUser());
								if(!hasViewIssuePermission) {
									List<Issue> requirements = traceabilityResourceHelper.findLinkedIssuesWithTest(test.getId().toString(), defect.getIssueTypeObject().getId().toString());
									html.tr();
									createReqHtmlEl(html, defectCell, schedules.size(), null);
									createExecutionHtmlEl(html, schedule, false, false, false);
									html.td().span().text(authContext.getI18nHelper().getText("zephyr.viewissue.permission.error","Test","Defect/Requirement")).end();
									createReqCellFromList(html, requirements);
									html.end();
									out.flush();
									defectCell = null;
								} else {
									List<Issue> requirements = traceabilityResourceHelper.findLinkedIssuesWithTest(test.getId().toString(), defect.getIssueTypeObject().getId().toString());
									html.tr();
									createReqHtmlEl(html, defectCell, schedules.size(), null);
									createExecutionHtmlEl(html, schedule, false, false,true);
									createTestHtmlEl(html, test, 0, true);
									createReqCellFromList(html, requirements);
									html.end();
									out.flush();
									defectCell = null;
								}
			                }
    	            	} else {
		                    html.tr();
		                    html.td().span().text(authContext.getI18nHelper().getText("zephyr.plugin.permissions.invalid.error")).end();
		                    createExecutionHtmlEl(html, null, false,true, true);
		                    html.td().span().text(authContext.getI18nHelper().getText("zephyr.plugin.permissions.invalid.error")).end();
		                    html.td().span().text(authContext.getI18nHelper().getText("zephyr.plugin.permissions.invalid.error")).end();
		                    html.end();
		                    out.flush();
		                    defectCell = null;
    	            	}
	                }
	            } catch (Exception exception) {
	                log.error("Error getting issue, probably this issue doesn't exist", exception);
	            }
	        }
        } catch (UnsupportedEncodingException e) {
            log.error("Error create DefectRequirement html file.", e);
            throw new Exception(e);
        } finally {
        	if(out != null) {
        		out.close();
        	}
        }
    }
    
    private void createReqHtmlEl (Html html, Issue requirement, Integer rowspan, Integer totalDefectCount) {
        if (requirement != null) {
        	if(JiraUtil.hasIssueViewPermission(null,requirement,authContext.getLoggedInUser())) {
				if (rowspan != 0) {
					html.td().attr("rowspan", rowspan.toString())
							.span().classAttr("keyColor").text(requirement.getKey()).end().br()
							.span().text(requirement.getSummary()).end().br()
							.strong().text(authContext.getI18nHelper().getText("zephyr-je.pdb.traceability.report.status.label") + ":").end().span().text(requirement.getStatusObject().getName()).end().br();
				} else {
					html.td().span().classAttr("keyColor").text(requirement.getKey()).end().br()
							.span().text(requirement.getSummary()).end().br()
							.strong().text(authContext.getI18nHelper().getText("zephyr-je.pdb.traceability.report.status.label") + ":").end().span().text(requirement.getStatusObject().getName()).end().br();
				}
				if (totalDefectCount != null) {
					html.strong().text("Total Unique Defects:").end().span().text(totalDefectCount.toString()).end();
				}
				html.end();
			} else {
                if(rowspan != 0) {
                    html.td()
                            .span().text(authContext.getI18nHelper().getText("zephyr.plugin.permissions.invalid.error")).end()
                            .br();
                } else {
                    html.td()
                            .span().text(authContext.getI18nHelper().getText("zephyr.plugin.permissions.invalid.error")).end()
                            .br();
                }
                if (totalDefectCount != null) {
                    html.strong().text("Total Unique Defects:").end().span().text(totalDefectCount.toString()).end();
                }
                html.end();
			}

        }
    }

    private void createTestHtmlEl (Html html, Issue test, Integer rowspan, Boolean createDashIfEmpty) {
        if (test != null) {
        	if(rowspan != 0) {
        		html.td().attr("rowspan", rowspan.toString())
		                .span().classAttr("keyColor").text(test.getKey()).end().br()
		                .span().text(test.getSummary()).end()
		                .end();
        	} else {
        		html.td().span().classAttr("keyColor").text(test.getKey()).end().br()
		                .span().text(test.getSummary()).end()
		                .end();
        	}            
        } else {
            if (createDashIfEmpty) {
                html.td().span().text("-").end().end();
            }
        }
    }

    private void createExecutionHtmlEl (Html html, Schedule schedule, Boolean stepLevel, boolean failedPermissionError, boolean hasIssueViewPermissionError) {
        if(!failedPermissionError) {
	    	if (schedule != null) {
	    		if(hasIssueViewPermissionError) {
					ExecutionStatus executionStatus = JiraUtil.getExecutionStatuses().get(Integer.parseInt(schedule.getStatus()));
					html.td()
							.strong().text(authContext.getI18nHelper().getText("je.gadget.common.cycle.label") + ":").end().span().text(schedule.getCycle() != null ? schedule.getCycle().getName() : ApplicationConstants.AD_HOC_CYCLE_NAME).end().br();
					if(null != schedule.getFolder()) {
					    html.strong().text(authContext.getI18nHelper().getText("enav.newfolder.name.label") + ":").end().span().text(schedule.getFolder() != null ? schedule.getFolder().getName() : StringUtils.EMPTY).end().br();
                    }
					html.strong().text(authContext.getI18nHelper().getText("zephyr-je.pdb.traceability.report.status.label") + ":").end().span().text(executionStatus.getName()).end();
					if (stepLevel) {
						html.span().classAttr("badge").text("step level").end();
					}
					html.end();
				} else {
					html.td()
							.strong().text(authContext.getI18nHelper().getText("je.gadget.common.cycle.label") + ":").end().span().text(ApplicationConstants.MASKED_DATA).end().br();
					if(null != schedule.getFolder()) {
					    html.strong().text(authContext.getI18nHelper().getText("enav.newfolder.name.label") + ":").end().span().text(ApplicationConstants.MASKED_DATA).end().br();
                    }

							html.strong().text(authContext.getI18nHelper().getText("zephyr-je.pdb.traceability.report.status.label") + ":").end().span().text(ApplicationConstants.MASKED_DATA).end();
					if (stepLevel) {
						html.span().classAttr("badge").text("step level").end();
					}
					html.end();
				}
	        }  else {
	            html.td()
                .span().text("-").end()
                .end();
	        }
        } else {
    		 html.td()
             .span().text(authContext.getI18nHelper().getText("zephyr.plugin.permissions.invalid.error")).end()
             .end();
        }
    }

    private void createDefectHtmlEl (Html html, Schedule schedule, boolean failedZephyrPermission) {
        if(!failedZephyrPermission) {
	    	if (schedule != null ) {
	            html.td().style("padding:0px");
	            Collection<ScheduleDefect> allDefects = scheduleManager.getAssociatedDefects(schedule.getID());
	            Collection<StepDefect> stepDefects = stepResultManager.getStepResultsWithDefectBySchedule(schedule.getID());
	            html.table().attr("rules", "rows").tbody();
	            for (ScheduleDefect executionLevelDefect : allDefects) {
	            	Issue defect = issueManager.getIssueObject(executionLevelDefect.getDefectId().longValue());
	            	boolean hasViewIssuePermission = JiraUtil.hasIssueViewPermission(null,defect,authContext.getLoggedInUser());
	            	if(!hasViewIssuePermission) {
						html
								.tr()
								.td()
								.span().classAttr("keyColor").text(authContext.getI18nHelper().getText("zephyr.viewissue.permission.error","Defect","Execution")).end().br()
								.span().text("-").end().br()
								.strong().text(authContext.getI18nHelper().getText("zephyr-je.pdb.traceability.report.status.label") + ":").end().span().text("-").end()
								.end()
								.end();
					} else {
						html
								.tr()
								.td()
								.span().classAttr("keyColor").text(defect.getKey()).end().br()
								.span().text(defect.getSummary()).end().br()
								.strong().text(authContext.getI18nHelper().getText("zephyr-je.pdb.traceability.report.status.label") + ":").end().span().text(defect.getStatusObject().getName()).end()
								.end()
								.end();
					}
	            }
	            for (StepDefect stepLevelDefectDefect : stepDefects) {
	            	Issue defect = issueManager.getIssueObject(stepLevelDefectDefect.getDefectId().longValue());
					boolean hasViewIssuePermission = JiraUtil.hasIssueViewPermission(null,defect,authContext.getLoggedInUser());
					if(!hasViewIssuePermission) {
						html
								.tr()
								.td()
								.span().classAttr("keyColor").text(authContext.getI18nHelper().getText("zephyr.viewissue.permission.error","Defect","StepResult")).end().br()
								.span().text("-").end().br()
								.strong().text(authContext.getI18nHelper().getText("zephyr-je.pdb.traceability.report.status.label") + ":").end().span().text("-").end()
								.span().classAttr("badge").text("step level").end()
								.end()
								.end();
					} else {
						html
								.tr()
								.td()
								.span().classAttr("keyColor").text(defect.getKey()).end().br()
								.span().text(defect.getSummary()).end().br()
								.strong().text(authContext.getI18nHelper().getText("zephyr-je.pdb.traceability.report.status.label") + ":").end().span().text(defect.getStatusObject().getName()).end()
								.span().classAttr("badge").text("step level").end()
								.end()
								.end();
					}
	            }
	            html.end().end().end();
	        } else {
	            html.td()
	                    .span().text("-").end()
	                    .end();
	        }
        } else {
   		 html.td()
         .span().text(authContext.getI18nHelper().getText("zephyr.plugin.permissions.invalid.error")).end()
         .end();       	
        }
    }
    
    
    private void createReqCellFromList (Html html, List<Issue> requirements) {
        if (requirements != null && requirements.size() != 0) {
            html.td().style("padding:0px");
            html.table().attr("rules", "rows").tbody();
            for (Issue requirement : requirements) {
            	boolean hasViewIssuePermission = JiraUtil.hasIssueViewPermission(null,requirement,authContext.getLoggedInUser());
            	if(!hasViewIssuePermission) {
					html
							.tr()
							.td()
							.span().classAttr("keyColor").text(authContext.getI18nHelper().getText("zephyr.viewissue.permission.error","Requirement","Execution")).end().br()
							.span().text("-").end().br()
							.strong().text(authContext.getI18nHelper().getText("zephyr-je.pdb.traceability.report.status.label") + ":").end().span().text("-").end()
							.end()
							.end();
				} else {
					html
							.tr()
							.td()
							.span().classAttr("keyColor").text(requirement.getKey()).end().br()
							.span().text(requirement.getSummary()).end().br()
							.strong().text(authContext.getI18nHelper().getText("zephyr-je.pdb.traceability.report.status.label") + ":").end().span().text(requirement.getStatusObject().getName()).end()
							.end()
							.end();
				}
            }
            html.end().end().end();
        } else {
            html.td().span().text("-").end().end();
        }
    }

   

   
    /**
     * Get custom fields data for execution id.
     * @param scheduleId
     * @param projectId
	 * @return
     */
    private String getCustomFieldsValueDataAsString(Integer scheduleId, Long projectId) {

        Map<String, CustomFieldValueResource.CustomFieldValueResponse> customFieldValueResponseMap =
                customFieldValueResourceDelegate.getCustomFieldValuesByEntityId(scheduleId, ApplicationConstants.ENTITY_TYPE.EXECUTION.name(), null);
        StringJoiner joiner = new StringJoiner(PIPE + "\n");
        if (MapUtils.isNotEmpty(customFieldValueResponseMap)) {
			CustomFieldProject[] allActiveCustomFieldProjects = zephyrCustomFieldManager.getAllActiveCustomFieldsProject();
			Map<Integer,List<Long>> allActiveCustomFieldsProject = new LinkedHashMap<>();
			if(allActiveCustomFieldProjects != null) {
				allActiveCustomFieldsProject = Arrays.stream(allActiveCustomFieldProjects).collect(Collectors.groupingBy(CustomFieldProject::getCustomFieldId,
						Collectors.mapping(f -> f.getProjectId(),
								Collectors.toList())));
			}
			Map<Integer, List<Long>> finalAllActiveCustomFieldsProject = allActiveCustomFieldsProject;
			allActiveCustomFieldsProject = null;
            customFieldValueResponseMap.entrySet().forEach(customFieldValueResponse -> {
				CustomFieldValueResource.CustomFieldValueResponse fieldValueResponse = customFieldValueResponse.getValue();
				if(finalAllActiveCustomFieldsProject.get(Integer.valueOf(customFieldValueResponse.getKey())) != null &&
						finalAllActiveCustomFieldsProject.get(Integer.valueOf(customFieldValueResponse.getKey())).contains(projectId)) {
					if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(fieldValueResponse.getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.DATE_TIME_VALUE)) {
						String value = StringUtils.isNotBlank(fieldValueResponse.getValue()) ? DATE_TIME_FORMAT.format(new Date(Long.parseLong(fieldValueResponse.getValue()))) : StringUtils.EMPTY;
						joiner.add(fieldValueResponse.getCustomFieldName() + COLON + value);
					} else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(fieldValueResponse.getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.DATE_VALUE)) {
						String value = StringUtils.isNotBlank(fieldValueResponse.getValue()) ? DATE_FORMAT.format(new Date(Long.parseLong(fieldValueResponse.getValue()))) : StringUtils.EMPTY;
						joiner.add(fieldValueResponse.getCustomFieldName() + COLON + value);
					} else {
						joiner.add(fieldValueResponse.getCustomFieldName() + COLON + fieldValueResponse.getValue());
					}
				}
            });
            return joiner.toString();
        }
        return EMPTY_VALUE;
    }
}
