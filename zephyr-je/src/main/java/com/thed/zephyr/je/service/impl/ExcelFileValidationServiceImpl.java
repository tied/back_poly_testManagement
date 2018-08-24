package com.thed.zephyr.je.service.impl;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.thed.zephyr.je.service.ExcelFileValidationService;
import com.thed.zephyr.je.vo.ImportDetails;
import com.thed.zephyr.je.vo.ImportFieldConfig;
import com.thed.zephyr.je.vo.ImportFieldMapping;
import com.thed.zephyr.je.vo.ImportJob;
import com.thed.zephyr.util.ExcelFileUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.record.RecordFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.thed.zephyr.util.ApplicationConstants.EXTRA_ROWS_IN_END;
import static com.thed.zephyr.util.ApplicationConstants.IMPORT_JOB_NORMALIZATION_FAILED;

public class ExcelFileValidationServiceImpl implements ExcelFileValidationService {
	private static final Logger log = LoggerFactory.getLogger(ExcelFileValidationServiceImpl.class);
	private JiraAuthenticationContext authContext;
	public ExcelFileValidationServiceImpl(JiraAuthenticationContext authContext){
		this.authContext = authContext;
	}
	@Override
	public boolean isValidRequiredMapping(ImportJob importJob) {
		boolean isValidMapping = true;
		//if(StringUtils.isEmpty(importJob.getIssueKey())) {
			StringBuffer missedFieldsStrBuf = new StringBuffer();
			ImportDetails importDetails = importJob.getImportDetails();
			Map<String, ImportFieldConfig> fieldConfigMap = importJob.getFieldConfigMap();
			Set<ImportFieldMapping> mappingSet = importDetails.getFieldMappingSet();
			Map<String, String> mappingMap = new HashMap<String, String>();
			for (ImportFieldMapping mapping : mappingSet) {
				mappingMap.put(mapping.getZephyrField(), mapping.getMappedField());
			}
			for(Map.Entry<String, ImportFieldConfig> entry:fieldConfigMap.entrySet()) {
				String fieldName = entry.getKey();
				//if(entry.getValue().getMandatory() && !isValidMapping(fileMapping.get(mappingMap.get(fieldName)))) {
				if(entry.getValue().getMandatory() && !isValidMapping(mappingMap.get(fieldName))) {
					missedFieldsStrBuf.append(fieldName + ", ");
				}
			}
			if(!isValidMapping) {
				ExcelFileUtil.addJobHistory(
						importJob,
						"The following required fields are missing :"
								+ missedFieldsStrBuf.replace(missedFieldsStrBuf.length() - 2,
										missedFieldsStrBuf.length(), "."));
			}
		//}
		return isValidMapping;
	}
	@Override
	public boolean isValidRequiredXMLMapping(ImportJob importJob) {
		boolean isValidMapping = true;
		StringBuffer missedFieldsStrBuf = new StringBuffer();
		ImportDetails importDetails = importJob.getImportDetails();
		Map<String, ImportFieldConfig> fieldConfigMap = importJob.getFieldConfigMap();
		Set<ImportFieldMapping> mappingSet = importDetails.getFieldMappingSet();
		Map<String, String> mappingMap = new HashMap<String, String>();
		for (ImportFieldMapping mapping : mappingSet) {
			mappingMap.put(mapping.getZephyrField(), mapping.getMappedField());
		}
		for(Map.Entry<String, ImportFieldConfig> entry:fieldConfigMap.entrySet()) {
			String fieldName = entry.getKey();
			//if(entry.getValue().getMandatory() && !isValidMapping(fileMapping.get(mappingMap.get(fieldName)))) {
			if(entry.getValue().getMandatory() && StringUtils.isEmpty(mappingMap.get(fieldName))) {
				missedFieldsStrBuf.append(fieldName + ", ");
			}
		}
		if(!isValidMapping) {
			ExcelFileUtil.addJobHistory(
					importJob,
					"The following required fields are missing :"
							+ missedFieldsStrBuf.replace(missedFieldsStrBuf.length() - 2,
									missedFieldsStrBuf.length(), "."));
		}
		return isValidMapping;
	}
	/*@Override
	public boolean isValidRequiredMapping(ImportJob importJob) {
		boolean isValidMapping = false;
		StringBuffer missedFieldsStrBuf = null;

		ImportDetails importDetails = importJob.getImportDetails();
		Set<ImportFieldMapping> mappingSet = importDetails.getFieldMappingSet();
		String nameMapping = null, stepsMapping = null, resultsMapping = null, externalIdMapping = null;
		for (ImportFieldMapping mapping : mappingSet) {

			if (ZephyrFieldEnum.NAME
					.equalsIgnoreCase(mapping.getZephyrField())) {
				nameMapping = mapping.getMappedField();
			} else if (ZephyrFieldEnum.STEPS.equalsIgnoreCase(mapping
					.getZephyrField())) {
				stepsMapping = mapping.getMappedField();
			} else if (ZephyrFieldEnum.RESULT.equalsIgnoreCase(mapping
					.getZephyrField())) {
				resultsMapping = mapping.getMappedField();
			} else if (ZephyrFieldEnum.EXTERNAL_ID.equalsIgnoreCase(mapping
					.getZephyrField())) {
				externalIdMapping = mapping.getMappedField();
			}
		}// for end
		if (importDetails.getDiscriminator() == BY_ID_CHANGE) {
			if (isValidMapping(nameMapping) && isValidMapping(stepsMapping) && isValidMapping(resultsMapping)
					&& isValidMapping(externalIdMapping)) {
				isValidMapping = true;
			}
		}
		if (importDetails.getDiscriminator() == BY_EMPTY_ROW
				|| importDetails.getDiscriminator() == BY_SHEET
				|| importDetails.getDiscriminator() == BY_TESTCASE_NAME_CHANGE) {
			if (isValidMapping(nameMapping) && isValidMapping(stepsMapping) && isValidMapping(resultsMapping)) {
				isValidMapping = true;
			}
		}
		if (!isValidMapping) { // for jobHistory purpose
			missedFieldsStrBuf = new StringBuffer();
			if (importDetails.getDiscriminator() == BY_ID_CHANGE) {
				if (!isValidMapping(nameMapping)) {
					missedFieldsStrBuf.append(" Name,");
				}
				if (!isValidMapping(stepsMapping)) {
					missedFieldsStrBuf.append(" Step,");
				}
				if (!isValidMapping(resultsMapping)) {
					missedFieldsStrBuf.append(" Result,");
				}
				if (!isValidMapping(externalIdMapping)) {
					missedFieldsStrBuf.append(" External Id,");
				}
			}
			if (importDetails.getDiscriminator()  == BY_EMPTY_ROW
					|| importDetails.getDiscriminator() == BY_SHEET
					|| importDetails.getDiscriminator() == BY_TESTCASE_NAME_CHANGE) {
				missedFieldsStrBuf = new StringBuffer();
				if (!isValidMapping(nameMapping)) {
					missedFieldsStrBuf.append(" Name,");
				}
				if (!isValidMapping(stepsMapping)) {
					missedFieldsStrBuf.append(" Step,");
				}
				if (!isValidMapping(resultsMapping)) {
					missedFieldsStrBuf.append(" Result,");
				}
			}

			ExcelFileUtil.addJobHistory(
					importJob,
					"The following required fields are missing :"
							+ missedFieldsStrBuf.replace(missedFieldsStrBuf.length() - 1,
									missedFieldsStrBuf.length(), "."));
		}

		return isValidMapping;
	}*/

