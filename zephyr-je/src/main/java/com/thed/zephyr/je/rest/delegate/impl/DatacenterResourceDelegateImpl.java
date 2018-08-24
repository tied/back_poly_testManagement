package com.thed.zephyr.je.rest.delegate.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URLEncoder;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.map.ObjectMapper;

import com.atlassian.beehive.compat.ClusterLock;
import com.atlassian.beehive.compat.ClusterLockService;
import com.atlassian.beehive.compat.ClusterLockServiceFactory;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.util.IndexPathManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.PathUtils;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.query.Query;
import com.atlassian.sal.api.ApplicationProperties;
import com.thed.zephyr.je.attachment.AttachmentUtils;
import com.thed.zephyr.je.helper.ScheduleSearchResourceHelper;
import com.thed.zephyr.je.index.ScheduleIndexManager;
import com.thed.zephyr.je.index.cluster.ClusterProperties;
import com.thed.zephyr.je.index.cluster.CronSyncupSchedulerService;
import com.thed.zephyr.je.rest.delegate.DatacenterResourceDelegate;
import com.thed.zephyr.je.rest.delegate.ScheduleResourceDelegate;
import com.thed.zephyr.je.service.CycleManager;
import com.thed.zephyr.je.service.DatacenterManager;
import com.thed.zephyr.je.service.ScheduleManager;
import com.thed.zephyr.je.vo.DataCenterStatus;
import com.thed.zephyr.je.vo.RecoveryFormBean;
import com.thed.zephyr.je.zql.core.SearchService;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.ZephyrCacheControl;
import com.thed.zephyr.util.ZephyrComponentAccessor;

/**
 * @author manjunath
 *
 */
public class DatacenterResourceDelegateImpl implements DatacenterResourceDelegate {
	
	private static final String ERROR_LOG_MESSAGE = "[Error] [Error code: %s %s Error Message : %s]";
	protected final Logger log = Logger.getLogger(DatacenterResourceDelegateImpl.class);
	private static final String  DATACENTER_STATUS = "datacenter_status";
	
	private final JiraAuthenticationContext authContext;
	private final ScheduleManager scheduleManager;
	private final CycleManager cycleManager;
	private final SearchService searchService;
	private final DatacenterManager datacenterManager;
	private final CronSyncupSchedulerService cronSyncupSchedulerService;
	private final ScheduleIndexManager scheduleIndexManager;
	private final ScheduleResourceDelegate scheduleResourceDelegate;
	private final ClusterLockService clusterLockService;
	private final IndexPathManager indexPathManager;
	private final ClusterProperties clusterProperties;
	
	public DatacenterResourceDelegateImpl(JiraAuthenticationContext authContext, ScheduleManager scheduleManager,
			CycleManager cycleManager, SearchService searchService, DatacenterManager datacenterManager,
			CronSyncupSchedulerService cronSyncupSchedulerService, ScheduleIndexManager scheduleIndexManager,
			ScheduleResourceDelegate scheduleResourceDelegate, ClusterLockServiceFactory clusterLockServiceFactory,
			IndexPathManager indexPathManager, ClusterProperties clusterProperties) {
		this.authContext = authContext;
		this.scheduleManager = scheduleManager;
		this.cycleManager = cycleManager;
		this.searchService = searchService;
		this.datacenterManager = datacenterManager;
		this.cronSyncupSchedulerService = cronSyncupSchedulerService;
		this.scheduleIndexManager = scheduleIndexManager;
		this.scheduleResourceDelegate = scheduleResourceDelegate;
		this.clusterLockService = clusterLockServiceFactory.getClusterLockService();
		this.indexPathManager = indexPathManager;
		this.clusterProperties = clusterProperties;
	}
	
