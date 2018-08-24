package com.thed.zephyr.je.service.impl;


import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.IssueService.IssueResult;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.issue.AttachmentManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.attachment.CreateAttachmentParamsBean;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONObject;
import com.google.gson.Gson;
import com.thed.zephyr.je.service.*;
import com.thed.zephyr.je.vo.*;
import com.thed.zephyr.je.vo.ImportFile.ColumnMetadata;
import com.thed.zephyr.je.vo.TestCase.Response;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.ExcelFileUtil;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class IssueImporterServiceImpl implements IssueImporterService {

	private static final Logger log = LoggerFactory.getLogger(IssueImporterServiceImpl.class);

	private final ExcelFileValidationService validationService;
	private final TestcaseManager testCaseManager;
	private final TeststepManager testStepManager;
	private final IssueService issueService;
	private final IssueManager issueManager;
	private final AttachmentManager attachmentManager;
	private final ProjectService projectService;
	private final JiraAuthenticationContext authContext;
	private final JobProgressService jobProgressService;


	public IssueImporterServiceImpl(ExcelFileValidationService validationService, IssueService issueService,
			ProjectService projectService, JiraAuthenticationContext authContext,
			TestcaseManager testCaseManager, TeststepManager testStepManager, IssueManager issueManager,
			AttachmentManager attachmentManager, JobProgressService jobProgressService) {
		this.validationService = validationService;
		this.issueService = issueService;
		this.projectService = projectService;
		this.authContext = authContext;
		this.testCaseManager = testCaseManager;
		this.testStepManager = testStepManager;
		this.issueManager = issueManager;
		this.attachmentManager = attachmentManager;
		this.jobProgressService = jobProgressService;
	}

	public boolean importFile(final JiraAuthenticationContext authContext, final JobProgressService jobProgressService,
							  final String jobProgressToken,
							  final ImportJob importJob, File file,
							  ApplicationUser user,
							  final List<Sheet> sheets
							  ) throws Exception {

		boolean isSuccess = false;
		JSONObject json = new JSONObject();

		List<ImportJobStatus> jobStatusList = new ArrayList<ImportJobStatus>();
		ObjectMapper objMapper = new ObjectMapper();
		try {
				ExecutorService importJobProcessPool  = Executors.newFixedThreadPool(ApplicationConstants.IMPORT_JOB_THREAD_POOL_SIZE);
				List<Future<ImportJobStatus>> importJobProcessResultList = new ArrayList<Future<ImportJobStatus>>();
				importJob.setJobProgressKey(jobProgressToken);
					boolean isValidationFailed = false;
					if(importJob.isFileTypeExcel()) {
						Map<String, String> fileMapping = getExcelElementsForMapping(sheets);
						//Replace names with excel column reference like A, B, C etc.
						ImportDetails importDetails = importJob.getImportDetails();
						Set<ImportFieldMapping> mappingSet = importDetails.getFieldMappingSet();
						for (ImportFieldMapping mapping : mappingSet) {
							String mappedFieldColumn = fileMapping.get(mapping.getMappedField());
							mapping.setMappedField(mappedFieldColumn);
						}
						if (validationService.isValidRequiredMapping(importJob)) {
							ExcelFileProcessThread excelFileProcessThread = new ExcelFileProcessThread(authContext,
									validationService, this,
									fileMapping, jobProgressService,
									jobProgressToken, sheets);
							excelFileProcessThread.setImportJob(importJob);
							excelFileProcessThread.setFile(file);
							excelFileProcessThread.setSheets(sheets);
							excelFileProcessThread.setUser(user);
							Future<ImportJobStatus> future = importJobProcessPool.submit(excelFileProcessThread);
							importJobProcessResultList.add(future);
						} else {
							isValidationFailed = true;
						}
						
					} else if (importJob.isFileTypeXml()) {
						if(validationService.isValidRequiredXMLMapping(importJob)){
							XMLFileProcessThread excelFileProcessThread = new XMLFileProcessThread(authContext,this,jobProgressService, jobProgressToken);
							excelFileProcessThread.setImportJob(importJob);
							excelFileProcessThread.setFile(file);
							excelFileProcessThread.setUser(user);
							Future<ImportJobStatus> future = importJobProcessPool.submit(excelFileProcessThread);
							importJobProcessResultList.add(future);
						}else{
							isValidationFailed = true;
						}
					}
					if(isValidationFailed){
						ImportJobStatus jobStatus = new ImportJobStatus();
						jobStatus.setStatus(authContext.getI18nHelper().getText("zephyr-je.pdb.importer.init.failed"));
						jobStatus.setErrorMsg(authContext.getI18nHelper().getText("zephyr-je.pdb.importer.required.field.missing"));
						jobStatusList.add(jobStatus);
					}

				importJobProcessPool.shutdown();
				while (!importJobProcessPool.isTerminated()) {
				}
				if(importJobProcessResultList != null && importJobProcessResultList.size() > 0) {
					for(Future<ImportJobStatus> result : importJobProcessResultList) {
						ImportJobStatus jobStatus = (ImportJobStatus)result.get();
						jobStatusList.add(jobStatus);
					}
				}
			

			return isSuccess;
		} catch (Exception e) {
			ImportJobStatus jobStatus = new ImportJobStatus();
			jobStatus.setStatus(ApplicationConstants.IMPORT_JOB_PROCESS_FAIL_STATUS);
			jobStatus.setErrorMsg(e.getMessage());
			jobStatusList.add(jobStatus);
			//jobProgressService.completedWithStatus(ApplicationConstants.JOB_STATUS_FAILED, jobProgressToken);
			ExcelFileUtil.addJobHistory(importJob, "Exception while performing job " + e.getMessage());
			log.warn("Exception in import processing" + e);
			throw e;
		}finally{
			json.put("jobstatus", objMapper.writeValueAsString(jobStatusList));
			jobProgressService.setMessage(jobProgressToken, json.toString());
			jobProgressService.addCompletedSteps(jobProgressToken,1);//final msg
//			Integer completedSteps = 0;
//			if(StringUtils.isNotEmpty(jobStatusList.get(0).getIssuesCount())){
//				completedSteps = Integer.parseInt(jobStatusList.get(0).getIssuesCount());
//			}else if(StringUtils.isNotEmpty(jobStatusList.get(0).getIssuesCountForSteps())){
//				completedSteps = Integer.parseInt(jobStatusList.get(0).getIssuesCountForSteps());
//			}
//			jobProgressService.addCompletedSteps(jobProgressToken, completedSteps);
			if(jobStatusList.get(0).getErrorMsg() != null){
				jobProgressService.setErrorMessage(jobProgressToken, jobStatusList.get(0).getErrorMsg());
			}
			deleteFiles(file);
		}
	}

	@Override
	public TestCase populateTestCase(ImportJob importJob,
			ApplicationUser user, Map<String, ColumnMetadata> columnMetadataMap, int lastRowIdForThisTC){

		TestCase testcase = new TestCase();
		testcase.setProjectID(importJob.getProjectId());
		if(StringUtils.isNotEmpty(importJob.getLinkingIssueKey()))
			testcase.setLinkTypeId(importJob.getLinkTypeId());
		testcase.setLinkingIssueKey(importJob.getLinkingIssueKey());

		Project project = getProject(user, importJob.getProjectId());
		IssueType issueType= getIssueType(project, importJob.getIssueType());
		testcase.setIssueTypID(issueType.getId());
		//Map<String, String> testCaseMap = new HashMap<String, String>();
		for (Map.Entry<String, ColumnMetadata> entry : columnMetadataMap.entrySet()) {
			ColumnMetadata holder = entry.getValue();
			String id = entry.getKey();
			if (ApplicationConstants.JIRA_FIELD_SUMMARY.equals(id)) {
				testcase.setName(holder.getValue());
			} else if (ApplicationConstants.JIRA_FIELD_EXTERNAL_ID.equals(id)) {
				testcase.setExternalId(holder.getValue());
			} else if (ApplicationConstants.JIRA_FIELD_PRIORITY.equals(id)) {
				List<String> prioritiesList = ((ImportFieldConfig)importJob.getFieldConfigMap().get("priority")).getAllowedValues();
				prioritiesList.stream().forEach(priority -> {
					String[] priorityValues = StringUtils.split(priority,":");
					if(priorityValues.length > 1) {
						if (StringUtils.equals(priorityValues[1],holder.getValue())) {
							testcase.setPriority(holder.getValue());
						}
					}
				});

			} else if (ApplicationConstants.JIRA_FIELD_LABELS.equals(id)) {
				testcase.setTag(holder.getValue());
			} else if (ApplicationConstants.JIRA_FIELD_COMMENTS.equals(id)) {
				testcase.setComments(holder.getValue());
			} else if (ApplicationConstants.JIRA_FIELD_DESCRIPTION.equals(id)) {
				testcase.setDescription(holder.getValue());
			} else if (ApplicationConstants.JIRA_FIELD_FIX_VERSIONS.equals(id)) {
				testcase.setFixVersions(holder.getValue());
			} else if (ApplicationConstants.JIRA_FIELD_COMPONENTS.equals(id)) {
				testcase.setComponents(holder.getValue());
			} else if (ApplicationConstants.JIRA_FIELD_ISSUE_KEY.equals(id)) {
				testcase.setIssueKey(holder.getValue());
			} else if (ApplicationConstants.JIRA_FIELD_DUE_DATE.equals(id)){
				testcase.setDueDate(holder.getValue());
			} else if (ApplicationConstants.JIRA_FIELD_ASSIGNEE.equals(id)) {
				testcase.setAssignee(holder.getValue());
			} else if (ApplicationConstants.JIRA_FIELD_PARENT.equals(id)) {
				testcase.setParent(holder.getValue());
			}else if (ApplicationConstants.JIRA_FIELD_AFFECTED_VERSIONS.equals(id)) {
				testcase.setAffectedVersions(holder.getValue());
			}else {
				/* custom fields */
				populateCustomField(testcase, holder.getFieldConfig(),
						holder.getValue());
			}
		}
		/*
		try {
			String issueId = "";
			if(StringUtils.isNotBlank(testcase.getIssueKey())) {
				//TODO - check if issueType is Test
				if(Pattern.matches(Constants.ISSUE_KEY_REGEX, testcase.getIssueKey())){
					LocalIssue issue = JiraServiceImpl.getIssue(testcase.getIssueKey());
					issueId = issue.getId();
				}else if(Pattern.matches("\\d+", testcase.getIssueKey())){
					issueId = testcase.getIssueKey();
				}
				System.out.println("fetched issue id " + issueId);
				//log.info("fetched issue id " + issueId);
			}else{
				issueId = JiraServiceImpl.saveTestcase(testcase);
			}

			importJob.getHistory().add(new ImportJobHistory(new Date(), "Issue " + issueId + " created!"));
			setTestCaseContents(issueId, testsValue, userId);
			importJob.getHistory().add(new ImportJobHistory(new Date(), "Issue steps for " + issueId + " created!"));
			return testcase.getIssueKey();
		} catch (Exception ex) {
			validationService.addJobHistory(importJob, "Unable to add testcase ending at rowNumber - " + lastRowIdForThisTC + ". Error:" + ex.getMessage());
			log.error("", ex);
		}
		 */
		return testcase;
	}

	protected void setTestCaseContents(String issueId,
			ArrayList<TestStepDetails> testsValue, Long userId) throws Exception {
		/*
		 * TestStep ts = new TestStep(); ts.setLastModificationDate(new Date());
		 * ts.setLastModifiedBy((userId != null ? userId : 1L));
		 * ts.setReleaseId(releaseId); ts.setTcId(testcase.getId());
		 * ts.setSteps(TestcaseContentsUtil.convert2(testsValue));
		 * ts.setMaxId(ts.getSteps().size());
		 */
		/*for (TestStepDetails ts : testsValue) {
			JiraServiceImpl.saveTestStep(issueId, new TestStep(ts.getStep(),
					ts.getData(), ts.getResult()));
		}*/
	}

	protected boolean isImportSuccess(boolean currentResult, boolean lastResult) {
		if (currentResult == true && lastResult == true)
			return true;
		else
			return false;
	}

	protected boolean isAllImportsFails(boolean currentResult, boolean lastResult) {
		if (currentResult || lastResult)
			return true;
		else
			return false;
	}

    /**
     * Initializes column details required for import.
     * @param fieldConfigs
     * @param mappingSet
     * @param fileMapping
     * @return
     */
	public Map<String, ColumnMetadata> populateColumnMetadata(Map<String, ImportFieldConfig> fieldConfigs, 
			Set<ImportFieldMapping> mappingSet, Map<String, String> fileMapping) {
		Map<String, ColumnMetadata> columnsMetadataMap = new HashMap<String, ColumnMetadata>();
		for (ImportFieldMapping fieldMapDetail : mappingSet) {
			ColumnMetadata columnMetadata = new ColumnMetadata();
			columnMetadata.setMappedField(fieldMapDetail.getMappedField());
			columnMetadata.setFieldConfig(fieldConfigs.get(fieldMapDetail.getZephyrField()));
			//columnsMetadataMap.put(columnMetadata.getFieldConfig().getId().toString(), columnMetadata);
			columnsMetadataMap.put(fieldMapDetail.getZephyrField(), columnMetadata);
			/* some fields require truncation info */
			//String idValue = columnMetadata.getFieldConfig().getId().toString();
			String idValue = fieldMapDetail.getZephyrField();
			if (idValue.equals(ApplicationConstants.JIRA_FIELD_SUMMARY)
					|| idValue.equals(ApplicationConstants.JIRA_FIELD_EXTERNAL_ID)
					|| idValue.equals(ApplicationConstants.JIRA_FIELD_PRIORITY)
					|| idValue.equals(ApplicationConstants.JIRA_FIELD_LABELS)) {
				columnMetadata.setTruncateInfoRequired(true);
			}
		}
		return columnsMetadataMap;
	}

    /**
     * Initializes column details required for import.
     * @param fieldConfigs
     * @param mappingSet
     * @return
     */
	public Map<String, ColumnMetadata> populateColumnMetadata(Map<String, ImportFieldConfig> fieldConfigs, 
			Set<ImportFieldMapping> mappingSet) {
		Map<String, ColumnMetadata> columnsMetadataMap = new HashMap<String, ColumnMetadata>();
		for (ImportFieldMapping fieldMapDetail : mappingSet) {
			ColumnMetadata columnMetadata = new ColumnMetadata();
			columnMetadata.setMappedField(fieldMapDetail.getMappedField());
			columnMetadata.setFieldConfig(fieldConfigs.get(fieldMapDetail.getZephyrField()));
			//columnsMetadataMap.put(columnMetadata.getFieldConfig().getId().toString(), columnMetadata);
			columnsMetadataMap.put(fieldMapDetail.getZephyrField(), columnMetadata);
			/* some fields require truncation info */
			//String idValue = columnMetadata.getFieldConfig().getId().toString();
			String idValue = fieldMapDetail.getZephyrField();
			if (idValue.equals(ApplicationConstants.JIRA_FIELD_SUMMARY)
					|| idValue.equals(ApplicationConstants.JIRA_FIELD_EXTERNAL_ID)
					|| idValue.equals(ApplicationConstants.JIRA_FIELD_PRIORITY)
					|| idValue.equals(ApplicationConstants.JIRA_FIELD_LABELS)) {
				columnMetadata.setTruncateInfoRequired(true);
			}
		}
		return columnsMetadataMap;
	}

	private static void populateCustomField(TestCase testcase, ImportFieldConfig fldConfig,
			final String rawValue) {
		if (//!fldConfig.getSystemField()
				//&& StringUtils.startsWith(fldConfig.getId(), "custom") &&
				rawValue != null) {
			/*FieldMetadata fldMetadata = Constants.fieldTypeMetadataMap
					.get(fldConfig.getFieldTypeMetadata());*/
			ImportFieldSchema schema = fldConfig.getFieldSchema();
			Gson gson = new Gson();

			// Treat arrays and customField with allowedValues the same way.
			switch(schema.getType()) {
			case "array":
				populateArrayTypeCustomField(testcase, fldConfig, rawValue, schema);
				break;
			case "group":
				testcase.getCustomProperties().put(fldConfig.getId(),  new SingleValueMap("name", rawValue));
				//testcase.getCustomProperties().put(fldConfig.getId(), new SingleValueMap("name", rawValue));
				break;
			case "project":
				testcase.getCustomProperties().put(fldConfig.getId(),  new SingleValueMap("key", rawValue));
				//testcase.getCustomProperties().put(fldConfig.getId(), new SingleValueMap("key", rawValue));
				break;
			case "user":
				testcase.getCustomProperties().put(fldConfig.getId(),  new SingleValueMap("name", rawValue));
				//testcase.getCustomProperties().put(fldConfig.getId(), new SingleValueMap("name", rawValue));
				break;
			case "version":
				testcase.getCustomProperties().put(fldConfig.getId(),  new SingleValueMap("name", rawValue));
				//testcase.getCustomProperties().put(fldConfig.getId(), new SingleValueMap("name", rawValue));
				break;
			case "string":
				if((fldConfig.getAllowedValues() != null && fldConfig.getAllowedValues().size() > 0)){
					testcase.getCustomProperties().put(fldConfig.getId(),  new SingleValueMap("value", rawValue));
					//testcase.getCustomProperties().put(fldConfig.getId(), new SingleValueMap("value", rawValue));
				}
				/*Multitextfield*/
				else{
					testcase.getCustomProperties().put(fldConfig.getId(), rawValue);
				}
				break;
			case "number":
				testcase.getCustomProperties().put(fldConfig.getId(), rawValue);
				break;
			case "option":
				//testcase.getCustomProperties().put(fldConfig.getId(),  gson.toJson(new SingleValueMap("value", rawValue), SingleValueMap.class));
				testcase.getCustomProperties().put(fldConfig.getId(), new SingleValueMap("value", rawValue));
				break;
			case "date":
				String inputPattern = System.getProperty("DATE_FORMAT", ApplicationConstants.DATE_FORMAT_SHORT );//"d/MMM/yy" DateFormatUtils.ISO_DATE_FORMAT.getPattern());
				setDateTypeCustomField(testcase, fldConfig, rawValue, inputPattern, ApplicationConstants.DATE_FORMAT_SHORT);
				break;
			case "datetime":
				inputPattern = System.getProperty("DATE_TIME_FORMAT", "dd/MMM/yy hh:mm a");//DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.getPattern());
				setDateTypeCustomField(testcase, fldConfig, rawValue, inputPattern, "dd/MMM/yy hh:mm a");
				break;
			case "option-with-child":
				//TODO
				break;
			default:
				testcase.getCustomProperties().put(fldConfig.getId(), rawValue);
			}
		}
	}

	private static void populateArrayTypeCustomField(TestCase testcase, ImportFieldConfig fldConfig, String rawValue, ImportFieldSchema schema) {
		//Gson gson = new Gson();
		if (StringUtils.equals("string", schema.getItems())
				|| StringUtils.equals("option", schema.getItems())) {
			/* Labels */
			if(StringUtils.equals(FieldMetadata.LABEL_TYPE, schema.getCustom())){
				testcase.getCustomProperties().put(fldConfig.getId(), rawValue);
				//testcase.getCustomProperties().put(fldConfig.getId(), rawValue.split(","));
			}
			//Special handling of JIRA Agile EPIC and Sprint Field
			else if(StringUtils.equals(FieldMetadata.GH_EPIC_LINK_TYPE, schema.getCustom())){
				if(!Pattern.matches(ApplicationConstants.ISSUE_KEY_REGEX, rawValue)){
					log.info("EPIC key doesn't seem to be a valid JIRA Key. If this errors, try with a valid JIRA Key");
					//log.error("EPIC key doesn't seem to be a valid JIRA Key. If this errors, try with a valid JIRA Key");
				};
				testcase.getCustomProperties().put(fldConfig.getId(), rawValue);
			}else if(StringUtils.equals(FieldMetadata.GH_SPRINT_TYPE, schema.getCustom())){
				if(!Pattern.matches("\\d+", rawValue)){
				log.info("Sprint id doesn't seem to be valid. Please check if import fails");
					//log.warn("Sprint id doesn't seem to be valid. Please check if import fails");
				};
				testcase.getCustomProperties().put(fldConfig.getId(), rawValue);
			}else{
				/* Multi select and Multi radio buttons */
				ArrayMap valueList = getArrayOfMapsWithKey(rawValue, "value");
				testcase.getCustomProperties().put(fldConfig.getId(),  valueList);
			}
		} else if(StringUtils.equals("user", schema.getItems()) && StringUtils.equals(FieldMetadata.MULTI_USER_PICKER_TYPE, schema.getCustom())){
			ArrayMap valueList = getArrayOfMapsWithKey(rawValue, "name");
			testcase.getCustomProperties().put(fldConfig.getId(),  valueList);
			//testcase.getCustomProperties().put(fldConfig.getId(), valueList);
		}else if(StringUtils.equals("version", schema.getItems()) && StringUtils.equals(FieldMetadata.MULTI_VERSION_TYPE, schema.getCustom())){
			ArrayMap valueList = getArrayOfMapsWithKey(rawValue, "name");
			testcase.getCustomProperties().put(fldConfig.getId(),  valueList);
			//testcase.getCustomProperties().put(fldConfig.getId(), valueList);
		} else if(StringUtils.equals("group", schema.getItems()) && StringUtils.equals(FieldMetadata.MULTI_GROUP_PICKER_TYPE, schema.getCustom())){
			ArrayMap valueList = getArrayOfMapsWithKey(rawValue, "name");
			testcase.getCustomProperties().put(fldConfig.getId(),  valueList);
			//testcase.getCustomProperties().put(fldConfig.getId(), valueList);
		} else
			log.info("Unknown items data type for Array " + schema.getItems());
		// log.error("Unknown items data type for Array " + fldMetadata.getItemsDataType());
	}

	private static ArrayMap getArrayOfMapsWithKey(String rawValue, final String mapKeyName) {
		return getArrayOfMapsWithKey(rawValue, mapKeyName, ",");
	}

	private static ArrayMap getArrayOfMapsWithKey(String rawValue, final String mapKeyName, final String seperator) {
		return new ArrayMap(mapKeyName, rawValue.split(seperator));
	}

	public void saveAttachment(String issueKey, File workbook) {
		IssueResult result = issueService.getIssue(authContext.getLoggedInUser(), issueKey);
		Issue issue = result.getIssue();
		CreateAttachmentParamsBean attachmentBean = new CreateAttachmentParamsBean.Builder(workbook, workbook.getName(),"application/octet-stream", authContext.getLoggedInUser(), issue).build();
		try {
			attachmentManager.createAttachment(attachmentBean);
		} catch(Exception ex) {
			log.warn("Exception during attachment creation", ex);
		}
	}

	@Override
	public Response createTestCase(TestCase testCase, ApplicationUser user) throws Exception{
		Response response = null;
		if(testCase != null) {
			if(StringUtils.isNotBlank(testCase.getIssueKey())) {
				if(Pattern.matches(ApplicationConstants.ISSUE_KEY_REGEX, testCase.getIssueKey())){
					IssueResult result = issueService.getIssue(user, testCase.getIssueKey());
					response = testCase.new Response();
					response.setIssueKey(result.getIssue().getKey());
				}else if(Pattern.matches("\\d+", testCase.getIssueKey())){
					response = testCase.new Response();
					response.setIssueKey(testCase.getIssueKey());
				}
				log.info("fetched issue key:" + response != null ? response.getIssueKey() : null);
			}else{
				response = testCaseManager.createTestCase(testCase, user);
			}
		}
		return response;
	}

	@Override
	public void createTestSteps(ImportJob importJob, String issueKey, List<Map<String, Object>> testStepsProperties) {
		Issue issue = null;
		try {
			issue = issueManager.getIssueObject(issueKey);
		} catch(Exception ex) {
			log.warn("Exception while getting issue with key:"+issueKey, ex);
			ExcelFileUtil.addJobHistory(importJob, "Exception while getting issue with key:" + issueKey + ":" + ex.getMessage());
		}
		if(testStepsProperties != null && testStepsProperties.size() > 0) {
			int order = 1;
			for(Map<String, Object> testStepMap : testStepsProperties) {
				TeststepBean testStepBean = new TeststepBean(order, order,  (String)testStepMap.get(ApplicationConstants.TESTSTEP_ACTION)
						, (String)testStepMap.get(ApplicationConstants.TESTSTEP_DATA), (String)testStepMap.get(ApplicationConstants.TESTSTEP_EXPECTED_RESULTS),  issue);
				testStepManager.createTeststep(testStepBean, issue.getId());
				order++;
			}
		}
	}

	@Override
	public Project getProject (ApplicationUser user, String projectID) {
		Project project = null;
		if(StringUtils.isNotEmpty(projectID)) {
			ProjectService.GetProjectResult projectResult = projectService.getProjectById(user, Long.parseLong(projectID));
			if (projectResult.isValid())
			{
				project = projectResult.getProject();
			}
		}
		return project;
	}

	@Override
	public IssueType getIssueType(Project project, String issueTypeName) {
		Collection<IssueType> issueTypes = project.getIssueTypes();
		if(issueTypes != null && issueTypes.size() > 0) {
			for(IssueType issueType: issueTypes) {
				if(issueType.getId().equalsIgnoreCase(issueTypeName)) {
					return issueType;
				}
			}
		}
		return null;
	}

	private static void setDateTypeCustomField(TestCase testcase, ImportFieldConfig fldConfig, String rawValue, String inputPattern, String pattern) {
		SimpleDateFormat df = (inputPattern == null) ? new SimpleDateFormat() : new SimpleDateFormat(inputPattern);
		try {
			testcase.getCustomProperties().put(fldConfig.getId(), DateFormatUtils.format(df.parse(rawValue), pattern));
		} catch (ParseException e) {
			e.printStackTrace();
			//log.error("Error in parsing date for custom field " + fldConfig.getId() + ", value " + rawValue, e);
			testcase.getCustomProperties().put(fldConfig.getId(), rawValue);
		}
	}

	static class SingleValueMap {
		private String mapKey;
		private String value;

		public SingleValueMap(String mapKey, String value) {
			this.mapKey = mapKey;
			this.value = value;
		}
		public SingleValueMap() {
			super();
		}

		public String getMapKey() {
			return mapKey;
		}

		public String getValue() {
			return value;
		}
	}

	static class ArrayMap {
		private String mapKey;
		private String[] values;

		public ArrayMap(String mapKey, String[] values) {
			this.mapKey = mapKey;
			this.values = values;
		}
		public ArrayMap() {
			super();
		}
		public String getMapKey() {
			return mapKey;
		}

		public String[] getValues() {
			return values;
		}
	}

	public Map<String, String> extractIssueMapping(ImportJob importJob, File file, List<Sheet> sheets) throws Exception{
		Map<String, String> response = new HashMap<String, String>();
		if(file == null) return null;

		if(importJob.isFileTypeExcel()){
			response = getExcelElementsForMapping(sheets);
		} else if(importJob.isFileTypeXml()){
			response = getXMLElementsForMapping(file);
		} else{
			log.error("Unrecognized file format for the importer, doing nothing for " + importJob.getFileType());
			throw new Exception(authContext.getI18nHelper().getText("zephyr-je.pdb.importer.issuemapping.fileformat.not.found"));
		}
		deleteFiles(file);
		return response;
	}

	/**
     * Delete files.
	 */
	private void deleteFiles(File file){
			try{
                if(file.exists()) {
                    try {
                        Files.delete(file.toPath());
                    } catch (NoSuchFileException x) {
                        log.error("%s: no such" + " file or directory%n", file.toPath());
                    } catch (DirectoryNotEmptyException x) {
                        log.error(file.toPath() + " not empty");
                    } catch (IOException x) {
                        log.error("exception occurred while deleting the file.",x);
                    }
                }
			}catch(Exception e){
				e.printStackTrace();
			}
	}

	private Map<String, String> getExcelElementsForMapping(List<Sheet> sheets) throws Exception{
		Map<String, String> columnHeaderMapping = null;
			Sheet sheet = sheets.get(0);//only 1st sheet would be fine to get mapping
			String sheetName = sheet.getSheetName();
			log.debug("Getting column header map from sheet: " + sheetName);
			//FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();// As a best practice, evaluator should be
			columnHeaderMapping = ExcelFileUtil.getColumnHeaderMap(sheet, 0);
			if(columnHeaderMapping == null){
				throw new Exception(authContext.getI18nHelper().getText("zephyr-je.pdb.importer.issuemapping.not.found"));
			}
		return columnHeaderMapping;
	}

	@SuppressWarnings("rawtypes")
	private Map<String, String> getXMLElementsForMapping(File file) throws Exception{
		Set<String> elements = new HashSet<>();
		//XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
		boolean isTestcaseFound = false;
//		for(File inputFile: files){
			XMLEventReader xmlEventReader = null;
            FileReader fileReader = null;
			try{
				//Map<String, String> xmlValuesMap = null;
				List<Map<String, Object>> testSteps = new ArrayList<>();
				Stack<String> stepStack = new Stack<>();
				boolean isCustomField = false;

				Map<String, Object> stepData = null;
				XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
				fileReader = new FileReader(file);
				//xmlEventReader = xmlInputFactory.createXMLEventReader(new FileInputStream(inputFile));
                xmlEventReader = xmlInputFactory.createXMLEventReader(fileReader);
				StringBuilder element = new StringBuilder("");
				int customfieldElementCount = -1;
				String customFieldKey = null;
				//				String customFieldValue = null;
				while(xmlEventReader.hasNext()){
					XMLEvent xmlEvent = xmlEventReader.nextEvent();
					if (xmlEvent.isStartElement()){
						StartElement startElement = xmlEvent.asStartElement();
						String elementName = startElement.getName().toString();
						/*if(StringUtils.endsWith(element, "custom_field") && startElement.getName().equals("name")){
	                    	//elementName = startElement.
	                    }*/
						//Incase the element is testcase, its a start of a new testcase
						if(elementName.equals("testcase")){
							isTestcaseFound = true;
							//createTestcase(xmlValuesMap, testSteps);
							//xmlValuesMap = new HashMap<String, String>();
						} else if(elementName.equals("step")){
							//Incase the element is step, its start of a new teststep
							if(stepStack.size() == 0){
								stepStack.push(elementName);
								stepData = new HashMap<>();
							}
						} else if(elementName.equals("custom_field")){
							isCustomField = true;
							customfieldElementCount = 1;
						} else if(isCustomField){
							elementName="";
						}
						if(element.length() > 0 //&& elementName.length() > 0
								){
							element.append("/");
						}
						if(elementName.length() > 0){
							element//.append("/")
							.append(elementName);
						}
						//elements.add(element.toString());
						Iterator attributeIterator = startElement.getAttributes();
						//if(xmlValuesMap != null){
						while(attributeIterator.hasNext()){
							Attribute attribute = (Attribute)attributeIterator.next();
							//elements.add(element + "." + attribute.getName());
							//element.append("." + attribute.getName());
							//xmlValuesMap.put(element + "." + attribute.getName(), attribute.getValue());
							elements.add(element.toString() + "." + attribute.getName());
						}
						//}
					}
					/*if(xmlEvent.isProcessingInstruction()){
	                	ProcessingInstruction pi = (ProcessingInstruction) xmlEvent;
	                	if(StringUtils.startsWith(pi.getData(), "customfield")){
	                		elements.add(element + "/" + pi.getData());
	                		element.delete(0, element.length());
	                	}
	                }*/
					if(xmlEvent.isCharacters()){
						javax.xml.stream.events.Characters pi = (javax.xml.stream.events.Characters) xmlEvent;
						if(!StringUtils.isEmpty(pi.getData().trim()) ){//&& xmlValuesMap != null){
							if(!stepStack.isEmpty()){
								//This is a step element
								stepData.put(element.toString(), pi.getData());
								elements.add(element.toString());
							}else if(isCustomField){
								//This is a custom field element
								if(customfieldElementCount == 1){
									customFieldKey = element.toString() + pi.getData();
								}else if (customfieldElementCount == 2){
									//xmlValuesMap.put(customFieldKey, pi.getData());
									elements.add(customFieldKey);
									isCustomField = false;	
								}
								++customfieldElementCount;
							}else {
								//xmlValuesMap.put(element.toString(), pi.getData());
								elements.add(element.toString());
								//This is a issue field element
							}
						}
						/*if(StringUtils.startsWith(pi.getData(), "customfield") || 
	                			(element.lastIndexOf("/") > 0 && StringUtils.endsWith(element.substring(0, element.lastIndexOf("/")), "custom_field"))){
	                		//elements.add(element.substring(0, element.lastIndexOf("/")) + "/" + pi.getData());
	                		//element.delete(0, element.length());
	                		element = new StringBuilder(element.substring(0, element.lastIndexOf("/")) + "/" + pi.getData());
	                		isCustomField = true;
	                	}*/
						//xmlValueMap.put(element.toString(), pi.getData());
					}
					if(xmlEvent.isEndElement()){
						EndElement endElement = xmlEvent.asEndElement();
						String elementName = endElement.getName().toString();
						//elements.add(element.toString());
						if(elementName.equals("testcase")){
							//createIssue(acw, xmlValuesMap, testSteps, importJob, columnMetadataMap, project, issueTypeObj, createOn);
							//xmlValuesMap = null;
						}else if(elementName.equals("step")){
							if(stepStack.size() > 0){
								stepStack.pop();
								testSteps.add(stepData);
								stepData = null;
							}
						}/*else if(elementName.equals("custom_field")){
	                    	isCustomField = false;
	                    }*/
						if(element.indexOf("/") > 0){
							element.delete(element.lastIndexOf("/"), element.length());
						}else {
							element.delete(0, element.length());
						}
					}
				}
			}catch(Exception e){
				log.error("Exception while getting elements from XML file:" + file.getName() + e.getMessage());
			}finally{
				try {
	                if(null != xmlEventReader) {
                        log.debug("attempt to close xml event reader.");
	                	xmlEventReader.close();
	                }
	                if(null != fileReader) {
                        log.debug("attempt to close file reader");
                        fileReader.close();
                    }
				} catch (XMLStreamException | IOException  e) {
					log.error("Exception while closing XmlEventReader for the file:"+ file.getName() + e.getMessage());
				}
			}
//		}
		if(!isTestcaseFound){
			throw new Exception(authContext.getI18nHelper().getText("zephyr-je.pdb.importer.invalid.xml.doc"));
		}
		//return elements;
		return elements.stream().collect(Collectors.toMap(value -> value, value -> value));
	}


}
