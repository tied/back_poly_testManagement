package com.thed.zephyr.je.rest.delegate.impl;

import com.atlassian.beehive.compat.ClusterLock;
import com.atlassian.beehive.compat.ClusterLockService;
import com.atlassian.beehive.compat.ClusterLockServiceFactory;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.thed.zephyr.je.rest.delegate.IssueImporterResourceDelegate;
import com.thed.zephyr.je.rest.exception.ImporterException;
import com.thed.zephyr.je.service.FileImportService;
import com.thed.zephyr.je.service.IssueImporterService;
import com.thed.zephyr.je.service.JobProgressService;
import com.thed.zephyr.je.vo.ImportDetails;
import com.thed.zephyr.je.vo.ImportJob;
import com.thed.zephyr.util.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Sheet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class IssueImporterResourceDelegateImpl implements IssueImporterResourceDelegate {

	protected final Logger log = Logger.getLogger(IssueImporterResourceDelegateImpl.class);

	private static final String ERROR_LOG_MESSAGE = "[Error] [Error code: %s %s Error Message : %s]";
	private static final String PROJECT_ID = "Project Id";
	private static final String ISSUE_TYPE ="Issue type";
	private static final String FILE_TYPE ="File type";
	private static final String FILE_NOT_FOUND ="No file";
	private final JiraAuthenticationContext authContext;
	private final FileImportService fileService;
	private final IssueImporterService issueImporterService;
	private final JobProgressService jobProgressService;
	private final ClusterLockService clusterLockService;

	public IssueImporterResourceDelegateImpl(final JiraAuthenticationContext authContext
			, FileImportService fileService, IssueImporterService fileImportService, 
			final JobProgressService jobProgressService,
			final ClusterLockServiceFactory clusterLockServiceFactory) {
		this.authContext=authContext;
		this.fileService = fileService;
		this.issueImporterService = fileImportService;
		this.jobProgressService = jobProgressService;
		this.clusterLockService = clusterLockServiceFactory.getClusterLockService();
	}

	@Override
	public Response importFiles(HttpServletRequest request) {
		List<File> filesList = new ArrayList<File>();
		ImportJob importJob =  null;
		JSONObject jsonObjectResponse = new JSONObject();
		//Map<String, String> jsonObject = new HashMap<String, String>();
		try {
			importJob = (ImportJob)fileService.processMultiPartRequest(request, filesList);
			ExcelFileUtil.validateImportFile(authContext,filesList);
		}catch (ImporterException ex){
			return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, null, ex.getMessage(), null);
		}catch (Exception ex) {
			log.warn("Exception while processing MultiPartRequest:", ex);
			//throw new RESTException(Status.BAD_REQUEST, authContext.getI18nHelper().getText("zephyr.importer.general.exception") + ex.getMessage());
			//jsonObjectResponse.put("Error", ex.getMessage());
			return getResponse(ex.getMessage(), jsonObjectResponse, Status.INTERNAL_SERVER_ERROR);
			//Response.status(Status.BAD_REQUEST).entity()
		}
		final ApplicationUser user = authContext.getLoggedInUser();
		if(importJob != null) {
			String projectID = importJob.getProjectId();

			Project project = issueImporterService.getProject(user, projectID);
			if(project == null) {
				log.warn(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("zephyr.common.error.invalid", PROJECT_ID, projectID)));
				//throw new RESTException(Status.BAD_REQUEST, authContext.getI18nHelper().getText("zephyr.common.error.invalid", PROJECT_ID, projectID));
				return getResponse(authContext.getI18nHelper().getText("zephyr.common.error.invalid", PROJECT_ID, projectID), jsonObjectResponse, Status.BAD_REQUEST);
			}
			final String issueTypeStr = importJob.getIssueType();
			IssueType issueType = null;
			Collection<IssueType> issueTypes = project.getIssueTypes();
			if(StringUtils.isEmpty(issueTypeStr)){
				log.warn(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("zephyr.common.error.invalid", PROJECT_ID, projectID)));
				return getResponse(authContext.getI18nHelper().getText("zephyr.common.error.required", ISSUE_TYPE), jsonObjectResponse, Status.BAD_REQUEST);
			}
			if(issueTypes != null && issueTypes.size() > 0 && StringUtils.isNotEmpty(issueTypeStr)) {
				issueType = issueTypes.stream()
						.filter(elem -> issueTypeStr.equals(elem.getId()))
						.findFirst()
						.orElse(null);
			}
			if(issueType == null) {
				log.warn(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("zephyr.common.error.invalid", PROJECT_ID, projectID)));
				return getResponse(authContext.getI18nHelper().getText("zephyr.common.error.invalid", ISSUE_TYPE, issueTypeStr), jsonObjectResponse, Status.BAD_REQUEST);
			}
			if(StringUtils.isBlank(importJob.getFileType())){
				return getResponse(authContext.getI18nHelper().getText("zephyr.common.error.invalid", FILE_TYPE, importJob.getFileType()), jsonObjectResponse, Status.BAD_REQUEST);
			}
			if("excel".equals(importJob.getFileType()) && importJob.getImportDetails().getStartingRowNumber() < 2){
				return getResponse(authContext.getI18nHelper().getText("zephyr-je.pdb.importer.issueimport.row.number.greater.label", FILE_TYPE, importJob.getFileType()), jsonObjectResponse, Status.BAD_REQUEST);
			}
		}
		if(filesList != null && filesList.size() > 0) {
			
			String jobProgressToken = new UniqueIdGenerator().getStringId();
			try {
				jsonObjectResponse.put(ApplicationConstants.JOB_PROGRESS_TOKEN, jobProgressToken);
			} catch (JSONException e) {
				log.error("Exception while assigning job progress token", e);
			}
		    jobProgressService.createJobProgress(ApplicationConstants.IMPORT_CREATE_ISSUES_JOB_PROGRESS, 0, jobProgressToken);
		    final ImportJob job = importJob;

			final String lockName = "zephyr-import-issue";
			final ClusterLock lock = clusterLockService.getLockForName(lockName);

		    try {
				ImportJob finalImportJob = importJob;
				Runnable runnable = () -> {
					try{
							if (lock.tryLock(1, TimeUnit.SECONDS)) {
								try {
									// setting user in auth context as it wouldn't be available on default auth context
									if(authContext != null && authContext.getLoggedInUser() == null) authContext.setLoggedInUser(user);

									final int[] rowCount = {0};
									List<Sheet> sheets = new ArrayList<>();
									ExcelFileUtil excelFileUtil = new ExcelFileUtil();
									try {
										ImportDetails importDetails = finalImportJob.getImportDetails();
										String sheetFilter = importDetails.getSheetFilter();
										//for xls/xlsx
										if (finalImportJob.isFileTypeExcel()) {

											sheets = excelFileUtil.getAllSheetsByFile(finalImportJob, filesList.get(0));

											if (!importDetails.isImportAllSheetsFlag()) {
												Sheet sheet = sheets.get(0);
												rowCount[0] = getTotalRowCount(finalImportJob, rowCount[0], sheet);
											} else {
												if (StringUtils.isNotEmpty(sheetFilter)) {
													sheets.forEach(sheet -> {
														if (Pattern.matches(sheetFilter, sheet.getSheetName())) {
															rowCount[0] = getTotalRowCount(finalImportJob, rowCount[0], sheet);
														}
													});
												}
											}
										}

										//for xml
										if (finalImportJob.isFileTypeXml()) {
											Element elements = null;
											try {
												DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
												DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
												Document doc = docBuilder.parse(filesList.get(0));
												elements = doc.getDocumentElement();
											} catch (Exception e) {
											} finally {
												rowCount[0] = elements.getChildNodes().getLength();
											}
										}

									} catch (Exception ex) {
									} finally {
										jobProgressService.setTotalSteps(jobProgressToken, rowCount[0]);
									}
									List<Sheet> finalSheets = sheets;
									File file = filesList.get(0);
									issueImporterService.importFile(authContext, jobProgressService, jobProgressToken, job, file, user, finalSheets);

								}catch (Exception ex){ }
								finally {
									lock.unlock();
									authContext.setLoggedInUser(null);
								}
							}else{
								String inProgressMsg = authContext.getI18nHelper().getText("zephyr-je.pdb.importer.in.progress");
								jobProgressService.setMessage(jobProgressToken,inProgressMsg);
							}
						} catch (InterruptedException e) {
							String error = "import issue operation interrupted";
							log.error("import issue operation : " + error, e);
						}
					};
					Executors.newSingleThreadExecutor().submit(runnable);
					jsonObjectResponse.put(ApplicationConstants.JOB_PROGRESS_TOKEN, jobProgressToken);


			} catch(Exception ex) {
				log.error("Exception while importing files", ex);
				//throw new RESTException(Status.BAD_REQUEST, authContext.getI18nHelper().getText("zephyr.importer.general.exception") + ex.getMessage());
				return getResponse(ex.getMessage(), jsonObjectResponse, Status.INTERNAL_SERVER_ERROR);
			}
		} else {
			log.warn(String.format(ERROR_LOG_MESSAGE, Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("zephyr.common.error.invalid", FILE_NOT_FOUND, "")));
			//throw new RESTException(Status.BAD_REQUEST, authContext.getI18nHelper().getText("zephyr.common.error.invalid", FILE_NOT_FOUND, ""));
			return getResponse(authContext.getI18nHelper().getText("zephyr.common.error.invalid", FILE_NOT_FOUND, ""), jsonObjectResponse, Status.BAD_REQUEST);
		}
		return Response.status(Status.OK).entity(jsonObjectResponse.toString()).build();
		//return Response.ok(jsonObjectResponse.toString()).build();
	}

	private Integer getTotalRowCount(ImportJob importJob, Integer issuesCount, Sheet sheet) {
		Integer rowStart = importJob.getImportDetails().getStartingRowNumber() - 1;
		rowStart = rowStart > 0 ? rowStart : 0;
		Integer sheetRowCount = (sheet.getPhysicalNumberOfRows() - rowStart);
		if (sheetRowCount > 0) {
            issuesCount += sheetRowCount;
        }
		return issuesCount;
	}

	@Override
	public Map<String, String> extractIssueMapping(HttpServletRequest request) throws Exception {
		List<File> filesList = new ArrayList<File>();
		ImportJob importJob = null;
		importJob = (ImportJob)fileService.processMultiPartRequest(request, filesList);
		ExcelFileUtil.validateImportFile(authContext, filesList);
		File file = filesList.get(0);
		ExcelFileUtil excelFileUtil = new ExcelFileUtil();
		List<Sheet> sheets = excelFileUtil.getAllSheetsByFile(importJob,file);
		if(importJob.isFileTypeExcel() && (sheets == null || sheets.size() == 0)){
			throw new ImporterException(authContext.getI18nHelper().getText("zephyr-je.pdb.importer.sheet.filter.regex.error"));
		}
		return issueImporterService.extractIssueMapping(importJob,file, sheets);
	}



	private Response getResponse(String errorMessage, JSONObject jsonObject, Status status) {
		try {
			jsonObject.put("error", errorMessage);
		} catch (JSONException e) {
		}
		return Response.status(status).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
	}
}