	@Override
	public JSONObject getDataForIntegrityCheck(boolean isTotalExecutionCount, boolean isTotalCycleCount,
			boolean isExecutionCountByCycle, boolean isExecutionCountByFolder, boolean isIssueCountByProject,
			boolean isTeststepResultCountByExecution, boolean isTeststepCountByIssue, Integer offset, Integer limit) throws Exception {
		JSONObject resultMap  = new JSONObject();
		List<Map<String, Object>> entityCountDtoList = null;
		Integer count = new Integer(0);
		if(isTotalExecutionCount) {
			count = scheduleManager.getTotalSchedulesCount();
			resultMap.put(ApplicationConstants.ZIC_TOTAL_EXECUTION_COUNT_DB, count);
			
			ScheduleSearchResourceHelper searchResourceHelper = new ScheduleSearchResourceHelper(authContext.getLoggedInUser(), searchService);
            Query countQuery = searchResourceHelper.getNewSearchQuery("").getQuery();
            long searchScheduledCount = searchService.searchCount(authContext.getLoggedInUser(), countQuery);
			resultMap.put(ApplicationConstants.ZIC_TOTAL_EXECUTION_COUNT, searchScheduledCount);
			addTabNameForActive(resultMap, ApplicationConstants.TAB_ACTIVE, ApplicationConstants.ZIC_TOTAL_EXECUTION_COUNT);
		}
		if(isTotalCycleCount) {
			count = cycleManager.getTotalCyclesCount();
			resultMap.put(ApplicationConstants.ZIC_TOTAL_CYCLE_COUNT, count);
			addTabNameForActive(resultMap, ApplicationConstants.TAB_ACTIVE, ApplicationConstants.ZIC_TOTAL_CYCLE_COUNT);
		}
		Map<String, Integer> totalCountsMap = datacenterManager.getTotalCountForAllQueries();
		JSONObject tempResultMap  = null;
		if(isExecutionCountByCycle) {
			entityCountDtoList = datacenterManager.getExecutionCountByCycle(offset, limit);
			tempResultMap = new JSONObject();
			tempResultMap.put(ApplicationConstants.TOTAL_COUNT, totalCountsMap.get(ApplicationConstants.ZIC_EXECUTION_COUNT_BY_CYCLE));
			tempResultMap.put(ApplicationConstants.DATA, entityCountDtoList);
			resultMap.put(ApplicationConstants.ZIC_EXECUTION_COUNT_BY_CYCLE, tempResultMap);
			addTabNameForActive(resultMap, ApplicationConstants.TAB_ACTIVE, ApplicationConstants.ZIC_EXECUTION_COUNT_BY_CYCLE);
		}
		if(isExecutionCountByFolder) {
			entityCountDtoList = datacenterManager.getExecutionCountByFolder(offset, limit);
			tempResultMap = new JSONObject();
			tempResultMap.put(ApplicationConstants.TOTAL_COUNT, totalCountsMap.get(ApplicationConstants.ZIC_EXECUTION_COUNT_BY_FOLDER));
			tempResultMap.put(ApplicationConstants.DATA, entityCountDtoList);
			resultMap.put(ApplicationConstants.ZIC_EXECUTION_COUNT_BY_FOLDER, tempResultMap);
			addTabNameForActive(resultMap, ApplicationConstants.TAB_ACTIVE, ApplicationConstants.ZIC_EXECUTION_COUNT_BY_FOLDER);
		}
		if(isIssueCountByProject) {
			entityCountDtoList = datacenterManager.getIssueCountByProject(offset, limit);
			tempResultMap = new JSONObject();
			tempResultMap.put(ApplicationConstants.TOTAL_COUNT, totalCountsMap.get(ApplicationConstants.ZIC_ISSUE_COUNT_BY_PROJECT));
			tempResultMap.put(ApplicationConstants.DATA, entityCountDtoList);
			resultMap.put(ApplicationConstants.ZIC_ISSUE_COUNT_BY_PROJECT, tempResultMap);
			addTabNameForActive(resultMap, ApplicationConstants.TAB_ACTIVE, ApplicationConstants.ZIC_ISSUE_COUNT_BY_PROJECT);
		}
		if(isTeststepResultCountByExecution) {
			entityCountDtoList = datacenterManager.getTeststepResultCountByExecution(offset, limit);
			tempResultMap = new JSONObject();
			tempResultMap.put(ApplicationConstants.TOTAL_COUNT, totalCountsMap.get(ApplicationConstants.ZIC_TESTSTEP_RESULT_COUNT_BY_EXECUTION));
			tempResultMap.put(ApplicationConstants.DATA, entityCountDtoList);
			resultMap.put(ApplicationConstants.ZIC_TESTSTEP_RESULT_COUNT_BY_EXECUTION, tempResultMap);
			addTabNameForActive(resultMap, ApplicationConstants.TAB_ACTIVE, ApplicationConstants.ZIC_TESTSTEP_RESULT_COUNT_BY_EXECUTION);
		}
		if(isTeststepCountByIssue) {
			entityCountDtoList = datacenterManager.getTeststepCountByIssue(offset, limit);
			tempResultMap = new JSONObject();
			tempResultMap.put(ApplicationConstants.TOTAL_COUNT, totalCountsMap.get(ApplicationConstants.ZIC_TESTSTEP_COUNT_BY_ISSUE));
			tempResultMap.put(ApplicationConstants.DATA, entityCountDtoList);
			resultMap.put(ApplicationConstants.ZIC_TESTSTEP_COUNT_BY_ISSUE, tempResultMap);
			addTabNameForActive(resultMap, ApplicationConstants.TAB_ACTIVE, ApplicationConstants.ZIC_TESTSTEP_COUNT_BY_ISSUE);
		}
		return resultMap;
	}
	
