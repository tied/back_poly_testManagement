package com.thed.zephyr.je.helper;


import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.search.SearchService.ParseResult;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.datetime.LocalDate;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.customfields.converters.DoubleConverter;
import com.atlassian.jira.issue.customfields.converters.DoubleConverterImpl;
import com.atlassian.jira.issue.fields.renderer.wiki.AtlassianWikiRenderer;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchHandler;
import com.atlassian.jira.issue.search.SearchHandler.ClauseRegistration;
import com.atlassian.jira.jql.util.JqlLocalDateSupport;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.usercompatibility.UserCompatibilityHelper;
import com.atlassian.jira.util.*;
import com.atlassian.jira.util.collect.CollectionUtil;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.query.Query;
import com.atlassian.sal.api.ApplicationProperties;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.opensymphony.util.TextUtils;
import com.thed.zephyr.je.attachment.AttachmentUtils;
import com.thed.zephyr.je.config.model.ExecutionStatus;
import com.thed.zephyr.je.index.ScheduleIndexManager;
import com.thed.zephyr.je.model.*;
import com.thed.zephyr.je.service.*;
import com.thed.zephyr.je.vo.*;
import com.thed.zephyr.je.zql.core.SearchService;
import com.thed.zephyr.je.zql.core.ZephyrClauseHandlerFactory;
import com.thed.zephyr.je.zql.core.mapper.ListValueComparator;
import com.thed.zephyr.je.zql.helper.SearchResult;
import com.thed.zephyr.util.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.NumberTools;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class ScheduleSearchResourceHelper {
	protected final Logger log = Logger.getLogger(ScheduleSearchResourceHelper.class);

	private final SearchService searchService;
	private final ApplicationUser user;
	private IssueManager issueManager;
	private CycleManager cycleManager;
	private VersionManager versionManager;
	private ExportService exportService;
	private TeststepManager testStepManager;
	private StepResultManager stepResultManager;
	private ZephyrClauseHandlerFactory zephyrClauseHandlerFactory;
	private static final int MAX_ADVANCED_VALIDATION_MESSAGES = 10;
	private int ALLOWED_MAX_PAGE_NUMBERS = 5;
	private static final int MAX_RECORDS_ALLOWED = 1000;
	private FolderManager folderManager;
	private JiraAuthenticationContext authContext;
	private ScheduleIndexManager scheduleIndexManager;
	private ZephyrCustomFieldManager zephyrCustomFieldManager;
    private static final DateFormat DATE_TIME_FORMAT = new SimpleDateFormat("MM/dd/yyyy hh:mm a");
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy");

	public ScheduleSearchResourceHelper(ApplicationUser user, SearchService searchService) {
		this.user = user;
		this.searchService = searchService;
	}

	public ScheduleSearchResourceHelper(ApplicationUser user, SearchService searchService, ExportService exportService) {
		this.user = user;
		this.searchService = searchService;
		this.exportService = exportService;
	}

	public ScheduleSearchResourceHelper(ApplicationUser user, SearchService searchService,ZephyrClauseHandlerFactory zephyrClauseHandlerFactory,
			IssueManager issueManager,CycleManager cycleManager,VersionManager versionManager, ZephyrCustomFieldManager zephyrCustomFieldManager) {
		this.user = user;
		this.searchService = searchService;
		this.zephyrClauseHandlerFactory = zephyrClauseHandlerFactory;
		this.issueManager = issueManager;
		this.cycleManager = cycleManager;
		this.versionManager = versionManager;
		this.zephyrCustomFieldManager = zephyrCustomFieldManager;
	}

	public ScheduleSearchResourceHelper(ApplicationUser user, SearchService searchService,ZephyrClauseHandlerFactory zephyrClauseHandlerFactory,
			IssueManager issueManager,CycleManager cycleManager,TeststepManager teststepManager,VersionManager versionManager,ZephyrCustomFieldManager zephyrCustomFieldManager) {
		this.user = user;
		this.searchService = searchService;
		this.zephyrClauseHandlerFactory = zephyrClauseHandlerFactory;
		this.issueManager = issueManager;
		this.cycleManager = cycleManager;
		this.versionManager = versionManager;
		this.testStepManager = teststepManager;
		this.zephyrCustomFieldManager=zephyrCustomFieldManager;
	}

    public ScheduleSearchResourceHelper(ApplicationUser user,
			SearchService searchService,
			ZephyrClauseHandlerFactory zephyrClauseHandlerFactory,
			IssueManager issueManager, VersionManager versionManager,
			CycleManager cycleManager) {
		this.user = user;
		this.searchService = searchService;
		this.issueManager = issueManager;
		this.cycleManager = cycleManager;
		this.versionManager = versionManager;
		this.zephyrClauseHandlerFactory = zephyrClauseHandlerFactory;
	}

	public ScheduleSearchResourceHelper(ApplicationUser user, SearchService searchService,ExportService exportService,
			IssueManager issueManager,CycleManager cycleManager,VersionManager versionManager,TeststepManager testStepManager,StepResultManager stepResultManager) {
		this.user = user;
		this.searchService = searchService;
		this.exportService = exportService;
		this.issueManager = issueManager;
		this.cycleManager = cycleManager;
		this.versionManager = versionManager;
		this.testStepManager = testStepManager;
		this.stepResultManager = stepResultManager;
	}

    public ScheduleSearchResourceHelper(ApplicationUser user, SearchService searchService,ExportService exportService,
                                        IssueManager issueManager,CycleManager cycleManager,VersionManager versionManager,TeststepManager testStepManager,StepResultManager stepResultManager,
                                        FolderManager folderManager,ZephyrCustomFieldManager zephyrCustomFieldManager) {
		this.user = user;
		this.searchService = searchService;
		this.exportService = exportService;
		this.issueManager = issueManager;
		this.cycleManager = cycleManager;
		this.versionManager = versionManager;
		this.testStepManager = testStepManager;
		this.stepResultManager = stepResultManager;
		this.folderManager = folderManager;
		this.zephyrCustomFieldManager = zephyrCustomFieldManager;
	}

    public ScheduleSearchResourceHelper(ApplicationUser user, SearchService searchService,ZephyrClauseHandlerFactory zephyrClauseHandlerFactory,
                                        IssueManager issueManager,CycleManager cycleManager,VersionManager versionManager,JiraAuthenticationContext authContext,
                                            ScheduleIndexManager scheduleIndexManager,ZephyrCustomFieldManager zephyrCustomFieldManager) {
		this.user = user;
		this.searchService = searchService;
		this.zephyrClauseHandlerFactory = zephyrClauseHandlerFactory;
		this.issueManager = issueManager;
		this.cycleManager = cycleManager;
		this.versionManager = versionManager;
		this.authContext = authContext;
		this.scheduleIndexManager = scheduleIndexManager;
		this.zephyrCustomFieldManager = zephyrCustomFieldManager;
	}

	/**
     * Validates the specified {@link com.atlassian.query.Query}. If the query does not pass validation, error
     * messages are added to the JSON Object.
	 *
     * @param query the search query to validate.
	 * @return true if the validation passed; false otherwise.
	 */
	public JSONObject validateQuery(final Query query) {
		JSONObject jsonObject = new JSONObject();
		try {
			if (query == null) {
				jsonObject.put("error", "Invalid Query");
				jsonObject.put("errorDescHtml", "Invalid Query");
				return jsonObject;
			}
			final MessageSet result = searchService.validateQuery(user, query);
	        if (result != null)
	        {
				int maxResult = MAX_ADVANCED_VALIDATION_MESSAGES;
	            if (result.hasAnyErrors())
	            {
					List<String> errorMessages = new ArrayList<String>();
	                for (Iterator<String> iter = result.getErrorMessages().iterator(); iter.hasNext() && maxResult > 0; maxResult--)
	                {
						String error = iter.next();
						errorMessages.add(error);
					}
					if (errorMessages != null && errorMessages.size() > 0) {
						jsonObject.put("error", errorMessages);
						return jsonObject;
					}
				}
				List<String> warningMessages = new ArrayList<String>();
	            for (Iterator<String> iter = result.getWarningMessages().iterator(); iter.hasNext() && maxResult > 0; maxResult--)
	            {
					String warning = iter.next();
					warningMessages.add(warning);
				}
				if (warningMessages != null && warningMessages.size() > 0) {
					jsonObject.put("warning", warningMessages);
					return jsonObject;
				}
			}
		} catch (JSONException e) {
			log.error("JSON Error", e);
			try {
				jsonObject.put("error", "Invalid Query");
				jsonObject.put("errorDescHtml", "Invalid Query");
			} catch (JSONException e1) {
				log.error("JSON Error populating error", e);
			}
		}  catch (Exception e) {
			log.error("Error Validating ZQL:", e);
			try {
				jsonObject.put("error", "Invalid Query");
				jsonObject.put("errorDescHtml", "Invalid Query");
			} catch (JSONException e1) {
				log.error("JSON Error populating error", e);
			}
		}
		return null;
	}

	/**
     * Attempts to parse the "zqlQuery" action parameter into a {@link com.atlassian.query.Query} object. If the
     * parse returns an error, the errors are added to the error collection.
	 * @param zqlQuery
	 *
     * @return the {@link com.atlassian.query.Query} object parsed from the "zqlQuery" action parameter, or null
     * if there was an error.
	 */
    public ParseResult getNewSearchQuery(String zqlQuery)
    {
		// if (StringUtils.isNotBlank(zqlQuery)) {
		// if(zqlQuery.contains("jqlName")) {
		// runJql(zqlQuery);
		// }
		final ParseResult parseResult = searchService.parseQuery(user, zqlQuery);
		return parseResult;
		// }
		// return null;
	}

	/**
	 * Converts Execution Lucene Document to JSON
	 * @param searchResults
	 * @param offset
	 * @param expand
	 * @return
	 */
    public ZQLSearchResultBean convertLuceneDocumentToJson(SearchResult searchResults,
    		Integer offset, Integer maxRecords, String expand) {
		ZQLSearchResultBean resultBean = new ZQLSearchResultBean();
		List<ZQLScheduleBean> schedules = new ArrayList<>();
		try {
			LoadingCache<Integer, String> cycleCache = getNewCycleCache();
			List<Integer> issuePermissions = new ArrayList<>();
					
			for (Document document : searchResults.getDocuments()) {
				ZQLScheduleBean schedule = documentsToJSON(document, cycleCache, expand, issuePermissions);
				if (schedule != null && schedule.getId() != null) {
					schedules.add(schedule);
				}
			}
			cycleCache = null;
			if (schedules.size() > 0) {
				resultBean.setExecutions(schedules);
			}
			resultBean.setOffset(offset);

			Integer maxResultHit = Integer.valueOf(JiraUtil.getSimpleDBProperty(ConfigurationConstants.ZEPHYR_ZQL_RESULT_MAX_ON_PAGE, "20").toString());
			if (maxRecords != null && maxRecords > 0) {
				maxResultHit = maxRecords;
			}
			resultBean.setMaxResultAllowed(maxResultHit);

			int current = offset != null && offset != 0 ? (Math.round(offset / maxResultHit) + 1) : 1;
			resultBean.setCurrentIndex(current);
			resultBean.setLinksNew(generatePageNumbers(searchResults, current, maxResultHit));
			int totalCount = searchResults.getTotal() > 0 ? searchResults.getTotal() : 0;
			resultBean.setTotalCount(totalCount);

			if (expand != null) {
				String[] expandStr = expand.split(",");
				for (String expandStatus : expandStr) {
					if(StringUtils.isNotBlank(expandStatus) && 
							StringUtils.containsIgnoreCase(expandStatus, "executionStatus")) {
						resultBean.setExecutionStatuses(JiraUtil.getExecutionStatusList());
						resultBean.setStepExecutionStatuses(JiraUtil.getStepExecutionStatusList());
					}
				}
			}
			if (resultBean.getExecutionIds() == null) {
				resultBean.setExecutionIds(new ArrayList<>());
			}
		} catch (JSONException e) {
			log.error("JSON Exception Converting Document to JSON",e);
		} catch (ExecutionException e) {
			log.error("ExecutionException Converting Document to JSON",e);
		} catch (Exception e) {
			log.error("Exception Converting Document to JSON",e);
		}
		return resultBean;
	}

	/**
	 * Lucene Document to JSON Response
	 * @param scheduleDocument
	 * @param expand
	 *            //@param securityCounter
	 * @return
	 * @throws JSONException
	 */
	public ZQLScheduleBean documentsToJSON(Document scheduleDocument, LoadingCache<Integer, String> cycleCache, String expand, List<Integer> issuePermissions) throws JSONException, ExecutionException,Exception {
		ZQLScheduleBean schedule = new ZQLScheduleBean();
		String issueId = scheduleDocument.get("ISSUE_ID");
		Issue issue = issueManager.getIssueObject(Long.valueOf(issueId));
		boolean hasIssueViewPermission = JiraUtil.hasIssueViewPermission(null, issue, user);
		if (!hasIssueViewPermission) {
			issuePermissions.add(issuePermissions.size() + 1);
		}
		if (issue != null) {
			String label = scheduleDocument.get("LABEL");
			List<String> labels = new ArrayList<String>();
			String scheduleId = scheduleDocument.get("schedule_id");
			String orderId = scheduleDocument.get("ORDER_ID");

			String executedOnDateStr = scheduleDocument.get("EXECUTED_ON");
			Date date = LuceneUtils.stringToDate(executedOnDateStr);

			DateTimeFormatterFactory dateTimeFormatterFactory = ComponentAccessor.getComponent(DateTimeFormatterFactory.class);
			DateTimeFormatter df = dateTimeFormatterFactory.formatter().forLoggedInUser().withStyle(DateTimeStyle.DATE_PICKER);

			String createdOnDateStr = scheduleDocument.get("DATE_CREATED");
			Date creationDate = LuceneUtils.stringToDate(createdOnDateStr);

			schedule.setIssueId(issueId);

			String projectName = issue.getProjectObject().getName();
			if (!hasIssueViewPermission) {
				maskIssueFields(schedule, issue, scheduleDocument, expand);
			} else {
				String comment = scheduleDocument.get("COMMENT");
				if (comment != null && !StringUtils.equalsIgnoreCase(comment, "-1")) {
					schedule.setComment(comment);
					schedule.setHtmlComment(TextUtils.plainTextToHtml(schedule.getComment(), "_blank", true));
				} else {
					schedule.setComment("");
					schedule.setHtmlComment("");
				}
				if (null != label && !StringUtils.equalsIgnoreCase(label, "-1")) {
					String[] labelsArray = scheduleDocument.getValues("LABEL");
					labels = Arrays.asList(labelsArray);
				}
				schedule.setLabels(labels);
				schedule.setIssueDescription(ComponentAccessor.getRendererManager().
						getRenderedContent(AtlassianWikiRenderer.RENDERER_TYPE, issue.getDescription(), issue.getIssueRenderContext()));
				Priority priorityObject = issue.getPriorityObject();
				String priorityName = null != priorityObject ? priorityObject.getName() : " ";
				String versionName = "";
				Long versionId = new Long(-1);
				if (scheduleDocument.get("VERSION_ID") != null) {
					if (StringUtils.equalsIgnoreCase(scheduleDocument.get("VERSION_ID"), "-1")) {
						versionName = "Unscheduled";
					} else {
						Version version = versionManager.getVersion(Long.valueOf(scheduleDocument.get("VERSION_ID")));
						if (version != null) {
							versionName = version.getName();
							versionId = version.getId();
						}
					}
				}
				List<Map<String, Object>> components = new ArrayList<Map<String, Object>>();
				for (ProjectComponent projectComponent : issue.getComponentObjects()) {
					Map<String, Object> componentMap = new HashMap<String, Object>();
					componentMap.put("id", projectComponent.getId());
					componentMap.put("name", projectComponent.getName());
					components.add(componentMap);
				}
				schedule.setIssueKey(issue.getKey());
				schedule.setIssueSummary(issue.getSummary());
				schedule.setPriority(priorityName);
				schedule.setComponents(components);
				schedule.setVersionName(versionName);
				schedule.setVersionId(versionId);
				schedule.setProjectKey(issue.getProjectObject().getKey());
				schedule.setProject(projectName);
				String executedBy = scheduleDocument.get("EXECUTED_BY");
				String executedByUserName = "";
				String executedByDisplay = "";
				if (executedBy == null || StringUtils.equalsIgnoreCase(executedBy, "-1")) {
					executedBy = "";
				} else {
					User user = UserCompatibilityHelper.getUserForKey(executedBy);
					executedBy = user != null ? user.getDisplayName() : executedBy;
					executedByUserName = user != null ? user.getName() : executedBy;
					executedByDisplay = (user != null && user.isActive()) ? executedBy : executedBy + " (Inactive)";
				}
				String executionStatus = scheduleDocument.get("STATUS");
				ExecutionStatus execStatus = JiraUtil.getExecutionStatuses().get(new Integer(executionStatus));

				Integer cycleId = Integer.valueOf(scheduleDocument.get("CYCLE_ID"));
				schedule.setCycleName(cycleCache.get(cycleId));

				schedule.setCycleId(cycleId);
				schedule.setId(StringUtils.isNotEmpty(scheduleId) ? Integer.parseInt(scheduleId) : 0);
				schedule.setOrderId(StringUtils.isNotEmpty(orderId) ? Integer.valueOf(orderId) : 0);
				schedule.setStatus(execStatus);
				schedule.setExecutedBy(executedBy);
				schedule.setExecutedByUserName(executedByUserName);
				schedule.setExecutedByDisplay(executedByDisplay);
				schedule.setCreationDate(df.format(creationDate));
				schedule.setProjectId(issue.getProjectObject().getId());
				schedule.setProjectAvatarId(issue.getProjectObject().getAvatar().getId());
				if (date != null && StringUtils.isNotBlank(executedBy)) {
					schedule.setExecutedOn(df.format(date));
				} else {
					schedule.setExecutedOn("");
				}

				String assignedTo = scheduleDocument.get("ASSIGNED_TO");
				String assigneeUserName = "";
				String assigneeDisplay = "";
				if (assignedTo == null || StringUtils.equalsIgnoreCase(assignedTo, "-1")) {
					assignedTo = "";
				} else {
					User user = UserCompatibilityHelper.getUserForKey(assignedTo);
					assignedTo = user != null ? user.getDisplayName() : assignedTo;
					assigneeUserName = user != null ? user.getName() : assignedTo;
					assigneeDisplay = (user != null && user.isActive()) ? assignedTo : assignedTo + " (Inactive)";
				}
				schedule.setAssignee(assignedTo);
				schedule.setAssigneeDisplay(assigneeDisplay);
				schedule.setAssigneeUserName(assigneeUserName);

				if (scheduleDocument.get("FOLDER_ID") != null && !scheduleDocument.get("FOLDER_ID").equals(String.valueOf(ApplicationConstants.NULL_VALUE))) {
					if(folderManager == null) {
						folderManager = (FolderManager) ZephyrComponentAccessor.getInstance().getComponent("folder-manager");
					}
					Folder folder = folderManager.getFolder(Long.valueOf(scheduleDocument.get("FOLDER_ID")));
					if (folder != null) {
						schedule.setFolderId(Long.valueOf(folder.getID() + ""));
						schedule.setFolderName(folder.getName());
					}
				}

				// Below code sets the ExecutionDefect/StepDefect and counts.
				String[] executionDefectKeys = scheduleDocument.getValues("SCHEDULE_DEFECT_KEY");
				List<String> testDefectsArrUnMasked = new ArrayList<>();
				if (executionDefectKeys != null && executionDefectKeys.length > 0) {
					List<ExecutionDefectBean> executionDefects = new ArrayList<ExecutionDefectBean>();

					for (String defectKey : executionDefectKeys) {
						testDefectsArrUnMasked.add(defectKey);
						ExecutionDefectBean executionDefect = IssueUtils.convertIssueToExecutionDefect(defectKey);
						if (executionDefect != null)
							executionDefects.add(executionDefect);
					}
					schedule.setExecutionDefects(executionDefects);
					schedule.setTestDefectsUnMasked(testDefectsArrUnMasked);
				} else {
					schedule.setExecutionDefects(new ArrayList<ExecutionDefectBean>(0));
					schedule.setTestDefectsUnMasked(new ArrayList<String>(0));
				}
				List<String> stepDefectsArrUnMasked = new ArrayList<>();
				String[] stepDefectKeys = scheduleDocument.getValues("STEP_DEFECT_KEY");
				if (stepDefectKeys != null && stepDefectKeys.length > 0) {
					if (JiraUtil.isIssueSecurityEnabled()) {
						List<String> stepDefectsArr = new ArrayList<>();
						for (String defectKey : stepDefectKeys) {
							Issue defectIssue = ComponentAccessor.getIssueManager().getIssueObject(defectKey);
							if (defectIssue != null) {
								stepDefectsArrUnMasked.add(defectIssue.getKey());
								if (JiraUtil.hasIssueViewPermission(null, defectIssue, ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser())) {
									stepDefectsArr.add(defectIssue.getKey());
								} else {
									stepDefectsArr.add(ApplicationConstants.MASKED_DATA);
								}
							}
						}
						schedule.setStepDefects(stepDefectsArr);
						schedule.setStepDefectsUnMasked(stepDefectsArrUnMasked);
					} else {
						schedule.setStepDefects(Arrays.asList(stepDefectKeys));
						schedule.setStepDefectsUnMasked(Arrays.asList(stepDefectKeys));
					}
				} else {
                    schedule.setStepDefects(new ArrayList<>(0));
                    schedule.setStepDefectsUnMasked(new ArrayList<>(0));
                }

				if(Objects.nonNull(scheduleDocument.get("ESTIMATED_TIME")) && !scheduleDocument.get("ESTIMATED_TIME").equals(String.valueOf(ApplicationConstants.NULL_VALUE))) {
					long estimatedTime = NumberTools.stringToLong(scheduleDocument.get("ESTIMATED_TIME"));
					schedule.setEstimatedTime(estimatedTime);
					JiraDurationUtils jiraDurationUtils = ComponentAccessor.getJiraDurationUtils();
					String formattedEstimatedTime = jiraDurationUtils.getFormattedDuration(estimatedTime, ComponentAccessor.getJiraAuthenticationContext().getLocale());
					schedule.setFormattedEstimatedTime(formattedEstimatedTime);
				}
				
				if(Objects.nonNull(scheduleDocument.get("LOGGED_TIME")) && !scheduleDocument.get("LOGGED_TIME").equals(String.valueOf(ApplicationConstants.NULL_VALUE))) {
					long loggedTime = NumberTools.stringToLong(scheduleDocument.get("LOGGED_TIME"));
					schedule.setLoggedTime(loggedTime);
					JiraDurationUtils jiraDurationUtils = ComponentAccessor.getJiraDurationUtils();
					String formattedLoggedTime = jiraDurationUtils.getFormattedDuration(loggedTime, ComponentAccessor.getJiraAuthenticationContext().getLocale());
					schedule.setFormattedLoggedTime(formattedLoggedTime);
				}

                if(Objects.nonNull(scheduleDocument.get("EXECUTION_WORKFLOW_STATUS")) && !scheduleDocument.get("EXECUTION_WORKFLOW_STATUS").equals(String.valueOf(ApplicationConstants.NULL_VALUE))) {
                    schedule.setExecutionWorkflowStatus(scheduleDocument.get("EXECUTION_WORKFLOW_STATUS"));
                }
			}
			schedule.setExecutionDefectCount(ZCollectionUtils.getAsOptionalInteger(scheduleDocument.get("SCHEDULE_DEFECT_COUNT")).or(0));
			schedule.setStepDefectCount(ZCollectionUtils.getAsOptionalInteger(scheduleDocument.get("STEP_DEFECT_COUNT")).or(0));
			schedule.setTotalDefectCount(ZCollectionUtils.getAsOptionalInteger(scheduleDocument.get("TOTAL_DEFECT_COUNT")).or(0));
            Map<String,String> customFieldsValueMap = getCustomFieldValuesFromScheduleDocument(Integer.parseInt(scheduleId),scheduleDocument);
            if(MapUtils.isNotEmpty(customFieldsValueMap)) {
                schedule.setCustomFieldsValueMap(customFieldsValueMap);
            }
			if (expand != null) {
				String[] expandStr = expand.split(",");
				for (String expandStatus : expandStr) {
						if(StringUtils.isNotBlank(expandStatus) &&
								StringUtils.containsIgnoreCase(expandStatus, "teststeps")) {
						
						final List<Teststep> values = testStepManager.getTeststeps(issue.getId(), Optional.empty(), Optional.empty());
						final List<StepResult> steps = stepResultManager.getStepResultsBySchedule(Integer.valueOf(scheduleId));
						final Map<Integer, String> commentMapByStepId = commentMapByStepId(steps);
						final Map<Integer, String> statusMapByStepId = statusMapByStepId(steps);
						if ((null != values && !values.isEmpty())) {
							List<TeststepBean> result = new ArrayList<TeststepBean>();
							for (Iterator<Teststep> it = values.iterator(); it.hasNext();) {
								Teststep teststepObj = it.next();
								if (teststepObj == null) {
									continue;
								}
								String stepComment = !commentMapByStepId.isEmpty() ? commentMapByStepId.get(teststepObj.getID()) : "";
								String stepStatus = !statusMapByStepId.isEmpty() ? statusMapByStepId.get(teststepObj.getID()) : "-1";
								TeststepBean stepBean = new TeststepBean(teststepObj.getID(), teststepObj.getOrderId(), teststepObj.getStep(), teststepObj.getData(),
										teststepObj.getResult(), stepComment,
										JiraUtil.getStepExecutionStatuses().get(new Integer(stepStatus != null ? stepStatus : "-1")).getName(), issue);
								/**
								 * Convert the wiki markup to text for export
								 */
									/*stepBean.step = ZephyrWikiParser.WIKIPARSER.convertWikiToText(stepBean.step);
									stepBean.data = ZephyrWikiParser.WIKIPARSER.convertWikiToText(stepBean.htmlData);
									stepBean.result = ZephyrWikiParser.WIKIPARSER.convertWikiToText(stepBean.htmlResult);
									stepBean.stepComment = ZephyrWikiParser.WIKIPARSER.convertWikiToText(stepBean.htmlStepComment);
								 */
								List<TestStepCf> testStepCfList = testStepManager.getCustomFieldValuesForTeststep(teststepObj.getID());
								if(CollectionUtils.isNotEmpty(testStepCfList)) {
								    testStepCfList.forEach(testStepCf -> {
								    	if(null != testStepCf.getCustomField().getName()) {
                                            stepBean.getCustomFieldValuesMap().put(testStepCf.getCustomField().getName().replaceAll("\\s",StringUtils.EMPTY), getValue(testStepCf));
                                        }
                                    });
                                }
								result.add(stepBean);
							}
							schedule.setTestStepBean(result);
						}

					}
				}
			}
		}

		return schedule;
	}

    private String getValue(Object object) {
		if (object instanceof TestStepCf) {
			TestStepCf testStepCf = (TestStepCf) object;
            if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(testStepCf.getCustomField().getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.DATE_TIME_VALUE)) {
                String value = testStepCf.getDateValue().getTime() + StringUtils.EMPTY;
                return StringUtils.isNotBlank(value) ? DATE_TIME_FORMAT.format(new Date(Long.parseLong(value))) : StringUtils.EMPTY;
            } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(testStepCf.getCustomField().getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.DATE_VALUE)) {
                String value = testStepCf.getDateValue().getTime() + StringUtils.EMPTY;
                return StringUtils.isNotBlank(value) ? DATE_FORMAT.format(new Date(Long.parseLong(value))) : StringUtils.EMPTY;
            } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(testStepCf.getCustomField().getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.LARGE_VALUE)) {
                return testStepCf.getLargeValue();
            } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(testStepCf.getCustomField().getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.NUMBER_VALUE)) {
				DoubleConverter doubleConverter = new DoubleConverterImpl(authContext);
				final String doubleValue = doubleConverter.getStringForChangelog(Double.valueOf(testStepCf.getNumberValue()));
            	return doubleValue;
            } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(testStepCf.getCustomField().getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.STRING_VALUE) ||
					ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(testStepCf.getCustomField().getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.LIST_VALUE)) {
                return testStepCf.getStringValue();
            }
		}
		return StringUtils.EMPTY;
	}

	public LoadingCache<Integer, String> getNewCycleCache() {
		return CacheBuilder.newBuilder().maximumSize(1000).build(new CacheLoader<Integer, String>() {
			@Override
			public String load(Integer cycleId) throws Exception {
				if (cycleId == ApplicationConstants.AD_HOC_CYCLE_ID) {
					return ComponentAccessor.getComponent(JiraAuthenticationContext.class).getI18nHelper().getText("zephyr.je.cycle.adhoc");
				} else {
					Cycle cycle = cycleManager.getCycle(Long.valueOf(cycleId));
					if (cycle != null) {
						return cycle.getName();
					}
				}
				return ComponentAccessor.getComponent(JiraAuthenticationContext.class).getI18nHelper().getText("project.cycle.summary.notfound.error", cycleId);
			}
		});
	}

	/**
	 * This method accepts a List<StepResult>, iterate over it and create a Map<Integer, String>.
	 * For any given step, key in this Map is "Step Id" and value is "Step Comment".
	 * @param steps
	 * @return commentMapByStepId
	 */
	private Map<Integer, String> commentMapByStepId(List<StepResult> steps) {
		Map<Integer, String> commentMapByStepId = new HashMap<Integer, String>();
		for (StepResult stepResult : steps) {
			if (!commentMapByStepId.containsKey(Integer.valueOf(stepResult.getStep().getID())))
				commentMapByStepId.put(Integer.valueOf(stepResult.getStep().getID()), stepResult.getComment());
		}
		return commentMapByStepId;
	}

	/**
	 * This method accepts a List<StepResult>, iterate over it and create a Map<Integer, String>.
	 * For any given step, key in this Map is "Step Id" and value is "Step STATUS".
	 * @param steps
	 * @return commentMapByStepId
	 */
	private Map<Integer, String> statusMapByStepId(List<StepResult> steps) {
		Map<Integer, String> statusMapByStepId = new HashMap<Integer, String>();
		for (StepResult stepResult : steps) {
			if (!statusMapByStepId.containsKey(Integer.valueOf(stepResult.getStep().getID())))
				statusMapByStepId.put(Integer.valueOf(stepResult.getStep().getID()), stepResult.getStatus());
		}
		return statusMapByStepId;
	}

	/**
	 * Exports Executions based on ZQL Search
	 * @param exportType
	 * @param zqlQuery
	 * @param startIndex
	 * @param maxAllowedResult
	 * @return
	 */
	public Response exportExecutions(final String exportType,
			final String zqlQuery, final Integer startIndex, final String expand,boolean maxAllowedResult) {
		ParseResult parseResult = getNewSearchQuery(zqlQuery);
		Query currentQuery = null;
		if (parseResult.isValid())
        {
			currentQuery = parseResult.getQuery();
		} else {
			if (parseResult.getErrors() != null && !parseResult.getErrors().getErrorMessages().isEmpty()) {
				return parseQuery(parseResult);
			}
		}
		log.debug("Search query for export executions: " + currentQuery);
		// validates Atlassian query object and returns JSONObject with the error if any
		JSONObject jsonObject = validateQuery(currentQuery);
		int start = startIndex;
		try {
			if (jsonObject != null) {
				log.error("[Error] [Error code:" + Status.NOT_ACCEPTABLE.getStatusCode() + " " + Status.NOT_ACCEPTABLE + " Error Message :" + jsonObject.toString());
				return getDefaultJSONError(jsonObject);
			} else {

				Integer maxResultHit = Integer.valueOf(JiraUtil.getSimpleDBProperty(ConfigurationConstants.ZEPHYR_ZQL_RESULT_MAX_ON_PAGE, "20").toString());
				if (startIndex != null) {
					start = startIndex.intValue() * maxResultHit;
				}
	    		// Converts Atlassian Query object to Lucene Query and searches the Index. This is the meat of the call.
				SearchResult searchResults = searchService.search(user, currentQuery, start, maxAllowedResult, null, false);
				if (searchResults != null && (searchResults.getDocuments() == null || searchResults.getDocuments().size() == 0)) {
					Map<String, String> errorMap = new HashMap<String, String>();
					ResponseBuilder builder = Response.status(Response.Status.NOT_ACCEPTABLE);
					builder.type(MediaType.APPLICATION_JSON);
					errorMap.put("warning", "No matching record found.");
					builder.entity(errorMap);
					log.error("[Error] [Error code:" + Status.NOT_ACCEPTABLE.getStatusCode() + " " + Status.NOT_ACCEPTABLE + " Error Message :" + errorMap);
					return builder.build();
				}
				LoadingCache<Integer, String> cycleCache = getNewCycleCache();
				List<ZQLScheduleBean> schedules = new ArrayList<ZQLScheduleBean>();
				List<Integer> issuePermission = new ArrayList<>();
				for (Document document : searchResults.getDocuments()) {
					ZQLScheduleBean schedule = documentsToJSON(document, cycleCache, expand, issuePermission);
					if (schedule.getId() != null) {
						schedules.add(schedule);
					}
				}
				DateFormat df = new SimpleDateFormat(ApplicationConstants.ZFJ_DATE_FORMAT);
				File file = null;
				if (StringUtils.equalsIgnoreCase(exportType, "XML")) {
					final StreamingOutput so = exportService.generate(schedules, "xml");
					String fileName = "ZFJ-Executions-" + URLEncoder.encode(df.format(new Date()), "UTF-8") + ".xml";
					file = createAttachment(so, fileName);
					return buildExportResponse(file);
				} else if (StringUtils.equalsIgnoreCase(exportType, "csv")) {
					final StreamingOutput so = exportService.generateCSV(schedules);
					String fileName = "ZFJ-Executions-" + URLEncoder.encode(df.format(new Date()), "UTF-8") + ".csv";
					file = createAttachment(so, fileName);
					return buildExportResponse(file);
				} else if (StringUtils.equalsIgnoreCase(exportType, "htm")) {
					final StreamingOutput so = exportService.generate(schedules, "htm");
					String fileName = "ZFJ-Executions-" + URLEncoder.encode(df.format(new Date()), "UTF-8") + ".html";
					file = createAttachment(so, fileName);
					return buildExportResponse(file);
				} else if (StringUtils.equalsIgnoreCase(exportType, "xls")) {
					final StreamingOutput so = exportService.generate(schedules, "xls");
					String fileName = "ZFJ-Executions-" + URLEncoder.encode(df.format(new Date()), "UTF-8") + ".xls";
					file = createAttachment(so, fileName);
					return buildExportResponse(file);
				} else {
					JSONObject error = new JSONObject();
					error.put("error", ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.retrieve.error"));
                    log.error("[Error] [Error code:"+ Status.BAD_REQUEST.getStatusCode()+" "+ Status.BAD_REQUEST+" Error Message :"+ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.retrieve.error"));
					return Response.status(Status.BAD_REQUEST).entity(error.toString()).build();
				}
			}
		} catch (SearchException e) {
			log.debug("SearchException=", e);
		} catch (JSONException e) {
			e.printStackTrace();
			log.debug("JSONException=", e);
		} catch (Exception e) {
			e.printStackTrace();
			log.debug("Exception=", e);
		}
		return Response.status(Status.BAD_REQUEST).entity(ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.common.retrieve.error")).build();
	}

	public JSONObject parseQueryAndReturnJSON(ParseResult parseResult) {
		String errorMessage;
		errorMessage = StringUtils.join(parseResult.getErrors().getErrorMessages(), "\n");
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject = new JSONObject();
			String updatedErrMsg = StringUtils.replace(errorMessage, "JQL", "ZQL");// Hack to replace all JQL String thrown from JQLParser
			jsonObject.put("error", updatedErrMsg);
			jsonObject.put("errorDescHtml", updatedErrMsg);
			return jsonObject;
		} catch (JSONException e) {
			return null;
		}
	}

	public Response parseQuery(ParseResult parseResult) {
		String errorMessage;
		errorMessage = StringUtils.join(parseResult.getErrors().getErrorMessages(), "\n");
		JSONObject jsonObject = new JSONObject();
		try {
			ResponseBuilder builder = Response.status(Response.Status.NOT_ACCEPTABLE);
			builder.type(MediaType.APPLICATION_JSON);
			jsonObject = new JSONObject();
			String updatedErrMsg = StringUtils.replace(errorMessage, "JQL", "ZQL");// Hack to replace all JQL String thrown from JQLParser
			jsonObject.put("error", updatedErrMsg);
			jsonObject.put("errorDescHtml", updatedErrMsg);
			builder.entity(jsonObject.toString());
			log.error("[Error] [Error code:" + Status.NOT_ACCEPTABLE.getStatusCode() + " " + Status.NOT_ACCEPTABLE + " Error Message :" + updatedErrMsg);
			return builder.build();
		} catch (JSONException e) {
			return getDefaultJSONError(jsonObject);
		}
	}

	private Response buildExportResponse(File file) {
		if (file != null) {
			JSONObject ob = new JSONObject();
			try {
				ApplicationProperties applicationProperties = (ApplicationProperties) ZephyrComponentAccessor.getInstance().getComponent("applicationProperties");
				String fileUrl = applicationProperties.getBaseUrl() + "/plugins/servlet/export/exportAttachment?fileName=" + file.getName();
				ob.put("url", fileUrl);
				return Response.ok(ob.toString()).build();
			} catch (JSONException e) {
				log.warn("Error exporting file", e);
				return Response.status(Status.SERVICE_UNAVAILABLE).build();
			}
		} else {
			log.error("[Error] [Error code:" + Status.SERVICE_UNAVAILABLE.getStatusCode() + " " + Status.SERVICE_UNAVAILABLE + " Error Message : Service unavailable to export.");
			return Response.status(Status.SERVICE_UNAVAILABLE).build();
		}
	}

	private File createAttachment(final StreamingOutput so, String fileName) {
		final File tmpDir = AttachmentUtils.getTemporaryAttachmentDirectory();
		File tempAttachmentFile = new File(tmpDir, fileName);
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(tempAttachmentFile);
			so.write(out);
		} catch (FileNotFoundException e) {
			log.error("createAttachment() : FileNotFoundException", e);
		} catch (WebApplicationException e) {
			log.error("createAttachment() : WebApplicationException", e);
		} catch (IOException e) {
			log.error("createAttachment() : IOException", e);
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
		return tempAttachmentFile;
	}

	/**
     * If a User goes n clicking after 5, we need to increment one link forward and decrement one link from backward. i.e. when a 
     * user clicks on 5, the ending link will be 10 and start will be 2
	 * @param searchResults
	 * @param currentIndex
	 * @param maxResultHit
	 */
	private List<Integer> generatePageNumbers(SearchResult searchResults, int currentIndex, Integer maxResultHit) {
		List<Integer> pageList = new ArrayList<Integer>();
		int pageNumber = 1;
		int endIndex = Math.round(searchResults.getTotal() / maxResultHit);
		int iterateIndex = 0;
        //If currentIndex >= 5 and less than endIndex + 4, than we increment by 4 . At all point, we keep the pages total displayed as 5
		if (currentIndex >= ALLOWED_MAX_PAGE_NUMBERS) { // 1
			if (endIndex >= (currentIndex + 2)) {
				pageNumber = currentIndex - 2;
			} else {
				if (searchResults.getTotal() % maxResultHit != 0) {
					pageNumber = (endIndex + 1) - ALLOWED_MAX_PAGE_NUMBERS + 1;
				} else {
					pageNumber = (endIndex + 1) - ALLOWED_MAX_PAGE_NUMBERS;
				}
			}
		}
        //If the Total/Max hit is not equal to 0, we will just add one to it as Round will round it off to previous value
		if (searchResults.getTotal() % maxResultHit != 0) {
			// If End Index >= 5, than we need to only show 5 links
			if (endIndex >= 5) {
				endIndex = 4;
			}
			iterateIndex = endIndex + 1;
		} else {
			// If End Index >= 5, than we need to only show 5 links
			if (endIndex >= 5) {
				endIndex = 5;
			}
			iterateIndex = endIndex;
		}
		for (int index = 0; index < iterateIndex; index++) {
			pageList.add(pageNumber++);
		}
		return pageList;
	}

	private void runJql(String foo) {
		while (StringUtils.indexOf(foo, "jqlName") != -1) {
			int indexStart = StringUtils.indexOf(foo, "(jqlName");
			String actualString = StringUtils.substringBefore(StringUtils.substring(foo, indexStart + 8), ")").trim();
			String me = StringUtils.substring(foo, 0, indexStart + 1);
			String me1 = StringUtils.substringAfter(foo, actualString);
			foo = me + me1;
			// executeJQL(actualString,me) ;
		}
	}

	public JSONObject getClauses() {
		Collection<SearchHandler> searchHandlers = zephyrClauseHandlerFactory.getZQLClauseSearchHandlers();
		Collection<SearchHandler> customSearchHandlers = zephyrClauseHandlerFactory.getZQLCustomClauseSearchHandlers();
		List<String> jqlClauseNames = new ArrayList<String>();
		JSONObject jsonObject = new JSONObject();
		for (SearchHandler searchHandler : searchHandlers) {
			List<ClauseRegistration> clauseRegistrations = searchHandler.getClauseRegistrations();
			for (ClauseRegistration clauseRegistration : clauseRegistrations) {
				jqlClauseNames.add(clauseRegistration.getHandler().getInformation().getJqlClauseNames().getPrimaryName());
			}
		}

		if(customSearchHandlers != null) {
			for (SearchHandler searchHandler : customSearchHandlers) {
				List<ClauseRegistration> clauseRegistrations = searchHandler.getClauseRegistrations();
				for (ClauseRegistration clauseRegistration : clauseRegistrations) {
					jqlClauseNames.add(clauseRegistration.getHandler().getInformation().getJqlClauseNames().getPrimaryName());
				}
			}
		}

		try {
			jsonObject.put("clauses", jqlClauseNames);
		} catch (JSONException e) {
			log.error("Error Creating json object", e);
			return null;
		}
		return jsonObject;
	}

	public Response performZQLSearch(String zqlQuery, Integer startIndex,
			Integer maxRecords, String expand, ScheduleSearchResourceHelper searchResourceHelper,List<Issue> issues) {
		ParseResult parseResult = searchResourceHelper.getNewSearchQuery(zqlQuery);
		folderManager = (FolderManager) ZephyrComponentAccessor.getInstance().getComponent("folder-manager");
		Query currentQuery = null;
		if (parseResult != null && parseResult.isValid()) 
        {
			currentQuery = parseResult.getQuery();
		} else {
			if (parseResult.getErrors() != null && !parseResult.getErrors().getErrorMessages().isEmpty()) {
				return parseQuery(parseResult);
			}
		}
		log.debug("ZQL search Query=" + currentQuery);
		// validates Atlassian query object and returns JSONObject with the error if any
		JSONObject jsonObject = searchResourceHelper.validateQuery(currentQuery);
		try {
			if (jsonObject != null) {
				log.error("[Error] [Error code:" + Status.NOT_ACCEPTABLE.getStatusCode() + " " + Status.NOT_ACCEPTABLE + " Error Message :" + jsonObject.toString());
				return getDefaultJSONError(jsonObject);
			} else {
				int start = startIndex != null ? startIndex : 0;
	    		// Converts Atlassian Query object to Lucene Query and searches the Index. This is the meat of the call.
				boolean overrideSecurity = false;
				SearchResult searchResults = searchService.search(user, currentQuery, start, false, maxRecords, overrideSecurity);
				if (searchResults != null && (searchResults.getDocuments() == null || searchResults.getDocuments().size() == 0)) {
					ZQLSearchResultBean resultBean = new ZQLSearchResultBean();
					List<ZQLScheduleBean> schedules = new ArrayList<ZQLScheduleBean>();
					resultBean.setExecutions(schedules);
					return Response.ok(resultBean).cacheControl(ZephyrCacheControl.never()).build();
				}
				// Convert Lucene document to JSON response
				ZQLSearchResultBean resultBean = convertLuceneDocumentToJson(searchResults, startIndex, maxRecords, expand);
				if (null != issues) {
					Map<String, Object> execSummary = getExecutionSummaryWithDefectCount(issues);
					resultBean.setTotalExecutions((Integer) execSummary.get("totalExecutions"));
					resultBean.setTotalExecuted((Integer) execSummary.get("totalExecuted"));
					resultBean.setTotalDefectCount((Integer) execSummary.get("totalDefectCount"));
					resultBean.setTotalOpenDefectCount((Integer) execSummary.get("totalOpenDefectCount"));
					JSONObject object = (JSONObject) execSummary.get("executionSummaries");
					resultBean.setExecutionSummaries(null != object ? object.toString() : StringUtils.EMPTY);
				}

				// separete out metadata
				return Response.ok().entity(resultBean).cacheControl(ZephyrCacheControl.never()).build();

			}
		} catch (SearchException e) {
			log.error("Error Searching ZQL:", e);
		} catch (JSONException e) {
			log.error("Error Creating JSONResponse for ZQL:", e);
		} catch (Exception e) {
			log.error("Error executing for ZQL:", e);
		}
		log.error("Error Searching ZQL.");
		return Response.serverError().build();
	}

	public ZQLSearchResultBean performRawZQLSearch(String zqlQuery, Integer startIndex,
			Integer maxRecords, ScheduleSearchResourceHelper searchResourceHelper) {
		ParseResult parseResult = searchResourceHelper.getNewSearchQuery(zqlQuery);
		folderManager = (FolderManager) ZephyrComponentAccessor.getInstance().getComponent("folder-manager");
		Query currentQuery = null;
		if (parseResult != null && parseResult.isValid())
        {
			currentQuery = parseResult.getQuery();
		} else {
			if (parseResult.getErrors() != null && !parseResult.getErrors().getErrorMessages().isEmpty()) {
				JSONObject jsonObject = parseQueryAndReturnJSON(parseResult);
				if (jsonObject == null) {
					return null;
				}
			}
		}
		log.debug("ZQL query to perform search: " + currentQuery);
		// validates Atlassian query object and returns JSONObject with the error if any
		JSONObject jsonObject = searchResourceHelper.validateQuery(currentQuery);
		try {
			if (jsonObject != null) {
				return null;
			} else {
				int start = startIndex != null ? startIndex : 0;
	    		// Converts Atlassian Query object to Lucene Query and searches the Index. This is the meat of the call.
				boolean overrideSecurity = false;
				SearchResult searchResults = searchService.search(user, currentQuery, start, false, maxRecords, overrideSecurity);
				if (searchResults != null && (searchResults.getDocuments() == null || searchResults.getDocuments().size() == 0)) {
					ZQLSearchResultBean resultBean = new ZQLSearchResultBean();
					List<ZQLScheduleBean> schedules = new ArrayList<ZQLScheduleBean>();
					resultBean.setExecutions(schedules);
					return resultBean;
				}
				// Convert Lucene document to JSON response
				ZQLSearchResultBean resultBean = convertLuceneDocumentToJson(searchResults, startIndex, maxRecords, null);
				// separete out metadata
				return resultBean;
			}
		} catch (SearchException e) {
			log.error("Error Searching ZQL:", e);
		} catch (JSONException e) {
			log.error("Error Creating JSONResponse for ZQL:", e);
		} catch (Exception e) {
			log.error("Error executing for ZQL:", e);
		}
		return new ZQLSearchResultBean();
	}

	/**
	 * Gets the StepDefect Count for a given schedule query
	 * @param zqlQuery
	 * @return
	 */
	public Map<String, Integer> getStepDefectCountBySchedule(String zqlQuery) {
		Map<String, Integer> defectCounts = new HashMap<String, Integer>();
		defectCounts.put("executionDefectCount", 0);
		defectCounts.put("stepDefectCount", 0);
		defectCounts.put("totalDefectCount", 0);

		ParseResult parseResult = getNewSearchQuery(zqlQuery);
		Query currentQuery = null;
		if (parseResult != null && parseResult.isValid()) {
			currentQuery = parseResult.getQuery();
		}
		log.debug("Query to getStepDefectCountBySchedule: " + currentQuery);
		// validates Atlassian query object and returns JSONObject with the error if any
		JSONObject jsonObject = validateQuery(currentQuery);
		try {
			if (jsonObject != null) {
				return defectCounts;
			} else {
				SearchResult searchResults = searchService.search(user, currentQuery, 0, false, null, false);
				if (searchResults != null && searchResults.getDocuments() != null && searchResults.getDocuments().size() > 0) {
					for (Document document : searchResults.getDocuments()) {
						Integer executionDefectCount = Integer.valueOf(document.get("SCHEDULE_DEFECT_COUNT"));
						Integer stepDefectCount = Integer.valueOf(document.get("STEP_DEFECT_COUNT"));
						Integer totalLinkedDefectCount = Integer.valueOf(document.get("TOTAL_DEFECT_COUNT"));

						defectCounts.put("executionDefectCount", executionDefectCount);
						defectCounts.put("stepDefectCount", stepDefectCount);
						defectCounts.put("totalDefectCount", totalLinkedDefectCount);
					}
				}
			}
		} catch (SearchException e) {
			log.error("Error Searching ZQL:", e);
		} catch (JSONException e) {
			log.error("Error Creating JSONResponse for ZQL:", e);
		} catch (Exception e) {
			log.error("Error executing for ZQL:", e);
		}
		return defectCounts;
	}

	private void maskIssueFields(ZQLScheduleBean schedule, Issue issue, Document scheduleDocument, String expand) {
		I18nHelper i18nHelper = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
		String comment = scheduleDocument.get("COMMENT");
		if (comment != null && !StringUtils.equalsIgnoreCase(comment, "-1")) {
			schedule.setComment(ApplicationConstants.MASKED_DATA);
			schedule.setHtmlComment(TextUtils.plainTextToHtml(ApplicationConstants.MASKED_DATA, "_blank", true));
		} else {
			schedule.setComment("");
			schedule.setHtmlComment("");
		}
		String label = scheduleDocument.get("LABEL");
		List<String> labels = new ArrayList<>();
		if (null != label && !StringUtils.equalsIgnoreCase(label, "-1")) {
			String[] labelsArray = new String[1];
			labelsArray[0] = ApplicationConstants.MASKED_DATA;
			labels = Arrays.asList(labelsArray);
		}
		schedule.setIssueDescription(ComponentAccessor.getRendererManager().
				getRenderedContent(AtlassianWikiRenderer.RENDERER_TYPE, ApplicationConstants.MASKED_DATA , issue.getIssueRenderContext()));
		schedule.setLabels(labels);
		Long versionId = new Long(-1);
		if (scheduleDocument.get("VERSION_ID") != null) {
			if (!StringUtils.equalsIgnoreCase(scheduleDocument.get("VERSION_ID"), "-1")) {
				Version version = versionManager.getVersion(Long.valueOf(scheduleDocument.get("VERSION_ID")));
				if (version != null) {
					versionId = version.getId();
				}
			}
		}
		List<Map<String, Object>> components = new ArrayList<Map<String, Object>>();
		for (ProjectComponent projectComponent : issue.getComponentObjects()) {
			Map<String, Object> componentMap = new HashMap<String, Object>();
			componentMap.put("id", projectComponent.getId());
			componentMap.put("name", ApplicationConstants.MASKED_DATA);
			components.add(componentMap);
		}
		schedule.setIssueKey(ApplicationConstants.MASKED_DATA);
		schedule.setIssueSummary(ApplicationConstants.MASKED_DATA);
		schedule.setPriority(ApplicationConstants.MASKED_DATA);
		schedule.setComponents(components);
		schedule.setVersionId(versionId);
		schedule.setVersionName(ApplicationConstants.MASKED_DATA);
		schedule.setVersionId(versionId);
		schedule.setProjectKey(ApplicationConstants.MASKED_DATA);
		schedule.setProject(ApplicationConstants.MASKED_DATA);
		schedule.setCanViewIssue(false);
		String executedBy = ApplicationConstants.MASKED_DATA;
		String executedByUserName = ApplicationConstants.MASKED_DATA;
		String executedByDisplay = ApplicationConstants.MASKED_DATA;
		// String executionStatus = scheduleDocument.get("STATUS");
		String executionStatus = scheduleDocument.get("STATUS");
		//ExecutionStatus execStatus = JiraUtil.getExecutionStatuses().get(new Integer(executionStatus));

		Integer cycleId = Integer.valueOf(scheduleDocument.get("CYCLE_ID"));
		schedule.setCycleName(ApplicationConstants.MASKED_DATA);
		schedule.setCycleId(cycleId);
		String scheduleId = scheduleDocument.get("schedule_id");
		String orderId = scheduleDocument.get("ORDER_ID");

		schedule.setId(StringUtils.isNotEmpty(scheduleId) ? Integer.parseInt(scheduleId) : 0);
		schedule.setOrderId(StringUtils.isNotEmpty(orderId) ? Integer.valueOf(orderId) : 0);
		// schedule.setStatus(execStatus);
		schedule.setStatusId(new Integer(executionStatus));
		schedule.setExecutedBy(executedBy);
		schedule.setExecutedByUserName(executedByUserName);
		schedule.setExecutedByDisplay(executedByDisplay);
		schedule.setCreationDate(ApplicationConstants.MASKED_DATA);
		schedule.setProjectId(issue.getProjectObject().getId());
		schedule.setProjectAvatarId(issue.getProjectObject().getAvatar().getId());
		schedule.setExecutedOn(ApplicationConstants.MASKED_DATA);
		String assigneeUserName = ApplicationConstants.MASKED_DATA;
		// String assigneeDisplay = ApplicationConstants.MASKED_DATA;
		schedule.setAssignee(ApplicationConstants.MASKED_DATA);
		// schedule.setAssigneeDisplay(assigneeDisplay);
		schedule.setAssigneeUserName(assigneeUserName);

		String assignedTo = scheduleDocument.get("ASSIGNED_TO");
		String assigneeDisplay = "";
		if (assignedTo == null || StringUtils.equalsIgnoreCase(assignedTo, "-1")) {
			assignedTo = "";
		} else {
			User user = UserCompatibilityHelper.getUserForKey(assignedTo);
			assignedTo = user != null ? user.getDisplayName() : assignedTo;
			assigneeDisplay = (user != null && user.isActive()) ? assignedTo : assignedTo + " (Inactive)";
		}
		schedule.setAssigneeDisplay(assigneeDisplay);
		// Below code sets the ExecutionDefect/StepDefect and counts.
		String[] executionDefectKeys = scheduleDocument.getValues("SCHEDULE_DEFECT_KEY");
		if (executionDefectKeys != null && executionDefectKeys.length > 0) {
			List<ExecutionDefectBean> executionDefects = new ArrayList<ExecutionDefectBean>();
			for (String defectKey : executionDefectKeys) {
				ExecutionDefectBean executionDefect = IssueUtils.maskIssueToExecutionDefect(defectKey);
				if (executionDefect != null)
					executionDefects.add(executionDefect);
			}
			schedule.setExecutionDefects(executionDefects);
		} else {
			schedule.setExecutionDefects(new ArrayList<ExecutionDefectBean>(0));
		}

		String[] stepDefectKeys = scheduleDocument.getValues("STEP_DEFECT_KEY");
		if (stepDefectKeys != null && stepDefectKeys.length > 0) {
			List<String> maskedKeys = new ArrayList<>();
			for (String stepDefectKey : stepDefectKeys) {
				maskedKeys.add(ApplicationConstants.MASKED_DATA);
			}
			schedule.setStepDefects(maskedKeys);
		} else {
			schedule.setStepDefects(new ArrayList<String>(0));
		}
		schedule.setExecutionDefectCount(ZCollectionUtils.getAsOptionalInteger(scheduleDocument.get("SCHEDULE_DEFECT_COUNT")).or(0));
		schedule.setStepDefectCount(ZCollectionUtils.getAsOptionalInteger(scheduleDocument.get("STEP_DEFECT_COUNT")).or(0));
		schedule.setTotalDefectCount(ZCollectionUtils.getAsOptionalInteger(scheduleDocument.get("TOTAL_DEFECT_COUNT")).or(0));

		if (expand != null) {
			String[] expandStr = expand.split(",");
			for (String expandStatus : expandStr) {
				if(StringUtils.isNotBlank(expandStatus) &&
						StringUtils.containsIgnoreCase(expandStatus, "teststeps")) {
					final List<Teststep> values = testStepManager.getTeststeps(issue.getId(), Optional.empty(), Optional.empty());
					if ((null != values && !values.isEmpty())) {
						List<TeststepBean> result = new ArrayList<>();
						for (Iterator<Teststep> it = values.iterator(); it.hasNext();) {
							Teststep teststepObj = it.next();
							if (teststepObj == null) {
								continue;
							}
							TeststepBean stepBean = new TeststepBean(teststepObj.getID(), teststepObj.getOrderId(), ApplicationConstants.MASKED_DATA,
									ApplicationConstants.MASKED_DATA,ApplicationConstants.MASKED_DATA,ApplicationConstants.MASKED_DATA,
									ApplicationConstants.MASKED_DATA, true);
							/**
							 * Convert the wiki markup to text for export
							 */
							stepBean.step = ApplicationConstants.MASKED_DATA;
							stepBean.data = ApplicationConstants.MASKED_DATA;
							stepBean.result = ApplicationConstants.MASKED_DATA;
							stepBean.stepComment = ApplicationConstants.MASKED_DATA;
							result.add(stepBean);
						}
						schedule.setTestStepBean(result);
					}

				}
			}
		}
	}

	public Response getDefaultJSONError(JSONObject jsonObject) {
		ResponseBuilder builder = Response.status(Response.Status.NOT_ACCEPTABLE);
		builder.type(MediaType.APPLICATION_JSON);
		builder.entity(jsonObject.toString());
		return builder.build();
	}

	private Map<String, Object> getExecutionSummaryWithDefectCount(List<Issue> issues) throws Exception {
		int totalExecuted = 0;
		int totalExecutions = 0;
		List<Long> issueIds = CollectionUtil.transform(issues, new Function<Issue, Long>() {
			@Override
			public Long get(final Issue issue) {
				return issue.getId();
			}
		});
		Map<String, Object> cntByIssueIdAndStatus = new HashMap<>();
		if (issueIds != null) {
			List<ExecutionSummaryImpl> allSummaries = scheduleIndexManager.getExecutionSummariesByIssueIds(issueIds);
			JSONObject jsonObject = new JSONObject();
			JSONArray expectedOperators = new JSONArray();
			Map<String, Object> test = new HashMap<>();
			if (allSummaries != null) {
				for (ExecutionSummaryImpl executionSummary : allSummaries) {
					if (executionSummary.getExecutionStatusKey().intValue() != -1 &&
							!StringUtils.equalsIgnoreCase(executionSummary.getExecutionStatusName(), "Unexecuted")) {
						totalExecuted += executionSummary.getCount();
					}
					totalExecutions += executionSummary.getCount();
					expectedOperators.put(ScheduleResourceHelper.executionSummaryToJSON(executionSummary));
				}
			}
			jsonObject.put("executionSummary", expectedOperators);
			test.put("executionSummaries", jsonObject);
			cntByIssueIdAndStatus.put("executionSummaries", jsonObject);
			cntByIssueIdAndStatus.put("execsummary", test);
		}
		cntByIssueIdAndStatus.put("totalExecutions", totalExecutions);
		cntByIssueIdAndStatus.put("totalExecuted", totalExecuted);
		int totalDefectCount = 0;
		int totalOpenDefectCount = 0;
		int totalResolvedDefectCount = 0;
		String defaultMax = ComponentAccessor.getApplicationProperties().getDefaultBackedString("jira.search.stable.max.results");
		// For version below 6.0 will be null
		if (StringUtils.isBlank(defaultMax)) {
			defaultMax = ComponentAccessor.getApplicationProperties().getDefaultBackedString(APKeys.JIRA_SEARCH_VIEWS_DEFAULT_MAX);
		}
		Integer allowedRecordLimit = defaultMax != null ? Integer.valueOf(defaultMax.trim()) : MAX_RECORDS_ALLOWED;
		if (!issueIds.isEmpty()) {
			String zqlQuery = "ISSUE IN (" + StringUtils.join(issueIds, ",") + ")";
            ScheduleSearchResourceHelper searchResourceHelper = new ScheduleSearchResourceHelper(authContext.getLoggedInUser(),
                    searchService,zephyrClauseHandlerFactory,issueManager,cycleManager,versionManager, zephyrCustomFieldManager);
			ZQLSearchResultBean zqlSearchResultBean = searchResourceHelper.performRawZQLSearch(zqlQuery, 0, allowedRecordLimit, searchResourceHelper);
			List<ZQLScheduleBean> zqlScheduleBeans = zqlSearchResultBean.getExecutions();
			Map<String, ExecutionDefectBean> uniqueDefectBean = new HashMap<String, ExecutionDefectBean>();
			for (ZQLScheduleBean zqlScheduleBean : zqlScheduleBeans) {
                if(zqlScheduleBean.getTestDefectsUnMasked() != null
                        && zqlScheduleBean.getTestDefectsUnMasked().size() > 0) {
					for (String testDefect : zqlScheduleBean.getTestDefectsUnMasked()) {
						ExecutionDefectBean testDefectBean = getConvertedExecutionDefectBean(testDefect);
						if (!uniqueDefectBean.containsKey(testDefectBean.getDefectKey())) {
							uniqueDefectBean.put(testDefectBean.getDefectKey(), testDefectBean);
							if (StringUtils.isNotBlank(testDefectBean.getDefectResolutionId())) {
								totalResolvedDefectCount += 1;
							}
						}
					}
				}

				// Now for Step Defect
                if(zqlScheduleBean.getStepDefectsUnMasked() != null
                        && zqlScheduleBean.getStepDefectsUnMasked().size() > 0) {
					for (String stepDefect : zqlScheduleBean.getStepDefectsUnMasked()) {
						ExecutionDefectBean stepDefectBean = getConvertedExecutionDefectBean(stepDefect);
						if (!uniqueDefectBean.containsKey(stepDefectBean.getDefectKey())) {
							uniqueDefectBean.put(stepDefectBean.getDefectKey(), stepDefectBean);
							if (StringUtils.isNotBlank(stepDefectBean.getDefectResolutionId())) {
								totalResolvedDefectCount += 1;
							}
						}
					}
				}
			}
			totalOpenDefectCount = uniqueDefectBean.size() > 0 ? uniqueDefectBean.size() - totalResolvedDefectCount : 0;
			totalDefectCount = uniqueDefectBean.size();
		}
		cntByIssueIdAndStatus.put("totalDefectCount", totalDefectCount);
		cntByIssueIdAndStatus.put("totalOpenDefectCount", totalOpenDefectCount);
		return cntByIssueIdAndStatus;
	}

	/**
	 * Get ConvertedTestExecutionDefectBean
	 * @param defectKey
	 * @return
	 */
	private ExecutionDefectBean getConvertedExecutionDefectBean(String defectKey) {
		ExecutionDefectBean executionDefect = new ExecutionDefectBean();
		Issue defectIssue = ComponentAccessor.getIssueManager().getIssueObject(defectKey);
		if (null != defectIssue) {
			executionDefect.setDefectId(defectIssue.getId().intValue());
			executionDefect.setDefectKey(defectKey);
			executionDefect.setDefectStatus(defectIssue.getStatus().getNameTranslation());
			executionDefect.setDefectSummary(defectIssue.getSummary());
			if (defectIssue.getResolutionId() != null)
				executionDefect.setDefectResolutionId(defectIssue.getResolution().getName());
			else
				executionDefect.setDefectResolutionId("");
		}
		return executionDefect;
	}

    /**
     * Get custom fields data from lucene document.
     * @param scheduleId
     * @param scheduleDocument
     * @return
     */
    private Map<String,String> getCustomFieldValuesFromScheduleDocument(Integer scheduleId, Document scheduleDocument) {

        CustomFieldValueManager customFieldValueManager = (CustomFieldValueManager) ZephyrComponentAccessor.getInstance().getComponent("customfield-value-manager");
        DateTimeFormatterFactory dateTimeFormatterFactory = ComponentAccessor.getComponent(DateTimeFormatterFactory.class);
        DateTimeFormatter dateTimePickerFormatter = dateTimeFormatterFactory.formatter().forLoggedInUser().withStyle(DateTimeStyle.DATE_TIME_PICKER);

        List<ExecutionCf> executionCfs = customFieldValueManager.getCustomFieldValuesForExecution(scheduleId);
        if(CollectionUtils.isNotEmpty(executionCfs)) {
            Map<String,String> customFieldValuesMap = new HashMap<>();
            List<CustomField> customFields = executionCfs.stream().map(ExecutionCf::getCustomField).collect(Collectors.toList());
			CustomFieldProject[] allActiveCustomFieldProjects = zephyrCustomFieldManager.getAllActiveCustomFieldsProject();
			Map<Integer,List<Long>> allActiveCustomFieldsProject = new LinkedHashMap<>();
			if(allActiveCustomFieldProjects != null) {
				allActiveCustomFieldsProject = Arrays.stream(allActiveCustomFieldProjects).collect(Collectors.groupingBy(CustomFieldProject::getCustomFieldId,
								Collectors.mapping(f -> f.getProjectId(),
										Collectors.toList())));
			}
			Map<Integer, List<Long>> finalAllActiveCustomFieldsProject = allActiveCustomFieldsProject;
			allActiveCustomFieldsProject = null;
			customFields.stream().forEach(customField -> {
				if(finalAllActiveCustomFieldsProject.get(customField.getID()) != null &&
						finalAllActiveCustomFieldsProject.get(customField.getID()).contains(Long.valueOf(scheduleDocument.get("PROJECT_ID")))) {
					String indexedValue = StringUtils.EMPTY;
					String customFieldType = ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customField.getCustomFieldType());
					String luceneValue = scheduleDocument.get(String.valueOf(customField.getID()));
					if (StringUtils.isNotBlank(customFieldType) && customFieldType.equalsIgnoreCase(ApplicationConstants.DATE_VALUE)) {
						if (StringUtils.isNotBlank(luceneValue) && !StringUtils.equalsIgnoreCase(luceneValue, ApplicationConstants.NULL_VALUE)) {
							LocalDate customFieldDate = LuceneUtils.stringToLocalDate(luceneValue);
							JqlLocalDateSupport jqlLocalDateSupport = ComponentAccessor.getComponentOfType(JqlLocalDateSupport.class);
							Date convertedDate = jqlLocalDateSupport.convertToDate(customFieldDate);
							DateTimeFormatter df = dateTimeFormatterFactory.formatter().forLoggedInUser().withStyle(DateTimeStyle.DATE_PICKER);
							indexedValue = df.format(convertedDate);
						}
					} else if (StringUtils.isNotBlank(customFieldType) && customFieldType.equalsIgnoreCase(ApplicationConstants.DATE_TIME_VALUE)) {
						if (StringUtils.isNotBlank(luceneValue) && !StringUtils.equalsIgnoreCase(luceneValue, ApplicationConstants.NULL_VALUE)) {
							Date customFieldDate = LuceneUtils.stringToDate(luceneValue);
							indexedValue = dateTimePickerFormatter.format(customFieldDate);
						}
					} else if (StringUtils.isNotBlank(customFieldType) && customFieldType.equalsIgnoreCase(ApplicationConstants.NUMBER_VALUE)) {
						DoubleConverter doubleConverter = new DoubleConverterImpl(authContext);
						if (StringUtils.isNotBlank(luceneValue) && !StringUtils.equalsIgnoreCase(luceneValue, ApplicationConstants.NULL_VALUE)) {
							final String doubleValue = doubleConverter.getStringForChangelog(Double.valueOf(luceneValue));
							indexedValue = doubleValue;
						} else {
							indexedValue = StringUtils.EMPTY;
						}
					} else if (StringUtils.isNotBlank(customFieldType) && customFieldType.equalsIgnoreCase(ApplicationConstants.LIST_VALUE)) {
						if (StringUtils.isNotBlank(luceneValue) && !StringUtils.equalsIgnoreCase(luceneValue, ApplicationConstants.NULL_VALUE) &&
								!(StringUtils.equalsIgnoreCase(customField.getCustomFieldType(), ApplicationConstants.MULTI_SELECT) ||
										StringUtils.equalsIgnoreCase(customField.getCustomFieldType(), ApplicationConstants.CHECKBOX))) {
							String[] optionValues = StringUtils.split(luceneValue, ",");
							List<String> customFieldOptionList = new ArrayList<>();
							indexedValue = retrieveCustomValues(indexedValue, optionValues, customFieldOptionList);
						} else {
							String[] luceneValues = scheduleDocument.getValues(String.valueOf(customField.getID()));
							if (luceneValues != null && luceneValues.length > 0) {
								Arrays.asList(luceneValues).sort(ListValueComparator.INSTANCE);
								List<String> customFieldOptionList = new ArrayList<>();
								indexedValue = retrieveCustomValues(indexedValue, luceneValues, customFieldOptionList);
							}
						}
					} else {
						if (StringUtils.isNotBlank(luceneValue) && !StringUtils.equalsIgnoreCase(luceneValue, ApplicationConstants.NULL_VALUE)) {
							indexedValue = TextUtils.plainTextToHtml(luceneValue);
						}
					}
					customFieldValuesMap.put(customField.getID() + StringUtils.EMPTY, StringUtils.isNotBlank(indexedValue) ? indexedValue : "-");
				}
            });
            return customFieldValuesMap;
        }
        return MapUtils.EMPTY_MAP;
    }

	private String retrieveCustomValues(String indexedValue, String[] optionValues, List<String> customFieldOptionList) {
    	try {
			if (optionValues != null && optionValues.length > 0) {
				List<String> optionList = Arrays.asList(optionValues);
				optionList.stream().forEach(customFieldOptionId -> {
					if(!StringUtils.equalsIgnoreCase(customFieldOptionId,ApplicationConstants.NULL_VALUE)) {
						CustomFieldOption customFieldOption = zephyrCustomFieldManager.getCustomFieldOptionById(Integer.valueOf(customFieldOptionId));
						if(customFieldOption != null) {
							customFieldOptionList.add(customFieldOption.getOptionValue());
						}
					}
				});
				indexedValue = StringUtils.join(customFieldOptionList, "," + "\n");
			}
		} catch(Exception e) {
    		log.error("Error retrieving Custom Fields");
		}
		return indexedValue;
	}
}