	@Override
	public boolean isValidData(ImportJob importJob, File file, Map<String, String> fileMapping, List<Sheet> sheets) throws Exception {
		boolean isValidData = true;
		//if(StringUtils.isEmpty(importJob.getIssueKey())){
//			InputStream fis = FileUtils.openInputStream(file);
			//int oldJobHistorySize = importJob.getHistory().size();
			ImportDetails importDetails = importJob.getImportDetails();
			Map<String, ImportFieldConfig> fieldConfigMap = importJob.getFieldConfigMap();
			Set<ImportFieldMapping> mappingSet = importDetails.getFieldMappingSet();
			Map<String, String> mappingMap = new HashMap<String, String>();
			for (ImportFieldMapping mapping : mappingSet) {
				mappingMap.put(mapping.getZephyrField(), mapping.getMappedField());
			}
			try {

//				Workbook wb = WorkbookFactory.create(fis);
				for(Sheet sheet : sheets ) {
//					FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
//					sheet.setDisplayGridlines(false);
					int lastRow = sheet.getLastRowNum() + EXTRA_ROWS_IN_END;
					int startPoint = importDetails.getStartingRowNumber() - 1;
					String uniqueId = "";
					boolean isLastRow = false;
					for (int i = startPoint; i < lastRow; i++) {

						Row row = sheet.getRow(i);
						if (ExcelFileUtil.isRowNull(row)) {
							continue;
						}

						for(Map.Entry<String, ImportFieldConfig> entry:fieldConfigMap.entrySet()) {
							String fieldName = entry.getKey();

							String mappedField = mappingMap.get(fieldName);
							if(entry.getValue().getMandatory() ) {
								String value = ExcelFileUtil.getCellValue(mappedField, sheet, row);
								if(StringUtils.isEmpty(value)) {
									ExcelFileUtil.addJobHistoryAndUpdateStatus(importJob,
											IMPORT_JOB_NORMALIZATION_FAILED, authContext.getI18nHelper().getText("zephyr-je.pdb.importer.issueimport.column.value.empty", fieldName));
									isValidData = false;
									break;
								}
							}
						}
						if (i == (lastRow - 2) || !isValidData) {
							break;
						}
					}
					if(!isValidData){
						break;
					}
				}

			} catch (RecordFormatException e) {
				String msg = ((file != null) ? (file.getName()) : (""))
						+ " Records contain invalid format/data";
				ExcelFileUtil.addJobHistoryAndUpdateStatus(importJob,
						IMPORT_JOB_NORMALIZATION_FAILED, msg);
				log.error("Exception during excel file data validation", e);
				return false;
			} catch (Exception e) {
				ExcelFileUtil.addJobHistoryAndUpdateStatus(importJob,
						IMPORT_JOB_NORMALIZATION_FAILED, e.getMessage());
				log.error("Exception during excel file data validation", e);
				return false;
			} finally {
//				try {
//					fis.close();
//				} catch (Exception e) {
//					ExcelFileUtil.addJobHistoryAndUpdateStatus(importJob,
//							IMPORT_JOB_NORMALIZATION_FAILED, e.getMessage());
//					log.error("Exception during file input stream closing", e);
//					return false;
//				}
			}
		//}
		return isValidData;
	}