	private void addTabNameForActive(JSONObject resultMap, String key, String value) throws JSONException {
		if(!resultMap.has(key)) {
			resultMap.put(key, value);
		}
	}
	 
	@Override
	public Response indexAllUsingCron(String expression, boolean flag,String userId)
			throws Exception {
		JSONObject ob = new JSONObject();
		RecoveryFormBean recoveryFormBean = null;
		if (flag) {
			recoveryFormBean = cronSyncupSchedulerService.schduleWithExpression(expression, userId);
		} else {
			recoveryFormBean = cronSyncupSchedulerService.unSchduleCronJob(userId);
		}
		ob.put("response", recoveryFormBean);
		return Response.ok(ob.toString()).build();
	}
	
	@Override
	public Response recoverIndexBackup(String fullIndexRecoveryFilePath, String indexRecoveryFileName) throws Exception {
		scheduleIndexManager.isZipFilePresentInIndexBackup(fullIndexRecoveryFilePath);
		String strDate = indexRecoveryFileName.substring(ApplicationConstants.INDEX_SNAPSHOT.length());
		SimpleDateFormat sm = new SimpleDateFormat("yyyy-MMM-dd-HH.mm");
		Date backupDate = sm.parse(strDate);
		scheduleIndexManager.deleteScheduleIndexes();
        scheduleIndexManager.unZipScheduleDirectory(fullIndexRecoveryFilePath);
        return scheduleResourceDelegate.indexAll(Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, backupDate);
	}

	@Override
	public Response getrecoveryForm() throws Exception {
		RecoveryFormBean recoveryForm = cronSyncupSchedulerService.getrecoveryForm();
		return Response.ok().entity(recoveryForm).build();
	}

