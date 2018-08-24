package com.thed.zephyr.je.rest.delegate;

import java.io.IOException;
import java.util.Map;

import javax.ws.rs.core.Response;

import com.atlassian.jira.util.json.JSONObject;

/**
 * @author manjunath
 *
 */
public interface DatacenterResourceDelegate {
	
	Response getSupporttoolFiles(Map<String, Boolean> downlaodFilesFlag) throws Exception;
	
	JSONObject getDataForIntegrityCheck(boolean isTotalExecutionCount, boolean isTotalCycleCount, boolean isExecutionCountByCycle, boolean isExecutionCountByFolder,
    		boolean isIssueCountByProject, boolean isTeststepResultCountByExecution, boolean isTeststepCountByIssue, Integer offset, Integer limit) throws Exception;
	
	Response indexAllUsingCron(String expression,boolean flag,String userId) throws Exception;
	
	Response recoverIndexBackup(String fullIndexRecoveryFilePath, String indexRecoveryFileName) throws Exception;
	
	Response getrecoveryForm() throws Exception;
	
	Response exportIntegrityCheckData(boolean isTotalExecutionCount, boolean isTotalCycleCount, boolean isExecutionCountByCycle, boolean isExecutionCountByFolder,
    		boolean isIssueCountByProject, boolean isTeststepResultCountByExecution, boolean isTeststepCountByIssue, Integer offset, Integer limit) throws Exception;
	
	boolean setDatacenterFlag(boolean dcStatus) throws IOException;
	boolean getDatacenterFlag() throws IOException;
	
}