	/*@Override
	public boolean validateFileByNameChange(File file, ImportJob importJob)
			throws IOException {
		InputStream fis = FileUtils.openInputStream(file);
		int oldJobHistorySize = importJob.getHistory().size();
		try {
			ImportDetails importDetails = importJob.getImportDetails();
			Workbook wb = WorkbookFactory.create(fis);
			for(Sheet sheet : SheetIterator.create(wb, importJob) ) {
				FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
				sheet.setDisplayGridlines(false);
				int lastRow = sheet.getLastRowNum() + EXTRA_ROWS_IN_END;
				Set<ImportFieldMapping> mappingSet = importDetails.getFieldMappingSet();
				String nameMapping = null, stepsMapping = null, resultsMapping = null;
				for (ImportFieldMapping mapping : mappingSet) {
					if (ZephyrFieldEnum.NAME.equalsIgnoreCase(mapping.getZephyrField())) {
						nameMapping = mapping.getMappedField();
					} else if (ZephyrFieldEnum.STEPS.equalsIgnoreCase(mapping.getZephyrField())) {
						stepsMapping = mapping.getMappedField();
					} else if (ZephyrFieldEnum.RESULT.equalsIgnoreCase(mapping.getZephyrField())) {
						resultsMapping = mapping.getMappedField();
					} 
				}

				boolean isNameExist = false, isStepsExist = false, isExpectedresultsExist = false, blockStart = false, isTestcaseNameExist = false, isLastRow = false;
				int startPoint = importDetails.getStartingRowNumber() - 1;
				String uniqueId = "";

				for (int i = startPoint; i < lastRow; i++) {
					blockStart = false;
					Row row = sheet.getRow(i);
					if (ExcelFileUtil.isRowNull(row)) {
						continue;
					}
					if (i == (lastRow - 2)) {
						isLastRow = true;
					}
					if (i == startPoint) {
						uniqueId = ExcelFileUtil.getCellValue(nameMapping, sheet, row, evaluator);
					}
					if (ExcelFileUtil.getCellValue(nameMapping, sheet, row, evaluator) != null
							&& uniqueId != null
							&& !(uniqueId.equalsIgnoreCase(ExcelFileUtil.getCellValue(nameMapping, sheet, row, evaluator)))) {
						uniqueId = ExcelFileUtil.getCellValue(nameMapping, sheet, row, evaluator);
						blockStart = true;
					}
					isNameExist = false;
					isStepsExist = false;
					isExpectedresultsExist = false;
					String nameValue = null;
					String stepValue = null;
					String resultValue = null;

					if (blockStart && !isTestcaseNameExist) {
						ExcelFileUtil.addJobHistory(importJob, file.getName()
								+ " Testcase name not exists at '" + (i + 1) + "' row");
					}

					if (blockStart) { // After next block start
						isTestcaseNameExist = false;
					}

					nameValue = ExcelFileUtil.getCellValue(nameMapping, sheet, row, evaluator);
					stepValue = ExcelFileUtil.getCellValue(stepsMapping, sheet, row, evaluator);
					resultValue = ExcelFileUtil.getCellValue(resultsMapping, sheet, row, evaluator);

					if (nameValue != null && !"".equalsIgnoreCase(nameValue)) {
						isNameExist = true;
						if (isNameExist) {
							isTestcaseNameExist = isTestcaseNameExist | isNameExist;
						}
					}
					if (stepValue != null && !"".equalsIgnoreCase(stepValue)) {
						isStepsExist = true;
					}
					if (resultValue != null && !"".equalsIgnoreCase(resultValue)) {
						isExpectedresultsExist = true;
					}
					if (isExpectedresultsExist && !isStepsExist) {
						ExcelFileUtil.addJobHistory(importJob, file.getName() + " Invalid at " + (i + 1)
								+ " Result without step");
					}
					if (isLastRow && !isTestcaseNameExist) { // if lastRow check for
						// testCase exists
						ExcelFileUtil.addJobHistory(importJob, file.getName()
								+ " Testcase name not exists at '" + (i + 1) + "' row");
					}
				}

			}
			if (importJob.getHistory().size() > oldJobHistorySize) {

				ExcelFileUtil.addJobHistory(importJob, file.getName() + " normalization failed..!");
				return false;
			} else {
				return true;
			}
		} catch (RecordFormatException e) {
			String msg = ((file != null) ? (file.getName()) : (""))
					+ " Records contain invalid format/data";
			ExcelFileUtil.addJobHistoryAndUpdateStatus(importJob,
					Constants.IMPORT_JOB_NORMALIZATION_FAILED, msg);
			log.error("", e);
			return false;
		} catch (Exception e) {
			ExcelFileUtil.addJobHistoryAndUpdateStatus(importJob,
					Constants.IMPORT_JOB_NORMALIZATION_FAILED, e.getMessage());
			log.error("", e);
			return false;
		} finally {
			try {
				fis.close();
			} catch (Exception e) {
				ExcelFileUtil.addJobHistoryAndUpdateStatus(importJob,
						Constants.IMPORT_JOB_NORMALIZATION_FAILED, e.getMessage());
				log.error("", e);
				return false;
			}
		}
	}

	@Override
	public boolean validateFileByIdChange(File file, ImportJob importJob)
			throws IOException {

		InputStream fis = FileUtils.openInputStream(file);

		if (importJob.getHistory() == null) {
			throw new IllegalStateException("Histories must not be null");
		}
		int oldJobHistorySize = importJob.getHistory().size();
		try {

			ImportDetails importDetails = importJob.getImportDetails();
			Workbook wb = WorkbookFactory.create(fis);
			for(Sheet sheet : SheetIterator.create(wb, importJob) ) {
				FormulaEvaluator evaluator = wb.getCreationHelper()
						.createFormulaEvaluator();// As a best practice, evaluator should be
				// one per sheet

				sheet.setDisplayGridlines(false);

				int lastRow = sheet.getLastRowNum() + EXTRA_ROWS_IN_END;
				Set<ImportFieldMapping> mappingSet = importDetails.getFieldMappingSet();

				String nameMapping = null, stepsMapping = null, resultsMapping = null, externalIdMapping = null;
				for (ImportFieldMapping mapping : mappingSet) {
					if (ZephyrFieldEnum.NAME.equalsIgnoreCase(mapping
							.getZephyrField())) {
						nameMapping = mapping.getMappedField();
					} else if (ZephyrFieldEnum.STEPS.equalsIgnoreCase(mapping
							.getZephyrField())) {
						stepsMapping = mapping.getMappedField();
					} else if (ZephyrFieldEnum.RESULT.equalsIgnoreCase(mapping
							.getZephyrField())) {
						resultsMapping = mapping.getMappedField();
					} else if (ZephyrFieldEnum.EXTERNAL_ID.equalsIgnoreCase(mapping
							.getZephyrField())) {
						externalIdMapping = mapping.getMappedField();
					}
				}// for end

				boolean isStepsExist = false, isExpectedresultsExist = false, blockStart = false, isTestcaseNameExist = false;
				int startPoint = importDetails.getStartingRowNumber() - 1;
				String uniqueId = new String();

				int i;
				for (i = startPoint; i < lastRow; i++) {
					blockStart = false;
					Row row = sheet.getRow(i);
					if (ExcelFileUtil.isRowNull(row)) {
						continue;
					}
					if (i == startPoint) {// || blockStart==true ){
						uniqueId = ExcelFileUtil.getCellValue(externalIdMapping, sheet, row, evaluator);
					}
					{
						String tempId = ExcelFileUtil.getCellValue(externalIdMapping, sheet, row, evaluator);
						if (!StringUtils.equalsIgnoreCase(tempId, uniqueId)) {

							if (!isTestcaseNameExist) {
								ExcelFileUtil.addJobHistory(importJob, file.getName()
										+ " Testcase name not exists in '" + uniqueId + "' block");
							}
							if (uniqueId == null || uniqueId == "") {
								ExcelFileUtil.addJobHistory(importJob, file.getName()
										+ " external Identifier not exists in row(s)' " + i
										+ "' and/or above"); // one row before the current row.
							}
							// reset the flag
							isTestcaseNameExist = false;

							uniqueId = tempId;
							// In some cases, this can cause two error rows for the same block
							// (when a empty extId block exists in the middle of the filled
							// blocks
							if (uniqueId == null || uniqueId == "") {
								ExcelFileUtil.addJobHistory(importJob, file.getName()
										+ " external Identifier not exists in '" + (i + 1)
										+ "th' row");
							}
							blockStart = true;
						}
					}
					isStepsExist = false;
					isExpectedresultsExist = false;

					String nameValue = null;
					String stepValue = null;
					String resultValue = null;

					nameValue = ExcelFileUtil.getCellValue(nameMapping, sheet, row, evaluator);
					stepValue = ExcelFileUtil.getCellValue(stepsMapping, sheet, row, evaluator);
					resultValue = ExcelFileUtil.getCellValue(resultsMapping, sheet, row, evaluator);

					if (nameValue != null && !"".equalsIgnoreCase(nameValue)) {
						isTestcaseNameExist = isTestcaseNameExist | true;
					}
					if (stepValue != null && !"".equalsIgnoreCase(stepValue)) {
						isStepsExist = true;
					}
					if (resultValue != null && !"".equalsIgnoreCase(resultValue)) {
						isExpectedresultsExist = true;
					}
					if (isExpectedresultsExist && !isStepsExist) {
						ExcelFileUtil.addJobHistory(importJob, file.getName() + " Invalid at " + (i + 1)
								+ " Result without step");
					}
				}// for end
				// lastRow > 2 means that there is at least one row.
				if (!blockStart && lastRow > 2) {
					if (uniqueId == null || uniqueId == "") {
						ExcelFileUtil.addJobHistory(importJob, file.getName()
								+ " external Identifier not exists in '" + i + "' row");
					}
				}
				// Checking the lastBlock for testCase ExtId and name validity
				if (!isTestcaseNameExist) {
					ExcelFileUtil.addJobHistory(importJob, file.getName()
							+ " Testcase name not exists in '" + uniqueId + "' block");
				}// end of processing
			}
			if (importJob.getHistory().size() > oldJobHistorySize) {
				ExcelFileUtil.addJobHistory(importJob, file.getName() + " normalization failed..!");
				return false;
			} else {
				return true;
			}
		} catch (RecordFormatException e) {
			String msg = ((file != null) ? (file.getName()) : (""))
					+ " Records contain invalid format/data";
			ExcelFileUtil.addJobHistoryAndUpdateStatus(importJob,
					Constants.IMPORT_JOB_NORMALIZATION_FAILED, msg);
			log.error("", e);
			return false;
		} catch (Exception e) {
			ExcelFileUtil.addJobHistoryAndUpdateStatus(importJob,
					Constants.IMPORT_JOB_NORMALIZATION_FAILED, e.getMessage());
			log.error("", e);
			return false;
		} finally {
			try {
				fis.close();
			} catch (Exception e) {
				ExcelFileUtil.addJobHistoryAndUpdateStatus(importJob,
						Constants.IMPORT_JOB_NORMALIZATION_FAILED, e.getMessage());
				log.error("", e);
				return false;
			}
		}
	}

	@Override
	public boolean validateFileByEmptyRow(File file, ImportJob importJob,
			boolean stopAfterFirst) throws IOException {

		InputStream fis = FileUtils.openInputStream(file);


		try {

			ImportDetails importDetails = importJob.getImportDetails();
			Workbook wb =null;
			try {
				wb = WorkbookFactory.create(fis);

			} catch(IOException ex) {
				ex.printStackTrace();
			} catch(InvalidFormatException ex) {
				ex.printStackTrace();
			} catch(EncryptedDocumentException ex) {
				ex.printStackTrace();
			}
			for(Sheet sheet : SheetIterator.create(wb, importJob) ) {
				FormulaEvaluator evaluator = wb.getCreationHelper()
						.createFormulaEvaluator();// As a best practice, evaluator should be
				// one per sheet

				sheet.setDisplayGridlines(false);


				// Start processing the sheet
				Set<ImportFieldMapping> mappingSet = importDetails.getFieldMappingSet();

				String nameMapping = null, stepsMapping = null, resultsMapping = null;
				for (ImportFieldMapping mapping : mappingSet) {
					if (ZephyrFieldEnum.NAME.equalsIgnoreCase(mapping
							.getZephyrField())) {
						nameMapping = mapping.getMappedField();
					} else if (ZephyrFieldEnum.STEPS.equalsIgnoreCase(mapping
							.getZephyrField())) {
						stepsMapping = mapping.getMappedField();
					} else if (ZephyrFieldEnum.RESULT.equalsIgnoreCase(mapping
							.getZephyrField())) {
						resultsMapping = mapping.getMappedField();
					}
				}

				boolean isNameExist = false, isStepsExist = false, isExpectedresultsExist = false, isSkip = false, blockFlag = false;
				int startPoint = importDetails.getStartingRowNumber() - 1;

				for (int i = startPoint; i < lastRow; i++) {
					Row row = sheet.getRow(i);
					if (ExcelFileUtil.isRowNull(row)) {
						if (!blockFlag) {
							if (!(isNameExist && isStepsExist && isExpectedresultsExist)) {
								// invalidList.add(startPoint); //here startPoint is invalid
								// testcase row no.
								if (!isNameExist) {
									ExcelFileUtil.addJobHistory(importJob, file.getName()
											+ ", testcase name not exists at " + (startPoint + 1)
											+ " row");
								}
							}
							if(stopAfterFirst) {
								break;
							}
						}
						isNameExist = false;
						isStepsExist = false;
						isExpectedresultsExist = false;
						isSkip = false;
						startPoint = i + 1;
						blockFlag = true;
					} else if (isSkip) {
						continue;
					} else {
						blockFlag = false;
						String nameValue = null;
						String stepValue = null;
						String resultValue = null;

						nameValue = ExcelFileUtil.getCellValue(nameMapping, sheet, row, evaluator);
						stepValue = ExcelFileUtil.getCellValue(stepsMapping, sheet, row, evaluator);
						resultValue = ExcelFileUtil.getCellValue(resultsMapping, sheet, row, evaluator);

						if (nameValue != null && !"".equalsIgnoreCase(nameValue)) {
							isNameExist = true;
						}
						if (stepValue != null && !"".equalsIgnoreCase(stepValue)
								&& resultValue != null && !"".equalsIgnoreCase(resultValue)) {
							isStepsExist = true;
							isExpectedresultsExist = true;
						}
						if (stepValue != null && !"".equalsIgnoreCase(stepValue)) {
							isStepsExist = true;
							isExpectedresultsExist = true;
						} else {
							if (resultValue != null && !"".equalsIgnoreCase(resultValue)) {
								isStepsExist = false;
								isExpectedresultsExist = false;
								ExcelFileUtil.addJobHistory(importJob, file.getName()
										+ ", Result exists without Step at " + (startPoint + 1)
										+ " row");
								isSkip = true;
							}
						}
					}
				}// for end
				// end of processing
			}


			return true;

		} catch (RecordFormatException e) {
			String msg = ((file != null) ? (file.getName()) : (""))
					+ " Records contain invalid format/data";
			ExcelFileUtil.addJobHistoryAndUpdateStatus(importJob,
					Constants.IMPORT_JOB_NORMALIZATION_FAILED, msg);
			log.error("", e);
			return false;
		} catch (Exception e) {
			ExcelFileUtil.addJobHistoryAndUpdateStatus(importJob,
					Constants.IMPORT_JOB_NORMALIZATION_FAILED, e.getMessage());
			log.error("", e);
			return false;
		} finally {
			try {
				fis.close();
			} catch (Exception e) {
				ExcelFileUtil.addJobHistoryAndUpdateStatus(importJob,
						Constants.IMPORT_JOB_NORMALIZATION_FAILED, e.getMessage());
				log.error("", e);
				return false;
			}
		}
	}


	@Override
	public String getMissingFields(ImportJob importJob) {
		ImportDetails importDetails = importJob.getImportDetails();
		StringBuffer fieldsStr = new StringBuffer();

		Set<ImportFieldMapping> mappingSet = importDetails.getFieldMappingSet();
		String nameMapping = null, stepsMapping = null, resultsMapping = null, externalIdMapping = null;
		for (ImportFieldMapping mapping : mappingSet) {

			if (ZephyrFieldEnum.NAME
					.equalsIgnoreCase(mapping.getZephyrField())) {
				nameMapping = mapping.getMappedField();
			} else if (ZephyrFieldEnum.STEPS.equalsIgnoreCase(mapping
					.getZephyrField())) {
				stepsMapping = mapping.getMappedField();
			} else if (ZephyrFieldEnum.RESULT.equalsIgnoreCase(mapping
					.getZephyrField())) {
				resultsMapping = mapping.getMappedField();
			} else if (ZephyrFieldEnum.EXTERNAL_ID.equalsIgnoreCase(mapping
					.getZephyrField())) {
				externalIdMapping = mapping.getMappedField();
			}
		}
		if (importDetails.getDiscriminator() == BY_ID_CHANGE) {
			if (isValidMapping(nameMapping)) {
				fieldsStr.append(" Name,");
			}
			if (isValidMapping(stepsMapping)) {
				fieldsStr.append(" Step,");
			}
			if (isValidMapping(resultsMapping)) {
				fieldsStr.append(" Result,");
			}
			if (isValidMapping(externalIdMapping)) {
				fieldsStr.append(" External Id,");
			}
		}
		if (importDetails.getDiscriminator()  == BY_EMPTY_ROW
				|| importDetails.getDiscriminator() == BY_SHEET
				|| importDetails.getDiscriminator() == BY_TESTCASE_NAME_CHANGE) {
			fieldsStr = new StringBuffer();
			if (isValidMapping(nameMapping)) {
				fieldsStr.append(" Name,");
			}
			if (isValidMapping(stepsMapping)) {
				fieldsStr.append(" Step,");
			}
			if (isValidMapping(resultsMapping)) {
				fieldsStr.append(" Result,");
			}
		}
		return (fieldsStr != null ? fieldsStr.toString() : null );
	}
	 */

	@Override
	public boolean isValidMapping(String cellMapping) {
		return ExcelFileUtil.isStaticMapping(cellMapping) || ExcelFileUtil.convertField(cellMapping) != null;
	}

}