	@Override
	public Response exportIntegrityCheckData(boolean isTotalExecutionCount, boolean isTotalCycleCount,
			boolean isExecutionCountByCycle, boolean isExecutionCountByFolder, boolean isIssueCountByProject,
			boolean isTeststepResultCountByExecution, boolean isTeststepCountByIssue, Integer offset, Integer limit)
			throws Exception {
		JSONObject resultMap = getDataForIntegrityCheck(isTotalExecutionCount, isTotalCycleCount, isExecutionCountByCycle, isExecutionCountByFolder, isIssueCountByProject,
				isTeststepResultCountByExecution, isTeststepCountByIssue, offset, limit);
		SimpleDateFormat sm = new SimpleDateFormat("yyyy-MMM-dd-HH.mm");
		String formattedDate = sm.format(Calendar.getInstance().getTime());
		
		Workbook workBook = new HSSFWorkbook();
		CellStyle headerStyle = workBook.createCellStyle();
		headerStyle.setFillForegroundColor((IndexedColors.GREY_50_PERCENT.getIndex()));
		headerStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
		headerStyle.setBorderBottom(CellStyle.BORDER_THIN);		
		headerStyle.setBorderLeft(CellStyle.BORDER_THIN);
		headerStyle.setBorderRight(CellStyle.BORDER_THIN);
		headerStyle.setBorderTop(CellStyle.BORDER_THIN);
		
		CellStyle borderCellStyle = workBook.createCellStyle();
		borderCellStyle.setBorderBottom(CellStyle.BORDER_THIN);
		borderCellStyle.setBorderLeft(CellStyle.BORDER_THIN);
		borderCellStyle.setBorderRight(CellStyle.BORDER_THIN);
		borderCellStyle.setBorderTop(CellStyle.BORDER_THIN);
        
		//Creating sheet for index count comparsion;
		if(resultMap.has(ApplicationConstants.ZIC_TOTAL_EXECUTION_COUNT) && resultMap.has(ApplicationConstants.ZIC_TOTAL_EXECUTION_COUNT_DB)) {
			Sheet sheet =  workBook.createSheet(ApplicationConstants.IC_EXECUTION_COUNT_AGAINST_DB_COUNT);
			Row row = sheet.createRow(0);
			createHeaderForCell(row.createCell(0), headerStyle, ApplicationConstants.IC_INDEXED_EXECUTION_COUNT);
			createHeaderForCell(row.createCell(1), headerStyle, ApplicationConstants.IC_EXECUTION_COUNT_FROM_DATABASE);
			createHeaderForCell(row.createCell(2), headerStyle, ApplicationConstants.IC_STATUS);
			
			Font redFont = workBook.createFont();
			redFont.setColor(IndexedColors.RED.getIndex());
	        
	        Font greenFont = workBook.createFont();
	        greenFont.setColor(IndexedColors.GREEN.getIndex());
	        
			Row row1 = sheet.createRow(1);
			long searchCount = resultMap.getLong(ApplicationConstants.ZIC_TOTAL_EXECUTION_COUNT);
			long dbCount = resultMap.getLong(ApplicationConstants.ZIC_TOTAL_EXECUTION_COUNT_DB);
			addBorderForLongValueCell(row1.createCell(0), borderCellStyle, searchCount);
			addBorderForLongValueCell(row1.createCell(1), borderCellStyle, dbCount);
			CellStyle statusCellStyle  = workBook.createCellStyle();
			statusCellStyle.cloneStyleFrom(borderCellStyle);
			addBorderForStringValueCell(row1.createCell(2), statusCellStyle, searchCount == dbCount ? "Green" : "Red", searchCount == dbCount ? greenFont : redFont);
		}
		
		//Creating sheet for index count comparsion;
		if(resultMap.has(ApplicationConstants.ZIC_TOTAL_CYCLE_COUNT)) {
			Sheet sheet1 =  workBook.createSheet(ApplicationConstants.IC_CYCLE_COUNT);
			Row row2 = sheet1.createRow(0);
			createHeaderForCell(row2.createCell(0), headerStyle, ApplicationConstants.IC_CYCLES_COUNT);
			Row row3 = sheet1.createRow(1);
			addBorderForLongValueCell(row3.createCell(0), borderCellStyle, resultMap.getLong(ApplicationConstants.ZIC_TOTAL_CYCLE_COUNT));
		}
		
		if(resultMap.has(ApplicationConstants.ZIC_EXECUTION_COUNT_BY_CYCLE)) {
			createSheetAndDataIfExists(workBook, (JSONObject) resultMap.get(ApplicationConstants.ZIC_EXECUTION_COUNT_BY_CYCLE), 
					ApplicationConstants.IC_EXECUTION_COUNT_BY_CYCLE, ApplicationConstants.IC_CYCLE_ID, headerStyle, borderCellStyle);
		}
		
		if(resultMap.has(ApplicationConstants.ZIC_EXECUTION_COUNT_BY_FOLDER)) {
			createSheetAndDataIfExists(workBook, (JSONObject) resultMap.get(ApplicationConstants.ZIC_EXECUTION_COUNT_BY_FOLDER), 
					ApplicationConstants.EXECUTION_COUNT_BY_FOLDER, ApplicationConstants.IC_FOLDER_ID, headerStyle, borderCellStyle);
		}
		
		if(resultMap.has(ApplicationConstants.ZIC_ISSUE_COUNT_BY_PROJECT)) {
			createSheetAndDataIfExists(workBook, (JSONObject) resultMap.get(ApplicationConstants.ZIC_ISSUE_COUNT_BY_PROJECT), 
					ApplicationConstants.IC_ISSUE_COUNT_BY_PROJECT, ApplicationConstants.IC_PROJECT_ID, headerStyle, borderCellStyle);
		}
		
		if(resultMap.has(ApplicationConstants.ZIC_TESTSTEP_RESULT_COUNT_BY_EXECUTION)) {
			createSheetAndDataIfExists(workBook, (JSONObject) resultMap.get(ApplicationConstants.ZIC_TESTSTEP_RESULT_COUNT_BY_EXECUTION),
					ApplicationConstants.IC_TESTSTEP_COUNT_BY_EXECUTION, ApplicationConstants.IC_EXECUTION_ID, headerStyle, borderCellStyle);
		}
		
		if(resultMap.has(ApplicationConstants.ZIC_TESTSTEP_COUNT_BY_ISSUE)) {
			createSheetAndDataIfExists(workBook, (JSONObject) resultMap.get(ApplicationConstants.ZIC_TESTSTEP_COUNT_BY_ISSUE),
					ApplicationConstants.IC_TESTSTEP_COUNT_BY_ISSUE, ApplicationConstants.IC_ISSUE_ID, headerStyle, borderCellStyle);
		}
		
		File file = createAttachment(workBook, ApplicationConstants.INTEGRITY_CHECKER + "-" +  URLEncoder.encode(formattedDate, "UTF-8") + ".xls");
		workBook.close();
		return buildExportResponse(file);
	}
	
