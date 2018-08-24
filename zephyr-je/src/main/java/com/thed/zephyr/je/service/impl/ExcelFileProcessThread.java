package com.thed.zephyr.je.service.impl;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.thed.zephyr.je.operation.JobProgress;
import com.thed.zephyr.je.service.ExcelFileValidationService;
import com.thed.zephyr.je.service.IssueImporterService;
import com.thed.zephyr.je.service.JobProgressService;
import com.thed.zephyr.je.vo.*;
import com.thed.zephyr.je.vo.ImportFile.ColumnMetadata;
import com.thed.zephyr.je.vo.TestCase.Response;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.ExcelFileUtil;
import com.thed.zephyr.util.JiraUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;

import static com.thed.zephyr.util.ApplicationConstants.*;


public class ExcelFileProcessThread implements Callable<ImportJobStatus> {
	private final ExcelFileValidationService validationService;
	private ImportJob importJob;
	private File file;
	private ApplicationUser user;
	private IssueImporterService issueImporterService;
	private Map<String, String> fileMapping;
	private JobProgressService jobProgressService;
	private String jobProgressToken;
	private JiraAuthenticationContext authContext;
	private List<Sheet> sheets;

	private static final Logger log = LoggerFactory.getLogger(ExcelFileProcessThread.class);

	public ExcelFileProcessThread(final JiraAuthenticationContext authContext,
								  final ExcelFileValidationService validationService,
								  IssueImporterService issueImporterService,
								  Map<String, String> fileMapping,
								  final JobProgressService jobProgressService,
								  final String jobProgressToken,
								  final List<Sheet> sheets) {
		this.validationService = validationService;
		this.issueImporterService = issueImporterService;
		this.fileMapping = fileMapping;
		this.jobProgressService = jobProgressService;
		this.jobProgressToken = jobProgressToken;
		this.authContext = authContext;
		this.sheets = sheets;
	}

	public ImportJob getImportJob() {
		return importJob;
	}