	@SuppressWarnings("unchecked")
	private void createSheetAndDataIfExists(Workbook workbook, JSONObject resultData, String sheetName, String columnHeaderName, CellStyle headerStyle, CellStyle borderCellStyle) throws JSONException {
		if(resultData != null && resultData.get(ApplicationConstants.DATA) instanceof List) {
			List<Map<String, Object>> resultDataList = (List<Map<String, Object>>) resultData.get(ApplicationConstants.DATA);
			if(resultDataList != null && resultDataList.size() > 0) {
				int totalSizeCount = resultDataList.size();				
				if(totalSizeCount < SpreadsheetVersion.EXCEL97.getMaxRows()-1) {
					log.info("Data is less than excel sheet supported rows.");
					writeDataFromIndex(workbook, resultDataList, sheetName, columnHeaderName, headerStyle, borderCellStyle, 0, totalSizeCount);
				} else { //splitting the data into different sheets and appending sheet name with numbers.
					int end = SpreadsheetVersion.EXCEL97.getMaxRows() - 1;
					int loopCount = totalSizeCount / end;
					int modDiff = totalSizeCount % end;
					loopCount = loopCount + (modDiff == 0 ? 0 : 1);
					int start = 0;
					for(int i = 0; i < loopCount; i++) {						
						writeDataFromIndex(workbook, resultDataList, sheetName + (i + 1), columnHeaderName, headerStyle, borderCellStyle, start, 
								i == loopCount-1 ? start + (modDiff == 0 ? end : modDiff) : (start + end));
						start = start + end;
					}
				}
			}
		}		
	}
	
	private void writeDataFromIndex(Workbook workbook, List<Map<String, Object>> resultDataList, String sheetName, String columnHeaderName, 
			CellStyle headerStyle, CellStyle borderCellStyle, int start, int end) {
		Sheet sheet = workbook.createSheet(sheetName);
		Row headerRow = sheet.createRow(0);
		createHeaderForCell(headerRow.createCell(0), headerStyle, columnHeaderName);
		createHeaderForCell(headerRow.createCell(1), headerStyle, "Count");
		Row dataRow = null;
		int row = 1;
		for(int i = start; i < end; i++) {
			dataRow = sheet.createRow(row++);
			addBorderForLongValueCell(dataRow.createCell(0), borderCellStyle, (Long)resultDataList.get(i).get("id"));
			addBorderForLongValueCell(dataRow.createCell(1), borderCellStyle, (Long)resultDataList.get(i).get("count"));
		}
	}
	
	private void createHeaderForCell(Cell cell, CellStyle headerStyle, String value) {
		cell.setCellStyle(headerStyle);
		cell.setCellValue(value);
	}
	
	private void addBorderForLongValueCell(Cell cell, CellStyle borderCellStyle, long value) {
		cell.setCellStyle(borderCellStyle);
		cell.setCellValue(value);
	}
	
	private void addBorderForStringValueCell(Cell cell, CellStyle borderCellStyle, String value, Font font) {
		cell.setCellStyle(borderCellStyle);
		borderCellStyle.setFont(font);
		cell.setCellValue(value);
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
	
	private File createAttachment(Workbook workBook, String fileName) {
		final File tmpDir = AttachmentUtils.getTemporaryAttachmentDirectory();
		File tempAttachmentFile = new File(tmpDir, fileName);
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(tempAttachmentFile);
			workBook.write(out);
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

	@Override
	public Response getSupporttoolFiles(Map<String, Boolean> downlaodFilesFlag) throws Exception {

		long startTime = System.nanoTime();
		JSONObject ob = new JSONObject();
		String lockName = ApplicationConstants.SUPPORTTOOL_LOCK;
		final ClusterLock lock = clusterLockService.getLockForName(lockName);

		try {
			File file = null;
			if (lock.tryLock(0, TimeUnit.SECONDS)) {
				try {
					Path sharedPath = Paths.get(clusterProperties.getSharedHome());
					String schedulePath = PathUtils.appendFileSeparator(indexPathManager.getIndexRootPath());
					Path sPath = Paths.get(schedulePath);
					String cachePath = sPath.toString().substring(0, sPath.toString().lastIndexOf(File.separator));
					String nodeRootPath = cachePath.substring(0, cachePath.lastIndexOf(File.separator));
					Path sourceFile = Paths.get(nodeRootPath);
					String zfjSupportPath = sharedPath + File.separator + ApplicationConstants.ZFJ + File.separator
							+ ApplicationConstants.ZFJ_SUPPORT_TOOL + getCurrentLocalDateTimeStamp();
					// Create a final support directory
					createNewDirectory(zfjSupportPath);

					if (downlaodFilesFlag.get(ApplicationConstants.ZFJ_SUPPORT_CHECKLIST[0])) {
						// Create and copy the ZFJ application log files
						Path zfjAppLogPath = Paths
								.get(zfjSupportPath + File.separator + ApplicationConstants.APPLICATION_LOG);
						createNewDirectory(zfjAppLogPath.toString());
						File root = new File(sourceFile.toString() + File.separator + ApplicationConstants.LOG);
						copyFilestoDir(root, zfjAppLogPath.toFile(), ApplicationConstants.ZFJ_APP_LOG);
					}
					if (downlaodFilesFlag.get(ApplicationConstants.ZFJ_SUPPORT_CHECKLIST[1])
							&& downlaodFilesFlag.get(ApplicationConstants.ZFJ_SUPPORT_CHECKLIST[2])) {
						// Create and copy the ZFJ db and shared configurations.
						Path zfjdbSharedPath = Paths.get(zfjSupportPath + File.separator + ApplicationConstants.DB_CLUSTER_CONFIG);
						createNewDirectory(zfjdbSharedPath.toString());
						File dbFile = new File(sourceFile.toString() + File.separator + ApplicationConstants.DB_CONFIG);
						//FileUtils.copyFileToDirectory(dbFile, zfjdbSharedPath.toFile());
						Files.copy(Paths.get(dbFile.toString()),Paths.get(zfjdbSharedPath+File.separator+dbFile.getName()), StandardCopyOption.REPLACE_EXISTING );
						File clusterPropFile = new File(sourceFile.toString() + File.separator + ApplicationConstants.CLUSTER_CONFIG);
						//FileUtils.copyFileToDirectory(clusterPropFile, zfjdbSharedPath.toFile());
						Files.copy(Paths.get(clusterPropFile.toString()),Paths.get(zfjdbSharedPath+File.separator+clusterPropFile.getName()), StandardCopyOption.REPLACE_EXISTING );
						File shaedPathdbFile = new File(zfjdbSharedPath.toFile() + File.separator + ApplicationConstants.DB_CONFIG);
						removeUserNamePassword(Paths.get(shaedPathdbFile.toString()));
					}
					if (downlaodFilesFlag.get(ApplicationConstants.ZFJ_SUPPORT_CHECKLIST[3])) {
						if (!Objects.isNull(System.getProperty("catalina.base"))) {
							log.debug("catalina - tomcat root path  "+System.getProperty("catalina.base"));
							// Create and copy the ZFJ tomcat log files
							Path sourceTomcatFile = Paths.get(System.getProperty("catalina.base"));
							Path zfjTomcatLogPath = Paths.get(zfjSupportPath + File.separator + ApplicationConstants.TOMCAT_LOG);
							createNewDirectory(zfjTomcatLogPath.toString());
							File tomcatRoot = new File(sourceTomcatFile.toString() + File.separator + ApplicationConstants.LOGS);
							// Get the today catalina log files.
							getTheNewestFile(tomcatRoot.toString(), "."+ApplicationConstants.LOG, zfjTomcatLogPath.toFile());
						} else {
							log.debug("CATALINA BASE is not proper : [" + System.getProperty("catalina.base")+ "]");
						}
					}
					Path zfjsupportzip = Paths.get(zfjSupportPath + ApplicationConstants.ZIP_EXTENSION);
					// Zip all the files
					zipFolder(Paths.get(zfjSupportPath), zfjsupportzip);
					// once zipping was done. delete the folder.
					FileUtils.forceDelete(new File(zfjSupportPath));
					// get the final zip file
					file = zfjsupportzip.toFile();
					long endTime = System.nanoTime();
					long totalTimeSec = TimeUnit.NANOSECONDS.toSeconds(endTime - startTime);
					log.info("Job total Time taken in Seconds :::" + totalTimeSec);
					if (file != null) {
						// Download the zip file
						ApplicationProperties applicationProperties = (ApplicationProperties) ZephyrComponentAccessor
								.getInstance().getComponent("applicationProperties");
						String fileUrl = applicationProperties.getBaseUrl()
								+ "/plugins/servlet/export/exportSupportAttachment?fileName=" + file.getName();
						ob.put("url", fileUrl);
					} else {
						log.info(String.format(ERROR_LOG_MESSAGE, Status.SERVICE_UNAVAILABLE.getStatusCode(),
								Status.SERVICE_UNAVAILABLE, "Export data service is unavailable."));
						extractReorderErrorMessage();
					}
				} catch (Exception e) {
					log.error("Error occurred while zipping the zfj support tool  -> ", e);
					throw new RuntimeException(e.getMessage(), e);
				} finally {
					lock.unlock();
				}
			} else {
				JSONObject errorJson = new JSONObject();
				String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper()
						.getText("zephyr.je.admin.supporttool.already.in.progress");
				errorJson.put("alreadyInprocess", errorMessage);
				return Response.ok(errorJson.toString()).build();
			}
		} catch (InterruptedException | JSONException e) {
			log.error("error during getting lock ");
		}
		return Response.ok(ob.toString()).build();

	}
	
	public static void copyFilestoDir(File sourcePath, File desPath, String name) throws IOException {
		FilenameFilter start = new FilenameFilter() {
			public boolean accept(File directory, String filename) {
				return filename.startsWith(name);
			}
		};
		File[] catalinaFiles = sourcePath.listFiles(start);
		for (File cf : catalinaFiles) {
			//FileUtils.copyFileToDirectory(cf, desPath);
			 Files.copy(Paths.get(cf.toString()),Paths.get(desPath+File.separator+cf.getName()), StandardCopyOption.REPLACE_EXISTING );
		}
	}
	
	public static void getTheNewestFile(String filePath, String ext, File desPath) throws IOException, ParseException {
		File dir = new File(filePath);
		FileFilter fileFilter = new WildcardFileFilter(ApplicationConstants.CATALINA+ApplicationConstants.ALL_SHEET_REGEX + ext);
		File[] files = dir.listFiles(fileFilter);
		Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
		//displayFiles(files, desPath);
		int count =1;
		for (File file : files) {
			if(count<=5) {
				//FileUtils.copyFileToDirectory(file, desPath);
				Files.copy(Paths.get(file.toString()),Paths.get(desPath+File.separator+file.getName()), StandardCopyOption.REPLACE_EXISTING );
			}
			if(count==5)
				break;
			count++;
		}
	}
		
	/*public static void displayFiles(File[] files, File desPath) throws IOException {
		int count =1;
		for (File file : files) {
			if(count==5) {
				FileUtils.copyFileToDirectory(file, desPath);
				break;
			}
			count++;
		}
	}
	*/
	private Response extractReorderErrorMessage() throws JSONException {
		JSONObject jsonErrorObject = new JSONObject();
		String message = authContext.getI18nHelper().getText("zephyr-je.supporttool.select.zipfile.downloaded.error");
		jsonErrorObject.put("error", message);
		log.error(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(), Status.BAD_REQUEST, message));
		return Response.status(Status.BAD_REQUEST).entity(jsonErrorObject.toString())
				.cacheControl(ZephyrCacheControl.never()).build();
	}
	
	public static String getCurrentLocalDateTimeStamp() {
		return LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
	}
	
	private static void createNewDirectory(String path) throws IOException {
		Path newPath = Paths.get(new File(path).getAbsolutePath());
		if (!Files.exists(newPath)) {
			Files.createDirectories(newPath);
		}
	}
	 
	private static void zipFolder(Path sourceFolderPath, Path zipPath) throws Exception {
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()));
		Files.walkFileTree(sourceFolderPath, new SimpleFileVisitor<Path>() {
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				zos.putNextEntry(new ZipEntry(sourceFolderPath.relativize(file).toString()));
				Files.copy(file, zos);
				zos.closeEntry();
				return FileVisitResult.CONTINUE;
			}
		});
		zos.close();
	}

	@Override
	public boolean setDatacenterFlag(boolean dcStatusflag) throws IOException{
		DataCenterStatus dataCenterStatus = new DataCenterStatus();
		dataCenterStatus.setStatusFlag(dcStatusflag);
		JiraUtil.setBackupRecoveryByKey(DATACENTER_STATUS, getObjectMapper().writeValueAsString(dataCenterStatus));
		String dcStatus = JiraUtil.getBackupRecoveryByKey(DATACENTER_STATUS);
		if(StringUtils.isNotBlank(dcStatus)) {
			dataCenterStatus =getObjectMapper().readValue(dcStatus,DataCenterStatus.class);
		}
		return dataCenterStatus.isStatusFlag();
	}

	@Override
	public boolean getDatacenterFlag() throws IOException{
		DataCenterStatus dataCenterStatus = new DataCenterStatus();
		String dcStatus = JiraUtil.getBackupRecoveryByKey(DATACENTER_STATUS);
		if(StringUtils.isNotBlank(dcStatus)) {
			dataCenterStatus =getObjectMapper().readValue(dcStatus,DataCenterStatus.class);
		}
		return dataCenterStatus.isStatusFlag();
	}
	
	
	private ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
        return mapper;
    } 
	
	private void removeUserNamePassword(Path dbxmlPath) throws IOException {
		List<String> allLines = Files.readAllLines(dbxmlPath);
		File fout = new File(dbxmlPath.toString());
		FileOutputStream fos = new FileOutputStream(fout);

		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
		for (String line : allLines) {
			if (line.contains("<username>")) {
				String str = line.substring(0, line.length());
				line = line.replaceAll(str, "    <username>Sanitized by Zephyr Support Utility</username>");
			}
			if (line.contains("<password>")) {
				String str = line.substring(0, line.length());
				line = line.replaceAll(str, "    <password>Sanitized by Zephyr Support Utility</password>");
			}
			bw.write(line);
			bw.newLine();

		}
		bw.close();
	}

}