	public void setImportJob(ImportJob importJob) {
		this.importJob = importJob;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public ApplicationUser getUser() {
		return user;
	}

	public void setUser(ApplicationUser user) {
		this.user = user;
	}

	public List<Sheet> getSheets() {
		return sheets;
	}

	public void setSheets(List<Sheet> sheets) {
		this.sheets = sheets;
	}

	@Override
	public ImportJobStatus call() throws Exception {
		
		ImportJobStatus jobStatus = new ImportJobStatus();
		
		if(validationService.isValidData(importJob, file, fileMapping, sheets)) {
			int issuesCount = importExcelFile(file, importJob, user, sheets);
			if(issuesCount > 0){
				jobStatus.setStatus(authContext.getI18nHelper().getText("zephyr-je.pdb.importer.init.successful"));
				if(importJob.isImportingTestStepsOnly()){
					jobStatus.setIssuesCountForSteps(String.valueOf(issuesCount));
				}else{
					jobStatus.setIssuesCount(String.valueOf(issuesCount));
				}
			} else {
				jobStatus.setStatus(authContext.getI18nHelper().getText("zephyr-je.pdb.importer.init.failed"));
				jobStatus.setErrorMsg(authContext.getI18nHelper().getText("zephyr-je.pdb.importer.failed.msg"));
			}
		} else {
			jobStatus.setStatus(authContext.getI18nHelper().getText("zephyr-je.pdb.importer.init.failed"));
			jobStatus.setErrorMsg(authContext.getI18nHelper().getText("zephyr-je.pdb.importer.validation.failed.msg"));
		}
		String fileName = file.getName();
		jobStatus.setFileName(fileName.substring(fileName.indexOf("_") + 1));
		//Overwrite the message if there is additional info from the history.
		if(importJob.getHistory() != null){
			StringBuilder errorMsg = new StringBuilder(); 
			for(ImportJobHistory hist : importJob.getHistory()){
				errorMsg.append(hist.getComments()).append("\n");
			}
			jobStatus.setErrorMsg(errorMsg.toString());
		}
		return jobStatus;
	}

	private int importExcelFile(File file, ImportJob importJob, ApplicationUser user, List<Sheet> sheets) throws Exception {
		int issuesCount = 0;
		//if(StringUtils.isBlank(importJob.getIssueKey())) {

			if(importJob.isFileTypeExcel()){
				String discriminator = importJob.getImportDetails().getDiscriminator();
				if (discriminator.equalsIgnoreCase(ApplicationConstants.EXCEL_DISCRIMINATOR_BY_EMPTY_ROW)) {
					issuesCount = importFileByEmptyRow(file, importJob, user, sheets);
				} else if (discriminator.equalsIgnoreCase(ApplicationConstants.EXCEL_DISCRIMINATOR_BY_SHEET)) {
					issuesCount = importFileBySheet(file, importJob, user, sheets);
				} else if (discriminator.equalsIgnoreCase(ApplicationConstants.EXCEL_DISCRIMINATOR_BY_ID_CHANGE)) {
					issuesCount = importFileById(file, importJob, user, sheets);
				} else if (discriminator.equalsIgnoreCase(ApplicationConstants.EXCEL_DISCRIMINATOR_BY_TESTNAME_CHANGE)) {
					issuesCount = importFileByName(file, importJob, user, sheets);
				}
			}
		/*} else {
			issuesCount = importTestStepsData(file, importJob, user);
		}*/
		return issuesCount;
	}

	protected int importFileByEmptyRow(File file, ImportJob importJob, ApplicationUser user, List<Sheet> sheets) {
//		InputStream fis = null;
		int issuesCount = 0;
		try {
//			fis = FileUtils.openInputStream(file);
			ImportDetails importDetails = importJob.getImportDetails();
			Set<ImportFieldMapping> mappingSet = importDetails.getFieldMappingSet();
			Map<String, ColumnMetadata>columnsMetadataMap = issueImporterService.populateColumnMetadata(importJob.getFieldConfigMap(), mappingSet, fileMapping);

//			Workbook wb = WorkbookFactory.create(fis);
			for(Sheet sheet : sheets ){

//				FormulaEvaluator evaluator = wb.getCreationHelper()
//						.createFormulaEvaluator();// As a best practice, evaluator should be
				// one per sheet

				int startPoint = importDetails.getStartingRowNumber() - 1;
				int lastRow = sheet.getLastRowNum() + EXTRA_ROWS_IN_END;
				
				List<Map<String, Object>> testStepsList = new ArrayList<Map<String, Object>>();
				Row row = null;
				Row previousRow = null;
				for (int i = startPoint; i < lastRow ; i++) {
					row = (Row) sheet.getRow(i);
					previousRow = (Row) sheet.getRow(i-1);
					
					if (ExcelFileUtil.isRowNull(row) ) {
						if(!ExcelFileUtil.isRowNull(previousRow)) {
						//if (blockFlag) {
							String issueKey = null;
							Response response = null;
							//if(StringUtils.isEmpty(importJob.getIssueKey())) {
							TestCase testCase =  issueImporterService.populateTestCase(importJob, user, columnsMetadataMap, i - 1);
							if(StringUtils.isEmpty(testCase.getIssueKey())){
								try{
									response = issueImporterService.createTestCase(testCase, user);
								}catch(Exception e){
									log.error("Exception while creating issue:", e);
									ExcelFileUtil.addJobHistoryAndUpdateStatus(importJob, IMPORT_JOB_IMPORT_FAILED, sheet.getSheetName() + ":Row:"+ (i+1) + ":" + e.getMessage());
									//e.printStackTrace();
								}
								if(response != null ){
									issueKey = response.getIssueKey();
//									handleJobProgress(issuesCount);
									++issuesCount;
									JobProgress jobProgress = jobProgressService.getJobProgress(jobProgressToken);
									if(jobProgress == null || jobProgress.getCanceledJob()){
										log.warn("Importer process stopped as the user has requested to stop:"+ file.getName());
										throw new Exception(authContext.getI18nHelper().getText("zephyr-je.pdb.importer.job.process.stopped"));
									}
								}
								/*Workbook newWb = ExcelFileUtil.createWorkBook(wb);
								Sheet newSheet = newWb.createSheet(sheet.getSheetName());
								List<List<String>> dataList = ExcelFileUtil.getExcelData(sheet,evaluator, startRowNum, i-1);
								ExcelFileUtil.populateExcelSheetWithData(newSheet, headerDataList, dataList);
								//ExcelFileUtil.copySheets(newSheet, sheet);
								File savedWorkbook = ExcelFileUtil.saveWorkbook(sheet, file, newWb);
								try {
									issueImporterService.saveAttachment(issueID, savedWorkbook);
								} finally {
									savedWorkbook.delete();
								}*/
							} else {
								issueKey = testCase.getIssueKey();
								importJob.setImportingTestStepsOnly(true);
							}
							if(issueKey != null){
								if(importJob.getIssueType().equalsIgnoreCase(JiraUtil.getTestcaseIssueTypeId())) {
									  issueImporterService.createTestSteps(importJob, issueKey, testStepsList);
									  testStepsList.clear();
									  if(!StringUtils.isEmpty(testCase.getIssueKey())) {
										  issueKey = response.getIssueKey();
//										  handleJobProgress(issuesCount);
										  ++issuesCount;
										  JobProgress jobProgress = jobProgressService.getJobProgress(jobProgressToken);
										  if(jobProgress == null || jobProgress.getCanceledJob()){
											  log.warn("Importer process stopped as the user has requested to stop:"+ file.getName());
											  throw new Exception(authContext.getI18nHelper().getText("zephyr-je.pdb.importer.job.process.stopped"));
										  }
									  }
								}
							}
							if(response != null && StringUtils.isNotEmpty(response.getResponseMsg())){
								ExcelFileUtil.addJobHistoryAndUpdateStatus(importJob, IMPORT_JOB_IMPORT_PARTIAL_SUCCESS, sheet.getSheetName() + ":Row:"+ (i+1) + ":" + response.getResponseMsg());
							}
							resetValues(columnsMetadataMap);
						 }
					} else {
						populateTestStep(columnsMetadataMap, row, testStepsList, i);
					}
					
					jobProgressService.addCompletedSteps(jobProgressToken,1);
				}
				/* save last row */
				//ColumnMetadata holder = columnsMetadataMap.get(zephyrField);
				//String value = holder.getValue();
				//if (value != null && !"".equals(value.trim())) {
					/*String issueKey =null;
					if(StringUtils.isEmpty(importJob.getIssueKey())) {
						TestCase testCase = issueImporterService.populateTestCase(importJob, user, columnsMetadataMap,lastRow -1);
						issueKey = issueImporterService.createTestCase(testCase, user);
					} else {
						issueKey =importJob.getIssueKey();
					}
					if(importJob.getIssueType().equalsIgnoreCase("Test")) {
						issueImporterService.createTestSteps(importJob, issueKey, testStepsList);
					}
					++issuesCount;*/
				/// end of processing
			}
			truncateUpdate(importJob, columnsMetadataMap);
			return issuesCount;
		} catch (Exception e) {
			log.warn("Exception in import process", e);
			ExcelFileUtil.addJobHistoryAndUpdateStatus(importJob, IMPORT_JOB_IMPORT_FAILED, e.getMessage());
			return issuesCount;
		} finally {
//			try {
//				if(null != fis) {
//					fis.close();
//				}
//			} catch (Exception e) {
//				log.warn("Exception in closing file stream process", e);
//				ExcelFileUtil.addJobHistoryAndUpdateStatus(importJob, IMPORT_JOB_NORMALIZATION_FAILED, e.getMessage());
//				return issuesCount;
//			}
		}
	}
	
	protected int importFileBySheet(File file, ImportJob importJob, ApplicationUser user, List<Sheet> sheets) {
//		InputStream fis = null;
		int issuesCount = 0;
		try {
//			fis = FileUtils.openInputStream(file);
			ImportDetails importDetails = importJob.getImportDetails();
			Set<ImportFieldMapping> mappingSet = importDetails.getFieldMappingSet();
			Map<String, ColumnMetadata>columnsMetadataMap = issueImporterService.populateColumnMetadata(importJob.getFieldConfigMap(), mappingSet, fileMapping);

//			Workbook wb = WorkbookFactory.create(fis);
			for(Sheet sheet : sheets ){

//				FormulaEvaluator evaluator = wb.getCreationHelper()
//						.createFormulaEvaluator();// As a best practice, evaluator should be
				// one per sheet

				int startPoint = importDetails.getStartingRowNumber() - 1;
				int lastRow = sheet.getLastRowNum() + EXTRA_ROWS_IN_END;
				if((lastRow - startPoint) > 1) {
				List<Map<String, Object>> testStepsList = new ArrayList<Map<String, Object>>();
				Row row = null;
				for (int i = startPoint; i < lastRow -1 ; i++) {
					row = (Row) sheet.getRow(i);
					if (!ExcelFileUtil.isRowNull(row)) {
						populateTestStep(columnsMetadataMap, row, testStepsList, i);
					}
					jobProgressService.addCompletedSteps(jobProgressToken,1);
				}
				/* save last row */
				    int i=0;
				    Response response = null;
					String issueKey = null;
					//if(StringUtils.isEmpty(importJob.getIssueKey())) {
					TestCase testCase = issueImporterService.populateTestCase(importJob, user, columnsMetadataMap,lastRow -1);
					if(StringUtils.isEmpty(testCase.getIssueKey())) {
						try{
							response = issueImporterService.createTestCase(testCase, user);
						}catch(Exception e){
							log.error("Exception while creating issue:", e);
							ExcelFileUtil.addJobHistoryAndUpdateStatus(importJob, IMPORT_JOB_IMPORT_FAILED, sheet.getSheetName() + ":Row:"+ (i+1) + ":" + e.getMessage());
						}
						if(response != null){
							issueKey = response.getIssueKey();
//							handleJobProgress(issuesCount);
							++issuesCount;
							JobProgress jobProgress = jobProgressService.getJobProgress(jobProgressToken);
							if(jobProgress == null || jobProgress.getCanceledJob()){
								log.warn("Importer process stopped as the user has requested to stop:"+ file.getName());
								throw new Exception(authContext.getI18nHelper().getText("zephyr-je.pdb.importer.job.process.stopped"));
							}
						}
					} else {
						issueKey = testCase.getIssueKey();
						importJob.setImportingTestStepsOnly(true);
					}
					
					if(issueKey != null){
						if(importJob.getIssueType().equalsIgnoreCase(JiraUtil.getTestcaseIssueTypeId())) {
							issueImporterService.createTestSteps(importJob, issueKey, testStepsList);
							testStepsList.clear();
							if(!StringUtils.isEmpty(testCase.getIssueKey())) {
								if(response != null && StringUtils.isNotEmpty(response.getIssueKey())) {
									issueKey = response.getIssueKey();
								}
//								handleJobProgress(issuesCount);
								++issuesCount;
								JobProgress jobProgress = jobProgressService.getJobProgress(jobProgressToken);
								if(jobProgress == null || jobProgress.getCanceledJob()){
									log.warn("Importer process stopped as the user has requested to stop:"+ file.getName());
									throw new Exception(authContext.getI18nHelper().getText("zephyr-je.pdb.importer.job.process.stopped"));
								}
							}
						}
					}
					if(response != null && StringUtils.isNotEmpty(response.getResponseMsg())){
						ExcelFileUtil.addJobHistoryAndUpdateStatus(importJob,
								IMPORT_JOB_IMPORT_PARTIAL_SUCCESS, sheet.getSheetName() + ":Row:"+ (i+1) + ":" + response.getResponseMsg());
					}
				}
				/// end of processing
			}
			truncateUpdate(importJob, columnsMetadataMap);
			return issuesCount;
		} catch (Exception e) {
			log.warn("Exception in import process", e);
			ExcelFileUtil.addJobHistoryAndUpdateStatus(importJob, IMPORT_JOB_IMPORT_FAILED, e.getMessage());
			//log.fatal("", e);
			//issuesCount = -1;
			return issuesCount;
		} finally {
//			try {
//				if(null != fis) {
//                    fis.close();
//                }
//			} catch (Exception e) {
//				log.warn("Exception in closing file stream process", e);
//				ExcelFileUtil.addJobHistoryAndUpdateStatus(importJob, IMPORT_JOB_NORMALIZATION_FAILED, e.getMessage());
//				log.fatal("", e);
//				issuesCount = -1;
//				return issuesCount;
//			}
		}
	}

	private void handleJobProgress(Integer issuesCount) throws Exception {
		++issuesCount;
		JobProgress jobProgress = jobProgressService.getJobProgress(jobProgressToken);
		if(jobProgress == null || jobProgress.getCanceledJob()){
			log.warn("Importer process stopped as the user has requested to stop:"+ file.getName());
			throw new Exception(authContext.getI18nHelper().getText("zephyr-je.pdb.importer.job.process.stopped"));
		}
	}


	protected int importFileById(File file, ImportJob importJob, ApplicationUser user, List<Sheet> sheets) {
		return importFileByChange(file, importJob, user, ApplicationConstants.JIRA_FIELD_EXTERNAL_ID, sheets);
	}

	protected int importFileByName(File file, ImportJob importJob, ApplicationUser user, List<Sheet> sheets) {
		return importFileByChange(file, importJob, user, ApplicationConstants.JIRA_FIELD_SUMMARY, sheets);
	}

	/*
	 * Common logic in importFileForByName() and importFileById() is abstracted in
	 * importFileByChange().
	 */
	protected int importFileByChange(File file, ImportJob importJob, ApplicationUser user, String zephyrField, List<Sheet> sheets) {
//		InputStream fis = null;
		int issuesCount = 0;
		try {
//			fis = FileUtils.openInputStream(file);
			ImportDetails importDetails = importJob.getImportDetails();

			Set<ImportFieldMapping> mappingSet = importDetails.getFieldMappingSet();
			Map<String, ColumnMetadata>columnsMetadataMap = issueImporterService.populateColumnMetadata(importJob.getFieldConfigMap(), mappingSet, fileMapping);
//			Workbook wb = WorkbookFactory.create(fis);

			for(Sheet sheet : sheets ) {
//				FormulaEvaluator evaluator = wb.getCreationHelper()
//						.createFormulaEvaluator();// As a best practice, evaluator should be
				int startPoint = importDetails.getStartingRowNumber() - 1;
				int lastRow = sheet.getLastRowNum() + EXTRA_ROWS_IN_END;
				//List<String> headerDataList = ExcelFileUtil.getRowData(sheet, 0);
				ColumnMetadata uniqueColumnHolder = columnsMetadataMap.get(zephyrField);

				int[] uniqueFieldRef = ExcelFileUtil.convertField(uniqueColumnHolder.getMappedField());
				int uniqueColumn = uniqueFieldRef[0];
				List<Map<String, Object>>testStepsList = new LinkedList<Map<String, Object>>();

				//ArrayList<TestStepDetails> testStepsList = new ArrayList<TestStepDetails>();
				Row row = null;
				String uniqueId = new String();
				boolean blockStart = false;
				int startRowNum = 0;
				for (int i = startPoint; i < lastRow -1; i++) {
					
					blockStart = false;
					row = sheet.getRow(i);
					if (ExcelFileUtil.isRowNull(row)) {
						continue;
					}
					if (i == startPoint) {// || blockStart==true ){
						uniqueId = ExcelFileUtil.getCellValue(row.getCell(uniqueColumn));
					}
					if (ExcelFileUtil.getCellValue(row.getCell(uniqueColumn)) != null
							&& uniqueId != null
							&& !(uniqueId.equalsIgnoreCase(ExcelFileUtil.getCellValue(
									row.getCell(uniqueColumn)))) ) {
						uniqueId = ExcelFileUtil.getCellValue(row.getCell(uniqueColumn));
						
						blockStart = true;
					}
					if (blockStart) {
						Response response = null;
						String issueKey = null;
						//if(StringUtils.isEmpty(importJob.getIssueKey())) {
						TestCase testCase = issueImporterService.populateTestCase(importJob, user, columnsMetadataMap, i - 1);
						if(StringUtils.isEmpty(testCase.getIssueKey())){
							try{
								response = issueImporterService.createTestCase(testCase, user);
							}catch(Exception e){
								log.error("Exception while creating issue:", e);
								ExcelFileUtil.addJobHistoryAndUpdateStatus(importJob, IMPORT_JOB_IMPORT_FAILED, sheet.getSheetName() + ":Row:"+ (i+1) + ":" + e.getMessage());
							}
							if(response != null){
								issueKey = response.getIssueKey();
//								handleJobProgress(issuesCount);
								++issuesCount;
								JobProgress jobProgress = jobProgressService.getJobProgress(jobProgressToken);
								if(jobProgress == null || jobProgress.getCanceledJob()){
									log.warn("Importer process stopped as the user has requested to stop:"+ file.getName());
									throw new Exception(authContext.getI18nHelper().getText("zephyr-je.pdb.importer.job.process.stopped"));
								}
							}
							/*Workbook newWb = ExcelFileUtil.createWorkBook(wb);
							Sheet newSheet = newWb.createSheet(sheet.getSheetName());
							List<List<String>> dataList = ExcelFileUtil.getExcelData(sheet, evaluator, startRowNum, i-1);
							ExcelFileUtil.populateExcelSheetWithData(newSheet, headerDataList, dataList);
							ExcelFileUtil.copySheets(newSheet, sheet);
							File savedWorkbook = ExcelFileUtil.saveWorkbook(sheet, file, newWb);
							try {
								issueImporterService.saveAttachment(issueKey, savedWorkbook);
							} finally {
								savedWorkbook.delete();
							}*/
						} else {
							issueKey = testCase.getIssueKey();
							importJob.setImportingTestStepsOnly(true);
						}
						resetValues(columnsMetadataMap);
						if(issueKey != null){
							if(importJob.getIssueType().equalsIgnoreCase(JiraUtil.getTestcaseIssueTypeId())) {
							  issueImporterService.createTestSteps(importJob, issueKey, testStepsList);
							  testStepsList.clear();
							  if(!StringUtils.isEmpty(testCase.getIssueKey())) {
								  issueKey = response.getIssueKey();
//								  handleJobProgress(issuesCount);
								  ++issuesCount;
								  JobProgress jobProgress = jobProgressService.getJobProgress(jobProgressToken);
								  if(jobProgress == null || jobProgress.getCanceledJob()){
									  log.warn("Importer process stopped as the user has requested to stop:"+ file.getName());
									  throw new Exception(authContext.getI18nHelper().getText("zephyr-je.pdb.importer.job.process.stopped"));
								  }
								}
							}
							
						}
						if(response != null && StringUtils.isNotEmpty(response.getResponseMsg())){
							ExcelFileUtil.addJobHistoryAndUpdateStatus(importJob,
									IMPORT_JOB_IMPORT_PARTIAL_SUCCESS, sheet.getSheetName() + ":Row:"+ (i+1) + ":" + response.getResponseMsg());
						}
						//updateJobProgress("","No. of issues created:" +issueCount);

					}

					/* process columns */
					
					populateTestStep(columnsMetadataMap, row, testStepsList, i);


					jobProgressService.addCompletedSteps(jobProgressToken,1);

				}// end for
				/* save last row */
				ColumnMetadata holder = columnsMetadataMap.get(zephyrField);
				String value = holder.getValue();
				if (value != null && !"".equals(value.trim())) {
					Response response = null;
					String issueKey = null;
					//if(StringUtils.isEmpty(importJob.getIssueKey())) {
					TestCase testCase = issueImporterService.populateTestCase(importJob, user, columnsMetadataMap,lastRow -1);
					if(StringUtils.isEmpty(testCase.getIssueKey())) {
						response = issueImporterService.createTestCase(testCase, user);
						if(response != null){
							issueKey = response.getIssueKey();
//							handleJobProgress(issuesCount);
							++issuesCount;
							JobProgress jobProgress = jobProgressService.getJobProgress(jobProgressToken);
							if(jobProgress == null || jobProgress.getCanceledJob()){
								log.warn("Importer process stopped as the user has requested to stop:"+ file.getName());
								throw new Exception(authContext.getI18nHelper().getText("zephyr-je.pdb.importer.job.process.stopped"));
							}
						}
						/*Workbook newWb = ExcelFileUtil.createWorkBook(wb);
						Sheet newSheet = newWb.createSheet(sheet.getSheetName());
						List<List<String>> dataList = ExcelFileUtil.getExcelData(sheet, evaluator, startRowNum, lastRow -1);
						ExcelFileUtil.populateExcelSheetWithData(newSheet, headerDataList, dataList);*/
					} else {
						issueKey =testCase.getIssueKey();
						importJob.setImportingTestStepsOnly(true);
					}
					if(importJob.getIssueType().equalsIgnoreCase(JiraUtil.getTestcaseIssueTypeId())) {
						if(testStepsList.size() > 0){
							issueImporterService.createTestSteps(importJob, issueKey, testStepsList);
							testStepsList.clear();
							if(!StringUtils.isEmpty(testCase.getIssueKey())) {
								++issuesCount;
//								JobProgress jobProgress = jobProgressService.setTotalSteps(importJob.getJobProgressKey(), issuesCount);
//								if(jobProgress == null){
//									log.warn("Importer process stopped as the admin has requested to stop:"+ file.getName());
//									throw new Exception("JobProcess Stopped");
//								}
							}
						}
						
					}
					if(response != null && StringUtils.isNotEmpty(response.getResponseMsg())){
						ExcelFileUtil.addJobHistoryAndUpdateStatus(importJob, IMPORT_JOB_IMPORT_PARTIAL_SUCCESS, sheet.getSheetName() + ":Row:"+ (lastRow-1) + ":" + response.getResponseMsg());
					}
				}/// end of processing
			}
			truncateUpdate(importJob, columnsMetadataMap);
			return issuesCount;
		} catch (Exception e) {
			log.warn("Exception in import process", e);
			ExcelFileUtil.addJobHistoryAndUpdateStatus(importJob,
					IMPORT_JOB_IMPORT_FAILED, e.getMessage());
			//issuesCount = -1;
			return issuesCount;
		} finally {
//			try {
//                if(null != fis) {
//                    fis.close();
//                }
//			} catch (Exception e) {
//				log.warn("Exception in closing file stream process", e);
//				ExcelFileUtil.addJobHistoryAndUpdateStatus(importJob, IMPORT_JOB_NORMALIZATION_FAILED, e.getMessage());
//				//issuesCount = -1;
//				return issuesCount;
//			}
		}
	}

	/*protected int importTestStepsData(File file, ImportJob importJob, ApplicationUser user) {
		InputStream fis = null;
		int issuesCount = 0;
		try {
			fis = FileUtils.openInputStream(file);
			ImportDetails importDetails = importJob.getImportDetails();

			Set<ImportFieldMapping> mappingSet = importDetails.getFieldMappingSet();

			Map<String, ColumnMetadata>columnsMetadataMap = issueImporterService.populateColumnMetadata(importJob.getFieldConfigMap(), mappingSet, fileMapping);
			Workbook wb = WorkbookFactory.create(fis);

			for(Sheet sheet : SheetIterator.create(wb, importJob) ) {
				FormulaEvaluator evaluator = wb.getCreationHelper()
						.createFormulaEvaluator();// As a best practice, evaluator should be
				int startPoint = importDetails.getStartingRowNumber() - 1;
				int lastRow = sheet.getLastRowNum() + EXTRA_ROWS_IN_END;
				List<Map<String, Object>>testStepsList = new LinkedList<Map<String, Object>>();

				//ArrayList<TestStepDetails> testStepsList = new ArrayList<TestStepDetails>();
				Row row = null;
				for (int i = startPoint; i <= lastRow; i++) {
					row = sheet.getRow(i);
					if (ExcelFileUtil.isRowNull(row)) {
						continue;
					}
					populateTestStep(columnsMetadataMap, evaluator, row, testStepsList, i);
					issuesCount = testStepsList.size();
				}	
				issueImporterService.createTestSteps(importJob, importJob.getIssueKey(), testStepsList);
			}
			truncateUpdate(importJob, columnsMetadataMap);
			return issuesCount;
		} catch (Exception e) {
			//log.warn("Exception during test step data import process", e);
			e.printStackTrace();
			ExcelFileUtil.addJobHistoryAndUpdateStatus(importJob,
					IMPORT_JOB_IMPORT_FAILED, e.getMessage());
			//log.fatal("", e);
			issuesCount = -1;
			return issuesCount;
		} finally {
			try {
                if(null != fis) {
                    fis.close();
                }
			} catch (Exception e) {
				//log.warn("Exception in closing file stream process", e);
				e.printStackTrace();
				ExcelFileUtil.addJobHistoryAndUpdateStatus(importJob,
						IMPORT_JOB_NORMALIZATION_FAILED, e.getMessage());
				System.out.println(""+e);
				//log.fatal("", e);
				issuesCount = -1;
				return issuesCount;
			}
		}
	}*/

	private void populateTestStep(Map<String, ColumnMetadata> columnMetadataMap, Row row,
			List<Map<String, Object>> testStepsList, int i) {

		//Teststep testStep = new Teststep();
		Map<String, Object> testStep = new HashMap<String, Object>();
		String step = null;
		for (Map.Entry<String, ColumnMetadata> entry : columnMetadataMap.entrySet()) {
			ColumnMetadata holder = entry.getValue();
			String value = ExcelFileUtil.getCellValue(holder.getMappedField(), row.getSheet(), row);
			if (value == null)
				continue;
			String idValue = entry.getKey();
			if (value != null) {

				/* field specific handling */
				if (idValue.equals(ApplicationConstants.JIRA_FIELD_PRIORITY)) {
					List<String> allowedValuesList = holder.getFieldConfig().getAllowedValues();
					if(allowedValuesList != null && allowedValuesList.size() > 0) {
						for(String allowedValue:allowedValuesList) {
							String[] values = allowedValue.split(":");
							if(values[0].equalsIgnoreCase(value)) {
								value = values[1];
								break;
							}
						}
					}
				} /*else if (holder.getFieldConfig().getId().toString()
						.equals(ApplicationConstants.FLAG_AUTOMATION)) {

					if (value.equalsIgnoreCase("A")
							|| value.toLowerCase().startsWith("auto")) {
						value = "true";
					} else {
						value = "false";
					}

				}*/ else {

					if (StringUtils.startsWith(idValue, "step")) {
						step = value;
						testStep.put(idValue, value);
					}
				}

				if (holder.isTruncateInfoRequired()) {
					Integer length = holder.getFieldConfig().getLength();

					if (length != null && holder.getFieldConfig().getLength() > 0) {
						if (value.length() > length) {
							value = value.substring(0, length);
							holder.addTruncatedRowIndex(i + 1);
						}
					}
				}

				holder.setValue(value);
			}
		}
		/* all columns for row are processed, add test steps */
		if(testStep.size() > 0){
			testStepsList.add(testStep);
		}

	}

	protected void truncateUpdate(ImportJob importJob, Map<String, ColumnMetadata> columns) {

		StringBuffer truncationInfo = new StringBuffer();

		for (Map.Entry<String, ColumnMetadata> entry : columns.entrySet()) {
			ColumnMetadata holder = entry.getValue();
			if (holder.getTruncateRowIndex().size() > 0) {
				truncationInfo.append("in rows " + holder.getTruncateRowIndex() + " "
						+ holder.getFieldConfig().getDisplayName() + " was truncated to "
						+ holder.getFieldConfig().getLength() + " chars.\n");
			}
		}

		if (truncationInfo.length() > 0) {
			ExcelFileUtil.addJobHistory(importJob, truncationInfo.toString());
		}

	}

	protected void resetValues(Map<String, ColumnMetadata> columns) {
		for (Map.Entry<String, ColumnMetadata> entry : columns.entrySet()) {
			entry.getValue().setValue(null);
		}
	}
}
